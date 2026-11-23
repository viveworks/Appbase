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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Binds a GitHub {@code /user/teams} response to the set of {@code org/team} slugs the subject belongs to.
 * Membership read only: it lists team memberships and exposes no call to create, delete or modify a team,
 * an org or a membership (spec §5, minimum scope).
 *
 * <p>Each element is {@code <organization login>/<team slug>}, in response order. An entry missing either
 * its team slug or its organization login is skipped rather than producing a half-formed identifier.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class GitHubOrgTeamBinding {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GitHubOrgTeamBinding() {
    }

    /**
     * @param json a GitHub {@code /user/teams} JSON response (an array of team objects)
     * @return the {@code org/team} slugs the subject is a member of, in response order
     * @throws IllegalArgumentException if {@code json} is not a well-formed teams response
     */
    public static Set<String> parse(String json) {
        try {
            JsonNode teams = MAPPER.readTree(json);
            if (teams == null || !teams.isArray()) {
                throw new IllegalArgumentException("not a well-formed GitHub /user/teams response");
            }
            Set<String> memberships = new LinkedHashSet<>();
            for (JsonNode team : teams) {
                String slug = text(team, "slug");
                String org = text(team.path("organization"), "login");
                if (slug != null && org != null) {
                    memberships.add(org + "/" + slug);
                }
            }
            return memberships;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("not a well-formed GitHub /user/teams response", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
