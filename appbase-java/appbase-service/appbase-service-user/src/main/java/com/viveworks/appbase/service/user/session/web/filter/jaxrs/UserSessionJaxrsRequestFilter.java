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
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.UserAccountServiceExceptionConfiguration;
import com.viveworks.appbase.service.user.account.data.RenewSessionRequest;
import com.viveworks.appbase.service.user.account.data.RenewSessionResponse;
import com.viveworks.appbase.service.user.account.web.endpoint.jaxrs.UserAccountJaxrsResource;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.web.filter.DefaultUserSessionFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredExceptionConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundExceptionConfiguration;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
@Priority(AppbaseServiceConstants.PRIORITY_HIGH)
public class UserSessionJaxrsRequestFilter extends AbstractJaxrsRequestFilter {
    private final UserSessionFilter userSessionFilter;

    /**
     * Test seam only (package-private): supplies {@code appbaseCoreFromContext()} directly, bypassing
     * the {@code Providers}-based JAX-RS context lookup that requires a running JAX-RS runtime. Always
     * {@code null} on every production (public) constructor path.
     */
    private final AppbaseCore testAppbaseCore;

    public UserSessionJaxrsRequestFilter() {
        this.userSessionFilter = new DefaultUserSessionFilter();
        this.testAppbaseCore = null;
    }

    public UserSessionJaxrsRequestFilter(final UserSessionFilter userSessionFilter) {
        this.userSessionFilter = userSessionFilter;
        this.testAppbaseCore = null;
    }

    public UserSessionJaxrsRequestFilter(
            final ResourceInfo resourceInfo,
            final UserSessionFilter userSessionFilter)
    {
        super(resourceInfo);
        this.userSessionFilter = userSessionFilter;
        this.testAppbaseCore = null;
    }

    /**
     * Test-seam constructor (package-private), mirroring {@code AuthorizationWebFilter}'s
     * package-private test-seam constructor: lets a test drive the real filter with a mock neutral
     * {@link UserSessionFilter} without also having to stand up the JAX-RS {@code Providers} machinery
     * that {@link #appbaseCoreFromContext()} normally goes through.
     *
     * @param appbaseCore        Appbase core (test seam)
     * @param userSessionFilter  the neutral session logic to delegate to (test seam)
     */
    UserSessionJaxrsRequestFilter(final AppbaseCore appbaseCore, final UserSessionFilter userSessionFilter) {
        this.testAppbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.userSessionFilter = AppbaseUtil.notNull(userSessionFilter, "userSessionFilter");
    }

    @Override
    protected AppbaseCore appbaseCoreFromContext() {
        return testAppbaseCore != null ? testAppbaseCore : super.appbaseCoreFromContext();
    }

