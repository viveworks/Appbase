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

package com.viveworks.appbase.core.util.net;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.ChannelOption;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Netty channel options applied via {@code io.netty.channel.ChannelOption}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ChannelOptionsConfiguration.Builder.class)
public final class ChannelOptionsConfiguration {
    @Nullable
    private final Integer connectTimeoutMillis;

    @Nullable
    private final Boolean autoRead;

    @Nullable
    private final Boolean autoClose;

    @Nullable
    private final Integer socketBacklog;

    @Nullable
    private final Integer socketLingerSeconds;

    @Nullable
    private final Integer socketReceiveBufferBytes;

    @Nullable
    private final Integer socketSendBufferBytes;

    @Nullable
    private final TimeoutConfiguration socketTimeoutConfiguration;

    @Nullable
    private final Boolean socketBroadcast;

    @Nullable
    private final Boolean socketKeepAlive;

    @Nullable
    private final Boolean socketReuseAddress;

    @Nullable
    private final Boolean singleEventExecutorPerGroup;

    private ChannelOptionsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        connectTimeoutMillis = builder.connectTimeoutMillis;
        autoRead = builder.autoRead;
        autoClose = builder.autoClose;
        socketBacklog = builder.socketBacklog;
        socketLingerSeconds = builder.socketLingerSeconds;
        socketReceiveBufferBytes = builder.socketReceiveBufferBytes;
        socketSendBufferBytes = builder.socketSendBufferBytes;
        socketTimeoutConfiguration = builder.socketTimeoutConfiguration;
        socketBroadcast = builder.socketBroadcast;
        socketKeepAlive = builder.socketKeepAlive;
        socketReuseAddress = builder.socketReuseAddress;
        singleEventExecutorPerGroup = builder.singleEventExecutorPerGroup;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    @Nullable
    public Boolean getAutoRead() {
        return autoRead;
    }

    @Nullable
    public Boolean getAutoClose() {
        return autoClose;
    }

    @Nullable
    public Integer getSocketBacklog() {
        return socketBacklog;
    }

    @Nullable
    public Integer getSocketLingerSeconds() {
        return socketLingerSeconds;
    }

    @Nullable
    public Integer getSocketReceiveBufferBytes() {
        return socketReceiveBufferBytes;
    }

    @Nullable
    public Integer getSocketSendBufferBytes() {
        return socketSendBufferBytes;
    }

    @Nullable
    public TimeoutConfiguration getSocketTimeoutConfiguration() {
        return socketTimeoutConfiguration;
    }

    @Nullable
    public Boolean getSocketBroadcast() {
        return socketBroadcast;
    }

    @Nullable
    public Boolean getSocketKeepAlive() {
        return socketKeepAlive;
    }

    @Nullable
    public Boolean getSocketReuseAddress() {
        return socketReuseAddress;
    }

    @Nullable
    public Boolean getSingleEventExecutorPerGroup() {
        return singleEventExecutorPerGroup;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ChannelOptionsConfiguration that = (ChannelOptionsConfiguration) o;
        return Objects.equals(connectTimeoutMillis, that.connectTimeoutMillis) &&
                Objects.equals(autoRead, that.autoRead) &&
                Objects.equals(autoClose, that.autoClose) &&
                Objects.equals(socketBacklog, that.socketBacklog) &&
                Objects.equals(socketLingerSeconds, that.socketLingerSeconds) &&
                Objects.equals(socketReceiveBufferBytes, that.socketReceiveBufferBytes) &&
                Objects.equals(socketSendBufferBytes, that.socketSendBufferBytes) &&
                Objects.equals(socketTimeoutConfiguration, that.socketTimeoutConfiguration) &&
                Objects.equals(socketBroadcast, that.socketBroadcast) &&
                Objects.equals(socketKeepAlive, that.socketKeepAlive) &&
                Objects.equals(socketReuseAddress, that.socketReuseAddress) &&
                Objects.equals(singleEventExecutorPerGroup, that.singleEventExecutorPerGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                connectTimeoutMillis,
                autoRead,
                autoClose,
                socketBacklog,
                socketLingerSeconds,
                socketReceiveBufferBytes,
                socketSendBufferBytes,
                socketTimeoutConfiguration,
                socketBroadcast,
                socketKeepAlive,
                socketReuseAddress,
                singleEventExecutorPerGroup);
    }

