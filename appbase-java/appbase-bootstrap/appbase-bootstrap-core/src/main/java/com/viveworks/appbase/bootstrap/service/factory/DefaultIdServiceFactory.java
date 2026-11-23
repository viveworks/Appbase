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

package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link IdService}.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultIdServiceFactory(IdService)}</li>
 *   <li>Config-based: {@link #DefaultIdServiceFactory(DefaultIdServiceConfiguration)}</li>
 *   <li>Default: {@link #DefaultIdServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class DefaultIdServiceFactory implements AppbaseServiceFactory<IdService> {

    @Nullable private final IdService predefinedService;
    @Nullable private final DefaultIdServiceConfiguration config;

    /** Wraps a pre-built {@link IdService} instance. */
    public DefaultIdServiceFactory(final IdService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultIdService} from {@code config}. */
    public DefaultIdServiceFactory(final DefaultIdServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultIdService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public DefaultIdServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<IdService> serviceType() {
        return IdService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.ID.order();
    }

    @Override
    public IdService create(final AppbaseCoreServices coreServices,
                             final AppbaseClientServices clientServices,
                             final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultIdServiceConfiguration.class, config);
        }
        final DefaultIdServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultIdServiceConfiguration.class);
        return new DefaultIdService(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg);
    }
}
