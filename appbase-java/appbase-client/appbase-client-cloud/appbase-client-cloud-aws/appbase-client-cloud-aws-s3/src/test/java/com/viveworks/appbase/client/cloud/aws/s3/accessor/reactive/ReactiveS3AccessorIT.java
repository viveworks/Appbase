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

import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkCommonUtil;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.s3.DefaultS3ClientsService;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientServiceTestHelper;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.S3Accessor;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.S3AccessorException;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Gated, live-backend integration test for the S3 twin's two accessor faces exercised end to end
 * against a real (or emulated, e.g. MinIO) S3 endpoint. It drives the reactive {@link
 * ReactiveS3Accessor} (over {@code S3AsyncClient}) and the synchronous {@link S3Accessor} (over the
 * real blocking {@code S3Client}) through the same object round-trip, proving both faces share
 * identical op vocabulary and error semantics.
 *
 * <p><b>Gated by default.</b> Like the other live S3 tests in this module ({@code
 * DefaultS3ClientServiceTest}, {@code S3ClientServiceGuiceModuleTest}) it is annotated {@link
 * EnabledIfSystemProperty} on {@code S3ClientServiceTests.enabled=true} and is SKIPPED in normal
 * builds. It reuses that same bootstrap helper, {@link S3ClientServiceTestHelper}, to build a live
 * {@link DefaultS3ClientsService} from the {@code appbase-client-aws-s3.yaml} classpath config, and
 * a bucket is created up front / torn down after via the pooled sync {@code S3Client}. Surefire's
 * default includes do not select {@code *IT} classes, but this module now binds {@code
 * maven-failsafe-plugin} in {@code develop-profile} / {@code staging-profile} / {@code
 * release-profile} (N05, internal work item 1295), so Failsafe selects it under {@code verify} once the gate is on:
 * {@code mvn -pl appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3
 * -am verify -P develop-profile -DS3ClientServiceTests.enabled=true}.
 *
 * <p><b>The backend is MinIO.</b> {@code appbase-client-aws-s3.yaml} uses the loopback, path-style
 * endpoint supplied by this module's {@code infra/docker/s3} lifecycle. This proves the sync and
 * async accessor behavior and {@code NoSuchKey} mapping, not AWS IAM or throttling semantics.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "S3ClientServiceTests.enabled", matches = "true")
class ReactiveS3AccessorIT {

    private static final String BUCKET = "appbase-reactive-s3-it-" + UUID.randomUUID();
    private static final String REACTIVE_KEY = "reactive/object-" + UUID.randomUUID() + ".txt";
    private static final String SYNC_KEY = "sync/object-" + UUID.randomUUID() + ".txt";
    private static final byte[] REACTIVE_PAYLOAD = "reactive-s3-payload".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SYNC_PAYLOAD = "sync-s3-payload".getBytes(StandardCharsets.UTF_8);

    private static DefaultS3ClientsService service;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        AwsSdkCommonUtil.registerObjectMapperSubtypes();
        DefaultAwsSdkHttpClientService.registerObjectMapperSubtypes();
        DefaultAwsSdkAsyncHttpClientService.registerObjectMapperSubtypes();
        DefaultS3ClientsService.registerObjectMapperSubtypes();

