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

package com.viveworks.appbase.core.service.exception.accessor;

import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.lucene.LuceneExceptionStore;
import com.viveworks.appbase.core.service.exception.store.lucene.LuceneExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.TimestampBasedIndexCleanerConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultExceptionAccessor}'s five reactive reads — fanning across every registered {@link
 * ExceptionStore} and preserving the multi-store {@code Map<storeName, result>} shape of the existing
 * blocking reads. A {@link LuceneExceptionStore} routes through {@link
 * com.viveworks.appbase.core.service.exception.store.lucene.ReactiveLuceneExceptionStore}; any other {@link
 * ExceptionStore} routes through {@link
 * com.viveworks.appbase.core.service.exception.store.SchedulerOffloadReactiveExceptionStore} and the N01
 * {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary}.
 *
 * <p>Only the always-registered {@link LuceneExceptionStore} (in-memory Lucene, no H2) plus per-test stubs
 * are exercised — the fixture accessor's JPA store is cleared in {@link #resetStores()} so no test depends
 * on H2. Per the "add no new dependency" constraint, {@code reactor-test}'s {@code StepVerifier} is
 * unavailable — this suite asserts observable behaviour via {@link reactor.core.publisher.Mono#block(
 * Duration)} instead.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultExceptionAccessorReactiveReadTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultLuceneService luceneService;
    private static LuceneExceptionStore luceneExceptionStore;
    private static DefaultExceptionAccessor defaultExceptionAccessor;
    private static long baseTimestamp;

    @BeforeAll
    static void setUpAll() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);

        final StackTraceHashGenerator stackTraceHashGenerator = new DefaultStackTraceHashGenerator();
        final ExceptionMessageContextCodec exceptionMessageContextCodec = new DefaultExceptionMessageContextCodec();
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(
                                TimestampBasedIndexCleanerConfiguration.builder().enabled(false).build())
                        .build(),
                CoreTestFixtures.getReactiveReadBoundary());
        luceneExceptionStore = new LuceneExceptionStore(
                logService,
                metricService,
                mediatorService,
                validationService,
                luceneService,
                stackTraceHashGenerator,
                exceptionMessageContextCodec,
                LuceneExceptionStoreConfiguration.builder().enabled(true).build());

        baseTimestamp = System.currentTimeMillis();
        final ExceptionMessage message0 = CoreTestFixtures.getExceptionMessage(baseTimestamp);
        final ExceptionMessage message1 = CoreTestFixtures.getExceptionMessage(baseTimestamp + 10_000);
        message1.setThrowable(new NullPointerException("npe-marker"));
        luceneExceptionStore.writeExceptionMessage(message0);
        luceneExceptionStore.writeExceptionMessage(message1);
        luceneExceptionStore.synchronize();

        defaultExceptionAccessor = CoreTestFixtures.getExceptionAccessor(
                logService, metricService, mediatorService, validationService);
    }

    @BeforeEach
    void resetStores() {
        // Drop the fixture accessor's JPA store (and any stub a previous test left) so reads touch only the
        // in-memory Lucene store plus whatever the test registers itself; keeps store counts deterministic
        // and keeps H2 out of the read path.
        defaultExceptionAccessor.clearExceptionStores();
        defaultExceptionAccessor.registerExceptionStore(luceneExceptionStore);
    }

    @AfterAll
    static void tearDownAll() {
        if (defaultExceptionAccessor != null) {
            defaultExceptionAccessor.shutdown();
        }
        if (luceneExceptionStore != null) {
            luceneExceptionStore.shutdown();
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
    void reactiveReadAllMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final Map<String, Collection<ExceptionMessageDetails>> sync =
                defaultExceptionAccessor.readAllExceptionMessageDetails();
        final Map<String, Collection<ExceptionMessageDetails>> got =
                defaultExceptionAccessor.readAllExceptionMessageDetailsReactive().block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertEquals(
                sync.get(LuceneExceptionStore.class.getName()).size(),
                got.get(LuceneExceptionStore.class.getName()).size());
    }

    @Test
    void reactiveByIdMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final int id = defaultExceptionAccessor.readAllExceptionMessageDetails()
                .get(LuceneExceptionStore.class.getName()).iterator().next().getId();

        final Map<String, ExceptionMessageDetails> sync =
                defaultExceptionAccessor.readExceptionMessageDetailsById(id);
        final Map<String, ExceptionMessageDetails> got =
                defaultExceptionAccessor.readExceptionMessageDetailsByIdReactive(id).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertEquals(
                sync.get(LuceneExceptionStore.class.getName()).getId(),
                got.get(LuceneExceptionStore.class.getName()).getId());
    }

    @Test
    void reactiveReadIncludesNonLuceneStoresThroughTheBoundary() throws Exception {
        final ExceptionStore stub = mock(ExceptionStore.class);
        when(stub.readAllExceptionMessageDetails()).thenReturn(List.of(mock(ExceptionMessageDetails.class)));
        defaultExceptionAccessor.registerExceptionStore(stub);

        final Map<String, Collection<ExceptionMessageDetails>> got =
                defaultExceptionAccessor.readAllExceptionMessageDetailsReactive().block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertTrue(got.containsKey(LuceneExceptionStore.class.getName()));
        Assertions.assertEquals(2, got.size());
        Assertions.assertEquals(1, got.get(stub.getClass().getName()).size());
    }

    @Test
    void reactiveByIdSkipsStoresWhoseReadReturnsNull() throws Exception {
        final ExceptionStore stub = mock(ExceptionStore.class);
        when(stub.readExceptionMessageDetailsById(org.mockito.ArgumentMatchers.anyInt())).thenReturn(null);
        defaultExceptionAccessor.registerExceptionStore(stub);

        final int id = defaultExceptionAccessor.readAllExceptionMessageDetails()
                .get(LuceneExceptionStore.class.getName()).iterator().next().getId();

        final Map<String, ExceptionMessageDetails> got =
                defaultExceptionAccessor.readExceptionMessageDetailsByIdReactive(id).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        // The Lucene store finds the id; the stub returns null and is dropped (a Mono cannot carry null),
        // matching the blocking accessor's skip-if-null behaviour.
        Assertions.assertTrue(got.containsKey(LuceneExceptionStore.class.getName()));
        Assertions.assertFalse(got.containsKey(stub.getClass().getName()));
    }

    @Test
    void reactiveByTimestampsMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        // Narrow the window to EXCLUDE message0 (at baseTimestamp) and include only message1 (at
        // baseTimestamp + 10_000), so the by-timestamps result (size 1) is distinguishable from an
        // unfiltered readAll (size 2) — otherwise a mis-wire to the wrong reactive method would not
        // redden this test (the wiring-slip class it exists to catch).
        final long inclusiveMin = baseTimestamp + 5_000;

        final Map<String, Collection<ExceptionMessageDetails>> sync =
                defaultExceptionAccessor.readAllExceptionMessageDetailsByTimestamps(inclusiveMin, Long.MAX_VALUE);
        final Map<String, Collection<ExceptionMessageDetails>> got =
                defaultExceptionAccessor
                        .readAllExceptionMessageDetailsByTimestampsReactive(inclusiveMin, Long.MAX_VALUE)
                        .block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        // Exactly one record is in-window: the filter must be applied (a mis-wire to readAll gives 2).
        Assertions.assertEquals(1, sync.get(LuceneExceptionStore.class.getName()).size());
        Assertions.assertEquals(
                sync.get(LuceneExceptionStore.class.getName()).size(),
                got.get(LuceneExceptionStore.class.getName()).size());
    }

    @Test
    void reactiveUniqueExceptionTypeNamesMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final Map<String, Collection<String>> sync = defaultExceptionAccessor.readUniqueExceptionTypeNames();
        final Map<String, Collection<String>> got =
                defaultExceptionAccessor.readUniqueExceptionTypeNamesReactive().block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertTrue(got.get(LuceneExceptionStore.class.getName()).contains(
                NullPointerException.class.getName()));
    }

    @Test
    void reactiveByThrowableTypeNameMatchesSyncAccessorForSingleLuceneStore() throws Exception {
        final String throwableTypeName = NullPointerException.class.getName();

        final Map<String, Collection<ExceptionMessageDetails>> sync =
                defaultExceptionAccessor.readExceptionMessageDetailsByThrowableTypeName(throwableTypeName);
        final Map<String, Collection<ExceptionMessageDetails>> got =
                defaultExceptionAccessor
                        .readExceptionMessageDetailsByThrowableTypeNameReactive(throwableTypeName).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(got);
        Assertions.assertEquals(sync.keySet(), got.keySet());
        Assertions.assertEquals(
                sync.get(LuceneExceptionStore.class.getName()).size(),
                got.get(LuceneExceptionStore.class.getName()).size());
    }

    @Test
    void reactiveReadPropagatesAStoreErrorThroughTheMono() throws Exception {
        final ExceptionStore stub = mock(ExceptionStore.class);
        when(stub.readAllExceptionMessageDetails()).thenThrow(new ExceptionStoreException("boom"));
        defaultExceptionAccessor.registerExceptionStore(stub);

        // The fan-out flatMap is fail-fast (as the blocking loop is), so a store erroring during the
        // reactive read surfaces the error through the aggregate Mono.
        final reactor.core.publisher.Mono<Map<String, Collection<ExceptionMessageDetails>>> mono =
                defaultExceptionAccessor.readAllExceptionMessageDetailsReactive();

        Assertions.assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
    }
}
