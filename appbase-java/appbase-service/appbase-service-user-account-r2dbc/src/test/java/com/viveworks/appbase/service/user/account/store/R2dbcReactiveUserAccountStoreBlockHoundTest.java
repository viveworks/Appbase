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

package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientIntegrationService;
import com.viveworks.appbase.client.service.r2dbc.accessor.R2dbcAccessorException;
import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.R2dbcTimeoutException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BlockHound guard for the R2DBC account twin's reactive read chains (internal work item 1113, P-027 R4 N05).
 *
 * <p>The arm: {@link BlockHound#install()} in {@link #armBlockHound()} instruments the JDK so any
 * blocking call made on one of Reactor's non-blocking worker threads throws
 * {@code BlockingOperationError}. Each read below is subscribed on {@link Schedulers#parallel()} (a
 * non-blocking scheduler BlockHound guards); a clean completion proves the read never blocked that
 * worker. Mutation-verify by inserting a {@code Thread.sleep(..)} into
 * {@code R2dbcReactiveUserAccountStore.readByEmailReactive} — this test then goes red with a
 * {@code BlockingOperationError}.
 *
 * <p>Runs against the same embedded H2 R2DBC engine as {@link R2dbcReactiveUserAccountStoreTest}
 * (no external infra), so it executes unconditionally offline and in CI. It carries its own
 * {@code CoreFixtures}, matching the per-test convention across the reactive accessor test suites.
 *
 * @author Navaneeth Mysore G
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class R2dbcReactiveUserAccountStoreBlockHoundTest {
    private static final String DATABASE_URL =
            "r2dbc:h2:mem:///useraccountblockhoundtest;DB_CLOSE_DELAY=-1";
    private static final String TABLE = "appbase_UserAccount";
    private static final Duration DATABASE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CONTROL_CLEANUP_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CONTROL_ACQUIRE_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(10);

    private static ConnectionPool pool;
    private static R2dbcReactiveUserAccountStore store;

    @BeforeAll
    static void armBlockHound() {
        // Install once for the JVM; idempotent. Must precede the reads it guards.
        BlockHound.install();

        final ConnectionPool setupPool = createPool(DATABASE_TIMEOUT);
        try {
            initializeSchema(setupPool, TABLE);
        } finally {
            setupPool.disposeLater().block(DATABASE_TIMEOUT);
        }

        pool = createPool(CONTROL_ACQUIRE_TIMEOUT);
        store = createStore(pool, TABLE);
    }

    private static ConnectionPool createPool(final Duration acquireTimeout) {
        return new ConnectionPool(ConnectionPoolConfiguration.builder(ConnectionFactories.get(DATABASE_URL))
                .initialSize(0).maxSize(1).maxAcquireTime(acquireTimeout).build());
    }

    private static ReactiveR2dbcAccessor createAccessor(
            final ConnectionPool connectionPool, final CoreFixtures core)
    {
        final R2dbcClientIntegrationService integrationService = Mockito.mock(R2dbcClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(connectionPool);

        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);

        return new ReactiveR2dbcAccessor(
                core.appbaseCore,
                reactorService,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(),
                () -> integrationService);
    }

    private static void initializeSchema(final ConnectionPool setupPool, final String table) {
        // The first DB op under the JaCoCo agent pays a one-time instrumentation warmup, hence this
        // setup uses a separate generously bounded pool. The one-second control pool remains tight
        // and is created only after initialization so bootstrap load cannot consume its RED margin.
        createAccessor(setupPool, CoreFixtures.create()).update("schema",
                "CREATE TABLE " + table + "(" +
                        "appbaseUserId VARCHAR(255), idpUserId VARCHAR(255), email VARCHAR(255), " +
                        "name VARCHAR(255), passwordHashes VARCHAR(1024), passwordResetEmailSent BOOLEAN, " +
                        "passwordResetCodeHash VARCHAR(1024), userTopic VARCHAR(255), " +
                        "userHomeDataCenter VARCHAR(255), emailVerified BOOLEAN, languageCode CHAR(10), " +
                        "addressLine1 VARCHAR(4000), addressLine2 VARCHAR(4000), city VARCHAR(255), " +
                        "stateProvince VARCHAR(255), postalCode VARCHAR(32), country VARCHAR(255), " +
                        "latitude VARCHAR(255), longitude VARCHAR(255), deleted BOOLEAN, " +
                        "creationTimestamp TIMESTAMP, lastAccessTimestamp TIMESTAMP, " +
                        "CONSTRAINT UserAccount_idpUserId_pk PRIMARY KEY(idpUserId), " +
                        "CONSTRAINT UserAccount_appbaseUserId_idx UNIQUE(appbaseUserId), " +
                        "CONSTRAINT UserAccount_email_idx UNIQUE(email))")
                .block(DATABASE_TIMEOUT);
    }

    private static R2dbcReactiveUserAccountStore createStore(
            final ConnectionPool connectionPool, final String table) {
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveR2dbcAccessor accessor = createAccessor(connectionPool, core);

        final PasswordService passwordService = Mockito.mock(PasswordService.class, Mockito.CALLS_REAL_METHODS);

        return new R2dbcReactiveUserAccountStore(
                core.appbaseCore.getLogService(),
                core.appbaseCore.getMetricService(),
                core.appbaseCore.getMediatorService(),
                Mockito.mock(ServiceRegistry.class),
                passwordService,
                accessor,
                R2dbcReactiveUserAccountStoreConfiguration.builder().userAccountTableName(table).build());
    }

    @AfterAll
    static void tearDown() {
        if (pool != null) {
            pool.disposeLater().block(DATABASE_TIMEOUT);
        }
    }

    @Test
    @Order(1)
    void readByEmailReactive_neverBlocksANonBlockingThread() {
        assertReadIsNonBlocking(store.readByEmailReactive("absent-" + UUID.randomUUID() + "@none.com"));
    }

    @Test
    @Order(2)
    void readByAppbaseUserIdReactive_neverBlocksANonBlockingThread() {
        assertReadIsNonBlocking(store.readByAppbaseUserIdReactive("absent-" + UUID.randomUUID()));
    }

    @Test
    @Order(3)
    void readByIdpUserIdReactive_neverBlocksANonBlockingThread() {
        assertReadIsNonBlocking(store.readByIdpUserIdReactive("absent-" + UUID.randomUUID()));
    }

    @Test
    @Order(4)
    void test_AC2521_1_neverCompletingReadFailsFast() {
        Assertions.assertThrows(
                AssertionError.class,
                () -> assertReadIsNonBlocking(Mono.never(), Duration.ofMillis(100)));
    }

    @Test
    @Order(5)
    void test_EXTRA_exhaustedRealPoolTimesOutWithDiagnosticsAndRecovers() {
        final Connection held = pool.create().block(DATABASE_TIMEOUT);
        Assertions.assertNotNull(held);
        try {
            final AtomicReference<Throwable> emittedError = new AtomicReference<>();
            final AssertionError failure = Assertions.assertThrows(AssertionError.class,
                    () -> assertReadIsNonBlocking(store.readByEmailReactive("pool-control@none.com")
                            .doOnError(emittedError::set)));
            final Throwable acquisitionFailure = emittedError.get();
            Assertions.assertInstanceOf(R2dbcAccessorException.class, acquisitionFailure,
                    "Expected the accessor to map the pool acquisition timeout");
            Assertions.assertInstanceOf(R2dbcTimeoutException.class, acquisitionFailure.getCause(),
                    "Expected the accessor failure to directly wrap the R2DBC acquisition timeout");
            Assertions.assertTrue(failure.getMessage().contains("pool acquired=1"));
            Assertions.assertTrue(failure.getMessage().contains("Thread snapshot after termination"));
        } finally {
            Mono.from(held.close()).block(CONTROL_CLEANUP_TIMEOUT);
        }
        // Native pool timeout ends the queued acquisition without cancellation. Once exhaustion
        // clears, the same store and shared pool must complete another real read.
        assertReadIsNonBlocking(store.readByEmailReactive("pool-recovery@none.com"));
    }

    /**
     * Subscribes the read on a BlockHound-guarded {@link Schedulers#parallel()} worker and asserts it
     * completes empty without a {@code BlockingOperationError}. The verification timeout makes a
     * stalled R2DBC pool acquisition fail at the named test rather than at the CI job timeout.
     */
    private static void assertReadIsNonBlocking(final Mono<UserAccount> read) {
        assertReadIsNonBlocking(read, VERIFY_TIMEOUT);
    }

    private static void assertReadIsNonBlocking(final Mono<UserAccount> read, final Duration timeout) {
        assertReadIsNonBlocking(read, timeout, pool);
    }

    private static void assertReadIsNonBlocking(
            final Mono<UserAccount> read, final Duration timeout, final ConnectionPool diagnosticPool) {
        try {
            StepVerifier.create(read.subscribeOn(Schedulers.parallel()))
                    .expectComplete()
                    .verify(timeout);
        } catch (final AssertionError failure) {
            // This is a post-termination snapshot, not proof that a worker was blocked in any
            // particular frame before the verifier terminated or cancelled its subscription.
            final StringBuilder diagnostic = new StringBuilder("R2DBC guarded read failed: ")
                    .append(failure.getMessage()).append('\n');
            diagnosticPool.getMetrics().ifPresent(metrics -> diagnostic.append("pool acquired=")
                    .append(metrics.acquiredSize()).append(" pending=")
                    .append(metrics.pendingAcquireSize()).append('\n'));
            diagnostic.append("Thread snapshot after termination:\n");
            for (final ThreadInfo thread : ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)) {
                diagnostic.append(thread.getThreadName()).append(" ").append(thread.getThreadState()).append('\n');
                for (final StackTraceElement frame : thread.getStackTrace()) {
                    diagnostic.append("    at ").append(frame).append('\n');
                }
            }
            throw new AssertionError(diagnostic.toString(), failure);
        }
    }

    /**
     * Minimal {@link AppbaseCore} test double for the accessor's logging / metric / tracing / mediator
     * touchpoints. Mirrors the {@code CoreFixtures} pattern used across the reactive accessor tests.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;

        private CoreFixtures(final AppbaseCore appbaseCore) {
            this.appbaseCore = appbaseCore;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenAnswer(invocation -> LoggerFactory.getLogger((Class<?>) invocation.getArgument(0)));

            final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
            final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
            final Timer timer = Mockito.mock(Timer.class);
            final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
            final Counter counter = Mockito.mock(Counter.class);
            Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
            Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
            Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
            Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(counter);
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

            return new CoreFixtures(appbaseCore);
        }
    }
}
