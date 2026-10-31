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
import org.apache.lucene.search.LRUQueryCache;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration for {@link LRUQueryCache}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@LruQueryCacheConfiguration.Valid
@JsonDeserialize(builder = LruQueryCacheConfiguration.Builder.class)
public final class LruQueryCacheConfiguration {
    private final boolean enabled;
    private final int maxQueries;
    private final long maxRamBytesUsed;

    private LruQueryCacheConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        LruQueryCacheConfigurationValidator.validate(
                builder.enabled,
                builder.maxQueries,
                builder.maxRamBytesUsed);

        enabled = builder.enabled;
        maxQueries = builder.maxQueries;
        maxRamBytesUsed = builder.maxRamBytesUsed;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxQueries() {
        return maxQueries;
    }

    public long getMaxRamBytesUsed() {
        return maxRamBytesUsed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LruQueryCacheConfiguration that = (LruQueryCacheConfiguration) o;
        return enabled == that.enabled &&
                maxQueries == that.maxQueries &&
                maxRamBytesUsed == that.maxRamBytesUsed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, maxQueries, maxRamBytesUsed);
    }

    @Override
    public String toString() {
        return "LruQueryCacheConfiguration{" +
                "enabled=" + enabled +
                ", maxQueries=" + maxQueries +
                ", maxRamBytesUsed=" + maxRamBytesUsed +
                '}';
    }

    /**
     * Builder for {@link LruQueryCacheConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final int DEFAULT_MAX_QUERIES = 1000;
        private static final long DEFAULT_MAX_RAM_BYTES_USED = 50 * 1024L * 1024L;

        private boolean enabled = DEFAULT_ENABLED;
        private int maxQueries = DEFAULT_MAX_QUERIES;
        private long maxRamBytesUsed = DEFAULT_MAX_RAM_BYTES_USED;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder maxQueries(final int maxQueries) {
            this.maxQueries = maxQueries;
            return this;
        }

        public Builder maxRamBytesUsed(final long maxRamBytesUsed) {
            this.maxRamBytesUsed = maxRamBytesUsed;
            return this;
        }

        public LruQueryCacheConfiguration build() {
            return new LruQueryCacheConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link LruQueryCacheConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = LruQueryCacheConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.lucene." +
                "LruQueryCacheConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
