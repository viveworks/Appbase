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
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkHttpClientIntegrationService;
import com.viveworks.appbase.client.cloud.aws.common.configuration.tls.AwsSdkTlsKeyManagersProviderConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.tls.FileStoreTlsKeyManagersProviderConfiguration;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.InetAddressConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsTrustManagersProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultAwsSdkHttpClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAwsSdkHttpClientIntegrationServiceConfiguration.Builder.class)
public class DefaultAwsSdkHttpClientIntegrationServiceConfiguration implements AwsSdkHttpClientIntegrationServiceConfiguration {
    private final TextValueConfiguration textValueKeyConfiguration;
    private final TimeoutConfiguration connectionAcquisitionTimeoutConfiguration;
    private final TimeoutConfiguration connectionTimeoutConfiguration;
    private final TimeoutConfiguration connectionMaxIdleTimeoutConfiguration;
    private final TimeUnitConfiguration connectionTimeToLiveConfiguration;
    private final Boolean expectContinueEnabled;
    private final Integer maxConnections;
    private final Boolean tcpKeepAlive;
    private final TimeoutConfiguration socketTimeoutConfiguration;
    private final Boolean useIdleConnectionReaper;
    private final AwsSdkApacheProxyConfiguration awsSdkApacheProxyConfiguration;
    private final AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration;
    private final InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
            tlsTrustManagersProviderInjectedTypeConfiguration;
    private final InetAddressConfiguration localAddressConfiguration;
    private final InjectedTypeConfiguration<? extends CredentialsProvider>
            credentialsProviderInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends DnsResolver>
            dnsResolverInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends HttpRoutePlanner>
            httpRoutePlannerInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends ConnectionSocketFactory>
            connectionSocketFactoryInjectedTypeConfiguration;

    private DefaultAwsSdkHttpClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        connectionAcquisitionTimeoutConfiguration = builder.connectionAcquisitionTimeoutConfiguration;
        connectionTimeoutConfiguration = builder.connectionTimeoutConfiguration;
        connectionMaxIdleTimeoutConfiguration = builder.connectionMaxIdleTimeoutConfiguration;
        connectionTimeToLiveConfiguration = builder.connectionTimeToLiveConfiguration;
        expectContinueEnabled = builder.expectContinueEnabled;
        maxConnections = builder.maxConnections;
        tcpKeepAlive = builder.tcpKeepAlive;
        socketTimeoutConfiguration = builder.socketTimeoutConfiguration;
        useIdleConnectionReaper = builder.useIdleConnectionReaper;
        awsSdkApacheProxyConfiguration = builder.awsSdkApacheProxyConfiguration;
        awsSdkTlsKeyManagersProviderConfiguration = builder.awsSdkTlsKeyManagersProviderConfiguration;
        tlsTrustManagersProviderInjectedTypeConfiguration = builder.tlsTrustManagersProviderInjectedTypeConfiguration;
        localAddressConfiguration = builder.localAddressConfiguration;
        credentialsProviderInjectedTypeConfiguration = builder.credentialsProviderInjectedTypeConfiguration;
        dnsResolverInjectedTypeConfiguration = builder.dnsResolverInjectedTypeConfiguration;
        httpRoutePlannerInjectedTypeConfiguration = builder.httpRoutePlannerInjectedTypeConfiguration;
        connectionSocketFactoryInjectedTypeConfiguration = builder.connectionSocketFactoryInjectedTypeConfiguration;
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
     * Returns the configured text value key configuration.
     *
     * @return the configured text value key configuration
     */
    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    /**
     * Returns the configured connection acquisition timeout configuration.
     *
     * @return the configured connection acquisition timeout configuration
     */
    public TimeoutConfiguration getConnectionAcquisitionTimeoutConfiguration() {
        return connectionAcquisitionTimeoutConfiguration;
    }

    /**
     * Returns the configured connection timeout configuration.
     *
     * @return the configured connection timeout configuration
     */
    public TimeoutConfiguration getConnectionTimeoutConfiguration() {
        return connectionTimeoutConfiguration;
    }

