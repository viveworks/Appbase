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
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.profiles.ProfileFile;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link ProfileFile}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkProfileFileConfiguration.Builder.class)
public final class AwsSdkProfileFileConfiguration {
    private final boolean useDefaultProfile;
    private final AwsSdkConfigurationFileConfiguration awsSdkConfigurationFileConfiguration;
    private final AwsSdkCredentialsFileConfiguration awsSdkCredentialsFileConfiguration;

    private AwsSdkProfileFileConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        useDefaultProfile = builder.useDefaultProfile;
        awsSdkConfigurationFileConfiguration = builder.awsSdkConfigurationFileConfiguration;
        awsSdkCredentialsFileConfiguration = builder.awsSdkCredentialsFileConfiguration;
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
     * Returns whether use default profile.
     *
     * @return whether use default profile
     */
    public boolean isUseDefaultProfile() {
        return useDefaultProfile;
    }

    /**
     * Returns the configured aws sdk configuration file configuration.
     *
     * @return the configured aws sdk configuration file configuration
     */
    public AwsSdkConfigurationFileConfiguration getAwsSdkConfigurationFileConfiguration() {
        return awsSdkConfigurationFileConfiguration;
    }

    /**
     * Returns the configured aws sdk credentials file configuration.
     *
     * @return the configured aws sdk credentials file configuration
     */
    public AwsSdkCredentialsFileConfiguration getAwsSdkCredentialsFileConfiguration() {
        return awsSdkCredentialsFileConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkProfileFileConfiguration that = (AwsSdkProfileFileConfiguration) o;
        return useDefaultProfile == that.useDefaultProfile &&
                Objects.equals(awsSdkConfigurationFileConfiguration, that.awsSdkConfigurationFileConfiguration) &&
                Objects.equals(awsSdkCredentialsFileConfiguration, that.awsSdkCredentialsFileConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useDefaultProfile, awsSdkConfigurationFileConfiguration, awsSdkCredentialsFileConfiguration);
    }

    @Override
    public String toString() {
        return "AwsSdkProfileFileConfiguration{" +
                "useDefaultProfile=" + useDefaultProfile +
                ", awsSdkConfigurationFileConfiguration=" + awsSdkConfigurationFileConfiguration +
                ", awsSdkCredentialsFileConfiguration=" + awsSdkCredentialsFileConfiguration +
                '}';
    }


    /**
     * Builder for {@link AwsSdkProfileFileConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_USE_DEFAULT_PROFILE = false;
        private static final AwsSdkConfigurationFileConfiguration DEFAULT_AWS_SDK_CONFIGURATION_FILE_CONFIGURATION =
                AwsSdkConfigurationFileConfiguration.builder().build();
        private static final AwsSdkCredentialsFileConfiguration DEFAULT_AWS_SDK_CREDENTIALS_FILE_CONFIGURATION =
                AwsSdkCredentialsFileConfiguration.builder().build();


        private boolean useDefaultProfile = DEFAULT_USE_DEFAULT_PROFILE;
        private AwsSdkConfigurationFileConfiguration awsSdkConfigurationFileConfiguration =
                DEFAULT_AWS_SDK_CONFIGURATION_FILE_CONFIGURATION;
        private AwsSdkCredentialsFileConfiguration awsSdkCredentialsFileConfiguration =
                DEFAULT_AWS_SDK_CREDENTIALS_FILE_CONFIGURATION;


        private Builder() {
        }

        public Builder useDefaultProfile(final boolean useDefaultProfile) {
            this.useDefaultProfile = useDefaultProfile;
            return this;
        }

        public Builder awsSdkConfigurationFileConfiguration(
                final AwsSdkConfigurationFileConfiguration awsSdkConfigurationFileConfiguration)
        {
            this.awsSdkConfigurationFileConfiguration = awsSdkConfigurationFileConfiguration;
            return this;
        }

        public Builder awsSdkCredentialsFileConfiguration(
                final AwsSdkCredentialsFileConfiguration awsSdkCredentialsFileConfiguration)
        {
            this.awsSdkCredentialsFileConfiguration = awsSdkCredentialsFileConfiguration;
            return this;
        }

        public AwsSdkProfileFileConfiguration build() {
            return new AwsSdkProfileFileConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
