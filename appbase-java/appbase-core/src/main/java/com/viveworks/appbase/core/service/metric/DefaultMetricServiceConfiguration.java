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

package com.viveworks.appbase.core.service.metric;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.metric.jmx.registry.JmxMeterRegistryConfiguration;
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
import java.util.Objects;

/**
 * Configuration for {@link DefaultMetricServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultMetricServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultMetricServiceConfiguration.Builder.class)
public final class DefaultMetricServiceConfiguration implements MetricServiceConfiguration {
    public static final String DEFAULT_YAML_NODE_NAME = "defaultMetricServiceConfiguration";

    private final boolean enableJvmAndSystemMetrics;
    private final JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration;

    private DefaultMetricServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enableJvmAndSystemMetrics = builder.enableJvmAndSystemMetrics;
        jmxMeterRegistryConfiguration = builder.jmxMeterRegistryConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnableJvmAndSystemMetrics() {
        return enableJvmAndSystemMetrics;
    }

    @Nullable
    public JmxMeterRegistryConfiguration getJmxMeterRegistryConfiguration() {
        return jmxMeterRegistryConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultMetricServiceConfiguration that = (DefaultMetricServiceConfiguration) o;
        return enableJvmAndSystemMetrics == that.enableJvmAndSystemMetrics &&
                Objects.equals(jmxMeterRegistryConfiguration, that.jmxMeterRegistryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableJvmAndSystemMetrics, jmxMeterRegistryConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultMetricServiceConfiguration{" +
                "enableJvmAndSystemMetrics=" + enableJvmAndSystemMetrics +
                ", jmxMeterRegistryConfiguration=" + jmxMeterRegistryConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultMetricServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLE_JVM_AND_SYSTEM_METRICS = true;
        private static final JmxMeterRegistryConfiguration DEFAULT_JMX_METER_REGISTRY_CONFIGURATION =
                JmxMeterRegistryConfiguration.builder().build();

        private boolean enableJvmAndSystemMetrics =
                DEFAULT_ENABLE_JVM_AND_SYSTEM_METRICS;
        private JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration =
                DEFAULT_JMX_METER_REGISTRY_CONFIGURATION;

        private Builder() {
        }

        public Builder enableJvmAndSystemMetrics(final boolean enableJvmAndSystemMetrics) {
            this.enableJvmAndSystemMetrics = enableJvmAndSystemMetrics;
            return this;
        }

        public Builder jmxMeterRegistryConfiguration(
                final JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration)
        {
            this.jmxMeterRegistryConfiguration = jmxMeterRegistryConfiguration;
            return this;
        }

        public DefaultMetricServiceConfiguration build() {
            return new DefaultMetricServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultMetricServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultMetricServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
