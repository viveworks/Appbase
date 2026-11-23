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
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AfterIdpCreateUserWithEmailPasswordRequest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AfterIdpCreateUserWithEmailPasswordRequestTest {

    @Test
    void toString_doesNotDiscloseIdpUserTokenOrClientPasswordHash() {
        // AUD-34 (CWE-532, CWE-200): idpUserToken and clientPasswordHash are each
        // credential-bearing (an IdP session token and a client-side password hash,
        // respectively); toString() logged both in plaintext. Both must be redacted with
        // AppbaseUtil.OBFUSCATE, matching the established toString()-redaction convention
        // applied elsewhere on this branch.
        final String idpUserTokenSentinel = "sentinel-idpUserToken-AUD34";
        final String hashSentinel = "sentinel-hash-AUD34";
        final String saltSentinel = "sentinel-salt-AUD34";
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(hashSentinel)
                .salt(saltSentinel)
                .shaRounds(1)
                .build();
        final AfterIdpCreateUserWithEmailPasswordRequest request = new AfterIdpCreateUserWithEmailPasswordRequest();
        request.setIdpUserToken(idpUserTokenSentinel);
        request.setClientPasswordHash(clientPasswordHash);

        final String result = request.toString();

        assertFalse(result.contains(idpUserTokenSentinel), "toString() must not contain idpUserToken's content");
        assertFalse(result.contains(hashSentinel), "toString() must not contain clientPasswordHash's hash content");
        assertFalse(result.contains(saltSentinel), "toString() must not contain clientPasswordHash's salt content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getIdpUserTokenAndGetClientPasswordHash_stillReturnTheRealValues() {
        final String idpUserToken = "sentinel-idpUserToken-AUD34";
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash("sentinel-hash-AUD34")
                .salt("sentinel-salt-AUD34")
                .shaRounds(1)
                .build();
        final AfterIdpCreateUserWithEmailPasswordRequest request = new AfterIdpCreateUserWithEmailPasswordRequest();
        request.setIdpUserToken(idpUserToken);
        request.setClientPasswordHash(clientPasswordHash);

        assertEquals(idpUserToken, request.getIdpUserToken());
        assertEquals(clientPasswordHash, request.getClientPasswordHash());
    }
}
