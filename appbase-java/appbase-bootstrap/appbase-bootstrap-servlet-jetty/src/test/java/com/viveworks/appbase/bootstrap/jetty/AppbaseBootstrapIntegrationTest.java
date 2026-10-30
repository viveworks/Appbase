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

package com.viveworks.appbase.bootstrap.jetty;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.guice.AppbaseClientServicesModule;
import com.viveworks.appbase.bootstrap.guice.AppbaseCoreServicesModule;
import com.viveworks.appbase.bootstrap.guice.AppbaseServicesModule;
import com.viveworks.appbase.bootstrap.jetty.guice.JettyAppbaseServerModule;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.service.factory.DefaultHealthCheckServiceFactory;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.service.health.DefaultHealthCheckServiceConfiguration;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.user.account.UserAccountService;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the appbase-bootstrap builder and Guice paths.
 *
 * <p>All tests use {@code appbase-test-config.yaml} from the test classpath. No external
 * infrastructure (database, Lucene index, JMX) is required — all heavy-weight optional
 * services are disabled in the config.
 *
 * @author Navaneeth Mysore G
 */
class AppbaseBootstrapIntegrationTest {

    private static final String CONFIG = "classpath:///appbase-test-config.yaml";

    // ── Builder path ───────────────────────────────────────────────────────────