    /**
     * Returns the configured connection max idle timeout configuration.
     *
     * @return the configured connection max idle timeout configuration
     */
    public TimeoutConfiguration getConnectionMaxIdleTimeoutConfiguration() {
        return connectionMaxIdleTimeoutConfiguration;
    }

    /**
     * Returns the configured connection time to live configuration.
     *
     * @return the configured connection time to live configuration
     */
    public TimeUnitConfiguration getConnectionTimeToLiveConfiguration() {
        return connectionTimeToLiveConfiguration;
    }

    /**
     * Returns the configured expect continue enabled.
     *
     * @return the configured expect continue enabled
     */
    public Boolean getExpectContinueEnabled() {
        return expectContinueEnabled;
    }

    /**
     * Returns the configured max connections.
     *
     * @return the configured max connections
     */
    public Integer getMaxConnections() {
        return maxConnections;
    }

    /**
     * Returns the configured tcp keep alive.
     *
     * @return the configured tcp keep alive
     */
    public Boolean getTcpKeepAlive() {
        return tcpKeepAlive;
    }

    /**
     * Returns the configured socket timeout configuration.
     *
     * @return the configured socket timeout configuration
     */
    public TimeoutConfiguration getSocketTimeoutConfiguration() {
        return socketTimeoutConfiguration;
    }

    /**
     * Returns the configured use idle connection reaper.
     *
     * @return the configured use idle connection reaper
     */
    public Boolean getUseIdleConnectionReaper() {
        return useIdleConnectionReaper;
    }

    /**
     * Returns the configured aws sdk apache proxy configuration.
     *
     * @return the configured aws sdk apache proxy configuration
     */
    public AwsSdkApacheProxyConfiguration getAwsSdkApacheProxyConfiguration() {
        return awsSdkApacheProxyConfiguration;
    }

    /**
     * Returns the configured aws sdk tls key managers provider configuration.
     *
     * @return the configured aws sdk tls key managers provider configuration
     */
    public AwsSdkTlsKeyManagersProviderConfiguration getAwsSdkTlsKeyManagersProviderConfiguration() {
        return awsSdkTlsKeyManagersProviderConfiguration;
    }

    /**
     * Returns the configured tls trust managers provider injected type configuration.
     *
     * @return the configured tls trust managers provider injected type configuration
     */
    public InjectedTypeConfiguration<? extends TlsTrustManagersProvider> getTlsTrustManagersProviderInjectedTypeConfiguration() {
        return tlsTrustManagersProviderInjectedTypeConfiguration;
    }

    /**
     * Returns the configured local address configuration.
     *
     * @return the configured local address configuration
     */
    public InetAddressConfiguration getLocalAddressConfiguration() {
        return localAddressConfiguration;
    }

    /**
     * Returns the configured credentials provider injected type configuration.
     *
     * @return the configured credentials provider injected type configuration
     */
    public InjectedTypeConfiguration<? extends CredentialsProvider> getCredentialsProviderInjectedTypeConfiguration() {
        return credentialsProviderInjectedTypeConfiguration;
    }

    /**
     * Returns the configured dns resolver injected type configuration.
     *
     * @return the configured dns resolver injected type configuration
     */
    public InjectedTypeConfiguration<? extends DnsResolver> getDnsResolverInjectedTypeConfiguration() {
        return dnsResolverInjectedTypeConfiguration;
    }

    /**
     * Returns the configured http route planner injected type configuration.
     *
     * @return the configured http route planner injected type configuration
     */
    public InjectedTypeConfiguration<? extends HttpRoutePlanner> getHttpRoutePlannerInjectedTypeConfiguration() {
        return httpRoutePlannerInjectedTypeConfiguration;
    }

