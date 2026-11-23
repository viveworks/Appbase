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

package com.viveworks.appbase.core.service.tracer.reporter;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.spi.Reporter;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Composite {@link Reporter} that allows adding reporters after creation.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class CompositeJaegerReporter implements Reporter {
    private final Logger logger;
    private final MetricService metricService;
    private final MediatorService mediatorService;
    private final AtomicInteger idGenerator;
    private final Map<String, Reporter> reporters;
    private final Map<String, Timer> timerByUniqueReporterNameMap;
    private final Map<String, Counter> failureCounterByUniqueReporterNameMap;
    private final ReentrantLock reportersLock;

    public CompositeJaegerReporter(final LogService logService,
                                   final MetricService metricService,
                                   final MediatorService mediatorService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");

        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
        this.mediatorService = mediatorService;
        this.idGenerator = new AtomicInteger(0);
        this.reporters = new ConcurrentHashMap<>();
        this.timerByUniqueReporterNameMap = new ConcurrentHashMap<>();
        this.failureCounterByUniqueReporterNameMap = new ConcurrentHashMap<>();
        this.reportersLock = new ReentrantLock();
    }

    /**
     * Add a reporter.
     *
     * @param reporter Reporter
     */
    public synchronized void addReporter(final Reporter reporter) {
        AppbaseUtil.notNull(reporter, "reporter");

        final int id = idGenerator.incrementAndGet();
        final String reporterName =
                metricService.name(reporter.getClass(), String.valueOf(id));
        logger.info("Adding jaeger reporter: {}", reporterName);

        final String reporterTimerName =
                metricService.name(reporter.getClass(), "timer", String.valueOf(id));
        final Timer timer = metricService
                .getMetricRegistry()
                .timer(reporterTimerName);

        final String reporterErrorCounterName =
                metricService.name(reporter.getClass(), "error", "counter", String.valueOf(id));
        final Counter counter = metricService
                .getMetricRegistry()
                .counter(reporterErrorCounterName);

        timerByUniqueReporterNameMap.put(reporterName, timer);
        failureCounterByUniqueReporterNameMap.put(reporterName, counter);
        reporters.put(reporterName, reporter);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void report(final JaegerSpan span) {
        if (span != null) {
            reportersLock.lock();
            try {
                reporters.forEach((name, reporter) -> {
                    final Timer timer = timerByUniqueReporterNameMap.get(name);
                    Timer.Context context = null;
                    if (timer != null) {
                        context = timer.time();
                    }

                    try {
                        reporter.report(span);
                    } catch (Exception ex) {
                        final String message = "Error reporting span: " + span;
                        final Message exceptionMessage = MessageFactory.exceptionMessage(
                                mediatorService,
                                getClass(),
                                message,
                                AppbaseUtil.threadName(),
                                ex,
                                null);
                        handleMessage(exceptionMessage);
                        failureCounterByUniqueReporterNameMap
                                .get(name)
                                .inc();
                    } finally {
                        if (context != null) {
                            context.stop();
                        }
                    }
                });
            } finally {
                reportersLock.unlock();
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        reportersLock.lock();
        try {
            reporters.forEach((name, reporter) -> {
               try {
                   reporter.close();
               } catch (Exception ex) {
                   final String message = "Error closing reporter: " + name;
                   final Message exceptionMessage = MessageFactory.exceptionMessage(
                           mediatorService,
                           getClass(),
                           message,
                           AppbaseUtil.threadName(),
                           ex,
                           null);
                   handleMessage(exceptionMessage);
               }
            });
        } finally {
            reportersLock.unlock();
        }
    }

    private void handleMessage(final Message message) {
        try {
            mediatorService.getMediator().asyncHandleMessage(message, true);
        } catch (MediatorException me) {
            logger.error("Error sending message to mediator: {}", message, me);
        }
    }

    public Map<String, Counter> getFailureCounterByUniqueReporterNameMap() {
        return failureCounterByUniqueReporterNameMap;
    }

    public Map<String, Timer> getTimerByUniqueReporterNameMap() {
        return timerByUniqueReporterNameMap;
    }

    @Override
    public String toString() {
        return "CompositeJaegerReporter{" +
                "reporters=" + reporters.keySet() +
                '}';
    }
}
