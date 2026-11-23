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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class HmacSha256TokenKeyHasherTest {

    private static final String RAW_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";

    @Test
    void differentKeysProduceDifferentKeysForSameToken() {
        // Key separation is the point: a cache dumped without the server key is uncorrelatable.
        // Mutation: ignore the key in hash() and this goes red.
        String a = new HmacSha256TokenKeyHasher("server-key-one").hash(RAW_TOKEN);
        String b = new HmacSha256TokenKeyHasher("server-key-two").hash(RAW_TOKEN);
        assertNotEquals(a, b);
    }

    @Test
    void sameKeyIsStableForSameToken() {
        String a = new HmacSha256TokenKeyHasher("server-key").hash(RAW_TOKEN);
        String b = new HmacSha256TokenKeyHasher("server-key").hash(RAW_TOKEN);
        assertEquals(a, b);
    }

    @Test
    void keyIsHexEncodedSha256() {
        assertTrue(new HmacSha256TokenKeyHasher("server-key").hash(RAW_TOKEN).matches("[0-9a-f]{64}"));
    }

    @Test
    void blankSecretFailsFastAtConstruction() {
        // A missing tokenKeyHmacKeyRef must fail at startup, not silently per request.
        assertThrows(IllegalArgumentException.class, () -> new HmacSha256TokenKeyHasher(""));
        assertThrows(IllegalArgumentException.class, () -> new HmacSha256TokenKeyHasher("   "));
        assertThrows(IllegalArgumentException.class, () -> new HmacSha256TokenKeyHasher(null));
    }

    @Test
    void keyedDigestDiffersFromBareSha256() {
        String hmac = new HmacSha256TokenKeyHasher("server-key").hash(RAW_TOKEN);
        String bare = new Sha256TokenKeyHasher().hash(RAW_TOKEN);
        assertNotEquals(bare, hmac);
    }
}
