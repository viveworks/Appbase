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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import com.viveworks.appbase.service.web.cors.CorsFilter;
import com.viveworks.appbase.service.web.cors.DefaultCorsFilter;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Thin adapter that reproduces {@code CorsServletFilter} on the transport-neutral exchange: it asks
 * the SAME {@link CorsFilter} neutral logic ({@code DefaultCorsFilter}) for the response headers and
 * copies them onto the response, then ALWAYS continues the chain.
 *
 * <p>Like the servlet twin, this filter passes {@code null} for the request-header map -- {@code
 * DefaultCorsFilter} never reads it -- and it does NOT short-circuit or special-case preflight
 * {@code OPTIONS} requests. The servlet filter has no preflight handling either; adding it here
 * would be new behaviour, not parity, so it is deliberately omitted.
 *
 * <p><strong>Path encoding, by design not by accident.</strong> The servlet twin forwards {@code
 * HttpServletRequest.getRequestURI()} -- context-path-inclusive and percent-ENCODED -- to {@code
 * corsHeaders}. This filter forwards {@link AppbaseServiceExchange#path()}, which is reactor-netty's
 * {@code fullPath()}: context-path-free and DECODED. {@code DefaultCorsFilter} looks its config up on
 * the exact string it is handed ({@code pathByCorsHeadersMap.get(requestUri)}), so for any path
 * containing an encoded character (e.g. a space) the two faces key on different strings and a CORS
 * entry registered for one face is missed on the other. This is reported as a cross-face
 * configuration finding for Task 3b rather than normalised here, because deciding which side
 * normalises (and whether authorization's identical lookup must agree) is a design call, not a
 * detail this adapter may quietly make.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class CorsWebFilter implements AppbaseServiceFilter {

    private final AppbaseCore appbaseCore;

    private final CorsFilter corsFilter;

    /**
     * @param appbaseCore Appbase core, forwarded to the neutral CORS logic
     */
    public CorsWebFilter(final AppbaseCore appbaseCore) {
        this(appbaseCore, new DefaultCorsFilter());
    }

    /**
     * @param appbaseCore Appbase core, forwarded to the neutral CORS logic
     * @param corsFilter  the neutral CORS logic to delegate to (test seam)
     */
    CorsWebFilter(final AppbaseCore appbaseCore, final CorsFilter corsFilter) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        this.corsFilter = AppbaseUtil.notNull(corsFilter, "corsFilter");
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return Mono.defer(() -> {
            final Map<String, String> responseHeaders =
                    corsFilter.corsHeaders(appbaseCore, exchange.path(), null);
            if (responseHeaders != null) {
                responseHeaders.forEach(exchange::responseHeader);
            }

            return chain.next(exchange);
        });
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.CORS;
    }
}
