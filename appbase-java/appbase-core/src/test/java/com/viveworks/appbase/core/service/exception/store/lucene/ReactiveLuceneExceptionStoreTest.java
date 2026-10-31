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

package com.viveworks.appbase.core.service.exception.store.lucene;

import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneServiceShutdownException;
import com.viveworks.appbase.core.util.lucene.TimestampBasedIndexCleanerConfiguration;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Tests {@link ReactiveLuceneExceptionStore} — the five reactive reads it adds on top of the N01
 * {@code readReactive} SPI ({@code LuceneService#readReactive}) — against a real, in-memory-backed Lucene
 * index (fresh per test). Also covers {@link LuceneExceptionStore}'s two cancellation-aware read overloads
 * ({@code readAllExceptionMessageDetails}, {@code readUniqueThrowableTypeNames}) that the reactive store's
 * {@code readAll}/{@code readUniqueThrowableTypeNames} route through.
 *
 * <p>Successful reads use {@link Mono#block(Duration)} for concise value assertions. Error-channel assertions use
 * {@link StepVerifier}, so checked store exceptions are observed directly rather than through the wrapper that
 * {@code block} must add before throwing them from a method with no checked-exception declaration.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveLuceneExceptionStoreTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    private DefaultLuceneService luceneService;
    private LuceneExceptionStore luceneExceptionStore;
    private ReactiveLuceneExceptionStore reactiveLuceneExceptionStore;

    @BeforeAll
    static void setUpAll() {
        logService = CoreTestFixtures.getLogService();
        // A live SimpleMeterRegistry (not CoreTestFixtures' default no-op composite) so the metrics-parity
        // assertions can observe recorded Timer/Counter counts.
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
        final StackTraceHashGenerator stackTraceHashGenerator = new DefaultStackTraceHashGenerator();
        final ExceptionMessageContextCodec exceptionMessageContextCodec = new DefaultExceptionMessageContextCodec();
        final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(
                                TimestampBasedIndexCleanerConfiguration.builder().enabled(false).build())
                        .build();
        luceneService = new DefaultLuceneService(
                logService,
                metricService,
                validationService,
                mediatorService,
                defaultLuceneServiceConfiguration,
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
        reactiveLuceneExceptionStore = new ReactiveLuceneExceptionStore(luceneExceptionStore);

        final long baseTimestamp = System.currentTimeMillis();
        final ExceptionMessage message0 = CoreTestFixtures.getExceptionMessage(baseTimestamp);
        final ExceptionMessage message1 = CoreTestFixtures.getExceptionMessage(baseTimestamp + 10_000);
        message1.setThrowable(new NullPointerException("npe-marker"));
        luceneExceptionStore.writeExceptionMessage(message0);
        luceneExceptionStore.writeExceptionMessage(message1);
        luceneExceptionStore.synchronize();
    }

    @AfterEach
    void tearDown() {
        if (luceneExceptionStore != null) {
            luceneExceptionStore.shutdown();
        }
        if (luceneService != null) {
            luceneService.shutdown();
        }
    }

    @Test
    void readAllReactiveMatchesBlockingRead() throws Exception {
        final Collection<ExceptionMessageDetails> blocking = luceneExceptionStore.readAllExceptionMessageDetails();
        final Collection<ExceptionMessageDetails> reactive =
                reactiveLuceneExceptionStore.readAllExceptionMessageDetailsReactive().block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertEquals(2, reactive.size());
    }

    @Test
    void byIdReactiveMatchesBlockingRead() throws Exception {
        final ExceptionMessageDetails any =
                luceneExceptionStore.readAllExceptionMessageDetails().iterator().next();

        final ExceptionMessageDetails reactive =
                reactiveLuceneExceptionStore.readExceptionMessageDetailsByIdReactive(any.getId()).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(any.getId(), reactive.getId());
    }

    @Test
    void byIdReactiveCompletesEmptyForAnAbsentId() {
        // F-2026-08-07-01: the by-id reactive read documents "completes empty if absent". A Mono cannot
        // legally carry a null onNext, so an id with no matching document resolves to an empty completion
        // (sink.success(null) completes empty), never an emitted null. Integer.MAX_VALUE is not indexed.
        final Optional<ExceptionMessageDetails> result =
                reactiveLuceneExceptionStore.readExceptionMessageDetailsByIdReactive(Integer.MAX_VALUE)
                        .blockOptional(BLOCK_TIMEOUT);

        Assertions.assertTrue(result.isEmpty(),
                "an absent id must complete empty (not emit null); got " + result);
    }

    @Test
    void byTimestampsReactiveMatchesBlockingRead() throws Exception {
        // inclusiveMinTimestamp must be positive (> 0); 1 is the minimum valid lower bound. Both indexed
        // records carry a current-millis timestamp, so [1, Long.MAX_VALUE) captures both.
        final Collection<ExceptionMessageDetails> blocking =
                luceneExceptionStore.readAllExceptionMessageDetailsByTimestamps(1, Long.MAX_VALUE);
        final Collection<ExceptionMessageDetails> reactive =
                reactiveLuceneExceptionStore
                        .readAllExceptionMessageDetailsByTimestampsReactive(1, Long.MAX_VALUE).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertEquals(2, reactive.size());
    }

    @Test
    void uniqueThrowableTypeNamesReactiveMatchesBlockingRead() throws Exception {
        final Collection<String> blocking = luceneExceptionStore.readUniqueThrowableTypeNames();
        final Collection<String> reactive =
                reactiveLuceneExceptionStore.readUniqueThrowableTypeNamesReactive().block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertTrue(reactive.contains(NullPointerException.class.getName()));
    }

    @Test
    void byThrowableTypeNameReactiveMatchesBlockingRead() throws Exception {
        final String throwableTypeName = NullPointerException.class.getName();
        final Collection<ExceptionMessageDetails> blocking =
                luceneExceptionStore.readExceptionMessageDetailsByThrowableTypeName(throwableTypeName);
        final Collection<ExceptionMessageDetails> reactive =
                reactiveLuceneExceptionStore
                        .readExceptionMessageDetailsByThrowableTypeNameReactive(throwableTypeName).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(blocking.size(), reactive.size());
        Assertions.assertEquals(1, reactive.size());
    }

    @Test
    void readErrorSurfaces() {
        // Deterministic error path: shut the underlying LuceneService down (closes the DirectoryReader
        // synchronously) BEFORE the reactive read, so the offloaded read observes an already-closed
        // reader and throws — no timing race.
        luceneService.shutdown();

        final Mono<Collection<ExceptionMessageDetails>> mono =
                reactiveLuceneExceptionStore.readAllExceptionMessageDetailsReactive();

        Assertions.assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
    }

    @Test
    void cancellationAwareReadAllShortCircuitsBeforeAnyDocument() throws Exception {
        // The cancel-aware overload checks the flag at the top of the maxDoc loop; an always-cancelled
        // supplier breaks before the first document, yielding an empty result even though two are indexed.
        final Collection<ExceptionMessageDetails> notCancelled =
                luceneExceptionStore.readAllExceptionMessageDetails(() -> false);
        final Collection<ExceptionMessageDetails> cancelled =
                luceneExceptionStore.readAllExceptionMessageDetails(() -> true);

        Assertions.assertEquals(2, notCancelled.size());
        Assertions.assertTrue(cancelled.isEmpty());
    }

    @Test
    void cancellationAwareUniqueThrowableTypeNamesShortCircuitsBeforeAnyDocument() throws Exception {
        final Collection<String> notCancelled = luceneExceptionStore.readUniqueThrowableTypeNames(() -> false);
        final Collection<String> cancelled = luceneExceptionStore.readUniqueThrowableTypeNames(() -> true);

        Assertions.assertFalse(notCancelled.isEmpty());
        Assertions.assertTrue(cancelled.isEmpty());
    }

    @Test
    void readAllReactiveRecordsTheSameTimerTheBlockingReadUses() throws Exception {
        // Metrics parity (N05 S04): the reactive read records the SAME per-store Timer the blocking
        // LuceneExceptionStore.readAllExceptionMessageDetails records (registry timer(name) is get-or-create).
        final String timerName = metricService.name(LuceneExceptionStore.class, "readAllExceptionMessageDetails", "timer");
        final long before = metricService.getMetricRegistry().timer(timerName).getCount();

        reactiveLuceneExceptionStore.readAllExceptionMessageDetailsReactive().block(BLOCK_TIMEOUT);

        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().timer(timerName).getCount(),
                "reactive Lucene exception read must record the same per-store Timer the blocking read does");
    }

    @Test
    void readErrorIncrementsTheSameErrorCounterTheBlockingReadUses() {
        final String counterName =
                metricService.name(LuceneExceptionStore.class, "readAllExceptionMessageDetails", "error", "counter");
        final long before = metricService.getMetricRegistry().counter(counterName).getCount();

        luceneService.shutdown();
        final Mono<Collection<ExceptionMessageDetails>> mono =
                reactiveLuceneExceptionStore.readAllExceptionMessageDetailsReactive();
        StepVerifier.create(mono)
                .expectErrorSatisfies(error -> {
                    Assertions.assertInstanceOf(ExceptionStoreException.class, error);
                    Assertions.assertInstanceOf(LuceneServiceShutdownException.class, error.getCause());
                })
                .verify(BLOCK_TIMEOUT);

        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().counter(counterName).getCount(),
                "a failed reactive Lucene exception read must increment the same per-store error Counter the "
                        + "blocking read does");
    }

    // EXTRA: protects the five read operations' distinct error counters after lifecycle rejection (review regression).
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "readExceptionMessageDetailsById, byId",
            "readAllExceptionMessageDetails, all",
            "readAllExceptionMessageDetailsByTimestamps, timestamps",
            "readUniqueExceptionTypeNames, uniqueTypes",
            "readExceptionMessageDetailsByThrowableTypeName, byTypeName"
    })
    void test_EXTRA_eachPostShutdownReadIncrementsItsExactErrorCounter(
            final String metricOperation, final String reactiveOperation)
    {
        final String counterName =
                metricService.name(LuceneExceptionStore.class, metricOperation, "error", "counter");
        final long before = metricService.getMetricRegistry().counter(counterName).getCount();

        luceneService.shutdown();
        StepVerifier.create(postShutdownRead(reactiveOperation))
                .expectErrorSatisfies(error -> {
                    Assertions.assertInstanceOf(ExceptionStoreException.class, error);
                    Assertions.assertInstanceOf(LuceneServiceShutdownException.class, error.getCause());
                })
                .verify(BLOCK_TIMEOUT);
        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().counter(counterName).getCount(),
                "reactive lifecycle rejection must increment only the counter belonging to " + metricOperation);
    }

    private Mono<?> postShutdownRead(final String operation) {
        switch (operation) {
            case "byId":
                return reactiveLuceneExceptionStore.readExceptionMessageDetailsByIdReactive(0);
            case "all":
                return reactiveLuceneExceptionStore.readAllExceptionMessageDetailsReactive();
            case "timestamps":
                return reactiveLuceneExceptionStore
                        .readAllExceptionMessageDetailsByTimestampsReactive(1, Long.MAX_VALUE);
            case "uniqueTypes":
                return reactiveLuceneExceptionStore.readUniqueThrowableTypeNamesReactive();
            case "byTypeName":
                return reactiveLuceneExceptionStore
                        .readExceptionMessageDetailsByThrowableTypeNameReactive(Throwable.class.getName());
            default:
                throw new IllegalArgumentException("Unknown reactive operation: " + operation);
        }
    }

    @Test
    void uniqueThrowableTypeNamesReactiveRecordsTheReadUniqueExceptionTypeNamesTimer() throws Exception {
        // Guards the metric-name mismatch: the blocking store registers the unique-type-names read under
        // "readUniqueExceptionTypeNames", NOT "readUniqueThrowableTypeNames" (the method name), so the
        // reactive mirror must record under that same (mismatched) name to preserve parity.
        final String timerName = metricService.name(LuceneExceptionStore.class, "readUniqueExceptionTypeNames", "timer");
        final long before = metricService.getMetricRegistry().timer(timerName).getCount();

        reactiveLuceneExceptionStore.readUniqueThrowableTypeNamesReactive().block(BLOCK_TIMEOUT);

        Assertions.assertEquals(before + 1, metricService.getMetricRegistry().timer(timerName).getCount(),
                "reactive readUniqueThrowableTypeNames must record the blocking read's readUniqueExceptionTypeNames "
                        + "timer (name-mismatch parity)");
    }
}
