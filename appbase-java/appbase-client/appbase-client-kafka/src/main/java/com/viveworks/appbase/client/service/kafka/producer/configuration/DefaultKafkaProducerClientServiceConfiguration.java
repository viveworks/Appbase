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

package com.viveworks.appbase.client.service.kafka.producer.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.DefaultKafkaProducerClientService;
import com.viveworks.appbase.client.service.kafka.producer.KafkaProducerClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultKafkaProducerClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultKafkaProducerClientServiceConfiguration.Builder.class)
public final class DefaultKafkaProducerClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultKafkaProducerClientServiceConfiguration.Builder>
        implements KafkaProducerClientServiceConfiguration
{
    /**
     * Configuration for {@link KafkaProducerClientService#getClientIntegrationService()}.
     */
    private final DefaultKafkaProducerClientIntegrationServiceConfiguration
            defaultDefaultKafkaProducerClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link KafkaProducerClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
    private final Set<DefaultKafkaProducerClientIntegrationServiceConfiguration>
            keyedDefaultKafkaProducerClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Kafka producer client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;


    private DefaultKafkaProducerClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultKafkaProducerClientIntegrationServiceConfiguration,
                "builder.defaultDefaultKafkaProducerClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultKafkaProducerClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultKafkaProducerClientIntegrationServiceConfigurations");
        }


        defaultDefaultKafkaProducerClientIntegrationServiceConfiguration =
                builder.defaultDefaultKafkaProducerClientIntegrationServiceConfiguration;
        keyedDefaultKafkaProducerClientIntegrationServiceConfigurations =
                builder.keyedDefaultKafkaProducerClientIntegrationServiceConfigurations;
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

    public DefaultKafkaProducerClientIntegrationServiceConfiguration
    getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration()
    {
        return defaultDefaultKafkaProducerClientIntegrationServiceConfiguration;
    }

    public Set<DefaultKafkaProducerClientIntegrationServiceConfiguration>
    getKeyedDefaultKafkaProducerClientIntegrationServiceConfigurations()
    {
        return keyedDefaultKafkaProducerClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Kafka producer client service.
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

        final DefaultKafkaProducerClientServiceConfiguration that = (DefaultKafkaProducerClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration,
                that.defaultDefaultKafkaProducerClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultKafkaProducerClientIntegrationServiceConfigurations,
                        that.keyedDefaultKafkaProducerClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration,
                keyedDefaultKafkaProducerClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultKafkaProducerClientServiceConfiguration{" +
                "defaultDefaultKafkaProducerClientIntegrationServiceConfiguration=" +
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration +
                ", keyedDefaultKafkaProducerClientIntegrationServiceConfigurations=" +
                keyedDefaultKafkaProducerClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultKafkaProducerClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultKafkaProducerClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_KAFKA_PRODUCER_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultKafkaProducerClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultKafkaProducerClientIntegrationServiceConfiguration
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_KAFKA_PRODUCER_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
        private Set<DefaultKafkaProducerClientIntegrationServiceConfiguration>
                keyedDefaultKafkaProducerClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultKafkaProducerClientIntegrationServiceConfiguration(
                final DefaultKafkaProducerClientIntegrationServiceConfiguration
                        defaultDefaultKafkaProducerClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultKafkaProducerClientIntegrationServiceConfiguration =
                    defaultDefaultKafkaProducerClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultKafkaProducerClientIntegrationServiceConfigurations(
                @NotNullConditionally
                @JsonSerialize(contentAs = DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultKafkaProducerClientIntegrationServiceConfiguration.class)
                final Set<DefaultKafkaProducerClientIntegrationServiceConfiguration>
                        keyedDefaultKafkaProducerClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultKafkaProducerClientIntegrationServiceConfigurations =
                    keyedDefaultKafkaProducerClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Kafka producer client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultKafkaProducerClientServiceConfiguration build() {
            return new DefaultKafkaProducerClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
