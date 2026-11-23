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

package com.viveworks.appbase.core.service.log.appender.disruptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventFactory;
import com.viveworks.appbase.core.service.mediator.event.delegator.DefaultMediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.DisruptorMediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService;
import com.viveworks.appbase.core.util.disruptor.DefaultExceptionHandler;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DisruptorMediatorEventDelegatingAppenderTest {
    private static LogService logService;
    private static MediatorService mediatorService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry;
    private static DefaultDisruptorService<MediatorEvent> defaultDisruptorService;
    private static DisruptorMediatorEventDelegatingAppender disruptorMediatorEventDelegatingAppender;
    private static AtomicInteger eventCounter;
    private static LoggingEvent loggingEvent;

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

        final DisruptorMediatorEventDelegatingAppenderConfiguration
                disruptorMediatorEventDelegatingAppenderConfiguration =
                DisruptorMediatorEventDelegatingAppenderConfiguration.builder().enabled(true).build();
        disruptorMediatorEventDelegatingAppender = new DisruptorMediatorEventDelegatingAppender(
                disruptorMediatorEventDelegatingAppenderConfiguration,
                defaultDisruptorService,
                mediatorEventDelegatorRegistry);
        disruptorMediatorEventDelegatingAppender.setMetricService(metricService);
        eventCounter = new AtomicInteger(0);
    }

    @AfterAll
    static void tearDown() {
        if (disruptorMediatorEventDelegatingAppender != null) {
            disruptorMediatorEventDelegatingAppender.stop();
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
    void testAppender() throws Exception {
        Assertions.assertTrue(disruptorMediatorEventDelegatingAppender.isStarted());
        Assertions.assertFalse(AppbaseUtil.isBlank(disruptorMediatorEventDelegatingAppender.getName()));

        final TestAppender testAppender = new TestAppender();
        Assertions.assertDoesNotThrow(() -> disruptorMediatorEventDelegatingAppender.addAppender(testAppender));

        loggingEvent = new LoggingEvent();
        loggingEvent.setLevel(Level.INFO);
        loggingEvent.setMessage("Message 1");
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setThreadName(Thread.currentThread().getName());
        final Throwable throwable = new AppbaseException(
                "Throwable message 1", new AppbaseRuntimeException("Cause message 1"));
        loggingEvent.setThrowableProxy(new ThrowableProxy(throwable));
        loggingEvent.setCallerData(Thread.currentThread().getStackTrace());
        loggingEvent.setMDCPropertyMap(Map.of("Key 1", "Value 1"));
        loggingEvent.addMarker(MarkerFactory.getMarker("MARKER 1"));
        loggingEvent.setLoggerName(getClass().getName());
        loggingEvent.setArgumentArray(new Object[] {"A1", "B1", "C1"});
        disruptorMediatorEventDelegatingAppender.append(loggingEvent);
        Thread.sleep(1000);
        Assertions.assertEquals(1, eventCounter.get());

        loggingEvent = new LoggingEvent();
        loggingEvent.setLevel(Level.INFO);
        loggingEvent.setMessage("Message 2");
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setThreadName(Thread.currentThread().getName());
        final Throwable throwable1 = new AppbaseException(
                "Throwable message 2", new AppbaseRuntimeException("Cause message 2"));
        loggingEvent.setThrowableProxy(new ThrowableProxy(throwable1));
        loggingEvent.setCallerData(Thread.currentThread().getStackTrace());
        loggingEvent.setMDCPropertyMap(Map.of("Key 2", "Value 2"));
        loggingEvent.addMarker(MarkerFactory.getMarker("MARKER 2"));
        loggingEvent.setLoggerName(getClass().getName());
        loggingEvent.setArgumentArray(new Object[] {"A2", "B2", "C2"});
        disruptorMediatorEventDelegatingAppender.append(loggingEvent);
        Thread.sleep(1000);
        Assertions.assertEquals(2, eventCounter.get());


        final Iterator<Appender<ILoggingEvent>> iterator =
                disruptorMediatorEventDelegatingAppender.iteratorForAppenders();
        Assertions.assertNotNull(iterator);
        final AtomicInteger appenderCount = new AtomicInteger(0);
        iterator.forEachRemaining(iLoggingEventAppender -> appenderCount.incrementAndGet());
        Assertions.assertEquals(1, appenderCount.get());

        final String appenderName = testAppender.getName();
        Assertions.assertFalse(AppbaseUtil.isBlank(appenderName));

        final Appender<ILoggingEvent> appender = disruptorMediatorEventDelegatingAppender.getAppender(appenderName);
        Assertions.assertNotNull(appender);
        Assertions.assertEquals(testAppender, appender);

        Assertions.assertTrue(disruptorMediatorEventDelegatingAppender.isAttached(testAppender));
        Assertions.assertTrue(disruptorMediatorEventDelegatingAppender.detachAppender(appenderName));
        Assertions.assertFalse(disruptorMediatorEventDelegatingAppender.isAttached(testAppender));

        Assertions.assertDoesNotThrow(() -> disruptorMediatorEventDelegatingAppender.addAppender(testAppender));
        Assertions.assertTrue(disruptorMediatorEventDelegatingAppender.detachAppender(testAppender));
        Assertions.assertFalse(disruptorMediatorEventDelegatingAppender.isAttached(testAppender));

        Assertions.assertDoesNotThrow(() -> disruptorMediatorEventDelegatingAppender.addAppender(testAppender));
        Assertions.assertTrue(disruptorMediatorEventDelegatingAppender.isAttached(testAppender));
        Assertions.assertDoesNotThrow(() -> disruptorMediatorEventDelegatingAppender.detachAndStopAllAppenders());
        Assertions.assertFalse(disruptorMediatorEventDelegatingAppender.isAttached(testAppender));
    }

    private static class TestAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        TestAppender() {
            started = true;
        }

        @Override
        protected void append(final ILoggingEvent eventObject) {
            Assertions.assertEquals(loggingEvent.getLevel(), eventObject.getLevel());
            Assertions.assertEquals(loggingEvent.getMessage(), eventObject.getMessage());
            Assertions.assertEquals(loggingEvent.getTimeStamp(), eventObject.getTimeStamp());
            Assertions.assertEquals(loggingEvent.getThreadName(), eventObject.getThreadName());
            Assertions.assertEquals(loggingEvent.getThrowableProxy(), eventObject.getThrowableProxy());
            Assertions.assertEquals(loggingEvent.getCallerData(), eventObject.getCallerData());
            Assertions.assertEquals(loggingEvent.getMDCPropertyMap(), eventObject.getMDCPropertyMap());
            Assertions.assertEquals(loggingEvent.getMarker(), eventObject.getMarker());
            Assertions.assertEquals(loggingEvent.getLoggerName(), eventObject.getLoggerName());
            Assertions.assertArrayEquals(loggingEvent.getArgumentArray(), eventObject.getArgumentArray());
            eventCounter.incrementAndGet();
        }

        @Override
        public String getName() {
            return getClass().getName();
        }
    }
}
