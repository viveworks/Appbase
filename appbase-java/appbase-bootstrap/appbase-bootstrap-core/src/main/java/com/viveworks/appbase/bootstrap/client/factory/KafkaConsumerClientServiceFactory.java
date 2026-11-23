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
import com.viveworks.appbase.client.service.kafka.consumer.DefaultKafkaConsumerClientService;
import com.viveworks.appbase.client.service.kafka.consumer.KafkaConsumerClientService;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link KafkaConsumerClientService}.
 *
 * <p>Raw types are used because {@link KafkaConsumerClientService} is generic ({@code <K, V>}).
 * The type parameters are not relevant at the bootstrap level.
 *
 * <p>Five construction modes:
 * <ul>
 *   <li>Pre-built: {@link #KafkaConsumerClientServiceFactory(KafkaConsumerClientService)}</li>
 *   <li>Config-based: {@link #KafkaConsumerClientServiceFactory(DefaultKafkaConsumerClientServiceConfiguration)}</li>
 *   <li>Classpath-file: {@link #KafkaConsumerClientServiceFactory(String)} — registers a YAML store
 *       via {@link #registerConfiguration} and loads config through the configuration service.</li>
 *   <li>Custom store: {@link #KafkaConsumerClientServiceFactory(ConfigurationStore, String)} — supply
 *       any {@link ConfigurationStore} and its raw config name.</li>
 *   <li>No-arg: default constructor — relies on config already loaded into the configuration service.</li>
 * </ul>
 */
@SuppressWarnings("rawtypes")
public final class KafkaConsumerClientServiceFactory
        implements AppbaseClientServiceFactory<KafkaConsumerClientService> {

    @Nullable private final KafkaConsumerClientService predefinedService;
    @Nullable private final DefaultKafkaConsumerClientServiceConfiguration config;
    @Nullable private final ConfigurationStore configStore;
    @Nullable private final String rawConfigName;

    /** Wraps a pre-built {@link KafkaConsumerClientService} instance. */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "No checked exceptions thrown")
    public KafkaConsumerClientServiceFactory(final KafkaConsumerClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultKafkaConsumerClientService} from {@code config}. */
    public KafkaConsumerClientServiceFactory(final DefaultKafkaConsumerClientServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /** Creates a {@link DefaultKafkaConsumerClientService} using defaults from the core configuration service. */
    @SuppressWarnings("rawtypes")
    public KafkaConsumerClientServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
        this.configStore       = null;
        this.rawConfigName     = null;
    }

    /**
     * Creates a {@link DefaultKafkaConsumerClientService} by loading config from a classpath YAML file.
     *
     * @param configClasspathFile classpath URI of the YAML config file
     */
    public KafkaConsumerClientServiceFactory(final String configClasspathFile) {
        this.configStore       = new ClasspathConfigurationStore(
            Objects.requireNonNull(configClasspathFile, "configClasspathFile"), MimeType.YAML);
        this.rawConfigName     = configClasspathFile;
        this.predefinedService = null;
        this.config            = null;
    }

    /**
     * Creates a {@link DefaultKafkaConsumerClientService} using config loaded from the supplied store.
     * {@code rawConfigName} is the key used in
     * {@code registerConfigurationTypeByRawConfigurationName} — for a classpath store this is
     * the classpath URI; for other stores use their identifying name.
     *
     * @param configStore   the configuration store to register
     * @param rawConfigName the raw configuration name key
     */
    public KafkaConsumerClientServiceFactory(final ConfigurationStore configStore,
                                              final String rawConfigName) {
        this.configStore       = Objects.requireNonNull(configStore, "configStore");
        this.rawConfigName     = Objects.requireNonNull(rawConfigName, "rawConfigName");
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<KafkaConsumerClientService> serviceType() {
        return KafkaConsumerClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.KAFKA_CONSUMER.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (configStore == null) {
            return;
        }
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultKafkaConsumerClientServiceConfiguration.class, rawConfigName);
        accessor.registerConfigurationStore(configStore);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public KafkaConsumerClientService create(final AppbaseCoreServices coreServices,
                            final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultKafkaConsumerClientServiceConfiguration.class, config);
        }
        return new DefaultKafkaConsumerClientService(appbaseClientCore);
    }
}
