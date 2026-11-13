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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.accessor.ThrowableRecord;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreException;
import com.viveworks.appbase.core.service.log.store.criteria.LogLevelCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.ExactTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.RangeTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.SetTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.TimestampCriteria;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.util.lucene.LuceneServiceException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * {@link LogStore} implementation that uses {@link LuceneLogIndex}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
@ThreadSafe
@SuppressWarnings("checkstyle:IllegalCatch")
public class LuceneLogStore implements LogStore {
    private final Logger logger;
    private final LuceneLogIndex luceneLogIndex;
    private final LuceneLogStoreConfiguration luceneLogStoreConfiguration;
    private final ExecutorService executorService;
    private final MetricService metricService;

    private final Timer readLogRecordsBetweenTimer;
    private final Timer readLogRecordsAtTimer;
    private final Timer readLogRecordsByCriteriaTimer;
    private final Timer writeLogRecordTimer;
    private final Timer removeLogRecordsBetweenTimer;
    private final Timer removeLogRecordsAtTimer;
    private final Timer removeLogRecordsByCriteriaTimer;

    private final Timer asyncReadLogRecordsBetweenTimer;
    private final Timer asyncReadLogRecordsAtTimer;
    private final Timer asyncReadLogRecordsByCriteriaTimer;
    private final Timer asyncWriteLogRecordTimer;
    private final Timer asyncRemoveLogRecordsBetweenTimer;
    private final Timer asyncRemoveLogRecordsAtTimer;
    private final Timer asyncRemoveLogRecordsByCriteriaTimer;

    private final Counter readLogRecordsBetweenErrorCounter;
    private final Counter readLogRecordsAtErrorCounter;
    private final Counter readLogRecordsByCriteriaErrorCounter;
    private final Counter writeLogRecordErrorCounter;
    private final Counter removeLogRecordsBetweenErrorCounter;
    private final Counter removeLogRecordsAtErrorCounter;
    private final Counter removeLogRecordsByCriteriaErrorCounter;

    private final Counter asyncReadLogRecordsBetweenErrorCounter;
    private final Counter asyncReadLogRecordsAtErrorCounter;
    private final Counter asyncReadLogRecordsByCriteriaErrorCounter;
    private final Counter asyncWriteLogRecordErrorCounter;
    private final Counter asyncRemoveLogRecordsBetweenErrorCounter;
    private final Counter asyncRemoveLogRecordsAtErrorCounter;
    private final Counter asyncRemoveLogRecordsByCriteriaErrorCounter;

    private final AtomicBoolean shutdown;

