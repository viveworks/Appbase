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

package com.viveworks.appbase.client.service.r2dbc.accessor.reactive;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationException;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientIntegrationService;
import com.viveworks.appbase.client.service.r2dbc.accessor.R2dbcAccessorException;
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
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveR2dbcAccessor}, against a REAL in-memory H2 R2DBC {@link
 * ConnectionPool} (no mocking of the R2DBC SPI itself). Only the {@link
 * R2dbcClientIntegrationService} borrow and the {@link AppbaseCore} spine are mocked, mirroring
 * {@code ReactiveVitessGrpcAccessorTest}'s {@code CoreFixtures} pattern.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveR2dbcAccessorTest {

    private static ConnectionPool pool;

    @BeforeAll
    static void setUpPool() {
        pool = new ConnectionPool(ConnectionPoolConfiguration.builder(
                ConnectionFactories.get("r2dbc:h2:mem:///accessortest;DB_CLOSE_DELAY=-1")).build());
    }

    @AfterAll
    static void tearDownPool() {
        if (pool != null) {
            pool.disposeLater().block();
        }
    }

    /**
     * Bundles a mocked {@link AppbaseCore} with the metric fixtures it was built from, so tests
     * can both drive the accessor and verify the shared spine's interactions (e.g. timer
     * recording). Mirrors {@code ReactiveVitessGrpcAccessorTest.CoreFixtures}.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Timer timer;
        final Timer.Context timerContext;
        final MetricRegistry metricRegistry;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer, final Timer.Context timerContext,
                              final MetricRegistry metricRegistry, final Tracer tracer, final Span span,
                              final Mediator mediator) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.metricRegistry = metricRegistry;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveR2dbcAccessorTest.class));

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

            return new CoreFixtures(appbaseCore, timer, timerContext, metricRegistry, tracer, span, mediator);
        }
    }

    private static R2dbcClientIntegrationService mockIntegrationService() {
        final R2dbcClientIntegrationService integrationService = Mockito.mock(R2dbcClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(pool);
        return integrationService;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveR2dbcAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<R2dbcClientIntegrationService> borrow) {
        return new ReactiveR2dbcAccessor(core.appbaseCore, mockReactorService(), configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    @Test
    void updateAndQueryRoundTripAgainstRealH2Pool() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.update("create", "CREATE TABLE t(id INT, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(accessor.update("insert", "INSERT INTO t VALUES (?, ?)", 1, "a"))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(accessor.query("select", "SELECT name FROM t WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 1))
                .expectNext("a")
                .verifyComplete();
    }

    @Test
    void badSqlQuerySurfacesR2dbcAccessorException() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.query("bad", "SELECT * FROM does_not_exist",
                        (row, rowMetadata) -> row.get(0, Object.class)))
                .expectError(R2dbcAccessorException.class)
                .verify();
    }

    @Test
    void integrationServiceReturnedOnSuccessAndError() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        accessor.query("select-const", "SELECT 1", (row, rowMetadata) -> row.get(0, Integer.class)).blockLast();
        accessor.query("bad", "SELECT * FROM does_not_exist", (row, rowMetadata) -> row.get(0, Object.class))
                .onErrorResume(e -> Flux.empty())
                .blockLast();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void timerRecordedForSuccessfulOperation() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveR2dbcAccessor accessor = accessor(core, defaultConfig(), () -> integrationService);

        accessor.query("select-const", "SELECT 1", (row, rowMetadata) -> row.get(0, Integer.class)).blockLast();

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }

    @Test
    void inTransactionCommits() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.update("create-tx", "CREATE TABLE IF NOT EXISTS tx(id INT, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(accessor.inTransaction("tx-commit",
                        session -> Mono.from(session.update("INSERT INTO tx VALUES (?, ?)", 2, "b"))))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(accessor.query("select-tx", "SELECT name FROM tx WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 2))
                .expectNext("b")
                .verifyComplete();
    }

    @Test
    void inTransactionRollsBackOnError() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.update("create-tx", "CREATE TABLE IF NOT EXISTS tx(id INT, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        // The base reactive spine's default error mapping wraps any non-driver failure (this
        // caller-supplied IllegalStateException is not an R2dbcException) in a
        // ReactiveOperationException, same as every other named op on this accessor -- but the
        // ORIGINAL error is preserved as its cause, and (this test's point) the insert is rolled
        // back regardless of which exception type surfaces.
        StepVerifier.create(accessor.inTransaction("tx-rollback",
                        session -> Mono.from(session.update("INSERT INTO tx VALUES (?, ?)", 3, "c"))
                                .then(Mono.error(new IllegalStateException("boom")))))
                .expectErrorSatisfies(error -> {
                    Assertions.assertInstanceOf(ReactiveOperationException.class, error);
                    Assertions.assertInstanceOf(IllegalStateException.class, error.getCause());
                    Assertions.assertEquals("boom", error.getCause().getMessage());
                })
                .verify();

        StepVerifier.create(accessor.query("select-tx", "SELECT name FROM tx WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 3))
                .verifyComplete();
    }

    @Test
    void inTransactionCommitsWhenWorkEmitsNothing() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.update("create-tx", "CREATE TABLE IF NOT EXISTS tx(id INT, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        // work collapses to an EMPTY completion via .then() after the insert -- this is the
        // "at most one value" case where the emitted count is ZERO. Before the MAJOR fix, the
        // flatMap driving the commit step never ran on an empty source, so commitTransaction()
        // was skipped and the usingWhen close silently rolled the insert back.
        StepVerifier.create(accessor.inTransaction("tx-commit-empty",
                        session -> Mono.from(session.update("INSERT INTO tx VALUES (?, ?)", 9, "i")).then()))
                .verifyComplete();

        StepVerifier.create(accessor.query("select-tx", "SELECT name FROM tx WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 9))
                .expectNext("i")
                .verifyComplete();
    }

    @Test
    void batchInsertsAllRows() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.update("create-batch", "CREATE TABLE IF NOT EXISTS batch_t(id INT, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(accessor.batch("batch", "INSERT INTO batch_t VALUES (?, ?)",
                        List.of(new Object[] {4, "d"}, new Object[] {5, "e"})))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(accessor.query("count-batch", "SELECT COUNT(*) FROM batch_t",
                        (row, rowMetadata) -> row.get(0, Long.class)))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void inTransactionFinalizesIntegrationServiceOnCommitAndRollback() {
        final R2dbcClientIntegrationService integrationService = mockIntegrationService();
        final ReactiveR2dbcAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);

        StepVerifier.create(accessor.update("create-tx", "CREATE TABLE IF NOT EXISTS tx(id INT, name VARCHAR(20))"))
                .expectNextCount(1)
                .verifyComplete();

        // The create-tx setup call above already borrowed/finalized the integration service once;
        // reset the mock's invocation count so this test measures ONLY the two inTransaction calls
        // below (one commit path, one rollback path).
        Mockito.clearInvocations(integrationService);

        accessor.inTransaction("tx-commit-finalize",
                        session -> Mono.from(session.update("INSERT INTO tx VALUES (?, ?)", 20, "x")))
                .block();

        accessor.inTransaction("tx-rollback-finalize",
                        session -> Mono.from(session.update("INSERT INTO tx VALUES (?, ?)", 21, "y"))
                                .then(Mono.error(new IllegalStateException("boom"))))
                .onErrorResume(e -> Mono.empty())
                .block();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }
}
