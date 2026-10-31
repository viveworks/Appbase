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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.service.factory.DefaultHealthCheckServiceFactory;
import com.viveworks.appbase.service.health.DefaultHealthCheckServiceConfiguration;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.user.account.UserAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end HTTP proof that {@code AppbaseServices.Builder}'s secure-by-default wiring (Tasks 1-2 --
 * {@code SecureByDefaultContextAuthorizer} plus the FORBIDDEN {@code
 * AuthorizationServiceExceptionConfiguration}) actually gates an admin read on the reactive face: a
 * real {@link AppbaseCoreServices} built from YAML, a real {@link AppbaseServices} built with the
 * production {@code from(...).build()} path (no stubbed authorizer, no bypassed filter), a real
 * {@link ReactorNettyAppbaseWebServer} started on an ephemeral port, and a real socket round trip via
 * reactor-netty's {@link HttpClient}. This is the deny test the Task 8 audit (D5) flagged as missing:
 * every prior reactive HTTP test either used an empty filter list or a mocked
 * {@link AppbaseServices}, so nothing had actually driven the assembled seven-filter chain, with a
 * real {@code SecureByDefaultContextAuthorizer} deciding, over the wire.
 *
 * <p><strong>Bare default -- no session/user-account scaffolding.</strong> The assembled chain runs
 * {@code UserSessionWebFilter} and {@code IdpUserTokenCheckWebFilter} on every request, ahead of
 * {@code AuthorizationWebFilter} -- regardless of secure-by-default. This test builds the BARE
 * default: a plain YAML core with NO {@code UserSessionFilterConfiguration} workaround and NO
 * {@link UserAccountService} wired at all. An anonymous request (no session cookie) resolves no
 * session; {@code UserSessionWebFilter} passes it through without a session attribute (its documented
 * no-session contract, and {@code DefaultUserSessionFilter} no longer eagerly requires a
 * user-account service on this path); {@code IdpUserTokenCheckWebFilter} then finds no {@code
 * UserSession} attribute and -- with the Task 9 fix -- continues the chain rather than throwing, so
 * the request reaches {@code AuthorizationWebFilter}. Nothing about the authorization decision is
 * stubbed: {@code SecureByDefaultContextAuthorizer} and {@code AuthorizationWebFilter} run unmodified.
 * This is the proof that secure-by-default works out of the box: anonymous {@code /health} -> 200,
 * anonymous {@code /configuration} -> 403.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactorNettyAppbaseWebServerSecureByDefaultTest {

    private static final String CONFIG = "classpath:///appbase-test-config.yaml";

    private static final String CONFIGURATION_ADMIN_READ = "/configuration?operation=securePropertyKeys";

    private static final String LOG_ADMIN_READ = "/log?operation=logStores";

    private final List<AppbaseCoreServices> startedCoreServices = new ArrayList<>();

    private final List<ReactorNettyAppbaseWebServer> startedServers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (final ReactorNettyAppbaseWebServer server : startedServers) {
            if (server.isStarted()) {
                server.stop();
            }
        }
        for (final AppbaseCoreServices coreServices : startedCoreServices) {
            coreServices.shutdown();
        }
    }

    /**
     * Builds a real, YAML-backed {@link AppbaseServices} with secure-by-default set as requested, at
     * the BARE default: the only application service wired is a real {@link HealthCheckService}
     * (required by {@code ReactorNettyAppbaseWebServer.Builder}). No {@code UserSessionFilterConfiguration}
     * workaround and no {@link UserAccountService} are wired -- the assembled chain must carry an
     * anonymous GET all the way to {@code AuthorizationWebFilter} on its own (see the class javadoc).
     */
    private AppbaseServices buildServices(final boolean secureByDefault) throws Exception {
        final AppbaseCoreServices coreServices = AppbaseCoreServices.builder().fromYaml(CONFIG).build();
        startedCoreServices.add(coreServices);

        final AppbaseServices services = AppbaseServices.builder()
                .from(coreServices)
                .secureByDefault(secureByDefault)
                .with(new DefaultHealthCheckServiceFactory(DefaultHealthCheckServiceConfiguration.builder().build()))
                .build();
        // Forces the background snapshot synchronously so /health does not race the scheduled
        // executor's first (0-delay, but still asynchronous) run.
        services.get(HealthCheckService.class).checkHealth(true);
        return services;
    }

    private ReactorNettyAppbaseWebServer startServer(final AppbaseServices services) {
        // An explicit ephemeral-port configuration is required: the builder's own default
        // (ReactorNettyAppbaseWebServerConfiguration.builder().build()) binds port 8080, which risks
        // colliding with an unrelated local process and silently asserting against it instead of this
        // server. Mirrors ReactorNettyAppbaseWebServerHttpTest#startOnEphemeralPort.
        final ReactorNettyAppbaseWebServerConfiguration configuration =
                ReactorNettyAppbaseWebServerConfiguration.builder()
                        .enabled(true).host("127.0.0.1").port(0).build();
        final ReactorNettyAppbaseWebServer server = ReactorNettyAppbaseWebServer.builder()
                .from(services)
                .configuration(configuration)
                .build();
        server.start();
        startedServers.add(server);
        return server;
    }

    private static int statusOf(final ReactorNettyAppbaseWebServer server, final String uri) {
        return HttpClient.create()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .get().uri(uri)
                .responseSingle((response, content) -> Mono.just(response.status().code()))
                .block();
    }

    @Test
    void secureByDefaultDeniesAnAdminConfigurationReadOverRealHttp() throws Exception {
        final ReactorNettyAppbaseWebServer server = startServer(buildServices(true));

        Assertions.assertEquals(403, statusOf(server, CONFIGURATION_ADMIN_READ),
                "secure-by-default must deny an ungated admin read over the wire");
        // A SECOND ungated admin read must also be denied, so a future regression that public-lists
        // one admin path cannot pass this suite by leaving another admin path reachable (final-review F5).
        Assertions.assertEquals(403, statusOf(server, LOG_ADMIN_READ),
                "secure-by-default must deny a second ungated admin read (/log) over the wire");
    }

    @Test
    void secureByDefaultDoesNotGateThePublicHealthPathOverRealHttp() throws Exception {
        final ReactorNettyAppbaseWebServer server = startServer(buildServices(true));

        Assertions.assertEquals(200, statusOf(server, "/health"),
                "the default public path /health must answer normally, not be denied");
    }

    @Test
    void secureByDefaultOffLetsTheAdminConfigurationReadReachTheHandler() throws Exception {
        final ReactorNettyAppbaseWebServer server = startServer(buildServices(false));

        // Stronger than "!= 403": prove the request actually reached the configuration handler and was
        // served (HTTP 200), not merely that it dodged the forbidden path (final-review F5).
        Assertions.assertEquals(200, statusOf(server, CONFIGURATION_ADMIN_READ),
                "secureByDefault(false) must let the request reach and be served by the handler (200)");
    }
}
