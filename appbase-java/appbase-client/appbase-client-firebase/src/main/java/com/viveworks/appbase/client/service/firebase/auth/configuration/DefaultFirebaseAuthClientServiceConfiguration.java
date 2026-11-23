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

package com.viveworks.appbase.client.service.firebase.auth.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.DefaultFirebaseAuthClientService;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultFirebaseAuthClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultFirebaseAuthClientServiceConfiguration.Builder.class)
public final class DefaultFirebaseAuthClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultFirebaseAuthClientServiceConfiguration.Builder>
        implements FirebaseAuthClientServiceConfiguration
{
    /**
     * Configuration for {@link FirebaseAuthClientService#getClientIntegrationService()}.
     */
    private final DefaultFirebaseAuthClientIntegrationServiceConfiguration
            defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link FirebaseAuthClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultFirebaseAuthClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultFirebaseAuthClientIntegrationServiceConfiguration.class)
    private final Set<DefaultFirebaseAuthClientIntegrationServiceConfiguration>
            keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Firebase Auth client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;



    private DefaultFirebaseAuthClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration =
                builder.defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration;
        keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations =
                builder.keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations;
        reactiveOperationConfiguration = builder.reactiveOperationConfiguration;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultFirebaseAuthClientIntegrationServiceConfiguration
    getDefaultDefaultFirebaseAuthClientIntegrationServiceConfiguration()
    {
        return defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration;
    }

    public Set<DefaultFirebaseAuthClientIntegrationServiceConfiguration>
    getKeyedDefaultFirebaseAuthClientIntegrationServiceConfigurations()
    {
        return keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Firebase Auth client service.
     *
     * @return reactive operation configuration
     */
    @Override
    public DefaultReactiveOperationConfiguration getReactiveOperationConfiguration() {
        return reactiveOperationConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final DefaultFirebaseAuthClientServiceConfiguration that = (DefaultFirebaseAuthClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration,
                that.defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations,
                        that.keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration,
                keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultFirebaseAuthClientServiceConfiguration{" +
                "defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration=" +
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration +
                ", keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations=" +
                keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultFirebaseAuthClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultFirebaseAuthClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_FIREBASE_AUTH_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultFirebaseAuthClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultFirebaseAuthClientIntegrationServiceConfiguration
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_FIREBASE_AUTH_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultFirebaseAuthClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultFirebaseAuthClientIntegrationServiceConfiguration.class)
        private Set<DefaultFirebaseAuthClientIntegrationServiceConfiguration>
                keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration(
                final DefaultFirebaseAuthClientIntegrationServiceConfiguration
                        defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration =
                    defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultFirebaseAuthClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultFirebaseAuthClientIntegrationServiceConfiguration.class)
                final Set<DefaultFirebaseAuthClientIntegrationServiceConfiguration>
                        keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations =
                    keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Firebase Auth client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultFirebaseAuthClientServiceConfiguration build() {
            return new DefaultFirebaseAuthClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