    @Test
    void builderPath_coreServicesBuildsFromYaml() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            assertNotNull(core.getLogService());
            assertNotNull(core.getMediatorService());
            assertNotNull(core.getExceptionService());
            assertNotNull(core.getConfigurationService());
            assertNotNull(core.getServiceRegistry());
            assertNotNull(core.getTracerService());
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_clientAndServiceContainersBuildOnTopOfCore() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            // No client factories registered — client container must be empty but non-null.
            AppbaseClientServices clients = AppbaseClientServices.builder()
                .from(core)
                .build();
            // No app-service factories registered, but the builder is secure-by-default: it injects
            // exactly one AuthorizationService. The service container therefore holds precisely that.
            AppbaseServices services = AppbaseServices.builder()
                .from(core)
                .withClientServices(clients)
                .build();
            assertTrue(clients.services().isEmpty());
            assertEquals(1, services.services().size(),
                "secure-by-default injects exactly one service (AuthorizationService)");
            assertEquals(Set.of(AuthorizationService.class), services.services().keySet());
            assertNotNull(services.get(AuthorizationService.class));
            services.shutdown();
            clients.shutdown();
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_secureByDefault_buildsFromRealYamlCoreWithoutPreRegisteredAuthConfig() {
        // Regression guard: a real YAML-configured core does NOT register a
        // DefaultAuthorizationServiceConfiguration. The secure-by-default wiring must still build
        // successfully (supplying a default config) and yield an AuthorizationService. A config-reading
        // factory would resolve null here and throw — exactly the Critical this test pins down.
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseServices services = AppbaseServices.builder()
                .from(core)
                .build();
            assertNotNull(services.get(AuthorizationService.class),
                "secure-by-default must yield an AuthorizationService on a plain YAML core");
            services.shutdown();
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_secureByDefaultOff_buildsFromRealYamlCoreAndStillYieldsAuthorizationService() {
        // secureByDefault(false) selects PermitAllContextAuthorizer, but still materialises an
        // AuthorizationService. Building from a real YAML core (no pre-registered auth config) must
        // succeed just the same — the permit-all path must not newly force a throwing auth service.
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseServices services = AppbaseServices.builder()
                .from(core)
                .secureByDefault(false)
                .build();
            assertNotNull(services.get(AuthorizationService.class),
                "secureByDefault(false) must still yield an AuthorizationService on a plain YAML core");
            services.shutdown();
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_jettyServerStartsAndStops() throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseClientServices clients = AppbaseClientServices.builder()
                .from(core)
                .build();
            AppbaseServices services = AppbaseServices.builder()
                .from(core)
                .withClientServices(clients)
                .build();
            // port(0) asks the OS for an ephemeral port — avoids conflicts in parallel CI runs
            JettyServletContainer server = JettyServletContainer.builder()
                .from(services)
                .port(0)
                .build();
            server.start();
            server.stop();
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_withDefaultExceptionStores_registersInMemoryStore() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .withDefaultExceptionStores()
            .build();
        try {
            boolean hasInMemory = core.getExceptionService()
                .getExceptionAccessor()
                .getExceptionStores()
                .stream()
                .anyMatch(s -> s instanceof InMemoryExceptionStore);
            assertTrue(hasInMemory, "withDefaultExceptionStores() must register an InMemoryExceptionStore");
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_withDefaultMXBeans_registersJmxLogServiceBean() throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseClientServices clients = AppbaseClientServices.builder().from(core).build();
            AppbaseServices services = AppbaseServices.builder().from(core).withClientServices(clients).build();
            JettyServletContainer server = JettyServletContainer.builder()
                .from(services)
                .port(0)
                .withDefaultMXBeans()
                .build();
            JmxService jmxService = core.appbaseCore().getJmxService();
            assertNotNull(jmxService.getMXBean(JmxConfigurationService.class),
                "withDefaultMXBeans() must register JmxConfigurationService");
            server.stop();
        } finally {
            core.shutdown();
        }
    }

    // ── Secure-by-default end-to-end (servlet face) ──────────────────────────────

    /**
     * Builds a secure-by-default {@link AppbaseServices} on top of {@code core} at the BARE default:
     * the only application service wired is a real {@link HealthCheckService}. No {@code
     * UserSessionFilterConfiguration} workaround and no {@link UserAccountService} are wired.
     *
     * <p><strong>Bare default, no session/user-account scaffolding.</strong> {@code
     * UserSessionServletFilter} and {@code IdpUserTokenCheckServletFilter} delegate to the SAME
     * neutral {@code DefaultUserSessionFilter} / {@code DefaultIdpUserTokenCheckFilter} logic the
     * reactive face uses, and run ahead of {@code AuthorizationServletFilter} on every request,
     * regardless of secure-by-default. An anonymous request resolves no session; {@code
     * UserSessionServletFilter} passes it through without a session attribute (its documented
     * no-session contract, and {@code DefaultUserSessionFilter} no longer eagerly requires a
     * user-account service on this path); the token-check filter then finds no {@code UserSession}
     * request attribute and -- with the Task 9 fix -- continues the chain rather than throwing, so the
     * request reaches {@code AuthorizationServletFilter}. Nothing about the authorization decision is
     * stubbed: {@code SecureByDefaultContextAuthorizer} and {@code AuthorizationServletFilter} run
     * unmodified. This proves secure-by-default works out of the box on the servlet face: anonymous
     * {@code /health} -> 200, anonymous {@code /configuration} -> 403.
     */
    private static AppbaseServices buildSecureByDefaultServices(
            final AppbaseCoreServices core, final boolean secureByDefault) throws Exception {
        final AppbaseServices services = AppbaseServices.builder()
                .from(core)
                .secureByDefault(secureByDefault)
                .with(new DefaultHealthCheckServiceFactory(DefaultHealthCheckServiceConfiguration.builder().build()))
                .build();
        // Forces the background snapshot synchronously so /health does not race the scheduled
        // executor's first (0-delay, but still asynchronous) run.
        services.get(HealthCheckService.class).checkHealth(true);
        return services;
    }

    /**
     * Starts a real Jetty server carrying the FULL production servlet deployment -- the nine core
     * servlets ({@code /configuration} among them), every service servlet ({@code /health} among
     * them), and the eight standard filters bound at {@code "/*"} (authorization last) -- exactly what
     * {@code JettyServletContainer.Builder.build()} leaves opt-in via {@code withAppbaseCoreServlets()}
     * / {@code withAppbaseServiceServlets()} / {@code withAppbaseServiceFilters()}. The {@link
     * AppbaseServiceConstants#APPBASE_CORE_ATTRIBUTE} servlet-context attribute every appbase servlet
     * and filter reads via {@code AppbaseServiceUtil.appbaseCore(ServletContext)} is set through the
     * {@code withServletContext(...)} customizer -- production bootstrap code sets this the same way.
     */
    private static JettyServletContainer startJettyServerWithFullFilterChain(
            final AppbaseCoreServices core, final AppbaseServices services) {
        final JettyServletContainer server = JettyServletContainer.builder()
                .from(services)
                .port(0)
                .withAppbaseCoreServlets()
                .withAppbaseServiceServlets()
                .withAppbaseServiceFilters()
                .withServletContext(context -> context.setAttribute(
                        AppbaseServiceConstants.APPBASE_CORE_ATTRIBUTE, core.appbaseCore()))
                .build();
        server.start();
        return server;
    }

    private static int httpGetStatus(final String uri) throws Exception {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpResponse<Void> response = client.send(
                HttpRequest.newBuilder(URI.create(uri)).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }

    @Test
    void builderPath_secureByDefault_jettyDeniesAdminConfigurationReadOverRealHttp() throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseServices services = buildSecureByDefaultServices(core, true);
            JettyServletContainer server = startJettyServerWithFullFilterChain(core, services);
            try {
                final int status = httpGetStatus(
                        "http://127.0.0.1:" + server.getPort() + "/configuration?operation=securePropertyKeys");
                assertEquals(403, status,
                        "secure-by-default must deny an admin read over real HTTP on the servlet face");
                // A SECOND ungated admin read must also be denied, so a future regression that
                // public-lists one admin path cannot slip past by leaving another reachable (final-review F5).
                final int logStatus = httpGetStatus(
                        "http://127.0.0.1:" + server.getPort() + "/log?operation=logStores");
                assertEquals(403, logStatus,
                        "secure-by-default must deny a second admin read (/log) over real HTTP on the servlet face");
            } finally {
                server.stop();
            }
        } finally {
            core.shutdown();
        }
    }

