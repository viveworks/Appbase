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

package com.viveworks.appbase.service.web.oauth.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Binds a GitHub {@code /user} response to a {@link GitHubUserProfile}. Read-only: it parses profile
 * fields and exposes no call that would revoke, deactivate or otherwise mutate the account.
 *
 * <p><strong>Caller obligation:</strong> before trusting a parsed profile, the caller must confirm the
 * response belongs to the token it just validated — {@code profile.subject().equals(validatedToken.subject())}
 * — since a {@code /user} response fetched with one token must not be bound to a different identity. This
 * binder deliberately parses only; the correlation is the integrating filter's responsibility.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class GitHubProfileBinding {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GitHubProfileBinding() {
    }

    /**
     * @param json a GitHub {@code /user} JSON response
     * @return the parsed profile
     * @throws IllegalArgumentException if {@code json} is not a well-formed {@code /user} response
     */
    public static GitHubUserProfile parse(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("not a well-formed GitHub /user response");
            }
            JsonNode id = node.get("id");
            if (id == null || !id.isNumber()) {
                // GitHub always returns a numeric account id. Reject a missing, string, object or array id
                // rather than stringifying it into the subject — the same guard GitHubTokenValidator applies.
                throw new IllegalArgumentException("GitHub /user response has no numeric id");
            }
            return new GitHubUserProfile(
                    id.asText(),
                    text(node, "login"),
                    text(node, "name"),
                    text(node, "email"),
                    text(node, "avatar_url"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("not a well-formed GitHub /user response", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
