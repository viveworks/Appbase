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

package com.viveworks.appbase.core.util.lucene;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.apache.lucene.store.MMapDirectory;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Represents {@link MMapDirectory} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@MmapDirectoryConfiguration.Valid
@JsonDeserialize(builder = MmapDirectoryConfiguration.Builder.class)
public final class MmapDirectoryConfiguration {
    private final long maxChunkSize;
    private final boolean useUnmap;
    private final boolean preload;

    private MmapDirectoryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        MmapDirectoryConfigurationValidator.validate(builder.maxChunkSize);

        maxChunkSize = builder.maxChunkSize;
        useUnmap = builder.useUnmap;
        preload = builder.preload;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public long getMaxChunkSize() {
        return maxChunkSize;
    }

    public boolean isUseUnmap() {
        return useUnmap;
    }

    public boolean isPreload() {
        return preload;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MmapDirectoryConfiguration that = (MmapDirectoryConfiguration) o;
        return maxChunkSize == that.maxChunkSize &&
                useUnmap == that.useUnmap &&
                preload == that.preload;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxChunkSize, useUnmap, preload);
    }

    @Override
    public String toString() {
        return "MmapDirectoryConfiguration{" +
                "maxChunkSize=" + maxChunkSize +
                ", useUnmap=" + useUnmap +
                ", preload=" + preload +
                '}';
    }

    /**
     * Builder for {@link MmapDirectoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings(value = "FCBL_FIELD_COULD_BE_LOCAL",
            justification = "Builder fields require instance scope for Jackson deserialization via @JsonPOJOBuilder")
    public static final class Builder {
        private static final long DEFAULT_MAX_CHUNK_SIZE = MMapDirectory.DEFAULT_MAX_CHUNK_SIZE;
        private static final boolean DEFAULT_USE_UNMAP = MMapDirectory.UNMAP_SUPPORTED;
        private static final boolean DEFAULT_PRELOAD = true;

        private long maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;
        private boolean useUnmap = DEFAULT_USE_UNMAP;
        private boolean preload = DEFAULT_PRELOAD;

        private Builder() {
        }

        public Builder maxChunkSize(final long maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
            return this;
        }

        public Builder useUnmap(final boolean useUnmap) {
            this.useUnmap = useUnmap;
            return this;
        }

        public Builder preload(final boolean preload) {
            this.preload = preload;
            return this;
        }

        public MmapDirectoryConfiguration build() {
            return new MmapDirectoryConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link MmapDirectoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = MmapDirectoryConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.lucene." +
                "MmapDirectoryConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
