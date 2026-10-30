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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.context.NettyOptions;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.Duration;

/**
 * Custom implementation of {@link NettyOptions} that uses shared {@code appbase} resources.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseCassandraNettyOptions implements NettyOptions {
    private final AppbaseClientCore appbaseClientCore;
    private final DriverExecutionProfile driverExecutionProfile;

    public AppbaseCassandraNettyOptions(
            final InternalDriverContext internalDriverContext,
            final AppbaseClientCore appbaseClientCore)
    {
        AppbaseUtil.notNull(internalDriverContext, "internalDriverContext");
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        this.appbaseClientCore = appbaseClientCore;
        this.driverExecutionProfile = internalDriverContext.getConfig().getDefaultProfile();
    }

    @Override
    public EventLoopGroup ioEventLoopGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    public Class<? extends Channel> channelClass() {
        return NioSocketChannel.class;
    }

    @Override
    public EventExecutorGroup adminEventExecutorGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    public ByteBufAllocator allocator() {
        return ByteBufAllocator.DEFAULT;
    }

    /**
     * This method is a copy of
     * {@link com.datastax.oss.driver.internal.core.context.DefaultNettyOptions#afterBootstrapInitialized(Bootstrap)}, so
     * the functionality of the underlying {@code Cassandra} driver configuration is preserved.
     *
     * @param bootstrap {@link Bootstrap}
     */
    @Override
    public void afterBootstrapInitialized(final Bootstrap bootstrap) {
        final boolean tcpNoDelay = driverExecutionProfile.getBoolean(DefaultDriverOption.SOCKET_TCP_NODELAY);
        bootstrap.option(ChannelOption.TCP_NODELAY, tcpNoDelay);

        if (driverExecutionProfile.isDefined(DefaultDriverOption.SOCKET_KEEP_ALIVE)) {
            final boolean keepAlive = driverExecutionProfile.getBoolean(DefaultDriverOption.SOCKET_KEEP_ALIVE);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, keepAlive);
        }

        if (driverExecutionProfile.isDefined(DefaultDriverOption.SOCKET_REUSE_ADDRESS)) {
            final boolean reuseAddress = driverExecutionProfile.getBoolean(DefaultDriverOption.SOCKET_REUSE_ADDRESS);
            bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);
        }

        if (driverExecutionProfile.isDefined(DefaultDriverOption.SOCKET_LINGER_INTERVAL)) {
            final int lingerInterval = driverExecutionProfile.getInt(DefaultDriverOption.SOCKET_LINGER_INTERVAL);
            bootstrap.option(ChannelOption.SO_LINGER, lingerInterval);
        }

        if (driverExecutionProfile.isDefined(DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE)) {
            final int receiveBufferSize = driverExecutionProfile.getInt(DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE);
            bootstrap
                    .option(ChannelOption.SO_RCVBUF, receiveBufferSize)
                    .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(receiveBufferSize));
        }

        if (driverExecutionProfile.isDefined(DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE)) {
            final int sendBufferSize = driverExecutionProfile.getInt(DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE);
            bootstrap.option(ChannelOption.SO_SNDBUF, sendBufferSize);
        }

        if (driverExecutionProfile.isDefined(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT)) {
            final Duration connectTimeout =
                    driverExecutionProfile.getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT);
            bootstrap.option(
                    ChannelOption.CONNECT_TIMEOUT_MILLIS, Long.valueOf(connectTimeout.toMillis()).intValue());
        }
    }

    @Override
    public void afterChannelInitialized(final Channel channel) {
        // No-op
    }

    @Override
    public Future<Void> onClose() {
        return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
    }

    @Override
    public Timer getTimer() {
        return new HashedWheelTimer();
    }
}
