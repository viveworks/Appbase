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
import com.viveworks.appbase.client.service.jdbc.DefaultJdbcClientService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientService;
import com.viveworks.appbase.client.service.jdbi.DefaultJdbiClientService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.client.service.jdbi.configuration.DefaultJdbiClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link JdbiClientService} using the Vitess JDBC driver via JDBI.
 *
 * <p>Four construction modes:
 * <ul>
 *   <li>Pre-built: {@link #VitessJdbiClientServiceFactory(JdbiClientService)}</li>
 *   <li>Config-based: {@link #VitessJdbiClientServiceFactory(DefaultJdbiClientServiceConfiguration,
 *       JdbcClientService)} — requires a pre-built {@link JdbcClientService}.</li>
 *   <li>Classpath-file: {@link #VitessJdbiClientServiceFactory(String)} — creates a transient
 *       {@link DefaultJdbcClientService} internally. Use this when JDBC is not registered as
 *       a separate service in the container.</li>
 *   <li>Custom store: {@link #VitessJdbiClientServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name; creates a transient
 *       {@link DefaultJdbcClientService} internally.</li>
 * </ul>
 */
public final class VitessJdbiClientServiceFactory implements AppbaseClientServiceFactory<JdbiClientService> {

    @Nullable private final JdbiClientService predefinedService;
    @Nullable private final DefaultJdbiClientServiceConfiguration config;
    @Nullable private final JdbcClientService jdbcClientService;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link JdbiClientService} instance. */
    public VitessJdbiClientServiceFactory(final JdbiClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.jdbcClientService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates a {@link DefaultJdbiClientService} from config and an explicit JDBC dependency.
     *
     * @param config            JDBI service configuration
     * @param jdbcClientService pre-built JDBC client service
     */
    public VitessJdbiClientServiceFactory(final DefaultJdbiClientServiceConfiguration config,
                                           final JdbcClientService jdbcClientService) {
        this.config            = Objects.requireNonNull(config, "config");
        this.jdbcClientService = Objects.requireNonNull(jdbcClientService, "jdbcClientService");
        this.predefinedService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates a {@link DefaultJdbiClientService} by loading config from a classpath YAML file.
     * A transient {@link DefaultJdbcClientService} is created internally.
     *
     * @param configClasspathFile classpath URI of the JDBI YAML config file
     */
    public VitessJdbiClientServiceFactory(final String configClasspathFile) {
        this.configStore       = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName     = configClasspathFile;
        this.predefinedService = null;
        this.config            = null;
        this.jdbcClientService = null;
    }

    /**
     * Creates a {@link DefaultJdbiClientService} using config loaded from the supplied store.
     * A transient {@link DefaultJdbcClientService} is created internally.
     * {@code rawConfigName} is the key used in
     * {@code registerConfigurationTypeByRawConfigurationName} — for a classpath store this is
     * the classpath URI; for other stores use their identifying name.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public VitessJdbiClientServiceFactory(final ConfigurationStore configStore,
                                           final String rawConfigName) {
        this.configStore       = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName     = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService = null;
        this.config            = null;
        this.jdbcClientService = null;
    }

    @Override
    public Class<JdbiClientService> serviceType() {
        return JdbiClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.VITESS_JDBI.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        DefaultJdbiClientService.registerObjectMapperSubtypes();
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultJdbiClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public JdbiClientService create(final AppbaseCoreServices coreServices,
                                     final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultJdbiClientServiceConfiguration.class, config);
            return new DefaultJdbiClientService(appbaseClientCore, jdbcClientService);
        }
        final JdbcClientService jdbc = new DefaultJdbcClientService(appbaseClientCore);
        return new DefaultJdbiClientService(appbaseClientCore, jdbc);
    }
}
