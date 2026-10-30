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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.burst.LogEntry;
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

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DuplicateTurboLogEventFilterHandlerTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultDuplicateLogHandler defaultDuplicateLogHandler;
    private static DuplicateTurboLogEventFilterHandler duplicateTurboLogEventFilterHandler1;
    private static DuplicateTurboLogEventFilterHandler duplicateTurboLogEventFilterHandler2;

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
        if (duplicateTurboLogEventFilterHandler1 != null) {
            duplicateTurboLogEventFilterHandler1.shutdown();
        }

        if (duplicateTurboLogEventFilterHandler2 != null) {
            duplicateTurboLogEventFilterHandler2.shutdown();
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
    void testConstructor1() {
        final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration =
                DuplicateLogTurboFilterHandlerConfiguration
                        .builder()
                        .enabled(true)
                        .build();
        duplicateTurboLogEventFilterHandler1 = new DuplicateTurboLogEventFilterHandler(
                mediatorService,
                duplicateLogTurboFilterHandlerConfiguration);
        Assertions.assertTrue(duplicateTurboLogEventFilterHandler1.isEnabled());
        duplicateTurboLogEventFilterHandler1.shutdown();
    }

    @Test
    void testHandler() {
        final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration =
                DuplicateLogTurboFilterHandlerConfiguration
                        .builder()
                        .enabled(true)
                        .build();
        duplicateTurboLogEventFilterHandler2 = new DuplicateTurboLogEventFilterHandler(
                mediatorService,
                duplicateLogTurboFilterHandlerConfiguration,
                new TestDuplicateLogHandler(),
                new TestLogDeduplicator1());
        Assertions.assertTrue(duplicateTurboLogEventFilterHandler2.isEnabled());

        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .timestamp(System.currentTimeMillis())
                .build();
        final boolean accept = duplicateTurboLogEventFilterHandler2.accept(turboLog1);
        Assertions.assertFalse(accept);
    }

    public static class TestDuplicateLogHandler implements DuplicateLogHandler {
        @Override
        public void duplicateFormattedMessage(final String loggerName,
                                              final String formattedMessage,
                                              final Map<Long, LogEntry> logEntryByTimestampMap)
        {
            // no-op
        }

        @Override
        public void duplicateThrowableTypeName(final String loggerName,
                                               final String throwableTypeName,
                                               final Map<Long, LogEntry> logEntryByTimestampMap)
        {
            // no-op
        }

        @Override
        public void duplicateThrowableMessage(final String loggerName,
                                              final String throwableTypeName,
                                              final String throwableMessage,
                                              final Map<Long, LogEntry> logEntryByTimestampMap)
        {
            // no-op
        }
    }

    public static class TestLogDeduplicator1 implements LogDeduplicator {
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

    public static class TestLogDeduplicator2 implements LogDeduplicator {
        @Override
        public boolean duplicate(final TurboLog turboLog) {
            return false;
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
