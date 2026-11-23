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

import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.opentracing.Span;
import org.eclipse.microprofile.metrics.Timer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Base for reactive accessors. Subclasses call the native driver to produce a
 * {@link Mono}/{@link Flux}; this base applies the configured scheduler, per-attempt
 * timeout, retry/backoff, backpressure buffering (Flux), and Appbase error mapping, plus
 * (via the inherited {@link AbstractAppbaseAccessor} spine) config-gated metrics, tracing,
 * and exception-store reporting for named operations. Reactive-ness is a property of the
 * operation surface only — connection, config, and lifecycle stay in the shared (synchronous)
 * client machinery.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public abstract class AbstractReactiveAccessor extends AbstractAppbaseAccessor implements ReactiveAccessor {

    protected final ReactorService reactorService;
    protected final ReactiveOperationConfiguration configuration;

    protected AbstractReactiveAccessor(final AppbaseCore appbaseCore,
                                       final ReactorService reactorService,
                                       final ReactiveOperationConfiguration configuration) {
        super(appbaseCore, AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(), configuration.isExceptionStoreEnabled());
        AppbaseUtil.notNull(reactorService, "reactorService");
        this.reactorService = reactorService;
        this.configuration = configuration;
    }

    @Override
    public ReactiveOperationConfiguration getReactiveOperationConfiguration() {
        return configuration;
    }

    /**
     * Wrap a driver call returning a single value, assembling the cross-cutting spine (timer,
     * span, error mapping, failure logging/notable-reporting) around the R0 operator chain:
     * timeout, retry, and the configured scheduler.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param source Supplier of the driver's {@link Mono}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    protected <T> Mono<T> mono(final String op, final Supplier<Mono<T>> source) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(source, "source");
        return Mono.defer(() -> {
            final Timer.Context timerContext = startTimer(op);
            final Span span = startSpan(op);
            Mono<T> mono = source.get().timeout(configuration.getTimeout());
            if (configuration.isRetryEnabled()) {
                mono = mono.retryWhen(retrySpec());
            }
            return mono
                    .onErrorMap(this::mapError)
                    .doOnError(e -> onOperationError(op, e))
                    .doFinally(signalType -> {
                        finishSpan(span, signalType != SignalType.ON_COMPLETE);
                        if (timerContext != null) {
                            timerContext.stop();
                        }
                    });
        }).subscribeOn(scheduler());
    }

    /**
     * Wrap a driver call returning a stream, assembling the cross-cutting spine around the R0
     * operator chain: timeout, retry, backpressure buffering, and the configured scheduler.
     * Buffering is applied after retry so that a buffer-overflow (slow consumer) is not itself
     * treated as a transient driver error and does not re-invoke the (possibly non-idempotent)
     * source.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param source Supplier of the driver's {@link Flux}
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    protected <T> Flux<T> flux(final String op, final Supplier<Flux<T>> source) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(source, "source");
        return Flux.defer(() -> {
            final Timer.Context timerContext = startTimer(op);
            final Span span = startSpan(op);
            Flux<T> flux = source.get().timeout(configuration.getTimeout());
            if (configuration.isRetryEnabled()) {
                flux = flux.retryWhen(retrySpec());
            }
            return flux
                    .onBackpressureBuffer(configuration.getBackpressureBufferSize())
                    .onErrorMap(this::mapError)
                    .doOnError(e -> onOperationError(op, e))
                    .doFinally(signalType -> {
                        finishSpan(span, signalType != SignalType.ON_COMPLETE);
                        if (timerContext != null) {
                            timerContext.stop();
                        }
                    });
        }).subscribeOn(scheduler());
    }

    /**
     * Lighter reactive spine for LONG-LIVED streams (e.g. a Kafka consumer
     * {@code KafkaReceiver.receive()}), where the full {@link #flux(String, Supplier)} chain is
     * actively harmful. Each per-operation operator assumes ONE bounded call and breaks on an
     * unbounded stream:
     * <ul>
     *   <li>{@code timeout} — kills a healthy consumer on an idle topic (inactivity is normal,
     *       not a fault).</li>
     *   <li>{@code retry} — resubscribing tears down the consumer and triggers a consumer-group
     *       rebalance; that is not a cheap replay of a bounded call.</li>
     *   <li>per-op {@code Timer}/span — a duration timer or span opened on subscribe never closes
     *       on an infinite stream (span/metric leak).</li>
     *   <li>{@code onBackpressureBuffer} — an extra buffer fights the receiver's own backpressure
     *       protocol and risks unbounded growth (OOM).</li>
     * </ul>
     * So this method applies ONLY the stream-appropriate cross-cutting: error mapping
     * ({@code onErrorMap}), failure reporting ({@code onOperationError}: error counter + failure
     * log + notable→exception-store) and scheduler dispatch. {@code onErrorMap} runs before
     * {@code doOnError} so {@code onOperationError} observes the mapped error — same ordering as
     * {@link #flux(String, Supplier)}. Per-record metrics (a {@code doOnNext} counter) are the
     * consumer accessor's concern, added there, not here.
     *
     * @param op Operation name, used for failure reporting
     * @param source Supplier of the driver's long-lived {@link Flux}
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    protected <T> Flux<T> fluxStream(final String op, final Supplier<Flux<T>> source) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(source, "source");
        return Flux.defer(source)
                .onErrorMap(this::mapError)
                .doOnError(e -> onOperationError(op, e))
                .subscribeOn(scheduler());
    }

    /**
     * Wrap a driver call returning a single value. Delegates to {@link #mono(String, Supplier)}
     * with the generic operation name {@code "reactiveOperation"}.
     *
     * @param source Supplier of the driver's {@link Mono}
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    protected <T> Mono<T> mono(final Supplier<Mono<T>> source) {
        return mono("reactiveOperation", source);
    }

    /**
     * Wrap a driver call returning a stream. Delegates to {@link #flux(String, Supplier)} with
     * the generic operation name {@code "reactiveOperation"}.
     *
     * @param source Supplier of the driver's {@link Flux}
     * @param <T> Value type
     * @return Wrapped {@link Flux}
     */
    protected <T> Flux<T> flux(final Supplier<Flux<T>> source) {
        return flux("reactiveOperation", source);
    }

    /**
     * Tier-1 async facade for a blocking driver call: offloads onto the configured
     * scheduler. NOTE: this is async, not non-blocking — one thread is held per
     * in-flight call. Use only where the driver has no reactive API.
     *
     * @param blockingCall Blocking driver call
     * @param <T> Value type
     * @return {@link Mono} that runs {@code blockingCall} on the configured scheduler
     */
    protected <T> Mono<T> monoFromBlocking(final Callable<T> blockingCall) {
        AppbaseUtil.notNull(blockingCall, "blockingCall");
        return mono(() -> Mono.fromCallable(blockingCall));
    }

    private Retry retrySpec() {
        return Retry.backoff(configuration.getRetryMaxAttempts(), configuration.getRetryMinBackoff())
                .maxBackoff(configuration.getRetryMaxBackoff())
                .jitter(configuration.getRetryJitterFactor())
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    private Scheduler scheduler() {
        final String schedulerId = configuration.getSchedulerId();
        if (schedulerId != null) {
            final Scheduler byId = reactorService.getSchedulerById(schedulerId);
            if (byId != null) {
                return byId;
            }
            logger.warn("Reactive scheduler id '{}' not found; using default scheduler", schedulerId);
        }
        return reactorService.getDefaultScheduler();
    }
}
