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

package com.viveworks.appbase.core.service.log.filter.event;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.mediator.event.LogEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.service.mediator.event.TurboLogEvent;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventListener;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Used to notify {@link LogEventFilterListener} and {@link TurboLogEventFilterListener}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DisruptorLogAndTurboLogEventFilterDelegator
        implements LogAndTurboLogEventFilterDelegator, MediatorEventListener
{
    private final boolean acceptLogAfterLogEventListeners;
    private final List<LogEventFilterListener> logEventFilterListeners;
    private final Map<String, Timer> timerByLogEventListenerTypeNameMap;
    private final Map<String, Counter> errorCounterByLogEventListenerTypeNameMap;
    private final AtomicReference<Counter> logEventPublishErrorCounterRef;

    private final boolean acceptLogAfterTurboLogEventListeners;
    private final List<TurboLogEventFilterListener> turboLogEventFilterListeners;
    private final Map<String, Timer> timerByTurboLogEventListenerTypeNameMap;
    private final Map<String, Counter> errorCounterByTurboLogEventListenerTypeNameMap;
    private final AtomicReference<Counter> turboLogEventPublishErrorCounterRef;

    private final DisruptorService<MediatorEvent> disruptorService;

    private final AtomicReference<MetricService> metricServiceRef;
    private final AtomicReference<Counter> insufficientRingBufferCapacityCounterRef;
    private final AtomicReference<Counter> eventTypeMismatchCounterRef;

    private final AtomicBoolean shutdown;

    public DisruptorLogAndTurboLogEventFilterDelegator(
            final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                    disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
            final DisruptorService<MediatorEvent> disruptorService,
            final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry)
    {
        AppbaseUtil.notNull(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                "disruptorLogAndTurboLogEventFilterDelegatorConfiguration");
        AppbaseUtil.notNull(disruptorService, "disruptorService");
        AppbaseUtil.notNull(mediatorEventDelegatorRegistry, "mediatorEventDelegatorRegistry");

        this.acceptLogAfterLogEventListeners =
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isAcceptLogAfterLogEventListeners();
        this.acceptLogAfterTurboLogEventListeners =
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isAcceptLogAfterTurboLogEventListeners();

        this.disruptorService = disruptorService;
        this.logEventFilterListeners = new CopyOnWriteArrayList<>();
        this.turboLogEventFilterListeners = new CopyOnWriteArrayList<>();
        this.metricServiceRef = new AtomicReference<>();
        this.insufficientRingBufferCapacityCounterRef = new AtomicReference<>();
        this.eventTypeMismatchCounterRef = new AtomicReference<>();
        this.timerByLogEventListenerTypeNameMap = new ConcurrentHashMap<>();
        this.errorCounterByLogEventListenerTypeNameMap = new ConcurrentHashMap<>();
        this.timerByTurboLogEventListenerTypeNameMap = new ConcurrentHashMap<>();
        this.errorCounterByTurboLogEventListenerTypeNameMap = new ConcurrentHashMap<>();
        this.logEventPublishErrorCounterRef = new AtomicReference<>();
        this.turboLogEventPublishErrorCounterRef = new AtomicReference<>();

        mediatorEventDelegatorRegistry.registerMediatorEventListener(LogEvent.class, this);
        mediatorEventDelegatorRegistry.registerMediatorEventListener(TurboLogEvent.class, this);

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public boolean accept(final TurboLog turboLog) {
        final Logger logger = turboLog.getLogger();
        if (logger != null && !turboLogEventFilterListeners.isEmpty()) {
            final RingBuffer<MediatorEvent> ringBuffer = disruptorService.getRingBuffer();

            final MetricService metricService = metricServiceRef.get();
            if (metricService != null) {
                final Counter counter = metricService
                        .getMetricRegistry()
                        .counter(metricService.name(getClass(), "insufficientRingBufferCapacity", "counter"));
                insufficientRingBufferCapacityCounterRef.set(counter);
            }

            final Counter insufficientRingBufferCapacityCounter = insufficientRingBufferCapacityCounterRef.get();
            if (ringBuffer.hasAvailableCapacity(AppbaseUtil.ONE)) {
                try {
                    final long sequence = ringBuffer.tryNext();
                    try {
                        final MediatorEvent mediatorEvent = ringBuffer.get(sequence);
                        mediatorEvent.reset();

                        final TurboLogEvent turboLogEvent = mediatorEvent.getTurboLogEvent();
                        mediatorEvent.setMediatorEventType(TurboLogEvent.class);
                        turboLogEvent.setTurboLog(turboLog);
                    } finally {
                        ringBuffer.publish(sequence);
                    }
                } catch (InsufficientCapacityException ice) {
                    if (insufficientRingBufferCapacityCounter != null) {
                        insufficientRingBufferCapacityCounter.inc();
                    }
                } catch (Exception ex) {
                    Counter errorCounter = turboLogEventPublishErrorCounterRef.get();
                    if (errorCounter == null) {
                        if (metricService != null) {
                            errorCounter = metricService
                                    .getMetricRegistry()
                                    .counter(
                                            metricService.name(getClass(), "turboLogEventPublish", "error", "counter"));
                            turboLogEventPublishErrorCounterRef.set(errorCounter);
                        }
                    }

                    if (errorCounter != null) {
                        errorCounter.inc();
                    }
                }
            } else {
                if (insufficientRingBufferCapacityCounter != null) {
                    insufficientRingBufferCapacityCounter.inc();
                }
            }
        }

        return acceptLogAfterTurboLogEventListeners;
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public boolean accept(final ILoggingEvent loggingEvent) {
        if (loggingEvent != null && !logEventFilterListeners.isEmpty()) {
            final RingBuffer<MediatorEvent> ringBuffer = disruptorService.getRingBuffer();

            final MetricService metricService = metricServiceRef.get();
            if (metricService != null) {
                final Counter counter = metricService
                        .getMetricRegistry()
                        .counter(metricService.name(getClass(), "insufficientRingBufferCapacity", "counter"));
                insufficientRingBufferCapacityCounterRef.set(counter);
            }

            final Counter insufficientRingBufferCapacityCounter = insufficientRingBufferCapacityCounterRef.get();
            if (ringBuffer.hasAvailableCapacity(AppbaseUtil.ONE)) {
                try {
                    final long sequence = disruptorService.getRingBuffer().tryNext();
                    try {
                        final MediatorEvent mediatorEvent = ringBuffer.get(sequence);
                        mediatorEvent.reset();

                        final LogEvent logEvent = mediatorEvent.getLogEvent();
                        mediatorEvent.setMediatorEventType(LogEvent.class);
                        logEvent.setLoggingEvent(loggingEvent);
                    } finally {
                        ringBuffer.publish(sequence);
                    }
                } catch (InsufficientCapacityException ice) {
                    if (insufficientRingBufferCapacityCounter != null) {
                        insufficientRingBufferCapacityCounter.inc();
                    }
                } catch (Exception ex) {
                    Counter errorCounter = logEventPublishErrorCounterRef.get();
                    if (errorCounter == null) {
                        if (metricService != null) {
                            errorCounter = metricService
                                    .getMetricRegistry()
                                    .counter(metricService.name(getClass(), "logEventPublish", "error", "counter"));
                            logEventPublishErrorCounterRef.set(errorCounter);
                        }
                    }

                    if (errorCounter != null) {
                        errorCounter.inc();
                    }
                }
            } else {
                if (insufficientRingBufferCapacityCounter != null) {
                    insufficientRingBufferCapacityCounter.inc();
                }
            }
        }

        return acceptLogAfterLogEventListeners;
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void onMediatorEvent(final MediatorEvent mediatorEvent) {
        if (mediatorEvent != null) {
            try {
                final MetricService metricService = metricServiceRef.get();
                final Class<? extends MediatorEventType> mediatorEventType = mediatorEvent.getMediatorEventType();
                if (mediatorEventType == null ||
                        !(LogEvent.class.isAssignableFrom(mediatorEventType) ||
                                TurboLogEvent.class.isAssignableFrom(mediatorEventType)))
                {
                    final Counter eventTypeMismatchCounter = eventTypeMismatchCounterRef.get();

                    if (eventTypeMismatchCounter == null) {
                        if (metricService != null) {
                            final Counter counter = metricService
                                    .getMetricRegistry()
                                    .counter(metricService.name(getClass(), "eventTypeMismatch", "counter"));
                            counter.inc();
                            eventTypeMismatchCounterRef.set(counter);
                        }
                    }

                    return;
                }

                if (LogEvent.class.isAssignableFrom(mediatorEventType)) {
                    final LogEvent logEvent = mediatorEvent.getLogEvent();
                    if (logEvent != null) {
                        final ILoggingEvent loggingEvent = logEvent.getLoggingEvent();
                        if (loggingEvent != null) {
                            logEventFilterListeners.forEach(logEventFilterListener -> {
                                if (logEventFilterListener != null) {
                                    final Timer timer;
                                    final Timer.Context context;
                                    final String listenerTypeName = logEventFilterListener.getClass().getName();

                                    if (metricService != null) {
                                        timer = timerByLogEventListenerTypeNameMap.computeIfAbsent(listenerTypeName,
                                                f -> {
                                                    return metricService
                                                            .getMetricRegistry()
                                                            .timer(metricService.name(
                                                                    getClass(), listenerTypeName, "timer"));
                                        });
                                        context = timer.time();
                                    } else {
                                        timer = null;
                                        context = null;
                                    }

                                    try {
                                        logEventFilterListener.onLog(logEvent.getLoggingEvent());
                                    } catch (Exception ex) {
                                        if (metricService != null) {
                                            errorCounterByLogEventListenerTypeNameMap
                                                    .computeIfAbsent(listenerTypeName, f -> {
                                                        return metricService
                                                                .getMetricRegistry()
                                                                .counter(metricService.name(
                                                                        getClass(),
                                                                        listenerTypeName,
                                                                        "error",
                                                                        "counter"));
                                                    })
                                                    .inc();
                                        }
                                    } finally {
                                        if (timer != null && context != null) {
                                            context.stop();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

                if (TurboLogEvent.class.isAssignableFrom(mediatorEventType)) {
                    final TurboLogEvent turboLogEvent = mediatorEvent.getTurboLogEvent();
                    if (turboLogEvent != null) {
                        final TurboLog turboLog = turboLogEvent.getTurboLog();
                        if (turboLog != null) {
                            turboLogEventFilterListeners.forEach(turboLogEventFilterListener -> {
                                if (turboLogEventFilterListener != null) {
                                    final Timer timer;
                                    final Timer.Context context;
                                    final String turboLogListenerTypeName =
                                            turboLogEventFilterListener.getClass().getName();

                                    if (metricService != null) {
                                        timer = timerByTurboLogEventListenerTypeNameMap.computeIfAbsent(
                                                turboLogListenerTypeName, f -> {
                                                    return metricService
                                                            .getMetricRegistry()
                                                            .timer(metricService.name(
                                                                    getClass(), turboLogListenerTypeName, "timer"));
                                                });
                                        context = timer.time();
                                    } else {
                                        timer = null;
                                        context = null;
                                    }

                                    try {
                                        final Logger logger = turboLog.getLogger();
                                        final String loggerName = (logger != null) ? logger.getName() : null;
                                        turboLogEventFilterListener.onTurboLog(turboLog);
                                    } catch (Exception ex) {
                                        if (metricService != null) {
                                            errorCounterByTurboLogEventListenerTypeNameMap
                                                    .computeIfAbsent(turboLogListenerTypeName, f -> {
                                                        return metricService
                                                                .getMetricRegistry()
                                                                .counter(metricService.name(
                                                                        getClass(),
                                                                        turboLogListenerTypeName,
                                                                        "error",
                                                                        "counter"));
                                                    })
                                                    .inc();
                                        }
                                    } finally {
                                        if (timer != null && context != null) {
                                            context.stop();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            } finally {
                mediatorEvent.reset();
            }
        }
    }

    @Override
    public Collection<LogEventFilterListener> getLogEventFilterListeners() {
        return Collections.unmodifiableCollection(logEventFilterListeners);
    }

    @Override
    public void registerLogEventListener(final LogEventFilterListener logEventFilterListener) {
        AppbaseUtil.notNull(logEventFilterListener, "logEventFilterListener");

        logEventFilterListeners.add(logEventFilterListener);
    }

    @Override
    public void unregisterLogEventListener(final Class<? extends LogEventFilterListener> logEventFilterListenerType) {
        AppbaseUtil.notNull(logEventFilterListenerType, "logEventListenerType");

        for (int i = 0; i < logEventFilterListeners.size(); ++i) {
            final LogEventFilterListener logEventFilterListener = logEventFilterListeners.get(i);
            if (logEventFilterListener.getClass().equals(logEventFilterListenerType)) {
                logEventFilterListeners.remove((int) i);
            }
        }
    }

    @Override
    public void clearLogEventFilterListeners() {
        logEventFilterListeners.clear();
    }

    @Override
    public Collection<TurboLogEventFilterListener> getTurboLogEventFilterListeners() {
        return Collections.unmodifiableList(turboLogEventFilterListeners);
    }

    @Override
    public void registerTurboLogEventListener(final TurboLogEventFilterListener turboLogEventFilterListener) {
        AppbaseUtil.notNull(turboLogEventFilterListener, "turboLogEventFilterListener");

        turboLogEventFilterListeners.add(turboLogEventFilterListener);
    }

    @Override
    public void unregisterTurboLogEventListener(
            final Class<? extends TurboLogEventFilterListener> turboLogEventFilterListenerType)
    {
        for (int i = 0; i < turboLogEventFilterListeners.size(); ++i) {
            final TurboLogEventFilterListener turboLogEventFilterListener = turboLogEventFilterListeners.get(i);
            if (turboLogEventFilterListener.getClass().equals(turboLogEventFilterListenerType)) {
                turboLogEventFilterListeners.remove((int) i);
            }
        }
    }

    @Override
    public void clearTurboLogEventListeners() {
        turboLogEventFilterListeners.clear();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        clearLogEventFilterListeners();
        clearTurboLogEventListeners();
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
