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

package com.viveworks.appbase.service.web.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class CaffeineTokenCacheTest {

    private static ValidatedToken sample(String tokenKey) {
        return new ValidatedToken("sub-1", "https://issuer.example", Set.of("api"),
                Instant.parse("2030-01-01T00:00:00Z"), Map.of("scope", "read"), tokenKey);
    }

    @Test
    void missReturnsEmpty() {
        assertTrue(new CaffeineTokenCache().get("absent").isEmpty());
    }

    @Test
    void putThenGetRoundTrips() {
        TokenCache cache = new CaffeineTokenCache();
        ValidatedToken token = sample("k1");
        cache.put("k1", token, Duration.ofMinutes(5));
        assertEquals(Optional.of(token), cache.get("k1"));
    }

    @Test
    void entryExpiresAfterItsOwnTtl() {
        // Per-entry TTL: advancing a controllable clock past the entry's ttl evicts it.
        // Mutation: ignore the ttl on put() and this goes red.
        MutableTicker ticker = new MutableTicker();
        CaffeineTokenCache cache = new CaffeineTokenCache(ticker);
        cache.put("k1", sample("k1"), Duration.ofSeconds(30));
        assertEquals(Optional.of(sample("k1")), cache.get("k1"));

        ticker.advance(Duration.ofSeconds(31));
        assertTrue(cache.get("k1").isEmpty());
    }

    /** A hand-advanced Caffeine {@link Ticker} so TTL eviction is testable without sleeping. */
    private static final class MutableTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration by) {
            nanos.addAndGet(by.toNanos());
        }
    }
}
