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
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code MetricService.name(String, String...)} is a real (not mocked) default method here -- it is
 * pure string concatenation with no other interface calls -- so the mocked {@link MetricService}
 * uses {@code Mockito.CALLS_REAL_METHODS} and only {@code getMetricRegistry()} is stubbed.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class MetricWebFilterTest {

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

    private final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
    private final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
    private final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
    private final Timer timer = Mockito.mock(Timer.class);
    private final Timer.Context timerContext = Mockito.mock(Timer.Context.class);

    private void wireMetricStack() {
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
        Mockito.when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
        Mockito.when(timer.time()).thenReturn(timerContext);
    }

    @Test
    void aSuccessfulChainTimesTheRequestAndStopsOnCompletion() {
        wireMetricStack();

        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain ok = ex -> Mono.empty();

        StepVerifier.create(new MetricWebFilter(appbaseCore).filter(exchange, ok)).verifyComplete();

        Mockito.verify(timer).time();
        Mockito.verify(timerContext).stop();
    }

    @Test
    void aFailedChainIncrementsAnErrorCounterAndStillStopsTheTimer() {
        wireMetricStack();
        final Counter counter = Mockito.mock(Counter.class);
        Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(counter);

        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain failing = ex -> Mono.error(new IllegalStateException("boom"));

        StepVerifier.create(new MetricWebFilter(appbaseCore).filter(exchange, failing))
                .expectError(IllegalStateException.class)
                .verify();

        Mockito.verify(counter).inc();
        Mockito.verify(timerContext).stop();
    }

    @Test
    void aCancelledChainStillStopsTheTimerSoItCannotLeak() {
        wireMetricStack();

        final RecordingExchange exchange = new RecordingExchange();
        final AppbaseServiceFilterChain hanging = ex -> Mono.never();

        StepVerifier.create(new MetricWebFilter(appbaseCore).filter(exchange, hanging))
                .thenAwait()
                .thenCancel()
                .verify();

        Mockito.verify(timerContext).stop();
    }
}
