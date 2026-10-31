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

package com.viveworks.appbase.core.service.log.store;

import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * {@link ReactiveLogStore} adapter for a non-Lucene {@link LogStore}: each read offloads the delegate's
 * blocking sync read onto the N01 {@link ReactiveReadBoundary} (via {@link ReactiveReadBoundary#read(
 * java.util.concurrent.Callable)}) rather than blocking the calling thread. Unlike
 * {@link com.viveworks.appbase.core.service.log.store.lucene.ReactiveLuceneLogStore}, there is no
 * cooperative-cancellation fetch loop to bound here — the delegate's read is a single blocking call.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class SchedulerOffloadReactiveLogStore implements ReactiveLogStore {
    private final LogStore delegate;
    private final ReactiveReadBoundary reactiveReadBoundary;

    public SchedulerOffloadReactiveLogStore(final LogStore delegate, final ReactiveReadBoundary reactiveReadBoundary) {
        AppbaseUtil.notNull(delegate, "delegate");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");

        this.delegate = delegate;
        this.reactiveReadBoundary = reactiveReadBoundary;
    }

    @Override
    public Mono<Collection<LogRecord>> readLogRecordsBetweenReactive(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize)
    {
        return reactiveReadBoundary.read(
                () -> delegate.readLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize));
    }

    @Override
    public Mono<Collection<LogRecord>> readLogRecordsAtReactive(final long timestamp, final int pageSize) {
        return reactiveReadBoundary.read(() -> delegate.readLogRecordsAt(timestamp, pageSize));
    }

    @Override
    public Mono<Collection<LogRecord>> readLogRecordsByCriteriaReactive(
            final LogRecordCriteria logRecordCriteria, final int pageSize)
    {
        return reactiveReadBoundary.read(() -> delegate.readLogRecordsByCriteria(logRecordCriteria, pageSize));
    }
}
