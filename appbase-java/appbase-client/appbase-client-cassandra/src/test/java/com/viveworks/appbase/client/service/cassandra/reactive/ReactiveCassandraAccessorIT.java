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

package com.viveworks.appbase.client.service.cassandra.reactive;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.cassandra.CassandraClientTestHelper;
import com.viveworks.appbase.client.service.cassandra.DefaultCassandraClientService;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integration tests for {@link ReactiveCassandraAccessor} against a live Cassandra instance.
 *
 * <p>Requires a running Cassandra instance. Enable via system property:
 * {@code -DCassandraClientServiceTests.enabled=true}
 *
 * <p>Use the Docker scripts in {@code infra/docker/cassandra/} to start a local Cassandra instance.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "CassandraClientServiceTests.enabled", matches = "true")
class ReactiveCassandraAccessorIT {

    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(30);
    private static final long ORPHAN_MAX_AGE_SECONDS = 60 * 60;
    private static final String CLOSED_PORT_CONFIG = "classpath:///appbase-client-cassandra-closed-port.yaml";
    private static final Pattern ORPHAN_KEYSPACE = Pattern.compile("abrit_(\\d{10})_[0-9a-f]{16}");
    private static final Pattern CLOSED_ENDPOINT = Pattern.compile("localhost\\S*:1(?![\\d:])");
    private static final String RUN_ID = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private static final String KEYSPACE = "abrit_" + Instant.now().getEpochSecond() + "_" + RUN_ID;
    private static final String TABLE = KEYSPACE + ".kv";
    private static final String TEST_KEY = "key-" + RUN_ID;
    private static final String MISSING_KEY = "missing-" + RUN_ID;
    private static final String TEST_VALUE = "appbase-reactive-cassandra-it-value";

