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

package com.viveworks.appbase.configwizard.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for {@link BrowserToken}. The per-run token is the value an attacker page must not be
 * able to guess (A10-01, internal work item 1449), so these assert it is unpredictable (distinct per call) and carries
 * enough entropy, encoded URL-safely so it can ride in a cookie and header unescaped.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class BrowserTokenTest {

    @Test
    void newTokenIsNotBlank() {
        assertTrue(BrowserToken.newToken() != null && !BrowserToken.newToken().isBlank(),
                "a per-run token must be a non-blank value");
    }

    @Test
    void twoTokensDiffer() {
        assertNotEquals(BrowserToken.newToken(), BrowserToken.newToken(),
                "each per-run token must be freshly randomised, not a constant");
    }

    @Test
    void newTokenCarriesAtLeast256BitsEncoded() {
        // 32 random bytes URL-safe-base64-encoded without padding is 43 chars.
        assertTrue(BrowserToken.newToken().length() >= 43,
                "the token must encode at least 256 bits of entropy");
    }

    @Test
    void newTokenIsUrlSafe() {
        assertTrue(BrowserToken.newToken().matches("[A-Za-z0-9_-]+"),
                "the token must be URL-safe so it rides in a cookie/header unescaped");
    }
}
