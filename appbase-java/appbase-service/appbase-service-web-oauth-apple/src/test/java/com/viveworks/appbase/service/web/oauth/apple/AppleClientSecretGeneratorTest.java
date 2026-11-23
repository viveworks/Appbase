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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Unlike the other provider modules, Sign in with Apple requires the relying party to mint its own
 * client secret: an ES256-signed JWT. This test pins the shape Apple demands — {@code iss}=team id,
 * {@code sub}=client id, {@code aud}={@code https://appleid.apple.com}, a {@code kid} header, an ES256
 * signature verifiable with the public key — and the 6-month lifetime ceiling Apple enforces on that
 * secret (spec §5).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppleClientSecretGeneratorTest {

    private static final String TEAM_ID = "ABC1234DEF";

    private static final String CLIENT_ID = "com.example.service";

    private static final String KEY_ID = "XYZ9876WVU";

    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void generatesAnEs256ClientSecretAppleWillAccept() throws Exception {
        ECKey signingKey = new ECKeyGenerator(Curve.P_256).keyID(KEY_ID).generate();
        AppleClientSecretGenerator generator =
                new AppleClientSecretGenerator(TEAM_ID, CLIENT_ID, signingKey, FIXED);

        String secret = generator.generate(Duration.ofDays(180));

        SignedJWT jwt = SignedJWT.parse(secret);
        assertEquals(JWSAlgorithm.ES256, jwt.getHeader().getAlgorithm());
        assertEquals(KEY_ID, jwt.getHeader().getKeyID());
        assertTrue(jwt.verify(new ECDSAVerifier(signingKey.toPublicJWK())), "signature must verify");

        assertEquals(TEAM_ID, jwt.getJWTClaimsSet().getIssuer());
        assertEquals(CLIENT_ID, jwt.getJWTClaimsSet().getSubject());
        assertEquals("https://appleid.apple.com", jwt.getJWTClaimsSet().getAudience().get(0));
        assertEquals(NOW.getEpochSecond(), jwt.getJWTClaimsSet().getIssueTime().toInstant().getEpochSecond());
        assertEquals(NOW.plus(Duration.ofDays(180)).getEpochSecond(),
                jwt.getJWTClaimsSet().getExpirationTime().toInstant().getEpochSecond());
    }

    @Test
    void rejectsALifetimeBeyondApplesSixMonthCeiling() throws Exception {
        ECKey signingKey = new ECKeyGenerator(Curve.P_256).keyID(KEY_ID).generate();
        AppleClientSecretGenerator generator =
                new AppleClientSecretGenerator(TEAM_ID, CLIENT_ID, signingKey, FIXED);

        assertThrows(IllegalArgumentException.class, () -> generator.generate(Duration.ofDays(200)));
    }
}
