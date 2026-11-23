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

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationContext;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.DefaultUserAccountService;
import com.viveworks.appbase.service.user.account.data.ReadSessionResponse;
import com.viveworks.appbase.service.user.session.store.ReactiveUserSessionStore;
import com.viveworks.appbase.service.web.reactive.TraceIdWebFilter;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Native, natively-non-blocking read of one {@code UserSession} through {@link
 * ReactiveUserSessionStore} -- a thin proof consumer that the reactive store landed by N01 is
 * reachable through a native route and serializes to the same response DTO the blocking servlet
 * returns.
 *
 * <p>Read-only, no offload: {@link ReactiveUserSessionStore} is natively non-blocking end to end,
 * so this handler runs directly on the event loop, with no {@code .timeout} -- unlike the offload
 * family in {@link OffloadedEndpoints}, this route does not cross the N01 offload boundary.
 *
 * <p>Authorization parity: before reading the store, this endpoint enforces the same
 * resource-scoped {@code checkAuthorized} gate that the blocking
 * {@code DefaultUserAccountService.readSession} enforces at the service layer, closing the BOLA gap
 * the native path would otherwise have.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class NativeUserSessionEndpoint {

    private NativeUserSessionEndpoint() {
    }

    /**
     * Registers the {@code GET} user-session read route on {@code registry}.
     *
     * @param registry route table to populate
     * @param appbaseCore Appbase core, providing the service registry
     */
    public static void register(final AppbaseRouteRegistry registry, final AppbaseCore appbaseCore) {
        AppbaseUtil.notNull(registry, "registry");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        registry.route("GET", AppbaseServiceConstants.DEFAULT_USER_SESSION_READ_PATH, exchange -> {
            final String appbaseUserId = exchange.queryParam("appbaseUserId");
            final String appbaseSessionId = exchange.queryParam("appbaseSessionId");
            final String appbaseRequesterId = exchange.queryParam("appbaseRequesterId");
            if (AppbaseUtil.isBlank(appbaseUserId) || AppbaseUtil.isBlank(appbaseSessionId)
                    || AppbaseUtil.isBlank(appbaseRequesterId)) {
                exchange.status(400);
                return exchange.writeJson(errorBody(400, "Invalid parameter"));
            }

            final AppbaseServiceEntry authzEntry =
                    appbaseCore.getServiceRegistry().getAppbaseService(AuthorizationService.class);
            final AuthorizationService authorizationService =
                    (authzEntry == null) ? null : (AuthorizationService) authzEntry.getService();
            if (authorizationService == null) {
                exchange.status(503);
                return exchange.writeJson(errorBody(503, "AuthorizationService unavailable"));
            }
            final AuthorizationContext authorizationContext = new AuthorizationContext();
            authorizationContext.setAppbaseRequesterId(appbaseRequesterId);
            authorizationContext.setAppbaseSessionId(appbaseSessionId);
            authorizationContext.setParameters(Map.of("appbaseUserId", appbaseUserId));
            try {
                authorizationService.checkAuthorized(DefaultUserAccountService.class, "readSession", authorizationContext);
            } catch (AuthorizationServiceException e) {
                exchange.status(403);
                return exchange.writeJson(errorBody(403, "Forbidden"));
            }

            final AppbaseServiceEntry entry =
                    appbaseCore.getServiceRegistry().getAppbaseService(ReactiveUserSessionStore.class);
            final ReactiveUserSessionStore store =
                    (entry == null) ? null : (ReactiveUserSessionStore) entry.getService();
            if (store == null) {
                exchange.status(503);
                return exchange.writeJson(errorBody(503, "ReactiveUserSessionStore unavailable"));
            }

            final String traceId = (String) exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE);
            return store.readSessionReactive(appbaseUserId, appbaseSessionId)
                    .<Object>map(session -> new ReadSessionResponse(traceId, session))
                    .switchIfEmpty(Mono.fromSupplier(() -> {
                        exchange.status(404);
                        return errorBody(404, "Not Found");
                    }))
                    .flatMap(exchange::writeJson);
        });
    }

    private static Map<String, Object> errorBody(final int status, final String message) {
        return Map.of("status", status, "message", message);
    }
}
