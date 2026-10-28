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

import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ReactiveExceptionStore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * {@link ReactiveExceptionStore} implementation over {@link LuceneExceptionStore} — each read is offloaded
 * through {@link LuceneService#readReactive(com.viveworks.appbase.core.service.reactive.read.CancellableRead)}
 * (the N01 reactive read boundary) rather than blocking the calling thread. The two reads that iterate every
 * document ({@code readAll}, {@code readUniqueThrowableTypeNames}) honour the boundary's cancellation flag
 * between documents via {@code LuceneExceptionStore}'s cancellation-aware overloads; the three bounded-search
 * reads run whole on the boundary. Mirrors {@code ReactiveLuceneLogStore}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class ReactiveLuceneExceptionStore implements ReactiveExceptionStore {
    private final LuceneExceptionStore luceneExceptionStore;

    public ReactiveLuceneExceptionStore(final LuceneExceptionStore luceneExceptionStore) {
        AppbaseUtil.notNull(luceneExceptionStore, "luceneExceptionStore");

        this.luceneExceptionStore = luceneExceptionStore;
    }

    @Override
    public Mono<ExceptionMessageDetails> readExceptionMessageDetailsByIdReactive(final int id) {
        final LuceneService luceneService = luceneExceptionStore.getLuceneService();
        return luceneService.readReactive(cancelled -> luceneExceptionStore.readExceptionMessageDetailsById(id));
    }

    @Override
    public Mono<Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsReactive() {
        final LuceneService luceneService = luceneExceptionStore.getLuceneService();
        return luceneService.readReactive(luceneExceptionStore::readAllExceptionMessageDetails);
    }

    @Override
    public Mono<Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsByTimestampsReactive(
            final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        final LuceneService luceneService = luceneExceptionStore.getLuceneService();
        return luceneService.readReactive(cancelled -> luceneExceptionStore
                .readAllExceptionMessageDetailsByTimestamps(inclusiveMinTimestamp, exclusiveMaxTimestamp));
    }

    @Override
    public Mono<Collection<String>> readUniqueThrowableTypeNamesReactive() {
        final LuceneService luceneService = luceneExceptionStore.getLuceneService();
        return luceneService.readReactive(luceneExceptionStore::readUniqueThrowableTypeNames);
    }

    @Override
    public Mono<Collection<ExceptionMessageDetails>> readExceptionMessageDetailsByThrowableTypeNameReactive(
            final String throwableTypeName)
    {
        final LuceneService luceneService = luceneExceptionStore.getLuceneService();
        return luceneService.readReactive(cancelled -> luceneExceptionStore
                .readExceptionMessageDetailsByThrowableTypeName(throwableTypeName));
    }
}
