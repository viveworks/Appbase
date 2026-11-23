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

package com.viveworks.appbase.client.core.accessor;

import com.viveworks.appbase.client.core.reactive.ReactiveErrorMapper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cross-cutting spine shared by synchronous and reactive Appbase accessors: per-operation
 * metrics (a success timer and an error counter, each cached by operation name), a config-gated
 * OpenTracing span around each operation, an overridable error-mapping/notability hook, failure
 * logging, and config-gated reporting of notable failures to the exception store via the
 * mediator. Subclasses assemble these primitives around their own operator chain; see
 * {@link #execute(String, Callable)} for the synchronous fork.
 *
 * <p>{@link #execute(String, Callable)} surfaces the {@linkplain #mapError(Throwable) mapped},
 * Appbase-consistent throwable to the caller — the same mapping the reactive named operators in
 * {@code AbstractReactiveAccessor} apply to the propagated signal (via Reactor's
 * {@code onErrorMap}). Synchronous and reactive faces therefore expose identical error
 * semantics: a driver-specific failure surfaces as the same mapped type on both faces, with the
 * original throwable preserved as the cause.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public abstract class AbstractAppbaseAccessor {

    private static final String TIMER_SUFFIX = "timer";
    private static final String ERROR_COUNTER_SUFFIX = "error";

    protected final AppbaseCore appbaseCore;
    protected final Logger logger;

    private final MetricService metricService;
    private final TracerService tracerService;
    private final MediatorService mediatorService;
    private final boolean metricsEnabled;
    private final boolean traceEnabled;
    private final boolean exceptionStoreEnabled;

    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    /**
     * Create the spine.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param metricsEnabled Whether per-operation timers/counters are recorded
     * @param traceEnabled Whether per-operation OpenTracing spans are created
     * @param exceptionStoreEnabled Whether notable failures are reported to the exception store
     */
    protected AbstractAppbaseAccessor(final AppbaseCore appbaseCore,
                                      final boolean metricsEnabled,
                                      final boolean traceEnabled,
                                      final boolean exceptionStoreEnabled) {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.appbaseCore = appbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.metricService = appbaseCore.getMetricService();
        this.tracerService = appbaseCore.getTracerService();
        this.mediatorService = appbaseCore.getMediatorService();
        this.metricsEnabled = metricsEnabled;
        this.traceEnabled = traceEnabled;
        this.exceptionStoreEnabled = exceptionStoreEnabled;
    }

    /**
     * Cached per-operation success timer.
     *
     * @param op Operation name
     * @return Timer for {@code op}
     */
    protected Timer operationTimer(final String op) {
        return timers.computeIfAbsent(op, key ->
                metricService.getMetricRegistry().timer(metricService.name(getClass(), key, TIMER_SUFFIX)));
    }

    /**
     * Cached per-operation error counter.
     *
     * @param op Operation name
     * @return Counter for {@code op}
     */
    protected Counter operationErrorCounter(final String op) {
        return errorCounters.computeIfAbsent(op, key ->
                metricService.getMetricRegistry().counter(metricService.name(getClass(), key, ERROR_COUNTER_SUFFIX)));
    }

    /**
     * Start a span for {@code op}, if tracing is enabled for this accessor.
     *
     * @param op Operation name
     * @return Started span, or {@code null} if tracing is disabled
     */
    @Nullable
    protected Span startSpan(final String op) {
        if (!traceEnabled) {
            return null;
        }
        return tracerService.getTracer().buildSpan(getClass().getName() + "." + op).start();
    }

    /**
     * Finish a span started by {@link #startSpan(String)}, tagging it as errored if applicable.
     *
     * @param span Span to finish, or {@code null} if tracing was disabled (a no-op then)
     * @param errored Whether the operation the span covers failed
     */
    protected void finishSpan(@Nullable final Span span, final boolean errored) {
        if (span != null) {
            if (errored) {
                Tags.ERROR.set(span, true);
            }
            span.finish();
        }
    }

    /**
     * Map a raised {@link Throwable} to an Appbase-consistent exception type. Overridable; the
     * default delegates to {@link ReactiveErrorMapper#map(Throwable)}.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    protected Throwable mapError(final Throwable throwable) {
        return ReactiveErrorMapper.map(throwable);
    }

    /**
     * Whether a mapped failure is notable enough to report to the exception store. Overridable;
     * the default reports every failure.
     *
     * @param mapped Mapped throwable
     * @return {@code true} if notable
     */
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }

    /**
     * Log an operation failure.
     *
     * @param op Operation name
     * @param throwable Failure
     */
    protected void logFailure(final String op, final Throwable throwable) {
        logger.error("Operation {} failed", op, throwable);
    }

    /**
     * Report a notable failure to the exception store via the mediator, when enabled and the
     * failure is {@link #isNotable(Throwable) notable}.
     *
     * @param op Operation name
     * @param mapped Mapped throwable
     */
    protected void reportNotable(final String op, final Throwable mapped) {
        if (exceptionStoreEnabled && isNotable(mapped)) {
            final Message exceptionMessage = MessageFactory.exceptionMessage(
                    mediatorService, getClass(), "Operation " + op + " failed", AppbaseUtil.threadName(), mapped);
            try {
                mediatorService.getMediator().asyncHandleMessage(exceptionMessage, true);
            } catch (MediatorException e) {
                logger.error("Error sending exception message to mediator: {}", mapped, e);
            }
        }
    }

    /**
     * Common failure handling shared by the synchronous and reactive faces: error counter,
     * logging, and notable reporting.
     *
     * @param op Operation name
     * @param throwable Mapped failure
     */
    protected void onOperationError(final String op, final Throwable throwable) {
        if (metricsEnabled) {
            operationErrorCounter(op).inc();
        }
        logFailure(op, throwable);
        reportNotable(op, throwable);
    }

    /**
     * Run a synchronous operation with the cross-cutting spine: timer, span, and (on failure)
     * error counter/logging/notable-reporting. On failure, the {@linkplain #mapError(Throwable)
     * mapped} throwable is thrown, not the original — matching the reactive face's
     * {@code onErrorMap} behavior.
     *
     * @param op Operation name
     * @param call Operation to run
     * @param <T> Result type
     * @return Result of {@code call}
     * @throws Exception The mapped exception, if {@code call} raises one
     */
    protected <T> T execute(final String op, final Callable<T> call) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(call, "call");
        final Timer.Context timerContext = startTimer(op);
        final Span span = startSpan(op);
        boolean errored = false;
        try {
            return call.call();
        } catch (Exception e) {
            errored = true;
            final Throwable mapped = mapError(e);
            onOperationError(op, mapped);
            if (mapped instanceof Exception) {
                throw (Exception) mapped;
            }
            throw new RuntimeException(mapped);
        } finally {
            finishSpan(span, errored);
            if (timerContext != null) {
                timerContext.stop();
            }
        }
    }

    /**
     * Start a per-operation timer, if metrics are enabled for this accessor.
     *
     * @param op Operation name
     * @return Started timer context, or {@code null} if metrics are disabled
     */
    @Nullable
    protected Timer.Context startTimer(final String op) {
        return metricsEnabled ? operationTimer(op).time() : null;
    }
}
