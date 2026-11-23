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

package com.viveworks.appbase.client.service.spark.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.service.spark.SparkClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultSparkClientServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultSparkClientServiceConfiguration.Builder.class)
public final class DefaultSparkClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultSparkClientServiceConfiguration.Builder>
        implements SparkClientServiceConfiguration
{
    /**
     * Configuration for {@link SparkClientService#getClientIntegrationService()}.
     */
    private final DefaultSparkClientIntegrationServiceConfiguration defaultDefaultSparkClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link SparkClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @JsonSerialize(contentAs = DefaultSparkClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultSparkClientIntegrationServiceConfiguration.class)
    private final Set<DefaultSparkClientIntegrationServiceConfiguration>
            keyedDefaultSparkClientIntegrationServiceConfigurations;

    private DefaultSparkClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultSparkClientIntegrationServiceConfiguration,
                "builder.defaultDefaultSparkClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultSparkClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultSparkClientIntegrationServiceConfigurations");
        }

        defaultDefaultSparkClientIntegrationServiceConfiguration =
                builder.defaultDefaultSparkClientIntegrationServiceConfiguration;
        keyedDefaultSparkClientIntegrationServiceConfigurations =
                builder.keyedDefaultSparkClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultSparkClientIntegrationServiceConfiguration getDefaultDefaultSparkClientIntegrationServiceConfiguration() {
        return defaultDefaultSparkClientIntegrationServiceConfiguration;
    }

    public Set<DefaultSparkClientIntegrationServiceConfiguration>
    getKeyedDefaultSparkClientIntegrationServiceConfigurations()
    {
        return keyedDefaultSparkClientIntegrationServiceConfigurations;
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

        final DefaultSparkClientServiceConfiguration that = (DefaultSparkClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultSparkClientIntegrationServiceConfiguration,
                that.defaultDefaultSparkClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultSparkClientIntegrationServiceConfigurations,
                        that.keyedDefaultSparkClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultSparkClientIntegrationServiceConfiguration,
                keyedDefaultSparkClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultSparkClientServiceConfiguration{" +
                "defaultDefaultSparkClientIntegrationServiceConfiguration=" +
                defaultDefaultSparkClientIntegrationServiceConfiguration +
                ", keyedDefaultSparkClientIntegrationServiceConfigurations=" +
                keyedDefaultSparkClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultSparkClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultSparkClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_SPARK_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultSparkClientIntegrationServiceConfiguration.builder().build();


        private DefaultSparkClientIntegrationServiceConfiguration defaultDefaultSparkClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_SPARK_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @JsonSerialize(contentAs = DefaultSparkClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultSparkClientIntegrationServiceConfiguration.class)
        private Set<DefaultSparkClientIntegrationServiceConfiguration>
                keyedDefaultSparkClientIntegrationServiceConfigurations;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultSparkClientIntegrationServiceConfiguration(
                final DefaultSparkClientIntegrationServiceConfiguration
                        defaultDefaultSparkClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultSparkClientIntegrationServiceConfiguration =
                    defaultDefaultSparkClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultSparkClientIntegrationServiceConfigurations(
                @JsonSerialize(contentAs = DefaultSparkClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultSparkClientIntegrationServiceConfiguration.class)
                final Set<DefaultSparkClientIntegrationServiceConfiguration>
                        keyedDefaultSparkClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultSparkClientIntegrationServiceConfigurations =
                    keyedDefaultSparkClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultSparkClientServiceConfiguration build() {
            return new DefaultSparkClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
