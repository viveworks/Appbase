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

package com.viveworks.appbase.core.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;

/**
 * Contains {@link List} of {@link String}s used to make it easier to annotate {@code Jackson} annotations
 * in nested collections.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = StringList.Builder.class)
public final class StringList {
    private final List<String> strings;

    private StringList(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

       strings = builder.strings;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public List<String> getStrings() {
        return strings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StringList that = (StringList) o;
        return Objects.equals(strings, that.strings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strings);
    }

    @Override
    public String toString() {
        return "StringList{" +
                "strings=" + strings +
                '}';
    }


    /**
     * Builder for {@link StringList}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private List<String> strings;

        private Builder() {
        }

        public Builder strings(final List<String> strings) {
            this.strings = strings;
            return this;
        }

        public StringList build() {
            return new StringList(this);
        }
    }
    // CheckStyle: Builder end
}
