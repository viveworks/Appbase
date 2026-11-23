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

package com.viveworks.appbase.service.web.oauth.microsoft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Entra profile and group bindings map representative Microsoft Graph responses onto the module's
 * bounded model. Graph returns the profile at {@code /me} and group membership at {@code /me/memberOf} —
 * an OData collection whose entries are heterogeneous (groups <em>and</em> directory roles), distinct
 * from Okta's inline {@code groups} name array and Google's Directory-list shape.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class EntraBindingsTest {

    private static final String ME = """
            {
              "id": "9f4c2e1a-0000-4b6d-a111-abcdef012345",
              "userPrincipalName": "ada@contoso.onmicrosoft.com",
              "displayName": "Ada Lovelace",
              "mail": "ada@contoso.com"
            }""";

    private static final String MEMBER_OF = """
            {
              "value": [
                { "@odata.type": "#microsoft.graph.group",
                  "id": "11111111-1111-1111-1111-111111111111",
                  "displayName": "Engineering" },
                { "@odata.type": "#microsoft.graph.directoryRole",
                  "id": "22222222-2222-2222-2222-222222222222",
                  "displayName": "Global Administrator" },
                { "@odata.type": "#microsoft.graph.group",
                  "id": "33333333-3333-3333-3333-333333333333",
                  "displayName": "All Staff" }
              ]
            }""";

    @Test
    void parsesAGraphMeResponse() {
        EntraUserProfile profile = EntraProfileBinding.parse(ME);

        assertEquals("9f4c2e1a-0000-4b6d-a111-abcdef012345", profile.subject());
        assertEquals("ada@contoso.onmicrosoft.com", profile.userPrincipalName());
        assertEquals("Ada Lovelace", profile.displayName());
        assertEquals("ada@contoso.com", profile.mail());
    }

    @Test
    void parsesGroupMembershipAsStableIdsFilteringOutDirectoryRoles() {
        // memberOf is heterogeneous: the directory-role entry must not be bound as a group, and the stable
        // GUID id — not the mutable displayName — is the membership identifier.
        Set<String> memberships = EntraGroupBinding.parse(MEMBER_OF);

        assertEquals(Set.of(
                "11111111-1111-1111-1111-111111111111",
                "33333333-3333-3333-3333-333333333333"), memberships);
    }
}
