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

package com.viveworks.appbase.core.service.tracer.configuration.reporter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentracing.Tracer;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration for {@link io.jaegertracing.internal.reporters.RemoteReporter}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@RemoteReporterConfiguration.Valid
@JsonDeserialize(builder = RemoteReporterConfiguration.Builder.class)
public final class RemoteReporterConfiguration implements JaegerReporterConfiguration {
    private final boolean enabled;
    private final String agentHost;
    private final int agentPort;
    private final String collectorEndpoint;
    private final int maxPacketSizeBytes;
    private final int flushIntervalMillis;
    private final int closeEnqueueTimeoutMillis;
    private final int maxQueueSize;
    private final String metricsPrefix;
    private final String authToken;
    private final String authUsername;
    private final String authPassword;

    private RemoteReporterConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        RemoteReporterConfigurationValidator.validate(
                builder.enabled,
                builder.agentHost,
                builder.agentPort,
                builder.collectorEndpoint,
                builder.maxPacketSizeBytes,
                builder.flushIntervalMillis,
                builder.closeEnqueueTimeoutMillis,
                builder.maxQueueSize,
                builder.metricsPrefix);

        enabled = builder.enabled;
        agentHost = builder.agentHost;
        agentPort = builder.agentPort;
        collectorEndpoint = builder.collectorEndpoint;
        flushIntervalMillis = builder.flushIntervalMillis;
        maxQueueSize = builder.maxQueueSize;
        maxPacketSizeBytes = builder.maxPacketSizeBytes;
        closeEnqueueTimeoutMillis = builder.closeEnqueueTimeoutMillis;
        metricsPrefix = builder.metricsPrefix;
        authToken = builder.authToken;
        authUsername = builder.authUsername;
        authPassword = builder.authPassword;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAgentHost() {
        return agentHost;
    }

    public int getAgentPort() {
        return agentPort;
    }

    public String getCollectorEndpoint() {
        return collectorEndpoint;
    }

    public int getMaxPacketSizeBytes() {
        return maxPacketSizeBytes;
    }

    public int getFlushIntervalMillis() {
        return flushIntervalMillis;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public int getCloseEnqueueTimeoutMillis() {
        return closeEnqueueTimeoutMillis;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public String getMetricsPrefix() {
        return metricsPrefix;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RemoteReporterConfiguration that = (RemoteReporterConfiguration) o;
        return enabled == that.enabled &&
                maxPacketSizeBytes == that.maxPacketSizeBytes &&
                flushIntervalMillis == that.flushIntervalMillis &&
                closeEnqueueTimeoutMillis == that.closeEnqueueTimeoutMillis &&
                maxQueueSize == that.maxQueueSize &&
                Objects.equals(agentHost, that.agentHost) &&
                Objects.equals(agentPort, that.agentPort) &&
                Objects.equals(collectorEndpoint, that.collectorEndpoint) &&
                Objects.equals(metricsPrefix, that.metricsPrefix) &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(authUsername, that.authUsername) &&
                Objects.equals(authPassword, that.authPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                agentHost,
                agentPort,
                collectorEndpoint,
                maxPacketSizeBytes,
                flushIntervalMillis,
                closeEnqueueTimeoutMillis,
                maxQueueSize,
                metricsPrefix,
                authToken,
                authUsername,
                authPassword);
    }

    @Override
    public String toString() {
        return "RemoteReporterConfiguration{" +
                "enabled=" + enabled +
                ", agentHost='" + agentHost + '\'' +
                ", agentPort=" + agentPort +
                ", collectorEndpoint='" + collectorEndpoint + '\'' +
                ", maxPacketSizeBytes=" + maxPacketSizeBytes +
                ", flushIntervalMillis=" + flushIntervalMillis +
                ", closeEnqueueTimeoutMillis=" + closeEnqueueTimeoutMillis +
                ", maxQueueSize=" + maxQueueSize +
                ", metricsPrefix='" + metricsPrefix + '\'' +
                ", authToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", authUsername='" + authUsername + '\'' +
                ", authPassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                '}';
    }

    /**
     * {@link RemoteReporterConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = false;
        private static final String DEFAULT_AGENT_HOST = "localhost";
        private static final int DEFAULT_AGENT_PORT = 6831;
        private static final int DEFAULT_MAX_PACKET_SIZE_BYTES = 1048576; // 1 MB in Bytes
        private static final int DEFAULT_FLUSH_INTERVAL_MILLIS = 60_000;
        private static final int DEFAULT_MAX_QUEUE_SIZE = 100;
        private static final int DEFAULT_CLOSE_ENQUEUE_TIMEOUT_MILLIS = 1000;
        private static final String DEFAULT_METRICS_PREFIX = Tracer.class.getName() + ".";

        private boolean enabled = DEFAULT_ENABLED;
        private String agentHost = DEFAULT_AGENT_HOST;
        private int agentPort = DEFAULT_AGENT_PORT;
        private String collectorEndpoint;
        private int maxPacketSizeBytes = DEFAULT_MAX_PACKET_SIZE_BYTES;
        private int flushIntervalMillis = DEFAULT_FLUSH_INTERVAL_MILLIS;
        private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
        private int closeEnqueueTimeoutMillis = DEFAULT_CLOSE_ENQUEUE_TIMEOUT_MILLIS;
        private String metricsPrefix = DEFAULT_METRICS_PREFIX;
        private String authToken;
        private String authUsername;
        private String authPassword;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder agentHost(final String agentHost) {
            this.agentHost = agentHost;
            return this;
        }

        public Builder agentPort(final int agentPort) {
            this.agentPort = agentPort;
            return this;
        }

        public Builder collectorEndpoint(final String collectorEndpoint) {
            this.collectorEndpoint = collectorEndpoint;
            return this;
        }

        public Builder maxPacketSizeBytes(final int maxPacketSizeBytes) {
            this.maxPacketSizeBytes = maxPacketSizeBytes;
            return this;
        }

        public Builder flushIntervalMillis(final int flushIntervalMillis) {
            this.flushIntervalMillis = flushIntervalMillis;
            return this;
        }

        public Builder maxQueueSize(final int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public Builder closeEnqueueTimeoutMillis(final int closeEnqueueTimeoutMillis) {
            this.closeEnqueueTimeoutMillis = closeEnqueueTimeoutMillis;
            return this;
        }

        public Builder metricsPrefix(final String metricsPrefix) {
            this.metricsPrefix = metricsPrefix;
            return this;
        }

        public Builder authToken(final String authToken) {
            this.authToken = authToken;
            return this;
        }

        public Builder authUsername(final String authUsername) {
            this.authUsername = authUsername;
            return this;
        }

        public Builder authPassword(final String authPassword) {
            this.authPassword = authPassword;
            return this;
        }

        public RemoteReporterConfiguration build() {
            return new RemoteReporterConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link RemoteReporterConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = RemoteReporterConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.reporter.configuration." +
                "RemoteReporterConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
