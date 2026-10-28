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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
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
class DuplicateTurboLogEventFilterTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultDuplicateLogHandler defaultDuplicateLogHandler;
    private static DuplicateTurboLogEventFilterHandler duplicateTurboLogEventFilterHandler1;
    private static DuplicateTurboLogEventFilter duplicateTurboLogEventFilter1;
    private static DuplicateTurboLogEventFilterHandler duplicateTurboLogEventFilterHandler2;
    private static DuplicateTurboLogEventFilter duplicateTurboLogEventFilter2;

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

        if (duplicateTurboLogEventFilter1 != null) {
            duplicateTurboLogEventFilter1.stop();
        }

        if (duplicateTurboLogEventFilterHandler2 != null) {
            duplicateTurboLogEventFilterHandler2.shutdown();
        }

        if (duplicateTurboLogEventFilter2 != null) {
            duplicateTurboLogEventFilter2.stop();
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
    void testNeutral() {
        final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration =
                DuplicateLogTurboFilterHandlerConfiguration
                        .builder()
                        .enabled(true)
                        .build();
        duplicateTurboLogEventFilterHandler2 = new DuplicateTurboLogEventFilterHandler(
                mediatorService,
                duplicateLogTurboFilterHandlerConfiguration,
                new DuplicateTurboLogEventFilterHandlerTest.TestDuplicateLogHandler(),
                new DuplicateTurboLogEventFilterHandlerTest.TestLogDeduplicator2());
        duplicateTurboLogEventFilter2 = new DuplicateTurboLogEventFilter(duplicateTurboLogEventFilterHandler2);
        Assertions.assertNotNull(duplicateTurboLogEventFilter2.getTurboFilterHandler());
        Assertions.assertTrue(duplicateTurboLogEventFilter2.isStarted());
        Assertions.assertEquals(DuplicateTurboLogEventFilter.class.getName(), duplicateTurboLogEventFilter2.getName());

        final FilterReply filterReply = duplicateTurboLogEventFilter2.decide(
                MarkerFactory.getMarker("MARKER1"),
                (Logger) LoggerFactory.getLogger(getClass()),
                Level.ERROR,
                "Formatted Message 1: {}",
                new Object[]{"Arg1"},
                null);
        Assertions.assertNotNull(filterReply);
        Assertions.assertEquals(FilterReply.NEUTRAL, filterReply);
    }

    @Test
    void testDeny() {
        final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration =
                DuplicateLogTurboFilterHandlerConfiguration
                        .builder()
                        .enabled(true)
                        .build();
        duplicateTurboLogEventFilterHandler1 = new DuplicateTurboLogEventFilterHandler(
                mediatorService,
                duplicateLogTurboFilterHandlerConfiguration,
                new DuplicateTurboLogEventFilterHandlerTest.TestDuplicateLogHandler(),
                new DuplicateTurboLogEventFilterHandlerTest.TestLogDeduplicator1());
        duplicateTurboLogEventFilter1 = new DuplicateTurboLogEventFilter(duplicateTurboLogEventFilterHandler1);
        Assertions.assertNotNull(duplicateTurboLogEventFilter1.getTurboFilterHandler());
        Assertions.assertTrue(duplicateTurboLogEventFilter1.isStarted());
        Assertions.assertEquals(DuplicateTurboLogEventFilter.class.getName(), duplicateTurboLogEventFilter1.getName());

        final FilterReply filterReply = duplicateTurboLogEventFilter1.decide(
                MarkerFactory.getMarker("MARKER1"),
                (Logger) LoggerFactory.getLogger(getClass()),
                Level.ERROR,
                "Formatted Message 1: {}",
                new Object[]{"Arg1"},
                null);
        Assertions.assertNotNull(filterReply);
        Assertions.assertEquals(FilterReply.DENY, filterReply);
    }
}
