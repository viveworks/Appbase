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

package com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.lmax.disruptor.SleepingWaitStrategy;
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

/**
 * Configuration for {@link SleepingWaitStrategy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@SleepingWaitStrategyConfiguration.Valid
@JsonDeserialize(builder = SleepingWaitStrategyConfiguration.Builder.class)
public final class SleepingWaitStrategyConfiguration implements WaitStrategyConfiguration {
    private final boolean enabled;
    private final int retries;
    private final long sleepTimeNanoSeconds;

    private SleepingWaitStrategyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        SleepingWaitStrategyConfigurationValidator.validate(
                builder.enabled,
                builder.retries,
                builder.sleepTimeNanoSeconds);

        enabled = builder.enabled;
        retries = builder.retries;
        sleepTimeNanoSeconds = builder.sleepTimeNanoSeconds;
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

    public int getRetries() {
        return retries;
    }

    public long getSleepTimeNanoSeconds() {
        return sleepTimeNanoSeconds;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SleepingWaitStrategyConfiguration)) {
            return false;
        }

        final SleepingWaitStrategyConfiguration that = (SleepingWaitStrategyConfiguration) o;
        return enabled == that.enabled &&
                retries == that.retries &&
                sleepTimeNanoSeconds == that.sleepTimeNanoSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, retries, sleepTimeNanoSeconds);
    }

    @Override
    public String toString() {
        return "SleepingWaitStrategyConfiguration{" +
                "enabled=" + enabled +
                ", retries=" + retries +
                ", sleepTimeNanoSeconds=" + sleepTimeNanoSeconds +
                '}';
    }

    /**
     * Builder for {@link SleepingWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final int DEFAULT_RETRIES = 100;
        private static final long DEFAULT_SLEEP_TIME_NANO_SECONDS = 100_1000L;

        private boolean enabled = DEFAULT_ENABLED;
        private int retries = DEFAULT_RETRIES;
        private long sleepTimeNanoSeconds = DEFAULT_SLEEP_TIME_NANO_SECONDS;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder retries(final int retries) {
            this.retries = retries;
            return this;
        }

        public Builder sleepTimeNanoSeconds(final long sleepTimeNanoSeconds) {
            this.sleepTimeNanoSeconds = sleepTimeNanoSeconds;
            return this;
        }

        public SleepingWaitStrategyConfiguration build() {
            return new SleepingWaitStrategyConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link SleepingWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = SleepingWaitStrategyConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy." +
                "SleepingWaitStrategyConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
