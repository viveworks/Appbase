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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.AwsSdkEventLoopGroupConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.AwsSdkHttp2Configuration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.AwsSdkProtocol;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.AwsSdkProxyConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.tls.AwsSdkTlsKeyManagersProviderConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.net.ChannelOptionsConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslImplementation;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AwsSdkAsyncHttpClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkAsyncHttpClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <SdkAsyncHttpClient, TextValue, DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration>
        implements AwsSdkAsyncHttpClientIntegrationService
{
    private final AtomicReference<SdkAsyncHttpClient> sdkAsyncHttpClientRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
            defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SdkAsyncHttpClient, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final SdkAsyncHttpClient sdkAsyncHttpClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SdkAsyncHttpClient.class);

        AppbaseUtil.notNull(sdkAsyncHttpClient, "sdkAsyncHttpClient");

        this.sdkAsyncHttpClientRef = new AtomicReference<>(sdkAsyncHttpClient);
        this.defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration = null;
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SdkAsyncHttpClient, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                    defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SdkAsyncHttpClient.class);

        AppbaseUtil.notNull(
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                "defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration");

        this.defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
        this.sdkAsyncHttpClientRef = new AtomicReference<>();

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                "defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration");

        final NettyNioAsyncHttpClient.Builder nettyNioAsyncHttpClientBuilder = NettyNioAsyncHttpClient.builder();


        final AwsSdkProtocol awsSdkProtocol =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getAwsSdkProtocol();
        if (awsSdkProtocol != null) {
            nettyNioAsyncHttpClientBuilder.protocol(awsSdkProtocol.getProtocol());
        }


        final SslImplementation sslImplementation =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getSslImplementation();
        if (sslImplementation != null) {
            nettyNioAsyncHttpClientBuilder.sslProvider(sslImplementation.getSslProvider());
        }


        final TimeoutConfiguration connectionAcquisitionTimeoutConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getConnectionAcquisitionTimeoutConfiguration();
        if (connectionAcquisitionTimeoutConfiguration != null) {
            nettyNioAsyncHttpClientBuilder
                    .connectionAcquisitionTimeout(AppbaseClientUtil.duration(connectionAcquisitionTimeoutConfiguration));
        }


        final TimeoutConfiguration connectionMaxIdleTimeoutConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getConnectionMaxIdleTimeoutConfiguration();
        if (connectionMaxIdleTimeoutConfiguration != null) {
            nettyNioAsyncHttpClientBuilder
                    .connectionMaxIdleTime(AppbaseClientUtil.duration(connectionMaxIdleTimeoutConfiguration));
        }


        final TimeoutConfiguration connectionTimeoutConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getConnectionTimeoutConfiguration();
        if (connectionTimeoutConfiguration != null && connectionTimeoutConfiguration.isEnabled()) {
            nettyNioAsyncHttpClientBuilder.connectionTimeout(AppbaseClientUtil.duration(connectionTimeoutConfiguration));
        }


        final TimeUnitConfiguration connectionTimeToLiveConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getConnectionTimeToLiveConfiguration();
        if (connectionTimeToLiveConfiguration != null && connectionTimeToLiveConfiguration.isEnabled()) {
            nettyNioAsyncHttpClientBuilder
                    .connectionTimeToLive(AppbaseClientUtil.duration(connectionTimeToLiveConfiguration));
        }


        final TimeoutConfiguration readTimeoutConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getReadTimeoutConfiguration();
        if (readTimeoutConfiguration != null && readTimeoutConfiguration.isEnabled()) {
            nettyNioAsyncHttpClientBuilder.readTimeout(AppbaseClientUtil.duration(readTimeoutConfiguration));
        }


        final TimeoutConfiguration writeTimeoutConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getWriteTimeoutConfiguration();
        if (writeTimeoutConfiguration != null && writeTimeoutConfiguration.isEnabled()) {
            nettyNioAsyncHttpClientBuilder.writeTimeout(AppbaseClientUtil.duration(writeTimeoutConfiguration));
        }


        final Integer maxConcurrency =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getMaxConcurrency();
        if (maxConcurrency != null) {
            nettyNioAsyncHttpClientBuilder.maxConcurrency(maxConcurrency);
        }


        final Integer maxPendingConnectionAcquires =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getMaxPendingConnectionAcquires();
        if (maxPendingConnectionAcquires != null) {
            nettyNioAsyncHttpClientBuilder.maxPendingConnectionAcquires(maxPendingConnectionAcquires);
        }


        final Boolean useIdleConnectionReaper =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getUseIdleConnectionReaper();
        if (useIdleConnectionReaper != null) {
            nettyNioAsyncHttpClientBuilder.useIdleConnectionReaper(useIdleConnectionReaper);
        }


        final AwsSdkEventLoopGroupConfiguration awsSdkEventLoopGroupConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getAwsSdkEventLoopGroupConfiguration();
        if (awsSdkEventLoopGroupConfiguration != null) {
            final AwsSdkEventLoopGroupFactory awsSdkEventLoopGroupFactory =
                    new DefaultAwsSdkEventLoopGroupFactory(appbaseClientCore, awsSdkEventLoopGroupConfiguration);
            final SdkEventLoopGroup sdkEventLoopGroup = awsSdkEventLoopGroupFactory.getInstance();
            nettyNioAsyncHttpClientBuilder.eventLoopGroup(sdkEventLoopGroup);
        }


        final AwsSdkHttp2Configuration awsSdkHttp2Configuration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getAwsSdkHttp2Configuration();
        if (awsSdkHttp2Configuration != null) {
            final Http2Configuration http2Configuration = http2Configuration(awsSdkHttp2Configuration);
            nettyNioAsyncHttpClientBuilder.http2Configuration(http2Configuration);
        }


        final AwsSdkProxyConfiguration awsSdkProxyConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getAwsSdkProxyConfiguration();
        if (awsSdkProxyConfiguration != null) {
            final ProxyConfiguration proxyConfiguration = proxyConfiguration(awsSdkProxyConfiguration);
            nettyNioAsyncHttpClientBuilder.proxyConfiguration(proxyConfiguration);
        }


        final AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getAwsSdkTlsKeyManagersProviderConfiguration();
        if (awsSdkTlsKeyManagersProviderConfiguration != null) {
            final AwsSdkTlsKeyManagersProviderFactory awsSdkTlsKeyManagersProviderFactory =
                    new DefaultAwsSdkTlsKeyManagersProviderFactory(awsSdkTlsKeyManagersProviderConfiguration);
            final TlsKeyManagersProvider tlsKeyManagersProvider = awsSdkTlsKeyManagersProviderFactory.getInstance();
            nettyNioAsyncHttpClientBuilder.tlsKeyManagersProvider(tlsKeyManagersProvider);
        }


        final InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
                tlsTrustManagersProviderInjectedTypeConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                        .getTlsTrustManagersProviderInjectedTypeConfiguration();
        if (tlsTrustManagersProviderInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();
            final TlsTrustManagersProvider tlsTrustManagersProvider =
                    AppbaseUtil.injectedInstance(guiceInjectorService, tlsTrustManagersProviderInjectedTypeConfiguration);
            nettyNioAsyncHttpClientBuilder.tlsTrustManagersProvider(tlsTrustManagersProvider);
        }


        final ChannelOptionsConfiguration channelOptionsConfiguration =
                defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.getChannelOptionsConfiguration();
        if (channelOptionsConfiguration != null) {
            initChannelOptions(nettyNioAsyncHttpClientBuilder, channelOptionsConfiguration);
        }


        final SdkAsyncHttpClient sdkAsyncHttpClient = nettyNioAsyncHttpClientBuilder.build();
        this.sdkAsyncHttpClientRef.set(sdkAsyncHttpClient);
    }

    /**
     * Builds the AWS SDK proxy configuration from the supplied client configuration.
     *
     * @return the AWS SDK proxy configuration
     */
    public ProxyConfiguration proxyConfiguration(
            final AwsSdkProxyConfiguration awsSdkProxyConfiguration)
    {
        AppbaseUtil.notNull(awsSdkProxyConfiguration, "awsSdkProxyConfiguration");

        final ProxyConfiguration.Builder builder = ProxyConfiguration.builder();

        final String host = awsSdkProxyConfiguration.getHost();
        builder.host(host);

        final int port = awsSdkProxyConfiguration.getPort();
        builder.port(port);

        final String scheme = awsSdkProxyConfiguration.getScheme();
        if (scheme != null) {
            builder.scheme(scheme);
        }

        final Set<String> nonProxyHosts = awsSdkProxyConfiguration.getNonProxyHosts();
        if (nonProxyHosts != null) {
            builder.nonProxyHosts(nonProxyHosts);
        }

        return builder.build();
    }

    private void initChannelOptions(
            final NettyNioAsyncHttpClient.Builder nettyNioAsyncHttpClientBuilder,
            final ChannelOptionsConfiguration channelOptionsConfiguration)
    {
        nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);


        final Boolean autoClose = channelOptionsConfiguration.getAutoClose();
        if (autoClose != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.AUTO_CLOSE, autoClose);
        }


        final Boolean autoRead = channelOptionsConfiguration.getAutoRead();
        if (autoRead != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.AUTO_READ, autoRead);
        }


        final Integer connectTimeoutMillis = channelOptionsConfiguration.getConnectTimeoutMillis();
        if (connectTimeoutMillis != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
        }


        final Boolean singleEventExecutorPerGroup = channelOptionsConfiguration.getSingleEventExecutorPerGroup();
        if (singleEventExecutorPerGroup != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(
                    ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP, singleEventExecutorPerGroup);
        }


        final Integer socketBacklog = channelOptionsConfiguration.getSocketBacklog();
        if (socketBacklog != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_BACKLOG, socketBacklog);
        }


        final Boolean socketBroadcast = channelOptionsConfiguration.getSocketBroadcast();
        if (socketBroadcast != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_BROADCAST, socketBroadcast);
        }


        final Boolean socketKeepAlive = channelOptionsConfiguration.getSocketKeepAlive();
        if (socketKeepAlive != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_KEEPALIVE, socketKeepAlive);
        }


        final Integer socketLingerSeconds = channelOptionsConfiguration.getSocketLingerSeconds();
        if (socketLingerSeconds != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_LINGER, socketLingerSeconds);
        }


        final Integer socketReceiveBufferBytes = channelOptionsConfiguration.getSocketReceiveBufferBytes();
        if (socketReceiveBufferBytes != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_RCVBUF, socketReceiveBufferBytes);
        }


        final Integer socketSendBufferBytes = channelOptionsConfiguration.getSocketSendBufferBytes();
        if (socketSendBufferBytes != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_SNDBUF, socketSendBufferBytes);
        }


        final TimeoutConfiguration socketTimeoutConfiguration = channelOptionsConfiguration.getSocketTimeoutConfiguration();
        if (socketTimeoutConfiguration != null && socketTimeoutConfiguration.isEnabled()) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(
                    ChannelOption.SO_TIMEOUT,
                    (int) socketTimeoutConfiguration.getTimeoutTimeUnit().toMillis(socketTimeoutConfiguration.getTimeout()));
        }


        final Boolean socketReuseAddress = channelOptionsConfiguration.getSocketReuseAddress();
        if (socketReuseAddress != null) {
            nettyNioAsyncHttpClientBuilder.putChannelOption(ChannelOption.SO_REUSEADDR, socketReuseAddress);
        }

        super.postInit();
    }

    private Http2Configuration http2Configuration(final AwsSdkHttp2Configuration awsSdkHttp2Configuration) {
        AppbaseUtil.notNull(awsSdkHttp2Configuration, "awsSdkHttp2Configuration");

        final Http2Configuration.Builder builder = Http2Configuration.builder();

        final Duration healthCheckPingPeriod = awsSdkHttp2Configuration.getHealthCheckPingPeriod();
        if (healthCheckPingPeriod != null) {
            builder.healthCheckPingPeriod(healthCheckPingPeriod);
        }

        final Integer initialWindowSize = awsSdkHttp2Configuration.getInitialWindowSize();
        if (initialWindowSize != null) {
            builder.initialWindowSize(initialWindowSize);
        }

        final Long maxStreams = awsSdkHttp2Configuration.getMaxStreams();
        if (maxStreams != null) {
            builder.maxStreams(maxStreams);
        }

        return builder.build();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final SdkAsyncHttpClient sdkAsyncHttpClient = getClient();
        if (sdkAsyncHttpClient != null) {
            if (!defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                    .getAwsSdkEventLoopGroupConfiguration()
                    .isUseSharedEventLoopGroup())
            {
                sdkAsyncHttpClient.close();
            }
        }

        sdkAsyncHttpClientRef.set(null);
    }

    @Override
    public SdkAsyncHttpClient getClient() {
        return sdkAsyncHttpClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final SdkAsyncHttpClient sdkAsyncHttpClient = getClient();
        return sdkAsyncHttpClient != null;
    }
}
