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

package com.viveworks.appbase.bootstrap.undertow.server;

import com.viveworks.appbase.bootstrap.servlet.AppbaseCoreOpenApiRegistrar;
import com.viveworks.appbase.bootstrap.servlet.AppbaseServletRegistrar;
import com.viveworks.appbase.bootstrap.servlet.JerseyResourceConfigCompatibility;
import com.viveworks.appbase.bootstrap.server.AppbaseWebServer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Undertow-based {@link AppbaseWebServer}. Boots an embedded Undertow with the
 * standard Appbase servlets/filters and a Jersey {@link ServletContainer} for
 * JAX-RS. A separate opt-in exposes the core-only OpenAPI document;
 * MXBean and extra-handler support remain out of scope.
 *
 * <p>Build via:
 * <pre>{@code
 * UndertowServletContainer server = UndertowServletContainer.builder()
 *     .from(appbaseServices)
 *     .port(8080)
 *     .resourceConfig(myResourceConfig)
 *     .withAppbaseCoreServlets()
 *     .build();
 * server.start();
 * }</pre>
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
public final class UndertowServletContainer implements AppbaseWebServer {

    static final int DEFAULT_PORT = 8080;
    static final String DEFAULT_HOST = "0.0.0.0";

    private final Undertow server;
    private final DeploymentManager manager;
    private final AppbaseServices appbaseServices;
    private final int port;

    private boolean started;

    private UndertowServletContainer(final Undertow server, final DeploymentManager manager,
                                     final AppbaseServices appbaseServices, final int port)
    {
        this.server = server;
        this.manager = manager;
        this.appbaseServices = appbaseServices;
        this.port = port;
    }

    /**
     * Starts the Undertow server and begins accepting connections.
     *
     * @throws AppbaseRuntimeException if the server fails to start
     */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void start() {
        try {
            server.start();
            started = true;
        } catch (final Exception exception) {
            throw new AppbaseRuntimeException(
                "Failed to start UndertowServletContainer", exception);
        }
    }

    /**
     * Stops the Undertow server, undeploys the servlet deployment, and shuts down all
     * Appbase services. Server stop exceptions are wrapped in {@link AppbaseRuntimeException};
     * undeploy and service-shutdown run unconditionally in the finally block.
     *
     * @throws AppbaseRuntimeException if the server fails to stop
     */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void stop() {
        try {
            server.stop();
        } catch (final Exception exception) {
            throw new AppbaseRuntimeException(
                "Failed to stop UndertowServletContainer", exception);
        } finally {
            started = false;
            try {
                manager.undeploy();
            } catch (final Exception cleanupException) {
                // secondary failure — log but don't mask primary exception
            }
            appbaseServices.shutdown();
        }
    }

    /**
     * Returns the TCP port this server is configured to listen on.
     *
     * @return the port configured at build time
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * Returns {@code true} if the server has been started via {@link #start()}.
     *
     * @return {@code true} when the server is running
     */
    @Override
    public boolean isStarted() {
        return started;
    }

    /**
     * Returns the {@link AppbaseServices} container backing this server.
     *
     * @return the application services
     */
    public AppbaseServices getAppbaseServices() {
        return appbaseServices;
    }

    /**
     * Returns a new builder for {@link UndertowServletContainer}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UndertowServletContainer}.
     */
    public static final class Builder {

