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

package com.viveworks.appbase.service.password;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PasswordHashes}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class PasswordHashesTest {

    @Test
    void toString_doesNotDiscloseClientOrServerPasswordHash() {
        // L2-13 (CWE-532, CWE-200): clientPasswordHash and serverPasswordHash are each a
        // PasswordHash, which itself chains into its own toString() carrying hash/salt material.
        // toString() logged both fields by delegating to their toString(), disclosing that
        // material. Both must be redacted DIRECTLY in PasswordHashes.toString() with
        // AppbaseUtil.OBFUSCATE — matching the pattern
        // DefaultFirebaseAppClientIntegrationServiceConfiguration.toString() applies for its
        // googleCredentialsConfiguration field (printing OBFUSCATE rather than delegating to the
        // nested object's own toString()) — for defense-in-depth independent of whether
        // PasswordHash.toString() itself is also fixed. serverPasswordHash carries @JsonIgnore,
        // which affects JSON serialization only and does nothing for toString() — exactly why it
        // still leaked before this fix.
        final String hashSentinel = "sentinel-passwordhashes-hash-L213";
        final String saltSentinel = "sentinel-passwordhashes-salt-L213";
        final PasswordHash passwordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(hashSentinel)
                .salt(saltSentinel)
                .shaRounds(1)
                .build();
        final PasswordHashes passwordHashes = PasswordHashes
                .builder()
                .clientPasswordHash(passwordHash)
                .serverPasswordHash(passwordHash)
                .build();

        final String result = passwordHashes.toString();

        assertFalse(result.contains(hashSentinel), "toString() must not contain hash's content");
        assertFalse(result.contains(saltSentinel), "toString() must not contain salt's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
        // Defense-in-depth check: the two fields must be redacted DIRECTLY in
        // PasswordHashes.toString() (not merely by delegating to the nested PasswordHash's own,
        // separately-fixed toString()). PasswordHash.toString() always renders as "PasswordHash{...}",
        // so that substring's absence here proves PasswordHashes.toString() is not delegating to it
        // — this holds regardless of whether PasswordHash.toString() itself is also fixed.
        assertFalse(
                result.contains("PasswordHash{"),
                "toString() must not delegate to the nested PasswordHash's own toString()");
    }

    @Test
    void getClientPasswordHashAndGetServerPasswordHash_stillReturnTheRealValues() {
        final PasswordHash clientPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash("sentinel-passwordhashes-hash-client-L213")
                .salt("sentinel-passwordhashes-salt-client-L213")
                .shaRounds(1)
                .build();
        final PasswordHash serverPasswordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash("sentinel-passwordhashes-hash-server-L213")
                .salt("sentinel-passwordhashes-salt-server-L213")
                .shaRounds(1)
                .build();
        final PasswordHashes passwordHashes = PasswordHashes
                .builder()
                .clientPasswordHash(clientPasswordHash)
                .serverPasswordHash(serverPasswordHash)
                .build();

        assertEquals(clientPasswordHash, passwordHashes.getClientPasswordHash());
        assertEquals(serverPasswordHash, passwordHashes.getServerPasswordHash());
    }
}
