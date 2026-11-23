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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Opt-in keyed {@link TokenKeyHasher}: an HMAC-SHA256 of the raw token under a rotatable server secret.
 *
 * <p>Same speed as {@link Sha256TokenKeyHasher}, with key separation added: a dumped cache is worthless
 * without the server key, and offline correlation across deployments is killed. The secret is resolved
 * from {@code tokenKeyHmacKeyRef}; a missing secret fails fast at construction rather than silently
 * per request.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class HmacSha256TokenKeyHasher implements TokenKeyHasher {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SecretKeySpec key;

    /**
     * @param secret the resolved HMAC secret (from {@code tokenKeyHmacKeyRef}); must be non-blank
     * @throws IllegalArgumentException if {@code secret} is {@code null} or blank
     */
    public HmacSha256TokenKeyHasher(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "HMAC_SHA256 token-key hashing requires a non-blank secret (tokenKeyHmacKeyRef)");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    @Override
    public String hash(String rawToken) {
        return HexFormat.of().formatHex(newMac().doFinal(rawToken.getBytes(StandardCharsets.UTF_8)));
    }

    private Mac newMac() {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException e) {
            // HmacSHA256 is a required algorithm on every conformant JRE; its absence is unrecoverable.
            throw new IllegalStateException("HmacSHA256 not available", e);
        } catch (InvalidKeyException e) {
            // The key is validated non-blank at construction, so this is not reachable in practice.
            throw new IllegalStateException("Invalid HMAC key", e);
        }
    }
}
