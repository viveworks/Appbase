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

package com.viveworks.appbase.core.service.log.filter.metrics;

import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MetricsTurboLogEventFilterHandlerTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }
    }

    @Test
    void testHandler() {
        final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration =
                MetricsTurboFilterHandlerConfiguration
                .builder()
                .enabled(true)
                .measureThrowableTypeNames(true)
                .measureLogLevels(true)
                .measureLoggerNames(true)
                .measureLogFormats(true)
                .blockedThrowableTypeNames(Set.of())
                .blockedLoggerNames(Set.of())
                .blockedLogFormats(Set.of("Blocked Formatted Message: {}"))
                .allowedThrowableTypeNames(Set.of(AppbaseException.class.getName()))
                .allowedLoggerNames(Set.of(getClass().getName()))
                .build();
        final MetricsTurboLogEventFilterHandler metricsTurboLogEventFilterHandler =
                new MetricsTurboLogEventFilterHandler(metricService, metricsTurboFilterHandlerConfiguration);
        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getMetricService());

        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getCounterByFormattedMessageMap());
        Assertions.assertEquals(0, metricsTurboLogEventFilterHandler.getCounterByFormattedMessageMap().size());

        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getCounterByLoggerNameMap());
        Assertions.assertEquals(0, metricsTurboLogEventFilterHandler.getCounterByLoggerNameMap().size());

        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getCounterByLogLevelMap());
        Assertions.assertEquals(0, metricsTurboLogEventFilterHandler.getCounterByLogLevelMap().size());

        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getCounterByThrowableTypeNameMap());
        Assertions.assertEquals(0, metricsTurboLogEventFilterHandler.getCounterByThrowableTypeNameMap().size());

        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getAllowedFormattedMessages());
        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getAllowedLoggerNames());
        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getAllowedThrowableTypeNames());
        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getBlockedFormattedMessages());
        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getBlockedLoggerNames());
        Assertions.assertNotNull(metricsTurboLogEventFilterHandler.getBlockedThrowableTypeNames());
        Assertions.assertTrue(metricsTurboLogEventFilterHandler.isMeasureFormattedMessages());
        Assertions.assertTrue(metricsTurboLogEventFilterHandler.isMeasureLoggerNames());
        Assertions.assertTrue(metricsTurboLogEventFilterHandler.isMeasureLogLevels());
        Assertions.assertTrue(metricsTurboLogEventFilterHandler.isMeasureThrowableTypeNames());

        final Throwable throwable1 = new AppbaseException(
                "Throwable message 2", new AppbaseRuntimeException("Cause message 2"));
        final TurboLog turboLog1 = TurboLog
                .builder()
                .logLevel(LogLevel.ERROR)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Formatted Message: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .throwable(throwable1)
                .timestamp(System.currentTimeMillis())
                .build();
        metricsTurboLogEventFilterHandler.accept(turboLog1);
        Assertions.assertEquals(1, metricsTurboLogEventFilterHandler.getCounterByFormattedMessageMap().size());
        Assertions.assertEquals(1, metricsTurboLogEventFilterHandler.getCounterByLoggerNameMap().size());
        Assertions.assertEquals(1, metricsTurboLogEventFilterHandler.getCounterByLogLevelMap().size());
        Assertions.assertEquals(1, metricsTurboLogEventFilterHandler.getCounterByThrowableTypeNameMap().size());

        final Throwable throwable2 = new AppbaseException(
                "Throwable message 2", new AppbaseRuntimeException("Cause message 2"));
        final TurboLog turboLog2 = TurboLog
                .builder()
                .logLevel(LogLevel.INFO)
                .logger(LoggerFactory.getLogger(getClass()))
                .formattedMessage("Blocked Formatted Message: {}")
                .argumentArray(new Object[] {"Arg1"})
                .marker(MarkerFactory.getMarker("MARKER1"))
                .throwable(throwable2)
                .timestamp(System.currentTimeMillis())
                .build();
        metricsTurboLogEventFilterHandler.accept(turboLog2);
        Assertions.assertEquals(1, metricsTurboLogEventFilterHandler.getCounterByFormattedMessageMap().size());
        Assertions.assertEquals(2, metricsTurboLogEventFilterHandler.getCounterByLogLevelMap().size());
    }
}
