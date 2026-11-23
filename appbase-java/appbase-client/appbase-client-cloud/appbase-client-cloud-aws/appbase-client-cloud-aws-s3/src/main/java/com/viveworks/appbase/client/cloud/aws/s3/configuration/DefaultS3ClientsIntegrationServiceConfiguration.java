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

package com.viveworks.appbase.client.cloud.aws.s3.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkClientAsyncConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkClientOverrideConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkCredentialsProviderConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.AwsSdkRegionConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.DefaultS3ClientsIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultS3ClientsIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultS3ClientsIntegrationServiceConfiguration.Builder.class)
public final class DefaultS3ClientsIntegrationServiceConfiguration implements S3ClientsIntegrationServiceConfiguration {
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;
    private final DefaultAwsSdkAsyncHttpClientServiceConfiguration
            customDefaultAwsSdkAsyncHttpClientServiceConfiguration;
    private final TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration;
    private final DefaultAwsSdkHttpClientServiceConfiguration
            customDefaultAwsSdkHttpClientServiceConfiguration;
    private final TextValueConfiguration awsSdkHttpClientIntegrationServiceKeyConfiguration;
    private final AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration;
    private final AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration;
    private final AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration;
    private final AwsSdkS3Configuration awsSdkS3Configuration;
    private final String endpointOverrideUri;
    private final AwsSdkRegionConfiguration awsSdkRegionConfiguration;


