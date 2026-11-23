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

package com.viveworks.appbase.service.web.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Default {@link TokenKeyHasher}: a bare SHA-256 digest of the raw token, hex-encoded.
 *
 * <p>A bearer token is a high-entropy secret, so a fast cryptographic hash is preimage-safe on the
 * authentication hot path; a slow password KDF would only add latency for no gain. Collision resistance
 * is a correctness requirement here — a collision would serve one caller's validated token to another —
 * which is why a non-cryptographic hash is not an option.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class Sha256TokenKeyHasher implements TokenKeyHasher {

    @Override
    public String hash(String rawToken) {
        byte[] digest = newSha256().digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a required algorithm on every conformant JRE; its absence is unrecoverable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
