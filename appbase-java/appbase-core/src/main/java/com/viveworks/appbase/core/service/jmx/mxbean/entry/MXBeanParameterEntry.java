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

package com.viveworks.appbase.core.service.jmx.mxbean.entry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import javax.management.MBeanParameterInfo;
import java.util.Map;
import java.util.Objects;

/**
 * This class is similar to {@link MBeanParameterInfo}, but more JSON friendly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = MXBeanParameterEntry.Builder.class)
public final class MXBeanParameterEntry {
    private final String name;

    private final String description;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> fieldMap;

    private final String type;

    private MXBeanParameterEntry(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        name = builder.name;
        description = builder.description;
        fieldMap = builder.fieldMap;
        type = builder.type;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convert {@link MBeanParameterInfo} to {@link MXBeanParameterEntry}.
     *
     * @param mBeanParameterInfo MBeanParameterInfo to convert to
     * @return MXBeanParameterEntry
     */
    public static MXBeanParameterEntry from(final MBeanParameterInfo mBeanParameterInfo) {
        if (mBeanParameterInfo != null) {
            return MXBeanParameterEntry
                    .builder()
                    .name(mBeanParameterInfo.getName())
                    .description(mBeanParameterInfo.getDescription())
                    .fieldMap(EntryUtil.descriptor2FieldMap(mBeanParameterInfo.getDescriptor()))
                    .type(mBeanParameterInfo.getType())
                    .build();
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getFieldMap() {
        return fieldMap;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MXBeanParameterEntry that = (MXBeanParameterEntry) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(fieldMap, that.fieldMap) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, fieldMap, type);
    }

    @Override
    public String toString() {
        return "MXBeanParameterEntry{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", fieldMap=" + fieldMap +
                ", type='" + type + '\'' +
                '}';
    }

    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String name;

        private String description;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> fieldMap;

        private String type;

        private Builder() {
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder fieldMap(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> fieldMap)
        {
            this.fieldMap = fieldMap;
            return this;
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public MXBeanParameterEntry build() {
            return new MXBeanParameterEntry(this);
        }
    }
    // CheckStyle: Builder end
}
