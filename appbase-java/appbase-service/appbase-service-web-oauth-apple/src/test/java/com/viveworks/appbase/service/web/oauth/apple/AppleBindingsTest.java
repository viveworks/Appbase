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

package com.viveworks.appbase.service.web.oauth.apple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Apple has no userinfo endpoint: the profile is exactly the minimum-scope claims already carried by the
 * validated id_token — {@code sub}, {@code email}, {@code email_verified} (spec §11 assumption 1). Apple
 * serialises {@code email_verified} as either a JSON boolean or the string {@code "true"}, so the binder
 * must read both.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppleBindingsTest {

    private static ValidatedToken tokenWith(Map<String, Object> claims) {
        return new ValidatedToken(
                (String) claims.get("sub"),
                "https://appleid.apple.com",
                Set.of("com.example.service"),
                Instant.parse("2026-08-13T13:00:00Z"),
                claims,
                "unused-token-key");
    }

    @Test
    void bindsMinimumScopeProfileFromABooleanEmailVerifiedClaim() {
        ValidatedToken token = tokenWith(Map.of(
                "sub", "001234.fedcba9876543210.1200",
                "email", "ada@privaterelay.appleid.com",
                "email_verified", true));

        AppleUserProfile profile = AppleProfileBinding.from(token);

        assertEquals("001234.fedcba9876543210.1200", profile.subject());
        assertEquals("ada@privaterelay.appleid.com", profile.email());
        assertTrue(profile.emailVerified());
    }

    @Test
    void readsEmailVerifiedWhenAppleSendsItAsTheStringTrue() {
        ValidatedToken token = tokenWith(Map.of(
                "sub", "001234.fedcba9876543210.1200",
                "email", "ada@privaterelay.appleid.com",
                "email_verified", "true"));

        AppleUserProfile profile = AppleProfileBinding.from(token);

        assertTrue(profile.emailVerified());
    }

    @Test
    void treatsAMissingEmailVerifiedClaimAsUnverified() {
        ValidatedToken token = tokenWith(Map.of(
                "sub", "001234.fedcba9876543210.1200",
                "email", "ada@privaterelay.appleid.com"));

        AppleUserProfile profile = AppleProfileBinding.from(token);

        assertFalse(profile.emailVerified());
    }
}
