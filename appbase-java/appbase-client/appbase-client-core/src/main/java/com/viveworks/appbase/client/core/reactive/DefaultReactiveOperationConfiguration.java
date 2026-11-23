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

package com.viveworks.appbase.client.core.reactive;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable {@link ReactiveOperationConfiguration} with sensible zero-config
 * defaults. Deserialized from YAML via its {@link Builder}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@DefaultReactiveOperationConfiguration.Valid
@JsonDeserialize(builder = DefaultReactiveOperationConfiguration.Builder.class)
public final class DefaultReactiveOperationConfiguration implements ReactiveOperationConfiguration {

    public static final String DEFAULT_YAML_NODE_NAME = "defaultReactiveOperationConfiguration";

    private final boolean enabled;
    @Nullable
    private final String schedulerId;
    private final Duration timeout;
    private final boolean retryEnabled;
    private final int retryMaxAttempts;
    private final Duration retryMinBackoff;
    private final Duration retryMaxBackoff;
    private final double retryJitterFactor;
    private final int backpressureBufferSize;
    private final boolean metricsEnabled;
    private final boolean traceEnabled;
    private final boolean exceptionStoreEnabled;

    private DefaultReactiveOperationConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultReactiveOperationConfigurationValidator.validate(
                builder.enabled,
                builder.schedulerId,
                builder.timeout,
                builder.retryEnabled,
                builder.retryMaxAttempts,
                builder.retryMinBackoff,
                builder.retryMaxBackoff,
                builder.retryJitterFactor,
                builder.backpressureBufferSize);
        enabled = builder.enabled;
        schedulerId = builder.schedulerId;
        timeout = builder.timeout;
        retryEnabled = builder.retryEnabled;
        retryMaxAttempts = builder.retryMaxAttempts;
        retryMinBackoff = builder.retryMinBackoff;
        retryMaxBackoff = builder.retryMaxBackoff;
        retryJitterFactor = builder.retryJitterFactor;
        backpressureBufferSize = builder.backpressureBufferSize;
        metricsEnabled = builder.metricsEnabled;
        traceEnabled = builder.traceEnabled;
        exceptionStoreEnabled = builder.exceptionStoreEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @Nullable
    public String getSchedulerId() {
        return schedulerId;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    @Override
    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    @Override
    public Duration getRetryMinBackoff() {
        return retryMinBackoff;
    }

    @Override
    public Duration getRetryMaxBackoff() {
        return retryMaxBackoff;
    }

    @Override
    public double getRetryJitterFactor() {
        return retryJitterFactor;
    }

    @Override
    public int getBackpressureBufferSize() {
        return backpressureBufferSize;
    }

    @Override
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    @Override
    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    @Override
    public boolean isExceptionStoreEnabled() {
        return exceptionStoreEnabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultReactiveOperationConfiguration that = (DefaultReactiveOperationConfiguration) o;
        return enabled == that.enabled
                && retryEnabled == that.retryEnabled
                && retryMaxAttempts == that.retryMaxAttempts
                && Double.compare(that.retryJitterFactor, retryJitterFactor) == 0
                && backpressureBufferSize == that.backpressureBufferSize
                && metricsEnabled == that.metricsEnabled
                && traceEnabled == that.traceEnabled
                && exceptionStoreEnabled == that.exceptionStoreEnabled
                && Objects.equals(schedulerId, that.schedulerId)
                && Objects.equals(timeout, that.timeout)
                && Objects.equals(retryMinBackoff, that.retryMinBackoff)
                && Objects.equals(retryMaxBackoff, that.retryMaxBackoff);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, schedulerId, timeout, retryEnabled, retryMaxAttempts,
                retryMinBackoff, retryMaxBackoff, retryJitterFactor, backpressureBufferSize,
                metricsEnabled, traceEnabled, exceptionStoreEnabled);
    }

