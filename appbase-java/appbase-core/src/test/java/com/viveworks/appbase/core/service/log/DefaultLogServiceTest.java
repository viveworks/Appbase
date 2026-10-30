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

package com.viveworks.appbase.core.service.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppenderConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListenerConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.TimestampBasedIndexCleanerConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLogServiceTest {
    private static DefaultLogServiceConfiguration defaultLogServiceConfiguration;
    private static DefaultLogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;

    @BeforeAll
    static void setUp() {
        defaultLogServiceConfiguration =
                DefaultLogServiceConfiguration
                .builder()
                .duplicateLogTurboFilterHandlerConfiguration(DuplicateLogTurboFilterHandlerConfiguration
                        .builder()
                        .enabled(true)
                        .build())
                .luceneLogAndTurboLogEventFilterListenerConfiguration(
                        LuceneLogAndTurboLogEventFilterListenerConfiguration
                        .builder()
                        .enabled(true)
                        .indexLogEvent(true)
                        .indexTurboLog(true)
                        .build())
                // R3 (internal work item 1073, §10): this test exercises the full Lucene log path (it asserts the appender,
                // index and store are created), so it opts them all in explicitly — they default off now.
                .luceneAppenderConfiguration(LuceneAppenderConfiguration.builder().enabled(true).build())
                .defaultLogAccessorConfiguration(DefaultLogAccessorConfiguration.builder()
                        .enabled(true)
                        .logStoreConfigurations(List.<LogStoreConfiguration>of(
                                LuceneLogStoreConfiguration.builder().enabled(true).build()))
                        .build())
                .luceneLogIndexConfiguration(
                        DefaultLuceneLogIndexConfiguration.builder().enabled(true).build())
                .build();
        logService = new DefaultLogService(
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                defaultLogServiceConfiguration);
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);

        final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(
                                TimestampBasedIndexCleanerConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .standardAnalyzerStopWords(List.of("zzaacdd"))
                        .build();
        luceneService = new DefaultLuceneService(
                logService, metricService, validationService, mediatorService, defaultLuceneServiceConfiguration,
                CoreTestFixtures.getReactiveReadBoundary());
        logService.setMediatorService(mediatorService);
        logService.setMetricService(metricService);
        logService.setLuceneService(luceneService);
        logService.setValidationService(validationService);
    }

    @AfterAll
    static void tearDown() {
        if (luceneService != null) {
            luceneService.shutdown();
        }

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
    }

    @Test
    void testDefaultLogService() throws Exception {
        Assertions.assertNotNull(logService);
        Assertions.assertNotNull(logService.getConfiguration());
        Assertions.assertNotNull(logService.getLogger(getClass()));

        logService.reload(defaultLogServiceConfiguration);
        Assertions.assertNotNull(logService.getConfiguration());
        Assertions.assertNotNull(logService.getLogger(getClass()));

        logService.shutdown();
        Assertions.assertNotNull(logService.getConfiguration());
        Assertions.assertNotNull(logService.getLogger(getClass()));
        Assertions.assertEquals(ServiceStatus.STARTED, logService.getStatus());

        Assertions.assertTrue(logService.isResetResistant());
        logService.initFiltersAndAppenders((LoggerContext) LoggerFactory.getILoggerFactory());
        Assertions.assertNotNull(logService.getBurstTurboLogEventFilter());
        Assertions.assertNotNull(logService.getDuplicateTurboLogEventFilter());
        Assertions.assertNotNull(logService.getLogAndTurboLogEventFilterDelegator());
        Assertions.assertNotNull(logService.getLogAccessor());
        Assertions.assertNotNull(logService.getLogEventFilter());
        Assertions.assertNotNull(logService.getLuceneLogAndTurboLogEventFilterListener());
        Assertions.assertNotNull(logService.getLuceneAppender());
        Assertions.assertNotNull(logService.getLuceneLogIndex());
        Assertions.assertNotNull(logService.getLuceneLogStore());
        Assertions.assertNotNull(logService.getMediatorEventDelegatingAppender());
        Assertions.assertNotNull(logService.getLuceneService());
        Assertions.assertNotNull(logService.getMetricsTurboLogEventFilter());
        Assertions.assertNotNull(logService.getTurboLogEventFilter());
        Assertions.assertNotNull(logService.getMediatorEventDelegatorRegistry());

        Assertions.assertDoesNotThrow(() -> logService.onStart((LoggerContext) LoggerFactory.getILoggerFactory()));
        Assertions.assertDoesNotThrow(() -> logService.onReset((LoggerContext) LoggerFactory.getILoggerFactory()));
        Assertions.assertDoesNotThrow(() -> logService.onStop((LoggerContext) LoggerFactory.getILoggerFactory()));
        Assertions.assertDoesNotThrow(() -> {
            logService.onLevelChange((Logger) LoggerFactory.getLogger(getClass()), Level.TRACE);
        });
    }
}
