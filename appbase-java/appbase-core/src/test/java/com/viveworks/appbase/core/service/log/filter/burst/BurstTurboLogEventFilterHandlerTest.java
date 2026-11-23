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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultDuplicateLogHandler;
import com.viveworks.appbase.core.service.log.filter.duplicate.LogDeduplicator;
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
public class BurstTurboLogEventFilterHandlerTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultDuplicateLogHandler defaultDuplicateLogHandler;
    private static BurstTurboLogEventFilterHandler burstTurboLogEventFilterHandler1;
    private static BurstTurboLogEventFilterHandler burstTurboLogEventFilterHandler2;
    private static BurstTurboLogEventFilterHandler burstTurboLogEventFilterHandler3;

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

        if (burstTurboLogEventFilterHandler2 != null) {
            burstTurboLogEventFilterHandler2.shutdown();
        }

        if (burstTurboLogEventFilterHandler3 != null) {
            burstTurboLogEventFilterHandler3.shutdown();
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
    void testConstructorWithConfiguration() {
        final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration =
                BurstTurboLogEventFilterHandlerConfiguration.builder().enabled(true).build();
        burstTurboLogEventFilterHandler1 =
                new BurstTurboLogEventFilterHandler(
                        burstTurboLogEventFilterHandlerConfiguration,
                        mediatorService);
        Assertions.assertTrue(burstTurboLogEventFilterHandler1.isEnabled());
        Assertions.assertNotNull(burstTurboLogEventFilterHandler1.getDuplicateLogHandler());
        Assertions.assertNotNull(burstTurboLogEventFilterHandler1.getLogDeduplicator());
        burstTurboLogEventFilterHandler1.shutdown();
    }

    @Test
    void testBursting() {
        burstTurboLogEventFilterHandler2 = new BurstTurboLogEventFilterHandler(
                1000.0,
                new TestLogRateCalculator1(),
                new TestLogDeduplicator(),
                defaultDuplicateLogHandler);
        Assertions.assertTrue(burstTurboLogEventFilterHandler2.isEnabled());
        Assertions.assertNotNull(burstTurboLogEventFilterHandler2.getDuplicateLogHandler());
        Assertions.assertNotNull(burstTurboLogEventFilterHandler2.getLogDeduplicator());
        Assertions.assertEquals(1000.0, burstTurboLogEventFilterHandler2.getBurstRateThreshold());
        Assertions.assertFalse(burstTurboLogEventFilterHandler2.isBursting());

        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .timestamp(System.currentTimeMillis())
                .build();
        final boolean accept = burstTurboLogEventFilterHandler2.accept(turboLog1);
        Assertions.assertTrue(burstTurboLogEventFilterHandler2.isBursting());
        Assertions.assertFalse(accept);
    }

    @Test
    void testNotBursting() {
        burstTurboLogEventFilterHandler3 = new BurstTurboLogEventFilterHandler(
                1000.0,
                new TestLogRateCalculator2(),
                new TestLogDeduplicator(),
                defaultDuplicateLogHandler);
        Assertions.assertTrue(burstTurboLogEventFilterHandler3.isEnabled());
        Assertions.assertNotNull(burstTurboLogEventFilterHandler3.getDuplicateLogHandler());
        Assertions.assertNotNull(burstTurboLogEventFilterHandler3.getLogDeduplicator());
        Assertions.assertEquals(1000.0, burstTurboLogEventFilterHandler3.getBurstRateThreshold());
        Assertions.assertFalse(burstTurboLogEventFilterHandler3.isBursting());

        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .timestamp(System.currentTimeMillis())
                .build();
        final boolean accept = burstTurboLogEventFilterHandler3.accept(turboLog1);
        Assertions.assertFalse(burstTurboLogEventFilterHandler3.isBursting());
        Assertions.assertTrue(accept);
    }

    public static class TestLogRateCalculator1 implements LogRateCalculator {
        @Override
        public double measure(final TurboLog turboLog) {
            return 10_000.0;
        }
    }

    public static class TestLogRateCalculator2 implements LogRateCalculator {
        @Override
        public double measure(final TurboLog turboLog) {
            return 100.0;
        }
    }

    public static class TestLogDeduplicator implements LogDeduplicator {
        @Override
        public boolean duplicate(final TurboLog turboLog) {
            return true;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public void shutdown() throws AppbaseRuntimeException {
            // no-op
        }
    }
}