    @Override
    public String toString() {
        return "DefaultReactiveOperationConfiguration{"
                + "enabled=" + enabled
                + ", schedulerId='" + schedulerId + '\''
                + ", timeout=" + timeout
                + ", retryEnabled=" + retryEnabled
                + ", retryMaxAttempts=" + retryMaxAttempts
                + ", retryMinBackoff=" + retryMinBackoff
                + ", retryMaxBackoff=" + retryMaxBackoff
                + ", retryJitterFactor=" + retryJitterFactor
                + ", backpressureBufferSize=" + backpressureBufferSize
                + ", metricsEnabled=" + metricsEnabled
                + ", traceEnabled=" + traceEnabled
                + ", exceptionStoreEnabled=" + exceptionStoreEnabled
                + '}';
    }

    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        @Nullable
        private static final String DEFAULT_SCHEDULER_ID = null;
        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
        private static final boolean DEFAULT_RETRY_ENABLED = true;
        private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 3;
        private static final Duration DEFAULT_RETRY_MIN_BACKOFF = Duration.ofMillis(100);
        private static final Duration DEFAULT_RETRY_MAX_BACKOFF = Duration.ofSeconds(5);
        private static final double DEFAULT_RETRY_JITTER_FACTOR = 0.5d;
        private static final int DEFAULT_BACKPRESSURE_BUFFER_SIZE = 256;
        private static final boolean DEFAULT_METRICS_ENABLED = true;
        private static final boolean DEFAULT_TRACE_ENABLED = true;
        private static final boolean DEFAULT_EXCEPTION_STORE_ENABLED = false;

        private boolean enabled = DEFAULT_ENABLED;
        @Nullable
        private String schedulerId = DEFAULT_SCHEDULER_ID;
        private Duration timeout = DEFAULT_TIMEOUT;
        private boolean retryEnabled = DEFAULT_RETRY_ENABLED;
        private int retryMaxAttempts = DEFAULT_RETRY_MAX_ATTEMPTS;
        private Duration retryMinBackoff = DEFAULT_RETRY_MIN_BACKOFF;
        private Duration retryMaxBackoff = DEFAULT_RETRY_MAX_BACKOFF;
        private double retryJitterFactor = DEFAULT_RETRY_JITTER_FACTOR;
        private int backpressureBufferSize = DEFAULT_BACKPRESSURE_BUFFER_SIZE;
        private boolean metricsEnabled = DEFAULT_METRICS_ENABLED;
        private boolean traceEnabled = DEFAULT_TRACE_ENABLED;
        private boolean exceptionStoreEnabled = DEFAULT_EXCEPTION_STORE_ENABLED;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder schedulerId(@Nullable final String schedulerId) {
            this.schedulerId = schedulerId;
            return this;
        }

        public Builder timeout(final Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retryEnabled(final boolean retryEnabled) {
            this.retryEnabled = retryEnabled;
            return this;
        }

        public Builder retryMaxAttempts(final int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
            return this;
        }

        public Builder retryMinBackoff(final Duration retryMinBackoff) {
            this.retryMinBackoff = retryMinBackoff;
            return this;
        }

        public Builder retryMaxBackoff(final Duration retryMaxBackoff) {
            this.retryMaxBackoff = retryMaxBackoff;
            return this;
        }

        public Builder retryJitterFactor(final double retryJitterFactor) {
            this.retryJitterFactor = retryJitterFactor;
            return this;
        }

        public Builder backpressureBufferSize(final int backpressureBufferSize) {
            this.backpressureBufferSize = backpressureBufferSize;
            return this;
        }

        public Builder metricsEnabled(final boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
            return this;
        }

        public Builder traceEnabled(final boolean traceEnabled) {
            this.traceEnabled = traceEnabled;
            return this;
        }

        public Builder exceptionStoreEnabled(final boolean exceptionStoreEnabled) {
            this.exceptionStoreEnabled = exceptionStoreEnabled;
            return this;
        }

        public DefaultReactiveOperationConfiguration build() {
            return new DefaultReactiveOperationConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultReactiveOperationConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{"
                + "com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration.message"
                + "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
