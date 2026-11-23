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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.DefaultElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchAccessor;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchAccessorException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.MatchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.Hits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientIntegrationService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientServiceTestHelper;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.util.ElasticsearchRestClientUtil;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Gated integration test exercising both faces of the Elasticsearch accessor layer --
 * {@link ReactiveElasticsearchAccessor} and {@link ElasticsearchAccessor} -- against a live
 * Elasticsearch cluster, mirroring the bootstrap of {@code DefaultElasticsearchRestClientTest}.
 *
 * <p>Requires a running Elasticsearch reachable per the module's gated-test config
 * (system property {@code ElasticsearchClientServiceTests.enabled=true}, which defaults to
 * {@code false} since N10 (internal work item 1300), like every other client's gate; Elasticsearch at
 * {@code localhost:9200}).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")
class ReactiveElasticsearchAccessorIT {
    private static final long SLEEP_SECONDS = 5;
    private static final String DOCUMENT_ID = "1";
    /** Version the lifecycle pins (appbase-client-elasticsearch/infra/docker/elasticsearch/env-elasticsearch.sh). */
    private static final String PINNED_VERSION = "8.5.3";
    private static final String BAD_PORT_RESOURCE = "/appbase-client-elasticsearch-low-level-badport.yaml";
    private static final String MISSING_INDEX_TYPE = "index_not_found_exception";

    private static AppbaseCore appbaseCore;
    private static DefaultElasticsearchLowLevelRestClientService defaultElasticsearchRestClientService;
    private static ElasticsearchRestClient elasticsearchRestClient;
    private static String reactiveIndexName;
    private static String syncIndexName;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultElasticsearchRestClient.registerObjectMapperSubtypes();

        reactiveIndexName = uniqueIndexName("reactive");
        syncIndexName = uniqueIndexName("sync");

