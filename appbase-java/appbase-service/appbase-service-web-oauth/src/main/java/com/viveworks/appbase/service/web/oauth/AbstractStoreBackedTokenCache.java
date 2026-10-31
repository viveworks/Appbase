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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Base for the external, byte-oriented {@link TokenCache} backends (Aerospike, Redis): it owns the two
 * behaviours those backends must share and leaves each subclass only the store round trip.
 *
 * <p><b>The store-TTL clamp is the single mechanism that stops an expired token being served.</b> A
 * validated token is written under a TTL of {@code min(requestedTtl, exp − now)}, so the backend's own
 * expiry eviction removes it no later than the token's {@code exp}; a non-positive result is not cached
 * at all. There is deliberately no second expiry check on {@link #get(String)} — that would mask a
 * regression in the clamp, and the clamp is what the N03 mutation test breaks to prove the guard works.
 *
 * <p><b>Store trust.</b> A cache hit is served without re-validation (see {@code OAuthFilter}), and the
 * stored bytes carry no integrity tag — so anyone who can <em>write</em> to the store can forge a
 * {@link ValidatedToken}, and with the default unkeyed {@link Sha256TokenKeyHasher} the cache key is
 * computable from public configuration, so the entry can be planted for an attacker-chosen token. Treat
 * the backing store as a trusted, network-isolated, authenticated datastore. Where it is shared or
 * otherwise untrusted, configure the keyed {@link HmacSha256TokenKeyHasher}
 * ({@code tokenKeyHashing=HMAC_SHA256}), which makes the key unpredictable without the server secret. An
 * entry that cannot be decoded is treated as a miss, never surfaced as an error on the auth path.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public abstract class AbstractStoreBackedTokenCache implements TokenCache {

    private final ValidatedTokenCodec codec;
    private final Clock clock;

    /**
     * @param codec serializes the token to and from the bytes the store holds
     * @param clock time source for the expiry clamp; {@link Clock#systemUTC()} in production, a fixed
     *              clock in tests
     */
    protected AbstractStoreBackedTokenCache(ValidatedTokenCodec codec, Clock clock) {
        this.codec = codec;
        this.clock = clock;
    }

    @Override
    public final void put(String tokenKey, ValidatedToken token, Duration ttl) {
        long ttlSeconds = clampedTtlSeconds(ttl, token.expiresAt(), clock.instant());
        if (ttlSeconds <= 0) {
            // The token is already at or past its expiry within the clock-skew margin: caching it would
            // either be pointless or serve it past exp. Skip the store entirely.
            return;
        }
        write(tokenKey, codec.encode(token), ttlSeconds);
    }

    @Override
    public final Optional<ValidatedToken> get(String tokenKey) {
        Optional<byte[]> stored = read(tokenKey);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(codec.decode(stored.get()));
        } catch (RuntimeException e) {
            // A stored value we cannot decode — corruption, a version skew, or a poisoned entry — must
            // never break the auth path. Treat it as a miss so the caller re-validates the raw token
            // rather than letting the exception propagate out of the filter.
            return Optional.empty();
        }
    }

    /**
     * The effective store TTL: never longer than the token has left to live.
     *
     * @return {@code min(requested, exp − now)} in whole seconds; zero or negative means "do not cache"
     */
    static long clampedTtlSeconds(Duration requested, Instant expiresAt, Instant now) {
        return Math.min(requested.getSeconds(), Duration.between(now, expiresAt).getSeconds());
    }

    /**
     * Stores {@code value} under {@code tokenKey} for at most {@code ttlSeconds}, relying on the
     * backend's native expiry so the entry is gone by then.
     */
    protected abstract void write(String tokenKey, byte[] value, long ttlSeconds);

    /**
     * @return the stored bytes, or empty on a miss or once the backend has expired the entry
     */
    protected abstract Optional<byte[]> read(String tokenKey);
}
