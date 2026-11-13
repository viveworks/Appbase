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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class TimestampBasedIndexCleanerTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration;
    private static DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration;
    private static DefaultLuceneService luceneService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);

        timestampBasedIndexCleanerConfiguration =
                TimestampBasedIndexCleanerConfiguration
                        .builder()
                        .removeBeforeTimePeriod(1)
                        .removeBeforeTimePeriodUnit(TimeUnit.MINUTES)
                        .build();
        defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(timestampBasedIndexCleanerConfiguration)
                        .build();

        luceneService = new DefaultLuceneService(
                logService, metricService, validationService, mediatorService, defaultLuceneServiceConfiguration,
                CoreTestFixtures.getReactiveReadBoundary());
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
    void testClean() throws Exception {
        Assertions.assertTrue(luceneService.getIndexCleaner().getDeletedSequenceNumber() >= 0);

        final Date nowDate = new Date(System.currentTimeMillis());
        final TimeUnit removeBeforeTimePeriodUnit =
                timestampBasedIndexCleanerConfiguration.getRemoveBeforeTimePeriodUnit();
        final long removeBeforeTimePeriod = timestampBasedIndexCleanerConfiguration.getRemoveBeforeTimePeriod();
        final long removeBeforeMillis = removeBeforeTimePeriodUnit.toMillis(removeBeforeTimePeriod);
        final long removeBeforeTimestamp = nowDate
                .toInstant()
                .minusMillis(removeBeforeMillis)
                .toEpochMilli();

        final String idFieldName = "ID";
        int id = 0;
        final String creationTimestampFieldName =
                timestampBasedIndexCleanerConfiguration.getCreationTimestampFieldName();
        final Document document1 = new Document();
        final long removeBeforeTimestamp1 = nowDate
                .toInstant()
                .minus(Duration.ofDays(100))
                .toEpochMilli();
        document1.add(new LongPoint(creationTimestampFieldName, removeBeforeTimestamp1));
        ++id;
        document1.add(new TextField(idFieldName, String.valueOf(id), Field.Store.YES));
        luceneService.getIndexWriter().addDocument(document1);

        final Document document2 = new Document();
        final long removeBeforeTimestamp2 = nowDate
                .toInstant()
                .plus(Duration.ofDays(100))
                .toEpochMilli();
        document2.add(new LongPoint(creationTimestampFieldName, removeBeforeTimestamp2));
        ++id;
        document2.add(new TextField(idFieldName, String.valueOf(id), Field.Store.YES));
        luceneService.getIndexWriter().addDocument(document2);
        luceneService.synchronizeIndex();

        final Query rangeQuery = LongPoint.newRangeQuery(
                creationTimestampFieldName, Long.MIN_VALUE, Long.MAX_VALUE);
        final TopDocs topDocs = luceneService.withSearcher(searcher -> searcher.search(rangeQuery, 10));
        final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Assertions.assertNotNull(scoreDocs);
        Assertions.assertTrue(scoreDocs.length > 0);

        luceneService.getIndexCleaner().clean();
        luceneService.synchronizeIndex();

        final Query rangeQuery1 = LongPoint.newRangeQuery(
                creationTimestampFieldName, Long.MIN_VALUE, removeBeforeTimestamp);
        final TopDocs topDocs1 = luceneService.withSearcher(searcher -> searcher.search(rangeQuery1, 10));
        final ScoreDoc[] scoreDocs1 = topDocs1.scoreDocs;
        Assertions.assertNotNull(scoreDocs1);
        Assertions.assertEquals(0, scoreDocs1.length);

        final Query rangeQuery2 = LongPoint.newRangeQuery(
                creationTimestampFieldName, removeBeforeTimestamp, Long.MAX_VALUE);
        final TopDocs topDocs2 = luceneService.withSearcher(searcher -> searcher.search(rangeQuery2, 10));
        final ScoreDoc[] scoreDocs2 = topDocs2.scoreDocs;
        Assertions.assertNotNull(scoreDocs2);
        Assertions.assertEquals(1, scoreDocs2.length);
    }
}
