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

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.DispatchException;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.authorization.AuthorizationContext;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.user.account.DefaultUserAccountService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.store.ReactiveUserAccountStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.errorBody;
import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.requireBoundedInt;
import static com.viveworks.appbase.bootstrap.reactornetty.endpoint.EndpointReads.requireParam;

/**
 * The reader-facing R5 showcase: one native reactor-netty request authorizes and reads a user
 * account through {@link ReactiveUserAccountStore}, then streams matching activity records through
 * the reactive {@code LogAccessor} path as NDJSON. The route uses {@link
 * com.viveworks.appbase.service.exchange.AppbaseServiceExchange#writeStream(Flux)}, so downstream
 * write demand is propagated to the stream instead of buffering the response in memory.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ReactiveActivityFeedEndpoint {

    /** Fixed showcase route; {@link AppbaseRouteRegistry} deliberately has no path patterns. */
    public static final String DEFAULT_PATH = "/reactive-showcase/activity";

    private ReactiveActivityFeedEndpoint() {
    }

    /**
     * Registers the activity-feed showcase route.
     *
     * @param registry route table to populate
     * @param appbaseCore source of authorization, the reactive account store, and reactive log reads
     * @param maxPageSize inclusive upper bound for {@code pageSize}
     */
    public static void register(
            final AppbaseRouteRegistry registry, final AppbaseCore appbaseCore, final int maxPageSize) {
        AppbaseUtil.notNull(registry, "registry");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        registry.route("GET", DEFAULT_PATH, exchange -> Mono.defer(() -> {
            try {
                return dispatch(appbaseCore, exchange, maxPageSize);
            } catch (final DispatchException dispatchException) {
                exchange.status(dispatchException.status());
                return exchange.writeJson(errorBody(dispatchException.status(), dispatchException.getMessage()));
            }
        }));
    }

    private static Mono<Void> dispatch(
            final AppbaseCore appbaseCore,
            final com.viveworks.appbase.service.exchange.AppbaseServiceExchange exchange,
            final int maxPageSize) {
        final String email = requireParam(exchange, "email");
        final String appbaseRequesterId = requireParam(exchange, "appbaseRequesterId");
        final long inclusiveStartTimestamp = requireLong(exchange, "inclusiveStartTimestamp");
        final long exclusiveEndTimestamp = requireLong(exchange, "exclusiveEndTimestamp");
        final int pageSize = requireBoundedInt(exchange, "pageSize", maxPageSize);
        if (exclusiveEndTimestamp <= inclusiveStartTimestamp) {
            throw new DispatchException(400, "Invalid parameter: exclusiveEndTimestamp");
        }

        final AppbaseServiceEntry authorizationEntry =
                appbaseCore.getServiceRegistry().getAppbaseService(AuthorizationService.class);
        final AuthorizationService authorizationService =
                (authorizationEntry == null) ? null : (AuthorizationService) authorizationEntry.getService();
        if (authorizationService == null) {
            exchange.status(503);
            return exchange.writeJson(errorBody(503, "AuthorizationService unavailable"));
        }
        final AuthorizationContext authorizationContext = new AuthorizationContext();
        authorizationContext.setAppbaseRequesterId(appbaseRequesterId);
        final String appbaseSessionId = resolve(exchange, AppbaseServiceConstants.APPBASE_SESSION_ID_PARAM);
        if (AppbaseUtil.isNotBlank(appbaseSessionId)) {
            authorizationContext.setAppbaseSessionId(appbaseSessionId);
        }
        authorizationContext.setParameters(Map.of("email", email));
        try {
            authorizationService.checkAuthorized(
                    DefaultUserAccountService.class, "readAccountByEmail", authorizationContext);
            // The feed carries global log records, not account-scoped audit entries. Require the
            // same path/method permission that protects the existing native /log read surface so
            // an account-reader grant alone can never become a log-reader grant.
            authorizationService.checkAuthorized(
                    AppbaseServiceConstants.DEFAULT_LOG_SERVLET_PATH, "GET", authorizationContext);
        } catch (final AuthorizationServiceException authorizationException) {
            exchange.status(403);
            return exchange.writeJson(errorBody(403, "Forbidden"));
        }

        final AppbaseServiceEntry accountEntry =
                appbaseCore.getServiceRegistry().getAppbaseService(ReactiveUserAccountStore.class);
        final ReactiveUserAccountStore accountStore =
                (accountEntry == null) ? null : (ReactiveUserAccountStore) accountEntry.getService();
        if (accountStore == null) {
            exchange.status(503);
            return exchange.writeJson(errorBody(503, "ReactiveUserAccountStore unavailable"));
        }

        final Mono<ActivityFeed> activityFeed = accountStore.readByEmailReactive(email)
                .map(account -> new ActivityFeed(activityEvents(
                        account,
                        appbaseCore.getLogService().getLogAccessor().streamLogRecordsBetweenReactive(
                                inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize))))
                .defaultIfEmpty(ActivityFeed.notFound())
                .onErrorReturn(ActivityFeed.unavailable());
        return activityFeed.flatMap(feed -> {
            if (feed.events() != null) {
                return exchange.writeStream(feed.events());
            }
            exchange.status(feed.status());
            return exchange.writeJson(errorBody(feed.status(), feed.message()));
        });
    }

    private static Flux<Map<String, Object>> activityEvents(
            final UserAccount account,
            final Flux<Map.Entry<String, LogRecord>> records) {
        final Flux<Map<String, Object>> accountEvent = Flux.just(Map.of(
                "type", "account", "account", Map.of("email", account.getEmail())));
        final Flux<Map<String, Object>> logEvents = records
                .map(entry -> Map.<String, Object>of(
                        "type", "activity", "store", entry.getKey(), "record", entry.getValue()))
                .onErrorResume(exception -> Flux.just(Map.of(
                        "type", "error", "status", 503, "message", "Activity feed unavailable")));
        return accountEvent.concatWith(logEvents);
    }

    private static long requireLong(
            final com.viveworks.appbase.service.exchange.AppbaseServiceExchange exchange, final String name) {
        try {
            return Long.parseLong(requireParam(exchange, name));
        } catch (final NumberFormatException numberFormatException) {
            throw new DispatchException(400, "Invalid parameter: " + name);
        }
    }

    private static String resolve(
            final com.viveworks.appbase.service.exchange.AppbaseServiceExchange exchange, final String name) {
        String value = exchange.cookie(name);
        if (AppbaseUtil.isBlank(value)) {
            value = exchange.header(name);
        }
        return AppbaseUtil.isBlank(value) ? exchange.queryParam(name) : value;
    }

    private record ActivityFeed(Flux<Map<String, Object>> events, int status, String message) {
        private ActivityFeed(final Flux<Map<String, Object>> events) {
            this(events, 200, null);
        }

        private static ActivityFeed notFound() {
            return new ActivityFeed(null, 404, "Not Found");
        }

        private static ActivityFeed unavailable() {
            return new ActivityFeed(null, 503, "Activity feed unavailable");
        }
    }

}
