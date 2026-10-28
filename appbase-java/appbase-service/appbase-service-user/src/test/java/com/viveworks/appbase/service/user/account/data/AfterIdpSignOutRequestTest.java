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
 * Unit tests for {@link AfterIdpSignOutRequest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AfterIdpSignOutRequestTest {

    @Test
    void toString_doesNotDiscloseIdpUserToken() {
        // AUD-34 (CWE-532, CWE-200): idpUserToken is an IdP session token being signed out;
        // toString() logged it in plaintext. It must be redacted with AppbaseUtil.OBFUSCATE,
        // matching the established toString()-redaction convention applied elsewhere on this
        // branch.
        final String idpUserTokenSentinel = "sentinel-idpUserToken-AUD34";
        final AfterIdpSignOutRequest request = new AfterIdpSignOutRequest();
        request.setIdpUserToken(idpUserTokenSentinel);

        final String result = request.toString();

        assertFalse(result.contains(idpUserTokenSentinel), "toString() must not contain idpUserToken's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getIdpUserToken_stillReturnsTheRealValue() {
        final String idpUserToken = "sentinel-idpUserToken-AUD34";
        final AfterIdpSignOutRequest request = new AfterIdpSignOutRequest();
        request.setIdpUserToken(idpUserToken);

        assertEquals(idpUserToken, request.getIdpUserToken());
    }
}
