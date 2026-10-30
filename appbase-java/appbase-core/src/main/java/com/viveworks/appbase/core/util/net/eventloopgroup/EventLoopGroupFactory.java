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

package com.viveworks.appbase.core.util.net.eventloopgroup;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.DefaultRejectedExecutionHandler;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategyFactory;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * Provider for {@link EventLoopGroup}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class EventLoopGroupFactory implements AppbaseFactory<EventLoopGroup> {
    private final Logger logger;
    private final AppbaseCore appbaseCore;
    private final EventLoopGroupConfiguration eventLoopGroupConfiguration;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    private final EventExecutorChooserFactory eventExecutorChooserFactory;
    private final SelectStrategyFactory selectStrategyFactory;
    private final EventLoopTaskQueueFactory eventLoopTaskQueueFactory;
    private final SelectorProvider nioEventLoopSelectorProvider;

    public EventLoopGroupFactory(
            final AppbaseCore appbaseCore,
            final EventLoopGroupConfiguration eventLoopGroupConfiguration)
    {
        AppbaseUtil.notNull(eventLoopGroupConfiguration, "eventLoopGroupConfiguration");
        AppbaseUtil.notNull(
                eventLoopGroupConfiguration.getEventLoopType(), "eventLoopGroupConfiguration.getEventLoopType()");
        AppbaseUtil.positive(
                eventLoopGroupConfiguration.getNumberOfThreads(), "eventLoopGroupConfiguration.getNumberOfThreads()");
        AppbaseUtil.notNull(
                eventLoopGroupConfiguration.getName(), "eventLoopGroupConfiguration.getName()");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        this.appbaseCore = appbaseCore;
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.eventLoopGroupConfiguration = eventLoopGroupConfiguration;
        this.rejectedExecutionHandler =
                rejectedExecutionHandler(eventLoopGroupConfiguration, appbaseCore);
        this.eventExecutorChooserFactory =
                eventExecutorChooserFactory(eventLoopGroupConfiguration, appbaseCore);
        this.selectStrategyFactory =
                selectStrategyFactory(eventLoopGroupConfiguration, appbaseCore);
        this.eventLoopTaskQueueFactory =
                eventLoopTaskQueueFactory(eventLoopGroupConfiguration, appbaseCore);
        this.nioEventLoopSelectorProvider =
                nioEventLoopSelectorProvider(eventLoopGroupConfiguration, appbaseCore);
    }

    @Inject
    public EventLoopGroupFactory(
            final AppbaseCore appbaseCore,
            final EventLoopGroupConfiguration eventLoopGroupConfiguration,
            @Nullable
            final RejectedExecutionHandler rejectedExecutionHandler,
            @Nullable
            final EventExecutorChooserFactory eventExecutorChooserFactory,
            @Nullable
            final SelectStrategyFactory selectStrategyFactory,
            @Nullable
            final EventLoopTaskQueueFactory eventLoopTaskQueueFactory,
            @Nullable
            final SelectorProvider nioEventLoopSelectorProvider)
    {
        AppbaseUtil.notNull(eventLoopGroupConfiguration, "eventLoopGroupConfiguration");
        AppbaseUtil.notNull(
                eventLoopGroupConfiguration.getEventLoopType(), "eventLoopGroupConfiguration.getEventLoopType()");
        AppbaseUtil.positive(
                eventLoopGroupConfiguration.getNumberOfThreads(), "eventLoopGroupConfiguration.getNumberOfThreads()");
        AppbaseUtil.notNull(
                eventLoopGroupConfiguration.getName(), "eventLoopGroupConfiguration.getName()");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.eventLoopGroupConfiguration = eventLoopGroupConfiguration;
        this.appbaseCore = appbaseCore;

        if (rejectedExecutionHandler != null) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;
        } else {
            this.rejectedExecutionHandler = new DefaultRejectedExecutionHandler(
                    appbaseCore.getMetricService(),
                    EventLoopGroup.class.getName());
        }

        if (selectStrategyFactory != null) {
            this.selectStrategyFactory = selectStrategyFactory;
        } else {
            this.selectStrategyFactory = DefaultSelectStrategyFactory.INSTANCE;
        }

        this.eventExecutorChooserFactory = eventExecutorChooserFactory;
        this.eventLoopTaskQueueFactory = eventLoopTaskQueueFactory;
        this.nioEventLoopSelectorProvider = nioEventLoopSelectorProvider;
    }

    private static RejectedExecutionHandler rejectedExecutionHandler(
            final EventLoopGroupConfiguration eventLoopGroupConfiguration,
            final AppbaseCore appbaseCore)
    {
        final InjectedTypeConfiguration<RejectedExecutionHandler>
                rejectedExecutionHandlerInjectedTypeConfiguration =
                eventLoopGroupConfiguration.getRejectedExecutionHandlerInjectedTypeConfiguration();
        final RejectedExecutionHandler rejectedExecutionHandler =
                object(rejectedExecutionHandlerInjectedTypeConfiguration, appbaseCore);
        return Objects.requireNonNullElseGet(
                rejectedExecutionHandler,
                () -> new DefaultRejectedExecutionHandler(
                        appbaseCore.getMetricService(),
                        EventLoopGroup.class.getName()));
    }

    private static EventExecutorChooserFactory eventExecutorChooserFactory(
            final EventLoopGroupConfiguration eventLoopGroupConfiguration,
            final AppbaseCore appbaseCore)
    {
        final InjectedTypeConfiguration<EventExecutorChooserFactory>
                eventExecutorChooserFactoryInjectedTypeConfiguration =
                eventLoopGroupConfiguration.getEventExecutorChooserFactoryInjectedTypeConfiguration();
        return object(eventExecutorChooserFactoryInjectedTypeConfiguration, appbaseCore);
    }

    private static SelectStrategyFactory selectStrategyFactory(
            final EventLoopGroupConfiguration eventLoopGroupConfiguration,
            final AppbaseCore appbaseCore)
    {
        final InjectedTypeConfiguration<SelectStrategyFactory> selectStrategyFactoryInjectedTypeConfiguration =
                eventLoopGroupConfiguration.getSelectStrategyFactoryInjectedTypeConfiguration();
        final SelectStrategyFactory selectStrategyFactory =
                object(selectStrategyFactoryInjectedTypeConfiguration, appbaseCore);
        return Objects.requireNonNullElse(selectStrategyFactory, DefaultSelectStrategyFactory.INSTANCE);
    }

    private static EventLoopTaskQueueFactory eventLoopTaskQueueFactory(
            final EventLoopGroupConfiguration eventLoopGroupConfiguration,
            final AppbaseCore appbaseCore)
    {
        final InjectedTypeConfiguration<EventLoopTaskQueueFactory> eventLoopTaskQueueFactoryInjectedTypeConfiguration =
                eventLoopGroupConfiguration.getEventLoopTaskQueueFactoryInjectedTypeConfiguration();
        return object(eventLoopTaskQueueFactoryInjectedTypeConfiguration, appbaseCore);
    }

    private static SelectorProvider nioEventLoopSelectorProvider(
            final EventLoopGroupConfiguration eventLoopGroupConfiguration,
            final AppbaseCore appbaseCore)
    {
        final InjectedTypeConfiguration<SelectorProvider> nioEventLoopSelectorProviderInjectedTypeConfiguration =
                eventLoopGroupConfiguration.getNioEventLoopSelectorProviderInjectedTypeConfiguration();
        return object(nioEventLoopSelectorProviderInjectedTypeConfiguration, appbaseCore);
    }

    private static <T> T object(final InjectedTypeConfiguration<T> injectedTypeConfiguration,
                                final AppbaseCore appbaseCore)
    {
        if (injectedTypeConfiguration != null) {
            final Injector injector = injector(appbaseCore);

            if (injectedTypeConfiguration.getQualifierAnnotationType() != null) {
                final Key<T> key = Key.get(
                        injectedTypeConfiguration.getObjectType(),
                        injectedTypeConfiguration.getQualifierAnnotationType());
                return injector.getInstance(key);
            } else {
                return injector.getInstance(injectedTypeConfiguration.getObjectType());
            }
        }

        return null;
    }

    private static Injector injector(final AppbaseCore appbaseCore) {
        final GuiceInjectorService guiceInjectorService = appbaseCore.getGuiceInjectorService();
        AppbaseUtil.notNull(guiceInjectorService, "guiceInjectorService");

        final Injector injector = guiceInjectorService.getInjector();
        AppbaseUtil.notNull(injector, "injector");

        return injector;
    }

    @Override
    public EventLoopGroup getInstance() {
        final EventLoopGroup eventLoopGroup;
        switch (eventLoopGroupConfiguration.getEventLoopType()) {
            case EPOLL:
                eventLoopGroup = epollEventLoopGroup();
                break;
            case KQUEUE:
                eventLoopGroup = kQueueEventLoopGroup();
                break;
            case NIO:
                eventLoopGroup = nioEventLoopGroup();
                break;
            case AUTO_DETECT:
            default:
                if (AppbaseNetUtil.isLinux()) {
                    eventLoopGroup = epollEventLoopGroup();
                } else if (AppbaseNetUtil.isOsx()) {
                    eventLoopGroup = kQueueEventLoopGroup();
                } else {
                    eventLoopGroup = nioEventLoopGroup();
                }
        }

        appbaseCore.getMetricService().registerExecutorServiceMetrics(eventLoopGroup, eventLoopGroupConfiguration.getName());
        return eventLoopGroup;
    }

    private EpollEventLoopGroup epollEventLoopGroup() {
        logger.info("Initializing {} with {}", EpollEventLoopGroup.class.getName(), eventLoopGroupConfiguration);

        if (eventLoopGroupConfiguration.isUseMediatorSharedExecutor()) {
            if (eventExecutorChooserFactory != null &&
                    selectStrategyFactory != null &&
                    rejectedExecutionHandler != null &&
                    eventLoopTaskQueueFactory != null)
            {
                return new EpollEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        selectStrategyFactory,
                        rejectedExecutionHandler,
                        eventLoopTaskQueueFactory);
            }

            if (eventExecutorChooserFactory != null &&
                    selectStrategyFactory != null &&
                    rejectedExecutionHandler != null)
            {
                return new EpollEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        selectStrategyFactory,
                        rejectedExecutionHandler);
            }

            if (eventExecutorChooserFactory != null && selectStrategyFactory != null) {
                return new EpollEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        selectStrategyFactory);
            }

            if (selectStrategyFactory != null) {
                return new EpollEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        selectStrategyFactory);
            }

            return new EpollEventLoopGroup(
                    eventLoopGroupConfiguration.getNumberOfThreads(),
                    appbaseCore.getMediatorService().getMediator().getSharedExecutorService());
        } else {
            final ThreadFactoryConfiguration customExecutorThreadFactoryConfiguration =
                    eventLoopGroupConfiguration.getCustomExecutorThreadFactoryConfiguration();
            AppbaseUtil.notNull(customExecutorThreadFactoryConfiguration, "customExecutorThreadFactoryConfiguration");

            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                    AppbaseUtil.defaultUncaughtExceptionHandler(logger);
            final ThreadFactory threadFactory =
                    AppbaseUtil.newThreadFactory(customExecutorThreadFactoryConfiguration, uncaughtExceptionHandler);

            if (selectStrategyFactory != null) {
                return new EpollEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        threadFactory,
                        selectStrategyFactory);
            }

            return new EpollEventLoopGroup(
                    eventLoopGroupConfiguration.getNumberOfThreads(),
                    threadFactory);
        }
    }

    private KQueueEventLoopGroup kQueueEventLoopGroup() {
        logger.info("Initializing {} with {}", KQueueEventLoopGroup.class.getName(), eventLoopGroupConfiguration);

        if (eventLoopGroupConfiguration.isUseMediatorSharedExecutor()) {
            if (eventExecutorChooserFactory != null &&
                    selectStrategyFactory != null &&
                    rejectedExecutionHandler != null &&
                    eventLoopTaskQueueFactory != null)
            {
                return new KQueueEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        selectStrategyFactory,
                        rejectedExecutionHandler,
                        eventLoopTaskQueueFactory);
            }

            if (eventExecutorChooserFactory != null &&
                    selectStrategyFactory != null &&
                    rejectedExecutionHandler != null)
            {
                return new KQueueEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        selectStrategyFactory,
                        rejectedExecutionHandler);
            }

            if (eventExecutorChooserFactory != null && selectStrategyFactory != null) {
                return new KQueueEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        selectStrategyFactory);
            }

            if (selectStrategyFactory != null) {
                return new KQueueEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        selectStrategyFactory);
            }

            return new KQueueEventLoopGroup(
                    eventLoopGroupConfiguration.getNumberOfThreads(),
                    appbaseCore.getMediatorService().getMediator().getSharedExecutorService());
        } else {
            final ThreadFactoryConfiguration customExecutorThreadFactoryConfiguration =
                    eventLoopGroupConfiguration.getCustomExecutorThreadFactoryConfiguration();
            AppbaseUtil.notNull(customExecutorThreadFactoryConfiguration, "customExecutorThreadFactoryConfiguration");

            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                    AppbaseUtil.defaultUncaughtExceptionHandler(logger);
            final ThreadFactory threadFactory =
                    AppbaseUtil.newThreadFactory(customExecutorThreadFactoryConfiguration, uncaughtExceptionHandler);

            if (selectStrategyFactory != null) {
                return new KQueueEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        threadFactory,
                        selectStrategyFactory);
            }

            return new KQueueEventLoopGroup(
                    eventLoopGroupConfiguration.getNumberOfThreads(),
                    threadFactory);
        }
    }

    private NioEventLoopGroup nioEventLoopGroup() {
        logger.info("Initializing {} with {}", NioEventLoopGroup.class.getName(), eventLoopGroupConfiguration);

        if (eventLoopGroupConfiguration.isUseMediatorSharedExecutor()) {
            if (eventExecutorChooserFactory != null &&
                    nioEventLoopSelectorProvider != null &&
                    selectStrategyFactory != null &&
                    rejectedExecutionHandler != null &&
                    eventLoopTaskQueueFactory != null)
            {
                return new NioEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        nioEventLoopSelectorProvider,
                        selectStrategyFactory,
                        rejectedExecutionHandler,
                        eventLoopTaskQueueFactory);
            }

            if (eventExecutorChooserFactory != null &&
                    nioEventLoopSelectorProvider != null &&
                    selectStrategyFactory != null &&
                    rejectedExecutionHandler != null)
            {
                return new NioEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        nioEventLoopSelectorProvider,
                        selectStrategyFactory,
                        rejectedExecutionHandler);
            }

            if (eventExecutorChooserFactory != null &&
                    selectStrategyFactory != null &&
                    nioEventLoopSelectorProvider != null)
            {
                return new NioEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        eventExecutorChooserFactory,
                        nioEventLoopSelectorProvider,
                        selectStrategyFactory);
            }

            if (selectStrategyFactory != null && nioEventLoopSelectorProvider != null) {
                return new NioEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        appbaseCore.getMediatorService().getMediator().getSharedExecutorService(),
                        nioEventLoopSelectorProvider,
                        selectStrategyFactory);
            }

            return new NioEventLoopGroup(
                    eventLoopGroupConfiguration.getNumberOfThreads(),
                    appbaseCore.getMediatorService().getMediator().getSharedExecutorService());
        } else {
            final ThreadFactoryConfiguration customExecutorThreadFactoryConfiguration =
                    eventLoopGroupConfiguration.getCustomExecutorThreadFactoryConfiguration();
            AppbaseUtil.notNull(customExecutorThreadFactoryConfiguration, "customExecutorThreadFactoryConfiguration");

            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                    AppbaseUtil.defaultUncaughtExceptionHandler(logger);
            final ThreadFactory threadFactory =
                    AppbaseUtil.newThreadFactory(customExecutorThreadFactoryConfiguration, uncaughtExceptionHandler);

            if (selectStrategyFactory != null && nioEventLoopSelectorProvider != null) {
                return new NioEventLoopGroup(
                        eventLoopGroupConfiguration.getNumberOfThreads(),
                        threadFactory,
                        nioEventLoopSelectorProvider,
                        selectStrategyFactory);
            }

            return new NioEventLoopGroup(
                    eventLoopGroupConfiguration.getNumberOfThreads(),
                    threadFactory);
        }
    }
}
