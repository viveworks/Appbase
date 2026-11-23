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

package com.viveworks.appbase.core.service.log.accessor;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreException;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests {@link DefaultLogAccessor}'s three reactive reads (plan Task 4) — fanning across every registered
 * {@link LogStore} and preserving the multi-store {@code Map<storeName, records>} shape of the existing
 * blocking reads. A {@link LuceneLogStore} routes through {@link
 * com.viveworks.appbase.core.service.log.store.lucene.ReactiveLuceneLogStore}; any other {@link LogStore}
 * routes through {@link com.viveworks.appbase.core.service.log.store.SchedulerOffloadReactiveLogStore} and
 * the N01 {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary}.
 *
 * <p>Per the "add no new dependency" constraint (see {@code DefaultReactiveReadBoundaryTest}'s note),
 * {@code reactor-test}'s {@code StepVerifier} is unavailable — this suite asserts observable behaviour via
 * {@link reactor.core.publisher.Mono#block(Duration)} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultLogAccessorReactiveReadTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static LuceneLogIndex luceneLogIndex;
    private static LuceneLogStore luceneLogStore;
    private static DefaultLogAccessor defaultLogAccessor;

    @BeforeAll
    static void setUpAll() throws Exception {
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

        final long baseTimestamp = System.currentTimeMillis();
        luceneLogStore.writeLogRecord(CoreTestFixtures.getLogRecord(
                "N03 acc fmt 0: {}", "N03 acc msg 0: ARG", "Throwable message0", "Cause Message0",
                "Key0", "Value0", "ACCMARKER0", baseTimestamp, CoreTestFixtures.class.getName(), LogLevel.INFO));
        luceneLogStore.writeLogRecord(CoreTestFixtures.getLogRecord(
                "N03 acc fmt 1: {}", "N03 acc msg 1: ARG", "Throwable message1", "Cause Message1",
                "Key1", "Value1", "ACCMARKER1", baseTimestamp + 1, CoreTestFixtures.class.getName(), LogLevel.INFO));
        luceneLogStore.synchronize();
    }

    @BeforeEach
    void resetStores() {
        // Each test only relies on the always-registered LuceneLogStore plus whatever it registers itself;
        // clear any stub a previous test left behind so store counts stay deterministic.
        defaultLogAccessor.clearLogStores();
        defaultLogAccessor.registerLogStore(luceneLogStore);
    }

    @AfterAll
    static void tearDownAll() {
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
    void pageSizeIsBoundedBeforeAnyStoreRead() throws Exception {
        defaultLogAccessor.clearLogStores();
        final LogStore stub = mock(LogStore.class);
        defaultLogAccessor.registerLogStore(stub);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> defaultLogAccessor.readLogRecordsBetween(0, Long.MAX_VALUE, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> defaultLogAccessor.readLogRecordsBetween(0, Long.MAX_VALUE, 1001));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> defaultLogAccessor.readLogRecordsAt(0, 1001));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> defaultLogAccessor.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 1001));
        verifyNoInteractions(stub);

        defaultLogAccessor.clearLogStores();
        Assertions.assertDoesNotThrow(
                () -> defaultLogAccessor.readLogRecordsBetween(0, Long.MAX_VALUE, 1000));
    }

    @Test
    void reactiveBetweenMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final Map<String, Collection<LogRecord>> sync =
                defaultLogAccessor.readLogRecordsBetween(0, Long.MAX_VALUE, 10);
        final Map<String, Collection<LogRecord>> got =
                defaultLogAccessor.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertEquals(
                sync.get(LuceneLogStore.class.getName()).size(),
                got.get(LuceneLogStore.class.getName()).size());
    }

    @Test
    void reactiveBetweenStreamsEachRecordWithItsStoreName() {
        final List<Map.Entry<String, LogRecord>> records = defaultLogAccessor
                .streamLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10)
                .collectList()
                .block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(records);
        Assertions.assertEquals(2, records.size());
        Assertions.assertTrue(records.stream()
                .allMatch(entry -> LuceneLogStore.class.getName().equals(entry.getKey())));
    }

    @Test
    void reactiveAtMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final Collection<LogRecord> any =
                defaultLogAccessor.readLogRecordsBetween(0, Long.MAX_VALUE, 10).get(LuceneLogStore.class.getName());
        final long timestamp = any.stream().findFirst().orElseThrow().getTimestamp();

        final Map<String, Collection<LogRecord>> sync = defaultLogAccessor.readLogRecordsAt(timestamp, 10);
        final Map<String, Collection<LogRecord>> got =
                defaultLogAccessor.readLogRecordsAtReactive(timestamp, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertEquals(
                sync.get(LuceneLogStore.class.getName()).size(),
                got.get(LuceneLogStore.class.getName()).size());
    }

    @Test
    void reactiveByCriteriaMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria criteria =
                com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria
                        .builder()
                        .markerName("ACCMARKER1")
                        .build();

        final Map<String, Collection<LogRecord>> sync = defaultLogAccessor.readLogRecordsByCriteria(criteria, 10);
        final Map<String, Collection<LogRecord>> got =
                defaultLogAccessor.readLogRecordsByCriteriaReactive(criteria, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertEquals(
                sync.get(LuceneLogStore.class.getName()).size(),
                got.get(LuceneLogStore.class.getName()).size());
    }

    @Test
    void reactiveReadIncludesNonLuceneStoresThroughTheBoundary() throws Exception {
        final LogStore stub = mock(LogStore.class);
        when(stub.readLogRecordsBetween(anyLong(), anyLong(), anyInt()))
                .thenReturn(List.of(CoreTestFixtures.getLogRecord(
                        "stub fmt: {}", "stub msg: ARG", "Throwable message", "Cause Message",
                        "Key", "Value", "STUBMARKER", System.currentTimeMillis(),
                        CoreTestFixtures.class.getName(), LogLevel.INFO)));
        defaultLogAccessor.registerLogStore(stub);

        final Map<String, Collection<LogRecord>> got =
                defaultLogAccessor.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertTrue(got.containsKey(LuceneLogStore.class.getName()));
        Assertions.assertEquals(2, got.size());
        Assertions.assertEquals(1, got.get(stub.getClass().getName()).size());
    }
}
