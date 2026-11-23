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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.read.CancellableRead;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.UsageTrackingQueryCachingPolicy;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Default implementation of {@link LuceneService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultLuceneService implements LuceneService {
    private static final long SHUTDOWN_LOCK_TIMEOUT_SECONDS = 1L;
    private final Logger logger;
    private final MetricService metricService;
    private final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration;
    private final Path indexPath;
    private final boolean deleteIndexPathUponShutdown;
    private final Directory directory;
    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;
    private final AtomicInteger activeSearcherAcquisitions;
    private final AtomicReference<LRUQueryCache> lruQueryCacheRef;
    private final AtomicReference<QueryCachingPolicy> queryCachingPolicyRef;
    private final AtomicReference<IndexCleaner> indexCleanerRef;
    private final Analyzer analyzer;
    private final QueryBuilder queryBuilder;
    private final IndexCleanerSchedulable indexCleanerSchedulable;
    private final ScheduledFuture<Void> indexCleanerScheduledFuture;
    private final ScheduledFuture<Void> indexSynchronizerScheduledFuture;
    private final Timer indexSynchronizerRunTimer;
    private final Counter indexSynchronizerErrorCounter;
    private final ReentrantLock lifecycleLock;
    private final AtomicBoolean shutdown;
    private final AtomicBoolean shutdownCleanupComplete;
    private final ReactiveReadBoundary reactiveReadBoundary;
    private volatile Consumer<IndexReader> indexSynchronizerBeforePublication = ignored -> { };

    @SuppressWarnings("IllegalCatch")
    public DefaultLuceneService(final LogService logService,
                                final MetricService metricService,
                                final ValidationService validationService,
                                final MediatorService mediatorService,
                                final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration,
                                final ReactiveReadBoundary reactiveReadBoundary)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(defaultLuceneServiceConfiguration, "defaultLuceneServiceConfiguration");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");
        AppbaseUtil.validate(validationService, defaultLuceneServiceConfiguration);

        this.reactiveReadBoundary = reactiveReadBoundary;
        this.logger = logService.getLogger(getClass());
        logger.info("Creating {} using coniguration {}", getClass().getName(), defaultLuceneServiceConfiguration);

        this.metricService = metricService;
        this.defaultLuceneServiceConfiguration = defaultLuceneServiceConfiguration;
        this.indexCleanerRef = new AtomicReference<>();
        this.indexPath = Path.of(defaultLuceneServiceConfiguration.getIndexBaseDirectory());
        this.deleteIndexPathUponShutdown = defaultLuceneServiceConfiguration.isDeleteIndexPathUponShutdown();
        this.directory = newDirectory(defaultLuceneServiceConfiguration);
        this.indexWriter = newIndexWriter();
        try {
            indexWriter.commit();
        } catch (IOException ioe) {
            throw new AppbaseRuntimeException("Error making initial indexWriter commit", ioe);
        }
        this.lruQueryCacheRef = new AtomicReference<>();
        this.queryCachingPolicyRef = new AtomicReference<>();
        final LruQueryCacheConfiguration lruQueryCacheConfiguration =
                defaultLuceneServiceConfiguration.getLruQueryCacheConfiguration();
        if (lruQueryCacheConfiguration != null && lruQueryCacheConfiguration.isEnabled()) {
            logger.info("Creating {} with configuration {}", LRUQueryCache.class.getName(), lruQueryCacheConfiguration);
            final LRUQueryCache lruQueryCache = new LRUQueryCache(
                    lruQueryCacheConfiguration.getMaxQueries(), lruQueryCacheConfiguration.getMaxRamBytesUsed());
            this.lruQueryCacheRef.set(lruQueryCache);

            final UsageTrackingQueryCachingPolicy usageTrackingQueryCachingPolicy =
                    new UsageTrackingQueryCachingPolicy();
            this.queryCachingPolicyRef.set(usageTrackingQueryCachingPolicy);

            registerQueryCacheMetrics(lruQueryCache, metricService);
        }

        CharArraySet charArraySet = null;
        final List<String> standardAnalyzerStopWords = defaultLuceneServiceConfiguration.getStandardAnalyzerStopWords();
        if (standardAnalyzerStopWords != null && !standardAnalyzerStopWords.isEmpty()) {
            charArraySet = new CharArraySet(
                    standardAnalyzerStopWords.size(), defaultLuceneServiceConfiguration.isStopWordsIgnoreCase());
            for (final String stopWord : standardAnalyzerStopWords) {
                if (stopWord != null) {
                    charArraySet.add(stopWord);
                }
            }
        }

        if (charArraySet == null) {
            charArraySet = CharArraySet.EMPTY_SET;
        }
        final StandardAnalyzer standardAnalyzer = new StandardAnalyzer(charArraySet);
        standardAnalyzer.setMaxTokenLength(defaultLuceneServiceConfiguration.getStandardAnalyzerMaxTokenLength());
        this.analyzer = standardAnalyzer;
        this.queryBuilder = new QueryBuilder(analyzer);

        registerIndexWriterMetrics(indexWriter);
        registerDirectoryMetrics(directory, metricService);

        activeSearcherAcquisitions = new AtomicInteger();
        lifecycleLock = new ReentrantLock();
        shutdown = new AtomicBoolean(false);
        shutdownCleanupComplete = new AtomicBoolean(false);
        try {
            this.searcherManager = new SearcherManager(indexWriter, new MetricWiringSearcherFactory());
        } catch (IOException ioe) {
            throw new AppbaseRuntimeException("Error creating " + SearcherManager.class.getName(), ioe);
        }
        registerSearcherManagerMetrics();
        try {
            synchronizeIndex();
        } catch (LuceneServiceException lse) {
            throw new AppbaseRuntimeException("Error initializing index readers", lse);
        }

        indexSynchronizerRunTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "indexSynchronizer", "run", "timer"));
        indexSynchronizerErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "indexSynchronizer", "error", "counter"));
        final SchedulableConfiguration indexSynchronizerSchedulableConfiguration =
                defaultLuceneServiceConfiguration.getIndexSynchronizerSchedulableConfiguration();
        if (indexSynchronizerSchedulableConfiguration != null &&
                indexSynchronizerSchedulableConfiguration.isEnabled())
        {
            logger.info("Scheduling index synchronizer using: {}", indexSynchronizerSchedulableConfiguration);
            indexSynchronizerScheduledFuture = mediatorService
                    .getMediator()
                    .scheduleWithFixedDelay(
                            new Schedulable<>() {
                                @Override
                                public Void schedule() {
                                    synchronizeIndexIfIdle();
                                    return null;
                                }

                                @Override
                                public SchedulableConfiguration getSchedulableConfiguration() {
                                    return indexSynchronizerSchedulableConfiguration;
                                }
                            },
                            indexSynchronizerSchedulableConfiguration.getInitialDelay(),
                            indexSynchronizerSchedulableConfiguration.getInterval(),
                            indexSynchronizerSchedulableConfiguration.getTimeUnit());
        } else {
            indexSynchronizerScheduledFuture = null;
        }

        final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration =
                defaultLuceneServiceConfiguration.getTimestampBasedIndexCleanerConfiguration();
        if (timestampBasedIndexCleanerConfiguration != null && timestampBasedIndexCleanerConfiguration.isEnabled()) {
            logger.info("Using {} to schedule {}",
                    timestampBasedIndexCleanerConfiguration, TimestampBasedIndexCleaner.class.getName());

            final SchedulableConfiguration schedulableConfiguration =
                    timestampBasedIndexCleanerConfiguration.getSchedulableConfiguration();
            final IndexCleaner indexCleaner = new TimestampBasedIndexCleaner(
                    logService,
                    metricService,
                    this,
                    timestampBasedIndexCleanerConfiguration);
            indexCleanerRef.set(indexCleaner);

            this.indexCleanerSchedulable = new IndexCleanerSchedulable(
                    indexCleaner, schedulableConfiguration, lifecycleLock, shutdown);
            this.indexCleanerScheduledFuture = mediatorService
                    .getMediator()
                    .scheduleWithFixedDelay(
                            indexCleanerSchedulable,
                            schedulableConfiguration.getInitialDelay(),
                            schedulableConfiguration.getInterval(),
                            schedulableConfiguration.getTimeUnit());
        } else {
            this.indexCleanerSchedulable = null;
            this.indexCleanerScheduledFuture = null;
        }
    }

    @SuppressWarnings("FallThrough")
    private Directory newDirectory(final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration) {
        final DirectoryType directoryType = defaultLuceneServiceConfiguration.getDirectoryType();
        logger.info("Creating {} with type {}", Directory.class.getName(), directoryType);

        switch (directoryType) {
            case NIO:
                try {
                    return new NIOFSDirectory(indexPath);
                } catch (IOException ioe) {
                    throw new AppbaseRuntimeException("Failed to create " +
                            NIOFSDirectory.class.getName() +
                            " for path " +
                            indexPath,
                            ioe);
                }
            case MMAP:
                final MmapDirectoryConfiguration mmapDirectoryConfiguration =
                        defaultLuceneServiceConfiguration.getMmapDirectoryConfiguration();
                try {
                    final MMapDirectory mMapDirectory =
                            new MMapDirectory(indexPath, mmapDirectoryConfiguration.getMaxChunkSize());
                    mMapDirectory.setPreload(mmapDirectoryConfiguration.isPreload());
                    mMapDirectory.setUseUnmap(mmapDirectoryConfiguration.isUseUnmap());
                } catch (IOException ioe) {
                    throw new AppbaseRuntimeException("Failed to create " +
                            MMapDirectory.class.getName() +
                            " for path " +
                            indexPath,
                            ioe);
                }
            case ANY:
            default:
                try {
                    return FSDirectory.open(indexPath);
                } catch (IOException ioe) {
                    throw new AppbaseRuntimeException("Failed to create any " +
                            FSDirectory.class.getName() +
                            " for path " +
                            indexPath,
                            ioe);
                }
        }
    }

    private void registerDirectoryMetrics(final Directory directory, final MetricService metricService) {
        logger.info("Registering {} metric", Directory.class.getName());

        final Gauge<Long> gauge = () -> {
            long totalBytesUsed = 0L;
            try {
                final String[] files = directory.listAll();
                for (String file : files) {
                    totalBytesUsed += directory.fileLength(file);
                }
            } catch (IOException ioe) {
                logger.error("Error calculating directory bytes used", ioe);
            }

            return totalBytesUsed;
        };
        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "directory", "bytesUsed", "gauge"),
                        gauge);
    }

    private void registerQueryCacheMetrics(final LRUQueryCache lruQueryCache, final MetricService metricService) {
        logger.info("Registering {} metric", LRUQueryCache.class.getName());

        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "lruQueryCache", "cacheCount", "gauge"),
                        (Gauge<Long>) lruQueryCache::getCacheCount);

        metricService
                .getMetricRegistry()
                .register(
                    metricService.name(
                        getClass(), indexPath.getFileName().toString(), "lruQueryCache", "cacheSize", "gauge"),
                        (Gauge<Long>) lruQueryCache::getCacheSize);

        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(),
                                indexPath.getFileName().toString(),
                                "lruQueryCache",
                                "evictionCount",
                                "gauge"),
                        (Gauge<Long>) lruQueryCache::getEvictionCount);

        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "lruQueryCache", "hitCount", "gauge"),
                        (Gauge<Long>) lruQueryCache::getHitCount);

        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "lruQueryCache", "missCount", "gauge"),
                        (Gauge<Long>) lruQueryCache::getMissCount);

        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "lruQueryCache", "totalCount", "gauge"),
                        (Gauge<Long>) lruQueryCache::getTotalCount);


        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(),
                                indexPath.getFileName().toString(),
                                "lruQueryCache",
                                "ramBytesUsed",
                                "gauge"),
                        (Gauge<Long>) lruQueryCache::ramBytesUsed);
    }

    public IndexCleaner getIndexCleaner() {
        return indexCleanerRef.get();
    }

    @Override
    public Path getIndexPath() {
        return indexPath;
    }

    @Override
    public Directory getDirectory() {
        return directory;
    }

    @Override
    public IndexWriter getIndexWriter() {
        ensureRunning();
        return indexWriter;
    }

    @Override
    public IndexWriter getIndexWriter(final boolean commit) throws LuceneServiceException {
        ensureRunning();
        if (commit) {
            try {
                indexWriter.flush();
                indexWriter.commit();
            } catch (IOException ioe) {
                throw new LuceneServiceException("Error while committing index writer", ioe);
            }
        }

        return indexWriter;
    }

    private IndexWriter newIndexWriter() {
        logger.info("Creating {}", IndexWriter.class.getName());

        final IndexWriterConfig indexWriterConfig = new IndexWriterConfig();
        indexWriterConfig.setCheckPendingFlushUpdate(defaultLuceneServiceConfiguration.isCheckPendingFlushUpdate());
        indexWriterConfig.setCommitOnClose(defaultLuceneServiceConfiguration.isCommitOnClose());
        indexWriterConfig.setMaxBufferedDocs(defaultLuceneServiceConfiguration.getMaxBufferedDocs());
        indexWriterConfig.setOpenMode(defaultLuceneServiceConfiguration.getOpenMode());
        indexWriterConfig.setRAMBufferSizeMB(defaultLuceneServiceConfiguration.getRamBufferSizeMB());
        indexWriterConfig.setRAMPerThreadHardLimitMB(defaultLuceneServiceConfiguration.getRamPerThreadHardLimitMB());
        indexWriterConfig.setReaderPooling(defaultLuceneServiceConfiguration.isReaderPooling());
        indexWriterConfig.setUseCompoundFile(defaultLuceneServiceConfiguration.isUseCompoundFile());
        try {
            return new IndexWriter(directory, indexWriterConfig);
        } catch (IOException ioe) {
            throw new AppbaseRuntimeException("Failed to create " + IndexWriter.class.getName(), ioe);
        }
    }

    private void registerIndexWriterMetrics(final IndexWriter indexWriter) {
        logger.info("Registering {} metric", IndexWriter.class.getName());

        final String docStatsMaxDocGaugeName =
                metricService.name(getClass(), indexPath.getFileName().toString(), "docStats", "maxDoc", "gauge");
        metricService
                .getMetricRegistry()
                .register(
                        docStatsMaxDocGaugeName,
                        (Gauge<Integer>) () -> {
                            final IndexWriter.DocStats docStats = indexWriter.getDocStats();
                            return docStats.maxDoc;
                        }
                );

        final String docStatsNumDocsGaugeName =
                metricService.name(getClass(), indexPath.getFileName().toString(), "docStats", "numDocs", "gauge");
        metricService
                .getMetricRegistry()
                .register(
                        docStatsNumDocsGaugeName,
                        (Gauge<Integer>) () -> {
                            final IndexWriter.DocStats docStats = indexWriter.getDocStats();
                            return docStats.numDocs;
                        });

        final String flushingBytesGaugeName =
                metricService.name(getClass(), indexPath.getFileName().toString(), "flushingBytes", "gauge");
        metricService
                .getMetricRegistry()
                .register(
                        flushingBytesGaugeName,
                        (Gauge<Long>) indexWriter::getFlushingBytes);

        final String maxCompletedSequenceNumberGaugeName =
                metricService.name(
                        getClass(), indexPath.getFileName().toString(), "maxCompletedSequenceNumber", "gauge");
        metricService
                .getMetricRegistry()
                .register(
                        maxCompletedSequenceNumberGaugeName,
                        (Gauge<Long>) indexWriter::getMaxCompletedSequenceNumber);

        final String ramBytesUsedGaugeName =
                metricService.name(getClass(), indexPath.getFileName().toString(), "ramBytesUsed", "gauge");
        metricService
                .getMetricRegistry()
                .register(
                        ramBytesUsedGaugeName,
                        (Gauge<Long>) indexWriter::ramBytesUsed);

        final String numRamDocsGaugeName =
                metricService.name(getClass(), indexPath.getFileName().toString(), "numRamDocs", "gauge");
        metricService
                .getMetricRegistry()
                .register(
                        numRamDocsGaugeName,
                        (Gauge<Integer>) indexWriter::numRamDocs);
    }

    @Override
    public void synchronizeIndex() throws LuceneServiceException {
        lifecycleLock.lock();
        try {
            if (shutdown.get()) {
                throw new LuceneServiceException("Lucene service is shut down");
            }
            logger.trace("Committing {}", indexWriter.getClass().getName());
            indexWriter.flush();
            indexWriter.commit();

            logger.trace("Refreshing {} from {}", SearcherManager.class.getName(), indexWriter.getClass().getName());
            searcherManager.maybeRefreshBlocking();

            if (shutdown.get()) {
                return;
            }

            final IndexSearcher refreshed = searcherManager.acquire();
            try {
                indexSynchronizerBeforePublication.accept(refreshed.getIndexReader());
            } finally {
                searcherManager.release(refreshed);
            }
        } catch (IOException ioe) {
            throw new LuceneServiceException("Failed to refresh " + SearcherManager.class.getName(), ioe);
        } finally {
            lifecycleLock.unlock();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void synchronizeIndexIfIdle() {
        if (!lifecycleLock.tryLock()) {
            logger.debug("Skipping scheduled index synchronization because a lifecycle operation is running");
            return;
        }
        try {
            if (shutdown.get()) {
                return;
            }
            final Timer.Context context = indexSynchronizerRunTimer.time();
            try {
                logger.debug("Running index synchronizer");
                synchronizeIndex();
            } finally {
                context.stop();
            }
        } catch (Exception exception) {
            // Broad on purpose: this runs on the shared scheduler, which must never die from one bad
            // refresh (a RuntimeException from the configured indexSynchronizerBeforePublication hook,
            // or any other failure synchronizeIndex() propagates unchecked).
            indexSynchronizerErrorCounter.inc();
            logger.error("Error while synchronizing index", exception);
        } finally {
            lifecycleLock.unlock();
        }
    }

    long getIndexSynchronizerErrorCount() {
        return indexSynchronizerErrorCounter.getCount();
    }

    /**
     * Registers reader-shaped metrics once, backed by a short acquire/release against
     * {@link #searcherManager} each time a gauge is polled, rather than re-registering against a new
     * reader instance on every {@link #synchronizeIndex()} refresh.
     */
    private void registerSearcherManagerMetrics() {
        logger.trace("Registering {} metric", SearcherManager.class.getName());

        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "directoryReader", "maxDoc", "gauge"),
                        (Gauge<Integer>) () -> withAcquiredReaderOrDefault(IndexReader::maxDoc, 0));
        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(),
                                indexPath.getFileName().toString(),
                                "directoryReader",
                                "numDeletedDocs",
                                "gauge"),
                        (Gauge<Integer>) () -> withAcquiredReaderOrDefault(IndexReader::numDeletedDocs, 0));
        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(),
                                indexPath.getFileName().toString(),
                                "directoryReader",
                                "numDocs",
                                "gauge"),
                        // Preserves the pre-existing behavior of this gauge, which reported numDeletedDocs
                        // under the "numDocs" name rather than IndexReader::numDocs.
                        (Gauge<Integer>) () -> withAcquiredReaderOrDefault(IndexReader::numDeletedDocs, 0));
        metricService
                .getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), indexPath.getFileName().toString(), "directoryReader", "refCount", "gauge"),
                        // No longer the raw Lucene reader refcount (there is no single long-lived reader to
                        // count references on anymore) -- reports how many withSearcher(...) reads currently
                        // hold an acquired searcher.
                        (Gauge<Integer>) activeSearcherAcquisitions::get);
    }

    private int withAcquiredReaderOrDefault(final java.util.function.ToIntFunction<IndexReader> read,
                                             final int defaultValue)
    {
        if (shutdown.get()) {
            return defaultValue;
        }
        try {
            final IndexSearcher searcher = searcherManager.acquire();
            try {
                return read.applyAsInt(searcher.getIndexReader());
            } finally {
                searcherManager.release(searcher);
            }
        } catch (IOException | AlreadyClosedException exception) {
            return defaultValue;
        }
    }

    @Override
    public <T> T withSearcher(final LuceneSearcherRead<T> read) throws IOException {
        AppbaseUtil.notNull(read, "read");
        ensureRunning();

        final IndexSearcher searcher;
        try {
            searcher = searcherManager.acquire();
        } catch (AlreadyClosedException alreadyClosedException) {
            throw new LuceneServiceShutdownException();
        }
        activeSearcherAcquisitions.incrementAndGet();
        try {
            return read.read(searcher);
        } finally {
            activeSearcherAcquisitions.decrementAndGet();
            searcherManager.release(searcher);
        }
    }

    void setIndexSynchronizerBeforePublication(final Consumer<IndexReader> hook) {
        AppbaseUtil.notNull(hook, "hook");
        indexSynchronizerBeforePublication = hook;
    }

    private void ensureRunning() {
        if (shutdown.get()) {
            throw new LuceneServiceShutdownException();
        }
    }

    @Override
    public QueryBuilder getQueryBuilder() {
        ensureRunning();
        return queryBuilder;
    }

    @Override
    public QueryParser getQueryParser(final String fieldName) {
        AppbaseUtil.notNull(fieldName, "fieldName");
        ensureRunning();

        return new QueryParser(fieldName, analyzer);
    }

    @Override
    public Mono<TopDocs> searchReactive(final Query query, final int maxHits) {
        AppbaseUtil.notNull(query, "query");

        return reactiveReadBoundary.read(() -> withSearcher(searcher -> searcher.search(query, maxHits)));
    }

    @Override
    public Mono<Document> documentReactive(final int docId) {
        return reactiveReadBoundary.read(
                () -> withSearcher(searcher -> searcher.getIndexReader().storedFields().document(docId)));
    }

    @Override
    public <T> Mono<T> readReactive(final CancellableRead<T> cancellableRead) {
        AppbaseUtil.notNull(cancellableRead, "cancellableRead");
        return reactiveReadBoundary.read(cancellableRead);
    }

    public IndexCleanerSchedulable getIndexCleanerSchedulable() {
        return indexCleanerSchedulable;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (shutdownCleanupComplete.get()) {
            return;
        }
        final boolean firstShutdown = shutdown.compareAndSet(false, true);
        if (firstShutdown) {
            logger.info("Shutting down: {}", getClass().getName());
            if (indexCleanerScheduledFuture != null) {
                indexCleanerScheduledFuture.cancel(false);
            }
            if (indexSynchronizerScheduledFuture != null) {
                indexSynchronizerScheduledFuture.cancel(false);
            }
        }

        try {
            if (!lifecycleLock.tryLock(SHUTDOWN_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AppbaseRuntimeException("Timed out waiting for Lucene lifecycle operation to finish");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AppbaseRuntimeException("Interrupted while waiting for Lucene lifecycle operation to finish",
                    interruptedException);
        }
        try {
            if (shutdownCleanupComplete.get()) {
                return;
            }
            boolean cleanupComplete = closeResource("analyzer", analyzer::close);

            // Closed before the writer it wraps: outstanding withSearcher(...) acquisitions stay valid
            // (SearcherManager defers the actual reader close until each is released) and no new
            // acquire()/refresh is accepted from this point on.
            cleanupComplete &= closeResource("directory reader", searcherManager::close);

            if (indexWriter.isOpen()) {
                cleanupComplete &= closeResource("index writer", indexWriter::close);
            }

            cleanupComplete &= closeResource("directory", directory::close);

            if (deleteIndexPathUponShutdown) {
                cleanupComplete &= closeResource("index path " + indexPath, () -> IOUtils.rm(indexPath));
            }
            shutdownCleanupComplete.set(cleanupComplete);
        } finally {
            lifecycleLock.unlock();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    boolean closeResource(final String resourceName, final ResourceCloser closer) {
        try {
            // Broad on purpose: ResourceCloser is generic over every resource shutdown() closes, each
            // of which throws a different checked exception; this must capture any of them so the
            // remaining resources still get a chance to close.
            closer.close();
            return true;
        } catch (Exception exception) {
            logger.error("Error while shutting down {}", resourceName, exception);
            return false;
        }
    }

    @FunctionalInterface
    interface ResourceCloser {
        void close() throws Exception;
    }

    static class IndexCleanerSchedulable implements Schedulable<Void> {
        private final IndexCleaner indexCleaner;
        private final SchedulableConfiguration schedulableConfiguration;
        private final ReentrantLock lifecycleLock;
        private final AtomicBoolean shutdown;

        IndexCleanerSchedulable(final IndexCleaner indexCleaner,
                                final SchedulableConfiguration schedulableConfiguration,
                                final ReentrantLock lifecycleLock,
                                final AtomicBoolean shutdown)
        {
            AppbaseUtil.notNull(indexCleaner, "indexCleaner");
            AppbaseUtil.notNull(schedulableConfiguration, "schedulableConfiguration");

            this.indexCleaner = indexCleaner;
            this.schedulableConfiguration = schedulableConfiguration;
            this.lifecycleLock = lifecycleLock;
            this.shutdown = shutdown;
        }

        @Override
        public Void schedule() {
            if (!lifecycleLock.tryLock()) {
                return null;
            }
            try {
                if (!shutdown.get()) {
                    indexCleaner.clean();
                }
            } finally {
                lifecycleLock.unlock();
            }
            return null;
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            return schedulableConfiguration;
        }
    }

    /**
     * Wires {@link #lruQueryCacheRef} / {@link #queryCachingPolicyRef} onto every {@link IndexSearcher}
     * {@link #searcherManager} creates on refresh, matching what the former manual
     * {@code new IndexSearcher(directoryReader)} construction in {@code synchronizeIndex()} did for each
     * reader it opened directly.
     */
    private final class MetricWiringSearcherFactory extends SearcherFactory {
        @Override
        public IndexSearcher newSearcher(final IndexReader reader, final IndexReader previousReader)
                throws IOException
        {
            final IndexSearcher indexSearcher = super.newSearcher(reader, previousReader);

            final LRUQueryCache lruQueryCache = lruQueryCacheRef.get();
            if (lruQueryCache != null) {
                indexSearcher.setQueryCache(lruQueryCache);
            }

            final QueryCachingPolicy queryCachingPolicy = queryCachingPolicyRef.get();
            if (queryCachingPolicy != null) {
                indexSearcher.setQueryCachingPolicy(queryCachingPolicy);
            }

            return indexSearcher;
        }
    }
}
