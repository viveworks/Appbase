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

package com.viveworks.appbase.client.cloud.aws.s3.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.DefaultS3ClientsService;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultS3ClientsService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultS3ClientsServiceConfiguration.Builder.class)
public final class DefaultS3ClientsServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultS3ClientsServiceConfiguration.Builder>
        implements S3ClientsServiceConfiguration
{
    /**
     * Configuration for {@link S3ClientsService#getClientIntegrationService()}.
     */
    private final DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientsIntegrationServiceConfiguration;


    /**
     * Configuration for {@link S3ClientsService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultS3ClientsIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultS3ClientsIntegrationServiceConfiguration.class)
    private final Set<DefaultS3ClientsIntegrationServiceConfiguration> keyedDefaultS3ClientsIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this S3 client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;


    private DefaultS3ClientsServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultS3ClientsIntegrationServiceConfiguration,
                "builder.defaultDefaultS3ClientsIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultS3ClientsIntegrationServiceConfigurations,
                    "builder.keyedDefaultS3ClientsIntegrationServiceConfigurations");
        }

        defaultDefaultS3ClientsIntegrationServiceConfiguration =
                builder.defaultDefaultS3ClientsIntegrationServiceConfiguration;
        keyedDefaultS3ClientsIntegrationServiceConfigurations =
                builder.keyedDefaultS3ClientsIntegrationServiceConfigurations;
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
     * Returns the configured default default s3 client integration service configuration.
     *
     * @return the configured default default s3 client integration service configuration
     */
    public DefaultS3ClientsIntegrationServiceConfiguration getDefaultDefaultS3ClientIntegrationServiceConfiguration() {
        return defaultDefaultS3ClientsIntegrationServiceConfiguration;
    }

    /**
     * Returns the configured keyed default s3 client integration service configurations.
     *
     * @return the configured keyed default s3 client integration service configurations
     */
    public Set<DefaultS3ClientsIntegrationServiceConfiguration> getKeyedDefaultS3ClientIntegrationServiceConfigurations() {
        return keyedDefaultS3ClientsIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this S3 client service.
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

        final DefaultS3ClientsServiceConfiguration that = (DefaultS3ClientsServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultS3ClientsIntegrationServiceConfiguration,
                that.defaultDefaultS3ClientsIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultS3ClientsIntegrationServiceConfigurations,
                        that.keyedDefaultS3ClientsIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultS3ClientsIntegrationServiceConfiguration,
                keyedDefaultS3ClientsIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultS3ClientsServiceConfiguration{" +
                "defaultDefaultS3ClientsIntegrationServiceConfiguration=" +
                defaultDefaultS3ClientsIntegrationServiceConfiguration +
                ", keyedDefaultS3ClientsIntegrationServiceConfigurations=" +
                keyedDefaultS3ClientsIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultS3ClientsServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultS3ClientsIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_S3_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultS3ClientsIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();


        private DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientsIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_S3_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultS3ClientsIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultS3ClientsIntegrationServiceConfiguration.class)
        private Set<DefaultS3ClientsIntegrationServiceConfiguration> keyedDefaultS3ClientsIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultS3ClientIntegrationServiceConfiguration(
                final DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientsIntegrationServiceConfiguration)
        {
            this.defaultDefaultS3ClientsIntegrationServiceConfiguration =
                    defaultDefaultS3ClientsIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultS3ClientIntegrationServiceConfigurations(
                final Set<DefaultS3ClientsIntegrationServiceConfiguration>
                        keyedDefaultS3ClientsIntegrationServiceConfigurations)
        {
            this.keyedDefaultS3ClientsIntegrationServiceConfigurations =
                    keyedDefaultS3ClientsIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this S3 client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultS3ClientsServiceConfiguration build() {
            return new DefaultS3ClientsServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
