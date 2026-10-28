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

package com.viveworks.appbase.service.web.oauth.google;

import static com.viveworks.appbase.service.web.oauth.JwksTestServer.publicSet;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.rsaKey;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.signRs256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.viveworks.appbase.service.web.oauth.JwksTestServer;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationResult;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * The Google provider defaults: a token carrying Google's real {@code iss} and this relying party's
 * client id as {@code aud} validates through the core engine when it is configured from
 * {@link GoogleOAuthDefaults}, and the pinned discovery endpoints are Google's.
 *
 * <p>Validation runs through the core module's public {@link JwksTestServer#validator(OAuthConfiguration)}
 * seam against a loopback-HTTPS fixture JWKS — offline, and without this test reaching the engine's
 * package-private internals.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class GoogleOAuthDefaultsTest {

    private static final String CLIENT_ID = "1234567890-abc.apps.googleusercontent.com";

    @Test
    void configurationValidatesAGoogleShapedToken() throws Exception {
        RSAKey signingKey = rsaKey("google-k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(signingKey))) {
            OAuthConfiguration cfg = withJwksUri(GoogleOAuthDefaults.configuration(CLIENT_ID), jwks.uri());
            String token = signRs256(signingKey, new JWTClaimsSet.Builder()
                    .subject("108273649182736451827")
                    .issuer(GoogleOAuthDefaults.ISSUER)
                    .audience(CLIENT_ID)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build());

            ValidationResult result = jwks.validator(cfg).validate(token);

            ValidatedToken validated = assertInstanceOf(ValidatedToken.class, result);
            assertEquals("108273649182736451827", validated.subject());
            assertEquals(GoogleOAuthDefaults.ISSUER, validated.issuer());
        }
    }

    @Test
    void defaultsPinGoogleDiscoveryEndpoints() {
        OAuthConfiguration cfg = GoogleOAuthDefaults.configuration(CLIENT_ID);
        assertEquals("https://accounts.google.com", cfg.issuer());
        assertEquals("https://www.googleapis.com/oauth2/v3/certs", cfg.jwksUri().toString());
        assertTrue("https".equalsIgnoreCase(cfg.jwksUri().getScheme()), "jwksUri must be HTTPS");
        // Google ID tokens carry the client id as their audience, so the expected aud value is the client id.
        assertEquals(CLIENT_ID, cfg.audienceClaim());
    }

    /** Google's issuer/audience defaults, but with the JWKS endpoint pointed at the offline fixture. */
    private static OAuthConfiguration withJwksUri(OAuthConfiguration base, java.net.URI jwksUri) {
        return new OAuthConfiguration(jwksUri, base.issuer(), base.clientId(), base.audienceClaim(),
                base.tokenExpiryMarginSeconds(), base.cacheTtlSeconds(), base.tokenKeyHashing(),
                base.tokenKeyHmacKeyRef());
    }
}
