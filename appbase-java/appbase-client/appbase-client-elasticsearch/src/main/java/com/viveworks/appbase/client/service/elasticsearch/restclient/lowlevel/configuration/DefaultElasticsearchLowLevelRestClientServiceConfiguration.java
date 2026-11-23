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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.ElasticsearchLowLevelRestClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for
 * {@link DefaultElasticsearchLowLevelRestClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultElasticsearchLowLevelRestClientServiceConfiguration.Builder.class)
public final class DefaultElasticsearchLowLevelRestClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultElasticsearchLowLevelRestClientServiceConfiguration.Builder>
        implements ElasticsearchLowLevelRestClientServiceConfiguration
{
    /**
     * Configuration for {@link ElasticsearchLowLevelRestClientService#getClientIntegrationService()}.
     */
    private final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
            defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link ElasticsearchLowLevelRestClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NullableConditionally
    @JsonSerialize(contentAs = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
    private final Set<DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration>
            keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations;

    /**
     * Configuration for the reactive accessor exposed by this Elasticsearch low-level client service.
     */
    private final DefaultReactiveOperationConfiguration reactiveOperationConfiguration;



    private DefaultElasticsearchLowLevelRestClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "builder.defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.reactiveOperationConfiguration,
                "builder.reactiveOperationConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations");
        }


        defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                builder.defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
        keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations =
                builder.keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations;
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

    public DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
    getDefaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration()
    {
        return defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
    }

    @NullableConditionally
    public Set<DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration>
    getKeyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations()
    {
        return keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations;
    }

    /**
     * Returns the reactive accessor configuration used by this Elasticsearch low-level client service.
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

        final DefaultElasticsearchLowLevelRestClientServiceConfiguration that =
                (DefaultElasticsearchLowLevelRestClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                that.defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations,
                        that.keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations) &&
                Objects.equals(reactiveOperationConfiguration, that.reactiveOperationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations,
                reactiveOperationConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultElasticsearchLowLevelRestClientServiceConfiguration{" +
                "defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration=" +
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration +
                ", keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations=" +
                keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations +
                ", reactiveOperationConfiguration=" + reactiveOperationConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultElasticsearchLowLevelRestClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_ELASTICSEARCH_REST_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.builder().build();

        private static final DefaultReactiveOperationConfiguration DEFAULT_REACTIVE_OPERATION_CONFIGURATION =
                DefaultReactiveOperationConfiguration.builder().build();



        private DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_ELASTICSEARCH_REST_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NullableConditionally
        @JsonSerialize(contentAs = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
        private Set<DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration>
                keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations;

        private DefaultReactiveOperationConfiguration reactiveOperationConfiguration =
                DEFAULT_REACTIVE_OPERATION_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration(
                final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                        defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                    defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations(
                @NullableConditionally
                @JsonSerialize(contentAs = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
                @JsonDeserialize(contentAs = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class)
                final Set<DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration>
                        keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations =
                    keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations;
            return this;
        }

        /**
         * Set the reactive accessor configuration for this Elasticsearch low-level client service.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder reactiveOperationConfiguration(final DefaultReactiveOperationConfiguration v) {
            this.reactiveOperationConfiguration = v;
            return this;
        }

        public DefaultElasticsearchLowLevelRestClientServiceConfiguration build() {
            return new DefaultElasticsearchLowLevelRestClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
