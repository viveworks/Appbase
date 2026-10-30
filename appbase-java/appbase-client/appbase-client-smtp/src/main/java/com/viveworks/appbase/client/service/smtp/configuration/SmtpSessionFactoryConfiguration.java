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

package com.viveworks.appbase.client.service.smtp.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.hubspot.smtp.client.SmtpSessionFactoryConfig;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.buffer.BufferAllocatorConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslClientContextConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.Channel;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link SmtpSessionFactoryConfig}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SmtpSessionFactoryConfiguration.Builder.class)
public final class SmtpSessionFactoryConfiguration {
    private final boolean useSharedBufferAllocator;
    private final BufferAllocatorConfiguration customBufferAllocatorConfiguration;
    private final Class<? extends Channel> channelType;
    private final boolean useSharedEventLoopGroup;
    private final EventLoopGroupConfiguration customEventLoopGroupConfiguration;
    private final boolean useSharedExecutorServiceConfiguration;
    private final ExecutorServiceConfiguration customExecutorServiceConfiguration;
    private final SslClientContextConfiguration sslClientContextConfiguration;


    private SmtpSessionFactoryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (!builder.useSharedBufferAllocator) {
            AppbaseUtil.notNull(builder.customBufferAllocatorConfiguration, "builder.customBufferAllocatorConfiguration");
        }

        if (!builder.useSharedEventLoopGroup) {
            AppbaseUtil.notNull(builder.customEventLoopGroupConfiguration, "builder.customEventLoopGroupConfiguration");
        }

        if (!builder.useSharedExecutorServiceConfiguration) {
            AppbaseUtil.notNull(builder.customExecutorServiceConfiguration, "builder.customExecutorServiceConfiguration");
        }

        useSharedBufferAllocator = builder.useSharedBufferAllocator;
        customBufferAllocatorConfiguration = builder.customBufferAllocatorConfiguration;
        channelType = builder.channelType;
        useSharedEventLoopGroup = builder.useSharedEventLoopGroup;
        customEventLoopGroupConfiguration = builder.customEventLoopGroupConfiguration;
        useSharedExecutorServiceConfiguration = builder.useSharedExecutorServiceConfiguration;
        customExecutorServiceConfiguration = builder.customExecutorServiceConfiguration;
        sslClientContextConfiguration = builder.sslClientContextConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isUseSharedBufferAllocator() {
        return useSharedBufferAllocator;
    }

    public BufferAllocatorConfiguration getCustomBufferAllocatorConfiguration() {
        return customBufferAllocatorConfiguration;
    }

    public Class<? extends Channel> getChannelType() {
        return channelType;
    }

    public boolean isUseSharedEventLoopGroup() {
        return useSharedEventLoopGroup;
    }

    public EventLoopGroupConfiguration getCustomEventLoopGroupConfiguration() {
        return customEventLoopGroupConfiguration;
    }

    public boolean isUseSharedExecutorServiceConfiguration() {
        return useSharedExecutorServiceConfiguration;
    }

    public ExecutorServiceConfiguration getCustomExecutorServiceConfiguration() {
        return customExecutorServiceConfiguration;
    }

    public SslClientContextConfiguration getSslClientContextConfiguration() {
        return sslClientContextConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SmtpSessionFactoryConfiguration that = (SmtpSessionFactoryConfiguration) o;
        return useSharedBufferAllocator == that.useSharedBufferAllocator &&
                useSharedEventLoopGroup == that.useSharedEventLoopGroup &&
                useSharedExecutorServiceConfiguration == that.useSharedExecutorServiceConfiguration &&
                Objects.equals(customBufferAllocatorConfiguration, that.customBufferAllocatorConfiguration) &&
                Objects.equals(channelType, that.channelType) &&
                Objects.equals(customEventLoopGroupConfiguration, that.customEventLoopGroupConfiguration) &&
                Objects.equals(customExecutorServiceConfiguration, that.customExecutorServiceConfiguration) &&
                Objects.equals(sslClientContextConfiguration, that.sslClientContextConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                useSharedBufferAllocator,
                customBufferAllocatorConfiguration,
                channelType,
                useSharedEventLoopGroup,
                customEventLoopGroupConfiguration,
                useSharedExecutorServiceConfiguration,
                customExecutorServiceConfiguration,
                sslClientContextConfiguration);
    }

