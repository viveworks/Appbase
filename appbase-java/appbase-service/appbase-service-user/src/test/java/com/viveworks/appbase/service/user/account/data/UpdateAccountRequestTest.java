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
 * Unit tests for {@link UpdateAccountRequest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class UpdateAccountRequestTest {

    @Test
    void toString_doesNotDiscloseClientPasswordHash() {
        // AUD-34 (CWE-532, CWE-200): clientPasswordHash is a client-side password hash;
        // toString() logged it in plaintext. It must be redacted with AppbaseUtil.OBFUSCATE,
        // matching the established toString()-redaction convention applied elsewhere on this
        // branch.
        final String hashSentinel = "sentinel-hash-AUD34";
        final String saltSentinel = "sentinel-salt-AUD34";
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(hashSentinel)
                .salt(saltSentinel)
                .shaRounds(1)
                .build();
        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setClientPasswordHash(clientPasswordHash);

        final String result = request.toString();

        assertFalse(result.contains(hashSentinel), "toString() must not contain clientPasswordHash's hash content");
        assertFalse(result.contains(saltSentinel), "toString() must not contain clientPasswordHash's salt content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getClientPasswordHash_stillReturnsTheRealValue() {
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash("sentinel-hash-AUD34")
                .salt("sentinel-salt-AUD34")
                .shaRounds(1)
                .build();
        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setClientPasswordHash(clientPasswordHash);

        assertEquals(clientPasswordHash, request.getClientPasswordHash());
    }
}
