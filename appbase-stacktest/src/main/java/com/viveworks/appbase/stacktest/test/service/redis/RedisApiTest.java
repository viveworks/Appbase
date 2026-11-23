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
package com.viveworks.appbase.stacktest.test.service.redis;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.client.service.redis.RedisClientService;
import com.viveworks.appbase.stacktest.StackTestRuntimeException;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;

/**
 * Smoke-tests the Redis client service: PING → SET → GET → DEL.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public class RedisApiTest {

    private static final String TEST_KEY = "appbase:stacktest:key";
    private static final String TEST_VALUE = "appbase-stacktest-value";

    private final Logger logger;
    private final RedisClientService redisClientService;

    public RedisApiTest(final AppbaseServices appbaseServices) {
        this.logger = appbaseServices.getCoreServices().appbaseCore().getLogService().getLogger(getClass());
        this.redisClientService = appbaseServices.getClientServices().get(RedisClientService.class);
    }

    /**
     * Runs PING, SET, GET, DEL operations against the configured Redis instance.
     *
     * @param appbaseServices running Appbase services (used to resolve the Redis client)
     */
    public static void runTests(final AppbaseServices appbaseServices) {
        new RedisApiTest(appbaseServices).runTests();
    }

    private void runTests() {
        logger.info("Running test: {}", getClass().getName());

        final RedisCommands<String, String> redis =
                redisClientService.getClientIntegrationService().getClient().sync();

        ping(redis);
        set(redis);
        get(redis);
        del(redis);

        logger.info("Done test: {}", getClass().getName());
    }

    private void ping(final RedisCommands<String, String> redis) {
        final String result = redis.ping();
        if (!"PONG".equals(result)) {
            throw new StackTestRuntimeException(
                    "[Class=RedisApiTest, Test=ping, Message=expected PONG but got " + result + "]");
        }
        logger.info("Redis PING → {}", result);
    }

    private void set(final RedisCommands<String, String> redis) {
        final String result = redis.set(TEST_KEY, TEST_VALUE);
        if (!"OK".equals(result)) {
            throw new StackTestRuntimeException(
                    "[Class=RedisApiTest, Test=set, Message=expected OK but got " + result + "]");
        }
        logger.info("Redis SET {} → {}", TEST_KEY, result);
    }

    private void get(final RedisCommands<String, String> redis) {
        final String result = redis.get(TEST_KEY);
        if (!TEST_VALUE.equals(result)) {
            throw new StackTestRuntimeException(
                    "[Class=RedisApiTest, Test=get, Message=expected " + TEST_VALUE + " but got " + result + "]");
        }
        logger.info("Redis GET {} → {}", TEST_KEY, result);
    }

    private void del(final RedisCommands<String, String> redis) {
        final Long count = redis.del(TEST_KEY);
        if (count == null || count != 1L) {
            throw new StackTestRuntimeException(
                    "[Class=RedisApiTest, Test=del, Message=expected 1 but got " + count + "]");
        }
        logger.info("Redis DEL {} → {}", TEST_KEY, count);
    }
}
