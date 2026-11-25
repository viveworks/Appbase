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

package com.viveworks.appbase.bootstrap.jetty.server;

import com.viveworks.appbase.bootstrap.servlet.AppbaseCoreOpenApiRegistrar;
import com.viveworks.appbase.bootstrap.servlet.AppbaseServletRegistrar;
import com.viveworks.appbase.bootstrap.servlet.JerseyResourceConfigCompatibility;
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
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Jetty-based HTTP server configured from an {@link AppbaseServices} container.
 *
 * <p>Build via:
 * <pre>{@code
 * // Default server on port 8080:
 * JettyServletContainer server = JettyServletContainer.builder()
 *     .from(appbaseServices)
 *     .resourceConfig(myResourceConfig)
 *     .build();
 * server.start();
 * server.join();
 *
 * // Custom port:
 * JettyServletContainer server = JettyServletContainer.builder()
 *     .from(appbaseServices)
 *     .port(9000)
 *     .build();
 *
 * // Bring-your-own Jetty Server (mutually exclusive with port()):
 * JettyServletContainer server = JettyServletContainer.builder()
 *     .from(appbaseServices)
 *     .server(myServer)
 *     .build();
 * }</pre>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
public final class JettyServletContainer implements AppbaseWebServer {

    /**
     * Jersey {@link org.glassfish.jersey.server.ServerProperties} key that enables
     * pretty-printed OpenAPI JSON output.
     * Use as: {@code resourceConfig.property(JettyServletContainer.OPENAPI_PRETTY_PRINT, true)}.
     */
    public static final String OPENAPI_PRETTY_PRINT = "openApi.configuration.prettyPrint";

    static final int DEFAULT_PORT = 8080;

    private final Server server;
    private final AppbaseServices appbaseServices;
    private final int port;

    private JettyServletContainer(final Server server, final AppbaseServices appbaseServices,
                                   final int port)
    {
        this.server = server;
        this.appbaseServices = appbaseServices;
        this.port = port;
    }

    /**
     * Starts the Jetty server.
     *
     * @throws AppbaseRuntimeException if the server fails to start
     */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void start() {
        try {
            server.start();
        } catch (final Exception exception) {
            throw new AppbaseRuntimeException("Failed to start JettyServletContainer", exception);
        }
    }

    /**
     * Stops the Jetty server and shuts down all Appbase services in reverse initialization order.
     *
     * @throws AppbaseRuntimeException if the server fails to stop
     */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void stop() {
        try {
            server.stop();
        } catch (final Exception exception) {
            throw new AppbaseRuntimeException("Failed to stop JettyServletContainer", exception);
        } finally {
            appbaseServices.shutdown();
        }
    }

    /**
     * Returns the port this server is bound to.
     *
     * <p>If this server was built with a fixed port, that port is returned immediately.
     * If a custom {@link Server} was supplied (port unknown at build time), the first
     * {@link ServerConnector}'s local port is returned after the server has started.
     *
     * @return the port, or {@code -1} if unavailable
     */
    @Override
    public int getPort() {
        if (port > 0) {
            return port;
        }
        final Connector[] connectors = server.getConnectors();
        if (connectors.length > 0 && connectors[0] instanceof ServerConnector sc) {
            return sc.getLocalPort();
        }
        return -1;
    }

    /**
     * Returns {@code true} if the underlying Jetty server has been started.
     *
     * @return {@code true} when the server is running
     */
    @Override
    public boolean isStarted() {
        return server.isStarted();
    }

    /**
     * Blocks the calling thread until the server stops.
     *
     * @throws AppbaseRuntimeException if the calling thread is interrupted
     */
    public void join() {
        try {
            server.join();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppbaseRuntimeException(
                "JettyServletContainer.join() interrupted", exception);
        }
    }

    /**
     * Returns the underlying Jetty {@link Server} instance.
     *
     * @return the underlying Jetty server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Returns the {@link AppbaseServices} container backing this server.
     *
     * @return the backing Appbase services container
     */
    public AppbaseServices getAppbaseServices() {
        return appbaseServices;
    }

