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

package com.viveworks.appbase.client.service.kafka.consumer.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.DefaultKafkaConsumerClientService;
import com.viveworks.appbase.client.service.kafka.consumer.KafkaConsumerClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultKafkaConsumerClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultKafkaConsumerClientServiceConfiguration.Builder.class)
public final class DefaultKafkaConsumerClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultKafkaConsumerClientServiceConfiguration.Builder>
        implements KafkaConsumerClientServiceConfiguration
{
    /**
     * Configuration for {@link KafkaConsumerClientService#getClientIntegrationService()}.
     */
    private final DefaultKafkaConsumerClientIntegrationServiceConfiguration
            defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link KafkaConsumerClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
    private final Set<DefaultKafkaConsumerClientIntegrationServiceConfiguration>
            keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Kafka consumer client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;


    private DefaultKafkaConsumerClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration,
                "builder.defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations");
        }


        defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration =
                builder.defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration;
        keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations =
                builder.keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations;
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

    public DefaultKafkaConsumerClientIntegrationServiceConfiguration
    getDefaultDefaultKafkaConsumerClientIntegrationServiceConfiguration()
    {
        return defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration;
    }

    public Set<DefaultKafkaConsumerClientIntegrationServiceConfiguration>
    getKeyedDefaultKafkaConsumerClientIntegrationServiceConfigurations()
    {
        return keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Kafka consumer client service.
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

        final DefaultKafkaConsumerClientServiceConfiguration that = (DefaultKafkaConsumerClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration,
                that.defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations,
                        that.keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration,
                keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultKafkaConsumerClientServiceConfiguration{" +
                "defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration=" +
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration +
                ", keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations=" +
                keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultKafkaConsumerClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private DefaultKafkaConsumerClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_KAFKA_CONSUMER_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultKafkaConsumerClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultKafkaConsumerClientIntegrationServiceConfiguration
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_KAFKA_CONSUMER_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
        private Set<DefaultKafkaConsumerClientIntegrationServiceConfiguration>
                keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration(
                final DefaultKafkaConsumerClientIntegrationServiceConfiguration
                        defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration =
                    defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations(
                @NotNullConditionally
                @JsonSerialize(contentAs = DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultKafkaConsumerClientIntegrationServiceConfiguration.class)
                final Set<DefaultKafkaConsumerClientIntegrationServiceConfiguration>
                        keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations =
                    keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Kafka consumer client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultKafkaConsumerClientServiceConfiguration build() {
            return new DefaultKafkaConsumerClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
