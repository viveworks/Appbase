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

package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.factory.DefaultAuthorizationServiceFactory;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.JmxAppbaseCore;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.accessor.LogAccessorException;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricService;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.authorization.ContextAuthorizer;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationServiceConfiguration;
import com.viveworks.appbase.service.authorization.PermitAllContextAuthorizer;
import com.viveworks.appbase.service.authorization.SecureByDefaultContextAuthorizer;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Immutable container holding all initialized Appbase application services.
 *
 * <p>Build via the fluent builder:
 * <pre>{@code
 * AppbaseServices services = AppbaseServices.builder()
 *     .from(coreServices)
 *     .withClientServices(clientServices)
 *     .with(new DefaultIdServiceFactory(idConfig))
 *     .with(new DefaultPasswordServiceFactory(pwdConfig))
 *     .build();
 *
 * IdService idService = services.get(IdService.class);
 * }</pre>
 *
 * <p>Register application services by passing their factories to {@link Builder#with}. Factories are
 * run in {@link AppbaseServiceFactory#initializationOrder()} order; each factory receives the
 * partially built container so it can retrieve services initialized before it.
 *
 * <p>By default the container is <em>secure by default</em>: unless an application supplies its own
 * authorizer, {@link Builder#build()} wires an {@code AuthorizationService} that denies every request
 * except {@link Builder#publicPaths(Set)} and registers a FORBIDDEN response configuration so the web
 * faces answer HTTP 403 on denial. See {@link Builder#secureByDefault(boolean)}.
 */
@Immutable
public final class AppbaseServices implements Shutdownable {

    private final AppbaseCoreServices coreServices;
    @Nullable
    private final AppbaseClientServices clientServices;
    private final Map<Class<?>, Object> services;
    private final List<Object> initOrder;
    private volatile boolean shutdown = false;

    private AppbaseServices(final AppbaseCoreServices coreServices,
                             @Nullable final AppbaseClientServices clientServices,
                             final Map<Class<?>, Object> services,
                             final List<Object> initOrder) {
        this.coreServices   = coreServices;
        this.clientServices = clientServices;
        this.services       = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        this.initOrder      = List.copyOf(initOrder);
    }

    // ── Typed access ──────────────────────────────────────────────────────────

    /**
     * Returns the application service registered under {@code serviceType}.
     *
     * @param serviceType the service interface class
     * @param <T>         the service type
     * @return the registered service instance
     * @throws AppbaseRuntimeException if no service of that type is registered
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Class<T> serviceType) {
        Object svc = services.get(Objects.requireNonNull(serviceType, "serviceType"));
        if (svc == null) {
            throw new AppbaseRuntimeException(
                "No application service registered for: " + serviceType.getName()
                + ". Provide it via AppbaseServices.builder().with(factory).");
        }
        return (T) svc;
    }

    /**
     * Returns an unmodifiable map of all registered application services, keyed by service interface.
     *
     * @return unmodifiable service map (may be empty)
     */
    public Map<Class<?>, Object> services() {
        return services;
    }

    /**
     * Returns the core services container used to build this application services container.
     *
     * @return core services (never null)
     */
    public AppbaseCoreServices getCoreServices() {
        return coreServices;
    }

    /**
     * Returns the client services container used to build this application services container,
     * or {@code null} if none was provided.
     *
     * @return client services, or null if not provided
     */
    @Nullable
    public AppbaseClientServices getClientServices() {
        return clientServices;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Shuts down all application services in reverse initialization order.
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
                org.slf4j.LoggerFactory.getLogger(AppbaseServices.class)
                    .error("Error shutting down application service {}", svc.getClass().getName(), e);
            }
        }
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /**
     * Builder for {@link AppbaseServices}.
     *
     * <p>Call {@link #from} to supply the core services, optionally call
     * {@link #withClientServices} to supply the client services, then register factories
     * with {@link #with}. {@link #build} runs factories in
     * {@link AppbaseServiceFactory#initializationOrder()} order, passing the partial
     * container to each factory so later factories can retrieve earlier services.
     */
    public static final class Builder {

        @Nullable
        private AppbaseCoreServices coreServices;
        @Nullable
        private AppbaseClientServices clientServices;
        private final List<AppbaseServiceFactory<?>> customFactories = new ArrayList<>();
        private boolean allJmxOperationsEnabled = false;

        /**
         * The authorizer explicitly supplied by {@link #withContextAuthorizer(ContextAuthorizer)}.
         * When set, it wins over the secure-by-default selection. Resolved into a
         * {@link DefaultAuthorizationServiceFactory} in {@link #build()}.
         */
        @Nullable
        private ContextAuthorizer explicitContextAuthorizer;

        /**
         * When {@code true} (the default), an app that does not supply its own authorizer denies
         * every request except {@link #publicPaths}, and a FORBIDDEN
         * {@link AuthorizationServiceExceptionConfiguration} is registered so the web filters answer
         * HTTP 403. When {@code false}, the default authorizer permits all paths.
         */
        private boolean secureByDefault = true;

        /**
         * Request paths authorized without further checks when {@link #secureByDefault} is on.
         * Defaults to {@code {"/health"}}; stored as an immutable copy.
         */
        private Set<String> publicPaths = Set.of("/health");

        /**
         * Supplies the core services container passed to each factory's {@code create} method.
         *
         * @param core the fully-built core services container (must not be null)
         * @return this builder
         */
        public Builder from(final AppbaseCoreServices core) {
            this.coreServices = Objects.requireNonNull(core, "core");
            return this;
        }

        /**
         * Supplies the client services container passed to each factory's {@code create} method.
         *
         * @param clients the fully-built client services container (must not be null)
         * @return this builder
         */
        public Builder withClientServices(final AppbaseClientServices clients) {
            this.clientServices = Objects.requireNonNull(clients, "clients");
            return this;
        }

        /**
         * Registers a {@link ContextAuthorizer} as the authorization strategy for all
         * incoming requests. Equivalent to calling
         * {@code with(new DefaultAuthorizationServiceFactory(contextAuthorizer))}.
         *
         * <p>Passing {@link PermitAllContextAuthorizer} disables path authorization entirely: every
         * request not matched by a blocklist entry is authorized.
         *
         * <p>An explicit authorizer set here always wins over the secure-by-default selection
         * driven by {@link #secureByDefault(boolean)} / {@link #publicPaths(Set)}.
         *
         * @param contextAuthorizer the authorization implementation (must not be null)
         * @return this builder
         */
        public Builder withContextAuthorizer(final ContextAuthorizer contextAuthorizer) {
            this.explicitContextAuthorizer =
                Objects.requireNonNull(contextAuthorizer, "contextAuthorizer");
            return this;
        }

        /**
         * Toggles secure-by-default authorization. When {@code true} (the default) and no explicit
         * {@link #withContextAuthorizer(ContextAuthorizer)} was supplied, the effective authorizer is a
         * {@link SecureByDefaultContextAuthorizer} over {@link #publicPaths(Set)} (deny everything
         * else), and a FORBIDDEN {@link AuthorizationServiceExceptionConfiguration} is registered so
         * the web filters answer HTTP 403 on denial. When {@code false}, the effective authorizer is a
         * {@link PermitAllContextAuthorizer}.
         *
         * @param secureByDefault whether to deny by default
         * @return this builder
         */
        public Builder secureByDefault(final boolean secureByDefault) {
            this.secureByDefault = secureByDefault;
            return this;
        }

        /**
         * Sets the request paths that a {@link SecureByDefaultContextAuthorizer} authorizes without
         * further checks. Only consulted when secure-by-default is on and no explicit authorizer was
         * supplied. Defaults to {@code {"/health"}}.
         *
         * @param publicPaths the public paths (must not be null); stored as an immutable copy
         * @return this builder
         */
        public Builder publicPaths(final Set<String> publicPaths) {
            this.publicPaths = Set.copyOf(Objects.requireNonNull(publicPaths, "publicPaths"));
            return this;
        }

        /**
         * Resolves the effective {@link ContextAuthorizer} that secure-by-default wiring will use:
         * an explicit {@link #withContextAuthorizer(ContextAuthorizer)} wins; otherwise a
         * {@link SecureByDefaultContextAuthorizer} over {@link #publicPaths} when secure-by-default is
         * on, else a {@link PermitAllContextAuthorizer}.
         *
         * <p>Package-private: exposed so tests can observe the selection without reaching into the
         * built {@link AuthorizationService}, which does not expose its authorizer.
         *
         * @return the effective authorizer (never null)
         */
        ContextAuthorizer resolveContextAuthorizer() {
            if (explicitContextAuthorizer != null) {
                return explicitContextAuthorizer;
            }
            // Canonicalize the public paths with the SAME canonicalizer the authorization rule keys and
            // the request paths go through, so a non-canonical operator entry (e.g. "/a/../health",
            // "/Health") still matches the canonical request path the authorizer compares against instead
            // of being silently denied. canonicalizeSet only touches "/"-prefixed entries; non-path
            // entries (should any appear) pass through untouched.
            return secureByDefault
                ? new SecureByDefaultContextAuthorizer(AppbasePathCanonicalizer.canonicalizeSet(publicPaths))
                : new PermitAllContextAuthorizer();
        }

        /**
         * A {@link AuthorizationServiceExceptionConfiguration} is NEUTRAL when it would make the web
         * authorization filter pass a denied request through untouched: it neither responds forbidden,
         * nor redirects, nor sets a positive HTTP status. Under secure-by-default such a config silently
         * fails open, so {@link #build()} replaces it with the FORBIDDEN default rather than honouring it.
         *
         * @param cfg the pre-registered exception configuration (must not be {@code null})
         * @return {@code true} if the config takes no action on denial and must be overridden
         */
        private static boolean isNeutral(final AuthorizationServiceExceptionConfiguration cfg) {
            final String redirectUrl = cfg.getRedirectUrl();
            return !cfg.isRespondForbidden()
                && (redirectUrl == null || redirectUrl.isBlank())
                && cfg.getHttpStatusCode() <= 0;
        }

        /**
         * Enables all JMX operation permissions on the core configuration service after
         * {@link #build()} completes. This sets the global and per-service
         * {@code AllOperationsPermission} properties and the shared reload and shutdown
         * operation-specific properties to {@code true}, allowing JMX clients to invoke all exposed
         * operations without restriction.
         *
         * @return this builder
         */
        public Builder withAllJmxOperationsEnabled() {
            this.allJmxOperationsEnabled = true;
            return this;
        }

        /**
         * Registers one or more application service factories.
         * A factory whose {@link AppbaseServiceFactory#serviceType()} matches a previously
         * registered factory replaces it. Calling {@code with()} multiple times accumulates
         * factories; last one per type wins.
         *
         * @param factories one or more application service factories
         * @return this builder
         */
        public Builder with(final AppbaseServiceFactory<?>... factories) {
            Objects.requireNonNull(factories, "factories");
            for (AppbaseServiceFactory<?> f : factories) {
                customFactories.add(Objects.requireNonNull(f, "factory element must not be null"));
            }
            return this;
        }

        /**
         * Builds the {@link AppbaseServices} container by running all registered factories
         * in initialization order, each receiving the partially built container.
         *
         * @return the fully initialized, immutable container
         * @throws AppbaseRuntimeException if {@link #from} was not called, or a factory throws
         */
        public AppbaseServices build() {
            AppbaseCoreServices core = Objects.requireNonNull(coreServices,
                "Call from(AppbaseCoreServices) before build()");

            // 0. Secure-by-default authorizer wiring. Both web faces read the AuthorizationService
            //    from the built container, so the authorizer must be materialised as a factory here.
            //    An explicit withContextAuthorizer(...) wins; otherwise, unless the operator already
            //    registered their own AuthorizationService factory via with(...), inject the
            //    secure-by-default (or permit-all) selection.
            //
            //    The injected factory must be self-sufficient on a plain YAML-configured core: a real
            //    bootstrap does NOT register a DefaultAuthorizationServiceConfiguration, so the
            //    config-reading factory constructor would resolve null and DefaultAuthorizationService
            //    would reject it. We therefore supply a built default config unless the operator has
            //    already registered one — in which case we use the config-reading path so their
            //    registration is honoured and never clobbered.
            final List<AppbaseServiceFactory<?>> effectiveFactories = new ArrayList<>(customFactories);
            final boolean operatorRegisteredAuthorization = effectiveFactories.stream()
                .anyMatch(f -> f.serviceType() == AuthorizationService.class);
            if (explicitContextAuthorizer != null || !operatorRegisteredAuthorization) {
                final boolean operatorRegisteredAuthConfig = core.getConfigurationService()
                    .getTypedConfiguration(DefaultAuthorizationServiceConfiguration.class) != null;
                effectiveFactories.add(operatorRegisteredAuthConfig
                    ? new DefaultAuthorizationServiceFactory(resolveContextAuthorizer())
                    : new DefaultAuthorizationServiceFactory(
                        DefaultAuthorizationServiceConfiguration.builder().build(),
                        resolveContextAuthorizer()));
            }

            // 1. Map by serviceType — last registration for a given type wins
            Map<Class<?>, AppbaseServiceFactory<?>> factoryMap = new LinkedHashMap<>();
            for (AppbaseServiceFactory<?> f : effectiveFactories) {
                factoryMap.put(f.serviceType(), f);
            }

            // 2. Sort by initializationOrder()
            List<AppbaseServiceFactory<?>> sorted = new ArrayList<>(factoryMap.values());
            sorted.sort(Comparator.comparingInt(AppbaseServiceFactory::initializationOrder));

            // 3. Run each factory; pass growing partial container so later factories see earlier deps
            Map<Class<?>, Object> built   = new LinkedHashMap<>();
            List<Object>          initOrd = new ArrayList<>();
            AppbaseServices partial = new AppbaseServices(core, clientServices, built, initOrd);

            for (AppbaseServiceFactory<?> factory : sorted) {
                try {
                    Object svc = factory.create(core, clientServices, partial);
                    built.put(factory.serviceType(), svc);
                    initOrd.add(svc);
                    partial = new AppbaseServices(core, clientServices,
                        new LinkedHashMap<>(built), new ArrayList<>(initOrd));
                } catch (Exception e) {
                    throw new AppbaseRuntimeException(
                        "Failed to create application service: " + factory.serviceType().getName(), e);
                }
            }

            AppbaseServices result = new AppbaseServices(core, clientServices, built, initOrd);

            // Secure-by-default: register a FORBIDDEN AuthorizationServiceExceptionConfiguration so the
            // web filters emit HTTP 403 when the authorizer denies. The filters read this config at
            // request time (not during build), so it only needs the ConfigurationService — available
            // from core as soon as from(...) was called — and has no ordering dependency on the
            // AuthorizationService factory (which reads a different config type).
            //
            // Register FORBIDDEN when there is NO config OR the existing config is NEUTRAL (see
            // isNeutral): a neutral config makes the filter pass through on deny, so leaving it in place
            // would let secure-by-default silently fail open (admin reads reachable). A config carrying a
            // genuine action (respondForbidden=true, a redirect, or a positive status) is a real operator
            // choice and is preserved — never clobbered.
            if (secureByDefault) {
                final var configurationService = core.getConfigurationService();
                final AuthorizationServiceExceptionConfiguration existing =
                    configurationService.getTypedConfiguration(
                        AuthorizationServiceExceptionConfiguration.class);
                if (existing == null || isNeutral(existing)) {
                    configurationService.putTypedConfiguration(
                        AuthorizationServiceExceptionConfiguration.class,
                        AuthorizationServiceExceptionConfiguration.builder().build());
                }
            }

            if (allJmxOperationsEnabled) {
                final var cs = core.getConfigurationService();
                cs.putBoolean(AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxConfigurationService.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxAppbaseCore.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxExceptionService.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxLogService.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxJmxService.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxMediatorService.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxMetricService.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
                cs.putBoolean(JmxServiceRegistry.ALL_OPERATIONS_PERMISSION_PROPERTY, true);
            }

            @Nullable final LogAccessor logAccessor = core.getLogService().getLogAccessor();
            if (logAccessor != null) {
                try {
                    logAccessor.synchronizeLogStores();
                } catch (LogAccessorException lae) {
                    throw new AppbaseRuntimeException("Error synchronizing log stores", lae);
                }
            }

            return result;
        }
    }
}
