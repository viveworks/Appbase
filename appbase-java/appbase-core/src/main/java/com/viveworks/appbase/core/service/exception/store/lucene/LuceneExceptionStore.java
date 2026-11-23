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

package com.viveworks.appbase.core.service.exception.store.lucene;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.util.lucene.LuceneServiceException;
import com.viveworks.appbase.core.util.lucene.LuceneUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * {@link com.viveworks.appbase.core.util.lucene.LuceneService} base implementation of {@link ExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings("checkstyle:IllegalCatch")
@SuppressFBWarnings({"HES_EXECUTOR_NEVER_SHUTDOWN", "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE"})
public class LuceneExceptionStore implements ExceptionStore {
    private final Logger logger;
    private final ValidationService validationService;
    private final LuceneService luceneService;
    private final AtomicInteger idGenerator;
    private final AtomicReference<LuceneExceptionStoreConfiguration> luceneExceptionStoreConfigurationRef;
    private final ExecutorService executorService;
    private final StackTraceHashGenerator stackTraceHashGenerator;
    private final ExceptionMessageContextCodec exceptionMessageContextCodec;

    private final Timer readExceptionMessageDetailsByIdTimer;
    private final Counter readExceptionMessageDetailsByIdErrorCounter;
    private final Timer asyncReadExceptionMessageDetailsByIdTimer;
    private final Counter asyncReadExceptionMessageDetailsByIdErrorCounter;

    private final Timer readAllExceptionMessageDetailsTimer;
    private final Counter readAllExceptionMessageDetailsErrorCounter;
    private final Timer asyncReadAllExceptionMessageDetailsTimer;
    private final Counter asyncReadAllExceptionMessageDetailsErrorCounter;

    private final Timer readAllExceptionMessageDetailsByTimestampsTimer;
    private final Counter readAllExceptionMessageDetailsByTimestampsErrorCounter;
    private final Timer asyncReadAllExceptionMessageDetailsByTimestampsTimer;
    private final Counter asyncReadAllExceptionMessageDetailsByTimestampsErrorCounter;

    private final Timer readUniqueExceptionTypeNamesTimer;
    private final Counter readUniqueExceptionTypeNamesErrorCounter;
    private final Timer asyncReadUniqueExceptionTypeNamesTimer;
    private final Counter asyncReadUniqueExceptionTypeNamesErrorCounter;

    private final Timer readExceptionMessageDetailsByTypeNameTimer;
    private final Counter readExceptionMessageDetailsByTypeNameErrorCounter;
    private final Timer asyncReadExceptionMessageDetailsByTypeNameTimer;
    private final Counter asyncReadExceptionMessageDetailsByTypeNameErrorCounter;

    private final Timer writeExceptionMessageTimer;
    private final Counter writeExceptionMessageErrorCounter;
    private final Timer asyncWriteExceptionMessageTimer;
    private final Counter asyncWriteExceptionMessageErrorCounter;

    private final Timer removeExceptionMessageDetailsByIdTimer;
    private final Counter removeExceptionMessageDetailsByIdErrorCounter;
    private final Timer asyncRemoveExceptionMessageDetailsByIdTimer;
    private final Counter asyncRemoveExceptionMessageDetailsByIdErrorCounter;

    private final Timer removeExceptionMessageDetailsBeforeTimer;
    private final Counter removeExceptionMessageDetailsBeforeErrorCounter;
    private final Timer asyncRemoveExceptionMessageDetailsBeforeTimer;
    private final Counter asyncRemoveExceptionMessageDetailsBeforeErrorCounter;

    private final AtomicBoolean shutdown;

    public LuceneExceptionStore(final LogService logService,
                                final MetricService metricService,
                                final MediatorService mediatorService,
                                final ValidationService validationService,
                                final LuceneService luceneService,
                                final StackTraceHashGenerator stackTraceHashGenerator,
                                final ExceptionMessageContextCodec exceptionMessageContextCodec,
                                final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(luceneService, "luceneService");
        AppbaseUtil.notNull(stackTraceHashGenerator, "stackTraceHashGenerator");
        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");
        AppbaseUtil.notNull(luceneExceptionStoreConfiguration, "luceneExceptionStoreConfiguration");
        AppbaseUtil.validate(validationService, luceneExceptionStoreConfiguration);

        this.logger = logService.getLogger(getClass());
        this.validationService = validationService;
        this.luceneService = luceneService;
        this.idGenerator = new AtomicInteger(0);
        this.luceneExceptionStoreConfigurationRef = new AtomicReference<>(luceneExceptionStoreConfiguration);
        this.executorService = mediatorService.getMediator().getSharedExecutorService();
        this.stackTraceHashGenerator = stackTraceHashGenerator;
        this.exceptionMessageContextCodec = exceptionMessageContextCodec;

        readExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readExceptionMessageDetailsById", "timer"));
        readExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readExceptionMessageDetailsById", "error", "counter"));
        asyncReadExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadExceptionMessageDetailsById", "timer"));
        asyncReadExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadExceptionMessageDetailsById", "error", "counter"));

        readAllExceptionMessageDetailsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readAllExceptionMessageDetails", "timer"));
        readAllExceptionMessageDetailsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readAllExceptionMessageDetails", "error", "counter"));
        asyncReadAllExceptionMessageDetailsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadAllExceptionMessageDetails", "timer"));
        asyncReadAllExceptionMessageDetailsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadAllExceptionMessageDetails", "error", "counter"));

        readAllExceptionMessageDetailsByTimestampsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readAllExceptionMessageDetailsByTimestamps", "timer"));
        readAllExceptionMessageDetailsByTimestampsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "readAllExceptionMessageDetailsByTimestamps", "error", "counter"));
        asyncReadAllExceptionMessageDetailsByTimestampsTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadAllExceptionMessageDetailsByTimestamps", "timer"));
        asyncReadAllExceptionMessageDetailsByTimestampsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "asyncReadAllExceptionMessageDetailsByTimestamps", "error", "counter"));

        readUniqueExceptionTypeNamesTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readUniqueExceptionTypeNames", "timer"));
        readUniqueExceptionTypeNamesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "readUniqueExceptionTypeNames", "error", "counter"));
        asyncReadUniqueExceptionTypeNamesTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadUniqueExceptionTypeNames", "timer"));
        asyncReadUniqueExceptionTypeNamesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncReadUniqueExceptionTypeNames", "error", "counter"));

        readExceptionMessageDetailsByTypeNameTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "readExceptionMessageDetailsByThrowableTypeName", "timer"));
        readExceptionMessageDetailsByTypeNameErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "readExceptionMessageDetailsByThrowableTypeName", "error", "counter"));
        asyncReadExceptionMessageDetailsByTypeNameTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncReadExceptionMessageDetailsByThrowableTypeName", "timer"));
        asyncReadExceptionMessageDetailsByTypeNameErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(
                        getClass(), "asyncReadExceptionMessageDetailsByThrowableTypeName", "error", "counter"));

        writeExceptionMessageTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "writeExceptionMessage", "timer"));
        writeExceptionMessageErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "writeExceptionMessage", "error", "counter"));
        asyncWriteExceptionMessageTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncWriteExceptionMessage", "timer"));
        asyncWriteExceptionMessageErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncWriteExceptionMessage", "error", "counter"));

        removeExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeExceptionMessageDetailsById", "timer"));
        removeExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removeExceptionMessageDetailsById", "error", "counter"));
        asyncRemoveExceptionMessageDetailsByIdTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsById", "timer"));
        asyncRemoveExceptionMessageDetailsByIdErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsById", "error", "counter"));

        removeExceptionMessageDetailsBeforeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "removeExceptionMessageDetailsBefore", "timer"));
        removeExceptionMessageDetailsBeforeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removeExceptionMessageDetailsBefore", "error", "counter"));
        asyncRemoveExceptionMessageDetailsBeforeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsBefore", "timer"));
        asyncRemoveExceptionMessageDetailsBeforeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "asyncRemoveExceptionMessageDetailsBefore", "error", "counter"));

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public ExceptionMessageDetails readExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context context = readExceptionMessageDetailsByIdTimer.time();
        try {
            final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            final Query idQuery = IntPoint.newExactQuery(luceneExceptionStoreConfiguration.getIdIntPointName(), id);
            booleanQueryBuilder.add(idQuery, BooleanClause.Occur.MUST);

            return luceneService.withSearcher(searcher -> {
                final TopDocs topDocs = searcher.search(booleanQueryBuilder.build(), 1);
                if (topDocs != null) {
                    final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                    if (scoreDocs != null) {
                        for (final ScoreDoc scoreDoc : scoreDocs) {
                            if (scoreDoc != null) {
                                final int documentId = scoreDoc.doc;
                                final Document document = searcher.getIndexReader().document(documentId);
                                if (document != null) {
                                    return toExceptionMessageDetails(document);
                                }
                            }

                            break;
                        }
                    }
                }
                return null;
            });
        } catch (Exception ex) {
            logger.error("Error in 'readExceptionMessageDetailsById()'", ex);
            readExceptionMessageDetailsByIdErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<ExceptionMessageDetails> asyncReadExceptionMessageDetailsById(final int id) {
        final CompletableFuture<ExceptionMessageDetails> resultFuture = new CompletableFuture<>();

        final Timer.Context context = asyncReadExceptionMessageDetailsByIdTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(readExceptionMessageDetailsById(id));
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadExceptionMessageDetailsById()'", ex);
                asyncReadExceptionMessageDetailsByIdErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetails() throws ExceptionStoreException {
        return readAllExceptionMessageDetails(() -> false);
    }

    // R3 (internal work item 1075): package-visible, cancellation-aware overload for ReactiveLuceneExceptionStore — the loop
    // iterates every document, so it honours the reactive read boundary's cancel flag between documents
    // (cooperative cancellation; never Thread.interrupt(), which would close the NIOFSDirectory).
    Collection<ExceptionMessageDetails> readAllExceptionMessageDetails(final BooleanSupplier cancelled)
            throws ExceptionStoreException
    {
        checkState();

        final Timer.Context context = readAllExceptionMessageDetailsTimer.time();
        try {
            return luceneService.withSearcher(searcher -> {
                final List<ExceptionMessageDetails> exceptionMessageDetailsList = new ArrayList<>();

                final IndexReader indexReader = searcher.getIndexReader();
                for (int i = 0; i < indexReader.maxDoc(); ++i) {
                    if (cancelled.getAsBoolean()) {
                        break;
                    }

                    final Document document = indexReader.document(i);
                    if (document != null) {
                        final ExceptionMessageDetails exceptionMessageDetails = toExceptionMessageDetails(document);
                        exceptionMessageDetailsList.add(exceptionMessageDetails);
                    }
                }

                return exceptionMessageDetailsList;
            });
        } catch (Exception ex) {
            logger.error("Error in 'readAllExceptionMessageDetails()'", ex);
            readAllExceptionMessageDetailsErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetails() {
        final CompletableFuture<Collection<ExceptionMessageDetails>> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context context = asyncReadAllExceptionMessageDetailsTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(readAllExceptionMessageDetails());
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadAllExceptionMessageDetails()'", ex);
                asyncReadAllExceptionMessageDetailsErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<ExceptionMessageDetails> readAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp) throws ExceptionStoreException
    {
        AppbaseUtil.positive(inclusiveMinTimestamp, "inclusiveMinTimestamp");
        AppbaseUtil.positive(exclusiveMaxTimestamp, "exclusiveMaxTimestamp");

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context context = readAllExceptionMessageDetailsByTimestampsTimer.time();
        try {
            final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            final Query timestampQuery = LongPoint.newRangeQuery(
                    luceneExceptionStoreConfiguration.getTimestampLongPointName(),
                    inclusiveMinTimestamp,
                    Math.addExact(exclusiveMaxTimestamp, -1));
            booleanQueryBuilder.add(timestampQuery, BooleanClause.Occur.MUST);

            return luceneService.withSearcher(searcher -> {
                final List<ExceptionMessageDetails> exceptionMessageDetailsList = new ArrayList<>();
                final TopDocs topDocs = searcher.search(booleanQueryBuilder.build(), Integer.MAX_VALUE);
                if (topDocs != null) {
                    final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                    if (scoreDocs != null) {
                        for (final ScoreDoc scoreDoc : scoreDocs) {
                            if (scoreDoc != null) {
                                final int documentId = scoreDoc.doc;
                                final Document document = searcher.getIndexReader().document(documentId);
                                if (document != null) {
                                    final ExceptionMessageDetails exceptionMessageDetails =
                                            toExceptionMessageDetails(document);
                                    exceptionMessageDetailsList.add(exceptionMessageDetails);
                                }
                            }
                        }
                    }
                }

                return exceptionMessageDetailsList;
            });
        } catch (Exception ex) {
            logger.error("Error in 'readAllExceptionMessageDetailsByTimestamps()'", ex);
            readAllExceptionMessageDetailsByTimestampsErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        AppbaseUtil.positive(inclusiveMinTimestamp, "inclusiveMinTimestamp");
        AppbaseUtil.positive(exclusiveMaxTimestamp, "exclusiveMaxTimestamp");

        final CompletableFuture<Collection<ExceptionMessageDetails>> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context context = asyncReadAllExceptionMessageDetailsByTimestampsTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(
                        readAllExceptionMessageDetailsByTimestamps(inclusiveMinTimestamp, exclusiveMaxTimestamp));
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadAllExceptionMessageDetailsByTimestamps()'", ex);
                asyncReadAllExceptionMessageDetailsByTimestampsErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<String> readUniqueThrowableTypeNames() throws ExceptionStoreException {
        return readUniqueThrowableTypeNames(() -> false);
    }

    // R3 (internal work item 1075): package-visible, cancellation-aware overload for ReactiveLuceneExceptionStore — see
    // readAllExceptionMessageDetails(BooleanSupplier). Iterates every document, honouring the cancel flag.
    Collection<String> readUniqueThrowableTypeNames(final BooleanSupplier cancelled) throws ExceptionStoreException {
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context context = readUniqueExceptionTypeNamesTimer.time();
        try {
            return luceneService.withSearcher(searcher -> {
                final Set<String> uniqueThrowableTypeNames = new HashSet<>();
                final IndexReader indexReader = searcher.getIndexReader();
                for (int i = 0; i < indexReader.maxDoc(); ++i) {
                    if (cancelled.getAsBoolean()) {
                        break;
                    }

                    final Document document = indexReader.document(i);
                    if (document != null) {
                        final IndexableField field =
                                document.getField(luceneExceptionStoreConfiguration.getThrowableTypeNameFieldName());
                        if (field != null) {
                            final String throwableTypeName = field.stringValue();
                            if (!AppbaseUtil.isBlank(throwableTypeName)) {
                                uniqueThrowableTypeNames.add(throwableTypeName);
                            }
                        }
                    }
                }

                return uniqueThrowableTypeNames;
            });
        } catch (Exception ex) {
            logger.error("Error in 'readUniqueThrowableTypeNames()'", ex);
            readUniqueExceptionTypeNamesErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<String>> asyncReadUniqueThrowableTypeNames() {
        final CompletableFuture<Collection<String>> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context context = asyncReadUniqueExceptionTypeNamesTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(readUniqueThrowableTypeNames());
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadUniqueThrowableTypeNames()'", ex);
                asyncReadUniqueExceptionTypeNamesErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public Collection<ExceptionMessageDetails> readExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName)
            throws ExceptionStoreException
    {
        AppbaseUtil.notBlank(throwableTypeName, "throwableTypeName");

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context context = readExceptionMessageDetailsByTypeNameTimer.time();
        try {
            final Query booleanQuery = luceneService.getQueryBuilder().createBooleanQuery(
                    luceneExceptionStoreConfiguration.getThrowableTypeNameFieldName(),
                    throwableTypeName,
                    BooleanClause.Occur.MUST);

            return luceneService.withSearcher(searcher -> {
                final List<ExceptionMessageDetails> exceptionMessageDetailsList = new ArrayList<>();
                final TopDocs topDocs = searcher.search(booleanQuery, Integer.MAX_VALUE);
                if (topDocs != null) {
                    final ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                    if (scoreDocs != null) {
                        for (final ScoreDoc scoreDoc : scoreDocs) {
                            if (scoreDoc != null) {
                                final int documentId = scoreDoc.doc;
                                final Document document = searcher.getIndexReader().document(documentId);
                                if (document != null) {
                                    final ExceptionMessageDetails exceptionMessageDetails =
                                            toExceptionMessageDetails(document);
                                    exceptionMessageDetailsList.add(exceptionMessageDetails);
                                }
                            }
                        }
                    }
                }

                return exceptionMessageDetailsList;
            });
        } catch (Exception ex) {
            logger.error("Error in 'readExceptionMessageDetailsByThrowableTypeName()'", ex);
            readExceptionMessageDetailsByTypeNameErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Collection<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName)
    {
        AppbaseUtil.notBlank(throwableTypeName, "throwableTypeName");

        final CompletableFuture<Collection<ExceptionMessageDetails>> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context context = asyncReadExceptionMessageDetailsByTypeNameTimer.time();
        executorService.submit(() -> {
            try {
                resultFuture.complete(readExceptionMessageDetailsByThrowableTypeName(throwableTypeName));
            } catch (Exception ex) {
                logger.error("Error in 'asyncReadExceptionMessageDetailsByThrowableTypeName()'", ex);
                asyncReadExceptionMessageDetailsByTypeNameErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void writeExceptionMessage(final ExceptionMessage exceptionMessage) throws ExceptionStoreException {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context timerContext = writeExceptionMessageTimer.time();
        try {
            final Document document = new Document();

            final int id = idGenerator.incrementAndGet();
            LuceneUtil.addIntPoint(
                    luceneExceptionStoreConfiguration.getIdIntPointName(),
                    luceneExceptionStoreConfiguration.getIdFieldName(),
                    id,
                    document);

            LuceneUtil.addTextField(
                    luceneExceptionStoreConfiguration.getDiscriminatorFieldName(),
                    luceneExceptionStoreConfiguration.getDiscriminator(),
                    document);

            final String message = exceptionMessage.getMessage();
            LuceneUtil.addTextField(
                    luceneExceptionStoreConfiguration.getMessageFieldName(),
                    message,
                    document);

            final String threadName = exceptionMessage.getThreadName();
            LuceneUtil.addTextField(
                    luceneExceptionStoreConfiguration.getThreadNameFieldName(),
                    threadName,
                    document);

            final Class<?> sourceType = exceptionMessage.getSourceType();
            if (sourceType != null) {
                LuceneUtil.addTextField(
                        luceneExceptionStoreConfiguration.getSourceTypeNameFieldName(),
                        sourceType.getName(),
                        document);
            }

            final Throwable throwable = exceptionMessage.getThrowable();
            if (throwable != null) {
                LuceneUtil.addTextField(
                        luceneExceptionStoreConfiguration.getThrowableTypeNameFieldName(),
                        throwable.getClass().getName(),
                        document);

                final String throwableMessage = throwable.getMessage();
                if (throwableMessage != null) {
                    LuceneUtil.addTextField(
                            luceneExceptionStoreConfiguration.getThrowableMessageFieldName(),
                            throwableMessage,
                            document);
                }

                final String stackTraceString = ExceptionStore.serializeThrowable(
                        throwable, luceneExceptionStoreConfiguration.getStackTraceSerializationMaxLength());
                LuceneUtil.addTextField(
                        luceneExceptionStoreConfiguration.getStackTraceFieldName(),
                        stackTraceString,
                        document);

                final int stackTraceHash = stackTraceHashGenerator.generateHash(stackTraceString);
                LuceneUtil.addIntPoint(
                        luceneExceptionStoreConfiguration.getStackTraceHashIntPointName(),
                        luceneExceptionStoreConfiguration.getStackTraceFieldName(),
                        stackTraceHash,
                        document);
            }

            final Map<String, String> context = exceptionMessage.getContext();
            if (context != null) {
                final String encodedContext = exceptionMessageContextCodec.encode(context);
                LuceneUtil.addTextField(
                        luceneExceptionStoreConfiguration.getContextFieldName(),
                        encodedContext,
                        document);
            }

            final long timestamp = exceptionMessage.getTimestamp();
            LuceneUtil.addLongPoint(
                    luceneExceptionStoreConfiguration.getTimestampLongPointName(),
                    luceneExceptionStoreConfiguration.getTimestampFieldName(),
                    timestamp,
                    document);

            luceneService.getIndexWriter().addDocument(document);
            luceneService.synchronizeIndex();
        } catch (Exception ex) {
            logger.error("Error in 'writeExceptionMessage()'", ex);
            writeExceptionMessageErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncWriteExceptionMessage(final ExceptionMessage exceptionMessage) {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context timerContext = asyncWriteExceptionMessageTimer.time();
        executorService.submit(() -> {
            try {
                writeExceptionMessage(exceptionMessage);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in 'asyncWriteExceptionMessage()'", ex);
                asyncWriteExceptionMessageErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                timerContext.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeExceptionMessageDetailsById(final int id) throws ExceptionStoreException {
        AppbaseUtil.positive(id, "id");

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context context = removeExceptionMessageDetailsByIdTimer.time();
        try {
            final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            final Query idQuery = IntPoint.newExactQuery(luceneExceptionStoreConfiguration.getIdIntPointName(), id);
            booleanQueryBuilder.add(idQuery, BooleanClause.Occur.MUST);
            luceneService.getIndexWriter(true).deleteDocuments(idQuery);
            luceneService.synchronizeIndex();
        } catch (Exception ex) {
            logger.error("Error in 'removeExceptionMessageDetailsById()'", ex);
            removeExceptionMessageDetailsByIdErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsById(final int id) {
        AppbaseUtil.positive(id, "id");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context context = asyncRemoveExceptionMessageDetailsByIdTimer.time();
        executorService.submit(() -> {
            try {
                removeExceptionMessageDetailsById(id);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in 'asyncRemoveExceptionMessageDetailsById()'", ex);
                asyncRemoveExceptionMessageDetailsByIdErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    @Override
    public void removeExceptionMessageDetailsExclusiveBefore(final long timestamp) throws ExceptionStoreException {
        AppbaseUtil.positive(timestamp, "timestamp");

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration = checkState();

        final Timer.Context context = removeExceptionMessageDetailsBeforeTimer.time();
        try {
            final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            final Query timestampQuery = LongPoint.newRangeQuery(
                    luceneExceptionStoreConfiguration.getTimestampLongPointName(),
                    Long.MIN_VALUE,
                    Math.addExact(timestamp, -1));
            booleanQueryBuilder.add(timestampQuery, BooleanClause.Occur.MUST);
            luceneService.getIndexWriter(true).deleteDocuments(timestampQuery);
            luceneService.synchronizeIndex();
        } catch (Exception ex) {
            logger.error("Error in 'removeExceptionMessageDetailsExclusiveBefore()'", ex);
            removeExceptionMessageDetailsBeforeErrorCounter.inc();
            throw new ExceptionStoreException(ex);
        } finally {
            context.stop();
        }
    }

    @Override
    public CompletableFuture<Void> asyncRemoveExceptionMessageDetailsExclusiveBefore(final long timestamp) {
        AppbaseUtil.positive(timestamp, "timestamp");

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        checkState(resultFuture);
        if (resultFuture.isCompletedExceptionally()) {
            return resultFuture;
        }

        final Timer.Context context = asyncRemoveExceptionMessageDetailsBeforeTimer.time();
        executorService.submit(() -> {
            try {
                removeExceptionMessageDetailsExclusiveBefore(timestamp);
                resultFuture.complete(null);
            } catch (Exception ex) {
                logger.error("Error in 'asyncRemoveExceptionMessageDetailsExclusiveBefore()'", ex);
                asyncRemoveExceptionMessageDetailsBeforeErrorCounter.inc();
                resultFuture.completeExceptionally(ex);
            } finally {
                context.stop();
            }
        });

        return resultFuture;
    }

    // R3 (internal work item 1075): package-visible so ReactiveLuceneExceptionStore can reach the N01 reactive read boundary
    // (LuceneService.readReactive), mirroring how ReactiveLuceneLogStore reaches it via LuceneLogStore.
    LuceneService getLuceneService() {
        return luceneService;
    }

    private ExceptionMessageDetails toExceptionMessageDetails(final Document document) {
        final List<IndexableField> fields = document.getFields();
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration =
                luceneExceptionStoreConfigurationRef.get();

        final ExceptionMessageDetails exceptionMessageDetails = new ExceptionMessageDetails();
        for (final IndexableField field : fields) {
            final String name = field.name();

            if (name.equals(luceneExceptionStoreConfiguration.getSourceTypeNameFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setSourceTypeName(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getStackTraceFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setStackTrace(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getContextFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setContext(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getMessageFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setMessage(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getThreadNameFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setThreadName(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getTimestampFieldName())) {
                final Number number = field.numericValue();
                if (number != null) {
                    final long value = number.longValue();
                    exceptionMessageDetails.setTimestamp(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getDiscriminatorFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setDiscriminator(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getIdFieldName())) {
                final Number number = field.numericValue();
                if (number != null) {
                    final int value = number.intValue();
                    exceptionMessageDetails.setId(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getStackTraceHashFieldName())) {
                final Number number = field.numericValue();
                if (number != null) {
                    final int value = number.intValue();
                    exceptionMessageDetails.setStackTraceHash(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getThrowableTypeNameFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setThrowableTypeName(value);
                }
            } else if (name.equals(luceneExceptionStoreConfiguration.getThrowableMessageFieldName())) {
                final String value = field.stringValue();
                if (!AppbaseUtil.isBlank(value)) {
                    exceptionMessageDetails.setThrowableMessage(value);
                }
            }
        }

        return exceptionMessageDetails;
    }

    private LuceneExceptionStoreConfiguration checkState() {
        if (shutdown.get()) {
            throw new IllegalStateException(getClass().getName() + " is shutdown");
        }

        return luceneExceptionStoreConfigurationRef.get();
    }

    private LuceneExceptionStoreConfiguration checkState(final CompletableFuture<?> resultFuture) {
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration =
                luceneExceptionStoreConfigurationRef.get();

        if (shutdown.get()) {
            resultFuture.completeExceptionally(new IllegalStateException(getClass().getName() + " is shutdown"));
            return luceneExceptionStoreConfiguration;
        }

        if (luceneExceptionStoreConfiguration == null) {
            resultFuture.completeExceptionally(new IllegalStateException(
                    LuceneExceptionStoreConfiguration.class.getName() + " is null"));
            return luceneExceptionStoreConfiguration;
        }

        if (!luceneExceptionStoreConfiguration.isEnabled()) {
            resultFuture.completeExceptionally(new IllegalStateException(getClass().getName() + "is disabled"));
        }

        return luceneExceptionStoreConfiguration;
    }

    @Override
    public <C extends ExceptionStoreConfiguration> void reload(
            final C exceptionStoreConfiguration) throws ExceptionStoreException
    {
        AppbaseUtil.notNull(exceptionStoreConfiguration, "exceptionStoreConfiguration");

        if (!(exceptionStoreConfiguration instanceof LuceneExceptionStoreConfiguration)) {
            throw new ExceptionStoreException(
                    "exceptionStoreConfiguration must be of type " + LuceneExceptionStoreConfiguration.class.getName());
        }

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration =
                (LuceneExceptionStoreConfiguration) exceptionStoreConfiguration;
        AppbaseUtil.validate(validationService, luceneExceptionStoreConfiguration);

        this.luceneExceptionStoreConfigurationRef.set(luceneExceptionStoreConfiguration);
    }

    @Override
    public void synchronize() throws ExceptionStoreException {
        try {
            logger.info("Synchronizing {}", getClass().getName());
            luceneService.synchronizeIndex();
        } catch (LuceneServiceException lse) {
            throw new ExceptionStoreException("Error while synchronizing " + getClass().getName(), lse);
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        shutdown.set(true);
    }
}
