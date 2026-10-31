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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Serializes a {@link ValidatedToken} to and from bytes for the external {@link TokenCache} backends
 * (Aerospike, Redis), which store bytes rather than live objects.
 *
 * <p>The built-in {@link CaffeineTokenCache} holds {@link ValidatedToken} instances directly and never
 * uses this codec. Fields are mapped explicitly (not by reflecting over the record's constructor), so
 * the record needs no serialization annotations and {@code expiresAt} is stored as epoch milliseconds.
 * The {@code claims} map round-trips through JSON, so its values return with JSON's types (a whole
 * number as {@code Integer}/{@code Long}, a nested array as {@code List}); the claim values a bearer
 * token carries are strings, numbers and small arrays, which survive that mapping.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ValidatedTokenCodec {

    // USE_LONG_FOR_INTS: Nimbus hands numeric claims to ValidatedToken as Long; without this a
    // whole-number claim would decode as Integer and no longer equal a freshly validated token.
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(DeserializationFeature.USE_LONG_FOR_INTS);

    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {};

    /**
     * @param token the validated token
     * @return the serialized form suitable for a byte-oriented {@link TokenCache} backend
     */
    public byte[] encode(ValidatedToken token) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("subject", token.subject());
        root.put("issuer", token.issuer());
        ArrayNode audience = root.putArray("audience");
        token.audience().forEach(audience::add);
        root.put("expiresAtEpochMs", token.expiresAt().toEpochMilli());
        root.set("claims", MAPPER.valueToTree(token.claims()));
        root.put("tokenKey", token.tokenKey());
        try {
            return MAPPER.writeValueAsBytes(root);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to encode ValidatedToken", e);
        }
    }

    /**
     * @param bytes a value previously produced by {@link #encode(ValidatedToken)}
     * @return the reconstructed token
     */
    public ValidatedToken decode(byte[] bytes) {
        final JsonNode root;
        try {
            root = MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to decode ValidatedToken", e);
        }
        Set<String> audience = new LinkedHashSet<>();
        root.get("audience").forEach(node -> audience.add(node.asText()));
        Map<String, Object> claims = MAPPER.convertValue(root.get("claims"), CLAIMS_TYPE);
        return new ValidatedToken(
                nullableText(root, "subject"),
                requiredText(root, "issuer"),
                audience,
                Instant.ofEpochMilli(root.get("expiresAtEpochMs").asLong()),
                claims,
                requiredText(root, "tokenKey"));
    }

    // subject is the only legitimately-null field (the §4.4 checks do not require a sub). A JSON null must
    // decode back to Java null, not the 4-char string "null": JsonNode.asText() on a NullNode returns
    // "null", which would silently corrupt a subject-less token on the byte-backed cache path only.
    // hasNonNull() distinguishes an absent/null field from a real value.
    private static String nullableText(JsonNode root, String field) {
        return root.hasNonNull(field) ? root.get(field).asText() : null;
    }

    // issuer and tokenKey are always populated when this service encodes a token, so an absent/null one in
    // a stored blob is corruption (a truncated write, a version skew, a poisoned entry). Throw rather than
    // decode it to a null-bearing token: AbstractStoreBackedTokenCache.get() treats a decode failure as a
    // cache miss and re-validates the raw token, so a corrupt entry fails CLOSED instead of being trusted.
    private static String requiredText(JsonNode root, String field) {
        if (!root.hasNonNull(field)) {
            throw new IllegalStateException("cached ValidatedToken is missing required field: " + field);
        }
        return root.get(field).asText();
    }
}
