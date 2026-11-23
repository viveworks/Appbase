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

/**
 * This package contains client services for {@code JDBC} (Java Database Connectivity).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.jdbi.DefaultJdbiClientIntegrationService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultJdbiClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJdbiClientServiceConfiguration.Builder.class)
public final class DefaultJdbiClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultJdbiClientServiceConfiguration.Builder>
        implements JdbiClientServiceConfiguration
{
    /**
     * Configuration for {@link JdbiClientService#getClientIntegrationService()}.
     */
    private final DefaultJdbiClientIntegrationServiceConfiguration defaultDefaultJdbiClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link JdbiClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultJdbiClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultJdbiClientIntegrationServiceConfiguration.class)
    private final Set<DefaultJdbiClientIntegrationServiceConfiguration>
            keyedDefaultJdbiClientIntegrationServiceConfigurations;


    private DefaultJdbiClientServiceConfiguration(final Builder builder) {
        super(builder);

        defaultDefaultJdbiClientIntegrationServiceConfiguration =
                builder.defaultDefaultJdbiClientIntegrationServiceConfiguration;
        keyedDefaultJdbiClientIntegrationServiceConfigurations =
                builder.keyedDefaultJdbiClientIntegrationServiceConfigurations;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultJdbiClientIntegrationServiceConfiguration getDefaultDefaultJdbiClientIntegrationServiceConfiguration() {
        return defaultDefaultJdbiClientIntegrationServiceConfiguration;
    }

    public Set<DefaultJdbiClientIntegrationServiceConfiguration>
    getKeyedDefaultJdbiClientIntegrationServiceConfigurations()
    {
        return keyedDefaultJdbiClientIntegrationServiceConfigurations;
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

        final DefaultJdbiClientServiceConfiguration that = (DefaultJdbiClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultJdbiClientIntegrationServiceConfiguration,
                that.defaultDefaultJdbiClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultJdbiClientIntegrationServiceConfigurations,
                        that.keyedDefaultJdbiClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultJdbiClientIntegrationServiceConfiguration,
                keyedDefaultJdbiClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultJdbiClientServiceConfiguration{" +
                "defaultDefaultJdbiClientIntegrationServiceConfiguration=" +
                defaultDefaultJdbiClientIntegrationServiceConfiguration +
                ", keyedDefaultJdbiClientIntegrationServiceConfigurations=" +
                keyedDefaultJdbiClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultJdbiClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultJdbiClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_JDBI_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultJdbiClientIntegrationServiceConfiguration.builder().build();


        private DefaultJdbiClientIntegrationServiceConfiguration defaultDefaultJdbiClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_JDBI_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultJdbiClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultJdbiClientIntegrationServiceConfiguration.class)
        private Set<DefaultJdbiClientIntegrationServiceConfiguration> keyedDefaultJdbiClientIntegrationServiceConfigurations;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultJdbiClientIntegrationServiceConfiguration(
                final DefaultJdbiClientIntegrationServiceConfiguration
                        defaultDefaultJdbiClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultJdbiClientIntegrationServiceConfiguration =
                    defaultDefaultJdbiClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultJdbiClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultJdbiClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultJdbiClientIntegrationServiceConfiguration.class)
                final Set<DefaultJdbiClientIntegrationServiceConfiguration>
                        keyedDefaultJdbiClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultJdbiClientIntegrationServiceConfigurations =
                    keyedDefaultJdbiClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultJdbiClientServiceConfiguration build() {
            return new DefaultJdbiClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
