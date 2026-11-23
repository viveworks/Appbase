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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.DefaultTracerService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;

import java.util.Objects;

/**
 * Factory that creates the {@link TracerService}.
 *
 * <p>The supplied {@link DefaultTracerServiceConfiguration} is registered with the
 * {@link ConfigurationService} before constructing {@link DefaultTracerService}, which
 * looks up that configuration at construction time.
 */
public final class TracerServiceFactory implements AppbaseCoreServiceFactory<TracerService> {

    private final DefaultTracerServiceConfiguration config;

    /**
     * Creates the factory with the given tracer service configuration.
     *
     * @param config tracer service configuration (must not be null)
     */
    public TracerServiceFactory(DefaultTracerServiceConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Class<TracerService> serviceType() {
        return TracerService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.TRACER.order();
    }

    @Override
    public TracerService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService          log        = partial.get(LogService.class);
        MetricService       metric     = partial.get(MetricService.class);
        MediatorService     mediator   = partial.get(MediatorService.class);
        ValidationService   validation = partial.get(ValidationService.class);
        ConfigurationService configSvc = partial.get(ConfigurationService.class);

        configSvc.putTypedConfiguration(DefaultTracerServiceConfiguration.class, config);

        return new DefaultTracerService(log, metric, mediator, validation, configSvc);
    }
}
