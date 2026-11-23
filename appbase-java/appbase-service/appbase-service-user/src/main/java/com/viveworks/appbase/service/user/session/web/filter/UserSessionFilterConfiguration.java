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

package com.viveworks.appbase.service.user.session.web.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = UserSessionFilterConfiguration.Builder.class)
public class UserSessionFilterConfiguration implements AppbaseConfiguration {
    @JsonSerialize(keyAs = String.class, contentAs = Boolean.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Boolean.class)
    private final Map<String, Boolean> createSessionIfAbsentByPathMap;

    private UserSessionFilterConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        // Canonicalized here, in the one constructor both Jackson's @JsonDeserialize(builder = ...)
        // and programmatic builder() construction reach, so a key authored as /%61dmin matches a
        // request canonicalized to /admin. A key that cannot canonicalize fails the build rather than
        // sitting inert in the map -- see AppbasePathCanonicalizer.canonicalizeKeys.
        createSessionIfAbsentByPathMap = AppbasePathCanonicalizer.canonicalizeKeys(builder.createSessionIfAbsentByPathMap);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Boolean> getCreateSessionIfAbsentByPathMap() {
        return createSessionIfAbsentByPathMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UserSessionFilterConfiguration that = (UserSessionFilterConfiguration) o;
        return Objects.equals(createSessionIfAbsentByPathMap, that.createSessionIfAbsentByPathMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createSessionIfAbsentByPathMap);
    }

    @Override
    public String toString() {
        return "UserSessionFilterConfiguration{" +
                "createSessionIfAbsentByPathMap=" + createSessionIfAbsentByPathMap +
                '}';
    }

    /**
     * Builder for {@link UserSessionFilterConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private Map<String, Boolean> createSessionIfAbsentByPathMap;

        private Builder() {
        }

        public Builder createSessionIfAbsentByPathMap(final Map<String, Boolean> createSessionIfAbsentByPathMap) {
            this.createSessionIfAbsentByPathMap = createSessionIfAbsentByPathMap;
            return this;
        }

        public UserSessionFilterConfiguration build() {
            return new UserSessionFilterConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
