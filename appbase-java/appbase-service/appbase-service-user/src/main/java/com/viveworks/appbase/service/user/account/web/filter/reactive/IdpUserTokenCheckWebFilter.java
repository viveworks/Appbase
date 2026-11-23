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

package com.viveworks.appbase.service.user.account.web.filter.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import com.viveworks.appbase.service.user.account.web.filter.DefaultIdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.session.data.UserSession;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Thin adapter that reproduces {@code IdpUserTokenCheckServletFilter} on the transport-neutral
 * exchange, delegating the token comparison to the SAME {@link IdpUserTokenCheckFilter} neutral logic
 * ({@code DefaultIdpUserTokenCheckFilter}).
 *
 * <p><strong>Ordering dependency, by design.</strong> This filter reads the {@link UserSession} that
 * {@code UserSessionWebFilter} stored in {@link AppbaseServiceExchange#attributes()} under {@link
 * AppbaseServiceConstants#USER_SESSION_PROPERTY_KEY}. That is exactly why the order constants exist:
 * {@link AppbaseServiceFilterOrder#USER_SESSION} (500) runs before {@link
 * AppbaseServiceFilterOrder#IDP_TOKEN_CHECK} (600). A missing attribute is NOT a wiring error:
 * {@code UserSessionWebFilter} legitimately passes an anonymous request through without a session, so
 * with no session there is no token to check and this filter -- like the servlet twin -- continues
 * the chain to authorization rather than throwing. This keeps secure-by-default reachable out of the
 * box (anonymous requests reach the authorization filter instead of being rejected 400 here).
 *
 * <p>When no {@code idpUserToken} is present the chain continues unchecked (the servlet does the
 * same). When a token is present and does NOT match, the mismatch handler consults the {@link
 * IdpUserTokenCheckFilterConfiguration}. Unlike the other filters' handlers, this one THROWS when the
 * configuration is absent (matching {@code handleIdpUserTokenMismatch}); a configuration present with
 * no forbidden/redirect/status action lets the request through. The servlet's {@code
 * sendError}/{@code sendRedirect} are emulated with {@code status(code)} + {@code writeJson(...)} and
 * {@code status(302)} + a {@code Location} header respectively.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class IdpUserTokenCheckWebFilter implements AppbaseServiceFilter {

    private static final String LOCATION_HEADER = "Location";

    private static final int STATUS_FORBIDDEN = 403;

    private static final int STATUS_FOUND = 302;

    private final AppbaseCore appbaseCore;

    private final IdpUserTokenCheckFilter idpUserTokenCheckFilter;

    /**
     * Creates the filter with the default neutral token-check logic.
     *
     * @param appbaseCore Appbase core, providing the configuration service
     */
    public IdpUserTokenCheckWebFilter(final AppbaseCore appbaseCore) {
        this(appbaseCore, new DefaultIdpUserTokenCheckFilter());
    }

    /**
     * Creates the filter with an explicit neutral token-check delegate.
     *
     * @param appbaseCore             Appbase core, providing the configuration service
     * @param idpUserTokenCheckFilter the neutral token-check logic to delegate to (test seam)
     */
    IdpUserTokenCheckWebFilter(final AppbaseCore appbaseCore, final IdpUserTokenCheckFilter idpUserTokenCheckFilter) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.idpUserTokenCheckFilter = AppbaseUtil.notNull(idpUserTokenCheckFilter, "idpUserTokenCheckFilter");
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return Mono.defer(() -> {
            final Object attribute = exchange.attributes().get(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY);
            if (attribute == null) {
                // Anonymous request: UserSessionWebFilter legitimately passed through without storing a
                // session (its documented no-session contract). No session means there is no
                // idpUserToken bound to a session to check, so continue the chain and let the
                // authorization filter decide -- do NOT treat the absence as a wiring error. This is
                // what makes secure-by-default reachable out of the box (anonymous /health -> 200,
                // anonymous /configuration -> 403 at the authorizer, instead of a blanket 400 here).
                return chain.next(exchange);
            }
            final UserSession userSession = (UserSession) attribute;

            // Gate on the SESSION's bound token, not the request-supplied one (see the servlet twin).
            // A session that carries a bound idpUserToken is an authenticated user-account session whose
            // binding must be enforced: the caller must present a matching token, so an ABSENT request
            // token fails the binding exactly as a MISMATCHED one does. A session with no bound token (a
            // guest session, or the anonymous request handled above) has nothing to bind and continues.
            final String boundIdpUserToken = userSession.getIdpUserToken();
            if (!AppbaseUtil.isBlank(boundIdpUserToken)) {
                final String idpUserToken = resolve(exchange, AppbaseServiceConstants.IDP_USER_TOKEN_PARAM);
                if (AppbaseUtil.isBlank(idpUserToken) ||
                        !idpUserTokenCheckFilter.idpUserTokenMatches(idpUserToken, userSession))
                {
                    final Mono<Void> shortCircuit = handleIdpUserTokenMismatch(exchange);
                    if (shortCircuit != null) {
                        return shortCircuit;
                    }
                }
            }

            return chain.next(exchange);
        });
    }

    /**
     * Reproduces {@code handleIdpUserTokenMismatch}: rejects a missing configuration with a thrown
     * {@link AppbaseServiceRuntimeException}, then applies the configured forbidden/redirect/status
     * action, or returns {@code null} to continue when a configuration is present without an action.
     *
     * @param exchange the transport-neutral service exchange
     * @return a short-circuit completion signal, or {@code null} to continue
     */
    private Mono<Void> handleIdpUserTokenMismatch(final AppbaseServiceExchange exchange) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final IdpUserTokenCheckFilterConfiguration configuration =
                configurationService.getTypedConfiguration(IdpUserTokenCheckFilterConfiguration.class);
        if (configuration == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find configuration: " + IdpUserTokenCheckFilterConfiguration.class.getName());
        }

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

        // No action configured: a bound-session binding failure still fails CLOSED with a default 403.
        // The IDP-token binding is mandatory (A10-10); only an explicit forbidden/redirect/status action
        // overrides this default. Parity with the servlet twin.
        exchange.status(STATUS_FORBIDDEN);
        return exchange.writeJson(Map.of(
                "error", "Forbidden",
                "status", STATUS_FORBIDDEN));
    }

    /**
     * Resolves a request value from cookie, then header (first non-blank wins). The query string is NOT
     * consulted: a credential in the URL (CWE-598) leaks into proxy/access logs, browser history and
     * referrer headers, so it is not honoured.
     *
     * @param exchange the transport-neutral service exchange
     * @param name     the cookie/header name
     * @return the resolved value, or {@code null}
     */
    private String resolve(final AppbaseServiceExchange exchange, final String name) {
        String value = exchange.cookie(name);
        if (AppbaseUtil.isBlank(value)) {
            value = exchange.header(name);
        }
        return value;
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.IDP_TOKEN_CHECK;
    }
}
