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

package com.viveworks.appbase.client.service.jasync.reactive;

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.ResultSet;
import com.github.jasync.sql.db.RowData;
import com.github.jasync.sql.db.exceptions.DatabaseException;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.jasync.JasyncAccessorException;
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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveJasyncAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveJasyncAccessorTest {

    /**
     * Thin test-only {@link ResultSet} backed by a real {@link ArrayList}, so {@link
     * QueryResult#getRows()} can be driven deterministically without relying on Mockito to
     * fake {@code List}'s default methods (e.g. {@code spliterator()}, which {@link
     * Flux#fromIterable} relies on and a plain mock leaves unstubbed/null).
     */
    private static final class TestResultSet extends ArrayList<RowData> implements ResultSet {

        TestResultSet(final List<RowData> rows) {
            super(rows);
        }

        @Override
        public List<String> columnNames() {
            return Collections.emptyList();
        }
    }

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
                    .thenReturn(LoggerFactory.getLogger(ReactiveJasyncAccessorTest.class));

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

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<Connection, TextValue> mockIntegrationService(
            final Connection connection) {
        final AppbaseClientIntegrationService<Connection, TextValue> integrationService =
                Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(connection);
        return integrationService;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveJasyncAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<Connection, TextValue>> borrow) {
        return new ReactiveJasyncAccessor(core.appbaseCore, mockReactorService(), configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    @Test
    void sendPreparedStatementEmitsQueryResult() {
        final Connection connection = Mockito.mock(Connection.class);
        final QueryResult queryResult = Mockito.mock(QueryResult.class);
        final List<Object> params = Collections.singletonList("p1");
        Mockito.when(connection.sendPreparedStatement("select ?", params))
                .thenReturn(CompletableFuture.completedFuture(queryResult));
        final AppbaseClientIntegrationService<Connection, TextValue> integrationService =
                mockIntegrationService(connection);
        final ReactiveJasyncAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.sendPreparedStatement("select", "select ?", params))
                .expectNext(queryResult)
                .verifyComplete();
    }

    @Test
    void failedFutureSurfacesJasyncAccessorException() {
        final Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.sendQuery("select 1"))
                .thenReturn(CompletableFuture.failedFuture(new DatabaseException("x")));
        final AppbaseClientIntegrationService<Connection, TextValue> integrationService =
                mockIntegrationService(connection);
        final ReactiveJasyncAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.sendQuery("select", "select 1"))
                .expectError(JasyncAccessorException.class)
                .verify();
    }

    @Test
    void integrationServiceReturnedOnSuccessAndError() {
        final Connection connection = Mockito.mock(Connection.class);
        final QueryResult queryResult = Mockito.mock(QueryResult.class);
        Mockito.when(connection.sendQuery("select ok"))
                .thenReturn(CompletableFuture.completedFuture(queryResult));
        Mockito.when(connection.sendQuery("select err"))
                .thenReturn(CompletableFuture.failedFuture(new DatabaseException("x")));
        final AppbaseClientIntegrationService<Connection, TextValue> integrationService =
                mockIntegrationService(connection);
        final ReactiveJasyncAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        accessor.sendQuery("select", "select ok").block();
        accessor.sendQuery("select", "select err").onErrorResume(e -> Mono.empty()).block();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void queryRowsEmitsRowsFromQueryResult() {
        final Connection connection = Mockito.mock(Connection.class);
        final QueryResult queryResult = Mockito.mock(QueryResult.class);
        final RowData row1 = Mockito.mock(RowData.class);
        final RowData row2 = Mockito.mock(RowData.class);
        final ResultSet resultSet = new TestResultSet(List.of(row1, row2));
        Mockito.when(queryResult.getRows()).thenReturn(resultSet);
        final List<Object> params = Collections.emptyList();
        Mockito.when(connection.sendPreparedStatement("select rows", params))
                .thenReturn(CompletableFuture.completedFuture(queryResult));
        final AppbaseClientIntegrationService<Connection, TextValue> integrationService =
                mockIntegrationService(connection);
        final ReactiveJasyncAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.queryRows("select", "select rows", params))
                .expectNext(row1, row2)
                .verifyComplete();
    }

    @Test
    void timerRecordedForSuccessfulOperation() {
        final Connection connection = Mockito.mock(Connection.class);
        final QueryResult queryResult = Mockito.mock(QueryResult.class);
        Mockito.when(connection.sendQuery("select 1"))
                .thenReturn(CompletableFuture.completedFuture(queryResult));
        final AppbaseClientIntegrationService<Connection, TextValue> integrationService =
                mockIntegrationService(connection);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveJasyncAccessor accessor = accessor(core, defaultConfig(), () -> integrationService);

        accessor.sendQuery("select", "select 1").block();

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
