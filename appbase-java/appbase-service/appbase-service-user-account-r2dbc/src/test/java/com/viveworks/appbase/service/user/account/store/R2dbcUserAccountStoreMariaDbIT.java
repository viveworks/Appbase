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

package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientIntegrationService;
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
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.password.PasswordService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Real-DB integration proof for the R2DBC user-account twin against MariaDB.
 *
 * <p>Gated OFF offline: the MariaDB image the infra scripts name was deleted from Docker Hub and
 * cannot be started in this environment (same wall recorded for the sibling {@code
 * ReactiveR2dbcAccessorMariaDbIT} in the J-018/P-051 gated-IT registry, clause (n); this class is
 * registered as clause (p). P-051 is specified in {@code
 * docs/spec/1.0.0/P04/01-P04-E01-integration-tests.md}; the clause letters come from the Release 1
 * brainstorm, removed by internal work item 1421 and readable in git history at {@code develop} 18d8ca2fe). The
 * offline proof of the twin against a real
 * engine is {@link R2dbcReactiveUserAccountStoreTest} against embedded H2; this class is additive
 * assurance against the production wire driver, to be run once a real MariaDB is reachable.
 *
 * <p>Enable with {@code APPBASE_MARIADB_IT=1} when a real MariaDB is reachable. Connection settings
 * come from the environment, with the same defaults as {@code appbase-client-r2dbc-mariadb.yaml}:
 * {@code APPBASE_MARIADB_HOST} (localhost), {@code APPBASE_MARIADB_PORT} (3306), {@code
 * APPBASE_MARIADB_DATABASE} (appbase-test-mariadb-db), {@code APPBASE_MARIADB_USER} (root) and
 * {@code APPBASE_MARIADB_PASSWORD} (password). The test uses its own per-run table, never the
 * production {@code appbase_UserAccount} name, and drops it afterwards.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named = "APPBASE_MARIADB_IT", matches = "1")
class R2dbcUserAccountStoreMariaDbIT {

    /** Per-run name so a repeated or concurrent run cannot collide with a leftover table. */
    private static final String TABLE = "appbase_UserAccount_it_" + System.nanoTime();

    private static ConnectionPool pool;
    private static ReactiveR2dbcAccessor accessor;
    private static R2dbcReactiveUserAccountStore store;

    @BeforeAll
    static void setUp() {
        pool = new ConnectionPool(ConnectionPoolConfiguration.builder(
                ConnectionFactories.get(mariaDbOptions())).maxSize(2).build());

        final CoreFixtures core = CoreFixtures.create();
        final R2dbcClientIntegrationService integrationService = Mockito.mock(R2dbcClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(pool);

        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);

        accessor = new ReactiveR2dbcAccessor(
                core.appbaseCore,
                reactorService,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(),
                () -> integrationService);

        // Schema: MariaDB translation of appbase_UserAccount from
        // appbase-client-vitess/infra/local/service-schema/001-sharded.sql (same columns and
        // constraints as the H2 test). Dropped first so a leftover from a crashed run cannot skew it.
        accessor.update("schema-drop", "DROP TABLE IF EXISTS " + TABLE).block();
        accessor.update("schema",
                "CREATE TABLE " + TABLE + "(" +
                        "appbaseUserId VARCHAR(255), " +
                        "idpUserId VARCHAR(255), " +
                        "email VARCHAR(255), " +
                        "name VARCHAR(255), " +
                        "passwordHashes VARCHAR(1024), " +
                        "passwordResetEmailSent BOOLEAN, " +
                        "passwordResetCodeHash VARCHAR(1024), " +
                        "userTopic VARCHAR(255), " +
                        "userHomeDataCenter VARCHAR(255), " +
                        "emailVerified BOOLEAN, " +
                        "languageCode CHAR(10), " +
                        "addressLine1 VARCHAR(4000), " +
                        "addressLine2 VARCHAR(4000), " +
                        "city VARCHAR(255), " +
                        "stateProvince VARCHAR(255), " +
                        "postalCode VARCHAR(32), " +
                        "country VARCHAR(255), " +
                        "latitude VARCHAR(255), " +
                        "longitude VARCHAR(255), " +
                        "deleted BOOLEAN, " +
                        "creationTimestamp TIMESTAMP, " +
                        "lastAccessTimestamp TIMESTAMP, " +
                        "CONSTRAINT UserAccount_idpUserId_pk PRIMARY KEY(idpUserId), " +
                        "CONSTRAINT UserAccount_appbaseUserId_idx UNIQUE(appbaseUserId), " +
                        "CONSTRAINT UserAccount_email_idx UNIQUE(email))")
                .block();

        // CALLS_REAL_METHODS: the serialize/deserialize helpers run for real, as in the H2 test.
        final PasswordService passwordService = Mockito.mock(PasswordService.class, Mockito.CALLS_REAL_METHODS);

        store = new R2dbcReactiveUserAccountStore(
                core.appbaseCore.getLogService(),
                core.appbaseCore.getMetricService(),
                core.appbaseCore.getMediatorService(),
                Mockito.mock(ServiceRegistry.class),
                passwordService,
                accessor,
                R2dbcReactiveUserAccountStoreConfiguration.builder().userAccountTableName(TABLE).build());
    }

