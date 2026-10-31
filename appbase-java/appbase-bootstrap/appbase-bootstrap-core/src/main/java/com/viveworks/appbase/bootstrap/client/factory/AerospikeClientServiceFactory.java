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
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientService;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link AerospikeClientService}.
 *
 * <p>Five construction modes:
 * <ul>
 *   <li>Pre-built: {@link #AerospikeClientServiceFactory(AerospikeClientService)}</li>
 *   <li>Config-based: {@link #AerospikeClientServiceFactory(DefaultAerospikeClientServiceConfiguration)}</li>
 *   <li>Classpath-file: {@link #AerospikeClientServiceFactory(String)} — registers a YAML store
 *       via {@link #registerConfiguration} and loads config through the configuration service.</li>
 *   <li>Custom store: {@link #AerospikeClientServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name.</li>
 *   <li>No-arg: default constructor — relies on config already loaded into the configuration service.</li>
 * </ul>
 */
public final class AerospikeClientServiceFactory
        implements AppbaseClientServiceFactory<AerospikeClientService> {

    @Nullable private final AerospikeClientService predefinedService;
    @Nullable private final DefaultAerospikeClientServiceConfiguration config;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link AerospikeClientService} instance. */
    public AerospikeClientServiceFactory(final AerospikeClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultAerospikeClientService} from {@code config}. */
    public AerospikeClientServiceFactory(final DefaultAerospikeClientServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates a {@link DefaultAerospikeClientService} by loading config from a classpath YAML file.
     * The file is registered via {@link #registerConfiguration} before the configuration service loads.
     *
     * @param configClasspathFile classpath URI of the YAML config file (e.g. {@code classpath:///aerospike.yaml})
     */
    public AerospikeClientServiceFactory(final String configClasspathFile) {
        this.configStore       = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName     = configClasspathFile;
        this.predefinedService = null;
        this.config            = null;
    }

    /**
     * Creates a {@link DefaultAerospikeClientService} using config loaded from the supplied store.
     * {@code rawConfigName} is the key used in
     * {@code registerConfigurationTypeByRawConfigurationName} — for a classpath store this is
     * the classpath URI; for other stores use their identifying name.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public AerospikeClientServiceFactory(final ConfigurationStore configStore,
                                          final String rawConfigName) {
        this.configStore       = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName     = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService = null;
        this.config            = null;
    }

    /** Creates a {@link DefaultAerospikeClientService} using defaults from the core configuration service. */
    public AerospikeClientServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    @Override
    public Class<AerospikeClientService> serviceType() {
        return AerospikeClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.AEROSPIKE.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        DefaultAerospikeClientService.registerObjectMapperSubtypes();
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultAerospikeClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public AerospikeClientService create(final AppbaseCoreServices coreServices,
                            final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultAerospikeClientServiceConfiguration.class, config);
        }
        return new DefaultAerospikeClientService(appbaseClientCore);
    }
}
