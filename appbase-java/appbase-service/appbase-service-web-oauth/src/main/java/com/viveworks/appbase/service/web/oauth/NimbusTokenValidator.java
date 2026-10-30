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

package com.viveworks.appbase.service.web.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The production {@link TokenValidator}: verifies a JWT locally against a background-refreshed JWKS cache,
 * with no per-request round trip to the provider (spec §4.3).
 *
 * <p>Nimbus performs the security-critical, non-hand-rolled part — signature verification against a
 * {@code kid}-keyed, refresh-ahead, rate-limited key set that refetches a newly published key on a cache
 * miss, so a token signed with a rotated key still validates. The claim checks that carry a typed
 * rejection reason ({@code exp}/{@code nbf}, {@code iss}, {@code aud}) are performed explicitly here so
 * each maps to a typed {@link ValidationFailure.Reason} rather than an opaque Nimbus exception.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class NimbusTokenValidator implements TokenValidator {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final TokenKeyHasher hasher;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final long clockSkewMarginSeconds;

    /**
     * @param configuration the OAuth configuration; supplies the JWKS endpoint and the expected claims
     * @param hasher        derives the non-reversible {@code tokenKey} carried on a validated token
     */
    public NimbusTokenValidator(OAuthConfiguration configuration, TokenKeyHasher hasher) {
        this(configuration, hasher, null);
    }

    NimbusTokenValidator(OAuthConfiguration configuration, TokenKeyHasher hasher, ResourceRetriever resourceRetriever) {
        requireHttpsJwksUri(configuration.jwksUri());
        this.hasher = hasher;
        this.expectedIssuer = configuration.issuer();
        this.expectedAudience = configuration.audienceClaim();
        this.clockSkewMarginSeconds = configuration.tokenExpiryMarginSeconds();
        this.jwtProcessor = processor(jwkSource(configuration, resourceRetriever));
    }

    private static void requireHttpsJwksUri(java.net.URI jwksUri) {
        if (!"https".equalsIgnoreCase(jwksUri.getScheme())) {
            throw new IllegalArgumentException("jwksUri must use HTTPS to prevent key injection via MITM: "
                    + jwksUri);
        }
    }

    private static ConfigurableJWTProcessor<SecurityContext> processor(JWKSource<SecurityContext> keys) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
        // Signature-only: the claim checks are performed in validate() so each yields a typed reason.
        processor.setJWTClaimsSetVerifier((claims, context) -> { });
        return processor;
    }

    private static JWKSource<SecurityContext> jwkSource(OAuthConfiguration configuration, ResourceRetriever resourceRetriever) {
        try {
            if (resourceRetriever == null) {
                return JWKSourceBuilder.<SecurityContext>create(configuration.jwksUri().toURL()).build();
            }
            return JWKSourceBuilder.<SecurityContext>create(configuration.jwksUri().toURL(), resourceRetriever).build();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("jwksUri is not a valid URL: " + configuration.jwksUri(), e);
        }
    }

    @Override
    public ValidationResult validate(String rawToken) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(rawToken);
        } catch (ParseException e) {
            return new ValidationFailure(ValidationFailure.Reason.MALFORMED, "token is not a well-formed JWT");
        }

        JWTClaimsSet claims;
        try {
            claims = jwtProcessor.process(jwt, null);
        } catch (KeySourceException e) {
            return new ValidationFailure(ValidationFailure.Reason.PROVIDER_UNREACHABLE,
                    "JWKS key source unavailable");
        } catch (BadJOSEException e) {
            return new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE,
                    "signature verification failed");
        } catch (JOSEException e) {
            return new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE,
                    "signature could not be verified");
        }

        // Claim checks in spec §4.4 order, each yielding its own typed rejection reason. The clock-skew
        // margin is applied symmetrically to exp and nbf.
        Instant now = Instant.now();
        Date expiration = claims.getExpirationTime();
        // A missing exp is not an unbounded lifetime: RFC 9068 makes exp REQUIRED on a JWT access token,
        // so an exp-less token is rejected rather than treated as valid forever.
        if (expiration == null) {
            return new ValidationFailure(ValidationFailure.Reason.EXPIRED, "token has no expiry claim");
        }
        if (now.isAfter(expiration.toInstant().plusSeconds(clockSkewMarginSeconds))) {
            return new ValidationFailure(ValidationFailure.Reason.EXPIRED, "token has expired");
        }
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && now.isBefore(notBefore.toInstant().minusSeconds(clockSkewMarginSeconds))) {
            return new ValidationFailure(ValidationFailure.Reason.EXPIRED, "token is not yet valid");
        }
        if (!expectedIssuer.equals(claims.getIssuer())) {
            return new ValidationFailure(ValidationFailure.Reason.UNTRUSTED_ISSUER,
                    "issuer is not trusted");
        }
        // expectedAudience is the expected aud VALUE (spec §4.2), not a claim name.
        List<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(expectedAudience)) {
            return new ValidationFailure(ValidationFailure.Reason.AUDIENCE_MISMATCH,
                    "token audience does not include this resource server");
        }

        return new ValidatedToken(
                claims.getSubject(),
                claims.getIssuer(),
                Set.copyOf(audience),
                expiration.toInstant(),
                claims.getClaims(),
                // A namespace-free hash of the raw token: non-reversible and never the raw token. This is
                // NOT the filter's cache key — OAuthFilter derives its own key over hash(namespace ‖ token)
                // and does not read this field. A shared-cache backend (N03) must not key on this value
                // alone, or it reintroduces the cross-audience bypass fixed in internal work item 1237.
                hasher.hash(rawToken));
    }
}
