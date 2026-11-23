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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ValidatedTokenCodecTest {

    @Test
    void encodeThenDecodeRoundTripsEveryField() {
        // Every field of the record must survive a byte round trip, including the multi-valued
        // audience set and a nested-array claim.
        // Mutation: drop any field in encode() (e.g. omit audience) and this goes red.
        ValidatedToken token = new ValidatedToken(
                "sub-1",
                "https://issuer.example",
                Set.of("api", "web"),
                Instant.parse("2030-01-01T00:00:00Z"),
                Map.of("scope", "read", "roles", List.of("admin", "user")),
                "hash-key");

        ValidatedTokenCodec codec = new ValidatedTokenCodec();

        ValidatedToken restored = codec.decode(codec.encode(token));

        assertEquals(token, restored);
    }

    @Test
    void numericClaimsSurviveTheRoundTripAsLong() {
        // Nimbus populates numeric claims as Long; a token restored from the cache must equal a freshly
        // validated one, so a numeric claim must come back Long — not Integer, which breaks Map.equals()
        // and ClassCastExceptions any (Long) cast on the caller's side.
        // Mutation: decode with a plain ObjectMapper (no USE_LONG_FOR_INTS) and the claim returns as
        // Integer -> both assertions go red.
        ValidatedToken token = new ValidatedToken(
                "sub-1",
                "https://issuer.example",
                Set.of("api"),
                Instant.parse("2030-01-01T00:00:00Z"),
                Map.of("auth_time", 1_700_000_000L, "level", 5L),
                "hash-key");

        ValidatedTokenCodec codec = new ValidatedTokenCodec();

        ValidatedToken restored = codec.decode(codec.encode(token));

        assertEquals(token, restored);
        assertEquals(Long.class, restored.claims().get("level").getClass(),
                "a whole-number claim must round-trip as Long, matching Nimbus");
    }

    @Test
    void subMillisecondExpiryIsNormalizedToTheDocumentedStoragePrecision() {
        Instant expiry = Instant.parse("2030-01-01T00:00:00.123456789Z");
        ValidatedToken token = new ValidatedToken(
                "sub-1", "https://issuer.example", Set.of("api"), expiry,
                Map.of("scope", "read"), "hash-key");

        ValidatedToken restored = new ValidatedTokenCodec().decode(new ValidatedTokenCodec().encode(token));

        assertEquals(expiry.truncatedTo(ChronoUnit.MILLIS), restored.expiresAt(),
                "external cache format stores the millisecond precision supplied by OAuth/Nimbus Date");
    }

    @Test
    void aNullSubjectRoundTripsAsNullNotTheStringNull() {
        // The core validator builds a ValidatedToken from claims.getSubject(), which may be null (a sub is
        // not required by the §4.4 checks). A null subject must survive the byte round trip AS null — not
        // decode back to the literal string "null", which would silently corrupt identity on the cached
        // (Aerospike/Redis) path only.
        // Mutation: decode subject with an unconditional node.asText() and the JSON null returns as the
        // 4-char String "null" -> this goes red.
        ValidatedToken token = new ValidatedToken(
                null,
                "https://issuer.example",
                Set.of("api"),
                Instant.parse("2030-01-01T00:00:00Z"),
                Map.of("scope", "read"),
                "hash-key");

        ValidatedTokenCodec codec = new ValidatedTokenCodec();

        ValidatedToken restored = codec.decode(codec.encode(token));

        assertEquals(token, restored);
        org.junit.jupiter.api.Assertions.assertNull(restored.subject(),
                "a null subject must not decode to the string \"null\"");
    }

    @Test
    void aBlobMissingARequiredFieldFailsClosedRatherThanDecodingToNull() {
        // Unlike subject, issuer and tokenKey are always populated on encode, so a null/absent one in a
        // stored blob is corruption (truncated write, version skew, poisoned entry). decode() must throw —
        // AbstractStoreBackedTokenCache treats a decode failure as a cache miss and re-validates the raw
        // token, so the entry fails CLOSED rather than being trusted with a null identity.
        // Mutation: read issuer with the nullable helper instead of the required one → no throw → red.
        ValidatedToken corruptIssuer = new ValidatedToken(
                "sub-1", null, Set.of("api"), Instant.parse("2030-01-01T00:00:00Z"),
                Map.of("scope", "read"), "hash-key");
        ValidatedTokenCodec codec = new ValidatedTokenCodec();
        byte[] blob = codec.encode(corruptIssuer);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> codec.decode(blob));
    }
}
