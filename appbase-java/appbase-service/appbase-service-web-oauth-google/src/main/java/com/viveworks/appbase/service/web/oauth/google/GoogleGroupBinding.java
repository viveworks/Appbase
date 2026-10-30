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

package com.viveworks.appbase.service.web.oauth.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Binds a Google Directory API {@code groups.list} response to the set of group identifiers (group emails)
 * the subject belongs to. Membership read only: it lists groups and exposes no call to create, delete or
 * modify a group or a membership (spec §5, minimum scope).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class GoogleGroupBinding {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GoogleGroupBinding() {
    }

    /**
     * @param json a Google Directory API {@code groups.list} JSON response
     * @return the set of group emails the subject is a member of, in response order
     * @throws IllegalArgumentException if {@code json} is not a well-formed groups response
     */
    public static Set<String> parse(String json) {
        try {
            JsonNode groups = MAPPER.readTree(json).path("groups");
            Set<String> memberships = new LinkedHashSet<>();
            for (JsonNode group : groups) {
                JsonNode email = group.get("email");
                if (email != null && !email.isNull()) {
                    memberships.add(email.asText());
                }
            }
            return memberships;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("not a well-formed Google groups response", e);
        }
    }
}
