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

package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.cluster.ClusterService;
import com.viveworks.appbase.service.cluster.StaticClusterService;
import com.viveworks.appbase.service.cluster.StaticClusterServiceConfiguration;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link ClusterService} using {@link StaticClusterService}.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #StaticClusterServiceFactory(ClusterService)}</li>
 *   <li>Config-based: {@link #StaticClusterServiceFactory(StaticClusterServiceConfiguration)}</li>
 *   <li>Default: {@link #StaticClusterServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class StaticClusterServiceFactory implements AppbaseServiceFactory<ClusterService> {

    @Nullable private final ClusterService predefinedService;
    @Nullable private final StaticClusterServiceConfiguration config;

    /** Wraps a pre-built {@link ClusterService} instance. */
    public StaticClusterServiceFactory(final ClusterService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link StaticClusterService} from {@code config}. */
    public StaticClusterServiceFactory(final StaticClusterServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link StaticClusterService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public StaticClusterServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<ClusterService> serviceType() {
        return ClusterService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.CLUSTER.order();
    }

    @Override
    public ClusterService create(final AppbaseCoreServices coreServices,
                                  final AppbaseClientServices clientServices,
                                  final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                StaticClusterServiceConfiguration.class, config);
        }
        final StaticClusterServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(StaticClusterServiceConfiguration.class);
        return new StaticClusterService(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg);
    }
}
