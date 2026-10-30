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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.ChannelOptionsConfiguration;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslClientContextConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.BinaryLog;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ProxyDetector;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration to use with {@link NettyChannelBuilder}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = NettyChannelConfiguration.Builder.class)
public final class NettyChannelConfiguration {
    private final HostPortConfiguration hostPortConfiguration;

    private final InjectedTypeConfiguration<? extends ChannelFactory<? extends Channel>>
            channelFactoryInjectedTypeConfiguration;
    private final Class<? extends Channel> channelType;
    private final ChannelOptionsConfiguration channelOptionsConfiguration;
    private final InjectedTypeConfiguration<? extends ChannelOptionsFactory>
            channelOptionsFactoryInjectedTypeConfiguration;

    private final boolean useSharedEventLoopGroup;
    private final EventLoopGroupConfiguration eventLoopGroupConfiguration;

    private final boolean useDirectExecutor;
    private final boolean useSharedExecutorServiceConfiguration;
    private final ExecutorServiceConfiguration executorServiceConfiguration;
    private final boolean useSharedOffloadExecutorServiceConfiguration;
    private final ExecutorServiceConfiguration offloadExecutorServiceConfiguration;

    private final Integer flowControlWindow;
    private final Integer initialFlowControlWindow;
    private final Integer maxInboundMessageSize;
    private final Integer maxInboundMetadataSize;
    private final Integer maxHedgedAttempts;
    private final Integer maxRetryAttempts;
    private final Integer maxTraceEvents;
    private final Integer perRpcBufferLimit;
    private final DataSizeConfiguration retryBufferSizeConfiguration;

    private final Boolean keepAliveWithoutCalls;
    private final Boolean enableRetry;
    private final Boolean enableServiceConfigLookUp;
    private final Boolean enableFullStreamDecompression;

    private final TimeUnitConfiguration keepAliveTimeConfiguration;
    private final TimeoutConfiguration keepAliveTimeoutConfiguration;
    private final TimeoutConfiguration idleTimeoutConfiguration;

    private final String userAgent;
    private final InjectedTypeConfiguration<? extends ProxyDetector>
            proxyDetectorInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends NettyChannelBuilder.LocalSocketPicker>
            localSocketPairInjectedTypeConfiguration;
    private final String defaultLoadBalancingPolicy;

    private final InjectedTypeConfiguration<? extends CompressorRegistry> compressorRegistryInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends DecompressorRegistry> decompressorRegistryInjectedTypeConfiguration;

    private final Http2NegotiationType http2NegotiationType;
    private final String overrideAuthority;
    private final SslClientContextConfiguration sslClientContextConfiguration;

    private final InjectedTypeConfiguration<? extends ClientInterceptorListFactory>
            clientInterceptorListFactoryInjectedTypeConfiguration;
    private final InjectedTypeConfiguration<? extends BinaryLog> binaryLogInjectedTypeConfiguration;



