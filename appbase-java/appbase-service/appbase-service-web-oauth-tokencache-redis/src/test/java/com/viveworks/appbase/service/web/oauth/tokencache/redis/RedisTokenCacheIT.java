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

package com.viveworks.appbase.service.web.oauth.tokencache.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.client.service.redis.RedisClientServiceTestHelper;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Real-backend integration tests for {@link RedisTokenCache}.
 *
 * <p>Requires a running Redis instance (see {@code infra/docker/redis/}); enable with
 * {@code -DRedisClientServiceTests.enabled=true}. The offline mutation-verification of the TTL clamp
 * lives in {@code AbstractStoreBackedTokenCacheTest}; this proves the clamp reaches Redis's own expiry.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "RedisClientServiceTests.enabled", matches = "true")
class RedisTokenCacheIT {

    private static RedisTokenCache cache;

    @BeforeAll
    static void setUp() {
        cache = new RedisTokenCache(RedisClientServiceTestHelper.defaultRedisClientService());
    }

    private static ValidatedToken tokenExpiringAt(Instant expiresAt) {
        return new ValidatedToken("sub-1", "https://issuer.example", Set.of("api"),
                expiresAt, Map.of("scope", "read"), "hash-key");
    }

    @Test
    void putThenGetRoundTripsThroughRedis() {
        // The shared byte codec deliberately stores OAuth expiry as epoch milliseconds (the precision
        // supplied by Nimbus/Date). Use a representable value so this test isolates the Redis round trip.
        ValidatedToken token = tokenExpiringAt(Instant.now().plusSeconds(3600)
                .truncatedTo(ChronoUnit.MILLIS));
        cache.put("it-roundtrip", token, Duration.ofSeconds(300));

        assertEquals(Optional.of(token), cache.get("it-roundtrip"));
    }

    @Test
    void clampToExpiryEvictsSoAnExpiredTokenIsNotServed() throws InterruptedException {
        // exp is 2s away, TTL is an hour: the clamp caps Redis's SETEX at ~2s.
        cache.put("it-clamp", tokenExpiringAt(Instant.now().plusSeconds(2)), Duration.ofHours(1));
        assertTrue(cache.get("it-clamp").isPresent(), "cached immediately after put");

        Thread.sleep(3000);

        assertTrue(cache.get("it-clamp").isEmpty(), "Redis must have evicted the entry by the token's exp");
    }
}
