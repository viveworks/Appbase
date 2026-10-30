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

package com.viveworks.appbase.client.cloud.aws.httpclient.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkAsyncHttpClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultAwsSdkAsyncHttpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAwsSdkAsyncHttpClientServiceConfiguration.Builder.class)
public final class DefaultAwsSdkAsyncHttpClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultAwsSdkAsyncHttpClientServiceConfiguration.Builder>
        implements AwsSdkAsyncHttpClientServiceConfiguration
{
    /**
     * Configuration for {@link AwsSdkAsyncHttpClientService#getClientIntegrationService()}.
     */
    private final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
            defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;


    /**
     * Configuration for {@link AwsSdkAsyncHttpClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.class)
    private final Set<DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration>
            keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations;



    private DefaultAwsSdkAsyncHttpClientServiceConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                "builder.defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration");

        if (builder.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            AppbaseUtil.notEmpty(
                    builder.keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations,
                    "builder.keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations");
        }


        defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                builder.defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
        keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations =
                builder.keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
    getDefaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration()
    {
        return defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
    }

    @NotNullConditionally
    public Set<DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration>
    getKeyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations()
    {
        return keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations;
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

        final DefaultAwsSdkAsyncHttpClientServiceConfiguration that = (DefaultAwsSdkAsyncHttpClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                that.defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations,
                        that.keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultAwsSdkAsyncHttpClientServiceConfiguration{" +
                "defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration=" +
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration +
                ", keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations=" +
                keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultAwsSdkAsyncHttpClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_DEFAULT_AWS_SDK_ASYNC_HTTP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.builder().build();



        private DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_DEFAULT_AWS_SDK_ASYNC_HTTP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.class)
        private Set<DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration>
                keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration(
                final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                        defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                    defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations(
                final Set<DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration>
                        keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations =
                    keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultAwsSdkAsyncHttpClientServiceConfiguration build() {
            return new DefaultAwsSdkAsyncHttpClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
