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
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
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
 * Configuration for {@link PhasedBackoffWaitStrategy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@PhasedBackoffWaitStrategyConfiguration.Valid
@JsonDeserialize(builder = PhasedBackoffWaitStrategyConfiguration.Builder.class)
public final class PhasedBackoffWaitStrategyConfiguration implements WaitStrategyConfiguration {
    private final boolean enabled;
    private final long spinTimeout;
    private final long yieldTimeout;
    private final TimeUnit units;
    private final WaitStrategyConfiguration waitStrategyConfiguration;

    private PhasedBackoffWaitStrategyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        PhasedBackoffWaitStrategyConfigurationValidator.validate(
                builder.enabled,
                builder.spinTimeout,
                builder.yieldTimeout,
                builder.units,
                builder.waitStrategyConfiguration);

        enabled = builder.enabled;
        spinTimeout = builder.spinTimeout;
        yieldTimeout = builder.yieldTimeout;
        units = builder.units;
        waitStrategyConfiguration = builder.waitStrategyConfiguration;
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

    public long getSpinTimeout() {
        return spinTimeout;
    }

    public long getYieldTimeout() {
        return yieldTimeout;
    }

    public TimeUnit getUnits() {
        return units;
    }

    public WaitStrategyConfiguration getWaitStrategyConfiguration() {
        return waitStrategyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PhasedBackoffWaitStrategyConfiguration that = (PhasedBackoffWaitStrategyConfiguration) o;
        return enabled == that.enabled &&
                spinTimeout == that.spinTimeout &&
                yieldTimeout == that.yieldTimeout &&
                units == that.units &&
                Objects.equals(waitStrategyConfiguration, that.waitStrategyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, spinTimeout, yieldTimeout, units, waitStrategyConfiguration);
    }

    @Override
    public String toString() {
        return "PhasedBackoffWaitStrategyConfiguration{" +
                "enabled=" + enabled +
                ", spinTimeout=" + spinTimeout +
                ", yieldTimeout=" + yieldTimeout +
                ", units=" + units +
                ", waitStrategyConfiguration=" + waitStrategyConfiguration +
                '}';
    }

    /**
     * Builder for {@link PhasedBackoffWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_SPIN_TIMEOUT = 100L;
        private static final long DEFAULT_YIELD_TIMEOUT = 100L;
        private static final TimeUnit DEFAULT_UNITS = TimeUnit.MILLISECONDS;
        private static final WaitStrategyConfiguration DEFAULT_WAIT_STRATEGY_CONFIGURATION =
                BlockingWaitStrategyConfiguration.builder().build();

        private boolean enabled = DEFAULT_ENABLED;
        private long spinTimeout = DEFAULT_SPIN_TIMEOUT;
        private long yieldTimeout = DEFAULT_YIELD_TIMEOUT;
        private TimeUnit units = DEFAULT_UNITS;
        private WaitStrategyConfiguration waitStrategyConfiguration = DEFAULT_WAIT_STRATEGY_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder spinTimeout(final long spinTimeout) {
            this.spinTimeout = spinTimeout;
            return this;
        }

        public Builder yieldTimeout(final long yieldTimeout) {
            this.yieldTimeout = yieldTimeout;
            return this;
        }

        public Builder units(final TimeUnit units) {
            this.units = units;
            return this;
        }

        public Builder waitStrategyConfiguration(final WaitStrategyConfiguration waitStrategyConfiguration) {
            this.waitStrategyConfiguration = waitStrategyConfiguration;
            return this;
        }

        public PhasedBackoffWaitStrategyConfiguration build() {
            return new PhasedBackoffWaitStrategyConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link PhasedBackoffWaitStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = PhasedBackoffWaitStrategyConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy." +
                "PhasedBackoffWaitStrategyConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
