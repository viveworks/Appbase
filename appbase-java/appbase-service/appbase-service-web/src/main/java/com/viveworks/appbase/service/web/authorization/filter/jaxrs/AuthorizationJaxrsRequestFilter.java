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

package com.viveworks.appbase.service.web.authorization.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.web.authorization.filter.AuthorizationFilter;
import com.viveworks.appbase.service.web.authorization.filter.DefaultAuthorizationFilter;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Applies transport-neutral authorization rules to JAX-RS requests.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
@Priority(AppbaseServiceConstants.PRIORITY_LOW)
public class AuthorizationJaxrsRequestFilter extends AbstractJaxrsRequestFilter {
    private final AuthorizationFilter authorizationFilter;

    /**
     * Test seam only (package-private): supplies {@code appbaseCoreFromContext()} directly, bypassing
     * the {@code Providers}-based JAX-RS context lookup that requires a running JAX-RS runtime. Always
     * {@code null} on every production (public) constructor path. Mirrors {@code
     * UserSessionJaxrsRequestFilter}'s test seam.
     */
    private final AppbaseCore testAppbaseCore;

    public AuthorizationJaxrsRequestFilter() {
        this.authorizationFilter = new DefaultAuthorizationFilter();
        this.testAppbaseCore = null;
    }

    public AuthorizationJaxrsRequestFilter(final AuthorizationFilter authorizationFilter) {
        AppbaseUtil.notNull(authorizationFilter, "authorizationFilter");

        this.authorizationFilter = authorizationFilter;
        this.testAppbaseCore = null;
    }

    public AuthorizationJaxrsRequestFilter(
            final ResourceInfo resourceInfo,
            final AuthorizationFilter authorizationFilter)
    {
        super(resourceInfo);

        AppbaseUtil.notNull(authorizationFilter, "authorizationFilter");
        this.authorizationFilter = authorizationFilter;
        this.testAppbaseCore = null;
    }

    /**
     * Test-seam constructor (package-private), mirroring {@code UserSessionJaxrsRequestFilter}: lets a
     * test drive the real filter with a mock {@link AuthorizationFilter} without standing up the JAX-RS
     * {@code Providers} machinery that {@link #appbaseCoreFromContext()} normally goes through.
     *
     * @param appbaseCore         Appbase core (test seam)
     * @param authorizationFilter the neutral authorization logic to delegate to (test seam)
     */
    AuthorizationJaxrsRequestFilter(final AppbaseCore appbaseCore, final AuthorizationFilter authorizationFilter) {
        this.testAppbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.authorizationFilter = AppbaseUtil.notNull(authorizationFilter, "authorizationFilter");
    }

    @Override
    protected AppbaseCore appbaseCoreFromContext() {
        return testAppbaseCore != null ? testAppbaseCore : super.appbaseCoreFromContext();
    }

    @Override
    protected void filterRequest(final ContainerRequestContext requestContext) throws IOException {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final String encodedPath = uriInfo.getPath(false);
        final String absoluteEncodedPath = encodedPath.startsWith("/") ? encodedPath : "/" + encodedPath;
        final String serviceName;
        try {
            serviceName = AppbasePathCanonicalizer.canonicalizeAuthorizationPath(absoluteEncodedPath);
        } catch (final AppbasePathException exception) {
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        // Authorization rules use one vocabulary on every HTTP transport: canonical request path
        // plus HTTP verb. ResourceInfo class/method identities and matched-resource class names are
        // deliberately ignored; preferring them lets path-keyed block rules miss in the normal
        // post-match JAX-RS path and can fall through to a non-path default-permit authorizer.
        final Request request = requestContext.getRequest();
        final String operation = request == null ? null : request.getMethod();

        String appbaseSessionId = cookieValue(requestContext, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        String appbaseRequesterId = cookieValue(requestContext, AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM);

        if (AppbaseUtil.isBlank(appbaseSessionId)) {
            appbaseSessionId = headerValue(requestContext, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        }
        if (AppbaseUtil.isBlank(appbaseRequesterId)) {
            appbaseRequesterId = headerValue(requestContext, AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM);
        }

        // Session and requester identity are read from cookies and headers only. A credential in the URL
        // query string (CWE-598) leaks into proxy/access logs, browser history and referrer headers, so
        // it is NOT honoured here.

        final AppbaseCore appbaseCore = appbaseCoreFromContext();
        final boolean authorized = authorizationFilter.isAuthorized(
                appbaseCore, serviceName, operation, appbaseSessionId, appbaseRequesterId);
        if (!authorized) {
            if (!handleAuthorizationServiceException(appbaseCore, operation, requestContext)) {
                // A denial without a configured response action must abort the request.
                sendForbidden(appbaseCore, operation, requestContext);
            }
        }
    }
}
