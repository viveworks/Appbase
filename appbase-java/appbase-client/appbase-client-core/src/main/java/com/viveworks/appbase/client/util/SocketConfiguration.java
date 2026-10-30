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

package com.viveworks.appbase.client.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents {@link Socket} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SocketConfiguration.Builder.class)
public final class SocketConfiguration {
    private final Boolean keepAlive;
    private final Boolean reuseAddress;
    private final DataSizeConfiguration receiveBufferDataSizeConfiguration;
    private final DataSizeConfiguration sendBufferDataSizeConfiguration;
    private final Boolean linger;
    private final TimeUnitConfiguration lingerTimeUnitConfiguration;
    private final TimeoutConfiguration connectTimeoutConfiguration;
    private final Boolean tcpNoDelay;

    private SocketConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.linger != null && builder.linger) {
            AppbaseUtil.notNull(builder.lingerTimeUnitConfiguration, "builder.lingerTimeUnitConfiguration");
            AppbaseUtil.notFalse(
                    builder.lingerTimeUnitConfiguration.isEnabled(),
                    "builder.lingerTimeUnitConfiguration.isEnabled()");
        }

        keepAlive = builder.keepAlive;
        reuseAddress = builder.reuseAddress;
        receiveBufferDataSizeConfiguration = builder.receiveBufferDataSizeConfiguration;
        sendBufferDataSizeConfiguration = builder.sendBufferDataSizeConfiguration;
        linger = builder.linger;
        lingerTimeUnitConfiguration = builder.lingerTimeUnitConfiguration;
        connectTimeoutConfiguration = builder.connectTimeoutConfiguration;
        tcpNoDelay = builder.tcpNoDelay;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public Boolean getReuseAddress() {
        return reuseAddress;
    }

    public DataSizeConfiguration getReceiveBufferDataSizeConfiguration() {
        return receiveBufferDataSizeConfiguration;
    }

    public DataSizeConfiguration getSendBufferDataSizeConfiguration() {
        return sendBufferDataSizeConfiguration;
    }

    public Boolean getLinger() {
        return linger;
    }

    public TimeUnitConfiguration getLingerTimeUnitConfiguration() {
        return lingerTimeUnitConfiguration;
    }

    public TimeoutConfiguration getConnectTimeoutConfiguration() {
        return connectTimeoutConfiguration;
    }

    public Boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SocketConfiguration that = (SocketConfiguration) o;
        return Objects.equals(keepAlive, that.keepAlive) &&
                Objects.equals(reuseAddress, that.reuseAddress) &&
                Objects.equals(receiveBufferDataSizeConfiguration, that.receiveBufferDataSizeConfiguration) &&
                Objects.equals(sendBufferDataSizeConfiguration, that.sendBufferDataSizeConfiguration) &&
                Objects.equals(linger, that.linger) &&
                Objects.equals(lingerTimeUnitConfiguration, that.lingerTimeUnitConfiguration) &&
                Objects.equals(connectTimeoutConfiguration, that.connectTimeoutConfiguration) &&
                Objects.equals(tcpNoDelay, that.tcpNoDelay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                keepAlive,
                reuseAddress,
                receiveBufferDataSizeConfiguration,
                sendBufferDataSizeConfiguration,
                linger,
                lingerTimeUnitConfiguration,
                connectTimeoutConfiguration,
                tcpNoDelay);
    }

    @Override
    public String toString() {
        return "SocketConfiguration{" +
                "keepAlive=" + keepAlive +
                ", reuseAddress=" + reuseAddress +
                ", receiveBufferDataSizeConfiguration=" + receiveBufferDataSizeConfiguration +
                ", sendBufferDataSizeConfiguration=" + sendBufferDataSizeConfiguration +
                ", linger=" + linger +
                ", lingerTimeUnitConfiguration=" + lingerTimeUnitConfiguration +
                ", connectTimeoutConfiguration=" + connectTimeoutConfiguration +
                ", tcpNoDelay=" + tcpNoDelay +
                '}';
    }

    /**
     * Builder for {@link SocketConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Boolean DEFAULT_KEEP_ALIVE = false;
        private static final Boolean DEFAULT_REUSE_ADDRESS = false;
        private static final DataSizeConfiguration DEFAULT_RECEIVE_BUFFER_DATA_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(87380)
                .unit(DataSizeConfiguration.Unit.BYTES)
                .build();
        private static final DataSizeConfiguration DEFAULT_SEND_BUFFER_DATA_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(16384)
                .unit(DataSizeConfiguration.Unit.BYTES)
                .build();
        private static final Boolean DEFAULT_LINGER = false;
        private static final TimeUnitConfiguration DEFAULT_LINGER_TIME_UNIT_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(false)
                .build();
        private static final TimeoutConfiguration DEFAULT_CONNECT_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(10)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final Boolean DEFAULT_TCP_NO_DELAY = false;


        private Boolean keepAlive = DEFAULT_KEEP_ALIVE;
        private Boolean reuseAddress = DEFAULT_REUSE_ADDRESS;
        private DataSizeConfiguration receiveBufferDataSizeConfiguration = DEFAULT_RECEIVE_BUFFER_DATA_SIZE_CONFIGURATION;
        private DataSizeConfiguration sendBufferDataSizeConfiguration = DEFAULT_SEND_BUFFER_DATA_SIZE_CONFIGURATION;
        private Boolean linger = DEFAULT_LINGER;
        private TimeUnitConfiguration lingerTimeUnitConfiguration = DEFAULT_LINGER_TIME_UNIT_CONFIGURATION;
        private TimeoutConfiguration connectTimeoutConfiguration = DEFAULT_CONNECT_TIMEOUT_CONFIGURATION;
        private Boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;


        private Builder() {
        }

        public Builder keepAlive(final Boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder reuseAddress(final Boolean reuseAddress) {
            this.reuseAddress = reuseAddress;
            return this;
        }

        public Builder receiveBufferDataSizeConfiguration(final DataSizeConfiguration receiveBufferDataSizeConfiguration) {
            this.receiveBufferDataSizeConfiguration = receiveBufferDataSizeConfiguration;
            return this;
        }

        public Builder sendBufferDataSizeConfiguration(final DataSizeConfiguration sendBufferDataSizeConfiguration) {
            this.sendBufferDataSizeConfiguration = sendBufferDataSizeConfiguration;
            return this;
        }

        public Builder linger(final Boolean linger) {
            this.linger = linger;
            return this;
        }

        public Builder lingerTimeUnitConfiguration(final TimeUnitConfiguration lingerTimeUnitConfiguration) {
            this.lingerTimeUnitConfiguration = lingerTimeUnitConfiguration;
            return this;
        }

        public Builder connectTimeoutConfiguration(final TimeoutConfiguration connectTimeoutConfiguration) {
            this.connectTimeoutConfiguration = connectTimeoutConfiguration;
            return this;
        }

        public Builder tcpNoDelay(final Boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        public SocketConfiguration build() {
            return new SocketConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
