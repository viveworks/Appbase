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

package com.viveworks.appbase.client.service.smtp.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.smtp.DefaultSmtpClientService;
import com.viveworks.appbase.client.service.smtp.SmtpClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultSmtpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultSmtpClientServiceConfiguration.Builder.class)
public final class DefaultSmtpClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultSmtpClientServiceConfiguration.Builder>
        implements SmtpClientServiceConfiguration
{
    /**
     * Configuration for {@link SmtpClientService#getClientIntegrationService()}.
     */
    private final DefaultSmtpClientIntegrationServiceConfiguration defaultDefaultSmtpClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link SmtpClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultSmtpClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultSmtpClientIntegrationServiceConfiguration.class)
    private final Set<DefaultSmtpClientIntegrationServiceConfiguration>
            keyedDefaultSmtpClientIntegrationServiceConfigurations;


    private DefaultSmtpClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultSmtpClientIntegrationServiceConfiguration,
                "builder.defaultDefaultSmtpClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultSmtpClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultSmtpClientIntegrationServiceConfigurations");
        }

        defaultDefaultSmtpClientIntegrationServiceConfiguration =
                builder.defaultDefaultSmtpClientIntegrationServiceConfiguration;
        keyedDefaultSmtpClientIntegrationServiceConfigurations =
                builder.keyedDefaultSmtpClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultSmtpClientIntegrationServiceConfiguration getDefaultDefaultSmtpClientIntegrationServiceConfiguration() {
        return defaultDefaultSmtpClientIntegrationServiceConfiguration;
    }

    public Set<DefaultSmtpClientIntegrationServiceConfiguration>
    getKeyedDefaultSmtpClientIntegrationServiceConfigurations()
    {
        return keyedDefaultSmtpClientIntegrationServiceConfigurations;
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

        final DefaultSmtpClientServiceConfiguration that = (DefaultSmtpClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultSmtpClientIntegrationServiceConfiguration,
                that.defaultDefaultSmtpClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultSmtpClientIntegrationServiceConfigurations,
                        that.keyedDefaultSmtpClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultSmtpClientIntegrationServiceConfiguration,
                keyedDefaultSmtpClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultSmtpClientServiceConfiguration{" +
                "defaultDefaultSmtpClientIntegrationServiceConfiguration=" +
                defaultDefaultSmtpClientIntegrationServiceConfiguration +
                ", keyedDefaultSmtpClientIntegrationServiceConfigurations=" +
                keyedDefaultSmtpClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultSmtpClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultSmtpClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_SMTP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultSmtpClientIntegrationServiceConfiguration.builder().build();


        private DefaultSmtpClientIntegrationServiceConfiguration defaultDefaultSmtpClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_SMTP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultSmtpClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultSmtpClientIntegrationServiceConfiguration.class)
        private Set<DefaultSmtpClientIntegrationServiceConfiguration> keyedDefaultSmtpClientIntegrationServiceConfigurations;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultSmtpClientIntegrationServiceConfiguration(
                final DefaultSmtpClientIntegrationServiceConfiguration
                        defaultDefaultSmtpClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultSmtpClientIntegrationServiceConfiguration =
                    defaultDefaultSmtpClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultSmtpClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultSmtpClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultSmtpClientIntegrationServiceConfiguration.class)
                final Set<DefaultSmtpClientIntegrationServiceConfiguration>
                        keyedDefaultSmtpClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultSmtpClientIntegrationServiceConfigurations =
                    keyedDefaultSmtpClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultSmtpClientServiceConfiguration build() {
            return new DefaultSmtpClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
