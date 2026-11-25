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

import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for {@link ConfigWizardWebServer}: verifies the /api base path.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class ConfigWizardWebServerTest {
    private ConfigWizardWebServer server;
    private HttpClient http;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        server = new ConfigWizardWebServer(0, Path.of("."));
        int port = server.bind();
        baseUrl = "http://localhost:" + port;
        http = HttpClient.newHttpClient();
    }

    @AfterEach
    void stop() throws Exception { server.stop(); }

    /**
     * @param url   an absolute request URL
     * @param token the per-run browser token the running server minted
     * @return a GET request carrying the token in {@value BrowserTokenRequestFilter#TOKEN_HEADER}, as
     *         the wizard UI sends it (A10-01, internal work item 1449)
     */
    private static HttpRequest tokenedGet(final String url, final String token) {
        return HttpRequest.newBuilder(URI.create(url))
                .header(BrowserTokenRequestFilter.TOKEN_HEADER, token)
                .GET().build();
    }

    @Test
    void configurations_endpoint_returns_200() throws Exception {
        HttpResponse<String> r = http.send(
            tokenedGet(baseUrl + "/api/configurations", server.browserToken()),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
    }

    /**
     * A10-01 (per-run token): the running assembly rejects an {@code /api} request that omits the
     * per-run token, and the app shell hands that token out as a same-site cookie. Together these
     * prove {@link BrowserTokenRequestFilter} and {@link BrowserTokenCookieFilter} are wired into the
     * server that ships.
     *
     * @throws Exception on I/O or server failure
     */
    @Test
    void apiRequestWithoutTheBrowserTokenIsRejected() throws Exception {
        HttpResponse<String> r = http.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/api/configurations")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(403, r.statusCode(),
                "an /api request without the per-run browser token must be rejected");
    }

    @Test
    void theAppShellHandsOutTheBrowserTokenCookie() throws Exception {
        HttpResponse<String> r = http.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        String setCookie = r.headers().firstValue("Set-Cookie").orElse("");
        int port = URI.create(baseUrl).getPort();
        assertTrue(setCookie.contains(BrowserTokenCookieFilter.cookieName(port) + "=" + server.browserToken()),
                "the app shell must plant the per-run token cookie: " + setCookie);
        assertTrue(setCookie.contains("SameSite=Strict"),
                "the token cookie must be SameSite=Strict: " + setCookie);
    }

    @Test
    void concurrentInstancesUseDifferentPortScopedTokenCookies() throws Exception {
        ConfigWizardWebServer second = new ConfigWizardWebServer(0, Path.of("."));
        int secondPort = second.bind();
        try {
            int firstPort = URI.create(baseUrl).getPort();
            HttpResponse<String> firstResponse = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> secondResponse = http.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + secondPort + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            String firstCookie = firstResponse.headers().firstValue("Set-Cookie").orElse("");
            String secondCookie = secondResponse.headers().firstValue("Set-Cookie").orElse("");
            assertTrue(firstCookie.contains(
                    BrowserTokenCookieFilter.cookieName(firstPort) + "=" + server.browserToken()));
            assertTrue(secondCookie.contains(
                    BrowserTokenCookieFilter.cookieName(secondPort) + "=" + second.browserToken()));
            assertFalse(firstCookie.contains(BrowserTokenCookieFilter.cookieName(secondPort) + "="));
            assertFalse(secondCookie.contains(BrowserTokenCookieFilter.cookieName(firstPort) + "="));
        } finally {
            second.stop();
        }
    }

    @Test
    void swaggerUiBootstrapsItsTokenAndConfiguresTheRequestHeader() throws Exception {
        int port = URI.create(baseUrl).getPort();
        HttpResponse<String> index = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/swagger-ui/index.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> initializer = http.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/swagger-ui/swagger-initializer.js")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, index.statusCode());
        String setCookie = index.headers().firstValue("Set-Cookie").orElse("");
        assertTrue(setCookie.contains(
                BrowserTokenCookieFilter.cookieName(port) + "=" + server.browserToken()));
        assertEquals(200, initializer.statusCode());
        assertTrue(initializer.body().contains("X-Appbase-Wizard-Token"));
        assertTrue(initializer.body().contains("requestInterceptor"));
        assertTrue(initializer.body().contains("appbase_wizard_token_"));
    }

    @Test
    void old_api_configs_path_returns_404() throws Exception {
        HttpResponse<String> r = http.send(
            tokenedGet(baseUrl + "/api/configs", server.browserToken()),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r.statusCode());
    }

    @Test
    void openapi_json_returns_200_with_json_content_type() throws Exception {
        HttpResponse<String> r = http.send(
            tokenedGet(baseUrl + "/api/openapi.json", server.browserToken()),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("")
                    .contains("application/json"));
    }

    /**
     * Pins the served API metadata to the <em>programmatic</em> {@code SwaggerConfiguration}.
     *
     * <p>R-6. A second copy of this metadata lived on {@code ConfigWizardApiApplication} as an
     * {@code @OpenAPIDefinition} annotation. That class was never constructed, and the swagger
     * scanner's {@code resourcePackages} covers {@code ...configurations.web} only — not the package
     * the class lived in — so the annotation contributed nothing while reading as though it did.
     * This arm is what makes its deletion checkable rather than asserted: the served document must
     * be identical before and after.
     *
     * @throws Exception on I/O or server failure
     */
    @Test
    void openapi_json_carriesTheProgrammaticallyConfiguredInfo() throws Exception {
        HttpResponse<String> r = http.send(
            tokenedGet(baseUrl + "/api/openapi.json", server.browserToken()),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"title\" : \"Appbase Config Wizard API\""),
                "the title must come from SwaggerConfiguration, not from a scanned annotation");
        assertTrue(r.body().contains("\"version\" : \"1.0.0\""));
        assertTrue(r.body().contains("REST API for discovering, configuring, and generating YAML"),
                "the description is the programmatic one; the annotation's copy had drifted");
    }

    @Test
    void swagger_ui_index_returns_200() throws Exception {
        HttpResponse<String> r = http.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/api/swagger-ui/index.html")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
    }

    /**
     * Pins the initializer to the document path the server actually serves
     * (internal work item 2380 A-L1 / internal work item 1593 F01).
     *
     * <p>The wizard serves the OpenAPI document at {@code /api/openapi.json} (pinned by the arms
     * above) while the initializer still pointed at {@code /admin/openapi.json} — a path left over
     * from the appbase-admin rename — so Swagger UI loaded with no document at all. The initializer
     * is served verbatim from {@code static/swagger-ui/}; this arm asserts the bytes on the wire, so
     * the stale path can never silently return. (The report also mentioned "aligning the token
     * filter" for the {@code /admin} route: this module has no token filter, so there is nothing to
     * align. The browser-level W-SWAGGER arm is also pinned to the served {@code /api} route.)
     *
     * @throws Exception on I/O or server failure
     */
    @Test
    void swagger_ui_initializer_points_at_the_served_openapi_document() throws Exception {
        HttpResponse<String> r = http.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/api/swagger-ui/swagger-initializer.js")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("/api/openapi.json"),
                "the initializer must load the document the server serves at /api/openapi.json");
        assertFalse(r.body().contains("/admin/"),
                "the initializer must not reference the stale /admin/ path the server no longer serves");
    }

    @Test
    void boundServer_refusesConnectionsOnNonLoopbackAddress() throws Exception {
        InetAddress nonLoopback = firstNonLoopbackAddress();
        assumeTrue(nonLoopback != null, "host has no non-loopback address to test against");

        int port = Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));

        try (Socket loopback = new Socket()) {
            loopback.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2000);
            assertTrue(loopback.isConnected(), "loopback must still reach the server");
        }
        try (Socket external = new Socket()) {
            assertThrows(IOException.class,
                    () -> external.connect(new InetSocketAddress(nonLoopback, port), 2000),
                    "the server must not be reachable off-host");
        }
    }

    /**
     * End-to-end confinement check against the <em>real</em> server assembly.
     *
     * <p>⚠️ Every other confinement test constructs a resource directly. This one goes over HTTP
     * through {@link ConfigWizardWebServer}'s own wiring, so it is the only test that fails if the
     * production assembly is built unconfined. Mutation M6 of internal work item 751 survived until this existed.
     *
     * @throws Exception on I/O or server failure
     */
    @Test
    void write_outsideRoot_overHttp_returnsForbidden(@TempDir final Path tmp) throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outside = Files.createDirectory(tmp.resolve("outside"));
        Path target = outside.resolve("authorized_keys");

        ConfigWizardWebServer confined = new ConfigWizardWebServer(0, root);
        int port = confined.bind();
        try {
            String fqcn = new ConfigScanner().scan().get(0).fqcn();
            String body = "{\"fieldValues\":{},\"outputPath\":\"" + target.toAbsolutePath() + "\"}";

            HttpResponse<String> r = http.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:" + port + "/api/configurations/" + fqcn + "/write"))
                            .header("Content-Type", "application/json")
                            .header(BrowserTokenRequestFilter.TOKEN_HEADER, confined.browserToken())
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(403, r.statusCode(),
                    "the running server must refuse a write outside its --root");
            assertFalse(Files.exists(target), "a refused write must not create its target");
        } finally {
            confined.stop();
        }
    }

    /**
     * The same end-to-end check for a path that escapes only through a <em>dangling</em> symlink.
     *
     * <p>⚠️ Distinct from the arm above, which names an outside path directly. Here every string in
     * the request is inside the root; the escape happens in the filesystem. The confinement climbed
     * with {@code Files.exists}, which follows the link and reports false for a broken one, so the
     * link read as a segment that did not exist yet and the write followed it out (internal work item 806 R-4).
     *
     * @param tmp a scratch directory holding both the root and the link's outside target
     * @throws Exception on I/O or server failure
     */
    @Test
    void write_throughDanglingSymlinkInsideRoot_overHttp_returnsForbidden(@TempDir final Path tmp)
            throws Exception {
        Path root = Files.createDirectory(tmp.resolve("root"));
        Path outsideTarget = tmp.resolve("outside-target.yaml");
        Path link = root.resolve("escape");
        Files.createSymbolicLink(link, outsideTarget);
        assertFalse(Files.exists(outsideTarget), "the link must dangle for this to be the R-4 case");

        ConfigWizardWebServer confined = new ConfigWizardWebServer(0, root);
        int port = confined.bind();
        try {
            String fqcn = new ConfigScanner().scan().get(0).fqcn();
            String body = "{\"fieldValues\":{},\"outputPath\":\"" + link.toAbsolutePath() + "\"}";

            HttpResponse<String> r = http.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://127.0.0.1:" + port + "/api/configurations/" + fqcn + "/write"))
                            .header("Content-Type", "application/json")
                            .header(BrowserTokenRequestFilter.TOKEN_HEADER, confined.browserToken())
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(403, r.statusCode(),
                    "the running server must refuse a write that escapes through a dangling symlink");
            assertFalse(Files.exists(outsideTarget, LinkOption.NOFOLLOW_LINKS),
                    "a refused write must not create the link's target outside the root");
        } finally {
            confined.stop();
        }
    }

    /**
     * SEC-1 (CWE-350-adjacent DNS-rebinding): the unauthenticated loopback API performed no
     * Host/Origin validation. A browser visiting an attacker-controlled DNS name that resolves to
     * {@code 127.0.0.1} (a "DNS rebinding" attack) sends same-origin-looking requests whose
     * {@code Host} header names that attacker domain, not {@code localhost}/{@code 127.0.0.1} —
     * and before this fix, this server answered them exactly as it would a legitimate request.
     * Uses a raw socket (not {@link HttpClient}) because the JDK's {@code HttpClient} refuses to
     * let a caller set a non-matching {@code Host} header at all, which would make this exact
     * attack untestable through it.
     */
    @Test
    void request_withForeignHostHeader_isRejected() throws Exception {
        int port = Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));
        String response = rawHttpGet(port, "/api/configurations", "evil.example.com", server.browserToken());
        assertTrue(response.startsWith("HTTP/1.1 403"),
                "a request addressed to a non-loopback hostname must be rejected: " + response);
    }

    @Test
    void request_withMissingHostHeader_isRejected() throws Exception {
        int port = Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));
        String response = rawHttpGetWithNoHostHeader(port, "/api/configurations", server.browserToken());
        assertTrue(response.startsWith("HTTP/1.1 403") || response.startsWith("HTTP/1.1 400"),
                "a request with no Host header at all must not be treated as same-origin: " + response);
    }

    @Test
    void request_withLoopbackIpHostHeader_isAccepted() throws Exception {
        int port = Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));
        String response = rawHttpGet(port, "/api/configurations", "127.0.0.1:" + port, server.browserToken());
        assertTrue(response.startsWith("HTTP/1.1 200"),
                "127.0.0.1 (the server's own bound address) must be accepted: " + response);
    }

    @Test
    void request_withLocalhostHostHeader_isAccepted() throws Exception {
        int port = Integer.parseInt(baseUrl.substring(baseUrl.lastIndexOf(':') + 1));
        String response = rawHttpGet(port, "/api/configurations", "localhost:" + port, server.browserToken());
        assertTrue(response.startsWith("HTTP/1.1 200"),
                "localhost must be accepted: " + response);
    }

    /**
     * Sends a raw HTTP/1.1 GET with an explicit {@code Host} header value, bypassing
     * {@link HttpClient}'s restricted-header enforcement entirely.
     *
     * @param port             the server's bound loopback port
     * @param path             the request path
     * @param hostHeaderValue  the literal value to send as the {@code Host} header
     * @return the full raw response (status line and headers; body not required by these tests)
     * @throws IOException on socket failure
     */
    private static String rawHttpGet(final int port, final String path, final String hostHeaderValue,
            final String token) throws IOException {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + hostHeaderValue + "\r\n"
                + BrowserTokenRequestFilter.TOKEN_HEADER + ": " + token + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        return sendRaw(port, request);
    }

    /** Same as {@link #rawHttpGet}, but omits the {@code Host} header entirely. */
    private static String rawHttpGetWithNoHostHeader(final int port, final String path, final String token)
            throws IOException {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + BrowserTokenRequestFilter.TOKEN_HEADER + ": " + token + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        return sendRaw(port, request);
    }

    private static String sendRaw(final int port, final String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append('\n');
                }
            }
            return response.toString();
        }
    }

    /**
     * Finds an IPv4 address on a live, non-loopback interface.
     *
     * @return the first such address, or {@code null} when the host has none
     * @throws SocketException when the interface list cannot be read
     */
    private static InetAddress firstNonLoopbackAddress() throws SocketException {
        for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!nic.isUp() || nic.isLoopback()) {
                continue;
            }
            for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    return address;
                }
            }
        }
        return null;
    }
}
