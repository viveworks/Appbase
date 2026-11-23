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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Mints the client secret Sign in with Apple requires. Unlike every other provider in this epic, Apple
 * does not issue a static client secret string: the relying party signs a short-lived ES256 JWT with the
 * private key downloaded from the Apple Developer console, and presents that JWT as the OAuth client
 * secret. This class produces exactly that JWT — {@code iss}=team id, {@code sub}=client id (the Services
 * ID), {@code aud}={@code https://appleid.apple.com}, a {@code kid} header naming the signing key, an
 * {@code iat}/{@code exp} pair — signed with the caller's P-256 {@link ECKey} (spec §5).
 *
 * <p><strong>Rotation obligation.</strong> Apple caps the secret's lifetime at <em>six months</em> and
 * rejects a JWT whose {@code exp} is further out; {@link #generate(Duration)} therefore refuses a lifetime
 * over {@value #MAX_LIFETIME_DAYS} days rather than emit a secret Apple will reject. Because the secret
 * expires, an integrator MUST re-mint it before {@code exp} — no more than every six months, and in
 * practice more often to leave margin. This class holds no schedule and performs no rotation itself; it is
 * a pure generator, and the surrounding deployment owns the cadence and the secure storage of the private
 * key. The key material never leaves the caller-supplied {@link ECKey}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AppleClientSecretGenerator {

    /** The fixed {@code aud} Apple requires on the client-secret JWT. */
    static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    /** Apple's ceiling on the client-secret lifetime, in days (six months). */
    static final long MAX_LIFETIME_DAYS = 180L;

    private static final Duration MAX_LIFETIME = Duration.ofDays(MAX_LIFETIME_DAYS);

    private final String teamId;

    private final String clientId;

    private final ECKey signingKey;

    private final Clock clock;

    /**
     * @param teamId     the Apple Developer team id ({@code iss} of the secret)
     * @param clientId   the Services ID / client id ({@code sub} of the secret)
     * @param signingKey the P-256 private key registered with Apple; its {@code kid} becomes the JWS header
     *                   key id, so it must carry the key id Apple issued
     * @param clock      the time source for {@code iat}/{@code exp} (injected for deterministic tests)
     */
    public AppleClientSecretGenerator(String teamId, String clientId, ECKey signingKey, Clock clock) {
        this.teamId = Objects.requireNonNull(teamId, "teamId");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.signingKey = Objects.requireNonNull(signingKey, "signingKey");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Mints a signed client-secret JWT valid for {@code lifetime} from now.
     *
     * @param lifetime how long the secret stays valid; must be positive and no more than six months
     * @return the serialized ES256 JWT to present as the OAuth client secret
     * @throws IllegalArgumentException if {@code lifetime} is not positive, or exceeds Apple's six-month
     *                                  ceiling (a secret Apple would reject)
     */
    public String generate(Duration lifetime) {
        Objects.requireNonNull(lifetime, "lifetime");
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive; got " + lifetime);
        }
        if (lifetime.compareTo(MAX_LIFETIME) > 0) {
            throw new IllegalArgumentException("Apple caps the client-secret lifetime at " + MAX_LIFETIME_DAYS
                    + " days (six months); requested " + lifetime.toDays() + " days");
        }
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(teamId)
                .subject(clientId)
                .audience(APPLE_AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(lifetime)))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).build();
        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new ECDSASigner(signingKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("failed to sign the Apple client secret", e);
        }
        return jwt.serialize();
    }
}
