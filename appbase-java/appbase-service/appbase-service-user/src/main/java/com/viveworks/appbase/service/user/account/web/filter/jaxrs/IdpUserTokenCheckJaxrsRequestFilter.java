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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.user.account.web.filter.DefaultIdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Provider
@Priority(AppbaseServiceConstants.PRIORITY_LOW)
public class IdpUserTokenCheckJaxrsRequestFilter extends AbstractJaxrsRequestFilter {
    private final IdpUserTokenCheckFilter idpUserTokenCheckFilter;

    /**
     * Test seam only (package-private): supplies {@code appbaseCoreFromContext()} directly, bypassing
     * the {@code Providers}-based JAX-RS context lookup that requires a running JAX-RS runtime. Always
     * {@code null} on every production (public) constructor path. Mirrors {@code
     * UserSessionJaxrsRequestFilter}'s test seam.
     */
    private final AppbaseCore testAppbaseCore;

    public IdpUserTokenCheckJaxrsRequestFilter() {
        this.idpUserTokenCheckFilter = new DefaultIdpUserTokenCheckFilter();
        this.testAppbaseCore = null;
    }

    public IdpUserTokenCheckJaxrsRequestFilter(final IdpUserTokenCheckFilter idpUserTokenCheckFilter) {
        AppbaseUtil.notNull(idpUserTokenCheckFilter, "idpUserTokenCheckFilter");

        this.idpUserTokenCheckFilter = idpUserTokenCheckFilter;
        this.testAppbaseCore = null;
    }

    public IdpUserTokenCheckJaxrsRequestFilter(
            final ResourceInfo resourceInfo,
            final IdpUserTokenCheckFilter idpUserTokenCheckFilter)
    {
        super(resourceInfo);

        AppbaseUtil.notNull(idpUserTokenCheckFilter, "idpUserTokenCheckFilter");
        this.idpUserTokenCheckFilter = idpUserTokenCheckFilter;
        this.testAppbaseCore = null;
    }

    /**
     * Test-seam constructor (package-private), mirroring {@code UserSessionJaxrsRequestFilter}: lets a
     * test drive the real filter with a mock {@link IdpUserTokenCheckFilter} without standing up the
     * JAX-RS {@code Providers} machinery that {@link #appbaseCoreFromContext()} normally goes through.
     *
     * @param appbaseCore             Appbase core (test seam)
     * @param idpUserTokenCheckFilter the neutral token-check logic to delegate to (test seam)
     */
    IdpUserTokenCheckJaxrsRequestFilter(
            final AppbaseCore appbaseCore,
            final IdpUserTokenCheckFilter idpUserTokenCheckFilter)
    {
        this.testAppbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.idpUserTokenCheckFilter = AppbaseUtil.notNull(idpUserTokenCheckFilter, "idpUserTokenCheckFilter");
    }

    @Override
    protected AppbaseCore appbaseCoreFromContext() {
        return testAppbaseCore != null ? testAppbaseCore : super.appbaseCoreFromContext();
    }

    @Override
    protected void filterRequest(final ContainerRequestContext requestContext) throws IOException {
        final Object property = requestContext.getProperty(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY);
        if (property == null) {
            throw new AppbaseServiceRuntimeException("Cannot find session for: " + requestContext.getUriInfo());
        }
        final UserSession userSession = (UserSession) property;

        // Gate on the SESSION's bound token, not on the request-supplied one (see the servlet and
        // reactive twins). A session that carries a bound idpUserToken is an authenticated user-account
        // session whose binding must be enforced: the caller must present a matching token, so an ABSENT
        // request token fails the binding exactly as a MISMATCHED one does. A session with no bound token
        // (a guest session) has nothing to bind and continues unchecked. This also stops the neutral
        // matcher being invoked with a null token, which it rejects with a raw validation error.
        final String boundIdpUserToken = userSession.getIdpUserToken();
        if (!AppbaseUtil.isBlank(boundIdpUserToken)) {
            // Read the token from cookies and headers only. A query-string credential (CWE-598) leaks
            // into proxy/access logs, browser history and referrers, so it is NOT honoured here.
            String idpUserToken = cookieValue(requestContext, AppbaseServiceConstants.IDP_USER_TOKEN_PARAM);
            if (AppbaseUtil.isBlank(idpUserToken)) {
                idpUserToken = headerValue(requestContext, AppbaseServiceConstants.IDP_USER_TOKEN_PARAM);
            }

            if (AppbaseUtil.isBlank(idpUserToken)
                    || !idpUserTokenCheckFilter.idpUserTokenMatches(idpUserToken, userSession)) {
                handleIdpUserTokenMismatch(requestContext);
            }
        }
    }

    private void handleIdpUserTokenMismatch(final ContainerRequestContext requestContext) throws IOException {
        final AppbaseCore appbaseCore = appbaseCoreFromContext();
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final IdpUserTokenCheckFilterConfiguration idpUserTokenCheckFilterConfiguration =
                configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class);

        if (idpUserTokenCheckFilterConfiguration == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find configuration: " + IdpUserTokenCheckFilterConfiguration.class.getName());
        }

        if (idpUserTokenCheckFilterConfiguration.isRespondForbidden()) {
            sendForbidden(
                    appbaseCore,
                    getClass().getName(),
                    requestContext);
        } else if (AppbaseUtil.isNotBlank(idpUserTokenCheckFilterConfiguration.getRedirectUrl())) {
            sendRedirect(
                    appbaseCore,
                    getClass().getName(),
                    requestContext,
                    idpUserTokenCheckFilterConfiguration.getRedirectUrl());
        } else if (idpUserTokenCheckFilterConfiguration.getHttpStatusCode() > 0) {
            sendNetHttpResponseStatus(
                    appbaseCore,
                    getClass().getName(),
                    requestContext,
                    idpUserTokenCheckFilterConfiguration.getHttpStatusCode());
        } else {
            // No action configured: a bound-session binding failure still fails CLOSED with a default
            // 403. The IDP-token binding is mandatory (A10-10); only an explicit action overrides this.
            sendForbidden(appbaseCore, getClass().getName(), requestContext);
        }
    }
}
