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

package com.viveworks.appbase.service.exchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Transport-neutral view of one HTTP request/response. Deliberately mentions neither the servlet
 * API nor Netty: it is the single abstraction over which reactive cross-cutting filters are
 * written once, rather than once per transport.
 *
 * <p>The response side returns {@link Mono}/{@link Flux} because that is the whole point. The
 * pre-existing neutral logic interfaces in {@code appbase-service-web} ({@code
 * AuthorizationFilter#isAuthorized}, {@code CorsFilter#corsHeaders}, ...) are request-side
 * predicates over extracted primitives; they work, and R1 reuses them unchanged. But a
 * primitives-only contract has nowhere to express "write this {@link Flux}, gated on downstream
 * demand", which is exactly what a non-blocking server face exists to do.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface AppbaseServiceExchange {

    /**
     * @return the HTTP method, upper case, e.g. {@code GET}
     */
    String method();

    /**
     * @return the request path, without query string, e.g. {@code /health}
     */
    String path();

    /**
     * @param name query parameter name
     * @return the first value, or {@code null} if absent
     */
    @Nullable
    String queryParam(String name);

    /**
     * @param name header name, case-insensitive
     * @return the first value, or {@code null} if absent
     */
    @Nullable
    String header(String name);

    /**
     * @param name cookie name
     * @return the cookie value, or {@code null} if absent
     */
    @Nullable
    String cookie(String name);

    /**
     * Set the response status. Must be called before the response body begins.
     *
     * @param code HTTP status code
     */
    void status(int code);

    /**
     * Set a response header. Must be called before the response body begins.
     *
     * @param name header name
     * @param value header value
     */
    void responseHeader(String name, String value);

    /**
     * Serialise {@code body} as JSON, write it, and complete the response.
     *
     * @param body value to serialise
     * @return completion signal
     */
    Mono<Void> writeJson(Object body);

    /**
     * Write each element of {@code body}, gated on downstream write demand. Implementations MUST
     * NOT buffer unbounded: a slow consumer must apply back-pressure to a fast source.
     *
     * @param body elements to stream
     * @param <T> element type
     * @return completion signal
     */
    <T> Mono<Void> writeStream(Flux<T> body);

    /**
     * Read the full request body without blocking.
     *
     * @return the body bytes, empty array when there is no body
     */
    Mono<byte[]> readBody();

    /**
     * Mutable per-request attributes, used by filters to pass values down the chain (e.g. the
     * resolved {@code UserSession}). Not thread-safe; confined to one request.
     *
     * @return the attribute map
     */
    Map<String, Object> attributes();
}
