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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.UUID;

/**
 * Integration tests for {@link DefaultRedisClientService} against a live Redis instance.
 *
 * <p>Requires a running Redis instance. Enable via system property:
 * {@code -DRedisClientServiceTests.enabled=true}
 *
 * <p>Use the Docker scripts in {@code infra/docker/redis/} to start a local Redis instance.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "RedisClientServiceTests.enabled", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisClientServiceIT {

    private static final String CLOSED_PORT_CONFIG = RedisItSupport.CLOSED_PORT_CONFIG;
    private static final String RUN_ID = UUID.randomUUID().toString();
    private static final String TEST_KEY = "appbase:it:" + RUN_ID + ":test-key";
    private static final String MISSING_KEY = "appbase:it:" + RUN_ID + ":missing-key";
    private static final String TEST_VALUE = "appbase-redis-it-value";

    private static DefaultRedisClientService redisClientService;
    private static RedisCommands<String, String> commands;

    @BeforeAll
    static void setUp() throws AppbaseClientIntegrationService.NotFoundException {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultRedisClientService.registerObjectMapperSubtypes();
        redisClientService = RedisClientServiceTestHelper.defaultRedisClientService();

        final StatefulRedisConnection<String, String> connection =
                redisClientService.getClientIntegrationService().getClient();
        commands = connection.sync();
    }

    @AfterAll
    static void tearDown() {
        if (commands != null) {
            commands.del(TEST_KEY);
        }
        if (redisClientService != null) {
            redisClientService.shutdown();
        }
    }

    /**
     * Verifies the service starts and the connection to Redis is open.
     */
    @Test
    @Order(1)
    void testServiceStartsAndConnectionIsOpen() throws AppbaseClientIntegrationService.NotFoundException {
        Assertions.assertNotNull(redisClientService);

        final RedisClientIntegrationService integrationService =
                redisClientService.getClientIntegrationService();
        Assertions.assertNotNull(integrationService);
        Assertions.assertTrue(integrationService.isHealthy());

        final StatefulRedisConnection<String, String> connection = integrationService.getClient();
        Assertions.assertNotNull(connection);
        Assertions.assertTrue(connection.isOpen());
    }

    /**
     * Verifies PING returns PONG.
     */
    @Test
    @Order(2)
    void testPing() {
        final String response = commands.ping();
        Assertions.assertEquals("PONG", response);
    }

    /**
     * Verifies SET stores a value.
     */
    @Test
    @Order(3)
    void testSet() {
        final String result = commands.set(TEST_KEY, TEST_VALUE);
        Assertions.assertEquals("OK", result);
    }

    /**
     * Verifies GET retrieves the value stored by SET.
     */
    @Test
    @Order(4)
    void testGet() {
        commands.set(TEST_KEY, TEST_VALUE);
        final String value = commands.get(TEST_KEY);
        Assertions.assertEquals(TEST_VALUE, value);
    }

    /**
     * Verifies GET on a non-existent key returns null.
     */
    @Test
    @Order(5)
    void testGetNonExistentKeyReturnsNull() {
        final String value = commands.get(MISSING_KEY);
        Assertions.assertNull(value, "Redis GET must return null for the run-unique missing key " + MISSING_KEY);
    }

    /**
     * Verifies DEL removes the key.
     */
    @Test
    @Order(6)
    void testDel() {
        commands.set(TEST_KEY, TEST_VALUE);
        final long deleted = commands.del(TEST_KEY);
        Assertions.assertEquals(1L, deleted);

        final String value = commands.get(TEST_KEY);
        Assertions.assertNull(value);
    }

    /** Verifies the Appbase service path surfaces its endpoint-specific connection failure. */
    @Test
    @Order(7)
    void closedPortFailsWithDiagnostic() {
        final AppbaseCore[] closedPortCore = new AppbaseCore[1];
        final DefaultAppbaseClientCore[] closedPortClientCore = new DefaultAppbaseClientCore[1];
        final DefaultRedisClientService[] closedPortService = new DefaultRedisClientService[1];
        final RuntimeException error;
        try {
            error = Assertions.assertTimeout(
                                Duration.ofSeconds(10),
                                () -> Assertions.assertThrows(RuntimeException.class,
                                        () -> {
                                            closedPortCore[0] = RedisClientServiceTestHelper.appbaseCore(CLOSED_PORT_CONFIG);
                                            closedPortClientCore[0] = ClientCoreTestHelper.defaultAppbaseClientCore(closedPortCore[0]);
                                            closedPortService[0] = new DefaultRedisClientService(closedPortClientCore[0]);
                                        }));
        } catch (RuntimeException | AssertionError failure) {
            shutdownClosedPort(failure, closedPortService[0], closedPortClientCore[0], closedPortCore[0]);
            throw failure;
        }
        shutdownClosedPort(error, closedPortService[0], closedPortClientCore[0], closedPortCore[0]);
        Assertions.assertEquals(0, error.getSuppressed().length, "Closed-port cleanup failures");
        final RedisConnectionException connectionError = RedisItSupport.findCause(error, RedisConnectionException.class);
        Assertions.assertNotNull(connectionError, error::toString);
        Assertions.assertTrue(RedisItSupport.CLOSED_ENDPOINT.matcher(String.valueOf(connectionError.getMessage())).find(), connectionError::getMessage);
    }


    private static void shutdownClosedPort(final Throwable primary,
            final DefaultRedisClientService service, final DefaultAppbaseClientCore clientCore,
            final AppbaseCore core) {
        suppressCleanup(primary, () -> {
            if (service != null) {
                service.shutdown();
            }
        });
        suppressCleanup(primary, () -> {
            if (clientCore != null) {
                clientCore.shutdown();
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
