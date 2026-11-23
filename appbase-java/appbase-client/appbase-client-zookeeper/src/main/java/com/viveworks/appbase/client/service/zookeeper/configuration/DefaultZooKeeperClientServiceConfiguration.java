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

package com.viveworks.appbase.client.service.zookeeper.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.zookeeper.DefaultZooKeeperClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultZooKeeperClientServiceConfiguration.Builder.class)
public final class DefaultZooKeeperClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultZooKeeperClientServiceConfiguration.Builder>
        implements ZooKeeperClientServiceConfiguration
{
    /**
     * Configuration for {@code ZooKeeper} cluster returned by
     * {@link ZooKeeperClientService#getClientIntegrationService()}.
     */
    private final DefaultZooKeeperClientIntegrationServiceConfiguration
            defaultDefaultZooKeeperClientIntegrationServiceConfiguration;


    /**
     * Configurations of {@code ZooKeeper} cluster returned by
     * {@link ZooKeeperClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NullableConditionally
    @JsonSerialize(contentAs = DefaultZooKeeperClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultZooKeeperClientIntegrationServiceConfiguration.class)
    private final Set<DefaultZooKeeperClientIntegrationServiceConfiguration>
            keyedDefaultZooKeeperClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this ZooKeeper client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;


    private DefaultZooKeeperClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultZooKeeperClientIntegrationServiceConfiguration,
                "builder.defaultDefaultZooKeeperClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultZooKeeperClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultZooKeeperClientIntegrationServiceConfigurations");
        }


        defaultDefaultZooKeeperClientIntegrationServiceConfiguration =
                builder.defaultDefaultZooKeeperClientIntegrationServiceConfiguration;
        keyedDefaultZooKeeperClientIntegrationServiceConfigurations =
                builder.keyedDefaultZooKeeperClientIntegrationServiceConfigurations;
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

    public DefaultZooKeeperClientIntegrationServiceConfiguration
    getDefaultDefaultZooKeeperClientIntegrationServiceConfiguration()
    {
        return defaultDefaultZooKeeperClientIntegrationServiceConfiguration;
    }

    @NullableConditionally
    public Set<DefaultZooKeeperClientIntegrationServiceConfiguration>
    getKeyedDefaultZooKeeperClientIntegrationServiceConfigurations()
    {
        return keyedDefaultZooKeeperClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this ZooKeeper client service.
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

        final DefaultZooKeeperClientServiceConfiguration that = (DefaultZooKeeperClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration,
                that.defaultDefaultZooKeeperClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultZooKeeperClientIntegrationServiceConfigurations,
                        that.keyedDefaultZooKeeperClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration,
                keyedDefaultZooKeeperClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultZooKeeperClientServiceConfiguration{" +
                "defaultDefaultZooKeeperClientIntegrationServiceConfiguration=" +
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration +
                ", keyedDefaultZooKeeperClientIntegrationServiceConfigurations=" +
                keyedDefaultZooKeeperClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultZooKeeperClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultZooKeeperClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_ZOOKEEPER_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultZooKeeperClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();



        private DefaultZooKeeperClientIntegrationServiceConfiguration
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_ZOOKEEPER_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NullableConditionally
        @JsonSerialize(contentAs = DefaultZooKeeperClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultZooKeeperClientIntegrationServiceConfiguration.class)
        private Set<DefaultZooKeeperClientIntegrationServiceConfiguration>
                keyedDefaultZooKeeperClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultZooKeeperClientIntegrationServiceConfiguration(
                final DefaultZooKeeperClientIntegrationServiceConfiguration
                        defaultDefaultZooKeeperClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultZooKeeperClientIntegrationServiceConfiguration =
                    defaultDefaultZooKeeperClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultZooKeeperClientIntegrationServiceConfigurations(
                @NullableConditionally
                @JsonSerialize(contentAs = DefaultZooKeeperClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultZooKeeperClientIntegrationServiceConfiguration.class)
                final Set<DefaultZooKeeperClientIntegrationServiceConfiguration>
                        keyedDefaultZooKeeperClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultZooKeeperClientIntegrationServiceConfigurations =
                    keyedDefaultZooKeeperClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this ZooKeeper client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultZooKeeperClientServiceConfiguration build() {
            return new DefaultZooKeeperClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
