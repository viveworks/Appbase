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

package com.viveworks.appbase.service.exchange;

import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable, ordered filter chain. Sorts the supplied filters by {@link AppbaseServiceFilter#order()}
 * ascending at construction, so callers may register filters in any order and still get the
 * security-critical sequence in {@link AppbaseServiceFilterOrder}.
 *
 * <p>Each {@link #next(AppbaseServiceExchange)} call advances one position via a fresh, cheap
 * continuation object rather than mutating shared state, so one chain instance is safe to reuse
 * across concurrent requests.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultAppbaseServiceFilterChain implements AppbaseServiceFilterChain {

    private final List<AppbaseServiceFilter> filters;

    private final AppbaseServiceHandler handler;

    private final int index;

    /**
     * @param filters filters to apply, in any order; sorted by {@code order()} ascending
     * @param handler terminal handler, invoked after the last filter
     */
    public DefaultAppbaseServiceFilterChain(final List<AppbaseServiceFilter> filters, final AppbaseServiceHandler handler) {
        AppbaseUtil.notNull(filters, "filters");
        AppbaseUtil.notNull(handler, "handler");

        final List<AppbaseServiceFilter> sorted = new ArrayList<>(filters);
        sorted.sort(Comparator.comparingInt(AppbaseServiceFilter::order));

        this.filters = List.copyOf(sorted);
        this.handler = handler;
        this.index = 0;
    }

    private DefaultAppbaseServiceFilterChain(
            final List<AppbaseServiceFilter> filters, final AppbaseServiceHandler handler, final int index) {
        this.filters = filters;
        this.handler = handler;
        this.index = index;
    }

    /**
     * Invokes the filter at this continuation's index with a fresh continuation, or invokes the terminal
     * handler once all filters are exhausted.
     * @param exchange request/response exchange passed unchanged through the chain
     * @return completion signal from the selected filter or terminal handler
     */
    @Override
    public Mono<Void> next(final AppbaseServiceExchange exchange) {
        if (index >= filters.size()) {
            return handler.handle(exchange);
        }

        final AppbaseServiceFilter filter = filters.get(index);
        final DefaultAppbaseServiceFilterChain rest =
                new DefaultAppbaseServiceFilterChain(filters, handler, index + 1);
        return filter.filter(exchange, rest);
    }
}