    @Override
    public String toString() {
        return "SmtpSessionFactoryConfiguration{" +
                "useSharedBufferAllocator=" + useSharedBufferAllocator +
                ", customBufferAllocatorConfiguration=" + customBufferAllocatorConfiguration +
                ", channelType=" + channelType +
                ", useSharedEventLoopGroup=" + useSharedEventLoopGroup +
                ", customEventLoopGroupConfiguration=" + customEventLoopGroupConfiguration +
                ", useSharedExecutorServiceConfiguration=" + useSharedExecutorServiceConfiguration +
                ", customExecutorServiceConfiguration=" + customExecutorServiceConfiguration +
                ", sslClientContextConfiguration=" + sslClientContextConfiguration +
                '}';
    }



    /**
     * Builder for {@link SmtpSessionFactoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_USE_SHARED_BUFFER_ALLOCATOR = true;
        private static final Class<? extends Channel> DEFAULT_CHANNEL_TYPE = AppbaseNetUtil.channelType();
        private static final boolean DEFAULT_USE_SHARED_EVENT_LOOP_GROUP = true;
        private static final boolean DEFAULT_USE_SHARED_EXECUTOR_SERVICE_CONFIGURATION = true;



        private boolean useSharedBufferAllocator = DEFAULT_USE_SHARED_BUFFER_ALLOCATOR;
        private BufferAllocatorConfiguration customBufferAllocatorConfiguration;
        private Class<? extends Channel> channelType = DEFAULT_CHANNEL_TYPE;
        private boolean useSharedEventLoopGroup = DEFAULT_USE_SHARED_EVENT_LOOP_GROUP;
        private EventLoopGroupConfiguration customEventLoopGroupConfiguration;
        private boolean useSharedExecutorServiceConfiguration = DEFAULT_USE_SHARED_EXECUTOR_SERVICE_CONFIGURATION;
        private ExecutorServiceConfiguration customExecutorServiceConfiguration;
        private SslClientContextConfiguration sslClientContextConfiguration;


        private Builder() {
        }

        public Builder useSharedBufferAllocator(final boolean useSharedBufferAllocator) {
            this.useSharedBufferAllocator = useSharedBufferAllocator;
            return this;
        }

        public Builder customBufferAllocatorConfiguration(
                final BufferAllocatorConfiguration customBufferAllocatorConfiguration)
        {
            this.customBufferAllocatorConfiguration = customBufferAllocatorConfiguration;
            return this;
        }

        public Builder channelType(final Class<? extends Channel> channelType) {
            this.channelType = channelType;
            return this;
        }

        public Builder useSharedEventLoopGroup(final boolean useSharedEventLoopGroup) {
            this.useSharedEventLoopGroup = useSharedEventLoopGroup;
            return this;
        }

        public Builder customEventLoopGroupConfiguration(
                final EventLoopGroupConfiguration customEventLoopGroupConfiguration)
        {
            this.customEventLoopGroupConfiguration = customEventLoopGroupConfiguration;
            return this;
        }

        public Builder useSharedExecutorServiceConfiguration(final boolean useSharedExecutorServiceConfiguration) {
            this.useSharedExecutorServiceConfiguration = useSharedExecutorServiceConfiguration;
            return this;
        }

        public Builder customExecutorServiceConfiguration(
                final ExecutorServiceConfiguration customExecutorServiceConfiguration)
        {
            this.customExecutorServiceConfiguration = customExecutorServiceConfiguration;
            return this;
        }

        public Builder sslClientContextConfiguration(final SslClientContextConfiguration sslClientContextConfiguration) {
            this.sslClientContextConfiguration = sslClientContextConfiguration;
            return this;
        }

        public SmtpSessionFactoryConfiguration build() {
            return new SmtpSessionFactoryConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
