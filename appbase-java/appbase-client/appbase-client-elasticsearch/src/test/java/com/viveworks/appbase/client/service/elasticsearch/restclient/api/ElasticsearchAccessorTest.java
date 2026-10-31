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

package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.document.AddOrUpdateDocumentResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
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

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ElasticsearchAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ElasticsearchAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the metric fixtures it was built from, so tests
     * can both drive the accessor and verify the shared spine's interactions (e.g. timer
     * recording), mirroring {@code ReactiveElasticsearchAccessorTest.CoreFixtures}.
     */
    private static final class CoreFixtures {
        private final AppbaseCore appbaseCore;
        private final Timer timer;
        private final Timer.Context timerContext;
        private final MetricRegistry metricRegistry;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer,
                              final Timer.Context timerContext, final MetricRegistry metricRegistry) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.metricRegistry = metricRegistry;
        }

        private static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ElasticsearchAccessorTest.class));

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

    private static ElasticsearchAccessor accessor(
            final CoreFixtures core,
            final Supplier<ElasticsearchRestClient> borrow) {
        return new ElasticsearchAccessor(
                core.appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(),
                borrow);
    }

    @Test
    void searchReturnsStubbedResponse() throws Exception {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final SearchResponse response = Mockito.mock(SearchResponse.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.search(request)).thenReturn(response);
        final ElasticsearchAccessor accessor = accessor(CoreFixtures.create(), () -> client);

        final SearchResponse result = accessor.search("search", request);

        assertSame(response, result);
    }

    @Test
    void addDocumentReturnsStubbedResponse() throws Exception {
        final AddOrUpdateDocumentRequest request = new AddOrUpdateDocumentRequest("{}", "my-index");
        final AddOrUpdateDocumentResponse response = Mockito.mock(AddOrUpdateDocumentResponse.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.addDocument(request)).thenReturn(response);
        final ElasticsearchAccessor accessor = accessor(CoreFixtures.create(), () -> client);

        final AddOrUpdateDocumentResponse result = accessor.addDocument("addDocument", request);

        assertSame(response, result);
    }

    @Test
    void responseExceptionMappedToElasticsearchAccessorException() throws Exception {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final ResponseException responseException = Mockito.mock(ResponseException.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.search(request)).thenThrow(responseException);
        final ElasticsearchAccessor accessor = accessor(CoreFixtures.create(), () -> client);

        assertThrows(ElasticsearchAccessorException.class, () -> accessor.search("search", request));
    }

    @Test
    void timerRecordedForSuccessfulOperation() throws Exception {
        final SearchRequest request = new SearchRequest("my-index", new SearchQueryBody());
        final SearchResponse response = Mockito.mock(SearchResponse.class);
        final ElasticsearchRestClient client = Mockito.mock(ElasticsearchRestClient.class);
        Mockito.when(client.search(request)).thenReturn(response);
        final CoreFixtures core = CoreFixtures.create();
        final ElasticsearchAccessor accessor = accessor(core, () -> client);

        accessor.search("search", request);

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
