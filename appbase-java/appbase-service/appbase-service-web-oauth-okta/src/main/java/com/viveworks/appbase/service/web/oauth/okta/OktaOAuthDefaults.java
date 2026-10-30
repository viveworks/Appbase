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

import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.TokenKeyHashing;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Pre-filled Okta OIDC discovery defaults. Unlike Google, Okta's issuer is per-tenant — the org's
 * authorization-server URL (for example {@code https://dev-12345.okta.com/oauth2/default}) — so the caller
 * supplies the issuer and audience, and the module derives the JWKS endpoint from Okta's fixed
 * {@code /v1/keys} convention. Validation itself is the core Nimbus engine, unchanged.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class OktaOAuthDefaults {

    /** Okta serves an authorization server's keys at {@code <issuer>/v1/keys}. */
    private static final String JWKS_PATH = "/v1/keys";

    private static final long DEFAULT_EXPIRY_MARGIN_SECONDS = 60L;

    private static final long DEFAULT_CACHE_TTL_SECONDS = 300L;

    private OktaOAuthDefaults() {
    }

    /**
     * Builds an {@link OAuthConfiguration} from Okta's discovery convention for the given tenant.
     *
     * @param issuer   the Okta authorization-server issuer (its {@code iss}); the JWKS endpoint is derived
     *                 as {@code issuer + "/v1/keys"}
     * @param audience the expected {@code aud} of the access tokens this resource server accepts (the
     *                 authorization server's audience, for example {@code api://default})
     * @param clientId this relying party's Okta client id
     * @return a configuration pointed at the tenant's issuer and derived JWKS endpoint, with SHA-256 keying
     */
    public static OAuthConfiguration configuration(String issuer, String audience, String clientId) {
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(clientId, "clientId");
        requireWellFormedIssuer(issuer);
        return new OAuthConfiguration(
                URI.create(issuer + JWKS_PATH),
                issuer,
                clientId,
                audience,
                DEFAULT_EXPIRY_MARGIN_SECONDS,
                DEFAULT_CACHE_TTL_SECONDS,
                TokenKeyHashing.SHA256,
                null);
    }

    /**
     * Rejects an issuer whose shape would let the derived JWKS endpoint point somewhere other than the
     * issuer's identity. The JWKS URI is {@code issuer + "/v1/keys"} and the core validator checks the
     * token's {@code iss} by exact string equality, so a URI like
     * {@code https://tenant.okta.com@evil.com/oauth2/default} — syntactically HTTPS, but whose real host is
     * {@code evil.com} with the tenant demoted to user-info — would fetch signing keys from an attacker
     * while a token carrying that exact literal {@code iss} still matches. The core engine's HTTPS check
     * alone does not catch this because the string is a valid HTTPS URI; this is the one place that encodes
     * what a legitimate Okta issuer looks like, so the shape check belongs here.
     *
     * <p>No host allowlist is imposed: Okta supports custom domains, so the legitimate host set is a
     * deployment concern, not a value this module can pin.
     */
    private static void requireWellFormedIssuer(String issuer) {
        URI uri;
        try {
            uri = new URI(issuer);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Okta issuer is not a valid URI: " + issuer, e);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Okta issuer must use HTTPS: " + issuer);
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Okta issuer must name a host: " + issuer);
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "Okta issuer must not contain user-info (authority-confusion guard): " + issuer);
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Okta issuer must not contain a query or fragment: " + issuer);
        }
    }
}
