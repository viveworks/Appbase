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

package com.viveworks.appbase.client.service.druid.sql.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.druid.sql.DefaultDruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultDruidSqlClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultDruidSqlClientServiceConfiguration.Builder.class)
public final class DefaultDruidSqlClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultDruidSqlClientServiceConfiguration.Builder>
        implements DruidSqlClientServiceConfiguration
{
    /**
     * Configuration for {@link DruidSqlClientService#getClientIntegrationService()}.
     */
    private final DefaultDruidSqlClientIntegrationServiceConfiguration defaultDruidSqlClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link DruidSqlClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultDruidSqlClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultDruidSqlClientIntegrationServiceConfiguration.class)
    private final Set<DefaultDruidSqlClientIntegrationServiceConfiguration>
            keyedDefaultDruidSqlClientIntegrationServiceConfigurations;


    private DefaultDruidSqlClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDruidSqlClientIntegrationServiceConfiguration,
                "builder.defaultDruidSqlClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultDruidSqlClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultDruidSqlClientIntegrationServiceConfigurations");
        }


        defaultDruidSqlClientIntegrationServiceConfiguration =
                builder.defaultDruidSqlClientIntegrationServiceConfiguration;
        keyedDefaultDruidSqlClientIntegrationServiceConfigurations =
                builder.keyedDefaultDruidSqlClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultDruidSqlClientIntegrationServiceConfiguration
    getDefaultDruidSqlClientIntegrationServiceConfiguration()
    {
        return defaultDruidSqlClientIntegrationServiceConfiguration;
    }

    public Set<DefaultDruidSqlClientIntegrationServiceConfiguration>
    getKeyedDefaultDruidSqlClientIntegrationServiceConfigurations()
    {
        return keyedDefaultDruidSqlClientIntegrationServiceConfigurations;
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

        final DefaultDruidSqlClientServiceConfiguration that = (DefaultDruidSqlClientServiceConfiguration) o;
        return Objects.equals(
                defaultDruidSqlClientIntegrationServiceConfiguration,
                that.defaultDruidSqlClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultDruidSqlClientIntegrationServiceConfigurations,
                        that.keyedDefaultDruidSqlClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDruidSqlClientIntegrationServiceConfiguration,
                keyedDefaultDruidSqlClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultDruidSqlClientServiceConfiguration{" +
                "defaultDruidSqlClientIntegrationServiceConfiguration=" +
                defaultDruidSqlClientIntegrationServiceConfiguration +
                ", keyedDefaultDruidSqlClientIntegrationServiceConfigurations=" +
                keyedDefaultDruidSqlClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultDruidSqlClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultDruidSqlClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DRUID_SQL_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultDruidSqlClientIntegrationServiceConfiguration.builder().build();


        private DefaultDruidSqlClientIntegrationServiceConfiguration
                defaultDruidSqlClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DRUID_SQL_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultDruidSqlClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultDruidSqlClientIntegrationServiceConfiguration.class)
        private Set<DefaultDruidSqlClientIntegrationServiceConfiguration>
                keyedDefaultDruidSqlClientIntegrationServiceConfigurations;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDruidSqlClientIntegrationServiceConfiguration(
                final DefaultDruidSqlClientIntegrationServiceConfiguration
                        defaultDruidSqlClientIntegrationServiceConfiguration)
        {
            this.defaultDruidSqlClientIntegrationServiceConfiguration =
                    defaultDruidSqlClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultDruidSqlClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultDruidSqlClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultDruidSqlClientIntegrationServiceConfiguration.class)
                final Set<DefaultDruidSqlClientIntegrationServiceConfiguration>
                        keyedDefaultDruidSqlClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultDruidSqlClientIntegrationServiceConfigurations =
                    keyedDefaultDruidSqlClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultDruidSqlClientServiceConfiguration build() {
            return new DefaultDruidSqlClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
