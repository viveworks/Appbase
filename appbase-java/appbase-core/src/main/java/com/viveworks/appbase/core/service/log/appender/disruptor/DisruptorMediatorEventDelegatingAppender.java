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

package com.viveworks.appbase.core.service.log.appender.disruptor;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilter;
import com.viveworks.appbase.core.service.mediator.event.LogEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link DisruptorService} based {@link Appender}. This appender simply drops log events when the ring buffer is full.
 * A {@link BurstTurboLogEventFilter} can be used to
 * handle such bursts. The main use case for this appender is to be able to perform disk IO in another thread, so
 * non-blocking framework threads (e.x server) are not held up.
 *
 * <p>
 * NOTE: It sets the {@link MediatorEvent#getMediatorEventType()} to this class and handles the event itself.
 * Appenders that wish to use this class, need to register using {@link #addAppender(Appender)}.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class DisruptorMediatorEventDelegatingAppender
        extends UnsynchronizedAppenderBase<ILoggingEvent> implements MediatorEventDelegatingAppender, MediatorEventType
{
    private static final int ONE = 1;

    private final boolean enabled;
    private final DisruptorService<MediatorEvent> disruptorService;
    private final AppenderAttachable<ILoggingEvent> appenderAttachable;
    private final AtomicReference<MetricService> metricServiceRef;
    private final AtomicReference<Counter> insufficientRingBufferCapacityCounterRef;
    private final AtomicReference<Counter> eventTypeMismatchCounterRef;
    private final Map<String, Counter> errorCounterByAppenderNameMap;
    private final Map<String, Timer> timerByAppenderNameMap;
    private final AtomicReference<Counter> publishMediatorEventErrorCounterRef;

    public DisruptorMediatorEventDelegatingAppender(
            final DisruptorMediatorEventDelegatingAppenderConfiguration
                    disruptorMediatorEventDelegatingAppenderConfiguration,
            final DisruptorService<MediatorEvent> disruptorService,
            final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry)
    {
        AppbaseUtil.notNull(
                disruptorMediatorEventDelegatingAppenderConfiguration,
                "disruptorMediatorEventDelegatingAppenderConfiguration");
        AppbaseUtil.notNull(disruptorService, "disruptorService");
        AppbaseUtil.notNull(mediatorEventDelegatorRegistry, "mediatorEventDelegatorRegistry");

        this.enabled = disruptorMediatorEventDelegatingAppenderConfiguration.isEnabled();
        this.disruptorService = disruptorService;
        this.appenderAttachable = new AppenderAttachableImpl<>();
        this.metricServiceRef = new AtomicReference<>();
        this.errorCounterByAppenderNameMap = new ConcurrentHashMap<>();
        this.timerByAppenderNameMap = new ConcurrentHashMap<>();
        this.insufficientRingBufferCapacityCounterRef = new AtomicReference<>();
        this.eventTypeMismatchCounterRef = new AtomicReference<>();
        publishMediatorEventErrorCounterRef = new AtomicReference<>();

        mediatorEventDelegatorRegistry.registerMediatorEventListener(getClass(), this);
        started = true;
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    protected void append(final ILoggingEvent eventObject) {
        if (!enabled) {
            return;
        }

        if (eventObject != null) {
            final RingBuffer<MediatorEvent> ringBuffer = disruptorService.getRingBuffer();

            final MetricService metricService = metricServiceRef.get();
            if (metricService != null) {
                final Counter counter = metricService
                        .getMetricRegistry()
                        .counter(metricService.name(getClass(), "insufficientRingBufferCapacity", "counter"));
                insufficientRingBufferCapacityCounterRef.set(counter);
            }

            final Counter counter = insufficientRingBufferCapacityCounterRef.get();
            if (ringBuffer.hasAvailableCapacity(ONE)) {
                try {
                    final long sequence = ringBuffer.tryNext();
                    try {
                        final MediatorEvent mediatorEvent = ringBuffer.get(sequence);
                        mediatorEvent.reset();
                        mediatorEvent.setMediatorEventType(getClass());
                        mediatorEvent.getLogEvent().setLoggingEvent(eventObject);
                    } finally {
                        ringBuffer.publish(sequence);
                    }
                } catch (InsufficientCapacityException ice) {
                    if (counter != null) {
                        counter.inc();
                    }
                } catch (Exception ex) {
                    Counter errorCounter = publishMediatorEventErrorCounterRef.get();
                    if (errorCounter == null) {
                        if (metricService != null) {
                            errorCounter = metricService
                                    .getMetricRegistry()
                                    .counter(metricService.name(
                                            getClass(), "publishMediatorEvent", "error", "counter"));
                            publishMediatorEventErrorCounterRef.set(errorCounter);
                        }
                    }

                    if (errorCounter != null) {
                        errorCounter.inc();
                    }
                }
            } else {
                if (counter != null) {
                    counter.inc();
                }
            }
        }
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public void onMediatorEvent(final MediatorEvent mediatorEvent) {
        if (!enabled) {
            return;
        }

        if (mediatorEvent != null) {
            try {
                final Class<? extends MediatorEventType> mediatorEventType = mediatorEvent.getMediatorEventType();
                if (mediatorEventType == null || !getClass().isAssignableFrom(mediatorEventType)) {
                    final Counter eventTypeMismatchCounter = eventTypeMismatchCounterRef.get();

                    if (eventTypeMismatchCounter == null) {
                        final MetricService metricService = metricServiceRef.get();
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

                final LogEvent logEvent = mediatorEvent.getLogEvent();
                if (logEvent != null) {
                    final ILoggingEvent loggingEvent = logEvent.getLoggingEvent();
                    if (loggingEvent != null) {
                        final Iterator<Appender<ILoggingEvent>> appenderIterator =
                                appenderAttachable.iteratorForAppenders();
                        if (appenderIterator != null) {
                            final MetricService metricService = metricServiceRef.get();
                            while (appenderIterator.hasNext()) {
                                final Appender<ILoggingEvent> appender = appenderIterator.next();

                                final String appenderName = appender.getName();
                                if (!AppbaseUtil.isBlank(appenderName)) {
                                    final Timer timer;
                                    final Timer.Context timerContext;
                                    if (metricService != null) {
                                        timer = timerByAppenderNameMap
                                                .computeIfAbsent(appenderName, f -> metricService
                                                        .getMetricRegistry()
                                                        .timer(metricService.name(
                                                                getClass(), appenderName, "timer")));
                                        timerContext = timer.time();
                                    } else {
                                        timer = null;
                                        timerContext = null;
                                    }

                                    try {
                                        appender.doAppend(loggingEvent);
                                    } catch (Exception ex) {
                                        if (metricService != null) {
                                            errorCounterByAppenderNameMap
                                                    .computeIfAbsent(appenderName, f -> metricService
                                                            .getMetricRegistry()
                                                            .counter(metricService.name(
                                                                    getClass(), appenderName, "error", "counter")))
                                                    .inc();
                                        }
                                    } finally {
                                        if (timer != null && timerContext != null) {
                                            timerContext.stop();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                mediatorEvent.reset();
            }
        }
    }

    @Override
    public void addAppender(final Appender<ILoggingEvent> newAppender) {
        final String appenderName = newAppender.getName();
        AppbaseUtil.notBlank(appenderName, "appenderName");

        appenderAttachable.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return appenderAttachable.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(final String name) {
        return appenderAttachable.getAppender(name);
    }

    @Override
    public boolean isAttached(final Appender<ILoggingEvent> appender) {
        return appenderAttachable.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenderAttachable.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(final Appender<ILoggingEvent> appender) {
        return appenderAttachable.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(final String name) {
        return appenderAttachable.detachAppender(name);
    }

    @Override
    public String getName() {
        return getClass().getName();
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
