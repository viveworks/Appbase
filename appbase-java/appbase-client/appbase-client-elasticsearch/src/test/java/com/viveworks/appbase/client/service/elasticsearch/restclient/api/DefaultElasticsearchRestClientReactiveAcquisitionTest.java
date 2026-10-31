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
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive.ReactiveElasticsearchAccessor;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientIntegrationService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import org.apache.http.StatusLine;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultElasticsearchRestClient#getReactiveAccessor()} and
 * {@link DefaultElasticsearchRestClient#getReactiveAccessor(AppbaseClientKey)}.
 *
 * <p>Verifies that the default accessor's borrow ({@code () -> this}) routes through the
 * no-arg {@link ElasticsearchLowLevelRestClientService#getClientIntegrationService()}, while the
 * keyed accessor's borrow ({@code () -> keyBound(key)}) routes through the keyed
 * {@link ElasticsearchLowLevelRestClientService#getClientIntegrationService(AppbaseClientKey)} --
 * i.e. that {@code boundKey} plumbing added in this task actually reaches the executor.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultElasticsearchRestClientReactiveAcquisitionTest {

    @Mock
    private AppbaseCore appbaseCore;

    @Mock
    private LogService logService;

    @Mock
    private ReactorService reactorService;

    @Mock
    private ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService;

    @Mock
    private ElasticsearchLowLevelRestClientIntegrationService defaultIntegrationService;

    @Mock
    private ElasticsearchLowLevelRestClientIntegrationService keyedIntegrationService;

    @Mock
    private RestClient restClient;

    private DefaultElasticsearchRestClient client;

    @BeforeEach
    void setUp() {
        when(appbaseCore.getLogService()).thenReturn(logService);
        when(logService.getLogger(any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultElasticsearchRestClientReactiveAcquisitionTest.class));
        lenient().when(appbaseCore.getReactorService()).thenReturn(reactorService);
        lenient().when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());

        final DefaultElasticsearchLowLevelRestClientServiceConfiguration configuration =
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.builder()
                        .reactiveOperationConfiguration(
                                DefaultReactiveOperationConfiguration.builder()
                                        .metricsEnabled(false)
                                        .traceEnabled(false)
                                        .retryEnabled(false)
                                        .build())
                        .build();
        when(elasticsearchLowLevelRestClientService.<DefaultElasticsearchLowLevelRestClientServiceConfiguration>getConfiguration())
                .thenReturn(configuration);

        client = new DefaultElasticsearchRestClient(appbaseCore, elasticsearchLowLevelRestClientService);
    }

    private static AppbaseClientKey<ClusterIdValue> testKey() {
        return AppbaseClientUtil.clusterIdValueKey(
                ClusterIdValueConfiguration.builder().clusterId("reactive-acquisition-test").build());
    }

    private static Response successResponse() {
        final Response response = org.mockito.Mockito.mock(Response.class);
        final StatusLine statusLine = org.mockito.Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(statusLine);
        return response;
    }

    @Test
    void getReactiveAccessorReturnsNonNullAccessor() {
        final ReactiveElasticsearchAccessor accessor = client.getReactiveAccessor();

        Assertions.assertNotNull(accessor);
    }

    @Test
    void keyedGetReactiveAccessorReturnsNonNullAccessor() {
        final ReactiveElasticsearchAccessor accessor = client.getReactiveAccessor(testKey());

        Assertions.assertNotNull(accessor);
    }

    @Test
    void defaultReactiveAccessorUsesNoArgIntegrationServiceLookup() {
        when(elasticsearchLowLevelRestClientService.getClientIntegrationService())
                .thenReturn(defaultIntegrationService);
        when(defaultIntegrationService.getClient()).thenReturn(restClient);
        doAnswer(invocation -> {
            final ResponseListener listener = invocation.getArgument(1);
            listener.onSuccess(successResponse());
            return null;
        }).when(restClient).performRequestAsync(any(Request.class), any(ResponseListener.class));

        final ReactiveElasticsearchAccessor accessor = client.getReactiveAccessor();
        accessor.createIndex("createIndex", new CreateIndexRequest("reactive-acquisition-index"))
                .block(Duration.ofSeconds(5));

        verify(elasticsearchLowLevelRestClientService, times(1)).getClientIntegrationService();
        verify(elasticsearchLowLevelRestClientService, never()).getClientIntegrationService(any());
    }

    @Test
    void keyedReactiveAccessorUsesKeyedIntegrationServiceLookup() {
        final AppbaseClientKey<ClusterIdValue> key = testKey();
        when(elasticsearchLowLevelRestClientService.getClientIntegrationService(key))
                .thenReturn(keyedIntegrationService);
        when(keyedIntegrationService.getClient()).thenReturn(restClient);
        doAnswer(invocation -> {
            final ResponseListener listener = invocation.getArgument(1);
            listener.onSuccess(successResponse());
            return null;
        }).when(restClient).performRequestAsync(any(Request.class), any(ResponseListener.class));

        final ReactiveElasticsearchAccessor accessor = client.getReactiveAccessor(key);
        accessor.createIndex("createIndex", new CreateIndexRequest("reactive-acquisition-index"))
                .block(Duration.ofSeconds(5));

        verify(elasticsearchLowLevelRestClientService, times(1)).getClientIntegrationService(key);
        verify(elasticsearchLowLevelRestClientService, never()).getClientIntegrationService();
    }

    @Test
    void getAccessorReturnsNonNullAccessor() {
        final ElasticsearchAccessor accessor = client.getAccessor();

        Assertions.assertNotNull(accessor);
    }

    @Test
    void keyedGetAccessorReturnsNonNullAccessor() {
        final ElasticsearchAccessor accessor = client.getAccessor(testKey());

        Assertions.assertNotNull(accessor);
    }

    @Test
    void defaultAccessorUsesNoArgIntegrationServiceLookup() throws Exception {
        when(elasticsearchLowLevelRestClientService.getClientIntegrationService())
                .thenReturn(defaultIntegrationService);
        when(defaultIntegrationService.getClient()).thenReturn(restClient);
        final Response response = successResponse();
        when(restClient.performRequest(any(Request.class))).thenReturn(response);

        final ElasticsearchAccessor accessor = client.getAccessor();
        accessor.createIndex("createIndex", new CreateIndexRequest("sync-acquisition-index"));

        verify(elasticsearchLowLevelRestClientService, times(1)).getClientIntegrationService();
        verify(elasticsearchLowLevelRestClientService, never()).getClientIntegrationService(any());
    }

    @Test
    void keyedAccessorUsesKeyedIntegrationServiceLookup() throws Exception {
        final AppbaseClientKey<ClusterIdValue> key = testKey();
        when(elasticsearchLowLevelRestClientService.getClientIntegrationService(key))
                .thenReturn(keyedIntegrationService);
        when(keyedIntegrationService.getClient()).thenReturn(restClient);
        final Response response = successResponse();
        when(restClient.performRequest(any(Request.class))).thenReturn(response);

        final ElasticsearchAccessor accessor = client.getAccessor(key);
        accessor.createIndex("createIndex", new CreateIndexRequest("sync-acquisition-index"));

        verify(elasticsearchLowLevelRestClientService, times(1)).getClientIntegrationService(key);
        verify(elasticsearchLowLevelRestClientService, never()).getClientIntegrationService();
    }
}
