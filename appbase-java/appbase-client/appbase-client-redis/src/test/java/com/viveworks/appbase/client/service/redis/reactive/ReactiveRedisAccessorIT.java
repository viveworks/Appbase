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

package com.viveworks.appbase.client.service.redis.reactive;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.redis.DefaultRedisClientService;
import com.viveworks.appbase.client.service.redis.RedisClientServiceTestHelper;
import com.viveworks.appbase.client.service.redis.RedisItSupport;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

/**
 * Integration tests for {@link ReactiveRedisAccessor} against a live Redis instance.
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
class ReactiveRedisAccessorIT {

    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(10);
    private static final String CLOSED_PORT_CONFIG = RedisItSupport.CLOSED_PORT_CONFIG;
    private static final String RUN_ID = UUID.randomUUID().toString();
    private static final String TEST_KEY = "appbase:it:reactive:" + RUN_ID;
    private static final String MISSING_KEY = TEST_KEY + ":missing";
    private static final String TEST_VALUE = "appbase-reactive-redis-it-value";

    private static DefaultRedisClientService redisClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultRedisClientService.registerObjectMapperSubtypes();
        redisClientService = RedisClientServiceTestHelper.defaultRedisClientService();
    }

    @AfterAll
    static void tearDown() {
        if (redisClientService != null) {
            redisClientService.shutdown();
        }
    }

    /**
     * Verifies a reactive SET followed by a reactive GET round-trips the stored value through
     * {@link ReactiveRedisAccessor}, and that a reactive DEL removes it afterward.
     */
    @Test
    void reactiveSetThenGetRoundTrip() {
        final ReactiveRedisAccessor accessor = redisClientService.getReactiveAccessor();

        try {
            StepVerifier.create(accessor.command("set", cmds -> cmds.set(TEST_KEY, TEST_VALUE)))
                    .expectNext("OK").expectComplete().verify(OPERATION_TIMEOUT);
            StepVerifier.create(accessor.command("get", cmds -> cmds.get(TEST_KEY)))
                    .expectNext(TEST_VALUE).expectComplete().verify(OPERATION_TIMEOUT);
            StepVerifier.create(accessor.command("del", cmds -> cmds.del(TEST_KEY)))
                    .expectNext(1L).expectComplete().verify(OPERATION_TIMEOUT);
        } finally {
            StepVerifier.create(accessor.command("cleanupDelete", cmds -> cmds.del(TEST_KEY)))
                    .thenConsumeWhile(ignored -> true).expectComplete().verify(OPERATION_TIMEOUT);
        }
    }

    @Test
    void reactiveGetMissingKeyCompletesEmpty() {
        StepVerifier.create(redisClientService.getReactiveAccessor().command("getMissing", cmds -> cmds.get(MISSING_KEY)))
                .as("Redis GET of run-unique missing key " + MISSING_KEY)
                .expectComplete().verify(OPERATION_TIMEOUT);
    }

    /** Verifies the Appbase helper path surfaces a typed connection failure for its configured endpoint. */
    @Test
    void closedPortFailsWithDiagnostic() {
        final AppbaseCore[] closedPortCore = new AppbaseCore[1];
        final DefaultAppbaseClientCore[] closedPortClientCore = new DefaultAppbaseClientCore[1];
        final DefaultRedisClientService[] closedPortService = new DefaultRedisClientService[1];
        final RuntimeException error;
        try {
            error = Assertions.assertTimeout(
                                OPERATION_TIMEOUT,
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
