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

package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientServiceTestHelper;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsServiceConfiguration;
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
public class S3ClientServiceTestHelper {
    public static final String S3_CLIENT_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "s3Client";
    private static final String S3_CLIENT_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-aws-s3.yaml";

    private S3ClientServiceTestHelper() {
    }

    /**
     * Creates the default s3 client service test fixture.
     *
     * @return the default s3 client service test fixture
     */
    public static DefaultS3ClientsService defaultS3ClientService() {
        return defaultS3ClientService(S3_CLIENT_CONFIG_CLASSPATH_FILE);
    }

    /**
     * Creates the default s3 client service test fixture.
     *
     * @return the default s3 client service test fixture
     */
    public static DefaultS3ClientsService defaultS3ClientService(final String s3ConfigClasspathFile) {
        final AppbaseCore appbaseCore = appbaseCore(s3ConfigClasspathFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService =
                AwsSdkAsyncHttpClientServiceTestHelper.awsSdkAsyncHttpClientService();
        final AwsSdkHttpClientService awsSdkHttpClientService =
                AwsSdkAsyncHttpClientServiceTestHelper.awsSdkHttpClientService();
        return new DefaultS3ClientsService(
                defaultAppbaseClientCore, awsSdkHttpClientService, awsSdkAsyncHttpClientService);
    }

    /**
     * Creates the appbase core test fixture.
     *
     * @return the appbase core test fixture
     */
    public static AppbaseCore appbaseCore() {
        final String s3ClientConfig = System.getProperty(
                S3_CLIENT_CONFIG_FILE_PROPERTY, S3_CLIENT_CONFIG_CLASSPATH_FILE);
        return appbaseCore(s3ClientConfig);
    }

    /**
     * Creates the appbase core test fixture.
     *
     * @return the appbase core test fixture
     */
    public static AppbaseCore appbaseCore(final String s3ConfigClasspathFile) {
        AppbaseUtil.notNull(s3ConfigClasspathFile, "s3ConfigClasspathFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultS3ClientsService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        final String awsSdkAsyncClientConfig = System.getProperty(
                AwsSdkAsyncHttpClientServiceTestHelper.AWS_SDK_ASYNC_HTTP_CLIENT_CONFIG_CLASSPATH_FILE_PROPERTY,
                AwsSdkAsyncHttpClientServiceTestHelper.AWS_SDK_ASYNC_HTTP_CLIENT_CLASSPATH_CONFIG_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class, awsSdkAsyncClientConfig);
        final ClasspathConfigurationStore asyncHttpClientClasspathConfigurationStore =
                new ClasspathConfigurationStore(awsSdkAsyncClientConfig, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(asyncHttpClientClasspathConfigurationStore);

        final String awsSdkClientConfig = System.getProperty(
                AwsSdkAsyncHttpClientServiceTestHelper.AWS_SDK_HTTP_CLIENT_CONFIG_CLASSPATH_FILE_PROPERTY,
                AwsSdkAsyncHttpClientServiceTestHelper.AWS_SDK_HTTP_CLIENT_CLASSPATH_CONFIG_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultAwsSdkHttpClientServiceConfiguration.class, awsSdkClientConfig);
        final ClasspathConfigurationStore httpClientClasspathConfigurationStore =
                new ClasspathConfigurationStore(awsSdkClientConfig, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(httpClientClasspathConfigurationStore);

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultS3ClientsServiceConfiguration.class,
                s3ConfigClasspathFile);
        final ClasspathConfigurationStore classpathConfigurationStore2 =
                new ClasspathConfigurationStore(s3ConfigClasspathFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore2);

        configurationService.load();

        return appbaseCore;
    }
}
