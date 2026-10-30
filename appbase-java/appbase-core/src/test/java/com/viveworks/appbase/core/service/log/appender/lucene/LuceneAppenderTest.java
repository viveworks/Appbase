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

package com.viveworks.appbase.core.service.log.appender.lucene;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndex;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexTest;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class LuceneAppenderTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static DefaultLuceneLogIndexConfiguration defaultLuceneLogIndexConfiguration;
    private static DefaultLuceneLogIndex defaultLuceneLogIndex;
    private static LuceneAppender luceneAppender;

    @BeforeAll
    static void setUp() {
        defaultLuceneLogIndexConfiguration =
                DefaultLuceneLogIndexConfiguration
                        .builder()
                        .enabled(true)
                        .mdcIndexEnabled(true)
                        .throwableStackTraceIndexEnabled(true)
                        .throwableCauseStackTraceIndexEnabled(true)
                        .markerIndexEnabled(true)
                        .callerStackIndexEnabled(true)
                        .build();
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                DefaultLuceneServiceConfiguration.builder().build(),
                CoreTestFixtures.getReactiveReadBoundary());
        defaultLuceneLogIndex = CoreTestFixtures.getDefaultLuceneLogIndex(
                logService,
                metricService,
                validationService,
                mediatorService,
                luceneService,
                defaultLuceneLogIndexConfiguration);
        luceneAppender = new LuceneAppender(defaultLuceneLogIndex);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (luceneService != null) {
            luceneService.shutdown();
        }

        defaultLuceneLogIndex.getLuceneService().shutdown();
        luceneAppender.stop();
    }

    @Test
    void testAppend() throws Exception {
        Assertions.assertTrue(luceneAppender.isStarted());
        Assertions.assertFalse(AppbaseUtil.isBlank(luceneAppender.getName()));

        final LoggingEvent loggingEvent = new LoggingEvent();
        final long timestamp = System.currentTimeMillis();
        final String throwableMessage = "Throwable message 1";
        final String causeMessage = "Cause message 1";
        final String message = "Message";
        final String threadName = Thread.currentThread().getName();
        final Level level = Level.INFO;
        final Throwable throwable = new AppbaseException(
                throwableMessage, new AppbaseRuntimeException(causeMessage));
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StringBuilder stackTraceBuilder = new StringBuilder();
        for (final StackTraceElement stackTraceElement : stackTrace) {
            stackTraceBuilder.append(stackTraceElement.toString());
        }
        final String stackTraceString = stackTraceBuilder.toString();
        final String mdcKey = "Key";
        final String mdcValue = "Value";
        final Map<String, String> mdcMap = Map.of(mdcKey, mdcValue);
        final String markerName = "MARKER";
        final String loggerName = getClass().getName();

        loggingEvent.setLevel(level);
        loggingEvent.setMessage(message);
        loggingEvent.setTimeStamp(timestamp);
        loggingEvent.setThreadName(threadName);
        loggingEvent.setThrowableProxy(new ThrowableProxy(throwable));
        loggingEvent.setCallerData(stackTrace);
        loggingEvent.setMDCPropertyMap(mdcMap);
        loggingEvent.addMarker(MarkerFactory.getMarker(markerName));
        loggingEvent.setLoggerName(loggerName);
        loggingEvent.setArgumentArray(new Object[] {"A", "B", "C"});

        luceneAppender.append(loggingEvent);
        defaultLuceneLogIndex.getLuceneService().synchronizeIndex();

        final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                defaultLuceneLogIndex.getLuceneLogIndexConfiguration();
        final Query messageQuery = luceneService
                .getQueryBuilder()
                .createBooleanQuery(luceneLogIndexConfiguration.getMessageFieldName(), message);

        DefaultLuceneLogIndexTest.assertFields(
                luceneService,
                defaultLuceneLogIndex.getLuceneLogIndexConfiguration(),
                LogLevel.from(level).name(),
                message,
                timestamp,
                threadName,
                loggerName,
                markerName,
                throwableMessage,
                throwable.getClass().getName(),
                stackTraceString,
                mdcValue,
                causeMessage,
                throwable.getCause().getClass().getName(),
                message,
                1,
                messageQuery);
    }
}
