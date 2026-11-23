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
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Objects;

/**
 * Creates the bootstrap metric service after the logging service is available.
 *
 * <p>Each service receives a new {@link CompositeMeterRegistry} containing a
 * {@link SimpleMeterRegistry}, so metrics work without an external registry.
 * Applications can obtain the resulting {@link MetricService} from the completed
 * {@link AppbaseCoreServices} container and attach additional registries with
 * {@link MetricService#registerMicrometerRegistry(io.micrometer.core.instrument.MeterRegistry)}.
 */
public final class MetricServiceFactory implements AppbaseCoreServiceFactory<MetricService> {

    private final DefaultMetricServiceConfiguration config;

    /**
     * Creates the factory with the given metric configuration.
     *
     * @param config metric service configuration (must not be null)
     */
    public MetricServiceFactory(DefaultMetricServiceConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Class<MetricService> serviceType() {
        return MetricService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.METRIC.order();
    }

    @Override
    public MetricService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService log = partial.get(LogService.class);
        CompositeMeterRegistry registry = new CompositeMeterRegistry();
        registry.add(new SimpleMeterRegistry());
        return new DefaultMetricService(log, registry, config);
    }
}
