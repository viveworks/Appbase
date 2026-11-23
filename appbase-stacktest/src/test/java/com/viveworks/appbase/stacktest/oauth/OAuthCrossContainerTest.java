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

package com.viveworks.appbase.stacktest.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.viveworks.appbase.service.web.oauth.CaffeineTokenCache;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenKeyHashing;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationFailure;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Cross-container verification for the servlet {@link OAuthFilter} (N09, internal work item 1199, spec §10 bullet 1).
 *
 * <p>The claim "one servlet {@code Filter}, works identically across containers" turned into a test: the
 * SAME {@code OAuthFilter} instance-per-run is registered on each of the three raw embedded servlet
 * containers — Jetty via {@link ServletContextHandler}, Undertow via {@link DeploymentInfo}, Tomcat via
 * {@link Context} — in front of a trivial echo servlet, and the same three requests are asserted to yield
 * the identical status on all three: no bearer token → 401, a token the validator accepts → 200 (forwarded
 * to the echo servlet), a token it rejects → 401. Parameterized over {@link Container}, so a divergence in
 * any one container's filter dispatch fails only that parameter.
 *
 * <p>Validation is a deterministic offline stub ({@link #STUB_VALIDATOR}): it neither reaches a JWKS
 * endpoint nor exercises Nimbus crypto (covered by the oauth module's own unit tests). This IT proves the
 * container REGISTRATION and dispatch are identical, not the signature/claims logic. The real
 * {@link Sha256TokenKeyHasher} and {@link CaffeineTokenCache} are used unchanged.
 *
 * <p>Runs on embedded servers bound to an ephemeral loopback port, so it needs no external infrastructure
 * and executes offline. It lives in {@code appbase-stacktest}, whose module-wide {@code skipTests} default
 * is {@code true}; run it explicitly with {@code -Dappbase-stacktest.skipTests=false} (the human-verify
 * step, evidence recorded in the ledger).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class OAuthCrossContainerTest {

    private static final String ISSUER = "https://issuer.example";
    private static final String ACCEPTED_TOKEN = "accepted-test-token";
    private static final String REJECTED_TOKEN = "rejected-test-token";

    /**
     * Accepts exactly {@link #ACCEPTED_TOKEN}; every other raw token is a {@code BAD_SIGNATURE} failure.
     * Deterministic and offline — the point of this IT is container parity, not crypto.
     */
    private static final TokenValidator STUB_VALIDATOR = rawToken -> ACCEPTED_TOKEN.equals(rawToken)
            ? new ValidatedToken("sub", ISSUER, Set.of("api"), Instant.now().plus(Duration.ofMinutes(10)),
                    Map.of(), "cross-container-it")
            : new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE, "stub reject");

    @ParameterizedTest
    @EnumSource(Container.class)
    void rejectsMissingTokenIdenticallyAcrossContainers(Container container) throws Exception {
        try (ServerHandle server = container.start(newOAuthFilter(), new EchoServlet())) {
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, statusFor(server, null),
                    container + ": a request with no bearer token must be rejected with 401");
        }
    }

    @ParameterizedTest
    @EnumSource(Container.class)
    void acceptsValidTokenIdenticallyAcrossContainers(Container container) throws Exception {
        try (ServerHandle server = container.start(newOAuthFilter(), new EchoServlet())) {
            assertEquals(HttpServletResponse.SC_OK, statusFor(server, "Bearer " + ACCEPTED_TOKEN),
                    container + ": a token the validator accepts must be forwarded to the servlet (200)");
        }
    }

    @ParameterizedTest
    @EnumSource(Container.class)
    void rejectsInvalidTokenIdenticallyAcrossContainers(Container container) throws Exception {
        try (ServerHandle server = container.start(newOAuthFilter(), new EchoServlet())) {
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, statusFor(server, "Bearer " + REJECTED_TOKEN),
                    container + ": a token the validator rejects must be rejected with 401");
        }
    }

    /** A fresh filter per run — each container gets its own {@link CaffeineTokenCache}, so no state bleeds. */
    private static OAuthFilter newOAuthFilter() {
        OAuthConfiguration configuration = new OAuthConfiguration(URI.create("https://issuer.example/jwks"),
                ISSUER, "client-1", "aud", 30L, 300L, TokenKeyHashing.SHA256, null);
        return new OAuthFilter(STUB_VALIDATOR, new CaffeineTokenCache(), new Sha256TokenKeyHasher(),
                configuration);
    }

    private static int statusFor(ServerHandle server, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(server.baseUrl()));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HttpClient.newHttpClient()
                .send(request.GET().build(), HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    /** The downstream target: reached only when the filter forwards an authenticated request. */
    private static final class EchoServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("ok");
        }
    }

    /** A started embedded server bound to a loopback port, stopped by try-with-resources. */
    private interface ServerHandle extends AutoCloseable {
        String baseUrl();

        @Override
        void close() throws Exception;
    }

    /**
     * The three servlet containers, each registering the filter instance through its own native API. Port
     * {@code 0} binds an ephemeral loopback port, read back after start so runs never collide.
     */
    private enum Container {
        JETTY {
            @Override
            ServerHandle start(Filter oauthFilter, HttpServlet downstream) throws Exception {
                Server server = new Server(0);
                ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
                context.setContextPath("/");
                context.addServlet(new ServletHolder(downstream), "/*");
                context.addFilter(new FilterHolder(oauthFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
                server.setHandler(context);
                server.start();
                int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
                awaitRouteReady("http://localhost:" + port + "/");
                return handle(port, server::stop);
            }
        },
        UNDERTOW {
            @Override
            ServerHandle start(final Filter oauthFilter, final HttpServlet downstream) throws Exception {
                // Unique deployment name + undeploy on close: start() runs once per test method against
                // the JVM-static Servlets.defaultContainer(), so a fixed name would re-register the same
                // context path across runs and leak the prior deployment. Mirrors the production
                // UndertowServletContainer, which uses a per-instance name and undeploys in stop().
                final DeploymentInfo info = Servlets.deployment()
                        .setClassLoader(OAuthCrossContainerTest.class.getClassLoader())
                        .setContextPath("/")
                        .setDeploymentName("oauth-cross-container-it-" + System.nanoTime())
                        .addServlet(Servlets.servlet("echo", downstream.getClass(),
                                new ImmediateInstanceFactory<>(downstream)).addMapping("/*"))
                        .addFilter(Servlets.filter("oauth", oauthFilter.getClass(),
                                new ImmediateInstanceFactory<>(oauthFilter)))
                        .addFilterUrlMapping("oauth", "/*", DispatcherType.REQUEST);
                final DeploymentManager manager = Servlets.defaultContainer().addDeployment(info);
                manager.deploy();
                final Undertow server = Undertow.builder()
                        .addHttpListener(0, "localhost")
                        .setHandler(manager.start())
                        .build();
                server.start();
                final int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
                awaitRouteReady("http://localhost:" + port + "/");
                return handle(port, () -> {
                    server.stop();
                    manager.undeploy();
                });
            }
        },
        TOMCAT {
            @Override
            ServerHandle start(final Filter oauthFilter, final HttpServlet downstream) throws Exception {
                // Reuse the shared temp dir rather than minting a per-run directory: the production
                // TomcatServletContainer does the same, so there is nothing to leak or clean up.
                final String baseDir = System.getProperty("java.io.tmpdir");
                final Tomcat tomcat = new Tomcat();
                tomcat.setBaseDir(baseDir);
                tomcat.setPort(0);
                tomcat.getConnector();
                final Context context = tomcat.addContext("", baseDir);
                Tomcat.addServlet(context, "echo", downstream);
                context.addServletMappingDecoded("/*", "echo");
                final FilterDef filterDef = new FilterDef();
                filterDef.setFilterName("oauth");
                filterDef.setFilter(oauthFilter);
                context.addFilterDef(filterDef);
                final FilterMap filterMap = new FilterMap();
                filterMap.setFilterName("oauth");
                filterMap.addURLPattern("/*");
                context.addFilterMap(filterMap);
                tomcat.start();
                final int port = tomcat.getConnector().getLocalPort();
                awaitRouteReady("http://localhost:" + port + "/");
                return handle(port, () -> {
                    tomcat.stop();
                    tomcat.destroy();
                });
            }
        };

        abstract ServerHandle start(Filter oauthFilter, HttpServlet downstream) throws Exception;

        /**
         * Polls {@code baseUrl} with a bare GET (no {@code Authorization} header) until it gets any
         * response OTHER than a 404, or a short deadline elapses. Each container's {@code start()} call
         * can return to the caller before its acceptor has the context fully wired into routing under
         * CPU contention; until then a request gets that container's own "no handler matched" 404
         * rather than reaching the filter chain at all -- observed on both JETTY and TOMCAT, so this is
         * applied to all three containers rather than just the one caught first. A bare GET with no
         * token always reaches a live filter and gets 401 (never 200), so "not 404" here only proves the
         * route exists -- it does not predict, let alone guarantee, any later assertion in the calling
         * test.
         */
        private static void awaitRouteReady(final String baseUrl) throws Exception {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl)).GET().build();
            final long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            while (System.nanoTime() < deadline) {
                try {
                    final int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                    if (status != HttpServletResponse.SC_NOT_FOUND) {
                        return;
                    }
                } catch (IOException ignored) {
                    // Connection refused/reset while the acceptor is still coming up -- keep polling.
                }
                Thread.sleep(10);
            }
        }

        private static ServerHandle handle(final int port, final Stopper stopper) {
            final String baseUrl = "http://localhost:" + port + "/";
            return new ServerHandle() {
                @Override
                public String baseUrl() {
                    return baseUrl;
                }

                @Override
                public void close() throws Exception {
                    stopper.stop();
                }
            };
        }

        @FunctionalInterface
        private interface Stopper {
            void stop() throws Exception;
        }
    }
}
