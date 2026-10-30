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

package com.viveworks.appbase.client.cloud.aws.s3.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkProfileFileConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.services.s3.S3Configuration;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Used to generate {@link S3Configuration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkS3Configuration.Builder.class)
public final class AwsSdkS3Configuration {
    private final Boolean accelerateModeEnabled;
    private final Boolean checksumValidationEnabled;
    private final Boolean chunkedEncodingEnabled;
    private final Boolean dualStackEnabled;
    private final Boolean pathStyleAccessEnabled;
    private final Boolean useArnRegionEnabled;
    private final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration;
    private final String profileName;

    private AwsSdkS3Configuration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        accelerateModeEnabled = builder.accelerateModeEnabled;
        checksumValidationEnabled = builder.checksumValidationEnabled;
        chunkedEncodingEnabled = builder.chunkedEncodingEnabled;
        dualStackEnabled = builder.dualStackEnabled;
        pathStyleAccessEnabled = builder.pathStyleAccessEnabled;
        useArnRegionEnabled = builder.useArnRegionEnabled;
        awsSdkProfileFileConfiguration = builder.awsSdkProfileFileConfiguration;
        profileName = builder.profileName;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /** @return whether S3 Transfer Acceleration is enabled */
    public Boolean getAccelerateModeEnabled() {
        return accelerateModeEnabled;
    }

    /** @return whether response checksum validation is enabled */
    public Boolean getChecksumValidationEnabled() {
        return checksumValidationEnabled;
    }

    /** @return whether chunked encoding is enabled for requests */
    public Boolean getChunkedEncodingEnabled() {
        return chunkedEncodingEnabled;
    }

    /** @return whether dual-stack IPv4/IPv6 endpoints are enabled */
    public Boolean getDualStackEnabled() {
        return dualStackEnabled;
    }

    /** @return whether requests use path-style rather than virtual-hosted bucket addressing */
    public Boolean getPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }

    /** @return whether an ARN's region may override the configured client region */
    public Boolean getUseArnRegionEnabled() {
        return useArnRegionEnabled;
    }

    /** @return profile-file configuration used when building the SDK S3 configuration */
    public AwsSdkProfileFileConfiguration getAwsSdkProfileFileConfiguration() {
        return awsSdkProfileFileConfiguration;
    }

    /** @return name of the profile read from the configured profile file */
    public String getProfileName() {
        return profileName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkS3Configuration that = (AwsSdkS3Configuration) o;
        return Objects.equals(accelerateModeEnabled, that.accelerateModeEnabled) &&
                Objects.equals(checksumValidationEnabled, that.checksumValidationEnabled) &&
                Objects.equals(chunkedEncodingEnabled, that.chunkedEncodingEnabled) &&
                Objects.equals(dualStackEnabled, that.dualStackEnabled) &&
                Objects.equals(pathStyleAccessEnabled, that.pathStyleAccessEnabled) &&
                Objects.equals(useArnRegionEnabled, that.useArnRegionEnabled) &&
                Objects.equals(awsSdkProfileFileConfiguration, that.awsSdkProfileFileConfiguration) &&
                Objects.equals(profileName, that.profileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                accelerateModeEnabled,
                checksumValidationEnabled,
                chunkedEncodingEnabled,
                dualStackEnabled,
                pathStyleAccessEnabled,
                useArnRegionEnabled,
                awsSdkProfileFileConfiguration,
                profileName);
    }

    @Override
    public String toString() {
        return "AwsSdkS3Configuration{" +
                "accelerateModeEnabled=" + accelerateModeEnabled +
                ", checksumValidationEnabled=" + checksumValidationEnabled +
                ", chunkedEncodingEnabled=" + chunkedEncodingEnabled +
                ", dualStackEnabled=" + dualStackEnabled +
                ", pathStyleAccessEnabled=" + pathStyleAccessEnabled +
                ", useArnRegionEnabled=" + useArnRegionEnabled +
                ", awsSdkProfileFileConfiguration=" + awsSdkProfileFileConfiguration +
                ", profileName='" + profileName + '\'' +
                '}';
    }


    /**
     * Builder for {@link AwsSdkS3Configuration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ACCELERATE_MODE_ENABLED = false;
        private static final boolean DEFAULT_CHECK_SUM_VALIDATION_ENABLED = true;
        private static final boolean DEFAULT_CHUNKED_ENCODING_ENABLED = true;
        private static final boolean DEFAULT_DUAL_STACK_ENABLED = false;
        private static final boolean DEFAULT_PATH_STYLE_ACCESS_ENABLED = false;
        private static final AwsSdkProfileFileConfiguration DEFAULT_AWS_SDK_PROFILE_FILE_CONFIGURATION =
                AwsSdkProfileFileConfiguration.builder().build();


        private Boolean accelerateModeEnabled = DEFAULT_ACCELERATE_MODE_ENABLED;
        private Boolean checksumValidationEnabled = DEFAULT_CHECK_SUM_VALIDATION_ENABLED;
        private Boolean chunkedEncodingEnabled = DEFAULT_CHUNKED_ENCODING_ENABLED;
        private Boolean dualStackEnabled = DEFAULT_DUAL_STACK_ENABLED;
        private Boolean pathStyleAccessEnabled = DEFAULT_PATH_STYLE_ACCESS_ENABLED;
        private Boolean useArnRegionEnabled;
        private AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration = DEFAULT_AWS_SDK_PROFILE_FILE_CONFIGURATION;
        private String profileName;


        private Builder() {
        }

        public Builder accelerateModeEnabled(final Boolean accelerateModeEnabled) {
            this.accelerateModeEnabled = accelerateModeEnabled;
            return this;
        }

        public Builder checksumValidationEnabled(final Boolean checksumValidationEnabled) {
            this.checksumValidationEnabled = checksumValidationEnabled;
            return this;
        }

        public Builder chunkedEncodingEnabled(final Boolean chunkedEncodingEnabled) {
            this.chunkedEncodingEnabled = chunkedEncodingEnabled;
            return this;
        }

        public Builder dualStackEnabled(final Boolean dualStackEnabled) {
            this.dualStackEnabled = dualStackEnabled;
            return this;
        }

        public Builder pathStyleAccessEnabled(final Boolean pathStyleAccessEnabled) {
            this.pathStyleAccessEnabled = pathStyleAccessEnabled;
            return this;
        }

        public Builder useArnRegionEnabled(final Boolean useArnRegionEnabled) {
            this.useArnRegionEnabled = useArnRegionEnabled;
            return this;
        }

        public Builder awsSdkProfileFileConfiguration(
                final AwsSdkProfileFileConfiguration awsSdkProfileFileConfiguration)
        {
            this.awsSdkProfileFileConfiguration = awsSdkProfileFileConfiguration;
            return this;
        }

        public Builder profileName(final String profileName) {
            this.profileName = profileName;
            return this;
        }

        public AwsSdkS3Configuration build() {
            return new AwsSdkS3Configuration(this);
        }
    }
    // CheckStyle: Builder end
}
