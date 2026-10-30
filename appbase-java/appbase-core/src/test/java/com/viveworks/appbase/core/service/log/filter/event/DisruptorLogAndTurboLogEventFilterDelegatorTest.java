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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventFactory;
import com.viveworks.appbase.core.service.mediator.event.delegator.DefaultMediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.DisruptorMediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService;
import com.viveworks.appbase.core.util.disruptor.DefaultExceptionHandler;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DisruptorLogAndTurboLogEventFilterDelegatorTest {
    private static LogService logService;
    private static MediatorService mediatorService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry;
    private static DefaultDisruptorService<MediatorEvent> defaultDisruptorService;
    private static DisruptorLogAndTurboLogEventFilterDelegator disruptorLogAndTurboLogEventFilterDelegator;
    private static long timestamp1;
    private static long timestamp2;
    private static AtomicInteger loggingEventCounter;
    private static AtomicInteger turboLogCounter;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        mediatorEventDelegatorRegistry = new DefaultMediatorEventDelegatorRegistry();
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(YieldingWaitStrategyConfiguration.builder().build())
                        .build();
        defaultDisruptorService = new DefaultDisruptorService<>(
                defaultDisruptorServiceConfiguration,
                logService,
                mediatorService,
                metricService,
                validationService,
                new DefaultExceptionHandler<>(),
                new MediatorEventFactory(),
                new DisruptorMediatorEventHandlerDelegator(mediatorEventDelegatorRegistry));
        disruptorLogAndTurboLogEventFilterDelegator = new DisruptorLogAndTurboLogEventFilterDelegator(
                DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.builder().enabled(true).build(),
                defaultDisruptorService,
                mediatorEventDelegatorRegistry);
        disruptorLogAndTurboLogEventFilterDelegator.setMetricService(metricService);
        timestamp1 = System.currentTimeMillis();
        timestamp2 = timestamp1 + 10000;
        loggingEventCounter = new AtomicInteger(0);
        turboLogCounter = new AtomicInteger(0);
    }

    @AfterAll
    static void tearDown() {
        if (disruptorLogAndTurboLogEventFilterDelegator != null) {
            disruptorLogAndTurboLogEventFilterDelegator.shutdown();
        }

        if (defaultDisruptorService != null) {
            defaultDisruptorService.shutdown();
        }

        if (mediatorEventDelegatorRegistry != null) {
            mediatorEventDelegatorRegistry.clearMediatorEventListeners();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testDelegator() throws Exception {
        Assertions.assertNotNull(disruptorLogAndTurboLogEventFilterDelegator);

        disruptorLogAndTurboLogEventFilterDelegator
                .registerLogEventListener(new TestLogEventFilterListener());
        disruptorLogAndTurboLogEventFilterDelegator
                .registerTurboLogEventListener(new TestTurboLogEventFilterListener());

        final Collection<LogEventFilterListener> unmodifiableLogEventFilterListeners =
                disruptorLogAndTurboLogEventFilterDelegator.getLogEventFilterListeners();
        Assertions.assertNotNull(unmodifiableLogEventFilterListeners);
        Assertions.assertEquals(1, unmodifiableLogEventFilterListeners.size());

        final Collection<TurboLogEventFilterListener> unmodifiableTurboLogEventFilterListeners =
                disruptorLogAndTurboLogEventFilterDelegator.getTurboLogEventFilterListeners();
        Assertions.assertNotNull(unmodifiableTurboLogEventFilterListeners);
        Assertions.assertEquals(1, unmodifiableTurboLogEventFilterListeners.size());

        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .timestamp(timestamp1)
                .build();
        disruptorLogAndTurboLogEventFilterDelegator.accept(turboLog1);
        Thread.sleep(1000);
        Assertions.assertEquals(1, turboLogCounter.get());

        final LoggingEvent loggingEvent1 = new LoggingEvent();
        final Throwable throwable = new AppbaseException(
                "Throwable message 1", new AppbaseRuntimeException("Cause message 1"));
        loggingEvent1.setLevel(Level.INFO);
        loggingEvent1.setMessage("Message");
        loggingEvent1.setTimeStamp(timestamp2);
        loggingEvent1.setThreadName(Thread.currentThread().getName());
        loggingEvent1.setThrowableProxy(new ThrowableProxy(throwable));
        loggingEvent1.setCallerData(Thread.currentThread().getStackTrace());
        loggingEvent1.setMDCPropertyMap(Map.of("Key", "Value"));
        loggingEvent1.addMarker(MarkerFactory.getMarker("MARKER"));
        loggingEvent1.setLoggerName(getClass().getName());
        loggingEvent1.setArgumentArray(new Object[] {"A", "B", "C"});
        disruptorLogAndTurboLogEventFilterDelegator.accept(loggingEvent1);
        Thread.sleep(1000);
        Assertions.assertEquals(1, loggingEventCounter.get());

        disruptorLogAndTurboLogEventFilterDelegator
                .unregisterLogEventListener(TestLogEventFilterListener.class);
        disruptorLogAndTurboLogEventFilterDelegator
                .unregisterTurboLogEventListener(TestTurboLogEventFilterListener.class);

        final Collection<LogEventFilterListener> unmodifiableLogEventFilterListeners1 =
                disruptorLogAndTurboLogEventFilterDelegator.getLogEventFilterListeners();
        Assertions.assertNotNull(unmodifiableLogEventFilterListeners1);
        Assertions.assertTrue(unmodifiableLogEventFilterListeners1.isEmpty());

        final Collection<TurboLogEventFilterListener> unmodifiableTurboLogEventFilterListeners2 =
                disruptorLogAndTurboLogEventFilterDelegator.getTurboLogEventFilterListeners();
        Assertions.assertNotNull(unmodifiableTurboLogEventFilterListeners2);
        Assertions.assertTrue(unmodifiableTurboLogEventFilterListeners2.isEmpty());
    }

    @Test
    @Order(1)
    void testShutdown() {
        disruptorLogAndTurboLogEventFilterDelegator.clearTurboLogEventListeners();
        disruptorLogAndTurboLogEventFilterDelegator.clearLogEventFilterListeners();

        disruptorLogAndTurboLogEventFilterDelegator.registerLogEventListener(new TestLogEventFilterListener());
        final Collection<LogEventFilterListener> logEventFilterListeners =
                disruptorLogAndTurboLogEventFilterDelegator.getLogEventFilterListeners();
        Assertions.assertNotNull(logEventFilterListeners);
        Assertions.assertEquals(1, logEventFilterListeners.size());

        disruptorLogAndTurboLogEventFilterDelegator.unregisterLogEventListener(TestLogEventFilterListener.class);
        final Collection<LogEventFilterListener> logEventFilterListeners1 =
                disruptorLogAndTurboLogEventFilterDelegator.getLogEventFilterListeners();
        Assertions.assertNotNull(logEventFilterListeners1);
        Assertions.assertTrue(logEventFilterListeners1.isEmpty());

        disruptorLogAndTurboLogEventFilterDelegator.registerLogEventListener(new TestLogEventFilterListener());
        final Collection<LogEventFilterListener> logEventFilterListeners2 =
                disruptorLogAndTurboLogEventFilterDelegator.getLogEventFilterListeners();
        Assertions.assertNotNull(logEventFilterListeners2);
        Assertions.assertEquals(1, logEventFilterListeners2.size());

        disruptorLogAndTurboLogEventFilterDelegator.clearLogEventFilterListeners();
        final Collection<LogEventFilterListener> logEventFilterListeners3 =
                disruptorLogAndTurboLogEventFilterDelegator.getLogEventFilterListeners();
        Assertions.assertNotNull(logEventFilterListeners3);
        Assertions.assertTrue(logEventFilterListeners3.isEmpty());


        disruptorLogAndTurboLogEventFilterDelegator.registerTurboLogEventListener(
                new TestTurboLogEventFilterListener());
        final Collection<TurboLogEventFilterListener> turboLogEventFilterListeners =
                disruptorLogAndTurboLogEventFilterDelegator.getTurboLogEventFilterListeners();
        Assertions.assertNotNull(turboLogEventFilterListeners);
        Assertions.assertEquals(1, turboLogEventFilterListeners.size());

        disruptorLogAndTurboLogEventFilterDelegator.unregisterTurboLogEventListener(
                TestTurboLogEventFilterListener.class);
        final Collection<TurboLogEventFilterListener> turboLogEventFilterListeners1 =
                disruptorLogAndTurboLogEventFilterDelegator.getTurboLogEventFilterListeners();
        Assertions.assertNotNull(turboLogEventFilterListeners1);
        Assertions.assertTrue(turboLogEventFilterListeners1.isEmpty());

        disruptorLogAndTurboLogEventFilterDelegator.registerTurboLogEventListener(
                new TestTurboLogEventFilterListener());
        final Collection<TurboLogEventFilterListener> turboLogEventFilterListeners2 =
                disruptorLogAndTurboLogEventFilterDelegator.getTurboLogEventFilterListeners();
        Assertions.assertNotNull(turboLogEventFilterListeners2);
        Assertions.assertEquals(1, turboLogEventFilterListeners2.size());

        disruptorLogAndTurboLogEventFilterDelegator.clearTurboLogEventListeners();
        final Collection<TurboLogEventFilterListener> turboLogEventFilterListeners3 =
                disruptorLogAndTurboLogEventFilterDelegator.getTurboLogEventFilterListeners();
        Assertions.assertNotNull(turboLogEventFilterListeners3);
        Assertions.assertTrue(turboLogEventFilterListeners3.isEmpty());
    }

    private static class TestTurboLogEventFilterListener implements TurboLogEventFilterListener {
        @Override
        public void onTurboLog(final TurboLog turboLog) {
            Assertions.assertNotNull(turboLog);
            Assertions.assertEquals(LogLevel.ERROR, turboLog.getLogLevel());
            Assertions.assertEquals("Formatted Message 1: {}", turboLog.getFormattedMessage());
            Assertions.assertEquals(timestamp1, turboLog.getTimestamp());
            turboLogCounter.incrementAndGet();
        }
    }

    private static class TestLogEventFilterListener implements LogEventFilterListener {
        @Override
        public void onLog(final ILoggingEvent loggingEvent) {
            Assertions.assertNotNull(loggingEvent);
            Assertions.assertEquals(Level.INFO, loggingEvent.getLevel());
            Assertions.assertEquals("Message", loggingEvent.getMessage());
            Assertions.assertEquals(timestamp2, loggingEvent.getTimeStamp());
            Assertions.assertEquals(Map.of("Key", "Value"), loggingEvent.getMDCPropertyMap());
            Assertions.assertEquals("MARKER", loggingEvent.getMarker().getName());
            Assertions.assertEquals(
                    DisruptorLogAndTurboLogEventFilterDelegatorTest.class.getName(), loggingEvent.getLoggerName());
            Assertions.assertArrayEquals(new Object[] {"A", "B", "C"}, loggingEvent.getArgumentArray());
            loggingEventCounter.incrementAndGet();
        }
    }
}
