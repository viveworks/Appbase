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
import com.viveworks.appbase.client.service.druid.sql.DefaultDruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameterFactory;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientSharedResourcesConfiguration;
import com.viveworks.appbase.client.service.druid.sql.configuration.DefaultDruidSqlClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link DruidSqlClientService}.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DruidSqlClientServiceFactory(DruidSqlClientService)}</li>
 *   <li>Classpath-file: {@link #DruidSqlClientServiceFactory(String, String)} — takes two YAML
 *       files (shared resources config and service config), registers them via
 *       {@link #registerConfiguration}, and builds a {@link DefaultDruidSqlClientService}
 *       with internally-created shared resources and parameter factory.</li>
 *   <li>Custom stores: {@link #DruidSqlClientServiceFactory(ConfigurationStore, String, ConfigurationStore, String)}
 *       — supply any two {@link ConfigurationStore}s and their raw config names.</li>
 * </ul>
 */
public final class DruidSqlClientServiceFactory implements AppbaseClientServiceFactory<DruidSqlClientService> {

    @Nullable private final DruidSqlClientService predefinedService;
    @Nullable private final ConfigurationStore sharedResourcesStore;
    @Nullable private final String sharedResourcesRawName;
    @Nullable private final ConfigurationStore serviceStore;
    @Nullable private final String serviceRawName;

    /** Wraps a pre-built {@link DruidSqlClientService} instance. */
    public DruidSqlClientServiceFactory(final DruidSqlClientService service) {
        this.predefinedService       = Objects.requireNonNull(service, "service");
        this.sharedResourcesStore    = null;
        this.sharedResourcesRawName  = null;
        this.serviceStore            = null;
        this.serviceRawName          = null;
    }

    /**
     * Creates a {@link DefaultDruidSqlClientService} by loading config from two classpath YAML files.
     * {@link DefaultDruidSqlClientSharedResources} and {@link DefaultDruidSqlParameterFactory}
     * are created internally.
     *
     * @param sharedResourcesClasspathFile classpath URI for the shared-resources YAML config
     * @param serviceClasspathFile         classpath URI for the service YAML config
     */
    public DruidSqlClientServiceFactory(final String sharedResourcesClasspathFile,
                                         final String serviceClasspathFile) {
        this.sharedResourcesStore   = new ClasspathConfigurationStore(
            Objects.requireNonNull(sharedResourcesClasspathFile, "sharedResourcesClasspathFile"),
            MimeType.YAML);
        this.sharedResourcesRawName = sharedResourcesClasspathFile;
        this.serviceStore           = new ClasspathConfigurationStore(
            Objects.requireNonNull(serviceClasspathFile, "serviceClasspathFile"),
            MimeType.YAML);
        this.serviceRawName         = serviceClasspathFile;
        this.predefinedService      = null;
    }

    /**
     * Creates a {@link DefaultDruidSqlClientService} using config loaded from the supplied stores.
     * {@link DefaultDruidSqlClientSharedResources} and {@link DefaultDruidSqlParameterFactory}
     * are created internally.
     * The raw name parameters are the keys used in
     * {@code registerConfigurationTypeByRawConfigurationName}.
     *
     * @param sharedResourcesStore   store for the shared-resources config
     * @param sharedResourcesRawName raw config name for the shared-resources store
     * @param serviceStore           store for the service config
     * @param serviceRawName         raw config name for the service store
     */
    public DruidSqlClientServiceFactory(final ConfigurationStore sharedResourcesStore,
                                         final String sharedResourcesRawName,
                                         final ConfigurationStore serviceStore,
                                         final String serviceRawName) {
        this.sharedResourcesStore   = Objects.requireNonNull(sharedResourcesStore, "sharedResourcesStore");
        this.sharedResourcesRawName = Objects.requireNonNull(sharedResourcesRawName, "sharedResourcesRawName");
        this.serviceStore           = Objects.requireNonNull(serviceStore, "serviceStore");
        this.serviceRawName         = Objects.requireNonNull(serviceRawName, "serviceRawName");
        this.predefinedService      = null;
    }

    @Override
    public Class<DruidSqlClientService> serviceType() {
        return DruidSqlClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.DRUID.order();
    }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        if (sharedResourcesStore == null || serviceStore == null) {
            return;
        }
        DefaultDruidSqlClientService.registerObjectMapperSubtypes();
        final ConfigurationAccessor accessor =
            coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultDruidSqlClientSharedResourcesConfiguration.class, sharedResourcesRawName);
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultDruidSqlClientServiceConfiguration.class, serviceRawName);
        accessor.registerConfigurationStore(sharedResourcesStore);
        accessor.registerConfigurationStore(serviceStore);
    }

    @Override
    public DruidSqlClientService create(final AppbaseCoreServices coreServices,
                                         final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        final DefaultDruidSqlClientSharedResources shared =
            new DefaultDruidSqlClientSharedResources(appbaseClientCore);
        final DefaultDruidSqlParameterFactory params =
            new DefaultDruidSqlParameterFactory(shared);
        return new DefaultDruidSqlClientService(appbaseClientCore, shared, params);
    }
}
