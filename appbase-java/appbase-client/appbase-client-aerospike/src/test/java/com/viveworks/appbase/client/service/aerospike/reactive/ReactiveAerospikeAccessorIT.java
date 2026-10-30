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

package com.viveworks.appbase.client.service.aerospike.reactive;

import com.aerospike.client.Bin;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.viveworks.appbase.client.service.aerospike.AerospikeAccessor;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientTestHelper;
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientService;
import com.viveworks.appbase.core.AppbaseCore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Integration tests for {@link ReactiveAerospikeAccessor} and its synchronous twin {@code
 * AerospikeAccessor}, both acquired from {@link DefaultAerospikeClientService}, against a live
 * Aerospike cluster.
 *
 * <p>Requires a reachable Aerospike instance. Enable via system property:
 * {@code -DAerospikeClientServiceTests.enabled=true} -- the same gate used by {@code
 * DefaultAerospikeClientServiceTest}. There is no Docker script for Aerospike in this repo (unlike
 * Cassandra/Kafka, see {@code infra/docker/}); this test connects using the same {@code
 * appbase-client-aerospike.yaml} test config the other gated Aerospike tests use (default
 * {@code localhost:3000}), loaded via {@link AerospikeClientTestHelper}. Point a local or remote
 * Aerospike instance at that host/port, or override the config file via the system property named
 * by {@link AerospikeClientTestHelper#AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY}.
 *
 * <p>Driver-native metrics (the {@code driverMetricsEnabled}-gated bridge onto the shared
 * composite registry via {@code AppbaseAerospikeMetricsListener}) are exercised at the unit level
 * by {@code DefaultAerospikeClientIntegrationServiceMetricsTest} and {@code
 * AppbaseAerospikeMetricsListenerTest}, not here: the driver only pushes a snapshot to the
 * listener on its own periodic {@code MetricsPolicy#interval}, so asserting on a bridged meter
 * from this IT would make it flaky/timing-dependent rather than exercising anything the unit
 * tests don't already cover.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "AerospikeClientServiceTests.enabled", matches = "true")
class ReactiveAerospikeAccessorIT {

    // Matches the pinned lifecycle and the sibling Aerospike token-cache/session integration tests.
    private static final String NAMESPACE = "dev";
    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern CLOSED_ENDPOINT = Pattern.compile("\\blocalhost 1(?!\\d)");
    private static final String CLOSED_PORT_CONFIG = "classpath:///appbase-client-aerospike-closed-port.yaml";
    private static final String RUN_ID = UUID.randomUUID().toString();
    // A fixed set avoids exhausting Aerospike 5.x namespace set metadata; keys remain run-unique.
    private static final String SET = "appbase_reactive_it";
    private static final int RECORD_TTL_SECONDS = 60 * 60;

    private static DefaultAerospikeClientService aerospikeClientService;

    @BeforeAll
    static void setUp() {
        aerospikeClientService = AerospikeClientTestHelper.defaultAerospikeClientService();
    }

    @AfterAll
    static void tearDown() {
        if (aerospikeClientService != null) {
            aerospikeClientService.shutdown();
        }
    }

    /**
     * Verifies a reactive {@code put} -> {@code get} -> {@code delete} round-trip through {@link
     * ReactiveAerospikeAccessor}: the written bin is read back with the expected value, deletion
     * emits the requested key, and a subsequent get completes empty.
     */
    @Test
    void reactivePutGetDeleteRoundTrip() {
        final ReactiveAerospikeAccessor reactive = aerospikeClientService.getReactiveAccessor();
        final Key key = new Key(NAMESPACE, SET, "reactive-" + RUN_ID);
        final WritePolicy writePolicy = expiringWritePolicy();
        boolean deleted = false;

        try {
            StepVerifier.create(reactive.put("put", writePolicy, key, new Bin("v", "hello")))
                    .expectNextCount(1)
                    .expectComplete().verify(OPERATION_TIMEOUT);

            StepVerifier.create(reactive.get("get", key).map(keyRecord -> keyRecord.record.getString("v")))
                    .expectNext("hello")
                    .expectComplete().verify(OPERATION_TIMEOUT);

            StepVerifier.create(reactive.delete("delete", key))
                    .expectNext(key)
                    .expectComplete().verify(OPERATION_TIMEOUT);

            StepVerifier.create(reactive.get("getAfterDelete", key))
                    .expectComplete().verify(OPERATION_TIMEOUT);
            deleted = true;
        } finally {
            if (!deleted) {
                try {
                    StepVerifier.create(reactive.delete("cleanupDelete", key))
                            .expectNextCount(1)
                            .expectComplete().verify(OPERATION_TIMEOUT);
                } catch (RuntimeException | AssertionError cleanupFailure) {
                    System.err.println("Suppressed Aerospike reactive cleanup failure: " + cleanupFailure);
                }
            }
        }
    }

    /**
     * Verifies a synchronous {@code put} -> {@code get} -> {@code exists} -> {@code delete} ->
     * {@code exists} round-trip through {@code AerospikeAccessor}.
     */
    @Test
    void syncPutGetExistsDeleteRoundTrip() throws Exception {
        final AerospikeAccessor sync = aerospikeClientService.getAccessor();
        final Key key = new Key(NAMESPACE, SET, "sync-" + RUN_ID);
        final WritePolicy writePolicy = expiringWritePolicy();

        try {
            sync.put("put", writePolicy, key, new Bin("v", "hello"));

            final Record record = sync.get("get", key);
            Assertions.assertNotNull(record);
            Assertions.assertEquals("hello", record.getString("v"));

            Assertions.assertTrue(sync.exists("exists", key));

            Assertions.assertTrue(sync.delete("delete", key));
            Assertions.assertFalse(sync.exists("existsAfterDelete", key));
        } finally {
            try {
                sync.delete("cleanupDelete", key);
            } catch (Exception cleanupFailure) {
                System.err.println("Suppressed Aerospike synchronous cleanup failure: " + cleanupFailure);
            }
        }
    }

    @Test
    void reactiveGetMissingKeyCompletesEmpty() {
        final Key missingKey = new Key(NAMESPACE, SET, "missing-" + RUN_ID);
        StepVerifier.create(aerospikeClientService.getReactiveAccessor().get("getMissing", missingKey))
                .as("Aerospike GET of run-unique missing record " + missingKey)
                .expectComplete().verify(OPERATION_TIMEOUT);
    }

    /** Verifies the Appbase helper path surfaces Aerospike's endpoint-specific connection failure. */
    @Test
    void closedPortFailsWithDiagnostic() {
        final String property = AerospikeClientTestHelper.AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY;
        final String previous = System.getProperty(property);
        try {
            System.setProperty(property, CLOSED_PORT_CONFIG);
            final AppbaseCore[] closedPortCore = new AppbaseCore[1];
            final DefaultAerospikeClientService[] closedPortService = new DefaultAerospikeClientService[1];
            final RuntimeException error;
            try {
                error = Assertions.assertTimeout(
                                        OPERATION_TIMEOUT,
                                        () -> Assertions.assertThrows(RuntimeException.class,
                                                () -> {
                                                    closedPortCore[0] = AerospikeClientTestHelper.appbaseCore();
                                                    closedPortService[0] = AerospikeClientTestHelper
                                                            .defaultAerospikeClientService(closedPortCore[0]);
                                                }));
            } catch (RuntimeException | AssertionError failure) {
                shutdownClosedPort(failure, closedPortService[0], closedPortCore[0]);
                throw failure;
            }
            final int suppressedBeforeCleanup = error.getSuppressed().length;
            shutdownClosedPort(error, closedPortService[0], closedPortCore[0]);
            Assertions.assertEquals(suppressedBeforeCleanup, error.getSuppressed().length,
                    "Closed-port cleanup must not add suppressed failures");
            final AerospikeException.Connection connectionError = ItCauses.findCause(error, AerospikeException.Connection.class);
            Assertions.assertNotNull(connectionError, error::toString);
            Assertions.assertTrue(CLOSED_ENDPOINT.matcher(String.valueOf(connectionError.getMessage())).find(), connectionError::getMessage);
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }
    }

    private static WritePolicy expiringWritePolicy() {
        final WritePolicy policy = new WritePolicy();
        policy.expiration = RECORD_TTL_SECONDS;
        return policy;
    }

    private static void shutdownClosedPort(final Throwable primary,
            final DefaultAerospikeClientService service, final AppbaseCore core) {
        suppressCleanup(primary, () -> {
            if (service != null) {
                service.shutdown();
            }
        });
        suppressCleanup(primary, () -> {
            if (core != null) {
                core.shutdown();
            }
        });
    }

    private static void suppressCleanup(final Throwable primary, final Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | AssertionError failure) {
            primary.addSuppressed(failure);
        }
    }
}
