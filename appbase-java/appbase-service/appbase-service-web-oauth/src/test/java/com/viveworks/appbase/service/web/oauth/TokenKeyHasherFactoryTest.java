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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class TokenKeyHasherFactoryTest {

    private static final String RAW_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
    private static final UnaryOperator<String> UNUSED_RESOLVER = ref -> {
        throw new AssertionError("secret resolver must not be consulted: " + ref);
    };

    private static OAuthConfiguration config(TokenKeyHashing hashing, String hmacKeyRef) {
        return new OAuthConfiguration(URI.create("https://issuer.example/jwks"), "https://issuer.example",
                "client-1", "aud", 30L, 300L, hashing, hmacKeyRef);
    }

    @Test
    void defaultsToSha256WhenHashingUnset() {
        TokenKeyHasher hasher = TokenKeyHasherFactory.create(config(null, null), UNUSED_RESOLVER);
        assertInstanceOf(Sha256TokenKeyHasher.class, hasher);
    }

    @Test
    void sha256ModeReturnsBareSha256Hasher() {
        TokenKeyHasher hasher = TokenKeyHasherFactory.create(config(TokenKeyHashing.SHA256, null),
                UNUSED_RESOLVER);
        assertInstanceOf(Sha256TokenKeyHasher.class, hasher);
    }

    @Test
    void hmacModeBuildsKeyedHasherFromResolvedSecret() {
        UnaryOperator<String> resolver = ref -> "vault:" + ref + ":secret";
        TokenKeyHasher hasher = TokenKeyHasherFactory.create(
                config(TokenKeyHashing.HMAC_SHA256, "hmac-key-ref"), resolver);

        assertInstanceOf(HmacSha256TokenKeyHasher.class, hasher);
        // Keyed output must differ from the bare SHA-256 of the same token.
        assertNotEquals(new Sha256TokenKeyHasher().hash(RAW_TOKEN), hasher.hash(RAW_TOKEN));
    }

    @Test
    void hmacResolvesTheConfiguredRef() {
        String[] seen = new String[1];
        UnaryOperator<String> resolver = ref -> {
            seen[0] = ref;
            return "a-secret";
        };
        TokenKeyHasherFactory.create(config(TokenKeyHashing.HMAC_SHA256, "the-ref"), resolver);
        assertEquals("the-ref", seen[0]);
    }

    @Test
    void hmacWithoutKeyRefFailsFast() {
        // Fail at startup when HMAC is selected but no ref is configured — never silently per request.
        assertThrows(IllegalArgumentException.class,
                () -> TokenKeyHasherFactory.create(config(TokenKeyHashing.HMAC_SHA256, null), UNUSED_RESOLVER));
        assertThrows(IllegalArgumentException.class,
                () -> TokenKeyHasherFactory.create(config(TokenKeyHashing.HMAC_SHA256, "  "), UNUSED_RESOLVER));
    }
}
