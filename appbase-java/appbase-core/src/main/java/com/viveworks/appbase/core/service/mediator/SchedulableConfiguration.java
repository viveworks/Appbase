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

package com.viveworks.appbase.core.service.mediator;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link Schedulable}.
 *
 * <blockquote>
 *     <p>
 *         <b>NOTE:</b> The default values are defined in {@link Builder}.
 *     </p>
 * </blockquote>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@SchedulableConfiguration.Valid
@JsonDeserialize(builder = SchedulableConfiguration.Builder.class)
public final class SchedulableConfiguration {
    private final boolean enabled;
    private final long initialDelay;
    private final long interval;
    private final TimeUnit timeUnit;

    private SchedulableConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        SchedulableConfigurationValidator.validate(
                builder.enabled,
                builder.initialDelay,
                builder.interval,
                builder.timeUnit);

        enabled = builder.enabled;
        initialDelay = builder.initialDelay;
        interval = builder.interval;
        timeUnit = builder.timeUnit;
    }

    /**
     * Get the builder for this object.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public long getInterval() {
        return interval;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SchedulableConfiguration that = (SchedulableConfiguration) o;
        return enabled == that.enabled &&
                timeUnit == that.timeUnit &&
                Objects.equals(initialDelay, that.initialDelay) &&
                Objects.equals(interval, that.interval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, initialDelay, interval, timeUnit);
    }

    @Override
    public String toString() {
        return "SchedulableConfiguration{" +
                "enabled=" + enabled +
                ", initialDelay=" + initialDelay +
                ", interval=" + interval +
                ", timeUnit=" + timeUnit +
                '}';
    }

    /**
     * {@link SchedulableConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_INITIAL_DELAY = 0L;
        private static final long DEFAULT_INTERVAL = 1000L;
        private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

        private boolean enabled = DEFAULT_ENABLED;
        private long initialDelay = DEFAULT_INITIAL_DELAY;
        private long interval = DEFAULT_INTERVAL;
        private TimeUnit timeUnit = DEFAULT_TIME_UNIT;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder initialDelay(final long initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder delay(final long interval) {
            this.interval = interval;
            return this;
        }

        public Builder timeUnit(final TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public SchedulableConfiguration build() {
            return new SchedulableConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link SchedulableConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = SchedulableConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.mediator.SchedulableConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
