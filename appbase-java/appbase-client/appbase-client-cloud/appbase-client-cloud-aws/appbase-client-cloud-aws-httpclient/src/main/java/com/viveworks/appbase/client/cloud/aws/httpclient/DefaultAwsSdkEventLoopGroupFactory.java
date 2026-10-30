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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.AwsSdkEventLoopGroupConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

/**
 * Default implementation of {@link AwsSdkEventLoopGroupFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkEventLoopGroupFactory implements AwsSdkEventLoopGroupFactory {
    private final AppbaseClientCore appbaseClientCore;
    private final AwsSdkEventLoopGroupConfiguration awsSdkEventLoopGroupConfiguration;

    /**
     * Creates a DefaultAwsSdkEventLoopGroupFactory using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkEventLoopGroupFactory(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkEventLoopGroupConfiguration awsSdkEventLoopGroupConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(awsSdkEventLoopGroupConfiguration, "awsSdkEventLoopGroupConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.awsSdkEventLoopGroupConfiguration = awsSdkEventLoopGroupConfiguration;
    }

    @Override
    public SdkEventLoopGroup getInstance() {
        if (awsSdkEventLoopGroupConfiguration.isUseBuilder()) {
            final SdkEventLoopGroup.Builder sdkEventLoopGroupBuilder = SdkEventLoopGroup.builder();
            return sdkEventLoopGroupBuilder
                    .numberOfThreads(awsSdkEventLoopGroupConfiguration.getThreadCount())
                    .threadFactory(
                            AppbaseUtil.newThreadFactory(
                                    awsSdkEventLoopGroupConfiguration.getThreadFactoryConfiguration(),
                                    AppbaseUtil.defaultUncaughtExceptionHandler(
                                            appbaseClientCore.getAppbaseCore().getLogService().getLogger(getClass()))))
                    .build();
        }

        final EventLoopGroup eventLoopGroup;
        if (awsSdkEventLoopGroupConfiguration.isUseSharedEventLoopGroup()) {
            eventLoopGroup = null;
        } else {
            final EventLoopGroupConfiguration eventLoopGroupConfiguration =
                    awsSdkEventLoopGroupConfiguration.getEventLoopGroupConfiguration();
            final EventLoopGroupFactory eventLoopGroupFactory = new EventLoopGroupFactory(
                    appbaseClientCore.getAppbaseCore(),
                    eventLoopGroupConfiguration);
            eventLoopGroup = eventLoopGroupFactory.getInstance();
        }

        final InjectedTypeConfiguration<ChannelFactory<? extends Channel>> channelFactoryInjectedTypeConfiguration =
                awsSdkEventLoopGroupConfiguration.getChannelFactoryInjectedTypeConfiguration();
        if (channelFactoryInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore
                    .getAppbaseCore()
                    .getGuiceInjectorService();
            final ChannelFactory<? extends Channel> channelFactory =
                    AppbaseUtil.injectedInstance(guiceInjectorService, channelFactoryInjectedTypeConfiguration);
            if (channelFactory != null) {
                return SdkEventLoopGroup.create(eventLoopGroup, channelFactory);
            }
        }

        return SdkEventLoopGroup.create(eventLoopGroup);
    }
}
