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

package com.viveworks.appbase.client.cloud.aws.httpclient.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration used to generate {@link Http2Configuration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkHttp2Configuration.Builder.class)
public final class AwsSdkHttp2Configuration {
    private final Long maxStreams;
    private final Integer initialWindowSize;
    private final Duration healthCheckPingPeriod;

    private AwsSdkHttp2Configuration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        maxStreams = builder.maxStreams;
        initialWindowSize = builder.initialWindowSize;
        healthCheckPingPeriod = builder.healthCheckPingPeriod;
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
     * Returns the configured max streams.
     *
     * @return the configured max streams
     */
    public Long getMaxStreams() {
        return maxStreams;
    }

    /**
     * Returns the configured initial window size.
     *
     * @return the configured initial window size
     */
    public Integer getInitialWindowSize() {
        return initialWindowSize;
    }

    /**
     * Returns the configured health check ping period.
     *
     * @return the configured health check ping period
     */
    public Duration getHealthCheckPingPeriod() {
        return healthCheckPingPeriod;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkHttp2Configuration that = (AwsSdkHttp2Configuration) o;
        return Objects.equals(maxStreams, that.maxStreams) &&
                Objects.equals(initialWindowSize, that.initialWindowSize) &&
                Objects.equals(healthCheckPingPeriod, that.healthCheckPingPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxStreams, initialWindowSize, healthCheckPingPeriod);
    }

    @Override
    public String toString() {
        return "AwsSdkHttp2Configuration{" +
                "maxStreams=" + maxStreams +
                ", initialWindowSize=" + initialWindowSize +
                ", healthCheckPingPeriod=" + healthCheckPingPeriod +
                '}';
    }


    /**
     * Builder for {@link AwsSdkHttp2Configuration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Long DEFAULT_MAX_STREAMS = 256L;
        private static final Integer DEFAULT_INITIAL_WINDOW_SIZE = 65535;
        private static final Duration DEFAULT_HEALTH_CHECK_PING_PERIOD =
                Duration.ofSeconds(NettyConfiguration.HTTP2_CONNECTION_PING_TIMEOUT_SECONDS);


        private Long maxStreams = DEFAULT_MAX_STREAMS;
        private Integer initialWindowSize = DEFAULT_INITIAL_WINDOW_SIZE;
        private Duration healthCheckPingPeriod = DEFAULT_HEALTH_CHECK_PING_PERIOD;


        private Builder() {
        }

        public Builder maxStreams(final Long maxStreams) {
            this.maxStreams = maxStreams;
            return this;
        }

        public Builder initialWindowSize(final Integer initialWindowSize) {
            this.initialWindowSize = initialWindowSize;
            return this;
        }

        public Builder healthCheckPingPeriod(final Duration healthCheckPingPeriod) {
            this.healthCheckPingPeriod = healthCheckPingPeriod;
            return this;
        }

        public AwsSdkHttp2Configuration build() {
            return new AwsSdkHttp2Configuration(this);
        }
    }
    // CheckStyle: Builder end
}
