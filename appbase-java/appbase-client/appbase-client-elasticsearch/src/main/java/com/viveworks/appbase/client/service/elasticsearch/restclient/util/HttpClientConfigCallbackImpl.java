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

package com.viveworks.appbase.client.service.elasticsearch.restclient.util;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.HttpAsyncClientConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;

import java.util.concurrent.ThreadFactory;

/**
 * Custom implementation of {@link RestClientBuilder.HttpClientConfigCallback}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class HttpClientConfigCallbackImpl implements RestClientBuilder.HttpClientConfigCallback {
    private final Logger logger;
    private final HttpAsyncClientConfiguration httpAsyncClientConfiguration;

    public HttpClientConfigCallbackImpl(
            final AppbaseClientCore appbaseClientCore,
            final HttpAsyncClientConfiguration httpAsyncClientConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(httpAsyncClientConfiguration, "httpAsyncClientConfiguration");

        this.logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.httpAsyncClientConfiguration = httpAsyncClientConfiguration;
    }

    @Override
    public HttpAsyncClientBuilder customizeHttpClient(final HttpAsyncClientBuilder httpClientBuilder) {
        AppbaseUtil.notNull(httpClientBuilder, "httpClientBuilder");

        if (httpAsyncClientConfiguration.isEnabled()) {
            final boolean sharedConnectionManager = httpAsyncClientConfiguration.isSharedConnectionManager();
            httpClientBuilder.setConnectionManagerShared(sharedConnectionManager);

            final int maxConnectionsPerRoute = httpAsyncClientConfiguration.getMaxConnectionsPerRoute();
            httpClientBuilder.setMaxConnPerRoute(maxConnectionsPerRoute);

            final ThreadFactoryConfiguration threadFactoryConfiguration =
                    httpAsyncClientConfiguration.getThreadFactoryConfiguration();
            final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                    threadFactoryConfiguration, AppbaseUtil.defaultUncaughtExceptionHandler(logger));
            httpClientBuilder.setThreadFactory(threadFactory);

            final int totalMaxConnections = httpAsyncClientConfiguration.getTotalMaxConnections();
            httpClientBuilder.setMaxConnTotal(totalMaxConnections);

            final String userAgent = httpAsyncClientConfiguration.getUserAgent();
            if (userAgent != null) {
                httpClientBuilder.setUserAgent(userAgent);
            }
        }

        return httpClientBuilder;
    }
}
