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

package com.viveworks.appbase.client.service.kafka.consumer.reactive;

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
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.KafkaException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Tests for {@link ReactiveKafkaConsumerAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveKafkaConsumerAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the tracer/span/mediator/counter mocks it was built
     * from, so tests can both drive the accessor and verify the lighter spine's failure-reporting
     * interactions.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;
        final Counter errorCounter;

        private CoreFixtures(final AppbaseCore appbaseCore, final Tracer tracer, final Span span,
                             final Mediator mediator, final Counter errorCounter) {
            this.appbaseCore = appbaseCore;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
            this.errorCounter = errorCounter;
        }

        @SuppressWarnings("unchecked")
        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveKafkaConsumerAccessorTest.class));

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

            return new CoreFixtures(appbaseCore, tracer, span, mediator, errorCounter);
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

    private static ReactiveKafkaConsumerAccessor<String, String> accessor(
            final CoreFixtures core, final KafkaReceiver<String, String> receiver) {
        return accessor(core, receiver, defaultConfig(), false);
    }

    private static ReactiveKafkaConsumerAccessor<String, String> accessor(
            final CoreFixtures core, final KafkaReceiver<String, String> receiver,
            final boolean driverMetricsEnabled) {
        return accessor(core, receiver, defaultConfig(), driverMetricsEnabled);
    }

    private static ReactiveKafkaConsumerAccessor<String, String> accessor(
            final CoreFixtures core, final KafkaReceiver<String, String> receiver,
            final DefaultReactiveOperationConfiguration configuration) {
        return accessor(core, receiver, configuration, false);
    }

    private static ReactiveKafkaConsumerAccessor<String, String> accessor(
            final CoreFixtures core, final KafkaReceiver<String, String> receiver,
            final DefaultReactiveOperationConfiguration configuration,
            final boolean driverMetricsEnabled) {
        return new ReactiveKafkaConsumerAccessor<>(
                core.appbaseCore, mockReactorService(), configuration, receiver, driverMetricsEnabled);
    }

    @SuppressWarnings("unchecked")
    private static KafkaReceiver<String, String> mockReceiver() {
        return Mockito.mock(KafkaReceiver.class);
    }

    @SuppressWarnings("unchecked")
    private static ReceiverRecord<String, String> mockRecord() {
        return Mockito.mock(ReceiverRecord.class);
    }

    /**
     * Stubs {@code receiver.doOnConsumer(fn)} to synchronously invoke the binding function with a mock
     * {@link Consumer} and complete with the resulting binder (mirrors reactor-kafka running the
     * function on the receiver's scheduler once a subscription is active).
     */
    @SuppressWarnings("unchecked")
    private static void stubDoOnConsumerSync(final KafkaReceiver<String, String> receiver) {
        Mockito.when(receiver.doOnConsumer(Mockito.any())).thenAnswer(invocation -> {
            final Function<Consumer<String, String>, Object> fn = invocation.getArgument(0);
            final Consumer<String, String> consumer = Mockito.mock(Consumer.class);
            return Mono.just(fn.apply(consumer));
        });
    }

    @Test
    void receiveEmitsRecords() {
        final ReceiverRecord<String, String> record = mockRecord();
        final KafkaReceiver<String, String> receiver = mockReceiver();
        Mockito.when(receiver.receive()).thenReturn(Flux.just(record));
        final ReactiveKafkaConsumerAccessor<String, String> accessor =
                accessor(CoreFixtures.create(), receiver);

        StepVerifier.create(accessor.receive("receive"))
                .expectNext(record)
                .verifyComplete();
    }

    @Test
    void receiveErrorMapsToKafkaAccessorException() {
        final KafkaReceiver<String, String> receiver = mockReceiver();
        Mockito.when(receiver.receive()).thenReturn(Flux.error(new KafkaException("boom")));
        final ReactiveKafkaConsumerAccessor<String, String> accessor =
                accessor(CoreFixtures.create(), receiver);

        StepVerifier.create(accessor.receive("receive"))
                .expectError(KafkaAccessorException.class)
                .verify();
    }

    @Test
    void receiveNoTimeout() {
        // Configured timeout is 50ms; the stream emits only after 150ms. A bounded flux() spine
        // would time out and error, proving receive() rides the lighter fluxStream spine which
        // deliberately omits the timeout operator (a long-lived consumer on an idle topic must not
        // be killed by inactivity).
        final ReceiverRecord<String, String> record = mockRecord();
        final KafkaReceiver<String, String> receiver = mockReceiver();
        Mockito.when(receiver.receive())
                .thenReturn(Flux.just(record).delayElements(Duration.ofMillis(150)));
        final ReactiveKafkaConsumerAccessor<String, String> accessor = accessor(
                CoreFixtures.create(), receiver,
                DefaultReactiveOperationConfiguration.builder()
                        .retryEnabled(false)
                        .timeout(Duration.ofMillis(50))
                        .build());

        StepVerifier.create(accessor.receive("receive"))
                .expectNext(record)
                .verifyComplete();
    }

    @Test
    void receiveFailureReported() {
        final KafkaReceiver<String, String> receiver = mockReceiver();
        Mockito.when(receiver.receive()).thenReturn(Flux.error(new KafkaException("boom")));
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveKafkaConsumerAccessor<String, String> accessor = accessor(core, receiver);

        StepVerifier.create(accessor.receive("receive"))
                .expectError(KafkaAccessorException.class)
                .verify();

        Mockito.verify(core.errorCounter).inc();
    }

    @Test
    void receiveBindsDriverMetricsWhenEnabled() {
        final ReceiverRecord<String, String> record = mockRecord();
        final KafkaReceiver<String, String> receiver = mockReceiver();
        final CoreFixtures core = CoreFixtures.create();
        Mockito.when(core.appbaseCore.getMetricService().getCompositeMeterRegistry())
                .thenReturn(new SimpleMeterRegistry());
        Mockito.when(receiver.receive()).thenReturn(Flux.just(record));
        stubDoOnConsumerSync(receiver);

        try (MockedConstruction<KafkaClientMetrics> mocked =
                     Mockito.mockConstruction(KafkaClientMetrics.class)) {
            final ReactiveKafkaConsumerAccessor<String, String> accessor = accessor(core, receiver, true);

            StepVerifier.create(accessor.receive("receive"))
                    .expectNext(record)
                    .verifyComplete();

            // doOnConsumer is invoked only after a record has flowed (active subscription), binding the
            // driver-native binder to the composite registry.
            Mockito.verify(receiver).doOnConsumer(Mockito.any());
            Mockito.verify(core.appbaseCore.getMetricService()).getCompositeMeterRegistry();
            final KafkaClientMetrics binder = mocked.constructed().get(0);
            Mockito.verify(binder).bindTo(Mockito.any());
        }
    }

    @Test
    void receiveDoesNotBindWhenDisabled() {
        final ReceiverRecord<String, String> record = mockRecord();
        final KafkaReceiver<String, String> receiver = mockReceiver();
        Mockito.when(receiver.receive()).thenReturn(Flux.just(record));
        final ReactiveKafkaConsumerAccessor<String, String> accessor =
                accessor(CoreFixtures.create(), receiver, false);

        StepVerifier.create(accessor.receive("receive"))
                .expectNext(record)
                .verifyComplete();

        Mockito.verify(receiver, Mockito.never()).doOnConsumer(Mockito.any());
    }

    @Test
    void receiveClosesBinderOnTerminate() {
        final ReceiverRecord<String, String> record = mockRecord();
        final KafkaReceiver<String, String> receiver = mockReceiver();
        final CoreFixtures core = CoreFixtures.create();
        Mockito.when(core.appbaseCore.getMetricService().getCompositeMeterRegistry())
                .thenReturn(new SimpleMeterRegistry());
        Mockito.when(receiver.receive()).thenReturn(Flux.just(record));
        stubDoOnConsumerSync(receiver);

        try (MockedConstruction<KafkaClientMetrics> mocked =
                     Mockito.mockConstruction(KafkaClientMetrics.class)) {
            final ReactiveKafkaConsumerAccessor<String, String> accessor = accessor(core, receiver, true);

            StepVerifier.create(accessor.receive("receive"))
                    .expectNext(record)
                    .verifyComplete();

            // doFinally closes the binder when the receive stream terminates.
            final KafkaClientMetrics binder = mocked.constructed().get(0);
            Mockito.verify(binder).close();
        }
    }

    @Test
    void receiveClosesLateBinderArrivingAfterTerminate() {
        final ReceiverRecord<String, String> record = mockRecord();
        final KafkaReceiver<String, String> receiver = mockReceiver();
        final CoreFixtures core = CoreFixtures.create();
        Mockito.when(core.appbaseCore.getMetricService().getCompositeMeterRegistry())
                .thenReturn(new SimpleMeterRegistry());
        Mockito.when(receiver.receive()).thenReturn(Flux.just(record));

        // doOnConsumer captures the binding function but defers emission of the binder until AFTER the
        // stream terminates, exercising the async-store race: the binder is stored only once the stream
        // has already ended, so the terminated re-check must close it (otherwise it leaks).
        final AtomicReference<Function<Consumer<String, String>, Object>> captured = new AtomicReference<>();
        final Sinks.One<Object> lateBinder = Sinks.one();
        Mockito.when(receiver.doOnConsumer(Mockito.any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return lateBinder.asMono();
        });

        try (MockedConstruction<KafkaClientMetrics> mocked =
                     Mockito.mockConstruction(KafkaClientMetrics.class)) {
            final ReactiveKafkaConsumerAccessor<String, String> accessor = accessor(core, receiver, true);

            StepVerifier.create(accessor.receive("receive"))
                    .expectNext(record)
                    .verifyComplete();

            // Stream already terminated; now the binder arrives late. Build it and emit it.
            final Consumer<String, String> consumer = Mockito.mock(Consumer.class);
            final Object lateBinderValue = captured.get().apply(consumer);
            lateBinder.tryEmitValue(lateBinderValue);

            final KafkaClientMetrics binder = mocked.constructed().get(0);
            Mockito.verify(binder).close();
        }
    }
}
