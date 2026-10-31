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

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
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
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive.ReactiveElasticsearchAccessor;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskResponse;
import com.viveworks.appbase.core.service.Shutdownable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ElasticsearchRestClient extends Shutdownable {
    SearchResponse search(SearchRequest searchRequest) throws IOException;

    MultiSearchResponse multiSearch(MultiSearchRequest multiSearchRequest) throws IOException;

    AddOrUpdateDocumentResponse addDocument(AddOrUpdateDocumentRequest addOrUpdateDocumentRequest) throws IOException;

    AddOrUpdateDocumentResponse updateDocument(AddOrUpdateDocumentRequest addOrUpdateDocumentRequest) throws IOException;

    AddOrUpdateDocumentResponse addOrUpdateDocument(AddOrUpdateDocumentRequest addOrUpdateDocumentRequest) throws IOException;

    DeleteDocumentResponse deleteDocument(DeleteDocumentRequest deleteDocumentRequest) throws IOException;

    CreateIndexResponse createIndex(CreateIndexRequest createIndexRequest) throws IOException;

    ExistsIndexResponse existsIndex(ExistsIndexRequest existsIndexRequest) throws IOException;

    DeleteIndexResponse deleteIndex(DeleteIndexRequest deleteIndexRequest) throws IOException;

    CancelTaskResponse cancelTask(CancelTaskRequest cancelTaskRequest) throws IOException;



    CompletableFuture<SearchResponse> asyncSearch(
            SearchRequest searchRequest);

    CompletableFuture<MultiSearchResponse> asyncMultiSearch(
            MultiSearchRequest multiSearchRequest);

    CompletableFuture<AddOrUpdateDocumentResponse> asyncAddDocument(
            AddOrUpdateDocumentRequest addOrUpdateDocumentRequest);

    CompletableFuture<AddOrUpdateDocumentResponse> asyncUpdateDocument(
            AddOrUpdateDocumentRequest addOrUpdateDocumentRequest);

    CompletableFuture<AddOrUpdateDocumentResponse> asyncAddOrUpdateDocument(
            AddOrUpdateDocumentRequest addOrUpdateDocumentRequest);

    CompletableFuture<DeleteDocumentResponse> asyncDeleteDocument(
            DeleteDocumentRequest deleteDocumentRequest);

    CompletableFuture<CreateIndexResponse> asyncCreateIndex(
            CreateIndexRequest createIndexRequest);

    CompletableFuture<ExistsIndexResponse> asyncExistsIndex(
            ExistsIndexRequest existsIndexRequest);

    CompletableFuture<DeleteIndexResponse> asyncDeleteIndex(
            DeleteIndexRequest deleteIndexRequest);

    CompletableFuture<CancelTaskResponse> asyncCancelTask(
            CancelTaskRequest cancelTaskRequest);

    /**
     * Get the reactive accessor bound to the default Elasticsearch cluster.
     *
     * @return Reactive accessor for the default cluster
     */
    ReactiveElasticsearchAccessor getReactiveAccessor();

    /**
     * Get the reactive accessor bound to a specific, keyed Elasticsearch cluster.
     *
     * @param appbaseClientKey Client key identifying the target cluster
     * @return Reactive accessor for the keyed cluster
     */
    ReactiveElasticsearchAccessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Get the synchronous accessor bound to the default Elasticsearch cluster.
     *
     * @return Synchronous accessor for the default cluster
     */
    ElasticsearchAccessor getAccessor();

    /**
     * Get the synchronous accessor bound to a specific, keyed Elasticsearch cluster.
     *
     * @param appbaseClientKey Client key identifying the target cluster
     * @return Synchronous accessor for the keyed cluster
     */
    ElasticsearchAccessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
