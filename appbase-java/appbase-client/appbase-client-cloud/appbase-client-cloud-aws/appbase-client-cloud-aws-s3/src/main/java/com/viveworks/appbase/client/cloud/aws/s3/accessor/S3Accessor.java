/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.client.cloud.aws.s3.accessor;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.S3Clients;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.reactive.ReactiveS3Accessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Synchronous accessor for AWS S3, the blocking twin of {@link ReactiveS3Accessor}. It runs over
 * AWS SDK v2's real blocking {@link S3Client} (the apache-client HTTP transport) fetched via {@code
 * svc.getClient().getS3Client()} -- it deliberately never blocks on the {@code S3AsyncClient},
 * since blocking a non-blocking client's completion still ties up a thread for the duration of the
 * call, defeating the purpose of using it. Each driver call returns a plain blocking value (not a
 * {@code Mono}/{@code Flux}); the cross-cutting spine (timer, span, error counter, failure logging,
 * and exception-store reporting) is supplied by the inherited {@link AbstractAppbaseAccessor} via
 * {@link AbstractAppbaseAccessor#execute(String, java.util.concurrent.Callable)}.
 *
 * <p>Each operation borrows one pooled {@link AppbaseClientIntegrationService} per call and returns
 * it to the pool in a {@code finally} block (on both success and error). There is deliberately no
 * eager client build or cache: the pooled {@link S3Clients} IS the client, fetched lazily on each
 * call (Cassandra-shaped, keyed-free acquisition).
 *
 * <p>The accessor stays at the S3 {@code bucket}+{@code key} driver-primitive level: {@code bucket}
 * is an argument on every named op (not fixed at construction), so one accessor instance serves any
 * bucket. The op set mirrors {@link ReactiveS3Accessor} exactly (read/write/delete an object, list
 * a bucket by prefix) plus the generic {@link #command(String, Function)} escape hatch.
 *
 * <p>AWS SDK v2 sync driver failures are unchecked ({@code SdkException extends RuntimeException}),
 * so a plain {@link Function} suffices as the driver lambda -- no bespoke checked functional
 * interface is needed. Those failures are unwrapped and mapped to {@link S3AccessorException} via
 * {@link S3AccessorException#map(Throwable, Function)} in {@link #mapError(Throwable)}, so this
 * synchronous face exposes identical error semantics to the reactive twin.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class S3Accessor extends AbstractAppbaseAccessor {

    private final ReactiveOperationConfiguration configuration;
    private final Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration (supplies the metrics/trace/
     *         exception-store gate flags shared with the reactive twin)
     * @param borrow Supplier of a pooled S3 integration service, invoked once per operation
     */
    public S3Accessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>> borrow) {
        super(appbaseCore,
                configuration.isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.configuration = AppbaseUtil.notNull(configuration, "configuration");
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * The reactive operation configuration this accessor was built from. Mirrors {@link
     * ReactiveS3Accessor#getReactiveOperationConfiguration()} so the synchronous and reactive twins
     * expose an identical acquisition contract.
     *
     * @return the reactive operation configuration
     */
    public ReactiveOperationConfiguration getReactiveOperationConfiguration() {
        return configuration;
    }

    /**
     * Read an object's full content as a byte array.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key of the object to read
     * @return the object's bytes
     * @throws Exception the mapped failure, if the driver call fails
     */
    public byte[] readObjectAsBytes(final String op, final String bucket, final String key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        return run(op, client -> client.getObject(getObjectRequest(bucket, key), ResponseTransformer.toBytes())
                .asByteArray());
    }

    /**
     * Read an object's full content directly into a local file.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key of the object to read
     * @param destination Local file path the object content is written to
     * @return {@code destination}
     * @throws Exception the mapped failure, if the driver call fails
     */
    public Path readObjectAsFile(final String op, final String bucket, final String key, final Path destination)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notNull(destination, "destination");
        return run(op, client -> {
            client.getObject(getObjectRequest(bucket, key), ResponseTransformer.toFile(destination));
            return destination;
        });
    }

    /**
     * Write an object from an in-memory byte array.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key to write to
     * @param bytes Object content
     * @return {@code key}
     * @throws Exception the mapped failure, if the driver call fails
     */
    public String writeObject(final String op, final String bucket, final String key, final byte[] bytes)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notNull(bytes, "bytes");
        return run(op, client -> {
            client.putObject(putObjectRequest(bucket, key), RequestBody.fromBytes(bytes));
            return key;
        });
    }

    /**
     * Write an object from a local file.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key to write to
     * @param sourcePath Local file path to read the object content from
     * @return {@code key}
     * @throws Exception the mapped failure, if the driver call fails
     */
    public String writeObject(final String op, final String bucket, final String key, final Path sourcePath)
            throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notNull(sourcePath, "sourcePath");
        return run(op, client -> {
            client.putObject(putObjectRequest(bucket, key), RequestBody.fromFile(sourcePath));
            return key;
        });
    }

    /**
     * Delete an object.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key of the object to delete
     * @return {@code true} once the delete completes
     * @throws Exception the mapped failure, if the driver call fails
     */
    public boolean deleteObject(final String op, final String bucket, final String key) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        return run(op, client -> {
            client.deleteObject(deleteObjectRequest(bucket, key));
            return Boolean.TRUE;
        });
    }

    /**
     * List the keys of the objects in a bucket under a prefix. Collects the single {@link
     * S3Client#listObjectsV2(ListObjectsV2Request)} response's contents into a {@link List} of
     * keys. This is the primitive over which higher-level directory-style behavior (existence
     * checks, prefix sweeps) is built by callers; the accessor itself keeps no bespoke directory
     * conveniences.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param prefix Key prefix to list under (empty string lists the whole bucket)
     * @return the matching object keys
     * @throws Exception the mapped failure, if the driver call fails
     */
    public List<String> listObjects(final String op, final String bucket, final String prefix) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notNull(prefix, "prefix");
        final ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
        return run(op, client -> client.listObjectsV2(request).contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList()));
    }

    /**
     * Run a general {@link S3Client} driver call: borrows a pooled integration service, applies
     * {@code fn} to its {@link S3Client}, and returns the integration service to the pool on
     * completion or error. Escape hatch for callers needing request shapes beyond the named ops
     * above.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from an {@link S3Client} to its driver result
     * @param <T> Value type
     * @return the driver result
     * @throws Exception the mapped failure, if the driver call fails
     */
    public <T> T command(final String op, final Function<S3Client, T> fn) throws Exception {
        return run(op, fn);
    }

    /**
     * Maps AWS SDK failures to {@link S3AccessorException}; all other throwables delegate to the
     * inherited default mapping. Uses the shared static helper in {@link S3AccessorException} so
     * behavior stays identical to the reactive twin. The unwrap handles the {@link SdkException}
     * base of all AWS SDK v2 errors.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return S3AccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level S3 error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }

    private <T> T run(final String op, final Function<S3Client, T> fn) throws Exception {
        return execute(op, () -> {
            final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> svc = borrow.get();
            try {
                return fn.apply(svc.getClient().getS3Client());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    private static GetObjectRequest getObjectRequest(final String bucket, final String key) {
        return GetObjectRequest.builder().bucket(bucket).key(key).build();
    }

    private static PutObjectRequest putObjectRequest(final String bucket, final String key) {
        return PutObjectRequest.builder().bucket(bucket).key(key).build();
    }

    private static DeleteObjectRequest deleteObjectRequest(final String bucket, final String key) {
        return DeleteObjectRequest.builder().bucket(bucket).key(key).build();
    }
}
