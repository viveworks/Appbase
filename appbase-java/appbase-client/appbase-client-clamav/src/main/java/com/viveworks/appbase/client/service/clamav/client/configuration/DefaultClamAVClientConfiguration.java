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

package com.viveworks.appbase.client.service.clamav.client.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.util.SocketConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultClamAVClientConfiguration.Builder.class)
public final class DefaultClamAVClientConfiguration {
    private final HostPortConfiguration hostPortConfiguration;
    private final SocketConfiguration socketConfiguration;
    private final DataSizeConfiguration chunkSizeConfiguration;

    private DefaultClamAVClientConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.hostPortConfiguration, "builder.hostPortConfiguration");
        AppbaseUtil.notNull(builder.socketConfiguration, "builder.socketConfiguration");
        AppbaseUtil.notNull(builder.chunkSizeConfiguration, "builder.chunkSizeConfiguration");
        AppbaseUtil.notFalse(builder.chunkSizeConfiguration.isEnabled(), "builder.chunkSizeConfiguration.isEnabled()");
        AppbaseUtil.positive(builder.chunkSizeConfiguration.getDataSize(), "builder.chunkSizeConfiguration.getDataSize()");

        hostPortConfiguration = builder.hostPortConfiguration;
        socketConfiguration = builder.socketConfiguration;
        chunkSizeConfiguration = builder.chunkSizeConfiguration;
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

    public SocketConfiguration getSocketConfiguration() {
        return socketConfiguration;
    }

    public DataSizeConfiguration getChunkSizeConfiguration() {
        return chunkSizeConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultClamAVClientConfiguration that = (DefaultClamAVClientConfiguration) o;
        return Objects.equals(hostPortConfiguration, that.hostPortConfiguration) &&
                Objects.equals(socketConfiguration, that.socketConfiguration) &&
                Objects.equals(chunkSizeConfiguration, that.chunkSizeConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostPortConfiguration, socketConfiguration, chunkSizeConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultClamAVClientConfiguration{" +
                "hostPortConfiguration=" + hostPortConfiguration +
                ", socketConfiguration=" + socketConfiguration +
                ", chunkSizeConfiguration=" + chunkSizeConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultClamAVClientConfiguration}.
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
                .host("localhost")
                .port(3310)
                .build();
        private static final SocketConfiguration DEFAULT_SOCKET_CONFIGURATION = SocketConfiguration.builder().build();
        private static final DataSizeConfiguration DEFAULT_CHUNK_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(2048)
                .unit(DataSizeConfiguration.Unit.BYTES)
                .build();


        private HostPortConfiguration hostPortConfiguration = DEFAULT_HOST_PORT_CONFIGURATION;
        private SocketConfiguration socketConfiguration = DEFAULT_SOCKET_CONFIGURATION;
        private DataSizeConfiguration chunkSizeConfiguration = DEFAULT_CHUNK_SIZE_CONFIGURATION;


        private Builder() {
        }

        public Builder hostPortConfiguration(final HostPortConfiguration hostPortConfiguration) {
            this.hostPortConfiguration = hostPortConfiguration;
            return this;
        }

        public Builder socketConfiguration(final SocketConfiguration socketConfiguration) {
            this.socketConfiguration = socketConfiguration;
            return this;
        }

        public Builder chunkSizeConfiguration(final DataSizeConfiguration chunkSizeConfiguration) {
            this.chunkSizeConfiguration = chunkSizeConfiguration;
            return this;
        }

        public DefaultClamAVClientConfiguration build() {
            return new DefaultClamAVClientConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
