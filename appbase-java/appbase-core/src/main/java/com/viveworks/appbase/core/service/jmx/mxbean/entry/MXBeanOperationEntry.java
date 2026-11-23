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
import javax.management.MBeanOperationInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class is similar to {@link MBeanOperationInfo}, but more JSON friendly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = MXBeanOperationEntry.Builder.class)
public final class MXBeanOperationEntry {
    private final String name;

    private final String description;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> fieldMap;

    @JsonSerialize(contentAs = MXBeanParameterEntry.class)
    @JsonDeserialize(contentAs = MXBeanParameterEntry.class)
    private final List<MXBeanParameterEntry> signature;

    private final String returnType;

    private final int impact;

    private MXBeanOperationEntry(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        name = builder.name;
        description = builder.description;
        fieldMap = builder.fieldMap;
        signature = builder.signature;
        returnType = builder.returnType;
        impact = builder.impact;
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
     * Convert {@link MBeanOperationInfo} to {@link MXBeanOperationEntry}.
     *
     * @param mBeanOperationInfo MBeanOperationInfo
     * @return MXBeanOperationEntry
     */
    public static MXBeanOperationEntry from(final MBeanOperationInfo mBeanOperationInfo) {
        if (mBeanOperationInfo != null) {
            return MXBeanOperationEntry
                    .builder()
                    .name(mBeanOperationInfo.getName())
                    .description(mBeanOperationInfo.getDescription())
                    .fieldMap(EntryUtil.descriptor2FieldMap(mBeanOperationInfo.getDescriptor()))
                    .impact(mBeanOperationInfo.getImpact())
                    .returnType(mBeanOperationInfo.getReturnType())
                    .signature(EntryUtil.signature2MxBeanParameterEntries(mBeanOperationInfo.getSignature()))
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

    public List<MXBeanParameterEntry> getSignature() {
        return signature;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getImpact() {
        return impact;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MXBeanOperationEntry that = (MXBeanOperationEntry) o;
        return impact == that.impact &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(fieldMap, that.fieldMap) &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, fieldMap, signature, returnType, impact);
    }

    @Override
    public String toString() {
        return "MXBeanOperationEntry{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", fieldMap=" + fieldMap +
                ", signature=" + signature +
                ", returnType='" + returnType + '\'' +
                ", impact=" + impact +
                '}';
    }

    /**
     * Builder for {@link MXBeanOperationEntry}.
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

        @JsonSerialize(contentAs = MXBeanParameterEntry.class)
        @JsonDeserialize(contentAs = MXBeanParameterEntry.class)
        private List<MXBeanParameterEntry> signature;

        private String returnType;

        private int impact;

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

        public Builder signature(
                @JsonSerialize(contentAs = MXBeanParameterEntry.class)
                @JsonDeserialize(contentAs = MXBeanParameterEntry.class)
                final List<MXBeanParameterEntry> signature)
        {
            this.signature = signature;
            return this;
        }

        public Builder returnType(final String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder impact(final int impact) {
            this.impact = impact;
            return this;
        }

        public MXBeanOperationEntry build() {
            return new MXBeanOperationEntry(this);
        }
    }
    // CheckStyle: Builder end
}
