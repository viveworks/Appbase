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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.Optional;

/**
 * The built-in in-memory {@link TokenCache}, backed by Caffeine, and the {@link java.util.ServiceLoader}
 * default. Each entry lives for exactly the {@link Duration} passed to {@link #put}, so a token drops out
 * of the cache no later than its own expiry.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class CaffeineTokenCache implements TokenCache {

    private final Cache<String, ValidatedToken> cache;
    private final Policy.VarExpiration<String, ValidatedToken> expiry;

    /** Creates a cache on the system clock; the no-arg constructor the {@code ServiceLoader} calls. */
    public CaffeineTokenCache() {
        this(Ticker.systemTicker());
    }

    /**
     * @param ticker the time source; a hand-advanced ticker makes TTL eviction testable without sleeping
     */
    CaffeineTokenCache(Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfter(new Expiry<String, ValidatedToken>() {
                    @Override
                    public long expireAfterCreate(String key, ValidatedToken value, long currentTime) {
                        // Entries are only ever added through put(ttl), which sets an explicit duration.
                        return Long.MAX_VALUE;
                    }

                    @Override
                    public long expireAfterUpdate(String key, ValidatedToken value, long currentTime,
                            long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, ValidatedToken value, long currentTime,
                            long currentDuration) {
                        // Reading a token never extends its life beyond the original ttl.
                        return currentDuration;
                    }
                })
                .build();
        this.expiry = cache.policy().expireVariably().orElseThrow(
                () -> new IllegalStateException("variable expiration was not enabled on the cache"));
    }

    @Override
    public Optional<ValidatedToken> get(String tokenKey) {
        return Optional.ofNullable(cache.getIfPresent(tokenKey));
    }

    @Override
    public void put(String tokenKey, ValidatedToken token, Duration ttl) {
        expiry.put(tokenKey, token, ttl);
    }
}
