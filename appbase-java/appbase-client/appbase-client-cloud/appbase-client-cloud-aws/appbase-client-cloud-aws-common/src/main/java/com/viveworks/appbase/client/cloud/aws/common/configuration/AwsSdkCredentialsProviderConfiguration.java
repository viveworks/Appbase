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

/**
 * This package contains client services for {@link software.amazon.awssdk.http.async.SdkAsyncHttpClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.cloud.aws.common.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link AwsCredentialsProvider}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkCredentialsProviderConfiguration.Builder.class)
public final class AwsSdkCredentialsProviderConfiguration {
    private final boolean asyncCredentialUpdateEnabled;
    private final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration;
    private final String profileName;
    private final boolean reuseLastProviderEnabled;


    private AwsSdkCredentialsProviderConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        awsSdkProfileFileConfiguration = builder.awsSdkProfileFileConfiguration;
        profileName = builder.profileName;
        reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
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
     * Returns whether async credential update enabled.
     *
     * @return whether async credential update enabled
     */
    public boolean isAsyncCredentialUpdateEnabled() {
        return asyncCredentialUpdateEnabled;
    }

    /**
     * Returns the configured aws sdk profile file configuration.
     *
     * @return the configured aws sdk profile file configuration
     */
    public AwsSdkProfileFileConfiguration getAwsSdkProfileFileConfiguration() {
        return awsSdkProfileFileConfiguration;
    }

    /**
     * Returns the configured profile name.
     *
     * @return the configured profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Returns whether reuse last provider enabled.
     *
     * @return whether reuse last provider enabled
     */
    public boolean isReuseLastProviderEnabled() {
        return reuseLastProviderEnabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkCredentialsProviderConfiguration that = (AwsSdkCredentialsProviderConfiguration) o;
        return asyncCredentialUpdateEnabled == that.asyncCredentialUpdateEnabled &&
                reuseLastProviderEnabled == that.reuseLastProviderEnabled &&
                Objects.equals(awsSdkProfileFileConfiguration, that.awsSdkProfileFileConfiguration) &&
                Objects.equals(profileName, that.profileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                asyncCredentialUpdateEnabled,
                awsSdkProfileFileConfiguration,
                profileName,
                reuseLastProviderEnabled);
    }

    @Override
    public String toString() {
        return "AwsSdkCredentialsProviderConfiguration{" +
                "asyncCredentialUpdateEnabled=" + asyncCredentialUpdateEnabled +
                ", awsSdkProfileFileConfiguration=" + awsSdkProfileFileConfiguration +
                ", profileName='" + profileName + '\'' +
                ", reuseLastProviderEnabled=" + reuseLastProviderEnabled +
                '}';
    }


    /**
     * Builder for {@link AwsSdkCredentialsProviderConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ASYNC_CREDENTIAL_UPDATE_ENABLED = false;
        private static final AwsSdkProfileFileConfiguration DEFAULT_AWS_SDK_PROFILE_FILE_CONFIGURATION =
                AwsSdkProfileFileConfiguration.builder().build();
        private static final String DEFAULT_PROFILE_NAME = "default";
        private static final boolean DEFAULT_REUSE_LAST_PROVIDER_ENABLED = true;


        private boolean asyncCredentialUpdateEnabled = DEFAULT_ASYNC_CREDENTIAL_UPDATE_ENABLED;
        private AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration = DEFAULT_AWS_SDK_PROFILE_FILE_CONFIGURATION;
        private String profileName = DEFAULT_PROFILE_NAME;
        private boolean reuseLastProviderEnabled = DEFAULT_REUSE_LAST_PROVIDER_ENABLED;


        private Builder() {
        }

        public Builder asyncCredentialUpdateEnabled(final boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        public Builder awsSdkProfileFileConfiguration(final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration) {
            this.awsSdkProfileFileConfiguration = awsSdkProfileFileConfiguration;
            return this;
        }

        public Builder profileName(final String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Builder reuseLastProviderEnabled(final boolean reuseLastProviderEnabled) {
            this.reuseLastProviderEnabled = reuseLastProviderEnabled;
            return this;
        }

        public AwsSdkCredentialsProviderConfiguration build() {
            return new AwsSdkCredentialsProviderConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
