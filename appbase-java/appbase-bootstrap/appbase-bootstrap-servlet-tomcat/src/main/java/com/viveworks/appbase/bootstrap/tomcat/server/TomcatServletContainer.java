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

package com.viveworks.appbase.bootstrap.tomcat.server;

import com.viveworks.appbase.bootstrap.servlet.AppbaseCoreOpenApiRegistrar;
import com.viveworks.appbase.bootstrap.servlet.AppbaseServletRegistrar;
import com.viveworks.appbase.bootstrap.servlet.JerseyResourceConfigCompatibility;
import com.viveworks.appbase.bootstrap.server.AppbaseWebServer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Tomcat-based {@link AppbaseWebServer}. Boots an embedded Tomcat with the
 * standard Appbase servlets/filters and a Jersey {@link ServletContainer} for
 * JAX-RS. A separate opt-in exposes the core-only OpenAPI document;
 * MXBean and extra-handler support remain out of scope.
 *
 * <p>Build via:
 * <pre>{@code
 * TomcatServletContainer server = TomcatServletContainer.builder()
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
public final class TomcatServletContainer implements AppbaseWebServer {

    static final int DEFAULT_PORT = 8080;
    static final String DEFAULT_HOST = "0.0.0.0";

    private final Tomcat tomcat;
    private final AppbaseServices appbaseServices;
    private final int port;

    private boolean started;

    private TomcatServletContainer(final Tomcat tomcat,
                                   final AppbaseServices appbaseServices,
                                   final int port)
    {
        this.tomcat = tomcat;
        this.appbaseServices = appbaseServices;
        this.port = port;
    }

    /**
     * Starts the Tomcat server and begins accepting connections.
     *
     * @throws AppbaseRuntimeException if the server fails to start
     */
    @Override
    public void start() {
        try {
            tomcat.start();
            started = true;
        } catch (final LifecycleException exception) {
            throw new AppbaseRuntimeException(
                "Failed to start TomcatServletContainer", exception);
        }
    }

    /**
     * Stops the Tomcat server, destroys the Catalina engine, and shuts down all
     * Appbase services. Stop/destroy exceptions are wrapped in {@link AppbaseRuntimeException};
     * service shutdown runs unconditionally in the finally block.
     *
     * @throws AppbaseRuntimeException if the server fails to stop
     */
    @Override
    public void stop() {
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (final LifecycleException exception) {
            throw new AppbaseRuntimeException(
                "Failed to stop TomcatServletContainer", exception);
        } finally {
            started = false;
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
     * Returns a new builder for {@link TomcatServletContainer}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TomcatServletContainer}.
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
         * Sets the HTTP port (default {@value TomcatServletContainer#DEFAULT_PORT}).
         *
         * @param port TCP port to listen on
         * @return this builder
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the bind host (default {@value TomcatServletContainer#DEFAULT_HOST}).
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
         * Builds (but does not start) the {@link TomcatServletContainer}.
         *
         * <p>Jersey is wired via a pre-configured {@link ServletContainer} instance so
         * any resources already registered on the {@link ResourceConfig} are active —
         * no init-param class-name indirection.
         *
         * @return the configured, not yet started, server
         * @throws NullPointerException if {@link #from} was not called
         * @throws IllegalStateException if package scanning is configured after Java 25, or if
         *                               future-Jersey compatibility cannot be inspected safely
         */
        public TomcatServletContainer build() {
            Objects.requireNonNull(appbaseServices,
                "appbaseServices required — call from(AppbaseServices) before build()");
            final ResourceConfig rc = resourceConfig != null ? resourceConfig : new ResourceConfig();
            if (coreServlets && coreOpenApi) {
                AppbaseCoreOpenApiRegistrar.register(rc);
            }
            JerseyResourceConfigCompatibility.validate(rc);

            final Tomcat tomcat = new Tomcat();
            tomcat.setBaseDir(System.getProperty("java.io.tmpdir"));
            tomcat.setPort(port);
            tomcat.getConnector().setProperty("address", host);

            final Context ctx = tomcat.addContext("", System.getProperty("java.io.tmpdir"));

            // Wire Jersey via an instance so the pre-configured ResourceConfig is used directly.
            Tomcat.addServlet(ctx, "jersey", new ServletContainer(rc));
            ctx.addServletMappingDecoded("/*", "jersey");

            final TomcatServletDeployment deployment = new TomcatServletDeployment(ctx);

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

            return new TomcatServletContainer(tomcat, appbaseServices, port);
        }
    }
}
