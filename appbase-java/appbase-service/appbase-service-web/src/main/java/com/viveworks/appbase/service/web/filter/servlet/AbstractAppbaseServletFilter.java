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

package com.viveworks.appbase.service.web.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractAppbaseServletFilter extends HttpFilter implements Filter {
    protected abstract void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore) throws IOException, ServletException;

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain) throws IOException, ServletException
    {
        final AppbaseCore appbaseCore = AppbaseServiceUtil.appbaseCore(request.getServletContext());
        final String path;
        try {
            // Strip before decode: getRequestURI() and getContextPath() are both encoded, and
            // stripping after decoding would let an encoded separator inside the prefix shift the
            // boundary. See AppbasePathCanonicalizer#stripContextPath.
            path = AppbasePathCanonicalizer.canonicalize(
                    AppbasePathCanonicalizer.stripContextPath(request.getRequestURI(), request.getContextPath()));
        } catch (final AppbasePathException e) {
            // A 400 is sent and the chain is NOT continued: no downstream filter (including
            // isFilterEnabled below, and every doFilter(..., appbaseCore) override) ever sees a path
            // that could not be reduced to its canonical form.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.flushBuffer();
            return;
        }
        final String method = request.getMethod();

        if (AppbaseServiceUtil.isFilterEnabled(
                appbaseCore.getConfigurationService(), getClass(), path, method.toUpperCase()))
        {
            doFilter(request, response, chain, appbaseCore);
            return;
        }

        chain.doFilter(request, response);
    }

    protected Cookie cookie(
            final HttpServletRequest httpServletRequest,
            final String cookieName)
    {
        final Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie;
                }
            }
        }

        return null;
    }

    protected String cookieValue(
            final HttpServletRequest httpServletRequest,
            final String cookieName)
    {
        final Cookie cookie = cookie(httpServletRequest, cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }

    protected String headerValue(
            final HttpServletRequest httpServletRequest,
            final String headerName)
    {
        return httpServletRequest.getHeader(headerName);
    }

    protected String queryParameter(
            final HttpServletRequest httpServletRequest,
            final String queryParameterName)
    {
        return httpServletRequest.getParameter(queryParameterName);
    }

    protected String traceId(final HttpServletRequest request, final HttpServletResponse response) {
        final AppbaseCore appbaseCore = AppbaseServiceUtil.appbaseCore(request.getServletContext());
        final String traceIdKey = AppbaseServiceUtil.traceIdKey(appbaseCore);

        String traceId = response.getHeader(traceIdKey);
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = MDC.get(traceIdKey);
        }

        return traceId;
    }

    protected void sendUnexpectedException(
            final Counter unexpectedExceptionCounter,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                unexpectedExceptionCounter,
                httpServletResponse,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                AppbaseServiceConstants.UNEXPECTED_EXCEPTION);
    }

    protected void sendForbidden(
            final AppbaseCore appbaseCore,
            final String operation,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                counter(
                        appbaseCore,
                        operation + MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_FORBIDDEN),
                httpServletResponse,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden");
    }

    protected void sendInternalServerError(
            final AppbaseCore appbaseCore,
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                counter(appbaseCore, operation +
                        MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
                httpServletResponse,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                message);
    }

    protected void sendBadRequest(
            final AppbaseCore appbaseCore,
            final String operation,
            final String message,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        sendError(
                counter(appbaseCore, operation +
                        MetricService.METRIC_NAME_SEPARATOR + HttpServletResponse.SC_BAD_REQUEST),
                httpServletResponse,
                HttpServletResponse.SC_BAD_REQUEST,
                message);
    }

    protected void sendError(
            final Counter counter,
            final HttpServletResponse httpServletResponse,
            final int statusCode,
            final String message)
            throws IOException
    {
        counter.inc();
        httpServletResponse.sendError(statusCode, message);
        httpServletResponse.flushBuffer();
    }

    protected void sendRedirect(
            final AppbaseCore appbaseCore,
            final String operation,
            final HttpServletResponse httpServletResponse,
            final String redirectUrl) throws IOException
    {
        counter(appbaseCore,
                operation + MetricService.METRIC_NAME_SEPARATOR + AppbaseServiceConstants.SEND_REDIRECT)
                .inc();
        httpServletResponse.sendRedirect(redirectUrl);
        httpServletResponse.flushBuffer();
    }

    protected void sendNetHttpResponseStatus(
            final AppbaseCore appbaseCore,
            final String operation,
            final HttpServletResponse httpServletResponse,
            final int httpStatusCode) throws IOException
    {
        counter(appbaseCore,
                operation + MetricService.METRIC_NAME_SEPARATOR + AppbaseServiceConstants.SEND_NET_HTTP_RESPONSE_STATUS)
                .inc();
        httpServletResponse.setStatus(httpStatusCode);
    }

    protected boolean handleAuthorizationServiceException(
            final AppbaseCore appbaseCore,
            final String operation,
            final HttpServletResponse httpServletResponse) throws IOException
    {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final AuthorizationServiceExceptionConfiguration authorizationServiceExceptionConfiguration =
                configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        if (authorizationServiceExceptionConfiguration != null) {
            if (authorizationServiceExceptionConfiguration.isRespondForbidden()) {
                sendForbidden(
                        appbaseCore,
                        operation,
                        httpServletResponse);
                return true;
            } else if (AppbaseUtil.isNotBlank(authorizationServiceExceptionConfiguration.getRedirectUrl())) {
                sendRedirect(
                        appbaseCore,
                        operation,
                        httpServletResponse,
                        authorizationServiceExceptionConfiguration.getRedirectUrl());
                return true;
            } else if (authorizationServiceExceptionConfiguration.getHttpStatusCode() > 0) {
                sendNetHttpResponseStatus(
                        appbaseCore,
                        operation,
                        httpServletResponse,
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
