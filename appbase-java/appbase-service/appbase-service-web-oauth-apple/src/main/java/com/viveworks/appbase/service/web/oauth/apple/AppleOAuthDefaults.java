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

package com.viveworks.appbase.service.web.oauth.apple;

import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.TokenKeyHashing;
import java.net.URI;
import java.util.Objects;

/**
 * Pre-filled Apple OIDC discovery defaults. Apple publishes a fixed issuer and JWKS endpoint, so a relying
 * party need only supply its own client id (the Services ID); the rest of the {@link OAuthConfiguration}
 * is filled from Apple's well-known values. Validation itself is the core Nimbus engine, unchanged — this
 * module adds no token-handling code, only these defaults plus {@link AppleClientSecretGenerator} and
 * {@link AppleProfileBinding}.
 *
 * <p>The endpoints are the stable values from Apple's OpenID configuration
 * ({@code https://appleid.apple.com/.well-known/openid-configuration}); an Apple id_token carries the
 * relying party's client id as its {@code aud}, so the expected audience value is the client id.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AppleOAuthDefaults {

    /** Apple's OIDC issuer ({@code iss} of an Apple id_token). */
    public static final String ISSUER = "https://appleid.apple.com";

    /** Apple's JWKS endpoint, over HTTPS as the core engine requires. */
    public static final URI JWKS_URI = URI.create("https://appleid.apple.com/auth/keys");

    private static final long DEFAULT_EXPIRY_MARGIN_SECONDS = 60L;

    private static final long DEFAULT_CACHE_TTL_SECONDS = 300L;

    private AppleOAuthDefaults() {
    }

    /**
     * Builds an {@link OAuthConfiguration} from Apple's discovery defaults for the given relying party.
     *
     * @param clientId this relying party's Apple Services ID; also the expected {@code aud} value
     * @return a configuration pointed at Apple's issuer and JWKS endpoint, with SHA-256 cache keying
     */
    public static OAuthConfiguration configuration(String clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return new OAuthConfiguration(
                JWKS_URI,
                ISSUER,
                clientId,
                clientId,
                DEFAULT_EXPIRY_MARGIN_SECONDS,
                DEFAULT_CACHE_TTL_SECONDS,
                TokenKeyHashing.SHA256,
                null);
    }
}
