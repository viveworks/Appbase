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

package com.viveworks.appbase.bootstrap.client.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServiceFactory;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.s3.DefaultS3ClientsService;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link S3ClientsService}.
 *
 * <p>Four construction modes:
 * <ul>
 *   <li>Pre-built: {@link #S3ClientsServiceFactory(S3ClientsService)}</li>
 *   <li>Config-based: {@link #S3ClientsServiceFactory(DefaultS3ClientsServiceConfiguration,
 *       AwsSdkHttpClientService, AwsSdkAsyncHttpClientService)} — requires pre-built HTTP services.</li>
 *   <li>Classpath-file: {@link #S3ClientsServiceFactory(String)} — creates transient
 *       {@link DefaultAwsSdkHttpClientService} and {@link DefaultAwsSdkAsyncHttpClientService}
 *       internally. Use this when AWS HTTP services are not registered separately in the container.</li>
 *   <li>Custom store: {@link #S3ClientsServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name; creates transient HTTP services internally.</li>
 * </ul>
 */
public final class S3ClientsServiceFactory implements AppbaseClientServiceFactory<S3ClientsService> {

    @Nullable private final S3ClientsService predefinedService;
    @Nullable private final DefaultS3ClientsServiceConfiguration config;
    @Nullable private final AwsSdkHttpClientService httpClientService;
    @Nullable private final AwsSdkAsyncHttpClientService asyncHttpClientService;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link S3ClientsService} instance. */
    public S3ClientsServiceFactory(final S3ClientsService service) {
        this.predefinedService      = Objects.requireNonNull(service, "service");
        this.config                 = null;
        this.httpClientService      = null;
        this.asyncHttpClientService = null;
        this.configStore            = null;
        this.rawConfigName          = null;
    }

    /**
     * Creates a {@link DefaultS3ClientsService} from config and explicit AWS HTTP client dependencies.
     *
     * @param config               S3 service configuration
     * @param httpClientService    pre-built synchronous AWS SDK HTTP client service
     * @param asyncHttpClientService pre-built asynchronous AWS SDK HTTP client service
     */
    public S3ClientsServiceFactory(final DefaultS3ClientsServiceConfiguration config,
                                    final AwsSdkHttpClientService httpClientService,
                                    final AwsSdkAsyncHttpClientService asyncHttpClientService) {
        this.config                 = Objects.requireNonNull(config, "config");
        this.httpClientService      = Objects.requireNonNull(httpClientService, "httpClientService");
        this.asyncHttpClientService = Objects.requireNonNull(asyncHttpClientService, "asyncHttpClientService");
        this.predefinedService      = null;
        this.configStore            = null;
        this.rawConfigName          = null;
    }

    /**
     * Creates a {@link DefaultS3ClientsService} by loading config from a classpath YAML file.
     * Transient {@link DefaultAwsSdkHttpClientService} and {@link DefaultAwsSdkAsyncHttpClientService}
     * instances are created internally.
     *
     * @param configClasspathFile classpath URI of the S3 YAML config file
     */
    public S3ClientsServiceFactory(final String configClasspathFile) {
        this.configStore            = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName          = configClasspathFile;
        this.predefinedService      = null;
        this.config                 = null;
        this.httpClientService      = null;
        this.asyncHttpClientService = null;
    }

    /**
     * Creates a {@link DefaultS3ClientsService} using config loaded from the supplied store.
     * Transient {@link DefaultAwsSdkHttpClientService} and {@link DefaultAwsSdkAsyncHttpClientService}
     * instances are created internally.
     * {@code rawConfigName} is the key used in
     * {@code registerConfigurationTypeByRawConfigurationName} — for a classpath store this is
     * the classpath URI; for other stores use their identifying name.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public S3ClientsServiceFactory(final ConfigurationStore configStore,
                                    final String rawConfigName) {
        this.configStore            = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName          = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService      = null;
        this.config                 = null;
        this.httpClientService      = null;
        this.asyncHttpClientService = null;
    }

    @Override
    public Class<S3ClientsService> serviceType() {
        return S3ClientsService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.S3.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        DefaultS3ClientsService.registerObjectMapperSubtypes();
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultS3ClientsServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public S3ClientsService create(final AppbaseCoreServices coreServices,
                                    final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultS3ClientsServiceConfiguration.class, config);
            return new DefaultS3ClientsService(appbaseClientCore, httpClientService, asyncHttpClientService);
        }
        final AwsSdkHttpClientService http = new DefaultAwsSdkHttpClientService(appbaseClientCore);
        final AwsSdkAsyncHttpClientService asyncHttp =
            new DefaultAwsSdkAsyncHttpClientService(appbaseClientCore);
        return new DefaultS3ClientsService(appbaseClientCore, http, asyncHttp);
    }
}
