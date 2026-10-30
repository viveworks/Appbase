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

package com.viveworks.appbase.service.web.oauth.okta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Okta profile and group bindings map a representative userinfo response onto the module's bounded
 * model. Okta returns group membership as a {@code groups} claim (an array of names), distinct from
 * Google's Directory-list shape.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class OktaBindingsTest {

    private static final String USERINFO = """
            {
              "sub": "00u1a2b3c4d5e6f7g8h9",
              "email": "ada@example.com",
              "email_verified": true,
              "name": "Ada Lovelace",
              "preferred_username": "ada@example.com",
              "groups": ["Engineering", "All Staff"]
            }""";

    @Test
    void parsesAnOktaUserInfoResponse() {
        OktaUserProfile profile = OktaProfileBinding.parse(USERINFO);

        assertEquals("00u1a2b3c4d5e6f7g8h9", profile.subject());
        assertEquals("ada@example.com", profile.email());
        assertTrue(profile.emailVerified());
        assertEquals("Ada Lovelace", profile.name());
        assertEquals("ada@example.com", profile.preferredUsername());
    }

    @Test
    void parsesGroupMembershipFromTheGroupsClaim() {
        Set<String> memberships = OktaGroupBinding.parse(USERINFO);

        assertEquals(Set.of("Engineering", "All Staff"), memberships);
    }
}
