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
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.S3Clients;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.S3AccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests for {@link ReactiveS3Accessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveS3AccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the metric fixtures it was built from, so tests
     * can both drive the accessor and verify the shared spine's interactions (e.g. timer
     * recording).
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Timer timer;
        final Timer.Context timerContext;
        final MetricRegistry metricRegistry;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer, final Timer.Context timerContext,
                              final MetricRegistry metricRegistry, final Tracer tracer, final Span span,
                              final Mediator mediator) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.metricRegistry = metricRegistry;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveS3AccessorTest.class));

            final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
            final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
            final Timer timer = Mockito.mock(Timer.class);
            final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
            final Counter errorCounter = Mockito.mock(Counter.class);
            Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
            Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
            Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
            Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(errorCounter);
            Mockito.when(timer.time()).thenReturn(timerContext);

            final TracerService tracerService = Mockito.mock(TracerService.class);
            final Tracer tracer = Mockito.mock(Tracer.class);
            final Tracer.SpanBuilder spanBuilder = Mockito.mock(Tracer.SpanBuilder.class);
            final Span span = Mockito.mock(Span.class);
            Mockito.when(appbaseCore.getTracerService()).thenReturn(tracerService);
            Mockito.when(tracerService.getTracer()).thenReturn(tracer);
            Mockito.when(tracer.buildSpan(Mockito.anyString())).thenReturn(spanBuilder);
            Mockito.when(spanBuilder.start()).thenReturn(span);

            final MediatorService mediatorService = Mockito.mock(MediatorService.class);
            final Mediator mediator = Mockito.mock(Mediator.class);
            Mockito.when(appbaseCore.getMediatorService()).thenReturn(mediatorService);
            Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
            Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

            return new CoreFixtures(appbaseCore, timer, timerContext, metricRegistry, tracer, span, mediator);
        }
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<S3Clients, ClusterIdValue> mockIntegrationService(
            final S3Clients s3Clients) {
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(s3Clients);
        return integrationService;
    }

    private static S3Clients mockS3Clients(final S3AsyncClient s3AsyncClient) {
        final S3Clients s3Clients = Mockito.mock(S3Clients.class);
        Mockito.when(s3Clients.getS3AsyncClient()).thenReturn(s3AsyncClient);
        return s3Clients;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveS3Accessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>> borrow) {
        return new ReactiveS3Accessor(core.appbaseCore, mockReactorService(), configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    /**
     * {@link ResponseBytes} is a {@code final} class, so it cannot be mocked; the real static
     * factory is used instead, with an empty {@link GetObjectResponse} since only the bytes are
     * asserted on.
     */
    private static ResponseBytes<GetObjectResponse> responseBytes(final byte[] bytes) {
        return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes);
    }

    @Test
    @SuppressWarnings("unchecked")
    void readObjectAsBytesEmitsBytes() {
        final byte[] payload = {1, 2, 3};
        final S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        Mockito.when(s3AsyncClient.getObject(Mockito.any(GetObjectRequest.class), Mockito.any(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.completedFuture(responseBytes(payload)));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3AsyncClient));
        final ReactiveS3Accessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.readObjectAsBytes("read", "my-bucket", "object-1"))
                .assertNext(bytes -> assertArrayEquals(payload, bytes))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedFutureSurfacesS3AccessorException() {
        final S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        Mockito.when(s3AsyncClient.getObject(Mockito.any(GetObjectRequest.class), Mockito.any(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        (S3Exception) S3Exception.builder().message("missing").build()));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3AsyncClient));
        final ReactiveS3Accessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.readObjectAsBytes("read", "my-bucket", "object-1"))
                .expectError(S3AccessorException.class)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void integrationServiceReturnedOnSuccessAndError() {
        final S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        Mockito.when(s3AsyncClient.getObject(
                        Mockito.eq(GetObjectRequest.builder().bucket("my-bucket").key("ok").build()),
                        Mockito.any(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.completedFuture(responseBytes(new byte[] {9})));
        Mockito.when(s3AsyncClient.getObject(
                        Mockito.eq(GetObjectRequest.builder().bucket("my-bucket").key("err").build()),
                        Mockito.any(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        (S3Exception) S3Exception.builder().message("x").build()));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3AsyncClient));
        final ReactiveS3Accessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        accessor.readObjectAsBytes("read", "my-bucket", "ok").block();
        accessor.readObjectAsBytes("read", "my-bucket", "err").onErrorResume(e -> Mono.empty()).block();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void deleteObjectEmitsTrue() {
        final S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        Mockito.when(s3AsyncClient.deleteObject(DeleteObjectRequest.builder().bucket("my-bucket").key("object-1").build()))
                .thenReturn(CompletableFuture.completedFuture(DeleteObjectResponse.builder().build()));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3AsyncClient));
        final ReactiveS3Accessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.deleteObject("delete", "my-bucket", "object-1"))
                .expectNext(Boolean.TRUE)
                .verifyComplete();
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void listObjectsEmitsKeys() {
        final S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        final ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("prefix/a").build(),
                        S3Object.builder().key("prefix/b").build())
                .build();
        Mockito.when(s3AsyncClient.listObjectsV2(
                        ListObjectsV2Request.builder().bucket("my-bucket").prefix("prefix/").build()))
                .thenReturn(CompletableFuture.completedFuture(response));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3AsyncClient));
        final ReactiveS3Accessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.listObjects("list", "my-bucket", "prefix/"))
                .expectNext("prefix/a", "prefix/b")
                .verifyComplete();
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    @SuppressWarnings("unchecked")
    void timerRecordedForSuccessfulOperation() {
        final S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        Mockito.when(s3AsyncClient.getObject(Mockito.any(GetObjectRequest.class), Mockito.any(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.completedFuture(responseBytes(new byte[] {1})));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3AsyncClient));
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveS3Accessor accessor = accessor(core, defaultConfig(), () -> integrationService);

        accessor.readObjectAsBytes("read", "my-bucket", "object-1").block();

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