    private NettyChannelConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.hostPortConfiguration, "builder.hostPortConfiguration");

        hostPortConfiguration = builder.hostPortConfiguration;
        channelFactoryInjectedTypeConfiguration = builder.channelFactoryInjectedTypeConfiguration;
        channelType = builder.channelType;
        channelOptionsConfiguration = builder.channelOptionsConfiguration;
        channelOptionsFactoryInjectedTypeConfiguration = builder.channelOptionsFactoryInjectedTypeConfiguration;

        useSharedEventLoopGroup = builder.useSharedEventLoopGroup;
        eventLoopGroupConfiguration = builder.eventLoopGroupConfiguration;

        useDirectExecutor = builder.useDirectExecutor;
        useSharedExecutorServiceConfiguration = builder.useSharedExecutorServiceConfiguration;
        executorServiceConfiguration = builder.executorServiceConfiguration;
        useSharedOffloadExecutorServiceConfiguration = builder.useSharedOffloadExecutorServiceConfiguration;
        offloadExecutorServiceConfiguration = builder.offloadExecutorServiceConfiguration;

        flowControlWindow = builder.flowControlWindow;
        initialFlowControlWindow = builder.initialFlowControlWindow;
        maxInboundMessageSize = builder.maxInboundMessageSize;
        maxInboundMetadataSize = builder.maxInboundMetadataSize;
        maxHedgedAttempts = builder.maxHedgedAttempts;
        maxRetryAttempts = builder.maxRetryAttempts;
        maxTraceEvents = builder.maxTraceEvents;
        perRpcBufferLimit = builder.perRpcBufferLimit;
        retryBufferSizeConfiguration = builder.retryBufferSizeConfiguration;

        keepAliveWithoutCalls = builder.keepAliveWithoutCalls;
        enableRetry = builder.enableRetry;
        enableServiceConfigLookUp = builder.enableServiceConfigLookUp;
        enableFullStreamDecompression = builder.enableFullStreamDecompression;

        keepAliveTimeConfiguration = builder.keepAliveTimeConfiguration;
        keepAliveTimeoutConfiguration = builder.keepAliveTimeoutConfiguration;
        idleTimeoutConfiguration = builder.idleTimeoutConfiguration;

        userAgent = builder.userAgent;
        proxyDetectorInjectedTypeConfiguration = builder.proxyDetectorInjectedTypeConfiguration;
        localSocketPairInjectedTypeConfiguration = builder.localSocketPairInjectedTypeConfiguration;
        defaultLoadBalancingPolicy = builder.defaultLoadBalancingPolicy;

        compressorRegistryInjectedTypeConfiguration = builder.compressorRegistryInjectedTypeConfiguration;
        decompressorRegistryInjectedTypeConfiguration = builder.decompressorRegistryInjectedTypeConfiguration;

        http2NegotiationType = builder.http2NegotiationType;
        overrideAuthority = builder.overrideAuthority;
        sslClientContextConfiguration = builder.sslClientContextConfiguration;

        clientInterceptorListFactoryInjectedTypeConfiguration =
                builder.clientInterceptorListFactoryInjectedTypeConfiguration;
        binaryLogInjectedTypeConfiguration = builder.binaryLogInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public HostPortConfiguration getHostPortConfiguration() {
        return hostPortConfiguration;
    }

    public InjectedTypeConfiguration<? extends ChannelFactory<? extends Channel>>
    getChannelFactoryInjectedTypeConfiguration()
    {
        return channelFactoryInjectedTypeConfiguration;
    }

    public Class<? extends Channel> getChannelType() {
        return channelType;
    }

    public ChannelOptionsConfiguration getChannelOptionsConfiguration() {
        return channelOptionsConfiguration;
    }

    public InjectedTypeConfiguration<? extends ChannelOptionsFactory> getChannelOptionsFactoryInjectedTypeConfiguration() {
        return channelOptionsFactoryInjectedTypeConfiguration;
    }

    public boolean isUseSharedEventLoopGroup() {
        return useSharedEventLoopGroup;
    }

    public EventLoopGroupConfiguration getEventLoopGroupConfiguration() {
        return eventLoopGroupConfiguration;
    }

    public boolean isUseDirectExecutor() {
        return useDirectExecutor;
    }

    public boolean isUseSharedExecutorServiceConfiguration() {
        return useSharedExecutorServiceConfiguration;
    }

    public ExecutorServiceConfiguration getExecutorServiceConfiguration() {
        return executorServiceConfiguration;
    }

    public boolean isUseSharedOffloadExecutorServiceConfiguration() {
        return useSharedOffloadExecutorServiceConfiguration;
    }

    public ExecutorServiceConfiguration getOffloadExecutorServiceConfiguration() {
        return offloadExecutorServiceConfiguration;
    }

    public Integer getFlowControlWindow() {
        return flowControlWindow;
    }

    public Integer getInitialFlowControlWindow() {
        return initialFlowControlWindow;
    }

    public Integer getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public Integer getMaxInboundMetadataSize() {
        return maxInboundMetadataSize;
    }

    public Integer getMaxHedgedAttempts() {
        return maxHedgedAttempts;
    }

    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public Integer getMaxTraceEvents() {
        return maxTraceEvents;
    }

    public Integer getPerRpcBufferLimit() {
        return perRpcBufferLimit;
    }

    public DataSizeConfiguration getRetryBufferSizeConfiguration() {
        return retryBufferSizeConfiguration;
    }

    public Boolean getKeepAliveWithoutCalls() {
        return keepAliveWithoutCalls;
    }

    public Boolean getEnableRetry() {
        return enableRetry;
    }

    public Boolean getEnableServiceConfigLookUp() {
        return enableServiceConfigLookUp;
    }

    public Boolean getEnableFullStreamDecompression() {
        return enableFullStreamDecompression;
    }

    public TimeUnitConfiguration getKeepAliveTimeConfiguration() {
        return keepAliveTimeConfiguration;
    }

    public TimeoutConfiguration getKeepAliveTimeoutConfiguration() {
        return keepAliveTimeoutConfiguration;
    }

    public TimeoutConfiguration getIdleTimeoutConfiguration() {
        return idleTimeoutConfiguration;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public InjectedTypeConfiguration<? extends ProxyDetector> getProxyDetectorInjectedTypeConfiguration() {
        return proxyDetectorInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<? extends NettyChannelBuilder.LocalSocketPicker>
    getLocalSocketPairInjectedTypeConfiguration()
    {
        return localSocketPairInjectedTypeConfiguration;
    }

    public String getDefaultLoadBalancingPolicy() {
        return defaultLoadBalancingPolicy;
    }

    public InjectedTypeConfiguration<? extends CompressorRegistry> getCompressorRegistryInjectedTypeConfiguration() {
        return compressorRegistryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<? extends DecompressorRegistry> getDecompressorRegistryInjectedTypeConfiguration() {
        return decompressorRegistryInjectedTypeConfiguration;
    }

    public Http2NegotiationType getHttp2NegotiationType() {
        return http2NegotiationType;
    }

    public String getOverrideAuthority() {
        return overrideAuthority;
    }

    public SslClientContextConfiguration getSslClientContextConfiguration() {
        return sslClientContextConfiguration;
    }

    public InjectedTypeConfiguration<? extends ClientInterceptorListFactory>
    getClientInterceptorListFactoryInjectedTypeConfiguration()
    {
        return clientInterceptorListFactoryInjectedTypeConfiguration;
    }

    public InjectedTypeConfiguration<? extends BinaryLog> getBinaryLogInjectedTypeConfiguration() {
        return binaryLogInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NettyChannelConfiguration that = (NettyChannelConfiguration) o;
        return useSharedEventLoopGroup == that.useSharedEventLoopGroup &&
                useDirectExecutor == that.useDirectExecutor &&
                useSharedExecutorServiceConfiguration == that.useSharedExecutorServiceConfiguration &&
                useSharedOffloadExecutorServiceConfiguration == that.useSharedOffloadExecutorServiceConfiguration &&
                Objects.equals(hostPortConfiguration, that.hostPortConfiguration) &&
                Objects.equals(channelFactoryInjectedTypeConfiguration, that.channelFactoryInjectedTypeConfiguration) &&
                Objects.equals(channelType, that.channelType) &&
                Objects.equals(channelOptionsConfiguration, that.channelOptionsConfiguration) &&
                Objects.equals(
                        channelOptionsFactoryInjectedTypeConfiguration,
                        that.channelOptionsFactoryInjectedTypeConfiguration) &&
                Objects.equals(eventLoopGroupConfiguration, that.eventLoopGroupConfiguration) &&
                Objects.equals(executorServiceConfiguration, that.executorServiceConfiguration) &&
                Objects.equals(offloadExecutorServiceConfiguration, that.offloadExecutorServiceConfiguration) &&
                Objects.equals(flowControlWindow, that.flowControlWindow) &&
                Objects.equals(initialFlowControlWindow, that.initialFlowControlWindow) &&
                Objects.equals(maxInboundMessageSize, that.maxInboundMessageSize) &&
                Objects.equals(maxInboundMetadataSize, that.maxInboundMetadataSize) &&
                Objects.equals(maxHedgedAttempts, that.maxHedgedAttempts) &&
                Objects.equals(maxRetryAttempts, that.maxRetryAttempts) &&
                Objects.equals(maxTraceEvents, that.maxTraceEvents) &&
                Objects.equals(perRpcBufferLimit, that.perRpcBufferLimit) &&
                Objects.equals(retryBufferSizeConfiguration, that.retryBufferSizeConfiguration) &&
                Objects.equals(keepAliveWithoutCalls, that.keepAliveWithoutCalls) &&
                Objects.equals(enableRetry, that.enableRetry) &&
                Objects.equals(enableServiceConfigLookUp, that.enableServiceConfigLookUp) &&
                Objects.equals(enableFullStreamDecompression, that.enableFullStreamDecompression) &&
                Objects.equals(keepAliveTimeConfiguration, that.keepAliveTimeConfiguration) &&
                Objects.equals(keepAliveTimeoutConfiguration, that.keepAliveTimeoutConfiguration) &&
                Objects.equals(idleTimeoutConfiguration, that.idleTimeoutConfiguration) &&
                Objects.equals(userAgent, that.userAgent) &&
                Objects.equals(proxyDetectorInjectedTypeConfiguration, that.proxyDetectorInjectedTypeConfiguration) &&
                Objects.equals(localSocketPairInjectedTypeConfiguration, that.localSocketPairInjectedTypeConfiguration) &&
                Objects.equals(defaultLoadBalancingPolicy, that.defaultLoadBalancingPolicy) &&
                Objects.equals(
                        compressorRegistryInjectedTypeConfiguration, that.compressorRegistryInjectedTypeConfiguration) &&
                Objects.equals(
                        decompressorRegistryInjectedTypeConfiguration, that.decompressorRegistryInjectedTypeConfiguration) &&
                http2NegotiationType == that.http2NegotiationType &&
                Objects.equals(overrideAuthority, that.overrideAuthority) &&
                Objects.equals(sslClientContextConfiguration, that.sslClientContextConfiguration) &&
                Objects.equals(
                        clientInterceptorListFactoryInjectedTypeConfiguration,
                        that.clientInterceptorListFactoryInjectedTypeConfiguration) &&
                Objects.equals(binaryLogInjectedTypeConfiguration, that.binaryLogInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hostPortConfiguration,
                channelFactoryInjectedTypeConfiguration,
                channelType,
                channelOptionsConfiguration,
                channelOptionsFactoryInjectedTypeConfiguration,
                useSharedEventLoopGroup,
                eventLoopGroupConfiguration,
                useDirectExecutor,
                useSharedExecutorServiceConfiguration,
                executorServiceConfiguration,
                useSharedOffloadExecutorServiceConfiguration,
                offloadExecutorServiceConfiguration,
                flowControlWindow,
                initialFlowControlWindow,
                maxInboundMessageSize,
                maxInboundMetadataSize,
                maxHedgedAttempts,
                maxRetryAttempts,
                maxTraceEvents,
                perRpcBufferLimit,
                retryBufferSizeConfiguration,
                keepAliveWithoutCalls,
                enableRetry,
                enableServiceConfigLookUp,
                enableFullStreamDecompression,
                keepAliveTimeConfiguration,
                keepAliveTimeoutConfiguration,
                idleTimeoutConfiguration,
                userAgent,
                proxyDetectorInjectedTypeConfiguration,
                localSocketPairInjectedTypeConfiguration,
                defaultLoadBalancingPolicy,
                compressorRegistryInjectedTypeConfiguration,
                decompressorRegistryInjectedTypeConfiguration,
                http2NegotiationType,
                overrideAuthority,
                sslClientContextConfiguration,
                clientInterceptorListFactoryInjectedTypeConfiguration,
                binaryLogInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "NettyChannelConfiguration{" +
                "hostPortConfiguration=" + hostPortConfiguration +
                ", channelFactoryInjectedTypeConfiguration=" + channelFactoryInjectedTypeConfiguration +
                ", channelType=" + channelType +
                ", channelOptionsConfiguration=" + channelOptionsConfiguration +
                ", channelOptionsFactoryInjectedTypeConfiguration=" + channelOptionsFactoryInjectedTypeConfiguration +
                ", useSharedEventLoopGroup=" + useSharedEventLoopGroup +
                ", eventLoopGroupConfiguration=" + eventLoopGroupConfiguration +
                ", useDirectExecutor=" + useDirectExecutor +
                ", useSharedExecutorServiceConfiguration=" + useSharedExecutorServiceConfiguration +
                ", executorServiceConfiguration=" + executorServiceConfiguration +
                ", useSharedOffloadExecutorServiceConfiguration=" + useSharedOffloadExecutorServiceConfiguration +
                ", offloadExecutorServiceConfiguration=" + offloadExecutorServiceConfiguration +
                ", flowControlWindow=" + flowControlWindow +
                ", initialFlowControlWindow=" + initialFlowControlWindow +
                ", maxInboundMessageSize=" + maxInboundMessageSize +
                ", maxInboundMetadataSize=" + maxInboundMetadataSize +
                ", maxHedgedAttempts=" + maxHedgedAttempts +
                ", maxRetryAttempts=" + maxRetryAttempts +
                ", maxTraceEvents=" + maxTraceEvents +
                ", perRpcBufferLimit=" + perRpcBufferLimit +
                ", retryBufferSizeConfiguration=" + retryBufferSizeConfiguration +
                ", keepAliveWithoutCalls=" + keepAliveWithoutCalls +
                ", enableRetry=" + enableRetry +
                ", enableServiceConfigLookUp=" + enableServiceConfigLookUp +
                ", enableFullStreamDecompression=" + enableFullStreamDecompression +
                ", keepAliveTimeConfiguration=" + keepAliveTimeConfiguration +
                ", keepAliveTimeoutConfiguration=" + keepAliveTimeoutConfiguration +
                ", idleTimeoutConfiguration=" + idleTimeoutConfiguration +
                ", userAgent='" + userAgent + '\'' +
                ", proxyDetectorInjectedTypeConfiguration=" + proxyDetectorInjectedTypeConfiguration +
                ", localSocketPairInjectedTypeConfiguration=" + localSocketPairInjectedTypeConfiguration +
                ", defaultLoadBalancingPolicy='" + defaultLoadBalancingPolicy + '\'' +
                ", compressorRegistryInjectedTypeConfiguration=" + compressorRegistryInjectedTypeConfiguration +
                ", decompressorRegistryInjectedTypeConfiguration=" + decompressorRegistryInjectedTypeConfiguration +
                ", http2NegotiationType=" + http2NegotiationType +
                ", overrideAuthority='" + overrideAuthority + '\'' +
                ", sslClientContextConfiguration=" + sslClientContextConfiguration +
                ", clientInterceptorListFactoryInjectedTypeConfiguration=" +
                clientInterceptorListFactoryInjectedTypeConfiguration +
                ", binaryLogInjectedTypeConfiguration=" + binaryLogInjectedTypeConfiguration +
                '}';
    }


    /**
     * Builder for {@link NettyChannelConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final HostPortConfiguration DEFAULT_HOST_PORT_CONFIGURATION = HostPortConfiguration
                .builder()
                .host("vitess.local")
                .port(15991)
                .build();
        private static final Class<? extends Channel> DEFAULT_CHANNEL_TYPE = AppbaseNetUtil.channelType();
        private static final ChannelOptionsConfiguration DEFAULT_CHANNEL_OPTIONS_CONFIGURATION =
                ChannelOptionsConfiguration.builder().build();
        private static final boolean DEFAULT_USE_SHARED_EVENT_LOOP_GROUP = true;
        private static final boolean DEFAULT_USE_DIRECT_EXECUTOR = true;
        private static final boolean DEFAULT_USE_SHARED_EXECUTOR_SERVICE_CONFIGURATION = true;
        private static final ExecutorServiceConfiguration DEFAULT_EXECUTOR_SERVICE_CONFIGURATION =
                ExecutorServiceConfiguration
                        .builder()
                        .executorType(ExecutorServiceConfiguration.Builder.DEFAULT_EXECUTOR_SERVICE_TYPE)
                        .fixedThreadPoolSize(ExecutorServiceConfiguration.Builder.DEFAULT_FIXED_THREAD_POOL_SIZE)
                        .threadFactoryConfiguration(
                                ThreadFactoryConfiguration
                                        .builder()
                                        .nameFormat("appbase-vitess-executor-thread-%d")
                                        .enableThreadAffinity(true)
                                        .daemon(true)
                                        .build())
                        .build();
        private static final boolean DEFAULT_USE_SHARED_OFFLOAD_EXECUTOR_SERVICE_CONFIGURATION = true;
        private static final ExecutorServiceConfiguration DEFAULT_OFFLOAD_EXECUTOR_SERVICE_CONFIGURATION =
                ExecutorServiceConfiguration
                        .builder()
                        .executorType(ExecutorServiceConfiguration.Builder.DEFAULT_EXECUTOR_SERVICE_TYPE)
                        .fixedThreadPoolSize(ExecutorServiceConfiguration.Builder.DEFAULT_FIXED_THREAD_POOL_SIZE)
                        .threadFactoryConfiguration(
                                ThreadFactoryConfiguration
                                        .builder()
                                        .nameFormat("appbase-vitess-offload-executor-thread-%d")
                                        .enableThreadAffinity(true)
                                        .daemon(true)
                                        .build())
                        .build();
        private static final Integer DEFAULT_FLOW_CONTROL_WINDOW = 1024 * 1024;
        private static final Integer DEFAULT_INITIAL_FLOW_CONTROL_WINDOW = DEFAULT_FLOW_CONTROL_WINDOW;
        private static final Integer DEFAULT_MAX_INBOUND_MESSAGE_SIZE = GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
        private static final Integer DEFAULT_MAX_INBOUND_METADATA_SIZE = GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
        private static final Boolean DEFAULT_KEEP_ALIVE_WITHOUT_CALLS = false;
        private static final Boolean DEFAULT_ENABLE_SERVICE_CONFIG_LOOKUP = true;
        private static final TimeUnitConfiguration DEFAULT_KEEP_ALIVE_TIME_CONFIGURATION =
                TimeUnitConfiguration
                        .builder()
                        .time(1000L)
                        .timeUnit(TimeUnit.DAYS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_KEEP_ALIVE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .timeout(20L)
                        .timeoutTimeUnit(TimeUnit.SECONDS)
                        .build();
        private static final TimeoutConfiguration DEFAULT_IDLE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration
                        .builder()
                        .timeout(30L)
                        .timeoutTimeUnit(TimeUnit.MINUTES)
                        .build();
        private static final Http2NegotiationType DEFAULT_HTTP2_NEGOTIATION_TYPE = Http2NegotiationType.TLS;
        private static final SslClientContextConfiguration DEFAULT_SSL_CLIENT_CONTEXT_CONFIGURATION =
                SslClientContextConfiguration.builder().build();



        private HostPortConfiguration hostPortConfiguration = DEFAULT_HOST_PORT_CONFIGURATION;
        private Class<? extends Channel> channelType = DEFAULT_CHANNEL_TYPE;
        private ChannelOptionsConfiguration channelOptionsConfiguration = DEFAULT_CHANNEL_OPTIONS_CONFIGURATION;
        private boolean useSharedEventLoopGroup = DEFAULT_USE_SHARED_EVENT_LOOP_GROUP;
        private boolean useDirectExecutor = DEFAULT_USE_DIRECT_EXECUTOR;
        private boolean useSharedExecutorServiceConfiguration = DEFAULT_USE_SHARED_EXECUTOR_SERVICE_CONFIGURATION;
        private ExecutorServiceConfiguration executorServiceConfiguration = DEFAULT_EXECUTOR_SERVICE_CONFIGURATION;
        private boolean useSharedOffloadExecutorServiceConfiguration =
                DEFAULT_USE_SHARED_OFFLOAD_EXECUTOR_SERVICE_CONFIGURATION;
        private ExecutorServiceConfiguration offloadExecutorServiceConfiguration =
                DEFAULT_OFFLOAD_EXECUTOR_SERVICE_CONFIGURATION;
        private Integer flowControlWindow = DEFAULT_FLOW_CONTROL_WINDOW;
        private Integer initialFlowControlWindow = DEFAULT_INITIAL_FLOW_CONTROL_WINDOW;
        private Integer maxInboundMessageSize = DEFAULT_MAX_INBOUND_MESSAGE_SIZE;
        private Integer maxInboundMetadataSize = DEFAULT_MAX_INBOUND_METADATA_SIZE;
        private Boolean keepAliveWithoutCalls = DEFAULT_KEEP_ALIVE_WITHOUT_CALLS;
        private Boolean enableServiceConfigLookUp = DEFAULT_ENABLE_SERVICE_CONFIG_LOOKUP;
        private TimeUnitConfiguration keepAliveTimeConfiguration = DEFAULT_KEEP_ALIVE_TIME_CONFIGURATION;
        private TimeoutConfiguration keepAliveTimeoutConfiguration = DEFAULT_KEEP_ALIVE_TIMEOUT_CONFIGURATION;
        private TimeoutConfiguration idleTimeoutConfiguration = DEFAULT_IDLE_TIMEOUT_CONFIGURATION;
        private Http2NegotiationType http2NegotiationType = DEFAULT_HTTP2_NEGOTIATION_TYPE;
        private SslClientContextConfiguration sslClientContextConfiguration = DEFAULT_SSL_CLIENT_CONTEXT_CONFIGURATION;

        private InjectedTypeConfiguration<? extends ChannelFactory<? extends Channel>>
                channelFactoryInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends ChannelOptionsFactory> channelOptionsFactoryInjectedTypeConfiguration;
        private EventLoopGroupConfiguration eventLoopGroupConfiguration;

        private Integer maxHedgedAttempts;
        private Integer maxRetryAttempts;
        private Integer maxTraceEvents;
        private Integer perRpcBufferLimit;
        private DataSizeConfiguration retryBufferSizeConfiguration;

        private Boolean enableRetry;
        private Boolean enableFullStreamDecompression;

        private String userAgent;

        private InjectedTypeConfiguration<? extends ProxyDetector> proxyDetectorInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends NettyChannelBuilder.LocalSocketPicker>
                localSocketPairInjectedTypeConfiguration;
        private String defaultLoadBalancingPolicy;
        private String overrideAuthority;
        private InjectedTypeConfiguration<? extends CompressorRegistry> compressorRegistryInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends DecompressorRegistry> decompressorRegistryInjectedTypeConfiguration;

        private InjectedTypeConfiguration<? extends ClientInterceptorListFactory>
                clientInterceptorListFactoryInjectedTypeConfiguration;
        private InjectedTypeConfiguration<? extends BinaryLog> binaryLogInjectedTypeConfiguration;



        private Builder() {
        }

        public Builder hostPortConfiguration(final HostPortConfiguration hostPortConfiguration) {
            this.hostPortConfiguration = hostPortConfiguration;
            return this;
        }

        public Builder channelFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends ChannelFactory<? extends Channel>>
                        channelFactoryInjectedTypeConfiguration)
        {
            this.channelFactoryInjectedTypeConfiguration = channelFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder channelType(final Class<? extends Channel> channelType) {
            this.channelType = channelType;
            return this;
        }

        public Builder channelOptionsConfiguration(final ChannelOptionsConfiguration channelOptionsConfiguration) {
            this.channelOptionsConfiguration = channelOptionsConfiguration;
            return this;
        }

        public Builder channelOptionsFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends ChannelOptionsFactory>
                        channelOptionsFactoryInjectedTypeConfiguration)
        {
            this.channelOptionsFactoryInjectedTypeConfiguration = channelOptionsFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder useSharedEventLoopGroup(final boolean useSharedEventLoopGroup) {
            this.useSharedEventLoopGroup = useSharedEventLoopGroup;
            return this;
        }

        public Builder eventLoopGroupConfiguration(final EventLoopGroupConfiguration eventLoopGroupConfiguration) {
            this.eventLoopGroupConfiguration = eventLoopGroupConfiguration;
            return this;
        }

        public Builder useDirectExecutor(final boolean useDirectExecutor) {
            this.useDirectExecutor = useDirectExecutor;
            return this;
        }

        public Builder useSharedExecutorServiceConfiguration(final boolean useSharedExecutorServiceConfiguration) {
            this.useSharedExecutorServiceConfiguration = useSharedExecutorServiceConfiguration;
            return this;
        }

        public Builder executorServiceConfiguration(final ExecutorServiceConfiguration executorServiceConfiguration) {
            this.executorServiceConfiguration = executorServiceConfiguration;
            return this;
        }

        public Builder useSharedOffloadExecutorServiceConfiguration(
                final boolean useSharedOffloadExecutorServiceConfiguration)
        {
            this.useSharedOffloadExecutorServiceConfiguration = useSharedOffloadExecutorServiceConfiguration;
            return this;
        }

        public Builder offloadExecutorServiceConfiguration(
                final ExecutorServiceConfiguration offloadExecutorServiceConfiguration)
        {
            this.offloadExecutorServiceConfiguration = offloadExecutorServiceConfiguration;
            return this;
        }

        public Builder flowControlWindow(final Integer flowControlWindow) {
            this.flowControlWindow = flowControlWindow;
            return this;
        }

        public Builder initialFlowControlWindow(final Integer initialFlowControlWindow) {
            this.initialFlowControlWindow = initialFlowControlWindow;
            return this;
        }

        public Builder maxInboundMessageSize(final Integer maxInboundMessageSize) {
            this.maxInboundMessageSize = maxInboundMessageSize;
            return this;
        }

        public Builder maxInboundMetadataSize(final Integer maxInboundMetadataSize) {
            this.maxInboundMetadataSize = maxInboundMetadataSize;
            return this;
        }

        public Builder maxHedgedAttempts(final Integer maxHedgedAttempts) {
            this.maxHedgedAttempts = maxHedgedAttempts;
            return this;
        }

        public Builder maxRetryAttempts(final Integer maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }

        public Builder maxTraceEvents(final Integer maxTraceEvents) {
            this.maxTraceEvents = maxTraceEvents;
            return this;
        }

        public Builder perRpcBufferLimit(final Integer perRpcBufferLimit) {
            this.perRpcBufferLimit = perRpcBufferLimit;
            return this;
        }

        public Builder retryBufferSizeConfiguration(final DataSizeConfiguration retryBufferSizeConfiguration) {
            this.retryBufferSizeConfiguration = retryBufferSizeConfiguration;
            return this;
        }

        public Builder keepAliveWithoutCalls(final Boolean keepAliveWithoutCalls) {
            this.keepAliveWithoutCalls = keepAliveWithoutCalls;
            return this;
        }

        public Builder enableRetry(final Boolean enableRetry) {
            this.enableRetry = enableRetry;
            return this;
        }

        public Builder enableServiceConfigLookUp(final Boolean enableServiceConfigLookUp) {
            this.enableServiceConfigLookUp = enableServiceConfigLookUp;
            return this;
        }

        public Builder enableFullStreamDecompression(final Boolean enableFullStreamDecompression) {
            this.enableFullStreamDecompression = enableFullStreamDecompression;
            return this;
        }

        public Builder keepAliveTimeConfiguration(final TimeUnitConfiguration keepAliveTimeConfiguration) {
            this.keepAliveTimeConfiguration = keepAliveTimeConfiguration;
            return this;
        }

        public Builder keepAliveTimeoutConfiguration(final TimeoutConfiguration keepAliveTimeoutConfiguration) {
            this.keepAliveTimeoutConfiguration = keepAliveTimeoutConfiguration;
            return this;
        }

        public Builder idleTimeoutConfiguration(final TimeoutConfiguration idleTimeoutConfiguration) {
            this.idleTimeoutConfiguration = idleTimeoutConfiguration;
            return this;
        }

        public Builder userAgent(final String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder proxyDetectorInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends ProxyDetector> proxyDetectorInjectedTypeConfiguration)
        {
            this.proxyDetectorInjectedTypeConfiguration = proxyDetectorInjectedTypeConfiguration;
            return this;
        }

        public Builder localSocketPairInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends NettyChannelBuilder.LocalSocketPicker>
                        localSocketPairInjectedTypeConfiguration)
        {
            this.localSocketPairInjectedTypeConfiguration = localSocketPairInjectedTypeConfiguration;
            return this;
        }

        public Builder defaultLoadBalancingPolicy(final String defaultLoadBalancingPolicy) {
            this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
            return this;
        }

        public Builder compressorRegistryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends CompressorRegistry>
                        compressorRegistryInjectedTypeConfiguration)
        {
            this.compressorRegistryInjectedTypeConfiguration = compressorRegistryInjectedTypeConfiguration;
            return this;
        }

        public Builder decompressorRegistryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends DecompressorRegistry>
                        decompressorRegistryInjectedTypeConfiguration)
        {
            this.decompressorRegistryInjectedTypeConfiguration = decompressorRegistryInjectedTypeConfiguration;
            return this;
        }

        public Builder http2NegotiationType(final Http2NegotiationType http2NegotiationType) {
            this.http2NegotiationType = http2NegotiationType;
            return this;
        }

        public Builder overrideAuthority(final String overrideAuthority) {
            this.overrideAuthority = overrideAuthority;
            return this;
        }

        public Builder sslClientContextConfiguration(final SslClientContextConfiguration sslClientContextConfiguration) {
            this.sslClientContextConfiguration = sslClientContextConfiguration;
            return this;
        }

        public Builder clientInterceptorListFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends ClientInterceptorListFactory>
                        clientInterceptorListFactoryInjectedTypeConfiguration)
        {
            this.clientInterceptorListFactoryInjectedTypeConfiguration =
                    clientInterceptorListFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder binaryLogInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends BinaryLog> binaryLogInjectedTypeConfiguration)
        {
            this.binaryLogInjectedTypeConfiguration = binaryLogInjectedTypeConfiguration;
            return this;
        }

        public NettyChannelConfiguration build() {
            return new NettyChannelConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
