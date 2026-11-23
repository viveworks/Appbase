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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.log.LogService;
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
 */
class MetricsTurboLogEventFilterTest {
    private static LogService logService;
    private static MetricService metricService;
    private static MetricsTurboLogEventFilter metricsTurboLogEventFilter;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (metricsTurboLogEventFilter != null) {
            metricsTurboLogEventFilter.stop();
        }

        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }
    }

    @Test
    void testNeutral() {
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
                        .allowedLoggerNames(Set.of(MetricsTurboLogEventFilterTest.class.getName()))
                        .build();
        final MetricsTurboLogEventFilterHandler metricsTurboLogEventFilterHandler =
                new MetricsTurboLogEventFilterHandler(metricService, metricsTurboFilterHandlerConfiguration);
        metricsTurboLogEventFilter = new MetricsTurboLogEventFilter(metricsTurboLogEventFilterHandler);
        Assertions.assertNotNull(metricsTurboLogEventFilter.getTurboFilterHandler());
        Assertions.assertTrue(metricsTurboLogEventFilter.isStarted());
        Assertions.assertEquals(MetricsTurboLogEventFilter.class.getName(), metricsTurboLogEventFilter.getName());

        final FilterReply filterReply = metricsTurboLogEventFilter.decide(
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
