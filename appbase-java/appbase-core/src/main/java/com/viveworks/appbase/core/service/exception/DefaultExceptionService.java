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

package com.viveworks.appbase.core.service.exception;

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
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessorException;
import com.viveworks.appbase.core.service.exception.listener.ExceptionListener;
import com.viveworks.appbase.core.service.exception.store.DefaultExpiredMessageCleaner;
import com.viveworks.appbase.core.service.exception.store.DefaultExpiredMessageCleanerConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExpiredMessageCleaner;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.MessageHandler;
import com.viveworks.appbase.core.service.mediator.MessageHandlerException;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link ExceptionService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultExceptionService
        extends AbstractAppbaseService implements ExceptionService, MessageHandler, ConfigurationListener
{
    private final ValidationService validationService;
    private final AtomicReference<DefaultExceptionServiceConfiguration> defaultExceptionServiceConfigurationRef;
    private final AtomicReference<ScheduledFuture<Void>> expiredMessagesCleanerScheduledFutureRef;
    private final ExceptionAccessor exceptionAccessor;

    private final Map<String, List<ExceptionListener>> exceptionListenersByExceptionTypeNameMap;
    private final Gauge exceptionListenersByExceptionTypeNameMapSizeGauge;
    private final Map<String, ExceptionListener> globalExceptionListenerByTypeNameMap;
    private final Gauge globalExceptionListenerByTypeNameMapSizeGauge;
    private final Counter exceptionListenerErrorCounter;
    private final Map<String, Counter> errorCounterByExceptionStoreTypeNameMap;
    private final Timer expiredMessagesCleanerTimer;
    private final Counter expiredMessagesCleanerErrorCounter;
    private final Counter configurationAddedCounter;
    private final Counter configurationChangedCounter;
    private final Counter configurationRemovedCounter;

    private final ReentrantLock expiredMessagesCleanerLock;

    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
    public DefaultExceptionService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationService configurationService,
            final ExceptionAccessor exceptionAccessor)
    {
        super(logService, metricService, mediatorService);

        AppbaseUtil.notNull(configurationService, "configurationService");
        AppbaseUtil.notNull(exceptionAccessor, "exceptionAccessor");
        AppbaseUtil.notNull(validationService, "validationService");

        this.exceptionAccessor = exceptionAccessor;
        this.validationService = validationService;

        DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                configurationService.getTypedConfiguration(DefaultExceptionServiceConfiguration.class);
        if (defaultExceptionServiceConfiguration == null) {
            final ExceptionServiceConfiguration exceptionServiceConfiguration =
                    configurationService.getTypedConfiguration(ExceptionServiceConfiguration.class);
            if (exceptionServiceConfiguration instanceof DefaultExceptionServiceConfiguration) {
                defaultExceptionServiceConfiguration =
                        (DefaultExceptionServiceConfiguration) exceptionServiceConfiguration;
            } else {
                throw new AppbaseRuntimeException("Cannot find " +
                        DefaultExceptionServiceConfiguration.class.getName() +
                        " in " +
                        ConfigurationService.class.getName());
            }
        }

        AppbaseUtil.validate(validationService, defaultExceptionServiceConfiguration);
        defaultExceptionServiceConfigurationRef = new AtomicReference<>(defaultExceptionServiceConfiguration);
        expiredMessagesCleanerScheduledFutureRef = new AtomicReference<>();

        try {
            mediatorService
                    .getMediator()
                    .registerMessageHandlerByMessageType(MessageType.EXCEPTION, this);
        } catch (MediatorException me) {
            throw new AppbaseRuntimeException("Failed to register " +
                    getClass().getName() +
                    " as " +
                    MessageHandler.class.getName(),
                    me);
        }

        exceptionListenersByExceptionTypeNameMap = new ConcurrentHashMap<>();
        exceptionListenersByExceptionTypeNameMapSizeGauge = exceptionListenersByExceptionTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "exceptionListenersByExceptionTypeNameMap", "size", "gauge"),
                        exceptionListenersByExceptionTypeNameMapSizeGauge);

        globalExceptionListenerByTypeNameMap = new ConcurrentHashMap<>();
        globalExceptionListenerByTypeNameMapSizeGauge = globalExceptionListenerByTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "globalExceptionListenerByTypeNameMap", "size", "gauge"),
                        globalExceptionListenerByTypeNameMapSizeGauge);

        exceptionListenerErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "exceptionListener", "error", "counter"));
        errorCounterByExceptionStoreTypeNameMap = new ConcurrentHashMap<>();

        expiredMessagesCleanerTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "expiredMessageCleaner", "timer"));
        expiredMessagesCleanerErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "expiredMessageCleaner", "error", "counter"));
        this.configurationAddedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationAdded", "counter"));
        this.configurationChangedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationChanged", "counter"));
        this.configurationRemovedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationRemoved", "counter"));

        this.expiredMessagesCleanerLock = new ReentrantLock();
        scheduleOldMessagesCleaner();
        statusRef.set(ServiceStatus.STARTED);
        configurationService.registerConfigurationListenerByKey(
                this, ConfigurationKey.fromTypeKey(DefaultExceptionServiceConfiguration.class));
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onMessage(final Message message) throws MessageHandlerException {
        AppbaseUtil.notNull(message, "message");

        logger.info("Got message: {}", message);
        final ExceptionMessage exceptionMessage = message.getExceptionMessage();
        if (exceptionMessage == null) {
            throw new MessageHandlerException("Exception message must not be null");
        }
        final Map<String, CompletableFuture<Void>> resultFutures =
                exceptionAccessor.asyncWriteExceptionMessage(exceptionMessage);
        resultFutures.forEach((storeTypeName, resultFuture) -> {
            resultFuture
                    .whenCompleteAsync((result, exception) -> {
                        if (exception != null) {
                            logger.error("Error writing to exception store={}", storeTypeName, exception);
                            errorCounterByExceptionStoreTypeNameMap
                                    .computeIfAbsent(storeTypeName, f ->
                                            metricService.getMetricRegistry().counter(metricService.name(
                                                    getClass(), storeTypeName, "error", "counter")))
                                    .inc();
                        }
                    });
        });

        final List<ExceptionListener> exceptionListeners =
                exceptionListenersByExceptionTypeNameMap.get(exceptionMessage.getThrowable().getClass().getName());
        if (exceptionListeners != null) {
            exceptionListeners.forEach(exceptionListener -> {
                try {
                    exceptionListener.onException(exceptionMessage);
                } catch (Exception ex) {
                    logger.error("Error in listener {}, while handling message {}",
                            exceptionListener.getClass().getName(), exceptionMessage);
                    exceptionListenerErrorCounter.inc();
                }
            });
        }

        globalExceptionListenerByTypeNameMap.values().forEach(exceptionListener -> {
            try {
                exceptionListener.onException(exceptionMessage);
            } catch (Exception ex) {
                logger.error("Error in listener {}, while handling message {}",
                        exceptionListener.getClass().getName(), exceptionMessage);
                exceptionListenerErrorCounter.inc();
            }
        });
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

    private void reloadOnEvent(
            final ConfigurationKey configurationKey,
            final ConfigurationValue<?> configurationValue)
            throws ServiceReloadException, ConfigurationListenerException
    {
        verifyConfigurationKey(configurationKey);
        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                (DefaultExceptionServiceConfiguration) configurationValue.getValue();
        reload(defaultExceptionServiceConfiguration);
    }

    private void verifyConfigurationKey(
            final ConfigurationKey configurationKey) throws ConfigurationListenerException
    {
        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        if (typeKey == null || !typeKey.isAssignableFrom(DefaultExceptionServiceConfiguration.class)) {
            throw new ConfigurationListenerException(
                    "Configuration type key is not of type " + DefaultExceptionServiceConfiguration.class.getName());
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

    @Nullable
    @Unmodifiable
    @Override
    public Map<String, Collection<ExceptionListener>> getExceptionListenersByExceptionTypeMap() {
        return Collections.unmodifiableMap(exceptionListenersByExceptionTypeNameMap);
    }

    @Override
    public void registerExceptionListenerByExceptionType(
            final Class<? extends Throwable> exceptionType, final ExceptionListener exceptionListener)
    {
        AppbaseUtil.notNull(exceptionType, "exceptionType");
        AppbaseUtil.notNull(exceptionListener, "exceptionListener");

        logger.info("Registering exceptionType listener {} for exceptionType type {}",
                exceptionListener.getClass().getName(), exceptionType);
        exceptionListenersByExceptionTypeNameMap
                .computeIfAbsent(exceptionType.getName(), f -> new CopyOnWriteArrayList<>())
                .add(exceptionListener);
    }

    @Override
    public void unregisterExceptionListenerByExceptionType(
            final Class<? extends Throwable> exceptionType,
            final Class<? extends ExceptionListener> exceptionListenerType)
    {
        AppbaseUtil.notNull(exceptionType, "exceptionType");
        AppbaseUtil.notNull(exceptionListenerType, "exceptionListenerType");

        logger.info("Unregistering exception listener [{}] for exception type [{}]",
                exceptionListenerType, exceptionType);
        final List<ExceptionListener> exceptionListeners =
                exceptionListenersByExceptionTypeNameMap.get(exceptionType.getName());
        if (exceptionListeners != null) {
            for (final ExceptionListener exceptionListener : exceptionListeners) {
                if (exceptionListener != null && exceptionListener.getClass().equals(exceptionListenerType)) {
                    exceptionListeners.remove(exceptionListener);
                }
            }

            if (exceptionListeners.isEmpty()) {
                exceptionListenersByExceptionTypeNameMap.remove(exceptionType.getName());
            }
        }
    }

    @Override
    public void clearExceptionListenersByExceptionTypeMap() {
        logger.info("Clearing exceptionListenersByExceptionTypeNameMap");
        exceptionListenersByExceptionTypeNameMap.clear();
    }

    @Unmodifiable
    @Override
    public Collection<ExceptionListener> getGlobalExceptionListeners() {
        return Collections.unmodifiableCollection(globalExceptionListenerByTypeNameMap.values());
    }

    @Override
    public void registerGlobalExceptionListener(final ExceptionListener exceptionListener) {
        AppbaseUtil.notNull(exceptionListener, "exceptionListener");

        logger.info("Registering global exception listener {}", exceptionListener.getClass().getName());
        globalExceptionListenerByTypeNameMap.put(exceptionListener.getClass().getName(), exceptionListener);
    }

    @Override
    public void unregisterGlobalExceptionListener(final Class<? extends ExceptionListener> exceptionListenerType) {
        AppbaseUtil.notNull(exceptionListenerType, "exceptionListenerType");

        final String exceptionListenerTypeName = exceptionListenerType.getName();
        logger.info("Unregistering global exception listener {}", exceptionListenerTypeName);
        globalExceptionListenerByTypeNameMap.remove(exceptionListenerTypeName);
    }

    @Override
    public void clearGlobalExceptionListeners() {
        logger.info("Clearing globalExceptionListenerByTypeNameMap");
        globalExceptionListenerByTypeNameMap.clear();
    }

    @Override
    public ExceptionAccessor getExceptionAccessor() {
        return exceptionAccessor;
    }

    @Override
    protected <C extends AppbaseConfiguration> void reloadImpl(
            final C configuration) throws ServiceReloadException
    {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultExceptionServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultExceptionServiceConfiguration.class.getName());
        }

        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                (DefaultExceptionServiceConfiguration) configuration;
        AppbaseUtil.validate(validationService, defaultExceptionServiceConfiguration);

        try {
            exceptionAccessor.reload(defaultExceptionServiceConfiguration.getDefaultExceptionAccessorConfiguration());
        } catch (ExceptionAccessorException eae) {
            throw new ServiceReloadException("Error while reloading {}" + exceptionAccessor.getClass().getName(), eae);
        }

        scheduleOldMessagesCleaner();
        this.defaultExceptionServiceConfigurationRef.set(defaultExceptionServiceConfiguration);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void scheduleOldMessagesCleaner() {
        final DefaultExceptionServiceConfiguration defaultExceptionServiceConfiguration =
                defaultExceptionServiceConfigurationRef.get();
        final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration =
                defaultExceptionServiceConfiguration.getDefaultExpiredMessageCleanerConfiguration();
        AppbaseUtil.validate(validationService, defaultExpiredMessageCleanerConfiguration);

        final ScheduledFuture<Void> currentScheduledFuture = expiredMessagesCleanerScheduledFutureRef.get();
        if (currentScheduledFuture != null) {
            currentScheduledFuture.cancel(true);
        }

        if (defaultExpiredMessageCleanerConfiguration.isEnabled()) {
            final Timer.Context context = expiredMessagesCleanerTimer.time();
            expiredMessagesCleanerLock.lock();

            try {
                final ExpiredMessageCleaner expiredMessageCleaner =
                        new DefaultExpiredMessageCleaner(
                                logService,
                                defaultExpiredMessageCleanerConfiguration.getExclusiveRemoveBeforeTimePeriod(),
                                defaultExpiredMessageCleanerConfiguration.getExclusiveRemoveBeforeTimePeriodUnit(),
                                exceptionAccessor,
                                mediatorService);
                final SchedulableConfiguration schedulableConfiguration =
                        defaultExpiredMessageCleanerConfiguration.getSchedulableConfiguration();
                final ExpiredMessageCleanerSchedulable expiredMessageCleanerSchedulable =
                        new ExpiredMessageCleanerSchedulable(expiredMessageCleaner, schedulableConfiguration);
                final ScheduledFuture<Void> scheduledFuture =
                        mediatorService.getMediator().scheduleWithFixedDelay(
                                expiredMessageCleanerSchedulable,
                                schedulableConfiguration.getInitialDelay(),
                                schedulableConfiguration.getInterval(),
                                schedulableConfiguration.getTimeUnit());
                expiredMessagesCleanerScheduledFutureRef.set(scheduledFuture);
            } catch (Exception ex) {
                final String exceptionMessage = "Error cleaning expired message";
                final Message message = MessageFactory.exceptionMessage(
                        mediatorService,
                        getClass(),
                        exceptionMessage,
                        AppbaseUtil.threadName(),
                        ex,
                        null);
                try {
                    mediatorService.getMediator().asyncHandleMessage(message, true);
                } catch (MediatorException me) {
                    logger.error("Error sending message to mediator: {}", message, me);
                }
            } finally {
                expiredMessagesCleanerLock.unlock();
                context.stop();
                expiredMessagesCleanerErrorCounter.inc();
            }
        }
    }

    @Override
    protected void shutdownImpl() {
        final ScheduledFuture<Void> currentScheduledFuture = expiredMessagesCleanerScheduledFutureRef.get();
        if (currentScheduledFuture != null) {
            currentScheduledFuture.cancel(true);
        }

        exceptionAccessor.shutdown();
        clearGlobalExceptionListeners();
        clearExceptionListenersByExceptionTypeMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultExceptionServiceConfiguration getConfiguration() {
        return defaultExceptionServiceConfigurationRef.get();
    }

    public Gauge getExceptionListenersByExceptionTypeNameMapSizeGauge() {
        return exceptionListenersByExceptionTypeNameMapSizeGauge;
    }

    public Gauge getGlobalExceptionListenerByTypeNameMapSizeGauge() {
        return globalExceptionListenerByTypeNameMapSizeGauge;
    }

    private static final class ExpiredMessageCleanerSchedulable implements Schedulable<Void> {
        private final ExpiredMessageCleaner expiredMessageCleaner;
        private final SchedulableConfiguration schedulableConfiguration;

        private ExpiredMessageCleanerSchedulable(
                final ExpiredMessageCleaner expiredMessageCleaner,
                final SchedulableConfiguration schedulableConfiguration)
        {
            AppbaseUtil.notNull(expiredMessageCleaner, "expiredMessageCleaner");
            AppbaseUtil.notNull(schedulableConfiguration, "schedulableConfiguration");

            this.expiredMessageCleaner = expiredMessageCleaner;
            this.schedulableConfiguration = schedulableConfiguration;
        }

        @Override
        public Void schedule() {
            expiredMessageCleaner.clean();
            return null;
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            return schedulableConfiguration;
        }
    }
}
