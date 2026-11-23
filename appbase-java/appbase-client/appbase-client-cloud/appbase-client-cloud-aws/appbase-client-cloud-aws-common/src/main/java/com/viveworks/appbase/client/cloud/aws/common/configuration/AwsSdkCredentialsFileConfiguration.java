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
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.profiles.ProfileFile;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link ProfileFile} with {@link ProfileFile.Type#CREDENTIALS}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkCredentialsFileConfiguration.Builder.class)
public final class AwsSdkCredentialsFileConfiguration {
    private final String location;

    private AwsSdkCredentialsFileConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.location, "builder.location");

        location = builder.location;
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
     * Returns the configured location.
     *
     * @return the configured location
     */
    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkCredentialsFileConfiguration that = (AwsSdkCredentialsFileConfiguration) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "AwsSdkCredentialsFileConfiguration{" +
                "location='" + location + '\'' +
                '}';
    }


    /**
     * Builder for {@link AwsSdkCredentialsFileConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_LOCATION = "classpath:///aws-credentials.profile";

        private String location = DEFAULT_LOCATION;

        private Builder() {
        }

        public Builder location(final String location) {
            this.location = location;
            return this;
        }

        public AwsSdkCredentialsFileConfiguration build() {
            return new AwsSdkCredentialsFileConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
