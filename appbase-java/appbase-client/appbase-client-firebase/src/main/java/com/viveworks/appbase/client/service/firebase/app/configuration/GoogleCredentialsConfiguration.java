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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auth.oauth2.GoogleCredentials;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link GoogleCredentials}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = GoogleCredentialsConfiguration.Builder.class)
public final class GoogleCredentialsConfiguration {
    private final FirebaseAccessTokenConfiguration firebaseAccessTokenConfiguration;

    private GoogleCredentialsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.firebaseAccessTokenConfiguration, "builder.firebaseAccessTokenConfiguration");

        firebaseAccessTokenConfiguration = builder.firebaseAccessTokenConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public FirebaseAccessTokenConfiguration getFirebaseAccessTokenConfiguration() {
        return firebaseAccessTokenConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GoogleCredentialsConfiguration that = (GoogleCredentialsConfiguration) o;
        return Objects.equals(firebaseAccessTokenConfiguration, that.firebaseAccessTokenConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firebaseAccessTokenConfiguration);
    }

    @Override
    public String toString() {
        return "GoogleCredentialsConfiguration{" +
                "firebaseAccessTokenConfiguration=" + AppbaseUtil.OBFUSCATE +
                '}';
    }


    /**
     * Builder for {@link GoogleCredentialsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private FirebaseAccessTokenConfiguration firebaseAccessTokenConfiguration;

        private Builder() {
        }

        public Builder firebaseAccessTokenConfiguration(
                final FirebaseAccessTokenConfiguration firebaseAccessTokenConfiguration)
        {
            this.firebaseAccessTokenConfiguration = firebaseAccessTokenConfiguration;
            return this;
        }

        public GoogleCredentialsConfiguration build() {
            return new GoogleCredentialsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
