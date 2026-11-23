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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * {@link AppbaseCoreBootstrap} creates this to bootstrap <code>core services</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class AppbaseCoreBootstrappedServices {
    private final LogService logService;
    private final MetricService metricService;
    private final ValidationService validationService;
    private final MediatorService mediatorService;
    private final ConfigurationService configurationService;
    private final MicroprofileConfigurationService microprofileConfigurationService;
    private final ServiceRegistry serviceRegistry;
    private final MicroprofileServiceRegistry microprofileServiceRegistry;
    private final ExceptionService exceptionService;
    private final TracerService tracerService;
    private final ReactorService reactorService;
    private final JmxService jmxService;

    @Nullable
    private final LuceneService luceneService;

    private AppbaseCoreBootstrappedServices(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.logService, "builder.logService");
        AppbaseUtil.notNull(builder.metricService, "builder.metricService");
        AppbaseUtil.notNull(builder.validationService, "builder.validationService");
        AppbaseUtil.notNull(builder.mediatorService, "builder.mediatorService");
        AppbaseUtil.notNull(builder.configurationService, "builder.configurationService");
        AppbaseUtil.notNull(builder.microprofileConfigurationService, "builder.microprofileConfigurationService");
        AppbaseUtil.notNull(builder.serviceRegistry, "builder.serviceRegistry");
        AppbaseUtil.notNull(builder.microprofileServiceRegistry, "builder.microprofileServiceRegistry");
        AppbaseUtil.notNull(builder.exceptionService, "builder.exceptionService");
        AppbaseUtil.notNull(builder.tracerService, "builder.tracerService");
        AppbaseUtil.notNull(builder.reactorService, "builder.reactorService");
        AppbaseUtil.notNull(builder.jmxService, "builder.jmxService");

        logService = builder.logService;
        metricService = builder.metricService;
        validationService = builder.validationService;
        mediatorService = builder.mediatorService;
        configurationService = builder.configurationService;
        microprofileConfigurationService = builder.microprofileConfigurationService;
        serviceRegistry = builder.serviceRegistry;
        microprofileServiceRegistry = builder.microprofileServiceRegistry;
        exceptionService = builder.exceptionService;
        tracerService = builder.tracerService;
        reactorService = builder.reactorService;
        jmxService = builder.jmxService;
        luceneService = builder.luceneService;
    }

    /**
     * Get the builder used to build this object.
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the logging service that receives core log events.
     *
     * @return the logging service that receives core log events
     */
    public LogService getLogService() {
        return logService;
    }

    /**
     * Returns the metrics service used to register and update core metrics.
     *
     * @return the metrics service used to register and update core metrics
     */
    public MetricService getMetricService() {
        return metricService;
    }

    /**
     * Returns the validation service used by bootstrapped components.
     *
     * @return the validation service used by bootstrapped components
     */
    public ValidationService getValidationService() {
        return validationService;
    }

    /**
     * Returns the mediator that dispatches core messages and events.
     *
     * @return the mediator that dispatches core messages and events
     */
    public MediatorService getMediatorService() {
        return mediatorService;
    }

    /**
     * Returns the service that owns the core configuration accessors.
     *
     * @return the service that owns the core configuration accessors
     */
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    /**
     * Returns the MicroProfile Config adapter backed by the configuration service.
     *
     * @return the MicroProfile Config adapter backed by the configuration service
     */
    public MicroprofileConfigurationService getMicroprofileConfigurationService() {
        return microprofileConfigurationService;
    }

    /**
     * Returns the registry containing the bootstrapped Appbase services.
     *
     * @return the registry containing the bootstrapped Appbase services
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Returns the MicroProfile-facing view of the service registry.
     *
     * @return the MicroProfile-facing view of the service registry
     */
    public MicroprofileServiceRegistry getMicroprofileServiceRegistry() {
        return microprofileServiceRegistry;
    }

    /**
     * Returns the service that records and publishes handled exceptions.
     *
     * @return the service that records and publishes handled exceptions
     */
    public ExceptionService getExceptionService() {
        return exceptionService;
    }

    /**
     * Returns the tracing service shared by the core services.
     *
     * @return the tracing service shared by the core services
     */
    public TracerService getTracerService() {
        return tracerService;
    }

    /**
     * Returns the Reactor integration used to create managed schedulers.
     *
     * @return the Reactor integration used to create managed schedulers
     */
    public ReactorService getReactorService() {
        return reactorService;
    }

    /**
     * Returns the service that registers and removes the core MXBeans.
     *
     * @return the service that registers and removes the core MXBeans
     */
    public JmxService getJmxService() {
        return jmxService;
    }

    /**
     * Returns the optional Lucene service, or {@code null} when the Lucene configuration is absent or disabled.
     *
     * @return the optional Lucene service, or {@code null} when the Lucene configuration is absent or disabled
     */
    @Nullable
    public LuceneService getLuceneService() {
        return luceneService;
    }

    /**
     * Builder for {@link AppbaseCoreBootstrappedServices}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private LogService logService;
        private MetricService metricService;
        private ValidationService validationService;
        private MediatorService mediatorService;
        private ConfigurationService configurationService;
        private MicroprofileConfigurationService microprofileConfigurationService;
        private ServiceRegistry serviceRegistry;
        private MicroprofileServiceRegistry microprofileServiceRegistry;
        private ExceptionService exceptionService;
        private TracerService tracerService;
        private ReactorService reactorService;
        private JmxService jmxService;

        @Nullable
        private LuceneService luceneService;

        private Builder() {
        }

        /**
         * Selects the logging service that receives core log events for the bootstrap result.
         *
         * @param logService logging service that receives core log events
         * @return this builder
         */
        public Builder logService(final LogService logService) {
            this.logService = logService;
            return this;
        }

        /**
         * Selects the metrics service used to register and update core metrics for the bootstrap result.
         *
         * @param metricService metrics service used to register and update core metrics
         * @return this builder
         */
        public Builder metricService(final MetricService metricService) {
            this.metricService = metricService;
            return this;
        }

        /**
         * Selects the validation service used by bootstrapped components for the bootstrap result.
         *
         * @param validationService validation service used by bootstrapped components
         * @return this builder
         */
        public Builder validationService(final ValidationService validationService) {
            this.validationService = validationService;
            return this;
        }

        /**
         * Selects the mediator that dispatches core messages and events for the bootstrap result.
         *
         * @param mediatorService mediator that dispatches core messages and events
         * @return this builder
         */
        public Builder mediatorService(final MediatorService mediatorService) {
            this.mediatorService = mediatorService;
            return this;
        }

        /**
         * Selects the service that owns the core configuration accessors for the bootstrap result.
         *
         * @param configurationService service that owns the core configuration accessors
         * @return this builder
         */
        public Builder configurationService(final ConfigurationService configurationService) {
            this.configurationService = configurationService;
            return this;
        }

        /**
         * Sets the MicroProfile configuration service. It is required: the aggregate constructor rejects
         * a builder that has none.
         *
         * @param microprofileConfigurationService service that reads MicroProfile configuration
         * @return this builder
         */
        public Builder microprofileConfigurationService(
                final MicroprofileConfigurationService microprofileConfigurationService)
        {
            this.microprofileConfigurationService = microprofileConfigurationService;
            return this;
        }

        /**
         * Selects the registry containing the bootstrapped Appbase services for the bootstrap result.
         *
         * @param serviceRegistry registry containing the bootstrapped Appbase services
         * @return this builder
         */
        public Builder serviceRegistry(final ServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
            return this;
        }

        /**
         * Selects the MicroProfile-facing view of the service registry for the bootstrap result.
         *
         * @param microprofileServiceRegistry MicroProfile-facing view of the service registry
         * @return this builder
         */
        public Builder microprofileServiceRegistry(final MicroprofileServiceRegistry microprofileServiceRegistry) {
            this.microprofileServiceRegistry = microprofileServiceRegistry;
            return this;
        }

        /**
         * Selects the service that records and publishes handled exceptions for the bootstrap result.
         *
         * @param exceptionService service that records and publishes handled exceptions
         * @return this builder
         */
        public Builder exceptionService(final ExceptionService exceptionService) {
            this.exceptionService = exceptionService;
            return this;
        }

        /**
         * Selects the tracing service shared by the core services for the bootstrap result.
         *
         * @param tracerService tracing service shared by the core services
         * @return this builder
         */
        public Builder tracerService(final TracerService tracerService) {
            this.tracerService = tracerService;
            return this;
        }

        /**
         * Selects the Reactor integration used to create managed schedulers for the bootstrap result.
         *
         * @param reactorService Reactor integration used to create managed schedulers
         * @return this builder
         */
        public Builder reactorService(final ReactorService reactorService) {
            this.reactorService = reactorService;
            return this;
        }

        /**
         * Selects the service that registers and removes the core MXBeans for the bootstrap result.
         *
         * @param jmxService service that registers and removes the core MXBeans
         * @return this builder
         */
        public Builder jmxService(final JmxService jmxService) {
            this.jmxService = jmxService;
            return this;
        }

        /**
         * Selects the optional Lucene service, or {@code null} when the Lucene configuration is absent or disabled.
         *
         * @param luceneService optional Lucene service, or {@code null} when the Lucene configuration is absent or disabled
         * @return this builder
         */
        public Builder luceneService(@Nullable final LuceneService luceneService) {
            this.luceneService = luceneService;
            return this;
        }

        /**
         * Validates the required service references and snapshots them into an immutable result.
         * Lucene is the sole optional service because bootstrap may run without a Lucene configuration.
         * @return the assembled collection of bootstrapped services
         * @throws com.viveworks.appbase.core.AppbaseRuntimeException if any required service has not been selected
         */
        public AppbaseCoreBootstrappedServices build() {
            return new AppbaseCoreBootstrappedServices(this);
        }
    }
    // CheckStyle: Builder end
}
