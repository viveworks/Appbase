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
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;

import java.util.Objects;

/**
 * Factory that creates the {@link ValidationService}.
 *
 * <p>{@link DefaultValidationService} accepts only {@link LogService} and {@link MetricService};
 * the supplied {@link DefaultValidationServiceConfiguration} is stored for API symmetry and
 * potential future use.
 */
public final class ValidationServiceFactory implements AppbaseCoreServiceFactory<ValidationService> {

    @SuppressWarnings("unused")
    private final DefaultValidationServiceConfiguration config;

    /**
     * Creates the factory with the given validation configuration.
     *
     * @param config validation service configuration (must not be null)
     */
    public ValidationServiceFactory(DefaultValidationServiceConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Class<ValidationService> serviceType() {
        return ValidationService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.VALIDATION.order();
    }

    @Override
    public ValidationService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService log    = partial.get(LogService.class);
        MetricService metric = partial.get(MetricService.class);
        return new DefaultValidationService(log, metric);
    }
}