    @Override
    public String toString() {
        return "ChannelOptionsConfiguration{" +
                "connectTimeoutMillis=" + connectTimeoutMillis +
                ", autoRead=" + autoRead +
                ", autoClose=" + autoClose +
                ", socketBacklog=" + socketBacklog +
                ", socketLingerSeconds=" + socketLingerSeconds +
                ", socketReceiveBufferBytes=" + socketReceiveBufferBytes +
                ", socketSendBufferBytes=" + socketSendBufferBytes +
                ", socketTimeoutConfiguration=" + socketTimeoutConfiguration +
                ", socketBroadcast=" + socketBroadcast +
                ", socketKeepAlive=" + socketKeepAlive +
                ", socketReuseAddress=" + socketReuseAddress +
                ", singleEventExecutorPerGroup=" + singleEventExecutorPerGroup +
                '}';
    }


    /**
     * Builder for {@link ChannelOptionsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_SINGLE_EVENT_EXECUTOR_PER_GROUP = true;
        private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 7200;
        private static final boolean DEFAULT_AUTO_READ = true;
        private static final boolean DEFAULT_AUTO_CLOSE = true;
        private static final int DEFAULT_SOCKET_BACKLOG = 512;
        private static final int DEFAULT_SOCKET_LINGER_SECONDS = 5;
        private static final int DEFAULT_SOCKET_RECEIVE_BUFFER_BYTES = 87380;
        private static final int DEFAULT_SOCKET_SEND_BUFFER_BYTES = 16384;
        private static final boolean DEFAULT_SOCKET_BROADCAST = false;
        private static final boolean DEFAULT_SOCKET_KEEP_ALIVE = true;
        private static final boolean DEFAULT_SOCKET_REUSE_ADDRESS = false;

        private Integer connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
        private Boolean autoRead = DEFAULT_AUTO_READ;
        private Boolean autoClose = DEFAULT_AUTO_CLOSE;
        private Integer socketBacklog = DEFAULT_SOCKET_BACKLOG;
        private Integer socketLingerSeconds = DEFAULT_SOCKET_LINGER_SECONDS;
        private Integer socketReceiveBufferBytes = DEFAULT_SOCKET_RECEIVE_BUFFER_BYTES;
        private Integer socketSendBufferBytes = DEFAULT_SOCKET_SEND_BUFFER_BYTES;
        private TimeoutConfiguration socketTimeoutConfiguration;
        private Boolean socketBroadcast = DEFAULT_SOCKET_BROADCAST;
        private Boolean socketKeepAlive = DEFAULT_SOCKET_KEEP_ALIVE;
        private Boolean socketReuseAddress = DEFAULT_SOCKET_REUSE_ADDRESS;
        private Boolean singleEventExecutorPerGroup = DEFAULT_SINGLE_EVENT_EXECUTOR_PER_GROUP;

        private Builder() {
        }

        public Builder connectTimeoutMillis(final Integer connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Builder autoRead(final Boolean autoRead) {
            this.autoRead = autoRead;
            return this;
        }

        public Builder autoClose(final Boolean autoClose) {
            this.autoClose = autoClose;
            return this;
        }

        public Builder socketBacklog(final Integer socketBacklog) {
            this.socketBacklog = socketBacklog;
            return this;
        }

        public Builder socketLingerSeconds(final Integer socketLingerSeconds) {
            this.socketLingerSeconds = socketLingerSeconds;
            return this;
        }

        public Builder socketReceiveBufferBytes(final Integer socketReceiveBufferBytes) {
            this.socketReceiveBufferBytes = socketReceiveBufferBytes;
            return this;
        }

        public Builder socketSendBufferBytes(final Integer socketSendBufferBytes) {
            this.socketSendBufferBytes = socketSendBufferBytes;
            return this;
        }

        public Builder socketTimeoutConfiguration(final TimeoutConfiguration socketTimeoutConfiguration) {
            this.socketTimeoutConfiguration = socketTimeoutConfiguration;
            return this;
        }

        public Builder socketBroadcast(final Boolean socketBroadcast) {
            this.socketBroadcast = socketBroadcast;
            return this;
        }

        public Builder socketKeepAlive(final Boolean socketKeepAlive) {
            this.socketKeepAlive = socketKeepAlive;
            return this;
        }

        public Builder socketReuseAddress(final Boolean socketReuseAddress) {
            this.socketReuseAddress = socketReuseAddress;
            return this;
        }

        public Builder singleEventExecutorPerGroup(final Boolean singleEventExecutorPerGroup) {
            this.singleEventExecutorPerGroup = singleEventExecutorPerGroup;
            return this;
        }

        public ChannelOptionsConfiguration build() {
            return new ChannelOptionsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
