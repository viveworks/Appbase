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

package com.viveworks.appbase.core;

import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.lucene.LuceneService;

import javax.annotation.Nullable;

/**
 * Facade for all the <code>core</code> services.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AppbaseCore extends Shutdownable {

    /**
     * Get the {@link LogService} implementation.
     *
     * @return Log service
     */
    LogService getLogService();


    /**
     * Get the {@link MetricService} implementation.
     *
     * @return Metric service
     */
    MetricService getMetricService();


    /**
     * Get the {@link ValidationService} implementation.
     *
     * @return Validator service
     */
    ValidationService getValidationService();


    /**
     * Get the {@link MediatorService} implementation.
     *
     * @return Mediator service
     */
    MediatorService getMediatorService();


    /**
     * Get the {@link ConfigurationService} implementation.
     *
     * @return Configuration service
     */
    ConfigurationService getConfigurationService();


    /**
     * Get the {@link ServiceRegistry} implementation.
     *
     * @return Service registry
     */
    ServiceRegistry getServiceRegistry();


    /**
     * Get the {@link ExceptionService} implementation.
     *
     * @return Exception service
     */
    ExceptionService getExceptionService();


    /**
     * Get the {@link TracerService} implementation.
     *
     * @return Tracing service
     */
    TracerService getTracerService();


    /**
     * Get the {@link ReactorService} implementation.
     *
     * @return Reactive service
     */
    ReactorService getReactorService();


    /**
     * Get the {@link JmxService} implementation.
     *
     * @return JMX service
     */
    JmxService getJmxService();


    /**
     * Get the {@link LuceneService}.
     *
     * @return  Lucene service
     */
    @Nullable
    LuceneService getLuceneService();


    /**
     * Get the configured {@link GuiceInjectorService}.
     *
     * @return Guice injector service
     */
    @Nullable
    GuiceInjectorService getGuiceInjectorService();


    /**
     * Get the {@link AppbaseCoreBootstrapConfiguration}.
     *
     * @return AppbaseCoreBootstrapConfiguration
     */
    AppbaseCoreBootstrapConfiguration getAppbaseBootstrapConfiguration();


    /**
     * Get overall status.
     *
     * @return Overall status
     */
    ServiceStatus getStatus();


    /**
     * Reload using current configuration.
     */
    void reload();


    /**
     * Reload using new {@link AppbaseCoreBootstrapConfiguration}.
     *
     * @param appbaseCoreBootstrapConfiguration AppbaseCoreBootstrapConfiguration
     */
    void reload(AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration);
}
