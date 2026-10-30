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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.util.ElasticsearchRestClientUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ElasticsearchLowLevelRestClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultElasticsearchLowLevelRestClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <RestClient, ClusterIdValue, DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration>
        implements ElasticsearchLowLevelRestClientIntegrationService
{
    private final AtomicReference<RestClient> restClientRef;

    @NullableConditionally
    private final RestClientBuilder.RequestConfigCallback requestConfigCallback;

    @NullableConditionally
    private final RestClient.FailureListener failureListener;

    @NullableConditionally
    private final RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
            defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;


    public DefaultElasticsearchLowLevelRestClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final RestClient restClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, RestClient.class);

        AppbaseUtil.notNull(restClient, "restClient");

        this.restClientRef = new AtomicReference<>(restClient);
        this.failureListener = null;
        this.httpClientConfigCallback = null;
        this.requestConfigCallback = null;
        this.defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration = null;
    }


    public DefaultElasticsearchLowLevelRestClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, RestClient.class);

        AppbaseUtil.notNull(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        this.defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
        this.restClientRef = new AtomicReference<>();
        this.requestConfigCallback = null;
        this.failureListener = null;
        this.httpClientConfigCallback = null;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }


    public DefaultElasticsearchLowLevelRestClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final RestClientBuilder.RequestConfigCallback requestConfigCallback,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, RestClient.class);

        AppbaseUtil.notNull(requestConfigCallback, "requestConfigCallback");
        AppbaseUtil.notNull(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        this.requestConfigCallback = requestConfigCallback;
        this.defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
        this.restClientRef = new AtomicReference<>();
        this.failureListener = null;
        this.httpClientConfigCallback = null;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }


    public DefaultElasticsearchLowLevelRestClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final RestClient.FailureListener failureListener,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, RestClient.class);

        AppbaseUtil.notNull(failureListener, "failureListener");
        AppbaseUtil.notNull(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        this.failureListener = failureListener;
        this.defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
        this.restClientRef = new AtomicReference<>();
        this.requestConfigCallback = null;
        this.httpClientConfigCallback = null;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }


    public DefaultElasticsearchLowLevelRestClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, RestClient.class);

        AppbaseUtil.notNull(httpClientConfigCallback, "httpClientConfigCallback");
        AppbaseUtil.notNull(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        this.httpClientConfigCallback = httpClientConfigCallback;
        this.defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
        this.restClientRef = new AtomicReference<>();
        this.failureListener = null;
        this.requestConfigCallback = null;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }


    public DefaultElasticsearchLowLevelRestClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final RestClientBuilder.RequestConfigCallback requestConfigCallback,
            final RestClient.FailureListener failureListener,
            final RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, RestClient.class);

        AppbaseUtil.notNull(failureListener, "failureListener");
        AppbaseUtil.notNull(httpClientConfigCallback, "httpClientConfigCallback");
        AppbaseUtil.notNull(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        this.requestConfigCallback = requestConfigCallback;
        this.failureListener = failureListener;
        this.httpClientConfigCallback = httpClientConfigCallback;
        this.defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
        this.restClientRef = new AtomicReference<>();

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        final RestClientBuilder restClientBuilder = ElasticsearchRestClientUtil.restClientBuilder(
                appbaseClientCore,
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                httpClientConfigCallback,
                failureListener,
                requestConfigCallback);

        final RestClient restClient = restClientBuilder.build();
        restClientRef.set(restClient);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final RestClient restClient = restClientRef.getAndSet(null);
        if (restClient == null) {
            return;
        }
        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "appbase-es-lowlevel-shutdown");
            t.setDaemon(true);
            return t;
        });
        try {
            executor.submit(() -> {
                try {
                    restClient.close();
                } catch (IOException ioe) {
                    throw new AppbaseRuntimeException("Error shutting down " + RestClient.class.getName(), ioe);
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException ignored) {
            // close() blocked too long; daemon thread will be abandoned on JVM exit
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            final Throwable cause = ee.getCause();
            if (cause instanceof AppbaseRuntimeException are) {
                throw are;
            }
            throw new AppbaseRuntimeException("Error shutting down " + RestClient.class.getName(), cause);
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public RestClient getClient() {
        return restClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final RestClient restClient = getClient();
        if (restClient == null) {
            return false;
        }

        final List<Node> nodes = restClient.getNodes();
        return nodes != null && !nodes.isEmpty();
    }
}
