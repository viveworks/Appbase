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

package com.viveworks.appbase.bootstrap.reactornetty.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@code ReactorNettyAppbaseWebServer}. Every knob has a sensible default; the
 * whole block is optional.
 *
 * <p>TLS, HTTP/2, WebSocket, access-log formats and custom event-loop sizing are deliberately
 * absent. reactor-netty supports all of them; none is needed to prove a non-blocking face, and each
 * is additive later. YAGNI.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ReactorNettyAppbaseWebServerConfiguration.Builder.class)
public class ReactorNettyAppbaseWebServerConfiguration implements AppbaseConfiguration {

    private final boolean enabled;

    private final String host;

    private final int port;

    private final String offloadSchedulerId;

    private final boolean accessLogEnabled;

    private final boolean compressionEnabled;

    private final boolean contextPropagationEnabled;

    private final int maxPageSize;

    private final int offloadTimeoutSeconds;

    private ReactorNettyAppbaseWebServerConfiguration(final Builder builder) {
        enabled = builder.enabled;
        host = builder.host;
        port = builder.port;
        offloadSchedulerId = builder.offloadSchedulerId;
        accessLogEnabled = builder.accessLogEnabled;
        compressionEnabled = builder.compressionEnabled;
        contextPropagationEnabled = builder.contextPropagationEnabled;
        maxPageSize = builder.maxPageSize;
        offloadTimeoutSeconds = builder.offloadTimeoutSeconds;
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
     * @return whether the server should be started at all
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the bind host, e.g. {@code 0.0.0.0}
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the listen port
     */
    public int getPort() {
        return port;
    }

    /**
     * Scheduler used for the Tier-1 offload of blocking endpoints, resolved through {@code
     * ReactorService#getSchedulerById(String)}. {@code null} means {@code
     * ReactorService#getDefaultScheduler()}.
     *
     * @return the scheduler id, or {@code null}
     */
    @Nullable
    public String getOffloadSchedulerId() {
        return offloadSchedulerId;
    }

    /**
     * @return whether reactor-netty access logging is enabled
     */
    public boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }

    /**
     * @return whether response compression is enabled
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * @return whether reactor automatic context propagation is enabled at startup
     */
    public boolean isContextPropagationEnabled() {
        return contextPropagationEnabled;
    }

    /**
     * @return the maximum page size for an offloaded, blocking read
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * @return the offload timeout, in seconds, before an offloaded, blocking read is considered stuck
     */
    public int getOffloadTimeoutSeconds() {
        return offloadTimeoutSeconds;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ReactorNettyAppbaseWebServerConfiguration that = (ReactorNettyAppbaseWebServerConfiguration) o;
        return enabled == that.enabled &&
                port == that.port &&
                accessLogEnabled == that.accessLogEnabled &&
                compressionEnabled == that.compressionEnabled &&
                contextPropagationEnabled == that.contextPropagationEnabled &&
                maxPageSize == that.maxPageSize &&
                offloadTimeoutSeconds == that.offloadTimeoutSeconds &&
                Objects.equals(host, that.host) &&
                Objects.equals(offloadSchedulerId, that.offloadSchedulerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                host,
                port,
                offloadSchedulerId,
                accessLogEnabled,
                compressionEnabled,
                contextPropagationEnabled,
                maxPageSize,
                offloadTimeoutSeconds);
    }

    @Override
    public String toString() {
        return "ReactorNettyAppbaseWebServerConfiguration{" +
                "enabled=" + enabled +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", offloadSchedulerId='" + offloadSchedulerId + '\'' +
                ", accessLogEnabled=" + accessLogEnabled +
                ", compressionEnabled=" + compressionEnabled +
                ", contextPropagationEnabled=" + contextPropagationEnabled +
                ", maxPageSize=" + maxPageSize +
                ", offloadTimeoutSeconds=" + offloadTimeoutSeconds +
                '}';
    }

    /**
     * Builder for {@link ReactorNettyAppbaseWebServerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @author Claude
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {

        private boolean enabled = true;

        private String host = "0.0.0.0";

        private int port = 8080;

        private String offloadSchedulerId;

        private boolean accessLogEnabled;

        private boolean compressionEnabled;

        private boolean contextPropagationEnabled = true;

        private int maxPageSize = 1000;

        private int offloadTimeoutSeconds = 30;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder offloadSchedulerId(final String offloadSchedulerId) {
            this.offloadSchedulerId = offloadSchedulerId;
            return this;
        }

        public Builder accessLogEnabled(final boolean accessLogEnabled) {
            this.accessLogEnabled = accessLogEnabled;
            return this;
        }

        public Builder compressionEnabled(final boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
            return this;
        }

        public Builder contextPropagationEnabled(final boolean contextPropagationEnabled) {
            this.contextPropagationEnabled = contextPropagationEnabled;
            return this;
        }

        public Builder maxPageSize(final int maxPageSize) {
            this.maxPageSize = maxPageSize;
            return this;
        }

        public Builder offloadTimeoutSeconds(final int offloadTimeoutSeconds) {
            this.offloadTimeoutSeconds = offloadTimeoutSeconds;
            return this;
        }

        public ReactorNettyAppbaseWebServerConfiguration build() {
            return new ReactorNettyAppbaseWebServerConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
