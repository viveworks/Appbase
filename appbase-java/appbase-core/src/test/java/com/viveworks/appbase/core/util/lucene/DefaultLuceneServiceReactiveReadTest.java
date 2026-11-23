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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tests the three reactive read methods PR2 of N01 adds to {@link LuceneService} /
 * {@link DefaultLuceneService} — {@code searchReactive}, {@code documentReactive} and {@code readReactive} —
 * against a real, in-memory-backed Lucene index (the {@link DefaultLuceneServiceConfiguration} default
 * directory is a temp path, deleted on shutdown; see {@link DefaultLuceneServiceTest} for the same pattern).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultLuceneServiceReactiveReadTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    private DefaultLuceneService luceneService;

    @BeforeAll
    static void setUpAll() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
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
                DefaultLuceneServiceConfiguration
                        .builder()
                        .defaultIndexCleanerConfiguration(
                                TimestampBasedIndexCleanerConfiguration.builder().enabled(false).build())
                        .build(),
                CoreTestFixtures.getReactiveReadBoundary());
        indexDocuments(luceneService, 2);
    }

    @AfterEach
    void tearDown() {
        if (luceneService != null) {
            luceneService.shutdown();
        }
    }

    private static void indexDocuments(final DefaultLuceneService service, final int count) throws Exception {
        final IndexWriter indexWriter = service.getIndexWriter();
        for (int i = 0; i < count; i++) {
            final Document document = new Document();
            document.add(new StringField("id", "doc-" + i, Field.Store.YES));
            document.add(new TextField("body", "hello world " + i, Field.Store.YES));
            indexWriter.addDocument(document);
        }
        service.synchronizeIndex();
    }

    @Test
    void searchReactiveEmitsTopDocsMatchingBlockingSearchParity() throws Exception {
        final Query query = new TermQuery(new Term("body", "hello"));

        final TopDocs blockingTopDocs = luceneService.withSearcher(searcher -> searcher.search(query, 10));
        final TopDocs reactiveTopDocs = luceneService.searchReactive(query, 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactiveTopDocs);
        Assertions.assertEquals(blockingTopDocs.totalHits.value, reactiveTopDocs.totalHits.value);
    }

    @Test
    void test_AC2520_3_readBeforeShutdownStillWorks() {
        final TopDocs result = luceneService.searchReactive(
                new TermQuery(new Term("body", "hello")), 10).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.totalHits.value);
    }

    @Test
    void documentReactiveEmitsStoredDocumentForKnownId() throws Exception {
        final Query idQuery = new TermQuery(new Term("id", "doc-0"));
        final TopDocs topDocs = luceneService.withSearcher(searcher -> searcher.search(idQuery, 1));
        Assertions.assertTrue(topDocs.scoreDocs.length > 0, "expected doc-0 to be found by the blocking search");
        final int docId = topDocs.scoreDocs[0].doc;

        final Document document = luceneService.documentReactive(docId).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(document);
        Assertions.assertEquals("doc-0", document.get("id"));
    }

    @Test
    void reactiveReadSurfacesAnUnderlyingError() {
        // Deterministic error path: readReactive delegates to the boundary, which routes a thrown exception
        // to the Mono's error channel — the same path searchReactive/documentReactive use. (The earlier
        // shutdown()-based variant was non-deterministic: whether a closed Lucene searcher actually throws
        // depends on timing/state, so it passed in isolation but failed under full-suite ordering.)
        final IllegalStateException boom = new IllegalStateException("index read failed");

        final Mono<TopDocs> mono = luceneService.readReactive(cancelled -> {
            throw boom;
        });

        final Throwable thrown = Assertions.assertThrows(Throwable.class, () -> mono.block(BLOCK_TIMEOUT));
        Assertions.assertTrue(thrown == boom || thrown.getCause() == boom,
                "expected the underlying error to propagate, got " + thrown);
    }

    @Test
    void readReactiveComposesSearchAndFetchIntoACooperativeRead() {
        // "id" is a StringField (not analyzed), unlike "body" (a TextField run through StandardAnalyzer,
        // which tokenizes "hello world 1" into ["hello", "world", "1"]) — an exact TermQuery needs the former.
        final Query query = new TermQuery(new Term("id", "doc-1"));

        final Document result = luceneService.readReactive(cancelled -> luceneService.withSearcher(searcher -> {
            final TopDocs topDocs = searcher.search(query, 1);
            final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            Assertions.assertTrue(scoreDocs.length > 0, "expected a hit for id=doc-1");
            return searcher.getIndexReader().storedFields().document(scoreDocs[0].doc);
        })).block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("doc-1", result.get("id"));
    }

    @Test
    void test_AC2520_2_readAfterShutdownFailsClearly() {
        luceneService.shutdown();

        final Mono<TopDocs> read = luceneService.searchReactive(
                new TermQuery(new Term("body", "hello")), 10);
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class, () -> read.block(BLOCK_TIMEOUT));
        Assertions.assertEquals("Lucene service is shut down", exception.getMessage());
    }

    // EXTRA: Preserves store error accounting when the generic read encounters lifecycle rejection.
    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void test_EXTRA_genericReadAfterShutdownDelegatesToTheStoreReadForErrorAccounting() {
        luceneService.shutdown();
        final AtomicBoolean invoked = new AtomicBoolean();

        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> luceneService.readReactive(cancelled -> {
                    invoked.set(true);
                    return luceneService.withSearcher(searcher -> searcher.getIndexReader().toString());
                }).block(BLOCK_TIMEOUT));

        Assertions.assertEquals("Lucene service is shut down", exception.getMessage());
        Assertions.assertTrue(invoked.get(), "store read must run so its error accounting can observe rejection");
    }

    @Test
    void test_AC2520_1_shutdownWaitsForConcurrentSynchronizerAndLeavesNoLiveReader() throws Exception {
        // Captured via withSearcher (acquire immediately released) rather than a raw getDirectoryReader()
        // accessor -- SearcherManager still holds this as "current" until the synchronizer below refreshes
        // past it, so release() here does not close it early.
        final IndexReader readerBeforeSynchronization = luceneService.withSearcher(IndexSearcher::getIndexReader);
        final CountDownLatch afterRefresh = new CountDownLatch(1);
        final CountDownLatch allowHookToReturn = new CountDownLatch(1);
        final AtomicReference<IndexReader> refreshedReader = new AtomicReference<>();
        luceneService.setIndexSynchronizerBeforePublication(reader -> {
            // Fires AFTER searcherManager.maybeRefreshBlocking() has already published the refreshed
            // reader (unlike the pre-SearcherManager design, there is no "opened but not yet published"
            // window to pause inside) -- it still fires while synchronizeIndex() holds the lifecycle lock,
            // which is what the rest of this test needs: a window where shutdown() is blocked waiting for
            // that lock.
            refreshedReader.set(reader);
            afterRefresh.countDown();
            try {
                Assertions.assertTrue(allowHookToReturn.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interruptedException);
            }
        });

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<?> synchronizer = executor.submit(() -> {
                luceneService.synchronizeIndex();
                return null;
            });
            Assertions.assertTrue(afterRefresh.await(5, TimeUnit.SECONDS));

            final Future<?> shutdown = executor.submit(luceneService::shutdown);
            final long shutdownDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!luceneService.isShutdown() && System.nanoTime() < shutdownDeadline) {
                Thread.yield();
            }
            Assertions.assertTrue(luceneService.isShutdown(), "shutdown did not establish its lifecycle boundary");
            Assertions.assertThrows(IllegalStateException.class, () -> luceneService.withSearcher(searcher -> null),
                    "shutdown must reject new reads before waiting for in-flight work");
            Assertions.assertFalse(shutdown.isDone(),
                    "shutdown must wait at the lifecycle lock while the synchronizer owns it");

            allowHookToReturn.countDown();
            synchronizer.get(5, TimeUnit.SECONDS);
            shutdown.get(5, TimeUnit.SECONDS);
            Assertions.assertEquals(0, readerBeforeSynchronization.getRefCount(),
                    "the reader replaced by synchronization must be closed");
            Assertions.assertEquals(0, refreshedReader.get().getRefCount(),
                    "the reader that became current must also be closed once shutdown tears the service down");

            final IllegalStateException exception = Assertions.assertThrows(
                    IllegalStateException.class, () -> luceneService.withSearcher(searcher -> null));
            Assertions.assertEquals("Lucene service is shut down", exception.getMessage());
        } finally {
            allowHookToReturn.countDown();
            executor.shutdownNow();
        }
    }

    // EXTRA: Bounds shutdown behind stalled lifecycle work and preserves retry after the stall clears.
    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void test_EXTRA_shutdownTimesOutWhenLifecycleWorkStallsAndCleanupCanBeRetried() throws Exception {
        final CountDownLatch synchronizerStalled = new CountDownLatch(1);
        final CountDownLatch releaseSynchronizer = new CountDownLatch(1);
        luceneService.setIndexSynchronizerBeforePublication(reader -> {
            synchronizerStalled.countDown();
            try {
                releaseSynchronizer.await();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interruptedException);
            }
        });

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<?> synchronizer = executor.submit(() -> {
                luceneService.synchronizeIndex();
                return null;
            });
            Assertions.assertTrue(synchronizerStalled.await(5, TimeUnit.SECONDS));

            final Future<?> shutdown = executor.submit(luceneService::shutdown);
            final ExecutionException failure = Assertions.assertThrows(
                    ExecutionException.class, () -> shutdown.get(2, TimeUnit.SECONDS));
            Assertions.assertInstanceOf(com.viveworks.appbase.core.AppbaseRuntimeException.class,
                    failure.getCause());
            Assertions.assertEquals("Timed out waiting for Lucene lifecycle operation to finish",
                    failure.getCause().getMessage());

            releaseSynchronizer.countDown();
            synchronizer.get(5, TimeUnit.SECONDS);
            luceneService.shutdown();
        } finally {
            releaseSynchronizer.countDown();
            executor.shutdownNow();
        }
    }

    // EXTRA: Ensures an unchecked close failure neither skips later resources nor prevents cleanup retry.
    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void test_EXTRA_shutdownContinuesAfterRuntimeCleanupFailureAndRetriesFailedResource() {
        luceneService.shutdown();
        final AtomicBoolean failAnalyzerOnce = new AtomicBoolean(true);
        final List<String> cleanupAttempts = new ArrayList<>();
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
                CoreTestFixtures.getReactiveReadBoundary()) {
            @Override
            boolean closeResource(final String resourceName, final ResourceCloser closer) {
                cleanupAttempts.add(resourceName);
                if ("analyzer".equals(resourceName) && failAnalyzerOnce.getAndSet(false)) {
                    return super.closeResource(resourceName, () -> {
                        throw new IllegalStateException("injected analyzer close failure");
                    });
                }
                return super.closeResource(resourceName, closer);
            }
        };

        luceneService.shutdown();
        Assertions.assertTrue(cleanupAttempts.contains("index writer"),
                "writer cleanup must still run after an unchecked analyzer failure");
        Assertions.assertTrue(cleanupAttempts.contains("directory reader"),
                "reader cleanup must still run after an unchecked analyzer failure");
        Assertions.assertTrue(cleanupAttempts.contains("directory"),
                "directory cleanup must still run after an unchecked analyzer failure");

        luceneService.shutdown();
        Assertions.assertEquals(2, cleanupAttempts.stream().filter("analyzer"::equals).count(),
                "a failed cleanup must remain retryable");
    }

    // EXTRA: Prevents scheduled cleaning from blocking the shared scheduler on lifecycle contention.
    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void test_EXTRA_scheduledCleanerDoesNotWaitForContendedLifecycleLock() throws Exception {
        final ReentrantLock lifecycleLock = new ReentrantLock();
        final AtomicBoolean cleaned = new AtomicBoolean();
        final IndexCleaner cleaner = new IndexCleaner() {
            @Override
            public void clean() {
                cleaned.set(true);
            }

            @Override
            public long getDeletedSequenceNumber() {
                return 0;
            }
        };
        final DefaultLuceneService.IndexCleanerSchedulable schedulable =
                new DefaultLuceneService.IndexCleanerSchedulable(
                        cleaner,
                        SchedulableConfiguration.builder().build(),
                        lifecycleLock,
                        new AtomicBoolean());
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        lifecycleLock.lock();
        try {
            executor.submit(schedulable::schedule).get(1, TimeUnit.SECONDS);
            Assertions.assertFalse(cleaned.get(), "a contended scheduled cleaner must skip rather than block");
        } finally {
            lifecycleLock.unlock();
            executor.shutdownNow();
        }
    }

    // EXTRA: Keeps ordinary scheduler contention nonblocking and out of synchronization error counts.
    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void test_EXTRA_scheduledSynchronizerDoesNotWaitOrRecordAnErrorForContention() throws Exception {
        final CountDownLatch lifecycleLockHeld = new CountDownLatch(1);
        final CountDownLatch releaseLifecycleLock = new CountDownLatch(1);
        luceneService.setIndexSynchronizerBeforePublication(reader -> {
            lifecycleLockHeld.countDown();
            try {
                releaseLifecycleLock.await();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interruptedException);
            }
        });
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<?> manualSynchronization = executor.submit(() -> {
                luceneService.synchronizeIndex();
                return null;
            });
            Assertions.assertTrue(lifecycleLockHeld.await(5, TimeUnit.SECONDS));
            final long errorsBeforeContention = luceneService.getIndexSynchronizerErrorCount();

            luceneService.synchronizeIndexIfIdle();

            Assertions.assertEquals(errorsBeforeContention, luceneService.getIndexSynchronizerErrorCount(),
                    "ordinary scheduler contention must not be recorded as a synchronization error");
            releaseLifecycleLock.countDown();
            manualSynchronization.get(5, TimeUnit.SECONDS);
        } finally {
            releaseLifecycleLock.countDown();
            executor.shutdownNow();
        }
    }

    // EXTRA: Stress-reproduces the original defect's shape -- concurrent withSearcher(...) reads racing
    // a tight-looped synchronizeIndex() refresh -- and proves it no longer throws AlreadyClosedException.
    // Before the SearcherManager-based fix, a reader thread calling getIndexSearcher()/getDirectoryReader()
    // could be handed a reference that synchronizeIndex() then closed out from under it while the search
    // was still in flight, exactly this shape but with manual reader lifecycle instead of acquire/release.
    @Test
    @SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:IllegalCatch"})
    void test_EXTRA_concurrentSearchesSurviveATightSynchronizeIndexLoopWithoutAlreadyClosedException()
            throws Exception
    {
        final int readerThreadCount = 8;
        final int refreshIterations = 200;
        final Query query = new TermQuery(new Term("body", "hello"));
        final AtomicBoolean stop = new AtomicBoolean(false);
        final List<Throwable> readerFailures = new CopyOnWriteArrayList<>();
        final AtomicInteger successfulSearches = new AtomicInteger();

        final ExecutorService readers = Executors.newFixedThreadPool(readerThreadCount);
        try {
            final List<Future<?>> readerFutures = new ArrayList<>();
            for (int i = 0; i < readerThreadCount; i++) {
                readerFutures.add(readers.submit(() -> {
                    while (!stop.get()) {
                        try {
                            luceneService.withSearcher(searcher -> {
                                final TopDocs topDocs = searcher.search(query, 10);
                                // Exercise the by-id fetch path too, against the same acquired searcher.
                                if (topDocs.scoreDocs.length > 0) {
                                    searcher.getIndexReader().document(topDocs.scoreDocs[0].doc);
                                }
                                return topDocs;
                            });
                            successfulSearches.incrementAndGet();
                        } catch (Throwable throwable) {
                            // Broad on purpose: this loop exists to detect ANY failure under the race
                            // (including an AssertionError or other Error from inside the searcher
                            // callback), not just checked/unchecked Exceptions, so narrowing the catch
                            // would defeat the test's own purpose.
                            readerFailures.add(throwable);
                        }
                    }
                }));
            }

            for (int i = 0; i < refreshIterations && readerFailures.isEmpty(); i++) {
                indexDocuments(luceneService, 1);
            }
            stop.set(true);

            for (final Future<?> readerFuture : readerFutures) {
                readerFuture.get(10, TimeUnit.SECONDS);
            }
        } finally {
            stop.set(true);
            readers.shutdownNow();
        }

        Assertions.assertTrue(readerFailures.isEmpty(),
                "expected zero reader failures across " + refreshIterations +
                        " refreshes, got: " + readerFailures);
        Assertions.assertTrue(successfulSearches.get() > 0, "expected at least one successful concurrent search");
    }
}
