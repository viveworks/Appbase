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

package com.viveworks.appbase.service.web.oauth.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the production {@link GitHubUserApi} over the JDK HTTP client, exercised against a real
 * loopback {@link HttpServer} so the header it sends and the status/body it returns are asserted end to
 * end, with no network.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class JdkGitHubUserApiTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsBearerTokenAndReturnsStatusAndBody() throws IOException {
        AtomicReference<String> seenAuthHeader = new AtomicReference<>();
        String body = "{\"id\":42,\"login\":\"octocat\"}";
        URI endpoint = startServer(exchange -> {
            seenAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, body);
        });

        GitHubUserApi.Response response =
                new JdkGitHubUserApi(endpoint, Duration.ofSeconds(5)).fetch("gho_secret-token");

        assertEquals("Bearer gho_secret-token", seenAuthHeader.get());
        assertEquals(200, response.statusCode());
        assertEquals(body, response.body());
    }

    @Test
    void propagatesUpstreamRejectionStatus() throws IOException {
        URI endpoint = startServer(exchange -> respond(exchange, 401, "{\"message\":\"Bad credentials\"}"));

        GitHubUserApi.Response response =
                new JdkGitHubUserApi(endpoint, Duration.ofSeconds(5)).fetch("revoked");

        assertEquals(401, response.statusCode());
    }

    @Test
    void unreachableEndpointThrowsTransportException() throws IOException {
        // Bind then immediately release the port so the address is routable but refuses the connection.
        URI endpoint = startServer(exchange -> respond(exchange, 200, "{}"));
        server.stop(0);
        server = null;

        GitHubUserApi api = new JdkGitHubUserApi(endpoint, Duration.ofSeconds(2));
        assertThrows(GitHubUserApiException.class, () -> api.fetch("t"));
    }

    private URI startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/user", handler);
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/user");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
