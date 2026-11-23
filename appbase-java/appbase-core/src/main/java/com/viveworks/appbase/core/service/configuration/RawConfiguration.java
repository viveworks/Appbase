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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a configuration read from a accessor.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RawConfiguration.Builder.class)
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class RawConfiguration {
    private final String name;
    private final byte[] value;
    private final MimeType mimeType;
    private final Map<String, String> context;
    private final long timestamp;

    private RawConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.name, "builder.name");
        AppbaseUtil.notNull(builder.value, "builder.value");
        AppbaseUtil.notNull(builder.mimeType, "builder.mimeType");
        AppbaseUtil.positive(builder.timestamp, "builder.timestamp");

        name = builder.name;
        value = builder.value;
        mimeType = builder.mimeType;
        context = builder.context;
        timestamp = builder.timestamp;
    }

    /**
     * Get the builder used to build this object.
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the source-specific name assigned to these configuration bytes.
     *
     * @return the source-specific name assigned to these configuration bytes
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the mutable byte array retained by this object.
     *
     * @return the mutable byte array retained by this object
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Returns the MIME type used to choose a configuration decoder.
     *
     * @return the MIME type used to choose a configuration decoder
     */
    public MimeType getMimeType() {
        return mimeType;
    }

    /**
     * Returns the context map retained by this object, possibly {@code null}.
     *
     * @return the context map retained by this object, possibly {@code null}
     */
    public Map<String, String> getContext() {
        return context;
    }

    /**
     * Returns the source timestamp associated with the raw value.
     *
     * @return the source timestamp associated with the raw value
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    @SuppressWarnings("checkstyle:MagicNumber")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RawConfiguration that = (RawConfiguration) o;
        return timestamp == that.timestamp &&
                Objects.equals(name, that.name) &&
                Arrays.equals(value, that.value) &&
                Objects.equals(context, that.context) &&
                Objects.equals(mimeType, that.mimeType);
    }

    @Override
    @SuppressWarnings("checkstyle:MagicNumber")
    public int hashCode() {
        final int result = Objects.hash(name, mimeType, timestamp, context);
        return 31 * result + Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "RawConfiguration{" +
                "name='" + name + '\'' +
                ", value=" + AppbaseUtil.OBFUSCATE +
                ", mimeType=" + mimeType +
                ", context=" + AppbaseUtil.OBFUSCATE +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * {@link RawConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String name;
        private byte[] value;
        private MimeType mimeType;
        private Map<String, String> context;
        private long timestamp;

        private Builder() {
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder value(final byte[] value) {
            this.value = value;
            return this;
        }

        public Builder mimeType(final MimeType mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder context(final Map<String, String> context) {
            this.context = context;
            return this;
        }

        public Builder timestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public RawConfiguration build() {
            return new RawConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
