/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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

package com.viveworks.appbase.client.cloud.aws.s3.accessor.reactive;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.S3Clients;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.S3AccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for AWS S3, over AWS SDK v2's {@link S3AsyncClient}. This is the true
 * non-blocking face of the S3 twin: {@link S3AsyncClient} is backed by the netty-nio HTTP
 * transport, its driver calls return {@link CompletableFuture}, and each one is bridged into the
 * Reactor world with {@link Mono#fromFuture(Supplier)} inside the full {@link #mono(String,
 * Supplier)} spine (timeout, retry, scheduler, metrics, tracing, and exception-store reporting are
 * all supplied by the inherited {@link AbstractReactiveAccessor}). The synchronous twin (Task 4 of
 * this plan) instead uses the real blocking {@code S3Client} -- it never blocks on the async
 * client, since blocking a non-blocking client's completion still ties up a thread for the
 * duration of the call, defeating the purpose of using it.
 *
 * <p>Each operation borrows one pooled {@link AppbaseClientIntegrationService} per subscription
 * and returns it to the pool on completion, error, or cancellation via {@link
 * Mono#usingWhen(org.reactivestreams.Publisher, Function, Function)} / {@link
 * Flux#usingWhen(org.reactivestreams.Publisher, Function, Function)}. There is deliberately no
 * eager client build or cache: the pooled {@link S3Clients} IS the client, fetched lazily on each
 * subscription (Cassandra-shaped, keyed-free acquisition).
 *
 * <p>The accessor stays at the S3 {@code bucket}+{@code key} driver-primitive level: {@code
 * bucket} is an argument on every named op (not fixed at construction), so one accessor instance
 * serves any bucket. The op set is the small primitive vocabulary (read/write/delete an object,
 * list a bucket by prefix) plus the generic {@link #command(String, Function)} escape hatch for
 * anything else -- deliberately no bespoke higher-level conveniences (e.g. the old {@code
 * ObjectStoreAccessor}'s base-directory markers), which are reachable via {@link #listObjects(
 * String, String, String)}/{@link #command(String, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveS3Accessor extends AbstractReactiveAccessor {

    private final Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled S3 integration service, invoked once per subscription
     */
    public ReactiveS3Accessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Read an object's full content as a byte array.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key of the object to read
     * @return Wrapped {@link Mono} of the object's bytes
     */
    public Mono<byte[]> readObjectAsBytes(final String op, final String bucket, final String key) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        return call(op, client -> client.getObject(getObjectRequest(bucket, key), AsyncResponseTransformer.toBytes()))
                .map(ResponseBytes::asByteArray);
    }

    /**
     * Read an object's full content directly into a local file.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key of the object to read
     * @param destination Local file path the object content is written to
     * @return Wrapped {@link Mono} of {@code destination}
     */
    public Mono<Path> readObjectAsFile(final String op, final String bucket, final String key, final Path destination) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notNull(destination, "destination");
        return call(op, client -> client.getObject(getObjectRequest(bucket, key), AsyncResponseTransformer.toFile(destination)))
                .thenReturn(destination);
    }

    /**
     * Write an object from an in-memory byte array.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key to write to
     * @param bytes Object content
     * @return Wrapped {@link Mono} of {@code key}
     */
    public Mono<String> writeObject(final String op, final String bucket, final String key, final byte[] bytes) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notNull(bytes, "bytes");
        return call(op, client -> client.putObject(putObjectRequest(bucket, key), AsyncRequestBody.fromBytes(bytes)))
                .thenReturn(key);
    }

    /**
     * Write an object from a local file.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key to write to
     * @param sourcePath Local file path to read the object content from
     * @return Wrapped {@link Mono} of {@code key}
     */
    public Mono<String> writeObject(final String op, final String bucket, final String key, final Path sourcePath) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        AppbaseUtil.notNull(sourcePath, "sourcePath");
        return call(op, client -> client.putObject(putObjectRequest(bucket, key), AsyncRequestBody.fromFile(sourcePath)))
                .thenReturn(key);
    }

    /**
     * Delete an object.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param key S3 key of the object to delete
     * @return Wrapped {@link Mono} emitting {@code true} once the delete completes
     */
    public Mono<Boolean> deleteObject(final String op, final String bucket, final String key) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notBlank(key, "key");
        return call(op, client -> client.deleteObject(deleteObjectRequest(bucket, key)))
                .thenReturn(Boolean.TRUE);
    }

    /**
     * List the keys of the objects in a bucket under a prefix, streamed as a {@link Flux}. Bridges
     * the single {@link S3AsyncClient#listObjectsV2(ListObjectsV2Request)} {@link
     * CompletableFuture} into the {@link #flux(String, Supplier)} spine, then flattens the
     * response's contents into a stream of keys. This is the primitive over which higher-level
     * directory-style behavior (existence checks, prefix sweeps) is built by callers; the accessor
     * itself keeps no bespoke directory conveniences.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param bucket S3 bucket name
     * @param prefix Key prefix to list under (empty string lists the whole bucket)
     * @return Wrapped {@link Flux} of the matching object keys
     */
    public Flux<String> listObjects(final String op, final String bucket, final String prefix) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notBlank(bucket, "bucket");
        AppbaseUtil.notNull(prefix, "prefix");
        final ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
        return flux(op, () -> Flux.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.fromFuture(() -> svc.getClient().getS3AsyncClient().listObjectsV2(request))
                        .flatMapMany(response -> Flux.fromIterable(response.contents()).map(S3Object::key)),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Run a general {@link S3AsyncClient} driver call: borrows a pooled integration service per
     * subscription, applies {@code fn} to its {@link S3AsyncClient}, bridges the returned {@link
     * CompletableFuture} into a {@link Mono}, and returns the integration service to the pool on
     * completion, error, or cancellation. Escape hatch for callers needing request shapes beyond
     * the named ops above.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from an {@link S3AsyncClient} to its driver {@link CompletableFuture}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op, final Function<S3AsyncClient, CompletableFuture<T>> fn) {
        return call(op, fn);
    }

    /**
     * Maps AWS SDK failures to {@link S3AccessorException}; all other throwables delegate to the
     * inherited default mapping. Uses the shared static helper in {@link S3AccessorException} so
     * behavior stays identical to the synchronous twin.
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

    private <T> Mono<T> call(final String op, final Function<S3AsyncClient, CompletableFuture<T>> fn) {
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.fromFuture(() -> fn.apply(svc.getClient().getS3AsyncClient())),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
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
