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
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Integration test for {@link DefaultRedisClientService}.
 *
 * <p>Requires a running Redis instance. Enable via system property:
 * {@code -DRedisClientServiceTests.enabled=true}
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "RedisClientServiceTests.enabled", matches = "true")
class DefaultRedisClientServiceTest {
    private static DefaultRedisClientService defaultRedisClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultRedisClientService.registerObjectMapperSubtypes();
        defaultRedisClientService = RedisClientServiceTestHelper.defaultRedisClientService();
    }

    @AfterAll
    static void tearDown() {
        if (defaultRedisClientService != null) {
            defaultRedisClientService.shutdown();
        }
    }

    @Test
    void testDefaultRedisClientService() throws AppbaseClientIntegrationService.NotFoundException {
        Assertions.assertNotNull(defaultRedisClientService);

        final RedisClientIntegrationService integrationService =
                defaultRedisClientService.getClientIntegrationService();
        Assertions.assertNotNull(integrationService);
        Assertions.assertTrue(integrationService.isHealthy());

        final StatefulRedisConnection<String, String> connection = integrationService.getClient();
        Assertions.assertNotNull(connection);
        Assertions.assertTrue(connection.isOpen());

        final String pong = connection.sync().ping();
        Assertions.assertEquals("PONG", pong);
    }
}
