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
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import com.viveworks.appbase.service.id.IdService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * Assigns a trace id per request, echoes it on the response, and puts it in the Reactor {@link
 * reactor.util.context.Context} so it survives the hop onto a scheduler.
 *
 * <p>Resolution order mirrors the servlet {@code TraceIdServletFilter}'s source chain with one
 * deliberate exception (MDC, see below): cookie, then header, then query parameter, then a
 * generated id ({@code IdService} if registered, otherwise a random {@link UUID}) -- all read
 * through {@link AppbaseServiceExchange#cookie(String)}, {@link
 * AppbaseServiceExchange#header(String)}, and {@link AppbaseServiceExchange#queryParam(String)}
 * so a caller that propagates its trace id via cookie or header is continued rather than
 * replaced with a fresh id, and so a request carrying both a cookie and a header resolves to the
 * same trace id on this face as it does on the servlet face.
 *
 * <p>This filter deliberately does NOT consult MDC, unlike {@code TraceIdServletFilter}. MDC is
 * {@code ThreadLocal}-backed, and on a reactive chain the thread that accepts the request carries
 * no inbound MDC state -- there is nothing there to read. That is precisely why this filter puts
 * the trace id into the Reactor {@link reactor.util.context.Context} instead: unlike a {@code
 * ThreadLocal}, {@code Context} survives a hop onto a different scheduler.
 *
 * <p>The header/cookie/parameter key is resolved via {@code AppbaseServiceUtil.traceIdKey}, the same
 * configuration-driven key the servlet filter reads and writes ({@code TracerService.TRACE_ID_KEY},
 * default {@code "appbase-traceId"}) -- NOT a hardcoded {@code X-*} literal -- so both faces agree on
 * the wire name.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class TraceIdWebFilter implements AppbaseServiceFilter {

    public static final String TRACE_ID_ATTRIBUTE = "appbase.traceId";

    private final AppbaseCore appbaseCore;

    /**
     * @param appbaseCore Appbase core, providing the configuration service
     */
    public TraceIdWebFilter(final AppbaseCore appbaseCore) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return Mono.defer(() -> {
            final String traceIdKey = AppbaseServiceUtil.traceIdKey(appbaseCore);
            return resolveTraceId(exchange, traceIdKey).flatMap(traceId -> {
                exchange.attributes().put(TRACE_ID_ATTRIBUTE, traceId);
                exchange.responseHeader(traceIdKey, traceId);

                return chain.next(exchange)
                        .contextWrite(context -> context.put(TRACE_ID_ATTRIBUTE, traceId));
            });
        });
    }

    /**
     * Resolves the inbound trace id, matching {@code TraceIdServletFilter}'s source chain (minus
     * MDC, which the reactive accepting thread has no inbound state for): cookie, then header,
     * then query parameter, then a generated id. The first non-blank value wins.
     *
     * @param exchange   the transport-neutral service exchange
     * @param traceIdKey the configuration-resolved trace id key
     * @return the resolved (or generated) trace id, never blank
     */
    private Mono<String> resolveTraceId(final AppbaseServiceExchange exchange, final String traceIdKey) {
        final String cookie = exchange.cookie(traceIdKey);
        if (AppbaseUtil.isNotBlank(cookie)) {
            return Mono.just(cookie);
        }

        final String header = exchange.header(traceIdKey);
        if (AppbaseUtil.isNotBlank(header)) {
            return Mono.just(header);
        }

        final String queryParam = exchange.queryParam(traceIdKey);
        if (AppbaseUtil.isNotBlank(queryParam)) {
            return Mono.just(queryParam);
        }

        // Both IdService and UUID's SecureRandom fallback may block. Only the generation path
        // leaves the event loop; propagated IDs remain on the request's current scheduler.
        return Mono.fromCallable(this::generateTraceId).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Generates a trace id the same way {@code TraceIdServletFilter} does when no inbound source
     * supplies one: via {@link IdService} if it is registered, falling back to a random {@link
     * UUID} otherwise.
     *
     * @return a freshly generated, never-blank trace id
     */
    private String generateTraceId() {
        final AppbaseServiceEntry appbaseServiceEntry =
                appbaseCore.getServiceRegistry().getAppbaseService(IdService.class);
        if (appbaseServiceEntry != null) {
            final IdService idService = (IdService) appbaseServiceEntry.getService();
            final String generated = idService.newId();
            if (AppbaseUtil.isNotBlank(generated)) {
                return generated;
            }
        }

        return UUID.randomUUID().toString();
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.TRACE_ID;
    }
}
