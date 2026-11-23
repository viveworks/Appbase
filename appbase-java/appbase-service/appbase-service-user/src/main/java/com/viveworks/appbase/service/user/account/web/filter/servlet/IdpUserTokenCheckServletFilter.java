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

package com.viveworks.appbase.service.user.account.web.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.user.account.web.filter.DefaultIdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * This needs the {@link com.viveworks.appbase.service.user.session.web.filter.servlet.UserSessionServletFilter} to
 * be configured before it executes.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class IdpUserTokenCheckServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = -3704936418095017688L;

    private final IdpUserTokenCheckFilter idpUserTokenCheckFilter;

    public IdpUserTokenCheckServletFilter() {
        this.idpUserTokenCheckFilter = new DefaultIdpUserTokenCheckFilter();
    }

    public IdpUserTokenCheckServletFilter(final IdpUserTokenCheckFilter idpUserTokenCheckFilter) {
        AppbaseUtil.notNull(idpUserTokenCheckFilter, "idpUserTokenCheckFilter");

        this.idpUserTokenCheckFilter = idpUserTokenCheckFilter;
    }

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore) throws IOException, ServletException
    {
        final Object attribute = request.getAttribute(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY);
        if (attribute == null) {
            // Anonymous request: UserSessionServletFilter legitimately passed through without setting a
            // session attribute (its documented no-session contract). No session means there is no
            // idpUserToken bound to a session to check, so continue the chain and let the authorization
            // filter decide -- do NOT treat the absence as a wiring error. This keeps secure-by-default
            // reachable out of the box (anonymous requests reach authorization, not a blanket 400 here).
            chain.doFilter(request, response);
            return;
        }
        final UserSession userSession = (UserSession) attribute;

        // Read the token from cookies and headers only. A query-string credential (CWE-598) leaks into
        // proxy/access logs, browser history and referrer headers, so it is NOT honoured here.
        String idpUserToken = cookieValue(request, AppbaseServiceConstants.IDP_USER_TOKEN_PARAM);
        if (AppbaseUtil.isBlank(idpUserToken)) {
            idpUserToken = headerValue(request, AppbaseServiceConstants.IDP_USER_TOKEN_PARAM);
        }

        // Gate on the SESSION's bound token, not on the request-supplied one. A session that carries a
        // bound idpUserToken is an authenticated user-account session (UserSession.Validator makes such
        // a session invalid without one); its binding must be enforced. The caller must present a
        // matching token to prove it holds the binding -- an ABSENT request token is as much a failure
        // as a MISMATCHED one, so both are handled identically. A session with no bound token (a guest
        // session, or an anonymous request handled above) has nothing to bind and continues unchecked.
        final String boundIdpUserToken = userSession.getIdpUserToken();
        if (!AppbaseUtil.isBlank(boundIdpUserToken)) {
            if (AppbaseUtil.isBlank(idpUserToken)
                    || !idpUserTokenCheckFilter.idpUserTokenMatches(idpUserToken, userSession)) {
                if (handleIdpUserTokenMismatch(appbaseCore, response)) {
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean handleIdpUserTokenMismatch(
            final AppbaseCore appbaseCore,
            final HttpServletResponse response)
            throws IOException
    {
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
                    response);
            return true;
        } else if (AppbaseUtil.isNotBlank(idpUserTokenCheckFilterConfiguration.getRedirectUrl())) {
            sendRedirect(
                    appbaseCore,
                    getClass().getName(),
                    response,
                    idpUserTokenCheckFilterConfiguration.getRedirectUrl());
            return true;
        } else if (idpUserTokenCheckFilterConfiguration.getHttpStatusCode() > 0) {
            sendNetHttpResponseStatus(
                    appbaseCore,
                    getClass().getName(),
                    response,
                    idpUserTokenCheckFilterConfiguration.getHttpStatusCode());
            return true;
        }

        // No action configured: a bound-session binding failure still fails CLOSED with a default 403.
        // The IDP-token binding is mandatory (A10-10) -- an absent or mismatched token on an
        // authenticated session is never forwarded; only an explicit forbidden/redirect/status action
        // overrides this default.
        sendForbidden(appbaseCore, getClass().getName(), response);
        return true;
    }
}
