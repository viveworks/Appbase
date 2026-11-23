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

package com.viveworks.appbase.bootstrap.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
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
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * User-facing configuration for {@link AppbaseCoreServices}.
 *
 * <p>Mirrors the field structure of {@code DefaultAppbaseCoreBootstrapConfiguration} from
 * {@code appbase-core} but lives in {@code appbase-bootstrap} so users never import internal
 * bootstrap classes. MicroProfile services ({@code MicroprofileConfigurationService},
 * {@code MicroprofileServiceRegistry}) are constructed from their service dependencies and
 * therefore require no separate configuration entry.
 *
 * <p>YAML node name: {@value #DEFAULT_YAML_NODE_NAME}
 *
 * <p>Usage:
 * <pre>{@code
 * // From YAML file:
 * AppbaseCoreServices core = AppbaseCoreServices.builder()
 *     .fromYaml("appbase-config.yaml")
 *     .build();
 *
 * // Programmatic:
 * AppbaseCoreServicesConfiguration cfg = AppbaseCoreServicesConfiguration.builder()
 *     .logServiceConfiguration(new DefaultLogServiceConfiguration.Builder()...build())
 *     ...
 *     .build();
 * AppbaseCoreServices core = AppbaseCoreServices.builder().from(cfg).build();
 * }</pre>
 */
@JsonDeserialize(builder = AppbaseCoreServicesConfiguration.Builder.class)
public final class AppbaseCoreServicesConfiguration {

    /** Default YAML node name for deserialization. */
    public static final String DEFAULT_YAML_NODE_NAME = "appbaseCoreServicesConfiguration";

    private final DefaultLogServiceConfiguration logServiceConfiguration;
    private final DefaultMetricServiceConfiguration metricServiceConfiguration;
    private final DefaultValidationServiceConfiguration validationServiceConfiguration;
    private final DefaultMediatorServiceConfiguration mediatorServiceConfiguration;
    private final DefaultConfigurationServiceConfiguration configurationServiceConfiguration;
    private final DefaultServiceRegistryConfiguration serviceRegistryConfiguration;
    private final DefaultExceptionServiceConfiguration exceptionServiceConfiguration;
    private final DefaultTracerServiceConfiguration tracerServiceConfiguration;
    private final DefaultJmxServiceConfiguration jmxServiceConfiguration;
    @Nullable
    private final DefaultReactorServiceConfiguration reactorServiceConfiguration;
    @Nullable
    private final DefaultLuceneServiceConfiguration luceneServiceConfiguration;

    private AppbaseCoreServicesConfiguration(Builder b) {
        this.logServiceConfiguration          = Objects.requireNonNull(b.logServiceConfiguration,          "logServiceConfiguration");
        this.metricServiceConfiguration       = Objects.requireNonNull(b.metricServiceConfiguration,       "metricServiceConfiguration");
        this.validationServiceConfiguration   = Objects.requireNonNull(b.validationServiceConfiguration,   "validationServiceConfiguration");
        this.mediatorServiceConfiguration     = Objects.requireNonNull(b.mediatorServiceConfiguration,     "mediatorServiceConfiguration");
        this.configurationServiceConfiguration = Objects.requireNonNull(b.configurationServiceConfiguration, "configurationServiceConfiguration");
        this.serviceRegistryConfiguration     = Objects.requireNonNull(b.serviceRegistryConfiguration,     "serviceRegistryConfiguration");
        this.exceptionServiceConfiguration    = Objects.requireNonNull(b.exceptionServiceConfiguration,    "exceptionServiceConfiguration");
        this.tracerServiceConfiguration       = Objects.requireNonNull(b.tracerServiceConfiguration,       "tracerServiceConfiguration");
        this.jmxServiceConfiguration          = Objects.requireNonNull(b.jmxServiceConfiguration,          "jmxServiceConfiguration");
        this.reactorServiceConfiguration      = b.reactorServiceConfiguration;
        this.luceneServiceConfiguration       = b.luceneServiceConfiguration;
    }

    /** Returns the log service configuration. */
    public DefaultLogServiceConfiguration getLogServiceConfiguration() {
        return logServiceConfiguration;
    }

    /** Returns the metric service configuration. */
    public DefaultMetricServiceConfiguration getMetricServiceConfiguration() {
        return metricServiceConfiguration;
    }

    /** Returns the validation service configuration. */
    public DefaultValidationServiceConfiguration getValidationServiceConfiguration() {
        return validationServiceConfiguration;
    }

    /** Returns the mediator service configuration. */
    public DefaultMediatorServiceConfiguration getMediatorServiceConfiguration() {
        return mediatorServiceConfiguration;
    }

    /** Returns the configuration service configuration. */
    public DefaultConfigurationServiceConfiguration getConfigurationServiceConfiguration() {
        return configurationServiceConfiguration;
    }

    /** Returns the service registry configuration. */
    public DefaultServiceRegistryConfiguration getServiceRegistryConfiguration() {
        return serviceRegistryConfiguration;
    }

    /** Returns the exception service configuration. */
    public DefaultExceptionServiceConfiguration getExceptionServiceConfiguration() {
        return exceptionServiceConfiguration;
    }

    /** Returns the tracer service configuration. */
    public DefaultTracerServiceConfiguration getTracerServiceConfiguration() {
        return tracerServiceConfiguration;
    }

    /** Returns the JMX service configuration. */
    public DefaultJmxServiceConfiguration getJmxServiceConfiguration() {
        return jmxServiceConfiguration;
    }

    /**
     * Returns the optional reactor service configuration, or {@code null} if reactor is disabled.
     *
     * @return reactor configuration or null
     */
    @Nullable
    public DefaultReactorServiceConfiguration getReactorServiceConfiguration() {
        return reactorServiceConfiguration;
    }

    /**
     * Returns the optional Lucene service configuration, or {@code null} if Lucene is disabled.
     *
     * @return Lucene configuration or null
     */
    @Nullable
    public DefaultLuceneServiceConfiguration getLuceneServiceConfiguration() {
        return luceneServiceConfiguration;
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AppbaseCoreServicesConfiguration}.
     * Also used by Jackson for YAML deserialization.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        @JsonProperty("logServiceConfiguration")
        private DefaultLogServiceConfiguration logServiceConfiguration;

        @JsonProperty("metricServiceConfiguration")
        private DefaultMetricServiceConfiguration metricServiceConfiguration;

        @JsonProperty("validationServiceConfiguration")
        private DefaultValidationServiceConfiguration validationServiceConfiguration;

        @JsonProperty("mediatorServiceConfiguration")
        private DefaultMediatorServiceConfiguration mediatorServiceConfiguration;

        @JsonProperty("configurationServiceConfiguration")
        private DefaultConfigurationServiceConfiguration configurationServiceConfiguration;

        @JsonProperty("serviceRegistryConfiguration")
        private DefaultServiceRegistryConfiguration serviceRegistryConfiguration;

        @JsonProperty("exceptionServiceConfiguration")
        private DefaultExceptionServiceConfiguration exceptionServiceConfiguration;

        @JsonProperty("tracerServiceConfiguration")
        private DefaultTracerServiceConfiguration tracerServiceConfiguration;

        @JsonProperty("jmxServiceConfiguration")
        private DefaultJmxServiceConfiguration jmxServiceConfiguration;

        @JsonProperty("reactorServiceConfiguration")
        @Nullable
        private DefaultReactorServiceConfiguration reactorServiceConfiguration;

        @JsonProperty("luceneServiceConfiguration")
        @Nullable
        private DefaultLuceneServiceConfiguration luceneServiceConfiguration;

        /** Sets log service configuration. */
        public Builder logServiceConfiguration(DefaultLogServiceConfiguration v) {
            logServiceConfiguration = v; return this;
        }

        /** Sets metric service configuration. */
        public Builder metricServiceConfiguration(DefaultMetricServiceConfiguration v) {
            metricServiceConfiguration = v; return this;
        }

        /** Sets validation service configuration. */
        public Builder validationServiceConfiguration(DefaultValidationServiceConfiguration v) {
            validationServiceConfiguration = v; return this;
        }

        /** Sets mediator service configuration. */
        public Builder mediatorServiceConfiguration(DefaultMediatorServiceConfiguration v) {
            mediatorServiceConfiguration = v; return this;
        }

        /** Sets configuration service configuration. */
        public Builder configurationServiceConfiguration(DefaultConfigurationServiceConfiguration v) {
            configurationServiceConfiguration = v; return this;
        }

        /** Sets service registry configuration. */
        public Builder serviceRegistryConfiguration(DefaultServiceRegistryConfiguration v) {
            serviceRegistryConfiguration = v; return this;
        }

        /** Sets exception service configuration. */
        public Builder exceptionServiceConfiguration(DefaultExceptionServiceConfiguration v) {
            exceptionServiceConfiguration = v; return this;
        }

        /** Sets tracer service configuration. */
        public Builder tracerServiceConfiguration(DefaultTracerServiceConfiguration v) {
            tracerServiceConfiguration = v; return this;
        }

        /** Sets JMX service configuration. */
        public Builder jmxServiceConfiguration(DefaultJmxServiceConfiguration v) {
            jmxServiceConfiguration = v; return this;
        }

        /** Sets optional reactor service configuration ({@code null} = reactor disabled). */
        public Builder reactorServiceConfiguration(@Nullable DefaultReactorServiceConfiguration v) {
            reactorServiceConfiguration = v; return this;
        }

        /** Sets optional Lucene service configuration ({@code null} = Lucene disabled). */
        public Builder luceneServiceConfiguration(@Nullable DefaultLuceneServiceConfiguration v) {
            luceneServiceConfiguration = v; return this;
        }

        /** Builds the configuration. All non-nullable fields must be set before calling. */
        public AppbaseCoreServicesConfiguration build() {
            return new AppbaseCoreServicesConfiguration(this);
        }
    }
}