    /**
     * Returns a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link JettyServletContainer}.
     *
     * <p>Exactly one of {@link #server(Server)} or {@link #port(int)} may be supplied;
     * calling both causes {@link #build()} to throw {@link IllegalStateException}.
     * If neither is called, port {@value JettyServletContainer#DEFAULT_PORT} is used.
     */
    public static final class Builder {

        private final List<Handler> additionalHandlers = new ArrayList<>();
        private final List<Consumer<ServletContextHandler>> servletContextCustomizers =
            new ArrayList<>();
        @Nullable private AppbaseServices appbaseServices;
        @Nullable private Server customServer;
        @Nullable private Integer port;
        @Nullable private ResourceConfig resourceConfig;
        private boolean registerDefaultMXBeans;
        // null = "don't register"; non-null (even empty) = "register, with these excludes/overrides"
        @Nullable private Set<Class<?>> coreServletExcludes;
        @Nullable private Map<Class<?>, String> coreServletPathOverrides;
        private boolean coreOpenApi;
        @Nullable private Set<Class<?>> serviceFilterExcludes;
        @Nullable private Set<Class<?>> serviceServletExcludes;
        @Nullable private Map<Class<?>, String> serviceServletPathOverrides;

        /**
         * Sets the Appbase services container. Required before calling {@link #build()}.
         *
         * @param services fully-built application services
         * @return this builder
         */
        public Builder from(final AppbaseServices services) {
            this.appbaseServices = Objects.requireNonNull(services, "services");
            return this;
        }

        /**
         * Provides a custom Jetty {@link Server} instance.
         * Mutually exclusive with {@link #port(int)}: calling both causes {@link #build()} to throw.
         *
         * @param server the Jetty server to use
         * @return this builder
         */
        public Builder server(final Server server) {
            this.customServer = Objects.requireNonNull(server, "server");
            return this;
        }

        /**
         * Sets the HTTP port. A new {@link Server} is created with this port.
         * Mutually exclusive with {@link #server(Server)}: calling both causes {@link #build()} to throw.
         * Defaults to {@value JettyServletContainer#DEFAULT_PORT} if neither is called.
         *
         * @param port the TCP port to listen on
         * @return this builder
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the Jersey {@link ResourceConfig} for JAX-RS resource registration.
         * If not set, an empty {@link ResourceConfig} is used.
         * Package scanning is supported through Java 25. On a later runtime, {@link #build()}
         * reports {@code Jersey package scanning is supported through Java 25, but this server is
         * running on Java N} and directs callers to use explicit
         * {@link ResourceConfig#register(Class)}, a supported Java runtime, or upgraded
         * Appbase/Jersey.
         *
         * @param resourceConfig the Jersey resource configuration
         * @return this builder
         */
        public Builder resourceConfig(final ResourceConfig resourceConfig) {
            this.resourceConfig = Objects.requireNonNull(resourceConfig, "resourceConfig");
            return this;
        }

        /**
         * Registers the standard set of Appbase JMX MXBeans (configuration, exception, log,
         * mediator, metric, service registry, JMX service, and core) on the server's JmxService.
         * Beans that are already registered are skipped (idempotent).
         *
         * @return this builder
         */
        public Builder withDefaultMXBeans() {
            this.registerDefaultMXBeans = true;
            return this;
        }

        /**
         * Registers all nine standard appbase core servlets at their default paths.
         *
         * @return this builder
         */
        public Builder withAppbaseCoreServlets() {
            return withAppbaseCoreServlets(Collections.emptySet(), Collections.emptyMap());
        }

        /**
         * Registers appbase core servlets, skipping any type in {@code excludes}.
         *
         * @param excludes servlet types to omit; must not be {@code null}
         * @return this builder
         */
        public Builder withAppbaseCoreServlets(final Set<Class<?>> excludes) {
            return withAppbaseCoreServlets(excludes, Collections.emptyMap());
        }

