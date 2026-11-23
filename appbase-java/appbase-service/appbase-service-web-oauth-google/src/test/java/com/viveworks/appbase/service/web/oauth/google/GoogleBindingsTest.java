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

package com.viveworks.appbase.service.web.oauth.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Google profile and group bindings map a representative userinfo / Directory-API response onto the
 * module's bounded model — profile fields and group memberships only, no management surface.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class GoogleBindingsTest {

    @Test
    void parsesAGoogleUserInfoResponse() {
        String userinfo = """
                {
                  "sub": "108273649182736451827",
                  "email": "ada@example.com",
                  "email_verified": true,
                  "name": "Ada Lovelace",
                  "picture": "https://lh3.googleusercontent.com/a/xyz"
                }""";

        GoogleUserProfile profile = GoogleProfileBinding.parse(userinfo);

        assertEquals("108273649182736451827", profile.subject());
        assertEquals("ada@example.com", profile.email());
        assertTrue(profile.emailVerified());
        assertEquals("Ada Lovelace", profile.name());
        assertEquals("https://lh3.googleusercontent.com/a/xyz", profile.picture());
    }

    @Test
    void parsesGroupMembershipFromADirectoryListResponse() {
        String groups = """
                {
                  "groups": [
                    {"email": "eng@example.com", "name": "Engineering"},
                    {"email": "all@example.com", "name": "All Staff"}
                  ]
                }""";

        Set<String> memberships = GoogleGroupBinding.parse(groups);

        assertEquals(Set.of("eng@example.com", "all@example.com"), memberships);
    }
}
