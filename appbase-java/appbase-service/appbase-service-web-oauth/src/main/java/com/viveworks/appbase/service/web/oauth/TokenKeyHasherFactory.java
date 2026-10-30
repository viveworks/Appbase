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

import java.util.function.UnaryOperator;

/**
 * Builds the configured {@link TokenKeyHasher} once, at startup.
 *
 * <p>{@link TokenKeyHashing#SHA256} (the default when unset) yields a {@link Sha256TokenKeyHasher};
 * {@link TokenKeyHashing#HMAC_SHA256} yields an {@link HmacSha256TokenKeyHasher} keyed with the secret
 * that {@code secretResolver} resolves from {@link OAuthConfiguration#tokenKeyHmacKeyRef()}. A missing
 * ref under HMAC fails fast here rather than silently per request.
 *
 * <p>The choice also hardens an external, shared {@link TokenCache} store (Aerospike/Redis): under
 * {@code HMAC_SHA256} the cache key is unpredictable without the server secret, so a party that can write
 * to the store cannot plant a forged entry under a key it computed. The default unkeyed {@code SHA256}
 * cannot prevent that, so it assumes a trusted, isolated store — see {@code AbstractStoreBackedTokenCache}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class TokenKeyHasherFactory {

    private TokenKeyHasherFactory() {
    }

    /**
     * @param configuration  the OAuth configuration
     * @param secretResolver resolves a config-secret reference to its secret value
     * @return the hasher selected by {@link OAuthConfiguration#tokenKeyHashing()}
     * @throws IllegalArgumentException if HMAC is selected without a {@code tokenKeyHmacKeyRef}
     */
    public static TokenKeyHasher create(OAuthConfiguration configuration,
            UnaryOperator<String> secretResolver) {
        TokenKeyHashing mode = configuration.tokenKeyHashing() == null
                ? TokenKeyHashing.SHA256
                : configuration.tokenKeyHashing();
        return switch (mode) {
            case SHA256 -> new Sha256TokenKeyHasher();
            case HMAC_SHA256 -> {
                String ref = configuration.tokenKeyHmacKeyRef();
                if (ref == null || ref.isBlank()) {
                    throw new IllegalArgumentException(
                            "tokenKeyHashing=HMAC_SHA256 requires a tokenKeyHmacKeyRef");
                }
                yield new HmacSha256TokenKeyHasher(secretResolver.apply(ref));
            }
        };
    }
}
