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

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.web.oauth.AbstractStoreBackedTokenCache;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.ValidatedTokenCodec;
import java.time.Clock;
import java.util.Optional;

/**
 * An Aerospike-backed {@link TokenCache}. The serialized token is written as a single blob bin under a
 * {@link WritePolicy#expiration} equal to the clamped TTL, so Aerospike's own record expiry evicts it no
 * later than the token's {@code exp} — the clamp is applied by {@link AbstractStoreBackedTokenCache}.
 *
 * <p>The stored bytes are trusted on read; a shared or untrusted Aerospike instance must be paired with
 * the keyed HMAC hasher — see the "Store trust" note on {@link AbstractStoreBackedTokenCache}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AerospikeTokenCache extends AbstractStoreBackedTokenCache {

    private static final String OP = "oauth-token-cache";
    private static final String SET = "oauth_token_cache";
    private static final String BIN = "token";

    private final AerospikeClientService client;
    private final String namespace;

    /**
     * @param client    the configured Aerospike client
     * @param namespace the Aerospike namespace the token-cache set lives in (a deployment concern)
     */
    public AerospikeTokenCache(AerospikeClientService client, String namespace) {
        this(client, namespace, new ValidatedTokenCodec(), Clock.systemUTC());
    }

    AerospikeTokenCache(AerospikeClientService client, String namespace, ValidatedTokenCodec codec,
            Clock clock) {
        super(codec, clock);
        this.client = AppbaseUtil.notNull(client, "client");
        this.namespace = AppbaseUtil.notNull(namespace, "namespace");
    }

    @Override
    protected void write(String tokenKey, byte[] value, long ttlSeconds) {
        WritePolicy policy = new WritePolicy();
        policy.expiration = Math.toIntExact(ttlSeconds);
        try {
            client.getAccessor().put(OP, policy, new Key(namespace, SET, tokenKey), new Bin(BIN, value));
        } catch (Exception e) {
            throw new AppbaseRuntimeException("failed to write a token to the Aerospike cache", e);
        }
    }

    @Override
    protected Optional<byte[]> read(String tokenKey) {
        final Record record;
        try {
            record = client.getAccessor().get(OP, new Key(namespace, SET, tokenKey));
        } catch (Exception e) {
            throw new AppbaseRuntimeException("failed to read a token from the Aerospike cache", e);
        }
        if (record == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((byte[]) record.getValue(BIN));
    }
}
