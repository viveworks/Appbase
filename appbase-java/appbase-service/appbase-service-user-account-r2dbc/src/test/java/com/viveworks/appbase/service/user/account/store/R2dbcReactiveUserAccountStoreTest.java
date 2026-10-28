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
import com.viveworks.appbase.client.service.r2dbc.accessor.R2dbcAccessorException;
import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
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
import com.viveworks.appbase.service.user.account.data.UserAccount;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tests for {@link R2dbcReactiveUserAccountStore} against a REAL in-memory H2 R2DBC {@link
 * ConnectionPool} (no mocking of the R2DBC SPI itself) -- mirrors {@code ReactiveR2dbcAccessorTest}'s
 * {@code CoreFixtures} pattern in {@code appbase-client-r2dbc}. Only the {@link
 * R2dbcClientIntegrationService} borrow and the {@link AppbaseCore} spine are mocked.
 *
 * <p>One row is seeded per test via {@link #setUp()}, with random {@code idpUserId}/{@code
 * appbaseUserId}/{@code email} values so tests sharing the same in-memory table never collide.
 *
 * <p>No reactive operation here is bounded by a tight timeout -- every assertion goes through
 * {@link StepVerifier} (unbounded {@code verify()}/{@code verifyComplete()}) and every fixture
 * {@code .block()} call is unbounded, exactly like the reference {@code ReactiveR2dbcAccessorTest}
 * in {@code appbase-client-r2dbc}. That reference test's own comments record why: under the JaCoCo
 * agent, the FIRST database operation triggers one-time class-loading/bytecode instrumentation of
 * the whole reactor/r2dbc-pool/H2 stack, which can exceed a tight per-op bound even though the
 * operation itself takes milliseconds once warmed up. A genuinely hung reactive chain is still
 * caught by Surefire's fork timeout, same as the reference test.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class R2dbcReactiveUserAccountStoreTest {
    private static final String TABLE = "appbase_UserAccount";

    private static ConnectionPool pool;
    private static ReactiveR2dbcAccessor accessor;
    private static R2dbcReactiveUserAccountStore store;

    private String idpUserId;
    private String appbaseUserId;
    private String email;

    @BeforeAll
    static void setUpClass() {
        pool = new ConnectionPool(ConnectionPoolConfiguration.builder(
                ConnectionFactories.get("r2dbc:h2:mem:///useraccountstoretest;DB_CLOSE_DELAY=-1")).build());

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

        // Schema: H2-dialect translation of appbase_UserAccount from
        // appbase-client-vitess/infra/local/service-schema/001-sharded.sql (TINYINT(1) -> BOOLEAN,
        // TEXT -> VARCHAR, DATETIME -> TIMESTAMP; column names/constraints otherwise unchanged).
        // Unbounded .block(): the first DB op under the JaCoCo agent pays a one-time instrumentation
        // warmup cost (see class Javadoc) that a tight timeout here would fail on.
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

        // CALLS_REAL_METHODS runs PasswordService's default (de)serialize methods for real -- they
        // are pure string-format helpers backed by the interface's own static impls, no stubbing
        // needed -- while leaving the store-agnostic instance methods (randomSalt/hashWithEmail/...)
        // unstubbed, since reads never call them.
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

    @AfterAll
    static void tearDownClass() {
        if (pool != null) {
            pool.disposeLater().block();
        }
    }

    @BeforeEach
    void setUp() {
        idpUserId = "idp-" + UUID.randomUUID();
        appbaseUserId = "appbase-" + UUID.randomUUID();
        email = UUID.randomUUID() + "@b.com";
        insertAccount(idpUserId, appbaseUserId, email);
    }

    // ------------------------------------------------------------------
    // readByEmailReactive
    // ------------------------------------------------------------------

    @Test
    void readByEmailReactive_returnsInsertedAccount() {
        StepVerifier.create(store.readByEmailReactive(email))
                .assertNext(got -> {
                    Assertions.assertEquals(idpUserId, got.getIdpUserId());
                    Assertions.assertEquals(appbaseUserId, got.getAppbaseUserId());
                    Assertions.assertEquals(email, got.getEmail());
                })
                .verifyComplete();
    }

    @Test
    void readByEmailReactive_completesEmptyWhenAbsent() {
        StepVerifier.create(store.readByEmailReactive("missing-" + UUID.randomUUID() + "@none.com"))
                .verifyComplete();
    }

    @Test
    void readByEmailReactive_multipleRows_errorsRatherThanSilentlyReturningOne() {
        // Parity with the blocking JdbiUserAccountStore.readByEmail, which uses JDBI findOne() and
        // throws on >1 row. The reactive twin must fail loud on duplicate rows for a "unique" key too
        // (spec S4, "no capability dropped") -- not silently return the first. Production schemas are
        // not guaranteed to enforce UNIQUE(email), so this uses a dedicated no-constraint table; the
        // shared test table declares UNIQUE(email), which would block seeding the duplicate.
        // Mutation-verify: revert readByEmailReactive's .singleOrEmpty() back to .next() and this test
        // reddens (the read completes with the first row instead of erroring).
        final String dupTable = "appbase_UserAccount_dup";
        accessor.update("schema-dup",
                        "CREATE TABLE " + dupTable + "("
                                + "appbaseUserId VARCHAR(255), idpUserId VARCHAR(255), email VARCHAR(255), "
                                + "name VARCHAR(255), passwordHashes VARCHAR(1024), passwordResetEmailSent BOOLEAN, "
                                + "passwordResetCodeHash VARCHAR(1024), userTopic VARCHAR(255), "
                                + "userHomeDataCenter VARCHAR(255), emailVerified BOOLEAN, languageCode CHAR(10), "
                                + "addressLine1 VARCHAR(4000), addressLine2 VARCHAR(4000), city VARCHAR(255), "
                                + "stateProvince VARCHAR(255), postalCode VARCHAR(32), country VARCHAR(255), "
                                + "latitude VARCHAR(255), longitude VARCHAR(255), deleted BOOLEAN, "
                                + "creationTimestamp TIMESTAMP, lastAccessTimestamp TIMESTAMP)")
                .block();

        final String dupEmail = "dup-" + UUID.randomUUID() + "@b.com";
        insertAccountInto(dupTable, "idp-" + UUID.randomUUID(), "appbase-" + UUID.randomUUID(), dupEmail);
        insertAccountInto(dupTable, "idp-" + UUID.randomUUID(), "appbase-" + UUID.randomUUID(), dupEmail);

        final CoreFixtures dupCore = CoreFixtures.create();
        final R2dbcReactiveUserAccountStore dupStore = new R2dbcReactiveUserAccountStore(
                dupCore.appbaseCore.getLogService(),
                dupCore.appbaseCore.getMetricService(),
                dupCore.appbaseCore.getMediatorService(),
                Mockito.mock(ServiceRegistry.class),
                Mockito.mock(PasswordService.class, Mockito.CALLS_REAL_METHODS),
                accessor,
                R2dbcReactiveUserAccountStoreConfiguration.builder().userAccountTableName(dupTable).build());

        // Unbounded verify(): JaCoCo cold-start warmup (see class Javadoc).
        StepVerifier.create(dupStore.readByEmailReactive(dupEmail))
                .expectError(IndexOutOfBoundsException.class)
                .verify();
    }

    // ------------------------------------------------------------------
    // real-engine proof (H2): the twin runs against a real SQL engine, not a mock
    // ------------------------------------------------------------------

    @Test
    void reactiveRead_againstMissingTable_surfacesRealEngineError_provingH2NotAMock() {
        // A mock or stub could never raise io.r2dbc.spi.R2dbcException for an unknown-table query --
        // only a real SQL engine parses the statement and rejects the missing relation. Driving it
        // through the accessor (the exact reactive read path R2dbcReactiveUserAccountStore uses)
        // additionally proves that path surfaces the driver error to the subscriber, mapped to
        // R2dbcAccessorException with the real R2dbcException preserved as its cause, rather than
        // swallowing it. Together with the happy-path store reads above (real rows inserted and read
        // back over the same H2 ConnectionPool), this is the account twin proven against a real
        // backend. Unbounded verify(): the JaCoCo first-op instrumentation warmup (see class Javadoc)
        // would trip a tight bound; a genuinely hung chain is still caught by Surefire's fork timeout.
        final Flux<Object> readOfMissingTable = accessor.query(
                "reactiveRead_againstMissingTable_provingH2",
                "SELECT appbaseUserId FROM appbase_table_that_does_not_exist",
                (row, meta) -> row.get(0));

        StepVerifier.create(readOfMissingTable)
                .expectErrorMatches(error -> error instanceof R2dbcAccessorException
                        && error.getCause() instanceof R2dbcException)
                .verify();
    }

    // ------------------------------------------------------------------
    // readByAppbaseUserIdReactive
    // ------------------------------------------------------------------

    @Test
    void readByAppbaseUserIdReactive_returnsInsertedAccount() {
        StepVerifier.create(store.readByAppbaseUserIdReactive(appbaseUserId))
                .assertNext(got -> {
                    Assertions.assertEquals(idpUserId, got.getIdpUserId());
                    Assertions.assertEquals(email, got.getEmail());
                })
                .verifyComplete();
    }

    @Test
    void readByAppbaseUserIdReactive_completesEmptyWhenAbsent() {
        StepVerifier.create(store.readByAppbaseUserIdReactive("missing-" + UUID.randomUUID()))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // readByIdpUserIdReactive
    // ------------------------------------------------------------------

    @Test
    void readByIdpUserIdReactive_returnsInsertedAccount() {
        StepVerifier.create(store.readByIdpUserIdReactive(idpUserId))
                .assertNext(got -> {
                    Assertions.assertEquals(appbaseUserId, got.getAppbaseUserId());
                    Assertions.assertEquals(email, got.getEmail());
                })
                .verifyComplete();
    }

    @Test
    void readByIdpUserIdReactive_completesEmptyWhenAbsent() {
        StepVerifier.create(store.readByIdpUserIdReactive("missing-" + UUID.randomUUID()))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // existsByAppbaseUserIdReactive
    // ------------------------------------------------------------------

    @Test
    void existsByAppbaseUserIdReactive_trueWhenPresent() {
        StepVerifier.create(store.existsByAppbaseUserIdReactive(appbaseUserId))
                .assertNext(b -> Assertions.assertTrue(b))
                .verifyComplete();
    }

    @Test
    void existsByAppbaseUserIdReactive_falseWhenAbsent() {
        StepVerifier.create(store.existsByAppbaseUserIdReactive("missing-" + UUID.randomUUID()))
                .assertNext(b -> Assertions.assertFalse(b))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // existsByIdpUserIdReactive
    // ------------------------------------------------------------------

    @Test
    void existsByIdpUserIdReactive_trueWhenPresent() {
        StepVerifier.create(store.existsByIdpUserIdReactive(idpUserId))
                .assertNext(b -> Assertions.assertTrue(b))
                .verifyComplete();
    }

    @Test
    void existsByIdpUserIdReactive_falseWhenAbsent() {
        StepVerifier.create(store.existsByIdpUserIdReactive("missing-" + UUID.randomUUID()))
                .assertNext(b -> Assertions.assertFalse(b))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // existsByEmailReactive
    // ------------------------------------------------------------------

    @Test
    void existsByEmailReactive_trueWhenPresent() {
        StepVerifier.create(store.existsByEmailReactive(email))
                .assertNext(b -> Assertions.assertTrue(b))
                .verifyComplete();
    }

    @Test
    void existsByEmailReactive_falseWhenAbsent() {
        StepVerifier.create(store.existsByEmailReactive("missing-" + UUID.randomUUID() + "@none.com"))
                .assertNext(b -> Assertions.assertFalse(b))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // R2dbcUserAccountRowMapper's null branch: passwordResetCodeHash == null. Every row seeded by
    // insertAccount() (used by all tests above) has a non-null passwordResetCodeHash, so this is
    // the only test exercising the mapper's null-safe ternary.
    // ------------------------------------------------------------------

    @Test
    void readByEmailReactive_nullPasswordResetCodeHash_mapsToNull() {
        final String localEmail = UUID.randomUUID() + "@nullreset.com";
        insertAccountWithNullPasswordResetCodeHash(
                "idp-" + UUID.randomUUID(), "appbase-" + UUID.randomUUID(), localEmail);

        StepVerifier.create(store.readByEmailReactive(localEmail))
                .assertNext(got -> Assertions.assertNull(got.getPasswordResetCodeHash()))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // R2dbcUserAccountRowMapper's null branch: passwordHashes == null. appbase_UserAccount.
    // passwordHashes has no NOT NULL constraint; the row mapper must guard it exactly like the
    // BLOCKING JdbiUserAccountStore does via PasswordHashes.PasswordHashesColumnMapper (str == null
    // -> null, never deserialize a null string) -- otherwise this R2DBC twin regresses the exact
    // null-passwordHashes NPE class fixed elsewhere in this PR, just one layer over (row mapper
    // instead of nullServerPasswordHash).
    // ------------------------------------------------------------------

    @Test
    void readByEmailReactive_nullPasswordHashes_mapsToNull() {
        final String localEmail = UUID.randomUUID() + "@nullhashes.com";
        insertAccountWithNullPasswordHashes(
                "idp-" + UUID.randomUUID(), "appbase-" + UUID.randomUUID(), localEmail);

        StepVerifier.create(store.readByEmailReactive(localEmail))
                .assertNext(got -> Assertions.assertNull(got.getPasswordHashes()))
                .verifyComplete();
    }

    // ------------------------------------------------------------------
    // Argument validation: AppbaseUtil.notBlank fires for null/blank arguments. One null and one
    // blank case is enough to prove the guard on the shared validation helper -- every read/exists
    // method calls it as its first statement, identically. These throw synchronously (before any
    // Mono is even constructed), so no reactive subscription/timeout is involved.
    // ------------------------------------------------------------------

    @Test
    void readByEmailReactive_rejectsNullEmail() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> store.readByEmailReactive(null));
    }

    @Test
    void existsByAppbaseUserIdReactive_rejectsBlankAppbaseUserId() {
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> store.existsByAppbaseUserIdReactive(" "));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private static void insertAccount(final String idpUserId, final String appbaseUserId, final String email) {
        insertAccountInto(TABLE, idpUserId, appbaseUserId, email);
    }

    private static void insertAccountInto(
            final String tableName, final String idpUserId, final String appbaseUserId, final String email) {
        final String serializedPasswordHashes = PasswordService.serializePasswordHashesImpl(passwordHashes());
        final String serializedPasswordResetCodeHash = PasswordService.serializePasswordHashImpl(passwordHash("reset"));
        final LocalDateTime now = LocalDateTime.now();

        // Unbounded .block(): see class Javadoc (JaCoCo cold-start warmup).
        accessor.update("insert-test-account",
                        "INSERT INTO " + tableName + "(" +
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

    /**
     * Inserts a row with {@code passwordResetCodeHash} SQL {@code NULL} -- {@code
     * ReactiveR2dbcAccessor.update(...)}'s positional binds are non-null only, so a typed null
     * bind must drop to {@code ReactiveR2dbcAccessor.command(...)} and bind the {@link Statement}
     * directly, per its Javadoc.
     */
    private static void insertAccountWithNullPasswordResetCodeHash(
            final String idpUserId, final String appbaseUserId, final String email) {
        final String serializedPasswordHashes = PasswordService.serializePasswordHashesImpl(passwordHashes());
        final LocalDateTime now = LocalDateTime.now();

        // Unbounded .blockLast(): see class Javadoc (JaCoCo cold-start warmup).
        accessor.command("insert-null-reset-hash", conn -> {
            final Statement statement = conn.createStatement(
                    "INSERT INTO " + TABLE + "(" +
                            "appbaseUserId, idpUserId, email, name, passwordHashes, passwordResetEmailSent, " +
                            "passwordResetCodeHash, userTopic, userHomeDataCenter, emailVerified, languageCode, " +
                            "addressLine1, addressLine2, city, stateProvince, postalCode, country, latitude, " +
                            "longitude, deleted, creationTimestamp, lastAccessTimestamp) VALUES " +
                            "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.bind(0, appbaseUserId);
            statement.bind(1, idpUserId);
            statement.bind(2, email);
            statement.bind(3, "name123");
            statement.bind(4, serializedPasswordHashes);
            statement.bind(5, false);
            statement.bindNull(6, String.class);
            statement.bind(7, "userTopic123");
            statement.bind(8, "us-east-1");
            statement.bind(9, true);
            statement.bind(10, "EN_CA");
            statement.bind(11, "123 Abcd Street");
            statement.bind(12, "Line 1 of address");
            statement.bind(13, "My city");
            statement.bind(14, "My state");
            statement.bind(15, "1A2 3B6");
            statement.bind(16, "My country");
            statement.bind(17, "1.23");
            statement.bind(18, "12.234");
            statement.bind(19, false);
            statement.bind(20, now);
            statement.bind(21, now);
            return Flux.from(statement.execute()).flatMap(Result::getRowsUpdated);
        }).blockLast();
    }

    /**
     * Inserts a row with {@code passwordHashes} SQL {@code NULL} -- same {@code
     * ReactiveR2dbcAccessor.command()}/{@code Statement.bindNull()} escape hatch as {@link
     * #insertAccountWithNullPasswordResetCodeHash}, just on the adjacent column (index 4 instead
     * of 6). {@code passwordResetCodeHash} is still bound to a real value here so this row exercises
     * ONLY the {@code passwordHashes} null branch, not both at once.
     */
    private static void insertAccountWithNullPasswordHashes(
            final String idpUserId, final String appbaseUserId, final String email) {
        final String serializedPasswordResetCodeHash = PasswordService.serializePasswordHashImpl(passwordHash("reset"));
        final LocalDateTime now = LocalDateTime.now();

        // Unbounded .blockLast(): see class Javadoc (JaCoCo cold-start warmup).
        accessor.command("insert-null-password-hashes", conn -> {
            final Statement statement = conn.createStatement(
                    "INSERT INTO " + TABLE + "(" +
                            "appbaseUserId, idpUserId, email, name, passwordHashes, passwordResetEmailSent, " +
                            "passwordResetCodeHash, userTopic, userHomeDataCenter, emailVerified, languageCode, " +
                            "addressLine1, addressLine2, city, stateProvince, postalCode, country, latitude, " +
                            "longitude, deleted, creationTimestamp, lastAccessTimestamp) VALUES " +
                            "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.bind(0, appbaseUserId);
            statement.bind(1, idpUserId);
            statement.bind(2, email);
            statement.bind(3, "name123");
            statement.bindNull(4, String.class);
            statement.bind(5, false);
            statement.bind(6, serializedPasswordResetCodeHash);
            statement.bind(7, "userTopic123");
            statement.bind(8, "us-east-1");
            statement.bind(9, true);
            statement.bind(10, "EN_CA");
            statement.bind(11, "123 Abcd Street");
            statement.bind(12, "Line 1 of address");
            statement.bind(13, "My city");
            statement.bind(14, "My state");
            statement.bind(15, "1A2 3B6");
            statement.bind(16, "My country");
            statement.bind(17, "1.23");
            statement.bind(18, "12.234");
            statement.bind(19, false);
            statement.bind(20, now);
            statement.bind(21, now);
            return Flux.from(statement.execute()).flatMap(Result::getRowsUpdated);
        }).blockLast();
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

    /**
     * Bundles a mocked {@link AppbaseCore} with the sub-service mocks it was built from, so both
     * the {@link ReactiveR2dbcAccessor} and the {@link R2dbcReactiveUserAccountStore} (a stateless
     * Appbase service) can be built off the same spine. Mirrors {@code
     * ReactiveR2dbcAccessorTest.CoreFixtures} in {@code appbase-client-r2dbc}.
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