    private static DefaultCassandraClientService cassandraClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultCassandraClientService.registerObjectMapperSubtypes();
        if (KEYSPACE.length() > 48) {
            throw new IllegalStateException("Cassandra keyspace exceeds 48 characters: " + KEYSPACE);
        }
        cassandraClientService = CassandraClientTestHelper.defaultCassandraClientService();
        final ReactiveCassandraAccessor accessor = cassandraClientService.getReactiveAccessor();
        final long cutoff = Instant.now().getEpochSecond() - ORPHAN_MAX_AGE_SECONDS;
        StepVerifier.create(accessor.executeReactive("findOrphanKeyspaces", SimpleStatement.newInstance(
                        "SELECT keyspace_name FROM system_schema.keyspaces"))
                .concatMap(row -> {
                    final String keyspace = row.getString("keyspace_name");
                    final Matcher matcher = ORPHAN_KEYSPACE.matcher(keyspace == null ? "" : keyspace);
                    if (!matcher.matches() || Long.parseLong(matcher.group(1)) >= cutoff) {
                        return Mono.empty();
                    }
                    return accessor.executeReactive("dropOrphanKeyspace", SimpleStatement.newInstance(
                            "DROP KEYSPACE IF EXISTS " + keyspace));
                }))
                .expectComplete().verify(OPERATION_TIMEOUT);
        StepVerifier.create(accessor.executeReactive("createKeyspace", SimpleStatement.newInstance(
                        "CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}")))
                .expectComplete().verify(OPERATION_TIMEOUT);
        StepVerifier.create(accessor.executeReactive("createTable", SimpleStatement.newInstance(
                        "CREATE TABLE IF NOT EXISTS " + TABLE + " (k text PRIMARY KEY, v text)")))
                .expectComplete().verify(OPERATION_TIMEOUT);
    }

    @AfterAll
    static void tearDown() {
        if (cassandraClientService != null) {
            try {
                StepVerifier.create(cassandraClientService.getReactiveAccessor().executeReactive(
                                "dropKeyspace", SimpleStatement.newInstance("DROP KEYSPACE IF EXISTS " + KEYSPACE)))
                        .expectComplete().verify(OPERATION_TIMEOUT);
            } finally {
                cassandraClientService.shutdown();
            }
        }
    }

    /**
     * Verifies a reactive schema setup, {@code INSERT}, and {@code SELECT} round-trips the stored value through
     * {@link ReactiveCassandraAccessor}: DDL and the write complete with no rows, and the read returns the value via
     * {@link ReactiveCassandraAccessor#executeReactiveFirst(String, com.datastax.oss.driver.api.core.cql.Statement)}.
     */
    @Test
    void reactiveInsertThenSelectRoundTrip() {
        final ReactiveCassandraAccessor accessor = cassandraClientService.getReactiveAccessor();

        StepVerifier.create(accessor.executeReactive("insert", SimpleStatement.newInstance(
                        "INSERT INTO " + TABLE + " (k, v) VALUES (?, ?)", TEST_KEY, TEST_VALUE)))
                .expectComplete().verify(OPERATION_TIMEOUT);

        StepVerifier.create(accessor.executeReactiveFirst("select", SimpleStatement.newInstance(
                        "SELECT v FROM " + TABLE + " WHERE k = ?", TEST_KEY))
                        .map(row -> row.getString("v")))
                .expectNext(TEST_VALUE)
                .expectComplete().verify(OPERATION_TIMEOUT);

        StepVerifier.create(accessor.executeReactive("delete", SimpleStatement.newInstance(
                        "DELETE FROM " + TABLE + " WHERE k = ?", TEST_KEY)))
                .expectComplete().verify(OPERATION_TIMEOUT);
    }

    @Test
    void reactiveSelectMissingKeyCompletesEmpty() {
        StepVerifier.create(cassandraClientService.getReactiveAccessor().executeReactiveFirst(
                        "selectMissing", SimpleStatement.newInstance(
                                "SELECT v FROM " + TABLE + " WHERE k = ?", MISSING_KEY)))
                .as("Cassandra SELECT of run-unique missing partition " + MISSING_KEY)
                .expectComplete().verify(OPERATION_TIMEOUT);
    }

    /** Verifies the Appbase helper path surfaces Cassandra's endpoint-specific connection failure. */
    @Test
    void closedPortFailsWithDiagnostic() {
        final String previous = System.getProperty(CassandraClientTestHelper.CASSANDRA_CLIENT_CONFIG_FILE_PROPERTY);
        try {
            System.setProperty(CassandraClientTestHelper.CASSANDRA_CLIENT_CONFIG_FILE_PROPERTY, CLOSED_PORT_CONFIG);
            final AppbaseCore[] closedPortCore = new AppbaseCore[1];
            final DefaultAppbaseClientCore[] closedPortClientCore = new DefaultAppbaseClientCore[1];
            final DefaultCassandraClientService[] closedPortService = new DefaultCassandraClientService[1];
            final RuntimeException error;
            try {
                error = Assertions.assertTimeout(
                                        OPERATION_TIMEOUT,
                                        () -> Assertions.assertThrows(RuntimeException.class,
                                                () -> {
                                                    closedPortCore[0] = CassandraClientTestHelper.appbaseCore();
                                                    closedPortClientCore[0] = ClientCoreTestHelper
                                                            .defaultAppbaseClientCore(closedPortCore[0]);
                                                    closedPortService[0] = new DefaultCassandraClientService(
                                                            closedPortClientCore[0]);
                                                }));
            } catch (AppbaseRuntimeException | AssertionError failure) {
                shutdownClosedPort(failure, closedPortService[0], closedPortClientCore[0], closedPortCore[0]);
                throw failure;
            }
            final int suppressedBeforeCleanup = error.getSuppressed().length;
            shutdownClosedPort(error, closedPortService[0], closedPortClientCore[0], closedPortCore[0]);
            final Throwable[] suppressedAfterCleanup = error.getSuppressed();
            final Throwable[] cleanupFailures = java.util.Arrays.copyOfRange(
                    suppressedAfterCleanup, suppressedBeforeCleanup, suppressedAfterCleanup.length);
            Assertions.assertEquals(0, cleanupFailures.length,
                    () -> "Closed-port cleanup failures: " + java.util.Arrays.toString(cleanupFailures));
            final AllNodesFailedException connectionError = ItCauses.findCause(error, AllNodesFailedException.class);
            Assertions.assertNotNull(connectionError, error::toString);
            Assertions.assertTrue(
                    CLOSED_ENDPOINT.matcher(String.valueOf(connectionError.getMessage())).find(),
                    connectionError::getMessage);
        } finally {
            restoreProperty(CassandraClientTestHelper.CASSANDRA_CLIENT_CONFIG_FILE_PROPERTY, previous);
        }
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void shutdownClosedPort(final Throwable primary,
            final DefaultCassandraClientService service, final DefaultAppbaseClientCore clientCore,
            final AppbaseCore core)
    {
        suppressCleanup(primary, "partial service", () -> {
            if (service != null) {
                service.shutdown();
            }
        });
        suppressCleanup(primary, "client core", () -> {
            if (clientCore != null) {
                clientCore.shutdown();
            }
        });
        suppressCleanup(primary, "Appbase core", () -> {
            if (core != null) {
                core.shutdown();
            }
        });
    }

    private static void suppressCleanup(final Throwable primary, final String phase, final Runnable cleanup) {
        try {
            cleanup.run();
        } catch (AppbaseRuntimeException | AssertionError failure) {
            primary.addSuppressed(new IllegalStateException(
                    phase + " cleanup failed with " + failure.getClass().getName() + ": " + failure.getMessage(),
                    failure));
        }
    }
}
