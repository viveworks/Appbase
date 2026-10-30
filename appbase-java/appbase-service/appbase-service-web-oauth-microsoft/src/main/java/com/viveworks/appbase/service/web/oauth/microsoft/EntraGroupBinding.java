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

package com.viveworks.appbase.service.web.oauth.microsoft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Binds a Microsoft Graph {@code /me/memberOf} response to the set of group ids the subject belongs to.
 * The OData {@code value} collection is heterogeneous — it carries directory roles and administrative
 * units alongside groups — so this binder keeps only entries typed {@code #microsoft.graph.group} and
 * binds each group's <em>id</em>, not its display name. Membership read only: it lists groups and exposes
 * no call to create, delete or modify a group or a membership (spec §5, minimum scope).
 *
 * <p><strong>Why the id, not the display name:</strong> the inverse of Okta's binding. An Entra group's
 * {@code id} is an immutable directory GUID, whereas its {@code displayName} can be renamed and reused;
 * authorization keyed on the id is therefore stable, while a name is not. The binder deliberately drops
 * non-group directory objects so a directory <em>role</em> (for example "Global Administrator") is never
 * mistaken for a group membership.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class EntraGroupBinding {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The OData type discriminator Graph stamps on a group entry in a {@code memberOf} collection. */
    private static final String GROUP_ODATA_TYPE = "#microsoft.graph.group";

    private EntraGroupBinding() {
    }

    /**
     * @param json a Microsoft Graph {@code /me/memberOf} JSON response
     * @return the set of group ids (immutable directory GUIDs) the subject is a member of, in response
     *         order, excluding directory roles and other non-group directory objects
     * @throws IllegalArgumentException if {@code json} is not a well-formed {@code memberOf} response
     */
    public static Set<String> parse(String json) {
        try {
            JsonNode value = MAPPER.readTree(json).path("value");
            Set<String> memberships = new LinkedHashSet<>();
            for (JsonNode entry : value) {
                if (!GROUP_ODATA_TYPE.equals(text(entry, "@odata.type"))) {
                    continue;
                }
                String id = text(entry, "id");
                if (id != null) {
                    memberships.add(id);
                }
            }
            return memberships;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "not a well-formed Microsoft Graph /me/memberOf response", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode fieldValue = node.get(field);
        return fieldValue == null || fieldValue.isNull() ? null : fieldValue.asText();
    }
}
