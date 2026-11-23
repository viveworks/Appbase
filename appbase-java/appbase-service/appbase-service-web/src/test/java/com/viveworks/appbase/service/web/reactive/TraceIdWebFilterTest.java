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
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.id.IdService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The header/key name under test is {@link TracerService#TRACE_ID_KEY} ({@code "appbase-traceId"}),
 * not an arbitrary {@code X-*} literal: {@code AppbaseServiceUtil.traceIdKey(AppbaseCore)} resolves
 * this key from configuration and falls back to that exact constant, and {@code
 * TraceIdServletFilter} uses the same key as its cookie/header/parameter/MDC name. A mocked {@link
 * AppbaseCore} (via {@code RETURNS_DEEP_STUBS}) returns a blank configured value, so resolution
 * falls through to the constant -- matching production's unconfigured default.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class TraceIdWebFilterTest {

    private static final class HeaderExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();
        private final Map<String, String> requestHeaders;
        private final Map<String, String> requestCookies;
        private final Map<String, String> requestQueryParams;
        private final Map<String, String> responseHeaders = new HashMap<>();
        private String responseThread;

        HeaderExchange(final Map<String, String> requestHeaders) {
            this(requestHeaders, Map.of(), Map.of());
        }

        HeaderExchange(
                final Map<String, String> requestHeaders,
                final Map<String, String> requestCookies,
                final Map<String, String> requestQueryParams)
        {
            this.requestHeaders = requestHeaders;
            this.requestCookies = requestCookies;
            this.requestQueryParams = requestQueryParams;
        }

        @Override public String method() { return "GET"; }
        @Override public String path() { return "/t"; }
        @Nullable @Override public String queryParam(final String name) { return requestQueryParams.get(name); }
        @Nullable @Override public String header(final String name) { return requestHeaders.get(name); }
        @Nullable @Override public String cookie(final String name) { return requestCookies.get(name); }
        @Override public void status(final int code) { }
        @Override public void responseHeader(final String name, final String value) {
            responseThread = Thread.currentThread().getName();
            responseHeaders.put(name, value);
        }
        @Override public Mono<Void> writeJson(final Object body) { return Mono.empty(); }
        @Override public <T> Mono<Void> writeStream(final Flux<T> body) { return body.then(); }
        @Override public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }
        @Override public Map<String, Object> attributes() { return attributes; }

        Map<String, String> responseHeaders() { return responseHeaders; }
        String responseThread() { return responseThread; }
    }

    private static AppbaseCore mockCore() {
        // No IdService is registered in the mocked registry, so a blank/absent trace id falls
        // through to the UUID fallback -- mirroring an environment where IdService isn't wired up.
        // A deep-stub AppbaseServiceEntry would otherwise hand back a mock whose getService() is
        // typed as AppbaseService, not IdService, and blow up on the cast in generateTraceId().
        final AppbaseCore core = Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(core.getServiceRegistry().getAppbaseService(IdService.class)).thenReturn(null);
        return core;
    }

    @Test
    void anInboundTraceIdIsPropagatedRatherThanReplaced() {
        final HeaderExchange exchange = new HeaderExchange(Map.of(TracerService.TRACE_ID_KEY, "abc-123"));
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        Assertions.assertEquals("abc-123", exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE));
        Assertions.assertEquals("abc-123", exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }

    @Test
    void anAbsentInboundTraceIdIsGenerated() {
        final HeaderExchange exchange = new HeaderExchange(Map.of());
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        final Object generated = exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE);
        Assertions.assertNotNull(generated);
        Assertions.assertEquals(generated, exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }

    @Test
    void anAbsentIdOffloadsGenerationFromTheReactiveScheduler() {
        final HeaderExchange exchange = new HeaderExchange(Map.of());

        StepVerifier.create(new TraceIdWebFilter(mockCore())
                .filter(exchange, ex -> Mono.empty())
                .subscribeOn(Schedulers.parallel())).verifyComplete();

        Assertions.assertTrue(exchange.responseThread().startsWith("boundedElastic-"),
                "UUID/SecureRandom fallback must not execute on a reactive event-loop thread");
    }

    @Test
    void anInboundIdStaysOnTheReactiveScheduler() {
        final HeaderExchange exchange = new HeaderExchange(Map.of(TracerService.TRACE_ID_KEY, "inbound"));

        StepVerifier.create(new TraceIdWebFilter(mockCore())
                .filter(exchange, ex -> Mono.empty())
                .subscribeOn(Schedulers.parallel())).verifyComplete();

        Assertions.assertTrue(exchange.responseThread().startsWith("parallel-"),
                "a propagated id should not require a blocking-scheduler hop");
    }

    @Test
    void theTraceIdSurvivesAHopOntoADifferentScheduler() {
        final HeaderExchange exchange = new HeaderExchange(Map.of(TracerService.TRACE_ID_KEY, "xyz-789"));
        final AppbaseCore core = mockCore();
        final AtomicReference<String> seenOnOtherThread = new AtomicReference<>();

        // The chain hops to boundedElastic, exactly as a Tier-1 offloaded endpoint does.
        final AppbaseServiceFilterChain hopping = ex -> Mono.deferContextual(contextView ->
                        Mono.fromRunnable(() -> seenOnOtherThread.set(contextView.get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE))))
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, hopping)).verifyComplete();

        Assertions.assertEquals("xyz-789", seenOnOtherThread.get(),
                "the trace id must ride Reactor Context, not a ThreadLocal, to survive the scheduler hop");
    }

    @Test
    void aCookieSuppliedTraceIdIsPropagatedWhenNoHeaderIsPresent() {
        final HeaderExchange exchange = new HeaderExchange(
                Map.of(), Map.of(TracerService.TRACE_ID_KEY, "cookie-456"), Map.of());
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        Assertions.assertEquals("cookie-456", exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE));
        Assertions.assertEquals("cookie-456", exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }

    @Test
    void aQueryParamSuppliedTraceIdIsPropagatedWhenNeitherHeaderNorCookieIsPresent() {
        final HeaderExchange exchange = new HeaderExchange(
                Map.of(), Map.of(), Map.of(TracerService.TRACE_ID_KEY, "query-789"));
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        Assertions.assertEquals("query-789", exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE));
        Assertions.assertEquals("query-789", exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }

    @Test
    void theCookieWinsOverHeaderAndQueryParamWhenAllThreeArePresent() {
        final HeaderExchange exchange = new HeaderExchange(
                Map.of(TracerService.TRACE_ID_KEY, "header-loses"),
                Map.of(TracerService.TRACE_ID_KEY, "cookie-wins"),
                Map.of(TracerService.TRACE_ID_KEY, "query-loses"));
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        Assertions.assertEquals("cookie-wins", exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE));
        Assertions.assertEquals("cookie-wins", exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }

    @Test
    void theHeaderWinsOverQueryParamWhenCookieIsAbsent() {
        final HeaderExchange exchange = new HeaderExchange(
                Map.of(TracerService.TRACE_ID_KEY, "header-wins"),
                Map.of(),
                Map.of(TracerService.TRACE_ID_KEY, "query-loses"));
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        Assertions.assertEquals("header-wins", exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE));
        Assertions.assertEquals("header-wins", exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }

    @Test
    void aBlankCookieFallsThroughToTheNextSourceRatherThanBeingPropagated() {
        final HeaderExchange exchange = new HeaderExchange(
                Map.of(TracerService.TRACE_ID_KEY, "header-wins"),
                Map.of(TracerService.TRACE_ID_KEY, "   "),
                Map.of());
        final AppbaseCore core = mockCore();

        StepVerifier.create(new TraceIdWebFilter(core).filter(exchange, ex -> Mono.empty())).verifyComplete();

        Assertions.assertEquals("header-wins", exchange.attributes().get(TraceIdWebFilter.TRACE_ID_ATTRIBUTE));
        Assertions.assertEquals("header-wins", exchange.responseHeaders().get(TracerService.TRACE_ID_KEY));
    }
}
