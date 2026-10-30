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
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.DefaultElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link ElasticsearchRestClient} (high-level REST client).
 *
 * <p>Creates an internal {@link ElasticsearchLowLevelRestClientService} from the supplied
 * configuration and wraps it in {@link DefaultElasticsearchRestClient}. Use this factory
 * when only the high-level REST client is needed; use {@link ElasticsearchClientServiceFactory}
 * directly if the low-level service must be independently accessible.
 *
 * <p>Four construction modes:
 * <ul>
 *   <li>Pre-built: {@link #ElasticsearchRestClientFactory(ElasticsearchRestClient)}</li>
 *   <li>Classpath-file: {@link #ElasticsearchRestClientFactory(String)} — registers a YAML store
 *       for the low-level service configuration via {@link #registerConfiguration}.</li>
 *   <li>Custom store: {@link #ElasticsearchRestClientFactory(ConfigurationStore, String)}</li>
 *   <li>No-arg: default constructor — relies on configuration already in the configuration service.</li>
 * </ul>
 */
public final class ElasticsearchRestClientFactory
        implements AppbaseClientServiceFactory<ElasticsearchRestClient> {

    @Nullable private final ElasticsearchRestClient predefinedService;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link ElasticsearchRestClient} instance. */
    public ElasticsearchRestClientFactory(final ElasticsearchRestClient service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates an {@link ElasticsearchRestClient} by loading the low-level service config
     * from a classpath YAML file registered via {@link #registerConfiguration}.
     *
     * @param configClasspathFile classpath URI of the low-level YAML config
     *                            (e.g. {@code classpath:///elasticsearch-low-level.yaml})
     */
    public ElasticsearchRestClientFactory(final String configClasspathFile) {
        this.configStore       = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName     = configClasspathFile;
        this.predefinedService = null;
    }

    /**
     * Creates an {@link ElasticsearchRestClient} using config from the supplied store.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public ElasticsearchRestClientFactory(final ConfigurationStore configStore,
                                          final String rawConfigName) {
        this.configStore       = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName     = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService = null;
    }

    /** Creates an {@link ElasticsearchRestClient} using defaults from the core configuration service. */
    public ElasticsearchRestClientFactory() {
        this.predefinedService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    @Override
    public Class<ElasticsearchRestClient> serviceType() {
        return ElasticsearchRestClient.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.ELASTICSEARCH.order() + 1;
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        DefaultElasticsearchLowLevelRestClientService.registerObjectMapperSubtypes();
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultElasticsearchLowLevelRestClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public ElasticsearchRestClient create(final AppbaseCoreServices coreServices,
                                          final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        final ElasticsearchLowLevelRestClientService lowLevel =
            new DefaultElasticsearchLowLevelRestClientService(appbaseClientCore);
        return new DefaultElasticsearchRestClient(coreServices.appbaseCore(), lowLevel);
    }
}
