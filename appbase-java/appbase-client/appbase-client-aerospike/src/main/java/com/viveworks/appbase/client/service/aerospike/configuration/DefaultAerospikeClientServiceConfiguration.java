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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of {@link AerospikeClientServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAerospikeClientServiceConfiguration.Builder.class)
public final class DefaultAerospikeClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultAerospikeClientServiceConfiguration.Builder>
        implements AerospikeClientServiceConfiguration
{
    /**
     * Configuration for
     * {@link com.viveworks.appbase.client.service.aerospike.AerospikeClientService#getClientIntegrationService()}.
     */
    private final DefaultAerospikeClientIntegrationServiceConfiguration
            defaultDefaultAerospikeClientIntegrationServiceConfiguration;


    /**
     * Configurations for
     * {@link com.viveworks.appbase.client.service.aerospike.AerospikeClientService#getClientIntegrationService(
     * AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultAerospikeClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultAerospikeClientIntegrationServiceConfiguration.class)
    private final Set<DefaultAerospikeClientIntegrationServiceConfiguration>
            keyedDefaultAerospikeClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Aerospike client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;



    private DefaultAerospikeClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultAerospikeClientIntegrationServiceConfiguration,
                "builder.defaultDefaultAerospikeClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultAerospikeClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultAerospikeClientIntegrationServiceConfigurations");
        }


        defaultDefaultAerospikeClientIntegrationServiceConfiguration =
                builder.defaultDefaultAerospikeClientIntegrationServiceConfiguration;
        keyedDefaultAerospikeClientIntegrationServiceConfigurations =
                builder.keyedDefaultAerospikeClientIntegrationServiceConfigurations;
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

    public DefaultAerospikeClientIntegrationServiceConfiguration
    getDefaultDefaultAerospikeClientIntegrationServiceConfiguration()
    {
        return defaultDefaultAerospikeClientIntegrationServiceConfiguration;
    }

    @NotNullConditionally
    public Set<DefaultAerospikeClientIntegrationServiceConfiguration>
    getKeyedDefaultAerospikeClientIntegrationServiceConfigurations()
    {
        return keyedDefaultAerospikeClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Aerospike client service.
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

        final DefaultAerospikeClientServiceConfiguration that = (DefaultAerospikeClientServiceConfiguration) o;
        return Objects.equals(
                        defaultDefaultAerospikeClientIntegrationServiceConfiguration,
                        that.defaultDefaultAerospikeClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultAerospikeClientIntegrationServiceConfigurations,
                        that.keyedDefaultAerospikeClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultAerospikeClientIntegrationServiceConfiguration,
                keyedDefaultAerospikeClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultAerospikeClientServiceConfiguration{" +
                "defaultDefaultAerospikeClientIntegrationServiceConfiguration=" +
                defaultDefaultAerospikeClientIntegrationServiceConfiguration +
                ", keyedDefaultAerospikeClientIntegrationServiceConfigurations=" +
                keyedDefaultAerospikeClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultAerospikeClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultAerospikeClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_AEROSPIKE_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultAerospikeClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();



        private DefaultAerospikeClientIntegrationServiceConfiguration
                defaultDefaultAerospikeClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_AEROSPIKE_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultAerospikeClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultAerospikeClientIntegrationServiceConfiguration.class)
        private Set<DefaultAerospikeClientIntegrationServiceConfiguration>
                keyedDefaultAerospikeClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultAerospikeClientIntegrationServiceConfiguration(
                final DefaultAerospikeClientIntegrationServiceConfiguration
                        defaultDefaultAerospikeClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultAerospikeClientIntegrationServiceConfiguration =
                    defaultDefaultAerospikeClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultAerospikeClientIntegrationServiceConfigurations(
                @NotNullConditionally
                @JsonSerialize(contentAs = DefaultAerospikeClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultAerospikeClientIntegrationServiceConfiguration.class)
                final Set<DefaultAerospikeClientIntegrationServiceConfiguration>
                        keyedDefaultAerospikeClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultAerospikeClientIntegrationServiceConfigurations =
                    keyedDefaultAerospikeClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Aerospike client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultAerospikeClientServiceConfiguration build() {
            return new DefaultAerospikeClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
