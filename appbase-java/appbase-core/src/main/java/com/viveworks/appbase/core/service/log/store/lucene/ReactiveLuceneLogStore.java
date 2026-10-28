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

import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.ReactiveLogStore;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * {@link ReactiveLogStore} implementation over {@link LuceneLogStore} — each read composes a
 * {@code pageSize}-bounded Lucene search with the existing reactive document-read boundary rather
 * than blocking the calling thread. The stream uses one downstream-demanded document read at a
 * time, so cancellation prevents subsequent documents from being fetched.
 *
 * <p>Each reactive read records the SAME per-store {@link Timer} and error {@link Counter} the blocking
 * {@link LuceneLogStore} read records — metrics parity (N05 S04). Because a native reactive read bypasses
 * the blocking store's timed entry points, the instrumentation is re-applied here, resolving the very same
 * metric instances by name from {@link LuceneLogStore#getMetricService()} (the registry's
 * {@code timer(name)}/{@code counter(name)} are get-or-create). Recording is non-blocking: the timer
 * start is a cheap {@code nanoTime} read on the subscribing thread, and the stop runs on the
 * terminal/cancel thread (the read is offloaded, so terminal signals originate off the caller's thread).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class ReactiveLuceneLogStore implements ReactiveLogStore {
    private final LuceneLogStore luceneLogStore;

    private final Timer readLogRecordsBetweenTimer;
    private final Timer readLogRecordsAtTimer;
    private final Timer readLogRecordsByCriteriaTimer;
    private final Counter readLogRecordsBetweenErrorCounter;
    private final Counter readLogRecordsAtErrorCounter;
    private final Counter readLogRecordsByCriteriaErrorCounter;

    public ReactiveLuceneLogStore(final LuceneLogStore luceneLogStore) {
        AppbaseUtil.notNull(luceneLogStore, "luceneLogStore");

        this.luceneLogStore = luceneLogStore;

        final MetricService metricService = luceneLogStore.getMetricService();
        this.readLogRecordsBetweenTimer = metricService.getMetricRegistry()
                .timer(metricService.name(luceneLogStore.getClass(), "readLogRecordsBetween", "timer"));
        this.readLogRecordsAtTimer = metricService.getMetricRegistry()
                .timer(metricService.name(luceneLogStore.getClass(), "readLogRecordsAt", "timer"));
        this.readLogRecordsByCriteriaTimer = metricService.getMetricRegistry()
                .timer(metricService.name(luceneLogStore.getClass(), "readLogRecordsByCriteria", "timer"));
        this.readLogRecordsBetweenErrorCounter = metricService.getMetricRegistry()
                .counter(metricService.name(luceneLogStore.getClass(), "readLogRecordsBetween", "error", "counter"));
        this.readLogRecordsAtErrorCounter = metricService.getMetricRegistry()
                .counter(metricService.name(luceneLogStore.getClass(), "readLogRecordsAt", "error", "counter"));
        this.readLogRecordsByCriteriaErrorCounter = metricService.getMetricRegistry()
                .counter(metricService.name(
                        luceneLogStore.getClass(), "readLogRecordsByCriteria", "error", "counter"));
    }

    @Override
    public Flux<LogRecord> streamLogRecordsBetweenReactive(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize) {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final LuceneService luceneService = luceneLogStore.getLuceneLogIndex().getLuceneService();
        final String timestampLongPointName = luceneLogStore.getLuceneLogIndex()
                .getLuceneLogIndexConfiguration()
                .getTimestampLongPointName();
        final Query query = LongPoint.newRangeQuery(
                timestampLongPointName, inclusiveStartTimestamp, Math.addExact(exclusiveEndTimestamp, -1));
        return timedFlux(readLogRecordsBetweenTimer, readLogRecordsBetweenErrorCounter,
                () -> luceneService.searchReactive(query, pageSize)
                        .flatMapMany(topDocs -> Flux.fromArray(topDocs.scoreDocs)
                                .concatMap(scoreDoc -> luceneService.documentReactive(scoreDoc.doc)
                                        .map(luceneLogStore::toLogRecord), 1)));
    }

    @Override
    public Mono<Collection<LogRecord>> readLogRecordsBetweenReactive(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize)
    {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final LuceneService luceneService = luceneLogStore.getLuceneLogIndex().getLuceneService();
        final String timestampLongPointName = luceneLogStore.getLuceneLogIndex()
                .getLuceneLogIndexConfiguration()
                .getTimestampLongPointName();

        return timed(readLogRecordsBetweenTimer, readLogRecordsBetweenErrorCounter,
                () -> luceneService.readReactive(cancelled -> {
                    final Query query = LongPoint.newRangeQuery(
                            timestampLongPointName, inclusiveStartTimestamp, Math.addExact(exclusiveEndTimestamp, -1));
                    final TopDocs topDocs = luceneService.getIndexSearcher().search(query, pageSize);
                    return luceneLogStore.readLogRecords(topDocs, cancelled);
                }));
    }

    @Override
    public Mono<Collection<LogRecord>> readLogRecordsAtReactive(final long timestamp, final int pageSize) {
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final LuceneService luceneService = luceneLogStore.getLuceneLogIndex().getLuceneService();
        final String timestampLongPointName = luceneLogStore.getLuceneLogIndex()
                .getLuceneLogIndexConfiguration()
                .getTimestampLongPointName();

        return timed(readLogRecordsAtTimer, readLogRecordsAtErrorCounter,
                () -> luceneService.readReactive(cancelled -> {
                    final Query query = LongPoint.newExactQuery(timestampLongPointName, timestamp);
                    final TopDocs topDocs = luceneService.getIndexSearcher().search(query, pageSize);
                    return luceneLogStore.readLogRecords(topDocs, cancelled);
                }));
    }

    @Override
    public Mono<Collection<LogRecord>> readLogRecordsByCriteriaReactive(
            final LogRecordCriteria logRecordCriteria, final int pageSize)
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");
        AppbaseUtil.positiveOrZero(pageSize, "pageSize");

        final LuceneService luceneService = luceneLogStore.getLuceneLogIndex().getLuceneService();

        return timed(readLogRecordsByCriteriaTimer, readLogRecordsByCriteriaErrorCounter,
                () -> luceneService.readReactive(cancelled -> {
                    final Query query = luceneLogStore.toQuery(logRecordCriteria);
                    final TopDocs topDocs = luceneService.getIndexSearcher().search(query, pageSize);
                    return luceneLogStore.readLogRecords(topDocs, cancelled);
                }));
    }

    /**
     * Wrap a reactive read so it records the given {@link Timer} and error {@link Counter} — parity with the
     * blocking read's {@code Timer.Context}/{@code errorCounter.inc()}/{@code stop()} pattern, expressed
     * reactively:
     * <ul>
     *   <li>the timer is started per subscription in {@link Mono#defer} (a cheap non-blocking
     *       {@code timer.time()} on the subscribing thread);</li>
     *   <li>the error counter increments on an error signal ({@code doOnError}) — mirroring the blocking
     *       {@code catch { errorCounter.inc() }};</li>
     *   <li>the timer context stops on every terminal outcome — {@code doOnTerminate} for complete/error
     *       (it runs before the terminal propagates downstream, so the recorded count is visible the instant
     *       the subscriber completes, which the parity tests rely on) AND {@code doOnCancel} for an early
     *       downstream unsubscribe, mirroring the blocking {@code finally} so a cancelled read cannot leak an
     *       unstopped context. These are mutually exclusive terminal paths, so the context stops exactly
     *       once.</li>
     * </ul>
     * A synchronous failure from {@code read.get()} (before any signal is emitted) is handled explicitly, so
     * neither the counter nor the stop is skipped on that path.
     */
    private static <T> Mono<T> timed(
            final Timer timer, final Counter errorCounter, final Supplier<Mono<T>> read)
    {
        return Mono.defer(() -> {
            final Timer.Context context = timer.time();
            final Mono<T> readMono;
            try {
                readMono = read.get();
            } catch (final RuntimeException | Error ex) {
                errorCounter.inc();
                context.stop();
                throw ex;
            }
            return readMono
                    .doOnError(throwable -> errorCounter.inc())
                    .doOnCancel(context::stop)
                    .doOnTerminate(context::stop);
        });
    }

    private static <T> Flux<T> timedFlux(
            final Timer timer, final Counter errorCounter, final Supplier<Flux<T>> read)
    {
        return Flux.defer(() -> {
            final Timer.Context context = timer.time();
            final Flux<T> readFlux;
            try {
                readFlux = read.get();
            } catch (final RuntimeException | Error ex) {
                errorCounter.inc();
                context.stop();
                throw ex;
            }
            return readFlux
                    .doOnError(throwable -> errorCounter.inc())
                    .doOnCancel(context::stop)
                    .doOnTerminate(context::stop);
        });
    }
}
