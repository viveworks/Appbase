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

package com.viveworks.appbase.service.web.oauth.microsoft;

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
 * The Microsoft / Entra provider defaults: Entra's issuer is per-tenant, but — unlike Okta — the caller
 * supplies only the tenant id and the module derives <em>both</em> the {@code v2.0} issuer and the
 * discovery-keys JWKS endpoint from Microsoft's fixed {@code login.microsoftonline.com} host. A token
 * carrying that derived issuer and the resource server's audience validates through the core engine; a
 * tenant id whose shape could redirect the derived discovery endpoint is rejected before it can become a
 * configuration.
 *
 * <p>Validation runs through the core module's public {@link JwksTestServer#validator(OAuthConfiguration)}
 * seam against a loopback-HTTPS fixture JWKS — offline, and without this test reaching the engine's
 * package-private internals.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class EntraOAuthDefaultsTest {

    private static final String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private static final String ISSUER = "https://login.microsoftonline.com/" + TENANT_ID + "/v2.0";

    private static final String JWKS =
            "https://login.microsoftonline.com/" + TENANT_ID + "/discovery/v2.0/keys";

    private static final String AUDIENCE = "api://appbase-resource";

    private static final String CLIENT_ID = "11112222-3333-4444-5555-666677778888";

    @Test
    void configurationValidatesAnEntraShapedToken() throws Exception {
        RSAKey signingKey = rsaKey("entra-k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(signingKey))) {
            OAuthConfiguration cfg = withJwksUri(
                    EntraOAuthDefaults.configuration(TENANT_ID, AUDIENCE, CLIENT_ID), jwks.uri());
            String token = signRs256(signingKey, new JWTClaimsSet.Builder()
                    .subject("AAAAAAAAAAAAAAAAAAAAAA")
                    .issuer(ISSUER)
                    .audience(AUDIENCE)
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build());

            ValidationResult result = jwks.validator(cfg).validate(token);

            ValidatedToken validated = assertInstanceOf(ValidatedToken.class, result);
            assertEquals("AAAAAAAAAAAAAAAAAAAAAA", validated.subject());
            assertEquals(ISSUER, validated.issuer());
        }
    }

    @Test
    void derivesIssuerAndJwksFromTenant() {
        OAuthConfiguration cfg = EntraOAuthDefaults.configuration(TENANT_ID, AUDIENCE, CLIENT_ID);
        assertEquals(ISSUER, cfg.issuer());
        assertEquals(JWKS, cfg.jwksUri().toString());
        assertTrue("https".equalsIgnoreCase(cfg.jwksUri().getScheme()), "jwksUri must be HTTPS");
        assertEquals("login.microsoftonline.com", cfg.jwksUri().getHost(),
                "the key-fetching JWKS endpoint must be host-pinned to Microsoft's authority");
        assertEquals(AUDIENCE, cfg.audienceClaim());
    }

    @Test
    void rejectsATenantThatWouldRedirectTheDiscoveryEndpoint() {
        // A slash lets the tenant break out of its path segment, walking the derived JWKS URI away from
        // the legitimate discovery document while a token whose real iss differs is still exact-matched
        // downstream. The input starts with a valid tenant character on purpose, so the assertion is
        // driven by the embedded slash — not by the regex's leading-character rule — and so it would go
        // red if a future edit ever loosened the tail of the tenant pattern to permit '/'.
        assertThrows(IllegalArgumentException.class,
                () -> EntraOAuthDefaults.configuration("contoso/../../evil", AUDIENCE, CLIENT_ID));
    }

    @Test
    void rejectsATenantWithAuthorityOrControlDelimiters() {
        // '@' (authority delimiter), whitespace/CRLF (request smuggling) and other non-tenant characters
        // have no place in an Entra tenant id (a GUID or a verified domain). Anything outside that shape
        // is rejected rather than interpolated into the discovery URIs.
        assertThrows(IllegalArgumentException.class,
                () -> EntraOAuthDefaults.configuration("tenant@evil.com", AUDIENCE, CLIENT_ID));
        assertThrows(IllegalArgumentException.class,
                () -> EntraOAuthDefaults.configuration("tenant\nid", AUDIENCE, CLIENT_ID));
    }

    @Test
    void rejectsAnEmptyTenant() {
        assertThrows(IllegalArgumentException.class,
                () -> EntraOAuthDefaults.configuration("", AUDIENCE, CLIENT_ID));
    }

    /** Entra's issuer/audience defaults, but with the JWKS endpoint pointed at the offline fixture. */
    private static OAuthConfiguration withJwksUri(OAuthConfiguration base, java.net.URI jwksUri) {
        return new OAuthConfiguration(jwksUri, base.issuer(), base.clientId(), base.audienceClaim(),
                base.tokenExpiryMarginSeconds(), base.cacheTtlSeconds(), base.tokenKeyHashing(),
                base.tokenKeyHmacKeyRef());
    }
}
