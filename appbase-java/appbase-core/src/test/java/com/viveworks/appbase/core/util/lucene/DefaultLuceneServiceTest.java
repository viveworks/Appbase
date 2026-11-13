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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLuceneServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (luceneService != null) {
            luceneService.shutdown();
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
    void testLuceneService() throws Exception {
        try {
            final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration1 =
                    DefaultLuceneServiceConfiguration
                            .builder()
                            .defaultIndexCleanerConfiguration(
                                    TimestampBasedIndexCleanerConfiguration
                                            .builder()
                                            .enabled(false)
                                            .build())
                            .standardAnalyzerStopWords(List.of("zzaacc"))
                            .build();
            luceneService = new DefaultLuceneService(
                    logService,
                    metricService,
                    validationService,
                    mediatorService,
                    defaultLuceneServiceConfiguration1,
                    CoreTestFixtures.getReactiveReadBoundary());
            Assertions.assertNull(luceneService.getIndexCleaner());
            runTests();
        } finally {
            if (luceneService != null) {
                luceneService.shutdown();
            }
        }

        try {
            final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration2 =
                    DefaultLuceneServiceConfiguration
                            .builder()
                            .directoryType(DirectoryType.MMAP)
                            .defaultIndexCleanerConfiguration(
                                    TimestampBasedIndexCleanerConfiguration
                                            .builder()
                                            .enabled(false)
                                            .build())
                            .build();
            luceneService = new DefaultLuceneService(
                    logService,
                    metricService,
                    validationService,
                    mediatorService,
                    defaultLuceneServiceConfiguration2,
                    CoreTestFixtures.getReactiveReadBoundary());
            Assertions.assertNull(luceneService.getIndexCleaner());
            runTests();
        } finally {
            if (luceneService != null) {
                luceneService.shutdown();
            }
        }

        try {
            final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration3 =
                    DefaultLuceneServiceConfiguration
                            .builder()
                            .directoryType(DirectoryType.ANY)
                            .defaultIndexCleanerConfiguration(
                                    TimestampBasedIndexCleanerConfiguration
                                            .builder()
                                            .enabled(true)
                                            .build())
                            .standardAnalyzerStopWords(List.of("zzaacc"))
                            .build();
            luceneService = new DefaultLuceneService(
                    logService,
                    metricService,
                    validationService,
                    mediatorService,
                    defaultLuceneServiceConfiguration3,
                    CoreTestFixtures.getReactiveReadBoundary());
            final DefaultLuceneService.IndexCleanerSchedulable indexCleanerSchedulable =
                    luceneService.getIndexCleanerSchedulable();
            Assertions.assertNotNull(luceneService.getIndexCleaner());
            Assertions.assertNotNull(indexCleanerSchedulable);
            Assertions.assertNotNull(indexCleanerSchedulable.getSchedulableConfiguration());
            runTests();
        } finally {
            if (luceneService != null) {
                luceneService.shutdown();
            }
        }
    }

    void runTests() throws Exception {
        Assertions.assertNotNull(luceneService.getIndexPath());
        Assertions.assertNotNull(luceneService.getDirectory());
        Assertions.assertNotNull(luceneService.getIndexWriter());
        Assertions.assertNotNull(luceneService.getIndexWriter(true));
        Assertions.assertNotNull(luceneService.withSearcher(searcher -> searcher));
        Assertions.assertNotNull(luceneService.withSearcher(searcher -> searcher.getIndexReader()));
        Assertions.assertNotNull(luceneService.getQueryBuilder());
        Assertions.assertNotNull(luceneService.getQueryParser("fieldName:Value"));
    }
}
