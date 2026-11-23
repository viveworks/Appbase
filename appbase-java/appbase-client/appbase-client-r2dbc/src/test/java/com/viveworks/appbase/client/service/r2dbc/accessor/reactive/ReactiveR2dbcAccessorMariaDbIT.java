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

import com.viveworks.appbase.client.core.reactive.ReactiveOperationException;
import com.viveworks.appbase.client.service.r2dbc.DefaultR2dbcClientService;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientServiceTestHelper;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import io.r2dbc.spi.R2dbcBadGrammarException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

/**
 * Reactive round-trip over a real MariaDB (or Vitess {@code vtgate}) via the {@code r2dbc-mariadb}
 * driver, driven through a fully bootstrapped {@link DefaultR2dbcClientService}. The SQL twin of
 * {@link ReactiveR2dbcAccessorH2Test}, against a server-backed database rather than in-memory H2.
 *
 * <p>GATED, and needs external infra. {@link EnabledIfSystemProperty} turns it off unless {@code
 * R2dbcClientServiceTests.enabled=true} (defaulted to {@code false} in {@code
 * appbase-client/pom.xml}). The module binds {@code maven-failsafe-plugin} in the develop, staging
 * and release profiles, so the {@code *IT} suffix is selected by the integration profile; {@code
 * -Dtest=} still selects it directly. The gate must be enabled either way:
 *
 * <pre>{@code
 * mvn test -pl appbase-client/appbase-client-r2dbc \
 *     -DR2dbcClientServiceTests.enabled=true \
 *     -Dtest=ReactiveR2dbcAccessorMariaDbIT
 * }</pre>
 *
 * <p>Vitess speaks the MySQL wire protocol, so this same IT covers the umbrella's reactive-SQL-over-
 * Vitess case: point {@code port} at {@code vtgate} (15306) instead of MariaDB (3306) in {@code
 * appbase-client-r2dbc-mariadb.yaml}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "R2dbcClientServiceTests.enabled", matches = "true")
class ReactiveR2dbcAccessorMariaDbIT {

    private static final String MARIADB_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-r2dbc-mariadb.yaml";

    private static final String MARIADB_BAD_PASSWORD_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-r2dbc-mariadb-badpassword.yaml";

    /** Version pinned by the shared MariaDB integration lifecycle (mariadb:10.11). */
    private static final String PINNED_VERSION_PREFIX = "10.11.";

    /** MySQL/MariaDB ER_NO_SUCH_TABLE (1146) carries SQLState 42S02. */
    private static final String NO_SUCH_TABLE_SQL_STATE = "42S02";

    static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(10);

    /** Per-run name, so a repeated run cannot collide with a table a previous run left behind. */
    private static final String TABLE = uniqueTableName();

    private static DefaultR2dbcClientService defaultR2dbcClientService;

    @BeforeAll
    static void setUp() {
        defaultR2dbcClientService = R2dbcClientServiceTestHelper.defaultR2dbcClientService(
                AppbaseTestInfraProfile.resolve(MARIADB_CONFIG_CLASSPATH_FILE));
    }

    /**
     * Drop any table left by a failed round trip and always attempt service shutdown. Cleanup is
     * bounded and failures remain observable; a shutdown failure is retained as suppressed when a
     * DROP failure is already primary.
     */
    @AfterAll
    static void tearDown() {
        cleanup(defaultR2dbcClientService, TABLE);
    }

    static void cleanup(final DefaultR2dbcClientService service, final String table) {
        if (service != null) {
            try (ShutdownOnClose ignored = new ShutdownOnClose(service)) {
                bounded(service.getReactiveAccessor().update("drop-cleanup", cleanupSql(table)))
                        .blockLast();
            }
        }
    }

    static String uniqueTableName() {
        return "mariadb_roundtrip_" + UUID.randomUUID().toString().replace("-", "");
    }

    static String cleanupSql(final String table) {
        return "DROP TABLE IF EXISTS " + table;
    }

    /** Publisher-level timeout cancels the upstream subscription, unlike StepVerifier.verify(Duration). */
    static <T> Flux<T> bounded(final Publisher<T> publisher) {
        return Flux.from(publisher).timeout(OPERATION_TIMEOUT);
    }

    @Test
    void fullReactiveRoundTripAgainstLiveMariaDb() {
        final ReactiveR2dbcAccessor accessor = defaultR2dbcClientService.getReactiveAccessor();

        // Version pin: the backend must be the MariaDB server the lifecycle pinned, not whatever
        // answers on the port.
        StepVerifier.create(bounded(accessor.query("version", "SELECT VERSION()",
                        (row, rowMetadata) -> row.get(0, String.class))))
                .assertNext(version -> {
                    Assertions.assertTrue(version.contains("MariaDB"),
                            "server must identify as MariaDB, got: " + version);
                    Assertions.assertTrue(version.startsWith(PINNED_VERSION_PREFIX),
                            "server must be the pinned MariaDB " + PINNED_VERSION_PREFIX + "x, got: " + version);
                })
                .verifyComplete();

        StepVerifier.create(bounded(accessor.update(
                        "create", "CREATE TABLE " + TABLE + "(id INT PRIMARY KEY, name VARCHAR(20))")))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(bounded(accessor.update("insert", "INSERT INTO " + TABLE + " VALUES (?, ?)", 1, "autocommit")))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(bounded(accessor.query("select", "SELECT name FROM " + TABLE + " WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 1)))
                .expectNext("autocommit")
                .verifyComplete();

        StepVerifier.create(bounded(accessor.inTransaction("tx-commit",
                        session -> Mono.from(session.update("INSERT INTO " + TABLE + " VALUES (?, ?)", 2, "committed")))))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(bounded(accessor.query("select-committed", "SELECT name FROM " + TABLE + " WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 2)))
                .expectNext("committed")
                .verifyComplete();

        StepVerifier.create(bounded(accessor.inTransaction("tx-rollback",
                        session -> Mono.from(session.update("INSERT INTO " + TABLE + " VALUES (?, ?)", 3, "rolledback"))
                                .then(Mono.error(new IllegalStateException("boom"))))))
                .expectErrorSatisfies(error -> {
                    Assertions.assertInstanceOf(ReactiveOperationException.class, error);
                    Assertions.assertInstanceOf(IllegalStateException.class, error.getCause());
                    Assertions.assertEquals("boom", error.getCause().getMessage());
                })
                .verify();

        StepVerifier.create(bounded(accessor.query("select-rolledback", "SELECT name FROM " + TABLE + " WHERE id = ?",
                        (row, rowMetadata) -> row.get("name", String.class), 3)))
                .verifyComplete();

        StepVerifier.create(bounded(accessor.update("drop", "DROP TABLE " + TABLE)))
                .expectNextCount(1)
                .verifyComplete();
    }

    /**
     * Negative path: a query against a table that does not exist must fail with an error signal. A
     * silent empty result would hide a connection that reached the wrong schema.
     */
    @Test
    void negativeMissingTableQueryFails() {
        final ReactiveR2dbcAccessor accessor = defaultR2dbcClientService.getReactiveAccessor();

        final String missing = TABLE + "_missing";
        StepVerifier.create(bounded(accessor.query("select-missing", "SELECT name FROM " + missing,
                        (row, rowMetadata) -> row.get("name", String.class))))
                .expectErrorSatisfies(error -> assertNoSuchTable(error, missing))
                .verify();
    }

    /**
     * Negative auth path: the same MariaDB server, reached with a wrong password, must refuse the
     * login. The refusal must carry the server's own "Access denied" text in its cause chain. A typo in
     * the fixture or an unreachable host fails differently, and that must not pass as a refusal.
     */
    @Test
    void negativeWrongPasswordIsRefusedByServer() {
        final Mono<Integer> badAuthQuery = Mono.using(
                () -> R2dbcClientServiceTestHelper.defaultR2dbcClientService(
                        MARIADB_BAD_PASSWORD_CLASSPATH_FILE),
                badAuth -> Mono.from(badAuth.getReactiveAccessor().query("badAuth", "SELECT 1",
                        (row, rowMetadata) -> row.get(0, Integer.class))),
                DefaultR2dbcClientService::shutdown);
        StepVerifier.create(bounded(badAuthQuery))
                .expectErrorMatches(ReactiveR2dbcAccessorMariaDbIT::isAccessDenied)
                .verify();
    }

    /**
     * The failure must be the server's own missing-table error: an {@link R2dbcBadGrammarException} with
     * SQLState 42S02, naming the table. Any other failure, such as a dead connection, does not pass.
     */
    private static void assertNoSuchTable(final Throwable failure, final String table) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof R2dbcBadGrammarException
                    && NO_SUCH_TABLE_SQL_STATE.equals(((R2dbcBadGrammarException) t).getSqlState())
                    && t.getMessage() != null && t.getMessage().contains(table)) {
                return;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        Assertions.fail("expected the server's missing-table error (42S02) naming " + table + ", got: " + failure);
    }

    private static final class ShutdownOnClose implements AutoCloseable {
        private final DefaultR2dbcClientService service;

        private ShutdownOnClose(final DefaultR2dbcClientService service) {
            this.service = service;
        }

        @Override
        public void close() {
            service.shutdown();
        }
    }

    private static boolean isAccessDenied(final Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            final String message = t.getMessage();
            if (message != null && message.contains("Access denied")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /**
     * Cleanup assertion: a table dropped through the accessor is gone. Querying it afterwards must
     * fail, which the round trip's own final drop does not prove.
     */
    @Test
    void cleanupDroppedTableIsGone() {
        final ReactiveR2dbcAccessor accessor = defaultR2dbcClientService.getReactiveAccessor();
        final String table = TABLE + "_cleanup";

        StepVerifier.create(bounded(accessor.update("createCleanup", "CREATE TABLE " + table + "(id INT PRIMARY KEY)")))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(bounded(accessor.update("dropCleanup", "DROP TABLE " + table)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(bounded(accessor.query("selectDropped", "SELECT id FROM " + table,
                        (row, rowMetadata) -> row.get("id", Integer.class))))
                .expectErrorSatisfies(error -> assertNoSuchTable(error, table))
                .verify();
    }
}
