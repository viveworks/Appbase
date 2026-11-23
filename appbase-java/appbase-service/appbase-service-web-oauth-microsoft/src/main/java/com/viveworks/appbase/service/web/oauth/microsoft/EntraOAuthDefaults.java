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

package com.viveworks.appbase.service.web.oauth.microsoft;

import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.TokenKeyHashing;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pre-filled Microsoft / Entra OIDC discovery defaults. Entra's issuer is per-tenant, but — unlike Okta,
 * where the caller passes the whole issuer URL — the caller supplies only the tenant id and the module
 * derives <em>both</em> the {@code v2.0} issuer and the discovery-keys JWKS endpoint from Microsoft's
 * fixed {@code login.microsoftonline.com} host. Validation itself is the core Nimbus engine, unchanged.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class EntraOAuthDefaults {

    /** Microsoft's fixed OIDC authority host; every derived URL must be served by exactly this host. */
    private static final String AUTHORITY_HOST = "login.microsoftonline.com";

    /** Fixed authority prefix; the tenant id is the first path segment beneath it. */
    private static final String AUTHORITY_BASE = "https://" + AUTHORITY_HOST + "/";

    /** {@code <authority>/<tenant>} + this = the v2.0 issuer the token's {@code iss} must equal. */
    private static final String ISSUER_SUFFIX = "/v2.0";

    /** {@code <authority>/<tenant>} + this = the tenant's OIDC signing-key (JWKS) endpoint. */
    private static final String JWKS_SUFFIX = "/discovery/v2.0/keys";

    /**
     * A legitimate Entra tenant id is a directory GUID or a verified domain (for example
     * {@code contoso.onmicrosoft.com}) — letters, digits, dot, hyphen; nothing that could break out of
     * the path segment it is interpolated into.
     */
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.-]*");

    private static final long DEFAULT_EXPIRY_MARGIN_SECONDS = 60L;

    private static final long DEFAULT_CACHE_TTL_SECONDS = 300L;

    private EntraOAuthDefaults() {
    }

    /**
     * Builds an {@link OAuthConfiguration} from Microsoft's v2.0 discovery convention for the given tenant.
     *
     * @param tenantId the Entra tenant (directory GUID or verified domain); the issuer is derived as
     *                 {@code https://login.microsoftonline.com/<tenantId>/v2.0} and the JWKS endpoint as
     *                 {@code .../<tenantId>/discovery/v2.0/keys}
     * @param audience the expected {@code aud} of the access tokens this resource server accepts (its
     *                 Entra application id URI or client id)
     * @param clientId this relying party's Entra application (client) id
     * @return a configuration pointed at the tenant's derived issuer and JWKS endpoint, with SHA-256 keying
     */
    public static OAuthConfiguration configuration(String tenantId, String audience, String clientId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(clientId, "clientId");
        requireWellFormedTenant(tenantId);
        String issuer = AUTHORITY_BASE + tenantId + ISSUER_SUFFIX;
        URI jwksUri = URI.create(AUTHORITY_BASE + tenantId + JWKS_SUFFIX);
        requireHostPinnedHttps(issuer, "issuer");
        requireHostPinnedHttps(jwksUri.toString(), "JWKS endpoint");
        return new OAuthConfiguration(
                jwksUri,
                issuer,
                clientId,
                audience,
                DEFAULT_EXPIRY_MARGIN_SECONDS,
                DEFAULT_CACHE_TTL_SECONDS,
                TokenKeyHashing.SHA256,
                null);
    }

    /**
     * Rejects a tenant id whose shape could redirect the derived discovery endpoints. Because the authority
     * host is a fixed prefix, the tenant lands in the URL <em>path</em>; a value containing {@code /},
     * {@code @}, {@code ?}, {@code #}, whitespace or other non-tenant characters could walk the derived
     * JWKS URI to another path (or smuggle a request) while a token carrying a mismatched {@code iss} is
     * still exact-matched downstream. The core engine's HTTPS check does not catch this because the derived
     * URI's scheme and host stay legitimate; this is the one place that encodes what an Entra tenant id may
     * look like, so the shape check belongs here.
     */
    private static void requireWellFormedTenant(String tenantId) {
        if (!TENANT_ID.matcher(tenantId).matches()) {
            throw new IllegalArgumentException(
                    "Entra tenant id must be a directory GUID or a verified domain "
                            + "(letters, digits, dot, hyphen only): " + tenantId);
        }
    }

    /**
     * Defence in depth over {@link #requireWellFormedTenant}: confirms a <em>derived</em> URL is a clean
     * HTTPS URI, served by exactly {@link #AUTHORITY_HOST}, with no user-info, query or fragment — so that
     * if the derivation is ever changed (a looser tenant shape, a different suffix) the host pinning still
     * holds. Applied to <em>both</em> derived URLs, and deliberately including the {@code JWKS endpoint}:
     * that is the URL from which signing-key material is fetched, so it is the one that most needs its host
     * pinned. The core validator checks the token's {@code iss} by exact string equality, so the issuer
     * must likewise name a single, unambiguous identity.
     *
     * @param url  the derived issuer or JWKS URL
     * @param kind a human label naming which URL, for the exception message
     */
    private static void requireHostPinnedHttps(String url, String kind) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Entra " + kind + " is not a valid URI: " + url, e);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Entra " + kind + " must use HTTPS: " + url);
        }
        if (!AUTHORITY_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException(
                    "Entra " + kind + " must be served by " + AUTHORITY_HOST
                            + " (authority-confusion guard): " + url);
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "Entra " + kind + " must not contain user-info (authority-confusion guard): " + url);
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Entra " + kind + " must not contain a query or fragment: " + url);
        }
    }
}
