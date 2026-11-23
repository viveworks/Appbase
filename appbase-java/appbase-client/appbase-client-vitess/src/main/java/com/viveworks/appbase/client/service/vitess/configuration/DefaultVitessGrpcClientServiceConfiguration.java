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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.vitess.DefaultVitessGrpcClientService;
import com.viveworks.appbase.client.service.vitess.VitessGrpcClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultVitessGrpcClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultVitessGrpcClientServiceConfiguration.Builder.class)
public final class DefaultVitessGrpcClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultVitessGrpcClientServiceConfiguration.Builder>
        implements VitessGrpcClientServiceConfiguration
{
    /**
     * Configuration for {@link VitessGrpcClientService#getClientIntegrationService()}.
     */
    private final DefaultVitessGrpcClientIntegrationServiceConfiguration
            defaultDefaultVitessGrpcClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link VitessGrpcClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultVitessGrpcClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultVitessGrpcClientIntegrationServiceConfiguration.class)
    private final Set<DefaultVitessGrpcClientIntegrationServiceConfiguration>
            keyedDefaultVitessGrpcClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Vitess gRPC client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;

    private DefaultVitessGrpcClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultVitessGrpcClientIntegrationServiceConfiguration,
                "builder.defaultDefaultVitessGrpcClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultVitessGrpcClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultVitessGrpcClientIntegrationServiceConfigurations");
        }

        defaultDefaultVitessGrpcClientIntegrationServiceConfiguration =
                builder.defaultDefaultVitessGrpcClientIntegrationServiceConfiguration;
        keyedDefaultVitessGrpcClientIntegrationServiceConfigurations =
                builder.keyedDefaultVitessGrpcClientIntegrationServiceConfigurations;
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

    public DefaultVitessGrpcClientIntegrationServiceConfiguration
    getDefaultDefaultVitessGrpcClientIntegrationServiceConfiguration()
    {
        return defaultDefaultVitessGrpcClientIntegrationServiceConfiguration;
    }

    public Set<DefaultVitessGrpcClientIntegrationServiceConfiguration>
    getKeyedDefaultVitessGrpcClientIntegrationServiceConfigurations()
    {
        return keyedDefaultVitessGrpcClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Vitess gRPC client service.
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

        final DefaultVitessGrpcClientServiceConfiguration that = (DefaultVitessGrpcClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration,
                that.defaultDefaultVitessGrpcClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultVitessGrpcClientIntegrationServiceConfigurations,
                        that.keyedDefaultVitessGrpcClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration,
                keyedDefaultVitessGrpcClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultVitessGrpcClientServiceConfiguration{" +
                "defaultDefaultVitessGrpcClientIntegrationServiceConfiguration=" +
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration +
                ", keyedDefaultVitessGrpcClientIntegrationServiceConfigurations=" +
                keyedDefaultVitessGrpcClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultVitessGrpcClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultVitessGrpcClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_VITESS_GRPC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultVitessGrpcClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultVitessGrpcClientIntegrationServiceConfiguration
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_VITESS_GRPC_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultVitessGrpcClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultVitessGrpcClientIntegrationServiceConfiguration.class)
        private Set<DefaultVitessGrpcClientIntegrationServiceConfiguration>
                keyedDefaultVitessGrpcClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultVitessGrpcClientIntegrationServiceConfiguration(
                final DefaultVitessGrpcClientIntegrationServiceConfiguration
                        defaultDefaultVitessGrpcClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultVitessGrpcClientIntegrationServiceConfiguration =
                    defaultDefaultVitessGrpcClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultVitessGrpcClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultVitessGrpcClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultVitessGrpcClientIntegrationServiceConfiguration.class)
                final Set<DefaultVitessGrpcClientIntegrationServiceConfiguration>
                        keyedDefaultVitessGrpcClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultVitessGrpcClientIntegrationServiceConfigurations =
                    keyedDefaultVitessGrpcClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Vitess gRPC client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultVitessGrpcClientServiceConfiguration build() {
            return new DefaultVitessGrpcClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
