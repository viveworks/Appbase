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

package com.viveworks.appbase.service.web.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthenticationServiceExceptionConfiguration;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.metrics.Counter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractAppbaseJaxrsFilter extends AbstractAppbaseJaxrsEntity {
    @Context
    protected ResourceInfo resourceInfo;

    public AbstractAppbaseJaxrsFilter() {
    }

    public AbstractAppbaseJaxrsFilter(final ResourceInfo resourceInfo) {
        AppbaseUtil.notNull(resourceInfo, "resourceInfo");

        this.resourceInfo = resourceInfo;
    }

    protected boolean isFiltered(final ContainerRequestContext containerRequestContext) {
        final AppbaseCore appbaseCore = appbaseCoreFromContext();
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();

        String path = null;
        String method = null;
        if (resourceInfo != null) {
            final Class<?> resourceClass = resourceInfo.getResourceClass();
            if (resourceClass != null) {
                path = resourceClass.getName();
            }

            final Method resourceMethod = resourceInfo.getResourceMethod();
            if (resourceMethod != null) {
                method = resourceMethod.getName();
            }
        }

        if (path == null) {
            // getPath(false) is the ENCODED, base-URI-relative path. getPath() decodes -- see
            // jakarta.ws.rs-api: "All sequences of escaped octets are decoded, equivalent to
            // getPath(true)". Canonicalization must see the encoded form.
            try {
                path = AppbasePathCanonicalizer.canonicalize(containerRequestContext.getUriInfo().getPath(false));
            } catch (final AppbasePathException e) {
                // Fail closed with a clean 400, not a JAX-RS default 500. Returning false here would
                // SKIP the filter -- including authorization -- so the request must be stopped, not
                // passed through.
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            }
        }

        if (method == null) {
            method = containerRequestContext.getMethod().toUpperCase();
        }

        return AppbaseServiceUtil.isFilterEnabled(configurationService, getClass(), path, method);
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(final ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    protected Cookie cookie(final ContainerRequestContext requestContext, final String cookieName) {
        final Map<String, Cookie> cookies = requestContext.getCookies();

        if (cookies != null) {
            return cookies.get(cookieName);
        }

        return null;
    }

    protected String cookieValue(final ContainerRequestContext requestContext, final String cookieName) {
        final Cookie cookie = cookie(requestContext, cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }

    protected String headerValue(final ContainerRequestContext requestContext, final String headerName) {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        if (headers != null) {
            return headers.getFirst(headerName);
        }

        return null;
    }

    protected String queryParameter(final ContainerRequestContext requestContext, final String queryParameterName) {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters(true);

        if (queryParameters != null) {
            return queryParameters.getFirst(queryParameterName);
        }

        return null;
    }

    protected void sendUnexpectedException(
            final Counter unexpectedExceptionCounter,
            final ContainerRequestContext requestContext)
            throws IOException
    {
        sendError(
                unexpectedExceptionCounter,
                requestContext,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AppbaseServiceConstants.UNEXPECTED_EXCEPTION);
    }

    protected void sendUnexpectedException(
            final Counter unexpectedExceptionCounter,
            final ContainerResponseContext responseContext)
            throws IOException
    {
        sendError(
                unexpectedExceptionCounter,
                responseContext,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    protected void sendForbidden(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerRequestContext requestContext)
            throws IOException
    {
        sendError(
                counter(
                        appbaseCore,
                        operation + MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_FORBIDDEN),
                requestContext,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden");
    }

    protected void sendForbidden(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerResponseContext responseContext)
            throws IOException
    {
        sendError(
                counter(
                        appbaseCore,
                        operation + MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_FORBIDDEN),
                responseContext,
                HttpServletResponse.SC_FORBIDDEN);
    }

    protected void sendInternalServerError(
            final AppbaseCore appbaseCore,
            final String operation,
            final String message,
            final ContainerRequestContext requestContext)
            throws IOException
    {
        sendError(
                counter(appbaseCore, operation +
                        MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
                requestContext,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                message);
    }

    protected void sendInternalServerError(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerResponseContext responseContext)
            throws IOException
    {
        sendError(
                counter(appbaseCore, operation +
                        MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
                responseContext,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    protected void sendBadRequest(
            final AppbaseCore appbaseCore,
            final String operation,
            final String message,
            final ContainerRequestContext requestContext)
            throws IOException
    {
        sendError(
                counter(appbaseCore, operation +
                        MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_BAD_REQUEST),
                requestContext,
                HttpServletResponse.SC_BAD_REQUEST,
                message);
    }

    protected void sendBadRequest(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerResponseContext responseContext)
            throws IOException
    {
        sendError(
                counter(appbaseCore, operation +
                        MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_BAD_REQUEST),
                responseContext,
                HttpServletResponse.SC_BAD_REQUEST);
    }

    protected void sendError(
            final Counter counter,
            final ContainerRequestContext requestContext,
            final int statusCode,
            final String message)
            throws IOException
    {
        counter.inc();
        if (!AppbaseUtil.isBlank(message)) {
            requestContext.abortWith(Response.status(statusCode, message).build());
        } else {
            requestContext.abortWith(Response.status(statusCode).build());
        }
    }

    protected void sendError(
            final Counter counter,
            final ContainerResponseContext responseContext,
            final int statusCode)
            throws IOException
    {
        counter.inc();
        responseContext.setStatus(statusCode);
    }

    protected void sendRedirect(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerRequestContext containerRequestContext,
            final String redirectUrl) throws IOException
    {
        counter(appbaseCore,
                operation + MetricService.METRIC_NAME_SEPARATOR + AppbaseServiceConstants.SEND_REDIRECT)
                .inc();
        containerRequestContext.abortWith(Response.temporaryRedirect(URI.create(redirectUrl)).build());
    }

    protected void sendNetHttpResponseStatus(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerRequestContext containerRequestContext,
            final int httpStatusCode) throws IOException
    {
        counter(appbaseCore,
                operation + MetricService.METRIC_NAME_SEPARATOR + AppbaseServiceConstants.SEND_NET_HTTP_RESPONSE_STATUS)
                .inc();
        containerRequestContext.abortWith(Response.status(httpStatusCode).build());
    }

    protected void sendNetHttpResponseStatus(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerResponseContext containerResponseContext,
            final int httpStatusCode) throws IOException
    {
        counter(appbaseCore,
                operation + MetricService.METRIC_NAME_SEPARATOR + AppbaseServiceConstants.SEND_NET_HTTP_RESPONSE_STATUS)
                .inc();
        containerResponseContext.setStatus(httpStatusCode);
    }

    protected boolean handleAuthenticationServiceException(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException
    {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final AuthenticationServiceExceptionConfiguration authenticationServiceExceptionConfiguration =
                configurationService.getTypedConfiguration(AuthenticationServiceExceptionConfiguration.class);
        if (authenticationServiceExceptionConfiguration != null) {
            if (authenticationServiceExceptionConfiguration.isRespondForbidden()) {
                sendForbidden(
                        appbaseCore,
                        operation,
                        responseContext);
                return true;
            } else if (AppbaseUtil.isNotBlank(authenticationServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        operation,
                        requestContext,
                        authenticationServiceExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (authenticationServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        operation,
                        responseContext,
                        authenticationServiceExceptionConfiguration.getHttpStatusCode());
                return true;
            }
        }

        // If there is no action configured, pass the request to filter chain
        return false;
    }

    protected boolean handleAuthorizationServiceException(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException
    {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final AuthorizationServiceExceptionConfiguration authorizationServiceExceptionConfiguration =
                configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        if (authorizationServiceExceptionConfiguration != null) {
            if (authorizationServiceExceptionConfiguration.isRespondForbidden()) {
                sendForbidden(
                        appbaseCore,
                        operation,
                        responseContext);
                return true;
            } else if (AppbaseUtil.isNotBlank(authorizationServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        operation,
                        requestContext,
                        authorizationServiceExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (authorizationServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        operation,
                        responseContext,
                        authorizationServiceExceptionConfiguration.getHttpStatusCode());
                return true;
            }
        }

        // If there is no action configured, pass the request to filter chain
        return false;
    }

    protected boolean handleAuthorizationServiceException(
            final AppbaseCore appbaseCore,
            final String operation,
            final ContainerRequestContext requestContext) throws IOException
    {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final AuthorizationServiceExceptionConfiguration authorizationServiceExceptionConfiguration =
                configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        if (authorizationServiceExceptionConfiguration != null) {
            if (authorizationServiceExceptionConfiguration.isRespondForbidden()) {
                sendForbidden(
                        appbaseCore,
                        operation,
                        requestContext);
                return true;
            } else if (AppbaseUtil.isNotBlank(authorizationServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        operation,
                        requestContext,
                        authorizationServiceExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (authorizationServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        operation,
                        requestContext,
                        authorizationServiceExceptionConfiguration.getHttpStatusCode());
                return true;
            }
        }

        // If there is no action configured, pass the request to filter chain
        return false;
    }

    protected Counter counter(final AppbaseCore appbaseCore, final String name) {
        final MetricService metricService = appbaseCore.getMetricService();
        final String metricName = metricService.name(
                getClass(),
                name + MetricService.METRIC_NAME_SEPARATOR + AppbaseServiceConstants.COUNTER);
        return metricService
                .getMetricRegistry()
                .counter(metricName);
    }
}
