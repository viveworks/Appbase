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

package com.viveworks.appbase.client.service.cassandra.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of {@link CassandraClientServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultCassandraClientServiceConfiguration.Builder.class)
public final class DefaultCassandraClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultCassandraClientServiceConfiguration.Builder>
        implements CassandraClientServiceConfiguration
{
    /**
     * Configuration for
     * {@link com.viveworks.appbase.client.service.cassandra.CassandraClientService
     * #pooledClientIntegrationService(AppbaseClientKey)}.
     */
    private final DefaultCassandraClientIntegrationServiceConfiguration
            defaultDefaultCassandraClientIntegrationServiceConfiguration;


    /**
     * Configurations for
     * {@link com.viveworks.appbase.client.service.cassandra.CassandraClientService
     * #pooledClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultCassandraClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultCassandraClientIntegrationServiceConfiguration.class)
    private final Set<DefaultCassandraClientIntegrationServiceConfiguration>
            keyedDefaultCassandraClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Cassandra client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;



    public DefaultCassandraClientServiceConfiguration(
            final DefaultCassandraClientServiceConfiguration.Builder builder)
    {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultCassandraClientIntegrationServiceConfiguration,
                "builder.defaultDefaultCassandraClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultCassandraClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultCassandraClientIntegrationServiceConfigurations");
        }


        defaultDefaultCassandraClientIntegrationServiceConfiguration =
                builder.defaultDefaultCassandraClientIntegrationServiceConfiguration;
        keyedDefaultCassandraClientIntegrationServiceConfigurations =
                builder.keyedDefaultCassandraClientIntegrationServiceConfigurations;
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

    public DefaultCassandraClientIntegrationServiceConfiguration
    getDefaultDefaultCassandraClientIntegrationServiceConfiguration()
    {
        return defaultDefaultCassandraClientIntegrationServiceConfiguration;
    }

    @NotNullConditionally
    public Set<DefaultCassandraClientIntegrationServiceConfiguration>
    getKeyedDefaultCassandraClientIntegrationServiceConfigurations()
    {
        return keyedDefaultCassandraClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Cassandra client service.
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

        final DefaultCassandraClientServiceConfiguration that = (DefaultCassandraClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultCassandraClientIntegrationServiceConfiguration,
                that.defaultDefaultCassandraClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultCassandraClientIntegrationServiceConfigurations,
                        that.keyedDefaultCassandraClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultCassandraClientIntegrationServiceConfiguration,
                keyedDefaultCassandraClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultCassandraClientServiceConfiguration{" +
                "defaultDefaultCassandraClientIntegrationServiceConfiguration=" +
                defaultDefaultCassandraClientIntegrationServiceConfiguration +
                ", keyedDefaultCassandraClientIntegrationServiceConfigurations=" +
                keyedDefaultCassandraClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultCassandraClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static class Builder
            extends AbstractAppbaseClientServiceConfiguration.Builder<DefaultCassandraClientServiceConfiguration.Builder>
    {
        private static final DefaultCassandraClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_CASSANDRA_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultCassandraClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();



        private DefaultCassandraClientIntegrationServiceConfiguration
                defaultDefaultCassandraClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_CASSANDRA_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultCassandraClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultCassandraClientIntegrationServiceConfiguration.class)
        private Set<DefaultCassandraClientIntegrationServiceConfiguration>
                keyedDefaultCassandraClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultCassandraClientIntegrationServiceConfiguration(
                final DefaultCassandraClientIntegrationServiceConfiguration
                        defaultDefaultCassandraClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultCassandraClientIntegrationServiceConfiguration =
                    defaultDefaultCassandraClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultCassandraClientIntegrationServiceConfigurations(
                @NotNullConditionally
                @JsonSerialize(contentAs = DefaultCassandraClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultCassandraClientIntegrationServiceConfiguration.class)
                final Set<DefaultCassandraClientIntegrationServiceConfiguration>
                        keyedDefaultCassandraClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultCassandraClientIntegrationServiceConfigurations =
                    keyedDefaultCassandraClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Cassandra client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultCassandraClientServiceConfiguration build() {
            return new DefaultCassandraClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
