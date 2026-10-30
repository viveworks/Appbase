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
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
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

/**
 * The status mapping here mirrors {@code ServletExceptionHandlerFilter} exactly: {@link
 * PermissionDeniedException} and {@link AuthorizationServiceException} map to 403, {@link
 * ValidationServiceException}, {@code jakarta.validation.ValidationException} and {@link
 * AppbaseRuntimeException} map to 400, anything else maps to 500. Both faces must agree on these
 * codes, so the mapping is exercised for each bucket rather than only the fallback case.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ExceptionHandlerWebFilterTest {

    private static final class RecordingExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();
        private int status = 200;
        private Object body;
        private String statusThread;

        @Override public String method() { return "GET"; }
        @Override public String path() { return "/boom"; }
        @Nullable @Override public String queryParam(final String name) { return null; }
        @Nullable @Override public String header(final String name) { return null; }
        @Nullable @Override public String cookie(final String name) { return null; }
        @Override public void status(final int code) {
            this.status = code;
            this.statusThread = Thread.currentThread().getName();
        }
        @Override public void responseHeader(final String name, final String value) { }
        @Override public Mono<Void> writeJson(final Object value) { this.body = value; return Mono.empty(); }
        @Override public <T> Mono<Void> writeStream(final Flux<T> value) { return value.then(); }
        @Override public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }
        @Override public Map<String, Object> attributes() { return attributes; }

        int status() { return status; }
        Object body() { return body; }
        String statusThread() { return statusThread; }
    }

    private static AppbaseCore mockCore() {
        return Mockito.mock(AppbaseCore.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Test
    void anUnmappedExceptionIsMappedToA500AndTheErrorDoesNotEscape() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing =
                ex -> Mono.error(new IllegalStateException("boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, failing))
                .verifyComplete();

        Assertions.assertEquals(500, exchange.status());
        Assertions.assertNotNull(exchange.body(), "an error body must be written");
    }

    @Test
    void missingTraceIdOffloadsCorrelationIdGeneration() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing = ex -> Mono.error(new IllegalStateException("boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore())
                .filter(exchange, failing)
                .subscribeOn(Schedulers.parallel())).verifyComplete();

        Assertions.assertTrue(exchange.statusThread().startsWith("boundedElastic-"),
                "the UUID/SecureRandom fallback must not run on the reactive scheduler");
    }

    @Test
    void existingTraceIdNeedsNoCorrelationIdSchedulerHop() {
        final RecordingExchange exchange = new RecordingExchange();
        exchange.attributes().put(TraceIdWebFilter.TRACE_ID_ATTRIBUTE, "already-set");
        final AppbaseServiceFilterChain failing = ex -> Mono.error(new IllegalStateException("boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore())
                .filter(exchange, failing)
                .subscribeOn(Schedulers.parallel())).verifyComplete();

        Assertions.assertTrue(exchange.statusThread().startsWith("parallel-"),
                "an existing trace id should remain on the reactive scheduler");
    }

    @Test
    void anAppbaseRuntimeExceptionIsMappedToA400JustLikeTheServletFilter() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing =
                ex -> Mono.error(new AppbaseRuntimeException("boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, failing))
                .verifyComplete();

        Assertions.assertEquals(400, exchange.status());
        Assertions.assertNotNull(exchange.body(), "an error body must be written");
    }

    @Test
    void aValidationServiceExceptionIsMappedToA400JustLikeTheServletFilter() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing =
                ex -> Mono.error(new ValidationServiceException("boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, failing))
                .verifyComplete();

        Assertions.assertEquals(400, exchange.status());
    }

    @Test
    void anAuthorizationServiceExceptionIsMappedToA403JustLikeTheServletFilter() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing =
                ex -> Mono.error(new AuthorizationServiceException("boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, failing))
                .verifyComplete();

        Assertions.assertEquals(403, exchange.status());
    }

    @Test
    void aPermissionDeniedExceptionIsMappedToA403JustLikeTheServletFilter() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing =
                ex -> Mono.error(new PermissionDeniedException(
                        "APPBASE_PERMISSION_ENV", "appbase.permission", false, "boom"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, failing))
                .verifyComplete();

        Assertions.assertEquals(403, exchange.status());
    }

    @Test
    void aSuccessfulChainPassesThroughUntouched() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain ok = ex -> Mono.empty();

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, ok))
                .verifyComplete();

        Assertions.assertEquals(200, exchange.status());
        Assertions.assertNull(exchange.body());
    }

    /**
     * L2-09 (CWE-209): every uncaught reactive request failure was serialized to the remote
     * client with the throwable's concrete class name and raw message -- which can carry
     * filesystem paths, driver/query details, configuration values and other internals. An
     * unauthenticated caller only needs to reach a failing route. Pins that the body no longer
     * echoes either, and that a correlation id is present so the failure can still be traced
     * server-side.
     */
    @Test
    void unexpectedFailureDoesNotExposeClassOrMessage() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing =
                ex -> Mono.error(new RuntimeException("secret-marker /srv/private"));

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, failing))
                .verifyComplete();

        Assertions.assertEquals(500, exchange.status());
        Assertions.assertNotNull(exchange.body(), "an error body must be written");
        Assertions.assertInstanceOf(Map.class, exchange.body());
        @SuppressWarnings("unchecked")
        final Map<String, Object> body = (Map<String, Object>) exchange.body();

        final String rendered = body.toString();
        Assertions.assertFalse(rendered.contains("RuntimeException"),
                "response body must not expose the throwable's concrete class");
        Assertions.assertFalse(rendered.contains("secret-marker"),
                "response body must not expose the throwable's raw message");
        Assertions.assertFalse(rendered.contains("/srv/private"),
                "response body must not expose message content such as filesystem paths");
        Assertions.assertTrue(body.containsKey("correlationId"),
                "response body must carry a correlation id so the failure can be traced server-side");
        Assertions.assertNotNull(body.get("correlationId"));
    }

    @Test
    void aCancelledExchangeDoesNotAttemptToWriteABody() {
        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain hanging = ex -> Mono.never();

        StepVerifier.create(new ExceptionHandlerWebFilter(mockCore()).filter(exchange, hanging))
                .thenAwait()
                .thenCancel()
                .verify();

        Assertions.assertNull(exchange.body(), "a cancelled exchange must not write a response body");
    }
}
