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
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.spi.FilterReply;
import com.viveworks.appbase.core.AppbaseException;
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
import com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService;
import com.viveworks.appbase.core.util.disruptor.DefaultExceptionHandler;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 */
class LogEventFilterTest {
    private static LogService logService;
    private static MediatorService mediatorService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry;
    private static DefaultDisruptorService<MediatorEvent> defaultDisruptorService;
    private static DisruptorLogAndTurboLogEventFilterDelegator disruptorLogAndTurboLogEventFilterDelegator;

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
    }

    @AfterAll
    static void tearDown() {
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
    void testFilter() {
        final LogEventFilter logEventFilter = new LogEventFilter(disruptorLogAndTurboLogEventFilterDelegator);

        final LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setLevel(Level.INFO);
        loggingEvent.setMessage("Message");
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setThreadName(Thread.currentThread().getName());
        loggingEvent.setThrowableProxy(new ThrowableProxy(new AppbaseException()));
        loggingEvent.setCallerData(Thread.currentThread().getStackTrace());
        loggingEvent.setMDCPropertyMap(Map.of("K", "V"));
        loggingEvent.addMarker(MarkerFactory.getMarker("MARKER"));
        loggingEvent.setLoggerName(getClass().getName());
        loggingEvent.setArgumentArray(new Object[] {"A", "B", "C"});

        final FilterReply filterReply = logEventFilter.decide(loggingEvent);
        Assertions.assertNotNull(filterReply);
        Assertions.assertEquals(FilterReply.NEUTRAL, filterReply);
    }
}
