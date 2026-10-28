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

package com.viveworks.appbase.client.service.firebase.auth.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.DefaultFirebaseAuthClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultFirebaseAuthClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultFirebaseAuthClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultFirebaseAuthClientIntegrationServiceConfiguration
        implements FirebaseAuthClientIntegrationServiceConfiguration
{
    @NullableConditionally(dependsOn = "FirebaseAppClientService.getDefaultAppbaseClientKey()")
    private final TextValueConfiguration textValueKeyConfiguration;

    private final boolean emulatedEnvironment;

    private final boolean useDefaultFirebaseAppClientIntegrationService;

    private final TextValueConfiguration firebaseAppClientIntegrationServiceKeyConfiguration;


    private DefaultFirebaseAuthClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (!builder.useDefaultFirebaseAppClientIntegrationService) {
            AppbaseUtil.notNull(
                    builder.firebaseAppClientIntegrationServiceKeyConfiguration,
                    "builder.firebaseAppClientIntegrationServiceKeyConfiguration");
        }

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        emulatedEnvironment = builder.emulatedEnvironment;
        useDefaultFirebaseAppClientIntegrationService = builder.useDefaultFirebaseAppClientIntegrationService;
        firebaseAppClientIntegrationServiceKeyConfiguration = builder.firebaseAppClientIntegrationServiceKeyConfiguration;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public boolean isEmulatedEnvironment() {
        return emulatedEnvironment;
    }

    public boolean isUseDefaultFirebaseAppClientIntegrationService() {
        return useDefaultFirebaseAppClientIntegrationService;
    }

    public TextValueConfiguration getFirebaseAppClientIntegrationServiceKeyConfiguration() {
        return firebaseAppClientIntegrationServiceKeyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultFirebaseAuthClientIntegrationServiceConfiguration that =
                (DefaultFirebaseAuthClientIntegrationServiceConfiguration) o;
        return useDefaultFirebaseAppClientIntegrationService == that.useDefaultFirebaseAppClientIntegrationService &&
                emulatedEnvironment == that.emulatedEnvironment &&
                Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(
                        firebaseAppClientIntegrationServiceKeyConfiguration,
                        that.firebaseAppClientIntegrationServiceKeyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration,
                emulatedEnvironment,
                useDefaultFirebaseAppClientIntegrationService,
                firebaseAppClientIntegrationServiceKeyConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultFirebaseAuthClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", emulatedEnvironment=" + emulatedEnvironment +
                ", useDefaultFirebaseAppClientIntegrationService=" + useDefaultFirebaseAppClientIntegrationService +
                ", firebaseAppClientIntegrationServiceKeyConfiguration=" +
                firebaseAppClientIntegrationServiceKeyConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultFirebaseAuthClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_USE_DEFAULT_FIREBASE_APP_CLIENT_INTEGRATION_SERVICE = true;
        private static final boolean DEFAULT_EMULATED_ENVIRONMENT = true;


        private TextValueConfiguration textValueKeyConfiguration;

        private boolean emulatedEnvironment = DEFAULT_EMULATED_ENVIRONMENT;

        private boolean useDefaultFirebaseAppClientIntegrationService =
                DEFAULT_USE_DEFAULT_FIREBASE_APP_CLIENT_INTEGRATION_SERVICE;

        private TextValueConfiguration firebaseAppClientIntegrationServiceKeyConfiguration;

        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder emulatedEnvironment(final boolean emulatedEnvironment) {
            this.emulatedEnvironment = emulatedEnvironment;
            return this;
        }

        public Builder useDefaultFirebaseAppClientIntegrationService(
                final boolean useDefaultFirebaseAppClientIntegrationService)
        {
            this.useDefaultFirebaseAppClientIntegrationService = useDefaultFirebaseAppClientIntegrationService;
            return this;
        }

        public Builder firebaseAppClientIntegrationServiceKeyConfiguration(
                final TextValueConfiguration firebaseAppClientIntegrationServiceKeyConfiguration)
        {
            this.firebaseAppClientIntegrationServiceKeyConfiguration =
                    firebaseAppClientIntegrationServiceKeyConfiguration;
            return this;
        }

        public DefaultFirebaseAuthClientIntegrationServiceConfiguration build() {
            return new DefaultFirebaseAuthClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
