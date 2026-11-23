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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api.reactive;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchAccessorException;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.Hits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveElasticsearchAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveElasticsearchAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the metric fixtures it was built from, so tests
     * can both drive the accessor and verify the shared spine's interactions (e.g. timer
     * recording).
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Timer timer;
        final Timer.Context timerContext;
        final MetricRegistry metricRegistry;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer,
                              final Timer.Context timerContext, final MetricRegistry metricRegistry) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.metricRegistry = metricRegistry;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveElasticsearchAccessorTest.class));

            final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
            final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
            final Timer timer = Mockito.mock(Timer.class);
            final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
            final Counter errorCounter = Mockito.mock(Counter.class);
            Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
            Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
            Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
            Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(errorCounter);
            Mockito.when(timer.time()).thenReturn(timerContext);

            final TracerService tracerService = Mockito.mock(TracerService.class);
            final Tracer tracer = Mockito.mock(Tracer.class);
            final Tracer.SpanBuilder spanBuilder = Mockito.mock(Tracer.SpanBuilder.class);
            final Span span = Mockito.mock(Span.class);
            Mockito.when(appbaseCore.getTracerService()).thenReturn(tracerService);
            Mockito.when(tracerService.getTracer()).thenReturn(tracer);
            Mockito.when(tracer.buildSpan(Mockito.anyString())).thenReturn(spanBuilder);
            Mockito.when(spanBuilder.start()).thenReturn(span);

            final MediatorService mediatorService = Mockito.mock(MediatorService.class);
            final Mediator mediator = Mockito.mock(Mediator.class);
            Mockito.when(appbaseCore.getMediatorService()).thenReturn(mediatorService);
            Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
            Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

            return new CoreFixtures(appbaseCore, timer, timerContext, metricRegistry);
        }
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveElasticsearchAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<ElasticsearchRestClient> borrow) {
        return new ReactiveElasticsearchAccessor(core.appbaseCore, mockReactorService(), configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    private static SearchResponse searchResponseWithHits(final Hits... hits) {
        final SearchHits searchHits = new SearchHits();
        searchHits.setHits(List.of(hits));
        final SearchResponseBody body = new SearchResponseBody();
        body.setHits(searchHits);
        return new SearchResponse(body);
    }

    @Test
    void searchEmitsSearchResponse() {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final SearchResponse response = Mockito.mock(SearchResponse.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.asyncSearch(request)).thenReturn(CompletableFuture.completedFuture(response));
        final ReactiveElasticsearchAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> client);

        StepVerifier.create(accessor.search("search", request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void failedFutureSurfacesElasticsearchAccessorException() {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final ResponseException responseException = Mockito.mock(ResponseException.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.asyncSearch(request)).thenReturn(CompletableFuture.failedFuture(responseException));
        final ReactiveElasticsearchAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> client);

        StepVerifier.create(accessor.search("search", request))
                .expectError(ElasticsearchAccessorException.class)
                .verify();
    }

    @Test
    void timerRecordedForSuccessfulOperation() {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final SearchResponse response = Mockito.mock(SearchResponse.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.asyncSearch(request)).thenReturn(CompletableFuture.completedFuture(response));
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveElasticsearchAccessor accessor = accessor(core, defaultConfig(), () -> client);

        accessor.search("search", request).block();

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }

    @Test
    void searchHitsEmitsEachHit() {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final Hits hit1 = new Hits();
        final Hits hit2 = new Hits();
        final SearchResponse response = searchResponseWithHits(hit1, hit2);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.asyncSearch(request)).thenReturn(CompletableFuture.completedFuture(response));
        final ReactiveElasticsearchAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> client);

        StepVerifier.create(accessor.searchHits("search", request))
                .expectNext(hit1, hit2)
                .verifyComplete();
    }
}
