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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.bootstrap.ReactorNettyEndpointRegistrar;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.bootstrap.reactornetty.endpoint.OffloadPolicy;
import com.viveworks.appbase.bootstrap.server.AppbaseWebServer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.JmxAppbaseCore;
import com.viveworks.appbase.core.jmx.JmxAppbaseCoreMXBean;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationServiceMXBean;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionServiceMXBean;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.jmx.JmxServiceException;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorServiceMXBean;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricService;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricServiceMXBean;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceHandler;
import com.viveworks.appbase.service.exchange.DefaultAppbaseServiceFilterChain;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.user.account.web.filter.reactive.IdpUserTokenCheckWebFilter;
import com.viveworks.appbase.service.user.session.web.filter.reactive.UserSessionWebFilter;
import com.viveworks.appbase.service.web.reactive.AuthorizationWebFilter;
import com.viveworks.appbase.service.web.reactive.CorsWebFilter;
import com.viveworks.appbase.service.web.reactive.ExceptionHandlerWebFilter;
import com.viveworks.appbase.service.web.reactive.MetricWebFilter;
import com.viveworks.appbase.service.web.reactive.TraceIdWebFilter;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A fully non-blocking HTTP face for Appbase, over reactor-netty. No servlet container, no
 * thread-per-request connector: a few event-loop threads multiplex every connection and are never
 * blocked by request handling.
 *
 * <p>Implements the EXISTING {@link AppbaseWebServer} lifecycle interface, which is already
 * servlet-free. When P-033 renames {@code AppbaseWebServer} to {@code AppbaseServletContainer} for
 * the servlet family, this class must be EXCLUDED from that rename -- it is not a servlet
 * container. P-033 needs a genuinely neutral lifecycle supertype.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactorNettyAppbaseWebServer implements AppbaseWebServer {

    private final ReactorNettyAppbaseWebServerConfiguration configuration;

    private final AppbaseRouteRegistry routeRegistry;

    private final List<AppbaseServiceFilter> filters;

    private final ObjectMapper objectMapper;

    private volatile DisposableServer disposableServer;

    /**
     * @param configuration server configuration
     * @param routeRegistry route table
     * @param filters cross-cutting filters, applied in {@code order()} sequence
     * @param objectMapper mapper used for JSON responses
     */
    public ReactorNettyAppbaseWebServer(
            final ReactorNettyAppbaseWebServerConfiguration configuration,
            final AppbaseRouteRegistry routeRegistry,
            final List<AppbaseServiceFilter> filters,
            final ObjectMapper objectMapper) {
        this.configuration = AppbaseUtil.notNull(configuration, "configuration");
        this.routeRegistry = AppbaseUtil.notNull(routeRegistry, "routeRegistry");
        this.filters = List.copyOf(AppbaseUtil.notNull(filters, "filters"));
        this.objectMapper = AppbaseUtil.notNull(objectMapper, "objectMapper");
    }

    @Override
    public void start() {
        if (disposableServer != null) {
            throw new IllegalStateException(
                    "ReactorNettyAppbaseWebServer is already started on port " + getPort());
        }

        if (configuration.isContextPropagationEnabled()) {
            // Without this, MDC / tracing / security context are lost the moment the chain
            // resumes on a scheduler thread rather than the accepting event-loop thread.
            //
            // CAUTION: this hook is JVM-global and irreversible -- Reactor exposes no way to
            // disable or scope it once enabled. The FIRST server started in this JVM decides the
            // outcome for every server that follows: if it enables the hook, a later server built
            // with contextPropagationEnabled=false still runs with propagation ON, silently. In
            // tests this leaks across every test class sharing a surefire fork, so a test that
            // asserts context-propagation is OFF can only be trusted if no earlier test in the
            // same fork started a server with it enabled.
            Hooks.enableAutomaticContextPropagation();
        }

        final HttpServer httpServer = HttpServer.create()
                .host(configuration.getHost())
                .port(configuration.getPort())
                .accessLog(configuration.isAccessLogEnabled())
                .compress(configuration.isCompressionEnabled())
                .handle((request, response) -> {
                    final NettyWebExchange exchange;
                    try {
                        exchange = new NettyWebExchange(request, response, objectMapper);
                    } catch (final AppbasePathException e) {
                        // Emitted before the filter chain is entered, so authorization never sees a
                        // path that could not be canonicalized. No filter is added: the seven-filter
                        // order invariant is untouched. The exchange does not exist at this point, so
                        // the 400 is written through reactor-netty's response directly.
                        return response.status(400).send();
                    }

                    final AppbaseServiceHandler handler = routeRegistry.lookup(exchange.method(), exchange.path());

                    final AppbaseServiceHandler terminal = handler != null
                            ? handler
                            : notFound();

                    return new DefaultAppbaseServiceFilterChain(filters, terminal).next(exchange);
                });

        disposableServer = httpServer.bindNow();
    }

    private static AppbaseServiceHandler notFound() {
        return exchange -> {
            exchange.status(404);
            // Canonical reactive error body: {status, message}. Matches OffloadedEndpoints'
            // DispatchException body so a client parses 400/404 uniformly. (The 500 path in
            // ExceptionHandlerWebFilter still carries an extra "error" field -- unifying that is
            // tracked separately, as it also drops the class-name leak.)
            return exchange.writeJson(Map.of(
                    "status", 404,
                    "message", "No route for " + exchange.method() + " " + exchange.path()));
        };
    }

    @Override
    public void stop() {
        final DisposableServer server = this.disposableServer;
        if (server != null) {
            server.disposeNow();
            this.disposableServer = null;
        }
    }

    @Override
    public int getPort() {
        final DisposableServer server = this.disposableServer;
        return server != null ? server.port() : -1;
    }

    @Override
    public boolean isStarted() {
        return disposableServer != null;
    }

    /** Returns the route registry backing this server (package-private, for tests). */
    AppbaseRouteRegistry routeRegistry() {
        return routeRegistry;
    }

    /** Returns the JSON mapper backing this server (package-private, for tests). */
    ObjectMapper objectMapper() {
        return objectMapper;
    }

    /** Returns the cross-cutting filters backing this server (package-private, for tests). */
    List<AppbaseServiceFilter> filters() {
        return filters;
    }

    /**
     * Returns a new builder for assembling a live {@link ReactorNettyAppbaseWebServer} from an
     * {@link AppbaseServices} container.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Assembles a runnable {@link ReactorNettyAppbaseWebServer} from an {@link AppbaseServices}
     * container -- the production wiring for the reactive web face. Mirrors
     * {@code JettyAppbaseWebServer.Builder}: {@link #from(AppbaseServices)} captures the services and
     * {@link #build()} resolves the configuration, offload scheduler, page/timeout policy, route
     * registry and filter chain, then registers the standard MXBeans (idempotently) before
     * constructing the server.
     *
     * <p>Unlike the servlet bootstrap, the reactor-netty {@link ReactorNettyAppbaseWebServerConfiguration}
     * is not carried inside {@link AppbaseServices}. It is supplied to the builder the same way the
     * Jetty builder takes its port/server -- via {@link #configuration(ReactorNettyAppbaseWebServerConfiguration)},
     * defaulting to an all-defaults configuration (host {@code 0.0.0.0}, port {@code 8080},
     * maxPageSize {@code 1000}, offloadTimeoutSeconds {@code 30}) when not set.
     */
    public static final class Builder {

        private AppbaseServices services;

        private ReactorNettyAppbaseWebServerConfiguration configuration;

        private Builder() {
        }

        /**
         * Captures the fully-built application services container this server is wired from. Required
         * before {@link #build()}.
         *
         * @param services the application services container
         * @return this builder
         */
        public Builder from(final AppbaseServices services) {
            this.services = Objects.requireNonNull(services, "services");
            return this;
        }

        /**
         * Supplies the server configuration. When omitted, an all-defaults
         * {@link ReactorNettyAppbaseWebServerConfiguration} is used.
         *
         * @param configuration the server configuration
         * @return this builder
         */
        public Builder configuration(final ReactorNettyAppbaseWebServerConfiguration configuration) {
            this.configuration = Objects.requireNonNull(configuration, "configuration");
            return this;
        }

        /**
         * Builds the server: resolves the offload policy and scheduler, builds the twelve-route
         * registry via {@link ReactorNettyEndpointRegistrar}, assembles the seven reactive filters in the
         * invariant order (authorization last), registers the standard MXBeans idempotently, and
         * constructs the server with the configured singleton {@link ObjectMapper}.
         *
         * @return the assembled, not-yet-started server
         * @throws NullPointerException if {@link #from(AppbaseServices)} was not called
         */
        public ReactorNettyAppbaseWebServer build() {
            Objects.requireNonNull(services,
                    "services required -- call from(AppbaseServices) before build()");

            final ReactorNettyAppbaseWebServerConfiguration config = configuration != null
                    ? configuration
                    : ReactorNettyAppbaseWebServerConfiguration.builder().build();

            final AppbaseCore appbaseCore = services.getCoreServices().appbaseCore();
            final HealthCheckService healthCheckService = services.get(HealthCheckService.class);

            final OffloadPolicy policy = OffloadPolicy.of(
                    config.getMaxPageSize(),
                    Duration.ofSeconds(config.getOffloadTimeoutSeconds()));

            final Scheduler scheduler = resolveOffloadScheduler(appbaseCore, config.getOffloadSchedulerId());

            final AppbaseRouteRegistry registry = ReactorNettyEndpointRegistrar.buildRegistry(
                    appbaseCore, healthCheckService, scheduler, policy, Set.of());

            final List<AppbaseServiceFilter> filters = assembleFilters(appbaseCore);

            // Idempotent -- safe to call even when an earlier bootstrap already registered the beans.
            registerMXBeans(appbaseCore);

            return new ReactorNettyAppbaseWebServer(config, registry, filters, JsonUtil.getJsonMapper());
        }

        /**
         * Resolves the Tier-1 offload scheduler. A blank/absent id falls back to a shared bounded
         * elastic scheduler; a configured id is resolved through the core {@link ReactorService}.
         */
        private static Scheduler resolveOffloadScheduler(final AppbaseCore appbaseCore,
                                                         final String offloadSchedulerId) {
            if (offloadSchedulerId == null || offloadSchedulerId.isBlank()) {
                return Schedulers.boundedElastic();
            }
            final ReactorService reactorService = appbaseCore.getReactorService();
            if (reactorService == null) {
                throw new AppbaseRuntimeException(
                        "offloadSchedulerId '" + offloadSchedulerId + "' was configured but no ReactorService"
                        + " is available -- enable the reactor service or leave offloadSchedulerId unset.");
            }
            return reactorService.getSchedulerById(offloadSchedulerId);
        }

        /**
         * Assembles the seven reactive filters in the security-invariant order:
         * {@code exceptionHandler -> CORS -> metric -> traceId -> userSession -> idpTokenCheck ->
         * authorization}. Authorization is always last.
         */
        private static List<AppbaseServiceFilter> assembleFilters(final AppbaseCore appbaseCore) {
            final List<AppbaseServiceFilter> filters = new ArrayList<>(7);
            filters.add(new ExceptionHandlerWebFilter(appbaseCore));
            filters.add(new CorsWebFilter(appbaseCore));
            filters.add(new MetricWebFilter(appbaseCore));
            filters.add(new TraceIdWebFilter(appbaseCore));
            filters.add(new UserSessionWebFilter(appbaseCore));
            filters.add(new IdpUserTokenCheckWebFilter(appbaseCore));
            filters.add(new AuthorizationWebFilter(appbaseCore));
            return filters;
        }
    }

    /**
     * Registers the standard set of Appbase JMX MXBeans on the given {@link AppbaseCore}.
     * Beans that are already registered are skipped (idempotent). Mirrors
     * {@code JettyAppbaseWebServer.registerMXBeans}.
     *
     * @param appbaseCore the core Appbase instance to register beans on
     * @throws AppbaseRuntimeException if any MXBean registration fails
     */
    public static void registerMXBeans(final AppbaseCore appbaseCore) {
        final LogService logService = appbaseCore.getLogService();
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final JmxService jmxService = appbaseCore.getJmxService();

        try {
            if (!jmxService.isMXBeanRegistered(JmxConfigurationServiceMXBean.class)) {
                jmxService.registerMXBean(new JmxConfigurationService(
                        logService, configurationService,
                        DefaultConfigurationServiceConfiguration.class, configurationService));
            }
            if (!jmxService.isMXBeanRegistered(JmxExceptionServiceMXBean.class)) {
                jmxService.registerMXBean(new JmxExceptionService(
                        logService, appbaseCore.getExceptionService(),
                        DefaultExceptionServiceConfiguration.class, configurationService));
            }
            if (!jmxService.isMXBeanRegistered(JmxLogServiceMXBean.class)
                    && logService.getLogAccessor() != null) {
                jmxService.registerMXBean(new JmxLogService(
                        logService, logService,
                        DefaultLogServiceConfiguration.class, configurationService));
            }
            if (!jmxService.isMXBeanRegistered(JmxMediatorServiceMXBean.class)) {
                jmxService.registerMXBean(new JmxMediatorService(
                        logService, appbaseCore.getMediatorService(),
                        DefaultMediatorServiceConfiguration.class, configurationService));
            }
            if (!jmxService.isMXBeanRegistered(JmxMetricServiceMXBean.class)) {
                jmxService.registerMXBean(new JmxMetricService(
                        logService, appbaseCore.getMetricService(),
                        DefaultMetricServiceConfiguration.class, configurationService));
            }

            final JmxServiceRegistryMXBean jmxServiceRegistry;
            if (!jmxService.isMXBeanRegistered(JmxServiceRegistryMXBean.class)) {
                jmxServiceRegistry = new JmxServiceRegistry(
                        logService, appbaseCore.getServiceRegistry(),
                        DefaultServiceRegistryConfiguration.class, configurationService);
                jmxService.registerMXBean(jmxServiceRegistry);
            } else {
                jmxServiceRegistry = jmxService.getMXBean(JmxServiceRegistryMXBean.class);
            }

            final JmxJmxServiceMXBean jmxJmxService;
            if (!jmxService.isMXBeanRegistered(JmxJmxServiceMXBean.class)) {
                jmxJmxService = new JmxJmxService(
                        logService, jmxService,
                        DefaultJmxServiceConfiguration.class, configurationService);
                jmxService.registerMXBean(jmxJmxService);
            } else {
                jmxJmxService = jmxService.getMXBean(JmxJmxServiceMXBean.class);
            }

            if (!jmxService.isMXBeanRegistered(JmxAppbaseCoreMXBean.class)) {
                jmxService.registerMXBean(new JmxAppbaseCore(
                        appbaseCore, appbaseCore.getServiceRegistry(),
                        jmxServiceRegistry, jmxJmxService));
            }
        } catch (JmxServiceException jse) {
            throw new AppbaseRuntimeException("Failed to register MX beans", jse);
        }
    }
}
