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

package com.viveworks.appbase.service.web.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.web.cors.CorsFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link CorsWebFilter} is a thin adapter over the same {@link CorsFilter} neutral logic the servlet
 * {@code CorsServletFilter} delegates to, so the tests mock {@link CorsFilter} and assert only the
 * adapter behaviour: returned headers land on the exchange, the chain always continues, and the
 * path is forwarded to {@code corsHeaders} verbatim (the {@code path()} encoding pin).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class CorsWebFilterTest {

    private static final class RecordingExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> responseHeaders = new LinkedHashMap<>();
        private final String path;

        private RecordingExchange(final String path) {
            this.path = path;
        }

        @Override public String method() { return "GET"; }
        @Override public String path() { return path; }
        @Nullable @Override public String queryParam(final String name) { return null; }
        @Nullable @Override public String header(final String name) { return null; }
        @Nullable @Override public String cookie(final String name) { return null; }
        @Override public void status(final int code) { }
        @Override public void responseHeader(final String name, final String value) {
            responseHeaders.put(name, value);
        }
        @Override public Mono<Void> writeJson(final Object body) { return Mono.empty(); }
        @Override public <T> Mono<Void> writeStream(final Flux<T> body) { return body.then(); }
        @Override public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }
        @Override public Map<String, Object> attributes() { return attributes; }

        Map<String, String> responseHeaders() { return responseHeaders; }
    }

    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final CorsFilter corsFilter = Mockito.mock(CorsFilter.class);

    @Test
    void corsHeadersReturnedByTheNeutralFilterAreSetOnTheResponseAndTheChainContinues() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST");
        Mockito.when(corsFilter.corsHeaders(Mockito.eq(appbaseCore), Mockito.anyString(), Mockito.isNull()))
                .thenReturn(headers);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(new CorsWebFilter(appbaseCore, corsFilter).filter(exchange, chain))
                .verifyComplete();

        Assertions.assertEquals("*", exchange.responseHeaders().get("Access-Control-Allow-Origin"));
        Assertions.assertEquals("GET,POST", exchange.responseHeaders().get("Access-Control-Allow-Methods"));
        Assertions.assertTrue(chainRan.get(), "the chain must always continue");
    }

    @Test
    void aNullHeaderMapSetsNoHeadersAndStillContinuesTheChain() {
        Mockito.when(corsFilter.corsHeaders(Mockito.any(), Mockito.anyString(), Mockito.isNull()))
                .thenReturn(null);

        final RecordingExchange exchange = new RecordingExchange("/things");
        final AtomicBoolean chainRan = new AtomicBoolean(false);
        final AppbaseServiceFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        StepVerifier.create(new CorsWebFilter(appbaseCore, corsFilter).filter(exchange, chain))
                .verifyComplete();

        Assertions.assertTrue(exchange.responseHeaders().isEmpty(), "no headers when the map is null");
        Assertions.assertTrue(chainRan.get(), "the chain must always continue");
    }

    /**
     * The {@code path()} agreement test (P-027 R1 path-canonicalization; was the divergence pin).
     *
     * <p>This filter forwards {@code exchange.path()} to {@code corsHeaders} verbatim, and that is
     * still its job. What changed is what {@code path()} now returns: {@link
     * com.viveworks.appbase.bootstrap.reactornetty.server.NettyWebExchange} canonicalizes the
     * request path once, at construction, so the string arriving here is already canonical. The
     * neutral {@code DefaultCorsFilter} looks CORS config up on that exact string
     * ({@code pathByCorsHeadersMap.get(path)}), and those config keys are ALSO canonicalized at load
     * ({@code CorsFilterConfiguration}). Both sides of the lookup therefore share one key space.
     *
     * <p>This once pinned a deliberate divergence: an encoded servlet key {@code /a%20b} would miss a
     * reactive request whose decoded path was {@code /a b}. That divergence is gone. The three faces
     * now agree on one canonical key for the same wire request -- pinned per face by
     * {@code NettyWebExchangeTest} (Task 4), {@code AuthorizationServletFilterTest} (Task 5), and
     * {@code UserSessionJaxrsRequestFilterTest} (Task 6), each mapping wire {@code /%61dmin} to
     * {@code /admin}. Here we pin only that this filter forwards the canonical path it is handed.
     */
    @Test
    void theCanonicalPathIsForwardedToCorsHeaders() {
        Mockito.when(corsFilter.corsHeaders(Mockito.any(), Mockito.anyString(), Mockito.isNull()))
                .thenReturn(null);

        // /admin is what NettyWebExchange.path() now yields for a wire request of /admin or /%61dmin.
        final RecordingExchange exchange = new RecordingExchange("/admin");
        final AppbaseServiceFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(new CorsWebFilter(appbaseCore, corsFilter).filter(exchange, chain))
                .verifyComplete();

        final ArgumentCaptor<String> forwardedPath = ArgumentCaptor.forClass(String.class);
        Mockito.verify(corsFilter).corsHeaders(Mockito.eq(appbaseCore), forwardedPath.capture(), Mockito.isNull());
        Assertions.assertEquals("/admin", forwardedPath.getValue(),
                "the canonical path is forwarded to the CORS lookup verbatim");
    }
}
