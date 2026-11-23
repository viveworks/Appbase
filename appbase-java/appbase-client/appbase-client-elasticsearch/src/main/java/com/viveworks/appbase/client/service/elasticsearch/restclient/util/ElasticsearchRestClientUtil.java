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

package com.viveworks.appbase.client.service.elasticsearch.restclient.util;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.HttpAsyncClientConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.HttpHostConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.NodeSelectorType;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.RequestConfiguration;
import org.elasticsearch.client.NodeSelector;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ElasticsearchRestClientUtil {
    private ElasticsearchRestClientUtil() {
    }

    /**
     * Build {@link RestClientBuilder} using the provided parameters.
     *
     * @param appbaseClientCore {@link AppbaseClientCore}
     * @param defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
     *      {@link DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration}
     * @param httpClientConfigCallback {@link RestClientBuilder.HttpClientConfigCallback}
     * @param failureListener {@link RestClient.FailureListener}
     * @param requestConfigCallback {@link RestClientBuilder.RequestConfigCallback}
     * @return {@link RestClientBuilder}
     */
    public static RestClientBuilder restClientBuilder(
            final AppbaseClientCore appbaseClientCore,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
            @Nullable final RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback,
            @Nullable final RestClient.FailureListener failureListener,
            @Nullable final RestClientBuilder.RequestConfigCallback requestConfigCallback)
    {
        AppbaseUtil.notNull(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");


        final Set<HttpHostConfiguration> httpHostConfigurations =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getHttpHostConfigurations();
        final HttpHost[] httpHosts = httpHosts(httpHostConfigurations);


        final RestClientBuilder restClientBuilder = RestClient.builder(httpHosts);

        final String pathPrefix =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getPathPrefix();
        if (pathPrefix != null) {
            restClientBuilder.setPathPrefix(pathPrefix);
        }

        final boolean strictDeprecationMode =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.isStrictDeprecationMode();
        restClientBuilder.setStrictDeprecationMode(strictDeprecationMode);

        final RestClientBuilder.RequestConfigCallback localRequestConfigCallback =
                requestConfigCallback(
                        appbaseClientCore,
                        defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                        requestConfigCallback);
        if (localRequestConfigCallback != null) {
            restClientBuilder.setRequestConfigCallback(localRequestConfigCallback);
        }

        final RestClient.FailureListener localFailureListener =
                failureListener(
                        appbaseClientCore,
                        defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                        failureListener);
        if (localFailureListener != null) {
            restClientBuilder.setFailureListener(localFailureListener);
        }

        final HttpAsyncClientConfiguration httpAsyncClientConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getHttpAsyncClientConfiguration();
        if (httpAsyncClientConfiguration != null && httpAsyncClientConfiguration.isEnabled()) {
            final RestClientBuilder.HttpClientConfigCallback localHttpClientConfigCallback =
                    httpAsyncClientBuilder(
                            appbaseClientCore,
                            defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                            httpClientConfigCallback);
            restClientBuilder.setHttpClientConfigCallback(localHttpClientConfigCallback);
        }

        final Header[] defaultHeaders = defaultHeaders(
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration);
        if (defaultHeaders != null) {
            restClientBuilder.setDefaultHeaders(defaultHeaders);
        }

        final NodeSelectorType nodeSelector =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getNodeSelector();
        if (nodeSelector != null && "SKIP_DEDICATED_MASTERS".equals(nodeSelector.getText())) {
            restClientBuilder.setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS);
        }

        final Boolean compressionEnabled =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getCompressionEnabled();
        if (Boolean.TRUE.equals(compressionEnabled)) {
            restClientBuilder.setCompressionEnabled(true);
        }

        return restClientBuilder;
    }

    private static RestClientBuilder.HttpClientConfigCallback httpAsyncClientBuilder(
            final AppbaseClientCore appbaseClientCore,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
            @Nullable final RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback)
    {
        if (httpClientConfigCallback != null) {
            return httpClientConfigCallback;
        }

        final InjectedTypeConfiguration<? extends RestClientBuilder.HttpClientConfigCallback>
                httpClientConfigCallbackInjectedTypeConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                        .getHttpClientConfigCallbackInjectedTypeConfiguration();
        if (httpClientConfigCallbackInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore
                    .getAppbaseCore()
                    .getGuiceInjectorService();
            return AppbaseUtil.injectedInstance(guiceInjectorService, httpClientConfigCallbackInjectedTypeConfiguration);
        }

        final HttpAsyncClientConfiguration httpAsyncClientConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getHttpAsyncClientConfiguration();
        return new HttpClientConfigCallbackImpl(appbaseClientCore, httpAsyncClientConfiguration);
    }

    @Nullable
    private static RestClient.FailureListener failureListener(
            final AppbaseClientCore appbaseClientCore,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
            @Nullable final RestClient.FailureListener failureListener)
    {
        if (failureListener != null) {
            return failureListener;
        }

        final InjectedTypeConfiguration<? extends RestClient.FailureListener> failureListenerInjectedTypeConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                        .getFailureListenerInjectedTypeConfiguration();
        if (failureListenerInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore
                    .getAppbaseCore()
                    .getGuiceInjectorService();
            return AppbaseUtil.injectedInstance(guiceInjectorService, failureListenerInjectedTypeConfiguration);
        }

        return null;
    }

    @Nullable
    private static RestClientBuilder.RequestConfigCallback requestConfigCallback(
            final AppbaseClientCore appbaseClientCore,
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
            @Nullable final RestClientBuilder.RequestConfigCallback requestConfigCallback)
    {
        if (requestConfigCallback != null) {
            return requestConfigCallback;
        }

        final RequestConfiguration requestConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getRequestConfiguration();
        final InjectedTypeConfiguration<? extends RestClientBuilder.RequestConfigCallback>
                requestConfigCallbackInjectedTypeConfiguration =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                        .getRequestConfigCallbackInjectedTypeConfiguration();
        if (requestConfigCallbackInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore
                    .getAppbaseCore()
                    .getGuiceInjectorService();
            return AppbaseUtil.injectedInstance(guiceInjectorService, requestConfigCallbackInjectedTypeConfiguration);
        } else if (requestConfiguration != null) {
            return new RequestConfigCallbackImpl(requestConfiguration);
        }

        return null;
    }

    private static HttpHost[] httpHosts(final Set<HttpHostConfiguration> httpHostConfigurations) {
        final HttpHost[] httpHosts = new HttpHost[httpHostConfigurations.size()];
        int httpHostsIndex = 0;
        for (final HttpHostConfiguration httpHostConfiguration : httpHostConfigurations) {
            final HttpHost httpHost = new HttpHost(
                    httpHostConfiguration.getHostname(),
                    httpHostConfiguration.getPort(),
                    httpHostConfiguration.getScheme());
            httpHosts[httpHostsIndex] = httpHost;
            ++httpHostsIndex;
        }

        return httpHosts;
    }

    private static Header[] defaultHeaders(
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                    defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
    {
        final Map<String, String> defaultHeaders =
                defaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.getDefaultHeaders();
        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            final Header[] headers = new Header[defaultHeaders.size()];
            int headersIndex = 0;

            for (final Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                final Header header = new BasicHeader(key, value);
                headers[headersIndex] = header;

                ++headersIndex;
            }

            return headers;
        }

        return null;
    }
}
