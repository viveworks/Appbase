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

package com.viveworks.appbase.client.service.jpa.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.jpa.DefaultJpaClientService;
import com.viveworks.appbase.client.service.jpa.JpaClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultJpaClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJpaClientServiceConfiguration.Builder.class)
public final class DefaultJpaClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultJpaClientServiceConfiguration.Builder>
        implements JpaClientServiceConfiguration
{
    /**
     * Configuration for {@link JpaClientService#getClientIntegrationService()}.
     */
    private final DefaultJpaClientIntegrationServiceConfiguration defaultDefaultJpaClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link JpaClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultJpaClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultJpaClientIntegrationServiceConfiguration.class)
    private final Set<DefaultJpaClientIntegrationServiceConfiguration> keyedDefaultJpaClientIntegrationServiceConfigurations;


    private DefaultJpaClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultJpaClientIntegrationServiceConfiguration,
                "builder.defaultDefaultJpaClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultJpaClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultJpaClientIntegrationServiceConfigurations");
        }


        defaultDefaultJpaClientIntegrationServiceConfiguration =
                builder.defaultDefaultJpaClientIntegrationServiceConfiguration;
        keyedDefaultJpaClientIntegrationServiceConfigurations =
                builder.keyedDefaultJpaClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultJpaClientIntegrationServiceConfiguration getDefaultDefaultJpaClientIntegrationServiceConfiguration() {
        return defaultDefaultJpaClientIntegrationServiceConfiguration;
    }

    @NotNullConditionally
    public Set<DefaultJpaClientIntegrationServiceConfiguration> getKeyedDefaultJpaClientIntegrationServiceConfigurations() {
        return keyedDefaultJpaClientIntegrationServiceConfigurations;
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

        final DefaultJpaClientServiceConfiguration that = (DefaultJpaClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultJpaClientIntegrationServiceConfiguration,
                that.defaultDefaultJpaClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultJpaClientIntegrationServiceConfigurations,
                        that.keyedDefaultJpaClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultJpaClientIntegrationServiceConfiguration,
                keyedDefaultJpaClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultJpaClientServiceConfiguration{" +
                "defaultDefaultJpaClientIntegrationServiceConfiguration=" +
                defaultDefaultJpaClientIntegrationServiceConfiguration +
                ", keyedDefaultJpaClientIntegrationServiceConfigurations=" +
                keyedDefaultJpaClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultJpaClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultJpaClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_JPA_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultJpaClientIntegrationServiceConfiguration.builder().build();


        private DefaultJpaClientIntegrationServiceConfiguration
                defaultDefaultJpaClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_JPA_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultJpaClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultJpaClientIntegrationServiceConfiguration.class)
        private Set<DefaultJpaClientIntegrationServiceConfiguration>
                keyedDefaultJpaClientIntegrationServiceConfigurations;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultJpaClientIntegrationServiceConfiguration(
                final DefaultJpaClientIntegrationServiceConfiguration
                        defaultDefaultJpaClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultJpaClientIntegrationServiceConfiguration =
                    defaultDefaultJpaClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultJpaClientIntegrationServiceConfigurations(
                @NotNullConditionally
                @JsonSerialize(contentAs = DefaultJpaClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultJpaClientIntegrationServiceConfiguration.class)
                final Set<DefaultJpaClientIntegrationServiceConfiguration>
                        keyedDefaultJpaClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultJpaClientIntegrationServiceConfigurations =
                    keyedDefaultJpaClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultJpaClientServiceConfiguration build() {
            return new DefaultJpaClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
