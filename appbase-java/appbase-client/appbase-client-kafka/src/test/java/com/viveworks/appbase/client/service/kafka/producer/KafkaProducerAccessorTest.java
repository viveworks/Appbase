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

package com.viveworks.appbase.client.service.kafka.producer;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.KafkaAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link KafkaProducerAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class KafkaProducerAccessorTest {

    /**
     * Holds an accessor's backing metric mocks so tests can assert on timer interactions without
     * re-deriving them from a fresh mock.
     */
    private static final class Fixture {
        private final AppbaseCore appbaseCore;
        private final Timer timer;
        private final Timer.Context timerContext;

        private Fixture(final AppbaseCore appbaseCore, final Timer timer, final Timer.Context timerContext) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
        }
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue>
            mockIntegrationService(final Producer<String, String> producer) {
        final AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(producer);
        return integrationService;
    }

    private static Fixture mockFixture() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(KafkaProducerAccessorTest.class));

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

        return new Fixture(appbaseCore, timer, timerContext);
    }

    private static KafkaProducerAccessor<String, String> accessor(
            final AppbaseCore appbaseCore,
            final Supplier<AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue>> borrow) {
        return new KafkaProducerAccessor<>(appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    private static ProducerRecord<String, String> record() {
        return new ProducerRecord<>("topic", "k", "v");
    }

    @Test
    void sendReturnsRecordMetadata() throws Exception {
        final RecordMetadata metadata = Mockito.mock(RecordMetadata.class);
        final Producer<String, String> producer = mockProducer();
        Mockito.when(producer.send(Mockito.any())).thenReturn(CompletableFuture.completedFuture(metadata));
        final AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(producer);
        final KafkaProducerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        final RecordMetadata result = accessor.send("send", record());

        assertSame(metadata, result);
    }

    @Test
    void commandRunsAndReturnsValue() throws Exception {
        final Producer<String, String> producer = mockProducer();
        final AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(producer);
        final KafkaProducerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        final String result = accessor.command("flush", p -> "ok");

        assertSame("ok", result);
    }

    @Test
    void integrationServiceReturnedToPoolOnSuccessAndError() throws Exception {
        final RecordMetadata metadata = Mockito.mock(RecordMetadata.class);
        final Producer<String, String> producer = mockProducer();
        Mockito.when(producer.send(Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(metadata))
                .thenReturn(failedFuture(new KafkaException("boom")));
        final AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(producer);
        final KafkaProducerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.send("send", record());
        try {
            accessor.send("send", record());
        } catch (final Exception ignored) {
            // expected: mapped/rethrown failure, asserted separately by driverErrorMappedToKafkaAccessorException
        }

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void driverErrorMappedToKafkaAccessorException() {
        final Producer<String, String> producer = mockProducer();
        Mockito.when(producer.send(Mockito.any())).thenReturn(failedFuture(new KafkaException("down")));
        final AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(producer);
        final KafkaProducerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        assertThrows(KafkaAccessorException.class, () -> accessor.send("send", record()));
    }

    @Test
    void metricsRecordedOnSend() throws Exception {
        final RecordMetadata metadata = Mockito.mock(RecordMetadata.class);
        final Producer<String, String> producer = mockProducer();
        Mockito.when(producer.send(Mockito.any())).thenReturn(CompletableFuture.completedFuture(metadata));
        final AppbaseClientIntegrationService<Producer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(producer);
        final Fixture fixture = mockFixture();
        final KafkaProducerAccessor<String, String> accessor = accessor(fixture.appbaseCore, () -> integrationService);

        accessor.send("send", record());

        Mockito.verify(fixture.timer).time();
        Mockito.verify(fixture.timerContext).stop();
    }

    @SuppressWarnings("unchecked")
    private static Producer<String, String> mockProducer() {
        return Mockito.mock(Producer.class);
    }

    private static Future<RecordMetadata> failedFuture(final Throwable cause) {
        final CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        future.completeExceptionally(cause);
        return future;
    }
}
