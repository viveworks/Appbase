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

package com.viveworks.appbase.core.service.mediator.event.delegator;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link com.lmax.disruptor.EventHandler} that delegates {@link MediatorEvent} to {@link MediatorEventListener}s
 * using {@link MediatorEventDelegatorRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public final class DisruptorMediatorEventHandlerDelegator implements MediatorEventHandlerDelegator {
    private final AtomicReference<Logger> loggerRef;
    private final AtomicReference<MetricService> metricServiceRef;
    private final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry;
    private final Map<String, Counter> emptyListenersCounterByEventTypeMap;
    private final Map<String, Timer> timerByListenerTypeMap;
    private final Map<String, Counter> errorCounterByListenerTypeMap;

    public DisruptorMediatorEventHandlerDelegator(final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry) {
        AppbaseUtil.notNull(mediatorEventDelegatorRegistry, "mediatorEventDelegatorRegistry");

        this.loggerRef = new AtomicReference<>();
        this.metricServiceRef = new AtomicReference<>();
        this.mediatorEventDelegatorRegistry = mediatorEventDelegatorRegistry;
        this.emptyListenersCounterByEventTypeMap = new ConcurrentHashMap<>();
        this.timerByListenerTypeMap = new ConcurrentHashMap<>();
        this.errorCounterByListenerTypeMap = new ConcurrentHashMap<>();
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void onEvent(final MediatorEvent mediatorEvent,
                        final long sequence,
                        final boolean endOfBatch)
    {
        if (mediatorEvent != null) {
            try {
                final Class<? extends MediatorEventType> mediatorEventType = mediatorEvent.getMediatorEventType();
                if (mediatorEventType != null) {
                    final Logger logger = loggerRef.get();
                    final MetricService metricService = metricServiceRef.get();

                    final String eventTypeName = mediatorEventType.getName();
                    final Collection<MediatorEventListener> mediatorEventListeners =
                            mediatorEventDelegatorRegistry.getMediatorEventListeners(mediatorEventType);
                    if (mediatorEventListeners == null || mediatorEventListeners.isEmpty()) {
                        if (metricService != null) {
                            emptyListenersCounterByEventTypeMap
                                    .computeIfAbsent(eventTypeName, f -> metricService
                                            .getMetricRegistry()
                                            .counter(metricService.name(
                                                    getClass(), eventTypeName, "emptyListeners", "counter")))
                                    .inc();
                        }

                        if (logger != null) {
                            logger.warn("No {} found for {}", MediatorEventListener.class.getName(), eventTypeName);
                        }
                    } else {
                        mediatorEventListeners.forEach(mediatorEventListener -> {
                            final String listenerTypeName = mediatorEventListener.getClass().getName();
                            final long startTimestamp = System.nanoTime();
                            try {
                                mediatorEventListener.onMediatorEvent(mediatorEvent);
                            } catch (Exception ex) {
                                if (logger != null) {
                                    logger.error("Error in {} for {}", listenerTypeName, eventTypeName, ex);
                                }

                                if (metricService != null) {
                                    errorCounterByListenerTypeMap
                                            .computeIfAbsent(listenerTypeName, f -> metricService
                                                    .getMetricRegistry()
                                                    .counter(metricService.name(
                                                            getClass(), listenerTypeName, "error", "counter")))
                                            .inc();
                                }
                            } finally {
                                if (metricService != null) {
                                    final Timer timer = timerByListenerTypeMap
                                            .computeIfAbsent(listenerTypeName, f -> metricService
                                                    .getMetricRegistry()
                                                    .timer(metricService.name(getClass(), listenerTypeName, "timer")));
                                    timer.update(Duration.ofMillis(System.nanoTime() - startTimestamp));
                                }
                            }
                        });
                    }
                }
            } finally {
                mediatorEvent.reset();
            }
        }
    }

    @Override
    public MediatorEventDelegatorRegistry getMediatorEventDelegatorRegistry() {
        return mediatorEventDelegatorRegistry;
    }


    /**
     * Set the {@link LogService}.
     *
     * @param logService Log service to set
     */
    public void setLogService(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        this.loggerRef.set(logService.getLogger(getClass()));
    }

    /**
     * Set the {@link MetricService}.
     *
     * @param metricService Metric service to set
     */
    public void setMetricService(final MetricService metricService) {
        AppbaseUtil.notNull(metricService, "metricService");

        this.metricServiceRef.set(metricService);
    }
}
