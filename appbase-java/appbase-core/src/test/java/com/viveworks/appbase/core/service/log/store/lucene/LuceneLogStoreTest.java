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

package com.viveworks.appbase.core.service.log.store.lucene;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.criteria.LogLevelCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.ExactTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.RangeTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.SetTimestampCriteria;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class LuceneLogStoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static LuceneLogIndex luceneLogIndex;
    private static LuceneLogStore luceneLogStore;

    @BeforeAll
    static void setUp() throws Exception {
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
        luceneLogIndex = CoreTestFixtures.getDefaultLuceneLogIndex(
                logService,
                metricService,
                validationService,
                mediatorService,
                luceneService,
                DefaultLuceneLogIndexConfiguration.builder().build());
        luceneLogStore = new LuceneLogStore(
                logService,
                metricService,
                luceneLogIndex,
                mediatorService.getMediator().getSharedExecutorService(),
                LuceneLogStoreConfiguration.builder().enabled(true).build());
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (luceneLogStore != null) {
            luceneLogStore.shutdown();
        }

        if (luceneLogIndex != null) {
            luceneLogIndex.getLuceneService().shutdown();
        }

        if (luceneService != null) {
            luceneService.shutdown();
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
    void testLogStore() throws Exception {
        final LogRecord logRecord1 = CoreTestFixtures.getLogRecord1();
        luceneLogStore.writeLogRecord(logRecord1);

        final LogRecord logRecord2 = CoreTestFixtures.getLogRecord2();
        luceneLogStore.asyncWriteLogRecord(logRecord2).get();


        final LuceneLogIndex containedLuceneLogIndex = luceneLogStore.getLuceneLogIndex();
        Assertions.assertNotNull(containedLuceneLogIndex);
        luceneLogStore.synchronize();

        runReadLogRecordsByCriteriaTests("Formatted Message1: ARG");
        runReadLogRecordsByCriteriaTests(logRecord1.getTimestamp());
        runReadLogRecordsByCriteriaTests("Formatted Message2: ARG");
        runReadLogRecordsByCriteriaTests(logRecord2.getTimestamp());
        runReadLogRecordsByCriteriaTests("Formatted Message1: {}");
        runReadLogRecordsByCriteriaTests("Formatted Message2: {}");

        final Collection<LogRecord> readLogRecordsAt0 =
                luceneLogStore.readLogRecordsAt(logRecord1.getTimestamp(), 10);
        Assertions.assertNotNull(readLogRecordsAt0);
        Assertions.assertEquals(1, readLogRecordsAt0.size());
        final LogRecord readLogRecordsAtOne0 = readLogRecordsAt0.stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsAtOne0);
        Assertions.assertEquals(logRecord1.getTimestamp(), readLogRecordsAtOne0.getTimestamp());

        final Collection<LogRecord> readLogRecordsAt =
                luceneLogStore
                        .asyncReadLogRecordsAt(logRecord1.getTimestamp(), 10)
                        .get();
        Assertions.assertNotNull(readLogRecordsAt);
        Assertions.assertEquals(1, readLogRecordsAt.size());
        final LogRecord readLogRecordsAtOne = readLogRecordsAt.stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsAtOne);
        Assertions.assertEquals(logRecord1.getTimestamp(), readLogRecordsAtOne.getTimestamp());


        final Collection<LogRecord> readLogRecordsBetween0 =
                luceneLogStore.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetween0);
        Assertions.assertEquals(2, readLogRecordsBetween0.size());


        final Collection<LogRecord> readLogRecordsBetween1 =
                luceneLogStore
                        .asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10)
                        .get();
        Assertions.assertNotNull(readLogRecordsBetween1);
        Assertions.assertEquals(2, readLogRecordsBetween1.size());


        luceneLogStore.removeLogRecordsAt(logRecord1.getTimestamp());
        luceneLogStore.asyncRemoveLogRecordsAt(logRecord1.getTimestamp());
        luceneLogStore.synchronize();
        final Collection<LogRecord> readLogRecordsBetween2 =
                luceneLogStore
                        .asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10)
                        .get();
        Assertions.assertNotNull(readLogRecordsBetween2);
        Assertions.assertTrue(readLogRecordsBetween2.size() > 0);


        final Collection<LogRecord> readRecordsBetween2 = luceneLogStore
                .asyncReadLogRecordsBetween(Long.MIN_VALUE, logRecord1.getTimestamp() - 100, 10)
                .get();
        Assertions.assertNotNull(readRecordsBetween2);
        Assertions.assertTrue(readRecordsBetween2.isEmpty());


        final LuceneLogStoreConfiguration luceneLogStoreConfiguration = luceneLogStore.getLuceneLogStoreConfiguration();
        Assertions.assertNotNull(luceneLogStoreConfiguration);

        final LogRecord logRecord3 = CoreTestFixtures.getLogRecord3();
        luceneLogStore.asyncWriteLogRecord(logRecord3).get();

        final LogRecord logRecord4 = CoreTestFixtures.getLogRecord4();
        luceneLogStore.asyncWriteLogRecord(logRecord4).get();
        luceneLogStore.synchronize();

        final Collection<LogRecord> readLogRecordsBetween3 =
                luceneLogStore.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10).get();
        Assertions.assertEquals(3, readLogRecordsBetween3.size());


        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .message("Another")
                .build();
        luceneLogStore.removeLogRecordsByCriteria(logRecordCriteria);
        luceneLogStore.synchronize();

        final Collection<LogRecord> readLogRecordsBetween4 =
                luceneLogStore.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10).get();
        Assertions.assertEquals(2, readLogRecordsBetween4.size());


        final LogRecordCriteria logRecordCriteria1 = LogRecordCriteria
                .builder()
                .timestampCriteria(new SetTimestampCriteria(Set.of(logRecord2.getTimestamp())))
                .build();
        luceneLogStore.removeLogRecordsByCriteria(logRecordCriteria1);
        luceneLogStore.synchronize();

        final Collection<LogRecord> readLogRecordsBetween5 =
                luceneLogStore.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10).get();
        Assertions.assertEquals(1, readLogRecordsBetween5.size());


        final LogRecordCriteria logRecordCriteria2 = LogRecordCriteria
                .builder()
                .timestampCriteria(new RangeTimestampCriteria(logRecord1.getTimestamp(), Long.MAX_VALUE))
                .build();
        luceneLogStore.asyncRemoveLogRecordsByCriteria(logRecordCriteria2).get();
        luceneLogStore.synchronize();

        final Collection<LogRecord> readLogRecordsBetween6 =
                luceneLogStore.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10).get();
        Assertions.assertTrue(readLogRecordsBetween6.isEmpty());


        final long timestamp5 = System.currentTimeMillis();
        final LogRecord logRecord5 = CoreTestFixtures.getLogRecord(
                "Msg fmt 5: {}",
                "Msg fmt 5: ARG",
                "Throwable message5",
                "Cause Message5",
                "Key5",
                "Value5",
                "MARKER5",
                timestamp5,
                CoreTestFixtures.class.getName(),
                LogLevel.TRACE);
        luceneLogStore.asyncWriteLogRecord(logRecord5).get();

        final long timestamp6 = System.currentTimeMillis();
        final LogRecord logRecord6 = CoreTestFixtures.getLogRecord(
                "Msg fmt 6: {}",
                "Msg fmt 6: ARG",
                "Throwable message6",
                "Cause Message6",
                "Key6",
                "Value6",
                "MARKER6",
                timestamp6,
                CoreTestFixtures.class.getName(),
                LogLevel.ERROR);
        luceneLogStore.asyncWriteLogRecord(logRecord6).get();
        luceneLogStore.synchronize();

        runReadLogRecordsByCriteriaTests(LogLevel.OFF, LogLevelCriteria.Match.GREATER_OR_EQUAL, 0);
        runReadLogRecordsByCriteriaTests(LogLevel.ERROR, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.WARN, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.INFO, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.DEBUG, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.TRACE, LogLevelCriteria.Match.GREATER_OR_EQUAL, 2);
        runReadLogRecordsByCriteriaTests(LogLevel.TRACE, LogLevelCriteria.Match.EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.ALL, LogLevelCriteria.Match.GREATER_OR_EQUAL, 2);
        luceneLogStore.removeLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE);
        luceneLogStore.asyncRemoveLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE);
        luceneLogStore.synchronize();

        final Collection<LogRecord> readLogRecordsBetween7 =
                luceneLogStore.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10).get();
        Assertions.assertTrue(readLogRecordsBetween7.isEmpty());
    }

    @Test
    void readLogRecordsHonoursCancellationBetweenDocuments() throws Exception {
        // Isolated fixture (own temp index dir — see DefaultLuceneServiceConfiguration.Builder's per-instance
        // UUID default) so this test's document count is not entangled with testLogStore()'s shared state;
        // JUnit5 does not guarantee method execution order.
        final DefaultLuceneService isolatedLuceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                DefaultLuceneServiceConfiguration.builder().build(),
                CoreTestFixtures.getReactiveReadBoundary());
        final LuceneLogIndex isolatedLuceneLogIndex = CoreTestFixtures.getDefaultLuceneLogIndex(
                logService,
                metricService,
                validationService,
                mediatorService,
                isolatedLuceneService,
                DefaultLuceneLogIndexConfiguration.builder().build());
        final LuceneLogStore store = new LuceneLogStore(
                logService,
                metricService,
                isolatedLuceneLogIndex,
                mediatorService.getMediator().getSharedExecutorService(),
                LuceneLogStoreConfiguration.builder().enabled(true).build());
        try {
            for (int i = 0; i < 5; i++) {
                store.writeLogRecord(CoreTestFixtures.getLogRecord(
                        "Cancellation fmt " + i + ": {}",
                        "Cancellation fmt " + i + ": ARG",
                        "Throwable message" + i,
                        "Cause Message" + i,
                        "Key" + i,
                        "Value" + i,
                        "MARKER" + i,
                        System.currentTimeMillis() + i,
                        CoreTestFixtures.class.getName(),
                        LogLevel.INFO));
            }
            store.synchronize();

            final AtomicInteger calls = new AtomicInteger();
            // cancel once two documents have been consumed
            final BooleanSupplier cancelAfterTwo = () -> calls.getAndIncrement() >= 2;

            final AtomicInteger scoreDocCount = new AtomicInteger();
            final Collection<LogRecord> got = store.getLuceneLogIndex().getLuceneService().withSearcher(searcher -> {
                final TopDocs all = searcher.search(new MatchAllDocsQuery(), 5);
                Assertions.assertEquals(5, all.scoreDocs.length, "expected exactly 5 indexed documents");
                scoreDocCount.set(all.scoreDocs.length);
                return store.readLogRecords(all, searcher.getIndexReader(), cancelAfterTwo);
            });

            Assertions.assertTrue(got.size() < 5, "a cancelled fetch must stop early, not read every document");
            // cancelAfterTwo is monotonic (calls only increases), so a loop that merely SKIPS a fetch while
            // still scanning every scoreDoc (e.g. `continue` instead of `break`) would land on the same
            // record count as one that truly stops — this call-count check is what tells the two apart.
            Assertions.assertTrue(calls.get() < scoreDocCount.get(),
                    "a cancelled fetch must stop consulting `cancelled` once cancelled, not scan every document");
        } finally {
            store.shutdown();
            isolatedLuceneService.shutdown();
        }
    }

    private void runReadLogRecordsByCriteriaTests(final String message) throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .message(message)
                .build();
        final Collection<LogRecord> readLogRecordsByCriteria =
                luceneLogStore.readLogRecordsByCriteria(logRecordCriteria, 10);
        Assertions.assertNotNull(readLogRecordsByCriteria);
        Assertions.assertFalse(readLogRecordsByCriteria.isEmpty());
    }

    private void runReadLogRecordsByCriteriaTests(final long timestamp) throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .timestampCriteria(new ExactTimestampCriteria(timestamp))
                .build();
        final Collection<LogRecord> readLogRecordsByCriteria =
                luceneLogStore.readLogRecordsByCriteria(logRecordCriteria, 10);
        Assertions.assertNotNull(readLogRecordsByCriteria);
        Assertions.assertFalse(readLogRecordsByCriteria.isEmpty());
        Assertions.assertEquals(1, readLogRecordsByCriteria.size());
    }

    private void runReadLogRecordsByCriteriaTests(final LogLevel logLevel,
                                                  final LogLevelCriteria.Match match,
                                                  final int size) throws Exception
    {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .logLevelCriteria(new LogLevelCriteria(logLevel, match))
                .build();
        final Collection<LogRecord> logRecords =
                luceneLogStore.asyncReadLogRecordsByCriteria(logRecordCriteria, 10).get();
        Assertions.assertNotNull(logRecords);
        Assertions.assertEquals(size, logRecords.size());
    }
}
