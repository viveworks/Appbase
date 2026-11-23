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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
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
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive.ReactiveElasticsearchAccessor;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequestEnvelope;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequestHeader;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.BooleanSearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.BoostingSearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.ConstantScoreSearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.DisjunctionMaxSearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.FieldRuntimeMapping;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.MatchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.RangeQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.RegexQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.ScriptRuntimeMapping;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryParam;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.TermQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.WildcardQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort.FieldSortValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort.GeoDistanceSortValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort.MissingSortValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort.NestedSortValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort.OrderedSortValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.sort.UnmappedTypeSortValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.ErrorCause;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.ErrorResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.task.CancelTaskResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientIntegrationService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.ElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultElasticsearchRestClient implements ElasticsearchRestClient {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private final Logger logger;
    private final AppbaseCore appbaseCore;
    private final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService;

    /**
     * Cluster key this client view is bound to, or {@code null} for the default cluster.
     *
     * <p>The reactive (and, later, sync) keyed accessor getters live at this typed-client layer
     * rather than being free from an {@link ElasticsearchLowLevelRestClientService} /
     * {@code AppbaseClientService}, so multi-cluster routing for a keyed accessor is threaded
     * through here: {@link #keyBound(AppbaseClientKey)} produces a private-ctor view carrying a
     * non-null {@code boundKey}, and {@link #integrationService()} resolves against it.
     * {@code boundKey == null} is the default cluster and preserves the pre-twin (single-cluster)
     * behavior exactly -- every existing caller of the {@code @Inject} constructor is unaffected.
     */
    @Nullable
    private final AppbaseClientKey<ClusterIdValue> boundKey;

    @Inject
    public DefaultElasticsearchRestClient(
            final AppbaseCore appbaseCore,
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService)
    {
        this(appbaseCore, elasticsearchLowLevelRestClientService, null);

        registerObjectMapperSubtypes();
    }

    /**
     * Shared constructor for both the default client (via the {@code @Inject} constructor, which
     * passes {@code boundKey = null}) and a key-bound view of this client (produced by {@link
     * #keyBound(AppbaseClientKey)}, which passes a non-null {@code boundKey}). Does not run
     * {@link #registerObjectMapperSubtypes()} itself: that registration is a one-time, class-level
     * (static) side effect the {@code @Inject} constructor performs once, after delegating here --
     * a key-bound view is always derived from an already-constructed default client, so it never
     * needs to re-run it.
     *
     * @param appbaseCore Appbase core, providing the log service
     * @param elasticsearchLowLevelRestClientService Low-level Elasticsearch REST client service
     * @param boundKey Client key this view is bound to, or {@code null} for the default cluster
     */
    private DefaultElasticsearchRestClient(
            final AppbaseCore appbaseCore,
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService,
            @Nullable final AppbaseClientKey<ClusterIdValue> boundKey)
    {
        AppbaseUtil.notNull(
                elasticsearchLowLevelRestClientService,
                "elasticsearchLowLevelRestClientService");

        this.appbaseCore = appbaseCore;
        this.elasticsearchLowLevelRestClientService = elasticsearchLowLevelRestClientService;
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.boundKey = boundKey;
    }

    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(BooleanSearchQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(BoostingSearchQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(ConstantScoreSearchQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(DisjunctionMaxSearchQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(MatchQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(RangeQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(RegexQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(TermQuery.class);
        AppbaseCoreObjectMapperSubTypes.register(WildcardQuery.class);


        AppbaseCoreObjectMapperSubTypes.register(FieldRuntimeMapping.class);
        AppbaseCoreObjectMapperSubTypes.register(ScriptRuntimeMapping.class);


        AppbaseCoreObjectMapperSubTypes.register(FieldSortValue.class);
        AppbaseCoreObjectMapperSubTypes.register(NestedSortValue.class);
        AppbaseCoreObjectMapperSubTypes.register(OrderedSortValue.class);
        AppbaseCoreObjectMapperSubTypes.register(MissingSortValue.class);
        AppbaseCoreObjectMapperSubTypes.register(UnmappedTypeSortValue.class);
        AppbaseCoreObjectMapperSubTypes.register(GeoDistanceSortValue.class);
    }

    /**
     * Resolve the {@link ElasticsearchLowLevelRestClientIntegrationService} every request-facing
     * method on this client borrows from ({@link #sendSyncRequest(Request)}, {@link
     * #sendRequest(Request, CompletableFuture, Function)}, {@link #shutdown()}, {@link
     * #isShutdown()}): the default, no-arg lookup when {@link #boundKey} is {@code null} (every
     * pre-existing, non-keyed caller -- byte-identical to the behavior before this method
     * existed), or the keyed lookup for a {@link #keyBound(AppbaseClientKey)} view.
     *
     * @return Integration service to use for the next request
     */
    private ElasticsearchLowLevelRestClientIntegrationService integrationService() {
        return boundKey == null
                ? elasticsearchLowLevelRestClientService.getClientIntegrationService()
                : elasticsearchLowLevelRestClientService.getClientIntegrationService(boundKey);
    }

    /**
     * Create a view of this client bound to {@code key}, used as the borrow supplier for a
     * keyed reactive accessor.
     *
     * @param key Client key to bind the view to
     * @return Key-bound {@link ElasticsearchRestClient} view
     */
    private ElasticsearchRestClient keyBound(final AppbaseClientKey<ClusterIdValue> key) {
        return new DefaultElasticsearchRestClient(appbaseCore, elasticsearchLowLevelRestClientService, key);
    }

    private ReactiveOperationConfiguration reactiveOperationConfiguration() {
        return elasticsearchLowLevelRestClientService
                .<ElasticsearchLowLevelRestClientServiceConfiguration>getConfiguration()
                .getReactiveOperationConfiguration();
    }

    @Override
    public ReactiveElasticsearchAccessor getReactiveAccessor() {
        return new ReactiveElasticsearchAccessor(
                appbaseCore,
                appbaseCore.getReactorService(),
                reactiveOperationConfiguration(),
                () -> this);
    }

    @Override
    public ReactiveElasticsearchAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> key) {
        return new ReactiveElasticsearchAccessor(
                appbaseCore,
                appbaseCore.getReactorService(),
                reactiveOperationConfiguration(),
                () -> keyBound(key));
    }

    @Override
    public ElasticsearchAccessor getAccessor() {
        return new ElasticsearchAccessor(
                appbaseCore,
                reactiveOperationConfiguration(),
                () -> this);
    }

    @Override
    public ElasticsearchAccessor getAccessor(final AppbaseClientKey<ClusterIdValue> key) {
        return new ElasticsearchAccessor(
                appbaseCore,
                reactiveOperationConfiguration(),
                () -> keyBound(key));
    }

    @Override
    public SearchResponse search(final SearchRequest searchRequest) throws IOException {
        AppbaseUtil.notNull(searchRequest, "searchRequest");

        final Response response = sendSyncRequest(searchElasticsearchRequest(searchRequest));
        return new SearchResponseConverter(logger).apply(response);
    }

    @Override
    public MultiSearchResponse multiSearch(final MultiSearchRequest multiSearchRequest) throws IOException {
        AppbaseUtil.notNull(multiSearchRequest, "multiSearchRequest");

        final Response response = sendSyncRequest(multiSearchElasticsearchRequest(multiSearchRequest));
        return new MultiSearchResponseConverter(logger).apply(response);
    }

    @Override
    public AddOrUpdateDocumentResponse addDocument(final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest) throws IOException {
        AppbaseUtil.notNull(addOrUpdateDocumentRequest, "addOrUpdateDocumentRequest");

        final Response response = sendSyncRequest(addOrUpdateDocumentRequest(addOrUpdateDocumentRequest, true, false));
        return new AddDocumentResponseConverter(logger).apply(response);
    }

    @Override
    public AddOrUpdateDocumentResponse updateDocument(final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest) throws IOException {
        AppbaseUtil.notNull(addOrUpdateDocumentRequest, "addOrUpdateDocumentRequest");

        final Response response = sendSyncRequest(addOrUpdateDocumentRequest(addOrUpdateDocumentRequest, false, true));
        return new AddDocumentResponseConverter(logger).apply(response);
    }

    @Override
    public AddOrUpdateDocumentResponse addOrUpdateDocument(final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest) throws IOException {
        AppbaseUtil.notNull(addOrUpdateDocumentRequest, "addOrUpdateDocumentRequest");

        final Response response = sendSyncRequest(addOrUpdateDocumentRequest(addOrUpdateDocumentRequest, false, true));
        return new AddDocumentResponseConverter(logger).apply(response);
    }

    @Override
    public DeleteDocumentResponse deleteDocument(final DeleteDocumentRequest deleteDocumentRequest) throws IOException {
        AppbaseUtil.notNull(deleteDocumentRequest, "deleteDocumentRequest");

        final Response response = sendSyncRequest(deleteDocumentRequest(deleteDocumentRequest));
        return new DeleteDocumentResponseConverter(logger).apply(response);
    }

    @Override
    public CreateIndexResponse createIndex(final CreateIndexRequest createIndexRequest) throws IOException {
        AppbaseUtil.notNull(createIndexRequest, "createIndexRequest");

        final Response response = sendSyncRequest(createIndexElasticsearchRequest(createIndexRequest));
        return new CreateIndexResponseConverter().apply(response);
    }

    @Override
    public ExistsIndexResponse existsIndex(final ExistsIndexRequest existsIndexRequest) throws IOException {
        AppbaseUtil.notNull(existsIndexRequest, "existsIndexRequest");

        final Response response = sendSyncRequest(existsIndexRequest(existsIndexRequest));
        return new ExistsIndexResponseConverter().apply(response);
    }

    @Override
    public DeleteIndexResponse deleteIndex(final DeleteIndexRequest deleteIndexRequest) throws IOException {
        AppbaseUtil.notNull(deleteIndexRequest, "deleteIndexRequest");

        final Response response = sendSyncRequest(deleteIndexRequest(deleteIndexRequest));
        return new DeleteIndexResponseConverter().apply(response);
    }

    @Override
    public CancelTaskResponse cancelTask(final CancelTaskRequest cancelTaskRequest) throws IOException {
        AppbaseUtil.notNull(cancelTaskRequest, "cancelTaskRequest");

        final Response response = sendSyncRequest(cancelTaskRequest(cancelTaskRequest));
        return new CancelTaskResponseConverter().apply(response);
    }

    @Override
    public CompletableFuture<CreateIndexResponse> asyncCreateIndex(final CreateIndexRequest createIndexRequest) {
        final CompletableFuture<CreateIndexResponse> future = new CompletableFuture<>();
        try {
            final Request request = createIndexElasticsearchRequest(createIndexRequest);
            sendRequest(request, future, new CreateIndexResponseConverter());
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<SearchResponse> asyncSearch(
            final SearchRequest searchRequest)
    {
        final CompletableFuture<SearchResponse> future = new CompletableFuture<>();
        try {
            final Request request = searchElasticsearchRequest(searchRequest);
            sendRequest(request, future, new SearchResponseConverter(logger));
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<MultiSearchResponse> asyncMultiSearch(
            final MultiSearchRequest multiSearchRequest)
    {
        final CompletableFuture<MultiSearchResponse> future = new CompletableFuture<>();
        try {
            final Request request = multiSearchElasticsearchRequest(multiSearchRequest);
            sendRequest(request, future, new MultiSearchResponseConverter(logger));
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<AddOrUpdateDocumentResponse> asyncAddDocument(
            final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest)
    {
        final CompletableFuture<AddOrUpdateDocumentResponse> future = new CompletableFuture<>();
        try {
            final Request request = addOrUpdateDocumentRequest(
                    addOrUpdateDocumentRequest, true, false);
            sendRequest(request, future, new AddDocumentResponseConverter(logger));
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<AddOrUpdateDocumentResponse> asyncUpdateDocument(
            final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest)
    {
        final CompletableFuture<AddOrUpdateDocumentResponse> future = new CompletableFuture<>();
        try {
            final Request request = addOrUpdateDocumentRequest(
                    addOrUpdateDocumentRequest, false, true);
            sendRequest(request, future, new AddDocumentResponseConverter(logger));
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<AddOrUpdateDocumentResponse> asyncAddOrUpdateDocument(
            final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest)
    {
        final CompletableFuture<AddOrUpdateDocumentResponse> future = new CompletableFuture<>();
        try {
            final Request request = addOrUpdateDocumentRequest(
                    addOrUpdateDocumentRequest, false, false);
            sendRequest(request, future, new AddDocumentResponseConverter(logger));
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<DeleteDocumentResponse> asyncDeleteDocument(
            final DeleteDocumentRequest deleteDocumentRequest)
    {
        final CompletableFuture<DeleteDocumentResponse> future = new CompletableFuture<>();
        try {
            final Request request = deleteDocumentRequest(deleteDocumentRequest);
            sendRequest(request, future, new DeleteDocumentResponseConverter(logger));
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<ExistsIndexResponse> asyncExistsIndex(
            final ExistsIndexRequest existsIndexRequest)
    {
        final CompletableFuture<ExistsIndexResponse> future = new CompletableFuture<>();
        try {
            final Request request = existsIndexRequest(existsIndexRequest);
            sendRequest(request, future, new ExistsIndexResponseConverter());
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<DeleteIndexResponse> asyncDeleteIndex(
            final DeleteIndexRequest deleteIndexRequest)
    {
        final CompletableFuture<DeleteIndexResponse> future = new CompletableFuture<>();
        try {
            final Request request = deleteIndexRequest(deleteIndexRequest);
            sendRequest(request, future, new DeleteIndexResponseConverter());
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public CompletableFuture<CancelTaskResponse> asyncCancelTask(
            final CancelTaskRequest cancelTaskRequest)
    {
        final CompletableFuture<CancelTaskResponse> future = new CompletableFuture<>();
        try {
            final Request request = cancelTaskRequest(cancelTaskRequest);
            sendRequest(request, future, new CancelTaskResponseConverter());
        } catch (IOException ioe) {
            future.completeExceptionally(ioe);
        }

        return future;
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        var client = integrationService()
                .getClient();
        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "appbase-es-rest-client-shutdown");
            t.setDaemon(true);
            return t;
        });
        try {
            executor.submit(() -> {
                try {
                    client.close();
                } catch (IOException ioe) {
                    throw new AppbaseRuntimeException("Error closing elasticsearch client", ioe);
                }
            }).get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            logger.warn("Elasticsearch RestClient close timed out after {} seconds", SHUTDOWN_TIMEOUT_SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Elasticsearch RestClient close interrupted");
        } catch (ExecutionException ee) {
            final Throwable cause = ee.getCause();
            if (cause instanceof AppbaseRuntimeException are) {
                throw are;
            }
            throw new AppbaseRuntimeException("Error closing elasticsearch client", cause);
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isShutdown() {
        return !integrationService()
                .getClient()
                .isRunning();
    }

    private Request searchElasticsearchRequest(
            final SearchRequest searchRequest) throws IOException
    {
        AppbaseUtil.notNull(searchRequest, "searchRequest");

        final StringBuilder pathBuilder = new StringBuilder("/");
        final String targetCsv = searchRequest.getTargetCsv();
        if (targetCsv != null) {
            pathBuilder.append(targetCsv).append("/");
        }
        pathBuilder.append("_search");

        final Map<SearchQueryParam, Object> queryParameters =
                searchRequest.getQueryParameters();
        final String requestUrl =
                appendSearchQueryParameters(pathBuilder.toString(), queryParameters);
        logger.info("Search request URL={}", requestUrl);

        final Request request = new Request("GET", requestUrl);

        final SearchQueryBody searchQueryBody = searchRequest.getSearchQueryBody();
        if (searchQueryBody != null) {
            final String json = JsonUtil
                    .getJsonMapper()
                    .writerFor(SearchQueryBody.class)
                    .writeValueAsString(searchQueryBody);
            request.setJsonEntity(json);
            logger.info("Search request JSON={}", json);
        }

        return request;
    }

    private void addToMap(
            final Map<SearchQueryParam, Object> queryParameters,
            final SearchQueryParam searchQueryParam,
            final Object value)
    {
        if (value != null) {
            queryParameters.put(searchQueryParam, value);
        }
    }

    private void addToStringMap(
            final Map<String, String> queryParameters,
            final String searchQueryParam,
            final Object value)
    {
        if (value != null) {
            queryParameters.put(searchQueryParam, String.valueOf(value));
        }
    }

    private Request deleteDocumentRequest(
            final DeleteDocumentRequest deleteDocumentRequest) throws IOException
    {
        AppbaseUtil.notNull(deleteDocumentRequest, "deleteDocumentRequest");

        String pathBuilder = "/" +
                encodePathSegment(deleteDocumentRequest.getIndex()) +
                "/_doc/" +
                encodePathSegment(deleteDocumentRequest.getId());

        final Map<String, String> queryParameters = new HashMap<>();
        addToStringMap(
                queryParameters,
                "if_seq_no",
                deleteDocumentRequest.getIfSeqNo());
        addToStringMap(
                queryParameters,
                "if_primary_term",
                deleteDocumentRequest.getIfPrimaryTerm());
        addToStringMap(
                queryParameters,
                "refresh",
                deleteDocumentRequest.getRefresh());
        addToStringMap(
                queryParameters,
                "timeout",
                deleteDocumentRequest.getTimeout());
        addToStringMap(
                queryParameters,
                "version",
                deleteDocumentRequest.getVersion());
        addToStringMap(
                queryParameters,
                "version_type",
                deleteDocumentRequest.getVersionType());
        addToStringMap(
                queryParameters,
                "wait_for_active_shards",
                deleteDocumentRequest.getWaitForActiveShards());

        final String requestUrl = appendQueryParameters(
                pathBuilder,
                queryParameters);
        logger.debug("Add document request URL={}", requestUrl);

        return new Request("DELETE", requestUrl);
    }

    private Request addOrUpdateDocumentRequest(
            final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest,
            final boolean addOnly,
            final boolean updateOnly) throws IOException
    {
        AppbaseUtil.notNull(addOrUpdateDocumentRequest, "addDocumentRequest");

        final String target = addOrUpdateDocumentRequest.getTarget();
        final StringBuilder pathBuilder = new StringBuilder("/")
                .append(target)
                .append("/");
        final String documentId = addOrUpdateDocumentRequest.getDocumentId();

        if (addOnly) {
            pathBuilder.append("_create/");

            if (!AppbaseUtil.isBlank(documentId)) {
                pathBuilder.append(documentId);
            }
        } else {
            pathBuilder.append("_doc/");

            if (updateOnly) {
                AppbaseUtil.notBlank(documentId, "documentId");

                pathBuilder.append(documentId);
            } else if (!AppbaseUtil.isBlank(documentId)) {
                pathBuilder.append(documentId);
            }
        }

        final Map<String, String> queryParameters = new HashMap<>();
        addToStringMap(
                queryParameters,
                "if_seq_no",
                addOrUpdateDocumentRequest.getIfSeqNo());
        addToStringMap(
                queryParameters,
                "if_primary_term",
                addOrUpdateDocumentRequest.getIfPrimaryTerm());
        addToStringMap(
                queryParameters,
                "op_type",
                addOrUpdateDocumentRequest.getOpType());
        addToStringMap(
                queryParameters,
                "pipeline",
                addOrUpdateDocumentRequest.getPipeline());
        addToStringMap(
                queryParameters,
                "refresh",
                addOrUpdateDocumentRequest.getRefresh());
        addToStringMap(
                queryParameters,
                "routing",
                addOrUpdateDocumentRequest.getRouting());
        addToStringMap(
                queryParameters,
                "timeout",
                addOrUpdateDocumentRequest.getTimeout());
        addToStringMap(
                queryParameters,
                "version",
                addOrUpdateDocumentRequest.getVersion());
        addToStringMap(
                queryParameters,
                "version_type",
                addOrUpdateDocumentRequest.getVersionType());
        addToStringMap(
                queryParameters,
                "wait_for_active_shards",
                addOrUpdateDocumentRequest.getVersionType());
        addToStringMap(
                queryParameters,
                "require_alias",
                addOrUpdateDocumentRequest.getWaitForActiveShards());

        final String requestUrl = appendQueryParameters(
                pathBuilder.toString(),
                queryParameters);
        logger.debug("Add document request URL={}", requestUrl);

        final Request request;
        if (addOnly) {
            request = new Request("POST", requestUrl);
        } else {
            request = new Request("PUT", requestUrl);
        }

        final String requestJson = addOrUpdateDocumentRequest.getRequestJson();

        logger.debug("Add document request JSON={}", requestJson);
        request.setJsonEntity(requestJson);

        return request;
    }

    private Request multiSearchElasticsearchRequest(
            final MultiSearchRequest multiSearchRequest) throws IOException
    {
        AppbaseUtil.notNull(multiSearchRequest, "multiSearchRequest");

        final StringBuilder pathBuilder = new StringBuilder("/");
        final String targetCsv = multiSearchRequest.getTargetCsv();
        if (targetCsv != null) {
            pathBuilder.append(targetCsv).append("/");
        }
        pathBuilder.append("_msearch");

        final Map<SearchQueryParam, Object> queryParameters = new HashMap<>();
        addToMap(queryParameters,
                SearchQueryParam.ALLOW_NO_INDICES, multiSearchRequest.getAllowNoIndices());
        addToMap(queryParameters,
                SearchQueryParam.CCS_MINIMIZE_ROUND_TRIPS,
                multiSearchRequest.getCcsMinimizeRoundTrips());
        addToMap(queryParameters,
                SearchQueryParam.EXPAND_WILDCARDS,
                multiSearchRequest.getExpandWildcards());
        addToMap(queryParameters,
                SearchQueryParam.IGNORE_THROTTLED,
                multiSearchRequest.getIgnoreThrottled());
        addToMap(queryParameters,
                SearchQueryParam.IGNORE_UNAVAILABLE,
                multiSearchRequest.getIgnoreUnavailable());
        addToMap(queryParameters,
                SearchQueryParam.MAX_CONCURRENT_SEARCHES,
                multiSearchRequest.getMaxConcurrentSearches());
        addToMap(queryParameters,
                SearchQueryParam.MAX_CONCURRENT_SHARD_REQUESTS,
                multiSearchRequest.getMaxConcurrentShardRequests());
        addToMap(queryParameters,
                SearchQueryParam.PRE_FILTER_SHARD_SIZE,
                multiSearchRequest.getPreFilterShardSize());
        addToMap(queryParameters,
                SearchQueryParam.REST_TOTAL_HITS_AS_INT,
                multiSearchRequest.getRestTotalHitsAsInt());
        addToMap(queryParameters,
                SearchQueryParam.ROUTING, multiSearchRequest.getRouting());
        addToMap(queryParameters,
                SearchQueryParam.SEARCH_TYPE, multiSearchRequest.getSearchType());
        addToMap(queryParameters,
                SearchQueryParam.TYPED_KEYS, multiSearchRequest.getTypedKeys());
        final String requestUrl =
                appendSearchQueryParameters(pathBuilder.toString(), queryParameters);
        logger.debug("Multi search request URL={}", requestUrl);

        final Request request = new Request("GET", requestUrl);

        final StringBuilder requestBodyBuilder = new StringBuilder();
        final List<MultiSearchRequestEnvelope> multiSearchRequests =
                multiSearchRequest.getMultiSearchRequests();
        multiSearchRequests.forEach(multiSearchRequestEnvelope -> {
            final MultiSearchRequestHeader header =
                    multiSearchRequestEnvelope.getMultiSearchRequestHeader();
            try {
                final String headerStr = JsonUtil
                        .getJsonMapper()
                        .writerFor(MultiSearchRequestHeader.class)
                        .writeValueAsString(header);
                requestBodyBuilder.append(headerStr).append("\n");
            } catch (IOException ioe) {
                throw new RuntimeException(
                        "Error serializing multi search header: " + header, ioe);
            }

            final MultiSearchRequestBody body =
                    multiSearchRequestEnvelope.getMultiSearchRequestBody();
            try {
                final String bodyStr = JsonUtil
                        .getJsonMapper()
                        .writerFor(MultiSearchRequestBody.class)
                        .writeValueAsString(body);
                logger.debug("Multi search request JSON={}", bodyStr);
                requestBodyBuilder.append(bodyStr).append("\n");
            } catch (IOException ioe) {
                throw new RuntimeException(
                        "Error serializing multi search body: " + header, ioe);
            }
        });

        final String requestBody = requestBodyBuilder.toString();
        if (!requestBody.isBlank()) {
            final RequestOptions requestOptions = RequestOptions.DEFAULT.toBuilder()
                    .addHeader("Content-Type", "application/x-ndjson")
                    .build();
            request.setOptions(requestOptions);
            request.setJsonEntity(requestBody);
        }

        return request;
    }

    private Request createIndexElasticsearchRequest(
            final CreateIndexRequest createIndexRequest) throws IOException
    {
        AppbaseUtil.notNull(createIndexRequest, "createIndexRequest");

        final String indexName = createIndexRequest.getIndexName();
        AppbaseUtil.notBlank(indexName, "indexName");

        final Map<String, String> queryParams = createIndexRequest.getQueryParams();
        final String requestUrl = appendQueryParameters("/" + encodePathSegment(indexName), queryParams);
        logger.debug("Create index request URL={}", requestUrl);

        final Request request = new Request("PUT", requestUrl);

        final CreateIndexRequestBody requestBody = createIndexRequest.getRequestBody();
        if (requestBody != null) {
            final String jsonRequestBody = JsonUtil
                    .getJsonMapper()
                    .writerFor(CreateIndexRequestBody.class)
                    .writeValueAsString(requestBody);
            logger.debug("Create index request JSON={}", jsonRequestBody);
            request.setJsonEntity(jsonRequestBody);
        }

        return request;
    }

    private Request existsIndexRequest(
            final ExistsIndexRequest existsIndexRequest) throws IOException
    {
        AppbaseUtil.notNull(existsIndexRequest, "existsIndexRequest");

        final String indexName = existsIndexRequest.getIndexName();
        AppbaseUtil.notBlank(indexName, "indexName");

        final Map<String, String> queryParams = existsIndexRequest.getQueryParams();
        final String requestUrl = appendQueryParameters(encodePathSegment(indexName), queryParams);
        logger.debug("Exists index request URL={}", existsIndexRequest);

        return new Request("HEAD", requestUrl);
    }

    private Request deleteIndexRequest(
            final DeleteIndexRequest deleteIndexRequest) throws IOException
    {
        AppbaseUtil.notNull(deleteIndexRequest, "deleteIndexRequest");

        final String indexName = deleteIndexRequest.getIndexName();
        AppbaseUtil.notBlank(indexName, "indexName");

        final Map<String, String> queryParams = deleteIndexRequest.getQueryParams();
        final String requestUrl = appendQueryParameters(encodePathSegment(indexName), queryParams);
        logger.debug("Delete index request URL={}", requestUrl);

        return new Request("DELETE", requestUrl);
    }

    private Request cancelTaskRequest(
            final CancelTaskRequest cancelTaskRequest) throws IOException
    {
        AppbaseUtil.notNull(cancelTaskRequest, "cancelTaskRequest");

        final String taskId = cancelTaskRequest.getTaskId();
        AppbaseUtil.notBlank(taskId, "taskId");

        final String requestUrl = "_tasks/" + encodePathSegment(taskId) + "/_cancel";
        logger.debug("Cancel task request URL={}", requestUrl);

        return new Request("POST", requestUrl);
    }

    private Response sendSyncRequest(final Request request) throws IOException {
        return integrationService()
                .getClient()
                .performRequest(request);
    }

    private <T> void sendRequest(
            final Request request,
            final CompletableFuture<T> future,
            final Function<Response, T> converter)
    {
        logger.debug("Sending request={}", request);

        integrationService()
                .getClient()
                .performRequestAsync(request, new ResponseListener() {
                    @Override
                    public void onSuccess(final Response response) {
                        final T converted = converter.apply(response);
                        future.complete(converted);
                    }

                    @Override
                    public void onFailure(final Exception exception) {
                        future.completeExceptionally(exception);
                    }
                });
    }

    /**
     * Builds {@code path?k1=v1&k2=v2} with every key and value percent-encoded.
     *
     * <p>⚠️ Package-private and {@code static} <strong>so the encoding can be tested without a live
     * Elasticsearch cluster.</strong> It reads no instance state. Every other test in this class
     * needs a running cluster, which is why F-06 shipped in internal work item 802 with no regression test at all
     * (internal work item 806 R-3) — the encoding was untestable only because of where it sat, not what it did.
     *
     * @param path        the request path to append the query string to
     * @param queryParams the parameters; null or empty returns {@code path} unchanged
     * @return the path with an encoded query string, or the bare path when there are no parameters
     */
    static String appendQueryParameters(
            final String path,
            @Nullable
            final Map<String, String> queryParams)
    {
        if (queryParams == null || queryParams.isEmpty()) {
            return path;
        }

        final StringBuilder result = new StringBuilder(path + "?");
        queryParams.forEach((key, value) ->
                appendQueryParameter(key, value, result));
        return deleteLastChar(result);
    }

    static String appendSearchQueryParameters(
            final String path,
            @Nullable
            final Map<SearchQueryParam, Object> queryParams)
    {
        if (queryParams == null || queryParams.isEmpty()) {
            return path;
        }

        final StringBuilder result = new StringBuilder(path + "?");
        queryParams.forEach((key, value) ->
                appendQueryParameter(key.getText(), value, result));
        return deleteLastChar(result);
    }

    /**
     * Appends one percent-encoded {@code key=value&} pair to the query string.
     *
     * <p>⚠️ Both halves are encoded. Unencoded, a key or value containing {@code &}, {@code =} or
     * {@code #} would inject additional query parameters into the Elasticsearch request rather
     * than being carried as data (internal work item 751 F-06).
     *
     * <p>{@link URLEncoder} implements {@code application/x-www-form-urlencoded}, which maps a
     * space to {@code +}. That is correct in a query string and is what Elasticsearch expects.
     *
     * @param key   the parameter name
     * @param value the parameter value; {@code null} is encoded as the literal {@code "null"}
     * @param sb    the query-string builder to append to
     */
    static void appendQueryParameter(
            final String key,
            final Object value,
            final StringBuilder sb)
    {
        sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8))
                .append("&");
    }

    /**
     * AUD-32 (CWE-116 family): percent-encodes {@code rawSegment} so it can only ever occupy ONE
     * REST path segment, never more and never fewer.
     *
     * <p>Caller-supplied identifiers (index name, document id, task id) were raw-concatenated
     * into the request path with no encoding. {@code '/'} lets one identifier introduce EXTRA
     * path segments; {@code '?'} lets it introduce an unintended query string; traversal-shaped
     * content (e.g. {@code "../"}) changes which endpoint the path names. Query PARAMETERS are
     * already encoded via {@link #appendQueryParameter}; this is the path-segment counterpart.
     *
     * <p>Deliberately NOT {@link URLEncoder} (used above for query parameters): that is {@code
     * application/x-www-form-urlencoded}, which maps a literal space to {@code '+'}. A {@code
     * '+'} has no special meaning in a URI PATH, so an identifier legitimately containing one
     * would be silently changed. This keeps every character that is a safe literal in a path
     * segment ({@code [A-Za-z0-9._~-]}) and percent-encodes everything else, including a space
     * as {@code %20}.
     *
     * @param rawSegment the untrusted identifier to confine to exactly one path segment
     * @return the percent-encoded segment
     */
    static String encodePathSegment(final String rawSegment) {
        AppbaseUtil.notNull(rawSegment, "rawSegment");

        final StringBuilder encoded = new StringBuilder(rawSegment.length());
        for (final byte rawByte : rawSegment.getBytes(StandardCharsets.UTF_8)) {
            final int b = rawByte & 0xFF;
            final boolean isUnreserved = (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z')
                    || (b >= '0' && b <= '9') || b == '-' || b == '.' || b == '_' || b == '~';
            if (isUnreserved) {
                encoded.append((char) b);
            } else {
                encoded.append('%').append(String.format("%02X", b));
            }
        }
        return encoded.toString();
    }

    static String deleteLastChar(final StringBuilder sb) {
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    private static boolean isSuccess(final Response response) {
        if (response == null) {
            return false;
        }

        final StatusLine statusLine = response.getStatusLine();
        if (statusLine == null) {
            return false;
        }

        final int statusCode = statusLine.getStatusCode();
        return statusCode > 199 && statusCode < 300;
    }

    private static ErrorResponseBody errorResponseBody(final Throwable throwable) {
        final ErrorResponseBody errorResponseBody = new ErrorResponseBody();
        final ErrorCause errorCause = new ErrorCause();
        errorCause.setType(Throwable.class.getName());
        errorCause.setReason(throwable.getMessage());
        errorResponseBody.setRootCause(errorCause);

        return errorResponseBody;
    }

    private static SearchResponse searchErrorResponse(final Throwable throwable) {
        return new SearchResponse(errorResponseBody(throwable));
    }

    private static MultiSearchResponse multiSearchErrorResponse(final Throwable throwable) {
        return new MultiSearchResponse(errorResponseBody(throwable));
    }

    private static AddOrUpdateDocumentResponse addDocumentResponse(final Throwable throwable) {
        return new AddOrUpdateDocumentResponse(errorResponseBody(throwable));
    }

    private static DeleteDocumentResponse deleteDocumentResponse(final Throwable throwable) {
        return new DeleteDocumentResponse(errorResponseBody(throwable));
    }

    private static class SearchResponseConverter
            implements Function<Response, SearchResponse>
    {
        private final Logger logger;

        private SearchResponseConverter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public SearchResponse apply(final Response response) {
            final boolean success = isSuccess(response);
            final HttpEntity entity = response.getEntity();

            if (!success) {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final ErrorResponseBody errorResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(ErrorResponseBody.class)
                                    .readValue(entityString);
                            return new SearchResponse(errorResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting error response from Elasticsearch", ioe);
                        return searchErrorResponse(ioe);
                    }
                }

                return new SearchResponse(true);
            } else {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final SearchResponseBody searchResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(SearchResponseBody.class)
                                    .readValue(entityString);
                            return new SearchResponse(searchResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting search response from Elasticsearch", ioe);
                        return searchErrorResponse(ioe);
                    }
                }

                return new SearchResponse(false);
            }
        }
    }

    private static class MultiSearchResponseConverter
            implements Function<Response, MultiSearchResponse>
    {
        private final Logger logger;

        private MultiSearchResponseConverter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public MultiSearchResponse apply(final Response response) {
            final boolean success = isSuccess(response);
            final HttpEntity entity = response.getEntity();

            if (!success) {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final ErrorResponseBody errorResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(ErrorResponseBody.class)
                                    .readValue(entityString);
                            return new MultiSearchResponse(errorResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting error response from Elasticsearch", ioe);
                        return multiSearchErrorResponse(ioe);
                    }
                }

                return new MultiSearchResponse(true);
            } else {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final MultiSearchResponseBody multiSearchResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(MultiSearchResponseBody.class)
                                    .readValue(entityString);
                            return new MultiSearchResponse(multiSearchResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting mulit-search response from Elasticsearch", ioe);
                        return multiSearchErrorResponse(ioe);
                    }
                }

                return new MultiSearchResponse(false);
            }
        }
    }

    private static class DeleteDocumentResponseConverter
            implements Function<Response, DeleteDocumentResponse>
    {
        private final Logger logger;

        public DeleteDocumentResponseConverter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public DeleteDocumentResponse apply(Response response) {
            final boolean success = isSuccess(response);
            final HttpEntity entity = response.getEntity();

            if (!success) {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final ErrorResponseBody errorResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(ErrorResponseBody.class)
                                    .readValue(entityString);
                            return new DeleteDocumentResponse(errorResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting error response from Elasticsearch", ioe);
                        return deleteDocumentResponse(ioe);
                    }
                }

                return new DeleteDocumentResponse(true);
            } else {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final DeleteDocumentResponseBody deleteDocumentResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(DeleteDocumentResponseBody.class)
                                    .readValue(entityString);
                            return new DeleteDocumentResponse(deleteDocumentResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting delete document response from Elasticsearch", ioe);
                        return deleteDocumentResponse(ioe);
                    }
                }

                return new DeleteDocumentResponse(false);
            }
        }
    }
    
    private static class AddDocumentResponseConverter 
            implements Function<Response, AddOrUpdateDocumentResponse>
    {
        private final Logger logger;

        public AddDocumentResponseConverter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public AddOrUpdateDocumentResponse apply(final Response response) {
            final boolean success = isSuccess(response);
            final HttpEntity entity = response.getEntity();

            if (!success) {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final ErrorResponseBody errorResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(ErrorResponseBody.class)
                                    .readValue(entityString);
                            return new AddOrUpdateDocumentResponse(errorResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting error response from Elasticsearch", ioe);
                        return addDocumentResponse(ioe);
                    }
                }

                return new AddOrUpdateDocumentResponse(true);
            } else {
                if (entity != null) {
                    try {
                        final String entityString = EntityUtils.toString(entity);
                        if (!AppbaseUtil.isBlank(entityString)) {
                            final AddOrUpdateDocumentResponseBody addOrUpdateDocumentResponseBody = JsonUtil
                                    .getJsonMapper()
                                    .readerFor(AddOrUpdateDocumentResponseBody.class)
                                    .readValue(entityString);
                            return new AddOrUpdateDocumentResponse(addOrUpdateDocumentResponseBody);
                        }
                    } catch (IOException ioe) {
                        logger.error("Exception getting add document response from Elasticsearch", ioe);
                        return addDocumentResponse(ioe);
                    }
                }

                return new AddOrUpdateDocumentResponse(false);
            }
        }
    }

    private static class CreateIndexResponseConverter
            implements Function<Response, CreateIndexResponse>
    {
        @Override
        public CreateIndexResponse apply(final Response response) {
            final boolean success = isSuccess(response);
            return new CreateIndexResponse(success);
        }
    }

    private static class ExistsIndexResponseConverter
            implements Function<Response, ExistsIndexResponse>
    {
        @Override
        public ExistsIndexResponse apply(final Response response) {
            final boolean success = isSuccess(response);
            return new ExistsIndexResponse(success);
        }
    }

    private static class DeleteIndexResponseConverter
            implements Function<Response, DeleteIndexResponse>
    {
        @Override
        public DeleteIndexResponse apply(final Response response) {
            final int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 404) {
                // Not deleted, because it was not found
                return new DeleteIndexResponse(false, false);
            }

            // Deleted because it was found
            return new DeleteIndexResponse(true, true);
        }
    }

    private static class CancelTaskResponseConverter
            implements Function<Response, CancelTaskResponse>
    {
        @Override
        public CancelTaskResponse apply(final Response response) {
            final boolean success = isSuccess(response);
            return new CancelTaskResponse(success);
        }
    }
}
