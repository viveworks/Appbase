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
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class is similar to {@link MBeanInfo}, but more JSON friendly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = MXBeanEntry.Builder.class)
public final class MXBeanEntry {
    private static final String DEFAULT_DESCRIPTION = "Information on the management interface of the MBean";

    private final String description;

    private final String className;

    @JsonSerialize(contentAs = MXBeanAttributeEntry.class)
    @JsonDeserialize(contentAs = MXBeanAttributeEntry.class)
    private final List<MXBeanAttributeEntry> mxBeanAttributeEntries;

    @JsonSerialize(contentAs = MXBeanOperationEntry.class)
    @JsonDeserialize(contentAs = MXBeanOperationEntry.class)
    private final List<MXBeanOperationEntry> mxBeanOperationEntries;

    @JsonSerialize(contentAs = MXBeanConstructorEntry.class)
    @JsonDeserialize(contentAs = MXBeanConstructorEntry.class)
    private final List<MXBeanConstructorEntry> mxBeanConstructorEntries;

    @JsonSerialize(contentAs = MXBeanNotificationEntry.class)
    @JsonDeserialize(contentAs = MXBeanNotificationEntry.class)
    private final List<MXBeanNotificationEntry> mxBeanNotificationEntries;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> fieldMap;

    private MXBeanEntry(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        description = builder.description;
        className = builder.className;
        mxBeanAttributeEntries = builder.mxBeanAttributeEntries;
        mxBeanOperationEntries = builder.mxBeanOperationEntries;
        mxBeanConstructorEntries = builder.mxBeanConstructorEntries;
        mxBeanNotificationEntries = builder.mxBeanNotificationEntries;
        fieldMap = builder.fieldMap;
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
     * Convert {@link MBeanInfo} to {@link MXBeanEntry}.
     *
     * @param mBeanInfo MBeanInfo
     * @return MXBeanEntry
     */
    public static MXBeanEntry from(final MBeanInfo mBeanInfo) {
        if (mBeanInfo != null) {
            final List<MXBeanAttributeEntry> mxBeanAttributeEntries = new ArrayList<>();
            final MBeanAttributeInfo[] mBeanAttributeInfos = mBeanInfo.getAttributes();
            if (mBeanAttributeInfos != null) {
                for (final MBeanAttributeInfo mBeanAttributeInfo : mBeanAttributeInfos) {
                    if (mBeanAttributeInfo != null) {
                        final MXBeanAttributeEntry mxBeanAttributeEntry = MXBeanAttributeEntry.from(mBeanAttributeInfo);
                        mxBeanAttributeEntries.add(mxBeanAttributeEntry);
                    }
                }
            }

            final List<MXBeanConstructorEntry> mxBeanConstructorEntries = new ArrayList<>();
            final MBeanConstructorInfo[] mBeanConstructorInfos = mBeanInfo.getConstructors();
            if (mBeanConstructorInfos != null) {
                for (final MBeanConstructorInfo mBeanConstructorInfo : mBeanConstructorInfos) {
                    if (mBeanConstructorInfo != null) {
                        final MXBeanConstructorEntry mxBeanConstructorEntry =
                                MXBeanConstructorEntry.from(mBeanConstructorInfo);
                        mxBeanConstructorEntries.add(mxBeanConstructorEntry);
                    }
                }
            }

            final List<MXBeanNotificationEntry> mxBeanNotificationEntries = new ArrayList<>();
            final MBeanNotificationInfo[] mBeanNotificationInfos = mBeanInfo.getNotifications();
            if (mBeanNotificationInfos != null) {
                for (final MBeanNotificationInfo mBeanNotificationInfo : mBeanNotificationInfos) {
                    if (mBeanNotificationInfo != null) {
                        final MXBeanNotificationEntry mxBeanNotificationEntry =
                                MXBeanNotificationEntry.from(mBeanNotificationInfo);
                        mxBeanNotificationEntries.add(mxBeanNotificationEntry);
                    }
                }
            }

            final List<MXBeanOperationEntry> mxBeanOperationEntries = new ArrayList<>();
            final MBeanOperationInfo[] mBeanOperationInfos = mBeanInfo.getOperations();
            if (mBeanOperationInfos != null) {
                for (final MBeanOperationInfo mBeanOperationInfo : mBeanOperationInfos) {
                    if (mBeanOperationInfo != null) {
                        final MXBeanOperationEntry mxBeanOperationEntry =
                                MXBeanOperationEntry.from(mBeanOperationInfo);
                        mxBeanOperationEntries.add(mxBeanOperationEntry);
                    }
                }
            }

            // Information on the management interface of the MBean
            String description = mBeanInfo.getDescription();
            if (description == null || DEFAULT_DESCRIPTION.equals(description.trim())) {
                description = mBeanInfo.getClassName();
            }

            return MXBeanEntry
                    .builder()
                    .className(mBeanInfo.getClassName())
                    .description(description)
                    .fieldMap(EntryUtil.descriptor2FieldMap(mBeanInfo.getDescriptor()))
                    .mxBeanAttributeEntries(Collections.unmodifiableList(mxBeanAttributeEntries))
                    .mxBeanConstructorEntries(Collections.unmodifiableList(mxBeanConstructorEntries))
                    .mxBeanNotificationEntries(Collections.unmodifiableList(mxBeanNotificationEntries))
                    .mxBeanOperationEntries(Collections.unmodifiableList(mxBeanOperationEntries))
                    .build();
        }

        return null;
    }

    public String getDescription() {
        return description;
    }

    public String getClassName() {
        return className;
    }

    public List<MXBeanAttributeEntry> getMxBeanAttributeEntries() {
        return mxBeanAttributeEntries;
    }

    public List<MXBeanOperationEntry> getMxBeanOperationEntries() {
        return mxBeanOperationEntries;
    }

    public List<MXBeanConstructorEntry> getMxBeanConstructorEntries() {
        return mxBeanConstructorEntries;
    }

    public List<MXBeanNotificationEntry> getMxBeanNotificationEntries() {
        return mxBeanNotificationEntries;
    }

    public Map<String, String> getFieldMap() {
        return fieldMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MXBeanEntry that = (MXBeanEntry) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(className, that.className) &&
                Objects.equals(mxBeanAttributeEntries, that.mxBeanAttributeEntries) &&
                Objects.equals(mxBeanOperationEntries, that.mxBeanOperationEntries) &&
                Objects.equals(mxBeanConstructorEntries, that.mxBeanConstructorEntries) &&
                Objects.equals(mxBeanNotificationEntries, that.mxBeanNotificationEntries) &&
                Objects.equals(fieldMap, that.fieldMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                description,
                className,
                mxBeanAttributeEntries,
                mxBeanOperationEntries,
                mxBeanConstructorEntries,
                mxBeanNotificationEntries,
                fieldMap);
    }

    @Override
    public String toString() {
        return "MXBeanEntry{" +
                "description='" + description + '\'' +
                ", className='" + className + '\'' +
                ", mxBeanAttributeEntries=" + mxBeanAttributeEntries +
                ", mxBeanOperationEntries=" + mxBeanOperationEntries +
                ", mxBeanConstructorEntries=" + mxBeanConstructorEntries +
                ", mxBeanNotificationEntries=" + mxBeanNotificationEntries +
                ", fieldMap=" + fieldMap +
                '}';
    }

    /**
     * Builder for {@link MXBeanEntry}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String description;

        private String className;

        @JsonSerialize(contentAs = MXBeanAttributeEntry.class)
        @JsonDeserialize(contentAs = MXBeanAttributeEntry.class)
        private List<MXBeanAttributeEntry> mxBeanAttributeEntries;

        @JsonSerialize(contentAs = MXBeanOperationEntry.class)
        @JsonDeserialize(contentAs = MXBeanOperationEntry.class)
        private List<MXBeanOperationEntry> mxBeanOperationEntries;

        @JsonSerialize(contentAs = MXBeanConstructorEntry.class)
        @JsonDeserialize(contentAs = MXBeanConstructorEntry.class)
        private List<MXBeanConstructorEntry> mxBeanConstructorEntries;

        @JsonSerialize(contentAs = MXBeanNotificationEntry.class)
        @JsonDeserialize(contentAs = MXBeanNotificationEntry.class)
        private List<MXBeanNotificationEntry> mxBeanNotificationEntries;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> fieldMap;

        private Builder() {
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder className(final String className) {
            this.className = className;
            return this;
        }

        public Builder mxBeanAttributeEntries(
                @JsonSerialize(contentAs = MXBeanAttributeEntry.class)
                @JsonDeserialize(contentAs = MXBeanAttributeEntry.class)
                final List<MXBeanAttributeEntry> mxBeanAttributeEntries)
        {
            this.mxBeanAttributeEntries = mxBeanAttributeEntries;
            return this;
        }

        public Builder mxBeanOperationEntries(
                @JsonSerialize(contentAs = MXBeanOperationEntry.class)
                @JsonDeserialize(contentAs = MXBeanOperationEntry.class)
                final List<MXBeanOperationEntry> mxBeanOperationEntries)
        {
            this.mxBeanOperationEntries = mxBeanOperationEntries;
            return this;
        }

        public Builder mxBeanConstructorEntries(
                @JsonSerialize(contentAs = MXBeanConstructorEntry.class)
                @JsonDeserialize(contentAs = MXBeanConstructorEntry.class)
                final List<MXBeanConstructorEntry> mxBeanConstructorEntries)
        {
            this.mxBeanConstructorEntries = mxBeanConstructorEntries;
            return this;
        }

        public Builder mxBeanNotificationEntries(
                @JsonSerialize(contentAs = MXBeanNotificationEntry.class)
                @JsonDeserialize(contentAs = MXBeanNotificationEntry.class)
                final List<MXBeanNotificationEntry> mxBeanNotificationEntries)
        {
            this.mxBeanNotificationEntries = mxBeanNotificationEntries;
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

        public MXBeanEntry build() {
            return new MXBeanEntry(this);
        }
    }
    // CheckStyle: Builder end
}
