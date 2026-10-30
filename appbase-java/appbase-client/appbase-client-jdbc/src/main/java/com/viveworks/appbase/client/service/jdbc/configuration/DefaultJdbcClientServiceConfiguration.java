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

package com.viveworks.appbase.client.service.jdbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.jdbc.DefaultJdbcClientService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultJdbcClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJdbcClientServiceConfiguration.Builder.class)
public final class DefaultJdbcClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultJdbcClientServiceConfiguration.Builder>
        implements JdbcClientServiceConfiguration
{
    /**
     * Configuration for {@link JdbcClientService#getClientIntegrationService()}.
     */
    private final DefaultJdbcClientIntegrationServiceConfiguration defaultDefaultJdbcClientIntegrationServiceConfiguration;


    /**
     * Configuration for
     * {@link JdbcClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultJdbcClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultJdbcClientIntegrationServiceConfiguration.class)
    private final Set<DefaultJdbcClientIntegrationServiceConfiguration>
            keyedDefaultJdbcClientIntegrationServiceConfigurations;


    private DefaultJdbcClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultJdbcClientIntegrationServiceConfiguration,
                "builder.defaultDefaultJdbcClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultJdbcClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultJdbcClientIntegrationServiceConfigurations");
        }


        defaultDefaultJdbcClientIntegrationServiceConfiguration =
                builder.defaultDefaultJdbcClientIntegrationServiceConfiguration;
        keyedDefaultJdbcClientIntegrationServiceConfigurations =
                builder.keyedDefaultJdbcClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultJdbcClientIntegrationServiceConfiguration getDefaultDefaultJdbcClientIntegrationServiceConfiguration() {
        return defaultDefaultJdbcClientIntegrationServiceConfiguration;
    }

    public Set<DefaultJdbcClientIntegrationServiceConfiguration>
    getKeyedDefaultJdbcClientIntegrationServiceConfigurations()
    {
        return keyedDefaultJdbcClientIntegrationServiceConfigurations;
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

        final DefaultJdbcClientServiceConfiguration that = (DefaultJdbcClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultJdbcClientIntegrationServiceConfiguration,
                that.defaultDefaultJdbcClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultJdbcClientIntegrationServiceConfigurations,
                        that.keyedDefaultJdbcClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultJdbcClientIntegrationServiceConfiguration,
                keyedDefaultJdbcClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultJdbcClientServiceConfiguration{" +
                "defaultDefaultJdbcClientIntegrationServiceConfiguration=" +
                defaultDefaultJdbcClientIntegrationServiceConfiguration +
                ", keyedDefaultJdbcClientIntegrationServiceConfigurations=" +
                keyedDefaultJdbcClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultJdbcClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultJdbcClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_JDBC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultJdbcClientIntegrationServiceConfiguration.builder().build();



        private DefaultJdbcClientIntegrationServiceConfiguration defaultDefaultJdbcClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_JDBC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultJdbcClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultJdbcClientIntegrationServiceConfiguration.class)
        private Set<DefaultJdbcClientIntegrationServiceConfiguration> keyedDefaultJdbcClientIntegrationServiceConfigurations;



        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultJdbcClientIntegrationServiceConfiguration(
                final DefaultJdbcClientIntegrationServiceConfiguration
                        defaultDefaultJdbcClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultJdbcClientIntegrationServiceConfiguration =
                    defaultDefaultJdbcClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultJdbcClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultJdbcClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultJdbcClientIntegrationServiceConfiguration.class)
                final Set<DefaultJdbcClientIntegrationServiceConfiguration>
                        keyedDefaultJdbcClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultJdbcClientIntegrationServiceConfigurations =
                    keyedDefaultJdbcClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultJdbcClientServiceConfiguration build() {
            return new DefaultJdbcClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
