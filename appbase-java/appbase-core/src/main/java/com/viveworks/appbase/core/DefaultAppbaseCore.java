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

package com.viveworks.appbase.core;

import com.viveworks.appbase.core.bootstrap.AppbaseBootstrapConfigurationException;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrappedServices;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default owner of the core service graph.
 *
 * <p>The instance bootstraps its services during construction and shuts down the old graph before publishing replacement
 * service references
 * individually during reload. Callers should retain this facade rather than caching service references
 * if they need to observe a later reload. Shutdown is idempotent. Although service references are
 * safe to read concurrently, callers must serialize lifecycle operations such as reload and shutdown.</p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:IllegalCatch")
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DefaultAppbaseCore implements AppbaseCore {
    private final AtomicReference<Logger> loggerRef;
    private final AtomicReference<LogService> logServiceRef;
    private final AtomicReference<MetricService> metricServiceRef;
    private final AtomicReference<ValidationService> validationServiceRef;
    private final AtomicReference<MediatorService> mediatorServiceRef;
    private final AtomicReference<ConfigurationService> configurationServiceRef;
    private final AtomicReference<ServiceRegistry> serviceRegistryRef;
    private final AtomicReference<ExceptionService> exceptionServiceRef;
    private final AtomicReference<TracerService> tracerServiceRef;
    private final AtomicReference<ReactorService> reactorServiceRef;
    private final AtomicReference<JmxService> jmxServiceRef;
    private final AtomicReference<LuceneService> luceneServiceRef;
    private final AtomicReference<AppbaseCoreBootstrapConfiguration> appbaseBootstrapConfigurationRef;
    @Nullable private final AtomicReference<AppbaseCoreBootstrappedServices> bootstrappedServicesRef;
    private final AtomicReference<GuiceInjectorService> guiceInjectorServiceRef;
    private final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap;
    private final AtomicBoolean shutdown;
    // Serializes shutdown()/reload()/reload(configuration) (Lens 3 L3-01): shutdown() used to read
    // `shutdown` with a plain get() and only write it after every service had been processed, so two
    // concurrent callers could both pass the read before either write happened and both proceed to
    // shut every service down -- or a concurrent reload() could bootstrap replacement services while
    // another caller's shutdown() was still tearing the old ones down. The lock is re-entrant so
    // reload()'s own call into shutdown() on the same thread still works.
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    /**
     * Bootstraps the standard service graph from the configured file path, environment, or default
     * classpath configuration, in that order.
     *
     * @throws AppbaseRuntimeException if configuration decoding or service bootstrap fails
     */
    public DefaultAppbaseCore() {
        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultLuceneServiceConfiguration.class,
                DefaultLuceneServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultDisruptorServiceConfiguration.class,
                DefaultDisruptorServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultLogServiceConfiguration.class,
                DefaultLogServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultMetricServiceConfiguration.class,
                DefaultMetricServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultServiceRegistryConfiguration.class,
                DefaultServiceRegistryConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultExceptionServiceConfiguration.class,
                DefaultExceptionServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultTracerServiceConfiguration.class,
                DefaultTracerServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultReactorServiceConfiguration.class,
                DefaultReactorServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultDisruptorServiceConfiguration.class,
                DefaultDisruptorServiceConfiguration.DEFAULT_YAML_NODE_NAME);

        this.logServiceRef = new AtomicReference<>();
        this.loggerRef = new AtomicReference<>();
        this.metricServiceRef = new AtomicReference<>();
        this.validationServiceRef = new AtomicReference<>();
        this.mediatorServiceRef = new AtomicReference<>();
        this.configurationServiceRef = new AtomicReference<>();
        this.serviceRegistryRef = new AtomicReference<>();
        this.exceptionServiceRef = new AtomicReference<>();
        this.tracerServiceRef = new AtomicReference<>();
        this.reactorServiceRef = new AtomicReference<>();
        this.jmxServiceRef = new AtomicReference<>();
        this.luceneServiceRef = new AtomicReference<>();
        this.appbaseBootstrapConfigurationRef = new AtomicReference<>();
        this.bootstrappedServicesRef = new AtomicReference<>();
        this.guiceInjectorServiceRef = new AtomicReference<>();

        String configurationFilePath = AppbaseUtil.propertyOrEnv(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY,
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_ENV_KEY);
        if (configurationFilePath != null) {
            configurationFilePath = configurationFilePath.trim();
        } else {
            try {
                YamlUtil.readConfigurationNodes(AppbaseConstants.DEFAULT_CONFIGURATION_FILE);
                configurationFilePath = AppbaseConstants.DEFAULT_CONFIGURATION_FILE;
            } catch (IOException ioe) {
                // File does not exist, skip setting the configuration file path
            }

        }
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> localAppbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, configurationFilePath);

        final DefaultAppbaseCoreBootstrapConfiguration defaultAppbaseBootstrapConfiguration =
                readDefaultAppbaseBootstrapConfiguration();
        AppbaseUtil.notNull(defaultAppbaseBootstrapConfiguration, "defaultAppbaseBootstrapConfiguration");

        final AppbaseCoreBootstrappedServices localAppbaseCoreBootstrappedServices =
                localAppbaseCoreBootstrap.bootstrap(defaultAppbaseBootstrapConfiguration);
        AppbaseUtil.notNull(localAppbaseCoreBootstrappedServices, "localAppbaseCoreBootstrappedServices");
        this.appbaseCoreBootstrap = localAppbaseCoreBootstrap;
        setServices(localAppbaseCoreBootstrappedServices, defaultAppbaseBootstrapConfiguration);

        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Bootstraps the standard configuration with a caller-supplied bootstrap strategy.
     *
     * @param appbaseCoreBootstrap non-null strategy used to construct the service graph
     * @throws AppbaseRuntimeException if {@code appbaseCoreBootstrap} is {@code null}
     */
    public DefaultAppbaseCore(final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap) {
        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");

        this.logServiceRef = new AtomicReference<>();
        this.loggerRef = new AtomicReference<>();
        this.metricServiceRef = new AtomicReference<>();
        this.validationServiceRef = new AtomicReference<>();
        this.mediatorServiceRef = new AtomicReference<>();
        this.configurationServiceRef = new AtomicReference<>();
        this.serviceRegistryRef = new AtomicReference<>();
        this.exceptionServiceRef = new AtomicReference<>();
        this.tracerServiceRef = new AtomicReference<>();
        this.reactorServiceRef = new AtomicReference<>();
        this.jmxServiceRef = new AtomicReference<>();
        this.luceneServiceRef = new AtomicReference<>();
        this.appbaseBootstrapConfigurationRef = new AtomicReference<>();
        this.bootstrappedServicesRef = new AtomicReference<>();
        this.guiceInjectorServiceRef = new AtomicReference<>();

        final DefaultAppbaseCoreBootstrapConfiguration defaultAppbaseBootstrapConfiguration =
                readDefaultAppbaseBootstrapConfiguration();
        AppbaseUtil.notNull(defaultAppbaseBootstrapConfiguration, "defaultAppbaseBootstrapConfiguration");

        final AppbaseCoreBootstrappedServices localAppbaseCoreBootstrappedServices =
                appbaseCoreBootstrap.bootstrap(defaultAppbaseBootstrapConfiguration);
        AppbaseUtil.notNull(localAppbaseCoreBootstrappedServices, "localAppbaseCoreBootstrappedServices");

        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        setServices(localAppbaseCoreBootstrappedServices, defaultAppbaseBootstrapConfiguration);

        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Bootstraps a service graph from an explicit strategy and configuration.
     *
     * @param appbaseCoreBootstrap non-null strategy used to construct the service graph
     * @param appbaseCoreBootstrapConfiguration non-null configuration passed to the strategy
     * @throws AppbaseRuntimeException if either argument is {@code null}
     */
    public DefaultAppbaseCore(final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
                              final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");

        this.logServiceRef = new AtomicReference<>();
        this.loggerRef = new AtomicReference<>();
        this.metricServiceRef = new AtomicReference<>();
        this.validationServiceRef = new AtomicReference<>();
        this.mediatorServiceRef = new AtomicReference<>();
        this.configurationServiceRef = new AtomicReference<>();
        this.serviceRegistryRef = new AtomicReference<>();
        this.exceptionServiceRef = new AtomicReference<>();
        this.tracerServiceRef = new AtomicReference<>();
        this.reactorServiceRef = new AtomicReference<>();
        this.jmxServiceRef = new AtomicReference<>();
        this.luceneServiceRef = new AtomicReference<>();
        this.guiceInjectorServiceRef = new AtomicReference<>();
        this.appbaseBootstrapConfigurationRef = new AtomicReference<>();
        this.bootstrappedServicesRef = new AtomicReference<>();

        final AppbaseCoreBootstrappedServices localAppbaseCoreBootstrappedServices =
                appbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
        AppbaseUtil.notNull(localAppbaseCoreBootstrappedServices, "localAppbaseCoreBootstrappedServices");

        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        setServices(localAppbaseCoreBootstrappedServices, appbaseCoreBootstrapConfiguration);

        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Creates a facade over service instances supplied by the caller. This form is useful for embedding
     * Appbase or supplying test doubles; all arguments except {@code luceneService} are required.
     * The facade also runs a default bootstrap with a default bootstrap configuration and retains the
     * resulting aggregate, so {@link #getBootstrappedServices()} is non-null after this constructor.
     *
     * @param logService logging service
     * @param metricService metrics service
     * @param validationService validation service
     * @param mediatorService message-mediation service
     * @param configurationService configuration service
     * @param serviceRegistry service registry
     * @param exceptionService exception service
     * @param tracerService tracing service
     * @param reactorService reactive scheduling service
     * @param jmxService JMX management service
     * @param luceneService optional Lucene service, or {@code null} when indexing is disabled
     * @throws AppbaseRuntimeException if a required service is {@code null}
     */
    public DefaultAppbaseCore(final LogService logService,
                              final MetricService metricService,
                              final ValidationService validationService,
                              final MediatorService mediatorService,
                              final ConfigurationService configurationService,
                              final ServiceRegistry serviceRegistry,
                              final ExceptionService exceptionService,
                              final TracerService tracerService,
                              final ReactorService reactorService,
                              final JmxService jmxService,
                              @Nullable final LuceneService luceneService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(configurationService, "configurationService");
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");
        AppbaseUtil.notNull(exceptionService, "exceptionService");
        AppbaseUtil.notNull(tracerService, "tracerService");
        AppbaseUtil.notNull(reactorService, "reactorService");
        AppbaseUtil.notNull(jmxService, "jmxService");

        this.logServiceRef = new AtomicReference<>(logService);
        this.loggerRef = new AtomicReference<>(logService.getLogger(getClass()));
        this.metricServiceRef = new AtomicReference<>(metricService);
        this.validationServiceRef = new AtomicReference<>(validationService);
        this.mediatorServiceRef = new AtomicReference<>(mediatorService);
        this.configurationServiceRef = new AtomicReference<>(configurationService);
        this.serviceRegistryRef = new AtomicReference<>(serviceRegistry);
        this.exceptionServiceRef = new AtomicReference<>(exceptionService);
        this.tracerServiceRef = new AtomicReference<>(tracerService);
        this.reactorServiceRef = new AtomicReference<>(reactorService);
        this.jmxServiceRef = new AtomicReference<>(jmxService);
        this.luceneServiceRef = new AtomicReference<>(luceneService);
        this.guiceInjectorServiceRef = new AtomicReference<>();

        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                DefaultAppbaseCoreBootstrapConfiguration.builder().build();
        this.appbaseBootstrapConfigurationRef = new AtomicReference<>(appbaseCoreBootstrapConfiguration);

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> localAppbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                localAppbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
        this.bootstrappedServicesRef = new AtomicReference<>(appbaseCoreBootstrappedServices);
        this.appbaseCoreBootstrap = localAppbaseCoreBootstrap;

        this.shutdown = new AtomicBoolean(false);
    }

    /**
     * Injection entry point for an externally assembled service graph. A missing bootstrap
     * configuration selects the default configuration. A missing bootstrap strategy selects the default
     * strategy, which bootstraps immediately; a supplied strategy is only retained, so
     * {@link #getBootstrappedServices()} stays {@code null} until that strategy has produced an aggregate.
     *
     * @param appbaseCoreBootstrapConfiguration optional bootstrap configuration
     * @param appbaseCoreBootstrap optional bootstrap strategy
     * @param logService logging service
     * @param metricService metrics service
     * @param validationService validation service
     * @param mediatorService message-mediation service
     * @param configurationService configuration service
     * @param serviceRegistry service registry
     * @param exceptionService exception service
     * @param tracerService tracing service
     * @param reactorService reactive scheduling service
     * @param jmxService JMX management service
     * @param guiceInjectorService injector service
     * @param luceneService optional Lucene service
     * @throws AppbaseRuntimeException if a required service is {@code null}
     */
    @Inject
    public DefaultAppbaseCore(
            @Nullable final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration,
            @Nullable final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
            final LogService logService,
            final MetricService metricService,
            final ValidationService validationService,
            final MediatorService mediatorService,
            final ConfigurationService configurationService,
            final ServiceRegistry serviceRegistry,
            final ExceptionService exceptionService,
            final TracerService tracerService,
            final ReactorService reactorService,
            final JmxService jmxService,
            final GuiceInjectorService guiceInjectorService,
            @Nullable final LuceneService luceneService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(configurationService, "configurationService");
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");
        AppbaseUtil.notNull(exceptionService, "exceptionService");
        AppbaseUtil.notNull(tracerService, "tracerService");
        AppbaseUtil.notNull(reactorService, "reactorService");
        AppbaseUtil.notNull(jmxService, "jmxService");
        AppbaseUtil.notNull(guiceInjectorService, "guiceInjectorService");

        this.logServiceRef = new AtomicReference<>(logService);
        this.loggerRef = new AtomicReference<>(logService.getLogger(getClass()));
        this.metricServiceRef = new AtomicReference<>(metricService);
        this.validationServiceRef = new AtomicReference<>(validationService);
        this.mediatorServiceRef = new AtomicReference<>(mediatorService);
        this.configurationServiceRef = new AtomicReference<>(configurationService);
        this.serviceRegistryRef = new AtomicReference<>(serviceRegistry);
        this.exceptionServiceRef = new AtomicReference<>(exceptionService);
        this.tracerServiceRef = new AtomicReference<>(tracerService);
        this.reactorServiceRef = new AtomicReference<>(reactorService);
        this.jmxServiceRef = new AtomicReference<>(jmxService);
        this.guiceInjectorServiceRef = new AtomicReference<>(guiceInjectorService);
        this.luceneServiceRef = new AtomicReference<>(luceneService);

        if (appbaseCoreBootstrapConfiguration != null) {
            this.appbaseBootstrapConfigurationRef = new AtomicReference<>(appbaseCoreBootstrapConfiguration);
        } else {
            final AppbaseCoreBootstrapConfiguration localAppbaseCoreBootstrapConfiguration =
                    DefaultAppbaseCoreBootstrapConfiguration.builder().build();
            this.appbaseBootstrapConfigurationRef = new AtomicReference<>(localAppbaseCoreBootstrapConfiguration);
        }

        if (appbaseCoreBootstrap != null) {
            this.appbaseCoreBootstrap = appbaseCoreBootstrap;
            this.bootstrappedServicesRef = new AtomicReference<>(null);
        } else {
            final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> localAppbaseCoreBootstrap =
                    new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                    localAppbaseCoreBootstrap.bootstrap(this.appbaseBootstrapConfigurationRef.get());
            this.bootstrappedServicesRef = new AtomicReference<>(appbaseCoreBootstrappedServices);
            this.appbaseCoreBootstrap = localAppbaseCoreBootstrap;
        }

        this.shutdown = new AtomicBoolean(false);
    }

    private void setServices(final AppbaseCoreBootstrappedServices localAppbaseCoreBootstrappedServices,
                             final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        final LogService logService = localAppbaseCoreBootstrappedServices.getLogService();
        this.logServiceRef.set(logService);
        this.loggerRef.set(logService.getLogger(getClass()));
        this.metricServiceRef.set(localAppbaseCoreBootstrappedServices.getMetricService());
        this.validationServiceRef.set(localAppbaseCoreBootstrappedServices.getValidationService());
        this.mediatorServiceRef.set(localAppbaseCoreBootstrappedServices.getMediatorService());
        this.configurationServiceRef.set(localAppbaseCoreBootstrappedServices.getConfigurationService());
        this.serviceRegistryRef.set(localAppbaseCoreBootstrappedServices.getServiceRegistry());
        this.exceptionServiceRef.set(localAppbaseCoreBootstrappedServices.getExceptionService());
        this.tracerServiceRef.set(localAppbaseCoreBootstrappedServices.getTracerService());
        this.reactorServiceRef.set(localAppbaseCoreBootstrappedServices.getReactorService());
        this.jmxServiceRef.set(localAppbaseCoreBootstrappedServices.getJmxService());
        this.luceneServiceRef.set(localAppbaseCoreBootstrappedServices.getLuceneService());
        this.appbaseBootstrapConfigurationRef.set(appbaseCoreBootstrapConfiguration);
        this.bootstrappedServicesRef.set(localAppbaseCoreBootstrappedServices);
    }

    @Override
    public LogService getLogService() {
        return logServiceRef.get();
    }

    @Override
    public MetricService getMetricService() {
        return metricServiceRef.get();
    }

    @Override
    public ValidationService getValidationService() {
        return validationServiceRef.get();
    }

    @Override
    public MediatorService getMediatorService() {
        return mediatorServiceRef.get();
    }

    @Override
    public ConfigurationService getConfigurationService() {
        return configurationServiceRef.get();
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistryRef.get();
    }

    @Override
    public ExceptionService getExceptionService() {
        return exceptionServiceRef.get();
    }

    @Override
    public TracerService getTracerService() {
        return tracerServiceRef.get();
    }

    @Override
    public ReactorService getReactorService() {
        return reactorServiceRef.get();
    }

    @Override
    public JmxService getJmxService() {
        return jmxServiceRef.get();
    }

    @Nullable
    @Override
    public LuceneService getLuceneService() {
        return luceneServiceRef.get();
    }

    @Nullable
    @Override
    public GuiceInjectorService getGuiceInjectorService() {
        return guiceInjectorServiceRef.get();
    }

    @Override
    public AppbaseCoreBootstrapConfiguration getAppbaseBootstrapConfiguration() {
        return appbaseBootstrapConfigurationRef.get();
    }

    /**
     * Exposes the strategy used for initial bootstrap and subsequent reloads.
     * @return the non-null bootstrap strategy retained by this facade
     */
    public AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> getAppbaseCoreBootstrap() {
        return appbaseCoreBootstrap;
    }

    /**
     * Returns the aggregate produced by the most recent bootstrap when this instance owns one.
     * An injected bootstrap strategy may defer construction, in which case no aggregate is available.
     * @return the last bootstrapped service aggregate, or {@code null} when an externally supplied
     *         bootstrap strategy has not produced one
     */
    @Nullable
    public AppbaseCoreBootstrappedServices getBootstrappedServices() {
        return bootstrappedServicesRef.get();
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
        lifecycleLock.lock();
        try {
            loggerRef.get().info("Reloading {}", getClass().getName());

            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                    appbaseBootstrapConfigurationRef.get();
            shutdown();

            final AppbaseCoreBootstrappedServices localBootstrapServices =
                    appbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
            setServices(localBootstrapServices, appbaseCoreBootstrapConfiguration);
            shutdown.set(false);
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public void reload(final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration) {
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");

        lifecycleLock.lock();
        try {
            loggerRef.get().info("Reloading {} using {}", getClass().getName(), appbaseCoreBootstrapConfiguration);
            shutdown();
            final AppbaseCoreBootstrappedServices localBootstrapServices =
                    appbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
            setServices(localBootstrapServices, appbaseCoreBootstrapConfiguration);
            this.appbaseBootstrapConfigurationRef.set(appbaseCoreBootstrapConfiguration);
            shutdown.set(false);
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        lifecycleLock.lock();
        try {
            // The read-then-act check must happen under the same lock that guards the write below
            // (Lens 3 L3-01); reading it unlocked is a check-then-act race against a concurrent caller.
            if (shutdown.get()) {
                return;
            }

            final Logger logger = loggerRef.get();
            logger.info("Shutting down {}", getClass().getName());

            final long timeoutMs = appbaseBootstrapConfigurationRef.get().getServiceShutdownTimeoutMs();
            // Clear any caller-set interrupt flag so all services are attempted even if the
            // shutdown hook thread was interrupted; restored after all services are processed.
            final boolean callerWasInterrupted = Thread.interrupted();
            boolean shutdownClean = true;

            final LuceneService luceneService = luceneServiceRef.get();
            if (luceneService != null) {
                shutdownClean &= shutdownWithTimeout(
                        luceneService::shutdown, luceneService.getClass().getName(), timeoutMs, logger);
            }

            final JmxService jmxService = jmxServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    jmxService::shutdown, jmxService.getClass().getName(), timeoutMs, logger);

            final ReactorService reactorService = reactorServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    reactorService::shutdown, reactorService.getClass().getName(), timeoutMs, logger);

            final TracerService tracerService = tracerServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    tracerService::shutdown, tracerService.getClass().getName(), timeoutMs, logger);

            final ExceptionService exceptionService = exceptionServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    exceptionService::shutdown, exceptionService.getClass().getName(), timeoutMs, logger);

            final ServiceRegistry serviceRegistry = serviceRegistryRef.get();
            shutdownClean &= shutdownWithTimeout(
                    serviceRegistry::shutdown, serviceRegistry.getClass().getName(), timeoutMs, logger);

            final ConfigurationService configurationService = configurationServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    configurationService::shutdown, configurationService.getClass().getName(), timeoutMs, logger);

            final MediatorService mediatorService = mediatorServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    mediatorService::shutdown, mediatorService.getClass().getName(), timeoutMs, logger);

            final ValidationService validationService = validationServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    validationService::shutdown, validationService.getClass().getName(), timeoutMs, logger);

            final MetricService metricService = metricServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    metricService::shutdown, metricService.getClass().getName(), timeoutMs, logger);

            final LogService logService = logServiceRef.get();
            shutdownClean &= shutdownWithTimeout(
                    logService::shutdown, logService.getClass().getName(), timeoutMs, logger);

            if (!shutdownClean) {
                logShutdownFailure(logger,
                        "Shutdown of {} was not clean — one or more services failed or timed out",
                        getClass().getName());
            }

            if (callerWasInterrupted) {
                Thread.currentThread().interrupt();
            }

            shutdown.set(true);
        } finally {
            lifecycleLock.unlock();
        }
    }

    boolean shutdownWithTimeout(final Runnable shutdownAction,
                                final String serviceName,
                                final long timeoutMs,
                                final Logger logger)
    {
        // Daemon thread so a hung service does not prevent JVM exit.
        final ExecutorService executor = Executors.newSingleThreadExecutor(
                r -> {
                    final Thread t = new Thread(r, "appbase-shutdown-" + serviceName);
                    t.setDaemon(true);
                    return t;
                });
        try {
            executor.submit(shutdownAction).get(timeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException timeoutException) {
            logShutdownFailure(logger, "Timeout shutting down {} after {}ms", serviceName, timeoutMs);
            return false;
        } catch (ExecutionException executionException) {
            logShutdownFailure(logger, "Error shutting down {}", serviceName, executionException.getCause());
            return false;
        } catch (InterruptedException interruptedException) {
            // Do not restore the interrupt flag here — shutdown() clears it before the
            // sequence and restores it afterward so all services are attempted regardless.
            logShutdownFailure(logger, "Interrupted while shutting down {}", serviceName);
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    private void logShutdownFailure(final Logger logger, final String message, final Object... arguments) {
        try {
            logger.error(message, arguments);
        } catch (RejectedExecutionException ignored) {
            // A mediator-backed appender can reject after the mediator's executor has terminated.
        }
    }

    private DefaultAppbaseCoreBootstrapConfiguration readDefaultAppbaseBootstrapConfiguration() {
        DefaultAppbaseCoreBootstrapConfiguration defaultAppbaseBootstrapConfiguration = null;

        // Get configuration file path ...
        String configurationFilePath = AppbaseUtil.propertyOrEnv(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY,
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_ENV_KEY);
        if (configurationFilePath != null) {
            configurationFilePath = configurationFilePath.trim();
        }

        if (!AppbaseUtil.isBlank(configurationFilePath)) {
            final Class<? extends DefaultAppbaseCoreBootstrapConfiguration> configurationType =
                    DefaultAppbaseCoreBootstrapConfiguration.class;

            try {
                defaultAppbaseBootstrapConfiguration =
                        YamlUtil.readConfigurationFromFile(
                                configurationFilePath,
                                AppbaseConstants.APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME_PROPERTY_KEY,
                                AppbaseConstants.APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME_ENV_KEY,
                                null,
                                configurationType);
            } catch (Exception ex) {
                // Fail closed (Lens 3 L3-03): an operator-selected configuration file that cannot
                // be loaded must refuse startup, never silently fall back to defaults.
                throw new AppbaseBootstrapConfigurationException(
                        "Could not load appbase configuration file " + configurationFilePath, ex);
            }
            if (defaultAppbaseBootstrapConfiguration == null) {
                throw new AppbaseBootstrapConfigurationException(
                        "Appbase configuration file " + configurationFilePath +
                                " contained no bootstrap configuration");
            }
        }

        if (defaultAppbaseBootstrapConfiguration == null) {
            // No operator-selected path: defaults are the intended configuration, not a fallback.
            defaultAppbaseBootstrapConfiguration = DefaultAppbaseCoreBootstrapConfiguration.builder().build();
        }

        return defaultAppbaseBootstrapConfiguration;
    }
}
