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

package com.viveworks.appbase.core.service.tracer.configuration.sampler;

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
 * Configuration for {@link io.jaegertracing.internal.samplers.RemoteControlledSampler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@RemoteControlledSamplerConfiguration.Valid
@JsonDeserialize(builder = RemoteControlledSamplerConfiguration.Builder.class)
public final class RemoteControlledSamplerConfiguration implements JaegerSamplerConfiguration {
    private final String hostPort;
    private final String serviceName;
    private final int pollingIntervalMillis;
    private final String metricsPrefix;

    private RemoteControlledSamplerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        RemoteControlledSamplerConfigurationValidator.validate(
                builder.hostPort,
                builder.serviceName,
                builder.pollingIntervalMillis,
                builder.metricsPrefix);

        hostPort = builder.hostPort;
        serviceName = builder.serviceName;
        pollingIntervalMillis = builder.pollingIntervalMillis;
        metricsPrefix = builder.metricsPrefix;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getHostPort() {
        return hostPort;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
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
        final RemoteControlledSamplerConfiguration that = (RemoteControlledSamplerConfiguration) o;
        return pollingIntervalMillis == that.pollingIntervalMillis &&
                Objects.equals(hostPort, that.hostPort) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(metricsPrefix, that.metricsPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostPort, serviceName, pollingIntervalMillis, metricsPrefix);
    }

    @Override
    public String toString() {
        return "RemoteControlledSamplerConfiguration{" +
                "hostPort='" + hostPort + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", pollingIntervalMillis=" + pollingIntervalMillis +
                ", metricsPrefix='" + metricsPrefix + '\'' +
                '}';
    }

    /**
     * {@link RemoteControlledSamplerConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_HOST_PORT = "localhost:5778";
        private static final String DEFAULT_SERVICE_NAME = "AppbaseService";
        private static final int DEFAULT_POLLING_INTERVAL_MILLIS = 60000;
        private static final String DEFAULT_METRICS_PREFIX = Tracer.class.getName() + ".";

        private String hostPort = DEFAULT_HOST_PORT;
        private String serviceName = DEFAULT_SERVICE_NAME;
        private int pollingIntervalMillis = DEFAULT_POLLING_INTERVAL_MILLIS;
        private String metricsPrefix = DEFAULT_METRICS_PREFIX;

        private Builder() {
        }

        public Builder hostPort(final String hostPort) {
            this.hostPort = hostPort;
            return this;
        }

        public Builder serviceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder pollingIntervalMillis(final int pollingIntervalMillis) {
            this.pollingIntervalMillis = pollingIntervalMillis;
            return this;
        }

        public Builder metricsPrefix(final String metricsPrefix) {
            this.metricsPrefix = metricsPrefix;
            return this;
        }

        public RemoteControlledSamplerConfiguration build() {
            return new RemoteControlledSamplerConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link RemoteControlledSamplerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = RemoteControlledSamplerConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.tracer.configuration.sampler." +
                "RemoteControlledSamplerConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
