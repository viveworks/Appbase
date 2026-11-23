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

package com.viveworks.appbase.core.service.exception.store;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
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
 * Configuration for {@link DefaultExpiredMessageCleaner}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultExpiredMessageCleanerConfiguration.Valid
@JsonDeserialize(builder = DefaultExpiredMessageCleanerConfiguration.Builder.class)
public final class DefaultExpiredMessageCleanerConfiguration {
    private final boolean enabled;
    private final long exclusiveRemoveBeforeTimePeriod;
    private final TimeUnit exclusiveRemoveBeforeTimePeriodUnit;
    private final SchedulableConfiguration schedulableConfiguration;

    private DefaultExpiredMessageCleanerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultExpiredMessageCleanerConfigurationValidator.validate(
                builder.enabled,
                builder.exclusiveRemoveBeforeTimePeriod,
                builder.exclusiveRemoveBeforeTimePeriodUnit,
                builder.schedulableConfiguration);

        enabled = builder.enabled;
        exclusiveRemoveBeforeTimePeriod = builder.exclusiveRemoveBeforeTimePeriod;
        exclusiveRemoveBeforeTimePeriodUnit = builder.exclusiveRemoveBeforeTimePeriodUnit;
        schedulableConfiguration = builder.schedulableConfiguration;
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

    public long getExclusiveRemoveBeforeTimePeriod() {
        return exclusiveRemoveBeforeTimePeriod;
    }

    public TimeUnit getExclusiveRemoveBeforeTimePeriodUnit() {
        return exclusiveRemoveBeforeTimePeriodUnit;
    }

    public SchedulableConfiguration getSchedulableConfiguration() {
        return schedulableConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultExpiredMessageCleanerConfiguration that = (DefaultExpiredMessageCleanerConfiguration) o;
        return Objects.equals(enabled, that.enabled) &&
                Objects.equals(exclusiveRemoveBeforeTimePeriod, that.exclusiveRemoveBeforeTimePeriod) &&
                exclusiveRemoveBeforeTimePeriodUnit == that.exclusiveRemoveBeforeTimePeriodUnit &&
                Objects.equals(schedulableConfiguration, that.schedulableConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                exclusiveRemoveBeforeTimePeriod,
                exclusiveRemoveBeforeTimePeriodUnit,
                schedulableConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultExpiredMessageCleanerConfiguration{" +
                "enabled=" + enabled +
                ", exclusiveRemoveBeforeTimePeriod=" + exclusiveRemoveBeforeTimePeriod +
                ", exclusiveRemoveBeforeTimePeriodUnit=" + exclusiveRemoveBeforeTimePeriodUnit +
                ", schedulableConfiguration=" + schedulableConfiguration +
                '}';
    }

    /**
     * {@link DefaultExpiredMessageCleanerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL", "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION"})
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD = 1L;
        private static final TimeUnit DEFAULT_EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT = TimeUnit.HOURS;
        private static final SchedulableConfiguration DEFAULT_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration
                        .builder()
                        .initialDelay(0L)
                        .delay(1L)
                        .timeUnit(TimeUnit.HOURS)
                        .build();

        private boolean enabled = DEFAULT_ENABLED;
        private long exclusiveRemoveBeforeTimePeriod = DEFAULT_EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD;
        private TimeUnit exclusiveRemoveBeforeTimePeriodUnit = DEFAULT_EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT;
        private SchedulableConfiguration schedulableConfiguration = DEFAULT_SCHEDULABLE_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder exclusiveRemoveBeforeTimePeriod(final long exclusiveRemoveBeforeTimePeriod) {
            this.exclusiveRemoveBeforeTimePeriod = exclusiveRemoveBeforeTimePeriod;
            return this;
        }

        public Builder exclusiveRemoveBeforeTimePeriodUnit(final TimeUnit exclusiveRemoveBeforeTimePeriodUnit) {
            this.exclusiveRemoveBeforeTimePeriodUnit = exclusiveRemoveBeforeTimePeriodUnit;
            return this;
        }

        public Builder schedulableConfiguration(final SchedulableConfiguration schedulableConfiguration) {
            this.schedulableConfiguration = schedulableConfiguration;
            return this;
        }

        public DefaultExpiredMessageCleanerConfiguration build() {
            return new DefaultExpiredMessageCleanerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultExpiredMessageCleanerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultExpiredMessageCleanerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.exception.store.DefaultExpiredMessageCleanerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
