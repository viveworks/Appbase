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
package com.viveworks.appbase.client.service.elasticsearch.restclient.accessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.DeleteDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.DeleteDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.DeleteDocumentResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.BooleanSearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.GeoBoundingBoxQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.GeoLocation;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.MatchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.RangeQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchTerm;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.TermQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.WildcardQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.ErrorResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.Hits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.util.FieldDataTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import jakarta.validation.ValidationException;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractSearchIndexAccessor<D, R> implements SearchIndexAccessor<D, R> {
    private static final String SUCCESS = "success";
    private static final String CREATED = "created";
    private static final String UPDATED = "updated";
    private static final String DELETED = "deleted";

    protected final Logger logger;

    protected final ElasticsearchRestClient elasticsearchRestClient;

    protected final String indexNamePrefix;

    private final Timer addDocumentTimer;
    private final Timer updateDocumentTimer;
    private final Timer deleteDocumentTimer;
    private final Timer searchTimer;
    private final Timer createIndexTimer;
    private final Timer existsIndexTimer;
    private final Timer deleteIndexTimer;
    private final Timer asyncAddDocumentTimer;
    private final Timer asyncUpdateDocumentTimer;
    private final Timer asyncDeleteDocumentTimer;
    private final Timer asyncSearchTimer;
    private final Timer asyncCreateIndexTimer;
    private final Timer asyncExistsIndexTimer;
    private final Timer asyncDeleteIndexTimer;

    protected AbstractSearchIndexAccessor(
            final AppbaseCore appbaseCore,
            final ElasticsearchRestClient elasticsearchRestClient,
            final String indexNamePrefix)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(elasticsearchRestClient, "elasticsearchRestClient");
        AppbaseUtil.notNull(indexNamePrefix, "indexNamePrefix");

        this.logger = appbaseCore
                .getLogService()
                .getLogger(getClass());
        this.elasticsearchRestClient = elasticsearchRestClient;
        this.indexNamePrefix = indexNamePrefix;

        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.addDocumentTimer = metricRegistry.timer(metricService.name(getClass(), "addDocumentTimer"));
        this.updateDocumentTimer = metricRegistry.timer(metricService.name(getClass(), "updateDocumentTimer"));
        this.deleteDocumentTimer = metricRegistry.timer(metricService.name(getClass(), "deleteDocumentTimer"));
        this.searchTimer = metricRegistry.timer(metricService.name(getClass(), "searchTimer"));
        this.createIndexTimer = metricRegistry.timer(metricService.name(getClass(), "createIndexTimer"));
        this.existsIndexTimer = metricRegistry.timer(metricService.name(getClass(), "existsIndexTimer"));
        this.deleteIndexTimer = metricRegistry.timer(metricService.name(getClass(), "deleteIndexTimer"));
        this.asyncAddDocumentTimer = metricRegistry.timer(metricService.name(getClass(), "asyncAddDocumentTimer"));
        this.asyncUpdateDocumentTimer = metricRegistry.timer(metricService.name(getClass(), "asyncUpdateDocumentTimer"));
        this.asyncDeleteDocumentTimer = metricRegistry.timer(metricService.name(getClass(), "asyncDeleteDocumentTimer"));
        this.asyncSearchTimer = metricRegistry.timer(metricService.name(getClass(), "asyncSearchTimer"));
        this.asyncCreateIndexTimer = metricRegistry.timer(metricService.name(getClass(), "asyncCreateIndexTimer"));
        this.asyncExistsIndexTimer = metricRegistry.timer(metricService.name(getClass(), "asyncExistsIndexTimer"));
        this.asyncDeleteIndexTimer = metricRegistry.timer(metricService.name(getClass(), "asyncDeleteIndexTimer"));
    }

    protected abstract Class<D> documentType();

    protected abstract String indexName();

    protected abstract String documentId(D document);

    protected abstract void validateDocument(D document) throws ValidationException;

    protected abstract Map<String, CreateIndexRequestBody.FieldProperties> fieldProperties();

    protected abstract SearchRequest searchRequest(R request) throws IOException;

    @Override
    public void addDocument(final D document) throws IOException {
        final Timer.Context timer = addDocumentTimer.time();
        try {
            addOrUpdateDocument(document, true);
        } finally {
          timer.stop();
        }
    }

    @Override
    public void updateDocument(final D document) throws IOException {
        final Timer.Context timer = updateDocumentTimer.time();
        try {
            addOrUpdateDocument(document, false);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteDocument(final String id) throws IOException {
        AppbaseUtil.notBlank(id, "id");

        final Timer.Context timer = deleteDocumentTimer.time();
        try {
            final DeleteDocumentResponse deleteDocumentResponse = elasticsearchRestClient.deleteDocument(deleteDocumentRequest(id));
            final ErrorResponseBody errorResponseBody = deleteDocumentResponse.getErrorResponseBody();
            if (errorResponseBody != null) {
                throw new IOException("Error while adding document [id=" + id + "]: " + errorResponseBody);
            }

            return !deleteDocumentResponse.isError();
        } finally {
            timer.stop();
        }
    }

    @Override
    public List<D> search(final R request) throws IOException {
        AppbaseUtil.notNull(request, "request");

        final Timer.Context timer = searchTimer.time();
        try {
            final SearchRequest searchRequest = searchRequest(request);
            final SearchResponse searchResponse = elasticsearchRestClient.search(searchRequest);
            final ErrorResponseBody errorResponseBody = searchResponse.getErrorResponseBody();

            if (searchResponse.isError() || errorResponseBody != null) {
                throw new IOException("Search request " + searchRequest + " failed with an error: " + errorResponseBody);
            }

            final SearchResponseBody searchResponseBody = searchResponse.getSearchResponseBody();
            if (searchResponseBody == null) {
                throw new IOException("Search request " + searchRequest + " failed with no response body");
            }

            return searchResult(searchResponseBody);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean createIndex() throws IOException {
        final Timer.Context timer = createIndexTimer.time();
        try {
            final CreateIndexResponse createIndexResponse = elasticsearchRestClient.createIndex(createIndexRequest());
            return createIndexResponse.isSuccess();
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsIndex() throws IOException {
        final Timer.Context timer = existsIndexTimer.time();
        try {
            final ExistsIndexResponse existsIndexResponse = elasticsearchRestClient.existsIndex(existsIndexRequest());
            return existsIndexResponse.isExists();
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteIndex() throws IOException {
        final Timer.Context timer = deleteIndexTimer.time();
        try {
            final DeleteIndexResponse deleteIndexResponse = elasticsearchRestClient.deleteIndex(deleteIndexRequest());
            return deleteIndexResponse.isDeleted();
        } finally {
            timer.stop();
        }
    }

    private void addOrUpdateDocument(final D document, final boolean add) throws IOException {
        AppbaseUtil.notNull(document, "document");

        final AddOrUpdateDocumentResponse addOrUpdateDocumentResponse;
        if (add) {
            addOrUpdateDocumentResponse = elasticsearchRestClient.addDocument(addDocumentRequest(document));
        } else {
            addOrUpdateDocumentResponse = elasticsearchRestClient.updateDocument(updateDocumentRequest(document));
        }

        final ErrorResponseBody errorResponseBody = addOrUpdateDocumentResponse.getErrorResponseBody();
        if (addOrUpdateDocumentResponse.isError() || errorResponseBody != null) {
            throw new IOException("Error while adding document: " + errorResponseBody);
        }
    }


    @Override
    public CompletableFuture<Void> asyncAddDocument(final D document) {
        AppbaseUtil.notNull(document, "document");

        return asyncAddOrUpdateDocument(document, true, asyncAddDocumentTimer);
    }

    @Override
    public CompletableFuture<Void> asyncUpdateDocument(final D document) {
        AppbaseUtil.notNull(document, "document");

        return asyncAddOrUpdateDocument(document, false, asyncUpdateDocumentTimer);
    }

    @Override
    public CompletableFuture<Boolean> asyncDeleteDocument(final String id) {
        AppbaseUtil.notBlank(id, "id");

        final Timer.Context timer = asyncDeleteDocumentTimer.time();
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        try {
            final DeleteDocumentRequest deleteDocumentRequest = deleteDocumentRequest(id);
            final CompletableFuture<DeleteDocumentResponse> clientFuture =
                    elasticsearchRestClient.asyncDeleteDocument(deleteDocumentRequest);
            clientFuture.whenComplete((deleteDocumentResponse, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                    return;
                }

                if (deleteDocumentResponse.isError()) {
                    final ErrorResponseBody errorResponseBody =
                            deleteDocumentResponse.getErrorResponseBody();

                    result.completeExceptionally(new RuntimeException(
                            "Delete document request " + deleteDocumentRequest +
                                    " failed with an error: " + errorResponseBody));
                    return;
                }

                final DeleteDocumentResponseBody deleteDocumentResponseBody =
                        deleteDocumentResponse.getDeleteDocumentResponseBody();
                if (deleteDocumentResponseBody == null) {
                    result.completeExceptionally(new RuntimeException(
                            "Delete document request " + deleteDocumentRequest +
                                    " failed with no response body"));
                    return;
                }

                final String status = deleteDocumentResponseBody.getResult();
                if (!(DELETED.equalsIgnoreCase(status) || SUCCESS.equalsIgnoreCase(status))) {
                    logger.warn(deleteDocumentRequest + " did not delete document, but was not an error");
                    result.complete(false);
                    return;
                }

                result.complete(true);
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }

        return result.whenComplete((aBoolean, throwable) -> {
            timer.stop();
        });
    }

    protected DeleteDocumentRequest deleteDocumentRequest(final String id) {
        AppbaseUtil.notBlank(id, "id");

        return new DeleteDocumentRequest(indexName(), id);
    }

    @Override
    public CompletableFuture<List<D>> asyncSearch(final R request) {
        AppbaseUtil.notNull(request, "request");

        final Timer.Context timer = asyncSearchTimer.time();
        final CompletableFuture<List<D>> result = new CompletableFuture<>();

        final SearchRequest searchRequest;

        try {
            searchRequest = searchRequest(request);
            logger.info("Sending search request: {}", searchRequest);

            final CompletableFuture<SearchResponse> clientFuture =
                    elasticsearchRestClient.asyncSearch(searchRequest);
            clientFuture.whenComplete((searchResponse, throwable) -> {
               if (throwable != null) {
                   logger.error("Search request {} failed: {}", searchRequest, searchResponse, throwable);
                   result.completeExceptionally(throwable);
                   return;
               }

               if (searchResponse.isError()) {
                   final ErrorResponseBody errorResponseBody =
                           searchResponse.getErrorResponseBody();

                   result.completeExceptionally(new RuntimeException(
                           "Search request " + searchRequest +
                                   " failed with an error: " + errorResponseBody));
                   return;
               }

                final SearchResponseBody searchResponseBody =
                        searchResponse.getSearchResponseBody();
                if (searchResponseBody == null) {
                    result.completeExceptionally(new RuntimeException(
                            "Search request " + searchRequest + " failed with no response body"));
                    return;
                }

                final List<D> searchResult;
                try {
                    searchResult = searchResult(searchResponseBody);
                } catch (Exception ex) {
                    result.completeExceptionally(ex);
                    return;
                }

                result.complete(searchResult);
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }

        return result.whenComplete((ds, throwable) -> {
            timer.stop();
        });
    }

    protected List<D> searchResult(SearchResponseBody searchResponseBody) {
        final List<D> result = new ArrayList<>();

        final SearchHits searchHits = searchResponseBody.getHits();
        if (searchHits != null) {
            final List<Hits> hits = searchHits.getHits();
            if (hits != null) {
                hits.forEach(hit -> {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> sourceJson = (Map<String, Object>) hit.getSourceJson();
                    final D value = JsonUtil.getJsonMapper().convertValue(sourceJson, documentType());
                    result.add(value);
                });
            }
        }

        return result;
    }

    @Override
    public CompletableFuture<Boolean> asyncCreateIndex() {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        final Timer.Context timer = asyncCreateIndexTimer.time();
        final CreateIndexRequest createIndexRequest;
        try {
            createIndexRequest = createIndexRequest();

            final CompletableFuture<CreateIndexResponse> clientFuture =
                    elasticsearchRestClient.asyncCreateIndex(createIndexRequest);
            clientFuture.whenComplete((createIndexResponse, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                    return;
                }

                result.complete(createIndexResponse.isSuccess());
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }

        return result.whenComplete((aBoolean, throwable) -> {
            timer.stop();
        });
    }

    protected CreateIndexRequest createIndexRequest() {
        final CreateIndexRequestBody createIndexRequestBody = new CreateIndexRequestBody();
        createIndexRequestBody.setMappings(mappings());

        return new CreateIndexRequest(indexName(), createIndexRequestBody);
    }

    protected CreateIndexRequestBody.Mappings mappings() {
        final CreateIndexRequestBody.Mappings mappings = new CreateIndexRequestBody.Mappings();
        final Map<String, CreateIndexRequestBody.FieldProperties> fieldProperties = fieldProperties();
        mappings.setProperties(fieldProperties);

        return mappings;
    }

    @Override
    public CompletableFuture<Boolean> asyncExistsIndex() {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        final Timer.Context timer = asyncExistsIndexTimer.time();
        final ExistsIndexRequest existsIndexRequest;
        try {
            existsIndexRequest = existsIndexRequest();

            final CompletableFuture<ExistsIndexResponse> clientFuture =
                    elasticsearchRestClient.asyncExistsIndex(existsIndexRequest);
            clientFuture.whenComplete((existsIndexResponse, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                    return;
                }

                result.complete(existsIndexResponse.isExists());
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }

        return result.whenComplete((aBoolean, throwable) -> {
            timer.stop();
        });
    }

    protected ExistsIndexRequest existsIndexRequest() {
        return new ExistsIndexRequest(indexName());
    }

    @Override
    public CompletableFuture<Boolean> asyncDeleteIndex() {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        final Timer.Context timer = asyncDeleteIndexTimer.time();
        final DeleteIndexRequest deleteIndexRequest;
        try {
            deleteIndexRequest = deleteIndexRequest();

            final CompletableFuture<DeleteIndexResponse> clientFuture =
                    elasticsearchRestClient.asyncDeleteIndex(deleteIndexRequest);
            clientFuture.whenComplete((deleteIndexResponse, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                    return;
                }

                result.complete(deleteIndexResponse.isDeleted());
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }

        return result.whenComplete((aBoolean, throwable) -> {
            timer.stop();
        });
    }

    protected DeleteIndexRequest deleteIndexRequest() {
        return new DeleteIndexRequest(indexName());
    }

    private CompletableFuture<Void> asyncAddOrUpdateDocument(
            final D document,
            final boolean add,
            final Timer asyncAddOrUpdateDocumentTimer)
    {
        AppbaseUtil.notNull(document, "document");

        final Timer.Context timer = asyncAddOrUpdateDocumentTimer.time();
        final CompletableFuture<Void> result = new CompletableFuture<>();

        try {
            final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest;
            if (add) {
                addOrUpdateDocumentRequest = addDocumentRequest(document);
            } else {
                addOrUpdateDocumentRequest = updateDocumentRequest(document);
            }

            final CompletableFuture<AddOrUpdateDocumentResponse> clientFuture;
            if (add) {
                clientFuture = elasticsearchRestClient.asyncAddDocument(addOrUpdateDocumentRequest);
            } else {
                clientFuture = elasticsearchRestClient.asyncUpdateDocument(addOrUpdateDocumentRequest);
            }

            clientFuture.whenComplete((addOrUpdateDocumentResponse, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                    return;
                }

                if (addOrUpdateDocumentResponse.isError()) {
                    final ErrorResponseBody errorResponseBody =
                            addOrUpdateDocumentResponse.getErrorResponseBody();

                    result.completeExceptionally(new RuntimeException(
                            "Add or update document request " + addOrUpdateDocumentRequest +
                                    " failed with an error: " + errorResponseBody));
                    return;
                }

                final AddOrUpdateDocumentResponseBody addDocumentResponseBody =
                        addOrUpdateDocumentResponse.getAddDocumentResponseBody();
                if (addDocumentResponseBody == null) {
                    result.completeExceptionally(new RuntimeException(
                            "Add or document request " + addOrUpdateDocumentRequest +
                                    " failed with no response body"));
                    return;
                }

                final String status = addDocumentResponseBody.getResult();
                if (add) {
                    if (!(CREATED.equalsIgnoreCase(status) || SUCCESS.equalsIgnoreCase(status))) {
                        result.completeExceptionally(new RuntimeException(
                                "Add document request " + addOrUpdateDocumentRequest +
                                        " failed with an error: " + addDocumentResponseBody));
                        return;
                    }
                } else {
                    if (!(UPDATED.equalsIgnoreCase(status) || SUCCESS.equalsIgnoreCase(status))) {
                        result.completeExceptionally(new RuntimeException(
                                "Update document request " + addOrUpdateDocumentRequest +
                                        " failed with an error: " + addDocumentResponseBody));
                        return;
                    }
                }

                result.complete(null);
            });
        } catch (Exception ex) {
            result.completeExceptionally(ex);
        }

        return result.whenComplete((unused, throwable) -> {
            timer.stop();
        });
    }

    protected AddOrUpdateDocumentRequest addDocumentRequest(D document) throws IOException {
        return addOrUpdateDocumentRequest(document);
    }

    protected AddOrUpdateDocumentRequest updateDocumentRequest(D document) throws IOException {
        return addOrUpdateDocumentRequest(document);
    }

    protected AddOrUpdateDocumentRequest addOrUpdateDocumentRequest(D document) throws IOException {
        AppbaseUtil.notNull(document, "document");

        validateDocument(document);

        final String json = json(document);

        final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest = new AddOrUpdateDocumentRequest(json, indexName());
        addOrUpdateDocumentRequest.setDocumentId(documentId(document));

        return addOrUpdateDocumentRequest;
    }

    protected String json(D document) throws JsonProcessingException {
        return JsonUtil.getJsonMapper().writerFor(documentType()).writeValueAsString(document);
    }

    protected CreateIndexRequestBody.FieldProperties textFieldProperties() {
        final CreateIndexRequestBody.FieldProperties textProperties =
                new CreateIndexRequestBody.FieldProperties();
        textProperties.setIndex(true);
        textProperties.setType(FieldDataTypes.TEXT.getText());

        return textProperties;
    }

    protected CreateIndexRequestBody.FieldProperties doubleProperties() {
        final CreateIndexRequestBody.FieldProperties doubleProperties =
                new CreateIndexRequestBody.FieldProperties();
        doubleProperties.setIndex(true);
        doubleProperties.setType(FieldDataTypes.DOUBLE.getText());

        return doubleProperties;
    }

    protected CreateIndexRequestBody.FieldProperties integerProperties() {
        final CreateIndexRequestBody.FieldProperties integerProperties =
                new CreateIndexRequestBody.FieldProperties();
        integerProperties.setIndex(true);
        integerProperties.setType(FieldDataTypes.INTEGER.getText());

        return integerProperties;
    }

    protected CreateIndexRequestBody.FieldProperties booleanFieldProperties() {
        final CreateIndexRequestBody.FieldProperties booleanProperties =
                new CreateIndexRequestBody.FieldProperties();
        booleanProperties.setIndex(true);
        booleanProperties.setType(FieldDataTypes.BOOLEAN.getText());

        return booleanProperties;
    }

    protected CreateIndexRequestBody.FieldProperties geoPointFieldProperties() {
        final CreateIndexRequestBody.FieldProperties geoPointProperties =
                new CreateIndexRequestBody.FieldProperties();
        geoPointProperties.setIndex(true);
        geoPointProperties.setType(FieldDataTypes.GEO_POINT.getText());

        return geoPointProperties;
    }

    protected CreateIndexRequestBody.FieldProperties dateTimeFieldProperties() {
        final CreateIndexRequestBody.FieldProperties dateTimeProperties =
                new CreateIndexRequestBody.FieldProperties();
        dateTimeProperties.setIndex(true);
        dateTimeProperties.setType(FieldDataTypes.DATE.getText());
        dateTimeProperties.setFormat(AppbaseConstants.DEFAULT_DATE_TIME_FORMAT);

        return dateTimeProperties;
    }

    protected void addParam(
            @Nullable final Object value,
            final String indexFieldName,
            final Map<String, MatchQuery.FieldParams> matchParams)
    {
        if (value != null) {
            final MatchQuery.FieldParams fieldParams = new MatchQuery.FieldParams();
            fieldParams.setQuery(value);
            matchParams.put(indexFieldName, fieldParams);
        }
    }

    protected void addStringParam(
            @Nullable final String value,
            final String indexFieldName,
            final Map<String, MatchQuery.FieldParams> matchParams)
    {
        if (!AppbaseUtil.isBlank(value)) {
            addParam(value, indexFieldName, matchParams);
        }
    }

    protected SearchRequest searchRequest(final List<SearchQuery> mustQueries) {
        final BooleanSearchQuery.Bool bool = new BooleanSearchQuery.Bool();
        bool.setMust(mustQueries);
        final BooleanSearchQuery booleanSearchQuery = new BooleanSearchQuery();
        booleanSearchQuery.setBool(bool);
        final SearchQueryBody searchQueryBody = new SearchQueryBody();
        searchQueryBody.setQuery(booleanSearchQuery);

        return new SearchRequest(indexName(), searchQueryBody);
    }

    protected void addTermQuery(
            final TermQuery termQuery,
            final List<SearchQuery> mustQueries)
    {
        if (termQuery != null) {
            mustQueries.add(termQuery);
        }
    }

    protected void addWildcardQuery(
            final WildcardQuery wildcardQuery,
            final List<SearchQuery> mustQueries)
    {
        if (wildcardQuery != null) {
            mustQueries.add(wildcardQuery);
        }
    }

    protected void addRangeQuery(final RangeQuery rangeQuery, final List<SearchQuery> mustQueries) {
        if (rangeQuery != null) {
            mustQueries.add(rangeQuery);
        }
    }

    protected void addGeoBoundingBoxQuery(
            final GeoBoundingBoxQuery geoBoundingBoxQuery,
            final List<SearchQuery> mustQueries)
    {
        if (geoBoundingBoxQuery != null) {
            mustQueries.add(geoBoundingBoxQuery);
        }
    }

    protected GeoBoundingBoxQuery.FieldParams geoBoundingBoxQueryFieldParams(
            final GeoLocation topLeft,
            final GeoLocation bottomRight)
    {
        AppbaseUtil.notNull(topLeft, "topLeft");
        AppbaseUtil.notNull(bottomRight, "bottomRight");

        final GeoBoundingBoxQuery.FieldParams fieldParams = new GeoBoundingBoxQuery.FieldParams();
        fieldParams.setTopLeft(topLeft);
        fieldParams.setBottomRight(bottomRight);

        return fieldParams;
    }

    protected GeoBoundingBoxQuery geoBoundingBoxQuery(
            final String fieldName,
            final GeoBoundingBoxQuery.FieldParams fieldParams)
    {
        AppbaseUtil.notBlank(fieldName, "fieldName");
        AppbaseUtil.notNull(fieldParams, "fieldParams");

        final Map<String, GeoBoundingBoxQuery.FieldParams> geoBoundingBoxParams = new HashMap<>();
        geoBoundingBoxParams.put(fieldName, fieldParams);

        final GeoBoundingBoxQuery geoBoundingBoxQuery = new GeoBoundingBoxQuery();
        geoBoundingBoxQuery.setGeoBoundingBox(geoBoundingBoxParams);

        return geoBoundingBoxQuery;
    }

    protected WildcardQuery wildcardQuery(
            final String fieldName, final WildcardQuery.FieldParams fieldParams)
    {
        AppbaseUtil.notBlank(fieldName, "fieldName");
        AppbaseUtil.notNull(fieldParams, "fieldParams");

        final Map<String, WildcardQuery.FieldParams> wildcardParams = new HashMap<>();
        wildcardParams.put(fieldName, fieldParams);

        final WildcardQuery wildcardQuery = new WildcardQuery();
        wildcardQuery.setWildcard(wildcardParams);

        return wildcardQuery;
    }

    protected TermQuery termQuery(final String fieldName, final Object value) {
        AppbaseUtil.notBlank(fieldName, "fieldName");
        AppbaseUtil.notNull(value, "value");

        final TermQuery termQuery = new TermQuery();

        final SearchTerm searchTerm = new SearchTerm();
        searchTerm.setValue(value);

        final Map<String, SearchTerm> term = new HashMap<>();
        term.put(fieldName, searchTerm);

        termQuery.setTerm(term);

        return termQuery;
    }

    protected RangeQuery rangeQueryBothInclusive(
            final String fieldName,
            final String greaterThanEqualTo,
            final String lessThanEqualTo)
    {
        AppbaseUtil.notBlank(fieldName, "fieldName");
        AppbaseUtil.notBlank(greaterThanEqualTo, "greaterThanEqualTo");
        AppbaseUtil.notBlank(lessThanEqualTo, "lessThanEqualTo");

        final RangeQuery.FieldParams fieldParams = new RangeQuery.FieldParams();
        fieldParams.setGreaterThanOrEqualTo(greaterThanEqualTo);
        fieldParams.setLessThanOrEqualTo(lessThanEqualTo);

        final RangeQuery rangeQuery = new RangeQuery();
        final Map<String, RangeQuery.FieldParams> range = new HashMap<>();
        range.put(fieldName, fieldParams);
        rangeQuery.setRange(range);

        return rangeQuery;
    }
}
