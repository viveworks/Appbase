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

package com.viveworks.appbase.service.user.account.data;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AfterIdpRefreshAuthenticationTokenRequest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AfterIdpRefreshAuthenticationTokenRequestTest {

    @Test
    void toString_doesNotDiscloseNewIdpUserToken() {
        // AUD-34 (CWE-532, CWE-200): newIdpUserToken is a freshly-issued IdP session token;
        // toString() logged it in plaintext. It must be redacted with AppbaseUtil.OBFUSCATE,
        // matching the established toString()-redaction convention applied elsewhere on this
        // branch.
        final String newIdpUserTokenSentinel = "sentinel-newIdpUserToken-AUD34";
        final AfterIdpRefreshAuthenticationTokenRequest request = new AfterIdpRefreshAuthenticationTokenRequest();
        request.setNewIdpUserToken(newIdpUserTokenSentinel);

        final String result = request.toString();

        assertFalse(result.contains(newIdpUserTokenSentinel), "toString() must not contain newIdpUserToken's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getNewIdpUserToken_stillReturnsTheRealValue() {
        final String newIdpUserToken = "sentinel-newIdpUserToken-AUD34";
        final AfterIdpRefreshAuthenticationTokenRequest request = new AfterIdpRefreshAuthenticationTokenRequest();
        request.setNewIdpUserToken(newIdpUserToken);

        assertEquals(newIdpUserToken, request.getNewIdpUserToken());
    }
}
