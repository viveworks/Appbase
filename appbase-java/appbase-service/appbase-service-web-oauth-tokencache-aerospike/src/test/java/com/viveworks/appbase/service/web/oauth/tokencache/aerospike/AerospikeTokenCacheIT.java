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

package com.viveworks.appbase.service.web.oauth.tokencache.aerospike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.client.service.aerospike.AerospikeClientTestHelper;
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
 * Real-backend integration tests for {@link AerospikeTokenCache}.
 *
 * <p>Requires a running Aerospike instance; enable with
 * {@code -DAerospikeClientServiceTests.enabled=true}. The offline mutation-verification of the TTL
 * clamp lives in {@code AbstractStoreBackedTokenCacheTest}; this proves the clamp reaches Aerospike's
 * own record expiry.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "AerospikeClientServiceTests.enabled", matches = "true")
class AerospikeTokenCacheIT {

    // The checked-in Aerospike lifecycle provisions `dev`, which is also the session-store default.
    private static final String NAMESPACE = "dev";

    private static AerospikeTokenCache cache;

    @BeforeAll
    static void setUp() {
        cache = new AerospikeTokenCache(AerospikeClientTestHelper.defaultAerospikeClientService(), NAMESPACE);
    }

    private static ValidatedToken tokenExpiringAt(Instant expiresAt) {
        return new ValidatedToken("sub-1", "https://issuer.example", Set.of("api"),
                expiresAt, Map.of("scope", "read"), "hash-key");
    }

    @Test
    void putThenGetRoundTripsThroughAerospike() {
        // The shared byte codec deliberately stores OAuth expiry as epoch milliseconds (the precision
        // supplied by Nimbus/Date). Use a representable value so this test isolates the Aerospike round trip.
        ValidatedToken token = tokenExpiringAt(Instant.now().plusSeconds(3600)
                .truncatedTo(ChronoUnit.MILLIS));
        cache.put("it-roundtrip", token, Duration.ofSeconds(300));

        assertEquals(Optional.of(token), cache.get("it-roundtrip"));
    }

    @Test
    void clampToExpiryEvictsSoAnExpiredTokenIsNotServed() throws InterruptedException {
        // exp is 2s away, TTL is an hour: the clamp caps the Aerospike record TTL at ~2s.
        cache.put("it-clamp", tokenExpiringAt(Instant.now().plusSeconds(2)), Duration.ofHours(1));
        assertTrue(cache.get("it-clamp").isPresent(), "cached immediately after put");

        Thread.sleep(3000);

        assertTrue(cache.get("it-clamp").isEmpty(), "Aerospike must have evicted the entry by the token's exp");
    }
}
