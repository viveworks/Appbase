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

package com.viveworks.appbase.client.service.r2dbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.r2dbc.pool.ConnectionPoolConfiguration;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for the native {@code io.r2dbc.pool.ConnectionPool} built per key. Field names
 * spell out their unit ({@code Seconds}) since {@link ConnectionPoolConfiguration.Builder} takes
 * {@link java.time.Duration}, and YAML has no native duration type.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = R2dbcPoolConfiguration.Builder.class)
public final class R2dbcPoolConfiguration {
    private final Integer maxSize;

    private final Integer initialSize;

    @Nullable
    private final Long maxIdleTimeSeconds;

    @Nullable
    private final Long maxAcquireTimeSeconds;

    @Nullable
    private final String validationQuery;


    private R2dbcPoolConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.maxSize, "builder.maxSize");
        AppbaseUtil.notNull(builder.initialSize, "builder.initialSize");

        maxSize = builder.maxSize;
        initialSize = builder.initialSize;
        maxIdleTimeSeconds = builder.maxIdleTimeSeconds;
        maxAcquireTimeSeconds = builder.maxAcquireTimeSeconds;
        validationQuery = builder.validationQuery;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public Integer getInitialSize() {
        return initialSize;
    }

    @Nullable
    public Long getMaxIdleTimeSeconds() {
        return maxIdleTimeSeconds;
    }

    @Nullable
    public Long getMaxAcquireTimeSeconds() {
        return maxAcquireTimeSeconds;
    }

    @Nullable
    public String getValidationQuery() {
        return validationQuery;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final R2dbcPoolConfiguration that = (R2dbcPoolConfiguration) o;
        return Objects.equals(maxSize, that.maxSize) &&
                Objects.equals(initialSize, that.initialSize) &&
                Objects.equals(maxIdleTimeSeconds, that.maxIdleTimeSeconds) &&
                Objects.equals(maxAcquireTimeSeconds, that.maxAcquireTimeSeconds) &&
                Objects.equals(validationQuery, that.validationQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxSize, initialSize, maxIdleTimeSeconds, maxAcquireTimeSeconds, validationQuery);
    }

    @Override
    public String toString() {
        return "R2dbcPoolConfiguration{" +
                "maxSize=" + maxSize +
                ", initialSize=" + initialSize +
                ", maxIdleTimeSeconds=" + maxIdleTimeSeconds +
                ", maxAcquireTimeSeconds=" + maxAcquireTimeSeconds +
                ", validationQuery='" + validationQuery + '\'' +
                '}';
    }


    /**
     * Builder for {@link R2dbcPoolConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final int DEFAULT_MAX_SIZE = 10;
        private static final int DEFAULT_INITIAL_SIZE = 0;
        private static final long DEFAULT_MAX_IDLE_TIME_SECONDS = 30L;


        private Integer maxSize = DEFAULT_MAX_SIZE;

        private Integer initialSize = DEFAULT_INITIAL_SIZE;

        private Long maxIdleTimeSeconds = DEFAULT_MAX_IDLE_TIME_SECONDS;

        @Nullable
        private Long maxAcquireTimeSeconds;

        @Nullable
        private String validationQuery;


        private Builder() {
        }

        public Builder maxSize(final Integer maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder initialSize(final Integer initialSize) {
            this.initialSize = initialSize;
            return this;
        }

        public Builder maxIdleTimeSeconds(@Nullable final Long maxIdleTimeSeconds) {
            this.maxIdleTimeSeconds = maxIdleTimeSeconds;
            return this;
        }

        public Builder maxAcquireTimeSeconds(@Nullable final Long maxAcquireTimeSeconds) {
            this.maxAcquireTimeSeconds = maxAcquireTimeSeconds;
            return this;
        }

        public Builder validationQuery(@Nullable final String validationQuery) {
            this.validationQuery = validationQuery;
            return this;
        }

        public R2dbcPoolConfiguration build() {
            return new R2dbcPoolConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
