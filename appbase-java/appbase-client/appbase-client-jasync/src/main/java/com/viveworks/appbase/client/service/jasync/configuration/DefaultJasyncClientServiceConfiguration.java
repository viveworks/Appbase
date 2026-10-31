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

package com.viveworks.appbase.client.service.jasync.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.jasync.DefaultJasyncClientService;
import com.viveworks.appbase.client.service.jasync.JasyncClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultJasyncClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJasyncClientServiceConfiguration.Builder.class)
public final class DefaultJasyncClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultJasyncClientServiceConfiguration.Builder>
        implements JasyncClientServiceConfiguration
{
    /**
     * Configuration for {@link JasyncClientService#getClientIntegrationService()}.
     */
    private final DefaultJasyncClientIntegrationServiceConfiguration
            defaultDefaultJasyncClientIntegrationServiceConfiguration;


    /**
     * Configuration for
     * {@link JasyncClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultJasyncClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultJasyncClientIntegrationServiceConfiguration.class)
    private final Set<DefaultJasyncClientIntegrationServiceConfiguration>
            keyedDefaultJasyncClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Jasync client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;


    private DefaultJasyncClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultJasyncClientIntegrationServiceConfiguration,
                "builder.defaultDefaultJasyncClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultJasyncClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultJasyncClientIntegrationServiceConfigurations");
        }


        defaultDefaultJasyncClientIntegrationServiceConfiguration =
                builder.defaultDefaultJasyncClientIntegrationServiceConfiguration;
        keyedDefaultJasyncClientIntegrationServiceConfigurations =
                builder.keyedDefaultJasyncClientIntegrationServiceConfigurations;
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

    public DefaultJasyncClientIntegrationServiceConfiguration
    getDefaultDefaultJasyncClientIntegrationServiceConfiguration()
    {
        return defaultDefaultJasyncClientIntegrationServiceConfiguration;
    }

    @NotNullConditionally
    public Set<DefaultJasyncClientIntegrationServiceConfiguration>
    getKeyedDefaultJasyncClientIntegrationServiceConfigurations()
    {
        return keyedDefaultJasyncClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Jasync client service.
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

        final DefaultJasyncClientServiceConfiguration that = (DefaultJasyncClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultJasyncClientIntegrationServiceConfiguration,
                that.defaultDefaultJasyncClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultJasyncClientIntegrationServiceConfigurations,
                        that.keyedDefaultJasyncClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultJasyncClientIntegrationServiceConfiguration,
                keyedDefaultJasyncClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultJasyncClientServiceConfiguration{" +
                "defaultDefaultJasyncClientIntegrationServiceConfiguration=" +
                defaultDefaultJasyncClientIntegrationServiceConfiguration +
                ", keyedDefaultJasyncClientIntegrationServiceConfigurations=" +
                keyedDefaultJasyncClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultJasyncClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultJasyncClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_JASYNC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultJasyncClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();



        private DefaultJasyncClientIntegrationServiceConfiguration
                defaultDefaultJasyncClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_JASYNC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultJasyncClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultJasyncClientIntegrationServiceConfiguration.class)
        private Set<DefaultJasyncClientIntegrationServiceConfiguration>
                keyedDefaultJasyncClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultJasyncClientIntegrationServiceConfiguration(
                final DefaultJasyncClientIntegrationServiceConfiguration
                        defaultDefaultJasyncClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultJasyncClientIntegrationServiceConfiguration =
                    defaultDefaultJasyncClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultJasyncClientIntegrationServiceConfigurations(
                @NotNullConditionally
                @JsonSerialize(contentAs = DefaultJasyncClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultJasyncClientIntegrationServiceConfiguration.class)
                final Set<DefaultJasyncClientIntegrationServiceConfiguration>
                        keyedDefaultJasyncClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultJasyncClientIntegrationServiceConfigurations =
                    keyedDefaultJasyncClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Jasync client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultJasyncClientServiceConfiguration build() {
            return new DefaultJasyncClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
