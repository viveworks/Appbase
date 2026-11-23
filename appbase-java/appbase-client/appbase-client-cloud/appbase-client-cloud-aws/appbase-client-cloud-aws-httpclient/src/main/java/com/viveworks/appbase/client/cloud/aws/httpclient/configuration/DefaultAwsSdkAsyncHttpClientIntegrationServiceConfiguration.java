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
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.DefaultAwsSdkAsyncHttpClientIntegrationService;
import com.viveworks.appbase.client.cloud.aws.common.configuration.tls.AwsSdkTlsKeyManagersProviderConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.tls.FileStoreTlsKeyManagersProviderConfiguration;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.ChannelOptionsConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslContextCommonConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslImplementation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsTrustManagersProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultAwsSdkAsyncHttpClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
        implements AwsSdkAsyncHttpClientIntegrationServiceConfiguration
{
    private final TextValueConfiguration textValueKeyConfiguration;
    private final AwsSdkEventLoopGroupConfiguration awsSdkEventLoopGroupConfiguration;
    private final AwsSdkProtocol awsSdkProtocol;
    private final ChannelOptionsConfiguration channelOptionsConfiguration;
    private final AwsSdkHttp2Configuration awsSdkHttp2Configuration;
    private final TimeoutConfiguration connectionAcquisitionTimeoutConfiguration;
    private final TimeoutConfiguration connectionTimeoutConfiguration;
    private final TimeoutConfiguration readTimeoutConfiguration;
    private final TimeoutConfiguration writeTimeoutConfiguration;
    private final TimeoutConfiguration connectionMaxIdleTimeoutConfiguration;
    private final TimeUnitConfiguration connectionTimeToLiveConfiguration;
    private final Integer maxConcurrency;
    private final Integer maxPendingConnectionAcquires;
    private final Boolean useIdleConnectionReaper;
    private final AwsSdkProxyConfiguration awsSdkProxyConfiguration;
    private final SslImplementation sslImplementation;
    private final AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration;
    private final InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
            tlsTrustManagersProviderInjectedTypeConfiguration;


    private DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");
        AppbaseUtil.notNull(builder.awsSdkEventLoopGroupConfiguration, "builder.awsSdkEventLoopGroupConfiguration");
        AppbaseUtil.notNull(builder.awsSdkProtocol, "builder.awsSdkProtocol");

        if (builder.maxConcurrency != null) {
            AppbaseUtil.positiveOrZero(builder.maxConcurrency, "builder.maxConcurrency");
        }

        if (builder.maxPendingConnectionAcquires != null) {
            AppbaseUtil.notNull(builder.maxPendingConnectionAcquires, "builder.maxPendingConnectionAcquires");
        }

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        awsSdkEventLoopGroupConfiguration = builder.awsSdkEventLoopGroupConfiguration;
        awsSdkProtocol = builder.awsSdkProtocol;
        channelOptionsConfiguration = builder.channelOptionsConfiguration;
        awsSdkHttp2Configuration = builder.awsSdkHttp2Configuration;
        connectionAcquisitionTimeoutConfiguration = builder.connectionAcquisitionTimeoutConfiguration;
        connectionTimeoutConfiguration = builder.connectionTimeoutConfiguration;
        readTimeoutConfiguration = builder.readTimeoutConfiguration;
        writeTimeoutConfiguration = builder.writeTimeoutConfiguration;
        connectionMaxIdleTimeoutConfiguration = builder.connectionMaxIdleTimeoutConfiguration;
        connectionTimeToLiveConfiguration = builder.connectionTimeToLiveConfiguration;
        maxConcurrency = builder.maxConcurrency;
        maxPendingConnectionAcquires = builder.maxPendingConnectionAcquires;
        useIdleConnectionReaper = builder.useIdleConnectionReaper;
        awsSdkProxyConfiguration = builder.awsSdkProxyConfiguration;
        sslImplementation = builder.sslImplementation;
        awsSdkTlsKeyManagersProviderConfiguration = builder.awsSdkTlsKeyManagersProviderConfiguration;
        tlsTrustManagersProviderInjectedTypeConfiguration = builder.tlsTrustManagersProviderInjectedTypeConfiguration;
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
     * Returns the configured aws sdk event loop group configuration.
     *
     * @return the configured aws sdk event loop group configuration
     */
    public AwsSdkEventLoopGroupConfiguration getAwsSdkEventLoopGroupConfiguration() {
        return awsSdkEventLoopGroupConfiguration;
    }

    /**
     * Returns the configured aws sdk protocol.
     *
     * @return the configured aws sdk protocol
     */
    public AwsSdkProtocol getAwsSdkProtocol() {
        return awsSdkProtocol;
    }

    /**
     * Returns the configured channel options configuration.
     *
     * @return the configured channel options configuration
     */
    public ChannelOptionsConfiguration getChannelOptionsConfiguration() {
        return channelOptionsConfiguration;
    }

    /**
     * Returns the configured aws sdk http2 configuration.
     *
     * @return the configured aws sdk http2 configuration
     */
    public AwsSdkHttp2Configuration getAwsSdkHttp2Configuration() {
        return awsSdkHttp2Configuration;
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
     * Returns the configured read timeout configuration.
     *
     * @return the configured read timeout configuration
     */
    public TimeoutConfiguration getReadTimeoutConfiguration() {
        return readTimeoutConfiguration;
    }

    /**
     * Returns the configured write timeout configuration.
     *
     * @return the configured write timeout configuration
     */
    public TimeoutConfiguration getWriteTimeoutConfiguration() {
        return writeTimeoutConfiguration;
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
     * Returns the configured max concurrency.
     *
     * @return the configured max concurrency
     */
    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * Returns the configured max pending connection acquires.
     *
     * @return the configured max pending connection acquires
     */
    public Integer getMaxPendingConnectionAcquires() {
        return maxPendingConnectionAcquires;
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
     * Returns the configured aws sdk proxy configuration.
     *
     * @return the configured aws sdk proxy configuration
     */
    public AwsSdkProxyConfiguration getAwsSdkProxyConfiguration() {
        return awsSdkProxyConfiguration;
    }

    /**
     * Returns the configured ssl implementation.
     *
     * @return the configured ssl implementation
     */
    public SslImplementation getSslImplementation() {
        return sslImplementation;
    }

    /**
     * Returns the configured aws sdk tls key managers provider configuration.
     *
     * @return the configured aws sdk tls key managers provider configuration
     */
    public AwsSdkTlsKeyManagersProviderConfiguration getAwsSdkTlsKeyManagersProviderConfiguration() {
        return awsSdkTlsKeyManagersProviderConfiguration;
    }

    public InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
    getTlsTrustManagersProviderInjectedTypeConfiguration()
    {
        return tlsTrustManagersProviderInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration that =
                (DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(awsSdkEventLoopGroupConfiguration, that.awsSdkEventLoopGroupConfiguration) &&
                awsSdkProtocol == that.awsSdkProtocol &&
                Objects.equals(channelOptionsConfiguration, that.channelOptionsConfiguration) &&
                Objects.equals(awsSdkHttp2Configuration, that.awsSdkHttp2Configuration) &&
                Objects.equals(connectionAcquisitionTimeoutConfiguration, that.connectionAcquisitionTimeoutConfiguration) &&
                Objects.equals(connectionTimeoutConfiguration, that.connectionTimeoutConfiguration) &&
                Objects.equals(readTimeoutConfiguration, that.readTimeoutConfiguration) &&
                Objects.equals(writeTimeoutConfiguration, that.writeTimeoutConfiguration) &&
                Objects.equals(connectionMaxIdleTimeoutConfiguration, that.connectionMaxIdleTimeoutConfiguration) &&
                Objects.equals(connectionTimeToLiveConfiguration, that.connectionTimeToLiveConfiguration) &&
                Objects.equals(maxConcurrency, that.maxConcurrency) &&
                Objects.equals(maxPendingConnectionAcquires, that.maxPendingConnectionAcquires) &&
                Objects.equals(useIdleConnectionReaper, that.useIdleConnectionReaper) &&
                Objects.equals(awsSdkProxyConfiguration, that.awsSdkProxyConfiguration) &&
                sslImplementation == that.sslImplementation &&
                Objects.equals(awsSdkTlsKeyManagersProviderConfiguration, that.awsSdkTlsKeyManagersProviderConfiguration) &&
                Objects.equals(
                        tlsTrustManagersProviderInjectedTypeConfiguration,
                        that.tlsTrustManagersProviderInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration,
                awsSdkEventLoopGroupConfiguration,
                awsSdkProtocol,
                channelOptionsConfiguration,
                awsSdkHttp2Configuration,
                connectionAcquisitionTimeoutConfiguration,
                connectionTimeoutConfiguration,
                readTimeoutConfiguration,
                writeTimeoutConfiguration,
                connectionMaxIdleTimeoutConfiguration,
                connectionTimeToLiveConfiguration,
                maxConcurrency,
                maxPendingConnectionAcquires,
                useIdleConnectionReaper,
                awsSdkProxyConfiguration,
                sslImplementation,
                awsSdkTlsKeyManagersProviderConfiguration,
                tlsTrustManagersProviderInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", awsSdkEventLoopGroupConfiguration=" + awsSdkEventLoopGroupConfiguration +
                ", awsSdkProtocol=" + awsSdkProtocol +
                ", channelOptionsConfiguration=" + channelOptionsConfiguration +
                ", awsSdkHttp2Configuration=" + awsSdkHttp2Configuration +
                ", connectionAcquisitionTimeoutConfiguration=" + connectionAcquisitionTimeoutConfiguration +
                ", connectionTimeoutConfiguration=" + connectionTimeoutConfiguration +
                ", readTimeoutConfiguration=" + readTimeoutConfiguration +
                ", writeTimeoutConfiguration=" + writeTimeoutConfiguration +
                ", connectionMaxIdleTimeoutConfiguration=" + connectionMaxIdleTimeoutConfiguration +
                ", connectionTimeToLiveConfiguration=" + connectionTimeToLiveConfiguration +
                ", maxConcurrency=" + maxConcurrency +
                ", maxPendingConnectionAcquires=" + maxPendingConnectionAcquires +
                ", useIdleConnectionReaper=" + useIdleConnectionReaper +
                ", awsSdkProxyConfiguration=" + awsSdkProxyConfiguration +
                ", sslImplementation=" + sslImplementation +
                ", awsSdkTlsKeyManagersProviderConfiguration=" + awsSdkTlsKeyManagersProviderConfiguration +
                ", tlsTrustManagersProviderInjectedTypeConfiguration=" + tlsTrustManagersProviderInjectedTypeConfiguration +
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
        private static final AwsSdkEventLoopGroupConfiguration DEFAULT_AWS_SDK_EVENT_LOOP_GROUP_CONFIGURATION =
                AwsSdkEventLoopGroupConfiguration.builder().build();
        private static final AwsSdkProtocol DEFAULT_AWS_SDK_PROTOCOL =
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                        .get(SdkHttpConfigurationOption.PROTOCOL) != null ?
                        AwsSdkProtocol.awsSdkProtocol(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.PROTOCOL)) : null;
        private static final ChannelOptionsConfiguration DEFAULT_CHANNEL_OPTIONS_CONFIGURATION =
                ChannelOptionsConfiguration.builder().build();
        private static final AwsSdkHttp2Configuration DEFAULT_AWS_SDK_HTTP2_CONFIGURATION =
                AwsSdkHttp2Configuration.builder().build();
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
        private static final TimeoutConfiguration DEFAULT_READ_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT) != null ?
                                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                                .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis() :
                                        60_000L)
                        .timeoutTimeUnit(TimeUnit.MILLISECONDS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_WRITE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .enabled(true)
                        .timeout(
                                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                        .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT) != null ?
                                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                                .get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis() :
                                        60_000L)
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
        private static final Integer DEFAULT_MAX_CONCURRENCY =
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                        .get(SdkHttpConfigurationOption.MAX_CONNECTIONS) != null ?
                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                .get(SdkHttpConfigurationOption.MAX_CONNECTIONS) :
                        1000;
        private static final Integer DEFAULT_MAX_PENDING_CONNECTION_ACQUIRES =
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                        .get(SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES) != null ?
                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                .get(SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES) :
                        1000;
        private static final Boolean DEFAULT_USE_IDLE_CONNECTION_REAPER =
                software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                        .get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS) != null ?
                        software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS
                                .get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS) :
                        true;
        private static final SslImplementation DEFAULT_SSL_IMPLEMENTATION =
                SslContextCommonConfiguration.Builder.DEFAULT_SSL_IMPLEMENTATION;
        private static final AwsSdkTlsKeyManagersProviderConfiguration
                DEFAULT_AWS_SDK_TLS_KEY_MANAGERS_PROVIDER_CONFIGURATION =
                FileStoreTlsKeyManagersProviderConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration =
                DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;
        private AwsSdkEventLoopGroupConfiguration awsSdkEventLoopGroupConfiguration =
                DEFAULT_AWS_SDK_EVENT_LOOP_GROUP_CONFIGURATION;
        private AwsSdkProtocol awsSdkProtocol =
                DEFAULT_AWS_SDK_PROTOCOL;
        private ChannelOptionsConfiguration channelOptionsConfiguration =
                DEFAULT_CHANNEL_OPTIONS_CONFIGURATION;
        private AwsSdkHttp2Configuration awsSdkHttp2Configuration =
                DEFAULT_AWS_SDK_HTTP2_CONFIGURATION;
        private TimeoutConfiguration connectionAcquisitionTimeoutConfiguration =
                DEFAULT_CONNECTION_ACQUISITION_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration connectionTimeoutConfiguration =
                DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration readTimeoutConfiguration =
                DEFAULT_READ_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration writeTimeoutConfiguration =
                DEFAULT_WRITE_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration connectionMaxIdleTimeoutConfiguration =
                DEFAULT_CONNECTION_MAX_IDLE_TIMEOUT_CONFIGURATION;
        private TimeUnitConfiguration connectionTimeToLiveConfiguration =
                DEFAULT_CONNECTION_TIME_TO_LIVE_CONFIGURATION;
        private Integer maxConcurrency =
                DEFAULT_MAX_CONCURRENCY;
        private Integer maxPendingConnectionAcquires =
                DEFAULT_MAX_PENDING_CONNECTION_ACQUIRES;
        private Boolean useIdleConnectionReaper =
                DEFAULT_USE_IDLE_CONNECTION_REAPER;
        private AwsSdkProxyConfiguration awsSdkProxyConfiguration;
        private SslImplementation sslImplementation =
                DEFAULT_SSL_IMPLEMENTATION;
        private AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration =
                DEFAULT_AWS_SDK_TLS_KEY_MANAGERS_PROVIDER_CONFIGURATION;
        private InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
                tlsTrustManagersProviderInjectedTypeConfiguration;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder awsSdkEventLoopGroupConfiguration(
                final AwsSdkEventLoopGroupConfiguration awsSdkEventLoopGroupConfiguration)
        {
            this.awsSdkEventLoopGroupConfiguration = awsSdkEventLoopGroupConfiguration;
            return this;
        }

        public Builder awsSdkProtocol(final AwsSdkProtocol awsSdkProtocol) {
            this.awsSdkProtocol = awsSdkProtocol;
            return this;
        }

        public Builder channelOptionsConfiguration(final ChannelOptionsConfiguration channelOptionsConfiguration) {
            this.channelOptionsConfiguration = channelOptionsConfiguration;
            return this;
        }

        public Builder awsSdkHttp2Configuration(final AwsSdkHttp2Configuration awsSdkHttp2Configuration) {
            this.awsSdkHttp2Configuration = awsSdkHttp2Configuration;
            return this;
        }

        public Builder connectionAcquisitionTimeoutConfiguration(
                final TimeoutConfiguration connectionAcquisitionTimeoutConfiguration)
        {
            this.connectionAcquisitionTimeoutConfiguration = connectionAcquisitionTimeoutConfiguration;
            return this;
        }

        public Builder connectionTimeoutConfiguration(final TimeoutConfiguration connectionTimeoutConfiguration) {
            this.connectionTimeoutConfiguration = connectionTimeoutConfiguration;
            return this;
        }

        public Builder readTimeoutConfiguration(final TimeoutConfiguration readTimeoutConfiguration) {
            this.readTimeoutConfiguration = readTimeoutConfiguration;
            return this;
        }

        public Builder writeTimeoutConfiguration(final TimeoutConfiguration writeTimeoutConfiguration) {
            this.writeTimeoutConfiguration = writeTimeoutConfiguration;
            return this;
        }

        public Builder connectionMaxIdleTimeoutConfiguration(
                final TimeoutConfiguration connectionMaxIdleTimeoutConfiguration)
        {
            this.connectionMaxIdleTimeoutConfiguration = connectionMaxIdleTimeoutConfiguration;
            return this;
        }

        public Builder connectionTimeToLiveConfiguration(final TimeUnitConfiguration connectionTimeToLiveConfiguration) {
            this.connectionTimeToLiveConfiguration = connectionTimeToLiveConfiguration;
            return this;
        }

        public Builder maxConcurrency(final Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public Builder maxPendingConnectionAcquires(final Integer maxPendingConnectionAcquires) {
            this.maxPendingConnectionAcquires = maxPendingConnectionAcquires;
            return this;
        }

        public Builder useIdleConnectionReaper(final Boolean useIdleConnectionReaper) {
            this.useIdleConnectionReaper = useIdleConnectionReaper;
            return this;
        }

        public Builder awsSdkProxyConfiguration(final AwsSdkProxyConfiguration awsSdkProxyConfiguration) {
            this.awsSdkProxyConfiguration = awsSdkProxyConfiguration;
            return this;
        }

        public Builder sslImplementation(final SslImplementation sslImplementation) {
            this.sslImplementation = sslImplementation;
            return this;
        }

        public Builder awsSdkTlsKeyManagersProviderConfiguration(
                final AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration)
        {
            this.awsSdkTlsKeyManagersProviderConfiguration = awsSdkTlsKeyManagersProviderConfiguration;
            return this;
        }

        public Builder tlsTrustManagersProviderInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
                        tlsTrustManagersProviderInjectedTypeConfiguration)
        {
            this.tlsTrustManagersProviderInjectedTypeConfiguration = tlsTrustManagersProviderInjectedTypeConfiguration;
            return this;
        }

        public DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration build() {
            return new DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
