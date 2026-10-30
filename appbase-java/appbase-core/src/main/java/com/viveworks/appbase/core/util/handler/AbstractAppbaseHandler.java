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

package com.viveworks.appbase.core.util.handler;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.SpanCustomizer;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Abstract implementation of {@link AppbaseHandler}.
 *
 * @author Navaneeth Mysore G
 * @param <R> {@link AppbaseHandlerRequest}
 * @since 1.0.0
 */
public abstract class AbstractAppbaseHandler<R extends AppbaseHandlerRequest> implements AppbaseHandler<R> {
    protected final Logger logger;
    protected final TracerService tracerService;
    protected final Timer executionTimer;
    protected final Counter errorCounter;

    protected AbstractAppbaseHandler(
            final LogService logService,
            final MetricService metricService,
            final TracerService tracerService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(tracerService, "tracerService");

        this.logger = logService.getLogger(getClass());
        this.tracerService = tracerService;
        this.executionTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "execution", "timer"));
        this.errorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "error", "counter"));
    }

    /**
     * Process the message.
     *
     * <p>
     *      For synchronous processing, override {@link #enableAutoEndHandler()} and return true.
     * </p>
     *
     * <p>
     *  But, for asynchronous (callback, future, reactive, ..) handling or for better control,
     *  call {@link AbstractAppbaseHandler#endHandler(AppbaseHandlerRequest)} on success or
     *  {@link AbstractAppbaseHandler#endHandlerWithException(AppbaseHandlerRequest, Throwable)} on exception manually.
     * </p>
     *
     * @param message {@link AppbaseHandlerRequest}
     */
    protected abstract void handleImpl(R message) throws AppbaseException;

    protected boolean enableAutoEndHandler() {
        return false;
    }

    protected void endHandler(final R message) {
        final Span span = currentSpan(message);
        if (span != null) {
            span.finish();
        }

        message.complete();
    }

    protected void endHandlerWithException(final R message, final Throwable throwable) {
        final Span span = currentSpan(message);
        if (span != null) {
            span.finish();
        }

        message.completeWithException(throwable);
    }

    protected void activateSpan(final R message) {
        AppbaseUtil.notNull(message, "message");

        final Tracer.SpanBuilder handlerSpanBuilder;
        if (traceEnabled(message)) {
            final Span parentSpan = parentSpan(message);
            if (parentSpan != null) {
                handlerSpanBuilder = spanBuilder(message, parentSpan);
            } else {
                handlerSpanBuilder = spanBuilder(message);
            }

            final SpanCustomizer<R> spanCustomizer = spanCustomizer(message);
            if (spanCustomizer != null) {
                spanCustomizer.customizeSpan(message, handlerSpanBuilder);
            }

            final Span span = handlerSpanBuilder.start();
            message.getHandlerContext().put(getClass().getName() + APPBASE_HANDLER_SPAN_KEY_SUFFIX, span);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected SpanCustomizer<R> spanCustomizer(final R message) {
        AppbaseUtil.notNull(message, "message");

        final Map<Object, Object> handlerContext = message.getHandlerContext();
        if (handlerContext != null) {
            final Object spanCustomizerValue = handlerContext.get(APPBASE_HANDLER_SPAN_CUSTOMIZER_KEY);
            if (spanCustomizerValue != null && SpanCustomizer.class.isAssignableFrom(spanCustomizerValue.getClass())) {
                return (SpanCustomizer<R>) spanCustomizerValue;
            }
        }

        return null;
    }

    protected Tracer.SpanBuilder spanBuilder(final R message, final Span parentSpan) {
        return tracerService
                .getTracer()
                .buildSpan(getClass().getName())
                .asChildOf(parentSpan);
    }

    protected Tracer.SpanBuilder spanBuilder(final R message) {
        return tracerService
                .getTracer()
                .buildSpan(getClass().getName());
    }

    @Nullable
    protected Span currentSpan(final R message) {
        AppbaseUtil.notNull(message, "message");

        final Map<Object, Object> handlerContext = message.getHandlerContext();
        if (handlerContext != null) {
            final Object currentSpanValue = handlerContext.get(getClass().getName() + APPBASE_HANDLER_SPAN_KEY_SUFFIX);
            if (currentSpanValue != null && Span.class.isAssignableFrom(currentSpanValue.getClass())) {
                return (Span) currentSpanValue;
            }
        }

        return null;
    }

    @Nullable
    protected Span parentSpan(final R message) {
        AppbaseUtil.notNull(message, "message");

        final Map<Object, Object> handlerContext = message.getHandlerContext();
        if (handlerContext != null) {
            final Object parentSpanValue = handlerContext.get(APPBASE_HANDLER_PARENT_SPAN_KEY);
            if (parentSpanValue != null && Span.class.isAssignableFrom(parentSpanValue.getClass())) {
                return (Span) parentSpanValue;
            }
        }

        return null;
    }

    protected boolean traceEnabled(final R message) {
        AppbaseUtil.notNull(message, "message");

        final Map<Object, Object> handlerContext = message.getHandlerContext();
        if (handlerContext != null) {
            final Object enableTraceValue = handlerContext.get(APPBASE_HANDLER_ENABLE_TRACE_KEY);
            if (enableTraceValue != null && Boolean.class.isAssignableFrom(enableTraceValue.getClass())) {
                return (Boolean) enableTraceValue;
            }
        }

        return false;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void handle(final R message) {
        AppbaseUtil.notNull(message, "handlerRequest");

        final Timer.Context timerContext = executionTimer.time();
        boolean abstractAppbaseHandlerRequestType = false;
        try {
            activateSpan(message);

            if (AbstractMutableAppbaseHandlerRequest.class.isAssignableFrom(message.getClass())) {
                final AbstractMutableAppbaseHandlerRequest abstractMutableAppbaseHandlerRequest =
                        (AbstractMutableAppbaseHandlerRequest) message;
                abstractMutableAppbaseHandlerRequest.setTimerContext(timerContext);
                abstractMutableAppbaseHandlerRequest.setErrorCounter(errorCounter);
                abstractAppbaseHandlerRequestType = true;
            }

            if (!message.isValid()) {
                errorCounter.inc();

                final String errorMessage = "The request " + message + " is not in a valid state";
                logger.error(errorMessage);
                message.completeWithException(new AppbaseRuntimeException(errorMessage));
                return;
            }

            handleImpl(message);
        } catch (Exception ex) {
            logger.error("Error in handler: {}", getClass().getName(), ex);

            if (!abstractAppbaseHandlerRequestType) {
                errorCounter.inc();
                timerContext.stop();
            }

            if (enableAutoEndHandler()) {
                endHandlerWithException(message, ex);
            }
        } finally {
            if (!abstractAppbaseHandlerRequestType) {
                timerContext.stop();
            }

            if (enableAutoEndHandler()) {
                endHandler(message);
            }
        }
    }
}
