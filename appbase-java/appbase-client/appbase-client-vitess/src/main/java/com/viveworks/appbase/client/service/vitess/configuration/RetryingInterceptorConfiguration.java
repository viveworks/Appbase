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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vitess.client.grpc.RetryingInterceptorConfig;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link RetryingInterceptorConfig}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RetryingInterceptorConfiguration.Builder.class)
public final class RetryingInterceptorConfiguration {
    private final boolean enabled;
    private final TimeUnitConfiguration initialBackoffConfiguration;
    private final TimeUnitConfiguration maxBackoffConfiguration;
    private final Double backoffMultiplier;

    private RetryingInterceptorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.enabled) {
            AppbaseUtil.notNull(builder.initialBackoffConfiguration, "builder.initialBackoffConfiguration");
            AppbaseUtil.notNull(builder.maxBackoffConfiguration, "builder.maxBackoffConfiguration");
            AppbaseUtil.positive(builder.backoffMultiplier, "builder.backoffMultiplier");
        }

        enabled = builder.enabled;
        initialBackoffConfiguration = builder.initialBackoffConfiguration;
        maxBackoffConfiguration = builder.maxBackoffConfiguration;
        backoffMultiplier = builder.backoffMultiplier;
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

    public TimeUnitConfiguration getInitialBackoffConfiguration() {
        return initialBackoffConfiguration;
    }

    public TimeUnitConfiguration getMaxBackoffConfiguration() {
        return maxBackoffConfiguration;
    }

    public Double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RetryingInterceptorConfiguration that = (RetryingInterceptorConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(that.backoffMultiplier, backoffMultiplier) &&
                Objects.equals(initialBackoffConfiguration, that.initialBackoffConfiguration) &&
                Objects.equals(maxBackoffConfiguration, that.maxBackoffConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, initialBackoffConfiguration, maxBackoffConfiguration, backoffMultiplier);
    }

    @Override
    public String toString() {
        return "RetryingInterceptorConfiguration{" +
                "enabled=" + enabled +
                ", initialBackoffConfiguration=" + initialBackoffConfiguration +
                ", maxBackoffConfiguration=" + maxBackoffConfiguration +
                ", backoffMultiplier=" + backoffMultiplier +
                '}';
    }


    /**
     * Builder for {@link RetryingInterceptorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final TimeUnitConfiguration DEFAULT_INITIAL_BACKOFF_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .time(10)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        private static final TimeUnitConfiguration DEFAULT_MAX_BACKOFF_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .time(2)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final double DEFAULT_BACKOFF_MULTIPLIER = 1.6;


        private boolean enabled = DEFAULT_ENABLED;
        private TimeUnitConfiguration initialBackoffConfiguration = DEFAULT_INITIAL_BACKOFF_CONFIGURATION;
        private TimeUnitConfiguration maxBackoffConfiguration = DEFAULT_MAX_BACKOFF_CONFIGURATION;
        private Double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;


        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder initialBackoffConfiguration(final TimeUnitConfiguration initialBackoffConfiguration) {
            this.initialBackoffConfiguration = initialBackoffConfiguration;
            return this;
        }

        public Builder maxBackoffConfiguration(final TimeUnitConfiguration maxBackoffConfiguration) {
            this.maxBackoffConfiguration = maxBackoffConfiguration;
            return this;
        }

        public Builder backoffMultiplier(final Double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public RetryingInterceptorConfiguration build() {
            return new RetryingInterceptorConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
