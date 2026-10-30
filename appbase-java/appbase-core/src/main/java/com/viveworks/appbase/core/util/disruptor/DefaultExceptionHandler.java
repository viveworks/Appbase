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

package com.viveworks.appbase.core.util.disruptor;

import com.lmax.disruptor.ExceptionHandler;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ExceptionHandler}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Event type
 * @since 1.0.0
 */
@ThreadSafe
public final class DefaultExceptionHandler<T> implements ExceptionHandler<T> {
    private final AtomicReference<Logger> loggerRef;
    private final AtomicReference<Counter> errorCounterRef;

    public DefaultExceptionHandler() {
        this.loggerRef = new AtomicReference<>();
        this.errorCounterRef = new AtomicReference<>();
    }

    @Override
    public void handleEventException(final Throwable ex, final long sequence, final T event) {
        final Counter errorCounter = errorCounterRef.get();
        if (errorCounter != null) {
            errorCounter.inc();
        }

        final Logger logger = loggerRef.get();
        if (logger != null) {
            logger.error("Exception in handling disruptor event: {}", event, ex);
        }
    }

    @Override
    public void handleOnStartException(final Throwable ex) {
        throw new AppbaseRuntimeException("Error while starting the disruptor", ex);
    }

    @Override
    public void handleOnShutdownException(final Throwable ex) {
        final Logger logger = loggerRef.get();
        if (logger != null) {
            logger.error("Error while shutting down the disruptor", ex);
        }
    }

    public Logger getLogger() {
        return loggerRef.get();
    }

    /**
     * Set {@link LogService}.
     *
     * @param logService Log service to set
     */
    public void setLogService(final LogService logService) {
        this.loggerRef.set(logService.getLogger(getClass()));
    }

    /**
     * Initialize metric using {@link MetricService}.
     *
     * @param metricService Metric service
     */
    public void initMetrics(final MetricService metricService) {
        final Counter errorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "error", "counter"));
        errorCounterRef.set(errorCounter);
    }

    public Counter getErrorCounter() {
        return errorCounterRef.get();
    }
}
