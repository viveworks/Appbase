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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * {@link ReactiveExceptionStore} adapter for a non-Lucene {@link ExceptionStore} (JPA or in-memory): each
 * read offloads the delegate's blocking sync read onto the N01 {@link ReactiveReadBoundary} (via
 * {@link ReactiveReadBoundary#read(java.util.concurrent.Callable)}) rather than blocking the calling
 * thread. The JPA delegate blocks on JDBC and the in-memory delegate is trivially fast; either way the
 * delegate's read is a single blocking call, so — unlike
 * {@link com.viveworks.appbase.core.service.exception.store.lucene.ReactiveLuceneExceptionStore} — there
 * is no cooperative-cancellation fetch loop to bound here.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class SchedulerOffloadReactiveExceptionStore implements ReactiveExceptionStore {
    private final ExceptionStore delegate;
    private final ReactiveReadBoundary reactiveReadBoundary;

    public SchedulerOffloadReactiveExceptionStore(
            final ExceptionStore delegate, final ReactiveReadBoundary reactiveReadBoundary)
    {
        AppbaseUtil.notNull(delegate, "delegate");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");

        this.delegate = delegate;
        this.reactiveReadBoundary = reactiveReadBoundary;
    }

    @Override
    public Mono<ExceptionMessageDetails> readExceptionMessageDetailsByIdReactive(final int id) {
        return reactiveReadBoundary.read(() -> delegate.readExceptionMessageDetailsById(id));
    }

    @Override
    public Mono<Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsReactive() {
        return reactiveReadBoundary.read(delegate::readAllExceptionMessageDetails);
    }

    @Override
    public Mono<Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsByTimestampsReactive(
            final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        return reactiveReadBoundary.read(
                () -> delegate.readAllExceptionMessageDetailsByTimestamps(
                        inclusiveMinTimestamp, exclusiveMaxTimestamp));
    }

    @Override
    public Mono<Collection<String>> readUniqueThrowableTypeNamesReactive() {
        return reactiveReadBoundary.read(delegate::readUniqueThrowableTypeNames);
    }

    @Override
    public Mono<Collection<ExceptionMessageDetails>> readExceptionMessageDetailsByThrowableTypeNameReactive(
            final String throwableTypeName)
    {
        return reactiveReadBoundary.read(
                () -> delegate.readExceptionMessageDetailsByThrowableTypeName(throwableTypeName));
    }
}
