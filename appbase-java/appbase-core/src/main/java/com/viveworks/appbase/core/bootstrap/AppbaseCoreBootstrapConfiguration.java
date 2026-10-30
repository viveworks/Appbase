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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.exception.ExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorServiceConfiguration;
import com.viveworks.appbase.core.service.registry.ServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.TracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.DisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneServiceConfiguration;

/**
 * Configuration for {@link AppbaseCoreBootstrap}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AppbaseCoreBootstrapConfiguration extends AppbaseConfiguration {
    /**
     * Get {@link DisruptorServiceConfiguration}.
     *
     * @return DisruptorServiceConfiguration
     */
    DisruptorServiceConfiguration getDisruptorServiceConfiguration();


    /**
     * Get {@link LuceneServiceConfiguration}.
     *
     * @return LuceneServiceConfiguration
     */
    LuceneServiceConfiguration getLuceneServiceConfiguration();


    /**
     * Get {@link LogServiceConfiguration}.
     *
     * @return LogServiceConfiguration
     */
    LogServiceConfiguration getLogServiceConfiguration();


    /**
     * Get {@link MetricServiceConfiguration}.
     *
     * @return MetricServiceConfiguration
     */
    MetricServiceConfiguration getMetricServiceConfiguration();


    /**
     * Get {@link ValidationServiceConfiguration}.
     *
     * @return ValidationServiceConfiguration
     */
    ValidationServiceConfiguration getValidationServiceConfiguration();


    /**
     * Get {@link MediatorServiceConfiguration}.
     *
     * @return MediatorServiceConfiguration
     */
    MediatorServiceConfiguration getMediatorServiceConfiguration();


    /**
     * Get {@link ConfigurationServiceConfiguration}.
     *
     * @return ConfigurationServiceConfiguration
     */
    ConfigurationServiceConfiguration getConfigurationServiceConfiguration();


    /**
     * Get {@link ServiceRegistryConfiguration}.
     *
     * @return ServiceRegistryConfiguration
     */
    ServiceRegistryConfiguration getServiceRegistryConfiguration();


    /**
     * Get {@link ExceptionServiceConfiguration}.
     *
     * @return ExceptionServiceConfiguration
     */
    ExceptionServiceConfiguration getExceptionServiceConfiguration();


    /**
     * Get {@link TracerServiceConfiguration}.
     *
     * @return TracerServiceConfiguration
     */
    TracerServiceConfiguration getTracerServiceConfiguration();


    /**
     * Get {@link ReactorServiceConfiguration}.
     *
     * @return ReactorServiceConfiguration
     */
    ReactorServiceConfiguration getReactorServiceConfiguration();


    /**
     * Get {@link JmxServiceConfiguration}.
     *
     * @return JmxServiceConfiguration
     */
    JmxServiceConfiguration getJmxServiceConfiguration();

    /**
     * Get the per-service shutdown timeout in milliseconds.
     * If a service's {@code shutdown()} does not return within this duration,
     * an ERROR is logged and shutdown proceeds to the next service.
     *
     * @return timeout in milliseconds; default is 5000
     */
    default long getServiceShutdownTimeoutMs() {
        return 5000L;
    }
}
