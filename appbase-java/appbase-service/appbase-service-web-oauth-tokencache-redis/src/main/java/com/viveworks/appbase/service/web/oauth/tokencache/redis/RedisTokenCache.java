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

import com.viveworks.appbase.client.service.redis.RedisClientService;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.web.oauth.AbstractStoreBackedTokenCache;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.ValidatedTokenCodec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;

/**
 * A Redis-backed {@link TokenCache}. The serialized token is stored as a UTF-8 string under a
 * {@code SETEX} whose expiry is the clamped TTL, so Redis evicts it no later than the token's
 * {@code exp} — the clamp is applied by {@link AbstractStoreBackedTokenCache}. Keys are namespaced with
 * a fixed prefix to keep the token cache clear of other data in a shared Redis.
 *
 * <p>The stored bytes are trusted on read; a shared or untrusted Redis instance must be paired with the
 * keyed HMAC hasher — see the "Store trust" note on {@link AbstractStoreBackedTokenCache}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class RedisTokenCache extends AbstractStoreBackedTokenCache {

    private static final String OP = "oauth-token-cache";
    private static final String KEY_PREFIX = "oauth:token:";

    private final RedisClientService client;

    /**
     * @param client the configured Redis client
     */
    public RedisTokenCache(RedisClientService client) {
        this(client, new ValidatedTokenCodec(), Clock.systemUTC());
    }

    RedisTokenCache(RedisClientService client, ValidatedTokenCodec codec, Clock clock) {
        super(codec, clock);
        this.client = AppbaseUtil.notNull(client, "client");
    }

    @Override
    protected void write(String tokenKey, byte[] value, long ttlSeconds) {
        String serialized = new String(value, StandardCharsets.UTF_8);
        try {
            client.getAccessor().command(OP, commands -> commands.setex(KEY_PREFIX + tokenKey,
                    ttlSeconds, serialized));
        } catch (Exception e) {
            throw new AppbaseRuntimeException("failed to write a token to the Redis cache", e);
        }
    }

    @Override
    protected Optional<byte[]> read(String tokenKey) {
        final String serialized;
        try {
            serialized = client.getAccessor().command(OP, commands -> commands.get(KEY_PREFIX + tokenKey));
        } catch (Exception e) {
            throw new AppbaseRuntimeException("failed to read a token from the Redis cache", e);
        }
        if (serialized == null) {
            return Optional.empty();
        }
        return Optional.of(serialized.getBytes(StandardCharsets.UTF_8));
    }
}
