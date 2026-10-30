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

package com.viveworks.appbase.service.user.session.web.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.data.RenewSessionResponse;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredExceptionConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Carries the JAX-RS third of the three-face agreement (amendment A3): {@link
 * #theJaxrsFaceHandsTheCanonicalPathToTheNeutralFilter()} drives the production {@link
 * UserSessionJaxrsRequestFilter} end to end and captures what actually reaches the neutral {@link
 * UserSessionFilter#onRequest}. It does NOT stub {@code UriInfo} and then canonicalize the stub's
 * return value -- that would pass even if the filter still called the decoding {@code getPath()}
 * (amendment A2).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class UserSessionJaxrsRequestFilterTest {

    private final AppbaseCore appbaseCore = mock(AppbaseCore.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final MetricService metricService = mock(MetricService.class);
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);
    private final ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

    @BeforeEach
    void wireAppbaseCore() {
        when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        when(appbaseCore.getMetricService()).thenReturn(metricService);
        when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        // nullable(), not anyString(): MetricService#name(...) is a default interface method, which a
        // Mockito mock does NOT execute for real, so it returns null here -- the matcher must accept
        // that null metric name too.
        when(metricRegistry.counter(nullable(String.class))).thenReturn(counter);
        when(containerRequestContext.getMethod()).thenReturn("GET");
    }

    @Test
    void theJaxrsFaceHandsTheCanonicalPathToTheNeutralFilter() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath(false)).thenReturn("/%61dmin");
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        final UserSessionFilter neutral = mock(UserSessionFilter.class);
        new UserSessionJaxrsRequestFilter(appbaseCore, neutral).filter(containerRequestContext);

        final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(neutral).onRequest(path.capture(), any(), any(), any());

        // Same wire path "/%61dmin" and expected key "/admin" as NettyWebExchangeTest (Task 4) and
        // AuthorizationServletFilterTest (Task 5). If these three literals drift, the faces have.
        assertEquals("/admin", path.getValue());
    }

    /**
     * A10-11 (CWE-598): an identity supplied in the URL query string is rejected. Query strings leak
     * into proxy/access logs, browser history and referrers.
     */
    @Test
    void anIdentitySuppliedInTheQueryStringIsRejected() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath(false)).thenReturn("/things");
        final MultivaluedHashMap<String, String> query = new MultivaluedHashMap<>();
        query.putSingle(AppbaseServiceConstants.APPBASE_USER_ID_PARAM, "queryUid");
        when(uriInfo.getQueryParameters(false)).thenReturn(query);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        final UserSessionFilter neutral = mock(UserSessionFilter.class);
        new UserSessionJaxrsRequestFilter(appbaseCore, neutral).filter(containerRequestContext);

        verify(containerRequestContext).abortWith(any());
        verify(neutral, never()).onRequest(any(), any(), any(), any());
    }

    @Test
    void aSuccessfullyRenewedExpiredSessionContinuesWithoutRethrowing() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath(false)).thenReturn("/things");
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);

        final UserSession expired = mock(UserSession.class);
        final UserSession renewed = mock(UserSession.class);
        when(renewed.getAppbaseSessionId()).thenReturn("newSid");
        final UserSessionFilter neutral = mock(UserSessionFilter.class);
        when(neutral.onRequest(any(), any(), any(), any()))
                .thenThrow(new UserSessionExpiredException(expired));
        when(configurationService.getTypedConfiguration(UserSessionExpiredExceptionConfiguration.class))
                .thenReturn(UserSessionExpiredExceptionConfiguration.builder().renewSession(true).build());

        final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        final AppbaseServiceEntry serviceEntry = mock(AppbaseServiceEntry.class);
        final UserAccountService userAccountService = mock(UserAccountService.class);
        final RenewSessionResponse renewSessionResponse = mock(RenewSessionResponse.class);
        when(appbaseCore.getServiceRegistry()).thenReturn(serviceRegistry);
        when(serviceRegistry.getAppbaseService(UserAccountService.class)).thenReturn(serviceEntry);
        when(serviceEntry.getService()).thenReturn(userAccountService);
        when(userAccountService.renewSession(any())).thenReturn(renewSessionResponse);
        when(renewSessionResponse.getRenewedUserSession()).thenReturn(renewed);

        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final TestableUserSessionJaxrsRequestFilter filter =
                new TestableUserSessionJaxrsRequestFilter(appbaseCore, neutral, servletRequest);

        assertDoesNotThrow(() -> filter.filter(containerRequestContext));
        verify(containerRequestContext).setProperty(
                AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM, "newSid");
        verify(containerRequestContext).setProperty(
                AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "newSid");
        verify(containerRequestContext).setProperty(
                AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, renewed);
        verify(servletRequest).setAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, renewed);
    }

    private static final class TestableUserSessionJaxrsRequestFilter extends UserSessionJaxrsRequestFilter {
        private TestableUserSessionJaxrsRequestFilter(
                final AppbaseCore appbaseCore,
                final UserSessionFilter userSessionFilter,
                final HttpServletRequest servletRequest)
        {
            super(appbaseCore, userSessionFilter);
            this.httpServletRequest = servletRequest;
        }
    }
}
