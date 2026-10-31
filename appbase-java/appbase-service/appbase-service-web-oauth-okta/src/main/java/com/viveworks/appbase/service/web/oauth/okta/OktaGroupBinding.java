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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Binds the {@code groups} claim of an Okta userinfo response to the set of group names the subject
 * belongs to. Okta returns membership inline as an array of names (distinct from Google's Directory-list
 * shape). Membership read only: it lists groups and exposes no call to create, delete or modify a group or
 * a membership (spec §5, minimum scope).
 *
 * <p><strong>Caller obligation:</strong> these are Okta group <em>display names</em>, not immutable group
 * ids (Okta's userinfo {@code groups} claim carries only names; ids live behind the separate Groups API).
 * Display names can be renamed and, after deletion, reused, so authorization that hard-codes a name
 * (for example {@code groups.contains("Admins")}) inherits that instability — the caller must weigh this
 * before keying access decisions on a name.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class OktaGroupBinding {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OktaGroupBinding() {
    }

    /**
     * @param json an Okta userinfo JSON response carrying a {@code groups} claim
     * @return the set of group names the subject is a member of, in response order
     * @throws IllegalArgumentException if {@code json} is not a well-formed userinfo response
     */
    public static Set<String> parse(String json) {
        try {
            JsonNode groups = MAPPER.readTree(json).path("groups");
            Set<String> memberships = new LinkedHashSet<>();
            for (JsonNode group : groups) {
                if (group != null && !group.isNull()) {
                    memberships.add(group.asText());
                }
            }
            return memberships;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("not a well-formed Okta userinfo response", e);
        }
    }
}
