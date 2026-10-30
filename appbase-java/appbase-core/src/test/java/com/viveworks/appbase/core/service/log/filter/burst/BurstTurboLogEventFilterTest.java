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

package com.viveworks.appbase.core.service.log.filter.burst;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultDuplicateLogHandler;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class BurstTurboLogEventFilterTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultDuplicateLogHandler defaultDuplicateLogHandler;
    private static BurstTurboLogEventFilterHandler burstTurboLogEventFilterHandler1;
    private static BurstTurboLogEventFilter burstTurboLogEventFilter1;
    private static BurstTurboLogEventFilterHandler burstTurboLogEventFilterHandler2;
    private static BurstTurboLogEventFilter burstTurboLogEventFilter2;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration =
                BurstTurboLogEventFilterHandlerConfiguration.builder().enabled(true).build();
        defaultDuplicateLogHandler = new DefaultDuplicateLogHandler(
                burstTurboLogEventFilterHandlerConfiguration.getDefaultDuplicateLogHandlerConfiguration());
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (burstTurboLogEventFilterHandler1 != null) {
            burstTurboLogEventFilterHandler1.shutdown();
        }

        if (burstTurboLogEventFilter1 != null) {
            burstTurboLogEventFilter1.stop();
        }

        if (burstTurboLogEventFilterHandler2 != null) {
            burstTurboLogEventFilterHandler2.shutdown();
        }

        if (burstTurboLogEventFilter2 != null) {
            burstTurboLogEventFilter2.stop();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testDeny() {
        burstTurboLogEventFilterHandler1 = new BurstTurboLogEventFilterHandler(
                1000.0,
                new BurstTurboLogEventFilterHandlerTest.TestLogRateCalculator1(),
                new BurstTurboLogEventFilterHandlerTest.TestLogDeduplicator(),
                defaultDuplicateLogHandler);
        burstTurboLogEventFilter1 = new BurstTurboLogEventFilter(burstTurboLogEventFilterHandler1);
        Assertions.assertNotNull(burstTurboLogEventFilter1.getTurboFilterHandler());
        Assertions.assertTrue(burstTurboLogEventFilter1.isStarted());
        Assertions.assertEquals(BurstTurboLogEventFilter.class.getName(), burstTurboLogEventFilter1.getName());

        final FilterReply filterReply = burstTurboLogEventFilter1.decide(
                MarkerFactory.getMarker("MARKER1"),
                (Logger) LoggerFactory.getLogger(getClass()),
                Level.ERROR,
                "Formatted Message 1: {}",
                new Object[]{"Arg1"},
                null);
        Assertions.assertNotNull(filterReply);
        Assertions.assertEquals(FilterReply.DENY, filterReply);
    }

    @Test
    void testNeutral() {
        burstTurboLogEventFilterHandler2 = new BurstTurboLogEventFilterHandler(
                1000.0,
                new BurstTurboLogEventFilterHandlerTest.TestLogRateCalculator2(),
                new BurstTurboLogEventFilterHandlerTest.TestLogDeduplicator(),
                defaultDuplicateLogHandler);
        burstTurboLogEventFilter2 = new BurstTurboLogEventFilter(burstTurboLogEventFilterHandler2);
        Assertions.assertNotNull(burstTurboLogEventFilter2.getTurboFilterHandler());
        Assertions.assertTrue(burstTurboLogEventFilter2.isStarted());
        Assertions.assertEquals(BurstTurboLogEventFilter.class.getName(), burstTurboLogEventFilter2.getName());

        final FilterReply filterReply = burstTurboLogEventFilter2.decide(
                MarkerFactory.getMarker("MARKER1"),
                (Logger) LoggerFactory.getLogger(getClass()),
                Level.ERROR,
                "Formatted Message 1: {}",
                new Object[]{"Arg1"},
                null);
        Assertions.assertNotNull(filterReply);
        Assertions.assertEquals(FilterReply.NEUTRAL, filterReply);
    }
}