        service = S3ClientServiceTestHelper.defaultS3ClientService();
        rawS3Client().createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }

    @AfterAll
    @SuppressWarnings("checkstyle:IllegalCatch")
    static void tearDown() {
        if (service != null) {
            try {
                final S3Client s3Client = rawS3Client();
                for (final String key : List.of(REACTIVE_KEY, SYNC_KEY)) {
                    try {
                        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET).key(key).build());
                    } catch (final Exception ignored) {
                        // best-effort cleanup
                    }
                }
                try {
                    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET).build());
                } catch (final Exception ignored) {
                    // best-effort cleanup
                }
            } catch (final Exception ignored) {
                // best-effort cleanup
            } finally {
                service.shutdown();
            }
        }
    }

    /**
     * Reactive round-trip through {@link ReactiveS3Accessor}: write bytes, read them back (payload
     * asserted), confirm the key is listed under its prefix, delete it, then confirm a read of the
     * now-missing key surfaces as {@link S3AccessorException} (a {@code NoSuchKeyException} mapped
     * through the shared spine). Every stage is verified with {@link StepVerifier}.
     */
    @Test
    void reactiveRoundTrip() {
        final ReactiveS3Accessor reactive = service.getReactiveAccessor();

        StepVerifier.create(reactive.writeObject("write", BUCKET, REACTIVE_KEY, REACTIVE_PAYLOAD))
                .expectNext(REACTIVE_KEY)
                .verifyComplete();

        StepVerifier.create(reactive.readObjectAsBytes("read", BUCKET, REACTIVE_KEY))
                .expectNextMatches(bytes -> Arrays.equals(bytes, REACTIVE_PAYLOAD))
                .verifyComplete();

        StepVerifier.create(reactive.listObjects("list", BUCKET, "reactive/").collectList())
                .expectNextMatches(keys -> keys.contains(REACTIVE_KEY))
                .verifyComplete();

        StepVerifier.create(reactive.deleteObject("delete", BUCKET, REACTIVE_KEY))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        StepVerifier.create(reactive.readObjectAsBytes("read-missing", BUCKET, REACTIVE_KEY))
                .expectError(S3AccessorException.class)
                .verify();
    }

    /**
     * Synchronous round-trip through {@link S3Accessor}: the same write → read → list → delete →
     * read-missing lifecycle as the reactive twin, asserted with blocking return values, ending in
     * a thrown {@link S3AccessorException} for the missing key.
     *
     * @throws Exception on an unexpected driver failure on the non-error stages
     */
    @Test
    void syncRoundTrip() throws Exception {
        final S3Accessor sync = service.getAccessor();

        Assertions.assertEquals(SYNC_KEY, sync.writeObject("write", BUCKET, SYNC_KEY, SYNC_PAYLOAD));

        Assertions.assertArrayEquals(SYNC_PAYLOAD, sync.readObjectAsBytes("read", BUCKET, SYNC_KEY));

        final List<String> keys = sync.listObjects("list", BUCKET, "sync/");
        Assertions.assertTrue(keys.contains(SYNC_KEY));

        Assertions.assertTrue(sync.deleteObject("delete", BUCKET, SYNC_KEY));

        Assertions.assertThrows(
                S3AccessorException.class, () -> sync.readObjectAsBytes("read-missing", BUCKET, SYNC_KEY));
    }

    /** Proves both accessor faces surface a request to a bucket that does not exist. */
    @Test
    void missingBucketIsRejected() {
        final String missingBucket = "appbase-missing-bucket-" + UUID.randomUUID();
        final ReactiveS3Accessor reactive = service.getReactiveAccessor();
        StepVerifier.create(reactive.readObjectAsBytes("read-missing-bucket", missingBucket, "object"))
                .expectErrorSatisfies(error -> assertS3Status(error, 404))
                .verify();

        final S3Accessor sync = service.getAccessor();
        final S3AccessorException error = Assertions.assertThrows(
                S3AccessorException.class,
                () -> sync.readObjectAsBytes("read-missing-bucket", missingBucket, "object"));
        assertS3Status(error, 404);
    }

    /** Proves MinIO refuses the test endpoint when presented with invalid emulator credentials. */
    @Test
    void badCredentialsAreRejected() {
        try (S3Client invalidCredentialsClient = S3Client.builder()
                .endpointOverride(URI.create("http://localhost:9000"))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("invalid-access-key", "invalid-secret-key")))
                .build())
        {
            final S3Exception error = Assertions.assertThrows(
                    S3Exception.class, invalidCredentialsClient::listBuckets);
            Assertions.assertEquals(403, error.statusCode());
        }
    }

    private static void assertS3Status(final Throwable throwable, final int expectedStatus) {
        Assertions.assertInstanceOf(S3AccessorException.class, throwable);
        Assertions.assertInstanceOf(S3Exception.class, throwable.getCause());
        Assertions.assertEquals(expectedStatus, ((S3Exception) throwable.getCause()).statusCode());
    }

    private static S3Client rawS3Client() {
        return service.getClientIntegrationService().getClient().getS3Client();
    }
}
