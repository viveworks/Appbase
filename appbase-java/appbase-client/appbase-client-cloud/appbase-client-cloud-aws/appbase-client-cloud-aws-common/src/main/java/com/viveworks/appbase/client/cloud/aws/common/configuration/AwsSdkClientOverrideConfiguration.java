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

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkAdvancedClientOptionsFactory;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkExecutionInterceptorsFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.StringList;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration used to generate {@link ClientOverrideConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkClientOverrideConfiguration.Builder.class)
public final class AwsSdkClientOverrideConfiguration {
    private final InjectedTypeConfiguration<? extends AwsSdkAdvancedClientOptionsFactory>
            awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration;

    private final InjectedTypeConfiguration<AwsSdkExecutionInterceptorsFactory>
            awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration;

    @JsonSerialize(keyAs = String.class, contentAs = StringList.class)
    @JsonDeserialize(keyAs = String.class, contentAs = StringList.class)
    private final Map<String, StringList> headers;

    private final AwsSdkRetryPolicyConfiguration awsSdkRetryPolicyConfiguration;

    private final TimeoutConfiguration apiCallAttemptTimeoutConfiguration;

    private final TimeoutConfiguration apiCallTimeoutConfiguration;

    private final AwsSdkProfileFileConfiguration defaultAwsSdkProfileFileConfiguration;

    private final String defaultProfileName;



    private AwsSdkClientOverrideConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration =
                builder.awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration;
        awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration =
                builder.awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration;
        headers = builder.headers;
        awsSdkRetryPolicyConfiguration = builder.awsSdkRetryPolicyConfiguration;
        apiCallAttemptTimeoutConfiguration = builder.apiCallAttemptTimeoutConfiguration;
        apiCallTimeoutConfiguration = builder.apiCallTimeoutConfiguration;
        defaultAwsSdkProfileFileConfiguration = builder.defaultAwsSdkProfileFileConfiguration;
        defaultProfileName = builder.defaultProfileName;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<? extends AwsSdkAdvancedClientOptionsFactory>
    getAwsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration()
    {
        return awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<AwsSdkExecutionInterceptorsFactory>
    getAwsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration()
    {
        return awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration;
    }

    /**
     * Returns the configured headers.
     *
     * @return the configured headers
     */
    public Map<String, StringList> getHeaders() {
        return headers;
    }

    /**
     * Returns the configured aws sdk retry policy configuration.
     *
     * @return the configured aws sdk retry policy configuration
     */
    public AwsSdkRetryPolicyConfiguration getAwsSdkRetryPolicyConfiguration() {
        return awsSdkRetryPolicyConfiguration;
    }

    /**
     * Returns the configured api call attempt timeout configuration.
     *
     * @return the configured api call attempt timeout configuration
     */
    public TimeoutConfiguration getApiCallAttemptTimeoutConfiguration() {
        return apiCallAttemptTimeoutConfiguration;
    }

    /**
     * Returns the configured api call timeout configuration.
     *
     * @return the configured api call timeout configuration
     */
    public TimeoutConfiguration getApiCallTimeoutConfiguration() {
        return apiCallTimeoutConfiguration;
    }

    /**
     * Returns the configured default aws sdk profile file configuration.
     *
     * @return the configured default aws sdk profile file configuration
     */
    public AwsSdkProfileFileConfiguration getDefaultAwsSdkProfileFileConfiguration() {
        return defaultAwsSdkProfileFileConfiguration;
    }

    /**
     * Returns the configured default profile name.
     *
     * @return the configured default profile name
     */
    public String getDefaultProfileName() {
        return defaultProfileName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkClientOverrideConfiguration that = (AwsSdkClientOverrideConfiguration) o;
        return Objects.equals(
                awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration,
                that.awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration,
                        that.awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(awsSdkRetryPolicyConfiguration, that.awsSdkRetryPolicyConfiguration) &&
                Objects.equals(apiCallAttemptTimeoutConfiguration, that.apiCallAttemptTimeoutConfiguration) &&
                Objects.equals(apiCallTimeoutConfiguration, that.apiCallTimeoutConfiguration) &&
                Objects.equals(defaultAwsSdkProfileFileConfiguration, that.defaultAwsSdkProfileFileConfiguration) &&
                Objects.equals(defaultProfileName, that.defaultProfileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration,
                awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration,
                headers,
                awsSdkRetryPolicyConfiguration,
                apiCallAttemptTimeoutConfiguration,
                apiCallTimeoutConfiguration,
                defaultAwsSdkProfileFileConfiguration,
                defaultProfileName);
    }

    @Override
    public String toString() {
        return "AwsSdkClientOverrideConfiguration{" +
                "awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration=" +
                awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration +
                ", awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration=" +
                awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration +
                ", headers=" + AppbaseUtil.OBFUSCATE +
                ", awsSdkRetryPolicyConfiguration=" + awsSdkRetryPolicyConfiguration +
                ", apiCallAttemptTimeoutConfiguration=" + apiCallAttemptTimeoutConfiguration +
                ", apiCallTimeoutConfiguration=" + apiCallTimeoutConfiguration +
                ", awsSdkProfileFileConfiguration=" + defaultAwsSdkProfileFileConfiguration +
                ", defaultProfileName='" + defaultProfileName + '\'' +
                '}';
    }


    /**
     * Builder for {@link AwsSdkClientOverrideConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final AwsSdkRetryPolicyConfiguration DEFAULT_AWS_SDK_RETRY_POLICY_CONFIGURATION =
                AwsSdkRetryPolicyConfiguration.builder().build();
        private static final TimeoutConfiguration DEFAULT_API_CALL_ATTEMPT_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(50)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_API_CALL_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(100)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final AwsSdkProfileFileConfiguration DEFAULT_DEFAULT_AWS_SDK_PROFILE_FILE_CONFIGURATION =
                AwsSdkProfileFileConfiguration.builder().build();



        private InjectedTypeConfiguration<? extends AwsSdkAdvancedClientOptionsFactory>
                awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<AwsSdkExecutionInterceptorsFactory>
                awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration;

        @JsonSerialize(keyAs = String.class, contentAs = StringList.class)
        @JsonDeserialize(keyAs = String.class, contentAs = StringList.class)
        private Map<String, StringList> headers;

        private AwsSdkRetryPolicyConfiguration awsSdkRetryPolicyConfiguration = DEFAULT_AWS_SDK_RETRY_POLICY_CONFIGURATION;

        private TimeoutConfiguration apiCallAttemptTimeoutConfiguration = DEFAULT_API_CALL_ATTEMPT_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration apiCallTimeoutConfiguration = DEFAULT_API_CALL_TIMEOUT_CONFIGURATION;

        private AwsSdkProfileFileConfiguration defaultAwsSdkProfileFileConfiguration =
                DEFAULT_DEFAULT_AWS_SDK_PROFILE_FILE_CONFIGURATION;

        private String defaultProfileName;



        private Builder() {
        }

        public Builder awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends AwsSdkAdvancedClientOptionsFactory>
                        awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration)
        {
            this.awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration =
                    awsSdkAdvancedClientOptionsFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<AwsSdkExecutionInterceptorsFactory>
                        awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration)
        {
            this.awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration =
                    awsSdkExecutionInterceptorsFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder headers(
                @JsonSerialize(keyAs = String.class, contentAs = StringList.class)
                @JsonDeserialize(keyAs = String.class, contentAs = StringList.class)
                final Map<String, StringList> headers)
        {
            this.headers = headers;
            return this;
        }

        public Builder awsSdkRetryPolicyConfiguration(final AwsSdkRetryPolicyConfiguration awsSdkRetryPolicyConfiguration) {
            this.awsSdkRetryPolicyConfiguration = awsSdkRetryPolicyConfiguration;
            return this;
        }

        public Builder apiCallAttemptTimeoutConfiguration(final TimeoutConfiguration apiCallAttemptTimeoutConfiguration) {
            this.apiCallAttemptTimeoutConfiguration = apiCallAttemptTimeoutConfiguration;
            return this;
        }

        public Builder apiCallTimeoutConfiguration(final TimeoutConfiguration apiCallTimeoutConfiguration) {
            this.apiCallTimeoutConfiguration = apiCallTimeoutConfiguration;
            return this;
        }

        public Builder defaultAwsSdkProfileFileConfiguration(
                final AwsSdkProfileFileConfiguration defaultAwsSdkProfileFileConfiguration)
        {
            this.defaultAwsSdkProfileFileConfiguration = defaultAwsSdkProfileFileConfiguration;
            return this;
        }

        public Builder defaultProfileName(final String defaultProfileName) {
            this.defaultProfileName = defaultProfileName;
            return this;
        }

        public AwsSdkClientOverrideConfiguration build() {
            return new AwsSdkClientOverrideConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
