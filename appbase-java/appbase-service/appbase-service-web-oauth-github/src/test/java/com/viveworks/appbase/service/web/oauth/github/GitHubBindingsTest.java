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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the GitHub read-only bindings (task internal work item 1197, spec §5 GitHub): the profile binding parses a
 * {@code /user} response to a bounded {@link GitHubUserProfile}, and the org/team binding parses a
 * {@code /user/teams} response to the set of {@code org/team} slugs the subject belongs to. Both are
 * parse-only and read-only — the scope guard enforces the absence of any mutation surface.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class GitHubBindingsTest {

    @Test
    void profileBindingParsesTheBoundedUserFields() {
        String json = "{\"id\":42,\"login\":\"octocat\",\"name\":\"The Octocat\","
                + "\"email\":\"octo@github.com\",\"avatar_url\":\"https://avatars/u/42\","
                + "\"site_admin\":true}";

        GitHubUserProfile profile = GitHubProfileBinding.parse(json);

        assertEquals("42", profile.subject());
        assertEquals("octocat", profile.login());
        assertEquals("The Octocat", profile.name());
        assertEquals("octo@github.com", profile.email());
        assertEquals("https://avatars/u/42", profile.avatarUrl());
    }

    @Test
    void profileBindingReturnsNullForAbsentOptionalFields() {
        GitHubUserProfile profile = GitHubProfileBinding.parse("{\"id\":42,\"login\":\"octocat\"}");

        assertEquals("42", profile.subject());
        assertEquals("octocat", profile.login());
        assertEquals(null, profile.email());
    }

    @Test
    void profileBindingRejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> GitHubProfileBinding.parse("not json"));
    }

    @Test
    void profileBindingRejectsNonNumericOrMissingId() {
        // GitHub always returns a numeric id. A non-number (object/array/string) or an absent id must be
        // rejected, not stringified into the subject — the same guard the validator applies to /user.
        for (String body : new String[] {
                "{\"id\":{\"x\":\"y\"},\"login\":\"a\"}",   // object id
                "{\"id\":[1,2],\"login\":\"a\"}",           // array id
                "{\"id\":\"42\",\"login\":\"a\"}",          // string id
                "{\"login\":\"a\"}"}) {                     // missing id
            assertThrows(IllegalArgumentException.class, () -> GitHubProfileBinding.parse(body),
                    "must reject a non-numeric or missing id: " + body);
        }
    }

    @Test
    void orgTeamBindingParsesOrgSlashTeamSlugsInOrder() {
        String json = "[{\"slug\":\"admins\",\"organization\":{\"login\":\"acme\"}},"
                + "{\"slug\":\"reviewers\",\"organization\":{\"login\":\"acme\"}},"
                + "{\"slug\":\"core\",\"organization\":{\"login\":\"widgets\"}}]";

        Set<String> memberships = GitHubOrgTeamBinding.parse(json);

        assertIterableEquals(List.of("acme/admins", "acme/reviewers", "widgets/core"), memberships);
    }

    @Test
    void orgTeamBindingSkipsEntriesMissingOrgOrSlug() {
        String json = "[{\"slug\":\"admins\",\"organization\":{\"login\":\"acme\"}},"
                + "{\"slug\":\"orphan\"},"                                   // no organization
                + "{\"organization\":{\"login\":\"acme\"}}]";                 // no slug

        Set<String> memberships = GitHubOrgTeamBinding.parse(json);

        assertIterableEquals(List.of("acme/admins"), memberships);
    }

    @Test
    void orgTeamBindingReturnsEmptyForNoTeams() {
        assertTrue(GitHubOrgTeamBinding.parse("[]").isEmpty());
    }

    @Test
    void orgTeamBindingRejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> GitHubOrgTeamBinding.parse("not json"));
    }
}
