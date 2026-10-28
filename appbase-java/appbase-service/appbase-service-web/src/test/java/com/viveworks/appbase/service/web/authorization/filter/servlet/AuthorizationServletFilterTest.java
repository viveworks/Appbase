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

package com.viveworks.appbase.service.web.authorization.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.web.authorization.filter.AuthorizationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Carries the D1 bypass regression test: the servlet container does NOT decode {@code
 * HttpServletRequest#getRequestURI()}, so handing that raw value straight into an exact-string
 * blocklist lookup lets an encoded ({@code /%61dmin}) or dotted ({@code /x/../admin}) request slip
 * past a rule written as {@code /admin}. {@link #anEncodedPathDoesNotBypassABlocklistEntry()} and
 * {@link #aTraversalPathDoesNotBypassABlocklistEntry()} fail before the production fix -- the chain
 * IS invoked, because the raw path misses the canonical key -- and that failure IS the live defect
 * being fixed, not a test bug.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AuthorizationServletFilterTest {

    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final ServletContext servletContext = mock(ServletContext.class);
    private final AppbaseCore appbaseCore = mock(AppbaseCore.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final MetricService metricService = mock(MetricService.class);
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);

    @BeforeEach
    void wireAppbaseCore() {
        when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        when(appbaseCore.getMetricService()).thenReturn(metricService);
        when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        // nullable(), not anyString(): MetricService#name(...) is a default interface method, which a
        // Mockito mock does NOT execute for real, so it returns null here -- the matcher must accept
        // that null metric name too.
        when(metricRegistry.counter(nullable(String.class))).thenReturn(counter);
        when(servletContext.getAttribute(AppbaseServiceConstants.APPBASE_CORE_ATTRIBUTE)).thenReturn(appbaseCore);
    }

    private HttpServletRequest request(
            final String requestUri,
            final String contextPath,
            final String method)
    {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getContextPath()).thenReturn(contextPath);
        when(request.getMethod()).thenReturn(method);
        when(request.getServletContext()).thenReturn(servletContext);
        return request;
    }

    /**
     * Builds an {@link AuthorizationFilter} that reproduces a real blocklist entry for {@code
     * blockedPath} using the same exact-string equality {@code DefaultAuthorizationService} applies
     * against its blocked-service map -- {@code isAuthorized} denies only an EXACT match, exactly the
     * property that lets an undecoded {@code /%61dmin} slip past a canonical {@code /admin} entry.
     *
     * <p>Also registers an {@link AuthorizationServiceExceptionConfiguration} with {@code
     * respondForbidden = true} on the shared {@code appbaseCore} to verify the explicit policy.
     *
     * @param blockedPath the canonical path the blocklist denies
     * @return an {@link AuthorizationFilter} test seam wired to that blocklist
     */
    private AuthorizationFilter appbaseCoreBlocking(final String blockedPath) {
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder().respondForbidden(true).build());

        final AuthorizationFilter authorizationFilter = mock(AuthorizationFilter.class);
        when(authorizationFilter.isAuthorized(any(), any(String.class), any(), any(), any()))
                .thenAnswer(invocation -> !blockedPath.equals(invocation.getArgument(1)));
        return authorizationFilter;
    }

    private AuthorizationFilter denyingFilter() {
        return mock(AuthorizationFilter.class);
    }

    @Test
    void deniedRequestWithoutPolicyIsForbidden() throws Exception {
        new AuthorizationServletFilter(denyingFilter()).doFilter(request("/things", "", "GET"), response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
    }

    @Test
    void deniedRequestWithActionlessPolicyIsForbidden() throws Exception {
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder().respondForbidden(false).build());

        new AuthorizationServletFilter(denyingFilter()).doFilter(request("/things", "", "GET"), response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
    }

    @Test
    void deniedRequestWithRedirectPolicyRetainsRedirect() throws Exception {
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder()
                        .respondForbidden(false).redirectUrl("https://login.example.com").build());

        new AuthorizationServletFilter(denyingFilter()).doFilter(request("/things", "", "GET"), response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).sendRedirect("https://login.example.com");
    }

    @Test
    void deniedRequestWithCustomStatusRetainsStatus() throws Exception {
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder()
                        .respondForbidden(false).httpStatusCode(418).build());

        new AuthorizationServletFilter(denyingFilter()).doFilter(request("/things", "", "GET"), response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(418);
    }

    @Test
    void anEncodedPathDoesNotBypassABlocklistEntry() throws Exception {
        final HttpServletRequest request = request("/%61dmin", "", "GET");

        // appbaseCore is configured with a blocklist entry for "/admin"
        final AuthorizationServletFilter filter = new AuthorizationServletFilter(appbaseCoreBlocking("/admin"));
        filter.doFilter(request, response, chain);

        // BEFORE the production fix this fails: request.getRequestURI() hands the raw "/%61dmin"
        // straight to the blocklist lookup, "/%61dmin" != "/admin", so isAuthorized returns true and
        // the chain runs. That is the live D1 bypass.
        verify(chain, never()).doFilter(any(), any());
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
    }

    @Test
    void aTraversalPathDoesNotBypassABlocklistEntry() throws Exception {
        final HttpServletRequest request = request("/x/../admin", "", "GET");

        final AuthorizationServletFilter filter = new AuthorizationServletFilter(appbaseCoreBlocking("/admin"));
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
    }

    @Test
    void theServletFaceHandsTheCanonicalPathToTheNeutralFilter() throws Exception {
        final HttpServletRequest request = request("/%61dmin", "", "GET");

        final AuthorizationFilter neutral = mock(AuthorizationFilter.class);
        when(neutral.isAuthorized(any(), any(String.class), any(), any(), any())).thenReturn(true);
        new AuthorizationServletFilter(neutral).doFilter(request, response, chain);

        final ArgumentCaptor<String> service = ArgumentCaptor.forClass(String.class);
        verify(neutral).isAuthorized(any(), service.capture(), any(), any(), any());

        // Same wire path "/%61dmin" and expected key "/admin" as NettyWebExchangeTest (Task 4) and
        // UserSessionJaxrsRequestFilterTest (Task 6) -- this is the servlet third of the three-face
        // agreement (amendment A3).
        assertEquals("/admin", service.getValue());
        verify(chain).doFilter(request, response);
    }

    /**
     * A10-11 (CWE-598): an identity supplied only in the URL query string must NOT be honoured. Query
     * strings leak into proxy/access logs, browser history and referrers, so the session/requester id
     * are read from cookies and headers only; a query-only value reaches the authorizer as null.
     */
    @Test
    void anIdentitySuppliedOnlyInTheQueryStringIsIgnored() throws Exception {
        final HttpServletRequest request = request("/things", "", "GET");
        when(request.getHeader(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM)).thenReturn("headerSid");
        when(request.getParameter(AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM)).thenReturn("queryRid");

        final AuthorizationFilter neutral = mock(AuthorizationFilter.class);
        when(neutral.isAuthorized(any(), any(String.class), any(), any(), any())).thenReturn(true);
        new AuthorizationServletFilter(neutral).doFilter(request, response, chain);

        // Session id from the header; requester id null -- the query-only requester id is ignored.
        verify(neutral).isAuthorized(any(), eq("/things"), eq("GET"), eq("headerSid"), isNull());
        verify(chain).doFilter(request, response);
    }

    /**
     * A path that cannot be reduced to canonical form (here, a trailing bare {@code %} that is not a
     * valid percent-escape) is rejected outright by {@code AbstractAppbaseServletFilter}, before this
     * filter -- or any downstream filter -- ever runs. The request never reaches the authorization
     * decision at all, so this is neither an allow nor a deny: it is a 400.
     */
    @Test
    void aMalformedEscapeIsRejectedWithA400AndTheChainIsNotContinued() throws Exception {
        final HttpServletRequest request = request("/admin%", "", "GET");

        final AuthorizationFilter neutral = mock(AuthorizationFilter.class);
        new AuthorizationServletFilter(neutral).doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
    }
}
