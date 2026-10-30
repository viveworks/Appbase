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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.firebase.app.DefaultFirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultFirebaseAppClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultFirebaseAppClientServiceConfiguration.Builder.class)
public final class DefaultFirebaseAppClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultFirebaseAppClientServiceConfiguration.Builder>
        implements FirebaseAppClientServiceConfiguration
{
    /**
     * Configuration for {@link FirebaseAppClientService#getClientIntegrationService()}.
     */
    private final DefaultFirebaseAppClientIntegrationServiceConfiguration
            defaultDefaultFirebaseAppClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link FirebaseAppClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultFirebaseAppClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultFirebaseAppClientIntegrationServiceConfiguration.class)
    private final Set<DefaultFirebaseAppClientIntegrationServiceConfiguration>
            keyedDefaultFirebaseAppClientIntegrationServiceConfigurations;


    private DefaultFirebaseAppClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultFirebaseAppClientIntegrationServiceConfiguration,
                "builder.defaultDefaultFirebaseAppClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultFirebaseAppClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultFirebaseAppClientIntegrationServiceConfigurations");
        }


        defaultDefaultFirebaseAppClientIntegrationServiceConfiguration =
                builder.defaultDefaultFirebaseAppClientIntegrationServiceConfiguration;
        keyedDefaultFirebaseAppClientIntegrationServiceConfigurations =
                builder.keyedDefaultFirebaseAppClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultFirebaseAppClientIntegrationServiceConfiguration
    getDefaultDefaultFirebaseAppClientIntegrationServiceConfiguration()
    {
        return defaultDefaultFirebaseAppClientIntegrationServiceConfiguration;
    }

    public Set<DefaultFirebaseAppClientIntegrationServiceConfiguration>
    getKeyedDefaultFirebaseAppClientIntegrationServiceConfigurations()
    {
        return keyedDefaultFirebaseAppClientIntegrationServiceConfigurations;
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

        final DefaultFirebaseAppClientServiceConfiguration that = (DefaultFirebaseAppClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration,
                that.defaultDefaultFirebaseAppClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultFirebaseAppClientIntegrationServiceConfigurations,
                        that.keyedDefaultFirebaseAppClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration,
                keyedDefaultFirebaseAppClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultFirebaseAppClientServiceConfiguration{" +
                "defaultDefaultFirebaseAppClientIntegrationServiceConfiguration=" +
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration +
                ", keyedDefaultFirebaseAppClientIntegrationServiceConfigurations=" +
                keyedDefaultFirebaseAppClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultFirebaseAppClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultFirebaseAppClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_FIREBASE_APP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultFirebaseAppClientIntegrationServiceConfiguration.builder().build();



        private DefaultFirebaseAppClientIntegrationServiceConfiguration
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_FIREBASE_APP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultFirebaseAppClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultFirebaseAppClientIntegrationServiceConfiguration.class)
        private Set<DefaultFirebaseAppClientIntegrationServiceConfiguration>
                keyedDefaultFirebaseAppClientIntegrationServiceConfigurations;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultFirebaseAppClientIntegrationServiceConfiguration(
                final DefaultFirebaseAppClientIntegrationServiceConfiguration
                        defaultDefaultFirebaseAppClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultFirebaseAppClientIntegrationServiceConfiguration =
                    defaultDefaultFirebaseAppClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultFirebaseAppClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultFirebaseAppClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultFirebaseAppClientIntegrationServiceConfiguration.class)
                final Set<DefaultFirebaseAppClientIntegrationServiceConfiguration>
                        keyedDefaultFirebaseAppClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultFirebaseAppClientIntegrationServiceConfigurations =
                    keyedDefaultFirebaseAppClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultFirebaseAppClientServiceConfiguration build() {
            return new DefaultFirebaseAppClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