        @Nullable private AppbaseServices appbaseServices;
        @Nullable private ResourceConfig resourceConfig;
        private int port = DEFAULT_PORT;
        private String host = DEFAULT_HOST;
        private boolean coreServlets;
        private boolean coreOpenApi;
        private boolean serviceFilters;
        private boolean serviceServlets;

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
         * Sets the HTTP port (default {@value UndertowServletContainer#DEFAULT_PORT}).
         *
         * @param port TCP port to listen on
         * @return this builder
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the bind host (default {@value UndertowServletContainer#DEFAULT_HOST}).
         *
         * @param host bind address
         * @return this builder
         */
        public Builder host(final String host) {
            this.host = Objects.requireNonNull(host, "host");
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
         * Registers the standard set of Appbase core servlets at their default paths.
         *
         * @return this builder
         */
        public Builder withAppbaseCoreServlets() {
            this.coreServlets = true;
            return this;
        }

        /**
         * Exposes the core-only OpenAPI document when core servlets are also enabled.
         * This option is off by default and is inert without
         * {@link #withAppbaseCoreServlets()}.
         *
         * @return this builder
         */
        public Builder withAppbaseCoreOpenApi() {
            this.coreOpenApi = true;
            return this;
        }

        /**
         * Registers the standard set of Appbase service filters at {@code "/*"}.
         *
         * @return this builder
         */
        public Builder withAppbaseServiceFilters() {
            this.serviceFilters = true;
            return this;
        }

        /**
         * Registers the standard set of Appbase service servlets at their default paths.
         *
         * @return this builder
         */
        public Builder withAppbaseServiceServlets() {
            this.serviceServlets = true;
            return this;
        }

        /**
         * Builds (but does not start) the {@link UndertowServletContainer}.
         *
         * <p>Jersey is wired via an {@link ImmediateInstanceFactory} wrapping a pre-configured
         * {@link ServletContainer}, so any resources already registered on the
         * {@link ResourceConfig} are active — no init-param class-name indirection.
         *
         * @return the configured, not yet started, server
         * @throws NullPointerException     if {@link #from} was not called
         * @throws IllegalStateException    if package scanning is configured after Java 25, or if
         *                                  future-Jersey compatibility cannot be inspected safely
         * @throws AppbaseRuntimeException  if deployment or Undertow construction fails
         */
        @SuppressWarnings("checkstyle:IllegalCatch")
        public UndertowServletContainer build() {
            Objects.requireNonNull(appbaseServices,
                "appbaseServices required — call from(AppbaseServices) before build()");
            final ResourceConfig rc = resourceConfig != null ? resourceConfig : new ResourceConfig();
            if (coreServlets && coreOpenApi) {
                AppbaseCoreOpenApiRegistrar.register(rc);
            }
            JerseyResourceConfigCompatibility.validate(rc);

            final DeploymentInfo info = Servlets.deployment()
                .setClassLoader(UndertowServletContainer.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("appbase-" + System.nanoTime() + ".war");

            // Wire Jersey via an ImmediateInstanceFactory so the pre-configured ResourceConfig
            // instance is used directly, rather than instantiating a new one by class name.
            info.addServlet(
                Servlets.servlet("jersey", ServletContainer.class,
                    new ImmediateInstanceFactory<>(new ServletContainer(rc)))
                .addMapping("/*"));

            final UndertowServletDeployment deployment = new UndertowServletDeployment(info);

            if (coreServlets) {
                AppbaseServletRegistrar.addCoreServlets(deployment,
                    AppbaseServletRegistrar.emptyExcludes(),
                    AppbaseServletRegistrar.emptyPathOverrides());
            }
            if (serviceFilters) {
                AppbaseServletRegistrar.addServiceFilters(deployment,
                    AppbaseServletRegistrar.emptyExcludes());
            }
            if (serviceServlets) {
                AppbaseServletRegistrar.addServiceServlets(deployment,
                    AppbaseServletRegistrar.emptyExcludes(),
                    AppbaseServletRegistrar.emptyPathOverrides());
            }

            try {
                final DeploymentManager manager = Servlets.defaultContainer().addDeployment(info);
                manager.deploy();
                final Undertow undertow = Undertow.builder()
                    .addHttpListener(port, host)
                    .setHandler(manager.start())
                    .build();
                return new UndertowServletContainer(undertow, manager, appbaseServices, port);
            } catch (final Exception exception) {
                throw new AppbaseRuntimeException(
                    "Failed to build UndertowServletContainer", exception);
            }
        }
    }
}
