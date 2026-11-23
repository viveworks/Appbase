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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AwsSdkAsyncHttpClientServiceTestHelper {
    public static final String AWS_SDK_HTTP_CLIENT_CONFIG_CLASSPATH_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "awssdk.httpClient";
    public static final String AWS_SDK_ASYNC_HTTP_CLIENT_CONFIG_CLASSPATH_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "awssdk.asyncHttpClient";

    public static final String AWS_SDK_HTTP_CLIENT_CLASSPATH_CONFIG_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-aws-http-client.yaml";
    public static final String AWS_SDK_ASYNC_HTTP_CLIENT_CLASSPATH_CONFIG_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-aws-async-http-client.yaml";

    private static final AwsSdkAsyncHttpClientService AWS_SDK_ASYNC_HTTP_CLIENT_SERVICE =
            initAwsSdkAsyncHttpClientService();
    private static final AwsSdkHttpClientService AWS_SDK_HTTP_CLIENT_SERVICE =
            initAwsSdkHttpClientService();

    private AwsSdkAsyncHttpClientServiceTestHelper() {
    }

    /**
     * Creates the aws sdk async http client service test fixture.
     *
     * @return the aws sdk async http client service test fixture
     */
    public static AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService() {
        return AWS_SDK_ASYNC_HTTP_CLIENT_SERVICE;
    }

    /**
     * Creates the aws sdk http client service test fixture.
     *
     * @return the aws sdk http client service test fixture
     */
    public static AwsSdkHttpClientService awsSdkHttpClientService() {
        return AWS_SDK_HTTP_CLIENT_SERVICE;
    }

    private static AwsSdkHttpClientService initAwsSdkHttpClientService() {
        final AppbaseCore appbaseCore = appbaseCore();
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultAwsSdkHttpClientService(defaultAppbaseClientCore);
    }

    private static AwsSdkAsyncHttpClientService initAwsSdkAsyncHttpClientService() {
        final AppbaseCore appbaseCore = appbaseCore();
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultAwsSdkAsyncHttpClientService(defaultAppbaseClientCore);
    }

    private static AppbaseCore appbaseCore() {
        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultAwsSdkAsyncHttpClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        final String asyncHttpClientConfig = System.getProperty(
                AWS_SDK_ASYNC_HTTP_CLIENT_CONFIG_CLASSPATH_FILE_PROPERTY,
                AWS_SDK_ASYNC_HTTP_CLIENT_CLASSPATH_CONFIG_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                asyncHttpClientConfig);
        final ClasspathConfigurationStore asynHttpClientClasspathConfigurationStore =
                new ClasspathConfigurationStore(asyncHttpClientConfig, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(asynHttpClientClasspathConfigurationStore);

        final String httpClientConfig = System.getProperty(
                AWS_SDK_HTTP_CLIENT_CONFIG_CLASSPATH_FILE_PROPERTY,
                AWS_SDK_HTTP_CLIENT_CLASSPATH_CONFIG_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultAwsSdkHttpClientServiceConfiguration.class, httpClientConfig);
        final ClasspathConfigurationStore httpClientClasspathConfigurationStore =
                new ClasspathConfigurationStore(httpClientConfig, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(httpClientClasspathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
