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

package com.viveworks.appbase.service.user.account.web.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit coverage for {@link IdpUserTokenCheckJaxrsRequestFilter}, the JAX-RS third of the three-transport
 * agreement. Tests drive the protected {@code filterRequest(requestContext)} directly (same package),
 * exercising the filter's binding logic without the JAX-RS runtime. The seam constructor supplies a
 * mock {@code AppbaseCore} so {@code appbaseCoreFromContext()} does not need the {@code Providers}
 * machinery.
 *
 * <p>Like the servlet and reactive twins, the filter gates on the SESSION's bound {@code idpUserToken}:
 * a session that carries one (authenticated user-account session) must present a matching request token
 * -- absent or mismatched both fail the binding -- while a session with no bound token (guest) has
 * nothing to bind and continues unchecked. This closes the A10-10 gap where the JAX-RS filter invoked
 * the neutral matcher unconditionally (with a null token for the absent case, and force-checking guests).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class IdpUserTokenCheckJaxrsRequestFilterTest {

    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final MetricService metricService = Mockito.mock(MetricService.class);
    private final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
    private final Counter counter = Mockito.mock(Counter.class);
    private final IdpUserTokenCheckFilter idpUserTokenCheckFilter = Mockito.mock(IdpUserTokenCheckFilter.class);
    private final ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);

    @BeforeEach
    void wireAppbaseCore() {
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
        Mockito.when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        Mockito.when(metricRegistry.counter(Mockito.nullable(String.class))).thenReturn(counter);
    }

    private IdpUserTokenCheckJaxrsRequestFilter filter() {
        return new IdpUserTokenCheckJaxrsRequestFilter(appbaseCore, idpUserTokenCheckFilter);
    }

    private void boundSession(final String boundToken) {
        final UserSession session = Mockito.mock(UserSession.class);
        Mockito.when(session.getIdpUserToken()).thenReturn(boundToken);
        Mockito.when(requestContext.getProperty(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY)).thenReturn(session);
    }

    /**
     * Stubs an empty request (no cookies, headers, or query parameters) so the token resolution finds
     * nothing without a {@link NullPointerException} on the {@link UriInfo} query-parameter path.
     */
    private void noRequestToken() {
        Mockito.when(requestContext.getCookies()).thenReturn(null);
        Mockito.when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        final UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getQueryParameters(true)).thenReturn(new MultivaluedHashMap<>());
        Mockito.when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    private void requestHeaderToken(final String token) {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, token);
        Mockito.when(requestContext.getCookies()).thenReturn(null);
        Mockito.when(requestContext.getHeaders()).thenReturn(headers);
    }

    /**
     * A10-10 regression (JAX-RS twin): a guest session that carries NO bound token must continue
     * without the filter consulting the neutral matcher or aborting the request. The old filter called
     * the matcher unconditionally, so a guest was force-checked (and, with a null token, the neutral
     * matcher rejected it). This is the behavioural failure the fix removes.
     */
    @Test
    void aSessionWithNoBoundTokenContinuesWithoutChecking() throws Exception {
        boundSession(null); // guest: no bound token
        noRequestToken();

        filter().filterRequest(requestContext);

        Mockito.verifyNoInteractions(idpUserTokenCheckFilter);
        Mockito.verify(requestContext, Mockito.never()).abortWith(Mockito.any());
    }

    /**
     * A session that carries a bound token but a request that supplies none must be rejected. Absent is
     * treated as a mismatch, so with no configuration registered it throws -- and the neutral matcher is
     * never invoked with a null token.
     */
    @Test
    void aBoundSessionTokenWithNoRequestTokenIsRejected() {
        boundSession("bound-tok");
        noRequestToken();
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);

        Assertions.assertThrows(AppbaseServiceRuntimeException.class,
                () -> filter().filterRequest(requestContext));

        Mockito.verify(idpUserTokenCheckFilter, Mockito.never())
                .idpUserTokenMatches(Mockito.isNull(), Mockito.any());
    }

    /**
     * A10-11 (CWE-598): a token supplied only in the query string must NOT be honoured -- query strings
     * leak into logs, history and referrers. It is treated as absent (a mismatch on a bound session) and
     * must never reach the neutral matcher, even though it would have matched.
     */
    @Test
    void aTokenSuppliedOnlyInTheQueryStringIsIgnoredAndRejected() {
        boundSession("bound-tok");
        final MultivaluedHashMap<String, String> query = new MultivaluedHashMap<>();
        query.putSingle(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM, "bound-tok");
        final UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getQueryParameters(true)).thenReturn(query);
        Mockito.when(requestContext.getUriInfo()).thenReturn(uriInfo);
        Mockito.when(requestContext.getCookies()).thenReturn(null);
        Mockito.when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("bound-tok"), Mockito.any()))
                .thenReturn(true);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);

        Assertions.assertThrows(AppbaseServiceRuntimeException.class,
                () -> filter().filterRequest(requestContext));

        Mockito.verify(idpUserTokenCheckFilter, Mockito.never())
                .idpUserTokenMatches(Mockito.eq("bound-tok"), Mockito.any());
    }

    @Test
    void aMatchingTokenContinues() throws Exception {
        boundSession("bound-tok");
        requestHeaderToken("tok");
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("tok"), Mockito.any())).thenReturn(true);

        filter().filterRequest(requestContext);

        Mockito.verify(idpUserTokenCheckFilter).idpUserTokenMatches(Mockito.eq("tok"), Mockito.any());
        Mockito.verify(requestContext, Mockito.never()).abortWith(Mockito.any());
    }

    /**
     * A10-10 hardening: a configuration present but with NO action set must NOT let a bound-session
     * binding failure through. It fails closed with a default 403 abort rather than continuing.
     */
    @Test
    void aMismatchWithAConfigurationButNoActionAbortsWith403() throws Exception {
        boundSession("bound-tok");
        requestHeaderToken("tok");
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("tok"), Mockito.any())).thenReturn(false);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(IdpUserTokenCheckFilterConfiguration.builder().respondForbidden(false).build());

        filter().filterRequest(requestContext);

        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(requestContext).abortWith(response.capture());
        Assertions.assertEquals(403, response.getValue().getStatus());
    }

    @Test
    void aMismatchWithRespondForbiddenAbortsWith403() throws Exception {
        boundSession("bound-tok");
        requestHeaderToken("tok");
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("tok"), Mockito.any())).thenReturn(false);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(IdpUserTokenCheckFilterConfiguration.builder().respondForbidden(true).build());

        filter().filterRequest(requestContext);

        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(requestContext).abortWith(response.capture());
        Assertions.assertEquals(403, response.getValue().getStatus());
    }
}
