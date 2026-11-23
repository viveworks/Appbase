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
import com.viveworks.appbase.core.service.exception.DefaultExceptionService;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessor;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;

import java.util.Objects;

/**
 * Factory that creates the {@link ExceptionService}.
 *
 * <p>Before constructing {@link DefaultExceptionService} (and its {@link DefaultExceptionAccessor}),
 * the factory registers the supplied {@link DefaultExceptionServiceConfiguration} with the
 * {@link ConfigurationService} via {@link ConfigurationService#putTypedConfiguration}. Both the
 * accessor and the service look up that configuration at construction time.
 */
public final class ExceptionServiceFactory implements AppbaseCoreServiceFactory<ExceptionService> {

    private final DefaultExceptionServiceConfiguration config;

    /**
     * Creates the factory with the given exception service configuration.
     *
     * @param config exception service configuration (must not be null)
     */
    public ExceptionServiceFactory(DefaultExceptionServiceConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Class<ExceptionService> serviceType() {
        return ExceptionService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.EXCEPTION.order();
    }

    @Override
    public ExceptionService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService          log        = partial.get(LogService.class);
        MetricService       metric     = partial.get(MetricService.class);
        MediatorService     mediator   = partial.get(MediatorService.class);
        ValidationService   validation = partial.get(ValidationService.class);
        ConfigurationService configSvc = partial.get(ConfigurationService.class);

        configSvc.putTypedConfiguration(DefaultExceptionServiceConfiguration.class, config);

        DefaultExceptionAccessor accessor = new DefaultExceptionAccessor(
                log, metric, mediator, validation, configSvc,
                new DefaultExceptionMessageContextCodec());

        return new DefaultExceptionService(log, metric, mediator, validation, configSvc, accessor);
    }
}
