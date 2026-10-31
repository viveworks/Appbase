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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
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
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskResponse;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Elasticsearch, built on the typed {@link ElasticsearchRestClient}. Each
 * operation borrows the typed client and invokes its matching synchronous method directly --
 * unlike {@code CassandraAccessor}, there is no separate pooled integration service to return: the
 * borrow is the typed {@link ElasticsearchRestClient} itself (the default client or a key-bound
 * view), whose synchronous methods already own their own integration-service lifecycle per call.
 * Cross-cutting concerns (metrics, tracing, and exception-store reporting) are supplied by the
 * inherited {@link AbstractAppbaseAccessor}; this class contributes only the driver operation and
 * Elasticsearch error mapping, shared with the reactive twin ({@link
 * com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive.ReactiveElasticsearchAccessor})
 * via {@link ElasticsearchAccessorException#map(Throwable, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ElasticsearchAccessor extends AbstractAppbaseAccessor {

    private final Supplier<ElasticsearchRestClient> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of the typed Elasticsearch REST client, invoked once per operation
     */
    public ElasticsearchAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<ElasticsearchRestClient> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Run a search request.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Search request
     * @return Search response
     * @throws Exception The mapped exception, if the operation fails
     */
    public SearchResponse search(final String op, final SearchRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().search(request));
    }

    /**
     * Run a multi-search request.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Multi-search request
     * @return Multi-search response
     * @throws Exception The mapped exception, if the operation fails
     */
    public MultiSearchResponse multiSearch(final String op, final MultiSearchRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().multiSearch(request));
    }

    /**
     * Add a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Add-document request
     * @return Add-or-update-document response
     * @throws Exception The mapped exception, if the operation fails
     */
    public AddOrUpdateDocumentResponse addDocument(final String op, final AddOrUpdateDocumentRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().addDocument(request));
    }

    /**
     * Update a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Update-document request
     * @return Add-or-update-document response
     * @throws Exception The mapped exception, if the operation fails
     */
    public AddOrUpdateDocumentResponse updateDocument(final String op, final AddOrUpdateDocumentRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().updateDocument(request));
    }

    /**
     * Add or update a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Add-or-update-document request
     * @return Add-or-update-document response
     * @throws Exception The mapped exception, if the operation fails
     */
    public AddOrUpdateDocumentResponse addOrUpdateDocument(final String op, final AddOrUpdateDocumentRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().addOrUpdateDocument(request));
    }

    /**
     * Delete a document.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Delete-document request
     * @return Delete-document response
     * @throws Exception The mapped exception, if the operation fails
     */
    public DeleteDocumentResponse deleteDocument(final String op, final DeleteDocumentRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().deleteDocument(request));
    }

    /**
     * Create an index.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Create-index request
     * @return Create-index response
     * @throws Exception The mapped exception, if the operation fails
     */
    public CreateIndexResponse createIndex(final String op, final CreateIndexRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().createIndex(request));
    }

    /**
     * Check whether an index exists.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Exists-index request
     * @return Exists-index response
     * @throws Exception The mapped exception, if the operation fails
     */
    public ExistsIndexResponse existsIndex(final String op, final ExistsIndexRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().existsIndex(request));
    }

    /**
     * Delete an index.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Delete-index request
     * @return Delete-index response
     * @throws Exception The mapped exception, if the operation fails
     */
    public DeleteIndexResponse deleteIndex(final String op, final DeleteIndexRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().deleteIndex(request));
    }

    /**
     * Cancel a task.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param request Cancel-task request
     * @return Cancel-task response
     * @throws Exception The mapped exception, if the operation fails
     */
    public CancelTaskResponse cancelTask(final String op, final CancelTaskRequest request) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(request, "request");
        return execute(op, () -> borrow.get().cancelTask(request));
    }

    /**
     * Maps Elasticsearch REST client failures to {@link ElasticsearchAccessorException}; all
     * other throwables delegate to the inherited default mapping. Uses the shared static helper
     * in {@link ElasticsearchAccessorException} so behavior stays identical to the reactive twin.
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
