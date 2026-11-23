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

package com.viveworks.appbase.bootstrap.jetty.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.CoreServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.StatusServlet;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 */
class JettyServletContainerTest {

    @Path("/ping")
    public static class PingResource {
        @GET
        public String ping() {
            return "pong";
        }
    }

    @Test
    void builder_withoutFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> JettyServletContainer.builder().build());
    }

    @Test
    void builder_defaultPort_buildsSuccessfully() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .build();
        assertNotNull(server);
    }

    @Test
    void builder_customServer_usesProvidedServer() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        Server customServer = new Server(9090);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .server(customServer)
            .build();
        assertNotNull(server);
        assertSame(customServer, server.getServer());
    }

    @Test
    void builder_port_overridesDefaultPort() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .port(9000)
            .build();
        assertNotNull(server);
    }

    @Test
    void builder_serverAndPort_throwsIllegalStateException() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        assertThrows(IllegalStateException.class, () ->
            JettyServletContainer.builder()
                .from(mockServices)
                .server(new Server(9090))
                .port(8080)
                .build()
        );
    }

    @Test
    void getAppbaseServices_returnsProvidedServices() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .build();
        assertSame(mockServices, server.getAppbaseServices());
    }

    @Test
    void getServer_nonNullAfterBuild() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .build();
        assertNotNull(server.getServer());
    }

    @Test
    void withAdditionalHandler_handlerAppearsInServerChain() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        Handler extra = mock(Handler.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .withAdditionalHandler(extra)
            .build();
        Handler root = server.getServer().getHandler();
        assertTrue(root instanceof HandlerCollection,
            "server root handler should be HandlerCollection when additional handlers are present");
        boolean found = Arrays.asList(((HandlerCollection) root).getHandlers()).contains(extra);
        assertTrue(found, "additional handler must appear in the HandlerCollection");
    }

    @Test
    void withServletContext_customizerIsApplied() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .withServletContext(ctx -> ctx.setAttribute("testKey", "testValue"))
            .build();
        Handler root = server.getServer().getHandler();
        ServletContextHandler context = (root instanceof HandlerCollection)
            ? (ServletContextHandler) Arrays.stream(((HandlerCollection) root).getHandlers())
                .filter(h -> h instanceof ServletContextHandler).findFirst().orElseThrow()
            : (ServletContextHandler) root;
        assertEquals("testValue", context.getAttribute("testKey"));
    }

    @Test
    void withoutAdditionalHandlers_rootHandlerIsServletContextHandler() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .build();
        assertTrue(server.getServer().getHandler() instanceof ServletContextHandler,
            "without additional handlers, root handler must be ServletContextHandler directly");
    }

    @Test
    void test_AC2548_coreBootstrapServesCoreOnlyOpenApiDocument() throws Exception {
        final AppbaseServices mockServices = mock(AppbaseServices.class);
        final Server jetty = new Server();
        final ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        jetty.addConnector(connector);
        final JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .server(jetty)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .withAppbaseCoreServlets()
            .withAppbaseCoreOpenApi()
            .build();

        try {
            server.start();
            final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/openapi.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());

            final JsonNode document = new ObjectMapper().readTree(response.body());
            assertTrue(document.path("openapi").asText().startsWith("3."));
            assertEquals("Appbase Core REST API", document.path("info").path("title").asText());
            final JsonNode paths = document.path("paths");
            for (String required : new String[] {
                AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_JMX_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_LOG_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_METRIC_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_MEDIATOR_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_EXCEPTION_RESTAPI_PATH,
                AppbaseServiceConstants.DEFAULT_CONFIGURATION_RESTAPI_PATH
            }) {
                assertTrue(hasPathStartingWith(paths, required), "missing core path: " + required);
            }
            assertFalse(hasPathStartingWith(paths,
                AppbaseServiceConstants.DEFAULT_HEALTH_CHECK_RESTAPI_PATH),
                "core bootstrap must not register service-level health OpenAPI");

            final HttpResponse<String> ping = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ping.statusCode(), "caller ResourceConfig registration must survive");
            assertEquals("pong", ping.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2574_setOnlyCoreBootstrapServesEffectiveOpenApiDocument() throws Exception {
        final AppbaseServices mockServices = mock(AppbaseServices.class);
        final Server jetty = new Server();
        final ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        jetty.addConnector(connector);
        final JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .server(jetty)
            .withAppbaseCoreServlets(Set.of(CoreServlet.class))
            .withAppbaseCoreOpenApi()
            .build();

        try {
            server.start();
            final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/openapi.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());

            final JsonNode document = new ObjectMapper().readTree(response.body());
            assertTrue(document.path("openapi").asText().startsWith("3."));
            final JsonNode paths = document.path("paths");
            assertFalse(hasPathStartingWith(paths, AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH),
                "the Set-only overload must exclude the requested core servlet");
            assertTrue(hasPathStartingWith(paths, AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH),
                "the Set-only overload must retain non-excluded core servlets");
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2548_customCoreBootstrapDocumentsEffectiveServletSetAndPaths() throws Exception {
        final AppbaseServices mockServices = mock(AppbaseServices.class);
        final Server jetty = new Server();
        final ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        jetty.addConnector(connector);
        final JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .server(jetty)
            .withAppbaseCoreServlets(
                Set.of(CoreServlet.class), Map.of(StatusServlet.class, "/custom-status"))
            .withAppbaseCoreOpenApi()
            .build();

        try {
            server.start();
            final JsonNode paths = new ObjectMapper().readTree(HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/openapi.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body()).path("paths");
            assertFalse(hasPathStartingWith(paths, AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH),
                "an excluded core servlet must not remain in OpenAPI");
            assertFalse(hasPathStartingWith(paths, AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH),
                "an overridden core servlet must not retain its default OpenAPI path");
            assertTrue(hasPathStartingWith(paths, "/custom-status"),
                "OpenAPI must use the effective status servlet path");
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2574_noArgCoreOpenApiIsOffByDefaultAndCallerResourceIsServed()
            throws Exception {
        assertCoreOpenApiOffByDefault(
            JettyServletContainer.Builder::withAppbaseCoreServlets,
            AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH,
            "no-argument core overload");
    }

    @Test
    void test_AC2574_coreOpenApiIsOffByDefaultAndCallerResourceIsServed() throws Exception {
        assertCoreOpenApiOffByDefault(
            builder -> builder.withAppbaseCoreServlets(Set.of(CoreServlet.class)),
            AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH,
            "Set-only core overload");
    }

    @Test
    void test_AC2574_setAndMapCoreOpenApiIsOffByDefaultAndCallerResourceIsServed()
            throws Exception {
        assertCoreOpenApiOffByDefault(
            builder -> builder.withAppbaseCoreServlets(
                Set.of(CoreServlet.class), Map.of(StatusServlet.class, "/custom-status")),
            "/custom-status",
            "Set-and-Map core overload");
    }

    private static void assertCoreOpenApiOffByDefault(
            final Consumer<JettyServletContainer.Builder> configureCoreServlets,
            final String expectedStatusPath,
            final String overloadDescription) throws Exception {
        final AppbaseServices mockServices = mock(AppbaseServices.class);
        final Server jetty = new Server();
        final ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        jetty.addConnector(connector);
        final JettyServletContainer.Builder builder = JettyServletContainer.builder()
            .from(mockServices)
            .server(jetty)
            .resourceConfig(new ResourceConfig().register(PingResource.class));
        configureCoreServlets.accept(builder);
        final JettyServletContainer server = builder.build();

        try {
            server.start();
            final HttpClient client = HttpClient.newHttpClient();
            final HttpResponse<String> openApi = client.send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/openapi.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(404, openApi.statusCode(), openApi.body());

            final HttpResponse<String> status = client.send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort()
                        + expectedStatusPath)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertNotEquals(404, status.statusCode(),
                overloadDescription + " must still install its effective status servlet");

            final HttpResponse<String> ping = client.send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ping.statusCode(),
                "caller ResourceConfig registration must survive " + overloadDescription);
            assertEquals("pong", ping.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2574_coreOpenApiOptInWithoutCoreBootstrapIsInert() throws Exception {
        final AppbaseServices mockServices = mock(AppbaseServices.class);
        final Server jetty = new Server();
        final ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        jetty.addConnector(connector);
        final JettyServletContainer server = JettyServletContainer.builder()
            .from(mockServices)
            .server(jetty)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .withAppbaseCoreOpenApi()
            .build();

        try {
            server.start();
            final HttpClient client = HttpClient.newHttpClient();
            for (String path : new String[] {
                "/openapi.json", AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH
            }) {
                final HttpResponse<String> absent = client.send(
                    HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + server.getPort() + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                assertEquals(404, absent.statusCode(), path);
            }
            final HttpResponse<String> ping = client.send(
                HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + server.getPort() + "/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ping.statusCode());
            assertEquals("pong", ping.body());
        } finally {
            server.stop();
        }
    }

    private static boolean hasPathStartingWith(final JsonNode paths, final String prefix) {
        for (Iterator<String> names = paths.fieldNames(); names.hasNext();) {
            if (names.next().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
