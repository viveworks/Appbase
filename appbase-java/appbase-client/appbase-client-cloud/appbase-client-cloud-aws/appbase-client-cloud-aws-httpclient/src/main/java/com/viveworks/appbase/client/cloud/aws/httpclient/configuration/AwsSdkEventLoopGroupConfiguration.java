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
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link SdkEventLoopGroup}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkEventLoopGroupConfiguration.Builder.class)
public final class AwsSdkEventLoopGroupConfiguration {
    private final boolean useBuilder;
    private final Integer threadCount;
    private final ThreadFactoryConfiguration threadFactoryConfiguration;
    private final boolean useSharedEventLoopGroup;
    private final InjectedTypeConfiguration<ChannelFactory<? extends Channel>> channelFactoryInjectedTypeConfiguration;
    private final EventLoopGroupConfiguration eventLoopGroupConfiguration;

    private AwsSdkEventLoopGroupConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.useBuilder) {
            AppbaseUtil.notNull(builder.threadCount, "builder.threadCount");
            AppbaseUtil.positiveOrZero(builder.threadCount, "builder.threadCount");
            AppbaseUtil.notNull(builder.threadFactoryConfiguration, "builder.threadFactoryConfiguration");
        } else if (!builder.useSharedEventLoopGroup) {
            AppbaseUtil.notNull(builder.eventLoopGroupConfiguration, "builder.eventLoopGroupConfiguration");
        }

        useBuilder = builder.useBuilder;
        threadCount = builder.threadCount;
        threadFactoryConfiguration = builder.threadFactoryConfiguration;
        useSharedEventLoopGroup = builder.useSharedEventLoopGroup;
        channelFactoryInjectedTypeConfiguration = builder.channelFactoryInjectedTypeConfiguration;
        eventLoopGroupConfiguration = builder.eventLoopGroupConfiguration;
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
     * Returns whether use builder.
     *
     * @return whether use builder
     */
    public boolean isUseBuilder() {
        return useBuilder;
    }

    /**
     * Returns the configured thread count.
     *
     * @return the configured thread count
     */
    public Integer getThreadCount() {
        return threadCount;
    }

    /**
     * Returns the configured thread factory configuration.
     *
     * @return the configured thread factory configuration
     */
    public ThreadFactoryConfiguration getThreadFactoryConfiguration() {
        return threadFactoryConfiguration;
    }

    /**
     * Returns whether use shared event loop group.
     *
     * @return whether use shared event loop group
     */
    public boolean isUseSharedEventLoopGroup() {
        return useSharedEventLoopGroup;
    }

    /**
     * Returns the configured channel factory injected type configuration.
     *
     * @return the configured channel factory injected type configuration
     */
    public InjectedTypeConfiguration<ChannelFactory<? extends Channel>> getChannelFactoryInjectedTypeConfiguration() {
        return channelFactoryInjectedTypeConfiguration;
    }

    /**
     * Returns the configured event loop group configuration.
     *
     * @return the configured event loop group configuration
     */
    public EventLoopGroupConfiguration getEventLoopGroupConfiguration() {
        return eventLoopGroupConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkEventLoopGroupConfiguration that = (AwsSdkEventLoopGroupConfiguration) o;
        return useBuilder == that.useBuilder &&
                useSharedEventLoopGroup == that.useSharedEventLoopGroup &&
                Objects.equals(threadCount, that.threadCount) &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration) &&
                Objects.equals(channelFactoryInjectedTypeConfiguration, that.channelFactoryInjectedTypeConfiguration) &&
                Objects.equals(eventLoopGroupConfiguration, that.eventLoopGroupConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                useBuilder,
                threadCount,
                threadFactoryConfiguration,
                channelFactoryInjectedTypeConfiguration,
                useSharedEventLoopGroup,
                eventLoopGroupConfiguration);
    }

    @Override
    public String toString() {
        return "AwsSdkEventLoopGroupConfiguration{" +
                "useBuilder=" + useBuilder +
                ", threadCount=" + threadCount +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                ", useSharedEventLoopGroup=" + useSharedEventLoopGroup +
                ", channelFactoryInjectedTypeConfiguration=" + channelFactoryInjectedTypeConfiguration +
                ", eventLoopGroupConfiguration=" + eventLoopGroupConfiguration +
                '}';
    }


    /**
     * Builder for {@link AwsSdkEventLoopGroupConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_USE_BUILDER = false;
        private static final boolean DEFAULT_USE_SHARED_EVENT_LOOP_GROUP = true;


        private boolean useBuilder = DEFAULT_USE_BUILDER;
        private Integer threadCount;
        private ThreadFactoryConfiguration threadFactoryConfiguration;
        private boolean useSharedEventLoopGroup = DEFAULT_USE_SHARED_EVENT_LOOP_GROUP;
        private InjectedTypeConfiguration<ChannelFactory<? extends Channel>> channelFactoryInjectedTypeConfiguration;
        private EventLoopGroupConfiguration eventLoopGroupConfiguration;


        private Builder() {
        }

        public Builder useBuilder(final boolean useBuilder) {
            this.useBuilder = useBuilder;
            return this;
        }

        public Builder threadCount(final Integer threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public Builder useSharedEventLoopGroup(final boolean useSharedEventLoopGroup) {
            this.useSharedEventLoopGroup = useSharedEventLoopGroup;
            return this;
        }

        public Builder channelFactoryInjectedTypeConfiguration(
                final InjectedTypeConfiguration<ChannelFactory<? extends Channel>> channelFactoryInjectedTypeConfiguration)
        {
            this.channelFactoryInjectedTypeConfiguration = channelFactoryInjectedTypeConfiguration;
            return this;
        }

        public Builder eventLoopGroupConfiguration(final EventLoopGroupConfiguration eventLoopGroupConfiguration) {
            this.eventLoopGroupConfiguration = eventLoopGroupConfiguration;
            return this;
        }

        public AwsSdkEventLoopGroupConfiguration build() {
            return new AwsSdkEventLoopGroupConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