    /**
     * Returns the configured connection socket factory injected type configuration.
     *
     * @return the configured connection socket factory injected type configuration
     */
    public InjectedTypeConfiguration<? extends ConnectionSocketFactory> getConnectionSocketFactoryInjectedTypeConfiguration() {
        return connectionSocketFactoryInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultAwsSdkHttpClientIntegrationServiceConfiguration that = (DefaultAwsSdkHttpClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(connectionAcquisitionTimeoutConfiguration, that.connectionAcquisitionTimeoutConfiguration) &&
                Objects.equals(connectionTimeoutConfiguration, that.connectionTimeoutConfiguration) &&
                Objects.equals(connectionMaxIdleTimeoutConfiguration, that.connectionMaxIdleTimeoutConfiguration) &&
                Objects.equals(connectionTimeToLiveConfiguration, that.connectionTimeToLiveConfiguration) &&
                Objects.equals(expectContinueEnabled, that.expectContinueEnabled) &&
                Objects.equals(maxConnections, that.maxConnections) &&
                Objects.equals(tcpKeepAlive, that.tcpKeepAlive) &&
                Objects.equals(socketTimeoutConfiguration, that.socketTimeoutConfiguration) &&
                Objects.equals(useIdleConnectionReaper, that.useIdleConnectionReaper) &&
                Objects.equals(awsSdkApacheProxyConfiguration, that.awsSdkApacheProxyConfiguration) &&
                Objects.equals(awsSdkTlsKeyManagersProviderConfiguration, that.awsSdkTlsKeyManagersProviderConfiguration) &&
                Objects.equals(tlsTrustManagersProviderInjectedTypeConfiguration, that.tlsTrustManagersProviderInjectedTypeConfiguration) &&
                Objects.equals(localAddressConfiguration, that.localAddressConfiguration) &&
                Objects.equals(credentialsProviderInjectedTypeConfiguration, that.credentialsProviderInjectedTypeConfiguration) &&
                Objects.equals(dnsResolverInjectedTypeConfiguration, that.dnsResolverInjectedTypeConfiguration) &&
                Objects.equals(httpRoutePlannerInjectedTypeConfiguration, that.httpRoutePlannerInjectedTypeConfiguration) &&
                Objects.equals(connectionSocketFactoryInjectedTypeConfiguration, that.connectionSocketFactoryInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textValueKeyConfiguration,
                connectionAcquisitionTimeoutConfiguration,
                connectionTimeoutConfiguration,
                connectionMaxIdleTimeoutConfiguration,
                connectionTimeToLiveConfiguration,
                expectContinueEnabled,
                maxConnections,
                tcpKeepAlive,
                socketTimeoutConfiguration,
                useIdleConnectionReaper,
                awsSdkApacheProxyConfiguration,
                awsSdkTlsKeyManagersProviderConfiguration,
                tlsTrustManagersProviderInjectedTypeConfiguration,
                localAddressConfiguration,
                credentialsProviderInjectedTypeConfiguration,
                dnsResolverInjectedTypeConfiguration,
                httpRoutePlannerInjectedTypeConfiguration,
                connectionSocketFactoryInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultAwsSdkHttpClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", connectionAcquisitionTimeoutConfiguration=" + connectionAcquisitionTimeoutConfiguration +
                ", connectionTimeoutConfiguration=" + connectionTimeoutConfiguration +
                ", connectionMaxIdleTimeoutConfiguration=" + connectionMaxIdleTimeoutConfiguration +
                ", connectionTimeToLiveConfiguration=" + connectionTimeToLiveConfiguration +
                ", expectContinueEnabled=" + expectContinueEnabled +
                ", maxConnections=" + maxConnections +
                ", tcpKeepAlive=" + tcpKeepAlive +
                ", socketTimeoutConfiguration=" + socketTimeoutConfiguration +
                ", useIdleConnectionReaper=" + useIdleConnectionReaper +
                ", awsSdkApacheProxyConfiguration=" + awsSdkApacheProxyConfiguration +
                ", awsSdkTlsKeyManagersProviderConfiguration=" + awsSdkTlsKeyManagersProviderConfiguration +
                ", tlsTrustManagersProviderInjectedTypeConfiguration=" + tlsTrustManagersProviderInjectedTypeConfiguration +
                ", localAddressConfiguration=" + localAddressConfiguration +
                ", credentialsProviderInjectedTypeConfiguration=" + credentialsProviderInjectedTypeConfiguration +
                ", dnsResolverInjectedTypeConfiguration=" + dnsResolverInjectedTypeConfiguration +
                ", httpRoutePlannerInjectedTypeConfiguration=" + httpRoutePlannerInjectedTypeConfiguration +
                ", connectionSocketFactoryInjectedTypeConfiguration=" + connectionSocketFactoryInjectedTypeConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TextValueConfiguration DEFAULT_TEXT_VALUE_KEY_CONFIGURATION = TextValueConfiguration.PRIMARY;
        private static final TimeoutConfiguration DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT) != null ?
                                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                                .get(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT).toMillis() :
                                        30_000L)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT) != null ?
                                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                                .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis() :
                                        30_000L)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_CONNECTION_MAX_IDLE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT) != null ?
                                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                                .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis() :
                                        30_000L)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final TimeUnitConfiguration DEFAULT_CONNECTION_TIME_TO_LIVE_CONFIGURATION =
                TimeUnitConfiguration
                        .builder()
                        .enabled(true)
                        .time(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE) != null ?
                                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                                .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis() :
                                        30_000L)
                        .timeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final Boolean DEFAULT_EXPECT_CONTINUE_ENABLED = true;
        private static final Integer DEFAULT_MAX_CONNECTIONS =
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                        .get(SdkHttpConfigurationOption.MAX_CONNECTIONS) != null ?
                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                .get(SdkHttpConfigurationOption.MAX_CONNECTIONS) :
                        1000;
        private static final Boolean DEFAULT_TCP_KEEP_ALIVE = software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                .get(SdkHttpConfigurationOption.TCP_KEEPALIVE) != null ?
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS.get(SdkHttpConfigurationOption.TCP_KEEPALIVE) :
                true;
        private static final TimeoutConfiguration DEFAULT_SOCKET_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .timeout(30L)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final Boolean DEFAULT_USE_IDLE_CONNECTION_REAPER =
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                        .get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS) != null ?
                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                .get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS) :
                        true;
        private static final AwsSdkTlsKeyManagersProviderConfiguration
                DEFAULT_AWS_SDK_TLS_KEY_MANAGERS_PROVIDER_CONFIGURATION =
                FileStoreTlsKeyManagersProviderConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration = DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;
        private TimeoutConfiguration connectionAcquisitionTimeoutConfiguration = DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration connectionTimeoutConfiguration = DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration connectionMaxIdleTimeoutConfiguration = DEFAULT_CONNECTION_MAX_IDLE_TIMEOUT_CONFIGURATION;
        private TimeUnitConfiguration connectionTimeToLiveConfiguration = DEFAULT_CONNECTION_TIME_TO_LIVE_CONFIGURATION;
        private Boolean expectContinueEnabled = DEFAULT_EXPECT_CONTINUE_ENABLED;
        private Integer maxConnections = DEFAULT_MAX_CONNECTIONS;
        private Boolean tcpKeepAlive = DEFAULT_TCP_KEEP_ALIVE;
        private TimeoutConfiguration socketTimeoutConfiguration = DEFAULT_SOCKET_TIMEOUT_CONFIGURATION;
        private Boolean useIdleConnectionReaper = DEFAULT_USE_IDLE_CONNECTION_REAPER;
        private AwsSdkApacheProxyConfiguration awsSdkApacheProxyConfiguration;
        private AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration =
                DEFAULT_AWS_SDK_TLS_KEY_MANAGERS_PROVIDER_CONFIGURATION;
        private InjectedTypeConfiguration<? extends TlsTrustManagersProvider> tlsTrustManagersProviderInjectedTypeConfiguration;
        private InetAddressConfiguration localAddressConfiguration;
        private InjectedTypeConfiguration<? extends CredentialsProvider> credentialsProviderInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends DnsResolver> dnsResolverInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends HttpRoutePlanner> httpRoutePlannerInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends ConnectionSocketFactory> connectionSocketFactoryInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder connectionAcquisitionTimeoutConfiguration(final TimeoutConfiguration connectionAcquisitionTimeoutConfiguration) {
            this.connectionAcquisitionTimeoutConfiguration = connectionAcquisitionTimeoutConfiguration;
            return this;
        }

        public Builder connectionTimeoutConfiguration(final TimeoutConfiguration connectionTimeoutConfiguration) {
            this.connectionTimeoutConfiguration = connectionTimeoutConfiguration;
            return this;
        }

        public Builder connectionMaxIdleTimeoutConfiguration(final TimeoutConfiguration connectionMaxIdleTimeoutConfiguration) {
            this.connectionMaxIdleTimeoutConfiguration = connectionMaxIdleTimeoutConfiguration;
            return this;
        }

        public Builder connectionTimeToLiveConfiguration(final TimeUnitConfiguration connectionTimeToLiveConfiguration) {
            this.connectionTimeToLiveConfiguration = connectionTimeToLiveConfiguration;
            return this;
        }

        public Builder expectContinueEnabled(final Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public Builder maxConnections(final Integer maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder tcpKeepAlive(final Boolean tcpKeepAlive) {
            this.tcpKeepAlive = tcpKeepAlive;
            return this;
        }

        public Builder socketTimeoutConfiguration(final TimeoutConfiguration socketTimeoutConfiguration) {
            this.socketTimeoutConfiguration = socketTimeoutConfiguration;
            return this;
        }

        public Builder useIdleConnectionReaper(final Boolean useIdleConnectionReaper) {
            this.useIdleConnectionReaper = useIdleConnectionReaper;
            return this;
        }

        public Builder awsSdkApacheProxyConfiguration(final AwsSdkApacheProxyConfiguration awsSdkApacheProxyConfiguration) {
            this.awsSdkApacheProxyConfiguration = awsSdkApacheProxyConfiguration;
            return this;
        }

        public Builder awsSdkTlsKeyManagersProviderConfiguration(
                final AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration)
        {
            this.awsSdkTlsKeyManagersProviderConfiguration = awsSdkTlsKeyManagersProviderConfiguration;
            return this;
        }

        public Builder tlsTrustManagersProviderInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends TlsTrustManagersProvider> tlsTrustManagersProviderInjectedTypeConfiguration)
        {
            this.tlsTrustManagersProviderInjectedTypeConfiguration = tlsTrustManagersProviderInjectedTypeConfiguration;
            return this;
        }

        public Builder localAddressConfiguration(final InetAddressConfiguration localAddressConfiguration) {
            this.localAddressConfiguration = localAddressConfiguration;
            return this;
        }

        public Builder credentialsProviderInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends CredentialsProvider> credentialsProviderInjectedTypeConfiguration)
        {
            this.credentialsProviderInjectedTypeConfiguration = credentialsProviderInjectedTypeConfiguration;
            return this;
        }

        public Builder dnsResolverInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends DnsResolver> dnsResolverInjectedTypeConfiguration)
        {
            this.dnsResolverInjectedTypeConfiguration = dnsResolverInjectedTypeConfiguration;
            return this;
        }

        public Builder httpRoutePlannerInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends HttpRoutePlanner> httpRoutePlannerInjectedTypeConfiguration)
        {
            this.httpRoutePlannerInjectedTypeConfiguration = httpRoutePlannerInjectedTypeConfiguration;
            return this;
        }

        public Builder connectionSocketFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends ConnectionSocketFactory> connectionSocketFactoryInjectedTypeConfiguration)
        {
            this.connectionSocketFactoryInjectedTypeConfiguration = connectionSocketFactoryInjectedTypeConfiguration;
            return this;
        }

        public DefaultAwsSdkHttpClientIntegrationServiceConfiguration build() {
            return new DefaultAwsSdkHttpClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
