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

package com.viveworks.appbase.client.service.kafka.consumer;

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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.KafkaException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link KafkaConsumerAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class KafkaConsumerAccessorTest {

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
    private static AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue>
            mockIntegrationService(final Consumer<String, String> consumer) {
        final AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(consumer);
        return integrationService;
    }

    private static Fixture mockFixture() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(KafkaConsumerAccessorTest.class));

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

    private static KafkaConsumerAccessor<String, String> accessor(
            final AppbaseCore appbaseCore,
            final Supplier<AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue>> borrow) {
        return new KafkaConsumerAccessor<>(appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    @SuppressWarnings("unchecked")
    private static Consumer<String, String> mockConsumer() {
        return Mockito.mock(Consumer.class);
    }

    @Test
    void pollReturnsConsumerRecords() throws Exception {
        final ConsumerRecords<String, String> records = Mockito.mock(ConsumerRecords.class);
        final Consumer<String, String> consumer = mockConsumer();
        Mockito.when(consumer.poll(Mockito.any(Duration.class))).thenReturn(records);
        final AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(consumer);
        final KafkaConsumerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        final ConsumerRecords<String, String> result = accessor.poll("poll", Duration.ofMillis(100));

        assertSame(records, result);
    }

    @Test
    void commandRunsAndReturnsValue() throws Exception {
        final Consumer<String, String> consumer = mockConsumer();
        final AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(consumer);
        final KafkaConsumerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        final String result = accessor.command("assignment", c -> "ok");

        assertSame("ok", result);
    }

    @Test
    void integrationServiceReturnedToPoolOnSuccessAndError() throws Exception {
        final ConsumerRecords<String, String> records = Mockito.mock(ConsumerRecords.class);
        final Consumer<String, String> consumer = mockConsumer();
        Mockito.when(consumer.poll(Mockito.any(Duration.class)))
                .thenReturn(records)
                .thenThrow(new KafkaException("boom"));
        final AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(consumer);
        final KafkaConsumerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.poll("poll", Duration.ofMillis(100));
        try {
            accessor.poll("poll", Duration.ofMillis(100));
        } catch (final Exception ignored) {
            // expected: mapped/rethrown failure, asserted separately by driverErrorMappedToKafkaAccessorException
        }

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void driverErrorMappedToKafkaAccessorException() {
        final Consumer<String, String> consumer = mockConsumer();
        Mockito.when(consumer.poll(Mockito.any(Duration.class))).thenThrow(new KafkaException("down"));
        final AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(consumer);
        final KafkaConsumerAccessor<String, String> accessor =
                accessor(mockFixture().appbaseCore, () -> integrationService);

        assertThrows(KafkaAccessorException.class, () -> accessor.poll("poll", Duration.ofMillis(100)));
    }

    @Test
    void metricsRecordedOnPoll() throws Exception {
        final ConsumerRecords<String, String> records = Mockito.mock(ConsumerRecords.class);
        final Consumer<String, String> consumer = mockConsumer();
        Mockito.when(consumer.poll(Mockito.any(Duration.class))).thenReturn(records);
        final AppbaseClientIntegrationService<Consumer<String, String>, ClusterIdValue> integrationService
                = mockIntegrationService(consumer);
        final Fixture fixture = mockFixture();
        final KafkaConsumerAccessor<String, String> accessor = accessor(fixture.appbaseCore, () -> integrationService);

        accessor.poll("poll", Duration.ofMillis(100));

        Mockito.verify(fixture.timer).time();
        Mockito.verify(fixture.timerContext).stop();
    }
}
