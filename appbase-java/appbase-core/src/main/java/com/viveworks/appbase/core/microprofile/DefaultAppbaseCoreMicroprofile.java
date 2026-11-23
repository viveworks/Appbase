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

package com.viveworks.appbase.core.microprofile;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.DefaultAppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrappedServices;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AppbaseCoreMicroprofile}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAppbaseCoreMicroprofile implements AppbaseCoreMicroprofile {
    private final Logger logger;
    private final AppbaseCore appbaseCore;
    private final AtomicReference<MicroprofileConfigurationService> microprofileConfigurationServiceRef;
    private final AtomicReference<MicroprofileServiceRegistry> microprofileServiceRegistryRef;
    private final AtomicReference<AppbaseCoreBootstrapConfiguration> appbaseBootstrapConfigurationRef;
    private final AtomicReference<AppbaseCoreBootstrappedServices> bootstrappedServicesRef;
    private final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap;
    private final AtomicBoolean shutdown;

    /**
     * Bootstraps a new default core and exposes the MicroProfile services produced during that bootstrap.
     */
    public DefaultAppbaseCoreMicroprofile() {
        final DefaultAppbaseCore defaultAppbaseCore = new DefaultAppbaseCore();
        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                defaultAppbaseCore.getBootstrappedServices();
        AppbaseUtil.notNull(appbaseCoreBootstrappedServices, "appbaseCoreBootstrappedServices");

        this.microprofileConfigurationServiceRef =
                new AtomicReference<>(appbaseCoreBootstrappedServices.getMicroprofileConfigurationService());
        this.microprofileServiceRegistryRef =
                new AtomicReference<>(appbaseCoreBootstrappedServices.getMicroprofileServiceRegistry());
        this.appbaseCore = defaultAppbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());

        this.appbaseBootstrapConfigurationRef =
                new AtomicReference<>(defaultAppbaseCore.getAppbaseBootstrapConfiguration());
        this.bootstrappedServicesRef = new AtomicReference<>(appbaseCoreBootstrappedServices);
        this.appbaseCoreBootstrap = defaultAppbaseCore.getAppbaseCoreBootstrap();
        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Runs the supplied bootstrap strategy with its default configuration and retains the resulting MicroProfile services.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to create the backing default core
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if the bootstrap strategy is {@code null}
     */
    public DefaultAppbaseCoreMicroprofile(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");

        final DefaultAppbaseCore defaultAppbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap);
        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                defaultAppbaseCore.getBootstrappedServices();
        AppbaseUtil.notNull(appbaseCoreBootstrappedServices, "appbaseCoreBootstrappedServices");

        microprofileConfigurationServiceRef =
                new AtomicReference<>(appbaseCoreBootstrappedServices.getMicroprofileConfigurationService());
        microprofileServiceRegistryRef =
                new AtomicReference<>(appbaseCoreBootstrappedServices.getMicroprofileServiceRegistry());
        appbaseCore = defaultAppbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());

        this.appbaseBootstrapConfigurationRef =
                new AtomicReference<>(defaultAppbaseCore.getAppbaseBootstrapConfiguration());
        this.bootstrappedServicesRef = new AtomicReference<>(appbaseCoreBootstrappedServices);
        this.appbaseCoreBootstrap = appbaseCoreBootstrap;

        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Runs the supplied bootstrap strategy with the explicit configuration and retains the resulting MicroProfile services.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to create the backing default core
     * @param appbaseCoreBootstrapConfiguration configuration supplied to that bootstrap strategy
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either bootstrap argument is {@code null}
     */
    public DefaultAppbaseCoreMicroprofile(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");

        final DefaultAppbaseCore defaultAppbaseCore =
                new DefaultAppbaseCore(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                defaultAppbaseCore.getBootstrappedServices();
        AppbaseUtil.notNull(appbaseCoreBootstrappedServices, "appbaseCoreBootstrappedServices");

        microprofileConfigurationServiceRef =
                new AtomicReference<>(appbaseCoreBootstrappedServices.getMicroprofileConfigurationService());
        microprofileServiceRegistryRef =
                new AtomicReference<>(appbaseCoreBootstrappedServices.getMicroprofileServiceRegistry());
        appbaseCore = defaultAppbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());

        this.appbaseBootstrapConfigurationRef = new AtomicReference<>(appbaseCoreBootstrapConfiguration);
        this.bootstrappedServicesRef = new AtomicReference<>(appbaseCoreBootstrappedServices);
        this.appbaseCoreBootstrap = appbaseCoreBootstrap;

        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Wraps the supplied core and MicroProfile services, and immediately bootstraps a separate default service graph used
     * by later reloads.
     *
     * @param appbaseCore existing core exposed by this facade
     * @param microprofileConfigurationService MicroProfile configuration service retained by the facade
     * @param microprofileServiceRegistry MicroProfile registry retained by the facade
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if any supplied service is {@code null}
     */
    @Inject
    public DefaultAppbaseCoreMicroprofile(
            final AppbaseCore appbaseCore,
            final MicroprofileConfigurationService microprofileConfigurationService,
            final MicroprofileServiceRegistry microprofileServiceRegistry)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(microprofileConfigurationService, "microprofileConfigurationService");
        AppbaseUtil.notNull(microprofileServiceRegistry, "microprofileServiceRegistry");

        this.appbaseCore = appbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.microprofileConfigurationServiceRef = new AtomicReference<>(microprofileConfigurationService);
        this.microprofileServiceRegistryRef = new AtomicReference<>(microprofileServiceRegistry);

        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                DefaultAppbaseCoreBootstrapConfiguration.builder().build();
        this.appbaseBootstrapConfigurationRef = new AtomicReference<>(appbaseCoreBootstrapConfiguration);

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> localAppbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                localAppbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
        AppbaseUtil.notNull(appbaseCoreBootstrappedServices, "appbaseCoreBootstrappedServices");

        this.bootstrappedServicesRef = new AtomicReference<>(appbaseCoreBootstrappedServices);
        this.appbaseCoreBootstrap = localAppbaseCoreBootstrap;

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public LogService getLogService() {
        return appbaseCore.getLogService();
    }

    @Override
    public MetricService getMetricService() {
        return appbaseCore.getMetricService();
    }

    @Override
    public ValidationService getValidationService() {
        return appbaseCore.getValidationService();
    }

    @Override
    public MediatorService getMediatorService() {
        return appbaseCore.getMediatorService();
    }

    @Override
    public ConfigurationService getConfigurationService() {
        return appbaseCore.getConfigurationService();
    }

    @Override
    public MicroprofileConfigurationService getMicroprofileConfigurationService() {
        return microprofileConfigurationServiceRef.get();
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return appbaseCore.getServiceRegistry();
    }

    @Override
    public MicroprofileServiceRegistry getMicroprofileServiceRegistry() {
        return microprofileServiceRegistryRef.get();
    }

    @Override
    public ExceptionService getExceptionService() {
        return appbaseCore.getExceptionService();
    }

    @Override
    public TracerService getTracerService() {
        return appbaseCore.getTracerService();
    }

    @Override
    public ReactorService getReactorService() {
        return appbaseCore.getReactorService();
    }

    @Override
    public JmxService getJmxService() {
        return appbaseCore.getJmxService();
    }

    @Nullable
    @Override
    public LuceneService getLuceneService() {
        return appbaseCore.getLuceneService();
    }

    @Nullable
    @Override
    public GuiceInjectorService getGuiceInjectorService() {
        return appbaseCore.getGuiceInjectorService();
    }

    @Override
    public AppbaseCoreBootstrapConfiguration getAppbaseBootstrapConfiguration() {
        return appbaseCore.getAppbaseBootstrapConfiguration();
    }

    @Override
    public ServiceStatus getStatus() {
        if (!shutdown.get()) {
            return ServiceStatus.STARTED;
        }
        return ServiceStatus.SHUTDOWN;
    }

    @Override
    public void reload() {
        logger.info("Reloading {}", getClass().getName());

        appbaseCore.reload();
        shutdown();
        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                appbaseBootstrapConfigurationRef.get();
        final AppbaseCoreBootstrappedServices localBootstrapServices =
                appbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
        microprofileConfigurationServiceRef.set(localBootstrapServices.getMicroprofileConfigurationService());
        microprofileServiceRegistryRef.set(localBootstrapServices.getMicroprofileServiceRegistry());
        shutdown.set(false);
    }

    @Override
    public void reload(final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration) {
        logger.info("Reloading {} using {}", getClass().getName(), appbaseCoreBootstrapConfiguration);

        appbaseCore.reload(appbaseCoreBootstrapConfiguration);
        shutdown();
        final AppbaseCoreBootstrappedServices localBootstrapServices =
                appbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
        microprofileConfigurationServiceRef.set(localBootstrapServices.getMicroprofileConfigurationService());
        microprofileServiceRegistryRef.set(localBootstrapServices.getMicroprofileServiceRegistry());
        this.appbaseBootstrapConfigurationRef.set(appbaseCoreBootstrapConfiguration);
        shutdown.set(false);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        if (shutdown.get()) {
            return;
        }

        logger.info("Shutting down {}", getClass().getName());
        final MicroprofileConfigurationService microprofileConfigurationService =
                microprofileConfigurationServiceRef.get();
        try {
            microprofileConfigurationService.shutdown();
        } catch (Exception ex) {
            logger.error("Error shutting down {}", microprofileConfigurationService.getClass().getName());
        }

        final MicroprofileServiceRegistry microprofileServiceRegistry = microprofileServiceRegistryRef.get();
        try {
            microprofileServiceRegistry.shutdown();
        } catch (Exception ex) {
            logger.error("Error shutting down {}", microprofileServiceRegistry.getClass().getName());
        }

        try {
            appbaseCore.shutdown();
        } catch (Exception ex) {
            logger.error("Error shutting down {}", microprofileServiceRegistry.getClass().getName());
        }

        shutdown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
