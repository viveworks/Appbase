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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.vitess.configuration.ChannelOptionsFactory;
import com.viveworks.appbase.client.service.vitess.configuration.ClientInterceptorListFactory;
import com.viveworks.appbase.client.service.vitess.configuration.CustomGrpcClientFactoryConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.Http2NegotiationType;
import com.viveworks.appbase.client.service.vitess.configuration.NettyChannelConfiguration;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.net.ChannelOptionsConfiguration;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupFactory;
import com.viveworks.appbase.core.util.net.ssl.SslClientContextConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslClientContextFactory;
import io.grpc.BinaryLog;
import io.grpc.ClientInterceptor;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ProxyDetector;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.vitess.client.grpc.GrpcClientFactory;
import io.vitess.client.grpc.RetryingInterceptorConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Customized implementation of {@link io.vitess.client.grpc.GrpcClientFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomGrpcClientFactory extends GrpcClientFactory {
    private final Logger logger;
    private final AppbaseClientCore appbaseClientCore;
    private final CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration;

    public CustomGrpcClientFactory(
            final AppbaseClientCore appbaseClientCore,
            final CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(customGrpcClientFactoryConfiguration, "customGrpcClientFactoryConfiguration");

        this.logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.appbaseClientCore = appbaseClientCore;
        this.customGrpcClientFactoryConfiguration = customGrpcClientFactoryConfiguration;
    }

    public CustomGrpcClientFactory(
            final AppbaseClientCore appbaseClientCore,
            final CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration,
            final RetryingInterceptorConfig retryingInterceptorConfig,
            final boolean useTracing)
    {
        super(retryingInterceptorConfig, useTracing);

        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(customGrpcClientFactoryConfiguration, "customGrpcClientFactoryConfiguration");

        this.logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.appbaseClientCore = appbaseClientCore;
        this.customGrpcClientFactoryConfiguration = customGrpcClientFactoryConfiguration;
    }

    private void initChannelOptions(
            final ChannelOptionsConfiguration channelOptionsConfiguration,
            final NettyChannelBuilder nettyChannelBuilder)
    {
        AppbaseUtil.notNull(channelOptionsConfiguration, "channelOptionsConfiguration");
        AppbaseUtil.notNull(nettyChannelBuilder, "nettyChannelBuilder");

        final Boolean autoClose = channelOptionsConfiguration.getAutoClose();
        if (autoClose != null) {
            nettyChannelBuilder.withOption(ChannelOption.AUTO_CLOSE, autoClose);
        }

        final Boolean autoRead = channelOptionsConfiguration.getAutoRead();
        if (autoRead != null) {
            nettyChannelBuilder.withOption(ChannelOption.AUTO_READ, autoRead);
        }

        final Integer connectTimeoutMillis = channelOptionsConfiguration.getConnectTimeoutMillis();
        if (connectTimeoutMillis != null) {
            nettyChannelBuilder.withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
        }

        final Boolean singleEventExecutorPerGroup = channelOptionsConfiguration.getSingleEventExecutorPerGroup();
        if (singleEventExecutorPerGroup != null) {
            nettyChannelBuilder.withOption(ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP, singleEventExecutorPerGroup);
        }

        final Integer socketBacklog = channelOptionsConfiguration.getSocketBacklog();
        if (socketBacklog != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_BACKLOG, socketBacklog);
        }

        final Boolean socketBroadcast = channelOptionsConfiguration.getSocketBroadcast();
        if (socketBroadcast != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_BROADCAST, socketBroadcast);
        }

        final Boolean socketKeepAlive = channelOptionsConfiguration.getSocketKeepAlive();
        if (socketKeepAlive != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_KEEPALIVE, socketKeepAlive);
        }

        final Integer socketLingerSeconds = channelOptionsConfiguration.getSocketLingerSeconds();
        if (socketLingerSeconds != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_LINGER, socketLingerSeconds);
        }

        final Integer socketReceiveBufferBytes = channelOptionsConfiguration.getSocketReceiveBufferBytes();
        if (socketReceiveBufferBytes != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_RCVBUF, socketReceiveBufferBytes);
        }

        final Boolean socketReuseAddress = channelOptionsConfiguration.getSocketReuseAddress();
        if (socketReuseAddress != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_REUSEADDR, socketReuseAddress);
        }

        final Integer socketSendBufferBytes = channelOptionsConfiguration.getSocketSendBufferBytes();
        if (socketSendBufferBytes != null) {
            nettyChannelBuilder.withOption(ChannelOption.SO_SNDBUF, socketSendBufferBytes);
        }

        final TimeoutConfiguration socketTimeoutConfiguration = channelOptionsConfiguration.getSocketTimeoutConfiguration();
        if (socketTimeoutConfiguration != null) {
            final long timeout = socketTimeoutConfiguration.getTimeout();
            final TimeUnit timeoutTimeUnit = socketTimeoutConfiguration.getTimeoutTimeUnit();
            nettyChannelBuilder.withOption(ChannelOption.SO_TIMEOUT, (int) timeoutTimeUnit.toMillis(timeout));
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:MethodLength"})
    protected NettyChannelBuilder channelBuilder(final String target) {
        final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();

        final NettyChannelConfiguration nettyChannelConfiguration =
                customGrpcClientFactoryConfiguration.getNettyChannelConfiguration();

        final HostPortConfiguration hostPortConfiguration = nettyChannelConfiguration.getHostPortConfiguration();
        final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                .forAddress(hostPortConfiguration.getHost(), hostPortConfiguration.getPort());

        final boolean useSharedEventLoopGroup = nettyChannelConfiguration.isUseSharedEventLoopGroup();
        final EventLoopGroupConfiguration eventLoopGroupConfiguration =
                nettyChannelConfiguration.getEventLoopGroupConfiguration();


        final InjectedTypeConfiguration<? extends ChannelFactory<? extends Channel>>
                channelFactoryInjectedTypeConfiguration =
                nettyChannelConfiguration.getChannelFactoryInjectedTypeConfiguration();
        final Class<? extends Channel> channelType = nettyChannelConfiguration.getChannelType();
        if (channelFactoryInjectedTypeConfiguration != null) {
            final ChannelFactory<? extends Channel> channelFactory =
                    AppbaseUtil.injectedInstance(guiceInjectorService, channelFactoryInjectedTypeConfiguration);
            nettyChannelBuilder.channelFactory(channelFactory);
        } else if (channelType != null && !useSharedEventLoopGroup && eventLoopGroupConfiguration != null) {
            // NettyChannelBuilder requires channelType and eventLoopGroup as a pair. The historical
            // shared group was removed, so the shared/default path must leave both unset and let gRPC
            // select its transport. A configured private event-loop group still receives its matching
            // channel type below.
            nettyChannelBuilder.channelType(channelType);
        }


        final InjectedTypeConfiguration<? extends ChannelOptionsFactory> channelOptionsFactoryInjectedTypeConfiguration =
                nettyChannelConfiguration.getChannelOptionsFactoryInjectedTypeConfiguration();
        final ChannelOptionsConfiguration channelOptionsConfiguration =
                nettyChannelConfiguration.getChannelOptionsConfiguration();
        if (channelOptionsFactoryInjectedTypeConfiguration != null) {
            final ChannelOptionsFactory channelOptionsFactory =
                    AppbaseUtil.injectedInstance(guiceInjectorService, channelOptionsFactoryInjectedTypeConfiguration);
            final Map<ChannelOption<?>, Object> channelOptions = channelOptionsFactory.getInstance();
            channelOptions.forEach((channelOption, value) -> {
                final ChannelOption<Object> castedChannelOption = (ChannelOption<Object>) channelOption;
                nettyChannelBuilder.withOption(castedChannelOption, value);
            });
        } else if (channelOptionsConfiguration != null) {
            initChannelOptions(channelOptionsConfiguration, nettyChannelBuilder);
        }


        if (useSharedEventLoopGroup) {
            // shared event loop group removed; skip setting one (gRPC will use its default)
        } else if (eventLoopGroupConfiguration != null) {
            final EventLoopGroupFactory sharedEventLoopGroupFactory = new EventLoopGroupFactory(
                    appbaseClientCore.getAppbaseCore(),
                    eventLoopGroupConfiguration);
            final EventLoopGroup eventLoopGroup = sharedEventLoopGroupFactory.getInstance();
            nettyChannelBuilder.eventLoopGroup(eventLoopGroup);
        }


        final boolean useDirectExecutor = nettyChannelConfiguration.isUseDirectExecutor();
        if (useDirectExecutor) {
            nettyChannelBuilder.directExecutor();
        }


        final boolean useSharedExecutorServiceConfiguration =
                nettyChannelConfiguration.isUseSharedExecutorServiceConfiguration();
        final ExecutorServiceConfiguration executorServiceConfiguration =
                nettyChannelConfiguration.getExecutorServiceConfiguration();
        if (useSharedExecutorServiceConfiguration) {
            final ExecutorService sharedExecutorService = appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .getSharedExecutorService();
            nettyChannelBuilder.executor(sharedExecutorService);
        } else if (executorServiceConfiguration != null) {
            final ExecutorService executorService = ExecutorServiceFactory.newExecutorService(
                    executorServiceConfiguration,
                    (t, e) -> logger.error("Uncaught exception in thread {}", t.getName(), e));
            nettyChannelBuilder.executor(executorService);
        }


        final boolean useSharedOffloadExecutorServiceConfiguration =
                nettyChannelConfiguration.isUseSharedOffloadExecutorServiceConfiguration();
        final ExecutorServiceConfiguration offloadExecutorServiceConfiguration =
                nettyChannelConfiguration.getOffloadExecutorServiceConfiguration();
        if (useSharedOffloadExecutorServiceConfiguration) {
            final ExecutorService sharedExecutorService = appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .getSharedExecutorService();
            nettyChannelBuilder.offloadExecutor(sharedExecutorService);
        } else if (offloadExecutorServiceConfiguration != null) {
            final ExecutorService executorService = ExecutorServiceFactory.newExecutorService(
                    offloadExecutorServiceConfiguration,
                    (t, e) -> logger.error("Uncaught exception in thread {}", t.getName(), e));
            nettyChannelBuilder.executor(executorService);
        }


        final Integer flowControlWindow = nettyChannelConfiguration.getFlowControlWindow();
        if (flowControlWindow != null) {
            nettyChannelBuilder.flowControlWindow(flowControlWindow);
        }


        final Integer initialFlowControlWindow = nettyChannelConfiguration.getInitialFlowControlWindow();
        if (initialFlowControlWindow != null) {
            nettyChannelBuilder.initialFlowControlWindow(initialFlowControlWindow);
        }


        final Integer maxInboundMessageSize = nettyChannelConfiguration.getMaxInboundMessageSize();
        if (maxInboundMessageSize != null) {
            nettyChannelBuilder.maxInboundMessageSize(maxInboundMessageSize);
        }


        final Integer maxInboundMetadataSize = nettyChannelConfiguration.getMaxInboundMetadataSize();
        if (maxInboundMetadataSize != null) {
            nettyChannelBuilder.maxInboundMetadataSize(maxInboundMetadataSize);
        }


        final Integer maxHedgedAttempts = nettyChannelConfiguration.getMaxHedgedAttempts();
        if (maxHedgedAttempts != null) {
            nettyChannelBuilder.maxHedgedAttempts(maxHedgedAttempts);
        }


        final Integer maxRetryAttempts = nettyChannelConfiguration.getMaxRetryAttempts();
        if (maxRetryAttempts != null) {
            nettyChannelBuilder.maxRetryAttempts(maxRetryAttempts);
        }


        final Integer maxTraceEvents = nettyChannelConfiguration.getMaxTraceEvents();
        if (maxTraceEvents != null) {
            nettyChannelBuilder.maxTraceEvents(maxTraceEvents);
        }


        final Integer perRpcBufferLimit = nettyChannelConfiguration.getPerRpcBufferLimit();
        if (perRpcBufferLimit != null) {
            nettyChannelBuilder.perRpcBufferLimit(perRpcBufferLimit);
        }


        final DataSizeConfiguration retryBufferSizeConfiguration =
                nettyChannelConfiguration.getRetryBufferSizeConfiguration();
        if (retryBufferSizeConfiguration != null && retryBufferSizeConfiguration.isEnabled()) {
            final int dataSize = retryBufferSizeConfiguration.getDataSize();
            final DataSizeConfiguration.Unit unit = retryBufferSizeConfiguration.getUnit();
            nettyChannelBuilder.retryBufferSize(unit.toBytes(dataSize));
        }


        final TimeUnitConfiguration keepAliveTimeConfiguration = nettyChannelConfiguration.getKeepAliveTimeConfiguration();
        if (keepAliveTimeConfiguration != null && keepAliveTimeConfiguration.isEnabled()) {
            nettyChannelBuilder.keepAliveTime(
                    keepAliveTimeConfiguration.getTime(), keepAliveTimeConfiguration.getTimeUnit());
        }


        final TimeoutConfiguration keepAliveTimeoutConfiguration =
                nettyChannelConfiguration.getKeepAliveTimeoutConfiguration();
        if (keepAliveTimeoutConfiguration != null && keepAliveTimeoutConfiguration.isEnabled()) {
            nettyChannelBuilder.keepAliveTimeout(
                    keepAliveTimeoutConfiguration.getTimeout(), keepAliveTimeoutConfiguration.getTimeoutTimeUnit());
        }


        final TimeoutConfiguration idleTimeoutConfiguration = nettyChannelConfiguration.getIdleTimeoutConfiguration();
        if (idleTimeoutConfiguration != null && idleTimeoutConfiguration.isEnabled()) {
            nettyChannelBuilder.idleTimeout(
                    idleTimeoutConfiguration.getTimeout(), idleTimeoutConfiguration.getTimeoutTimeUnit());
        }


        final String userAgent = nettyChannelConfiguration.getUserAgent();
        if (userAgent != null) {
            nettyChannelBuilder.userAgent(userAgent);
        }


        final InjectedTypeConfiguration<? extends ProxyDetector> proxyDetectorInjectedTypeConfiguration =
                nettyChannelConfiguration.getProxyDetectorInjectedTypeConfiguration();
        if (proxyDetectorInjectedTypeConfiguration != null) {
            final ProxyDetector proxyDetector =
                    AppbaseUtil.injectedInstance(guiceInjectorService, proxyDetectorInjectedTypeConfiguration);
            nettyChannelBuilder.proxyDetector(proxyDetector);
        }


        final InjectedTypeConfiguration<? extends NettyChannelBuilder.LocalSocketPicker>
                localSocketPairInjectedTypeConfiguration =
                nettyChannelConfiguration.getLocalSocketPairInjectedTypeConfiguration();
        if (localSocketPairInjectedTypeConfiguration != null) {
            final NettyChannelBuilder.LocalSocketPicker localSocketPicker =
                    AppbaseUtil.injectedInstance(guiceInjectorService, localSocketPairInjectedTypeConfiguration);
            nettyChannelBuilder.localSocketPicker(localSocketPicker);
        }


        final String defaultLoadBalancingPolicy = nettyChannelConfiguration.getDefaultLoadBalancingPolicy();
        if (defaultLoadBalancingPolicy != null) {
            nettyChannelBuilder.defaultLoadBalancingPolicy(defaultLoadBalancingPolicy);
        }


        final InjectedTypeConfiguration<? extends CompressorRegistry> compressorRegistryInjectedTypeConfiguration =
                nettyChannelConfiguration.getCompressorRegistryInjectedTypeConfiguration();
        if (compressorRegistryInjectedTypeConfiguration != null) {
            final CompressorRegistry compressorRegistry =
                    AppbaseUtil.injectedInstance(guiceInjectorService, compressorRegistryInjectedTypeConfiguration);
            nettyChannelBuilder.compressorRegistry(compressorRegistry);
        }


        final InjectedTypeConfiguration<? extends DecompressorRegistry> decompressorRegistryInjectedTypeConfiguration =
                nettyChannelConfiguration.getDecompressorRegistryInjectedTypeConfiguration();
        if (decompressorRegistryInjectedTypeConfiguration != null) {
            final DecompressorRegistry decompressorRegistry =
                    AppbaseUtil.injectedInstance(guiceInjectorService, decompressorRegistryInjectedTypeConfiguration);
            nettyChannelBuilder.decompressorRegistry(decompressorRegistry);
        }


        final Http2NegotiationType http2NegotiationType = nettyChannelConfiguration.getHttp2NegotiationType();
        if (http2NegotiationType != null) {
            nettyChannelBuilder.negotiationType(http2NegotiationType.getNegotiationType());
        }


        final String overrideAuthority = nettyChannelConfiguration.getOverrideAuthority();
        if (overrideAuthority != null) {
            nettyChannelBuilder.overrideAuthority(overrideAuthority);
        }


        final SslClientContextConfiguration sslClientContextConfiguration =
                nettyChannelConfiguration.getSslClientContextConfiguration();
        if (sslClientContextConfiguration != null) {
            final SslClientContextFactory sslClientContextFactory =
                    new SslClientContextFactory(sslClientContextConfiguration);
            final SslContext sslContext = sslClientContextFactory.getInstance();
            nettyChannelBuilder.sslContext(sslContext);
        }


        final InjectedTypeConfiguration<? extends ClientInterceptorListFactory>
                clientInterceptorListFactoryInjectedTypeConfiguration =
                nettyChannelConfiguration.getClientInterceptorListFactoryInjectedTypeConfiguration();
        if (clientInterceptorListFactoryInjectedTypeConfiguration != null) {
            final ClientInterceptorListFactory clientInterceptorListFactory =
                    AppbaseUtil.injectedInstance(
                            guiceInjectorService, clientInterceptorListFactoryInjectedTypeConfiguration);
            final List<ClientInterceptor> clientInterceptors = clientInterceptorListFactory.getInstance();
            nettyChannelBuilder.intercept(clientInterceptors);
        }


        final InjectedTypeConfiguration<? extends BinaryLog> binaryLogInjectedTypeConfiguration =
                nettyChannelConfiguration.getBinaryLogInjectedTypeConfiguration();
        if (binaryLogInjectedTypeConfiguration != null) {
            final BinaryLog binaryLog =
                    AppbaseUtil.injectedInstance(guiceInjectorService, binaryLogInjectedTypeConfiguration);
            nettyChannelBuilder.setBinaryLog(binaryLog);
        }

        return nettyChannelBuilder;
    }
}
