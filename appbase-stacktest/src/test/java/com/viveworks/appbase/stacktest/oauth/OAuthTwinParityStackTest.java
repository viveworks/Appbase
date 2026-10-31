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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.configuration.ReactorNettyAppbaseWebServerConfiguration;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.bootstrap.reactornetty.server.ReactorNettyAppbaseWebServer;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import com.viveworks.appbase.service.web.oauth.CaffeineTokenCache;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationFailure;
import com.viveworks.appbase.service.web.oauth.google.GoogleOAuthDefaults;
import com.viveworks.appbase.service.web.oauth.reactive.ReactiveOAuthFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.publisher.Mono;

/**
 * Stack arm for the reactive OAuth twin (T1330, internal work item 1330; the {@code GAP-R1} row 46 of the internal work item 1328
 * representative coverage matrix).
 *
 * <p><b>The behaviour this proves.</b> {@code OAuthFilter} and {@link ReactiveOAuthFilter} both document
 * the same sentence — "the identical spec §4.4 lifecycle over the same {@code TokenValidator} /
 * {@code TokenCache} SPIs", so {@code appbase-bootstrap-reactor-netty} is not shipped auth-less. They are
 * two independent implementations of one contract, each with its own unit tests, and nothing asserted
 * that they <em>agree</em>. This arm runs both over real HTTP — the servlet twin on embedded Jetty, the
 * reactive twin registered as an {@link AppbaseServiceFilter} on the shipped
 * {@link ReactorNettyAppbaseWebServer} — and asserts the same request yields the same status from each.
 *
 * <p>Registering through {@link AppbaseServiceFilter} is itself part of the claim: that is the SPI the
 * reactor-netty server accepts, and it is how an integrator actually mounts
 * {@link ReactiveOAuthFilter#filter(AppbaseServiceExchange,
 * com.viveworks.appbase.service.web.oauth.reactive.ReactiveWebFilterChain)} into the shipped server. The
 * adapter below is the whole of that wiring, so this arm also proves the two interfaces compose.
 *
 * <p><b>Reused lower-level evidence, deliberately not duplicated here</b> (internal work item 1330 acceptance):
 * <ul>
 *   <li>{@code ReactiveOAuthFilterTest} already covers the reactive twin's lifecycle branches in
 *       isolation — the non-bearer scheme, the fail-closed validator exception, the "does not mask a
 *       downstream error" case. This arm asserts only the outcomes an HTTP client can observe, and only
 *       where it can compare them against the servlet twin.</li>
 *   <li>{@code ReactiveOAuthFilterBlockHoundTest} proves the blocking SPI calls stay off the event loop.
 *       That is a property of the scheduler, not of the response, so it is not re-asserted here.</li>
 *   <li>{@code OAuthFilterTest} and {@code ReactiveOAuthFilterTest} each prove cache-key namespacing
 *       <em>within</em> their own paradigm. {@link #theReactiveTwinServesAnEntryCachedByTheServletTwin()}
 *       adds the claim neither can make alone: the two twins derive the SAME key for the same
 *       configuration, which is what makes a shared Redis/Aerospike token cache usable across a mixed
 *       servlet + reactive deployment.</li>
 * </ul>
 *
 * <p>Both servers bind an ephemeral loopback port, so this arm needs no external infrastructure and runs
 * offline. It lives in {@code appbase-stacktest}, whose module-wide {@code skipTests} default is
 * {@code true}; run it explicitly with {@code -Dappbase-stacktest.skipTests=false}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class OAuthTwinParityStackTest {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private static final String PATH = "/twin";
    private static final String ACCEPTED_TOKEN = "stacktest-twin-accepted";
    private static final String REJECTED_TOKEN = "stacktest-twin-rejected";

    /**
     * One configuration, from a real provider's public entry point, shared by both twins: parity is only
     * meaningful when the two filters are configured identically.
     */
    private static final OAuthConfiguration CONFIGURATION =
            GoogleOAuthDefaults.configuration("twin-parity-client.apps.googleusercontent.com");

    private Server jetty;
    private ReactorNettyAppbaseWebServer reactorNetty;

    @AfterEach
    void stopServers() throws Exception {
        Exception failure = null;
        if (jetty != null) {
            try {
                jetty.stop();
            } catch (Exception stopFailure) {
                failure = stopFailure;
            } finally {
                try {
                    jetty.destroy();
                } catch (RuntimeException destroyFailure) {
                    if (failure == null) {
                        failure = destroyFailure;
                    } else {
                        failure.addSuppressed(destroyFailure);
                    }
                }
                jetty = null;
            }
        }
        if (reactorNetty != null) {
            try {
                reactorNetty.stop();
            } catch (Exception stopFailure) {
                if (failure == null) {
                    failure = stopFailure;
                } else {
                    failure.addSuppressed(stopFailure);
                }
            } finally {
                reactorNetty = null;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Test
    @Timeout(20)
    void bothTwinsRejectAMissingBearerTokenWithTheSameStatus() throws Exception {
        Downstream servletDownstream = new Downstream();
        Downstream reactiveDownstream = new Downstream();
        String servletUrl = startServletTwin(filter(new CaffeineTokenCache(), new AtomicInteger()),
                servletDownstream);
        String reactiveUrl = startReactiveTwin(reactiveFilter(new CaffeineTokenCache(), new AtomicInteger()),
                reactiveDownstream);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status(servletUrl, null),
                "servlet twin: no bearer token must be 401");
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status(reactiveUrl, null),
                "reactive twin must agree with the servlet twin: no bearer token must be 401");
        assertEquals(0, servletDownstream.invocations(), "servlet twin must not forward");
        assertEquals(0, reactiveDownstream.invocations(), "reactive twin must not forward");
    }

    @Test
    @Timeout(20)
    void bothTwinsForwardAnAcceptedTokenWithTheSameStatus() throws Exception {
        Downstream servletDownstream = new Downstream();
        Downstream reactiveDownstream = new Downstream();
        String servletUrl = startServletTwin(filter(new CaffeineTokenCache(), new AtomicInteger()),
                servletDownstream);
        String reactiveUrl = startReactiveTwin(reactiveFilter(new CaffeineTokenCache(), new AtomicInteger()),
                reactiveDownstream);

        assertEquals(HttpServletResponse.SC_OK, status(servletUrl, "Bearer " + ACCEPTED_TOKEN),
                "servlet twin: an accepted token must be forwarded (200)");
        assertEquals(HttpServletResponse.SC_OK, status(reactiveUrl, "Bearer " + ACCEPTED_TOKEN),
                "reactive twin must agree with the servlet twin: an accepted token must be forwarded (200)");
        assertEquals(1, servletDownstream.invocations(), "servlet twin must forward exactly once");
        assertEquals(1, reactiveDownstream.invocations(), "reactive twin must forward exactly once");
    }

    @Test
    @Timeout(20)
    void bothTwinsRejectAnInvalidTokenWithTheSameStatus() throws Exception {
        Downstream servletDownstream = new Downstream();
        Downstream reactiveDownstream = new Downstream();
        String servletUrl = startServletTwin(filter(new CaffeineTokenCache(), new AtomicInteger()),
                servletDownstream);
        String reactiveUrl = startReactiveTwin(reactiveFilter(new CaffeineTokenCache(), new AtomicInteger()),
                reactiveDownstream);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status(servletUrl, "Bearer " + REJECTED_TOKEN),
                "servlet twin: a rejected token must be 401");
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, status(reactiveUrl, "Bearer " + REJECTED_TOKEN),
                "reactive twin must agree with the servlet twin: a rejected token must be 401");
        assertEquals(0, servletDownstream.invocations(),
                "servlet twin must not forward a rejected token");
        assertEquals(0, reactiveDownstream.invocations(),
                "reactive twin must not forward a rejected token");
    }

    /**
     * The claim a shared token-cache backend rests on: for one configuration the two twins derive the
     * SAME cache key, so the reactive twin serves an entry the servlet twin validated and never consults
     * its validator again.
     *
     * <p>The validator is shared and counts its calls. The servlet request forces one validation; the
     * reactive request with the same token must be answered from the shared cache, leaving the count at
     * one. Were the twins to namespace their keys differently, the reactive twin would miss, validate a
     * second time, and the count would be two — the mutation this assertion exists to catch.
     */
    @Test
    @Timeout(20)
    void theReactiveTwinServesAnEntryCachedByTheServletTwin() throws Exception {
        TokenCache sharedCache = new CaffeineTokenCache();
        AtomicInteger validations = new AtomicInteger();
        Downstream servletDownstream = new Downstream();
        Downstream reactiveDownstream = new Downstream();
        String servletUrl = startServletTwin(filter(sharedCache, validations), servletDownstream);
        String reactiveUrl = startReactiveTwin(reactiveFilter(sharedCache, validations), reactiveDownstream);

        assertEquals(HttpServletResponse.SC_OK, status(servletUrl, "Bearer " + ACCEPTED_TOKEN),
                "precondition: the servlet twin must accept and cache the token");
        assertEquals(1, validations.get(),
                "precondition: the servlet twin must have consulted the validator exactly once");

        assertEquals(HttpServletResponse.SC_OK, status(reactiveUrl, "Bearer " + ACCEPTED_TOKEN),
                "the reactive twin must accept the same token");
        assertEquals(1, validations.get(),
                "the reactive twin must serve the entry the servlet twin cached, without re-validating; "
                        + "a second validation would mean the twins do not agree on the cache key");
        assertEquals(1, reactiveDownstream.invocations(),
                "the reactive twin must forward the cache-served request downstream");
    }

    /** A validator shared between the twins, counting how many times it was actually consulted. */
    private static TokenValidator validator(AtomicInteger validations) {
        return rawToken -> {
            validations.incrementAndGet();
            return ACCEPTED_TOKEN.equals(rawToken)
                    ? new ValidatedToken("subject", CONFIGURATION.issuer(), Set.of("api"),
                            Instant.now().plus(Duration.ofMinutes(10)), Map.of(), "twin-parity-arm")
                    : new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE, "stub reject");
        };
    }

    private static OAuthFilter filter(TokenCache cache, AtomicInteger validations) {
        return new OAuthFilter(validator(validations), cache, new Sha256TokenKeyHasher(), CONFIGURATION);
    }

    private static ReactiveOAuthFilter reactiveFilter(TokenCache cache, AtomicInteger validations) {
        return new ReactiveOAuthFilter(validator(validations), cache, new Sha256TokenKeyHasher(),
                CONFIGURATION);
    }

    private String startServletTwin(OAuthFilter oauthFilter, Downstream downstream) throws Exception {
        jetty = new Server();
        ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        jetty.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new CountingServlet(downstream)), PATH);
        context.addFilter(new FilterHolder(oauthFilter), PATH, EnumSet.of(DispatcherType.REQUEST));
        jetty.setHandler(context);
        try {
            jetty.start();
        } catch (Exception startupFailure) {
            try {
                jetty.stop();
            } catch (Exception cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            try {
                jetty.destroy();
            } catch (RuntimeException cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            jetty = null;
            throw startupFailure;
        }
        return "http://127.0.0.1:" + connector.getLocalPort() + PATH;
    }

    /**
     * Mounts the reactive twin on the shipped reactor-netty server through the server's own filter SPI.
     * The adapter is the entire integration: {@link AppbaseServiceFilterChain#next} is exactly the
     * {@code ReactiveWebFilterChain} shape the reactive filter continues through.
     */
    private String startReactiveTwin(ReactiveOAuthFilter oauthFilter, Downstream downstream) {
        AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", PATH, exchange -> {
            downstream.record();
            exchange.status(HttpServletResponse.SC_OK);
            return exchange.writeJson(Map.of("reached", true));
        });
        AppbaseServiceFilter adapter = new AppbaseServiceFilter() {
            @Override
            public Mono<Void> filter(AppbaseServiceExchange exchange, AppbaseServiceFilterChain chain) {
                return oauthFilter.filter(exchange, chain::next);
            }

            @Override
            public int order() {
                return AppbaseServiceFilterOrder.IDP_TOKEN_CHECK;
            }
        };
        reactorNetty = new ReactorNettyAppbaseWebServer(
                ReactorNettyAppbaseWebServerConfiguration.builder().host("127.0.0.1").port(0).build(),
                registry,
                List.of(adapter),
                new ObjectMapper());
        try {
            reactorNetty.start();
        } catch (RuntimeException startupFailure) {
            try {
                reactorNetty.stop();
            } catch (RuntimeException cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            reactorNetty = null;
            throw startupFailure;
        }
        return "http://127.0.0.1:" + reactorNetty.getPort() + PATH;
    }

    private static int status(String url, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        try {
            return HTTP_CLIENT.send(request.GET().build(), HttpResponse.BodyHandlers.discarding())
                    .statusCode();
        } catch (IOException failure) {
            throw new IOException("HTTP request failed for " + url + " within " + REQUEST_TIMEOUT,
                    failure);
        }
    }

    /** Counts downstream arrivals, so "was it forwarded?" is asserted rather than inferred. */
    private static final class Downstream {

        private final AtomicInteger invocations = new AtomicInteger();

        void record() {
            invocations.incrementAndGet();
        }

        int invocations() {
            return invocations.get();
        }
    }

    /** The servlet twin's downstream target. */
    private static final class CountingServlet extends HttpServlet {

        private final transient Downstream downstream;

        private CountingServlet(Downstream downstream) {
            this.downstream = downstream;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            downstream.record();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"reached\":true}");
        }
    }
}
