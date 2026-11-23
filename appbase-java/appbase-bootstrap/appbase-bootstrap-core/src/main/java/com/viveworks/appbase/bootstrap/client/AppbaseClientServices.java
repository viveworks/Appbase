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

package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCoreConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.Shutdownable;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable container holding all initialized Appbase client services.
 *
 * <p>Build via the fluent builder:
 * <pre>{@code
 * AppbaseClientServices clients = AppbaseClientServices.builder()
 *     .from(coreServices)
 *     .with(new AerospikeClientServiceFactory(config))
 *     .build();
 *
 * AerospikeClientService aerospike = clients.get(AerospikeClientService.class);
 * }</pre>
 *
 * <p>Unlike {@link AppbaseCoreServices}, there are no mandatory client services — the container
 * may be empty. Register client services by passing their factories to {@link Builder#with}.
 *
 * <p>The builder creates a single {@link AppbaseClientCore} before running factories. All factories
 * share the same instance, passed as the second argument to
 * {@link AppbaseClientServiceFactory#create}. Supply a custom instance via
 * {@link Builder#withAppbaseClientCore} or a custom factory via
 * {@link Builder#withAppbaseClientCoreFactory}; otherwise a {@link DefaultAppbaseClientCore} is
 * created automatically using {@link AppbaseCoreServices#appbaseCore()}.
 */
@Immutable
public final class AppbaseClientServices implements Shutdownable {

    private final AppbaseCoreServices coreServices;
    private final AppbaseClientCore appbaseClientCore;
    private final Map<Class<?>, Object> services;
    private final List<Object> initOrder;
    private volatile boolean shutdown = false;

    private AppbaseClientServices(AppbaseCoreServices coreServices,
                                   AppbaseClientCore appbaseClientCore,
                                   Map<Class<?>, Object> services,
                                   List<Object> initOrder) {
        this.coreServices      = coreServices;
        this.appbaseClientCore = appbaseClientCore;
        this.services          = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        this.initOrder         = List.copyOf(initOrder);
    }

    // ── Typed access ──────────────────────────────────────────────────────────

    /**
     * Returns the client service registered under {@code serviceType}.
     *
     * @param serviceType the client service interface class
     * @param <T>         the service type
     * @return the registered service instance
     * @throws AppbaseRuntimeException if no client service of that type is registered
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceType) {
        Object svc = services.get(Objects.requireNonNull(serviceType, "serviceType"));
        if (svc == null) {
            throw new AppbaseRuntimeException(
                "No client service registered for: " + serviceType.getName()
                + ". Provide it via AppbaseClientServices.builder().with(factory).");
        }
        return (T) svc;
    }

    /**
     * Returns an unmodifiable map of all registered client services, keyed by service interface.
     *
     * @return unmodifiable service map (may be empty)
     */
    public Map<Class<?>, Object> services() {
        return services;
    }

    /**
     * Returns the core services container that was used to build this client container.
     *
     * @return core services (never null)
     */
    public AppbaseCoreServices getCoreServices() {
        return coreServices;
    }

    /**
     * Returns the shared {@link AppbaseClientCore} used by all client service factories.
     *
     * @return the client core (never null)
     */
    public AppbaseClientCore getAppbaseClientCore() {
        return appbaseClientCore;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Shuts down all client services in reverse initialization order, then shuts down the
     * shared {@link AppbaseClientCore}.
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
                org.slf4j.LoggerFactory.getLogger(AppbaseClientServices.class)
                    .error("Error shutting down client service {}", svc.getClass().getName(), e);
            }
        }
        try {
            appbaseClientCore.shutdown();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AppbaseClientServices.class)
                .error("Error shutting down AppbaseClientCore {}", appbaseClientCore.getClass().getName(), e);
        }
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Builder for {@link AppbaseClientServices}.
     *
     * <p>Call {@link #from} to supply the fully-initialized core services, then {@link #with}
     * to register client service factories. {@link #build} runs factories in
     * {@link AppbaseClientServiceFactory#initializationOrder()} order.
     */
    public static final class Builder {

        @Nullable
        private AppbaseCoreServices coreServices;
        @Nullable
        private AppbaseClientCore predefinedClientCore;
        @Nullable
        private AppbaseClientCoreFactory clientCoreFactory;
        private final List<AppbaseClientServiceFactory<?>> customFactories = new ArrayList<>();

        /**
         * Supplies the core services container used to build the client core and passed to each
         * factory's {@code create} method.
         *
         * @param core the fully-built core services container (must not be null)
         * @return this builder
         */
        public Builder from(AppbaseCoreServices core) {
            this.coreServices = Objects.requireNonNull(core, "core");
            return this;
        }

        /**
         * Supplies a pre-built {@link AppbaseClientCore} to be shared across all factories.
         * Takes precedence over {@link #withAppbaseClientCoreFactory}.
         *
         * @param appbaseClientCore the client core instance (must not be null)
         * @return this builder
         */
        public Builder withAppbaseClientCore(AppbaseClientCore appbaseClientCore) {
            this.predefinedClientCore = Objects.requireNonNull(appbaseClientCore, "appbaseClientCore");
            return this;
        }

        /**
         * Supplies a factory used to create the {@link AppbaseClientCore}.
         * Ignored if {@link #withAppbaseClientCore} was also called.
         * If neither is set, a {@link DefaultAppbaseClientCore} is created automatically.
         *
         * @param factory the client core factory (must not be null)
         * @return this builder
         */
        public Builder withAppbaseClientCoreFactory(AppbaseClientCoreFactory factory) {
            this.clientCoreFactory = Objects.requireNonNull(factory, "factory");
            return this;
        }

        /**
         * Registers one or more client service factories.
         * A factory whose {@link AppbaseClientServiceFactory#serviceType()} matches a previously
         * registered factory replaces it. Calling {@code with()} multiple times accumulates
         * factories; last one per type wins.
         *
         * @param factories one or more client service factories
         * @return this builder
         */
        public Builder with(AppbaseClientServiceFactory<?>... factories) {
            Objects.requireNonNull(factories, "factories");
            for (AppbaseClientServiceFactory<?> f : factories) {
                customFactories.add(Objects.requireNonNull(f, "factory element must not be null"));
            }
            return this;
        }

        /**
         * Builds the {@link AppbaseClientServices} container by running all registered factories
         * in initialization order.
         *
         * @return the fully initialized, immutable container
         * @throws AppbaseRuntimeException if {@link #from} was not called, or a factory throws
         */
        public AppbaseClientServices build() {
            AppbaseCoreServices core = Objects.requireNonNull(coreServices,
                "Call from(AppbaseCoreServices) before build()");

            // Auto-register client ObjectMapper subtypes before any config deserialization
            AppbaseClientObjectMapperSubTypes.register();

            // 1. Map by serviceType — last registration for a given type wins
            Map<Class<?>, AppbaseClientServiceFactory<?>> factoryMap = new LinkedHashMap<>();
            for (AppbaseClientServiceFactory<?> f : customFactories) {
                factoryMap.put(f.serviceType(), f);
            }

            // 2. Sort by initializationOrder()
            List<AppbaseClientServiceFactory<?>> sorted = new ArrayList<>(factoryMap.values());
            sorted.sort(Comparator.comparingInt(AppbaseClientServiceFactory::initializationOrder));

            // 3. Let each factory register its configuration stores.
            // Pre-register a default DefaultAppbaseClientCoreConfiguration only if the caller has
            // not already supplied one — avoids silently overwriting caller-supplied config.
            for (AppbaseClientServiceFactory<?> factory : sorted) {
                factory.registerConfiguration(core);
            }
            if (predefinedClientCore == null && clientCoreFactory == null
                    && core.getConfigurationService()
                           .getTypedConfiguration(DefaultAppbaseClientCoreConfiguration.class) == null) {
                core.getConfigurationService().putTypedConfiguration(
                    DefaultAppbaseClientCoreConfiguration.class,
                    DefaultAppbaseClientCoreConfiguration.builder().build());
            }
            core.getConfigurationService().load();

            // 4. Resolve the shared AppbaseClientCore AFTER load() so custom factories can read config
            AppbaseClientCore clientCore;
            if (predefinedClientCore != null) {
                clientCore = predefinedClientCore;
            } else if (clientCoreFactory != null) {
                clientCore = clientCoreFactory.create(core.appbaseCore());
            } else {
                clientCore = new DefaultAppbaseClientCore(core.appbaseCore());
            }

            // 5. Run each factory with the shared client core; shut down partial services on failure
            Map<Class<?>, Object> built   = new LinkedHashMap<>();
            List<Object>          initOrd = new ArrayList<>();
            try {
                for (AppbaseClientServiceFactory<?> factory : sorted) {
                    Object svc = factory.create(core, clientCore);
                    built.put(factory.serviceType(), svc);
                    initOrd.add(svc);
                }
            } catch (Exception e) {
                for (int i = initOrd.size() - 1; i >= 0; i--) {
                    Object svc = initOrd.get(i);
                    if (svc instanceof Shutdownable) {
                        try { ((Shutdownable) svc).shutdown(); } catch (Exception se) { /* best effort */ }
                    }
                }
                throw new AppbaseRuntimeException(
                    "Failed to create client services, partial services shut down", e);
            }

            // 6. Validate dependency constraints
            validateDep(built,
                "VitessJdbi",
                "com.viveworks.appbase.client.service.vitess.VitessJdbiClientService",
                "com.viveworks.appbase.client.service.vitess.VitessJdbcClientService",
                "VitessJdbi requires VitessJdbc");
            validateDep(built,
                "FirebaseAuth",
                "com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService",
                "com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService",
                "FirebaseAuth requires FirebaseApp");

            return new AppbaseClientServices(core, clientCore, built, initOrd);
        }

        private static void validateDep(Map<Class<?>, Object> built,
                                        String label, String dependentFqn, String requiredFqn,
                                        String msg) {
            try {
                Class<?> dep = Class.forName(dependentFqn);
                Class<?> req = Class.forName(requiredFqn);
                if (built.containsKey(dep) && !built.containsKey(req)) {
                    throw new AppbaseRuntimeException(
                        msg + ". Register " + requiredFqn + " factory first.");
                }
            } catch (ClassNotFoundException ignored) {
                // class not on classpath — skip validation
            }
        }
    }
}
