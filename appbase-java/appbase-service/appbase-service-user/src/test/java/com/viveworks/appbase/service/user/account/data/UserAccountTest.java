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
import com.viveworks.appbase.service.password.PasswordHashes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UserAccount}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class UserAccountTest {

    @Test
    void toString_doesNotDisclosePasswordHashesOrPasswordResetCodeHash() {
        // AUD-34 (CWE-532, CWE-200): passwordHashes (client/server password hashes) and
        // passwordResetCodeHash (a password-reset code hash) are each credential-bearing;
        // toString() logged both in plaintext (or by delegating to a nested type). Both must
        // be redacted DIRECTLY with AppbaseUtil.OBFUSCATE, matching the established
        // toString()-redaction convention applied elsewhere on this branch, rather than
        // relying on PasswordHashes's own (already-fixed) toString().
        final String clientHashSentinel = "sentinel-clientHash-AUD34";
        final String clientSaltSentinel = "sentinel-clientSalt-AUD34";
        final String resetHashSentinel = "sentinel-resetHash-AUD34";
        final String resetSaltSentinel = "sentinel-resetSalt-AUD34";
        final PasswordHashes passwordHashes = PasswordHashes
                .builder()
                .clientPasswordHash(PasswordHash
                        .builder()
                        .hashAlgorithm(HashAlgorithm.SHA512)
                        .hash(clientHashSentinel)
                        .salt(clientSaltSentinel)
                        .shaRounds(1)
                        .build())
                .build();
        final PasswordHash passwordResetCodeHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash(resetHashSentinel)
                .salt(resetSaltSentinel)
                .shaRounds(1)
                .build();
        final UserAccount userAccount = new UserAccount();
        userAccount.setPasswordHashes(passwordHashes);
        userAccount.setPasswordResetCodeHash(passwordResetCodeHash);

        final String result = userAccount.toString();

        assertFalse(result.contains(clientHashSentinel), "toString() must not contain the client hash content");
        assertFalse(result.contains(clientSaltSentinel), "toString() must not contain the client salt content");
        assertFalse(result.contains(resetHashSentinel), "toString() must not contain the reset code hash content");
        assertFalse(result.contains(resetSaltSentinel), "toString() must not contain the reset code salt content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted fields");
    }

    @Test
    void getPasswordHashesAndGetPasswordResetCodeHash_stillReturnTheRealValues() {
        final PasswordHashes passwordHashes = PasswordHashes
                .builder()
                .clientPasswordHash(PasswordHash
                        .builder()
                        .hashAlgorithm(HashAlgorithm.SHA512)
                        .hash("sentinel-clientHash-AUD34")
                        .salt("sentinel-clientSalt-AUD34")
                        .shaRounds(1)
                        .build())
                .build();
        final PasswordHash passwordResetCodeHash = PasswordHash
                .builder()
                .hashAlgorithm(HashAlgorithm.SHA512)
                .hash("sentinel-resetHash-AUD34")
                .salt("sentinel-resetSalt-AUD34")
                .shaRounds(1)
                .build();
        final UserAccount userAccount = new UserAccount();
        userAccount.setPasswordHashes(passwordHashes);
        userAccount.setPasswordResetCodeHash(passwordResetCodeHash);

        assertEquals(passwordHashes, userAccount.getPasswordHashes());
        assertEquals(passwordResetCodeHash, userAccount.getPasswordResetCodeHash());
    }
}
