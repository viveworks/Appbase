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

package com.viveworks.appbase.service.web.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behaviour shared by every store-backed {@link TokenCache}, exercised through an in-memory store that
 * models a backend's native TTL eviction.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractStoreBackedTokenCacheTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final InMemoryStoreCache cache = new InMemoryStoreCache(new ValidatedTokenCodec(), clock);

    private static ValidatedToken tokenExpiringAt(Instant expiresAt) {
        return new ValidatedToken("sub-1", "https://issuer.example", Set.of("api"),
                expiresAt, Map.of("scope", "read"), "hash-key");
    }

    @Test
    void clampsTtlToTokenExpirySoAnExpiredTokenIsNeverServed() {
        // exp is 30s away but the caller asks for a 1h TTL. The clamp must cap the store TTL at 30s so
        // the backend evicts the entry by exp.
        // Mutation: store the requested TTL unclamped -> the entry survives to 1h -> get() serves a
        // token that is already 1s past exp -> red.
        cache.put("k", tokenExpiringAt(NOW.plusSeconds(30)), Duration.ofHours(1));

        clock.advance(Duration.ofSeconds(31));

        assertTrue(cache.get("k").isEmpty(), "an expired token must never be served");
    }

    @Test
    void honoursTheRequestedTtlWhenItIsShorterThanExpiry() {
        // exp is an hour away, TTL is 30s: min() picks the requested TTL, so the entry is gone at 31s.
        cache.put("k", tokenExpiringAt(NOW.plusSeconds(3600)), Duration.ofSeconds(30));

        clock.advance(Duration.ofSeconds(31));

        assertTrue(cache.get("k").isEmpty());
    }

    @Test
    void servesTheTokenWithinTheClampedTtl() {
        ValidatedToken token = tokenExpiringAt(NOW.plusSeconds(3600));
        cache.put("k", token, Duration.ofSeconds(300));

        clock.advance(Duration.ofSeconds(299));

        assertEquals(Optional.of(token), cache.get("k"));
    }

    @Test
    void doesNotCacheWhenTheClampedTtlIsNonPositive() {
        // The token is already at exp within the clock-skew margin: exp - now <= 0, so nothing is stored.
        cache.put("k", tokenExpiringAt(NOW), Duration.ofHours(1));

        assertTrue(cache.get("k").isEmpty());
        assertEquals(0, cache.storeSize(), "a non-positive TTL must not write to the store");
    }

    @Test
    void aStoredValueThatCannotBeDecodedIsTreatedAsAMiss() {
        // A corrupted, version-skewed, or poisoned entry must not break the auth path: get() returns a
        // miss so the caller re-validates the raw token, instead of throwing out of the filter.
        // Mutation: decode the stored bytes without catching the failure and this throws -> red.
        cache.seedRaw("k", "}{ not valid codec output".getBytes(StandardCharsets.UTF_8));

        assertTrue(cache.get("k").isEmpty());
    }

    /** An in-memory {@link TokenCache} whose store expires entries at write-time-plus-TTL, like a real backend. */
    private static final class InMemoryStoreCache extends AbstractStoreBackedTokenCache {

        private record Entry(byte[] value, Instant expiresAt) {}

        private final Clock clock;
        private final Map<String, Entry> store = new HashMap<>();

        InMemoryStoreCache(ValidatedTokenCodec codec, Clock clock) {
            super(codec, clock);
            this.clock = clock;
        }

        @Override
        protected void write(String tokenKey, byte[] value, long ttlSeconds) {
            store.put(tokenKey, new Entry(value, clock.instant().plusSeconds(ttlSeconds)));
        }

        @Override
        protected Optional<byte[]> read(String tokenKey) {
            Entry entry = store.get(tokenKey);
            if (entry == null || !clock.instant().isBefore(entry.expiresAt())) {
                return Optional.empty();
            }
            return Optional.of(entry.value());
        }

        int storeSize() {
            return store.size();
        }

        /** Seeds a raw value directly, bypassing the codec — models a corrupted or poisoned store entry. */
        void seedRaw(String tokenKey, byte[] value) {
            store.put(tokenKey, new Entry(value, clock.instant().plus(Duration.ofDays(1))));
        }
    }

    /** A hand-advanced {@link Clock} so the TTL clamp is testable without sleeping. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            this.now = this.now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
