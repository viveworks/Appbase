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

package com.viveworks.appbase.bootstrap.client.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServiceFactory;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.service.cassandra.CassandraClientService;
import com.viveworks.appbase.client.service.cassandra.DefaultCassandraClientService;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link CassandraClientService}.
 *
 * <p>Five construction modes:
 * <ul>
 *   <li>Pre-built: {@link #CassandraClientServiceFactory(CassandraClientService)}</li>
 *   <li>Config-based: {@link #CassandraClientServiceFactory(DefaultCassandraClientServiceConfiguration)}</li>
 *   <li>Classpath-file: {@link #CassandraClientServiceFactory(String)} — registers a YAML store
 *       via {@link #registerConfiguration} and loads config through the configuration service.</li>
 *   <li>Custom store: {@link #CassandraClientServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name.</li>
 *   <li>No-arg: default constructor — relies on config already loaded into the configuration service.</li>
 * </ul>
 */
public final class CassandraClientServiceFactory
        implements AppbaseClientServiceFactory<CassandraClientService> {

    @Nullable private final CassandraClientService predefinedService;
    @Nullable private final DefaultCassandraClientServiceConfiguration config;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link CassandraClientService} instance. */
    public CassandraClientServiceFactory(final CassandraClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultCassandraClientService} from {@code config}. */
    public CassandraClientServiceFactory(final DefaultCassandraClientServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultCassandraClientService} using defaults from the core configuration service. */
    public CassandraClientServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates a {@link DefaultCassandraClientService} by loading config from a classpath YAML file.
     *
     * @param configClasspathFile classpath URI of the YAML config file
     */
    public CassandraClientServiceFactory(final String configClasspathFile) {
        this.configStore       = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName     = configClasspathFile;
        this.predefinedService = null;
        this.config            = null;
    }

    /**
     * Creates a {@link DefaultCassandraClientService} using config loaded from the supplied store.
     * {@code rawConfigName} is the key used in
     * {@code registerConfigurationTypeByRawConfigurationName} — for a classpath store this is
     * the classpath URI; for other stores use their identifying name.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public CassandraClientServiceFactory(final ConfigurationStore configStore,
                                          final String rawConfigName) {
        this.configStore       = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName     = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<CassandraClientService> serviceType() {
        return CassandraClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.CASSANDRA.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultCassandraClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public CassandraClientService create(final AppbaseCoreServices coreServices,
                            final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultCassandraClientServiceConfiguration.class, config);
        }
        return new DefaultCassandraClientService(appbaseClientCore);
    }
}
