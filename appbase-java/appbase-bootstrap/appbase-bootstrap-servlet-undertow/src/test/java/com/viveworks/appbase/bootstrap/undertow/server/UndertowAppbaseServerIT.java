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
package com.viveworks.appbase.bootstrap.undertow.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that boots an embedded Undertow server and asserts an HTTP endpoint
 * answers 200.
 *
 * <p>A trivial JAX-RS ping resource is used instead of the full appbase health endpoint
 * because the health endpoint requires a {@code HealthCheckService} instance to be present
 * in the Appbase service registry — wiring that is out of scope for a container-level boot
 * test. The purpose of this test is to prove that Undertow starts, accepts HTTP connections,
 * and routes requests through the Jersey {@code ServletContainer} correctly.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
final class UndertowAppbaseServerIT {

    /**
     * Trivial JAX-RS resource with no service dependencies, used only by this IT.
     * Proves that a JAX-RS resource registered via {@link ResourceConfig} is reachable
     * on the booted Undertow instance.
     */
    @Path("/ping")
    public static class PingResource {
        /**
         * Returns a plain-text {@code "pong"} body with HTTP 200.
         *
         * @return the pong body
         */
        @GET
        public String ping() {
            return "pong";
        }
    }

    @Test
    void pingEndpoint_returns200() throws Exception {
        final int port = freePort();
        final AppbaseServices services = UndertowTestServices.minimal();
        final UndertowServletContainer server = UndertowServletContainer.builder()
            .from(services)
            .port(port)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .build();
        server.start();
        try {
            final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ping"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(),
                "Undertow + Jersey must return 200 on GET /ping");
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2548_coreOpenApiAndCallerResourceAreBothServed() throws Exception {
        final int port = freePort();
        final UndertowServletContainer server = UndertowServletContainer.builder()
            .from(UndertowTestServices.minimal())
            .port(port)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .withAppbaseCoreServlets()
            .withAppbaseCoreOpenApi()
            .build();
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpResponse<String> openApi = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/openapi.json"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, openApi.statusCode(), openApi.body());
            final JsonNode document = new ObjectMapper().readTree(openApi.body());
            assertTrue(document.path("openapi").asText().startsWith("3."));
            final List<String> servedPaths = new ArrayList<>();
            document.path("paths").fieldNames().forEachRemaining(servedPaths::add);
            final String corePath = AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH;
            assertTrue(servedPaths.stream().anyMatch(path ->
                    path.equals(corePath) || path.startsWith(corePath + "/")),
                () -> "served paths: " + servedPaths);

            final HttpResponse<String> ping = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ping"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ping.statusCode(), "caller ResourceConfig registration must survive");
            assertEquals("pong", ping.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2574_coreOpenApiIsOffByDefaultAndCallerResourceIsServed() throws Exception {
        final int port = freePort();
        final UndertowServletContainer server = UndertowServletContainer.builder()
            .from(UndertowTestServices.minimal())
            .port(port)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .withAppbaseCoreServlets()
            .build();
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpResponse<String> openApi = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/openapi.json"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, openApi.statusCode(), openApi.body());

            final HttpResponse<String> status = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port
                    + AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertNotEquals(404, status.statusCode(),
                "core bootstrap must remain mounted while OpenAPI is off");

            final HttpResponse<String> ping = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ping"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ping.statusCode(), "caller ResourceConfig registration must survive");
            assertEquals("pong", ping.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void test_AC2574_coreOpenApiOptInWithoutCoreBootstrapIsInert() throws Exception {
        final int port = freePort();
        final UndertowServletContainer server = UndertowServletContainer.builder()
            .from(UndertowTestServices.minimal())
            .port(port)
            .resourceConfig(new ResourceConfig().register(PingResource.class))
            .withAppbaseCoreOpenApi()
            .build();
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();
            for (String path : new String[] {
                "/openapi.json", AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH
            }) {
                final HttpResponse<String> absent = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(404, absent.statusCode(), path);
            }
            final HttpResponse<String> ping = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ping"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ping.statusCode());
            assertEquals("pong", ping.body());
        } finally {
            server.stop();
        }
    }

    /**
     * Returns an available TCP port by briefly binding to port 0 and reading the
     * OS-assigned port before releasing the socket.
     */
    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
