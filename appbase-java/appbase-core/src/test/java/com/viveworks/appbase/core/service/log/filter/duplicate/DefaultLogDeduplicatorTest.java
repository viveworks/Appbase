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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.burst.LogEntry;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLogDeduplicatorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static AtomicBoolean bursting;
    private static DefaultDuplicateLogHandler defaultDuplicateLogHandler;
    private static DefaultLogDeduplicator defaultLogDeduplicator;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);

        bursting = new AtomicBoolean(false);
        final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration =
                DefaultDuplicateLogHandlerConfiguration.builder().build();
        defaultDuplicateLogHandler = new DefaultDuplicateLogHandler(defaultDuplicateLogHandlerConfiguration);

        final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration =
                DefaultLogDeduplicatorConfiguration
                        .builder()
                        .duplicateLogHandlerSchedulableConfiguration(
                                SchedulableConfiguration
                                        .builder()
                                        .initialDelay(1L)
                                        .delay(1L)
                                        .timeUnit(TimeUnit.DAYS)
                                        .build())
                        .build();
        defaultLogDeduplicator = new DefaultLogDeduplicator(
                defaultLogDeduplicatorConfiguration,
                mediatorService,
                defaultDuplicateLogHandler,
                bursting);
    }

    @AfterAll
    static void tearDown() throws Exception {
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

        if (defaultLogDeduplicator != null) {
            defaultLogDeduplicator.shutdown();
        }
    }

    @Test
    void testDeduplicator() {
        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .timestamp(System.currentTimeMillis())
                .build();
        defaultLogDeduplicator.duplicate(turboLog1);
        final Map<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
                logEntryByTimestampByFormatByLoggerNameMap =
                defaultLogDeduplicator.getLogEntryByTimestampByFormatByLoggerNameMap();
        Assertions.assertNotNull(logEntryByTimestampByFormatByLoggerNameMap);
        Assertions.assertEquals(1, logEntryByTimestampByFormatByLoggerNameMap.size());

        final AtomicBoolean ranAsserts = new AtomicBoolean(false);
        logEntryByTimestampByFormatByLoggerNameMap.forEach((loggerName, formatMap) -> {
            formatMap.forEach((format, logEntryMap) -> {
                final Collection<LogEntry> values = logEntryMap.values();
                for (final LogEntry value : values) {
                    Assertions.assertEquals(turboLog1.getLogger().getName(), loggerName);
                    Assertions.assertEquals(turboLog1.getFormattedMessage(), format);
                    Assertions.assertEquals(turboLog1.getLogLevel(), value.getLogLevel());
                    Assertions.assertArrayEquals(turboLog1.getArgumentArray(), value.getParams());
                    Assertions.assertEquals(turboLog1.getMarker().getName(), value.getMarker().getName());
                    Assertions.assertEquals(turboLog1.getThrowable(), value.getThrowable());
                    ranAsserts.set(true);
                    break;
                }
            });
        });
        Assertions.assertTrue(ranAsserts.get());

        final TurboLog turboLog2 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .throwable(new AppbaseRuntimeException())
                .timestamp(System.currentTimeMillis())
                .build();
        defaultLogDeduplicator.duplicate(turboLog2);
        final Map<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
                logEntryByTimestampByThrowableTypeNameByLoggerNameMap =
                defaultLogDeduplicator.getLogEntryByTimestampByThrowableTypeNameByLoggerNameMap();
        Assertions.assertNotNull(logEntryByTimestampByThrowableTypeNameByLoggerNameMap);
        Assertions.assertEquals(1, logEntryByTimestampByThrowableTypeNameByLoggerNameMap.size());
        Assertions.assertEquals(1, logEntryByTimestampByFormatByLoggerNameMap.size());

        ranAsserts.set(false);
        logEntryByTimestampByThrowableTypeNameByLoggerNameMap.forEach((loggerName, throwableTypeNameMap) -> {
            throwableTypeNameMap.forEach((throwableTypeName, logEntryMap) -> {
                final Collection<LogEntry> values = logEntryMap.values();
                for (final LogEntry value : values) {
                    Assertions.assertEquals(turboLog2.getLogger().getName(), loggerName);
                    Assertions.assertEquals(turboLog2.getFormattedMessage(), value.getFormattedMessage());
                    Assertions.assertEquals(turboLog2.getLogLevel(), value.getLogLevel());
                    Assertions.assertArrayEquals(turboLog2.getArgumentArray(), value.getParams());
                    Assertions.assertEquals(turboLog2.getMarker().getName(), value.getMarker().getName());
                    Assertions.assertEquals(turboLog2.getThrowable(), value.getThrowable());
                    ranAsserts.set(true);
                    break;
                }
            });
        });
        Assertions.assertTrue(ranAsserts.get());

        final Throwable throwable1 = new AppbaseException(
                "Throwable message 1",
                new AppbaseRuntimeException("Cause message 1"));
        final TurboLog turboLog3 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message 1: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .throwable(throwable1)
                .timestamp(System.currentTimeMillis())
                .build();
        defaultLogDeduplicator.duplicate(turboLog3);
        final Map<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>>
                logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap =
                defaultLogDeduplicator.getLogEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap();
        Assertions.assertNotNull(logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap);
        Assertions.assertEquals(1, logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap.size());
        Assertions.assertEquals(1, logEntryByTimestampByThrowableTypeNameByLoggerNameMap.size());
        Assertions.assertEquals(1, logEntryByTimestampByFormatByLoggerNameMap.size());

        ranAsserts.set(false);
        logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap.forEach((loggerName, typeNameMap) -> {
            typeNameMap.forEach((typeName, throwableMessageMap) -> {
                throwableMessageMap.forEach((throwableMessage, logEntryMap) -> {
                    final Collection<LogEntry> values = logEntryMap.values();
                    for (final LogEntry value : values) {
                        Assertions.assertEquals(turboLog3.getLogger().getName(), loggerName);
                        Assertions.assertEquals(turboLog3.getFormattedMessage(), value.getFormattedMessage());
                        Assertions.assertEquals(turboLog3.getLogLevel(), value.getLogLevel());
                        Assertions.assertArrayEquals(turboLog3.getArgumentArray(), value.getParams());
                        Assertions.assertEquals(turboLog3.getMarker().getName(), value.getMarker().getName());
                        Assertions.assertEquals(turboLog3.getThrowable(), value.getThrowable());
                        ranAsserts.set(true);
                        break;
                    }
                });
            });
        });
        Assertions.assertTrue(ranAsserts.get());

        Assertions.assertFalse(defaultLogDeduplicator.isBursting());
        final DefaultLogDeduplicator.DuplicateLogHandlerSchedulable duplicateLogHandlerSchedulable =
                defaultLogDeduplicator.getDuplicateLogHandlerSchedulable();
        Assertions.assertNotNull(duplicateLogHandlerSchedulable);
        Assertions.assertNotNull(duplicateLogHandlerSchedulable.getSchedulableConfiguration());

        duplicateLogHandlerSchedulable.schedule();
        final Map<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
                logEntryByTimestampByFormatByLoggerNameMap1 =
                defaultLogDeduplicator.getLogEntryByTimestampByFormatByLoggerNameMap();
        Assertions.assertTrue(logEntryByTimestampByFormatByLoggerNameMap1.isEmpty());

        final Map<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>
                logEntryByTimestampByThrowableTypeNameByLoggerNameMap1 =
                defaultLogDeduplicator.getLogEntryByTimestampByThrowableTypeNameByLoggerNameMap();
        Assertions.assertTrue(logEntryByTimestampByThrowableTypeNameByLoggerNameMap1.isEmpty());

        final Map<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Long, LogEntry>>>>
                logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap1 =
                defaultLogDeduplicator.getLogEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap();
        Assertions.assertTrue(logEntryByTimestampByThrowableMessageByTypeNameByLoggerNameMap1.isEmpty());
    }
}
