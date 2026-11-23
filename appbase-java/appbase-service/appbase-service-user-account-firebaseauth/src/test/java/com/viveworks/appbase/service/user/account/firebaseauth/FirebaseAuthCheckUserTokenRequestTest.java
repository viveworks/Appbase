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

package com.viveworks.appbase.service.user.account.firebaseauth;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FirebaseAuthCheckUserTokenRequest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class FirebaseAuthCheckUserTokenRequestTest {

    @Test
    void toString_doesNotDiscloseToken() {
        // AUD-34 (CWE-532, CWE-200): token is a Firebase auth token being checked;
        // toString() logged it in plaintext. It must be redacted with AppbaseUtil.OBFUSCATE,
        // matching the established toString()-redaction convention applied elsewhere on this
        // branch.
        final String tokenSentinel = "sentinel-token-AUD34";
        final FirebaseAuthCheckUserTokenRequest request = new FirebaseAuthCheckUserTokenRequest();
        request.setToken(tokenSentinel);

        final String result = request.toString();

        assertFalse(result.contains(tokenSentinel), "toString() must not contain token's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getToken_stillReturnsTheRealValue() {
        final String token = "sentinel-token-AUD34";
        final FirebaseAuthCheckUserTokenRequest request = new FirebaseAuthCheckUserTokenRequest();
        request.setToken(token);

        assertEquals(token, request.getToken());
    }
}
