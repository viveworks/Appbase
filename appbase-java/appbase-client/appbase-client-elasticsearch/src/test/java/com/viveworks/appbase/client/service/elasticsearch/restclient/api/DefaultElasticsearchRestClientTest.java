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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.DeleteIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchHits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequestEnvelope;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchRequestHeader;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi.MultiSearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.MatchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQuery;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.Hits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientServiceTestHelper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")
class DefaultElasticsearchRestClientTest {
    private static final long SLEEP_SECONDS = 5;
    private static String indexName1;
    private static String indexName2;
    private static AppbaseCore appbaseCore;
    private static DefaultElasticsearchLowLevelRestClientService defaultElasticsearchRestClientService;
    private static ElasticsearchRestClient elasticsearchRestClient;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultElasticsearchRestClient.registerObjectMapperSubtypes();

        indexName1 = ("index1" + DefaultElasticsearchRestClientTest.class.getSimpleName()
                + String.valueOf(Math.random() * 10000).substring(0, 4)).toLowerCase();
        indexName2 = ("index2" + DefaultElasticsearchRestClientTest.class.getSimpleName()
                + String.valueOf(Math.random() * 10000).substring(0, 4)).toLowerCase();
        appbaseCore = ElasticsearchLowLevelRestClientServiceTestHelper.appbaseCore();
        defaultElasticsearchRestClientService = ElasticsearchLowLevelRestClientServiceTestHelper
                .defaultElasticsearchRestClientService(appbaseCore);
        elasticsearchRestClient = new DefaultElasticsearchRestClient(
                appbaseCore, defaultElasticsearchRestClientService);
    }

    @AfterAll
    static void tearDown() {
        if (elasticsearchRestClient != null) {
            try {
                elasticsearchRestClient
                        .asyncDeleteIndex(new DeleteIndexRequest(indexName1))
                        .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            try {
                elasticsearchRestClient
                        .asyncDeleteIndex(new DeleteIndexRequest(indexName2))
                        .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            elasticsearchRestClient.shutdown();
        }

        if (defaultElasticsearchRestClientService != null) {
            defaultElasticsearchRestClientService.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    void getIndices() {
        try {
            final RestClient restClient = defaultElasticsearchRestClientService
                    .getClientIntegrationService()
                    .getClient();
            final Request request1 = new Request("GET", "http://localhost:9200/" + indexName1);
            final Response response1 = restClient.performRequest(request1);
            final String responseString1 = EntityUtils.toString(response1.getEntity());
            System.out.println(">>>> " + responseString1);

            final Request request2 = new Request("GET", "http://localhost:9200/" + indexName2);
            final Response response2 = restClient.performRequest(request2);
            final String responseString2 = EntityUtils.toString(response2.getEntity());
            System.out.println(">>>> " + responseString2);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void getDocuments() {
        try {
            final RestClient restClient = defaultElasticsearchRestClientService
                    .getClientIntegrationService()
                    .getClient();
            final Request request1 = new Request("GET", "http://localhost:9200/" + indexName1 + "/_doc/1");
            final Response response1 = restClient.performRequest(request1);
            final String responseString1 = EntityUtils.toString(response1.getEntity());
            System.out.println(">>>> " + responseString1);

            final Request request2 = new Request("GET", "http://localhost:9200/" + indexName2 + "/_doc/2");
            final Response response2 = restClient.performRequest(request2);
            final String responseString2 = EntityUtils.toString(response2.getEntity());
            System.out.println(">>>> " + responseString2);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void testDefaultElasticsearchRestClient() {
        try {
            final CreateIndexRequest createIndexRequest1 = createIndexRequest1();
            final CreateIndexResponse createIndexResponse1 = elasticsearchRestClient
                    .asyncCreateIndex(createIndexRequest1)
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            Assertions.assertNotNull(createIndexResponse1);
            Assertions.assertTrue(createIndexResponse1.isSuccess());

            final CreateIndexRequest createIndexRequest2 = createIndexRequest2();
            final CreateIndexResponse createIndexResponse2 = elasticsearchRestClient
                    .asyncCreateIndex(createIndexRequest2)
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            Assertions.assertNotNull(createIndexResponse2);
            Assertions.assertTrue(createIndexResponse2.isSuccess());

            final ExistsIndexResponse existsIndexResponse1 = elasticsearchRestClient
                    .asyncExistsIndex(new ExistsIndexRequest(indexName1))
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            Assertions.assertNotNull(existsIndexResponse1);
            Assertions.assertTrue(existsIndexResponse1.isExists());

            final ExistsIndexResponse existsIndexResponse2 = elasticsearchRestClient
                    .asyncExistsIndex(new ExistsIndexRequest(indexName2))
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            Assertions.assertNotNull(existsIndexResponse2);
            Assertions.assertTrue(existsIndexResponse2.isExists());

            final AddOrUpdateDocumentResponse addOrUpdateDocumentResponse1 = elasticsearchRestClient
                    .asyncAddDocument(addDocumentRequest1())
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            assertAddDocumentResponse(indexName1, addOrUpdateDocumentResponse1);

            final AddOrUpdateDocumentResponse addOrUpdateDocumentResponse2 = elasticsearchRestClient
                    .asyncAddDocument(addDocumentRequest2())
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            assertAddDocumentResponse(indexName2, addOrUpdateDocumentResponse2);

            getIndices();
            getDocuments();

            Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_SECONDS));

            final SearchRequest searchRequest1 = searchRequest1();
            final SearchResponse searchResponse1 = elasticsearchRestClient
                    .asyncSearch(searchRequest1)
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            final Hits hits1 = assertSearchResponse(searchResponse1);
            final Map<String, Object> sourceJson1 = (Map<String, Object>) hits1.getSourceJson();
            Assertions.assertEquals("Leo", sourceJson1.get("name"));
            Assertions.assertEquals(1, sourceJson1.get("age"));

            final SearchRequest searchRequest2 = searchRequest2();
            final SearchResponse searchResponse2 = elasticsearchRestClient
                    .asyncSearch(searchRequest2)
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            final Hits hits2 = assertSearchResponse(searchResponse2);
            final Map<String, Object> sourceJson2 = (Map<String, Object>) hits2.getSourceJson();
            Assertions.assertEquals("Toronto", sourceJson2.get("city"));
            Assertions.assertEquals(24.09, sourceJson2.get("temperature"));

            final MultiSearchRequest multiSearchRequest = multiSearchRequest();
            final MultiSearchResponse multiSearchResponse = elasticsearchRestClient
                    .asyncMultiSearch(multiSearchRequest)
                    .get(SLEEP_SECONDS, TimeUnit.SECONDS);
            assertMultiSearchResponse(multiSearchResponse);
        } catch (Exception ex) {
            throw new RuntimeException(
                    DefaultElasticsearchRestClientTest.class.getName() + " failed", ex);
        }
    }

    private void assertMultiSearchResponse(
            final MultiSearchResponse multiSearchResponse)
    {
        Assertions.assertNotNull(multiSearchResponse);
        Assertions.assertNull(multiSearchResponse.getErrorResponseBody());

        final MultiSearchResponseBody multiSearchResponseBody =
                multiSearchResponse.getMultiSearchResponseBody();
        Assertions.assertNotNull(multiSearchResponseBody);

        final List<MultiSearchHits> responses = multiSearchResponseBody.getResponses();
        Assertions.assertNotNull(responses);
        Assertions.assertEquals(2, responses.size());

        final MultiSearchHits multiSearchHits1 = responses.get(0);
        final SearchHits searchHits1 = multiSearchHits1.getHits();
        Assertions.assertNotNull(searchHits1);

        final List<Hits> hitsList1 = searchHits1.getHits();
        Assertions.assertNotNull(hitsList1);

        final Hits hits1 = hitsList1.get(0);
        Assertions.assertEquals("1", hits1.getId());

        final Map<String, Object> sourceJson1 = (Map<String, Object>) hits1.getSourceJson();
        Assertions.assertEquals("Leo", sourceJson1.get("name"));
        Assertions.assertEquals(1, sourceJson1.get("age"));


        final MultiSearchHits multiSearchHits2 = responses.get(1);
        final SearchHits searchHits2 = multiSearchHits2.getHits();
        Assertions.assertNotNull(searchHits2);

        final List<Hits> hitsList2 = searchHits2.getHits();
        Assertions.assertNotNull(hitsList2);

        final Hits hits2 = hitsList2.get(0);
        Assertions.assertEquals("2", hits2.getId());

        final Map<String, Object> sourceJson2 = (Map<String, Object>) hits2.getSourceJson();
        Assertions.assertEquals("Toronto", sourceJson2.get("city"));
        Assertions.assertEquals(24.09, sourceJson2.get("temperature"));
    }

    private MultiSearchRequest multiSearchRequest() {
        final MultiSearchRequestHeader multiSearchRequestHeader1 = new MultiSearchRequestHeader();
        multiSearchRequestHeader1.setIndex(indexName1);

        final MultiSearchRequestBody multiSearchRequestBody1 = new MultiSearchRequestBody();
        multiSearchRequestBody1.setQuery(searchQuery1());

        final MultiSearchRequestEnvelope multiSearchRequestEnvelope1 =
                new MultiSearchRequestEnvelope(multiSearchRequestHeader1, multiSearchRequestBody1);


        final MultiSearchRequestHeader multiSearchRequestHeader2 = new MultiSearchRequestHeader();
        multiSearchRequestHeader2.setIndex(indexName2);

        final MultiSearchRequestBody multiSearchRequestBody2 = new MultiSearchRequestBody();
        multiSearchRequestBody2.setQuery(searchQuery2());

        final MultiSearchRequestEnvelope multiSearchRequestEnvelope2 =
                new MultiSearchRequestEnvelope(multiSearchRequestHeader2, multiSearchRequestBody2);


        final List<MultiSearchRequestEnvelope> multiSearchRequests = new ArrayList<>();
        multiSearchRequests.add(multiSearchRequestEnvelope1);
        multiSearchRequests.add(multiSearchRequestEnvelope2);


        return new MultiSearchRequest(multiSearchRequests);
    }

    private Hits assertSearchResponse(final SearchResponse searchResponse) {
        Assertions.assertNotNull(searchResponse);
        Assertions.assertNull(searchResponse.getErrorResponseBody());

        final SearchResponseBody searchResponseBody = searchResponse.getSearchResponseBody();
        Assertions.assertNotNull(searchResponseBody);

        final SearchHits searchHits = searchResponseBody.getHits();
        Assertions.assertNotNull(searchHits);

        final List<Hits> hits = searchHits.getHits();
        Assertions.assertNotNull(hits);
        Assertions.assertEquals(1, hits.size());

        final Hits hit = hits.get(0);
        Assertions.assertNotNull(hit);
        Assertions.assertNotNull(hit.getId());

        return hit;
    }

    private SearchRequest searchRequest1() {
        return new SearchRequest(indexName1, searchQueryBody1());
    }

    private SearchQueryBody searchQueryBody1() {
        final SearchQueryBody searchQueryBody = new SearchQueryBody();
        searchQueryBody.setQuery(searchQuery1());

        /*
        final Field field1 = new Field("name");
        final Field field2 = new Field("age");
        searchQueryBody.setFields(List.of(field1, field2));
         */

        return searchQueryBody;
    }

    private SearchQuery searchQuery1() {
        final MatchQuery.FieldParams nameParams = new MatchQuery.FieldParams();
        nameParams.setQuery("Leo");

        final Map<String, MatchQuery.FieldParams> match = new HashMap<>();
        match.put("name", nameParams);

        final MatchQuery matchQuery = new MatchQuery();
        matchQuery.setMatch(match);

        return matchQuery;
    }

    private SearchRequest searchRequest2() {
        return new SearchRequest(indexName2, searchQueryBody2());
    }

    private SearchQueryBody searchQueryBody2() {
        final SearchQueryBody searchQueryBody = new SearchQueryBody();
        searchQueryBody.setQuery(searchQuery2());

        /*
        final Field field1 = new Field("city");
        final Field field2 = new Field("temperature");
        searchQueryBody.setFields(List.of(field1, field2));
         */

        return searchQueryBody;
    }

    private SearchQuery searchQuery2() {
        final MatchQuery.FieldParams cityParams = new MatchQuery.FieldParams();
        cityParams.setQuery("Toronto");

        final Map<String, MatchQuery.FieldParams> match = new HashMap<>();
        match.put("city", cityParams);

        final MatchQuery matchQuery = new MatchQuery();
        matchQuery.setMatch(match);

        return matchQuery;
    }

    private void assertAddDocumentResponse(
            final String indexName,
            final AddOrUpdateDocumentResponse addOrUpdateDocumentResponse)
    {
        Assertions.assertNotNull(addOrUpdateDocumentResponse);

        final AddOrUpdateDocumentResponseBody addOrUpdateDocumentResponseBody =
                addOrUpdateDocumentResponse.getAddDocumentResponseBody();
        Assertions.assertNotNull(addOrUpdateDocumentResponseBody);

        Assertions.assertNotNull(addOrUpdateDocumentResponseBody.getId());
        Assertions.assertEquals(indexName, addOrUpdateDocumentResponseBody.getIndex());
    }

    private static class Document1 {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }
    }

    private static class Document2 {
        private String city;
        private Double temperature;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(final Double temperature) {
            this.temperature = temperature;
        }
    }

    private AddOrUpdateDocumentRequest addDocumentRequest1() {
        final Document1 document1 = new Document1();
        document1.setName("Leo");
        document1.setAge(1);

        final String requestJson1;
        try {
            requestJson1 = JsonUtil
                    .getJsonMapper()
                    .writerFor(Document1.class)
                    .writeValueAsString(document1);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest1 = new AddOrUpdateDocumentRequest(requestJson1, indexName1);
        addOrUpdateDocumentRequest1.setDocumentId("1");

        return addOrUpdateDocumentRequest1;
    }

    private AddOrUpdateDocumentRequest addDocumentRequest2() {
        final Document2 document2 = new Document2();
        document2.setCity("Toronto");
        document2.setTemperature(24.09);

        final String requestJson2;
        try {
            requestJson2 = JsonUtil
                    .getJsonMapper()
                    .writerFor(Document2.class)
                    .writeValueAsString(document2);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        final AddOrUpdateDocumentRequest addOrUpdateDocumentRequest2 = new AddOrUpdateDocumentRequest(requestJson2, indexName2);
        addOrUpdateDocumentRequest2.setDocumentId("2");

        return addOrUpdateDocumentRequest2;
    }

    private CreateIndexRequest createIndexRequest1() {
        final CreateIndexRequestBody.FieldProperties fieldProperties1 = new CreateIndexRequestBody.FieldProperties();
        fieldProperties1.setIndex(true);
        fieldProperties1.setType("text");

        final CreateIndexRequestBody.FieldProperties fieldProperties2 = new CreateIndexRequestBody.FieldProperties();
        fieldProperties2.setIndex(true);
        fieldProperties2.setType("integer");

        final Map<String, CreateIndexRequestBody.FieldProperties> properties1 = new HashMap<>();
        properties1.put("name", fieldProperties1);
        properties1.put("age", fieldProperties2);

        final CreateIndexRequestBody.Mappings mappings1 = new CreateIndexRequestBody.Mappings();
        mappings1.setProperties(properties1);

        final CreateIndexRequestBody createIndexRequestBody1 = new CreateIndexRequestBody();
        createIndexRequestBody1.setMappings(mappings1);

        return new CreateIndexRequest(indexName1, createIndexRequestBody1);
    }

    private CreateIndexRequest createIndexRequest2() {
        final CreateIndexRequestBody.FieldProperties fieldProperties1 = new CreateIndexRequestBody.FieldProperties();
        fieldProperties1.setIndex(true);
        fieldProperties1.setType("text");

        final CreateIndexRequestBody.FieldProperties fieldProperties2 = new CreateIndexRequestBody.FieldProperties();
        fieldProperties2.setIndex(true);
        fieldProperties2.setType("double");

        final Map<String, CreateIndexRequestBody.FieldProperties> properties1 = new HashMap<>();
        properties1.put("city", fieldProperties1);
        properties1.put("temperature", fieldProperties2);

        final CreateIndexRequestBody.Mappings mappings1 = new CreateIndexRequestBody.Mappings();
        mappings1.setProperties(properties1);

        final CreateIndexRequestBody createIndexRequestBody1 = new CreateIndexRequestBody();
        createIndexRequestBody1.setMappings(mappings1);

        return new CreateIndexRequest(indexName2, createIndexRequestBody1);
    }
}