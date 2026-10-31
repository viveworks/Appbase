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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.web.authorization.filter.AuthorizationFilter;
import com.viveworks.appbase.service.web.authorization.filter.DefaultAuthorizationFilter;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * This needs the downstream {@code UserSessionServletFilter} from {@code appbase-service-user} to be
 * configured before it executes.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AuthorizationServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = 4372766938291605472L;

    private final AuthorizationFilter authorizationFilter;

    public AuthorizationServletFilter() {
        this.authorizationFilter = new DefaultAuthorizationFilter();
    }

    public AuthorizationServletFilter(final AuthorizationFilter authorizationFilter) {
        AppbaseUtil.notNull(authorizationFilter, "authorizationFilter");

        this.authorizationFilter = authorizationFilter;
    }

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore) throws IOException, ServletException
    {
        final String service;
        try {
            // Strip before decode: both getRequestURI() and getContextPath() are encoded; stripping
            // after decoding would let an encoded separator inside the prefix shift the boundary. This
            // is the fix for D1: an exact-string blocklist lookup keyed on the canonical "/admin" must
            // never see the raw, undecoded "/%61dmin" the servlet container hands back.
            service = AppbasePathCanonicalizer.canonicalize(
                    AppbasePathCanonicalizer.stripContextPath(request.getRequestURI(), request.getContextPath()));
        } catch (final AppbasePathException e) {
            // A 400 is sent and the chain is NOT continued -- an unparseable path is refused rather
            // than risking it being matched (or missed) by any authorization rule.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.flushBuffer();
            return;
        }
        final String operation = request.getMethod();

        String appbaseSessionId = cookieValue(request, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        String appbaseRequesterId = cookieValue(request, AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM);

        if (AppbaseUtil.isBlank(appbaseSessionId)) {
            appbaseSessionId = headerValue(request, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        }
        if (AppbaseUtil.isBlank(appbaseRequesterId)) {
            appbaseRequesterId = headerValue(request, AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM);
        }

        // Session and requester identity are read from cookies and headers only. A credential in the URL
        // query string (CWE-598) leaks into proxy/access logs, browser history and referrer headers, so
        // it is NOT honoured here.

        if (!authorizationFilter.isAuthorized(appbaseCore, service, operation, appbaseSessionId, appbaseRequesterId)) {
            if (!handleAuthorizationServiceException(appbaseCore, operation, response)) {
                // A denial without a configured response action must never reach the application.
                sendForbidden(appbaseCore, operation, response);
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