    @Test
    void builderPath_secureByDefault_jettyPermitsHealthPathOverRealHttp() throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseServices services = buildSecureByDefaultServices(core, true);
            JettyServletContainer server = startJettyServerWithFullFilterChain(core, services);
            try {
                final int status = httpGetStatus("http://127.0.0.1:" + server.getPort() + "/health");
                assertEquals(200, status,
                        "the public /health path must not be gated by secure-by-default -- same setting, same"
                                + " outcome as the reactive face");
            } finally {
                server.stop();
            }
        } finally {
            core.shutdown();
        }
    }

    // ── Guice path ─────────────────────────────────────────────────────────────

    @Test
    void builderPath_secureByDefaultOff_jettyDoesNotDenyTheAdminConfigurationReadOverRealHttp() throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseServices services = buildSecureByDefaultServices(core, false);
            JettyServletContainer server = startJettyServerWithFullFilterChain(core, services);
            try {
                final int status = httpGetStatus(
                        "http://127.0.0.1:" + server.getPort() + "/configuration?operation=securePropertyKeys");
                // Concrete-status proof the request REACHED the configuration servlet rather than being
                // stopped at the authorization filter: a 403 is produced by AuthorizationServletFilter
                // BEFORE the servlet runs, whereas this 500 is produced by the configuration servlet
                // itself after authorization permitted the request (the bare test deployment has no
                // backing config store for the securePropertyKeys admin op, so the servlet errors). The
                // reactive face serves the same read as 200; both faces prove the same thing -- the
                // authorizer let the request through -- which "!= 403" alone did not (final-review F5).
                assertEquals(500, status,
                        "secureByDefault(false) must let the request reach the configuration servlet (500 here),"
                                + " never the 403-forbidden path produced by the authorization filter");
            } finally {
                server.stop();
            }
        } finally {
            core.shutdown();
        }
    }

    @Test
    void guicePath_allModulesInstallCorrectly() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG)
            .build();
        try {
            AppbaseClientServices clients = AppbaseClientServices.builder()
                .from(core)
                .build();
            AppbaseServices services = AppbaseServices.builder()
                .from(core)
                .withClientServices(clients)
                .build();
            JettyServletContainer server = JettyServletContainer.builder()
                .from(services)
                .port(0)
                .build();

            Injector injector = Guice.createInjector(
                new AppbaseCoreServicesModule(core),
                new AppbaseClientServicesModule(clients),
                new AppbaseServicesModule(services),
                new JettyAppbaseServerModule(server)
            );

            assertSame(core,     injector.getInstance(AppbaseCoreServices.class));
            assertSame(clients,  injector.getInstance(AppbaseClientServices.class));
            assertSame(services, injector.getInstance(AppbaseServices.class));
            assertSame(server,   injector.getInstance(JettyServletContainer.class));
            assertNotNull(injector.getInstance(LogService.class));

            server.stop();
        } finally {
            core.shutdown();
        }
    }
}
