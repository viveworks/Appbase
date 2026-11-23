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

package com.viveworks.appbase.bootstrap.core;

import ch.qos.logback.classic.LoggerContext;

import com.viveworks.appbase.bootstrap.core.factory.ConfigurationServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.ExceptionServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.JmxServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.LogServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.LuceneServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.MediatorServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.MetricServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.MicroprofileConfigurationServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.MicroprofileServiceRegistryFactory;
import com.viveworks.appbase.bootstrap.core.factory.ReactorServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.ServiceRegistryFactory;
import com.viveworks.appbase.bootstrap.core.factory.TracerServiceFactory;
import com.viveworks.appbase.bootstrap.core.factory.ValidationServiceFactory;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Immutable container holding all initialized Appbase core services.
 *
 * <p>Build via the fluent builder:
 * <pre>{@code
 * // From YAML file (classpath or filesystem path):
 * AppbaseCoreServices core = AppbaseCoreServices.builder()
 *     .fromYaml("appbase-config.yaml")
 *     .build();
 *
 * // From programmatic config:
 * AppbaseCoreServicesConfiguration cfg = AppbaseCoreServicesConfiguration.builder()
 *     .logServiceConfiguration(...)
 *     .build();
 * AppbaseCoreServices core = AppbaseCoreServices.builder().from(cfg).build();
 *
 * // Override one factory while keeping the rest from config:
 * AppbaseCoreServices core = AppbaseCoreServices.builder()
 *     .fromYaml("appbase-config.yaml")
 *     .with(new LogServiceFactory(myCustomLog))
 *     .build();
 * }</pre>
 *
 * <p>Eleven services are mandatory; Reactor and Lucene are optional. Pass their factories
 * via {@link Builder#with} or include their config in {@link AppbaseCoreServicesConfiguration}
 * to enable them.
 */
@Immutable
public final class AppbaseCoreServices implements Shutdownable {

    private static final List<Class<? extends AppbaseService>> MANDATORY_TYPES = List.of(
        LogService.class,
        MetricService.class,
        ValidationService.class,
        MediatorService.class,
        ConfigurationService.class,
        MicroprofileConfigurationService.class,
        ServiceRegistry.class,
        MicroprofileServiceRegistry.class,
        ExceptionService.class,
        TracerService.class,
        JmxService.class
    );

    private final Map<Class<?>, Object> services;
    private final List<Object> initOrder;
    private final AppbaseCore appbaseCore;
    private final boolean registerDefaultExceptionStores;
    private volatile boolean shutdown = false;

    @SuppressWarnings("checkstyle:FinalParameters")
    private AppbaseCoreServices(Map<Class<?>, Object> services,
                                List<Object> initOrder,
                                boolean registerDefaultExceptionStores) {
        this.services                    = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        this.initOrder                   = List.copyOf(initOrder);
        this.appbaseCore                 = new CoreAdapter(this);
        this.registerDefaultExceptionStores = registerDefaultExceptionStores;
    }

    // ── Mandatory typed getters ────────────────────────────────────────────────

    /** Returns the log service. */
    public LogService getLogService() { return get(LogService.class); }

    /** Returns the metric service. */
    public MetricService getMetricService() { return get(MetricService.class); }

    /** Returns the validation service. */
    public ValidationService getValidationService() { return get(ValidationService.class); }

    /** Returns the mediator service. */
    public MediatorService getMediatorService() { return get(MediatorService.class); }

    /** Returns the configuration service. */
    public ConfigurationService getConfigurationService() { return get(ConfigurationService.class); }

    /** Returns the MicroProfile configuration service. */
    public MicroprofileConfigurationService getMicroprofileConfigurationService() {
        return get(MicroprofileConfigurationService.class);
    }

    /** Returns the service registry. */
    public ServiceRegistry getServiceRegistry() { return get(ServiceRegistry.class); }

    /** Returns the MicroProfile service registry. */
    public MicroprofileServiceRegistry getMicroprofileServiceRegistry() {
        return get(MicroprofileServiceRegistry.class);
    }

    /** Returns the exception service. */
    public ExceptionService getExceptionService() { return get(ExceptionService.class); }

    /** Returns the tracer service. */
    public TracerService getTracerService() { return get(TracerService.class); }

    /** Returns the JMX service. */
    public JmxService getJmxService() { return get(JmxService.class); }

    /**
     * Re-registers the default {@link InMemoryExceptionStore} on the exception service.
     * Has no effect if {@link Builder#withDefaultExceptionStores()} was not called during build.
     *
     * <p>Call this after {@link com.viveworks.appbase.core.service.exception.ExceptionService#shutdown()}
     * to restore stores before a subsequent reload — the exception service clears all store
     * registrations on shutdown.
     */
    public void ensureDefaultExceptionStores() {
        if (registerDefaultExceptionStores) {
            getExceptionService()
                .getExceptionAccessor()
                .registerExceptionStore(
                    new InMemoryExceptionStore(InMemoryExceptionStoreConfiguration.builder().build()));
        }
    }

    // ── Optional typed getters ─────────────────────────────────────────────────

    /**
     * Returns the reactor service, or {@code null} if not configured.
     *
     * @return reactor service or null
     */
    @Nullable
    public ReactorService getReactorService() {
        return (ReactorService) services.get(ReactorService.class);
    }

    /**
     * Returns the Lucene service, or {@code null} if not configured.
     *
     * @return Lucene service or null
     */
    @Nullable
    public LuceneService getLuceneService() {
        return (LuceneService) services.get(LuceneService.class);
    }

    // ── Generic typed access ───────────────────────────────────────────────────

    /**
     * Returns the service registered under {@code serviceType}.
     *
     * @param serviceType the service interface class
     * @param <T> the service type
     * @return the registered service instance
     * @throws AppbaseRuntimeException if no service of that type is registered
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceType) {
        Object svc = services.get(Objects.requireNonNull(serviceType, "serviceType"));
        if (svc == null) {
            throw new AppbaseRuntimeException(
                "No core service registered for: " + serviceType.getName()
                + ". Provide it via AppbaseCoreServicesConfiguration or with(AppbaseCoreServiceFactory).");
        }
        return (T) svc;
    }

    /**
     * Returns an unmodifiable map of all registered core services, keyed by service interface.
     *
     * @return unmodifiable service map
     */
    public Map<Class<?>, Object> services() {
        return services;
    }

    /**
     * Returns an {@link AppbaseCore} view of this container for internal bootstrapping use.
     *
     * <p>The returned object delegates each getter to the corresponding typed getter on this
     * container (for example, {@link #getLogService()} rather than
     * {@code appbaseCore().getLogService()}). Prefer the direct typed getters in application
     * code; this method is intended for bootstrapping contexts where an {@link AppbaseCore}
     * reference is required (e.g., constructing a
     * {@link com.viveworks.appbase.client.core.DefaultAppbaseClientCore}).
     *
     * @return an {@link AppbaseCore} holder backed by this container
     */
    public AppbaseCore appbaseCore() {
        return appbaseCore;
    }

    /**
     * Returns {@code true} if {@link #shutdown()} has been called.
     *
     * @return true after shutdown
     */
    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Shuts down all services in reverse initialization order.
     * Errors are logged via SLF4J and NOT rethrown.
     */
    @Override
    public void shutdown() {
        shutdown = true;
        List<Object> reversed = new ArrayList<>(initOrder);
        Collections.reverse(reversed);
        for (Object svc : reversed) {
            if (!(svc instanceof Shutdownable)) {
                continue;
            }
            try {
                ((Shutdownable) svc).shutdown();
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AppbaseCoreServices.class)
                    .error("Error shutting down core service {}", svc.getClass().getName(), e);
            }
        }
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    /**
     * Builder for {@link AppbaseCoreServices}.
     *
     * <p>Call {@link #from} or {@link #fromYaml} to load built-in factories from config,
     * then optionally call {@link #with} to override or add custom factories.
     * If only {@link #with} is called (no config), all 11 mandatory factory types must be supplied.
     */
    public static final class Builder {

        @Nullable
        private AppbaseCoreServicesConfiguration config;
        private final List<AppbaseCoreServiceFactory<?>> customFactories = new ArrayList<>();
        private boolean registerDefaultExceptionStores = false;

        /**
         * Configures core services from the given configuration object.
         * Built-in factories are constructed for each sub-config; call {@link #with} to override any.
         *
         * @param cfg the user-facing configuration (must not be null)
         * @return this builder
         */
        public Builder from(AppbaseCoreServicesConfiguration cfg) {
            this.config = Objects.requireNonNull(cfg, "config");
            return this;
        }

        /**
         * Loads core services configuration from a YAML file.
         * Resolved first as a classpath resource, then as an absolute filesystem path.
         * The YAML must have a top-level {@value AppbaseCoreServicesConfiguration#DEFAULT_YAML_NODE_NAME} node.
         *
         * @param classpathOrFilePath classpath resource path or absolute file path
         * @return this builder
         * @throws AppbaseRuntimeException if the file cannot be read or parsed
         */
        public Builder fromYaml(String classpathOrFilePath) {
            Objects.requireNonNull(classpathOrFilePath, "classpathOrFilePath");
            // Register all core Jackson subtypes before YAML parsing so polymorphic configs deserialize correctly
            AppbaseCoreObjectMapperSubTypes.register();
            try {
                this.config = YamlUtil.readConfigurationFromFile(
                    classpathOrFilePath,
                    null,
                    null,
                    AppbaseCoreServicesConfiguration.DEFAULT_YAML_NODE_NAME,
                    AppbaseCoreServicesConfiguration.class);
            } catch (Exception e) {
                throw new AppbaseRuntimeException(
                    "Failed to load core services configuration from: " + classpathOrFilePath, e);
            }
            return this;
        }

        /**
         * Registers a default {@link InMemoryExceptionStore} with the built exception service.
         * This makes the exception service immediately usable for storing exceptions without
         * requiring callers to register a store manually.
         *
         * @return this builder
         */
        public Builder withDefaultExceptionStores() {
            this.registerDefaultExceptionStores = true;
            return this;
        }

        /**
         * Registers custom service factories.
         * A factory whose {@link AppbaseCoreServiceFactory#serviceType()} matches a built-in replaces it.
         * Calling {@code with()} multiple times accumulates factories; last one per type wins.
         *
         * @param factories one or more custom factories
         * @return this builder
         */
        public Builder with(AppbaseCoreServiceFactory<?>... factories) {
            Objects.requireNonNull(factories, "factories");
            for (AppbaseCoreServiceFactory<?> f : factories) {
                customFactories.add(Objects.requireNonNull(f, "factory element must not be null"));
            }
            return this;
        }

        /**
         * Builds the {@link AppbaseCoreServices} container by running all factories in
         * initialization order and validating that all 11 mandatory service types are present.
         *
         * @return the fully initialized, immutable container
         * @throws AppbaseRuntimeException if a mandatory service is missing or a factory throws
         */
        public AppbaseCoreServices build() {
            // 1. Seed with built-in factories from config (if provided)
            Map<Class<?>, AppbaseCoreServiceFactory<?>> factoryMap = new LinkedHashMap<>();
            if (config != null) {
                registerBuiltinFactories(factoryMap, config);
            }
            // 2. Custom factories override built-ins by serviceType()
            for (AppbaseCoreServiceFactory<?> f : customFactories) {
                factoryMap.put(f.serviceType(), f);
            }
            // 3. Sort by initializationOrder()
            List<AppbaseCoreServiceFactory<?>> sorted = new ArrayList<>(factoryMap.values());
            sorted.sort(Comparator.comparingInt(AppbaseCoreServiceFactory::initializationOrder));

            // 4. Run each factory; pass growing partial container so later factories see earlier deps
            Map<Class<?>, Object> built   = new LinkedHashMap<>();
            List<Object>          initOrd = new ArrayList<>();
            AppbaseCoreServices partial = new AppbaseCoreServices(built, initOrd, false);
            for (AppbaseCoreServiceFactory<?> factory : sorted) {
                try {
                    factory.registerConfiguration(partial);
                    Object svc = factory.create(partial);
                    built.put(factory.serviceType(), svc);
                    initOrd.add(svc);
                    partial = new AppbaseCoreServices(new LinkedHashMap<>(built), new ArrayList<>(initOrd), false);
                } catch (Exception e) {
                    throw new AppbaseRuntimeException(
                        "Failed to create core service: " + factory.serviceType().getName(), e);
                }
            }

            // 5. Validate all 11 mandatory services are present
            for (Class<? extends AppbaseService> mandatory : MANDATORY_TYPES) {
                if (!built.containsKey(mandatory)) {
                    throw new AppbaseRuntimeException(
                        "Missing mandatory core service: " + mandatory.getName()
                        + ". Provide it via AppbaseCoreServicesConfiguration or with(AppbaseCoreServiceFactory).");
                }
            }

            AppbaseCoreServices result = new AppbaseCoreServices(built, initOrd, registerDefaultExceptionStores);

            // DefaultLogService is constructed first so later factories can depend on it, but its
            // accessor and appenders require services that do not exist until the factory pass is complete.
            if (factoryMap.get(LogService.class) instanceof LogServiceFactory &&
                    result.getLogService() instanceof DefaultLogService)
            {
                final DefaultLogService defaultLogService = (DefaultLogService) result.getLogService();
                defaultLogService.setMetricService(result.getMetricService());
                defaultLogService.setValidationService(result.getValidationService());
                defaultLogService.setMediatorService(result.getMediatorService());
                final LuceneService luceneService = result.getLuceneService();
                if (luceneService != null) {
                    defaultLogService.setLuceneService(luceneService);
                }
                try {
                    defaultLogService.initFiltersAndAppenders(
                            (LoggerContext) LoggerFactory.getILoggerFactory());
                } catch (final Exception e) {
                    throw new AppbaseRuntimeException("Failed to finish initializing default log service", e);
                }
            }

            // 6. Register default exception store if requested
            if (registerDefaultExceptionStores) {
                result.getExceptionService()
                    .getExceptionAccessor()
                    .registerExceptionStore(
                        new InMemoryExceptionStore(InMemoryExceptionStoreConfiguration.builder().build()));
            }

            return result;
        }

        private static void registerBuiltinFactories(Map<Class<?>, AppbaseCoreServiceFactory<?>> map,
                                                     AppbaseCoreServicesConfiguration cfg) {
            LogServiceFactory logFactory = new LogServiceFactory(cfg.getLogServiceConfiguration());
            put(map, logFactory);
            put(map, new MetricServiceFactory(cfg.getMetricServiceConfiguration()));
            put(map, new ValidationServiceFactory(cfg.getValidationServiceConfiguration()));
            put(map, new MediatorServiceFactory(logFactory, cfg.getMediatorServiceConfiguration()));
            put(map, new ConfigurationServiceFactory(cfg.getConfigurationServiceConfiguration()));
            put(map, new MicroprofileConfigurationServiceFactory());
            put(map, new ServiceRegistryFactory(cfg.getServiceRegistryConfiguration()));
            put(map, new MicroprofileServiceRegistryFactory());
            put(map, new ExceptionServiceFactory(cfg.getExceptionServiceConfiguration()));
            put(map, new TracerServiceFactory(cfg.getTracerServiceConfiguration()));
            put(map, new JmxServiceFactory(cfg.getJmxServiceConfiguration()));
            if (cfg.getReactorServiceConfiguration() != null) {
                put(map, new ReactorServiceFactory(cfg.getReactorServiceConfiguration()));
            }
            if (cfg.getLuceneServiceConfiguration() != null) {
                put(map, new LuceneServiceFactory(cfg.getLuceneServiceConfiguration()));
            }
        }

        private static void put(Map<Class<?>, AppbaseCoreServiceFactory<?>> map,
                                AppbaseCoreServiceFactory<?> factory) {
            map.put(factory.serviceType(), factory);
        }
    }

    // ── CoreAdapter ───────────────────────────────────────────────────────────

    /**
     * Private {@link AppbaseCore} adapter backed by an {@link AppbaseCoreServices} container.
     * Lifecycle methods are no-ops; Guice and bootstrap-configuration methods return {@code null}.
     */
    private static final class CoreAdapter implements AppbaseCore {

        private final AppbaseCoreServices c;

        CoreAdapter(final AppbaseCoreServices c) {
            this.c = c;
        }

        @Override public com.viveworks.appbase.core.service.log.LogService getLogService()                         { return c.getLogService(); }
        @Override public com.viveworks.appbase.core.service.metric.MetricService getMetricService()               { return c.getMetricService(); }
        @Override public com.viveworks.appbase.core.service.validation.ValidationService getValidationService()   { return c.getValidationService(); }
        @Override public com.viveworks.appbase.core.service.mediator.MediatorService getMediatorService()         { return c.getMediatorService(); }
        @Override public com.viveworks.appbase.core.service.configuration.ConfigurationService getConfigurationService() { return c.getConfigurationService(); }
        @Override public com.viveworks.appbase.core.service.registry.ServiceRegistry getServiceRegistry()         { return c.getServiceRegistry(); }
        @Override public com.viveworks.appbase.core.service.exception.ExceptionService getExceptionService()      { return c.getExceptionService(); }
        @Override public com.viveworks.appbase.core.service.tracer.TracerService getTracerService()               { return c.getTracerService(); }
        @Override public com.viveworks.appbase.core.service.reactive.reactor.ReactorService getReactorService()   { return c.getReactorService(); }
        @Override public com.viveworks.appbase.core.service.jmx.JmxService getJmxService()                       { return c.getJmxService(); }
        @Override public com.viveworks.appbase.core.util.lucene.LuceneService getLuceneService()                  { return c.getLuceneService(); }

        /** Returns {@code null} — Guice is not used in the builder bootstrap path. */
        @javax.annotation.Nullable
        @Override public GuiceInjectorService getGuiceInjectorService() { return null; }

        /** Returns {@code null} — no internal bootstrap configuration in the builder path. */
        @javax.annotation.Nullable
        @Override public AppbaseCoreBootstrapConfiguration getAppbaseBootstrapConfiguration() { return null; }

        /** Returns {@link ServiceStatus#STARTED} — the container is always fully initialized. */
        @Override public ServiceStatus getStatus() { return ServiceStatus.STARTED; }

        /** No-op. Lifecycle is managed by {@link AppbaseCoreServices}. */
        @Override public void reload() { /* no-op */ }

        /** No-op. Lifecycle is managed by {@link AppbaseCoreServices}. */
        @Override public void reload(final AppbaseCoreBootstrapConfiguration cfg) { /* no-op */ }

        /** Returns {@code false} — the adapter does not own the lifecycle. */
        @Override public boolean isShutdown() { return false; }

        /** No-op. Call {@link AppbaseCoreServices#shutdown()} to shut down the container. */
        @Override public void shutdown() { /* no-op */ }
    }
}
