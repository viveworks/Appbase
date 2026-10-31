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

package com.viveworks.appbase.service.web.oauth.okta;

import static com.viveworks.appbase.service.web.oauth.JwksTestServer.publicSet;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.rsaKey;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.signRs256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * The Okta provider defaults: Okta's issuer is per-tenant (the org's authorization-server URL), so the
 * caller supplies it and the module derives the JWKS endpoint from Okta's fixed {@code /v1/keys}
 * convention. A token carrying that issuer and the authorization server's audience validates through the
 * core engine; a malformed or host-confused issuer is rejected before it can become a configuration.
 *
 * <p>Validation runs through the core module's public {@link JwksTestServer#validator(OAuthConfiguration)}
 * seam against a loopback-HTTPS fixture JWKS — offline, and without this test reaching the engine's
 * package-private internals.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class OktaOAuthDefaultsTest {

    private static final String ISSUER = "https://dev-12345.okta.com/oauth2/default";

    private static final String AUDIENCE = "api://default";

    private static final String CLIENT_ID = "0oa1a2b3c4D5e6F7g8h9";

    @Test
    void configurationValidatesAnOktaShapedToken() throws Exception {
        RSAKey signingKey = rsaKey("okta-k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(signingKey))) {
            OAuthConfiguration cfg = withJwksUri(
                    OktaOAuthDefaults.configuration(ISSUER, AUDIENCE, CLIENT_ID), jwks.uri());
            String token = signRs256(signingKey, new JWTClaimsSet.Builder()
                    .subject("00u1a2b3c4d5e6f7g8h9")
                    .issuer(ISSUER)
                    .audience(AUDIENCE)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build());

            ValidationResult result = jwks.validator(cfg).validate(token);

            ValidatedToken validated = assertInstanceOf(ValidatedToken.class, result);
            assertEquals("00u1a2b3c4d5e6f7g8h9", validated.subject());
            assertEquals(ISSUER, validated.issuer());
        }
    }

    @Test
    void derivesJwksUriFromIssuer() {
        OAuthConfiguration cfg = OktaOAuthDefaults.configuration(ISSUER, AUDIENCE, CLIENT_ID);
        assertEquals(ISSUER, cfg.issuer());
        assertEquals(ISSUER + "/v1/keys", cfg.jwksUri().toString());
        assertTrue("https".equalsIgnoreCase(cfg.jwksUri().getScheme()), "jwksUri must be HTTPS");
        assertEquals(AUDIENCE, cfg.audienceClaim());
    }

    @Test
    void rejectsAnIssuerWhoseUserInfoHidesTheRealHost() {
        // Syntactically HTTPS, but the real host is evil.com — the tenant is demoted to user-info, so the
        // derived JWKS URI would fetch keys from the attacker while a token carrying this exact literal iss
        // still matches. Must be rejected before it can become a config.
        String authorityConfusion = "https://dev-12345.okta.com@evil.com/oauth2/default";
        assertThrows(IllegalArgumentException.class,
                () -> OktaOAuthDefaults.configuration(authorityConfusion, AUDIENCE, CLIENT_ID));
    }

    @Test
    void rejectsANonHttpsIssuer() {
        assertThrows(IllegalArgumentException.class,
                () -> OktaOAuthDefaults.configuration("http://dev-12345.okta.com/oauth2/default",
                        AUDIENCE, CLIENT_ID));
    }

    /** Okta's issuer/audience defaults, but with the JWKS endpoint pointed at the offline fixture. */
    private static OAuthConfiguration withJwksUri(OAuthConfiguration base, java.net.URI jwksUri) {
        return new OAuthConfiguration(jwksUri, base.issuer(), base.clientId(), base.audienceClaim(),
                base.tokenExpiryMarginSeconds(), base.cacheTtlSeconds(), base.tokenKeyHashing(),
                base.tokenKeyHmacKeyRef());
    }
}
