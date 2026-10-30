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

package com.viveworks.appbase.service.web.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.web.authorization.filter.AuthorizationFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * {@link AuthorizationWebFilter} is a thin adapter over the same {@link AuthorizationFilter} neutral
 * logic the servlet {@code AuthorizationServletFilter} delegates to. The single most important test
 * here is {@link #anUnauthorizedRequestWithNoConfigurationRegisteredIsForbidden()}: an explicit
 * authorization denial must never continue the chain, even without a response policy.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AuthorizationWebFilterTest {

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

        @Override public void responseHeader(final String name, final String value) {
            responseHeaders.put(name, value);
        }

        @Override public Mono<Void> writeJson(final Object value) {
            this.body = value;
            return Mono.empty();
        }

        @Override public <T> Mono<Void> writeStream(final Flux<T> value) { return value.then(); }

        @Override public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }

        @Override public Map<String, Object> attributes() { return attributes; }

        Object body() { return body; }

        Map<String, String> responseHeaders() { return responseHeaders; }
    }

    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final AuthorizationFilter authorizationFilter = Mockito.mock(AuthorizationFilter.class);

    private void configuration(@Nullable final AuthorizationServiceExceptionConfiguration configuration) {
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(configuration);
    }

    private void authorized(final boolean authorized) {
        Mockito.when(authorizationFilter.isAuthorized(
                        Mockito.eq(appbaseCore),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(authorized);
    }

    private AuthorizationWebFilter filter() {
        return new AuthorizationWebFilter(appbaseCore, authorizationFilter);
    }

    @Test
    void anAuthorizedRequestContinuesTheChainUntouched() {
        authorized(true);

        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();

        Assertions.assertTrue(chainRan.get(), "an authorized request must continue");
        Assertions.assertEquals(200, exchange.status());
    }

    @Test
    void anUnauthorizedRequestWithRespondForbiddenIsForbiddenAndTheChainIsNotContinued() {
        authorized(false);
        configuration(AuthorizationServiceExceptionConfiguration.builder().respondForbidden(true).build());

        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();

        Assertions.assertEquals(403, exchange.status());
        Assertions.assertNotNull(exchange.body(), "a forbidden response must write an error body");
        Assertions.assertFalse(chainRan.get(), "a forbidden request must NOT continue");
    }

    @Test
    void anUnauthorizedRequestWithARedirectConfiguredRespondsWith302AndALocationHeader() {
        authorized(false);
        configuration(AuthorizationServiceExceptionConfiguration.builder()
                .respondForbidden(false)
                .redirectUrl("https://login.example.com")
                .build());

        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();

        Assertions.assertEquals(302, exchange.status());
        Assertions.assertEquals("https://login.example.com", exchange.responseHeaders().get("Location"));
        Assertions.assertFalse(chainRan.get(), "a redirected request must NOT continue");
    }

    @Test
    void anUnauthorizedRequestWithAConfiguredStatusCodeRespondsWithThatCode() {
        authorized(false);
        configuration(AuthorizationServiceExceptionConfiguration.builder()
                .respondForbidden(false)
                .httpStatusCode(418)
                .build());

        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();

        Assertions.assertEquals(418, exchange.status());
        Assertions.assertFalse(chainRan.get(), "a configured-status request must NOT continue");
    }

    @Test
    void anUnauthorizedRequestWithNoConfigurationRegisteredIsForbidden() {
        authorized(false);
        configuration(null);

        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();

        Assertions.assertFalse(chainRan.get(), "an explicit denial must not reach the chain");
        Assertions.assertEquals(403, exchange.status());
        Assertions.assertNotNull(exchange.body());
    }

    @Test
    void anUnauthorizedRequestWithActionlessConfigurationIsForbidden() {
        authorized(false);
        configuration(AuthorizationServiceExceptionConfiguration.builder().respondForbidden(false).build());

        final RecordingExchange exchange = new RecordingExchange();
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        StepVerifier.create(filter().filter(exchange, ex -> {
            chainRan.set(true);
            return Mono.empty();
        })).verifyComplete();

        Assertions.assertFalse(chainRan.get());
        Assertions.assertEquals(403, exchange.status());
        Assertions.assertNotNull(exchange.body());
    }

    @Test
    void theSessionIdAndRequesterIdResolveCookieThenHeaderOnly() {
        authorized(true);

        // Session id present in cookie and header; requester id present only in header.
        final RecordingExchange exchange = new RecordingExchange();
        exchange.cookies.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "cookieSid");
        exchange.headers.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "headerSid");
        exchange.headers.put(AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM, "headerRid");

        StepVerifier.create(filter().filter(exchange, ex -> Mono.empty())).verifyComplete();

        final ArgumentCaptor<String> sessionId = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> requesterId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(authorizationFilter).isAuthorized(
                Mockito.eq(appbaseCore),
                Mockito.eq("/things"),
                Mockito.eq("GET"),
                sessionId.capture(),
                requesterId.capture());

        Assertions.assertEquals("cookieSid", sessionId.getValue(), "cookie wins over header");
        Assertions.assertEquals("headerRid", requesterId.getValue(), "header supplies the requester id");
    }

    /**
     * A10-11 (CWE-598): an identity supplied only in the URL query string must NOT be honoured. Query
     * strings leak into proxy/access logs, browser history and referrers, so the session/requester id
     * are resolved from cookies and headers only; a query-only value reaches the authorizer as null.
     */
    @Test
    void anIdentitySuppliedOnlyInTheQueryStringIsIgnored() {
        authorized(true);

        final RecordingExchange exchange = new RecordingExchange();
        exchange.cookies.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "cookieSid");
        exchange.queryParams.put(AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM, "queryRid");

        StepVerifier.create(filter().filter(exchange, ex -> Mono.empty())).verifyComplete();

        final ArgumentCaptor<String> requesterId = ArgumentCaptor.forClass(String.class);
        Mockito.verify(authorizationFilter).isAuthorized(
                Mockito.eq(appbaseCore),
                Mockito.eq("/things"),
                Mockito.eq("GET"),
                Mockito.eq("cookieSid"),
                requesterId.capture());

        Assertions.assertNull(requesterId.getValue(), "a query-only requester id must be ignored");
    }
}
