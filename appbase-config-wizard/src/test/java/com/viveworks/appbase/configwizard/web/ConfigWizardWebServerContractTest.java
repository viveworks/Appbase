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

package com.viveworks.appbase.configwizard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;
import com.viveworks.appbase.configwizard.configurations.scan.ConfigScanner;
import com.viveworks.appbase.configwizard.configurations.writer.YamlWriter;
import com.viveworks.appbase.configwizard.security.PathConfinement;
import jakarta.ws.rs.core.Application;

import java.nio.file.Path;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HTTP-level contract tests for {@link ConfigurationsApiResource} via an in-process
 * Jersey/Grizzly2 test server.
 *
 * <p>These tests use the Jersey Test Framework (JUnit 5 integration) to spin up an in-process HTTP
 * server with the same {@link org.glassfish.jersey.server.ResourceConfig} production builds, and
 * verify the HTTP contract of the Configurations API without starting a full Jetty instance.
 *
 * <p><strong>Path note:</strong> targets are resource-relative (e.g. {@code configurations}), because
 * this test builds the assembly production builds. Production adds the {@code /api} prefix at the
 * Jetty servlet-mapping layer, not via an {@code @ApplicationPath} annotation. A
 * {@code ConfigWizardApiApplication} carrying that annotation used to exist alongside it; it was
 * deleted in internal work item 806 R-6 once nothing constructed it, because a second declaration of the API base path
 * is a place to fix a path bug that has no effect.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigWizardWebServerContractTest {

    /**
     * JerseyTest delegate — used for JUnit 5 compatibility because {@link JerseyTest}
     * is designed for JUnit 4 lifecycle annotations ({@code @Before}/{@code @After}).
     */
    private JerseyTest jerseyTest;

    /**
     * The server whose shipping assembly this test drives — held so the per-run browser token it
     * minted can be presented on {@code /api} requests (A10-01, internal work item 1449).
     */
    private ConfigWizardWebServer server;

    /**
     * Creates and starts the in-process Jersey/Grizzly2 test server before each test.
     *
     * @throws Exception if the Grizzly2 server fails to start
     */
    @BeforeEach
    void setUp() throws Exception {
        // ⚠️ Build the assembly PRODUCTION builds, not a parallel one. This test previously
        // constructed its own wiring via a separate JAX-RS Application class while
        // ConfigWizardWebServer built a different ResourceConfig inline, so the contract verified
        // here was not the contract that shipped — which is how the second unconfined write
        // endpoint (internal work item 751 F-08) reached develop untested.
        server = new ConfigWizardWebServer(0, Path.of("."));
        jerseyTest = new JerseyTest() {
            @Override
            protected Application configure() {
                return server.buildResourceConfig(new ObjectMapper());
            }
        };
        jerseyTest.setUp();
    }

    /**
     * @param path a resource-relative target (e.g. {@code configurations})
     * @return a request builder carrying the valid per-run browser token, as the wizard UI sends it
     */
    private jakarta.ws.rs.client.Invocation.Builder tokened(final String path) {
        return jerseyTest.target(path).request()
                .header(BrowserTokenRequestFilter.TOKEN_HEADER, server.browserToken());
    }

    /**
     * Stops the in-process test server after each test.
     *
     * @throws Exception if the Grizzly2 server fails to stop
     */
    @AfterEach
    void tearDown() throws Exception {
        jerseyTest.tearDown();
    }

    /**
     * Verifies that {@code GET /api/configurations} returns HTTP 200 and lists known
     * Appbase configuration classes.
     */
    @Test
    void getConfigurations_returns200() {
        Response r = tokened("configurations").get();
        assertEquals(200, r.getStatus(),
                "GET /api/configurations must return 200 OK");
    }

    /**
     * A10-01 (per-run token): proves {@link BrowserTokenRequestFilter} is wired into the production
     * assembly — a loopback, same-origin request that omits the token is still rejected, because an
     * attacker page that survives the Host/Origin checks cannot read the SameSite cookie to forge it.
     */
    @Test
    void aRequestWithoutTheBrowserTokenIsRejected() {
        Response r = jerseyTest.target("configurations").request().get();
        assertEquals(403, r.getStatus(),
                "a request missing the per-run browser token must be rejected (A10-01 token defence)");
    }

    /**
     * A10-01 (DNS rebinding): proves the {@link DnsRebindingGuardRequestFilter} is wired into the
     * production assembly and rejects a cross-origin request end to end. Uses {@code Referer} rather than
     * {@code Origin} because the JDK HttpURLConnection connector the Jersey test client uses silently
     * drops restricted headers ({@code Origin}, {@code Host}); {@code Referer} is delivered, and the
     * filter checks it when {@code Origin} is absent. The full header matrix (Host, Origin, IPv6,
     * missing-Host) is covered deterministically in {@code DnsRebindingGuardRequestFilterTest}.
     */
    @Test
    void aCrossOriginRefererRequestIsRejected() {
        Response r = tokened("configurations")
                .header("Referer", "http://evil.example/page").get();
        assertEquals(403, r.getStatus(),
                "a cross-origin request to the wizard API must be rejected (DNS-rebinding defence)");
    }

    /**
     * Verifies that {@code GET /api/configurations/{fqcn}} for an unknown class returns
     * HTTP 404 Not Found.
     */
    @Test
    void getConfigurations_unknown_returns404() {
        Response r = tokened("configurations/com.example.DoesNotExist").get();
        assertEquals(404, r.getStatus(),
                "GET /api/configurations/com.example.DoesNotExist must return 404");
    }

    /**
     * Verifies that {@code POST /api/configurations/{fqcn}/build} for an unknown class
     * returns HTTP 404 Not Found.
     */
    @Test
    void buildConfigurations_unknown_returns404() {
        Response r = tokened("configurations/com.example.DoesNotExist/build").post(null);
        assertEquals(404, r.getStatus(),
                "POST /api/configurations/com.example.DoesNotExist/build must return 404");
    }
}
