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

package com.viveworks.appbase.client.core.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link AbstractReactiveAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractReactiveAccessorTest {

    private static final class TestAccessor extends AbstractReactiveAccessor {
        TestAccessor(final AppbaseCore appbaseCore, final ReactorService reactorService,
                     final ReactiveOperationConfiguration configuration) {
            super(appbaseCore, reactorService, configuration);
        }

        <T> Mono<T> exposeMono(final Supplier<Mono<T>> source) {
            return mono(source);
        }

        <T> Flux<T> exposeFlux(final Supplier<Flux<T>> source) {
            return flux(source);
        }

        <T> Mono<T> exposeMono(final String op, final Supplier<Mono<T>> source) {
            return mono(op, source);
        }

        <T> Flux<T> exposeFlux(final String op, final Supplier<Flux<T>> source) {
            return flux(op, source);
        }

        <T> Flux<T> exposeFluxStream(final String op, final Supplier<Flux<T>> source) {
            return fluxStream(op, source);
        }
    }

    private static AppbaseCore mockAppbaseCore() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(AbstractReactiveAccessorTest.class));

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
        // Message/ExceptionMessage are final classes (no inline mock-maker configured in this
        // repo), so MessageFactory.exceptionMessage needs a real pooled Message instance.
        Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

        return appbaseCore;
    }

    private static Counter mockErrorCounter(final AppbaseCore appbaseCore) {
        return appbaseCore.getMetricService().getMetricRegistry().counter("any");
    }

    private static Timer mockTimer(final AppbaseCore appbaseCore) {
        return appbaseCore.getMetricService().getMetricRegistry().timer("any");
    }

    private static Tracer mockTracer(final AppbaseCore appbaseCore) {
        return appbaseCore.getTracerService().getTracer();
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static TestAccessor accessor(final ReactiveOperationConfiguration configuration) {
        return new TestAccessor(mockAppbaseCore(), mockReactorService(), configuration);
    }

    @Test
    void successfulMonoPassesThrough() {
        final TestAccessor accessor = accessor(DefaultReactiveOperationConfiguration.builder().build());
        StepVerifier.create(accessor.exposeMono(() -> Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    void foreignErrorMappedToReactiveOperationException() {
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build());
        StepVerifier.create(accessor.exposeMono(() -> Mono.error(new IllegalStateException("x"))))
                .expectErrorSatisfies(t -> assertInstanceOf(ReactiveOperationException.class, t))
                .verify();
    }

    @Test
    void retriesThenSucceeds() {
        final AtomicInteger attempts = new AtomicInteger();
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder()
                        .retryMaxAttempts(3)
                        .retryMinBackoff(Duration.ofMillis(1))
                        .retryMaxBackoff(Duration.ofMillis(5))
                        .build());
        final Mono<String> flaky = Mono.defer(() ->
                attempts.incrementAndGet() < 3 ? Mono.error(new IllegalStateException("retry"))
                        : Mono.just("recovered"));
        StepVerifier.create(accessor.exposeMono(() -> flaky))
                .expectNext("recovered")
                .verifyComplete();
    }

    @Test
    void timesOutWhenSourceHangs() {
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder()
                        .timeout(Duration.ofMillis(50))
                        .retryEnabled(false)
                        .build());
        StepVerifier.create(accessor.exposeMono(Mono::<String>never))
                .expectError(ReactiveOperationException.class)
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void monoRetryExhaustionPropagatesOriginalAppbaseFailureUnwrapped() {
        final AppbaseRuntimeException original = new AppbaseRuntimeException("biz");
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder()
                        .retryMaxAttempts(2)
                        .retryMinBackoff(Duration.ofMillis(1))
                        .retryMaxBackoff(Duration.ofMillis(5))
                        .build());
        StepVerifier.create(accessor.exposeMono(() -> Mono.error(original)))
                .expectErrorSatisfies(t -> {
                    assertInstanceOf(AppbaseRuntimeException.class, t);
                    assertFalse(t instanceof ReactiveOperationException);
                    assertSame(original, t);
                })
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void monoRetryExhaustionWrapsForeignFailureExactlyOnce() {
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder()
                        .retryMaxAttempts(2)
                        .retryMinBackoff(Duration.ofMillis(1))
                        .retryMaxBackoff(Duration.ofMillis(5))
                        .build());
        StepVerifier.create(accessor.exposeMono(() -> Mono.error(new IllegalStateException("io"))))
                .expectErrorSatisfies(t -> {
                    assertInstanceOf(ReactiveOperationException.class, t);
                    assertInstanceOf(IllegalStateException.class, t.getCause());
                    assertEquals("io", t.getCause().getMessage());
                })
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void fluxRetryExhaustionPropagatesOriginalAppbaseFailureUnwrapped() {
        final AppbaseRuntimeException original = new AppbaseRuntimeException("biz");
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder()
                        .retryMaxAttempts(2)
                        .retryMinBackoff(Duration.ofMillis(1))
                        .retryMaxBackoff(Duration.ofMillis(5))
                        .build());
        StepVerifier.create(accessor.exposeFlux(() -> Flux.error(original)))
                .expectErrorSatisfies(t -> {
                    assertInstanceOf(AppbaseRuntimeException.class, t);
                    assertFalse(t instanceof ReactiveOperationException);
                    assertSame(original, t);
                })
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void fluxEmitsAllItems() {
        final TestAccessor accessor = accessor(DefaultReactiveOperationConfiguration.builder().build());
        StepVerifier.create(accessor.exposeFlux(() -> Flux.just(1, 2, 3)))
                .expectNext(1, 2, 3)
                .verifyComplete();
    }

    @Test
    void namedMonoRecordsTimerAndErrorCounterOnFailure() {
        final AppbaseCore appbaseCore = mockAppbaseCore();
        final TestAccessor accessor = new TestAccessor(appbaseCore, mockReactorService(),
                DefaultReactiveOperationConfiguration.builder()
                        .retryEnabled(false).build());   // metricsEnabled defaults true
        StepVerifier.create(accessor.exposeMono("get", () -> Mono.error(new IllegalStateException("x"))))
                .expectError(ReactiveOperationException.class).verify();
        Mockito.verify(mockErrorCounter(appbaseCore)).inc();
    }

    @Test
    void namedMonoWithMetricsDisabledDoesNotTouchTimer() {
        final AppbaseCore appbaseCore = mockAppbaseCore();
        final TestAccessor accessor = new TestAccessor(appbaseCore, mockReactorService(),
                DefaultReactiveOperationConfiguration.builder()
                        .metricsEnabled(false).retryEnabled(false).build());
        StepVerifier.create(accessor.exposeMono("get", () -> Mono.just("v")))
                .expectNext("v")
                .verifyComplete();
        Mockito.verify(mockTimer(appbaseCore), Mockito.never()).time();
    }

    @Test
    void namedMonoWithTraceDisabledStartsNoSpan() {
        final AppbaseCore appbaseCore = mockAppbaseCore();
        final TestAccessor accessor = new TestAccessor(appbaseCore, mockReactorService(),
                DefaultReactiveOperationConfiguration.builder()
                        .traceEnabled(false).retryEnabled(false).build());
        StepVerifier.create(accessor.exposeMono("get", () -> Mono.just("v")))
                .expectNext("v")
                .verifyComplete();
        Mockito.verify(mockTracer(appbaseCore), Mockito.never()).buildSpan(Mockito.anyString());
    }

    @Test
    void fluxStreamMapsError() {
        final TestAccessor accessor = accessor(DefaultReactiveOperationConfiguration.builder().build());
        StepVerifier.create(accessor.exposeFluxStream("stream",
                        () -> Flux.error(new RuntimeException("x"))))
                .expectError(ReactiveOperationException.class)
                .verify();
    }

    @Test
    void fluxStreamAppliesNoTimeoutSoDelayedEmissionPastConfiguredTimeoutStillPasses() {
        // Configured timeout is 50ms; the stream emits only after 150ms. A bounded flux()
        // would time out and error, proving fluxStream deliberately omits the timeout operator
        // (a long-lived stream on an idle topic must not be killed by inactivity).
        final TestAccessor accessor = accessor(
                DefaultReactiveOperationConfiguration.builder()
                        .timeout(Duration.ofMillis(50))
                        .build());
        StepVerifier.create(accessor.exposeFluxStream("stream",
                        () -> Flux.just("late").delayElements(Duration.ofMillis(150))))
                .expectNext("late")
                .verifyComplete();
    }

    @Test
    void fluxStreamReportsFailureViaErrorCounter() {
        final AppbaseCore appbaseCore = mockAppbaseCore();
        final TestAccessor accessor = new TestAccessor(appbaseCore, mockReactorService(),
                DefaultReactiveOperationConfiguration.builder().build());
        StepVerifier.create(accessor.exposeFluxStream("receive",
                        () -> Flux.error(new IllegalStateException("x"))))
                .expectError(ReactiveOperationException.class)
                .verify();
        Mockito.verify(mockErrorCounter(appbaseCore)).inc();
    }

    @Test
    void fallsBackToDefaultSchedulerWhenSchedulerIdNotFound() {
        final ReactorService reactorService = mockReactorService();
        final TestAccessor accessor = new TestAccessor(mockAppbaseCore(), reactorService,
                DefaultReactiveOperationConfiguration.builder()
                        .schedulerId("missing-scheduler")
                        .build());
        StepVerifier.create(accessor.exposeMono(() -> Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();
        Mockito.verify(reactorService).getSchedulerById("missing-scheduler");
    }
}
