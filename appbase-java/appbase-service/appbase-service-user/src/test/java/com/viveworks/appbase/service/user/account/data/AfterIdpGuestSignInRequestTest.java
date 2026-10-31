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

package com.viveworks.appbase.service.user.account.data;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AfterIdpGuestSignInRequest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AfterIdpGuestSignInRequestTest {

    @Test
    void toString_doesNotDiscloseIdpGuestUserToken() {
        // AUD-34 (CWE-532, CWE-200): idpGuestUserToken is an IdP session token for a guest
        // sign-in; toString() logged it in plaintext. It must be redacted with
        // AppbaseUtil.OBFUSCATE, matching the established toString()-redaction convention
        // applied elsewhere on this branch.
        final String idpGuestUserTokenSentinel = "sentinel-idpGuestUserToken-AUD34";
        final AfterIdpGuestSignInRequest request = new AfterIdpGuestSignInRequest();
        request.setIdpGuestUserToken(idpGuestUserTokenSentinel);

        final String result = request.toString();

        assertFalse(result.contains(idpGuestUserTokenSentinel), "toString() must not contain idpGuestUserToken's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getIdpGuestUserToken_stillReturnsTheRealValue() {
        final String idpGuestUserToken = "sentinel-idpGuestUserToken-AUD34";
        final AfterIdpGuestSignInRequest request = new AfterIdpGuestSignInRequest();
        request.setIdpGuestUserToken(idpGuestUserToken);

        assertEquals(idpGuestUserToken, request.getIdpGuestUserToken());
    }
}
