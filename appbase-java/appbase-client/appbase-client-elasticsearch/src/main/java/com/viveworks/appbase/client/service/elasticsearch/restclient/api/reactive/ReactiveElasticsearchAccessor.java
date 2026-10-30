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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive;

import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchAccessorException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.DeleteDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.DeleteDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.Hits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskResponse;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Reactive accessor for Elasticsearch, built on the typed {@link ElasticsearchRestClient}. Each
 * operation bridges the typed client's {@code asyncXxx} {@link CompletableFuture}
 * API into the Reactor world with {@link Mono#fromFuture(Supplier)} inside the full {@link
 * #mono(String, Supplier)} spine (timeout, retry, scheduler, metrics, tracing, and
 * exception-store reporting are all supplied by the inherited {@link AbstractReactiveAccessor});
 * Reactor unwraps a future failure's {@link CompletionException}, so {@link
 * #mapError(Throwable)} sees the raw driver exception.
 *
 * <p>Unlike the Cassandra/jasync reactive accessors, there is no {@code finalizeOperation} here:
 * the borrow is the typed {@link ElasticsearchRestClient} itself (the default client or a
 * key-bound view), not a pooled {@code AppbaseClientIntegrationService}. The typed client's
 * {@code asyncXxx} methods already manage their own internal integration-service lifecycle
 * (borrow/return) per call, so this accessor's borrow is used only to obtain that client
 * reference -- there is nothing left for this accessor to return to a pool.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveElasticsearchAccessor extends AbstractReactiveAccessor {

    private final Supplier<ElasticsearchRestClient> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of the typed Elasticsearch REST client, invoked once per subscription
     */
    public ReactiveElasticsearchAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<ElasticsearchRestClient> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Run a search request.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Search request
     * @return Wrapped {@link Mono} of the {@link SearchResponse}
     */
    public Mono<SearchResponse> search(final String op, final SearchRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncSearch(request)));
    }

    /**
     * Run a multi-search request.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Multi-search request
     * @return Wrapped {@link Mono} of the {@link MultiSearchResponse}
     */
    public Mono<MultiSearchResponse> multiSearch(final String op, final MultiSearchRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncMultiSearch(request)));
    }

    /**
     * Add a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Add-document request
     * @return Wrapped {@link Mono} of the {@link AddOrUpdateDocumentResponse}
     */
    public Mono<AddOrUpdateDocumentResponse> addDocument(final String op, final AddOrUpdateDocumentRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncAddDocument(request)));
    }

    /**
     * Update a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Update-document request
     * @return Wrapped {@link Mono} of the {@link AddOrUpdateDocumentResponse}
     */
    public Mono<AddOrUpdateDocumentResponse> updateDocument(final String op, final AddOrUpdateDocumentRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncUpdateDocument(request)));
    }

    /**
     * Add or update a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Add-or-update-document request
     * @return Wrapped {@link Mono} of the {@link AddOrUpdateDocumentResponse}
     */
    public Mono<AddOrUpdateDocumentResponse> addOrUpdateDocument(final String op, final AddOrUpdateDocumentRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncAddOrUpdateDocument(request)));
    }

    /**
     * Delete a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Delete-document request
     * @return Wrapped {@link Mono} of the {@link DeleteDocumentResponse}
     */
    public Mono<DeleteDocumentResponse> deleteDocument(final String op, final DeleteDocumentRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncDeleteDocument(request)));
    }

    /**
     * Create an index.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Create-index request
     * @return Wrapped {@link Mono} of the {@link CreateIndexResponse}
     */
    public Mono<CreateIndexResponse> createIndex(final String op, final CreateIndexRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncCreateIndex(request)));
    }

    /**
     * Check whether an index exists.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Exists-index request
     * @return Wrapped {@link Mono} of the {@link ExistsIndexResponse}
     */
    public Mono<ExistsIndexResponse> existsIndex(final String op, final ExistsIndexRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncExistsIndex(request)));
    }

    /**
     * Delete an index.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Delete-index request
     * @return Wrapped {@link Mono} of the {@link DeleteIndexResponse}
     */
    public Mono<DeleteIndexResponse> deleteIndex(final String op, final DeleteIndexRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncDeleteIndex(request)));
    }

    /**
     * Cancel a task.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Cancel-task request
     * @return Wrapped {@link Mono} of the {@link CancelTaskResponse}
     */
    public Mono<CancelTaskResponse> cancelTask(final String op, final CancelTaskRequest request) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return mono(op, () -> Mono.fromFuture(() -> borrow.get().asyncCancelTask(request)));
    }

    /**
     * Convenience stream over the search hit list: runs {@link #search(String, SearchRequest)}
     * and flattens its {@link SearchResponse} into the individual {@link Hits}; no separate
     * driver call.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Search request
     * @return Wrapped {@link Flux} of {@link Hits}
     */
    public Flux<Hits> searchHits(final String op, final SearchRequest request) {
        return search(op, request).flatMapMany(r -> Flux.fromIterable(hitsOf(r)));
    }

    private List<Hits> hitsOf(final SearchResponse response) {
        final SearchResponseBody body = response.getSearchResponseBody();
        if (body == null) {
            return Collections.emptyList();
        }
        final SearchHits searchHits = body.getHits();
        if (searchHits == null) {
            return Collections.emptyList();
        }
        final List<Hits> hits = searchHits.getHits();
        return hits == null ? Collections.emptyList() : hits;
    }

    /**
     * Maps Elasticsearch REST client failures to {@link ElasticsearchAccessorException}; all
     * other throwables delegate to the inherited default mapping. Uses the shared static helper
     * in {@link ElasticsearchAccessorException} so behavior stays identical to the synchronous
     * twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return ElasticsearchAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level Elasticsearch error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
