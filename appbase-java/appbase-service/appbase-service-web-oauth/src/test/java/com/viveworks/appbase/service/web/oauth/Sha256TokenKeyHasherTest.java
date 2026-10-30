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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class Sha256TokenKeyHasherTest {

    private static final String RAW_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";

    private final TokenKeyHasher hasher = new Sha256TokenKeyHasher();

    @Test
    void producesSameKeyForSameToken() {
        assertEquals(hasher.hash(RAW_TOKEN), hasher.hash(RAW_TOKEN));
    }

    @Test
    void keyIsNotTheRawToken() {
        // The cache key must never be the replayable credential itself.
        // Mutation: return rawToken from hash() and this goes red.
        assertNotEquals(RAW_TOKEN, hasher.hash(RAW_TOKEN));
    }

    @Test
    void keyIsHexEncodedSha256() {
        // SHA-256 is 32 bytes -> 64 lowercase hex characters.
        assertTrue(hasher.hash(RAW_TOKEN).matches("[0-9a-f]{64}"),
                "expected 64 lowercase hex chars, got: " + hasher.hash(RAW_TOKEN));
    }

    @Test
    void differentTokensProduceDifferentKeys() {
        assertNotEquals(hasher.hash(RAW_TOKEN), hasher.hash(RAW_TOKEN + "x"));
    }
}
