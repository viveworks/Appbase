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

package com.viveworks.appbase.client.service.clamav.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.clamav.ClamAVClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.clamav.DefaultClamAVClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultClamAVClientServiceConfiguration.Builder.class)
public final class DefaultClamAVClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultClamAVClientServiceConfiguration.Builder>
        implements ClamAVClientServiceConfiguration
{
    /**
     * Configuration for {@link ClamAVClientService#getClientIntegrationService()}.
     */
    private final DefaultClamAVClientIntegrationServiceConfiguration
            defaultDefaultClamAVClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link ClamAVClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultClamAVClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultClamAVClientIntegrationServiceConfiguration.class)
    private final Set<DefaultClamAVClientIntegrationServiceConfiguration>
            keyedDefaultClamAVClientIntegrationServiceConfigurations;


    private DefaultClamAVClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultClamAVClientIntegrationServiceConfiguration,
                "builder.defaultDefaultClamAVClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultClamAVClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultClamAVClientIntegrationServiceConfigurations");
        }


        defaultDefaultClamAVClientIntegrationServiceConfiguration =
                builder.defaultDefaultClamAVClientIntegrationServiceConfiguration;
        keyedDefaultClamAVClientIntegrationServiceConfigurations =
                builder.keyedDefaultClamAVClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultClamAVClientIntegrationServiceConfiguration
    getDefaultDefaultClamAVClientIntegrationServiceConfiguration()
    {
        return defaultDefaultClamAVClientIntegrationServiceConfiguration;
    }

    public Set<DefaultClamAVClientIntegrationServiceConfiguration>
    getKeyedDefaultClamAVClientIntegrationServiceConfigurations()
    {
        return keyedDefaultClamAVClientIntegrationServiceConfigurations;
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

        final DefaultClamAVClientServiceConfiguration that = (DefaultClamAVClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultClamAVClientIntegrationServiceConfiguration,
                that.defaultDefaultClamAVClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultClamAVClientIntegrationServiceConfigurations,
                        that.keyedDefaultClamAVClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                defaultDefaultClamAVClientIntegrationServiceConfiguration,
                keyedDefaultClamAVClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultClamAVClientServiceConfiguration{" +
                "defaultDefaultClamAVClientIntegrationServiceConfiguration=" +
                defaultDefaultClamAVClientIntegrationServiceConfiguration +
                ", keyedDefaultClamAVClientIntegrationServiceConfigurations=" +
                keyedDefaultClamAVClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultClamAVClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultClamAVClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_CLAMAV_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultClamAVClientIntegrationServiceConfiguration.builder().build();


        private DefaultClamAVClientIntegrationServiceConfiguration
                defaultDefaultClamAVClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_CLAMAV_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultClamAVClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultClamAVClientIntegrationServiceConfiguration.class)
        private Set<DefaultClamAVClientIntegrationServiceConfiguration>
                keyedDefaultClamAVClientIntegrationServiceConfigurations;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultClamAVClientIntegrationServiceConfiguration(
                final DefaultClamAVClientIntegrationServiceConfiguration
                        defaultDefaultClamAVClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultClamAVClientIntegrationServiceConfiguration =
                    defaultDefaultClamAVClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultClamAVClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultClamAVClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultClamAVClientIntegrationServiceConfiguration.class)
                final Set<DefaultClamAVClientIntegrationServiceConfiguration>
                        keyedDefaultClamAVClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultClamAVClientIntegrationServiceConfigurations =
                    keyedDefaultClamAVClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultClamAVClientServiceConfiguration build() {
            return new DefaultClamAVClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
