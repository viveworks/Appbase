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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests filter ordering and short-circuit behaviour of {@link DefaultAppbaseServiceFilterChain}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultAppbaseServiceFilterChainTest {

    /** Minimal in-memory exchange; the chain never touches the response side. */
    private static final class FakeExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();
        private int status = 200;

        @Override public String method() { return "GET"; }
        @Override public String path() { return "/test"; }
        @Nullable @Override public String queryParam(final String name) { return null; }
        @Nullable @Override public String header(final String name) { return null; }
        @Nullable @Override public String cookie(final String name) { return null; }
        @Override public void status(final int code) { this.status = code; }
        @Override public void responseHeader(final String name, final String value) { }
        @Override public Mono<Void> writeJson(final Object body) { return Mono.empty(); }
        @Override public <T> Mono<Void> writeStream(final Flux<T> body) { return body.then(); }
        @Override public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }
        @Override public Map<String, Object> attributes() { return attributes; }

        int status() { return status; }
    }

    /** Records its name on invocation, then continues the chain. */
    private static AppbaseServiceFilter recordingFilter(final String name, final int order, final List<String> log) {
        return new AppbaseServiceFilter() {
            @Override public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
                log.add(name);
                return chain.next(exchange);
            }
            @Override public int order() { return order; }
        };
    }

    @Test
    void filtersRunInAscendingOrderRegardlessOfListOrder() {
        final List<String> log = new ArrayList<>();
        // Deliberately supplied out of order: authorization first in the list, last in effect.
        final List<AppbaseServiceFilter> filters = List.of(
                recordingFilter("authorization", AppbaseServiceFilterOrder.AUTHORIZATION, log),
                recordingFilter("cors", AppbaseServiceFilterOrder.CORS, log),
                recordingFilter("exceptionHandler", AppbaseServiceFilterOrder.EXCEPTION_HANDLER, log));

        final AppbaseServiceHandler handler = exchange -> {
            log.add("handler");
            return Mono.empty();
        };

        final DefaultAppbaseServiceFilterChain chain = new DefaultAppbaseServiceFilterChain(filters, handler);

        StepVerifier.create(chain.next(new FakeExchange())).verifyComplete();

        Assertions.assertEquals(List.of("exceptionHandler", "cors", "authorization", "handler"), log);
    }

    @Test
    void authorizationIsAlwaysTheLastFilterBeforeTheHandler() {
        Assertions.assertTrue(AppbaseServiceFilterOrder.AUTHORIZATION > AppbaseServiceFilterOrder.IDP_TOKEN_CHECK);
        Assertions.assertTrue(AppbaseServiceFilterOrder.IDP_TOKEN_CHECK > AppbaseServiceFilterOrder.USER_SESSION);
        Assertions.assertTrue(AppbaseServiceFilterOrder.USER_SESSION > AppbaseServiceFilterOrder.TRACE_ID);
        Assertions.assertTrue(AppbaseServiceFilterOrder.TRACE_ID > AppbaseServiceFilterOrder.METRIC);
        Assertions.assertTrue(AppbaseServiceFilterOrder.METRIC > AppbaseServiceFilterOrder.CORS);
        Assertions.assertTrue(AppbaseServiceFilterOrder.CORS > AppbaseServiceFilterOrder.EXCEPTION_HANDLER);
    }

    @Test
    void aFilterThatDoesNotCallNextShortCircuitsDownstreamFiltersAndTheHandler() {
        final List<String> log = new ArrayList<>();
        final AppbaseServiceFilter rejecting = new AppbaseServiceFilter() {
            @Override public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
                exchange.status(403);
                return Mono.empty();
            }
            @Override public int order() { return AppbaseServiceFilterOrder.IDP_TOKEN_CHECK; }
        };
        // Higher-order filter placed after the rejecting one; must never observe an invocation.
        final AppbaseServiceFilter neverRuns = recordingFilter("neverRuns", AppbaseServiceFilterOrder.AUTHORIZATION, log);

        final AppbaseServiceHandler handler = exchange -> {
            log.add("handler");
            return Mono.empty();
        };

        final FakeExchange exchange = new FakeExchange();
        StepVerifier.create(new DefaultAppbaseServiceFilterChain(List.of(rejecting, neverRuns), handler).next(exchange))
                .verifyComplete();

        Assertions.assertEquals(List.of(), log);
        Assertions.assertEquals(403, exchange.status());
    }

    @Test
    void chainWithNoFiltersInvokesTheHandlerDirectly() {
        final List<String> log = new ArrayList<>();
        final AppbaseServiceHandler handler = exchange -> {
            log.add("handler");
            return Mono.empty();
        };

        StepVerifier.create(new DefaultAppbaseServiceFilterChain(List.of(), handler).next(new FakeExchange()))
                .verifyComplete();

        Assertions.assertEquals(List.of("handler"), log);
    }
}
