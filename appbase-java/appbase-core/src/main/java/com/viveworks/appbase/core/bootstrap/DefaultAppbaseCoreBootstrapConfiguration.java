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

package com.viveworks.appbase.core.bootstrap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration used by {@link DefaultAppbaseCoreBootstrap}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAppbaseCoreBootstrapConfiguration.Builder.class)
public final class DefaultAppbaseCoreBootstrapConfiguration implements AppbaseCoreBootstrapConfiguration {
    /**
     * YAML mapping key consumed by the default bootstrap configuration accessor.
     */
    public static final String DEFAULT_YAML_NODE_NAME = "defaultAppbaseBootstrapConfiguration";

    private final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration;
    private final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration;
    private final DefaultLogServiceConfiguration defaultLogServiceConfiguration;
    private final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration;
    private final DefaultValidationServiceConfiguration defaultValidationServiceConfiguration;
    private final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration;
    private final DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration;
    private final DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration;
    private final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration;
    private final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration;
    private final DefaultReactorServiceConfiguration defaultReactorServiceConfiguration;
    private final DefaultJmxServiceConfiguration defaultJmxServiceConfiguration;
    private final long serviceShutdownTimeoutMs;

    private DefaultAppbaseCoreBootstrapConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(
                builder.defaultDisruptorServiceConfiguration, "builder.defaultDisruptorServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultLuceneServiceConfiguration, "builder.defaultLuceneServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultLogServiceConfiguration, "builder.defaultLogServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultMetricServiceConfiguration, "builder.defaultMetricServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultValidationServiceConfiguration, "builder.defaultValidationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultMediatorServiceConfiguration, "builder.defaultMediatorServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultConfigurationServiceConfiguration, "builder.defaultConfigurationServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultServiceRegistryConfiguration, "builder.defaultServiceRegistryConfiguration");
        AppbaseUtil.notNull(
                builder.defaultExceptionServiceConfiguration, "builder.defaultExceptionServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultTracerServiceConfiguration, "builder.defaultTracerServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultReactiveServiceConfiguration, "builder.defaultReactiveServiceConfiguration");
        AppbaseUtil.notNull(
                builder.defaultJmxServiceConfiguration, "builder.defaultJmxServiceConfiguration");

        defaultDisruptorServiceConfiguration = builder.defaultDisruptorServiceConfiguration;
        defaultLuceneServiceConfiguration = builder.defaultLuceneServiceConfiguration;
        defaultLogServiceConfiguration = builder.defaultLogServiceConfiguration;
        defaultMetricServiceConfiguration = builder.defaultMetricServiceConfiguration;
        defaultValidationServiceConfiguration = builder.defaultValidationServiceConfiguration;
        defaultMediatorServiceConfiguration = builder.defaultMediatorServiceConfiguration;
        defaultConfigurationServiceConfiguration = builder.defaultConfigurationServiceConfiguration;
        defaultServiceRegistryConfiguration = builder.defaultServiceRegistryConfiguration;
        defaultExceptionServiceConfiguration = builder.defaultExceptionServiceConfiguration;
        defaultTracerServiceConfiguration = builder.defaultTracerServiceConfiguration;
        defaultReactorServiceConfiguration = builder.defaultReactiveServiceConfiguration;
        defaultJmxServiceConfiguration = builder.defaultJmxServiceConfiguration;
        serviceShutdownTimeoutMs = builder.serviceShutdownTimeoutMs;
    }

