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

package com.viveworks.appbase.service.web.authorization.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.web.authorization.filter.AuthorizationFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for {@link AuthorizationJaxrsRequestFilter} focused on the A10-11 transport rule: the
 * session/requester id are resolved from cookies and headers only. Drives the protected {@code
 * filterRequest} directly (same package); the seam constructor supplies a mock {@code AppbaseCore} so
 * {@code appbaseCoreFromContext()} does not need the {@code Providers} machinery.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AuthorizationJaxrsRequestFilterTest {

    private final AppbaseCore appbaseCore = mock(AppbaseCore.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final MetricService metricService = mock(MetricService.class);
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final Counter counter = mock(Counter.class);
    private final AuthorizationFilter authorizationFilter = mock(AuthorizationFilter.class);
    private final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    private final UriInfo uriInfo = mock(UriInfo.class);

    @BeforeEach
    void wireAppbaseCore() {
        when(appbaseCore.getConfigurationService()).thenReturn(configurationService);
        when(appbaseCore.getMetricService()).thenReturn(metricService);
        when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        when(metricRegistry.counter(nullable(String.class))).thenReturn(counter);
    }

    private void deniedRequest() {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath(false)).thenReturn("things");
        final Request request = mock(Request.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(request.getMethod()).thenReturn("GET");
    }

    private Response deniedResponse() throws Exception {
        new TestFilter(appbaseCore, authorizationFilter).invoke(requestContext);
        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(response.capture());
        return response.getValue();
    }

    @Test
    void deniedRequestWithoutPolicyIsForbidden() throws Exception {
        deniedRequest();
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), deniedResponse().getStatus());
    }

    @Test
    void deniedRequestWithActionlessPolicyIsForbidden() throws Exception {
        deniedRequest();
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder().respondForbidden(false).build());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), deniedResponse().getStatus());
    }

    @Test
    void deniedRequestWithRedirectPolicyRetainsRedirect() throws Exception {
        deniedRequest();
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder()
                        .respondForbidden(false).redirectUrl("https://login.example.com").build());
        final Response response = deniedResponse();
        assertEquals(Response.Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
        assertEquals("https://login.example.com", response.getLocation().toString());
    }

    @Test
    void deniedRequestWithCustomStatusRetainsStatus() throws Exception {
        deniedRequest();
        when(configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class))
                .thenReturn(AuthorizationServiceExceptionConfiguration.builder()
                        .respondForbidden(false).httpStatusCode(418).build());
        assertEquals(418, deniedResponse().getStatus());
    }

    @Test
    void encodedBaseRelativeFallbackReachesAuthorizationAsCanonicalAbsolutePath() throws Exception {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getMatchedResources()).thenReturn(List.of());
        when(uriInfo.getPath(false)).thenReturn("%61dmin/.");
        final Request request = mock(Request.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(request.getMethod()).thenReturn("GET");
        when(authorizationFilter.isAuthorized(any(), anyString(), any(), any(), any())).thenReturn(true);

        new TestFilter(appbaseCore, authorizationFilter).invoke(requestContext);

        final ArgumentCaptor<String> service = ArgumentCaptor.forClass(String.class);
        verify(authorizationFilter).isAuthorized(any(), service.capture(), any(), any(), any());
        assertEquals("/admin", service.getValue());
    }

    @Test
    void malformedEncodedSeparatorIsAbortedBeforeAuthorization() throws Exception {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getMatchedResources()).thenReturn(List.of());
        when(uriInfo.getPath(false)).thenReturn("admin%2Fescape");

        new TestFilter(appbaseCore, authorizationFilter).invoke(requestContext);

        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(response.capture());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getValue().getStatus());
        verify(authorizationFilter, never()).isAuthorized(any(), anyString(), any(), any(), any());
    }

    @Test
    void malformedUtf8AbsolutePathAbortsBeforeAuthorization() throws Exception {
        assertMalformedUtf8AbortsBeforeAuthorization("/%C3%28");
    }

    @Test
    void malformedUtf8BaseRelativeFallbackAbortsBeforeAuthorization() throws Exception {
        assertMalformedUtf8AbortsBeforeAuthorization("%E2%28%A1");
    }

    /**
     * CP10-01: the production JAX-RS path supplies {@link ResourceInfo}, but authorization policy is
     * keyed by canonical request path and HTTP verb. Resource class/method identifiers must never
     * replace that vocabulary or a block such as {@code DELETE /admin} can miss its configured rule.
     * Keeping matched resources populated also proves that fallback cannot reintroduce a class name.
     */
    @Test
    void resourceInfoAndMatchedResourceStillAuthorizeCanonicalPathAndHttpVerb() throws Exception {
        final ResourceInfo resourceInfo = mock(ResourceInfo.class);
        doReturn(AdminResource.class).when(resourceInfo).getResourceClass();
        when(resourceInfo.getResourceMethod()).thenReturn(AdminResource.class.getDeclaredMethod("wipeDatabase"));

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getMatchedResources()).thenReturn(List.of(new AdminResource()));
        when(uriInfo.getPath(false)).thenReturn("admin/.");
        final Request request = mock(Request.class);
        when(requestContext.getRequest()).thenReturn(request);
        when(request.getMethod()).thenReturn("DELETE");
        when(authorizationFilter.isAuthorized(any(), anyString(), any(), any(), any())).thenReturn(true);

        new TestFilter(resourceInfo, appbaseCore, authorizationFilter).invoke(requestContext);

        verify(authorizationFilter).isAuthorized(
                eq(appbaseCore), eq("/admin"), eq("DELETE"), isNull(), isNull());
    }

    private void assertMalformedUtf8AbortsBeforeAuthorization(final String encodedPath) throws Exception {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getMatchedResources()).thenReturn(List.of());
        when(uriInfo.getPath(false)).thenReturn(encodedPath);

        new TestFilter(appbaseCore, authorizationFilter).invoke(requestContext);

        final ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(response.capture());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getValue().getStatus());
        verify(authorizationFilter, never()).isAuthorized(any(), anyString(), any(), any(), any());
    }

    /**
     * A10-11 (CWE-598): an identity supplied only in the URL query string must NOT be honoured. The
     * session/requester id are read from cookies and headers only; a query-only value reaches the
     * authorizer as null. Query strings leak into proxy/access logs, browser history and referrers.
     */
    @Test
    void anIdentitySuppliedOnlyInTheQueryStringIsIgnored() throws Exception {
        when(uriInfo.getMatchedResources()).thenReturn(List.of());
        when(uriInfo.getPath(false)).thenReturn("/things");
        final MultivaluedHashMap<String, String> query = new MultivaluedHashMap<>();
        query.putSingle(AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM, "queryRid");
        when(uriInfo.getQueryParameters(true)).thenReturn(query);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        final Request request = mock(Request.class);
        when(request.getMethod()).thenReturn("GET");
        when(requestContext.getRequest()).thenReturn(request);

        when(requestContext.getCookies()).thenReturn(null);
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM, "headerSid");
        when(requestContext.getHeaders()).thenReturn(headers);

        when(authorizationFilter.isAuthorized(any(), any(String.class), any(), any(), any())).thenReturn(true);

        new TestFilter(appbaseCore, authorizationFilter).invoke(requestContext);

        // Session id from the header; requester id null -- the query-only requester id is ignored.
        verify(authorizationFilter).isAuthorized(eq(appbaseCore), eq("/things"), eq("GET"), eq("headerSid"), isNull());
    }

    private static final class TestFilter extends AuthorizationJaxrsRequestFilter {
        private final AppbaseCore appbaseCore;

        private TestFilter(final AppbaseCore appbaseCore, final AuthorizationFilter authorizationFilter) {
            super(authorizationFilter);
            this.appbaseCore = appbaseCore;
        }

        private TestFilter(
                final ResourceInfo resourceInfo,
                final AppbaseCore appbaseCore,
                final AuthorizationFilter authorizationFilter)
        {
            super(resourceInfo, authorizationFilter);
            this.appbaseCore = appbaseCore;
        }

        @Override
        protected AppbaseCore appbaseCoreFromContext() {
            return appbaseCore;
        }

        private void invoke(final ContainerRequestContext context) throws IOException {
            filterRequest(context);
        }
    }

    private static final class AdminResource {
        @SuppressWarnings("unused")
        private void wipeDatabase() {
            // Method identity is deliberately different from the HTTP verb used by authorization.
        }
    }
}
