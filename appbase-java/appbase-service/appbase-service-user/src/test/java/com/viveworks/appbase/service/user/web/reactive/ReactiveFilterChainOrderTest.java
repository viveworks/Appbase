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

package com.viveworks.appbase.service.user.web.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceHandler;
import com.viveworks.appbase.service.exchange.DefaultAppbaseServiceFilterChain;
import com.viveworks.appbase.service.user.account.web.filter.reactive.IdpUserTokenCheckWebFilter;
import com.viveworks.appbase.service.user.session.web.filter.reactive.UserSessionWebFilter;
import com.viveworks.appbase.service.web.reactive.AuthorizationWebFilter;
import com.viveworks.appbase.service.web.reactive.CorsWebFilter;
import com.viveworks.appbase.service.web.reactive.ExceptionHandlerWebFilter;
import com.viveworks.appbase.service.web.reactive.MetricWebFilter;
import com.viveworks.appbase.service.web.reactive.TraceIdWebFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Security invariant: the seven standard reactive filters must resolve to the execution order
 * {@code exceptionHandler -> CORS -> metric -> traceId -> userSession -> idpTokenCheck ->
 * authorization}, with <strong>authorization LAST</strong>. Authorizing before the session is
 * resolved would authorize an unauthenticated principal, so any reordering is a security regression.
 *
 * <p>{@code appbase-service-user} is the only module that sees all seven types on its classpath (it
 * depends on {@code appbase-service-web}), so this test lives here. The filters are the REAL
 * instances -- so their {@link AppbaseServiceFilter#order()} values are exercised -- wrapped in a
 * recorder that logs identity and continues without running the wrapped body (whose real work would
 * need the full Appbase runtime). The wrapping does NOT influence order: the recorder delegates
 * {@code order()} to the wrapped real filter, and the REAL {@link DefaultAppbaseServiceFilterChain}
 * performs the sort and sequencing.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveFilterChainOrderTest {

    private static final class RecordingExchange implements AppbaseServiceExchange {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override public String method() { return "GET"; }
        @Override public String path() { return "/things"; }
        @Nullable @Override public String queryParam(final String name) { return null; }
        @Nullable @Override public String header(final String name) { return null; }
        @Nullable @Override public String cookie(final String name) { return null; }
        @Override public void status(final int code) { }
        @Override public void responseHeader(final String name, final String value) { }
        @Override public Mono<Void> writeJson(final Object body) { return Mono.empty(); }
        @Override public <T> Mono<Void> writeStream(final Flux<T> body) { return body.then(); }
        @Override public Mono<byte[]> readBody() { return Mono.just(new byte[0]); }
        @Override public Map<String, Object> attributes() { return attributes; }
    }

    /**
     * Records the wrapped filter's class on execution and continues the chain, delegating {@code
     * order()} to the wrapped real filter so the real order values drive the resolved sequence.
     */
    private static final class OrderRecordingFilter implements AppbaseServiceFilter {
        private final AppbaseServiceFilter delegate;
        private final List<Class<?>> executionLog;

        private OrderRecordingFilter(final AppbaseServiceFilter delegate, final List<Class<?>> executionLog) {
            this.delegate = delegate;
            this.executionLog = executionLog;
        }

        @Override
        public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
            executionLog.add(delegate.getClass());
            return chain.next(exchange);
        }

        @Override
        public int order() {
            return delegate.order();
        }
    }

    @Test
    void theSevenReactiveFiltersResolveToTheSecurityOrderWithAuthorizationLast() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(ReactiveFilterChainOrderTest.class));

        final AppbaseServiceFilter exceptionHandler = new ExceptionHandlerWebFilter(appbaseCore);
        final AppbaseServiceFilter cors = new CorsWebFilter(appbaseCore);
        final AppbaseServiceFilter metric = new MetricWebFilter(appbaseCore);
        final AppbaseServiceFilter traceId = new TraceIdWebFilter(appbaseCore);
        final AppbaseServiceFilter userSession = new UserSessionWebFilter(appbaseCore);
        final AppbaseServiceFilter idpTokenCheck = new IdpUserTokenCheckWebFilter(appbaseCore);
        final AppbaseServiceFilter authorization = new AuthorizationWebFilter(appbaseCore);

        final List<Class<?>> executionLog = new ArrayList<>();

        // Deliberately scrambled: authorization first, exceptionHandler buried in the middle.
        final List<AppbaseServiceFilter> scrambled = new ArrayList<>(List.of(
                new OrderRecordingFilter(authorization, executionLog),
                new OrderRecordingFilter(metric, executionLog),
                new OrderRecordingFilter(idpTokenCheck, executionLog),
                new OrderRecordingFilter(exceptionHandler, executionLog),
                new OrderRecordingFilter(userSession, executionLog),
                new OrderRecordingFilter(cors, executionLog),
                new OrderRecordingFilter(traceId, executionLog)));
        Collections.shuffle(scrambled);

        final AppbaseServiceHandler terminal = exchange -> Mono.empty();
        final AppbaseServiceFilterChain chain = new DefaultAppbaseServiceFilterChain(scrambled, terminal);

        StepVerifier.create(chain.next(new RecordingExchange())).verifyComplete();

        Assertions.assertEquals(
                List.of(
                        ExceptionHandlerWebFilter.class,
                        CorsWebFilter.class,
                        MetricWebFilter.class,
                        TraceIdWebFilter.class,
                        UserSessionWebFilter.class,
                        IdpUserTokenCheckWebFilter.class,
                        AuthorizationWebFilter.class),
                executionLog,
                "authorization must run LAST; any other order is a security regression");

        Assertions.assertEquals(AuthorizationWebFilter.class, executionLog.get(executionLog.size() - 1),
                "authorization must be the final filter");
    }
}
