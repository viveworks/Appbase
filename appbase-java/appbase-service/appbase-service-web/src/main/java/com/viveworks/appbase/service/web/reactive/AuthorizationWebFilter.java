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

package com.viveworks.appbase.service.web.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import com.viveworks.appbase.service.web.authorization.filter.AuthorizationFilter;
import com.viveworks.appbase.service.web.authorization.filter.DefaultAuthorizationFilter;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Thin adapter that reproduces {@code AuthorizationServletFilter} on the transport-neutral exchange.
 * It delegates the authorization decision to the SAME {@link AuthorizationFilter} neutral logic
 * ({@code DefaultAuthorizationFilter}); it does not reimplement any authorization rule. It runs LAST
 * in the chain ({@link AppbaseServiceFilterOrder#AUTHORIZATION}), after the user session is resolved
 * and the IDP token is checked -- authorizing before the session is resolved would authorize an
 * unauthenticated principal, so this order is a security invariant.
 *
 * <p>When {@code isAuthorized(...)} returns {@code false}, the filter consults the {@link
 * AuthorizationServiceExceptionConfiguration}. An absent or actionless response policy produces a
 * forbidden response; only an authorized request reaches the downstream chain.
 *
 * <p>The servlet base signals errors with {@code HttpServletResponse.sendError(status, message)} and
 * redirects with {@code sendRedirect(url)}, neither of which the neutral exchange exposes. They are
 * emulated: {@link AppbaseServiceExchange#status(int)} plus {@link
 * AppbaseServiceExchange#writeJson(Object)} for the forbidden body, and {@code status(302)} plus a
 * {@code Location} response header for the redirect. The servlet's per-outcome metric counters are
 * servlet-internal bookkeeping (keyed on the servlet class) and are not reproduced here.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AuthorizationWebFilter implements AppbaseServiceFilter {

    private static final String LOCATION_HEADER = "Location";

    private static final int STATUS_FORBIDDEN = 403;

    private static final int STATUS_FOUND = 302;

    private final AppbaseCore appbaseCore;

    private final AuthorizationFilter authorizationFilter;

    /**
     * Creates an authorization filter backed by the default authorization logic.
     *
     * @param appbaseCore Appbase core, providing the configuration service
     */
    public AuthorizationWebFilter(final AppbaseCore appbaseCore) {
        this(appbaseCore, new DefaultAuthorizationFilter());
    }

    /**
     * Creates an authorization filter with an injectable authorization-logic seam.
     *
     * @param appbaseCore         Appbase core, providing the configuration service
     * @param authorizationFilter the neutral authorization logic to delegate to (test seam)
     */
    AuthorizationWebFilter(final AppbaseCore appbaseCore, final AuthorizationFilter authorizationFilter) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.authorizationFilter = AppbaseUtil.notNull(authorizationFilter, "authorizationFilter");
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return Mono.defer(() -> {
            final String service = exchange.path();
            final String operation = exchange.method();

            final String appbaseSessionId = resolve(exchange, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
            final String appbaseRequesterId = resolve(exchange, AppbaseServiceConstants.APPBASE_REQUESTER_ID_PARAM);

            if (!authorizationFilter.isAuthorized(appbaseCore, service, operation, appbaseSessionId, appbaseRequesterId)) {
                return handleAuthorizationServiceException(exchange);
            }

            return chain.next(exchange);
        });
    }

    /**
     * Resolves a request value from cookie, then header -- the first non-blank source wins -- exactly as
     * {@code AuthorizationServletFilter} does with {@code AppbaseUtil.isBlank(...)} between steps. The
     * query string is NOT consulted: a credential in the URL (CWE-598) leaks into proxy/access logs,
     * browser history and referrer headers, so it is not honoured.
     *
     * @param exchange the transport-neutral service exchange
     * @param name     the cookie/header name
     * @return the resolved value, or {@code null} when no source supplies one
     */
    private String resolve(final AppbaseServiceExchange exchange, final String name) {
        String value = exchange.cookie(name);
        if (AppbaseUtil.isBlank(value)) {
            value = exchange.header(name);
        }
        // The query string is NOT consulted: a credential in the URL (CWE-598) leaks into proxy/access
        // logs, browser history and referrer headers, so it is not honoured.
        return value;
    }

    /**
     * Mirrors the configured response actions handled by the servlet authorization filter. An absent
     * or actionless {@link AuthorizationServiceExceptionConfiguration} defaults to forbidden here;
     * the servlet authorization filter applies the same fallback in its caller.
     *
     * @param exchange the transport-neutral service exchange
     * @return a completion signal to short-circuit the chain
     */
    private Mono<Void> handleAuthorizationServiceException(final AppbaseServiceExchange exchange) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final AuthorizationServiceExceptionConfiguration configuration =
                configurationService.getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        if (configuration != null) {
            if (configuration.isRespondForbidden()) {
                exchange.status(STATUS_FORBIDDEN);
                return exchange.writeJson(Map.of(
                        "error", "Forbidden",
                        "status", STATUS_FORBIDDEN));
            } else if (AppbaseUtil.isNotBlank(configuration.getRedirectUrl())) {
                exchange.status(STATUS_FOUND);
                exchange.responseHeader(LOCATION_HEADER, configuration.getRedirectUrl());
                return Mono.empty();
            } else if (configuration.getHttpStatusCode() > 0) {
                exchange.status(configuration.getHttpStatusCode());
                return Mono.empty();
            }
        }

        exchange.status(STATUS_FORBIDDEN);
        return exchange.writeJson(Map.of(
                "error", "Forbidden",
                "status", STATUS_FORBIDDEN));
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.AUTHORIZATION;
    }
}
