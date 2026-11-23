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

package com.viveworks.appbase.service.user.session.web.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.UserAccountServiceException;
import com.viveworks.appbase.service.user.account.UserAccountServiceExceptionConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.web.filter.DefaultUserSessionFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilter;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
public class UserSessionJaxrsResponseFilter extends AbstractJaxrsResponseFilter {
    private final UserSessionFilter userSessionFilter;

    public UserSessionJaxrsResponseFilter() {
        this.userSessionFilter = new DefaultUserSessionFilter();
    }

    public UserSessionJaxrsResponseFilter(final UserSessionFilter userSessionFilter) {
        this.userSessionFilter = userSessionFilter;
    }

    public UserSessionJaxrsResponseFilter(
            final UserSessionFilter userSessionFilter,
            final ResourceInfo resourceInfo)
    {
        super(resourceInfo);
        this.userSessionFilter = userSessionFilter;
    }

    @Override
    protected void filterResponse(
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException
    {
        final Object property = requestContext.getProperty(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY);
        if (property != null) {
            final UserSession userSession = (UserSession) property;
            final AppbaseCore appbaseCore = appbaseCoreFromContext();
            try {
                final UserSession newOrUpdatedUserSession = userSessionFilter.onResponse(appbaseCore, userSession);
                final Object newAppbaseSessionIdProperty =
                        requestContext.getProperty(AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM);
                if (newAppbaseSessionIdProperty != null) {
                    responseContext
                            .getHeaders()
                            .add(AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM, newAppbaseSessionIdProperty);
                }
                else if (newOrUpdatedUserSession != null && !Objects.equals(
                        userSession.getAppbaseSessionId(), newOrUpdatedUserSession.getAppbaseSessionId()))
                {
                    if (!AppbaseUtil.isBlank(newOrUpdatedUserSession.getAppbaseSessionId())) {
                        responseContext.getHeaders().add(
                                AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM,
                                newOrUpdatedUserSession.getAppbaseSessionId());
                    }
                }
            } catch (UserAccountServiceException e) {
                handleUserAccountServiceException(appbaseCore, requestContext, responseContext);
            } catch (AuthorizationServiceException e) {
                handleAuthorizationServiceException(
                        appbaseCore, getClass().getName(), requestContext, responseContext);
            } catch (AuthenticationServiceException e) {
                handleAuthenticationServiceException(
                        appbaseCore, getClass().getName(), requestContext, responseContext);
            }
        }
    }

    private void handleUserAccountServiceException(
            final AppbaseCore appbaseCore,
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException
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
                        responseContext);
            } else if (AppbaseUtil.isNotBlank(userAccountServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        getClass().getName(),
                        requestContext,
                        userAccountServiceExceptionConfiguration.getRedirectUrl());
            } else if (userAccountServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        getClass().getName(),
                        responseContext,
                        userAccountServiceExceptionConfiguration.getHttpStatusCode());
            }
        }
    }
}
