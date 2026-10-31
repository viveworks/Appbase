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

package com.viveworks.appbase.client.service.r2dbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.r2dbc.DefaultR2dbcClientService;
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultR2dbcClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultR2dbcClientServiceConfiguration.Builder.class)
public final class DefaultR2dbcClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultR2dbcClientServiceConfiguration.Builder>
        implements R2dbcClientServiceConfiguration
{
    /**
     * Configuration for {@link R2dbcClientService#getClientIntegrationService()}.
     */
    private final DefaultR2dbcClientIntegrationServiceConfiguration defaultDefaultR2dbcClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link R2dbcClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultR2dbcClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultR2dbcClientIntegrationServiceConfiguration.class)
    private final Set<DefaultR2dbcClientIntegrationServiceConfiguration>
            keyedDefaultR2dbcClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this R2DBC client service (added in a
     * later task; the getter is scaffolded here so the configuration shape is stable).
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;

    private DefaultR2dbcClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultR2dbcClientIntegrationServiceConfiguration,
                "builder.defaultDefaultR2dbcClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultR2dbcClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultR2dbcClientIntegrationServiceConfigurations");
        }

        defaultDefaultR2dbcClientIntegrationServiceConfiguration =
                builder.defaultDefaultR2dbcClientIntegrationServiceConfiguration;
        keyedDefaultR2dbcClientIntegrationServiceConfigurations =
                builder.keyedDefaultR2dbcClientIntegrationServiceConfigurations;
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

    public DefaultR2dbcClientIntegrationServiceConfiguration getDefaultDefaultR2dbcClientIntegrationServiceConfiguration() {
        return defaultDefaultR2dbcClientIntegrationServiceConfiguration;
    }

    public Set<DefaultR2dbcClientIntegrationServiceConfiguration>
    getKeyedDefaultR2dbcClientIntegrationServiceConfigurations()
    {
        return keyedDefaultR2dbcClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this R2DBC client service.
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

        final DefaultR2dbcClientServiceConfiguration that = (DefaultR2dbcClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultR2dbcClientIntegrationServiceConfiguration,
                that.defaultDefaultR2dbcClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultR2dbcClientIntegrationServiceConfigurations,
                        that.keyedDefaultR2dbcClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultR2dbcClientIntegrationServiceConfiguration,
                keyedDefaultR2dbcClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultR2dbcClientServiceConfiguration{" +
                "defaultDefaultR2dbcClientIntegrationServiceConfiguration=" +
                defaultDefaultR2dbcClientIntegrationServiceConfiguration +
                ", keyedDefaultR2dbcClientIntegrationServiceConfigurations=" +
                keyedDefaultR2dbcClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultR2dbcClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultR2dbcClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_R2DBC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultR2dbcClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultR2dbcClientIntegrationServiceConfiguration
                defaultDefaultR2dbcClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_R2DBC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultR2dbcClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultR2dbcClientIntegrationServiceConfiguration.class)
        private Set<DefaultR2dbcClientIntegrationServiceConfiguration>
                keyedDefaultR2dbcClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultR2dbcClientIntegrationServiceConfiguration(
                final DefaultR2dbcClientIntegrationServiceConfiguration
                        defaultDefaultR2dbcClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultR2dbcClientIntegrationServiceConfiguration =
                    defaultDefaultR2dbcClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultR2dbcClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultR2dbcClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultR2dbcClientIntegrationServiceConfiguration.class)
                final Set<DefaultR2dbcClientIntegrationServiceConfiguration>
                        keyedDefaultR2dbcClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultR2dbcClientIntegrationServiceConfigurations =
                    keyedDefaultR2dbcClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this R2DBC client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultR2dbcClientServiceConfiguration build() {
            return new DefaultR2dbcClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
