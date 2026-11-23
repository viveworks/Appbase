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
package com.viveworks.appbase.client.cloud.aws.httpclient.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AbstractAppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkHttpClientService;
import com.viveworks.appbase.core.util.NotNullConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkHttpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAwsSdkHttpClientServiceConfiguration.Builder.class)
public class DefaultAwsSdkHttpClientServiceConfiguration
        extends AbstractAppbaseClientServiceConfiguration<DefaultAwsSdkHttpClientServiceConfiguration.Builder>
        implements AwsSdkHttpClientServiceConfiguration
{
    /**
     * Configuration for {@link AwsSdkHttpClientService#getClientIntegrationService()}.
     */
    private final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
            defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration;

    /**
     * Configuration for {@link AwsSdkHttpClientService#getClientIntegrationService(AppbaseClientKey)}.
     */
    @NotNullConditionally
    @JsonSerialize(contentAs = DefaultAwsSdkHttpClientIntegrationServiceConfiguration.class)
    @JsonDeserialize(contentAs = DefaultAwsSdkHttpClientIntegrationServiceConfiguration.class)
    private final Set<DefaultAwsSdkHttpClientIntegrationServiceConfiguration>
            keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations;

    private DefaultAwsSdkHttpClientServiceConfiguration(Builder builder) {
        super(builder);

        defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration =
                builder.defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration;
        keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations =
                builder.keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DefaultAwsSdkHttpClientIntegrationServiceConfiguration
    getDefaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration()
    {
        return defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration;
    }

    public Set<DefaultAwsSdkHttpClientIntegrationServiceConfiguration>
    getKeyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations()
    {
        return keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations;
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

        final DefaultAwsSdkHttpClientServiceConfiguration that = (DefaultAwsSdkHttpClientServiceConfiguration) o;
        return Objects.equals(
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration,
                that.defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration) &&
                Objects.equals(
                        keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations,
                        that.keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration,
                keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultAwsSdkHttpClientServiceConfiguration{" +
                "defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration=" + defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration +
                ", keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations=" + keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations +
                "} " + super.toString();
    }

    /**
     * Builder for {@link DefaultAwsSdkHttpClientServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseClientServiceConfiguration.Builder<Builder> {
        private static final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                DEFAULT_DEFAULT_AWS_SDK_HTTP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION =
                DefaultAwsSdkHttpClientIntegrationServiceConfiguration.builder().build();

        private DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration =
                DEFAULT_DEFAULT_AWS_SDK_HTTP_CLIENT_INTEGRATION_SERVICE_CONFIGURATION;

        @NotNullConditionally
        @JsonSerialize(contentAs = DefaultAwsSdkHttpClientIntegrationServiceConfiguration.class)
        @JsonDeserialize(contentAs = DefaultAwsSdkHttpClientIntegrationServiceConfiguration.class)
        private Set<DefaultAwsSdkHttpClientIntegrationServiceConfiguration>
                keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations;

        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration(
                final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                        defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration)
        {
            this.defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration =
                    defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration;
            return this;
        }

        public Builder keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations(
                final Set<DefaultAwsSdkHttpClientIntegrationServiceConfiguration>
                        keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations)
        {
            this.keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations =
                    keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations;
            return this;
        }

        public DefaultAwsSdkHttpClientServiceConfiguration build() {
            return new DefaultAwsSdkHttpClientServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