        /**
         * Registers appbase core servlets with per-type path overrides and an exclude list.
         * Servlets in {@code excludes} are skipped entirely. Servlets in {@code pathOverrides}
         * are mounted at the given path instead of their default.
         *
         * @param excludes      servlet types to omit; must not be {@code null}
         * @param pathOverrides per-type path overrides; must not be {@code null}
         * @return this builder
         */
        public Builder withAppbaseCoreServlets(final Set<Class<?>> excludes,
                                                final Map<Class<?>, String> pathOverrides)
        {
            this.coreServletExcludes = Collections.unmodifiableSet(
                new HashSet<>(Objects.requireNonNull(excludes, "excludes")));
            this.coreServletPathOverrides = Collections.unmodifiableMap(
                new HashMap<>(Objects.requireNonNull(pathOverrides, "pathOverrides")));
            return this;
        }

        /**
         * Exposes the core-only OpenAPI document when core servlets are also enabled.
         * This option is off by default and has no effect without
         * {@link #withAppbaseCoreServlets()}.
         *
         * @return this builder
         */
        public Builder withAppbaseCoreOpenApi() {
            this.coreOpenApi = true;
            return this;
        }

        /**
         * Registers all standard appbase service filters at {@code "/*"}.
         *
         * @return this builder
         */
        public Builder withAppbaseServiceFilters() {
            return withAppbaseServiceFilters(Collections.emptySet());
        }

        /**
         * Registers appbase service filters, skipping any type in {@code excludes}.
         *
         * @param excludes filter types to omit; must not be {@code null}
         * @return this builder
         */
        public Builder withAppbaseServiceFilters(final Set<Class<?>> excludes) {
            this.serviceFilterExcludes = Collections.unmodifiableSet(
                new HashSet<>(Objects.requireNonNull(excludes, "excludes")));
            return this;
        }

        /**
         * Registers all standard appbase service servlets at their default paths.
         *
         * @return this builder
         */
        public Builder withAppbaseServiceServlets() {
            return withAppbaseServiceServlets(Collections.emptySet(), Collections.emptyMap());
        }

        /**
         * Registers appbase service servlets, skipping any type in {@code excludes}.
         *
         * @param excludes servlet types to omit; must not be {@code null}
         * @return this builder
         */
        public Builder withAppbaseServiceServlets(final Set<Class<?>> excludes) {
            return withAppbaseServiceServlets(excludes, Collections.emptyMap());
        }

        /**
         * Registers appbase service servlets with per-type path overrides and an exclude list.
         * Servlets in {@code excludes} are skipped entirely. Servlets in {@code pathOverrides}
         * are mounted at the given path instead of their default.
         *
         * @param excludes      servlet types to omit; must not be {@code null}
         * @param pathOverrides per-type path overrides; must not be {@code null}
         * @return this builder
         */
        public Builder withAppbaseServiceServlets(final Set<Class<?>> excludes,
                                                   final Map<Class<?>, String> pathOverrides)
        {
            this.serviceServletExcludes = Collections.unmodifiableSet(
                new HashSet<>(Objects.requireNonNull(excludes, "excludes")));
            this.serviceServletPathOverrides = Collections.unmodifiableMap(
                new HashMap<>(Objects.requireNonNull(pathOverrides, "pathOverrides")));
            return this;
        }

        /**
         * Adds a Swagger UI static resource handler at the given context path.
         * The handler serves files from the specified classpath base resource directory
         * and uses {@code welcomeFile} as the directory index.
         *
         * <p>Example:
         * <pre>{@code
         * .withSwaggerUI("/restapi", "/swagger-ui/dist", "index.html")
         * }</pre>
         *
         * @param contextPath        the URL context path (e.g. {@code "/restapi"})
         * @param classpathBaseResource classpath resource directory containing the Swagger UI files
         *                           (e.g. {@code "/swagger-ui/dist"})
         * @param welcomeFile        the welcome file name (e.g. {@code "index.html"})
         * @return this builder
         */
        public Builder withSwaggerUI(final String contextPath,
                                     final String classpathBaseResource,
                                     final String welcomeFile)
        {
            final ContextHandler ctxHandler =
                new ContextHandler(Objects.requireNonNull(contextPath, "contextPath"));
            final ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setBaseResource(
                Resource.newClassPathResource(
                    Objects.requireNonNull(classpathBaseResource, "classpathBaseResource")));
            resourceHandler.setWelcomeFiles(
                new String[] {Objects.requireNonNull(welcomeFile, "welcomeFile")});
            ctxHandler.setHandler(resourceHandler);
            additionalHandlers.add(ctxHandler);
            return this;
        }

