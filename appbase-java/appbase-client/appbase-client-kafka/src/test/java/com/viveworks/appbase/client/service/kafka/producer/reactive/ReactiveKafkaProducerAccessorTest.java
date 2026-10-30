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

package com.viveworks.appbase.client.service.kafka.producer.reactive;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.KafkaAccessorException;
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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

/**
 * Tests for {@link ReactiveKafkaProducerAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveKafkaProducerAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the tracer/span/mediator/timer mocks it was built
     * from, so tests can both drive the accessor and verify the shared spine's interactions.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;
        final Timer timer;
        final Timer.Context timerContext;

        private CoreFixtures(final AppbaseCore appbaseCore, final Tracer tracer, final Span span,
                             final Mediator mediator, final Timer timer, final Timer.Context timerContext) {
            this.appbaseCore = appbaseCore;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
            this.timer = timer;
            this.timerContext = timerContext;
        }

        @SuppressWarnings("unchecked")
        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveKafkaProducerAccessorTest.class));

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

            return new CoreFixtures(appbaseCore, tracer, span, mediator, timer, timerContext);
        }
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    private static ReactiveKafkaProducerAccessor<String, String> accessor(
            final CoreFixtures core, final KafkaSender<String, String> sender) {
        return new ReactiveKafkaProducerAccessor<>(
                core.appbaseCore, mockReactorService(), defaultConfig(), sender);
    }

    @SuppressWarnings("unchecked")
    private static KafkaSender<String, String> mockSender() {
        return Mockito.mock(KafkaSender.class);
    }

    @SuppressWarnings("unchecked")
    private static SenderResult<Object> mockResult() {
        return Mockito.mock(SenderResult.class);
    }

    private static ProducerRecord<String, String> record() {
        return new ProducerRecord<>("topic", "k", "v");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSingleRecordReturnsMetadata() {
        final RecordMetadata meta = Mockito.mock(RecordMetadata.class);
        final SenderResult<Object> result = mockResult();
        Mockito.when(result.exception()).thenReturn(null);
        Mockito.when(result.recordMetadata()).thenReturn(meta);
        final KafkaSender<String, String> sender = mockSender();
        Mockito.when(sender.send(Mockito.any())).thenReturn(Flux.just(result));
        final ReactiveKafkaProducerAccessor<String, String> accessor =
                accessor(CoreFixtures.create(), sender);

        StepVerifier.create(accessor.send("send", record()))
                .expectNext(meta)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSingleRecordErrorMapsToKafkaAccessorException() {
        final SenderResult<Object> result = mockResult();
        Mockito.when(result.exception()).thenReturn(new KafkaException("boom"));
        final KafkaSender<String, String> sender = mockSender();
        Mockito.when(sender.send(Mockito.any())).thenReturn(Flux.just(result));
        final ReactiveKafkaProducerAccessor<String, String> accessor =
                accessor(CoreFixtures.create(), sender);

        StepVerifier.create(accessor.send("send", record()))
                .expectError(KafkaAccessorException.class)
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendBatchReturnsSenderResults() {
        final SenderResult<Object> result = mockResult();
        final SenderResult<String> stringResult = (SenderResult<String>) (SenderResult<?>) result;
        final KafkaSender<String, String> sender = mockSender();
        Mockito.when(sender.send(Mockito.any())).thenReturn(Flux.just(result));
        final ReactiveKafkaProducerAccessor<String, String> accessor =
                accessor(CoreFixtures.create(), sender);

        StepVerifier.create(accessor.send("send", Flux.just(SenderRecord.create(record(), "corr"))))
                .expectNext(stringResult)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void metricsRecordedOnSend() {
        final RecordMetadata meta = Mockito.mock(RecordMetadata.class);
        final SenderResult<Object> result = mockResult();
        Mockito.when(result.exception()).thenReturn(null);
        Mockito.when(result.recordMetadata()).thenReturn(meta);
        final KafkaSender<String, String> sender = mockSender();
        Mockito.when(sender.send(Mockito.any())).thenReturn(Flux.just(result));
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveKafkaProducerAccessor<String, String> accessor = accessor(core, sender);

        accessor.send("send", record()).block();

        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
