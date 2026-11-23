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
 * Unit tests for {@link PasswordHash}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class PasswordHashTest {

    @Test
    void toString_doesNotDiscloseHashOrSalt() {
        // L2-13 (CWE-532, CWE-200): hash and salt are the password-hash material itself;
        // toString() logged both in plaintext. Both must be redacted in toString(), matching
        // the pattern DruidConnectionConfiguration already applies to its own
        // credential-bearing fields.
        final String hashSentinel = "sentinel-passwordhash-hash-L213";
        final String saltSentinel = "sentinel-passwordhash-salt-L213";
        final PasswordHash passwordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(hashSentinel)
                .salt(saltSentinel)
                .shaRounds(1)
                .build();

        final String result = passwordHash.toString();

        assertFalse(result.contains(hashSentinel), "toString() must not contain hash's content");
        assertFalse(result.contains(saltSentinel), "toString() must not contain salt's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getHashAndGetSalt_stillReturnTheRealValues() {
        final String hash = "sentinel-passwordhash-hash-L213";
        final String salt = "sentinel-passwordhash-salt-L213";
        final PasswordHash passwordHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(hash)
                .salt(salt)
                .shaRounds(1)
                .build();

        assertEquals(hash, passwordHash.getHash());
        assertEquals(salt, passwordHash.getSalt());
    }
}
