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

package com.viveworks.appbase.core.util.net.eventloopgroup;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategyFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;

/**
 * Configuration for {@link io.netty.channel.EventLoopGroup}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = EventLoopGroupConfiguration.Builder.class)
public final class EventLoopGroupConfiguration {
    private final String name;
    private final EventLoopType eventLoopType;
    private final int numberOfThreads;
    private final boolean useMediatorSharedExecutor;
    private final ThreadFactoryConfiguration customExecutorThreadFactoryConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<RejectedExecutionHandler>
            rejectedExecutionHandlerInjectedTypeConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<EventExecutorChooserFactory>
            eventExecutorChooserFactoryInjectedTypeConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<SelectStrategyFactory>
            selectStrategyFactoryInjectedTypeConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<EventLoopTaskQueueFactory>
            eventLoopTaskQueueFactoryInjectedTypeConfiguration;

    @Nullable
    private final InjectedTypeConfiguration<SelectorProvider>
            nioEventLoopSelectorProviderInjectedTypeConfiguration;

    private EventLoopGroupConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.name, "builder.name");
        AppbaseUtil.notNull(builder.eventLoopType, "builder.eventLoopType");
        AppbaseUtil.positive(builder.numberOfThreads, "builder.numberOfThreads");

        if (!builder.useMediatorSharedExecutor) {
            AppbaseUtil.notNull(
                    builder.customExecutorThreadFactoryConfiguration,
                    "builder.customExecutorThreadFactoryConfiguration");
        }

        name = builder.name;
        eventLoopType = builder.eventLoopType;
        numberOfThreads = builder.numberOfThreads;
        useMediatorSharedExecutor = builder.useMediatorSharedExecutor;
        customExecutorThreadFactoryConfiguration = builder.customExecutorThreadFactoryConfiguration;
        rejectedExecutionHandlerInjectedTypeConfiguration =
                builder.rejectedExecutionHandlerInjectedTypeConfiguration;
        eventExecutorChooserFactoryInjectedTypeConfiguration =
                builder.eventExecutorChooserFactoryInjectedTypeConfiguration;
        selectStrategyFactoryInjectedTypeConfiguration =
                builder.selectStrategyFactoryInjectedTypeConfiguration;
        eventLoopTaskQueueFactoryInjectedTypeConfiguration =
                builder.eventLoopTaskQueueFactoryInjectedTypeConfiguration;
        nioEventLoopSelectorProviderInjectedTypeConfiguration =
                builder.nioEventLoopSelectorProviderInjectedTypeConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public EventLoopType getEventLoopType() {
        return eventLoopType;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public boolean isUseMediatorSharedExecutor() {
        return useMediatorSharedExecutor;
    }

    public ThreadFactoryConfiguration getCustomExecutorThreadFactoryConfiguration() {
        return customExecutorThreadFactoryConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<RejectedExecutionHandler> getRejectedExecutionHandlerInjectedTypeConfiguration() {
        return rejectedExecutionHandlerInjectedTypeConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<EventExecutorChooserFactory>
    getEventExecutorChooserFactoryInjectedTypeConfiguration()
    {
        return eventExecutorChooserFactoryInjectedTypeConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<SelectStrategyFactory>
    getSelectStrategyFactoryInjectedTypeConfiguration()
    {
        return selectStrategyFactoryInjectedTypeConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<EventLoopTaskQueueFactory>
    getEventLoopTaskQueueFactoryInjectedTypeConfiguration()
    {
        return eventLoopTaskQueueFactoryInjectedTypeConfiguration;
    }

    @Nullable
    public InjectedTypeConfiguration<SelectorProvider>
    getNioEventLoopSelectorProviderInjectedTypeConfiguration()
    {
        return nioEventLoopSelectorProviderInjectedTypeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EventLoopGroupConfiguration that = (EventLoopGroupConfiguration) o;
        return numberOfThreads == that.numberOfThreads &&
                useMediatorSharedExecutor == that.useMediatorSharedExecutor &&
                eventLoopType == that.eventLoopType &&
                Objects.equals(name, that.name) &&
                Objects.equals(
                        customExecutorThreadFactoryConfiguration,
                        that.customExecutorThreadFactoryConfiguration) &&
                Objects.equals(
                        rejectedExecutionHandlerInjectedTypeConfiguration,
                        that.rejectedExecutionHandlerInjectedTypeConfiguration) &&
                Objects.equals(
                        eventExecutorChooserFactoryInjectedTypeConfiguration,
                        that.eventExecutorChooserFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        selectStrategyFactoryInjectedTypeConfiguration,
                        that.selectStrategyFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        eventLoopTaskQueueFactoryInjectedTypeConfiguration,
                        that.eventLoopTaskQueueFactoryInjectedTypeConfiguration) &&
                Objects.equals(
                        nioEventLoopSelectorProviderInjectedTypeConfiguration,
                        that.nioEventLoopSelectorProviderInjectedTypeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                eventLoopType,
                numberOfThreads,
                useMediatorSharedExecutor,
                customExecutorThreadFactoryConfiguration,
                rejectedExecutionHandlerInjectedTypeConfiguration,
                eventExecutorChooserFactoryInjectedTypeConfiguration,
                selectStrategyFactoryInjectedTypeConfiguration,
                eventLoopTaskQueueFactoryInjectedTypeConfiguration,
                nioEventLoopSelectorProviderInjectedTypeConfiguration);
    }

    @Override
    public String toString() {
        return "EventLoopGroupConfiguration{" +
                "name=" + name +
                ", eventLoopType=" + eventLoopType +
                ", numberOfThreads=" + numberOfThreads +
                ", useMediatorSharedExecutor=" + useMediatorSharedExecutor +
                ", customExecutorThreadFactoryConfiguration=" + customExecutorThreadFactoryConfiguration +
                ", rejectedExecutionHandlerInjectedTypeConfiguration=" +
                rejectedExecutionHandlerInjectedTypeConfiguration +
                ", eventExecutorChooserFactoryInjectedTypeConfiguration=" +
                eventExecutorChooserFactoryInjectedTypeConfiguration +
                ", selectStrategyFactoryInjectedTypeConfiguration=" +
                selectStrategyFactoryInjectedTypeConfiguration +
                ", eventLoopTaskQueueFactoryInjectedTypeConfiguration=" +
                eventLoopTaskQueueFactoryInjectedTypeConfiguration +
                ", nioEventLoopSelectorProviderInjectedTypeConfiguration=" +
                nioEventLoopSelectorProviderInjectedTypeConfiguration +
                '}';
    }

    /**
     * Builder for {@link EventLoopGroupConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_NAME = EventLoopGroup.class.getName();
        private static final EventLoopType DEFAULT_EVENT_LOOP_TYPE = EventLoopType.AUTO_DETECT;
        private static final int DEFAULT_NUMBER_OF_THREADS =
                (Runtime.getRuntime().availableProcessors() * 2 - 2) <= 0
                        ? Runtime.getRuntime().availableProcessors() * 2
                        : (Runtime.getRuntime().availableProcessors() * 2 - 2);
        private static final boolean DEFAULT_USE_MEDIATOR_SHARED_EXECUTOR = true;
        private static final ThreadFactoryConfiguration DEFAULT_CUSTOM_EXECUTOR_THREAD_FACTORY_CONFIGURATION =
                ThreadFactoryConfiguration.builder().build();

        private String name = DEFAULT_NAME;
        private EventLoopType eventLoopType = DEFAULT_EVENT_LOOP_TYPE;
        private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
        private boolean useMediatorSharedExecutor = DEFAULT_USE_MEDIATOR_SHARED_EXECUTOR;
        private ThreadFactoryConfiguration customExecutorThreadFactoryConfiguration =
                DEFAULT_CUSTOM_EXECUTOR_THREAD_FACTORY_CONFIGURATION;

        @Nullable
        private InjectedTypeConfiguration<RejectedExecutionHandler>
                rejectedExecutionHandlerInjectedTypeConfiguration;

        @Nullable
        private InjectedTypeConfiguration<EventExecutorChooserFactory>
                eventExecutorChooserFactoryInjectedTypeConfiguration;

        @Nullable
        private InjectedTypeConfiguration<SelectStrategyFactory>
                selectStrategyFactoryInjectedTypeConfiguration;

        @Nullable
        private InjectedTypeConfiguration<EventLoopTaskQueueFactory>
                eventLoopTaskQueueFactoryInjectedTypeConfiguration;

        @Nullable
        private InjectedTypeConfiguration<SelectorProvider>
                nioEventLoopSelectorProviderInjectedTypeConfiguration;

        private Builder() {
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder eventLoopType(final EventLoopType eventLoopType) {
            this.eventLoopType = eventLoopType;
            return this;
        }

        public Builder numberOfThreads(final int numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        public Builder useMediatorSharedExecutor(final boolean useMediatorSharedExecutor) {
            this.useMediatorSharedExecutor = useMediatorSharedExecutor;
            return this;
        }

        public Builder customExecutorThreadFactoryConfiguration(
                final ThreadFactoryConfiguration customExecutorThreadFactoryConfiguration)
        {
            this.customExecutorThreadFactoryConfiguration = customExecutorThreadFactoryConfiguration;
            return this;
        }

        public Builder rejectedExecutionHandlerInjectedTypeConfiguration(
                @Nullable
                final InjectedTypeConfiguration<RejectedExecutionHandler>
                        rejectedExecutionHandlerInjectedTypeConfiguration)
        {
            this.rejectedExecutionHandlerInjectedTypeConfiguration = rejectedExecutionHandlerInjectedTypeConfiguration;
            return this;
        }

        public Builder eventExecutorChooserFactoryInjectedTypeConfiguration(
                @Nullable
                final InjectedTypeConfiguration<EventExecutorChooserFactory>
                        eventExecutorChooserFactoryInjectedTypeConfiguration)
        {
            this.eventExecutorChooserFactoryInjectedTypeConfiguration =
                    eventExecutorChooserFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder selectStrategyFactoryInjectedTypeConfiguration(
                @Nullable
                final InjectedTypeConfiguration<SelectStrategyFactory>
                        selectStrategyFactoryInjectedTypeConfiguration)
        {
            this.selectStrategyFactoryInjectedTypeConfiguration = selectStrategyFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder eventLoopTaskQueueFactoryInjectedTypeConfiguration(
                @Nullable
                final InjectedTypeConfiguration<EventLoopTaskQueueFactory>
                        eventLoopTaskQueueFactoryInjectedTypeConfiguration)
        {
            this.eventLoopTaskQueueFactoryInjectedTypeConfiguration =
                    eventLoopTaskQueueFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder nioEventLoopSelectorProviderInjectedTypeConfiguration(
                @Nullable
                final InjectedTypeConfiguration<SelectorProvider>
                        nioEventLoopSelectorProviderInjectedTypeConfiguration)
        {
            this.nioEventLoopSelectorProviderInjectedTypeConfiguration =
                    nioEventLoopSelectorProviderInjectedTypeConfiguration;
            return this;
        }

        public EventLoopGroupConfiguration build() {
            return new EventLoopGroupConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
