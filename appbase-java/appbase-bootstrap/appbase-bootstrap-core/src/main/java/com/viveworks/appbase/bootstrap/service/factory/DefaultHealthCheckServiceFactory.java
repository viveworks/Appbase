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
import com.viveworks.appbase.service.health.DefaultHealthCheckService;
import com.viveworks.appbase.service.health.DefaultHealthCheckServiceConfiguration;
import com.viveworks.appbase.service.health.HealthCheckService;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link HealthCheckService}.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultHealthCheckServiceFactory(HealthCheckService)}</li>
 *   <li>Config-based: {@link #DefaultHealthCheckServiceFactory(DefaultHealthCheckServiceConfiguration)}</li>
 *   <li>Default: {@link #DefaultHealthCheckServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class DefaultHealthCheckServiceFactory implements AppbaseServiceFactory<HealthCheckService> {

    @Nullable private final HealthCheckService predefinedService;
    @Nullable private final DefaultHealthCheckServiceConfiguration config;

    /** Wraps a pre-built {@link HealthCheckService} instance. */
    public DefaultHealthCheckServiceFactory(final HealthCheckService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultHealthCheckService} from {@code config}. */
    public DefaultHealthCheckServiceFactory(final DefaultHealthCheckServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultHealthCheckService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public DefaultHealthCheckServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<HealthCheckService> serviceType() {
        return HealthCheckService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.HEALTH_CHECK.order();
    }

    @Override
    public HealthCheckService create(final AppbaseCoreServices coreServices,
                                      final AppbaseClientServices clientServices,
                                      final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultHealthCheckServiceConfiguration.class, config);
        }
        final DefaultHealthCheckServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultHealthCheckServiceConfiguration.class);
        return new DefaultHealthCheckService(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg);
    }
}
