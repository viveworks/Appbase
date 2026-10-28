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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.DefaultReadinessChecker;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.validation.ValidationService;

import java.util.Objects;

/**
 * Factory that creates the {@link ServiceRegistry}.
 *
 * <p>A {@link DefaultReadinessChecker} is created internally — it has no configuration and
 * checks all registered services for {@code STARTED} status.
 */
public final class ServiceRegistryFactory implements AppbaseCoreServiceFactory<ServiceRegistry> {

    @SuppressWarnings("unused")
    private final DefaultServiceRegistryConfiguration config;

    /**
     * Creates the factory with the given service registry configuration.
     *
     * @param config service registry configuration (must not be null)
     */
    public ServiceRegistryFactory(DefaultServiceRegistryConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Class<ServiceRegistry> serviceType() {
        return ServiceRegistry.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.SERVICE_REGISTRY.order();
    }

    @Override
    public ServiceRegistry create(AppbaseCoreServices partial) throws AppbaseException {
        LogService          log        = partial.get(LogService.class);
        MetricService       metric     = partial.get(MetricService.class);
        MediatorService     mediator   = partial.get(MediatorService.class);
        ValidationService   validation = partial.get(ValidationService.class);
        ConfigurationService configSvc = partial.get(ConfigurationService.class);
        configSvc.putTypedConfiguration(DefaultServiceRegistryConfiguration.class, config);
        return new DefaultServiceRegistry(log, metric, mediator, validation, configSvc,
                new DefaultReadinessChecker());
    }
}