    /**
     * Get the builder used to build this object.
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public DefaultDisruptorServiceConfiguration getDisruptorServiceConfiguration() {
        return defaultDisruptorServiceConfiguration;
    }

    @Override
    public DefaultLuceneServiceConfiguration getLuceneServiceConfiguration() {
        return defaultLuceneServiceConfiguration;
    }

    @Override
    public DefaultLogServiceConfiguration getLogServiceConfiguration() {
        return defaultLogServiceConfiguration;
    }

    @Override
    public DefaultMetricServiceConfiguration getMetricServiceConfiguration() {
        return defaultMetricServiceConfiguration;
    }

    @Override
    public DefaultValidationServiceConfiguration getValidationServiceConfiguration() {
        return defaultValidationServiceConfiguration;
    }

    @Override
    public DefaultMediatorServiceConfiguration getMediatorServiceConfiguration() {
        return defaultMediatorServiceConfiguration;
    }

    @Override
    public DefaultConfigurationServiceConfiguration getConfigurationServiceConfiguration() {
        return defaultConfigurationServiceConfiguration;
    }

    @Override
    public DefaultServiceRegistryConfiguration getServiceRegistryConfiguration() {
        return defaultServiceRegistryConfiguration;
    }

    @Override
    public DefaultExceptionServiceConfiguration getExceptionServiceConfiguration() {
        return defaultExceptionServiceConfiguration;
    }

    @Override
    public DefaultTracerServiceConfiguration getTracerServiceConfiguration() {
        return defaultTracerServiceConfiguration;
    }

    @Override
    public DefaultReactorServiceConfiguration getReactorServiceConfiguration() {
        return defaultReactorServiceConfiguration;
    }

    @Override
    public DefaultJmxServiceConfiguration getJmxServiceConfiguration() {
        return defaultJmxServiceConfiguration;
    }

    @Override
    public long getServiceShutdownTimeoutMs() {
        return serviceShutdownTimeoutMs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultAppbaseCoreBootstrapConfiguration that = (DefaultAppbaseCoreBootstrapConfiguration) o;
        return Objects.equals(defaultDisruptorServiceConfiguration, that.defaultDisruptorServiceConfiguration) &&
                Objects.equals(defaultLuceneServiceConfiguration, that.defaultLuceneServiceConfiguration) &&
                Objects.equals(defaultLogServiceConfiguration, that.defaultLogServiceConfiguration) &&
                Objects.equals(defaultMetricServiceConfiguration, that.defaultMetricServiceConfiguration) &&
                Objects.equals(defaultValidationServiceConfiguration, that.defaultValidationServiceConfiguration) &&
                Objects.equals(defaultMediatorServiceConfiguration, that.defaultMediatorServiceConfiguration) &&
                Objects.equals(
                        defaultConfigurationServiceConfiguration, that.defaultConfigurationServiceConfiguration) &&
                Objects.equals(defaultServiceRegistryConfiguration, that.defaultServiceRegistryConfiguration) &&
                Objects.equals(defaultExceptionServiceConfiguration, that.defaultExceptionServiceConfiguration) &&
                Objects.equals(defaultTracerServiceConfiguration, that.defaultTracerServiceConfiguration) &&
                Objects.equals(defaultReactorServiceConfiguration, that.defaultReactorServiceConfiguration) &&
                Objects.equals(defaultJmxServiceConfiguration, that.defaultJmxServiceConfiguration) &&
                serviceShutdownTimeoutMs == that.serviceShutdownTimeoutMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                defaultDisruptorServiceConfiguration,
                defaultLuceneServiceConfiguration,
                defaultLogServiceConfiguration,
                defaultMetricServiceConfiguration,
                defaultValidationServiceConfiguration,
                defaultMediatorServiceConfiguration,
                defaultConfigurationServiceConfiguration,
                defaultServiceRegistryConfiguration,
                defaultExceptionServiceConfiguration,
                defaultTracerServiceConfiguration,
                defaultReactorServiceConfiguration,
                defaultJmxServiceConfiguration,
                serviceShutdownTimeoutMs);
    }

    @Override
    public String toString() {
        return "DefaultAppbaseCoreBootstrapConfiguration{" +
                "defaultDisruptorServiceConfiguration=" + defaultDisruptorServiceConfiguration +
                ", defaultLuceneServiceConfiguration=" + defaultLuceneServiceConfiguration +
                ", defaultLogServiceConfiguration=" + defaultLogServiceConfiguration +
                ", defaultMetricServiceConfiguration=" + defaultMetricServiceConfiguration +
                ", defaultValidationServiceConfiguration=" + defaultValidationServiceConfiguration +
                ", defaultMediatorServiceConfiguration=" + defaultMediatorServiceConfiguration +
                ", defaultConfigurationServiceConfiguration=" + defaultConfigurationServiceConfiguration +
                ", defaultServiceRegistryConfiguration=" + defaultServiceRegistryConfiguration +
                ", defaultExceptionServiceConfiguration=" + defaultExceptionServiceConfiguration +
                ", defaultTracerServiceConfiguration=" + defaultTracerServiceConfiguration +
                ", defaultReactiveServiceConfiguration=" + defaultReactorServiceConfiguration +
                ", defaultJmxServiceConfiguration=" + defaultJmxServiceConfiguration +
                ", serviceShutdownTimeoutMs=" + serviceShutdownTimeoutMs +
                '}';
    }

    /**
     * Builder for {@link DefaultAppbaseCoreBootstrapConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration.builder().build();
        private DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration.builder().build();
        private DefaultLogServiceConfiguration defaultLogServiceConfiguration =
                DefaultLogServiceConfiguration.builder().build();
        private DefaultMetricServiceConfiguration defaultMetricServiceConfiguration =
                DefaultMetricServiceConfiguration.builder().build();
        private DefaultValidationServiceConfiguration defaultValidationServiceConfiguration =
                new DefaultValidationServiceConfiguration();
        private DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration.builder().build();
        private DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration =
                DefaultConfigurationServiceConfiguration.builder().build();
        private DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration =
                DefaultServiceRegistryConfiguration.builder().build();
        private DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                DefaultExceptionServiceConfiguration.builder().build();
        private DefaultTracerServiceConfiguration defaultTracerServiceConfiguration =
                DefaultTracerServiceConfiguration.builder().build();
        private DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration =
                DefaultReactorServiceConfiguration.builder().build();
        private DefaultJmxServiceConfiguration defaultJmxServiceConfiguration =
                DefaultJmxServiceConfiguration.builder().build();
        private long serviceShutdownTimeoutMs = 5000L;

        private Builder() {
        }

        public Builder defaultDisruptorServiceConfiguration(
                final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration)
        {
            this.defaultDisruptorServiceConfiguration = defaultDisruptorServiceConfiguration;
            return this;
        }

        public Builder defaultLuceneServiceConfiguration(
                final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration)
        {
            this.defaultLuceneServiceConfiguration = defaultLuceneServiceConfiguration;
            return this;
        }

        public Builder defaultLogServiceConfiguration(
                final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
        {
            this.defaultLogServiceConfiguration = defaultLogServiceConfiguration;
            return this;
        }

        public Builder defaultMetricServiceConfiguration(
                final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration)
        {
            this.defaultMetricServiceConfiguration = defaultMetricServiceConfiguration;
            return this;
        }

        public Builder defaultValidationServiceConfiguration(
                final DefaultValidationServiceConfiguration defaultValidationServiceConfiguration)
        {
            this.defaultValidationServiceConfiguration = defaultValidationServiceConfiguration;
            return this;
        }

        public Builder defaultMediatorServiceConfiguration(
                final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration)
        {
            this.defaultMediatorServiceConfiguration = defaultMediatorServiceConfiguration;
            return this;
        }

        public Builder defaultConfigurationServiceConfiguration(
                final DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration)
        {
            this.defaultConfigurationServiceConfiguration = defaultConfigurationServiceConfiguration;
            return this;
        }

        public Builder defaultServiceRegistryConfiguration(
                final DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration)
        {
            this.defaultServiceRegistryConfiguration = defaultServiceRegistryConfiguration;
            return this;
        }

        public Builder defaultExceptionServiceConfiguration(
                final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration)
        {
            this.defaultExceptionServiceConfiguration = defaultExceptionServiceConfiguration;
            return this;
        }

        public Builder defaultTracerServiceConfiguration(
                final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration)
        {
            this.defaultTracerServiceConfiguration = defaultTracerServiceConfiguration;
            return this;
        }

        public Builder defaultReactiveServiceConfiguration(
                final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration)
        {
            this.defaultReactiveServiceConfiguration = defaultReactiveServiceConfiguration;
            return this;
        }

        public Builder defaultJmxServiceConfiguration(
                final DefaultJmxServiceConfiguration defaultJmxServiceConfiguration)
        {
            this.defaultJmxServiceConfiguration = defaultJmxServiceConfiguration;
            return this;
        }

        public Builder serviceShutdownTimeoutMs(final long serviceShutdownTimeoutMs) {
            if (serviceShutdownTimeoutMs <= 0) {
                throw new AppbaseRuntimeException(
                        "serviceShutdownTimeoutMs must be positive, was: " + serviceShutdownTimeoutMs);
            }
            this.serviceShutdownTimeoutMs = serviceShutdownTimeoutMs;
            return this;
        }

        public DefaultAppbaseCoreBootstrapConfiguration build() {
            return new DefaultAppbaseCoreBootstrapConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
