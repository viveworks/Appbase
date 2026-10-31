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

package com.viveworks.appbase.service.user.session.web.filter.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
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
import com.viveworks.appbase.service.web.reactive.TraceIdWebFilter;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Thin adapter that reproduces {@code UserSessionServletFilter} on the transport-neutral exchange,
 * delegating to the SAME {@link UserSessionFilter} neutral logic ({@code DefaultUserSessionFilter}).
 * It resolves the {@code appbaseSessionId} and {@code appbaseUserId} from cookie, then header (first
 * non-blank wins; the query string is not consulted, per CWE-598), calls {@link
 * UserSessionFilter#onRequest}, and on success
 * stores the resolved {@link UserSession} in {@link AppbaseServiceExchange#attributes()} under
 * {@link AppbaseServiceConstants#USER_SESSION_PROPERTY_KEY} -- the same key {@code
 * IdpUserTokenCheckWebFilter} reads. It runs at {@link AppbaseServiceFilterOrder#USER_SESSION},
 * before the IDP-token check and authorization.
 *
 * <p><strong>{@code onResponse} placement.</strong> The servlet calls {@code onResponse} AFTER the
 * chain returns and only when a session was resolved, discarding its return value and wrapping its
 * checked exceptions. This is reproduced with {@code chain.next(...).then(Mono.fromRunnable(...))}:
 * {@code then} runs the runnable only after a SUCCESSFUL chain, not on error and not on cancel. A
 * {@code doFinally} would run on all three and would diverge from the servlet, which never calls
 * {@code onResponse} when the chain throws.
 *
 * <p><strong>Two different "no session" paths, matching the servlet exactly.</strong> When {@code
 * onRequest} RETURNS {@code null} (no exception), an unconfigured {@link
 * UserSessionNotFoundExceptionConfiguration} lets the request THROUGH without an attribute and
 * without {@code onResponse}. When {@code onRequest} THROWS a checked exception whose handler finds
 * no configured action, the servlet falls through to {@code throw new
 * AppbaseServiceRuntimeException(ex)} -- it does NOT pass through. Both are reproduced here; do not
 * "simplify" the exception path to a pass-through.
 *
 * <p><strong>Emulations, documented at the source.</strong> The servlet's {@code
 * sendError(status, message)} and {@code sendRedirect(url)} have no exchange equivalent: errors are
 * emulated as {@code status(code)} + {@code writeJson(...)}, redirects as {@code status(302)} + a
 * {@code Location} header. The servlet's {@code AppbaseHttpServletRequestWrapper.setUserSession(...)}
 * call has no neutral equivalent -- the attribute map IS the neutral mechanism -- so it is skipped.
 * The renew path builds a {@link RenewSessionRequest} with the trace id published by {@code
 * TraceIdWebFilter} into {@link AppbaseServiceExchange#attributes()} (the reactive analogue of the
 * servlet's response-header/MDC trace lookup). The servlet's per-outcome metric counters are
 * servlet-internal bookkeeping and are not reproduced.
 *
 * <p><strong>Expired-session redirect/status.</strong> The servlet's {@code handleSessionExpired}
 * writes a redirect/status and then returns {@code null}, after which the caller throws "No session
 * found after handling expiration" -- a write-then-throw that leaves the committed redirect winning
 * but a merely {@code setStatus} status overwritten by the 500. That defect cannot be reproduced on
 * an exchange with no commit semantics, so here a configured redirect short-circuits with 302 and a
 * configured status short-circuits with that status (the intended outcomes), while an expired
 * session with renew disabled and nothing else configured errors, matching the servlet's terminal
 * signal. See the Task 3b report for the full divergence note.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class UserSessionWebFilter implements AppbaseServiceFilter {

    private static final String LOCATION_HEADER = "Location";

    private static final int STATUS_FOUND = 302;

    private static final int STATUS_BAD_REQUEST = 400;

    private static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    private static final int STATUS_FORBIDDEN = 403;

    private static final String NO_SESSION_AFTER_EXPIRATION = "No session found after handling expiration";

    private final AppbaseCore appbaseCore;

    private final UserSessionFilter userSessionFilter;

    /**
     * Creates the filter with the default neutral session logic.
     *
     * @param appbaseCore Appbase core, providing the configuration and service registry
     */
    public UserSessionWebFilter(final AppbaseCore appbaseCore) {
        this(appbaseCore, new DefaultUserSessionFilter());
    }

    /**
     * Creates the filter with an explicit neutral session-logic delegate.
     *
     * @param appbaseCore       Appbase core, providing the configuration and service registry
     * @param userSessionFilter the neutral session logic to delegate to (test seam)
     */
    UserSessionWebFilter(final AppbaseCore appbaseCore, final UserSessionFilter userSessionFilter) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.userSessionFilter = AppbaseUtil.notNull(userSessionFilter, "userSessionFilter");
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return Mono.defer(() -> {
            final String appbaseSessionId = resolve(exchange, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
            final String appbaseUserId = resolve(exchange, AppbaseServiceConstants.APPBASE_USER_ID_PARAM);

            final UserSession userSession;
            try {
                userSession = userSessionFilter.onRequest(exchange.path(), appbaseCore, appbaseSessionId, appbaseUserId);
            } catch (UserAccountServiceException | AuthorizationServiceException ex) {
                return onRequestFailed(exchange, chain, ex);
            }

            return proceed(exchange, chain, userSession);
        });
    }

    /**
     * Reproduces the servlet's {@code catch (Exception ex)} block: the sign-out no-op, the
     * expired-session handling (renew / redirect / status / error), and the not-found / user-account
     * / authorization handlers with the terminal {@code throw new AppbaseServiceRuntimeException(ex)}
     * when no handler short-circuits.
     *
     * @param exchange the transport-neutral service exchange
     * @param chain    the chain continuation
     * @param ex       the exception raised by {@code onRequest}
     * @return the completion signal
     */
    private Mono<Void> onRequestFailed(
            final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain, final Exception ex)
    {
        // Sign-out request: the servlet returns without continuing the chain or running onResponse.
        if (exchange.path().endsWith(AfterIdpSignOutServlet.DEFAULT_PATH)) {
            return Mono.empty();
        }

        if (ex instanceof UserSessionExpiredException expired) {
            return handleSessionExpired(exchange, chain, expired.getExpiredUserSession());
        }

        if (ex instanceof UserSessionNotFoundException) {
            final Mono<Void> shortCircuit = handleSessionNotFound(exchange);
            if (shortCircuit != null) {
                return shortCircuit;
            }
        }
        if (ex instanceof UserAccountServiceException) {
            final Mono<Void> shortCircuit = handleUserAccountServiceException(exchange, ex.getMessage());
            if (shortCircuit != null) {
                return shortCircuit;
            }
        }
        if (ex instanceof AuthorizationServiceException) {
            final Mono<Void> shortCircuit = handleAuthorizationServiceException(exchange);
            if (shortCircuit != null) {
                return shortCircuit;
            }
        }

        return Mono.error(new AppbaseServiceRuntimeException(ex));
    }

    /**
     * After the try/catch, the servlet stores the resolved session and continues, or (when the
     * session is {@code null} and not-found is unconfigured) continues without an attribute and
     * without {@code onResponse}.
     *
     * @param exchange    the transport-neutral service exchange
     * @param chain       the chain continuation
     * @param userSession the resolved session, possibly {@code null}
     * @return the completion signal
     */
    private Mono<Void> proceed(
            final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain, final UserSession userSession)
    {
        if (userSession == null) {
            final Mono<Void> shortCircuit = handleSessionNotFound(exchange);
            if (shortCircuit != null) {
                return shortCircuit;
            }
            // No configured action for a null session: continue without attribute and without onResponse.
            return chain.next(exchange);
        }

        exchange.attributes().put(AppbaseServiceConstants.USER_SESSION_PROPERTY_KEY, userSession);
        return chain.next(exchange)
                .then(Mono.fromRunnable(() -> onResponse(userSession)));
    }

    /**
     * Reproduces {@code handleSessionExpired}: renew (continue with the renewed session and set the
     * {@code newAppbaseSessionId} header), or redirect/status short-circuit, or -- when nothing
     * renewable is configured -- the servlet's terminal "No session found after handling expiration".
     *
     * @param exchange        the transport-neutral service exchange
     * @param chain           the chain continuation
     * @param expiredSession  the expired session carried by the exception
     * @return the completion signal
     */
    private Mono<Void> handleSessionExpired(
            final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain, final UserSession expiredSession)
    {
        final UserSessionExpiredExceptionConfiguration configuration =
                getTypedConfiguration(UserSessionExpiredExceptionConfiguration.class);
        if (configuration != null) {
            if (configuration.isRenewSession()) {
                final UserSession renewed = renewSession(exchange, expiredSession);
                if (renewed == null) {
                    return Mono.error(new AppbaseServiceRuntimeException(NO_SESSION_AFTER_EXPIRATION));
                }
                exchange.responseHeader(
                        AppbaseServiceConstants.NEW_APPBASE_SESSION_ID_PARAM, renewed.getAppbaseSessionId());
                return proceed(exchange, chain, renewed);
            } else if (AppbaseUtil.isNotBlank(configuration.getRedirectUrl())) {
                return respondRedirect(exchange, configuration.getRedirectUrl());
            } else if (configuration.getHttpStatusCode() > 0) {
                exchange.status(configuration.getHttpStatusCode());
                return Mono.empty();
            }
        }

        return Mono.error(new AppbaseServiceRuntimeException(NO_SESSION_AFTER_EXPIRATION));
    }

    /**
     * Renews the expired session via {@link UserAccountService}, mirroring {@code
     * handleSessionExpired}'s renew branch. The servlet raises {@code ServletException} when the
     * service is missing or renewal fails; the reactive face has no {@code ServletException}, so it
     * raises {@link AppbaseServiceRuntimeException} instead.
     *
     * @param exchange       the transport-neutral service exchange
     * @param expiredSession the expired session to renew
     * @return the renewed session, or {@code null} if the response carried none
     */
    private UserSession renewSession(final AppbaseServiceExchange exchange, final UserSession expiredSession) {
        final AppbaseServiceEntry appbaseServiceEntry =
                appbaseCore.getServiceRegistry().getAppbaseService(UserAccountService.class);
        if (appbaseServiceEntry == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find service entry for " + UserAccountService.class.getName());
        }
        final UserAccountService userAccountService = (UserAccountService) appbaseServiceEntry.getService();
        if (userAccountService == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find an instance of " + UserAccountService.class.getName());
        }

        final RenewSessionRequest renewSessionRequest = new RenewSessionRequest(traceId(exchange), expiredSession);
        try {
            final RenewSessionResponse renewSessionResponse = userAccountService.renewSession(renewSessionRequest);
            return renewSessionResponse.getRenewedUserSession();
        } catch (UserAccountServiceException ex) {
            throw new AppbaseServiceRuntimeException("Error while renewing session", ex);
        }
    }

    /**
     * Runs {@code onResponse}, discarding its return value and wrapping its checked exceptions -- the
     * servlet's post-chain behaviour, exactly.
     *
     * @param userSession the resolved session
     */
    private void onResponse(final UserSession userSession) {
        try {
            userSessionFilter.onResponse(appbaseCore, userSession);
        } catch (UserAccountServiceException | AuthorizationServiceException | AuthenticationServiceException ex) {
            throw new AppbaseServiceRuntimeException("Error writing session to the store", ex);
        }
    }

    /**
     * Reproduces {@code handleSessionNotFound}. Returns a short-circuit signal when an action is
     * configured, or {@code null} to let the caller continue.
     *
     * @param exchange the transport-neutral service exchange
     * @return a short-circuit completion signal, or {@code null}
     */
    private Mono<Void> handleSessionNotFound(final AppbaseServiceExchange exchange) {
        final UserSessionNotFoundExceptionConfiguration configuration =
                getTypedConfiguration(UserSessionNotFoundExceptionConfiguration.class);
        if (configuration != null) {
            if (configuration.isRespondBadRequest()) {
                exchange.status(STATUS_BAD_REQUEST);
                return exchange.writeJson(Map.of(
                        "error", AppbaseServiceConstants.SESSION_NOT_FOUND,
                        "status", STATUS_BAD_REQUEST));
            } else if (AppbaseUtil.isNotBlank(configuration.getRedirectUrl())) {
                return respondRedirect(exchange, configuration.getRedirectUrl());
            } else if (configuration.getHttpStatusCode() > 0) {
                exchange.status(configuration.getHttpStatusCode());
                return Mono.empty();
            }
        }

        return null;
    }

    /**
     * Reproduces {@code handleUserAccountServiceException}.
     *
     * @param exchange the transport-neutral service exchange
     * @param message  the exception message
     * @return a short-circuit completion signal, or {@code null}
     */
    private Mono<Void> handleUserAccountServiceException(final AppbaseServiceExchange exchange, final String message) {
        final UserAccountServiceExceptionConfiguration configuration =
                getTypedConfiguration(UserAccountServiceExceptionConfiguration.class);
        if (configuration != null) {
            if (configuration.isRespondInternalServerError()) {
                exchange.status(STATUS_INTERNAL_SERVER_ERROR);
                return exchange.writeJson(Map.of(
                        "error", String.valueOf(message),
                        "status", STATUS_INTERNAL_SERVER_ERROR));
            } else if (AppbaseUtil.isNotBlank(configuration.getRedirectUrl())) {
                return respondRedirect(exchange, configuration.getRedirectUrl());
            } else if (configuration.getHttpStatusCode() > 0) {
                exchange.status(configuration.getHttpStatusCode());
                return Mono.empty();
            }
        }

        return null;
    }

    /**
     * Reproduces {@code AbstractAppbaseServletFilter#handleAuthorizationServiceException} as invoked
     * from the session filter.
     *
     * @param exchange the transport-neutral service exchange
     * @return a short-circuit completion signal, or {@code null}
     */
    private Mono<Void> handleAuthorizationServiceException(final AppbaseServiceExchange exchange) {
        final AuthorizationServiceExceptionConfiguration configuration =
                getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        if (configuration != null) {
            if (configuration.isRespondForbidden()) {
                exchange.status(STATUS_FORBIDDEN);
                return exchange.writeJson(Map.of(
                        "error", "Forbidden",
                        "status", STATUS_FORBIDDEN));
            } else if (AppbaseUtil.isNotBlank(configuration.getRedirectUrl())) {
                return respondRedirect(exchange, configuration.getRedirectUrl());
            } else if (configuration.getHttpStatusCode() > 0) {
                exchange.status(configuration.getHttpStatusCode());
                return Mono.empty();
            }
        }

        return null;
    }

    /**
     * Emulates {@code sendRedirect}: 302 status plus a {@code Location} header, then complete.
     *
     * @param exchange    the transport-neutral service exchange
     * @param redirectUrl the redirect target
     * @return a completing signal
     */
    private Mono<Void> respondRedirect(final AppbaseServiceExchange exchange, final String redirectUrl) {
        exchange.status(STATUS_FOUND);
        exchange.responseHeader(LOCATION_HEADER, redirectUrl);
        return Mono.empty();
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

    /**
     * Reads the trace id published by {@code TraceIdWebFilter} into the exchange attributes, the
     * reactive analogue of the servlet's response-header/MDC trace lookup.
     *
     * @param exchange the transport-neutral service exchange
     * @return the trace id, or {@code null} when none was published
     */
    private String traceId(final AppbaseServiceExchange exchange) {
        final Object traceId = exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE);
        return traceId != null ? traceId.toString() : null;
    }

    private <T extends AppbaseConfiguration> T getTypedConfiguration(final Class<T> configurationType) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        return configurationService.getTypedConfiguration(configurationType);
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.USER_SESSION;
    }
}