    @Override
    protected void filterRequest(final ContainerRequestContext requestContext) throws IOException {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters(false);
        if (queryParameters != null &&
                (queryParameters.containsKey(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM) ||
                        queryParameters.containsKey(AppbaseServiceConstants.APPBASE_USER_ID_PARAM)))
        {
            // Identity in a URL leaks into proxy/access logs, browser history and referrer headers
            // (CWE-598). Fail closed instead of silently treating it as an anonymous request.
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        String appbaseSessionId = cookieValue(requestContext, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        String appbaseUserId = cookieValue(requestContext, AppbaseServiceConstants.APPBASE_USER_ID_PARAM);

        if (AppbaseUtil.isBlank(appbaseSessionId)) {
            appbaseSessionId = headerValue(requestContext, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        }
        if (AppbaseUtil.isBlank(appbaseUserId)) {
            appbaseUserId = headerValue(requestContext, AppbaseServiceConstants.APPBASE_USER_ID_PARAM);
        }

        final AppbaseCore appbaseCore = appbaseCoreFromContext();

        final String path;
        try {
            // getPath(false) is the ENCODED, base-URI-relative path. getPath() decodes -- see
            // jakarta.ws.rs-api: "All sequences of escaped octets are decoded, equivalent to
            // getPath(true)". Canonicalization must see the encoded form.
            path = AppbasePathCanonicalizer.canonicalize(uriInfo.getPath(false));
        } catch (final AppbasePathException e) {
            // A 400 is sent and the chain is NOT continued: no session decision is made against a
            // path that could not be reduced to its canonical form.
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        UserSession userSession = null;
        try {
            userSession = userSessionFilter.onRequest(path, appbaseCore, appbaseSessionId, appbaseUserId);
        } catch (Exception ex) {
            if (path.endsWith(UserAccountJaxrsResource.AFTER_IDP_SIGN_OUT_PATH)) {
                // no-op if it is a sign out request
                return;
            }

            if (UserSessionExpiredException.class.isAssignableFrom(ex.getClass())) {
                userSession = handleSessionExpired(
                        appbaseCore,
                        ((UserSessionExpiredException) ex).getExpiredUserSession(),
                        requestContext);
                if (userSession == null) {
                    return;
                }
                requestContext.setProperty(
                        AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM,
                        userSession.getAppbaseSessionId());
                requestContext.setProperty(
                        AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM,
                        userSession.getAppbaseSessionId());
            }

            if (userSession == null) {
                if (UserSessionNotFoundException.class.isAssignableFrom(ex.getClass())) {
                    if (handleSessionNotFound(appbaseCore, requestContext)) {
                        return;
                    }
                }

                if (UserAccountServiceException.class.isAssignableFrom(ex.getClass())) {
                    if (handleUserAccountServiceException(appbaseCore, ex.getMessage(), requestContext)) {
                        return;
                    }
                }

                if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                    if (handleAuthorizationServiceException(
                            appbaseCore, getClass().getName(), requestContext))
                    {
                        return;
                    }
                }

                throw new AppbaseServiceRuntimeException(ex);
            }
        }

        if (userSession == null) {
            if (handleSessionNotFound(appbaseCore, requestContext)) {
                return;
            }
        } else {
            requestContext.setProperty(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, userSession);
            httpServletRequest.setAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, userSession);
        }
    }

    private boolean handleUserAccountServiceException(
            final AppbaseCore appbaseCore,
            final String message,
            final ContainerRequestContext requestContext) throws IOException
    {
        counter(appbaseCore, AppbaseServiceConstants.HANDLE_USER_ACCOUNT_SERVICE_EXCEPTION).inc();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final UserAccountServiceExceptionConfiguration userAccountServiceExceptionConfiguration =
                configurationService.getTypedConfiguration(
                        UserAccountServiceExceptionConfiguration.class);
        if (userAccountServiceExceptionConfiguration != null) {
            if (userAccountServiceExceptionConfiguration.isRespondInternalServerError()) {
                sendInternalServerError(
                        appbaseCore,
                        getClass().getName(),
                        message,
                        requestContext);
                return true;
            } else if (AppbaseUtil.isNotBlank(userAccountServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userAccountServiceExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (userAccountServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userAccountServiceExceptionConfiguration.getHttpStatusCode());
                return true;
            }
        }

        // If there is no action configured, pass the request to filter chain
        return false;
    }

    private UserSession handleSessionExpired(
            final AppbaseCore appbaseCore,
            final UserSession expiredUserSession,
            final ContainerRequestContext requestContext) throws IOException
    {
        counter(appbaseCore, AppbaseServiceConstants.HANDLE_SESSION_EXPIRED).inc();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final UserSessionExpiredExceptionConfiguration userSessionExpiredExceptionConfiguration =
                configurationService.getTypedConfiguration(UserSessionExpiredExceptionConfiguration.class);
        if (userSessionExpiredExceptionConfiguration != null) {
            if (userSessionExpiredExceptionConfiguration.isRenewSession()) {
                final AppbaseServiceEntry appbaseServiceEntry = appbaseCore
                        .getServiceRegistry()
                        .getAppbaseService(UserAccountService.class);
                if (appbaseServiceEntry == null) {
                    throw new IOException("Cannot find service entry for " + UserAccountService.class.getName());
                }
                final UserAccountService userAccountService = (UserAccountService) appbaseServiceEntry.getService();
                if (userAccountService == null) {
                    throw new IOException("Cannot find an instance of " + UserAccountService.class.getName());
                }

                final RenewSessionRequest renewSessionRequest =
                        new RenewSessionRequest(traceId(requestContext), expiredUserSession);
                try {
                    final RenewSessionResponse renewSessionResponse =
                            userAccountService.renewSession(renewSessionRequest);
                    return renewSessionResponse.getRenewedUserSession();
                } catch (UserAccountServiceException e) {
                    throw new IOException("Error while renewing session", e);
                }
            } else if (AppbaseUtil.isNotBlank(userSessionExpiredExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userSessionExpiredExceptionConfiguration.getRedirectUrl());
            } else if (userSessionExpiredExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userSessionExpiredExceptionConfiguration.getHttpStatusCode());
            }
        }

        return null;
    }

    private boolean handleSessionNotFound(
            final AppbaseCore appbaseCore,
            final ContainerRequestContext requestContext) throws IOException
    {
        counter(appbaseCore, AppbaseServiceConstants.HANDLE_SESSION_NOT_FOUND).inc();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final UserSessionNotFoundExceptionConfiguration userSessionNotFoundExceptionConfiguration =
                configurationService.getTypedConfiguration(UserSessionNotFoundExceptionConfiguration.class);
        if (userSessionNotFoundExceptionConfiguration != null) {
            if (userSessionNotFoundExceptionConfiguration.isRespondBadRequest()) {
                sendBadRequest(
                        appbaseCore,
                        getClass().getName(),
                        AppbaseServiceConstants.SESSION_NOT_FOUND,
                        requestContext);
                return true;
            } else if (AppbaseUtil.isNotBlank(userSessionNotFoundExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userSessionNotFoundExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (userSessionNotFoundExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userSessionNotFoundExceptionConfiguration.getHttpStatusCode());
                return true;
            }
        }

        // If there is no action configured, pass the request to filter chain
        return false;
    }
}
