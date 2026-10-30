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
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.S3Clients;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link S3Accessor}, the synchronous S3 twin over the real blocking {@link S3Client}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class S3AccessorTest {

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
                    .thenReturn(LoggerFactory.getLogger(S3AccessorTest.class));

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

    private static S3Clients mockS3Clients(final S3Client s3Client) {
        final S3Clients s3Clients = Mockito.mock(S3Clients.class);
        Mockito.when(s3Clients.getS3Client()).thenReturn(s3Client);
        return s3Clients;
    }

    private static S3Accessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<S3Clients, ClusterIdValue>> borrow) {
        return new S3Accessor(core.appbaseCore, configuration, borrow);
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
    void readObjectAsBytesReturnsBytes() throws Exception {
        final byte[] payload = {1, 2, 3};
        final S3Client s3Client = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.getObject(Mockito.any(GetObjectRequest.class), Mockito.any(ResponseTransformer.class)))
                .thenReturn(responseBytes(payload));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final S3Accessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        assertArrayEquals(payload, accessor.readObjectAsBytes("read", "my-bucket", "object-1"));
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void writeObjectBytesReturnsKey() throws Exception {
        final S3Client s3Client = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final S3Accessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        assertEquals("object-1", accessor.writeObject("write", "my-bucket", "object-1", new byte[] {7}));
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void deleteObjectReturnsTrue() throws Exception {
        final S3Client s3Client = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.deleteObject(DeleteObjectRequest.builder().bucket("my-bucket").key("object-1").build()))
                .thenReturn(DeleteObjectResponse.builder().build());
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final S3Accessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        assertTrue(accessor.deleteObject("delete", "my-bucket", "object-1"));
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void listObjectsReturnsKeys() throws Exception {
        final S3Client s3Client = Mockito.mock(S3Client.class);
        final ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("prefix/a").build(),
                        S3Object.builder().key("prefix/b").build())
                .build();
        Mockito.when(s3Client.listObjectsV2(
                        ListObjectsV2Request.builder().bucket("my-bucket").prefix("prefix/").build()))
                .thenReturn(response);
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final S3Accessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        assertEquals(List.of("prefix/a", "prefix/b"), accessor.listObjects("list", "my-bucket", "prefix/"));
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    @SuppressWarnings("unchecked")
    void integrationServiceReturnedOnSuccessAndError() throws Exception {
        final S3Client s3Client = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.getObject(
                        Mockito.eq(GetObjectRequest.builder().bucket("my-bucket").key("ok").build()),
                        Mockito.any(ResponseTransformer.class)))
                .thenReturn(responseBytes(new byte[] {9}));
        Mockito.when(s3Client.getObject(
                        Mockito.eq(GetObjectRequest.builder().bucket("my-bucket").key("err").build()),
                        Mockito.any(ResponseTransformer.class)))
                .thenThrow((S3Exception) S3Exception.builder().message("x").build());
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final S3Accessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        accessor.readObjectAsBytes("read", "my-bucket", "ok");
        assertThrows(S3AccessorException.class, () -> accessor.readObjectAsBytes("read", "my-bucket", "err"));

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    @SuppressWarnings("unchecked")
    void thrownS3ExceptionSurfacesAsS3AccessorException() {
        final S3Client s3Client = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.getObject(Mockito.any(GetObjectRequest.class), Mockito.any(ResponseTransformer.class)))
                .thenThrow((S3Exception) S3Exception.builder().message("missing").build());
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final S3Accessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        assertThrows(S3AccessorException.class, () -> accessor.readObjectAsBytes("read", "my-bucket", "object-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void timerRecordedForSuccessfulOperation() throws Exception {
        final S3Client s3Client = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.getObject(Mockito.any(GetObjectRequest.class), Mockito.any(ResponseTransformer.class)))
                .thenReturn(responseBytes(new byte[] {1}));
        final AppbaseClientIntegrationService<S3Clients, ClusterIdValue> integrationService =
                mockIntegrationService(mockS3Clients(s3Client));
        final CoreFixtures core = CoreFixtures.create();
        final S3Accessor accessor = accessor(core, defaultConfig(), () -> integrationService);

        accessor.readObjectAsBytes("read", "my-bucket", "object-1");

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
