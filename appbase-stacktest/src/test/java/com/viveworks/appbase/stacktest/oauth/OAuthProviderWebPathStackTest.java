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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.service.web.oauth.CaffeineTokenCache;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationFailure;
import com.viveworks.appbase.service.web.oauth.apple.AppleOAuthDefaults;
import com.viveworks.appbase.service.web.oauth.google.GoogleOAuthDefaults;
import com.viveworks.appbase.service.web.oauth.microsoft.EntraOAuthDefaults;
import com.viveworks.appbase.service.web.oauth.okta.OktaOAuthDefaults;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Stack arm for the four OIDC provider modules whose shipped public entry point is an
 * {@link OAuthConfiguration} factory — Apple, Google, Microsoft (Entra) and Okta (T1330, internal work item 1330; the
 * {@code GAP-R1} rows 41, 43, 44, 45 of the internal work item 1328 representative coverage matrix).
 *
 * <p><b>The behaviour this proves.</b> Each provider module advertises a web path: hand
 * {@code XOAuthDefaults.configuration(...)} to the servlet {@link OAuthFilter} and requests are
 * authenticated. Until this arm nothing exercised that sentence. The provider modules' own unit tests
 * assert their defaults in isolation ({@code GoogleOAuthDefaultsTest.defaultsPinGoogleDiscoveryEndpoints}
 * and the Apple/Entra/Okta equivalents) and {@code OAuthFilterTest} asserts the filter's lifecycle
 * against a mocked request/response — but no test took a provider's OWN configuration through a real
 * servlet container and read a real HTTP status back. This arm does exactly that, and no more.
 *
 * <p><b>Reused lower-level evidence, deliberately not duplicated here</b> (internal work item 1330 acceptance: "do not
 * duplicate lower-level evidence without adding signal"):
 * <ul>
 *   <li><b>Container parity</b> — that one {@code OAuthFilter} behaves identically on Jetty, Undertow and
 *       Tomcat is already proved by the sibling {@link OAuthCrossContainerTest}, parameterized over all
 *       three. This arm therefore varies the <em>provider</em> and holds the container fixed at Jetty;
 *       re-running three containers per provider would multiply runtime without adding a claim.</li>
 *   <li><b>Signature and claims validation</b> — {@code NimbusTokenValidatorTest} (with its
 *       {@code JwksTestServer}) and each provider's {@code XOAuthDefaultsTest} cover the crypto and the
 *       {@code iss}/{@code aud} checks. This arm's validator is a deterministic offline stub, so no
 *       provider JWKS endpoint is contacted and the arm runs with no network.</li>
 *   <li><b>Cache-key namespacing in isolation</b> — {@code OAuthFilterTest
 *       .doesNotServeACachedTokenToADifferentlyConfiguredFilter} proves the mechanism against mocks with
 *       two hand-built configurations. {@link #aTokenCachedForOneProviderIsNeverServedToAnother()} adds
 *       the signal that is missing there: the two configurations are two <em>real shipped providers</em>,
 *       the cache is shared between two live servers, and the verdict is read as an HTTP status.</li>
 * </ul>
 *
 * <p><b>GitHub is covered separately.</b> {@code GitHubOAuthDefaults.configuration(...)} returns a
 * {@code GitHubOAuthConfiguration}, not an {@link OAuthConfiguration}: GitHub issues opaque tokens with no
 * JWKS endpoint, so it cannot be handed to {@link OAuthFilter}. The sibling
 * {@link GitHubOAuthWebPathStackTest} exercises its dedicated servlet filter with an offline
 * {@code /user} transport; {@code JdkGitHubUserApiTest} separately proves the production transport.
 *
 * <p>Runs on an embedded Jetty bound to an ephemeral loopback port, so it needs no external
 * infrastructure and executes offline. It lives in {@code appbase-stacktest}, whose module-wide
 * {@code skipTests} default is {@code true}; run it explicitly with
 * {@code -Dappbase-stacktest.skipTests=false}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class OAuthProviderWebPathStackTest {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    /** Marks the raw token this arm's stub validator will accept for a given issuer. */
    private static final String TOKEN_PREFIX = "stacktest-token-for:";

    private static final String DOWNSTREAM_BODY_PREFIX = "reached:";

    @ParameterizedTest
    @EnumSource(Provider.class)
    @Timeout(15)
    void missingBearerTokenIsRejectedOverTheWire(Provider provider) throws Exception {
        CountingServlet downstream = new CountingServlet();
        try (ServerHandle server = startJetty(provider.filter(new CaffeineTokenCache()), downstream)) {
            Response response = get(server, null);

            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status(),
                    provider + ": a request carrying no bearer token must be rejected with 401");
            assertEquals(0, downstream.invocations(),
                    provider + ": an unauthenticated request must never reach the downstream servlet");
        }
    }

    @ParameterizedTest
    @EnumSource(Provider.class)
    @Timeout(15)
    void providerAcceptedTokenReachesTheDownstreamServlet(Provider provider) throws Exception {
        CountingServlet downstream = new CountingServlet();
        try (ServerHandle server = startJetty(provider.filter(new CaffeineTokenCache()), downstream)) {
            Response response = get(server, "Bearer " + provider.acceptableToken());

            assertEquals(HttpServletResponse.SC_OK, response.status(),
                    provider + ": a token this provider's validator accepts must be forwarded (200)");
            assertEquals(1, downstream.invocations(),
                    provider + ": an authenticated request must reach the downstream servlet exactly once");
        }
    }

    @ParameterizedTest
    @EnumSource(Provider.class)
    @Timeout(15)
    void providerRejectedTokenNeverReachesTheDownstreamServlet(Provider provider) throws Exception {
        CountingServlet downstream = new CountingServlet();
        try (ServerHandle server = startJetty(provider.filter(new CaffeineTokenCache()), downstream)) {
            Response response = get(server, "Bearer " + TOKEN_PREFIX + "https://attacker.example");

            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status(),
                    provider + ": a token minted for another issuer must be rejected with 401");
            assertEquals(0, downstream.invocations(),
                    provider + ": a rejected request must never reach the downstream servlet");
        }
    }

    /**
     * Asserts that the stub validator and provider-derived configuration flow through the servlet path.
     * Provider unit tests separately pin the shipped issuer/discovery constants; this arm does not
     * independently validate them or perform cryptographic validation.
     */
    @ParameterizedTest
    @EnumSource(Provider.class)
    @Timeout(15)
    void theProvidersOwnIssuerIsWhatTheAuthenticatedRequestCarriesDownstream(Provider provider)
            throws Exception {
        CountingServlet downstream = new CountingServlet();
        try (ServerHandle server = startJetty(provider.filter(new CaffeineTokenCache()), downstream)) {
            Response response = get(server, "Bearer " + provider.acceptableToken());

            assertEquals(HttpServletResponse.SC_OK, response.status(), provider + ": expected 200");
            assertEquals(DOWNSTREAM_BODY_PREFIX + provider.issuer(), response.body(),
                    provider + ": the downstream handler must see a ValidatedToken stamped with this "
                            + "provider's shipped issuer");
        }
    }

    /**
     * Two providers, two live servers, ONE shared {@link TokenCache} — the arrangement an operator gets
     * from a Redis or Aerospike token cache backing more than one relying party.
     *
     * <p>Google's filter validates and caches the token. Okta's filter is then offered the SAME raw
     * token. Because {@link OAuthFilter} binds its cache key to its own configuration identity, Okta
     * misses the cache, consults its own validator, and rejects — 401. Were the namespace dropped from
     * the cache key, Okta would hit Google's cached entry and forward the request with 200, which is the
     * cross-tenant token-confusion this assertion exists to catch.
     */
    @Test
    @Timeout(15)
    void aTokenCachedForOneProviderIsNeverServedToAnother() throws Exception {
        TokenCache sharedCache = new CaffeineTokenCache();
        CountingServlet googleDownstream = new CountingServlet();
        CountingServlet oktaDownstream = new CountingServlet();
        AtomicInteger oktaValidations = new AtomicInteger();

        try (ServerHandle google = startJetty(Provider.GOOGLE.filter(sharedCache), googleDownstream);
                ServerHandle okta = startJetty(
                        filter(Provider.OKTA.configuration(),
                                countingValidator(Provider.OKTA.issuer(), oktaValidations), sharedCache),
                        oktaDownstream)) {

            String googleToken = Provider.GOOGLE.acceptableToken();

            assertEquals(HttpServletResponse.SC_OK, get(google, "Bearer " + googleToken).status(),
                    "precondition: Google's filter must accept and cache its own token");
            assertEquals(1, googleDownstream.invocations(), "precondition: Google forwarded once");

            Response fromOkta = get(okta, "Bearer " + googleToken);

            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, fromOkta.status(),
                    "a token cached by the Google-configured filter must NOT authenticate against the "
                            + "Okta-configured filter sharing the same cache");
            assertEquals(0, oktaDownstream.invocations(),
                    "the Okta-configured filter must not forward a token it never validated");
            assertEquals(1, oktaValidations.get(),
                    "the Okta-configured filter must MISS the shared cache and consult its own validator; "
                            + "a hit here would mean the cache key is not namespaced by configuration");
        }
    }

    @Test
    @Timeout(30)
    void everyProviderConfigurationProducesItsOwnObservableCacheNamespace() throws Exception {
        Set<String> cacheKeys = new HashSet<>();
        for (Provider provider : Provider.values()) {
            RecordingTokenCache cache = new RecordingTokenCache();
            CountingServlet downstream = new CountingServlet();
            try (ServerHandle server = startJetty(
                    filter(provider.configuration(), acceptingValidator(provider.expectedIssuer()), cache),
                    downstream)) {
                Response response = get(server, "Bearer shared-raw-token");
                assertEquals(HttpServletResponse.SC_OK, response.status(), provider + ": expected 200");
                assertTrue(response.body().startsWith(DOWNSTREAM_BODY_PREFIX),
                        provider + ": downstream response did not contain the validated issuer: "
                                + response.body());
                assertEquals(provider.expectedIssuer(), response.body().substring(DOWNSTREAM_BODY_PREFIX.length()),
                        provider + ": provider factory substitution must be observable downstream");
                assertTrue(cacheKeys.add(cache.lastPutKey()),
                        provider + ": provider configuration must produce a distinct cache namespace");
            }
        }
    }

    /** The four OIDC providers, each constructed through its own shipped public entry point. */
    private enum Provider {
        APPLE("https://appleid.apple.com") {
            @Override
            OAuthConfiguration configuration() {
                return AppleOAuthDefaults.configuration("com.example.stacktest");
            }
        },
        GOOGLE("https://accounts.google.com") {
            @Override
            OAuthConfiguration configuration() {
                return GoogleOAuthDefaults.configuration("stacktest-client.apps.googleusercontent.com");
            }
        },
        MICROSOFT_ENTRA("https://login.microsoftonline.com/contoso.onmicrosoft.com/v2.0") {
            @Override
            OAuthConfiguration configuration() {
                return EntraOAuthDefaults.configuration("contoso.onmicrosoft.com",
                        "api://stacktest", "stacktest-client");
            }
        },
        OKTA("https://dev-12345.okta.com/oauth2/default") {
            @Override
            OAuthConfiguration configuration() {
                return OktaOAuthDefaults.configuration("https://dev-12345.okta.com/oauth2/default",
                        "api://default", "stacktest-client");
            }
        };

        private final String expectedIssuer;

        Provider(String expectedIssuer) {
            this.expectedIssuer = expectedIssuer;
        }

        /** The configuration under test, from this provider module's own public factory. */
        abstract OAuthConfiguration configuration();

        /** The issuer the provider module itself derived — never a literal chosen by this test. */
        String issuer() {
            OAuthConfiguration configuration = configuration();
            assertEquals(expectedIssuer, configuration.issuer(),
                    this + ": shipped provider factory returned an unexpected issuer");
            return configuration.issuer();
        }

        String expectedIssuer() {
            return expectedIssuer;
        }

        String acceptableToken() {
            return TOKEN_PREFIX + issuer();
        }

        OAuthFilter filter(TokenCache cache) {
            return OAuthProviderWebPathStackTest.filter(configuration(),
                    issuerBoundValidator(issuer()), cache);
        }
    }

    private static OAuthFilter filter(OAuthConfiguration configuration, TokenValidator validator,
            TokenCache cache) {
        return new OAuthFilter(validator, cache, new Sha256TokenKeyHasher(), configuration);
    }

    /**
     * Accepts exactly the token minted for {@code issuer} and rejects everything else. Deterministic and
     * offline: this arm proves the provider configuration reaches the web path, not Nimbus crypto, which
     * {@code NimbusTokenValidatorTest} already covers against a real JWKS server.
     */
    private static TokenValidator issuerBoundValidator(String issuer) {
        return countingValidator(issuer, new AtomicInteger());
    }

    private static TokenValidator acceptingValidator(String issuer) {
        return rawToken -> new ValidatedToken("subject", issuer, Set.of("api"),
                Instant.now().plus(Duration.ofMinutes(10)), Map.of(), "provider-cache-namespace-arm");
    }

    /** As {@link #issuerBoundValidator}, and records how many times it was actually consulted. */
    private static TokenValidator countingValidator(String issuer, AtomicInteger validations) {
        String accepted = TOKEN_PREFIX + issuer;
        return rawToken -> {
            validations.incrementAndGet();
            return accepted.equals(rawToken)
                    ? new ValidatedToken("subject", issuer, Set.of("api"),
                            Instant.now().plus(Duration.ofMinutes(10)), Map.of(), "provider-web-path-arm")
                    : new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE,
                            "not minted for " + issuer);
        };
    }

    private static Response get(ServerHandle server, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(server.baseUrl()))
                .timeout(REQUEST_TIMEOUT);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException failure) {
            throw new IOException("HTTP request failed for " + server.baseUrl() + " within "
                    + REQUEST_TIMEOUT, failure);
        }
        return new Response(response.statusCode(), response.body());
    }

    /** One real HTTP exchange: the status line and the body that crossed the wire. */
    private record Response(int status, String body) {
    }

    /**
     * The downstream target. Counts its invocations, so "was the request forwarded?" is asserted on the
     * handler rather than inferred from a status code, and echoes the {@link ValidatedToken} issuer the
     * filter attached to the request.
     */
    private static final class CountingServlet extends HttpServlet {

        private final AtomicInteger invocations = new AtomicInteger();

        int invocations() {
            return invocations.get();
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            invocations.incrementAndGet();
            Object attribute = request.getAttribute(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE);
            assertNotNull(attribute, "the filter must attach the validated token before forwarding");
            assertTrue(attribute instanceof ValidatedToken,
                    "the attached attribute must be a ValidatedToken");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(DOWNSTREAM_BODY_PREFIX + ((ValidatedToken) attribute).issuer());
        }
    }

    private static final class RecordingTokenCache implements TokenCache {

        private final TokenCache delegate = new CaffeineTokenCache();
        private String lastPutKey;

        @Override
        public Optional<ValidatedToken> get(String tokenKey) {
            return delegate.get(tokenKey);
        }

        @Override
        public void put(String tokenKey, ValidatedToken token, Duration ttl) {
            lastPutKey = tokenKey;
            delegate.put(tokenKey, token, ttl);
        }

        String lastPutKey() {
            assertNotNull(lastPutKey, "accepted request must populate the token cache");
            return lastPutKey;
        }
    }

    /** A started embedded server bound to a loopback port, stopped by try-with-resources. */
    private interface ServerHandle extends AutoCloseable {
        String baseUrl();

        @Override
        void close() throws Exception;
    }

    /**
     * Jetty only, on purpose: {@link OAuthCrossContainerTest} already proves the filter registers and
     * dispatches identically on Undertow and Tomcat, so this arm varies the provider instead.
     */
    private static ServerHandle startJetty(Filter oauthFilter, HttpServlet downstream) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(downstream), "/*");
        context.addFilter(new FilterHolder(oauthFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        try {
            server.start();
        } catch (Exception startupFailure) {
            try {
                server.stop();
            } catch (Exception cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            try {
                server.destroy();
            } catch (RuntimeException cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            throw startupFailure;
        }
        int port = connector.getLocalPort();
        String baseUrl = "http://127.0.0.1:" + port + "/";
        return new ServerHandle() {
            @Override
            public String baseUrl() {
                return baseUrl;
            }

            @Override
            public void close() throws Exception {
                Exception failure = null;
                try {
                    server.stop();
                } catch (Exception stopFailure) {
                    failure = stopFailure;
                } finally {
                    try {
                        server.destroy();
                    } catch (RuntimeException destroyFailure) {
                        if (failure == null) {
                            failure = destroyFailure;
                        } else {
                            failure.addSuppressed(destroyFailure);
                        }
                    }
                }
                if (failure != null) {
                    throw failure;
                }
            }
        };
    }
}
