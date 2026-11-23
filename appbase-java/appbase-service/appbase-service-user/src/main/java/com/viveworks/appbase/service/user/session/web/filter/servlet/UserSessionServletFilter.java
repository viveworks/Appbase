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

package com.viveworks.appbase.service.user.session.web.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.UserAccountServiceExceptionConfiguration;
import com.viveworks.appbase.service.user.account.data.RenewSessionRequest;
import com.viveworks.appbase.service.user.account.data.RenewSessionResponse;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpSignOutServlet;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.web.filter.DefaultUserSessionFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredExceptionConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundException;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundExceptionConfiguration;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import com.viveworks.appbase.service.web.filter.servlet.AppbaseHttpServletRequestWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserSessionServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = -970024780238198664L;

    private final UserSessionFilter userSessionFilter;

    public UserSessionServletFilter() {
        this.userSessionFilter = new DefaultUserSessionFilter();
    }

    public UserSessionServletFilter(final UserSessionFilter userSessionFilter) {
        this.userSessionFilter = userSessionFilter;
    }

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore) throws IOException, ServletException
    {
        if (request.getQueryString() != null &&
                (request.getParameterMap().containsKey(AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM) ||
                        request.getParameterMap().containsKey(AppbaseServiceConstants.APPBASE_USER_ID_PARAM)))
        {
            // Reject identity in the URL at the servlet boundary, before Jersey or the neutral
            // session logic can turn the request into a new anonymous session (CWE-598).
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            response.flushBuffer();
            return;
        }

        String appbaseSessionId = cookieValue(request, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        String appbaseUserId = cookieValue(request, AppbaseServiceConstants.APPBASE_USER_ID_PARAM);

        if (AppbaseUtil.isBlank(appbaseSessionId)) {
            appbaseSessionId = headerValue(request, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        }
        if (AppbaseUtil.isBlank(appbaseUserId)) {
            appbaseUserId = headerValue(request, AppbaseServiceConstants.APPBASE_USER_ID_PARAM);
        }

        final String path;
        try {
            // Strip before decode: both getRequestURI() and getContextPath() are encoded; stripping
            // after decoding would let an encoded separator inside the prefix shift the boundary.
            path = AppbasePathCanonicalizer.canonicalize(
                    AppbasePathCanonicalizer.stripContextPath(request.getRequestURI(), request.getContextPath()));
        } catch (final AppbasePathException e) {
            // A 400 is sent and the chain is NOT continued.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.flushBuffer();
            return;
        }

        UserSession userSession = null;
        try {
            userSession = userSessionFilter
                    .onRequest(path, appbaseCore, appbaseSessionId, appbaseUserId);
        } catch (Exception ex) {
            if (path.endsWith(AfterIdpSignOutServlet.DEFAULT_PATH)) {
                // no-op if it is a sign out request
                return;
            }

            if (UserSessionExpiredException.class.isAssignableFrom(ex.getClass())) {
                userSession = handleSessionExpired(
                        appbaseCore,
                        ((UserSessionExpiredException) ex).getExpiredUserSession(),
                        request,
                        response);
                if (userSession == null) {
                    throw new AppbaseServiceRuntimeException("No session found after handling expiration");
                }
                response.setHeader(
                        AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM,
                        userSession.getAppbaseSessionId());
            }

            if (userSession == null) {
                if (UserSessionNotFoundException.class.isAssignableFrom(ex.getClass())) {
                    if (handleSessionNotFound(appbaseCore, response)) {
                        return;
                    }
                }

                if (UserAccountServiceException.class.isAssignableFrom(ex.getClass())) {
                    if (handleUserAccountServiceException(appbaseCore, ex.getMessage(), response)) {
                        return;
                    }
                }

                if (AuthorizationServiceException.class.isAssignableFrom(ex.getClass())) {
                    if (handleAuthorizationServiceException(
                            appbaseCore, getClass().getName(), response))
                    {
                        return;
                    }
                }

                throw new AppbaseServiceRuntimeException(ex);
            }
        }

        if (userSession == null) {
            if (handleSessionNotFound(appbaseCore, response)) {
                return;
            }
        } else {
            if (AppbaseHttpServletRequestWrapper.class.isAssignableFrom(request.getClass())) {
                ((AppbaseHttpServletRequestWrapper) request).setUserSession(userSession);
            }
            request.setAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, userSession);
        }

        chain.doFilter(request, response);

        if (userSession != null) {
            try {
                userSessionFilter.onResponse(appbaseCore, userSession);
            } catch (AuthorizationServiceException | UserAccountServiceException | AuthenticationServiceException e) {
                throw new AppbaseServiceRuntimeException("Error writing session to the store", e);
            }
        }
    }

    private boolean handleUserAccountServiceException(
            final AppbaseCore appbaseCore,
            final String message,
            final HttpServletResponse response) throws IOException
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
                        response);
                return true;
            } else if (AppbaseUtil.isNotBlank(userAccountServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        response,
                        userAccountServiceExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (userAccountServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        response,
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
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException
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
                    throw new ServletException("Cannot find service entry for " + UserAccountService.class.getName());
                }
                final UserAccountService userAccountService = (UserAccountService) appbaseServiceEntry.getService();
                if (userAccountService == null) {
                    throw new ServletException("Cannot find an instance of " + UserAccountService.class.getName());
                }

                final RenewSessionRequest renewSessionRequest =
                        new RenewSessionRequest(traceId(request, response), expiredUserSession);
                try {
                    final RenewSessionResponse renewSessionResponse =
                            userAccountService.renewSession(renewSessionRequest);
                    return renewSessionResponse.getRenewedUserSession();
                } catch (UserAccountServiceException e) {
                    throw new ServletException("Error while renewing session", e);
                }
            } else if (AppbaseUtil.isNotBlank(userSessionExpiredExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        response,
                        userSessionExpiredExceptionConfiguration.getRedirectUrl());
            } else if (userSessionExpiredExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        response,
                        userSessionExpiredExceptionConfiguration.getHttpStatusCode());
            }
        }

        return null;
    }

    private boolean handleSessionNotFound(
            final AppbaseCore appbaseCore,
            final HttpServletResponse response) throws IOException
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
                        response);
                return true;
            } else if (AppbaseUtil.isNotBlank(userSessionNotFoundExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        response,
                        userSessionNotFoundExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (userSessionNotFoundExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        response,
                        userSessionNotFoundExceptionConfiguration.getHttpStatusCode());
                return true;
            }
        }

        // If there is no action configured, pass the request to filter chain
        return false;
    }
}
