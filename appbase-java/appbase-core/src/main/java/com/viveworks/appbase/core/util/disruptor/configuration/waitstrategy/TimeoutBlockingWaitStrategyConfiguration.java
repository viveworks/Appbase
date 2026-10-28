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
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
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
 * Configuration for {@link TimeoutBlockingWaitStrategy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@TimeoutBlockingWaitStrategyConfiguration.Valid
@JsonDeserialize(builder = TimeoutBlockingWaitStrategyConfiguration.Builder.class)
public final class TimeoutBlockingWaitStrategyConfiguration implements WaitStrategyConfiguration {
    private final boolean enabled;
    private final long timeout;
    private final TimeUnit units;

    private TimeoutBlockingWaitStrategyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        TimeoutBlockingWaitStrategyConfigurationValidator.validate(
                builder.enabled,
                builder.timeout,
                builder.units);

        enabled = builder.enabled;
        timeout = builder.timeout;
        units = builder.units;
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

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getUnits() {
        return units;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TimeoutBlockingWaitStrategyConfiguration that = (TimeoutBlockingWaitStrategyConfiguration) o;
        return enabled == that.enabled &&
                timeout == that.timeout &&
                units == that.units;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, timeout, units);
    }

    @Override
    public String toString() {
        return "TimeoutBlockingWaitStrategyConfiguration{" +
                "enabled=" + enabled +
                ", timeout=" + timeout +
                ", units=" + units +
                '}';
    }

    /**
     * Builder for {@link TimeoutBlockingWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_TIMEOUT = 100L;
        private static final TimeUnit DEFAULT_UNITS = TimeUnit.MILLISECONDS;

        private boolean enabled = DEFAULT_ENABLED;
        private long timeout = DEFAULT_TIMEOUT;
        private TimeUnit units = DEFAULT_UNITS;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder timeout(final long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder units(final TimeUnit units) {
            this.units = units;
            return this;
        }

        public TimeoutBlockingWaitStrategyConfiguration build() {
            return new TimeoutBlockingWaitStrategyConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link TimeoutBlockingWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = TimeoutBlockingWaitStrategyConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy." +
                "TimeoutBlockingWaitStrategyConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