        appbaseCore = ElasticsearchLowLevelRestClientServiceTestHelper.appbaseCore();
        defaultElasticsearchRestClientService = ElasticsearchLowLevelRestClientServiceTestHelper
                .defaultElasticsearchRestClientService(appbaseCore);
        elasticsearchRestClient = new DefaultElasticsearchRestClient(
                appbaseCore, defaultElasticsearchRestClientService);
    }

    @AfterAll
    static void tearDown() {
        cleanup(elasticsearchRestClient, defaultElasticsearchRestClientService, appbaseCore,
                reactiveIndexName, syncIndexName);
    }

    /** Attempt every cleanup action and retain every failure instead of aborting after the first one. */
    static void cleanup(final ElasticsearchRestClient client,
            final DefaultElasticsearchLowLevelRestClientService service,
            final AppbaseCore core, final String reactiveIndex, final String syncIndex)
    {
        Assertions.assertAll("Elasticsearch fixture cleanup",
                () -> deleteIndexIfPresent(client, reactiveIndex),
                () -> deleteIndexIfPresent(client, syncIndex),
                () -> {
                    if (client != null) {
                        client.shutdown();
                    }
                },
                () -> {
                    if (service != null) {
                        service.shutdown();
                    }
                },
                () -> {
                    if (core != null) {
                        core.shutdown();
                    }
                });
    }

    private static void deleteIndexIfPresent(final ElasticsearchRestClient client, final String index)
            throws Exception
    {
        if (client != null && index != null) {
            client.asyncDeleteIndex(new DeleteIndexRequest(index)).get(SLEEP_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static String uniqueIndexName(final String prefix) {
        return (prefix +
                ReactiveElasticsearchAccessorIT.class.getSimpleName() + Long.toString(System.nanoTime())).toLowerCase();
    }

    /**
     * Reactive round trip via {@link ReactiveElasticsearchAccessor}: {@code createIndex} ->
     * {@code addDocument} (refreshed immediately so the document is searchable without a sleep) ->
     * {@code search} (asserting the indexed document is found). Index cleanup is handled
     * unconditionally by {@link #tearDown()}, mirroring {@code DefaultElasticsearchRestClientTest},
     * so a failed assertion here does not orphan the index on the live cluster.
     */
    @Test
    void reactiveRoundTrip() {
        final ReactiveElasticsearchAccessor reactive = elasticsearchRestClient.getReactiveAccessor();

        StepVerifier.create(reactive.createIndex("createIndex", createIndexRequest(reactiveIndexName)))
                .assertNext(response -> Assertions.assertTrue(response.isSuccess()))
                .verifyComplete();

        StepVerifier.create(reactive.addDocument("addDocument", addDocumentRequest(reactiveIndexName, DOCUMENT_ID)))
                .assertNext(ReactiveElasticsearchAccessorIT::assertDocumentAdded)
                .verifyComplete();

        StepVerifier.create(reactive.search("search", searchRequest(reactiveIndexName)))
                .assertNext(ReactiveElasticsearchAccessorIT::assertHitFound)
                .verifyComplete();
    }

    /**
     * Synchronous round trip via {@link ElasticsearchAccessor}, mirroring the same
     * {@code createIndex} -> {@code addDocument} -> {@code search} sequence as
     * {@link #reactiveRoundTrip()}. Index cleanup is handled unconditionally by
     * {@link #tearDown()}, mirroring {@code DefaultElasticsearchRestClientTest}, so a failed
     * assertion here does not orphan the index on the live cluster.
     *
     * @throws Exception the mapped exception, if any operation fails
     */
    @Test
    void syncRoundTrip() throws Exception {
        final ElasticsearchAccessor sync = elasticsearchRestClient.getAccessor();

        final CreateIndexResponse createIndexResponse =
                sync.createIndex("createIndex", createIndexRequest(syncIndexName));
        Assertions.assertNotNull(createIndexResponse);
        Assertions.assertTrue(createIndexResponse.isSuccess());

        final AddOrUpdateDocumentResponse addOrUpdateDocumentResponse =
                sync.addDocument("addDocument", addDocumentRequest(syncIndexName, DOCUMENT_ID));
        assertDocumentAdded(addOrUpdateDocumentResponse);

        final SearchResponse searchResponse = sync.search("search", searchRequest(syncIndexName));
        assertHitFound(searchResponse);
    }

    /**
     * Version pin: the node answering on the configured endpoint must be the pinned Elasticsearch version,
     * not whatever happens to listen on the port.
     *
     * @throws Exception if the root endpoint cannot be read
     */
    @Test
    void versionPinIsAsserted() throws Exception {
        final Response response = ((DefaultElasticsearchLowLevelRestClientIntegrationService)
                defaultElasticsearchRestClientService.getClientIntegrationService())
                .getClient()
                .performRequest(new Request("GET", "/"));
        final JsonNode root = JsonUtil.getJsonMapper()
                .readTree(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));

        Assertions.assertEquals(PINNED_VERSION, root.path("version").path("number").asText(),
                "the node must be the pinned Elasticsearch version");
    }

    /**
     * Negative path, reactive: a search against an index that was never created must fail. A clean
     * response with no error and no hits would be a silent pass, so the failure is required, either
     * as an error signal or as an error body on the response.
     */
    @Test
    void negativeMissingIndexSearchFailsReactive() {
        final ReactiveElasticsearchAccessor reactive = elasticsearchRestClient.getReactiveAccessor();
        final String indexName = uniqueIndexName("missing");

        StepVerifier.create(reactive.search("searchMissing", searchRequest(indexName)))
                .expectErrorSatisfies(failure -> assertMissingIndex(failure, indexName))
                .verify();
    }

    /**
     * Negative path, synchronous: the same missing-index search must fail. A thrown exception and an
     * error body on the response both count as a failed search; a clean response does not.
     */
    @Test
    void negativeMissingIndexSearchFailsSync() throws Exception {
        final ElasticsearchAccessor sync = elasticsearchRestClient.getAccessor();
        final String indexName = uniqueIndexName("missing");

        final ElasticsearchAccessorException failure = Assertions.assertThrows(
                ElasticsearchAccessorException.class,
                () -> sync.search("searchMissing", searchRequest(indexName)));
        assertMissingIndex(failure, indexName);
    }

    /**
     * Cleanup assertion: an index deleted through the reactive accessor is no longer searchable. This
     * checks the removal itself, which {@link #tearDown()} does not, since it runs after the assertions
     * and cannot report a leftover index as a failure.
     */
    @Test
    void cleanupDeletedIndexIsNotSearchable() {
        final String indexName = uniqueIndexName("cleanup");
        final ReactiveElasticsearchAccessor reactive = elasticsearchRestClient.getReactiveAccessor();

        StepVerifier.create(reactive.createIndex("createCleanup", createIndexRequest(indexName)))
                .assertNext(response -> Assertions.assertTrue(response.isSuccess()))
                .verifyComplete();

        StepVerifier.create(reactive.deleteIndex("deleteCleanup", new DeleteIndexRequest(indexName)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(reactive.search("searchDeleted", searchRequest(indexName)))
                .expectErrorSatisfies(failure -> assertMissingIndex(failure, indexName))
                .verify();
    }

    /**
     * Negative config path: decode a copied fixture whose port is closed at run time (bound and released
     * here, so nothing listens on it), then construct the Elasticsearch client directly from that decoded
     * configuration. The request must fail with a connection refusal specifically: any other failure,
     * such as a typo in the fixture, must not pass.
     *
     * @throws Exception if the fixture cannot be written
     */
    @Test
    void negativeUnreachableEndpointConfigFails() throws Exception {
        final int closedPort = closedLoopbackPort();
        final String fixtureName = "appbase-client-elasticsearch-low-level-closedport-" + System.nanoTime() + ".yaml";
        final Path fixture = writeFixture(fixtureName, closedPort);

        AppbaseCore badCore = null;
        DefaultAppbaseClientCore badClientCore = null;
        RestClient badClient = null;
        try {
            final DefaultElasticsearchLowLevelRestClientServiceConfiguration badConfiguration =
                    YamlUtil.getYamlMapper()
                            .readerFor(DefaultElasticsearchLowLevelRestClientServiceConfiguration.class)
                            .readValue(Files.readAllBytes(fixture));
            Assertions.assertTrue(badConfiguration
                            .getDefaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration()
                            .getHttpHostConfigurations()
                            .stream()
                            .anyMatch(host -> host.getPort() == closedPort),
                    "decoded fixture must retain generated closed port " + closedPort);
            badCore = ClientCoreTestHelper.appbaseCore();
            badClientCore = ClientCoreTestHelper.defaultAppbaseClientCore(badCore);
            badClient = ElasticsearchRestClientUtil.restClientBuilder(
                    badClientCore,
                    badConfiguration.getDefaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration(),
                    null,
                    null,
                    null).build();
            try {
                badClient.performRequest(new Request("GET", "/"));
            } catch (final IOException failure) {
                Assertions.assertTrue(reportsConnectionRefused(failure),
                        "an unreachable endpoint must report a connection refusal: " + failure);
                return;
            }
            Assertions.fail("a request through closed port " + closedPort + " unexpectedly succeeded");
        } finally {
            if (badClient != null) {
                badClient.close();
            }
            if (badClientCore != null) {
                badClientCore.shutdown();
            }
            if (badCore != null) {
                badCore.shutdown();
            }
            Files.deleteIfExists(fixture);
        }
    }

    private static int closedLoopbackPort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    /**
     * Writes the bad-port fixture next to the compiled test classes, so the classpath store can read it, with
     * the port replaced by the one closed at run time. The substitution is checked, so a fixture that did not
     * change cannot pass as a closed-port test.
     */
    private static Path writeFixture(final String fixtureName, final int port) throws IOException, URISyntaxException {
        final String template;
        try (InputStream in = ReactiveElasticsearchAccessorIT.class.getResourceAsStream(BAD_PORT_RESOURCE)) {
            Assertions.assertNotNull(in, "bad-port fixture missing: " + BAD_PORT_RESOURCE);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        final String clusterId = "closed-port-" + port;
        final String yaml = template
                .replace("clusterId: \"primary\"", "clusterId: \"" + clusterId + "\"")
                .replace("port: 9299", "port: " + port);
        Assertions.assertFalse(yaml.contains("9299"), "the fixture's port must be replaced");
        Assertions.assertTrue(yaml.contains("port: " + port),
                "the fixture must contain the generated closed port");
        Assertions.assertTrue(yaml.contains("clusterId: \"" + clusterId + "\""),
                "the fixture must use a distinct client-pool key");

        final Path testClasses = Paths.get(ReactiveElasticsearchAccessorIT.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        final Path fixture = testClasses.resolve(fixtureName);
        Files.write(fixture, yaml.getBytes(StandardCharsets.UTF_8));
        try (InputStream generated = ReactiveElasticsearchAccessorIT.class
                .getResourceAsStream("/" + fixtureName))
        {
            Assertions.assertNotNull(generated, "generated fixture must be visible on the test classpath");
            final String classpathYaml = new String(generated.readAllBytes(), StandardCharsets.UTF_8);
            Assertions.assertTrue(classpathYaml.contains("port: " + port),
                    "the classpath-visible fixture must contain the generated closed port");
        }
        return fixture;
    }

    /**
     * The failure is a connection refusal: a {@link ConnectException} in the cause chain, or the text
     * "Connection refused". A generic "connect" substring is deliberately not enough.
     */
    private static boolean reportsConnectionRefused(final Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof ConnectException) {
                return true;
            }
            final String message = t.getMessage();
            if (message != null && message.contains("Connection refused")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /**
     * A search against a missing index must return the server's own {@code index_not_found_exception} with
     * the index named in its reason. A clean response, or any other error, does not pass.
     */
    private static void assertMissingIndex(final Throwable failure, final String indexName) {
        final ElasticsearchAccessorException accessorFailure = Assertions.assertInstanceOf(
                ElasticsearchAccessorException.class, failure);
        final ResponseException responseFailure = Assertions.assertInstanceOf(
                ResponseException.class, accessorFailure.getCause());
        Assertions.assertEquals(404, responseFailure.getResponse().getStatusLine().getStatusCode());

        final JsonNode envelope;
        try {
            envelope = JsonUtil.getJsonMapper().readTree(EntityUtils.toString(
                    responseFailure.getResponse().getEntity(), StandardCharsets.UTF_8));
        } catch (final IOException parseFailure) {
            throw new AssertionError("Elasticsearch missing-index error body must be valid JSON", parseFailure);
        }
        final JsonNode error = envelope.path("error");
        Assertions.assertEquals(MISSING_INDEX_TYPE, error.path("type").asText());
        Assertions.assertEquals("no such index [" + indexName + "]", error.path("reason").asText());
    }

    private static void assertDocumentAdded(final AddOrUpdateDocumentResponse response) {
        Assertions.assertNotNull(response);

        final AddOrUpdateDocumentResponseBody body = response.getAddDocumentResponseBody();
        Assertions.assertNotNull(body);
        Assertions.assertNotNull(body.getId());
    }

    private static void assertHitFound(final SearchResponse response) {
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getErrorResponseBody());

        final SearchResponseBody searchResponseBody = response.getSearchResponseBody();
        Assertions.assertNotNull(searchResponseBody);

        final SearchHits searchHits = searchResponseBody.getHits();
        Assertions.assertNotNull(searchHits);

        final List<Hits> hits = searchHits.getHits();
        Assertions.assertNotNull(hits);
        Assertions.assertEquals(1, hits.size());

        final Hits hit = hits.get(0);
        Assertions.assertNotNull(hit);
        Assertions.assertEquals(DOCUMENT_ID, hit.getId());
    }

    private static CreateIndexRequest createIndexRequest(final String indexName) {
        final CreateIndexRequestBody.FieldProperties nameProperties = new CreateIndexRequestBody.FieldProperties();
        nameProperties.setIndex(true);
        nameProperties.setType("text");

        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();
        properties.put("name", nameProperties);

        final CreateIndexRequestBody.Mappings mappings = new CreateIndexRequestBody.Mappings();
        mappings.setProperties(properties);

        final CreateIndexRequestBody createIndexRequestBody = new CreateIndexRequestBody();
        createIndexRequestBody.setMappings(mappings);

        return new CreateIndexRequest(indexName, createIndexRequestBody);
    }

    private static AddOrUpdateDocumentRequest addDocumentRequest(final String indexName, final String documentId) {
        final Document document = new Document();
        document.setName("Leo");

        final String requestJson;
        try {
            requestJson = JsonUtil
                    .getJsonMapper()
                    .writerFor(Document.class)
                    .writeValueAsString(document);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }

        final AddOrUpdateDocumentRequest request = new AddOrUpdateDocumentRequest(requestJson, indexName);
        request.setDocumentId(documentId);
        // Refresh the shard immediately so the document is searchable without a fixed sleep.
        request.setRefresh("true");

        return request;
    }

    private static SearchRequest searchRequest(final String indexName) {
        final MatchQuery.FieldParams nameParams = new MatchQuery.FieldParams();
        nameParams.setQuery("Leo");

        final Map<String, MatchQuery.FieldParams> match = new HashMap<>();
        match.put("name", nameParams);

        final MatchQuery matchQuery = new MatchQuery();
        matchQuery.setMatch(match);

        final SearchQueryBody searchQueryBody = new SearchQueryBody();
        searchQueryBody.setQuery(matchQuery);

        return new SearchRequest(indexName, searchQueryBody);
    }

    private static final class Document {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