    private DefaultS3ClientsIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.awsSdkS3Configuration, "builder.awsSdkS3Configuration");
        AppbaseUtil.notNull(builder.awsSdkRegionConfiguration, "builder.awsSdkRegionConfiguration");

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        customDefaultAwsSdkAsyncHttpClientServiceConfiguration =
                builder.customDefaultAwsSdkAsyncHttpClientServiceConfiguration;
        awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration =
                builder.awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration;
        customDefaultAwsSdkHttpClientServiceConfiguration =
                builder.customDefaultAwsSdkHttpClientServiceConfiguration;
        awsSdkHttpClientIntegrationServiceKeyConfiguration =
                builder.awsSdkHttpClientIntegrationServiceKeyConfiguration;
        awsSdkClientOverrideConfiguration = builder.awsSdkClientOverrideConfiguration;
        awsSdkClientAsyncConfiguration = builder.awsSdkClientAsyncConfiguration;
        awsSdkCredentialsProviderConfiguration = builder.awsSdkCredentialsProviderConfiguration;
        awsSdkS3Configuration = builder.awsSdkS3Configuration;
        endpointOverrideUri = builder.endpointOverrideUri;
        awsSdkRegionConfiguration = builder.awsSdkRegionConfiguration;
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
     * Returns the configured cluster id value key configuration.
     *
     * @return the configured cluster id value key configuration
     */
    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    /**
     * Returns the configured custom default aws sdk async http client service configuration.
     *
     * @return the configured custom default aws sdk async http client service configuration
     */
    public DefaultAwsSdkAsyncHttpClientServiceConfiguration getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration() {
        return customDefaultAwsSdkAsyncHttpClientServiceConfiguration;
    }

    /**
     * Returns the configured aws sdk async http client integration service key configuration.
     *
     * @return the configured aws sdk async http client integration service key configuration
     */
    public TextValueConfiguration getAwsSdkAsyncHttpClientIntegrationServiceKeyConfiguration() {
        return awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration;
    }

    /**
     * Returns the configured custom default aws sdk http client service configuration.
     *
     * @return the configured custom default aws sdk http client service configuration
     */
    public DefaultAwsSdkHttpClientServiceConfiguration getCustomDefaultAwsSdkHttpClientServiceConfiguration() {
        return customDefaultAwsSdkHttpClientServiceConfiguration;
    }

    /**
     * Returns the configured aws sdk http client integration service key configuration.
     *
     * @return the configured aws sdk http client integration service key configuration
     */
    public TextValueConfiguration getAwsSdkHttpClientIntegrationServiceKeyConfiguration() {
        return awsSdkHttpClientIntegrationServiceKeyConfiguration;
    }

    /**
     * Returns the configured aws sdk client override configuration.
     *
     * @return the configured aws sdk client override configuration
     */
    public AwsSdkClientOverrideConfiguration getAwsSdkClientOverrideConfiguration() {
        return awsSdkClientOverrideConfiguration;
    }

    /**
     * Returns the configured aws sdk client async configuration.
     *
     * @return the configured aws sdk client async configuration
     */
    public AwsSdkClientAsyncConfiguration getAwsSdkClientAsyncConfiguration() {
        return awsSdkClientAsyncConfiguration;
    }

    /**
     * Returns the configured aws sdk credentials provider configuration.
     *
     * @return the configured aws sdk credentials provider configuration
     */
    public AwsSdkCredentialsProviderConfiguration getAwsSdkCredentialsProviderConfiguration() {
        return awsSdkCredentialsProviderConfiguration;
    }

    /**
     * Returns the configured aws sdk s3 configuration.
     *
     * @return the configured aws sdk s3 configuration
     */
    public AwsSdkS3Configuration getAwsSdkS3Configuration() {
        return awsSdkS3Configuration;
    }

    /**
     * Returns the configured endpoint override uri.
     *
     * @return the configured endpoint override uri
     */
    public String getEndpointOverrideUri() {
        return endpointOverrideUri;
    }

    /**
     * Returns the configured aws sdk region configuration.
     *
     * @return the configured aws sdk region configuration
     */
    public AwsSdkRegionConfiguration getAwsSdkRegionConfiguration() {
        return awsSdkRegionConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultS3ClientsIntegrationServiceConfiguration that = (DefaultS3ClientsIntegrationServiceConfiguration) o;
        return Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(
                        customDefaultAwsSdkAsyncHttpClientServiceConfiguration,
                        that.customDefaultAwsSdkAsyncHttpClientServiceConfiguration) &&
                Objects.equals(
                        awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration,
                        that.awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration) &&
                Objects.equals(
                        customDefaultAwsSdkHttpClientServiceConfiguration,
                        that.customDefaultAwsSdkHttpClientServiceConfiguration) &&
                Objects.equals(
                        awsSdkHttpClientIntegrationServiceKeyConfiguration,
                        that.awsSdkHttpClientIntegrationServiceKeyConfiguration) &&
                Objects.equals(awsSdkClientOverrideConfiguration, that.awsSdkClientOverrideConfiguration) &&
                Objects.equals(awsSdkClientAsyncConfiguration, that.awsSdkClientAsyncConfiguration) &&
                Objects.equals(awsSdkCredentialsProviderConfiguration, that.awsSdkCredentialsProviderConfiguration) &&
                Objects.equals(awsSdkS3Configuration, that.awsSdkS3Configuration) &&
                Objects.equals(endpointOverrideUri, that.endpointOverrideUri) &&
                Objects.equals(awsSdkRegionConfiguration, that.awsSdkRegionConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueKeyConfiguration,
                customDefaultAwsSdkAsyncHttpClientServiceConfiguration,
                awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration,
                customDefaultAwsSdkHttpClientServiceConfiguration,
                awsSdkHttpClientIntegrationServiceKeyConfiguration,
                awsSdkClientOverrideConfiguration,
                awsSdkClientAsyncConfiguration,
                awsSdkCredentialsProviderConfiguration,
                awsSdkS3Configuration,
                endpointOverrideUri,
                awsSdkRegionConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultS3ClientsIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", customDefaultAwsSdkAsyncHttpClientServiceConfiguration=" +
                customDefaultAwsSdkAsyncHttpClientServiceConfiguration +
                ", awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration=" +
                awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration +
                ", customDefaultAwsSdkHttpClientServiceConfiguration=" +
                customDefaultAwsSdkHttpClientServiceConfiguration +
                ", awsSdkHttpClientIntegrationServiceKeyConfiguration=" +
                awsSdkHttpClientIntegrationServiceKeyConfiguration +
                ", awsSdkClientOverrideConfiguration=" + awsSdkClientOverrideConfiguration +
                ", awsSdkClientAsyncConfiguration=" + awsSdkClientAsyncConfiguration +
                ", awsSdkCredentialsProviderConfiguration=" + awsSdkCredentialsProviderConfiguration +
                ", awsSdkS3Configuration=" + awsSdkS3Configuration +
                ", endpointOverrideUri='" + endpointOverrideUri + '\'' +
                ", awsSdkRegionConfiguration=" + awsSdkRegionConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultS3ClientsIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final ClusterIdValueConfiguration DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION =
                ClusterIdValueConfiguration.PRIMARY;
        private static final TextValueConfiguration DEFAULT_AWS_SDK_ASYNC_HTTP_CLIENT_INTEGRATION_SERVICE_KEY_CONFIGURATION =
                 TextValueConfiguration.PRIMARY;
        private static final TextValueConfiguration DEFAULT_AWS_SDK_HTTP_CLIENT_INTEGRATION_SERVICE_KEY_CONFIGURATION =
                TextValueConfiguration.PRIMARY;
        private static final AwsSdkClientOverrideConfiguration DEFAULT_AWS_SDK_CLIENT_OVERRIDE_CONFIGURATION =
                AwsSdkClientOverrideConfiguration.builder().build();
        private static final AwsSdkClientAsyncConfiguration DEFAULT_AWS_SDK_CLIENT_ASYNC_CONFIGURATION =
                AwsSdkClientAsyncConfiguration.builder().build();
        private static final AwsSdkCredentialsProviderConfiguration DEFAULT_AWS_SDK_CREDENTIALS_PROVIDER_CONFIGURATION =
                AwsSdkCredentialsProviderConfiguration.builder().build();
        private static final AwsSdkS3Configuration DEFAULT_AWS_SDK_S3_CONFIGURATION =
                AwsSdkS3Configuration.builder().build();
        private static final AwsSdkRegionConfiguration DEFAULT_AWS_SDK_REGION_CONFIGURATION =
                AwsSdkRegionConfiguration.builder().build();


        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration =
                DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;
        private DefaultAwsSdkAsyncHttpClientServiceConfiguration customDefaultAwsSdkAsyncHttpClientServiceConfiguration;
        private TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration =
                DEFAULT_AWS_SDK_ASYNC_HTTP_CLIENT_INTEGRATION_SERVICE_KEY_CONFIGURATION;
        private DefaultAwsSdkHttpClientServiceConfiguration customDefaultAwsSdkHttpClientServiceConfiguration;
        private TextValueConfiguration awsSdkHttpClientIntegrationServiceKeyConfiguration =
                DEFAULT_AWS_SDK_HTTP_CLIENT_INTEGRATION_SERVICE_KEY_CONFIGURATION;
        private AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration =
                DEFAULT_AWS_SDK_CLIENT_OVERRIDE_CONFIGURATION;
        private AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration =
                DEFAULT_AWS_SDK_CLIENT_ASYNC_CONFIGURATION;
        private AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration =
                DEFAULT_AWS_SDK_CREDENTIALS_PROVIDER_CONFIGURATION;
        private AwsSdkS3Configuration awsSdkS3Configuration =
                DEFAULT_AWS_SDK_S3_CONFIGURATION;
        private String endpointOverrideUri;
        private AwsSdkRegionConfiguration awsSdkRegionConfiguration =
                DEFAULT_AWS_SDK_REGION_CONFIGURATION;


        private Builder() {
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public Builder customDefaultAwsSdkAsyncHttpClientServiceConfiguration(
                final DefaultAwsSdkAsyncHttpClientServiceConfiguration
                        customDefaultAwsSdkAsyncHttpClientServiceConfiguration)
        {
            this.customDefaultAwsSdkAsyncHttpClientServiceConfiguration =
                    customDefaultAwsSdkAsyncHttpClientServiceConfiguration;
            return this;
        }

        public Builder awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration(
                final TextValueConfiguration awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration)
        {
            this.awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration =
                    awsSdkAsyncHttpClientIntegrationServiceKeyConfiguration;
            return this;
        }

        public Builder customDefaultAwsSdkHttpClientServiceConfiguration(
                final DefaultAwsSdkHttpClientServiceConfiguration customDefaultAwsSdkHttpClientServiceConfiguration)
        {
            this.customDefaultAwsSdkHttpClientServiceConfiguration =
                    customDefaultAwsSdkHttpClientServiceConfiguration;
            return this;
        }

        public Builder awsSdkHttpClientIntegrationServiceKeyConfiguration(
                final TextValueConfiguration awsSdkHttpClientIntegrationServiceKeyConfiguration)
        {
            this.awsSdkHttpClientIntegrationServiceKeyConfiguration =
                    awsSdkHttpClientIntegrationServiceKeyConfiguration;
            return this;
        }

        public Builder awsSdkClientOverrideConfiguration(
                final AwsSdkClientOverrideConfiguration awsSdkClientOverrideConfiguration)
        {
            this.awsSdkClientOverrideConfiguration = awsSdkClientOverrideConfiguration;
            return this;
        }

        public Builder awsSdkClientAsyncConfiguration(final AwsSdkClientAsyncConfiguration awsSdkClientAsyncConfiguration) {
            this.awsSdkClientAsyncConfiguration = awsSdkClientAsyncConfiguration;
            return this;
        }

        public Builder awsSdkCredentialsProviderConfiguration(
                final AwsSdkCredentialsProviderConfiguration awsSdkCredentialsProviderConfiguration)
        {
            this.awsSdkCredentialsProviderConfiguration = awsSdkCredentialsProviderConfiguration;
            return this;
        }

        public Builder awsSdkS3Configuration(final AwsSdkS3Configuration awsSdkS3Configuration) {
            this.awsSdkS3Configuration = awsSdkS3Configuration;
            return this;
        }

        public Builder endpointOverrideUri(final String endpointOverrideUri) {
            this.endpointOverrideUri = endpointOverrideUri;
            return this;
        }

        public Builder awsSdkRegionConfiguration(final AwsSdkRegionConfiguration awsSdkRegionConfiguration) {
            this.awsSdkRegionConfiguration = awsSdkRegionConfiguration;
            return this;
        }

        public DefaultS3ClientsIntegrationServiceConfiguration build() {
            return new DefaultS3ClientsIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
