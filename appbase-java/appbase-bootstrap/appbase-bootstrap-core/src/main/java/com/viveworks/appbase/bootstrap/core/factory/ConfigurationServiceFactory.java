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
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.DefaultConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Factory that creates the {@link ConfigurationService}.
 *
 * <p>A {@link DefaultConfigurationAccessor} is created with empty codec maps and no backing
 * configuration stores. Typed configurations needed by later services (tracer, JMX, exception,
 * reactor) are registered programmatically via
 * {@link ConfigurationService#putTypedConfiguration} inside each respective service factory
 * before those services are constructed.
 *
 * <p>To add real configuration stores or codecs, supply a custom factory via
 * {@link com.viveworks.appbase.bootstrap.core.AppbaseCoreServices.Builder#with}.
 */
public final class ConfigurationServiceFactory implements AppbaseCoreServiceFactory<ConfigurationService> {

    @Nullable
    private final DefaultConfigurationServiceConfiguration config;

    /**
     * Creates the factory with the given configuration service configuration.
     *
     * @param config configuration service configuration; {@code null} uses service defaults
     */
    public ConfigurationServiceFactory(@Nullable DefaultConfigurationServiceConfiguration config) {
        this.config = config;
    }

    @Override
    public Class<ConfigurationService> serviceType() {
        return ConfigurationService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.CONFIGURATION.order();
    }

    @Override
    public ConfigurationService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService        log        = partial.get(LogService.class);
        MetricService     metric     = partial.get(MetricService.class);
        MediatorService   mediator   = partial.get(MediatorService.class);
        ValidationService validation = partial.get(ValidationService.class);

        DefaultConfigurationAccessor accessor = new DefaultConfigurationAccessor(
                log,
                metric,
                Map.of(MimeType.YAML, new YamlConfigurationCodec()),
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyMap());

        return new DefaultConfigurationService(log, metric, mediator, validation, accessor, config);
    }
}