        /**
         * Adds a Jetty {@link Handler} to the server's handler chain alongside the main
         * Jersey servlet context. When at least one additional handler is registered,
         * the server root becomes a {@link HandlerCollection} containing all additional handlers
         * followed by the servlet context handler. Without any additional handlers, the
         * servlet context is set directly as the root handler.
         *
         * @param handler the handler to add (e.g., a Swagger UI {@code ResourceHandler})
         * @return this builder
         */
        public Builder withAdditionalHandler(final Handler handler) {
            additionalHandlers.add(Objects.requireNonNull(handler, "handler"));
            return this;
        }

        /**
         * Registers a customizer that receives the {@link ServletContextHandler} during
         * {@link #build()}, after it is created but before it is added to the server.
         * Multiple customizers are applied in registration order. Typical use: adding
         * servlets, filters, or init parameters.
         *
         * @param customizer a consumer that configures the servlet context
         * @return this builder
         */
        public Builder withServletContext(final Consumer<ServletContextHandler> customizer) {
            servletContextCustomizers.add(Objects.requireNonNull(customizer, "customizer"));
            return this;
        }

        /**
         * Builds the {@link JettyServletContainer}.
         *
         * @return the configured but not yet started server
         * @throws NullPointerException  if {@link #from} was not called
         * @throws IllegalStateException if both {@link #server} and {@link #port} were called, if
         *                               package scanning is configured after Java 25, or if
         *                               future-Jersey compatibility cannot be inspected safely
         */
        public JettyServletContainer build() {
            Objects.requireNonNull(appbaseServices,
                "appbaseServices required — call from(AppbaseServices) before build()");
            if (customServer != null && port != null) {
                throw new IllegalStateException(
                    "server(Server) and port(int) are mutually exclusive — use one or the other.");
            }

            final int resolvedPort = customServer != null ? -1 : (port != null ? port : DEFAULT_PORT);

            final Server jettyServer = customServer != null ?
                customServer :
                new Server(resolvedPort);

            final ResourceConfig rc = resourceConfig != null ? resourceConfig : new ResourceConfig();
            if (coreServletExcludes != null && coreOpenApi) {
                AppbaseCoreOpenApiRegistrar.register(
                    rc, coreServletExcludes, coreServletPathOverrides);
            }
            JerseyResourceConfigCompatibility.validate(rc);
            final ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            context.setContextPath("/");
            context.addServlet(new ServletHolder(new ServletContainer(rc)), "/*");

            final JettyServletDeployment deployment = new JettyServletDeployment(context);
            if (coreServletExcludes != null) {
                AppbaseServletRegistrar.addCoreServlets(deployment, coreServletExcludes, coreServletPathOverrides);
            }
            if (serviceFilterExcludes != null) {
                AppbaseServletRegistrar.addServiceFilters(deployment, serviceFilterExcludes);
            }
            if (serviceServletExcludes != null) {
                AppbaseServletRegistrar.addServiceServlets(deployment, serviceServletExcludes, serviceServletPathOverrides);
            }
            servletContextCustomizers.forEach(c -> c.accept(context));

            if (additionalHandlers.isEmpty()) {
                jettyServer.setHandler(context);
            } else {
                final HandlerCollection collection = new HandlerCollection();
                additionalHandlers.forEach(collection::addHandler);
                collection.addHandler(context);
                jettyServer.setHandler(collection);
            }

            if (registerDefaultMXBeans) {
                registerMXBeans(appbaseServices.getCoreServices().appbaseCore());
            }

            return new JettyServletContainer(jettyServer, appbaseServices, resolvedPort);
        }
    }

    /**
     * Registers the standard set of Appbase JMX MXBeans on the given {@link AppbaseCore}.
     * Beans that are already registered are skipped (idempotent).
     *
     * @param appbaseCore the core Appbase instance to register beans on
     * @throws AppbaseRuntimeException if any MXBean registration fails
     */
    @SuppressWarnings("checkstyle:InnerTypeLast")
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
            if (!jmxService.isMXBeanRegistered(JmxLogServiceMXBean.class) &&
                    logService.getLogAccessor() != null)
            {
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
