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

package com.viveworks.appbase.client.service.redis.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.redis.DefaultRedisClientService;
import com.viveworks.appbase.client.service.redis.RedisClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultRedisClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultRedisClientServiceConfiguration.Builder.class)
public final class DefaultRedisClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultRedisClientServiceConfiguration.Builder>
        implements RedisClientServiceConfiguration
{
    /**
     * Configuration for {@link RedisClientService#getClientIntegrationService()}.
     */
    private final DefaultRedisClientIntegrationServiceConfiguration
            defaultDefaultRedisClientIntegrationServiceConfiguration;

    /**
     * Configuration for {@link RedisClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultRedisClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultRedisClientIntegrationServiceConfiguration.class)
    private final Set<DefaultRedisClientIntegrationServiceConfiguration>
            keyedDefaultRedisClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Redis client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;


    private DefaultRedisClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultRedisClientIntegrationServiceConfiguration,
                "builder.defaultDefaultRedisClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultRedisClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultRedisClientIntegrationServiceConfigurations");
        }

        defaultDefaultRedisClientIntegrationServiceConfiguration =
                builder.defaultDefaultRedisClientIntegrationServiceConfiguration;
        keyedDefaultRedisClientIntegrationServiceConfigurations =
                builder.keyedDefaultRedisClientIntegrationServiceConfigurations;
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

    /**
     * Returns the primary (default) Redis integration service configuration.
     *
     * @return default integration service configuration
     */
    public DefaultRedisClientIntegrationServiceConfiguration
    getDefaultDefaultRedisClientIntegrationServiceConfiguration()
    {
        return defaultDefaultRedisClientIntegrationServiceConfiguration;
    }

    /**
     * Returns the set of keyed Redis integration service configurations, or {@code null}.
     *
     * @return keyed integration service configurations
     */
    public Set<DefaultRedisClientIntegrationServiceConfiguration>
    getKeyedDefaultRedisClientIntegrationServiceConfigurations()
    {
        return keyedDefaultRedisClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Redis client service.
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

        final DefaultRedisClientServiceConfiguration that = (DefaultRedisClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultRedisClientIntegrationServiceConfiguration,
                that.defaultDefaultRedisClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultRedisClientIntegrationServiceConfigurations,
                        that.keyedDefaultRedisClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultRedisClientIntegrationServiceConfiguration,
                keyedDefaultRedisClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultRedisClientServiceConfiguration{" +
                "defaultDefaultRedisClientIntegrationServiceConfiguration=" +
                defaultDefaultRedisClientIntegrationServiceConfiguration +
                ", keyedDefaultRedisClientIntegrationServiceConfigurations=" +
                keyedDefaultRedisClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultRedisClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultRedisClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_REDIS_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultRedisClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultRedisClientIntegrationServiceConfiguration
                defaultDefaultRedisClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_REDIS_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultRedisClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultRedisClientIntegrationServiceConfiguration.class)
        private Set<DefaultRedisClientIntegrationServiceConfiguration>
                keyedDefaultRedisClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        /**
         * Set the primary Redis integration service configuration.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder defaultDefaultRedisClientIntegrationServiceConfiguration(
                final DefaultRedisClientIntegrationServiceConfiguration v)
        {
            this.defaultDefaultRedisClientIntegrationServiceConfiguration = v;
            return this;
        }

        /**
         * Set additional keyed Redis integration service configurations.
         *
         * @param v set of keyed configurations
         * @return this builder
         */
        public Builder keyedDefaultRedisClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultRedisClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultRedisClientIntegrationServiceConfiguration.class)
                final Set<DefaultRedisClientIntegrationServiceConfiguration> v)
        {
            this.keyedDefaultRedisClientIntegrationServiceConfigurations = v;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Redis client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        /**
         * Build the configuration.
         *
         * @return new {@link DefaultRedisClientServiceConfiguration}
         */
        public DefaultRedisClientServiceConfiguration build() {
            return new DefaultRedisClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
