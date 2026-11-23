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

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.AwsSdkBackoffStrategyConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.backoff.FullJitterBackoffStrategyConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link software.amazon.awssdk.core.retry.RetryPolicy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkRetryPolicyConfiguration.Builder.class)
public final class AwsSdkRetryPolicyConfiguration {
    private final boolean useDefaultRetryPolicy;
    private final AwsSdkRetryMode awsSdkRetryMode;
    private final boolean useBuilderWithRetryMode;
    private final Boolean additionalRetryConditionsAllowed;
    private final AwsSdkBackoffStrategyConfiguration awsSdkBackoffStrategyConfiguration;
    private final Integer retryCount;
    private final InjectedTypeConfiguration<? extends RetryCondition> retryConditionInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends RetryCondition> retryCapacityConditionInjectedTypeConfiguration;
    private final AwsSdkBackoffStrategyConfiguration throttlingBackoffStrategyConfiguration;


    private AwsSdkRetryPolicyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.useBuilderWithRetryMode) {
            AppbaseUtil.notNull(builder.awsSdkRetryMode, "builder.awsSdkRetryMode");
        }

        useDefaultRetryPolicy = builder.useDefaultRetryPolicy;
        awsSdkRetryMode = builder.awsSdkRetryMode;
        useBuilderWithRetryMode = builder.useBuilderWithRetryMode;
        additionalRetryConditionsAllowed = builder.additionalRetryConditionsAllowed;
        awsSdkBackoffStrategyConfiguration = builder.awsSdkBackoffStrategyConfiguration;
        retryCount = builder.retryCount;
        retryConditionInjectedTypeConfiguration = builder.retryConditionInjectedTypeConfiguration;
        retryCapacityConditionInjectedTypeConfiguration = builder.retryCapacityConditionInjectedTypeConfiguration;
        throttlingBackoffStrategyConfiguration = builder.throttlingBackoffStrategyConfiguration;
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
     * Returns whether use default retry policy.
     *
     * @return whether use default retry policy
     */
    public boolean isUseDefaultRetryPolicy() {
        return useDefaultRetryPolicy;
    }

    /**
     * Returns the configured aws sdk retry mode.
     *
     * @return the configured aws sdk retry mode
     */
    public AwsSdkRetryMode getAwsSdkRetryMode() {
        return awsSdkRetryMode;
    }

    /**
     * Returns whether use builder with retry mode.
     *
     * @return whether use builder with retry mode
     */
    public boolean isUseBuilderWithRetryMode() {
        return useBuilderWithRetryMode;
    }

    /**
     * Returns the configured additional retry conditions allowed.
     *
     * @return the configured additional retry conditions allowed
     */
    public Boolean getAdditionalRetryConditionsAllowed() {
        return additionalRetryConditionsAllowed;
    }

    /**
     * Returns the configured aws sdk backoff strategy configuration.
     *
     * @return the configured aws sdk backoff strategy configuration
     */
    public AwsSdkBackoffStrategyConfiguration getAwsSdkBackoffStrategyConfiguration() {
        return awsSdkBackoffStrategyConfiguration;
    }

    /**
     * Returns the configured retry count.
     *
     * @return the configured retry count
     */
    public Integer getRetryCount() {
        return retryCount;
    }

    /**
     * Returns the configured retry condition injected type configuration.
     *
     * @return the configured retry condition injected type configuration
     */
    public InjectedTypeConfiguration<? extends RetryCondition> getRetryConditionInjectedTypeConfiguration() {
        return retryConditionInjectedTypeConfiguration;
    }

    /**
     * Returns the configured retry capacity condition injected type configuration.
     *
     * @return the configured retry capacity condition injected type configuration
     */
    public InjectedTypeConfiguration<? extends RetryCondition> getRetryCapacityConditionInjectedTypeConfiguration() {
        return retryCapacityConditionInjectedTypeConfiguration;
    }

    /**
     * Returns the configured throttling backoff strategy configuration.
     *
     * @return the configured throttling backoff strategy configuration
     */
    public AwsSdkBackoffStrategyConfiguration getThrottlingBackoffStrategyConfiguration() {
        return throttlingBackoffStrategyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkRetryPolicyConfiguration that = (AwsSdkRetryPolicyConfiguration) o;
        return useDefaultRetryPolicy == that.useDefaultRetryPolicy &&
                awsSdkRetryMode == that.awsSdkRetryMode &&
                useBuilderWithRetryMode == that.useBuilderWithRetryMode &&
                Objects.equals(additionalRetryConditionsAllowed, that.additionalRetryConditionsAllowed) &&
                Objects.equals(awsSdkBackoffStrategyConfiguration, that.awsSdkBackoffStrategyConfiguration) &&
                Objects.equals(retryCount, that.retryCount) &&
                Objects.equals(retryConditionInjectedTypeConfiguration, that.retryConditionInjectedTypeConfiguration) &&
                Objects.equals(
                        retryCapacityConditionInjectedTypeConfiguration,
                        that.retryCapacityConditionInjectedTypeConfiguration) &&
                Objects.equals(throttlingBackoffStrategyConfiguration, that.throttlingBackoffStrategyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                useDefaultRetryPolicy,
                awsSdkRetryMode,
                useBuilderWithRetryMode,
                additionalRetryConditionsAllowed,
                awsSdkBackoffStrategyConfiguration,
                retryCount,
                retryConditionInjectedTypeConfiguration,
                retryCapacityConditionInjectedTypeConfiguration,
                throttlingBackoffStrategyConfiguration);
    }

    @Override
    public String toString() {
        return "AwsSdkRetryPolicyConfiguration{" +
                "useDefaultRetryPolicy=" + useDefaultRetryPolicy +
                ", awsSdkRetryMode=" + awsSdkRetryMode +
                ", useBuilderWithRetryMode=" + useBuilderWithRetryMode +
                ", additionalRetryConditionsAllowed=" + additionalRetryConditionsAllowed +
                ", awsSdkBackoffStrategyConfiguration=" + awsSdkBackoffStrategyConfiguration +
                ", retryCount=" + retryCount +
                ", retryConditionInjectedTypeConfiguration=" + retryConditionInjectedTypeConfiguration +
                ", retryCapacityConditionInjectedTypeConfiguration=" + retryCapacityConditionInjectedTypeConfiguration +
                ", throttlingBackoffStrategyConfiguration=" + throttlingBackoffStrategyConfiguration +
                '}';
    }


    /**
     * Builder for {@link AwsSdkRetryPolicyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_USE_DEFAULT_RETRY_POLICY = true;
        private static final AwsSdkRetryMode DEFAULT_AWS_SDK_RETRY_MODE = AwsSdkRetryMode.getDefaultRetryMode();
        private static final boolean DEFAULT_USE_BUILDER_WITH_RETRY_MODE = true;
        private static final boolean DEFAULT_ADDITIONAL_RETRY_CONDITIONS_ALLOWED = true;
        private static final AwsSdkBackoffStrategyConfiguration DEFAULT_AWS_SDK_BACKOFF_STRATEGY_CONFIGURATION =
                FullJitterBackoffStrategyConfiguration.builder().build();


        private boolean useDefaultRetryPolicy = DEFAULT_USE_DEFAULT_RETRY_POLICY;
        private AwsSdkRetryMode awsSdkRetryMode = DEFAULT_AWS_SDK_RETRY_MODE;
        private boolean useBuilderWithRetryMode = DEFAULT_USE_BUILDER_WITH_RETRY_MODE;
        private Boolean additionalRetryConditionsAllowed = DEFAULT_ADDITIONAL_RETRY_CONDITIONS_ALLOWED;
        private AwsSdkBackoffStrategyConfiguration awsSdkBackoffStrategyConfiguration =
                DEFAULT_AWS_SDK_BACKOFF_STRATEGY_CONFIGURATION;
        private Integer retryCount;
        private InjectedTypeConfiguration<? extends RetryCondition> retryConditionInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends RetryCondition> retryCapacityConditionInjectedTypeConfiguration;
        private AwsSdkBackoffStrategyConfiguration throttlingBackoffStrategyConfiguration;


        private Builder() {
        }

        public Builder useDefaultRetryPolicy(final boolean useDefaultRetryPolicy) {
            this.useDefaultRetryPolicy = useDefaultRetryPolicy;
            return this;
        }

        public Builder awsSdkRetryMode(final AwsSdkRetryMode awsSdkRetryMode) {
            this.awsSdkRetryMode = awsSdkRetryMode;
            return this;
        }

        public Builder useBuilderWithRetryMode(final boolean useBuilderWithRetryMode) {
            this.useBuilderWithRetryMode = useBuilderWithRetryMode;
            return this;
        }

        public Builder additionalRetryConditionsAllowed(final Boolean additionalRetryConditionsAllowed) {
            this.additionalRetryConditionsAllowed = additionalRetryConditionsAllowed;
            return this;
        }

        public Builder awsSdkBackoffStrategyConfiguration(
                final AwsSdkBackoffStrategyConfiguration awsSdkBackoffStrategyConfiguration)
        {
            this.awsSdkBackoffStrategyConfiguration = awsSdkBackoffStrategyConfiguration;
            return this;
        }

        public Builder retryCount(final Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder retryConditionInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends RetryCondition> retryConditionInjectedTypeConfiguration)
        {
            this.retryConditionInjectedTypeConfiguration = retryConditionInjectedTypeConfiguration;
            return this;
        }

        public Builder retryCapacityConditionInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends RetryCondition> retryCapacityConditionInjectedTypeConfiguration)
        {
            this.retryCapacityConditionInjectedTypeConfiguration = retryCapacityConditionInjectedTypeConfiguration;
            return this;
        }

        public Builder throttlingBackoffStrategyConfiguration(
                final AwsSdkBackoffStrategyConfiguration throttlingBackoffStrategyConfiguration)
        {
            this.throttlingBackoffStrategyConfiguration = throttlingBackoffStrategyConfiguration;
            return this;
        }

        public AwsSdkRetryPolicyConfiguration build() {
            return new AwsSdkRetryPolicyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
