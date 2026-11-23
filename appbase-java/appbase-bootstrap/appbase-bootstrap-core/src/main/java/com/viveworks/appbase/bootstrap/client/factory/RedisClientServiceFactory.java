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
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.redis.DefaultRedisClientService;
import com.viveworks.appbase.client.service.redis.RedisClientService;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link RedisClientService}.
 *
 * <p>Five construction modes:
 * <ul>
 *   <li>Pre-built: {@link #RedisClientServiceFactory(RedisClientService)}</li>
 *   <li>Config-based: {@link #RedisClientServiceFactory(DefaultRedisClientServiceConfiguration)}</li>
 *   <li>Classpath-file: {@link #RedisClientServiceFactory(String)} — registers a YAML store
 *       via {@link #registerConfiguration} and loads config through the configuration service.</li>
 *   <li>Custom store: {@link #RedisClientServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name.</li>
 *   <li>No-arg: default constructor — relies on config already loaded into the configuration service.</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class RedisClientServiceFactory implements AppbaseClientServiceFactory<RedisClientService> {

    @Nullable private final RedisClientService predefinedService;
    @Nullable private final DefaultRedisClientServiceConfiguration config;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link RedisClientService} instance. */
    public RedisClientServiceFactory(final RedisClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultRedisClientService} from {@code config}. */
    public RedisClientServiceFactory(final DefaultRedisClientServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultRedisClientService} using defaults from the core configuration service. */
    public RedisClientServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates a {@link DefaultRedisClientService} by loading config from a classpath YAML file.
     *
     * @param configClasspathFile classpath URI of the YAML config file
     */
    public RedisClientServiceFactory(final String configClasspathFile) {
        this.configStore       = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName     = configClasspathFile;
        this.predefinedService = null;
        this.config            = null;
    }

    /**
     * Creates a {@link DefaultRedisClientService} using config loaded from the supplied store.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public RedisClientServiceFactory(final ConfigurationStore configStore,
                                      final String rawConfigName) {
        this.configStore       = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName     = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<RedisClientService> serviceType() {
        return RedisClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.REDIS.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultRedisClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    public RedisClientService create(final AppbaseCoreServices coreServices,
                                      final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultRedisClientServiceConfiguration.class, config);
        }
        return new DefaultRedisClientService(appbaseClientCore);
    }
}
