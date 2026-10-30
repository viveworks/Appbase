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

package com.viveworks.appbase.stacktest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ConfigurationJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.CoreJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ExceptionJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.JmxJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.LogJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MediatorJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.MetricJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.ServiceRegistryJaxrsResource;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.StatusJaxrsResource;
import com.viveworks.appbase.service.web.health.endpoint.jaxrs.HealthCheckJaxrsResource;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Opt-in local documentation capture of the REAL Appbase core JAX-RS resources.
 *
 * <p><strong>Label.</strong> The resources are the production classes from {@code appbase-service-web}
 * and they are served through the production {@link JettyServletContainer}. What is test-only is the
 * {@link OpenApiResource} registered on the {@link ResourceConfig} and the static Swagger UI page: no
 * core-service main code (appbase-core, appbase-service-*) registers an OpenAPI resource (internal work item 2548; the
 * configuration wizard's own web server does), so a stock core service does not answer
 * {@code /openapi.json}. A capture taken from this fixture documents the annotations of the real
 * resources; it does not show a stock service serving them. In the Swagger UI the operations are never
 * invoked ("try it out" is disabled) and no external service (Docker, database) is started.
 *
 * <p><strong>Exposure.</strong> The real core resources (including state-changing ones such as service
 * shutdown and reload, and log-store deletion) are served unauthenticated on 127.0.0.1 for the hold
 * window (at most 600 seconds). To limit that, {@link LoopbackReadOnlyFilter} is registered on every path:
 * it answers 403 to any request whose {@code Host} header is not exactly {@code 127.0.0.1:<the port the
 * request arrived on>} (a page that reaches the port through a DNS-rebinding name is refused) and to any
 * method other than GET and HEAD (so no state-changing call and no cross-origin preflight is answered).
 * The capture itself only issues GET requests to 127.0.0.1. What stays reachable is a GET of the
 * documentation and of the resources' read operations by any local process or by a page that requests
 * {@code http://127.0.0.1:<port>} directly. This is test-only and opt-in; run it on a machine whose
 * browser you do not use for other sites while it is up.
 *
 * <p>Opt-in only: it runs solely with {@code -Dstacktest.coreswagger.capture=true}, so normal CI and
 * local test runs skip it. Other properties: {@code stacktest.coreswagger.assets} (a directory holding
 * {@code swagger-ui-bundle.js} and {@code swagger-ui.css}), {@code stacktest.coreswagger.evidence} (a fresh
 * directory that receives {@code ready.url}, {@code openapi.json} and {@code harness.json}; the test waits for
 * {@code capture.done} in it) and optional {@code stacktest.coreswagger.holdSeconds} (1..600).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
@EnabledIfSystemProperty(named = "stacktest.coreswagger.capture", matches = "true")
class CoreSwaggerCaptureStackTest {

    private static final String CONFIG = "classpath:///appbase-core-swagger-capture.yaml";

    // EXTRA: lets the User Guide capture the real core resources without Docker or an external backend.
    @Test
    void test_EXTRA_liveCoreResourcesSwaggerCapture() throws Exception {
        final Path assets = Path.of(System.getProperty("stacktest.coreswagger.assets")).toRealPath();
        final Path evidence = Path.of(System.getProperty("stacktest.coreswagger.evidence")).toAbsolutePath();
        final int holdSeconds = Integer.getInteger("stacktest.coreswagger.holdSeconds", 180);
        Assertions.assertTrue(holdSeconds > 0 && holdSeconds <= 600, "capture hold must be 1..600 seconds");
        Assertions.assertTrue(Files.isRegularFile(assets.resolve("swagger-ui-bundle.js")));
        Assertions.assertTrue(Files.isRegularFile(assets.resolve("swagger-ui.css")));
        Files.createDirectories(evidence);
        final Path ready = evidence.resolve("ready.url");
        final Path stop = evidence.resolve("capture.done");
        Assertions.assertFalse(Files.exists(ready), "use a fresh evidence directory");
        Assertions.assertFalse(Files.exists(stop), "stale completion marker must not pass capture");

        // The real core resources (package-info lists nine) plus the health check resource. The user-account
        // and support-message resources are left out: they need stores this bare core does not build.
        final Class<?>[] resources = {CoreJaxrsResource.class, JmxJaxrsResource.class,
                ServiceRegistryJaxrsResource.class, StatusJaxrsResource.class, LogJaxrsResource.class,
                MetricJaxrsResource.class, MediatorJaxrsResource.class, ExceptionJaxrsResource.class,
                ConfigurationJaxrsResource.class, HealthCheckJaxrsResource.class};
        final Set<String> resourceNames = Arrays.stream(resources).map(Class::getName).collect(Collectors.toSet());
        new JaxrsOpenApiContextBuilder<>()
                .openApiConfiguration(new SwaggerConfiguration()
                        .openAPI(new OpenAPI().info(new Info()
                                // Same title as the @OpenAPIDefinition in the resources' package-info.
                                .title("Appbase Core REST API")
                                .version("1.0.0-SNAPSHOT")
                                .description("Real core JAX-RS resources documented through a test-only "
                                        + "OpenAPI resource (no core-service main code registers one, internal work item 2548). Backend "
                                        + "operations are not exercised by this local capture fixture.")))
                        .prettyPrint(true).resourceClasses(resourceNames))
                .buildContext(true);
        final ResourceConfig config = new ResourceConfig().registerClasses(resources)
                .register(OpenApiResource.class);

        final AppbaseCoreServices core = AppbaseCoreServices.builder().fromYaml(CONFIG).build();
        JettyServletContainer container = null;
        try {
            final AppbaseServices services = AppbaseServices.builder().from(core).build();
            final Server server = new Server();
            final ServerConnector connector = new ServerConnector(server);
            connector.setHost("127.0.0.1");
            connector.setPort(0);
            server.addConnector(connector);
            container = JettyServletContainer.builder()
                    .from(services)
                    .server(server)
                    .resourceConfig(config)
                    .withServletContext(context -> {
                        context.addFilter(new FilterHolder(new LoopbackReadOnlyFilter()), "/*",
                                EnumSet.of(DispatcherType.REQUEST));
                        context.setAttribute(AppbaseServiceConstants.APPBASE_CORE_ATTRIBUTE, core.appbaseCore());
                        final ServletHolder staticFiles = new ServletHolder(new DefaultServlet());
                        staticFiles.setInitParameter("resourceBase", assets.toString());
                        staticFiles.setInitParameter("pathInfoOnly", "true");
                        staticFiles.setInitParameter("dirAllowed", "false");
                        context.addServlet(staticFiles, "/swagger-assets/*");
                        context.addServlet(new ServletHolder(new HttpServlet() {
                            @Override
                            protected void doGet(final HttpServletRequest request,
                                                 final HttpServletResponse response) throws IOException {
                                response.setContentType("text/html;charset=UTF-8");
                                response.getWriter().write(UI_PAGE);
                            }
                        }), "/restapi/*");
                    })
                    .build();
            container.start();
            final String base = "http://127.0.0.1:" + container.getPort();
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
                final HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(URI.create(base + "/openapi.json"))
                                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString());
                Assertions.assertEquals(200, response.statusCode(), response.body());
                final JsonNode spec = new ObjectMapper().readTree(response.body());
                Assertions.assertTrue(spec.path("openapi").asText().startsWith("3."));
                Assertions.assertEquals("Appbase Core REST API", spec.path("info").path("title").asText());
                final JsonNode paths = spec.path("paths");
                for (String required : new String[]{AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_JMX_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_LOG_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_METRIC_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_MEDIATOR_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_EXCEPTION_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_CONFIGURATION_RESTAPI_PATH,
                        AppbaseServiceConstants.DEFAULT_HEALTH_CHECK_RESTAPI_PATH}) {
                    Assertions.assertTrue(hasPathStartingWith(paths, required),
                            "missing actual resource path: " + required);
                }
                int operations = 0;
                for (Iterator<JsonNode> it = paths.elements(); it.hasNext(); ) {
                    operations += it.next().size();
                }
                Files.writeString(evidence.resolve("openapi.json"), response.body());
                Files.writeString(evidence.resolve("harness.json"), "{\"title\":\"Appbase Core REST API\","
                        + "\"resourceClasses\":" + resources.length + ",\"paths\":" + paths.size()
                        + ",\"operations\":" + operations + "}\n");
            }
            Files.writeString(ready, base + "/restapi/\n");
            final long deadline = System.nanoTime() + Duration.ofSeconds(holdSeconds).toNanos();
            while (!Files.exists(stop) && System.nanoTime() < deadline) {
                Thread.sleep(200);
            }
            Assertions.assertTrue(Files.exists(stop), "capture did not finish before bounded hold expired");
        } finally {
            try {
                if (container != null) {
                    container.stop();
                }
            } finally {
                core.shutdown();
                Files.deleteIfExists(ready);
            }
        }
    }

    /**
     * Answers 403 to every request whose Host header is not exactly {@code 127.0.0.1:<local port>} and to every
     * method other than GET and HEAD. The port is read from the request (the connector's port is only known
     * after the server started), so a DNS-rebinding host name and any state-changing call are refused.
     */
    static final class LoopbackReadOnlyFilter implements Filter {
        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            final HttpServletRequest http = (HttpServletRequest) request;
            final String method = http.getMethod();
            final boolean readOnly = "GET".equals(method) || "HEAD".equals(method);
            final boolean ownHost = ("127.0.0.1:" + request.getLocalPort()).equals(http.getHeader("Host"));
            if (!readOnly || !ownHost) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN,
                        "capture fixture: only GET and HEAD to 127.0.0.1:<port>");
                return;
            }
            chain.doFilter(request, response);
        }
    }

    private static boolean hasPathStartingWith(final JsonNode paths, final String prefix) {
        for (Iterator<String> it = paths.fieldNames(); it.hasNext(); ) {
            if (it.next().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static final String UI_PAGE = """
            <!doctype html><html><head><meta charset="utf-8">
            <title>Appbase Core REST API - real core resources, test-only OpenAPI harness</title>
            <link rel="icon" href="data:,">
            <link rel="stylesheet" href="/swagger-assets/swagger-ui.css"></head>
            <body><p>Real Appbase core JAX-RS resources, served through a test-only OpenAPI harness because
            no core-service main code (appbase-core, appbase-service-*) registers an OpenApiResource (internal work item 2548; the
            configuration wizard's own web server does). In this page the operations are not invoked.</p>
            <div id="swagger-ui"></div><script src="/swagger-assets/swagger-ui-bundle.js"></script>
            <script>window.ui=SwaggerUIBundle({url:'/openapi.json',dom_id:'#swagger-ui',
            validatorUrl:null,supportedSubmitMethods:[],docExpansion:'list'});</script></body></html>
            """;
}
