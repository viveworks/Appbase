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

package com.viveworks.appbase.service.user.session.web.filter.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.UserAccountServiceExceptionConfiguration;
import com.viveworks.appbase.service.user.account.data.RenewSessionResponse;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredExceptionConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundExceptionConfiguration;
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
 * {@link UserSessionWebFilter} is a thin adapter over the SAME {@link UserSessionFilter} neutral
 * logic the servlet {@code UserSessionServletFilter} delegates to. These tests pin the parts of the
 * servlet control flow that are easy to get subtly wrong on a reactive chain: the resolved session
 * lands in {@code attributes()} under {@link AppbaseServiceConstants#USER_SESSION_PROPERTY_KEY};
 * {@code onResponse} runs after a SUCCESSFUL chain and NOT when the chain errors (a {@code
 * doFinally} would wrongly run on both and on cancel); the renew path sets the {@code
 * newAppbaseSessionId} response header; and -- matching the servlet, NOT the task brief's prose --
 * an unconfigured checked exception ERRORS (the servlet falls through to {@code throw new
 * AppbaseServiceRuntimeException(ex)}), whereas a null session RETURN with no configuration
 * continues the chain.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class UserSessionWebFilterTest {

    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final UserSessionFilter userSessionFilter = Mockito.mock(UserSessionFilter.class);

    private UserSessionWebFilter filter() {
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        return new UserSessionWebFilter(appbaseCore, userSessionFilter);
    }

    private static AppbaseServiceFilterChain recordingChain(final AtomicBoolean flag) {
        return ex -> {
            flag.set(true);
            return Mono.empty();
        };
    }

    @Test
    void aResolvedSessionIsStoredInAttributesTheChainContinuesAndOnResponseRunsAfterwards() throws Exception {
        final UserSession session = Mockito.mock(UserSession.class);
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(session);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertSame(session, exchange.attributes().get(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY));
        Assertions.assertTrue(chainRan.get(), "a resolved session must continue the chain");
        Mockito.verify(userSessionFilter).onResponse(appbaseCore, session);
    }

    @Test
    void onResponseDoesNotRunWhenTheChainErrors() throws Exception {
        final UserSession session = Mockito.mock(UserSession.class);
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(session);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AppbaseServiceFilterChain failing = ex -> Mono.error(new IllegalStateException("boom"));

        StepVerifier.create(filter().filter(exchange, failing))
                .expectError(IllegalStateException.class)
                .verify();

        Mockito.verify(userSessionFilter, Mockito.never()).onResponse(Mockito.any(), Mockito.any());
    }

    @Test
    void theSessionIdAndUserIdResolveCookieThenHeaderOnly() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(UserSession.class));

        final RecordingExchange exchange = new RecordingExchange("/things");
        exchange.cookies.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "cookieSid");
        exchange.headers.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "headerSid");
        exchange.headers.put(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, "headerUid");

        StepVerifier.create(filter().filter(exchange, ex -> Mono.empty())).verifyComplete();

        Mockito.verify(userSessionFilter).onRequest("/things", appbaseCore, "cookieSid", "headerUid");
    }

    /**
     * A10-11 (CWE-598): an identity supplied only in the URL query string must NOT be honoured. Query
     * strings leak into proxy/access logs, browser history and referrers, so the session/user id are
     * resolved from cookies and headers only; a query-only value reaches the neutral filter as null.
     */
    @Test
    void anIdentitySuppliedOnlyInTheQueryStringIsIgnored() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(UserSession.class));

        final RecordingExchange exchange = new RecordingExchange("/things");
        exchange.cookies.put(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "cookieSid");
        exchange.queryParams.put(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, "queryUid");

        StepVerifier.create(filter().filter(exchange, ex -> Mono.empty())).verifyComplete();

        Mockito.verify(userSessionFilter).onRequest("/things", appbaseCore, "cookieSid", null);
    }

    @Test
    void aNullSessionReturnWithNotFoundConfiguredRespondsWith400AndDoesNotContinue() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(null);
        Mockito.when(configurationService.getTypedConfiguration(UserSessionNotFoundExceptionConfiguration.class))
                .thenReturn(UserSessionNotFoundExceptionConfiguration.builder().respondBadRequest(true).build());

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertEquals(400, exchange.status());
        Assertions.assertNotNull(exchange.body());
        Assertions.assertFalse(chainRan.get(), "a bad-request response must not continue");
    }

    @Test
    void aNullSessionReturnWithNoConfigurationContinuesTheChainWithoutAttributeOrOnResponse() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(null);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertTrue(chainRan.get(), "an unconfigured null session passes through");
        Assertions.assertFalse(exchange.attributes().containsKey(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY));
        Mockito.verify(userSessionFilter, Mockito.never()).onResponse(Mockito.any(), Mockito.any());
    }

    @Test
    void aNotFoundExceptionWithConfigurationRespondsWith400AndDoesNotContinue() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new UserSessionNotFoundException("missing"));
        Mockito.when(configurationService.getTypedConfiguration(UserSessionNotFoundExceptionConfiguration.class))
                .thenReturn(UserSessionNotFoundExceptionConfiguration.builder().respondBadRequest(true).build());

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertEquals(400, exchange.status());
        Assertions.assertFalse(chainRan.get());
    }

    /**
     * Matches the servlet, NOT the brief's prose: a checked exception with no configured action
     * falls through to {@code throw new AppbaseServiceRuntimeException(ex)} in the servlet. It does
     * NOT pass the request through. (The pass-through path in the servlet is the null-RETURN case,
     * covered separately.)
     */
    @Test
    void aNotFoundExceptionWithNoConfigurationErrorsRatherThanContinuing() throws Exception {
        final UserSessionNotFoundException cause = new UserSessionNotFoundException("missing");
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(cause);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan)))
                .expectError(AppbaseServiceRuntimeException.class)
                .verify();

        Assertions.assertFalse(chainRan.get(), "an unconfigured checked exception must not continue");
    }

    @Test
    void aUserAccountServiceExceptionWithConfigurationRespondsWith500() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new UserAccountServiceException("account down"));
        Mockito.when(configurationService.getTypedConfiguration(UserAccountServiceExceptionConfiguration.class))
                .thenReturn(UserAccountServiceExceptionConfiguration.builder().respondInternalServerError(true).build());

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertEquals(500, exchange.status());
        Assertions.assertFalse(chainRan.get());
    }

    @Test
    void anAuthorizationServiceExceptionWithConfigurationRespondsWith403() throws Exception {
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new AuthorizationServiceException("nope"));
        Mockito.when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder().respondForbidden(true).build());

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertEquals(403, exchange.status());
        Assertions.assertFalse(chainRan.get());
    }

    @Test
    void anExpiredSessionOnTheRenewPathSetsTheNewSessionIdHeaderAndContinues() throws Exception {
        final UserSession expired = Mockito.mock(UserSession.class);
        final UserSession renewed = Mockito.mock(UserSession.class);
        Mockito.when(renewed.getAppbaseSessionId()).thenReturn("newSid");
        Mockito.when(userSessionFilter.onRequest(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new UserSessionExpiredException(expired));
        Mockito.when(configurationService.getTypedConfiguration(UserSessionExpiredExceptionConfiguration.class))
                .thenReturn(UserSessionExpiredExceptionConfiguration.builder().renewSession(true).build());

        final ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);
        final AppbaseServiceEntry serviceEntry = Mockito.mock(AppbaseServiceEntry.class);
        final UserAccountService userAccountService = Mockito.mock(UserAccountService.class);
        final RenewSessionResponse renewSessionResponse = Mockito.mock(RenewSessionResponse.class);
        Mockito.when(appbaseCore.getServiceRegistry()).thenReturn(serviceRegistry);
        Mockito.when(serviceRegistry.getAppbaseService(UserAccountService.class)).thenReturn(serviceEntry);
        Mockito.when(serviceEntry.getService()).thenReturn(userAccountService);
        Mockito.when(userAccountService.renewSession(Mockito.any())).thenReturn(renewSessionResponse);
        Mockito.when(renewSessionResponse.getRenewedUserSession()).thenReturn(renewed);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);

        StepVerifier.create(filter().filter(exchange, recordingChain(chainRan))).verifyComplete();

        Assertions.assertEquals("newSid",
                exchange.responseHeaders().get(AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM));
        Assertions.assertSame(renewed, exchange.attributes().get(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY));
        Assertions.assertTrue(chainRan.get(), "a renewed session continues the chain");
        Mockito.verify(userSessionFilter).onResponse(appbaseCore, renewed);
    }

    private static final class RecordingExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> cookies = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();
        private final Map<String, String> responseHeaders = new LinkedHashMap<>();
        private final String path;
        private int status = 200;
        private Object body;

        private RecordingExchange(final String path) {
            this.path = path;
        }

        @Override
        public String method() { return "GET"; }

        @Override
        public String path() { return path; }

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

        Object body() { return body; }

        @Override
        public <T> Mono<Void> writeStream(final Flux<T> value) { return value.then(); }

        @Override
        public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }

        @Override
        public Map<String, Object> attributes() { return attributes; }
    }
}