    /** Best-effort cleanup: a failure here must not mask the real test failure. */
    @AfterAll
    static void tearDown() {
        if (pool != null) {
            try {
                if (accessor != null) {
                    accessor.update("drop-cleanup", "DROP TABLE IF EXISTS " + TABLE)
                            .onErrorResume(error -> Mono.empty())
                            .block();
                }
            } finally {
                pool.disposeLater().block();
            }
        }
    }

    @Test
    void readByEmail_againstRealMariaDb_returnsPersistedAccount() {
        final String idpUserId = "idp-" + UUID.randomUUID();
        final String appbaseUserId = "appbase-" + UUID.randomUUID();
        final String email = UUID.randomUUID() + "@b.com";
        insertAccount(idpUserId, appbaseUserId, email);

        StepVerifier.create(store.readByEmailReactive(email))
                .assertNext(got -> {
                    Assertions.assertEquals(idpUserId, got.getIdpUserId());
                    Assertions.assertEquals(appbaseUserId, got.getAppbaseUserId());
                    Assertions.assertEquals(email, got.getEmail());
                    Assertions.assertEquals("name123", got.getName());
                })
                .verifyComplete();

        StepVerifier.create(store.readByEmailReactive("missing-" + UUID.randomUUID() + "@none.com"))
                .verifyComplete();
    }

    private static void insertAccount(final String idpUserId, final String appbaseUserId, final String email) {
        final String serializedPasswordHashes = PasswordService.serializePasswordHashesImpl(passwordHashes());
        final String serializedPasswordResetCodeHash = PasswordService.serializePasswordHashImpl(passwordHash("reset"));
        final LocalDateTime now = LocalDateTime.now();

        accessor.update("insert-test-account",
                        "INSERT INTO " + TABLE + "(" +
                                "appbaseUserId, idpUserId, email, name, passwordHashes, passwordResetEmailSent, " +
                                "passwordResetCodeHash, userTopic, userHomeDataCenter, emailVerified, languageCode, " +
                                "addressLine1, addressLine2, city, stateProvince, postalCode, country, latitude, " +
                                "longitude, deleted, creationTimestamp, lastAccessTimestamp) VALUES " +
                                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        appbaseUserId,
                        idpUserId,
                        email,
                        "name123",
                        serializedPasswordHashes,
                        false,
                        serializedPasswordResetCodeHash,
                        "userTopic123",
                        "us-east-1",
                        true,
                        "EN_CA",
                        "123 Abcd Street",
                        "Line 1 of address",
                        "My city",
                        "My state",
                        "1A2 3B6",
                        "My country",
                        "1.23",
                        "12.234",
                        false,
                        now,
                        now)
                .block();
    }

    private static ConnectionFactoryOptions mariaDbOptions() {
        return ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "mariadb")
                .option(ConnectionFactoryOptions.HOST, env("APPBASE_MARIADB_HOST", "localhost"))
                .option(ConnectionFactoryOptions.PORT, Integer.parseInt(env("APPBASE_MARIADB_PORT", "3306")))
                .option(ConnectionFactoryOptions.DATABASE, env("APPBASE_MARIADB_DATABASE", "appbase-test-mariadb-db"))
                .option(ConnectionFactoryOptions.USER, env("APPBASE_MARIADB_USER", "root"))
                .option(ConnectionFactoryOptions.PASSWORD, env("APPBASE_MARIADB_PASSWORD", "password"))
                .build();
    }

    private static String env(final String name, final String defaultValue) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static PasswordHashes passwordHashes() {
        return PasswordHashes
                .builder()
                .clientPasswordHash(passwordHash("client"))
                .serverPasswordHash(passwordHash("server"))
                .build();
    }

    private static PasswordHash passwordHash(final String tag) {
        return PasswordHash
                .builder()
                .hash(tag + "PasswordHash-123")
                .hashAlgorithm(HashAlgorithm.SHA512)
                .shaRounds(80)
                .salt("salt123")
                .build();
    }

    /** Mirrors the H2 test's fixtures: only the AppbaseCore spine is mocked, the store runs for real. */
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
