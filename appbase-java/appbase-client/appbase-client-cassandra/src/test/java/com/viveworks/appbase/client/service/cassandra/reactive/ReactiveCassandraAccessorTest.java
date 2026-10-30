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

package com.viveworks.appbase.client.service.cassandra.reactive;

import com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveRow;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveCassandraAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveCassandraAccessorTest {

    /**
     * Thin test-only {@link ReactiveResultSet} adapter delegating {@code subscribe} to a
     * {@link Flux}, so driver behavior (rows/errors) can be driven deterministically without a
     * real Cassandra cluster.
     */
    private static final class FluxReactiveResultSet implements ReactiveResultSet {

        private final Flux<ReactiveRow> rows;

        FluxReactiveResultSet(final Flux<ReactiveRow> rows) {
            this.rows = rows;
        }

        @Override
        public void subscribe(final Subscriber<? super ReactiveRow> subscriber) {
            rows.subscribe(subscriber);
        }

        @Override
        public Publisher<? extends ColumnDefinitions> getColumnDefinitions() {
            return Flux.empty();
        }

        @Override
        public Publisher<? extends ExecutionInfo> getExecutionInfos() {
            return Flux.empty();
        }

        @Override
        public Publisher<Boolean> wasApplied() {
            return Flux.just(true);
        }
    }

    /**
     * Bundles a mocked {@link AppbaseCore} with the tracer/span/mediator mocks it was built
     * from, so tests can both drive the accessor and verify the shared spine's interactions.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;

        private CoreFixtures(final AppbaseCore appbaseCore, final Tracer tracer,
                              final Span span, final Mediator mediator) {
            this.appbaseCore = appbaseCore;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveCassandraAccessorTest.class));

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

            return new CoreFixtures(appbaseCore, tracer, span, mediator);
        }
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<CqlSession, ClusterIdValue> mockIntegrationService(
            final CqlSession session) {
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(session);
        return integrationService;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveCassandraAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrow) {
        return new ReactiveCassandraAccessor(core.appbaseCore, mockReactorService(), configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    @Test
    void executeReactiveEmitsRows() {
        final ReactiveRow row = Mockito.mock(ReactiveRow.class);
        final CqlSession session = Mockito.mock(CqlSession.class);
        final Statement<?> statement = SimpleStatement.newInstance("select 1");
        Mockito.when(session.executeReactive(statement))
                .thenReturn(new FluxReactiveResultSet(Flux.just(row)));
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        final ReactiveCassandraAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.executeReactive("select", statement))
                .expectNext(row)
                .verifyComplete();
    }

    @Test
    void integrationServiceReturnedOnSuccessAndError() {
        final ReactiveRow row = Mockito.mock(ReactiveRow.class);
        final CqlSession session = Mockito.mock(CqlSession.class);
        final Statement<?> statement = SimpleStatement.newInstance("select 1");
        final Statement<?> errorStatement = SimpleStatement.newInstance("select 2");
        Mockito.when(session.executeReactive(statement))
                .thenReturn(new FluxReactiveResultSet(Flux.just(row)));
        Mockito.when(session.executeReactive(errorStatement))
                .thenReturn(new FluxReactiveResultSet(Flux.error(new IllegalStateException("x"))));
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        final ReactiveCassandraAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        accessor.executeReactive("select", statement).blockLast();
        accessor.executeReactive("select", errorStatement).onErrorResume(e -> Flux.empty()).blockLast();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void driverExceptionMappedToCassandraAccessorException() {
        final CqlSession session = Mockito.mock(CqlSession.class);
        final Statement<?> statement = SimpleStatement.newInstance("select 1");
        Mockito.when(session.executeReactive(statement))
                .thenReturn(new FluxReactiveResultSet(Flux.error(new DriverTimeoutException("timeout"))));
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        final ReactiveCassandraAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.executeReactive("select", statement))
                .expectError(CassandraAccessorException.class)
                .verify();
    }

    @Test
    void borrowIsLazyPerSubscription() {
        final CqlSession session = Mockito.mock(CqlSession.class);
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        @SuppressWarnings("unchecked")
        final Supplier<AppbaseClientIntegrationService<CqlSession, ClusterIdValue>> borrowSupplier =
                Mockito.mock(Supplier.class);
        Mockito.when(borrowSupplier.get()).thenReturn(integrationService);
        final ReactiveCassandraAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), borrowSupplier);
        final Statement<?> statement = SimpleStatement.newInstance("select 1");

        accessor.executeReactive("select", statement);

        Mockito.verifyNoInteractions(borrowSupplier);
    }

    @Test
    void traceSpanStartedPerOpWhenTraceEnabled() {
        final ReactiveRow row = Mockito.mock(ReactiveRow.class);
        final CqlSession session = Mockito.mock(CqlSession.class);
        final Statement<?> statement = SimpleStatement.newInstance("select 1");
        Mockito.when(session.executeReactive(statement))
                .thenReturn(new FluxReactiveResultSet(Flux.just(row)));
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveCassandraAccessor accessor = accessor(core,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).traceEnabled(true).build(),
                () -> integrationService);

        accessor.executeReactive("select", statement).blockLast();

        Mockito.verify(core.tracer).buildSpan(Mockito.anyString());
        Mockito.verify(core.span).finish();
    }

    @Test
    void notableFailureReportedToExceptionStoreWhenEnabled() throws MediatorException {
        final CqlSession session = Mockito.mock(CqlSession.class);
        final Statement<?> errorStatement = SimpleStatement.newInstance("select err");
        Mockito.when(session.executeReactive(errorStatement))
                .thenReturn(new FluxReactiveResultSet(Flux.error(new IllegalStateException("x"))));
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveCassandraAccessor accessor = accessor(core,
                DefaultReactiveOperationConfiguration.builder()
                        .retryEnabled(false)
                        .exceptionStoreEnabled(true)
                        .build(),
                () -> integrationService);

        accessor.executeReactive("select", errorStatement).onErrorResume(e -> Flux.empty()).blockLast();

        Mockito.verify(core.mediator).asyncHandleMessage(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void exceptionStoreNotTouchedWhenDisabled() {
        final CqlSession session = Mockito.mock(CqlSession.class);
        final Statement<?> errorStatement = SimpleStatement.newInstance("select err");
        Mockito.when(session.executeReactive(errorStatement))
                .thenReturn(new FluxReactiveResultSet(Flux.error(new IllegalStateException("x"))));
        final AppbaseClientIntegrationService<CqlSession, ClusterIdValue> integrationService =
                mockIntegrationService(session);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveCassandraAccessor accessor = accessor(core, defaultConfig(), () -> integrationService);

        accessor.executeReactive("select", errorStatement).onErrorResume(e -> Flux.empty()).blockLast();

        Mockito.verifyNoInteractions(core.mediator);
    }
}
