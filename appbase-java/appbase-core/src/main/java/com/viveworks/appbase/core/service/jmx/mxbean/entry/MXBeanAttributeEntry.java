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
import javax.management.MBeanAttributeInfo;
import java.util.Map;
import java.util.Objects;

/**
 * This class is similar to {@link MBeanAttributeInfo}, but more JSON friendly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = MXBeanAttributeEntry.Builder.class)
public final class MXBeanAttributeEntry {
    private final String name;

    private final String description;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> fieldMap;

    private final String attributeType;

    private final boolean writable;

    private final boolean readable;

    private final boolean is;

    private MXBeanAttributeEntry(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        name = builder.name;
        description = builder.description;
        fieldMap = builder.fieldMap;
        attributeType = builder.attributeType;
        writable = builder.writable;
        readable = builder.readable;
        is = builder.is;
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
     * Convert {@link MBeanAttributeInfo} to {@link MXBeanAttributeEntry}.
     *
     * @param mBeanAttributeInfo MBeanAttributeInfo
     * @return MXBeanAttributeEntry
     */
    public static MXBeanAttributeEntry from(final MBeanAttributeInfo mBeanAttributeInfo) {
        if (mBeanAttributeInfo != null) {
            return MXBeanAttributeEntry
                    .builder()
                    .name(mBeanAttributeInfo.getName())
                    .description(mBeanAttributeInfo.getDescription())
                    .attributeType(mBeanAttributeInfo.getType())
                    .fieldMap(EntryUtil.descriptor2FieldMap(mBeanAttributeInfo.getDescriptor()))
                    .is(mBeanAttributeInfo.isIs())
                    .readable(mBeanAttributeInfo.isReadable())
                    .writable(mBeanAttributeInfo.isWritable())
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

    public String getAttributeType() {
        return attributeType;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isIs() {
        return is;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MXBeanAttributeEntry that = (MXBeanAttributeEntry) o;
        return writable == that.writable &&
                readable == that.readable &&
                is == that.is &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(fieldMap, that.fieldMap) &&
                Objects.equals(attributeType, that.attributeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, fieldMap, attributeType, writable, readable, is);
    }

    @Override
    public String toString() {
        return "MXBeanAttributeEntry{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", fieldMap=" + fieldMap +
                ", attributeType='" + attributeType + '\'' +
                ", writable=" + writable +
                ", readable=" + readable +
                ", is=" + is +
                '}';
    }

    /**
     * Builder for {@link MXBeanAttributeEntry}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String name;

        private String description;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> fieldMap;

        private String attributeType;

        private boolean writable;

        private boolean readable;

        private boolean is;

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

        public Builder attributeType(final String attributeType) {
            this.attributeType = attributeType;
            return this;
        }

        public Builder writable(final boolean writable) {
            this.writable = writable;
            return this;
        }

        public Builder readable(final boolean readable) {
            this.readable = readable;
            return this;
        }

        public Builder is(final boolean is) {
            this.is = is;
            return this;
        }

        public MXBeanAttributeEntry build() {
            return new MXBeanAttributeEntry(this);
        }
    }
    // CheckStyle: Builder end
}
