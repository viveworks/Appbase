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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultDuplicateLogHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultLogDeduplicatorConfiguration;
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
 * Configuration for {@link BurstTurboLogEventFilterHandler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@BurstTurboLogEventFilterHandlerConfiguration.Valid
@JsonDeserialize(builder = BurstTurboLogEventFilterHandlerConfiguration.Builder.class)
public final class BurstTurboLogEventFilterHandlerConfiguration {
    private final boolean enabled;
    private final double burstRateThreshold;
    private final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration;
    private final DefaultLogRateCalculatorConfiguration defaultLogRateCalculatorConfiguration;
    private final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration;

    private BurstTurboLogEventFilterHandlerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        BurstTurboLogEventFilterHandlerConfigurationValidator.validate(
                builder.enabled,
                builder.burstRateThreshold,
                builder.defaultLogDeduplicatorConfiguration,
                builder.defaultLogRateCalculatorConfiguration,
                builder.defaultDuplicateLogHandlerConfiguration);

        enabled = builder.enabled;
        burstRateThreshold = builder.burstRateThreshold;
        defaultLogDeduplicatorConfiguration = builder.defaultLogDeduplicatorConfiguration;
        defaultLogRateCalculatorConfiguration = builder.defaultLogRateCalculatorConfiguration;
        defaultDuplicateLogHandlerConfiguration = builder.defaultDuplicateLogHandlerConfiguration;
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

    public double getBurstRateThreshold() {
        return burstRateThreshold;
    }

    public DefaultLogDeduplicatorConfiguration getDefaultLogDeduplicatorConfiguration() {
        return defaultLogDeduplicatorConfiguration;
    }

    public DefaultLogRateCalculatorConfiguration getDefaultLogRateCalculatorConfiguration() {
        return defaultLogRateCalculatorConfiguration;
    }

    public DefaultDuplicateLogHandlerConfiguration getDefaultDuplicateLogHandlerConfiguration() {
        return defaultDuplicateLogHandlerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BurstTurboLogEventFilterHandlerConfiguration that = (BurstTurboLogEventFilterHandlerConfiguration) o;
        return enabled == that.enabled &&
                Double.compare(that.burstRateThreshold, burstRateThreshold) == 0 &&
                Objects.equals(defaultLogDeduplicatorConfiguration, that.defaultLogDeduplicatorConfiguration) &&
                Objects.equals(defaultLogRateCalculatorConfiguration, that.defaultLogRateCalculatorConfiguration) &&
                Objects.equals(defaultDuplicateLogHandlerConfiguration, that.defaultDuplicateLogHandlerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                burstRateThreshold,
                defaultLogDeduplicatorConfiguration,
                defaultLogRateCalculatorConfiguration,
                defaultDuplicateLogHandlerConfiguration);
    }

    @Override
    public String toString() {
        return "BurstTurboLogEventFilterHandlerConfiguration{" +
                "enabled=" + enabled +
                ", burstRateThreshold=" + burstRateThreshold +
                ", defaultLogDeduplicatorConfiguration=" + defaultLogDeduplicatorConfiguration +
                ", defaultLogRateCalculatorConfiguration=" + defaultLogRateCalculatorConfiguration +
                ", defaultDuplicateLogHandlerConfiguration=" + defaultDuplicateLogHandlerConfiguration +
                '}';
    }

    /**
     * {@link BurstTurboLogEventFilterHandlerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final double DEFAULT_BURST_RATE_THRESHOLD = 1000.0;
        private static final DefaultLogDeduplicatorConfiguration DEFAULT_DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION =
                DefaultLogDeduplicatorConfiguration.builder().build();
        private static final DefaultLogRateCalculatorConfiguration DEFAULT_DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION =
                DefaultLogRateCalculatorConfiguration.builder().build();
        private static final DefaultDuplicateLogHandlerConfiguration
                DEFAULT_DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION =
                DefaultDuplicateLogHandlerConfiguration.builder().build();


        private boolean enabled = DEFAULT_ENABLED;
        private double burstRateThreshold = DEFAULT_BURST_RATE_THRESHOLD;
        private DefaultLogDeduplicatorConfiguration
                defaultLogDeduplicatorConfiguration = DEFAULT_DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION;
        private DefaultLogRateCalculatorConfiguration
                defaultLogRateCalculatorConfiguration = DEFAULT_DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION;
        private DefaultDuplicateLogHandlerConfiguration
                defaultDuplicateLogHandlerConfiguration = DEFAULT_DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder burstRateThreshold(final double burstRateThreshold) {
            this.burstRateThreshold = burstRateThreshold;
            return this;
        }

        public Builder defaultLogDeduplicatorConfiguration(
                final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration)
        {
            this.defaultLogDeduplicatorConfiguration = defaultLogDeduplicatorConfiguration;
            return this;
        }

        public Builder defaultLogRateCalculatorConfiguration(
                final DefaultLogRateCalculatorConfiguration defaultLogRateCalculatorConfiguration)
        {
            this.defaultLogRateCalculatorConfiguration = defaultLogRateCalculatorConfiguration;
            return this;
        }

        public Builder defaultDuplicateLogHandlerConfiguration(
                final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration)
        {
            this.defaultDuplicateLogHandlerConfiguration = defaultDuplicateLogHandlerConfiguration;
            return this;
        }

        public BurstTurboLogEventFilterHandlerConfiguration build() {
            return new BurstTurboLogEventFilterHandlerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link BurstTurboLogEventFilterHandlerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = BurstTurboLogEventFilterHandlerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.filter.burst." +
                    "BurstTurboLogEventFilterHandlerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
