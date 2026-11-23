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

import static com.viveworks.appbase.service.web.oauth.JwksTestServer.publicSet;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.rsaKey;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.signRs256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the Nimbus JWKS/JWT validation engine (S01 of task internal work item 1192): the happy path plus the
 * signature / expiry / issuer / audience / rotation rows of the spec §6 matrix, and the adversarial rows
 * (malformed, {@code alg:none}, algorithm-confusion, provider-outage). The adversarial rows were the
 * scope of S02 (internal work item 1239) but were never implemented there; they were folded in here during the epic
 * close-out whole-surface audit — see the close-out quality ledger.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NimbusTokenValidatorTest {

    private static final String ISSUER = "https://issuer.example";
    private static final String AUDIENCE = "client-abc";

    private static OAuthConfiguration config(java.net.URI jwksUri) {
        return new OAuthConfiguration(jwksUri, ISSUER, "client-abc", AUDIENCE, 60, 300,
                TokenKeyHashing.SHA256, null);
    }

    private static JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder()
                .subject("user-123")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .expirationTime(Date.from(Instant.now().plusSeconds(300)));
    }

    private static ValidationResult validate(JwksTestServer jwks, String token) {
        return new NimbusTokenValidator(config(jwks.uri()), new Sha256TokenKeyHasher(), jwks.resourceRetriever())
                .validate(token);
    }

    private static ValidationFailure.Reason reasonOf(ValidationResult result) {
        return assertInstanceOf(ValidationFailure.class, result).reason();
    }

    @Test
    void validatesAWellFormedRs256Token() throws Exception {
        RSAKey signingKey = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(signingKey))) {
            String token = signRs256(signingKey, claims().build());
            NimbusTokenValidator validator = new NimbusTokenValidator(config(jwks.uri()),
                    new Sha256TokenKeyHasher(), jwks.resourceRetriever());

            ValidationResult result = validator.validate(token);

            ValidatedToken validated = assertInstanceOf(ValidatedToken.class, result);
            assertEquals("user-123", validated.subject());
            assertEquals(ISSUER, validated.issuer());
            assertTrue(validated.audience().contains(AUDIENCE), "audience should carry the aud claim");
            assertNotEquals(token, validated.tokenKey(), "tokenKey must never be the raw token");
        }
    }

    @Test
    void rejectsAPlaintextJwksEndpointAtConstruction() {
        OAuthConfiguration plaintextConfiguration = config(java.net.URI.create("http://issuer.example/jwks"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new NimbusTokenValidator(plaintextConfiguration, new Sha256TokenKeyHasher()));

        assertTrue(exception.getMessage().contains("HTTPS"));
    }

    @Test
    void rejectsExpiredTokenBeyondTheClockSkewMargin() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // exp an hour past, well beyond the 60s margin. Mutate: delete the exp check → this returns
            // ValidatedToken and the assertion goes red.
            String token = signRs256(key,
                    claims().expirationTime(Date.from(Instant.now().minusSeconds(3600))).build());
            assertEquals(ValidationFailure.Reason.EXPIRED, reasonOf(validate(jwks, token)));
        }
    }

    @Test
    void rejectsATokenWithNoExpiryClaim() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // A validly-signed token that omits exp must not be treated as valid forever (RFC 9068 makes
            // exp REQUIRED for a JWT access token). Mutate: accept a null exp (stamp Instant.MAX) → this
            // returns ValidatedToken and the assertion goes red.
            JWTClaimsSet noExpiry = new JWTClaimsSet.Builder()
                    .subject("user-123").issuer(ISSUER).audience(AUDIENCE).build();
            String token = signRs256(key, noExpiry);
            assertEquals(ValidationFailure.Reason.EXPIRED, reasonOf(validate(jwks, token)));
        }
    }

    @Test
    void rejectsTokenNotYetValidBeyondTheClockSkewMargin() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // nbf an hour in the future, exp further still. Mutate: delete the nbf check → ValidatedToken.
            String token = signRs256(key, claims()
                    .notBeforeTime(Date.from(Instant.now().plusSeconds(3600)))
                    .expirationTime(Date.from(Instant.now().plusSeconds(7200)))
                    .build());
            assertEquals(ValidationFailure.Reason.EXPIRED, reasonOf(validate(jwks, token)));
        }
    }

    @Test
    void rejectsUntrustedIssuer() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // Correctly signed by the trusted key, but the iss is not the one we trust. Mutate: delete the
            // issuer check → ValidatedToken.
            String token = signRs256(key, claims().issuer("https://attacker.example").build());
            assertEquals(ValidationFailure.Reason.UNTRUSTED_ISSUER, reasonOf(validate(jwks, token)));
        }
    }

    @Test
    void rejectsAudienceMintedForAnotherClient() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // Correctly signed and issued, but minted for a different audience. Mutate: delete the audience
            // check → ValidatedToken, i.e. a token intended for another client is accepted.
            String token = signRs256(key, claims().audience("some-other-client").build());
            assertEquals(ValidationFailure.Reason.AUDIENCE_MISMATCH, reasonOf(validate(jwks, token)));
        }
    }

    @Test
    void rejectsASignatureThatDoesNotMatchThePublishedKey() throws Exception {
        RSAKey signingKey = rsaKey("k1");
        RSAKey publishedKey = rsaKey("k1"); // same kid, different key material
        try (JwksTestServer jwks = new JwksTestServer(publicSet(publishedKey))) {
            // The selector resolves kid k1 to the published key, whose public half cannot verify a
            // signature made with a different private key. Mutate: drop the JWS key selector so signatures
            // are not verified → this token is accepted and the assertion goes red.
            String token = signRs256(signingKey, claims().build());
            assertEquals(ValidationFailure.Reason.BAD_SIGNATURE, reasonOf(validate(jwks, token)));
        }
    }

    // --- Adversarial matrix (S02 internal work item 1239, folded in during epic close-out) ------------------------------

    @Test
    void rejectsAMalformedTokenThatIsNotAJwt() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // A string that is not a well-formed JWT must be rejected before any key/claim work. Mutate:
            // return ValidatedToken on a ParseException instead of MALFORMED → this goes red.
            assertEquals(ValidationFailure.Reason.MALFORMED,
                    reasonOf(validate(jwks, "this-is-not.a-valid.jwt")));
        }
    }

    @Test
    void rejectsAnUnsignedAlgNoneToken() throws Exception {
        RSAKey key = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(key))) {
            // The classic JWT footgun: a token declaring alg:none and carrying no signature. It must be
            // rejected, never trusted. Mutate: accept an unsecured JWT → this returns ValidatedToken and
            // the assertion goes red.
            String algNone = new com.nimbusds.jwt.PlainJWT(claims().build()).serialize();
            // An unsigned JWT has an empty signature segment, so it does not even parse as a signed JWT →
            // MALFORMED. The security property is only that it is never trusted; the reason is pinned so
            // the OAuth primer's rejection matrix can state it accurately.
            assertEquals(ValidationFailure.Reason.MALFORMED, reasonOf(validate(jwks, algNone)));
        }
    }

    @Test
    void rejectsRs256ToHs256AlgorithmConfusion() throws Exception {
        RSAKey signingKey = rsaKey("k1");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(signingKey))) {
            // Algorithm-confusion: the attacker takes the provider's PUBLIC RSA key (published in the JWKS)
            // and re-signs a forged token with HS256, using the public key bytes as the HMAC secret. A
            // validator that read alg from the token and reused "the configured key" would verify it. This
            // engine pins the verifier to RS256, so an HS256 token is rejected. Mutate: widen the key
            // selector to accept HS256 → this token verifies and the assertion goes red.
            byte[] publicKeyAsHmacSecret = signingKey.toRSAPublicKey().getEncoded();
            com.nimbusds.jwt.SignedJWT forged = new com.nimbusds.jwt.SignedJWT(
                    new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims().build());
            forged.sign(new com.nimbusds.jose.crypto.MACSigner(publicKeyAsHmacSecret));
            // The RS256-pinned key selector rejects the HS256 token as a bad signature (no HMAC verifier is
            // ever selected). Reason pinned for the primer's rejection matrix.
            assertEquals(ValidationFailure.Reason.BAD_SIGNATURE, reasonOf(validate(jwks, forged.serialize())));
        }
    }

    @Test
    void reportsProviderUnreachableWhenTheJwksCannotBeFetched() throws Exception {
        RSAKey key = rsaKey("k1");
        JwksTestServer jwks = new JwksTestServer(publicSet(key));
        NimbusTokenValidator validator = new NimbusTokenValidator(config(jwks.uri()),
                new Sha256TokenKeyHasher(), jwks.resourceRetriever());
        String token = signRs256(key, claims().build());
        // Take the provider offline BEFORE the first validation, so the JWKS fetch fails rather than the
        // token being proven bad — a distinct, typed outcome (not BAD_SIGNATURE). Mutate: map a key-source
        // failure to BAD_SIGNATURE instead of PROVIDER_UNREACHABLE → this goes red.
        jwks.close();
        assertEquals(ValidationFailure.Reason.PROVIDER_UNREACHABLE, reasonOf(validator.validate(token)));
    }

    @Test
    void validatesATokenSignedWithAKeyRotatedInAfterTheFirstFetch() throws Exception {
        RSAKey oldKey = rsaKey("key-old");
        RSAKey newKey = rsaKey("key-new");
        try (JwksTestServer jwks = new JwksTestServer(publicSet(oldKey))) {
            NimbusTokenValidator validator = new NimbusTokenValidator(config(jwks.uri()), new Sha256TokenKeyHasher(),
                    jwks.resourceRetriever());
            // Prime the JWKS cache with the old key.
            assertInstanceOf(ValidatedToken.class,
                    validator.validate(signRs256(oldKey, claims().build())));

            // The provider rotates: a token arrives signed with a kid published only after that first
            // fetch. It must still validate — Nimbus refetches the set on the cache miss rather than
            // rejecting a legitimately signed token. Mutate: pin the engine to a static/immutable JWK set
            // (no refetch) → this validation fails and the assertion goes red.
            jwks.publish(publicSet(oldKey, newKey));
            ValidationResult rotated = validator.validate(signRs256(newKey, claims().build()));

            assertInstanceOf(ValidatedToken.class, rotated);
        }
    }
}
