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

package com.viveworks.appbase.client.service.vitess.accessor.reactive;

import com.viveworks.appbase.client.service.vitess.DefaultVitessGrpcClientService;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClientServiceTestHelper;
import com.viveworks.appbase.client.service.vitess.accessor.VitessGrpcAccessor;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.vitess.client.cursor.Cursor;
import io.vitess.client.cursor.Row;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;

/**
 * Gated integration test exercising both faces of the Vitess gRPC accessor layer --
 * {@link ReactiveVitessGrpcAccessor} and {@link VitessGrpcAccessor} -- against a live Vitess
 * cluster, mirroring the bootstrap of {@code DefaultVitessGrpcClientServiceTest}. The reactive
 * face runs over the async {@code VTGateConnection}; the synchronous face runs over the real
 * blocking {@code VTGateBlockingConnection} -- it never blocks on the async connection, since
 * blocking on a non-blocking client's completion still ties up a thread for the duration of the
 * call, defeating the purpose of using it (see {@link ReactiveVitessGrpcAccessor}'s class
 * Javadoc).
 *
 * <p>Requires a running Vitess cluster reachable per the module's gated-test config: system
 * property {@code VitessClientServiceTests.enabled=true} (defaults to {@code false} in {@code
 * appbase-client/pom.xml}, the same gate {@code DefaultVitessGrpcClientServiceTest}/{@code
 * VitessGrpcClientServiceGuiceModuleTest} use), vtgate reachable per {@code
 * appbase-client-vitess-grpc.yaml}. The module binds {@code maven-failsafe-plugin} in the develop,
 * staging and release profiles, so this {@code *IT} class is selected by the integration profile.
 * Surefire's default includes do not select {@code *IT} classes; {@code -Dtest=} still selects it
 * directly.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "VitessClientServiceTests.enabled", matches = "true")
class ReactiveVitessGrpcAccessorIT {

    private static final String BAD_PORT_RESOURCE = "/appbase-client-vitess-grpc-badport.yaml";
    /** MySQL/Vitess ER_NO_SUCH_TABLE (1146) carries SQLState 42S02. */
    private static final String NO_SUCH_TABLE_SQL_STATE = "42S02";
    // Per-run table names: a repeated run cannot collide with a table a previous run left behind.
    private static final String REACTIVE_TABLE = "appbase_reactive_it_reactive_" + System.nanoTime();
    private static final String SYNC_TABLE = "appbase_reactive_it_sync_" + System.nanoTime();
    private static final String REACTIVE_CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " + REACTIVE_TABLE + " (id INT, val VARCHAR(255)) ENGINE=InnoDB";
    private static final String SYNC_CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " + SYNC_TABLE + " (id INT, val VARCHAR(255)) ENGINE=InnoDB";
    private static final String REACTIVE_DROP_TABLE_SQL = "DROP TABLE " + REACTIVE_TABLE;
    private static final String SYNC_DROP_TABLE_SQL = "DROP TABLE " + SYNC_TABLE;
    private static final String REACTIVE_DROP_TABLE_IF_EXISTS_SQL = "DROP TABLE IF EXISTS " + REACTIVE_TABLE;
    private static final String SYNC_DROP_TABLE_IF_EXISTS_SQL = "DROP TABLE IF EXISTS " + SYNC_TABLE;
    private static final String REACTIVE_VALUE = "appbase-reactive-vitess-it-reactive-value";
    private static final String SYNC_VALUE = "appbase-reactive-vitess-it-sync-value";

    private static DefaultVitessGrpcClientService defaultVitessGrpcClientService;

    /**
     * Boots a live {@link DefaultVitessGrpcClientService} against vtgate, mirroring {@code
     * DefaultVitessGrpcClientServiceTest}'s bootstrap via {@link
     * VitessGrpcClientServiceTestHelper#defaultVitessGrpcClientService()}.
     */
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultVitessGrpcClientService.registerObjectMapperSubtypes();
        defaultVitessGrpcClientService = VitessGrpcClientServiceTestHelper.defaultVitessGrpcClientService();
    }

    /**
     * Best-effort drops each round trip's own test table before shutting down the service, so a
     * failed assertion in either round trip does not orphan its table on the live cluster, then
     * shuts down {@link #defaultVitessGrpcClientService}.
     */
    @AfterAll
    @SuppressWarnings("checkstyle:IllegalCatch")
    static void tearDown() {
        if (defaultVitessGrpcClientService != null) {
            try {
                defaultVitessGrpcClientService.getAccessor()
                        .execute("cleanupDrop", REACTIVE_DROP_TABLE_IF_EXISTS_SQL, Collections.emptyMap());
            } catch (final Exception ignored) {
                // best-effort: tolerate the table already being gone (the test itself drops it on
                // success) and any other failure here, so cleanup never masks the test's own
                // assertion failures.
            }

            try {
                defaultVitessGrpcClientService.getAccessor()
                        .execute("cleanupDrop", SYNC_DROP_TABLE_IF_EXISTS_SQL, Collections.emptyMap());
            } catch (final Exception ignored) {
                // best-effort: tolerate the table already being gone (the test itself drops it on
                // success) and any other failure here, so cleanup never masks the test's own
                // assertion failures.
            }

            defaultVitessGrpcClientService.shutdown();
        }
    }

    private static Row readRow(final Cursor cursor) {
        try {
            final Row row = cursor.next();
            Assertions.assertNotNull(row);
            return row;
        } catch (final SQLException ex) {
            return Assertions.fail(ex);
        }
    }

    private static String readVal(final Row row) {
        try {
            return new String(row.getBytes("val"), StandardCharsets.UTF_8);
        } catch (final SQLException ex) {
            return Assertions.fail(ex);
        }
    }

    private static void assertRowsAffected(final Cursor cursor, final long expected) {
        try {
            Assertions.assertEquals(expected, cursor.getRowsAffected());
        } catch (final SQLException ex) {
            Assertions.fail(ex);
        }
    }

    /**
     * Identity pin: vtgate must answer as Vitess. The pinned Vitess release (17.0.0, root pom
     * {@code vitess.version}) is held by the image digest in env-vitess.sh and is not visible from the
     * server's version string, so this asserts the server identity only; the exact release is a campaign row.
     *
     * @throws Exception if the version query fails
     */
    @Test
    void versionPinIsAsserted() throws Exception {
        final Cursor cursor = defaultVitessGrpcClientService.getAccessor()
                .execute("version", "SELECT @@version AS version", Collections.emptyMap());
        try {
            final String version = new String(readRow(cursor).getBytes("version"), StandardCharsets.UTF_8);
            Assertions.assertTrue(version.toLowerCase(Locale.ROOT).contains("vitess"),
                    "vtgate must identify as Vitess, got: " + version);
        } finally {
            cursor.close();
        }
    }

    /**
     * Reactive round trip via {@link ReactiveVitessGrpcAccessor}: {@code create} the test table
     * -&gt; {@code insert} a row (asserting one row affected) -&gt; {@code select} the row back
     * (draining the returned {@link Cursor} via {@link Cursor#next()} and reading the {@code val}
     * column) -&gt; {@code streamExecute} the same row (asserting the streamed {@link Row}
     * contains the expected value) -&gt; {@code drop} the table. Cleanup is also handled
     * unconditionally by {@link #tearDown()}, so a failed assertion here does not orphan the table
     * on the live cluster.
     */
    @Test
    void reactiveRoundTrip() {
        final ReactiveVitessGrpcAccessor reactive = defaultVitessGrpcClientService.getReactiveAccessor();

        StepVerifier.create(reactive.execute("create", REACTIVE_CREATE_TABLE_SQL, Collections.emptyMap()))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();

        StepVerifier.create(reactive.execute(
                        "insert", "INSERT INTO " + REACTIVE_TABLE + " (id, val) VALUES (1, '" + REACTIVE_VALUE + "')",
                        Collections.emptyMap()))
                .assertNext(cursor -> assertRowsAffected(cursor, 1L))
                .verifyComplete();

        StepVerifier.create(reactive.execute("select", "SELECT val FROM " + REACTIVE_TABLE + " WHERE id = 1",
                        Collections.emptyMap()))
                .assertNext(cursor -> Assertions.assertEquals(REACTIVE_VALUE, readVal(readRow(cursor))))
                .verifyComplete();

        StepVerifier.create(reactive.streamExecute("stream",
                        "SELECT val FROM " + REACTIVE_TABLE + " WHERE id = 1", Collections.emptyMap())
                        .map(ReactiveVitessGrpcAccessorIT::readVal))
                .expectNext(REACTIVE_VALUE)
                .verifyComplete();

        StepVerifier.create(reactive.execute("drop", REACTIVE_DROP_TABLE_SQL, Collections.emptyMap()))
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
    }

    /**
     * Synchronous round trip via {@link VitessGrpcAccessor}, mirroring the same {@code create} ->
     * {@code insert} -> {@code select} -> {@code streamExecute} -> {@code drop} sequence as {@link
     * #reactiveRoundTrip()}, with blocking assertions. Cleanup is also handled unconditionally by
     * {@link #tearDown()}, so a failed assertion here does not orphan the table on the live
     * cluster.
     *
     * @throws Exception the mapped exception, if any operation fails
     */
    @Test
    void syncRoundTrip() throws Exception {
        final VitessGrpcAccessor sync = defaultVitessGrpcClientService.getAccessor();

        final Cursor createCursor = sync.execute("create", SYNC_CREATE_TABLE_SQL, Collections.emptyMap());
        Assertions.assertNotNull(createCursor);

        final Cursor insertCursor = sync.execute("insert",
                "INSERT INTO " + SYNC_TABLE + " (id, val) VALUES (2, '" + SYNC_VALUE + "')",
                Collections.emptyMap());
        assertRowsAffected(insertCursor, 1L);

        // sync execute()'s Cursor fully materializes before returning and its borrowed connection
        // is already released -- closing it here is just good hygiene (AutoCloseable), best-effort.
        try (Cursor selectCursor = sync.execute("select",
                "SELECT val FROM " + SYNC_TABLE + " WHERE id = 2", Collections.emptyMap())) {
            Assertions.assertEquals(SYNC_VALUE, readVal(readRow(selectCursor)));
        }

        // sync streamExecute()'s Cursor, unlike execute()'s, holds its borrowed connection open
        // for the duration of the stream -- it now MUST be closed to return the connection to the
        // pool (see VitessGrpcAccessor#streamExecute's Javadoc / BorrowReleasingCursor).
        try (Cursor streamCursor = sync.streamExecute("stream",
                "SELECT val FROM " + SYNC_TABLE + " WHERE id = 2", Collections.emptyMap())) {
            Assertions.assertEquals(SYNC_VALUE, readVal(readRow(streamCursor)));
        }

        final Cursor dropCursor = sync.execute("drop", SYNC_DROP_TABLE_SQL, Collections.emptyMap());
        Assertions.assertNotNull(dropCursor);
    }

    /**
     * Negative path, reactive: a select against a table that does not exist must fail with an error
     * signal. An empty result would mean the query reached a schema that is not the one under test.
     */
    @Test
    void negativeMissingTableSelectFailsReactive() {
        final ReactiveVitessGrpcAccessor reactive = defaultVitessGrpcClientService.getReactiveAccessor();

        final String missing = REACTIVE_TABLE + "_missing";
        StepVerifier.create(reactive.execute("selectMissing",
                        "SELECT val FROM " + missing, Collections.emptyMap()))
                .expectErrorSatisfies(error -> assertNoSuchTable(error, missing))
                .verify();
    }

    /**
     * Negative config path: the same client pointed at a gRPC port that is closed at run time (bound and
     * released here, so nothing listens on it). The operation must fail with a connection refusal
     * specifically; a bootstrap failure is accepted only when it is the same refusal.
     *
     * @throws Exception if the fixture cannot be written
     */
    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    void negativeUnreachableGrpcPortFails() throws Exception {
        final int closedPort = closedLoopbackPort();
        final Path fixture = writeFixture(closedPort);

        DefaultVitessGrpcClientService badService = null;
        try {
            try {
                badService = VitessGrpcClientServiceTestHelper.defaultVitessGrpcClientService(fixture);
            } catch (final RuntimeException bootstrapFailure) {
                Assertions.assertTrue(reportsConnectionRefused(bootstrapFailure),
                        "bootstrap against a closed gRPC port must report a connection refusal: " + bootstrapFailure);
                return;
            }
            final VitessGrpcAccessor sync = badService.getAccessor();
            final Exception failure = Assertions.assertThrows(Exception.class, () ->
                    sync.execute("unreachable", "SELECT 1", Collections.emptyMap()));
            Assertions.assertTrue(reportsConnectionRefused(failure),
                    "an unreachable gRPC port must report a connection refusal: " + failure);
        } finally {
            if (badService != null) {
                badService.shutdown();
            }
            Files.deleteIfExists(fixture);
        }
    }

    private static int closedLoopbackPort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    /**
     * Writes a bad-port fixture with the port replaced by the one closed at run time. The
     * substitution is checked.
     */
    private static Path writeFixture(final int port) throws IOException {
        final String template;
        try (InputStream in = ReactiveVitessGrpcAccessorIT.class.getResourceAsStream(BAD_PORT_RESOURCE)) {
            Assertions.assertNotNull(in, "bad-port fixture missing: " + BAD_PORT_RESOURCE);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        final String yaml = template.replaceAll("(?m)(port:\\s*)15999\\b", "$1" + port);
        Assertions.assertFalse(yaml.contains("15999"), "the fixture's port must be replaced");

        final Path fixture = Files.createTempFile("appbase-client-vitess-grpc-closedport-", ".yaml");
        Files.write(fixture, yaml.getBytes(StandardCharsets.UTF_8));
        return fixture;
    }

    /**
     * A connection refusal: a {@link ConnectException} in the cause chain, or the text
     * "Connection refused" (gRPC reports it as an UNAVAILABLE status carrying that cause). A
     * generic "connect" substring is not enough.
     */
    private static boolean reportsConnectionRefused(final Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof ConnectException) {
                return true;
            }
            final String message = t.getMessage();
            if (message != null && message.contains("Connection refused")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /**
     * The failure must be the server's own missing-table error: SQLState 42S02, or its "not found" /
     * "doesn't exist" text, naming the table. Any other failure, such as a dead connection, does not pass.
     */
    private static void assertNoSuchTable(final Throwable failure, final String table) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            final String message = t.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(table.toLowerCase(Locale.ROOT))) {
                final boolean sqlState = t instanceof SQLException
                        && NO_SUCH_TABLE_SQL_STATE.equals(((SQLException) t).getSQLState());
                if (sqlState || message.contains("not found") || message.contains("doesn't exist")) {
                    return;
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        Assertions.fail("expected the server's missing-table error naming " + table + ", got: " + failure);
    }

}
