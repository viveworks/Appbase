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
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.DefaultElasticsearchLowLevelRestClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for {@link DefaultElasticsearchLowLevelRestClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
        implements ElasticsearchLowLevelRestClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    private final Set<HttpHostConfiguration> httpHostConfigurations;

    @Nullable
    private final String pathPrefix;

    private final boolean strictDeprecationMode;

    private final RequestConfiguration requestConfiguration;

    private final HttpAsyncClientConfiguration httpAsyncClientConfiguration;

    @Nullable
    private final Map<String, String> defaultHeaders;

    @Nullable
    private final InjectedTypeConfiguration<? extends RestClientBuilder.RequestConfigCallback>
            requestConfigCallbackInjectedTypeConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<? extends RestClient.FailureListener>
            failureListenerInjectedTypeConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<? extends RestClientBuilder.HttpClientConfigCallback>
            httpClientConfigCallbackInjectedTypeConfiguration;

    @Nullable
    private final NodeSelectorType nodeSelector;

    @Nullable
    private final Boolean compressionEnabled;



    private DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.clusterIdValueKeyConfiguration, "builder.clusterIdValueKeyConfiguration");
        AppbaseUtil.notNull(builder.httpHostConfigurations, "builder.httpHostConfigurations");
        AppbaseUtil.notEmpty(builder.httpHostConfigurations, "builder.httpHostConfigurations");

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        httpHostConfigurations = builder.httpHostConfigurations;
        pathPrefix = builder.pathPrefix;
        strictDeprecationMode = builder.strictDeprecationMode;
        requestConfiguration = builder.requestConfiguration;
        httpAsyncClientConfiguration = builder.httpAsyncClientConfiguration;
        defaultHeaders = builder.defaultHeaders;
        requestConfigCallbackInjectedTypeConfiguration = builder.requestConfigCallbackInjectedTypeConfiguration;
        failureListenerInjectedTypeConfiguration = builder.failureListenerInjectedTypeConfiguration;
        httpClientConfigCallbackInjectedTypeConfiguration = builder.httpClientConfigCallbackInjectedTypeConfiguration;
        nodeSelector = builder.nodeSelector;
        compressionEnabled = builder.compressionEnabled;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    public Set<HttpHostConfiguration> getHttpHostConfigurations() {
        return httpHostConfigurations;
    }

    @Nullable
    public String getPathPrefix() {
        return pathPrefix;
    }

    public boolean isStrictDeprecationMode() {
        return strictDeprecationMode;
    }

    public RequestConfiguration getRequestConfiguration() {
        return requestConfiguration;
    }

    public HttpAsyncClientConfiguration getHttpAsyncClientConfiguration() {
        return httpAsyncClientConfiguration;
    }

    @Nullable
    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    @Nullable
    public InjectedTypeConfiguration<? extends RestClientBuilder.RequestConfigCallback>
    getRequestConfigCallbackInjectedTypeConfiguration()
    {
        return requestConfigCallbackInjectedTypeConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<? extends RestClient.FailureListener> getFailureListenerInjectedTypeConfiguration() {
        return failureListenerInjectedTypeConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<? extends RestClientBuilder.HttpClientConfigCallback>
    getHttpClientConfigCallbackInjectedTypeConfiguration()
    {
        return httpClientConfigCallbackInjectedTypeConfiguration;
    }

    @Nullable
    public NodeSelectorType getNodeSelector() {
        return nodeSelector;
    }

    @Nullable
    public Boolean getCompressionEnabled() {
        return compressionEnabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration that =
                (DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration) o;
        return strictDeprecationMode == that.strictDeprecationMode &&
                Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(httpHostConfigurations, that.httpHostConfigurations) &&
                Objects.equals(pathPrefix, that.pathPrefix) &&
                Objects.equals(requestConfiguration, that.requestConfiguration) &&
                Objects.equals(httpAsyncClientConfiguration, that.httpAsyncClientConfiguration) &&
                Objects.equals(defaultHeaders, that.defaultHeaders) &&
                Objects.equals(
                        requestConfigCallbackInjectedTypeConfiguration,
                        that.requestConfigCallbackInjectedTypeConfiguration) &&
                Objects.equals(failureListenerInjectedTypeConfiguration, that.failureListenerInjectedTypeConfiguration) &&
                Objects.equals(
                        httpClientConfigCallbackInjectedTypeConfiguration,
                        that.httpClientConfigCallbackInjectedTypeConfiguration) &&
                nodeSelector == that.nodeSelector &&
                Objects.equals(compressionEnabled, that.compressionEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueKeyConfiguration,
                httpHostConfigurations,
                pathPrefix,
                strictDeprecationMode,
                requestConfiguration,
                httpAsyncClientConfiguration,
                defaultHeaders,
                requestConfigCallbackInjectedTypeConfiguration,
                failureListenerInjectedTypeConfiguration,
                httpClientConfigCallbackInjectedTypeConfiguration,
                nodeSelector,
                compressionEnabled);
    }

    @Override
    public String toString() {
        return "DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", httpHostConfigurations=" + httpHostConfigurations +
                ", pathPrefix='" + pathPrefix + '\'' +
                ", strictDeprecationMode=" + strictDeprecationMode +
                ", requestConfiguration=" + requestConfiguration +
                ", httpAsyncClientConfiguration=" + httpAsyncClientConfiguration +
                ", defaultHeaders=" + AppbaseUtil.OBFUSCATE +
                ", requestConfigCallbackInjectedTypeConfiguration=" + requestConfigCallbackInjectedTypeConfiguration +
                ", failureListenerInjectedTypeConfiguration=" + failureListenerInjectedTypeConfiguration +
                ", httpClientConfigCallbackInjectedTypeConfiguration=" + httpClientConfigCallbackInjectedTypeConfiguration +
                ", nodeSelector=" + nodeSelector +
                ", compressionEnabled=" + compressionEnabled +
                '}';
    }


    /**
     * Builder for {@link DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration}.
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
        private static final Set<HttpHostConfiguration> DEFAULT_HTTP_HOST_CONFIGURATIONS = Set.of(
                HttpHostConfiguration
                        .builder()
                        .scheme("http")
                        .hostname("localhost")
                        .port(9200)
                        .build());
        private static final String DEFAULT_PATH_PREFIX = null;
        private static final boolean DEFAULT_STRICT_DEPRECATION_MODE = true;
        private static final RequestConfiguration DEFAULT_REQUEST_CONFIGURATION =
                RequestConfiguration.builder().build();
        private static final HttpAsyncClientConfiguration DEFAULT_HTTP_ASYNC_CLIENT_CONFIGURATION =
                HttpAsyncClientConfiguration.builder().build();



        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration = DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;

        private Set<HttpHostConfiguration> httpHostConfigurations = DEFAULT_HTTP_HOST_CONFIGURATIONS;

        @Nullable
        private String pathPrefix = DEFAULT_PATH_PREFIX;

        private boolean strictDeprecationMode = DEFAULT_STRICT_DEPRECATION_MODE;

        private RequestConfiguration requestConfiguration = DEFAULT_REQUEST_CONFIGURATION;

        private HttpAsyncClientConfiguration httpAsyncClientConfiguration = DEFAULT_HTTP_ASYNC_CLIENT_CONFIGURATION;

        @Nullable
        private Map<String, String> defaultHeaders;

        @Nullable
        private InjectedTypeConfiguration<? extends RestClientBuilder.RequestConfigCallback>
                requestConfigCallbackInjectedTypeConfiguration;

        @Nullable
        private InjectedTypeConfiguration<? extends RestClient.FailureListener> failureListenerInjectedTypeConfiguration;

        @Nullable
        private InjectedTypeConfiguration<? extends RestClientBuilder.HttpClientConfigCallback>
                httpClientConfigCallbackInjectedTypeConfiguration;

        @Nullable
        private NodeSelectorType nodeSelector;

        @Nullable
        private Boolean compressionEnabled;



        private Builder() {
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public Builder httpHostConfigurations(final Set<HttpHostConfiguration> httpHostConfigurations) {
            this.httpHostConfigurations = httpHostConfigurations;
            return this;
        }

        public Builder pathPrefix(@Nullable final String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        public Builder strictDeprecationMode(final boolean strictDeprecationMode) {
            this.strictDeprecationMode = strictDeprecationMode;
            return this;
        }

        public Builder requestConfiguration(final RequestConfiguration requestConfiguration) {
            this.requestConfiguration = requestConfiguration;
            return this;
        }

        public Builder httpAsyncClientConfiguration(final HttpAsyncClientConfiguration httpAsyncClientConfiguration) {
            this.httpAsyncClientConfiguration = httpAsyncClientConfiguration;
            return this;
        }

        public Builder defaultHeaders(@Nullable final Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
            return this;
        }

        public Builder requestConfigCallbackInjectedTypeConfiguration(
                @Nullable final InjectedTypeConfiguration<? extends RestClientBuilder.RequestConfigCallback>
                        requestConfigCallbackInjectedTypeConfiguration)
        {
            this.requestConfigCallbackInjectedTypeConfiguration = requestConfigCallbackInjectedTypeConfiguration;
            return this;
        }

        public Builder failureListenerInjectedTypeConfiguration(
                @Nullable final InjectedTypeConfiguration<? extends RestClient.FailureListener>
                        failureListenerInjectedTypeConfiguration)
        {
            this.failureListenerInjectedTypeConfiguration = failureListenerInjectedTypeConfiguration;
            return this;
        }

        public Builder httpClientConfigCallbackInjectedTypeConfiguration(
                @Nullable final InjectedTypeConfiguration<? extends RestClientBuilder.HttpClientConfigCallback>
                        httpClientConfigCallbackInjectedTypeConfiguration)
        {
            this.httpClientConfigCallbackInjectedTypeConfiguration = httpClientConfigCallbackInjectedTypeConfiguration;
            return this;
        }

        public Builder nodeSelector(@Nullable final NodeSelectorType nodeSelector) {
            this.nodeSelector = nodeSelector;
            return this;
        }

        public Builder compressionEnabled(@Nullable final Boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
            return this;
        }

        public DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration build() {
            return new DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