    public LuceneLogStore(final LogService logService,
                          final MetricService metricService,
                          final LuceneLogIndex luceneLogIndex,
                          final ExecutorService executorService,
                          final LuceneLogStoreConfiguration luceneLogStoreConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(luceneLogIndex, "luceneLogIndex");
        AppbaseUtil.notNull(executorService, "executorService");
        AppbaseUtil.notNull(luceneLogStoreConfiguration, "luceneLogStoreConfiguration");
        AppbaseUtil.notNull(
                luceneLogIndex.getLuceneLogIndexConfiguration(),
                "luceneLogIndex.getLuceneLogIndexConfiguration()");

        this.logger = logService.getLogger(getClass());
        this.executorService = executorService;
        this.luceneLogIndex = luceneLogIndex;
        this.luceneLogStoreConfiguration = luceneLogStoreConfiguration;
        this.metricService = metricService;

        readLogRecordsBetweenTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readLogRecordsBetween", "timer"));
        readLogRecordsAtTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readLogRecordsAt", "timer"));
        readLogRecordsByCriteriaTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readLogRecordsByCriteria", "timer"));
        writeLogRecordTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "writeLogRecord", "timer"));
        removeLogRecordsBetweenTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeLogRecordsBetween", "timer"));
        removeLogRecordsAtTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeLogRecordsAt", "timer"));
        removeLogRecordsByCriteriaTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeLogRecordsByCriteria", "timer"));

        asyncReadLogRecordsBetweenTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadLogRecordsBetweenTimer", "timer"));
        asyncReadLogRecordsAtTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadLogRecordsAtTimer", "timer"));
        asyncReadLogRecordsByCriteriaTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadLogRecordsByCriteriaTimer", "timer"));
        asyncWriteLogRecordTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncWriteLogRecordTimer", "timer"));
        asyncRemoveLogRecordsBetweenTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveLogRecordsBetweenTimer", "timer"));
        asyncRemoveLogRecordsAtTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveLogRecordsAtTimer", "timer"));
        asyncRemoveLogRecordsByCriteriaTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveLogRecordsByCriteriaTimer", "timer"));

        readLogRecordsBetweenErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readLogRecordsBetween", "error", "counter"));
        readLogRecordsAtErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readLogRecordsAt", "error", "counter"));
        readLogRecordsByCriteriaErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readLogRecordsByCriteria", "error", "counter"));
        removeLogRecordsBetweenErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removeLogRecordsBetween", "error", "counter"));
        writeLogRecordErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "writeLogRecord", "error", "counter"));
        removeLogRecordsAtErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removeLogRecordsAt", "error", "counter"));
        removeLogRecordsByCriteriaErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removeLogRecordsByCriteria", "error", "counter"));

        asyncReadLogRecordsBetweenErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadLogRecordsBetweenErrorCounter", "error", "counter"));
        asyncReadLogRecordsAtErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadLogRecordsAtErrorCounter", "error", "counter"));
        asyncReadLogRecordsByCriteriaErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadLogRecordsByCriteriaErrorCounter", "error", "counter"));
        asyncWriteLogRecordErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncWriteLogRecordErrorCounter", "error", "counter"));
        asyncRemoveLogRecordsBetweenErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncRemoveLogRecordsBetweenErrorCounter", "error", "counter"));
        asyncRemoveLogRecordsAtErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncRemoveLogRecordsAtErrorCounter", "error", "counter"));
        asyncRemoveLogRecordsByCriteriaErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncRemoveLogRecordsByCriteriaErrorCounter", "error", "counter"));

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Collection<LogRecord> readLogRecordsBetween(
            long inclusiveStartTimestamp, long exclusiveEndTimestamp, int pageSize) throws LogStoreException
    {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final Timer.Context context = readLogRecordsBetweenTimer.time();
        try {
            final LuceneService luceneService = luceneLogIndex.getLuceneService();
            final String timestampLongPointName =
                    luceneLogIndex.getLuceneLogIndexConfiguration().getTimestampLongPointName();
            final Query query = LongPoint.newRangeQuery(
                    timestampLongPointName, inclusiveStartTimestamp, Math.addExact(exclusiveEndTimestamp, -1));
            return luceneService.withSearcher(
                    searcher -> readLogRecords(searcher.search(query, pageSize), searcher.getIndexReader()));
        } catch (Exception ex) {
            final String message = String.format(
                    "Error in readLogRecordsBetween [inclusiveStartTimestamp=%s, exclusiveEndTimestamp=%s]",
                    inclusiveStartTimestamp, exclusiveEndTimestamp);
            readLogRecordsBetweenErrorCounter.inc();
            throw new LogStoreException(message, ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsBetween(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize)
    {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final CompletableFuture<Collection<LogRecord>> resultFuture = new CompletableFuture<>();

        final Timer.Context context = asyncReadLogRecordsBetweenTimer.time();
        executorService.submit(() -> {
            try {
                final Collection<LogRecord> logRecords =
                        readLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize);
                resultFuture.complete(logRecords);
            } catch (Exception ex) {
                logger.error(
                        "Error in asyncReadLogRecordsBetween [inclusiveStartTimestamp={}, exclusiveEndTimestamp={}]",
                        inclusiveStartTimestamp, exclusiveEndTimestamp, ex);
                asyncReadLogRecordsBetweenErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<LogRecord> readLogRecordsAt(final long timestamp, final int pageSize) throws LogStoreException {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final Timer.Context context = readLogRecordsAtTimer.time();
        try {
            final LuceneService luceneService = luceneLogIndex.getLuceneService();
            final String timestampLongPointName =
                    luceneLogIndex.getLuceneLogIndexConfiguration().getTimestampLongPointName();
            final Query query = LongPoint.newExactQuery(timestampLongPointName, timestamp);
            return luceneService.withSearcher(
                    searcher -> readLogRecords(searcher.search(query, pageSize), searcher.getIndexReader()));
        } catch (Exception ex) {
            final String message = String.format("Error in readLogRecordsAt [timestamp=%s]", timestamp);
            readLogRecordsAtErrorCounter.inc();
            throw new LogStoreException(message, ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsAt(final long timestamp, final int pageSize) {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final CompletableFuture<Collection<LogRecord>> resultFuture = new CompletableFuture<>();

        final Timer.Context context = asyncReadLogRecordsAtTimer.time();
        executorService.submit(() -> {
            try {
                final Collection<LogRecord> logRecords = readLogRecordsAt(timestamp, pageSize);
                resultFuture.complete(logRecords);
            } catch (Exception ex) {
                logger.error("Error in asyncReadLogRecordsAt [timestamp={}]", timestamp, ex);
                asyncReadLogRecordsAtErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<LogRecord> readLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria,
            final int pageSize)
            throws LogStoreException
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final Timer.Context context = readLogRecordsByCriteriaTimer.time();
        try {
            final LuceneService luceneService = luceneLogIndex.getLuceneService();
            final Query query = toQuery(logRecordCriteria);
            return luceneService.withSearcher(
                    searcher -> readLogRecords(searcher.search(query, pageSize), searcher.getIndexReader()));
        } catch (Exception ex) {
            final String message = String.format(
                    "Error in readLogRecordsByCriteria [logRecordCriteria=%s]", logRecordCriteria);
            readLogRecordsByCriteriaErrorCounter.inc();
            throw new LogStoreException(message, ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<LogRecord>> asyncReadLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria, final int pageSize)
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final Timer.Context context = asyncReadLogRecordsByCriteriaTimer.time();
        final CompletableFuture<Collection<LogRecord>> resultFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                final Collection<LogRecord> logRecords = readLogRecordsByCriteria(logRecordCriteria, pageSize);
                resultFuture.complete(logRecords);
            } catch (Exception ex) {
                logger.error("Error in asyncReadLogRecordsByCriteria [logRecordCriteria={}]", logRecordCriteria, ex);
                asyncReadLogRecordsByCriteriaErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void writeLogRecord(final LogRecord logRecord) throws LogStoreException {
        AppbaseUtil.notNull(logRecord, "logRecord");

        final Timer.Context context = writeLogRecordTimer.time();
        try {
            luceneLogIndex.indexLogRecord(logRecord);
        } catch (Exception ex) {
            final String message = String.format("Error in writeLogRecord [logRecord=%s]", logRecord);
            logger.error(message, ex);
            writeLogRecordErrorCounter.inc();
            throw new LogStoreException(message, ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncWriteLogRecord(final LogRecord logRecord) {
        AppbaseUtil.notNull(logRecord, "logRecord");

        final Timer.Context context = asyncWriteLogRecordTimer.time();
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                luceneLogIndex.indexLogRecord(logRecord);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in asyncWriteLogRecord [logRecord={}]", logRecord, ex);
                asyncWriteLogRecordErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp)
            throws LogStoreException
    {
        final Timer.Context context = removeLogRecordsBetweenTimer.time();
        try {
            final LuceneService luceneService = luceneLogIndex.getLuceneService();
            final String timestampLongPointName =
                    luceneLogIndex.getLuceneLogIndexConfiguration().getTimestampLongPointName();
            final Query query = LongPoint.newRangeQuery(
                    timestampLongPointName, inclusiveStartTimestamp, Math.addExact(exclusiveEndTimestamp, -1));
            luceneService.getIndexWriter().deleteDocuments(query);
        } catch (Exception ex) {
            final String message = String.format(
                    "Error in removeLogRecordsBetween [inclusiveStartTimestamp=%s, exclusiveEndTimestamp=%s]",
                    inclusiveStartTimestamp, exclusiveEndTimestamp);
            logger.error(message, ex);
            removeLogRecordsBetweenErrorCounter.inc();
            throw new LogStoreException(message, ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveLogRecordsBetween(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp)
    {
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        final Timer.Context context = asyncRemoveLogRecordsBetweenTimer.time();
        executorService.submit(() -> {
            try {
                removeLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in asyncRemoveLogRecordsBetween [inclusiveStartTimestamp={}, exclusiveEndTimestamp={}]",
                        inclusiveStartTimestamp, exclusiveEndTimestamp, ex);
                removeLogRecordsBetweenErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeLogRecordsAt(final long timestamp) throws LogStoreException {
        final Timer.Context context = removeLogRecordsAtTimer.time();

        try {
            final LuceneService luceneService = luceneLogIndex.getLuceneService();
            final String timestampLongPointName =
                    luceneLogIndex.getLuceneLogIndexConfiguration().getTimestampLongPointName();
            final Query query = LongPoint.newExactQuery(timestampLongPointName, timestamp);
            luceneService.getIndexWriter().deleteDocuments(query);
        } catch (Exception ex) {
            final String message = String.format("Error in removeLogRecordsAt [timestamp=%s]", timestamp);
            logger.error(message, ex);
            removeLogRecordsAtErrorCounter.inc();
            throw new LogStoreException(message, ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveLogRecordsAt(final long timestamp) {
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        final Timer.Context context = asyncRemoveLogRecordsAtTimer.time();
        executorService.submit(() -> {
            try {
                removeLogRecordsAt(timestamp);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in asyncRemoveLogRecordsAt [timestamp={}]", timestamp, ex);
                asyncRemoveLogRecordsAtErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) throws LogStoreException {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");

        final Timer.Context context = removeLogRecordsByCriteriaTimer.time();
        try {
            final LuceneService luceneService = luceneLogIndex.getLuceneService();
            final Query query = toQuery(logRecordCriteria);
            luceneService.getIndexWriter().deleteDocuments(query);
        } catch (Exception ex) {
            logger.error("Error in removeLogRecordsByCriteria [logRecordCriteria={}]", logRecordCriteria, ex);
            removeLogRecordsByCriteriaErrorCounter.inc();
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveLogRecordsByCriteria(final LogRecordCriteria logRecordCriteria) {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        final Timer.Context context = asyncRemoveLogRecordsByCriteriaTimer.time();
        executorService.submit(() -> {
            try {
                removeLogRecordsByCriteria(logRecordCriteria);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in asyncRemoveLogRecordsByCriteria [logRecordCriteria={}]", logRecordCriteria, ex);
                asyncRemoveLogRecordsByCriteriaErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    private Collection<LogRecord> readLogRecords(final TopDocs topDocs, final IndexReader indexReader)
            throws IOException
    {
        return readLogRecords(topDocs, indexReader, () -> false);
    }

    /**
     * Fetch {@link LogRecord}s for the given {@link TopDocs}, checking {@code cancelled} between documents
     * and aborting cooperatively — never via {@link Thread#interrupt()}, which would raise
     * {@code ClosedByInterruptException} against the underlying {@code NIOFSDirectory} and close it.
     *
     * <p>{@code indexReader} must be the same index generation {@code topDocs} was searched against — the
     * caller should obtain both from the one {@link LuceneService#withSearcher(LuceneSearcherRead)}
     * invocation (e.g. {@code searcher.search(...)} and {@code searcher.getIndexReader()}), not from two
     * separate calls, which could observe different generations if a refresh lands between them.
     *
     * @param topDocs Search hits to fetch documents for
     * @param indexReader Reader to fetch stored documents from — the same generation {@code topDocs} was
     *      searched against
     * @param cancelled Supplies {@code true} once the caller has abandoned this read
     * @return the fetched log records, possibly fewer than {@code topDocs} if cancelled mid-fetch
     * @throws IOException if a document fetch fails
     */
    Collection<LogRecord> readLogRecords(final TopDocs topDocs,
                                         final IndexReader indexReader,
                                         final BooleanSupplier cancelled)
            throws IOException
    {
        final List<LogRecord> logRecords = new ArrayList<>();

        if (topDocs != null) {
            final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            if (scoreDocs != null) {
                for (final ScoreDoc scoreDoc : scoreDocs) {
                    if (cancelled.getAsBoolean()) {
                        break;
                    }
                    if (scoreDoc != null) {
                        final int documentId = scoreDoc.doc;
                        final Document document = indexReader.document(documentId);
                        if (document != null) {
                            final LogRecord logRecord = toLogRecord(document);
                            logRecords.add(logRecord);
                        }
                    }
                }
            }
        }

        return logRecords;
    }

    LogRecord toLogRecord(final Document document) {
        final LogRecord.Builder logRecordBuilder = LogRecord.builder();
        final ThrowableRecord.Builder throwableRecordBuilder = ThrowableRecord.builder();
        final ThrowableRecord.Builder throwableCauseRecordBuilder = ThrowableRecord.builder();
        final Map<String, String> mdcPropertyMap = new ConcurrentHashMap<>();

        final List<IndexableField> fields = document.getFields();
        if (fields != null) {
            final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                    luceneLogIndex.getLuceneLogIndexConfiguration();

            for (final IndexableField indexableField : fields) {
                final String name = indexableField.name();
                if (name.equals(luceneLogIndexConfiguration.getTimestampStoredFieldName())) {
                    final Number value = indexableField.numericValue();
                    if (value != null) {
                        logRecordBuilder.timestamp(value.longValue());
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThreadNameFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.threadName(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getLogLevelFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.logLevel(LogLevel.valueOf(value));
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getMessageFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.message(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getFormattedMessageFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.formattedMessage(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getLoggerNameFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.loggerName(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getMarkerFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.markerName(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThrowableMessageFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        throwableRecordBuilder.message(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThrowableTypeNameFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        throwableRecordBuilder.className(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThrowableStackTraceFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        throwableRecordBuilder.stackTrace(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThrowableCauseMessageFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        throwableCauseRecordBuilder.message(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        throwableCauseRecordBuilder.className(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        throwableCauseRecordBuilder.stackTrace(value);
                    }
                    continue;
                }
                if (name.equals(luceneLogIndexConfiguration.getCallerStackFieldName())) {
                    final String value = indexableField.stringValue();
                    if (value != null) {
                        logRecordBuilder.callerData(value);
                    }
                    continue;
                } else {
                    String fieldName = null;
                    final String mdcFieldNamePrefix = luceneLogIndexConfiguration.getMdcFieldNamePrefix();
                    if (mdcFieldNamePrefix != null &&
                            !mdcFieldNamePrefix.isEmpty() &&
                            name.startsWith(mdcFieldNamePrefix))
                    {
                        fieldName = name.substring(mdcFieldNamePrefix.length());
                    }

                    final String mdcFieldNameSuffix = luceneLogIndexConfiguration.getMdcFieldNameSuffix();
                    if (mdcFieldNameSuffix != null &&
                            !mdcFieldNameSuffix.isEmpty() &&
                            name.endsWith(mdcFieldNameSuffix))
                    {
                        final int lastIndexOf = name.lastIndexOf(mdcFieldNameSuffix);
                        fieldName = name.substring(0, lastIndexOf);
                    }

                    if (fieldName != null) {
                        final String value = indexableField.stringValue();
                        if (value != null) {
                            mdcPropertyMap.put(fieldName, value);
                        }
                    }
                }
            }
        }

        throwableRecordBuilder.cause(throwableCauseRecordBuilder.build());
        logRecordBuilder
                .throwableRecord(throwableRecordBuilder.build())
                .mdcPropertyMap(mdcPropertyMap);
        return logRecordBuilder.build();
    }

    private void addQuery(final String fieldName,
                          final String value,
                          final QueryBuilder queryBuilder,
                          final BooleanQuery.Builder booleanQueryBuilder)
    {
        if (!AppbaseUtil.isBlank(fieldName) && !AppbaseUtil.isBlank(value)) {
            final Query query = queryBuilder.createBooleanQuery(fieldName, value);
            booleanQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
        }
    }

    private void addLogLevels(
            final QueryBuilder queryBuilder,
            final BooleanQuery.Builder booleanQueryBuilder,
            final LogLevel... logLevels)
    {
        final LuceneLogIndexConfiguration luceneLogIndexConfiguration = luceneLogIndex.getLuceneLogIndexConfiguration();
        final String logLevelFieldName = luceneLogIndexConfiguration.getLogLevelFieldName();

        if (!AppbaseUtil.isBlank(logLevelFieldName)) {
            for (final LogLevel logLevel : logLevels) {
                addQuery(logLevelFieldName, logLevel.name(), queryBuilder, booleanQueryBuilder);
            }
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    Query toQuery(final LogRecordCriteria logRecordCriteria) {
        final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
        final LuceneLogIndexConfiguration luceneLogIndexConfiguration = luceneLogIndex.getLuceneLogIndexConfiguration();
        final QueryBuilder queryBuilder = luceneLogIndex.getLuceneService().getQueryBuilder();

        addQuery(
                luceneLogIndexConfiguration.getCallerStackFieldName(),
                logRecordCriteria.getCallerData(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getFormattedMessageFieldName(),
                logRecordCriteria.getFormattedMessage(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getLoggerNameFieldName(),
                logRecordCriteria.getLoggerName(),
                queryBuilder,
                booleanQueryBuilder);

        final LogLevelCriteria logLevelCriteria = logRecordCriteria.getLogLevelCriteria();
        if (logLevelCriteria != null) {
            final LogLevel logLevel = logLevelCriteria.getLogLevel();
            final LogLevelCriteria.Match match = logLevelCriteria.getMatch();
            if (logLevel != null && match != null) {
                final String logLevelName = logLevel.name();

                switch (match) {
                    case EQUAL:
                        addQuery(
                                luceneLogIndexConfiguration.getLogLevelFieldName(),
                                logLevelName,
                                queryBuilder,
                                booleanQueryBuilder);
                        break;

                    default:
                    case GREATER_OR_EQUAL:
                        switch (logLevel) {
                            case OFF:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.OFF);
                                break;
                            case ERROR:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.ERROR,
                                        LogLevel.OFF);
                                break;
                            case WARN:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.WARN,
                                        LogLevel.ERROR,
                                        LogLevel.OFF);
                                break;
                            case INFO:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.INFO,
                                        LogLevel.WARN,
                                        LogLevel.ERROR,
                                        LogLevel.OFF);
                                break;
                            case DEBUG:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.DEBUG,
                                        LogLevel.INFO,
                                        LogLevel.WARN,
                                        LogLevel.ERROR,
                                        LogLevel.OFF);
                                break;
                            case TRACE:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.TRACE,
                                        LogLevel.DEBUG,
                                        LogLevel.INFO,
                                        LogLevel.WARN,
                                        LogLevel.ERROR,
                                        LogLevel.OFF);
                                break;
                            case ALL:
                            default:
                                addLogLevels(
                                        queryBuilder,
                                        booleanQueryBuilder,
                                        LogLevel.OFF,
                                        LogLevel.ERROR,
                                        LogLevel.WARN,
                                        LogLevel.INFO,
                                        LogLevel.DEBUG,
                                        LogLevel.TRACE,
                                        LogLevel.ALL);
                                break;
                        }
                        break;
                }
            }
        }

        addQuery(
                luceneLogIndexConfiguration.getMarkerFieldName(),
                logRecordCriteria.getMarkerName(),
                queryBuilder,
                booleanQueryBuilder);

        final Map<String, String> mdcPropertyMap = logRecordCriteria.getMdcPropertyMap();
        if (mdcPropertyMap != null) {
            mdcPropertyMap.forEach((key, value) -> {
                if (!AppbaseUtil.isBlank(key) && !AppbaseUtil.isBlank(value)) {
                    final String fieldName = luceneLogIndexConfiguration.getMdcFieldName(key);
                    addQuery(fieldName, value, queryBuilder, booleanQueryBuilder);
                }
            });
        }

        addQuery(
                luceneLogIndexConfiguration.getMessageFieldName(),
                logRecordCriteria.getMessage(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getThreadNameFieldName(),
                logRecordCriteria.getThreadName(),
                queryBuilder,
                booleanQueryBuilder);

        addQuery(
                luceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                logRecordCriteria.getThrowableTypeName(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getThrowableMessageFieldName(),
                logRecordCriteria.getThrowableMessage(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                logRecordCriteria.getThrowableStackTrace(),
                queryBuilder,
                booleanQueryBuilder);

        addQuery(
                luceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                logRecordCriteria.getThrowableCauseTypeName(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                logRecordCriteria.getThrowableCauseMessage(),
                queryBuilder,
                booleanQueryBuilder);
        addQuery(
                luceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                logRecordCriteria.getThrowableStackTrace(),
                queryBuilder,
                booleanQueryBuilder);

        final TimestampCriteria timestampCriteria = logRecordCriteria.getTimestampCriteria();
        if (timestampCriteria != null) {
            if (timestampCriteria instanceof ExactTimestampCriteria) {
                final Query exactQuery = LongPoint.newExactQuery(
                        luceneLogIndexConfiguration.getTimestampLongPointName(),
                        ((ExactTimestampCriteria) timestampCriteria).getTimestamp());
                booleanQueryBuilder.add(exactQuery, BooleanClause.Occur.SHOULD);
            } else if (timestampCriteria instanceof SetTimestampCriteria) {
                final Set<Long> timestamps = ((SetTimestampCriteria) timestampCriteria).getTimestamps();
                if (timestamps != null && !timestamps.isEmpty()) {
                    final Query setQuery = LongPoint.newSetQuery(
                            luceneLogIndexConfiguration.getTimestampLongPointName(),
                            timestamps);
                    booleanQueryBuilder.add(setQuery, BooleanClause.Occur.SHOULD);
                }
            } else if (timestampCriteria instanceof RangeTimestampCriteria) {
                final RangeTimestampCriteria rangeTimestampCriteria = (RangeTimestampCriteria) timestampCriteria;
                final long inclusiveStartTimestamp = rangeTimestampCriteria.getInclusiveStartTimestamp();
                final long exclusiveEndTimestamp = rangeTimestampCriteria.getExclusiveEndTimestamp();
                final Query rangeQuery = LongPoint.newRangeQuery(
                        luceneLogIndexConfiguration.getTimestampLongPointName(),
                        inclusiveStartTimestamp,
                        Math.addExact(exclusiveEndTimestamp, -1));
                booleanQueryBuilder.add(rangeQuery, BooleanClause.Occur.SHOULD);
            } else {
                throw new AppbaseRuntimeException("Unsupported timestamp criteria type: " +
                        timestampCriteria.getClass().getName());
            }
        }

        return booleanQueryBuilder.build();
    }

    @Override
    public void synchronize() throws LogStoreException {
        try {
            luceneLogIndex.getLuceneService().synchronizeIndex();
        } catch (LuceneServiceException lse) {
            throw new LogStoreException("Error synchronizing: " + getClass().getName(), lse);
        }
    }

    public LuceneLogStoreConfiguration getLuceneLogStoreConfiguration() {
        return luceneLogStoreConfiguration;
    }

    public LuceneLogIndex getLuceneLogIndex() {
        return luceneLogIndex;
    }

    /**
     * Get the metric service backing this store's per-operation metrics. Exposed so the reactive view
     * ({@link ReactiveLuceneLogStore}) can record the SAME per-store Timer/error-Counter instances the
     * blocking reads record (the registry's {@code timer(name)}/{@code counter(name)} are get-or-create by
     * name), giving the reactive read path metrics parity with the blocking path.
     *
     * @return the metric service
     */
    public MetricService getMetricService() {
        return metricService;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        shutdown.set(true);
    }

    public void setShutdown(final boolean value) {
        shutdown.set(value);
    }
}
