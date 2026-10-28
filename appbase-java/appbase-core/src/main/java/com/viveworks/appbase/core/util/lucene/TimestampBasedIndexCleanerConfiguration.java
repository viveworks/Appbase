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
 * Configuration for {@link TimestampBasedIndexCleaner}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@TimestampBasedIndexCleanerConfiguration.Valid
@JsonDeserialize(builder = TimestampBasedIndexCleanerConfiguration.Builder.class)
public final class TimestampBasedIndexCleanerConfiguration {
    private final boolean enabled;
    private final long removeBeforeTimePeriod;
    private final TimeUnit removeBeforeTimePeriodUnit;
    private final String creationTimestampFieldName;
    private final SchedulableConfiguration schedulableConfiguration;

    private TimestampBasedIndexCleanerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        TimestampBasedIndexCleanerConfigurationValidator.validate(
                builder.enabled,
                builder.removeBeforeTimePeriod,
                builder.removeBeforeTimePeriodUnit,
                builder.creationTimestampFieldName,
                builder.schedulableConfiguration);

        enabled = builder.enabled;
        removeBeforeTimePeriod = builder.removeBeforeTimePeriod;
        removeBeforeTimePeriodUnit = builder.removeBeforeTimePeriodUnit;
        creationTimestampFieldName = builder.creationTimestampFieldName;
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

    public long getRemoveBeforeTimePeriod() {
        return removeBeforeTimePeriod;
    }

    public TimeUnit getRemoveBeforeTimePeriodUnit() {
        return removeBeforeTimePeriodUnit;
    }

    public String getCreationTimestampFieldName() {
        return creationTimestampFieldName;
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
        final TimestampBasedIndexCleanerConfiguration that = (TimestampBasedIndexCleanerConfiguration) o;
        return enabled == that.enabled &&
                removeBeforeTimePeriod == that.removeBeforeTimePeriod &&
                removeBeforeTimePeriodUnit == that.removeBeforeTimePeriodUnit &&
                Objects.equals(creationTimestampFieldName, that.creationTimestampFieldName) &&
                Objects.equals(schedulableConfiguration, that.schedulableConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                removeBeforeTimePeriod,
                removeBeforeTimePeriodUnit,
                creationTimestampFieldName,
                schedulableConfiguration);
    }

    @Override
    public String toString() {
        return "TimestampBasedIndexCleanerConfiguration{" +
                "enabled=" + enabled +
                ", removeBeforeTimePeriod=" + removeBeforeTimePeriod +
                ", removeBeforeTimePeriodUnit=" + removeBeforeTimePeriodUnit +
                ", creationTimestampFieldName=" + creationTimestampFieldName +
                ", schedulableConfiguration=" + schedulableConfiguration +
                '}';
    }

    /**
     * Builder for {@link TimestampBasedIndexCleanerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_REMOVE_BEFORE_TIME_PERIOD = 1L;
        private static final TimeUnit DEFAULT_REMOVE_BEFORE_TIME_PERIOD_UNIT = TimeUnit.DAYS;
        private static final String DEFAULT_CREATION_TIMESTAMP_FIELD_NAME = "creationTimestampLongPoint";
        private static final SchedulableConfiguration DEFAULT_SCHEDULABLE_CONFIGURATION = SchedulableConfiguration
                .builder()
                .initialDelay(0L)
                .delay(10L)
                .timeUnit(TimeUnit.MINUTES)
                .build();

        private boolean enabled = DEFAULT_ENABLED;
        private long removeBeforeTimePeriod = DEFAULT_REMOVE_BEFORE_TIME_PERIOD;
        private TimeUnit removeBeforeTimePeriodUnit = DEFAULT_REMOVE_BEFORE_TIME_PERIOD_UNIT;
        private String creationTimestampFieldName = DEFAULT_CREATION_TIMESTAMP_FIELD_NAME;
        private SchedulableConfiguration schedulableConfiguration = DEFAULT_SCHEDULABLE_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder removeBeforeTimePeriod(final long removeBeforeTimePeriod) {
            this.removeBeforeTimePeriod = removeBeforeTimePeriod;
            return this;
        }

        public Builder removeBeforeTimePeriodUnit(final TimeUnit removeBeforeTimePeriodUnit) {
            this.removeBeforeTimePeriodUnit = removeBeforeTimePeriodUnit;
            return this;
        }

        public Builder creationTimestampFieldName(final String creationTimestampFieldName) {
            this.creationTimestampFieldName = creationTimestampFieldName;
            return this;
        }

        public Builder schedulableConfiguration(final SchedulableConfiguration schedulableConfiguration) {
            this.schedulableConfiguration = schedulableConfiguration;
            return this;
        }

        public TimestampBasedIndexCleanerConfiguration build() {
            return new TimestampBasedIndexCleanerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link TimestampBasedIndexCleanerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = TimestampBasedIndexCleanerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.lucene." +
                "TimestampBasedIndexCleanerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
