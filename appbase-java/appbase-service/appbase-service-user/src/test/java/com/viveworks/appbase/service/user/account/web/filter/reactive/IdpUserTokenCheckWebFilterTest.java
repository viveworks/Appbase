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

package com.viveworks.appbase.service.user.account.web.filter.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link IdpUserTokenCheckWebFilter} is a thin adapter over the same {@link IdpUserTokenCheckFilter}
 * neutral logic the servlet {@code IdpUserTokenCheckServletFilter} delegates to. Like the servlet
 * twin it runs after {@code UserSessionWebFilter} ({@code USER_SESSION(500)} before {@code
 * IDP_TOKEN_CHECK(600)}). A missing session attribute is NOT an error: {@code UserSessionWebFilter}
 * legitimately passes an anonymous request through without a session, so with no session there is no
 * {@code idpUserToken} to check and this filter continues the chain (to authorization) rather than
 * throwing. Matching the servlet -- NOT the brief's prose -- a token mismatch with NO configuration
 * registered throws (the servlet's {@code handleIdpUserTokenMismatch} rejects a null configuration),
 * whereas a configuration present with no action continues the chain.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class IdpUserTokenCheckWebFilterTest {

    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final IdpUserTokenCheckFilter idpUserTokenCheckFilter = Mockito.mock(IdpUserTokenCheckFilter.class);

    private IdpUserTokenCheckWebFilter filter() {
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        return new IdpUserTokenCheckWebFilter(appbaseCore, idpUserTokenCheckFilter);
    }

    private void matches(final boolean matches) {
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.anyString(), Mockito.any()))
                .thenReturn(matches);
    }

    /**
     * An anonymous request that {@code UserSessionWebFilter} passed through without a session leaves
     * no {@code USER_SESSION_PROPERTY_KEY} attribute. There is no token to check, so this filter must
     * continue the chain (letting authorization decide) rather than treating the absence as a wiring
     * error. This is the secure-by-default contract fix: an anonymous request has to reach the
     * authorization filter, not 400 here.
     */
    @Test
    void anAbsentSessionAttributePassesThroughToTheChain() {
        // no session attribute
        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        Assertions.assertTrue(chainRan.get(), "anonymous (no session) continues to authorization; it does not throw");
        Mockito.verifyNoInteractions(idpUserTokenCheckFilter);
    }

    /**
     * A session that carries NO bound {@code idpUserToken} -- a guest session, which {@code
     * UserSession.Validator} permits without a token -- has nothing to bind, so the filter continues
     * without consulting the token check. This is NOT the old bypass: the old code continued for ANY
     * session when the request omitted a token; here it continues only because there is no binding.
     */
    @Test
    void aSessionWithNoBoundTokenContinuesTheChain() {
        final RecordingExchange exchange = new RecordingExchange();
        // guest: no bound token
        exchange.withBoundSession(null);
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        Assertions.assertTrue(chainRan.get(), "no bound token means no binding to enforce; the chain continues");
        Mockito.verifyNoInteractions(idpUserTokenCheckFilter);
    }

    /**
     * A10-10 regression (reactive twin): a session that carries a bound {@code idpUserToken} but a
     * request that supplies NO token must be rejected, not passed through. Absent-on-a-bound-session is
     * treated as a mismatch, so with no configuration registered it errors.
     */
    @Test
    void aBoundSessionTokenWithNoRequestTokenIsRejected() {
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);
        final RecordingExchange exchange = new RecordingExchange();
        // no request token supplied
        exchange.withBoundSession("bound-tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .expectError(AppbaseServiceRuntimeException.class)
                .verify();

        Assertions.assertFalse(chainRan.get());
    }

    /**
     * A10-11 (CWE-598): a token supplied only in the query string must NOT be honoured -- query strings
     * leak into logs, history and referrers. It is treated as absent (a mismatch on a bound session) and
     * must never reach the neutral matcher.
     */
    @Test
    void aTokenSuppliedOnlyInTheQueryStringIsIgnoredAndRejected() {
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);
        final RecordingExchange exchange = new RecordingExchange();
        exchange.withBoundSession("tok");
        exchange.queryParams.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .expectError(AppbaseServiceRuntimeException.class)
                .verify();

        Assertions.assertFalse(chainRan.get());
        Mockito.verify(idpUserTokenCheckFilter, Mockito.never())
                .idpUserTokenMatches(Mockito.eq("tok"), Mockito.any());
    }

    @Test
    void aMatchingTokenContinuesTheChain() {
        matches(true);
        final RecordingExchange exchange = new RecordingExchange();
        exchange.withBoundSession("tok");
        exchange.headers.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        Assertions.assertTrue(chainRan.get());
    }

    @Test
    void aMismatchWithRespondForbiddenIsForbiddenAndDoesNotContinue() {
        matches(false);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(IdpUserTokenCheckFilterConfiguration.builder().respondForbidden(true).build());

        final RecordingExchange exchange = new RecordingExchange();
        exchange.withBoundSession("tok");
        exchange.headers.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        Assertions.assertEquals(403, exchange.status());
        Assertions.assertFalse(chainRan.get());
    }

    @Test
    void aMismatchWithARedirectRespondsWith302() {
        matches(false);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(IdpUserTokenCheckFilterConfiguration.builder()
                        .respondForbidden(false)
                        .redirectUrl("https://login.example.com")
                        .build());

        final RecordingExchange exchange = new RecordingExchange();
        exchange.withBoundSession("tok");
        exchange.headers.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        Assertions.assertEquals(302, exchange.status());
        Assertions.assertEquals("https://login.example.com", exchange.responseHeaders().get("Location"));
        Assertions.assertFalse(chainRan.get());
    }

    /**
     * Matches the servlet: {@code handleIdpUserTokenMismatch} throws when the {@link
     * IdpUserTokenCheckFilterConfiguration} is absent -- it does NOT pass through. This is the one
     * mismatch handler that rejects a null configuration rather than continuing.
     */
    @Test
    void aMismatchWithNoConfigurationThrows() {
        matches(false);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);

        final RecordingExchange exchange = new RecordingExchange();
        exchange.withBoundSession("tok");
        exchange.headers.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .expectError(AppbaseServiceRuntimeException.class)
                .verify();

        Assertions.assertFalse(chainRan.get());
    }

    /**
     * A10-10 hardening: a configuration present but with NO action set must NOT let a bound-session
     * binding failure through. The binding is mandatory, so an absent-or-mismatched token on a session
     * that carries a bound token fails closed with a default 403; only an explicitly configured action
     * (forbidden / redirect / status) overrides that default. (A missing configuration still throws.)
     */
    @Test
    void aMismatchWithAConfigurationButNoActionIsForbidden() {
        matches(false);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(IdpUserTokenCheckFilterConfiguration.builder()
                        .respondForbidden(false)
                        .build());

        final RecordingExchange exchange = new RecordingExchange();
        exchange.withBoundSession("tok");
        exchange.headers.put(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "tok");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, ex -> {
                    chainRan.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        Assertions.assertEquals(403, exchange.status());
        Assertions.assertFalse(chainRan.get(), "a bound-session binding failure fails closed");
    }

    private static final class RecordingExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> cookies = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();
        private final Map<String, String> responseHeaders = new LinkedHashMap<>();
        private int status = 200;
        private Object body;

        @Override
        public String method() { return "GET"; }

        @Override
        public String path() { return "/things"; }

        @Nullable
        @Override
        public String queryParam(final String name) { return queryParams.get(name); }

        @Nullable
        @Override
        public String header(final String name) { return headers.get(name); }

        @Nullable
        @Override
        public String cookie(final String name) { return cookies.get(name); }

        @Override
        public void status(final int code) { this.status = code; }

        int status() { return status; }

        @Override
        public void responseHeader(final String name, final String value) {
            responseHeaders.put(name, value);
        }

        Map<String, String> responseHeaders() { return responseHeaders; }

        @Override
        public Mono<Void> writeJson(final Object value) {
            this.body = value;
            return Mono.empty();
        }

        @Override
        public <T> Mono<Void> writeStream(final Flux<T> value) { return value.then(); }

        @Override
        public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }

        @Override
        public Map<String, Object> attributes() { return attributes; }

        /**
         * A session that carries a server-side bound {@code idpUserToken} -- an authenticated
         * user-account session, whose binding the filter must enforce. Pass {@code null} for a guest
         * session that has no bound token.
         */
        void withBoundSession(final String boundToken) {
            final UserSession session = Mockito.mock(UserSession.class);
            Mockito.when(session.getIdpUserToken()).thenReturn(boundToken);
            attributes.put(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, session);
        }
    }
}
