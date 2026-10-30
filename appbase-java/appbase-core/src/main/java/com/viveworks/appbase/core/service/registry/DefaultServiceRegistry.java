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

package com.viveworks.appbase.core.service.registry;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AbstractAppbaseService;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListenerException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.MessageHandler;
import com.viveworks.appbase.core.service.mediator.MessageHandlerException;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.message.ServiceStatusMessage;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.MutableReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link ServiceRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultServiceRegistry extends AbstractAppbaseService implements ServiceRegistry {
    private final ValidationService validationService;
    private final AtomicReference<DefaultServiceRegistryConfiguration> defaultServiceRegistryConfigurationRef;
    private final AtomicReference<ScheduledFuture<?>> scheduledFutureRef;
    private final ConcurrentMap<String, AppbaseServiceEntry> appbaseServiceEntryByNameMap;
    private final ReadinessChecker readinessChecker;

    private final Gauge appbaseServiceEntryByNameMapGauge;
    private final Counter configurationAddedCounter;
    private final Counter configurationChangedCounter;
    private final Counter configurationRemovedCounter;
    private final Counter serviceStatusMessageCounter;
    private final Counter appbaseServiceEntryDoesNotExistCounter;

    private final ReentrantLock eventLock;
    private final ReentrantLock pollLock;

    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS")
    public DefaultServiceRegistry(final LogService logService,
                                  final MetricService metricService,
                                  final MediatorService mediatorService,
                                  final ValidationService validationService,
                                  final ConfigurationService configurationService,
                                  final ReadinessChecker readinessChecker)
    {
        super(logService, metricService, mediatorService);
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(configurationService, "configurationService");
        AppbaseUtil.notNull(readinessChecker, "readinessChecker");

        this.validationService = validationService;
        this.readinessChecker = readinessChecker;

        DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration =
                configurationService.getTypedConfiguration(DefaultServiceRegistryConfiguration.class);
        if (defaultServiceRegistryConfiguration == null) {
            final ServiceRegistryConfiguration serviceRegistryConfiguration =
                    configurationService.getTypedConfiguration(ServiceRegistryConfiguration.class);
            if (serviceRegistryConfiguration instanceof DefaultServiceRegistryConfiguration) {
                defaultServiceRegistryConfiguration =
                        (DefaultServiceRegistryConfiguration) serviceRegistryConfiguration;
            } else {
                throw new AppbaseRuntimeException("Cannot find " +
                        DefaultServiceRegistryConfiguration.class.getName() +
                        " in " +
                        ConfigurationService.class.getName());
            }
        }

        AppbaseUtil.validate(validationService, defaultServiceRegistryConfiguration);
        this.defaultServiceRegistryConfigurationRef = new AtomicReference<>(defaultServiceRegistryConfiguration);
        try {
            mediatorService
                    .getMediator()
                    .registerMessageHandlerByMessageType(MessageType.SERVICE_STATUS, this);
        } catch (MediatorException me) {
            throw new AppbaseRuntimeException("Failed to register " +
                    getClass().getName() +
                    " as " +
                    MessageHandler.class.getName(),
                    me);
        }

        this.appbaseServiceEntryByNameMap = new ConcurrentHashMap<>();

        this.scheduledFutureRef = new AtomicReference<>();
        if (defaultServiceRegistryConfiguration.isEnablePolling()) {
            init(defaultServiceRegistryConfiguration);
        }

        if (defaultServiceRegistryConfiguration.isRegisterAsConfigurationListener()) {
            configurationService.registerConfigurationListenerByKey(
                    this,
                    ConfigurationKey.fromTypeKey(DefaultServiceRegistryConfiguration.class));
        }

        this.appbaseServiceEntryByNameMapGauge = metricService
                .getMetricRegistry()
                .register(
                        metricService.name(getClass(), "appbaseServiceEntryByNameMap", "size", "gauge"),
                        (Gauge<Integer>) appbaseServiceEntryByNameMap::size);
        configurationAddedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationAdded", "counter"));
        configurationChangedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationChanged", "counter"));
        configurationRemovedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationRemoved", "counter"));
        serviceStatusMessageCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "serviceStatusMessage", "counter"));
        appbaseServiceEntryDoesNotExistCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "appbaseServiceEntryDoesNotExist", "counter"));

        eventLock = new ReentrantLock();
        pollLock = new ReentrantLock();
        statusRef.set(ServiceStatus.STARTED);
    }

    @Override
    public void pollAndUpdateStatuses() {
        pollLock.lock();
        try {
            appbaseServiceEntryByNameMap.forEach((serviceName, appbaseServiceEntry) -> {
                final AppbaseService service = appbaseServiceEntry.getService();
                final ServiceStatus newServiceStatus = service.getStatus();

                if (newServiceStatus != null &&
                        !(appbaseServiceEntry.getServiceStatus() == newServiceStatus))
                {
                    final AppbaseServiceEntry newAppbaseServiceEntry =
                            new AppbaseServiceEntry(service, newServiceStatus);
                    appbaseServiceEntryByNameMap.put(serviceName, newAppbaseServiceEntry);
                }
            });
        } finally {
            pollLock.unlock();
        }
    }

    private void init(final DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration) {
        AppbaseUtil.validate(validationService, defaultServiceRegistryConfiguration);

        if (defaultServiceRegistryConfiguration.isEnablePolling()) {
            final SchedulableConfiguration schedulableConfiguration =
                    defaultServiceRegistryConfiguration.getPollingSchedulableConfiguration();
            AppbaseUtil.notNull(schedulableConfiguration, "schedulableConfiguration");

            final long initialDelay = schedulableConfiguration.getInitialDelay();
            final long interval = schedulableConfiguration.getInterval();
            final TimeUnit timeUnit = schedulableConfiguration.getTimeUnit();

            final Schedulable<Object> schedulable = new Schedulable<>() {
                @Override
                public Object schedule() {
                    pollAndUpdateStatuses();
                    return null;
                }

                @Override
                public SchedulableConfiguration getSchedulableConfiguration() {
                    return schedulableConfiguration;
                }
            };

            final ScheduledFuture<?> currentScheduledFuture = this.scheduledFutureRef.get();
            if (currentScheduledFuture != null) {
                currentScheduledFuture.cancel(true);
            }

            final ScheduledFuture<Object> scheduledFuture = mediatorService
                    .getMediator()
                    .scheduleWithFixedDelay(schedulable, initialDelay, interval, timeUnit);
            this.scheduledFutureRef.set(scheduledFuture);
        }

        this.defaultServiceRegistryConfigurationRef.set(defaultServiceRegistryConfiguration);
    }

    @Override
    public void onMessage(final Message message) throws MessageHandlerException {
        AppbaseUtil.notNull(message, "message");

        logger.info("Got message: {}", message);
        final ServiceStatusMessage serviceStatusMessage = message.getServiceStatusMessage();
        if (serviceStatusMessage == null) {
            throw new MessageHandlerException("Service status message must not be null");

        }

        final Class<? extends AppbaseService> serviceType = serviceStatusMessage.getServiceType();
        if (serviceType == null) {
            throw new MessageHandlerException("Service type must not be null");
        }

        final ServiceStatus newServiceStatus = serviceStatusMessage.getServiceStatus();
        if (newServiceStatus == null) {
            throw new MessageHandlerException("New service status must not be null");
        }

        final String serviceName = serviceType.getName();
        final AppbaseServiceEntry appbaseServiceEntry = appbaseServiceEntryByNameMap.get(serviceName);
        if (appbaseServiceEntry != null) {
            serviceStatusMessageCounter.inc();

            final ServiceStatus serviceStatus = appbaseServiceEntry.getServiceStatus();
            if (!(newServiceStatus == serviceStatus)) {
                final AppbaseService service = appbaseServiceEntry.getService();
                final AppbaseServiceEntry newAppbaseServiceEntry =
                        new AppbaseServiceEntry(service, newServiceStatus);
                appbaseServiceEntryByNameMap.put(serviceName, newAppbaseServiceEntry);
            }
        } else {
            logger.warn("Appbase service entry does not exist for {}", serviceName);
            appbaseServiceEntryDoesNotExistCounter.inc();
        }
    }

    private void initOnNewConfiguration(
            final ConfigurationKey configurationKey,
            final ConfigurationValue<?> configurationValue,
            final Counter counter) throws ConfigurationListenerException
    {
        if (configurationKey == null) {
            throw new ConfigurationListenerException("Configuration key must not be null");
        }
        if (configurationValue == null) {
            throw new ConfigurationListenerException(
                    "Configuration value must not be null for key " + configurationKey);
        }

        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        final Object value = configurationValue.getValue();
        if (typeKey == null) {
            throw new ConfigurationListenerException(
                    "Type key must not be null for " + configurationKey);
        }

        if (value == null) {
            throw new ConfigurationListenerException(
                    "Value must not be null for " + configurationKey);
        }

        if (!typeKey.equals(DefaultServiceRegistryConfiguration.class)) {
            throw new ConfigurationListenerException(
                    "Type key must be of type " +
                            DefaultServiceRegistryConfiguration.class.getName() +
                            " in " +
                            configurationKey);
        }

        if (!DefaultServiceRegistryConfiguration.class.isAssignableFrom(value.getClass())) {
            throw new ConfigurationListenerException(
                    "Value must be of type " +
                            DefaultServiceRegistryConfiguration.class.getName() +
                            ". It is of type " +
                            value.getClass().getName());
        }

        final DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration =
                (DefaultServiceRegistryConfiguration) value;
        init(defaultServiceRegistryConfiguration);
        updateStatus(ServiceStatus.STARTED);
        counter.inc();
    }

    @Override
    public void onConfigurationAdded(
            final ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationAddedEvent, "configurationAddedEvent");
        AppbaseUtil.validate(validationService, configurationAddedEvent);

        logger.info("Received event: {}", configurationAddedEvent);
        eventLock.lock();
        try {
            final ConfigurationKey configurationKey = configurationAddedEvent.getConfigurationKey();
            final ConfigurationValue<?> configurationValue = configurationAddedEvent.getConfigurationValue();
            initOnNewConfiguration(configurationKey, configurationValue, configurationAddedCounter);
        } finally {
            eventLock.unlock();
        }
    }

    @Override
    public void onConfigurationChanged(
            final ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationChangedEvent, "configurationChangedEvent");
        AppbaseUtil.validate(validationService, configurationChangedEvent);

        logger.info("Received event: {}", configurationChangedEvent);
        eventLock.lock();
        try {
            final ConfigurationKey configurationKey = configurationChangedEvent.getConfigurationKey();
            final ConfigurationValue<?> newConfigurationValue = configurationChangedEvent.getNewConfigurationValue();
            initOnNewConfiguration(configurationKey, newConfigurationValue, configurationChangedCounter);
        } finally {
            eventLock.unlock();
        }
    }

    @Override
    public void onConfigurationRemoved(
            final ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationRemovedEvent, "configurationRemovedEvent");
        AppbaseUtil.validate(validationService, configurationRemovedEvent);

        logger.info("Received event: {}", configurationRemovedEvent);
        eventLock.lock();
        try {
            final ConfigurationKey configurationKey = configurationRemovedEvent.getConfigurationKey();
            final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();

            if (typeKey == null || !typeKey.equals(DefaultServiceRegistryConfiguration.class)) {
                throw new ConfigurationListenerException(
                        "Type key cannot be null and must be of type " +
                                DefaultServiceRegistryConfiguration.class.getName());
            }

            shutdown();
            configurationRemovedCounter.inc();
        } finally {
            eventLock.unlock();
        }
    }

    @Override
    public AppbaseServiceEntry getAppbaseService(final Class<? extends AppbaseService> appbaseServiceType) {
        return appbaseServiceEntryByNameMap.get(appbaseServiceType.getName());
    }

    @Override
    public Collection<AppbaseServiceEntry> getAppbaseServices() {
        return Collections.unmodifiableCollection(appbaseServiceEntryByNameMap.values());
    }

    @Override
    public Optional<UnreadyAppbaseServiceEntries> checkReady() {
        return readinessChecker.checkReady(getAppbaseServices());
    }

    @Override
    public OverallStatus overallStatus() {
        return overallStatus(getAppbaseServices());
    }

    static OverallStatus overallStatus(final Collection<AppbaseServiceEntry> appbaseServices) {
        final Set<String> startedServiceNames = new HashSet<>();
        final Map<String, ServiceStatus> notStartedServiceStatusByNameMap = new HashMap<>();
        final MutableReference<Boolean> startedRef = new MutableReference<>(true);

        appbaseServices.forEach(appbaseServiceEntry -> {
            final AppbaseService appbaseService = appbaseServiceEntry.getService();
            final ServiceStatus serviceStatus = appbaseService.getStatus();
            final String name = appbaseService.getClass().getName();

            if (serviceStatus == ServiceStatus.STARTED) {
                startedServiceNames.add(name);
            } else {
                if (startedRef.getReference()) {
                    startedRef.setReference(false);
                }
                notStartedServiceStatusByNameMap.put(name, appbaseService.getStatus());
            }
        });

        return new OverallStatus(startedRef.getReference(), startedServiceNames, notStartedServiceStatusByNameMap);
    }

    @Override
    public OverallStatus pollOverallStatus() {
        pollAndUpdateStatuses();
        return overallStatus();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultServiceRegistryConfiguration getConfiguration() {
        return defaultServiceRegistryConfigurationRef.get();
    }

    @Override
    public void registerAppbaseService(final AppbaseService service) {
        AppbaseUtil.notNull(service, "service");
        AppbaseUtil.notNull(service.getStatus(), "service.getStatus()");

        logger.info("Registering service {} with status {}", service.getClass(), service.getStatus());
        final AppbaseServiceEntry appbaseServiceEntry = new AppbaseServiceEntry(service, service.getStatus());
        appbaseServiceEntryByNameMap.put(service.getClass().getName(), appbaseServiceEntry);
    }

    @Override
    public <T extends AppbaseService> void registerAppbaseService(
            final Class<T> appbaseServiceType, final T appbaseService)
    {
        AppbaseUtil.notNull(appbaseServiceType, "serviceType");
        AppbaseUtil.notNull(appbaseService, "service");
        AppbaseUtil.notNull(appbaseService.getStatus(), "service.getStatus()");

        logger.info("Registering service type {} for service {} with status {}",
                appbaseServiceType.getName(), appbaseService.getClass().getName(), appbaseService.getStatus());
        final AppbaseServiceEntry appbaseServiceEntry =
                new AppbaseServiceEntry(appbaseService, appbaseService.getStatus());
        appbaseServiceEntryByNameMap.put(appbaseServiceType.getName(), appbaseServiceEntry);
    }

    @Override
    public void unregisterAppbaseService(final Class<? extends AppbaseService> appbaseServiceType) {
        AppbaseUtil.notNull(appbaseServiceType, "appbaseServiceType");

        final String appbaseServiceTypeName = appbaseServiceType.getName();
        logger.info("Unregistering service [{}]", appbaseServiceTypeName);
        appbaseServiceEntryByNameMap.remove(appbaseServiceTypeName);
    }

    @Override
    public void clearAppbaseServices() {
        logger.info("Clearing appbaseServiceEntryByNameMap");
        appbaseServiceEntryByNameMap.clear();
    }

    @Override
    protected <C extends AppbaseConfiguration> void reloadImpl(
            final C configuration) throws ServiceReloadException
    {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultServiceRegistryConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultServiceRegistryConfiguration.class.getName());
        }

        eventLock.lock();
        try {
            init((DefaultServiceRegistryConfiguration) configuration);
        } finally {
            eventLock.unlock();
        }
    }

    @Override
    protected void shutdownImpl() {
        final ScheduledFuture<?> scheduledFuture = scheduledFutureRef.get();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        clearAppbaseServices();
    }

    public Gauge getAppbaseServiceEntryByNameMapGauge() {
        return appbaseServiceEntryByNameMapGauge;
    }
}
