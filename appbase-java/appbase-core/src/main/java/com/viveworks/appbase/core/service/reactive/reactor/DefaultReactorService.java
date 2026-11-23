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

package com.viveworks.appbase.core.service.reactive.reactor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AbstractAppbaseService;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListenerException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.SchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.BoundedElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ImmediateSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ParallelSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SingleSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.BoundedElasticSchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ElasticSchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ExecutorServiceSchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ImmediateSchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.MediatorExecutorServiceSchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.ParallelSchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory.SingleSchedulerFactory;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;
import reactor.core.scheduler.Scheduler;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link ReactorService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultReactorService
        extends AbstractAppbaseService implements ReactorService, ConfigurationListener
{
    private final ValidationService validationService;
    private final AtomicReference<Scheduler> defaultSchedulerRef;
    private final Map<String, Scheduler> schedulerByIdMap;
    private final Map<String, SchedulerFactory<?>> schedulerFactoryBySchedulerConfigurationTypeNameMap;
    private final AtomicReference<DefaultReactorServiceConfiguration> defaultReactiveServiceConfigurationRef;
    private final Counter configurationAddedCounter;
    private final Counter configurationChangedCounter;
    private final Counter configurationRemovedCounter;
    private final ReentrantLock initLock;

    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS")
    public DefaultReactorService(final LogService logService,
                                 final MetricService metricService,
                                 final MediatorService mediatorService,
                                 final ValidationService validationService,
                                 final ConfigurationService configurationService)
    {
        super(logService, metricService, mediatorService);
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(configurationService, "configurationService");

        this.validationService = validationService;

        DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration =
                configurationService.getTypedConfiguration(DefaultReactorServiceConfiguration.class);
        if (defaultReactiveServiceConfiguration == null) {
            final ReactorServiceConfiguration reactorServiceConfiguration =
                    configurationService.getTypedConfiguration(ReactorServiceConfiguration.class);
            if (reactorServiceConfiguration instanceof DefaultReactorServiceConfiguration) {
                defaultReactiveServiceConfiguration =
                        (DefaultReactorServiceConfiguration) reactorServiceConfiguration;
            } else {
                throw new AppbaseRuntimeException("Cannot find " +
                        DefaultReactorServiceConfiguration.class.getName() +
                        " in " +
                        ConfigurationService.class.getName());
            }
        }

        AppbaseUtil.validate(validationService, defaultReactiveServiceConfiguration);
        this.defaultReactiveServiceConfigurationRef = new AtomicReference<>(defaultReactiveServiceConfiguration);
        this.defaultSchedulerRef = new AtomicReference<>();

        this.schedulerByIdMap = new ConcurrentHashMap<>();
        this.schedulerFactoryBySchedulerConfigurationTypeNameMap = new ConcurrentHashMap<>();
        this.initLock = new ReentrantLock();
        initDefaultSchedulerFactories();
        try {
            init(defaultReactiveServiceConfiguration);
        } catch (SchedulerFactoryNotFoundException | UnsupportedSchedulerConfigurationTypeException ex) {
            throw new AppbaseRuntimeException("Failed to initialize " + getClass().getName(), ex);
        }

        this.configurationAddedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationAdded", "counter"));
        this.configurationChangedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationChanged", "counter"));
        this.configurationRemovedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationRemoved", "counter"));
        statusRef.set(ServiceStatus.STARTED);
        configurationService.registerConfigurationListenerByKey(
                this, ConfigurationKey.fromTypeKey(DefaultReactorServiceConfiguration.class));
    }

    private void initDefaultSchedulerFactories() {
        final BoundedElasticSchedulerFactory boundedElasticSchedulerFactory =
                new BoundedElasticSchedulerFactory(logService);
        registerSchedulerFactoryByConfigurationType(
                BoundedElasticSchedulerConfiguration.class,
                boundedElasticSchedulerFactory);

        final ElasticSchedulerFactory elasticSchedulerFactory = new ElasticSchedulerFactory(logService);
        registerSchedulerFactoryByConfigurationType(
                ElasticSchedulerConfiguration.class,
                elasticSchedulerFactory);

        final ExecutorServiceSchedulerFactory executorServiceSchedulerFactory =
                new ExecutorServiceSchedulerFactory(logService, metricService);
        registerSchedulerFactoryByConfigurationType(
                ExecutorServiceSchedulerConfiguration.class,
                executorServiceSchedulerFactory);

        final ImmediateSchedulerFactory immediateSchedulerFactory = new ImmediateSchedulerFactory();
        registerSchedulerFactoryByConfigurationType(
                ImmediateSchedulerConfiguration.class,
                immediateSchedulerFactory);

        final MediatorExecutorServiceSchedulerFactory mediatorExecutorServiceSchedulerFactory =
                new MediatorExecutorServiceSchedulerFactory(mediatorService);
        registerSchedulerFactoryByConfigurationType(
                MediatorExecutorServiceSchedulerConfiguration.class,
                mediatorExecutorServiceSchedulerFactory);

        final ParallelSchedulerFactory parallelSchedulerFactory = new ParallelSchedulerFactory(logService);
        registerSchedulerFactoryByConfigurationType(
                ParallelSchedulerConfiguration.class,
                parallelSchedulerFactory);

        final SingleSchedulerFactory singleSchedulerFactory = new SingleSchedulerFactory(logService);
        registerSchedulerFactoryByConfigurationType(
                SingleSchedulerConfiguration.class,
                singleSchedulerFactory);
    }

    @Override
    public Scheduler getDefaultScheduler() {
        return defaultSchedulerRef.get();
    }

    @Nullable
    @Override
    public Scheduler getSchedulerById(final String schedulerId) {
        AppbaseUtil.notNull(schedulerId, "schedulerId");

        return schedulerByIdMap.get(schedulerId);
    }

    @SuppressWarnings({"checkstyle:RightCurly", "checkstyle:LeftCurly"})
    private void init(
            final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration)
            throws SchedulerFactoryNotFoundException, UnsupportedSchedulerConfigurationTypeException
    {
        AppbaseUtil.notNull(defaultReactiveServiceConfiguration, "defaultReactiveServiceConfiguration");

        initLock.lock();
        try {
            if (defaultReactiveServiceConfiguration.isEnabled()) {
                final List<SchedulerConfiguration> schedulerConfigurations =
                        defaultReactiveServiceConfiguration.getSchedulerConfigurations();
                if (schedulerConfigurations != null) {
                    for (final SchedulerConfiguration schedulerConfiguration: schedulerConfigurations) {
                        final Class<? extends SchedulerConfiguration> schedulerConfigurationType =
                                schedulerConfiguration.getClass();

                        if (schedulerConfigurationType
                                .isAssignableFrom(BoundedElasticSchedulerConfiguration.class))
                        {
                            final BoundedElasticSchedulerConfiguration boundedElasticSchedulerConfiguration =
                                    (BoundedElasticSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, boundedElasticSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    BoundedElasticSchedulerConfiguration.class, boundedElasticSchedulerConfiguration);
                        }
                        else if (schedulerConfigurationType
                                .isAssignableFrom(ElasticSchedulerConfiguration.class))
                        {
                            final ElasticSchedulerConfiguration elasticSchedulerConfiguration =
                                    (ElasticSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, elasticSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    ElasticSchedulerConfiguration.class, elasticSchedulerConfiguration);
                        }
                        else if (schedulerConfigurationType
                                .isAssignableFrom(ExecutorServiceSchedulerConfiguration.class))
                        {
                            final ExecutorServiceSchedulerConfiguration executorServiceSchedulerConfiguration =
                                    (ExecutorServiceSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, executorServiceSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    ExecutorServiceSchedulerConfiguration.class, executorServiceSchedulerConfiguration);
                        }
                        else if (schedulerConfigurationType
                                .isAssignableFrom(ImmediateSchedulerConfiguration.class))
                        {
                            final ImmediateSchedulerConfiguration immediateSchedulerConfiguration =
                                    (ImmediateSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, immediateSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    ImmediateSchedulerConfiguration.class, immediateSchedulerConfiguration);
                        } else if (schedulerConfigurationType.isAssignableFrom(
                                MediatorExecutorServiceSchedulerConfiguration.class))
                        {
                            final MediatorExecutorServiceSchedulerConfiguration
                                    mediatorExecutorServiceSchedulerConfiguration =
                                    (MediatorExecutorServiceSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, mediatorExecutorServiceSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    MediatorExecutorServiceSchedulerConfiguration.class,
                                    (MediatorExecutorServiceSchedulerConfiguration) schedulerConfiguration);
                        }
                        else if (schedulerConfigurationType.isAssignableFrom(ParallelSchedulerConfiguration.class))
                        {
                            final ParallelSchedulerConfiguration parallelSchedulerConfiguration =
                                    (ParallelSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, parallelSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    ParallelSchedulerConfiguration.class,
                                    (ParallelSchedulerConfiguration) schedulerConfiguration);
                        }
                        else if (schedulerConfigurationType.isAssignableFrom(SingleSchedulerConfiguration.class))
                        {
                            final SingleSchedulerConfiguration singleSchedulerConfiguration =
                                    (SingleSchedulerConfiguration) schedulerConfiguration;
                            AppbaseUtil.validate(validationService, singleSchedulerConfiguration);

                            processSchedulerConfiguration(
                                    SingleSchedulerConfiguration.class,
                                    (SingleSchedulerConfiguration) schedulerConfiguration);
                        }
                        else
                        {
                            throw new UnsupportedSchedulerConfigurationTypeException(
                                    "Unsupported scheduler configuration type " +
                                            schedulerConfiguration.getClass().getName() +
                                            ". Configuration=" + schedulerConfiguration);
                        }
                    }
                }

                final Scheduler defaultScheduler = defaultSchedulerRef.get();
                if (defaultScheduler == null) {
                    final SchedulerFactory<?> schedulerFactory =
                            this.schedulerFactoryBySchedulerConfigurationTypeNameMap.get(
                                    MediatorExecutorServiceSchedulerConfiguration.class.getName());
                    if (schedulerFactory == null) {
                        final MediatorExecutorServiceSchedulerFactory mediatorExecutorServiceSchedulerFactory =
                                new MediatorExecutorServiceSchedulerFactory(mediatorService);
                        final MediatorExecutorServiceSchedulerConfiguration
                                mediatorExecutorServiceSchedulerConfiguration =
                                MediatorExecutorServiceSchedulerConfiguration.builder().build();
                        final Scheduler scheduler = mediatorExecutorServiceSchedulerFactory.newScheduler(
                                mediatorExecutorServiceSchedulerConfiguration);
                        defaultSchedulerRef.set(scheduler);

                    }
                }
            } else {
                shutdown();
            }
        } finally {
            initLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends SchedulerConfiguration> void processSchedulerConfiguration(
            final Class<C> schedulerConfigurationType,
            final C schedulerConfiguration)
            throws SchedulerFactoryNotFoundException
    {
        final SchedulerFactory<C> schedulerFactory =
                (SchedulerFactory<C>) this.schedulerFactoryBySchedulerConfigurationTypeNameMap.get(
                        schedulerConfigurationType.getName());

        if (schedulerFactory == null) {
            throw new SchedulerFactoryNotFoundException(
                    "Cannot find scheduler factory for configuration type " + schedulerConfigurationType.getName());
        }
        logger.info("Using scheduler factory {} for scheduler configuration type {}",
                schedulerFactory.getClass().getName(), schedulerConfigurationType.getName());

        final Scheduler scheduler = schedulerFactory.newScheduler(schedulerConfiguration);
        logger.info("Registering scheduler {} with ID {}",
                scheduler.getClass().getName(), schedulerConfiguration.getId());
        this.schedulerByIdMap.put(schedulerConfiguration.getId(), scheduler);

        if (schedulerConfiguration.isDefaultScheduler()) {
            logger.info("Registering default scheduler {}", scheduler.getClass().getName());
            defaultSchedulerRef.set(scheduler);
        }
    }

    @Override
    public void onConfigurationAdded(
            final ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationAddedEvent, "configurationAddedEvent");
        AppbaseUtil.validate(validationService, configurationAddedEvent);

        logger.info("Received event: {}", configurationAddedEvent);
        final ConfigurationKey configurationKey = configurationAddedEvent.getConfigurationKey();
        final ConfigurationValue<?> configurationValue = configurationAddedEvent.getConfigurationValue();

        try {
            reloadOnEvent(configurationKey, configurationValue);
            configurationAddedCounter.inc();
        } catch (ServiceReloadException sre) {
            throw new ConfigurationListenerException(
                    "Error while reloading service for event " + configurationAddedEvent, sre);
        }
    }

    @Override
    public void onConfigurationChanged(
            final ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationChangedEvent, "configurationChangedEvent");
        AppbaseUtil.validate(validationService, configurationChangedEvent);

        logger.info("Received event: {}", configurationChangedEvent);
        final ConfigurationKey configurationKey = configurationChangedEvent.getConfigurationKey();
        final ConfigurationValue<?> configurationValue = configurationChangedEvent.getNewConfigurationValue();

        try {
            reloadOnEvent(configurationKey, configurationValue);
            configurationChangedCounter.inc();
        } catch (ServiceReloadException sre) {
            throw new ConfigurationListenerException(
                    "Error while reloading service for event " + configurationChangedEvent, sre);
        }
    }

    @Override
    public void onConfigurationRemoved(
            final ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationRemovedEvent, "configurationRemovedEvent");
        AppbaseUtil.validate(validationService, configurationRemovedEvent);

        logger.info("Received event: {}", configurationRemovedEvent);
        final ConfigurationKey configurationKey = configurationRemovedEvent.getConfigurationKey();
        verifyConfigurationKey(configurationKey);
        shutdown();
        configurationRemovedCounter.inc();
    }

    private void reloadOnEvent(
            final ConfigurationKey configurationKey,
            final ConfigurationValue<?> configurationValue)
            throws ServiceReloadException, ConfigurationListenerException
    {
        verifyConfigurationKey(configurationKey);
        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration =
                (DefaultReactorServiceConfiguration) configurationValue.getValue();
        reload(defaultReactiveServiceConfiguration);
    }

    private void verifyConfigurationKey(
            final ConfigurationKey configurationKey) throws ConfigurationListenerException
    {
        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        if (typeKey == null || !typeKey.isAssignableFrom(DefaultReactorServiceConfiguration.class)) {
            throw new ConfigurationListenerException(
                    "Configuration type key is not of type " + DefaultReactorServiceConfiguration.class.getName());
        }
    }

    @Override
    @Unmodifiable
    public Map<String, SchedulerFactory<?>> getSchedulerFactoryByConfigurationTypeMap() {
        return Collections.unmodifiableMap(schedulerFactoryBySchedulerConfigurationTypeNameMap);
    }

    @Override
    public <C extends SchedulerConfiguration> void registerSchedulerFactoryByConfigurationType(
            final Class<C> schedulerConfigurationType, final SchedulerFactory<C> schedulerFactory)
    {
        AppbaseUtil.notNull(schedulerConfigurationType, "schedulerConfigurationType");
        AppbaseUtil.notNull(schedulerFactory, "schedulerFactory");

        logger.info("Registering scheduler factory {} for scheduler configuration type {}",
                schedulerFactory.getClass().getName(), schedulerConfigurationType.getName());
        this.schedulerFactoryBySchedulerConfigurationTypeNameMap.put(
                schedulerConfigurationType.getName(), schedulerFactory);
    }

    @Override
    public void unregisterSchedulerFactoryByConfigurationType(
            final Class<? extends SchedulerConfiguration> schedulerConfigurationType)
    {
        AppbaseUtil.notNull(schedulerConfigurationType, "schedulerConfigurationType");

        logger.info("Unregistering scheduler factory by configuration type [{}]", schedulerConfigurationType);
        schedulerFactoryBySchedulerConfigurationTypeNameMap.remove(schedulerConfigurationType.getName());
    }

    @Override
    public void clearSchedulerFactoryByConfigurationTypeMap() {
        logger.info("Clearing schedulerFactoryBySchedulerConfigurationTypeNameMap");
        schedulerFactoryBySchedulerConfigurationTypeNameMap.clear();
    }

    @Override
    protected <C extends AppbaseConfiguration> void reloadImpl(
            final C configuration) throws ServiceReloadException
    {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultReactorServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultReactorServiceConfiguration.class.getName());
        }

        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration =
                (DefaultReactorServiceConfiguration) configuration;
        AppbaseUtil.validate(validationService, defaultReactiveServiceConfiguration);

        try {
            init(defaultReactiveServiceConfiguration);
        } catch (SchedulerFactoryNotFoundException | UnsupportedSchedulerConfigurationTypeException ex) {
            throw new ServiceReloadException("Failed to reload " + getClass().getName(), ex);
        }
        this.defaultReactiveServiceConfigurationRef.set(defaultReactiveServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        schedulerByIdMap.forEach((id, scheduler) -> scheduler.dispose());

        final Scheduler scheduler = defaultSchedulerRef.get();
        if (scheduler != null) {
            scheduler.dispose();
        }

        clearSchedulerFactoryByConfigurationTypeMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultReactorServiceConfiguration getConfiguration() {
        return defaultReactiveServiceConfigurationRef.get();
    }
}
