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

import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.RequestConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.client.RestClientBuilder;

/**
 * Custom implementation of {@link RestClientBuilder.RequestConfigCallback}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class RequestConfigCallbackImpl implements RestClientBuilder.RequestConfigCallback {
    private final RequestConfiguration requestConfiguration;

    public RequestConfigCallbackImpl(final RequestConfiguration requestConfiguration) {
        AppbaseUtil.notNull(requestConfiguration, "requestConfiguration");

        this.requestConfiguration = requestConfiguration;
    }

    @Override
    public RequestConfig.Builder customizeRequestConfig(final RequestConfig.Builder requestConfigBuilder) {
        AppbaseUtil.notNull(requestConfigBuilder, "requestConfigBuilder");

        requestConfigBuilder.setCircularRedirectsAllowed(requestConfiguration.isCircularRedirectsAllowed());
        requestConfigBuilder.setConnectionRequestTimeout(requestConfiguration.getConnectionRequestTimeout());
        requestConfigBuilder.setConnectTimeout(requestConfiguration.getConnectTimeout());
        requestConfigBuilder.setContentCompressionEnabled(requestConfiguration.isContentCompressionEnabled());
        requestConfigBuilder.setExpectContinueEnabled(requestConfiguration.isExpectContinueEnabled());
        requestConfigBuilder.setMaxRedirects(requestConfiguration.getMaxRedirects());
        requestConfigBuilder.setNormalizeUri(requestConfiguration.isNormalizeUri());
        requestConfigBuilder.setRedirectsEnabled(requestConfiguration.isRedirectsEnabled());
        requestConfigBuilder.setRelativeRedirectsAllowed(requestConfiguration.isRelativeRedirectsAllowed());
        requestConfigBuilder.setSocketTimeout(requestConfiguration.getSocketTimeout());

        return requestConfigBuilder;
    }
}
