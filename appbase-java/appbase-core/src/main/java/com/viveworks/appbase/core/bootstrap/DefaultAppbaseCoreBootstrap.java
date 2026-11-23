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

import ch.qos.logback.classic.LoggerContext;
import com.lmax.disruptor.ExceptionHandler;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationException;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.CoreBootstrapConfigurationLoader;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.DefaultConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationServiceMXBean;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationServiceImpl;
import com.viveworks.appbase.core.service.configuration.property.deserializer.YamlPropertyDeserializer;
import com.viveworks.appbase.core.service.exception.DefaultExceptionService;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.exception.ExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessor;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionServiceMXBean;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.jmx.JmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxServiceException;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.LogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogServiceException;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventFactory;
import com.viveworks.appbase.core.service.mediator.event.delegator.DefaultMediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.DisruptorMediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorServiceMXBean;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.metric.MetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricService;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricServiceMXBean;
import com.viveworks.appbase.core.service.reactive.read.DefaultReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundaryConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorService;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultReadinessChecker;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ReadinessChecker;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.ServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistryImpl;
import com.viveworks.appbase.core.service.tracer.DefaultTracerService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.tracer.TracerServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.jmx.JmxTracerService;
import com.viveworks.appbase.core.service.tracer.jmx.JmxTracerServiceMXBean;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.validation.ValidationServiceConfiguration;
import com.viveworks.appbase.core.service.validation.jmx.JmxValidationService;
import com.viveworks.appbase.core.service.validation.jmx.JmxValidationServiceMXBean;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService;
import com.viveworks.appbase.core.util.disruptor.DefaultExceptionHandler;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.DisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.util.lucene.LuceneServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link AppbaseCoreBootstrap}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAppbaseCoreBootstrap implements AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> {
    private final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry;
    @Nullable private final String bootstrapConfigurationFilePath;

    /**
     * Configures bootstrap to populate the supplied registry. A non-blank configuration path is
     * also written to {@link AppbaseConstants#APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY}, making
     * it visible to configuration accessors created later in the bootstrap sequence.
     *
     * @param appbaseCoreBootstrapRegistry registry that receives configuration accessors and codecs
     * @param bootstrapConfigurationFilePath optional configuration file path; blank values leave the
     *                                       current system property unchanged
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrapRegistry} is {@code null}
     */
    public DefaultAppbaseCoreBootstrap(final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry,
                                       @Nullable final String bootstrapConfigurationFilePath)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrapRegistry, "appbaseCoreBootstrapRegistry");

        this.appbaseCoreBootstrapRegistry = appbaseCoreBootstrapRegistry;
        this.bootstrapConfigurationFilePath = bootstrapConfigurationFilePath;
        if (!AppbaseUtil.isBlank(bootstrapConfigurationFilePath)) {
            System.setProperty(
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, bootstrapConfigurationFilePath);
        }
    }

    /**
     * Builds and starts the configured core service graph, reading configuration accessors and codecs from the
     * bootstrap registry and registering the started services with the new service registry and the JMX service.
     *
     * @param appbaseCoreBootstrapConfiguration bootstrap settings; must use the default implementation
     * @return the started service aggregate
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if the configuration is {@code null} or unsupported
     */
    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public AppbaseCoreBootstrappedServices bootstrap(
            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");
        if (!DefaultAppbaseCoreBootstrapConfiguration.class.isAssignableFrom(
                appbaseCoreBootstrapConfiguration.getClass()))
        {
            throw new AppbaseRuntimeException(
                    appbaseCoreBootstrapConfiguration +
                            " must be of type " +
                            DefaultAppbaseCoreBootstrapConfiguration.class);
        }
        final DefaultAppbaseCoreBootstrapConfiguration defaultAppbaseBootstrapConfiguration =
                (DefaultAppbaseCoreBootstrapConfiguration) appbaseCoreBootstrapConfiguration;

        // MediatorEventDelegatorRegistry ...
        final DefaultMediatorEventDelegatorRegistry defaultMediatorEventDelegatorRegistry =
                newDefaultMediatorEventDelegatorRegistry();
        AppbaseUtil.notNull(defaultMediatorEventDelegatorRegistry, "defaultMediatorEventDelegatorRegistry");


        // DisruptorService ...
        final int disruptorEventHandlerCount = defaultAppbaseBootstrapConfiguration
                .getDisruptorServiceConfiguration()
                .getEventHandlerCount();
        AppbaseUtil.positive(disruptorEventHandlerCount, "disruptorEventHandlerCount");

        final DefaultExceptionHandler<MediatorEvent> disruptorDefaultExceptionHandler = newDefaultExceptionHandler();
        AppbaseUtil.notNull(disruptorDefaultExceptionHandler, "disruptorDefaultExceptionHandler");

        final DisruptorMediatorEventHandlerDelegator[] disruptorMediatorEventHandlerDelegators =
                newDisruptorMediatorEventHandlerDelegators(
                        defaultMediatorEventDelegatorRegistry, disruptorEventHandlerCount);
        AppbaseUtil.notNull(disruptorMediatorEventHandlerDelegators, "disruptorMediatorEventHandlerDelegators");

        final DisruptorService<MediatorEvent> disruptorService = newDisruptorService(
                defaultAppbaseBootstrapConfiguration.getDisruptorServiceConfiguration(),
                disruptorDefaultExceptionHandler,
                disruptorMediatorEventHandlerDelegators);
        AppbaseUtil.notNull(disruptorService, "disruptorService");


        // LogService init - Phase 1 ...
        final LogService logService = newLogService(
                defaultMediatorEventDelegatorRegistry,
                defaultAppbaseBootstrapConfiguration.getLogServiceConfiguration());
        AppbaseUtil.notNull(logService, "logService");

        final Logger logger = logService.getLogger(getClass());
        logger.info("Bootstrapped: {}", defaultMediatorEventDelegatorRegistry.getClass().getName());
        logger.info("Bootstrapped: {}", disruptorDefaultExceptionHandler.getClass().getName());
        logger.info("Bootstrapped {}: {}",
                disruptorEventHandlerCount,
                DisruptorMediatorEventHandlerDelegator.class.getName());
        logger.info("Bootstrapped: {}", disruptorService.getClass().getName());
        logger.info("Bootstrapped: {}", logService.getClass().getName());


        // MetricService ...
        final DefaultMetricService metricService = newMetricService(
                logService,
                defaultAppbaseBootstrapConfiguration.getMetricServiceConfiguration());
        AppbaseUtil.notNull(metricService, "metricService");
        logger.info("Bootstrapped: {}", metricService.getClass().getName());


        // ValidationService ...
        final ValidationService validationService = newValidationService(logService, metricService);
        AppbaseUtil.notNull(validationService, "validationService");
        logger.info("Bootstrapped: {}", validationService.getClass().getName());


        // MediatorService ...
        final MediatorService mediatorService = newMediatorService(
                logService, metricService, validationService, disruptorService, defaultMediatorEventDelegatorRegistry);
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        logger.info("Bootstrapped: {}", mediatorService.getClass().getName());


        // LuceneService ...
        final LuceneService luceneService;
        final DefaultLuceneServiceConfiguration luceneServiceConfiguration =
                defaultAppbaseBootstrapConfiguration.getLuceneServiceConfiguration();
        if (luceneServiceConfiguration != null && luceneServiceConfiguration.isEnabled()) {
            luceneService = newLuceneService(
                    logService, metricService, validationService, mediatorService, luceneServiceConfiguration);
            AppbaseUtil.notNull(luceneService, "luceneService");
            logger.info("Bootstrapped: {}", luceneService.getClass().getName());
        } else {
            luceneService = null;
        }


        // DefaultLogService full init ...
        if (DefaultLogService.class.isAssignableFrom(logService.getClass())) {
            final DefaultLogService defaultLogService = (DefaultLogService) logService;
            defaultLogService.setMediatorService(mediatorService);
            defaultLogService.setMetricService(metricService);
            defaultLogService.setValidationService(validationService);

            if (luceneService != null) {
                defaultLogService.setLuceneService(luceneService);
            }

            try {
                defaultLogService.initFiltersAndAppenders((LoggerContext) LoggerFactory.getILoggerFactory());
            } catch (LogServiceException lse) {
                throw new AppbaseRuntimeException("Error while initializing filters and appenders in " +
                        DefaultLogService.class.getName(), lse);
            }
            logger.info("Initialized: {} completely", logService.getClass().getName());
        }


        // Init earlier services with LogService and MetricService ...
        if (DefaultDisruptorService.class.isAssignableFrom(disruptorService.getClass())) {
            ((DefaultDisruptorService) disruptorService).setLogService(logService);
            logger.info("Initialized: {} in {}",
                    logService.getClass().getName(),
                    disruptorService.getClass().getName());
        }
        defaultMediatorEventDelegatorRegistry.setLogService(logService);
        logger.info("Initialized: {} in {}",
                logService.getClass().getName(),
                defaultMediatorEventDelegatorRegistry.getClass().getName());

        disruptorDefaultExceptionHandler.setLogService(logService);
        logger.info("Initialized: {} in {}",
                logService.getClass().getName(),
                disruptorDefaultExceptionHandler.getClass().getName());

        disruptorDefaultExceptionHandler.initMetrics(metricService);
        logger.info("Initialized: {} in {}",
                metricService.getClass().getName(),
                disruptorDefaultExceptionHandler.getClass().getName());

        for (int i = 0; i < disruptorEventHandlerCount; ++i) {
            disruptorMediatorEventHandlerDelegators[i].setLogService(logService);
            logger.info("Initialized: {} in {}",
                    disruptorMediatorEventHandlerDelegators[i].getClass().getName(),
                    logService.getClass().getName());

            disruptorMediatorEventHandlerDelegators[i].setMetricService(metricService);
            logger.info("Initialized: {} in {}",
                    disruptorMediatorEventHandlerDelegators[i].getClass().getName(),
                    metricService.getClass().getName());
        }


        // ConfigurationService ...
        final ConfigurationService configurationService = newDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService,
                appbaseCoreBootstrapRegistry,
                defaultAppbaseBootstrapConfiguration);
        AppbaseUtil.notNull(configurationService, "configurationService");
        logger.info("Bootstrapped: {}", configurationService.getClass().getName());


        // ServiceRegistry ...
        final ServiceRegistry serviceRegistry = newServiceRegistry(
                logService, metricService, mediatorService, validationService, configurationService);
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");
        logger.info("Bootstrapped: {}", serviceRegistry.getClass().getName());


        // ExceptionService ...
        final ExceptionService exceptionService = newExceptionService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService,
                appbaseCoreBootstrapRegistry.getExceptionMessageContextCodec());
        AppbaseUtil.notNull(exceptionService, "exceptionService");
        logger.info("Bootstrapped: {}", exceptionService.getClass().getName());


        // TracerService ...
        final TracerService tracerService = newTracerService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService);
        AppbaseUtil.notNull(tracerService, "tracerService");
        logger.info("Bootstrapped: {}", tracerService.getClass().getName());


        // ReactorService ...
        final ReactorService reactorService = newReactiveService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService);
        AppbaseUtil.notNull(reactorService, "reactorService");
        logger.info("Bootstrapped: {}", reactorService.getClass().getName());


        // JmxService ...
        final JmxService jmxService = newJmxService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService);
        AppbaseUtil.notNull(jmxService, "jmxService");
        logger.info("Bootstrapped: {}", jmxService.getClass().getName());


        // MicroprofileConfigurationService ...
        final MicroprofileConfigurationService microprofileConfigurationService =
                newMicroprofileConfigurationService(configurationService);
        AppbaseUtil.notNull(microprofileConfigurationService, "microprofileConfigurationService");
        logger.info("Bootstrapped: {}", microprofileConfigurationService.getClass().getName());


        // MicroprofileServiceRegistry ...
        final MicroprofileServiceRegistry microprofileServiceRegistry =
                newMicroprofileServiceRegistry(serviceRegistry);
        AppbaseUtil.notNull(microprofileServiceRegistry, "microprofileServiceRegistry");
        logger.info("Bootstrapped: {}", microprofileServiceRegistry.getClass().getName());


        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices = AppbaseCoreBootstrappedServices
                .builder()
                .configurationService(configurationService)
                .exceptionService(exceptionService)
                .jmxService(jmxService)
                .logService(logService)
                .mediatorService(mediatorService)
                .metricService(metricService)
                .reactorService(reactorService)
                .serviceRegistry(serviceRegistry)
                .tracerService(tracerService)
                .validationService(validationService)
                .microprofileConfigurationService(microprofileConfigurationService)
                .microprofileServiceRegistry(microprofileServiceRegistry)
                .luceneService(luceneService)
                .build();


        // Register services ...
        logger.info("Registering bootstrapped services: {}", appbaseCoreBootstrappedServices);
        registerAppbaseServices(appbaseCoreBootstrappedServices);


        // Register JMX beans ...
        logger.info("Registering JMX beans using: {}", appbaseCoreBootstrappedServices);
        try {
            registerJmxBeans(
                    appbaseCoreBootstrappedServices, defaultAppbaseBootstrapConfiguration.getJmxServiceConfiguration());
        } catch (JmxServiceException jse) {
            throw new AppbaseRuntimeException("Error while registering JMX beans", jse);
        }

        // Initialize JmxMeterRegistry ...
        logger.info("Initializing JmxMeterRegistry");
        metricService.initJmxMeterRegistryIfEnabled();

        // Done ...
        logger.info("Finished bootstrapping: {}", appbaseCoreBootstrappedServices.getClass().getName());
        return appbaseCoreBootstrappedServices;
    }

    protected void registerJmxBeans(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
                                    final JmxServiceConfiguration jmxServiceConfiguration) throws JmxServiceException
    {
        final JmxService jmxService = appbaseCoreBootstrappedServices.getJmxService();
        if (jmxService != null && jmxServiceConfiguration.isEnabled()) {
            final Set<Class<?>> excludedMxBeanRegistrations = jmxServiceConfiguration.getExcludedMxBeanRegistrations();

            registerLogServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerMetricServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerValidationServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerMediatorServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerConfigurationServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerServiceRegistryMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerExceptionServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerTraceServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
            registerJmxServiceMXBean(appbaseCoreBootstrappedServices, excludedMxBeanRegistrations);
        }
    }

    protected void registerJmxServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final JmxService jmxService = appbaseCoreBootstrappedServices.getJmxService();
        if (!excludedMxBeanRegistrations.contains(JmxJmxService.class) &&
                !excludedMxBeanRegistrations.contains(JmxJmxServiceMXBean.class))
        {
            final JmxJmxServiceMXBean jmxJmxServiceMXBean =
                    new JmxJmxService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            jmxService,
                            DefaultJmxServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxJmxServiceMXBean);
        }
    }

    protected void registerTraceServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final TracerService tracerService = appbaseCoreBootstrappedServices.getTracerService();
        if (!excludedMxBeanRegistrations.contains(JmxTracerService.class) &&
                !excludedMxBeanRegistrations.contains(JmxTracerServiceMXBean.class))
        {
            final JmxTracerServiceMXBean jmxTracerServiceMXBean =
                    new JmxTracerService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            tracerService,
                            DefaultTracerServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxTracerServiceMXBean);
        }
    }

    protected void registerExceptionServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final ExceptionService exceptionService = appbaseCoreBootstrappedServices.getExceptionService();
        if (!excludedMxBeanRegistrations.contains(JmxExceptionService.class) &&
                !excludedMxBeanRegistrations.contains(JmxExceptionServiceMXBean.class))
        {
            final JmxExceptionServiceMXBean jmxExceptionServiceMXBean =
                    new JmxExceptionService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            exceptionService,
                            DefaultExceptionServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxExceptionServiceMXBean);
        }
    }

    protected void registerServiceRegistryMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final ServiceRegistry serviceRegistry = appbaseCoreBootstrappedServices.getServiceRegistry();
        if (!excludedMxBeanRegistrations.contains(JmxServiceRegistry.class) &&
                !excludedMxBeanRegistrations.contains(JmxServiceRegistryMXBean.class))
        {
            final JmxServiceRegistryMXBean jmxServiceRegistryMXBean =
                    new JmxServiceRegistry(
                            appbaseCoreBootstrappedServices.getLogService(),
                            serviceRegistry,
                            DefaultServiceRegistryConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxServiceRegistryMXBean);
        }
    }

    protected void registerConfigurationServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final ConfigurationService configurationService = appbaseCoreBootstrappedServices.getConfigurationService();
        if (!excludedMxBeanRegistrations.contains(JmxConfigurationService.class) &&
                !excludedMxBeanRegistrations.contains(JmxConfigurationServiceMXBean.class))
        {
            final JmxConfigurationServiceMXBean jmxConfigurationServiceMXBean =
                    new JmxConfigurationService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            configurationService,
                            DefaultConfigurationServiceConfiguration.class,
                            configurationService);
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxConfigurationServiceMXBean);
        }
    }

    protected void registerMediatorServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final MediatorService mediatorService = appbaseCoreBootstrappedServices.getMediatorService();
        if (!excludedMxBeanRegistrations.contains(JmxMediatorService.class) &&
                !excludedMxBeanRegistrations.contains(JmxMetricServiceMXBean.class))
        {
            final JmxMediatorServiceMXBean jmxMediatorServiceMXBean =
                    new JmxMediatorService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            mediatorService,
                            DefaultMediatorServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxMediatorServiceMXBean);
        }
    }

    protected void registerValidationServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final ValidationService validationService = appbaseCoreBootstrappedServices.getValidationService();
        if (!excludedMxBeanRegistrations.contains(JmxValidationService.class) &&
                !excludedMxBeanRegistrations.contains(JmxValidationServiceMXBean.class))
        {
            final JmxValidationServiceMXBean jmxValidationServiceMXBean =
                    new JmxValidationService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            validationService,
                            DefaultValidationServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxValidationServiceMXBean);
        }
    }

    protected void registerMetricServiceMXBean(
            final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final MetricService metricService = appbaseCoreBootstrappedServices.getMetricService();
        if (!excludedMxBeanRegistrations.contains(JmxMetricService.class) &&
                !excludedMxBeanRegistrations.contains(JmxMetricServiceMXBean.class))
        {
            final JmxMetricServiceMXBean jmxMetricServiceMXBean =
                    new JmxMetricService(
                            appbaseCoreBootstrappedServices.getLogService(),
                            metricService,
                            DefaultMetricServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxMetricServiceMXBean);
        }
    }

    protected void registerLogServiceMXBean(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices,
                                            final Set<Class<?>> excludedMxBeanRegistrations) throws JmxServiceException
    {
        final LogService logService = appbaseCoreBootstrappedServices.getLogService();
        if (!excludedMxBeanRegistrations.contains(JmxLogService.class) &&
                !excludedMxBeanRegistrations.contains(JmxLogServiceMXBean.class))
        {
            final JmxLogServiceMXBean jmxLogServiceMXBean =
                    new JmxLogService(
                            logService,
                            logService,
                            DefaultLogServiceConfiguration.class,
                            appbaseCoreBootstrappedServices.getConfigurationService());
            appbaseCoreBootstrappedServices
                    .getJmxService()
                    .registerMXBean(jmxLogServiceMXBean);
        }
    }

    protected void registerAppbaseServices(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        registerLogService(appbaseCoreBootstrappedServices);
        registerMetricService(appbaseCoreBootstrappedServices);
        registerValidationService(appbaseCoreBootstrappedServices);
        registerMediatorService(appbaseCoreBootstrappedServices);
        registerConfigurationService(appbaseCoreBootstrappedServices);
        registerServiceRegistry(appbaseCoreBootstrappedServices);
        registerExceptionService(appbaseCoreBootstrappedServices);
        registerTraceService(appbaseCoreBootstrappedServices);
        registerReactiveService(appbaseCoreBootstrappedServices);
        registerJmxService(appbaseCoreBootstrappedServices);
    }

    protected void registerLogService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final LogService logService = appbaseCoreBootstrappedServices.getLogService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(logService);

        final AppbaseConfiguration configuration = logService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultLogServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices.getConfigurationService().registerManagedServiceByConfigurationKey(
                    logService,
                    ConfigurationKey.fromTypeKey(DefaultLogServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices.getConfigurationService().registerManagedServiceByConfigurationKey(
                    logService,
                    ConfigurationKey.fromTypeKey(LogServiceConfiguration.class));
        }
    }

    protected void registerMetricService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final MetricService metricService = appbaseCoreBootstrappedServices.getMetricService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(metricService);

        final AppbaseConfiguration configuration = metricService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultMetricServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            metricService,
                            ConfigurationKey.fromTypeKey(DefaultMetricServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            metricService,
                            ConfigurationKey.fromTypeKey(MetricServiceConfiguration.class));
        }
    }

    protected void registerValidationService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final ValidationService validationService = appbaseCoreBootstrappedServices.getValidationService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(validationService);

        final AppbaseConfiguration configuration = validationService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultValidationServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            validationService,
                            ConfigurationKey.fromTypeKey(DefaultValidationServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            validationService,
                            ConfigurationKey.fromTypeKey(ValidationServiceConfiguration.class));
        }
    }

    protected void registerMediatorService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final MediatorService mediatorService = appbaseCoreBootstrappedServices.getMediatorService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(mediatorService);

        final AppbaseConfiguration configuration = mediatorService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultMediatorServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            mediatorService,
                            ConfigurationKey.fromTypeKey(DefaultMediatorServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            mediatorService,
                            ConfigurationKey.fromTypeKey(MediatorServiceConfiguration.class));
        }
    }

    protected void registerConfigurationService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final ConfigurationService configurationService = appbaseCoreBootstrappedServices.getConfigurationService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(configurationService);

        final AppbaseConfiguration configuration = configurationService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultConfigurationServiceConfiguration.class)) {
            configurationService.registerManagedServiceByConfigurationKey(
                    configurationService,
                    ConfigurationKey.fromTypeKey(DefaultConfigurationServiceConfiguration.class));
        } else {
            configurationService.registerManagedServiceByConfigurationKey(
                    configurationService,
                    ConfigurationKey.fromTypeKey(ConfigurationServiceConfiguration.class));
        }
    }

    protected void registerServiceRegistry(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final ServiceRegistry serviceRegistry = appbaseCoreBootstrappedServices.getServiceRegistry();
        serviceRegistry.registerAppbaseService(serviceRegistry);

        final AppbaseConfiguration configuration = serviceRegistry.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultServiceRegistryConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            serviceRegistry,
                            ConfigurationKey.fromTypeKey(DefaultServiceRegistryConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            serviceRegistry,
                            ConfigurationKey.fromTypeKey(ServiceRegistryConfiguration.class));
        }
    }

    protected void registerExceptionService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final ExceptionService exceptionService = appbaseCoreBootstrappedServices.getExceptionService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(exceptionService);

        final AppbaseConfiguration configuration = exceptionService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultExceptionServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            exceptionService,
                            ConfigurationKey.fromTypeKey(DefaultExceptionServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            exceptionService,
                            ConfigurationKey.fromTypeKey(ExceptionServiceConfiguration.class));
        }
    }

    protected void registerTraceService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final TracerService tracerService = appbaseCoreBootstrappedServices.getTracerService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(tracerService);

        final AppbaseConfiguration configuration = tracerService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultTracerServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            tracerService,
                            ConfigurationKey.fromTypeKey(DefaultTracerServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            tracerService,
                            ConfigurationKey.fromTypeKey(TracerServiceConfiguration.class));
        }
    }

    protected void registerReactiveService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final ReactorService reactorService = appbaseCoreBootstrappedServices.getReactorService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(reactorService);

        final AppbaseConfiguration configuration = reactorService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultReactorServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            reactorService,
                            ConfigurationKey.fromTypeKey(DefaultReactorServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            reactorService,
                            ConfigurationKey.fromTypeKey(ReactorServiceConfiguration.class));
        }
    }

    protected void registerJmxService(final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices) {
        final JmxService jmxService = appbaseCoreBootstrappedServices.getJmxService();
        appbaseCoreBootstrappedServices
                .getServiceRegistry()
                .registerAppbaseService(jmxService);

        final AppbaseConfiguration configuration = jmxService.getConfiguration();
        if (configuration.getClass().isAssignableFrom(DefaultJmxServiceConfiguration.class)) {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            jmxService,
                            ConfigurationKey.fromTypeKey(DefaultJmxServiceConfiguration.class));
        } else {
            appbaseCoreBootstrappedServices
                    .getConfigurationService()
                    .registerManagedServiceByConfigurationKey(
                            jmxService,
                            ConfigurationKey.fromTypeKey(JmxServiceConfiguration.class));
        }
    }

    protected MicroprofileServiceRegistry newMicroprofileServiceRegistry(final ServiceRegistry serviceRegistry) {
        return new MicroprofileServiceRegistryImpl(serviceRegistry);
    }

    protected MicroprofileConfigurationService newMicroprofileConfigurationService(
            final ConfigurationService configurationService)
    {
        return new MicroprofileConfigurationServiceImpl(configurationService);
    }

    protected JmxService newJmxService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationService configurationService)
    {
        return new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
    }

    protected ReactorService newReactiveService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationService configurationService)
    {
        return new DefaultReactorService(
                logService, metricService, mediatorService, validationService, configurationService);
    }

    protected TracerService newTracerService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationService configurationService)
    {
        return new DefaultTracerService(
                logService, metricService, mediatorService, validationService, configurationService);
    }

    protected ExceptionService newExceptionService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationService configurationService,
            final ExceptionMessageContextCodec exceptionMessageContextCodec)
    {
        final ExceptionAccessor exceptionAccessor = new DefaultExceptionAccessor(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationService,
                exceptionMessageContextCodec);
        return new DefaultExceptionService(
                logService, metricService, mediatorService, validationService, configurationService, exceptionAccessor);
    }

    protected ServiceRegistry newServiceRegistry(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationService configurationService)
    {
        final ReadinessChecker readinessChecker = new DefaultReadinessChecker();
        return new DefaultServiceRegistry(
                logService, metricService, mediatorService, validationService, configurationService, readinessChecker);
    }


    protected ConfigurationService newDefaultConfigurationService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry,
            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        // DefaultConfigurationAccessor ...
        final Map<String, ConfigurationStore> configurationStoreByIdMap =
                appbaseCoreBootstrapRegistry.getConfigurationStoreByIdMap();
        final ConfigurationAccessor configurationAccessor = new DefaultConfigurationAccessor(
                logService,
                metricService,
                appbaseCoreBootstrapRegistry.getConfigurationCodecByMimeTypeMap(),
                appbaseCoreBootstrapRegistry.getConfigurationTypeByRawConfigurationNameMap(),
                configurationStoreByIdMap != null ? configurationStoreByIdMap.values() : Collections.emptyList(),
                appbaseCoreBootstrapRegistry.getWritingConfigurationStoreIdsByKeyMap());

        // YamlConfigurationCodec init ...
        final YamlConfigurationCodec yamlConfigurationCodec = new YamlConfigurationCodec();
        appbaseCoreBootstrapRegistry
                .getConfigurationTypeByYamlNodeNameToDecodeMap()
                .forEach(yamlConfigurationCodec::registerConfigurationTypeByYamlNodeNameToDecode);
        configurationAccessor.registerConfigurationCodecByMimeType(
                yamlConfigurationCodec, MimeType.YAML);
        yamlConfigurationCodec.setLogService(logService);


        // DefaultConfigurationService ...
        final DefaultConfigurationService defaultConfigurationService = new DefaultConfigurationService(
                logService, metricService, mediatorService, validationService, configurationAccessor, null);
        defaultConfigurationService.registerPropertyDeserializer(new YamlPropertyDeserializer());
        registerBootstrapConfigurationsIndividually(appbaseCoreBootstrapConfiguration, defaultConfigurationService);
        if (!AppbaseUtil.isBlank(bootstrapConfigurationFilePath)) {
            final CoreBootstrapConfigurationLoader coreBootstrapConfigurationLoader =
                    new CoreBootstrapConfigurationLoader(
                            bootstrapConfigurationFilePath,
                            appbaseCoreBootstrapRegistry.getConfigurationTypeByYamlNodeNameToDecodeMap(),
                            defaultConfigurationService);
            defaultConfigurationService.setBootstrapConfigurationLoader(coreBootstrapConfigurationLoader);
            try {
                coreBootstrapConfigurationLoader.load();
            } catch (AppbaseException ae) {
                throw new AppbaseRuntimeException("Error while loading bootstrap configurations", ae);
            }
        }
        defaultConfigurationService.load();

        return defaultConfigurationService;
    }

    protected void registerBootstrapConfigurationsIndividually(
            final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration,
            final ConfigurationService configurationService)
    {
        final long timestamp = System.currentTimeMillis();
        final ConfigurationServiceConfiguration configurationServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getConfigurationServiceConfiguration();
        if (configurationServiceConfiguration.getClass().isAssignableFrom(DefaultConfigurationServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultConfigurationServiceConfiguration) configurationServiceConfiguration,
                    DefaultConfigurationServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    configurationServiceConfiguration,
                    ConfigurationServiceConfiguration.class,
                    timestamp);
        }

        final DisruptorServiceConfiguration disruptorServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getDisruptorServiceConfiguration();
        if (disruptorServiceConfiguration.getClass().isAssignableFrom(DefaultDisruptorServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultDisruptorServiceConfiguration) disruptorServiceConfiguration,
                    DefaultDisruptorServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    disruptorServiceConfiguration,
                    DisruptorServiceConfiguration.class,
                    timestamp);
        }

        final ExceptionServiceConfiguration exceptionServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getExceptionServiceConfiguration();
        if (exceptionServiceConfiguration.getClass().isAssignableFrom(DefaultExceptionServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultExceptionServiceConfiguration) exceptionServiceConfiguration,
                    DefaultExceptionServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    exceptionServiceConfiguration,
                    ExceptionServiceConfiguration.class,
                    timestamp);
        }

        final JmxServiceConfiguration jmxServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getJmxServiceConfiguration();
        if (jmxServiceConfiguration.getClass().isAssignableFrom(DefaultJmxServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultJmxServiceConfiguration) jmxServiceConfiguration,
                    DefaultJmxServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    jmxServiceConfiguration,
                    JmxServiceConfiguration.class,
                    timestamp);
        }

        final LogServiceConfiguration logServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getLogServiceConfiguration();
        if (logServiceConfiguration.getClass().isAssignableFrom(DefaultLogServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultLogServiceConfiguration) logServiceConfiguration,
                    DefaultLogServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    logServiceConfiguration,
                    LogServiceConfiguration.class,
                    timestamp);
        }

        final LuceneServiceConfiguration luceneServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getLuceneServiceConfiguration();
        if (luceneServiceConfiguration.getClass().isAssignableFrom(DefaultLuceneServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultLuceneServiceConfiguration) appbaseCoreBootstrapConfiguration.getLuceneServiceConfiguration(),
                    DefaultLuceneServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    appbaseCoreBootstrapConfiguration.getLuceneServiceConfiguration(),
                    LuceneServiceConfiguration.class,
                    timestamp);
        }

        final MediatorServiceConfiguration mediatorServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getMediatorServiceConfiguration();
        if (mediatorServiceConfiguration.getClass().isAssignableFrom(DefaultMediatorServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultMediatorServiceConfiguration) mediatorServiceConfiguration,
                    DefaultMediatorServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    mediatorServiceConfiguration,
                    MediatorServiceConfiguration.class,
                    timestamp);
        }

        final MetricServiceConfiguration metricServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getMetricServiceConfiguration();
        if (metricServiceConfiguration.getClass().isAssignableFrom(DefaultMetricServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultMetricServiceConfiguration) metricServiceConfiguration,
                    DefaultMetricServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    metricServiceConfiguration,
                    MetricServiceConfiguration.class,
                    timestamp);
        }

        final ReactorServiceConfiguration reactiveServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getReactorServiceConfiguration();
        if (reactiveServiceConfiguration.getClass().isAssignableFrom(DefaultReactorServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultReactorServiceConfiguration) reactiveServiceConfiguration,
                    DefaultReactorServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    reactiveServiceConfiguration,
                    ReactorServiceConfiguration.class,
                    timestamp);
        }

        final ServiceRegistryConfiguration serviceRegistryConfiguration =
                appbaseCoreBootstrapConfiguration.getServiceRegistryConfiguration();
        if (serviceRegistryConfiguration.getClass().isAssignableFrom(DefaultServiceRegistryConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultServiceRegistryConfiguration) serviceRegistryConfiguration,
                    DefaultServiceRegistryConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultServiceRegistryConfiguration) serviceRegistryConfiguration,
                    DefaultServiceRegistryConfiguration.class,
                    timestamp);
        }

        final TracerServiceConfiguration tracerServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getTracerServiceConfiguration();
        if (tracerServiceConfiguration.getClass().isAssignableFrom(DefaultTracerServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultTracerServiceConfiguration) tracerServiceConfiguration,
                    DefaultTracerServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    tracerServiceConfiguration,
                    TracerServiceConfiguration.class,
                    timestamp);
        }

        final ValidationServiceConfiguration validationServiceConfiguration =
                appbaseCoreBootstrapConfiguration.getValidationServiceConfiguration();
        if (validationServiceConfiguration.getClass().isAssignableFrom(DefaultValidationServiceConfiguration.class)) {
            registerIndividualConfiguration(
                    configurationService,
                    (DefaultValidationServiceConfiguration)
                            appbaseCoreBootstrapConfiguration.getValidationServiceConfiguration(),
                    DefaultValidationServiceConfiguration.class,
                    timestamp);
        } else {
            registerIndividualConfiguration(
                    configurationService,
                    appbaseCoreBootstrapConfiguration.getValidationServiceConfiguration(),
                    ValidationServiceConfiguration.class,
                    timestamp);
        }
    }

    protected <T extends AppbaseConfiguration> void registerIndividualConfiguration(
            final ConfigurationService configurationService,
            final T configuration,
            final Class<T> configurationType,
            final long timestamp)
    {
        if (configurationService != null && configuration != null && configurationType != null) {
            configurationService.putTypedConfiguration(configurationType, configuration);

            final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(configurationType);
            final ConfigurationValue<AppbaseConfiguration> configurationValue =
                    ConfigurationValue.fromValue(configuration);
            final ConfigurationAddedEvent configurationAddedEvent =
                    new ConfigurationAddedEvent(configurationKey, configurationValue, timestamp);
            try {
                configurationService.notifyConfigurationAddedEvent(configurationAddedEvent);
            } catch (ConfigurationException ce) {
                throw new AppbaseRuntimeException(
                        "Error during configuration added event: " + configurationAddedEvent, ce);
            }
        }
    }


    protected LuceneService newLuceneService(final LogService logService,
                                             final MetricService metricService,
                                             final ValidationService validationService,
                                             final MediatorService mediatorService,
                                             final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration)
    {
        // The default reactive-read boundary config carries no offloadSchedulerId, so it uses
        // Schedulers.boundedElastic() and never consults ReactorService — hence null is passed here rather
        // than reordering this bootstrap to build ReactorService before LuceneService. If a custom
        // offloadSchedulerId is ever wired in, this must thread the ReactorService through instead.
        final ReactiveReadBoundary reactiveReadBoundary = new DefaultReactiveReadBoundary(
                ReactiveReadBoundaryConfiguration.builder().build(), null);
        return new DefaultLuceneService(
                logService, metricService, validationService, mediatorService,
                defaultLuceneServiceConfiguration, reactiveReadBoundary);
    }

    protected MediatorService newMediatorService(final LogService logService,
                                                 final MetricService metricService,
                                                 final ValidationService validationService,
                                                 final DisruptorService<MediatorEvent> disruptorService,
                                                 final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry)
    {
        return new DefaultMediatorService(
                logService, metricService, disruptorService, mediatorEventDelegatorRegistry, validationService);
    }


    protected ValidationService newValidationService(final LogService logService,
                                                     final MetricService metricService)
    {
        return new DefaultValidationService(logService, metricService);
    }


    protected DefaultMetricService newMetricService(final LogService logService,
                                             final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration)
    {
        return new DefaultMetricService(logService, defaultMetricServiceConfiguration);
    }


    protected LogService newLogService(
            final DefaultMediatorEventDelegatorRegistry defaultMediatorEventDelegatorRegistry,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        return new DefaultLogService(defaultMediatorEventDelegatorRegistry, defaultLogServiceConfiguration);
    }


    protected DisruptorService<MediatorEvent> newDisruptorService(
            final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration,
            final ExceptionHandler<MediatorEvent> defaultExceptionHandler,
            final DisruptorMediatorEventHandlerDelegator[] disruptorMediatorEventHandlerDelegators)
    {
        final MediatorEventFactory mediatorEventFactory = new MediatorEventFactory();
        return new DefaultDisruptorService<>(
                disruptorServiceConfiguration,
                mediatorEventFactory,
                defaultExceptionHandler,
                disruptorMediatorEventHandlerDelegators);
    }

    protected DisruptorMediatorEventHandlerDelegator[] newDisruptorMediatorEventHandlerDelegators(
            final DefaultMediatorEventDelegatorRegistry defaultMediatorEventDelegatorRegistry,
            final int disruptorEventHandlerCount)
    {
        final DisruptorMediatorEventHandlerDelegator[] defaultDisruptorMediatorEventDelegators =
                new DisruptorMediatorEventHandlerDelegator[disruptorEventHandlerCount];
        for (int i = 0; i < disruptorEventHandlerCount; ++i) {
            defaultDisruptorMediatorEventDelegators[i] =
                    new DisruptorMediatorEventHandlerDelegator(defaultMediatorEventDelegatorRegistry);
        }

        return defaultDisruptorMediatorEventDelegators;
    }

    protected DefaultExceptionHandler<MediatorEvent> newDefaultExceptionHandler() {
        return new DefaultExceptionHandler<>();
    }

    protected DefaultMediatorEventDelegatorRegistry newDefaultMediatorEventDelegatorRegistry() {
        return new DefaultMediatorEventDelegatorRegistry();
    }
}
