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

package com.viveworks.appbase.service.user.account.web.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit coverage for {@link IdpUserTokenCheckServletFilter}, the servlet twin of {@code
 * IdpUserTokenCheckWebFilter}. It runs after {@code UserSessionServletFilter} ({@code
 * USER_SESSION(500)} before {@code IDP_TOKEN_CHECK(600)}). A missing session attribute is NOT an
 * error: {@code UserSessionServletFilter} legitimately passes an anonymous request through without a
 * session, so with no session there is no {@code idpUserToken} to check and this filter must continue
 * the chain (to authorization) rather than throwing. When a session IS present the token is still
 * validated: a mismatch with no {@link IdpUserTokenCheckFilterConfiguration} registered throws, and a
 * matching (or absent) token continues the chain.
 *
 * <p>Tests drive the protected {@code doFilter(request, response, chain, appbaseCore)} overload
 * directly (same package), so they exercise the filter's own logic without the servlet-context lookup
 * and path canonicalisation of the public {@code doFilter(request, response, chain)} entry point.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class IdpUserTokenCheckServletFilterTest {

    private final IdpUserTokenCheckFilter idpUserTokenCheckFilter = Mockito.mock(IdpUserTokenCheckFilter.class);
    private final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    private final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    private final FilterChain chain = Mockito.mock(FilterChain.class);
    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final MetricService metricService = Mockito.mock(MetricService.class);
    private final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
    private final Counter counter = Mockito.mock(Counter.class);

    /** Wires the metric plumbing that {@code sendForbidden} needs to record its 403 counter. */
    private void wireMetrics() {
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
        Mockito.when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        Mockito.when(metricRegistry.counter(Mockito.nullable(String.class))).thenReturn(counter);
    }

    private IdpUserTokenCheckServletFilter filter() {
        return new IdpUserTokenCheckServletFilter(idpUserTokenCheckFilter);
    }

    /**
     * A session that carries a server-side bound {@code idpUserToken} -- i.e. an authenticated
     * user-account session, which {@code UserSession.Validator} makes invalid unless the token is
     * present. This is the session kind whose binding the filter must enforce.
     */
    private void withBoundSession(final String boundToken) {
        final UserSession session = Mockito.mock(UserSession.class);
        Mockito.when(session.getIdpUserToken()).thenReturn(boundToken);
        Mockito.when(request.getAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY)).thenReturn(session);
    }

    /**
     * The secure-by-default contract fix: an anonymous request (no session attribute) must continue to
     * the authorization filter, not be rejected here as a wiring error.
     */
    @Test
    void anAbsentSessionAttributePassesThroughToTheChain() throws Exception {
        Mockito.when(request.getAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY)).thenReturn(null);

        filter().doFilter(request, response, chain, appbaseCore);

        Mockito.verify(chain).doFilter(request, response);
        Mockito.verifyNoInteractions(idpUserTokenCheckFilter);
    }

    /**
     * A session that carries NO bound {@code idpUserToken} -- a guest session, which {@code
     * UserSession.Validator} permits without a token -- has nothing to bind, so the filter continues
     * the chain without consulting the token check. This is NOT the old bypass: the old code continued
     * for ANY session when the request omitted a token, including authenticated ones; here it continues
     * only because the session itself has no binding to enforce.
     */
    @Test
    void aSessionWithNoBoundTokenContinuesTheChain() throws Exception {
        final UserSession guestSession = Mockito.mock(UserSession.class);
        Mockito.when(guestSession.getIdpUserToken()).thenReturn(null);
        Mockito.when(request.getAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY)).thenReturn(guestSession);

        filter().doFilter(request, response, chain, appbaseCore);

        Mockito.verify(chain).doFilter(request, response);
        Mockito.verifyNoInteractions(idpUserTokenCheckFilter);
    }

    /**
     * A10-10 regression: a session that carries a bound {@code idpUserToken} (authenticated) but a
     * request that supplies NO token must be rejected, not passed through. The old behaviour gated the
     * check on the request-supplied token being present, so omitting it bypassed the binding entirely.
     * Absent-on-a-bound-session is treated as a mismatch, so with no configuration it throws.
     */
    @Test
    void aBoundSessionTokenWithNoRequestTokenIsRejected() throws Exception {
        withBoundSession("bound-tok");
        Mockito.when(request.getRequestURI()).thenReturn("/things");
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);

        Assertions.assertThrows(AppbaseServiceRuntimeException.class,
                () -> filter().doFilter(request, response, chain, appbaseCore));

        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }

    /**
     * A10-11 (CWE-598): a credential supplied only in the URL query string must NOT be honoured. Query
     * strings leak into proxy/access logs, browser history and referrers, so the filter reads the token
     * from cookies and headers only. A query-only token is therefore treated as absent -- on a bound
     * session that is a mismatch -- and it must never reach the neutral matcher.
     */
    @Test
    void aTokenSuppliedOnlyInTheQueryStringIsIgnoredAndRejected() throws Exception {
        withBoundSession("tok");
        Mockito.when(request.getParameter(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM)).thenReturn("tok");
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);

        Assertions.assertThrows(AppbaseServiceRuntimeException.class,
                () -> filter().doFilter(request, response, chain, appbaseCore));

        Mockito.verify(idpUserTokenCheckFilter, Mockito.never())
                .idpUserTokenMatches(Mockito.eq("tok"), Mockito.any());
    }

    /**
     * A10-10 hardening: a configuration present but with NO action set must NOT let a bound-session
     * binding failure through. The binding is mandatory, so it fails closed with a default 403 rather
     * than continuing the chain. (A missing configuration still throws.)
     */
    @Test
    void aMismatchWithAConfigurationButNoActionIsForbidden() throws Exception {
        withBoundSession("bound-tok");
        Mockito.when(request.getHeader(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM)).thenReturn("tok");
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("tok"), Mockito.any())).thenReturn(false);
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(IdpUserTokenCheckFilterConfiguration.builder().respondForbidden(false).build());
        wireMetrics();

        filter().doFilter(request, response, chain, appbaseCore);

        Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }

    @Test
    void aMatchingTokenContinuesTheChain() throws Exception {
        withBoundSession("tok");
        Mockito.when(request.getHeader(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM)).thenReturn("tok");
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("tok"), Mockito.any())).thenReturn(true);

        filter().doFilter(request, response, chain, appbaseCore);

        Mockito.verify(idpUserTokenCheckFilter).idpUserTokenMatches(Mockito.eq("tok"), Mockito.any());
        Mockito.verify(chain).doFilter(request, response);
    }

    /**
     * A session IS present and the token does NOT match: the token is still validated and a mismatch
     * with no {@link IdpUserTokenCheckFilterConfiguration} registered throws, exactly as before -- the
     * fix must not weaken the check on a real session.
     */
    @Test
    void aMismatchWithNoConfigurationThrowsAndDoesNotContinue() throws Exception {
        withBoundSession("bound-tok");
        Mockito.when(request.getHeader(AppbaseServiceConstants.IDP_USER_TOKEN_PARAM)).thenReturn("tok");
        Mockito.when(idpUserTokenCheckFilter.idpUserTokenMatches(Mockito.eq("tok"), Mockito.any())).thenReturn(false);
        Mockito.when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        Mockito.when(configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class))
                .thenReturn(null);

        Assertions.assertThrows(AppbaseServiceRuntimeException.class,
                () -> filter().doFilter(request, response, chain, appbaseCore));

        Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(), Mockito.any());
    }
}
