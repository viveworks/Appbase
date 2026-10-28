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
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Tests {@link ReactiveLuceneLogStore} — the three reactive reads it adds on top of the N01
 * {@code readReactive} SPI ({@code LuceneService#readReactive}) — against a real, in-memory-backed Lucene
 * index (fresh per test; see {@link DefaultLuceneServiceConfiguration}'s per-instance temp-dir default).
 *
 * <p>Per the "add no new dependency" constraint (see {@code DefaultReactiveReadBoundaryTest}'s note),
 * {@code reactor-test}'s {@code StepVerifier} is unavailable — this suite asserts observable behaviour via
 * {@link reactor.core.publisher.Mono#block(Duration)} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveLuceneLogStoreTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    private DefaultLuceneService luceneService;
    private LuceneLogStore luceneLogStore;
    private ReactiveLuceneLogStore reactiveLuceneLogStore;

    private LogRecord record0;
    private LogRecord record1;
    private LogRecord record2;

    @BeforeAll
    static void setUpAll() {
        logService = CoreTestFixtures.getLogService();
        // A live SimpleMeterRegistry (not CoreTestFixtures' default no-op composite) so the metrics-parity
        // assertions below can observe recorded Timer/Counter counts — the reactive read records onto the
        // same per-store meters the blocking read does (see the metrics-parity tests).
        metricService = new DefaultMetricService(
                logService,
                new CompositeMeterRegistry().add(new SimpleMeterRegistry()),
                DefaultMetricServiceConfiguration.builder().build());
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDownAll() {
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

    @BeforeEach
    void setUp() throws Exception {
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                DefaultLuceneServiceConfiguration.builder().build(),
                CoreTestFixtures.getReactiveReadBoundary());
        final LuceneLogIndex luceneLogIndex = CoreTestFixtures.getDefaultLuceneLogIndex(
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
        reactiveLuceneLogStore = new ReactiveLuceneLogStore(luceneLogStore);

        final long baseTimestamp = System.currentTimeMillis();
        record0 = CoreTestFixtures.getLogRecord(
                "N03 fmt 0: {}", "N03 msg 0: ARG", "Throwable message0", "Cause Message0",
                "Key0", "Value0", "MARKER0", baseTimestamp, CoreTestFixtures.class.getName(), LogLevel.INFO);
        record1 = CoreTestFixtures.getLogRecord(
                "N03 fmt 1: {}", "N03 msg 1: ARG", "Throwable message1", "Cause Message1",
                "Key1", "Value1", "MARKER1", baseTimestamp + 1, CoreTestFixtures.class.getName(), LogLevel.INFO);
        record2 = CoreTestFixtures.getLogRecord(
                "N03 fmt 2: {}", "N03 msg 2: ARG", "Throwable message2", "Cause Message2",
                "Key2", "Value2", "MARKER2", baseTimestamp + 2, CoreTestFixtures.class.getName(), LogLevel.INFO);

        luceneLogStore.writeLogRecord(record0);
        luceneLogStore.writeLogRecord(record1);
        luceneLogStore.writeLogRecord(record2);
        luceneLogStore.synchronize();
    }

    @AfterEach
    void tearDown() {
        if (luceneLogStore != null) {
            luceneLogStore.shutdown();
        }
        if (luceneService != null) {
            luceneService.shutdown();
        }
    }

    @Test
    void betweenReactiveMatchesBlockingRead() throws Exception {
        final Collection<LogRecord> blocking = luceneLogStore.readLogRecordsBetween(0, Long.MAX_VALUE, 10);
        final Collection<LogRecord> reactive =
                reactiveLuceneLogStore.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertEquals(3, reactive.size());
    }

    @Test
    void betweenReactiveStreamsRecordsIndividually() {
        final List<LogRecord> records = reactiveLuceneLogStore
                .streamLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10)
                .collectList()
                .block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(records);
        Assertions.assertEquals(3, records.size());
    }

    @Test
    void betweenReactiveStreamRecordsTheSameTimerTheBlockingReadUses() {
        final String timerName = metricService.name(LuceneLogStore.class, "readLogRecordsBetween", "timer");
        final long before = metricService.getMetricRegistry().timer(timerName).getCount();

        reactiveLuceneLogStore.streamLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10)
                .collectList()
                .block(BLOCK_TIMEOUT);

        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().timer(timerName).getCount(),
                "reactive Lucene streams must record the same per-store Timer the blocking read does");
    }

    @Test
    void atReactiveMatchesBlockingRead() throws Exception {
        final Collection<LogRecord> blocking = luceneLogStore.readLogRecordsAt(record1.getTimestamp(), 10);
        final Collection<LogRecord> reactive =
                reactiveLuceneLogStore.readLogRecordsAtReactive(record1.getTimestamp(), 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertEquals(1, reactive.size());
        final LogRecord found = reactive.stream().findFirst().orElse(null);
        Assertions.assertNotNull(found);
        Assertions.assertEquals(record1.getTimestamp(), found.getTimestamp());
    }

    @Test
    void byCriteriaReactiveMatchesBlockingRead() throws Exception {
        // markerName is a single un-delimited token ("marker1"), so — unlike `message`, whose shared words
        // ("n03", "msg", "arg") across all three fixtures make an OR-token match hit every record — this
        // uniquely matches record1 alone.
        final LogRecordCriteria criteria = LogRecordCriteria.builder().markerName("MARKER1").build();

        final Collection<LogRecord> blocking = luceneLogStore.readLogRecordsByCriteria(criteria, 10);
        final Collection<LogRecord> reactive =
                reactiveLuceneLogStore.readLogRecordsByCriteriaReactive(criteria, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertEquals(1, reactive.size());
    }

    // AC2520-2 requires this historical regression to pass 50/50, with a fresh fixture per repetition.
    @RepeatedTest(50)
    void readErrorSurfaces() {
        // Deterministic error path: shut the underlying LuceneService down (closes the DirectoryReader
        // synchronously, on this thread) BEFORE issuing the reactive read, so the offloaded search
        // observes an already-closed reader and throws — no timing race, unlike a shutdown racing a
        // concurrently-scheduled read would be.
        luceneService.shutdown();

        final reactor.core.publisher.Mono<Collection<LogRecord>> mono =
                reactiveLuceneLogStore.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10);

        Assertions.assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
    }

    @Test
    void betweenReactiveRecordsTheSameTimerTheBlockingReadUses() throws Exception {
        // Metrics parity (N05 S04): the reactive Lucene read must record the SAME per-store Timer the
        // blocking LuceneLogStore.readLogRecordsBetween records — the registry's timer(name) is
        // get-or-create by name, so recording under the LuceneLogStore-keyed name hits the same series.
        final String timerName = metricService.name(LuceneLogStore.class, "readLogRecordsBetween", "timer");
        final long before = metricService.getMetricRegistry().timer(timerName).getCount();

        reactiveLuceneLogStore.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10).block(BLOCK_TIMEOUT);

        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().timer(timerName).getCount(),
                "reactive Lucene read must record the same per-store Timer the blocking read does");
    }

    @Test
    void readErrorIncrementsTheSameErrorCounterTheBlockingReadUses() {
        final String counterName =
                metricService.name(LuceneLogStore.class, "readLogRecordsBetween", "error", "counter");
        final long before = metricService.getMetricRegistry().counter(counterName).getCount();

        luceneService.shutdown();
        final reactor.core.publisher.Mono<Collection<LogRecord>> mono =
                reactiveLuceneLogStore.readLogRecordsBetweenReactive(0, Long.MAX_VALUE, 10);
        Assertions.assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));

        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().counter(counterName).getCount(),
                "a failed reactive Lucene read must increment the same per-store error Counter the blocking "
                        + "read does");
    }
}
