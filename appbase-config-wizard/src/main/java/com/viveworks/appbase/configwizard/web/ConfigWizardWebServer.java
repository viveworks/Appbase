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

package com.viveworks.appbase.configwizard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.web.ConfigurationsApiResource;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.HostValidationFilter;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import com.viveworks.appbase.core.util.jackson.ObjectMapperConfigurator;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import jakarta.servlet.DispatcherType;
import javax.annotation.Nullable;
import java.net.BindException;
import java.net.URL;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * Boots a minimal embedded Jetty server for the Appbase Config Wizard web UI.
 *
 * <p>Mounts:
 * <ul>
 *   <li>{@code /api/*} → Jersey {@link ServletContainer} via {@link ResourceConfig}</li>
 *   <li>{@code /} → Jetty {@link DefaultServlet} serving {@code /static/web/} from the classpath</li>
 * </ul>
 *
 * <p>No authentication or session management — this is a localhost developer tool only.
 * Uses port {@code 0} in tests for an OS-assigned ephemeral port; call {@link #getActualPort()}
 * after {@link #start()} has bound the port (i.e. from another thread).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ConfigWizardWebServer {

    /** Maximum number of sequential ports to try when the preferred port is in use. */
    private static final int MAX_PORT_ATTEMPTS = 10;

    /**
     * The only interface this server binds. The config wizard has no authentication, so binding
     * anything wider exposes an unauthenticated file-write API to the network (internal work item 751).
     * Deliberately not configurable.
     */
    private static final String LOOPBACK_HOST = "127.0.0.1";

    // Must match swagger-ui.version in root pom.xml depMgmt
    private static final String SWAGGER_UI_VERSION = "5.9.0";

    private final int port;
    private final ConfigScanner scanner;
    private final FieldPrompter prompter;
    private final YamlWriter writer;
    private final PathConfinement confinement;

    /**
     * The unguessable per-run browser token minted once at construction. Delivered to the UI as a
     * same-site cookie ({@link BrowserTokenCookieFilter}) and required back on every {@code /api}
     * request ({@link BrowserTokenRequestFilter}) — the token half of the A10-01 (internal work item 1449) defence.
     */
    private final String browserToken = BrowserToken.newToken();

    @Nullable
    private volatile Server jettyServer;

    /**
     * Production constructor — creates all dependencies with default configurations.
     *
     * @param port the TCP port to listen on; use {@code 0} for an OS-assigned ephemeral port
     * @param root the only directory tree the API may read from or write to
     */
    public ConfigWizardWebServer(final int port, final Path root) {
        this(port,
                new ConfigScanner(),
                new FieldPrompter(fieldPrompterMapper()),
                new YamlWriter(YamlUtil.getYamlMapper(), PathConfinement.of(root)),
                PathConfinement.of(root));
    }

    /**
     * Package-private testing constructor — all dependencies injected for easier test control.
     *
     * @param port    the TCP port to listen on; use {@code 0} for an OS-assigned ephemeral port
     * @param scanner config-class scanner
     * @param prompter interactive field prompter
     * @param writer   YAML serialiser
     * @param confinement bounds every caller-supplied path
     */
    ConfigWizardWebServer(final int port,
                          final ConfigScanner scanner,
                          final FieldPrompter prompter,
                          final YamlWriter writer,
                          final PathConfinement confinement)
    {
        this.port = port;
        this.scanner = scanner;
        this.prompter = prompter;
        this.writer = writer;
        this.confinement = confinement;
    }

    /**
     * Builds the {@link ObjectMapper} used by this server's {@link FieldPrompter}.
     *
     * <p>Measured 2026-10-08 (internal work item 2536 L-10): this mapper MUST go through
     * {@link ObjectMapperConfigurator#configure}, the same call {@link ConfigurationsApiResource}'s
     * own mapper gets for {@code GET /default}. Before this fix, {@code FieldPrompter} used a plain,
     * unconfigured mapper, so a polymorphic field's default value round-tripped through
     * {@code GET /default} (configured mapper, names subtypes by fully-qualified class name per
     * {@link ObjectMapperConfigurator#registerSubType}) could not be read back by
     * {@code POST /build} (plain mapper, only recognising the short names
     * {@code @JsonSubTypes} declares statically) — reproduced exactly as
     * {@code DefaultTracerServiceConfiguration}'s {@code jaegerSamplerConfiguration} failing with
     * Jackson's {@code InvalidTypeIdException}. Configuring this mapper the same way keeps both
     * endpoints on one naming convention; the static {@code @JsonSubTypes} short names remain
     * readable too; see {@link ObjectMapperConfigurator}'s own subtype registrations.
     *
     * @return a mapper configured identically to {@link ConfigurationsApiResource}'s
     */
    private static ObjectMapper fieldPrompterMapper() {
        final ObjectMapper mapper = new ObjectMapper()
                // findAndRegisterModules registers the Scala module (from transitive Spark deps)
                // so that Scala maps produced by Jersey's body reader are serialized correctly
                // during convertValue() in buildFromMap(), rather than as bean POJOs.
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ObjectMapperConfigurator.configure(mapper);
        return mapper;
    }

    /**
     * The per-run browser token this server requires on {@code /api} requests. Package-private so the
     * contract and HTTP tests can present the same token the running assembly minted; there is no
     * production reason to read it.
     *
     * @return the per-run browser token
     */
    String browserToken() {
        return browserToken;
    }

    /**
     * Tries to bind the embedded Jetty server to the preferred port, automatically
     * incrementing the port on {@link BindException} up to {@value #MAX_PORT_ATTEMPTS} times.
     *
     * <p>Returns once the server is bound and accepting connections. Call {@link #join()} to
     * block until the server is stopped, or {@link #start()} as a convenience that does both.
     *
     * @return the actual TCP port the server bound to
     * @throws Exception when no port in the range {@code port..port+MAX_PORT_ATTEMPTS-1} is free,
     *                   or when Jetty fails to start for a non-port reason
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public int bind() throws Exception {
        Exception lastError = null;
        for (int attempt = 0; attempt < MAX_PORT_ATTEMPTS; attempt++) {
            final int tryPort = port + attempt;
            final Server server = buildServer(tryPort);
            try {
                server.start();
                jettyServer = server;
                return getActualPort();
            } catch (Exception ex) {
                stopQuietly(server);
                if (isCausedByBindException(ex)) {
                    lastError = ex;
                } else {
                    throw ex;
                }
            }
        }
        throw new Exception(
                "Could not bind to any port in range " + port + "–" + (port + MAX_PORT_ATTEMPTS - 1),
                lastError);
    }

    /**
     * Blocks until the server is stopped (via {@link #stop()}).
     * Must be called after {@link #bind()} has returned successfully.
     *
     * @throws InterruptedException when the thread is interrupted while waiting
     */
    public void join() throws InterruptedException {
        final Server server = jettyServer;
        if (server != null) {
            server.join();
        }
    }

    /**
     * Convenience method — binds and then blocks until stopped.
     * Equivalent to {@code bind(); join()}.
     *
     * @throws Exception when Jetty fails to start or no port is available
     */
    public void start() throws Exception {
        bind();
        join();
    }

    /**
     * Builds the Jersey {@link ResourceConfig} that this server serves at {@code /api/*}.
     *
     * <p>⚠️ Package-private <strong>so tests can exercise the assembly that actually ships.</strong>
     * Before internal work item 751 the contract test built its own wiring via a separate JAX-RS {@code Application}
     * class while production built this one inline, so the two could — and did — diverge. That is
     * how a second unconfined write endpoint (F-08) reached {@code develop} with a green suite.
     * That class was deleted in internal work item 806 R-6 once nothing constructed it.
     *
     * @param jerseyMapper the ObjectMapper Jersey uses for request and response bodies
     * @return the resource configuration, including the confinement this server was built with
     */
    ResourceConfig buildResourceConfig(final ObjectMapper jerseyMapper) {
        final ConfigurationsApiResource resource =
                new ConfigurationsApiResource(scanner, prompter, writer, confinement);
        return new ResourceConfig()
                .register(new JacksonJsonProvider(jerseyMapper))
                // Fail closed on non-loopback Host / cross-origin requests before any resource runs: the
                // wizard is unauthenticated and loopback-bound, so this is the DNS-rebinding boundary
                // (A10-01, internal work item 1449). Registered here so the assembly that ships is the one under test.
                .register(new DnsRebindingGuardRequestFilter())
                // Defence-in-depth: require the unguessable per-run token an attacker page cannot read
                // from the SameSite cookie (A10-01, internal work item 1449). Registered in the shipping assembly.
                .register(new BrowserTokenRequestFilter(browserToken))
                .register(ConfigurationsApiResource.class)
                .register(OpenApiResource.class)
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(resource).to(ConfigurationsApiResource.class);
                    }
                });
    }

    /**
     * Builds and configures a Jetty {@link Server} on the given port without starting it.
     *
     * <p>Mounts:
     * <ul>
     *   <li>{@code /api/swagger-ui/*} → Swagger UI static files (webjar + custom initializer)</li>
     *   <li>{@code /api/*} → Jersey {@link ServletContainer} including OpenAPI JSON endpoint</li>
     *   <li>{@code /} → Jetty {@link DefaultServlet} serving Flutter web UI from the classpath</li>
     * </ul>
     *
     * @param listenPort the TCP port to configure
     * @return a configured but not yet started server
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Server buildServer(final int listenPort) {
        // ⚠️ new Server(listenPort) binds 0.0.0.0. This module has no authentication of any kind,
        // so a wider bind exposes an unauthenticated file-write API to the whole network (internal work item 751
        // F-01). Deliberately not configurable — there is no flag to widen it.
        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost(LOOPBACK_HOST);
        connector.setPort(listenPort);
        server.addConnector(connector);

        // Configure OpenAPI context so the swagger scanner knows which packages to inspect
        final SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(new OpenAPI()
                        .info(new Info()
                                .title("Appbase Config Wizard API")
                                .description("REST API for discovering, configuring, and generating YAML " +
                                        "configuration files for Appbase services.")
                                .version("1.0.0")))
                .prettyPrint(true)
                .resourcePackages(Set.of("com.viveworks.appbase.configwizard.configurations.web"));
        try {
            new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(oasConfig)
                    .buildContext(true);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialise OpenAPI context", ex);
        }

        // Use an explicit ObjectMapper for Jersey's body reader/writer so that
        // jackson-module-scala (a transitive Spark dependency) is NOT auto-discovered.
        // Without this, JSON objects arrive as scala.collection.immutable.Map, which
        // causes the FieldPrompter mapper to serialise them as POJOs and expose
        // internal Scala trait methods (e.g. isTraversableAgain) as JSON properties.
        final ObjectMapper jerseyMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final ResourceConfig resourceConfig = buildResourceConfig(jerseyMapper);

        // Main context: Jersey at /api/* + static Flutter web files at /
        final ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        // SEC-1: first filter on every request, before Jersey or the static servlet see it.
        // A loopback bind alone does not stop DNS rebinding -- see HostValidationFilter's javadoc.
        context.addFilter(new org.eclipse.jetty.servlet.FilterHolder(new HostValidationFilter()),
                "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
        // Plant the per-run token as a same-site cookie on the app-shell responses so the UI can echo
        // it back on /api calls (A10-01, internal work item 1449). Mapped across the context; the filter itself skips
        // /api paths, which are guarded by BrowserTokenRequestFilter instead.
        context.addFilter(new FilterHolder(new BrowserTokenCookieFilter(browserToken)),
                "/*", EnumSet.of(DispatcherType.REQUEST));

        final ServletHolder jerseyHolder = new ServletHolder(new ServletContainer(resourceConfig));
        context.addServlet(jerseyHolder, "/api/*");

        final Resource staticBase = Resource.newClassPathResource("/static/web/");
        if (staticBase != null) {
            context.setBaseResource(staticBase);
        }
        final ServletHolder staticHolder = new ServletHolder("default", DefaultServlet.class);
        staticHolder.setInitParameter("dirAllowed", "false");
        context.addServlet(staticHolder, "/");

        // Swagger UI context: webjar files with custom swagger-initializer.js override
        final String webjarsPath = "META-INF/resources/webjars/swagger-ui/" + SWAGGER_UI_VERSION + "/";
        final URL webjarsCheck = ConfigWizardWebServer.class.getClassLoader().getResource(webjarsPath + "swagger-ui.css");
        if (webjarsCheck == null) {
            throw new IllegalStateException(
                    "swagger-ui webjar not found at " + webjarsPath +
                            " — update SWAGGER_UI_VERSION constant to match pom.xml swagger-ui.version");
        }
        final ContextHandlerCollection handlers = new ContextHandlerCollection();
        final Resource swaggerWebjar = Resource.newClassPathResource(webjarsPath);
        if (swaggerWebjar != null) {
            final Resource customSwaggerDir = Resource.newClassPathResource("/static/swagger-ui/");
            final Resource swaggerBase = customSwaggerDir != null ?
                    new ResourceCollection(customSwaggerDir, swaggerWebjar) :
                    swaggerWebjar;
            final ServletContextHandler swaggerCtx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            swaggerCtx.setContextPath("/api/swagger-ui");
            swaggerCtx.setBaseResource(swaggerBase);
            // SEC-1: same Host check as the main context -- this context has its own filter chain.
            swaggerCtx.addFilter(new org.eclipse.jetty.servlet.FilterHolder(new HostValidationFilter()),
                    "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));
            // Swagger is served by a separate Jetty context, so it needs the same token bootstrap
            // cookie as the main app shell. Its requestInterceptor echoes the port-scoped value on
            // the protected OpenAPI and "Try it out" requests.
            swaggerCtx.addFilter(new FilterHolder(new BrowserTokenCookieFilter(browserToken)),
                    "/*", EnumSet.of(DispatcherType.REQUEST));
            final ServletHolder swaggerHolder = new ServletHolder("swagger-ui", DefaultServlet.class);
            swaggerHolder.setInitParameter("dirAllowed", "false");
            swaggerCtx.addServlet(swaggerHolder, "/");
            handlers.addHandler(swaggerCtx);
        }

        handlers.addHandler(context);
        server.setHandler(handlers);
        return server;
    }

    private static boolean isCausedByBindException(final Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof BindException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void stopQuietly(final Server server) {
        try {
            server.stop();
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    /**
     * Stops the embedded Jetty server.
     *
     * <p>Calling this from any thread will cause the blocking {@link #start()} call to return.
     *
     * @throws Exception when Jetty fails to stop cleanly
     */
    public void stop() throws Exception {
        final Server server = jettyServer;
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Returns the TCP port the server is currently bound to.
     *
     * <p>When the server was constructed with port {@code 0} (ephemeral), this method
     * returns the OS-assigned port after {@link #start()} has completed its
     * {@code server.start()} call (i.e. once the server is accepting connections).
     * Returns the configured port (which may be {@code 0} or {@code -1}) when the server
     * has not yet started.
     *
     * @return the bound port, or {@code 0} / {@code -1} when not yet started
     */
    public int getActualPort() {
        final Server server = jettyServer;
        if (server != null) {
            for (Connector connector : server.getConnectors()) {
                if (connector instanceof ServerConnector serverConnector) {
                    return serverConnector.getLocalPort();
                }
            }
        }
        return port;
    }

    /**
     * Returns the URL of the running server, e.g. {@code http://localhost:8080}.
     * Only valid after {@link #start()} has been called.
     *
     * @return server URL string
     * @since 1.0.0
     */
    public String url() {
        return "http://localhost:" + getActualPort();
    }
}
