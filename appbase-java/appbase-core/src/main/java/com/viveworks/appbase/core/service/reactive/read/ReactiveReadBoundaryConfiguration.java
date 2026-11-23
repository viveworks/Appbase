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

package com.viveworks.appbase.core.service.reactive.read;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link ReactiveReadBoundary}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@ReactiveReadBoundaryConfiguration.Valid
@JsonDeserialize(builder = ReactiveReadBoundaryConfiguration.Builder.class)
public final class ReactiveReadBoundaryConfiguration {
    private final int maxPageSize;
    private final int offloadTimeoutSeconds;

    @Nullable
    private final String offloadSchedulerId;

    private ReactiveReadBoundaryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ReactiveReadBoundaryConfigurationValidator.validate(
                builder.maxPageSize,
                builder.offloadTimeoutSeconds);

        maxPageSize = builder.maxPageSize;
        offloadTimeoutSeconds = builder.offloadTimeoutSeconds;
        offloadSchedulerId = builder.offloadSchedulerId;
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
     * Get the maximum number of items returned by a single bounded read.
     *
     * @return max page size
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * Get the deadline, in seconds, after which an offloaded read is timed out.
     *
     * @return offload timeout, in seconds
     */
    public int getOffloadTimeoutSeconds() {
        return offloadTimeoutSeconds;
    }

    /**
     * Get the id of the {@link reactor.core.scheduler.Scheduler} (resolved via
     * {@link com.viveworks.appbase.core.service.reactive.reactor.ReactorService#getSchedulerById(String)})
     * that offloaded reads run on. {@code null} or blank means {@link reactor.core.scheduler.Schedulers#boundedElastic()}.
     *
     * @return offload scheduler id, or {@code null}
     */
    @Nullable
    public String getOffloadSchedulerId() {
        return offloadSchedulerId;
    }

    /**
     * Convenience accessor for {@link #getOffloadTimeoutSeconds()} as a {@link Duration}.
     *
     * @return offload timeout duration
     */
    public Duration timeout() {
        return Duration.ofSeconds(offloadTimeoutSeconds);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReactiveReadBoundaryConfiguration that = (ReactiveReadBoundaryConfiguration) o;
        return maxPageSize == that.maxPageSize &&
                offloadTimeoutSeconds == that.offloadTimeoutSeconds &&
                Objects.equals(offloadSchedulerId, that.offloadSchedulerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxPageSize, offloadTimeoutSeconds, offloadSchedulerId);
    }

    @Override
    public String toString() {
        return "ReactiveReadBoundaryConfiguration{" +
                "maxPageSize=" + maxPageSize +
                ", offloadTimeoutSeconds=" + offloadTimeoutSeconds +
                ", offloadSchedulerId='" + offloadSchedulerId + '\'' +
                '}';
    }

    /**
     * {@link ReactiveReadBoundaryConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings(value = "FCBL_FIELD_COULD_BE_LOCAL",
            justification = "Builder fields require instance scope for Jackson deserialization via @JsonPOJOBuilder")
    public static final class Builder {
        private static final int DEFAULT_MAX_PAGE_SIZE = 1000;
        private static final int DEFAULT_OFFLOAD_TIMEOUT_SECONDS = 30;
        private static final String DEFAULT_OFFLOAD_SCHEDULER_ID = null;

        private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;
        private int offloadTimeoutSeconds = DEFAULT_OFFLOAD_TIMEOUT_SECONDS;

        @Nullable
        private String offloadSchedulerId = DEFAULT_OFFLOAD_SCHEDULER_ID;

        private Builder() {
        }

        public Builder maxPageSize(final int maxPageSize) {
            this.maxPageSize = maxPageSize;
            return this;
        }

        public Builder offloadTimeoutSeconds(final int offloadTimeoutSeconds) {
            this.offloadTimeoutSeconds = offloadTimeoutSeconds;
            return this;
        }

        public Builder offloadSchedulerId(@Nullable final String offloadSchedulerId) {
            this.offloadSchedulerId = offloadSchedulerId;
            return this;
        }

        public ReactiveReadBoundaryConfiguration build() {
            return new ReactiveReadBoundaryConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ReactiveReadBoundaryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ReactiveReadBoundaryConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.reactive.read." +
                "ReactiveReadBoundaryConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
