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

package com.viveworks.appbase.service.web.oauth.reactive;

import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import reactor.core.publisher.Mono;

/**
 * The continuation invoked when {@link ReactiveOAuthFilter} authenticates a request: the reactive
 * counterpart of the servlet {@code FilterChain}. Returning a {@link Mono} keeps the downstream handling
 * non-blocking, so the filter never has to block the event loop to hand a request onward.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@FunctionalInterface
public interface ReactiveWebFilterChain {

    /**
     * Continue processing the authenticated exchange.
     *
     * @param exchange the request/response exchange, carrying the validated token as an attribute
     * @return a {@link Mono} that completes when downstream handling completes
     */
    Mono<Void> filter(AppbaseServiceExchange exchange);
}
