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

package com.viveworks.appbase.core.service.log.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreException;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.criteria.LogLevelCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.ExactTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.RangeTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.SetTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreFactory;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLogAccessorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static LuceneLogIndex luceneLogIndex;
    private static LuceneLogStore luceneLogStore;
    private static DefaultLogAccessor defaultLogAccessor;

    @BeforeAll
    static void setUp() {
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
        defaultLogAccessor = new DefaultLogAccessor(
                logService,
                metricService,
                DefaultLogAccessorConfiguration.builder().enabled(true).build(),
                luceneLogStore);
    }

    @AfterAll
    static void tearDown() {
        if (defaultLogAccessor != null) {
            defaultLogAccessor.shutdown();
        }

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
    @Order(1)
    void testLogAccessor() throws Exception {
        runTests();

        final LuceneLogStoreFactory luceneLogStoreFactory = new LuceneLogStoreFactory(
                logService,
                metricService,
                luceneLogIndex,
                mediatorService.getMediator().getSharedExecutorService());
        defaultLogAccessor.registerLogStoreFactoryByConfigurationType(
                LuceneLogStoreConfiguration.class,
                luceneLogStoreFactory);
        defaultLogAccessor.reload(DefaultLogAccessorConfiguration.builder().build());
        runTests();
    }

    private void runTests() throws Exception {
        final LogRecord logRecord1 = CoreTestFixtures.getLogRecord1();
        final Collection<String> writeLogRecordMap1 = defaultLogAccessor.writeLogRecord(logRecord1);
        Assertions.assertNotNull(writeLogRecordMap1);

        final LogRecord logRecord2 = CoreTestFixtures.getLogRecord2();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap2 = defaultLogAccessor.asyncWriteLogRecord(logRecord2);
        Assertions.assertNotNull(writeLogRecordMap2);
        getFuture(writeLogRecordMap2);


        defaultLogAccessor.synchronizeLogStores();


        runReadLogRecordsByCriteriaTests("Formatted Message1: ARG");
        runReadLogRecordsByCriteriaTests(logRecord1.getTimestamp());
        runReadLogRecordsByCriteriaTests("Formatted Message2: ARG");
        runReadLogRecordsByCriteriaTests(logRecord2.getTimestamp());
        runReadLogRecordsByCriteriaTests("Formatted Message1: {}");
        runReadLogRecordsByCriteriaTests("Formatted Message2: {}");

        final Map<String, Collection<LogRecord>> readLogRecordsAtMap0 =
                defaultLogAccessor.readLogRecordsAt(logRecord1.getTimestamp(), 10);
        Assertions.assertNotNull(readLogRecordsAtMap0);
        final Set<Map.Entry<String, Collection<LogRecord>>> readLogRecordsAtEntries0 =
                readLogRecordsAtMap0.entrySet();
        Collection<LogRecord> logRecords0 = null;
        for (final Map.Entry<String, Collection<LogRecord>> entry : readLogRecordsAtEntries0) {
            logRecords0 = entry.getValue();
            Assertions.assertNotNull(logRecords0);
            break;
        }
        Assertions.assertNotNull(logRecords0);
        Assertions.assertEquals(1, logRecords0.size());
        final LogRecord readLogRecords0AtOne = logRecords0.stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecords0AtOne);
        Assertions.assertEquals(logRecord1.getTimestamp(), readLogRecords0AtOne.getTimestamp());

        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsAtMap1 =
                defaultLogAccessor.asyncReadLogRecordsAt(logRecord1.getTimestamp(), 10);
        Assertions.assertNotNull(readLogRecordsAtMap1);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordsAtEntries1 =
                readLogRecordsAtMap1.entrySet();
        Collection<LogRecord> logRecords1 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordsAtEntries1) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords1 = future.get();
            Assertions.assertNotNull(logRecords1);
            break;
        }
        Assertions.assertNotNull(logRecords1);
        Assertions.assertEquals(1, logRecords1.size());
        final LogRecord readLogRecordsAtOne = logRecords1.stream().findFirst().orElse(null);
        Assertions.assertNotNull(readLogRecordsAtOne);
        Assertions.assertEquals(logRecord1.getTimestamp(), readLogRecordsAtOne.getTimestamp());


        final Map<String, Collection<LogRecord>> readLogRecordsBetweenMap1 =
                defaultLogAccessor.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetweenMap1);
        final Set<Map.Entry<String, Collection<LogRecord>>> readLogRecordsBetweenEntries1 =
                readLogRecordsBetweenMap1.entrySet();
        Collection<LogRecord> logRecords2 = null;
        for (final Map.Entry<String, Collection<LogRecord>> entry : readLogRecordsBetweenEntries1) {
            logRecords2 = entry.getValue();
            Assertions.assertNotNull(logRecords2);
            break;
        }
        Assertions.assertNotNull(logRecords2);
        Assertions.assertEquals(2, logRecords2.size());


        final Map<String, CompletableFuture<Void>> removeLogRecordsAtMap1 =
                defaultLogAccessor.asyncRemoveLogRecordsAt(logRecord1.getTimestamp());
        final Set<Map.Entry<String, CompletableFuture<Void>>> removeLogRecordsAtMapEntries1 =
                removeLogRecordsAtMap1.entrySet();
        for (final Map.Entry<String, CompletableFuture<Void>> entry : removeLogRecordsAtMapEntries1) {
            final CompletableFuture<Void> future = entry.getValue();
            Assertions.assertNotNull(future);
            future.get();
            break;
        }


        final Collection<String> removeLogRecordsAtMap2 =
                defaultLogAccessor.removeLogRecordsAt(logRecord1.getTimestamp());
        for (final String entry : removeLogRecordsAtMap2) {
            Assertions.assertNotNull(entry);
            break;
        }


        defaultLogAccessor.synchronizeLogStores();


        final Map<String, Collection<LogRecord>> readLogRecordsBetweenMap2 =
                defaultLogAccessor.readLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetweenMap2);
        final Set<Map.Entry<String, Collection<LogRecord>>> readLogRecordsBetweenEntries2 =
                readLogRecordsBetweenMap2.entrySet();
        Collection<LogRecord> logRecords3 = null;
        for (final Map.Entry<String, Collection<LogRecord>> entry : readLogRecordsBetweenEntries2) {
            logRecords3 = entry.getValue();
            Assertions.assertNotNull(logRecords3);
            break;
        }
        Assertions.assertNotNull(logRecords3);
        Assertions.assertEquals(1, logRecords3.size());


        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsBetweenMap3 =
                defaultLogAccessor.asyncReadLogRecordsBetween(Long.MIN_VALUE, logRecord1.getTimestamp() - 100, 10);
        Assertions.assertNotNull(readLogRecordsBetweenMap3);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordsBetweenEntries3 =
                readLogRecordsBetweenMap3.entrySet();
        Collection<LogRecord> logRecords4 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordsBetweenEntries3) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords4 = future.get();
            break;
        }
        Assertions.assertNotNull(logRecords4);
        Assertions.assertTrue(logRecords4.isEmpty());


        final LogRecord logRecord3 = CoreTestFixtures.getLogRecord3();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap3 = defaultLogAccessor.asyncWriteLogRecord(logRecord3);
        Assertions.assertNotNull(writeLogRecordMap3);
        getFuture(writeLogRecordMap3);

        final LogRecord logRecord4 = CoreTestFixtures.getLogRecord4();
        final Map<String, CompletableFuture<Void>> writeLogRecordMap4 = defaultLogAccessor.asyncWriteLogRecord(logRecord4);
        Assertions.assertNotNull(writeLogRecordMap4);
        getFuture(writeLogRecordMap4);


        defaultLogAccessor.synchronizeLogStores();


        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsBetweenMap4 =
                defaultLogAccessor.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetweenMap4);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordsBetweenEntries4 =
                readLogRecordsBetweenMap4.entrySet();
        Collection<LogRecord> logRecords5 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordsBetweenEntries4) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords5 = future.get();
            break;
        }
        Assertions.assertNotNull(logRecords5);
        Assertions.assertEquals(3, logRecords5.size());


        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .message("Another")
                .build();
        final Collection<String> removeLogRecordsByCriteria1 =
                defaultLogAccessor.removeLogRecordsByCriteria(logRecordCriteria);
        Assertions.assertNotNull(removeLogRecordsByCriteria1);
        for (final String entry : removeLogRecordsByCriteria1) {
            Assertions.assertNotNull(entry);
            break;
        }
        defaultLogAccessor.synchronizeLogStores();
        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsBetweenMap5 =
                defaultLogAccessor.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetweenMap5);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordsBetweenEntries5 =
                readLogRecordsBetweenMap5.entrySet();
        Collection<LogRecord> logRecords6 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordsBetweenEntries5) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords6 = future.get();
            break;
        }
        Assertions.assertNotNull(logRecords6);
        Assertions.assertEquals(2, logRecords6.size());


        final LogRecordCriteria logRecordCriteria1 = LogRecordCriteria
                .builder()
                .timestampCriteria(new SetTimestampCriteria(Set.of(logRecord2.getTimestamp())))
                .build();
        final Map<String, CompletableFuture<Void>> removeLogRecordsByCriteria =
                defaultLogAccessor.asyncRemoveLogRecordsByCriteria(logRecordCriteria1);
        getFuture(removeLogRecordsByCriteria);
        defaultLogAccessor.synchronizeLogStores();


        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsBetween6 =
                defaultLogAccessor.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordBetweenEntries6 =
                readLogRecordsBetween6.entrySet();
        Collection<LogRecord> logRecords7 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordBetweenEntries6) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords7 = future.get();
            break;
        }
        Assertions.assertNotNull(logRecords7);
        Assertions.assertEquals(1, logRecords7.size());


        final LogRecordCriteria logRecordCriteria2 = LogRecordCriteria
                .builder()
                .timestampCriteria(new RangeTimestampCriteria(logRecord1.getTimestamp(), Long.MAX_VALUE))
                .build();
        final Map<String, CompletableFuture<Void>> removeLogRecordsByCriteria2 =
                defaultLogAccessor.asyncRemoveLogRecordsByCriteria(logRecordCriteria2);
        getFuture(removeLogRecordsByCriteria2);
        defaultLogAccessor.synchronizeLogStores();


        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsBetweenMap6 =
                defaultLogAccessor.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        Assertions.assertNotNull(readLogRecordsBetweenMap6);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordsBetweenEntries6 =
                readLogRecordsBetweenMap6.entrySet();
        Collection<LogRecord> logRecords8 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordsBetweenEntries6) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords8 = future.get();
            break;
        }
        Assertions.assertNotNull(logRecords8);
        Assertions.assertTrue(logRecords8.isEmpty());


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
        final Map<String, CompletableFuture<Void>> writeLogRecordMap5 = defaultLogAccessor.asyncWriteLogRecord(logRecord5);
        Assertions.assertNotNull(writeLogRecordMap5);
        getFuture(writeLogRecordMap5);

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
        final Map<String, CompletableFuture<Void>> writeLogRecordMap6 = defaultLogAccessor.asyncWriteLogRecord(logRecord6);
        Assertions.assertNotNull(writeLogRecordMap6);
        getFuture(writeLogRecordMap6);


        defaultLogAccessor.synchronizeLogStores();


        runReadLogRecordsByCriteriaTests(LogLevel.OFF, LogLevelCriteria.Match.GREATER_OR_EQUAL, 0);
        runReadLogRecordsByCriteriaTests(LogLevel.ERROR, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.WARN, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.INFO, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.DEBUG, LogLevelCriteria.Match.GREATER_OR_EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.TRACE, LogLevelCriteria.Match.GREATER_OR_EQUAL, 2);
        runReadLogRecordsByCriteriaTests(LogLevel.TRACE, LogLevelCriteria.Match.EQUAL, 1);
        runReadLogRecordsByCriteriaTests(LogLevel.ALL, LogLevelCriteria.Match.GREATER_OR_EQUAL, 2);

        final Map<String, CompletableFuture<Void>> removeLogRecordsBetweenMap7 =
                defaultLogAccessor.asyncRemoveLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE);
        getFuture(removeLogRecordsBetweenMap7);
        defaultLogAccessor.synchronizeLogStores();

        final Collection<String> removeLogRecordsBetweenMap8 =
                defaultLogAccessor.removeLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE);
        Assertions.assertNotNull(removeLogRecordsBetweenMap8);
        defaultLogAccessor.synchronizeLogStores();


        final Map<String, CompletableFuture<Collection<LogRecord>>> readLogRecordsBetween9 =
                defaultLogAccessor.asyncReadLogRecordsBetween(Long.MIN_VALUE, Long.MAX_VALUE, 10);
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> readLogRecordsBetweenEntries9 =
                readLogRecordsBetween9.entrySet();
        Collection<LogRecord> logRecords9 = null;
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : readLogRecordsBetweenEntries9) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            Assertions.assertNotNull(future);

            logRecords9 = future.get();
            break;
        }
        Assertions.assertNotNull(logRecords9);
        Assertions.assertTrue(logRecords9.isEmpty());


        final Gauge<?> logStoreByTypeNameMapSizeGauge = defaultLogAccessor.getLogStoreByTypeNameMapSizeGauge();
        Assertions.assertNotNull(logStoreByTypeNameMapSizeGauge);

        final Gauge<?> logStoreConfigurationByLogStoreTypeNameMapSizeGauge =
                defaultLogAccessor.getLogStoreConfigurationByLogStoreTypeNameMapSizeGauge();
        Assertions.assertNotNull(logStoreConfigurationByLogStoreTypeNameMapSizeGauge);

        final Gauge<?> logStoreConfigurationByTypeNameMapSizeGauge =
                defaultLogAccessor.getLogStoreConfigurationByTypeNameMapSizeGauge();
        Assertions.assertNotNull(logStoreConfigurationByTypeNameMapSizeGauge);

        final Gauge<?> logStoreFactoryByTypeNameMapSizeGauge =
                defaultLogAccessor.getLogStoreFactoryByTypeNameMapSizeGauge();
        Assertions.assertNotNull(logStoreFactoryByTypeNameMapSizeGauge);
    }

    @Test
    @Order(2)
    void testCollections() throws Exception {
        defaultLogAccessor.clearLogStoreFactoryByConfigurationTypeMap();
        defaultLogAccessor.clearLogStores();

        defaultLogAccessor.registerLogStoreFactoryByConfigurationType(
                TestLogStoreConfiguration.class, new TestLogStoreFactory());
        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> logStoreFactoryByConfigurationTypeNameMap1 =
                defaultLogAccessor.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap1);
        Assertions.assertEquals(1, logStoreFactoryByConfigurationTypeNameMap1.size());
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap1
                .containsKey(TestLogStoreConfiguration.class.getName()));

        defaultLogAccessor.unregisterLogStoreFactoryByConfigurationType(
                TestLogStoreConfiguration.class, TestLogStoreFactory.class);
        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> logStoreFactoryByConfigurationTypeNameMap2 =
                defaultLogAccessor.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap2);
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap2.isEmpty());

        defaultLogAccessor.registerLogStoreFactoryByConfigurationType(
                TestLogStoreConfiguration.class, new TestLogStoreFactory());
        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> logStoreFactoryByConfigurationTypeNameMap3 =
                defaultLogAccessor.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap3);
        Assertions.assertEquals(1, logStoreFactoryByConfigurationTypeNameMap3.size());
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap3
                .containsKey(TestLogStoreConfiguration.class.getName()));

        defaultLogAccessor.clearLogStoreFactoryByConfigurationTypeMap();
        final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>> logStoreFactoryByConfigurationTypeNameMap4 =
                defaultLogAccessor.getLogStoreFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(logStoreFactoryByConfigurationTypeNameMap4);
        Assertions.assertTrue(logStoreFactoryByConfigurationTypeNameMap4.isEmpty());


        defaultLogAccessor.registerLogStore(new TestLogStore());
        final Collection<LogStore> logStores = defaultLogAccessor.getLogStores();
        Assertions.assertNotNull(logStores);
        Assertions.assertEquals(1, logStores.size());

        defaultLogAccessor.unregisterLogStore(TestLogStore.class);
        final Collection<LogStore> logStores1 = defaultLogAccessor.getLogStores();
        Assertions.assertNotNull(logStores1);
        Assertions.assertTrue(logStores1.isEmpty());

        defaultLogAccessor.registerLogStore(new TestLogStore());
        final Collection<LogStore> logStores2 = defaultLogAccessor.getLogStores();
        Assertions.assertNotNull(logStores2);
        Assertions.assertEquals(1, logStores2.size());

        defaultLogAccessor.clearLogStores();
        final Collection<LogStore> logStores3 = defaultLogAccessor.getLogStores();
        Assertions.assertNotNull(logStores3);
        Assertions.assertTrue(logStores3.isEmpty());
    }

    private void getFuture(final Map<String, CompletableFuture<Void>> completableFutureMap) throws Exception {
        final Set<Map.Entry<String, CompletableFuture<Void>>> entries = completableFutureMap.entrySet();
        for (final Map.Entry<String, CompletableFuture<Void>> entry : entries) {
            final CompletableFuture<Void> future = entry.getValue();
            future.get();
        }
    }

    private void runReadLogRecordsByCriteriaTests(final String message) throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .message(message)
                .build();
        final Map<String, Collection<LogRecord>> resultMap =
                defaultLogAccessor.readLogRecordsByCriteria(logRecordCriteria, 10);
        Assertions.assertNotNull(resultMap);

        Collection<LogRecord> readLogRecordsByCriteria = null;
        final Set<Map.Entry<String, Collection<LogRecord>>> entries = resultMap.entrySet();
        for (final Map.Entry<String, Collection<LogRecord>> entry : entries) {
            readLogRecordsByCriteria = entry.getValue();
            break;
        }

        Assertions.assertNotNull(readLogRecordsByCriteria);
        Assertions.assertFalse(readLogRecordsByCriteria.isEmpty());
    }

    private void runReadLogRecordsByCriteriaTests(final long timestamp) throws Exception {
        final LogRecordCriteria logRecordCriteria = LogRecordCriteria
                .builder()
                .timestampCriteria(new ExactTimestampCriteria(timestamp))
                .build();
        final Map<String, CompletableFuture<Collection<LogRecord>>> futureMap =
                defaultLogAccessor.asyncReadLogRecordsByCriteria(logRecordCriteria, 10);
        Assertions.assertNotNull(futureMap);

        Collection<LogRecord> readLogRecordsByCriteria = null;
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> entries = futureMap.entrySet();
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : entries) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            readLogRecordsByCriteria = future.get();
            break;
        }

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
        final Map<String, CompletableFuture<Collection<LogRecord>>> futureMap =
                defaultLogAccessor.asyncReadLogRecordsByCriteria(logRecordCriteria, 10);
        Assertions.assertNotNull(futureMap);

        Collection<LogRecord> readLogRecordsByCriteria = null;
        final Set<Map.Entry<String, CompletableFuture<Collection<LogRecord>>>> entries = futureMap.entrySet();
        for (final Map.Entry<String, CompletableFuture<Collection<LogRecord>>> entry : entries) {
            final CompletableFuture<Collection<LogRecord>> future = entry.getValue();
            readLogRecordsByCriteria = future.get();
            break;
        }
        Assertions.assertNotNull(readLogRecordsByCriteria);
        Assertions.assertEquals(size, readLogRecordsByCriteria.size());
    }

    private static class TestLogStore implements LogStore {
        @Override
        public Collection<LogRecord> readLogRecordsBetween(
                final long inclusiveStartTimestamp,
                final long exclusiveEndTimestamp,
                final int pageSize) {
            return null;
        }

        @Override
        public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsBetween(
                final long inclusiveStartTimestamp,
                final long exclusiveEndTimestamp,
                final int pageSize)
        {
            return null;
        }

        @Override
        public Collection<LogRecord> readLogRecordsAt(final long timestamp, final int pageSize) {
            return null;
        }

        @Override
        public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsAt(final long timestamp, final int pageSize) {
            return null;
        }

        @Override
        public Collection<LogRecord> readLogRecordsByCriteria(
                final LogRecordCriteria logRecordCriteria,
                final int pageSize)
        {
            return null;
        }

        @Override
        public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsByCriteria(
                final LogRecordCriteria logRecordCriteria, final int pageSize)
        {
            return null;
        }

        @Override
        public void writeLogRecord(final LogRecord logRecord) {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncWriteLogRecord(final LogRecord logRecord) {
            return null;
        }

        @Override
        public void removeLogRecordsBetween(
                final long inclusiveStartTimestamp,
                final long exclusiveEndTimestamp)
        {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncRemoveLogRecordsBetween(
                final long inclusiveStartTimestamp, final long exclusiveEndTimestamp)
        {
            return null;
        }

        @Override
        public void removeLogRecordsAt(final long timestamp) {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncRemoveLogRecordsAt(final long timestamp) {
            return null;
        }

        @Override
        public void removeLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) {
            // no-op
        }

        @Override
        public CompletableFuture<Void> asyncRemoveLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) {
            return null;
        }

        @Override
        public void synchronize() throws LogStoreException {

        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public void shutdown() throws AppbaseRuntimeException {

        }
    }

    private static class TestLogStoreConfiguration implements LogStoreConfiguration {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends LogStore> Class<S> getLogStoreType() {
            return (Class<S>) TestLogStore.class;
        }
    }

    private static class TestLogStoreFactory implements LogStoreFactory<TestLogStore, TestLogStoreConfiguration> {
        @Override
        public TestLogStore newLogStore(
                final TestLogStoreConfiguration logStoreConfiguration)
        {
            return new TestLogStore();
        }
    }
}
