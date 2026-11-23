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

package com.viveworks.appbase.client.core.pool.manager;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.AppbaseClientPoolRegistry;
import com.viveworks.appbase.client.core.pool.DefaultAppbaseClientPool;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceFactory;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import com.viveworks.appbase.core.util.objectpool.Poolable;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link AppbaseClientPoolManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAppbaseClientPoolManager implements AppbaseClientPoolManager, AppbaseClientPoolManagerRegistry {
    private final Logger logger;
    private final AppbaseClientCore appbaseClientCore;
    private final DefaultAppbaseClientPoolManagerConfiguration defaultAppbaseClientPoolManagerConfiguration;


    private final List<AppbaseClientPool<?, ? extends ComparableValue>> appbaseClientPools;
    private final Queue<UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>>
            unhealthyClientIntegrationServices;
    private final ManyToManyQueuedFixedObjectPool<RepairListenerImpl> repairListenerObjectPool;
    private final ManyToManyQueuedFixedObjectPool<UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>>
            unhealthyClientIntegrationServiceEntryObjectPool;


    private final Map<AppbaseClientKey<? extends ComparableValue>, Boolean> repairDisabledAppbaseClientKeys;
    private final Map<String, Boolean> repairDisabledAppbaseClientIntegrationServiceTypeNames;
    private final Map<AppbaseClientKey<? extends ComparableValue>, Integer> maxRepairAttemptsByAppbaseClientKeyMap;
    private final Map<String, Integer> maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap;
    private final Map<AppbaseClientKey<? extends ComparableValue>, Boolean>
            disableUnregisterUnhealthyIrreparableByAppbaseClientKeyMap;
    private final Map<String, Boolean> disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap;


    private final ScheduledExecutorService scheduledExecutorService;
    private final ScheduledFuture<Void> clientPoolCheckerTaskScheduledFuture;
    private final ScheduledFuture<Void> clientIntegrationServiceRepairTaskScheduledFuture;


    private final Map<String, Counter>
            unhealthyAppbaseClientIntegrationServiceManagementDisabledCounterBySourcePoolTypeNameMap;
    private final Map<String, Counter> repairDisabledAppbaseClientKeyCounterBySourcePoolTypeNameMap;
    private final Map<String, Counter> repairDisabledAppbaseClientIntegrationServiceTypeNameCounterByTypeNameMap;
    private final Map<String, Counter> globalMaxRepairCountCounterByAppbaseClientIntegrationServiceTypeNameMap;
    private final Map<String, Counter> maxRepairCountAppbaseClientKeyCounterByAppbaseClientIntegrationServiceTypeNameMap;
    private final Map<String, Counter> maxRepairCountAppbaseClientIntegrationServiceTypeNameCounterByTypeNameMap;
    private final Map<String, Counter>
            disableUnregisterUnhealthyIrreparableByAppbaseClientKeyCounterMap;
    private final Map<String, Counter>
            disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameCounterMap;
    private final Map<String, Counter> unhealthyAppbaseClientIntegrationServiceCounterByTypeNameMap;
    private final Map<String, Counter> repairListenerSuccessAppbaseClientIntegrationServiceCounterByTypeNameMap;
    private final Map<String, Counter>
            repairListenerFailureCounterByAppbaseClientIntegrationServiceTypeNameCounterMap;
    private final Map<String, Counter>
            repairListenerFailureExceptionCounterByAppbaseClientIntegrationServiceTypeNameCounterMap;


    private final Lock appbaseClientPoolsWriteLock;
    private final Lock unhealthyClientIntegrationServicesLifecycleLock;
    private final AtomicBoolean shutdown;



    @Inject
    public DefaultAppbaseClientPoolManager(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAppbaseClientPoolManagerConfiguration defaultAppbaseClientPoolManagerConfiguration)
    {
        this(defaultAppbaseClientPoolManagerConfiguration, appbaseClientCore, false, null);
    }

    public DefaultAppbaseClientPoolManager(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAppbaseClientPoolManagerConfiguration defaultAppbaseClientPoolManagerConfiguration,
            final List<AppbaseClientPool<?, ? extends ComparableValue>> appbaseClientPools)
    {
        this(defaultAppbaseClientPoolManagerConfiguration, appbaseClientCore, true, appbaseClientPools);
    }

    @SuppressWarnings("unchecked")
    private DefaultAppbaseClientPoolManager(
            final DefaultAppbaseClientPoolManagerConfiguration defaultAppbaseClientPoolManagerConfiguration,
            final AppbaseClientCore appbaseClientCore,
            final boolean expectNotNullAppbaseClientPools,
            @Nullable final List<AppbaseClientPool<?, ? extends ComparableValue>> appbaseClientPools)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(defaultAppbaseClientPoolManagerConfiguration, "defaultAppbaseClientPoolManagerConfiguration");

        if (expectNotNullAppbaseClientPools) {
            AppbaseUtil.notNull(appbaseClientPools, "appbaseClientPools");
        }

        this.logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.appbaseClientCore = appbaseClientCore;
        this.defaultAppbaseClientPoolManagerConfiguration = defaultAppbaseClientPoolManagerConfiguration;


        this.appbaseClientPools = new CopyOnWriteArrayList<>();
        this.repairListenerObjectPool = AppbaseUtil.manyToManyQueuedFixedObjectPool(
                appbaseClientCore.getAppbaseCore().getMetricService(),
                defaultAppbaseClientPoolManagerConfiguration.getRepairListenerImplObjectPoolConfiguration(),
                new MutableRepairListenerImplFactory());
        this.unhealthyClientIntegrationServiceEntryObjectPool = AppbaseUtil.manyToManyQueuedFixedObjectPool(
                appbaseClientCore.getAppbaseCore().getMetricService(),
                defaultAppbaseClientPoolManagerConfiguration
                        .getUnhealthyClientIntegrationServiceEntryObjectPoolConfiguration(),
                new UnhealthyClientIntegrationServiceEntryFactory());
        this.unhealthyClientIntegrationServices = new ManyToOneConcurrentLinkedQueue<>();


        this.repairDisabledAppbaseClientKeys = new ConcurrentHashMap<>();
        this.repairDisabledAppbaseClientIntegrationServiceTypeNames = new ConcurrentHashMap<>();
        this.maxRepairAttemptsByAppbaseClientKeyMap = new ConcurrentHashMap<>();
        this.maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap = new ConcurrentHashMap<>();
        this.disableUnregisterUnhealthyIrreparableByAppbaseClientKeyMap = new ConcurrentHashMap<>();
        this.disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap = new ConcurrentHashMap<>();
        this.repairDisabledAppbaseClientKeyCounterBySourcePoolTypeNameMap = new ConcurrentHashMap<>();
        this.unhealthyAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
        this.maxRepairCountAppbaseClientKeyCounterByAppbaseClientIntegrationServiceTypeNameMap = new ConcurrentHashMap<>();
        this.globalMaxRepairCountCounterByAppbaseClientIntegrationServiceTypeNameMap = new ConcurrentHashMap<>();


        this.maxRepairCountAppbaseClientIntegrationServiceTypeNameCounterByTypeNameMap = new ConcurrentHashMap<>();
        this.repairDisabledAppbaseClientIntegrationServiceTypeNameCounterByTypeNameMap = new ConcurrentHashMap<>();
        this.unhealthyAppbaseClientIntegrationServiceManagementDisabledCounterBySourcePoolTypeNameMap =
                new ConcurrentHashMap<>();
        this.disableUnregisterUnhealthyIrreparableByAppbaseClientKeyCounterMap = new ConcurrentHashMap<>();
        this.disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameCounterMap =
                new ConcurrentHashMap<>();
        this.repairListenerSuccessAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
        this.repairListenerFailureCounterByAppbaseClientIntegrationServiceTypeNameCounterMap =
                new ConcurrentHashMap<>();
        this.repairListenerFailureExceptionCounterByAppbaseClientIntegrationServiceTypeNameCounterMap =
                new ConcurrentHashMap<>();

        this.unhealthyClientIntegrationServicesLifecycleLock = new ReentrantLock();

        final AppbaseClientPoolCheckerTask appbaseClientPoolCheckerTask;
        final AppbaseClientIntegrationServiceRepairTask appbaseClientIntegrationServiceRepairTask;
        if (defaultAppbaseClientPoolManagerConfiguration.isUnhealthyAppbaseClientIntegrationServiceManagementDisabled()) {
            this.scheduledExecutorService = null;
            this.clientPoolCheckerTaskScheduledFuture = null;
            this.clientIntegrationServiceRepairTaskScheduledFuture = null;
        } else {
            if (appbaseClientPools != null) {
                appbaseClientPools.forEach(this::registerAppbaseClientPool);
            }

            final Set<AppbaseClientKeyConfiguration<?>>
                    repairDisabledAppbaseClientKeyConfigurations =
                    defaultAppbaseClientPoolManagerConfiguration.getRepairDisabledAppbaseClientKeyConfigurations();
            if (repairDisabledAppbaseClientKeyConfigurations != null) {
                repairDisabledAppbaseClientKeyConfigurations.forEach(appbaseClientKeyConfiguration -> {
                    final AppbaseClientKey<?> appbaseClientKey =
                            AppbaseClientUtil.appbaseClientKeyFromPool(
                                    appbaseClientCore, appbaseClientKeyConfiguration);
                    repairDisabledAppbaseClientKeys.put(appbaseClientKey, true);
                });
            }

            final Set<String> configuredRepairDisabledAppbaseClientIntegrationServiceTypeNames =
                    defaultAppbaseClientPoolManagerConfiguration.getRepairDisabledAppbaseClientIntegrationServiceTypeNames();
            if (configuredRepairDisabledAppbaseClientIntegrationServiceTypeNames != null) {
                configuredRepairDisabledAppbaseClientIntegrationServiceTypeNames.forEach(
                        repairDisabledAppbaseClientIntegrationServiceTypeName ->
                                this.repairDisabledAppbaseClientIntegrationServiceTypeNames.put(
                                        repairDisabledAppbaseClientIntegrationServiceTypeName, true));
            }

            final Set<MaxRepairAttempsConfiguration> maxRepairAttemptsByAppbaseClientKeyConfigurations =
                    defaultAppbaseClientPoolManagerConfiguration.getMaxRepairAttemptsByAppbaseClientKeyConfigurations();
            if (maxRepairAttemptsByAppbaseClientKeyConfigurations != null) {
                maxRepairAttemptsByAppbaseClientKeyConfigurations.forEach(
                        maxRepairAttempsConfiguration -> {
                            final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration =
                                    maxRepairAttempsConfiguration.getAppbaseClientKeyConfiguration();
                            final int count = maxRepairAttempsConfiguration.getCount();
                            final AppbaseClientKey<?> appbaseClientKey =
                                    AppbaseClientUtil.appbaseClientKeyFromPool(
                                            appbaseClientCore, appbaseClientKeyConfiguration);
                            maxRepairAttemptsByAppbaseClientKeyMap.put(appbaseClientKey, count);
                        });
            }

            final Map<String, Integer> configuredMaxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap =
                    defaultAppbaseClientPoolManagerConfiguration
                            .getMaxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap();
            if (configuredMaxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap != null) {
                this.maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap
                        .putAll(configuredMaxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap);
            }

            final Set<DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration>
                    disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations =
                    defaultAppbaseClientPoolManagerConfiguration
                            .getDisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations();
            if (disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations != null) {
                disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations
                        .forEach(disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration -> {
                            final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration =
                                    disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration
                                            .getAppbaseClientKeyConfiguration();
                            final boolean disable =
                                    disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.isDisable();
                            final AppbaseClientKey<?> appbaseClientKey =
                                    AppbaseClientUtil.appbaseClientKeyFromPool(
                                            appbaseClientCore, appbaseClientKeyConfiguration);
                            this.disableUnregisterUnhealthyIrreparableByAppbaseClientKeyMap
                                    .put(appbaseClientKey, disable);
                        });
            }

            final Map<String, Boolean>
                    configuredDisableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap =
                    defaultAppbaseClientPoolManagerConfiguration
                            .getDisableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap();
            if (configuredDisableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap != null) {
                this.disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap
                        .putAll(configuredDisableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap);

            }

            final SchedulableConfiguration clientPoolCheckerTaskSchedulableConfiguration =
                    defaultAppbaseClientPoolManagerConfiguration
                            .getClientPoolCheckerTaskSchedulableConfiguration();
            appbaseClientPoolCheckerTask =
                    new AppbaseClientPoolCheckerTask(clientPoolCheckerTaskSchedulableConfiguration);

            final SchedulableConfiguration clientIntegrationServiceRepairTaskSchedulableConfiguration =
                    defaultAppbaseClientPoolManagerConfiguration
                            .getClientIntegrationServiceRepairTaskSchedulableConfiguration();
            appbaseClientIntegrationServiceRepairTask =
                    new AppbaseClientIntegrationServiceRepairTask(
                            clientIntegrationServiceRepairTaskSchedulableConfiguration);

            if (defaultAppbaseClientPoolManagerConfiguration.isUseMediatorSharedScheduler()) {
                this.scheduledExecutorService = null;

                final Mediator mediator = appbaseClientCore
                        .getAppbaseCore()
                        .getMediatorService()
                        .getMediator();
                this.clientPoolCheckerTaskScheduledFuture = mediator.scheduleWithFixedDelay(
                        appbaseClientPoolCheckerTask,
                        clientPoolCheckerTaskSchedulableConfiguration.getInitialDelay(),
                        clientPoolCheckerTaskSchedulableConfiguration.getInterval(),
                        clientPoolCheckerTaskSchedulableConfiguration.getTimeUnit());
                this.clientIntegrationServiceRepairTaskScheduledFuture = mediator.scheduleWithFixedDelay(
                        appbaseClientIntegrationServiceRepairTask,
                        clientIntegrationServiceRepairTaskSchedulableConfiguration.getInitialDelay(),
                        clientIntegrationServiceRepairTaskSchedulableConfiguration.getInterval(),
                        clientIntegrationServiceRepairTaskSchedulableConfiguration.getTimeUnit());
            } else {
                final ScheduledExecutorServiceConfiguration customScheduledExecutorServiceConfiguration =
                        defaultAppbaseClientPoolManagerConfiguration.getCustomScheduledExecutorServiceConfiguration();
                AppbaseUtil.notNull(
                        customScheduledExecutorServiceConfiguration, "customScheduledExecutorServiceConfiguration");

                final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) ->
                        logger.error("Unexpected exception in " + thread.getName() + ", ", throwable);

                this.scheduledExecutorService = ScheduledExecutorServiceFactory.newScheduledExecutorService(
                        customScheduledExecutorServiceConfiguration, uncaughtExceptionHandler);
                this.clientPoolCheckerTaskScheduledFuture = (ScheduledFuture<Void>)
                        scheduledExecutorService.scheduleWithFixedDelay(
                                appbaseClientPoolCheckerTask::schedule,
                                clientPoolCheckerTaskSchedulableConfiguration.getInitialDelay(),
                                clientPoolCheckerTaskSchedulableConfiguration.getInterval(),
                                clientPoolCheckerTaskSchedulableConfiguration.getTimeUnit());
                this.clientIntegrationServiceRepairTaskScheduledFuture = (ScheduledFuture<Void>)
                        scheduledExecutorService.scheduleWithFixedDelay(
                                appbaseClientIntegrationServiceRepairTask::schedule,
                                clientIntegrationServiceRepairTaskSchedulableConfiguration.getInitialDelay(),
                                clientIntegrationServiceRepairTaskSchedulableConfiguration.getInterval(),
                                clientIntegrationServiceRepairTaskSchedulableConfiguration.getTimeUnit());
            }
        }

        this.appbaseClientPoolsWriteLock = new ReentrantLock();
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, V extends ComparableValue> void handleUnhealthyAppbaseClientIntegrationService(
            final AppbaseClientPool<T, V> sourceAppbaseClientPool,
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(sourceAppbaseClientPool, "sourceAppbaseClientPool");
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        unhealthyClientIntegrationServicesLifecycleLock.lock();
        try {
            if (shutdown.get()) {
                return;
            }
            final UnhealthyClientIntegrationServiceEntry<T, V> unhealthyClientIntegrationServiceEntry =
                    (UnhealthyClientIntegrationServiceEntry<T, V>)
                            unhealthyClientIntegrationServiceEntryObjectPool.pollCleanObject();
            unhealthyClientIntegrationServiceEntry.init(
                    sourceAppbaseClientPool,
                    appbaseClientIntegrationService);
            enqueueUnhealthyClientWhileLocked(unhealthyClientIntegrationServiceEntry);
        } finally {
            unhealthyClientIntegrationServicesLifecycleLock.unlock();
        }
    }

    @Override
    public AppbaseClientPoolManagerRegistry getAppbaseClientPoolManagerRegistry() {
        return this;
    }

    @Override
    @Unmodifiable
    public Collection<AppbaseClientPool<?, ? extends ComparableValue>> getAppbaseClientPools() {
        return appbaseClientPools;
    }

    @Override
    public void registerAppbaseClientPool(final AppbaseClientPool<?, ? extends ComparableValue> appbaseClientPool) {
        AppbaseUtil.notNull(appbaseClientPool, "appbaseClientPool");

        logger.info("Registering {}", appbaseClientPool.getClass().getName());

        appbaseClientPoolsWriteLock.lock();
        try {
            appbaseClientPools.add(appbaseClientPool);
        } finally {
            appbaseClientPoolsWriteLock.unlock();
        }
    }

    @Override
    public void unregisterAppbaseClientPool(final AppbaseClientPool<?, ? extends ComparableValue> appbaseClientPool) {
        AppbaseUtil.notNull(appbaseClientPool, "appbaseClientPool");

        logger.info("Unregistering {}", appbaseClientPool);

        appbaseClientPoolsWriteLock.lock();
        try {
            int index = 0;
            for (final AppbaseClientPool<?, ? extends ComparableValue> registeredAppbaseClientPool : appbaseClientPools) {
                if (registeredAppbaseClientPool.equals(appbaseClientPool)) {
                    appbaseClientPools.remove(index);
                }
                ++index;
            }
        } finally {
            appbaseClientPoolsWriteLock.unlock();
        }
    }

    @Override
    public void unregisterAppbaseClientPool(
            final Class<AppbaseClientPool<?, ? extends ComparableValue>> appbaseClientPoolType)
    {
        AppbaseUtil.notNull(appbaseClientPoolType, "appbaseClientPoolType");

        logger.info("Unregistering {}", appbaseClientPoolType);

        appbaseClientPoolsWriteLock.lock();
        try {
            int index = 0;
            for (final AppbaseClientPool<?, ? extends ComparableValue> registeredAppbaseClientPool : appbaseClientPools) {
                if (registeredAppbaseClientPool.getClass().equals(appbaseClientPoolType)) {
                    appbaseClientPools.remove(index);
                }
                ++index;
            }
        } finally {
            appbaseClientPoolsWriteLock.unlock();
        }
    }

    @Override
    public void clearAllAppbaseClientPools() {
        logger.info("Clearing all {}", AppbaseClientPool.class.getName());

        appbaseClientPoolsWriteLock.lock();
        try {
            appbaseClientPools.clear();
        } finally {
            appbaseClientPoolsWriteLock.unlock();
        }
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        unhealthyClientIntegrationServicesLifecycleLock.lock();
        try {
            if (!shutdown.compareAndSet(false, true)) {
                return;
            }
        } finally {
            unhealthyClientIntegrationServicesLifecycleLock.unlock();
        }

        logger.info("Shutting down {}", getClass().getName());

        if (clientPoolCheckerTaskScheduledFuture != null) {
            clientPoolCheckerTaskScheduledFuture.cancel(true);
        }
        if (clientIntegrationServiceRepairTaskScheduledFuture != null) {
            clientIntegrationServiceRepairTaskScheduledFuture.cancel(true);
        }
        if (scheduledExecutorService != null) {
            AppbaseUtil.shutdownExecutor(scheduledExecutorService);
        }

        clearAllAppbaseClientPools();
        unhealthyClientIntegrationServicesLifecycleLock.lock();
        try {
            // Agrona's ManyToOneConcurrentLinkedQueue deliberately does not support clear().
            UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue> unhealthyEntry;
            while ((unhealthyEntry = unhealthyClientIntegrationServices.poll()) != null) {
                unhealthyClientIntegrationServiceEntryObjectPool.offerCleanObject(unhealthyEntry);
            }
            repairListenerObjectPool.shutdown();
            unhealthyClientIntegrationServiceEntryObjectPool.shutdown();
        } finally {
            unhealthyClientIntegrationServicesLifecycleLock.unlock();
        }
        repairDisabledAppbaseClientKeys.clear();
        repairDisabledAppbaseClientIntegrationServiceTypeNames.clear();
        maxRepairAttemptsByAppbaseClientKeyMap.clear();
        maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap.clear();
        disableUnregisterUnhealthyIrreparableByAppbaseClientKeyMap.clear();
        disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap.clear();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    private <T, V extends ComparableValue> void enqueueUnhealthyClient(
            final UnhealthyClientIntegrationServiceEntry<T, V> unhealthyClientIntegrationServiceEntry)
    {
        AppbaseUtil.notNull(unhealthyClientIntegrationServiceEntry, "unhealthyClientIntegrationServiceEntry");

        unhealthyClientIntegrationServicesLifecycleLock.lock();
        try {
            enqueueUnhealthyClientWhileLocked(unhealthyClientIntegrationServiceEntry);
        } finally {
            unhealthyClientIntegrationServicesLifecycleLock.unlock();
        }
    }

    private <T, V extends ComparableValue> boolean enqueueUnhealthyClientWhileLocked(
            final UnhealthyClientIntegrationServiceEntry<T, V> unhealthyClientIntegrationServiceEntry)
    {
        if (shutdown.get()) {
            unhealthyClientIntegrationServiceEntry.clean();
            return false;
        }

        final AppbaseClientPool<T, V> sourceAppbaseClientPool =
                unhealthyClientIntegrationServiceEntry.getSourceAppbaseClientPool();
        final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService =
                unhealthyClientIntegrationServiceEntry.getAppbaseClientIntegrationService();

        AppbaseUtil.notNull(sourceAppbaseClientPool, "sourceAppbaseClientPool");
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        if (defaultAppbaseClientPoolManagerConfiguration.isUnhealthyAppbaseClientIntegrationServiceManagementDisabled()) {
            unhealthyAppbaseClientIntegrationServiceManagementDisabledCounterBySourcePoolTypeNameMap
                    .computeIfAbsent(sourceAppbaseClientPool.getClass().getName(), f -> {
                        final MetricService metricService = appbaseClientCore
                                .getAppbaseCore()
                                .getMetricService();
                        final String name = metricService.name(
                                sourceAppbaseClientPool.getClass(),
                                "unhealthyAppbaseClientIntegrationServiceManagementDisabled",
                                "counter");
                        return metricService
                                .getMetricRegistry()
                                .counter(name);
                    })
                    .inc();
            unhealthyClientIntegrationServiceEntryObjectPool.offerCleanObject(unhealthyClientIntegrationServiceEntry);
            return true;
        }

        final AppbaseClientKey<? extends ComparableValue> appbaseClientKey =
                appbaseClientIntegrationService.getAppbaseClientKey();
        if (repairDisabledAppbaseClientKeys != null && repairDisabledAppbaseClientKeys.containsKey(appbaseClientKey)) {
            repairDisabledAppbaseClientKeyCounterBySourcePoolTypeNameMap
                    .computeIfAbsent(sourceAppbaseClientPool.getClass().getName(), f -> {
                        final MetricService metricService = appbaseClientCore
                                .getAppbaseCore()
                                .getMetricService();
                        final String name = metricService.name(
                                sourceAppbaseClientPool.getClass(),
                                "repairDisabledAppbaseClientKey",
                                "counter");
                        return metricService
                                .getMetricRegistry()
                                .counter(name);
                    })
                    .inc();
            unhealthyClientIntegrationServiceEntryObjectPool.offerCleanObject(unhealthyClientIntegrationServiceEntry);
            return true;
        }

        if (repairDisabledAppbaseClientIntegrationServiceTypeNames != null &&
                repairDisabledAppbaseClientIntegrationServiceTypeNames.containsKey(
                        appbaseClientIntegrationService.getClass().getName()))
        {
            repairDisabledAppbaseClientIntegrationServiceTypeNameCounterByTypeNameMap
                    .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                        final MetricService metricService = appbaseClientCore
                                .getAppbaseCore()
                                .getMetricService();
                        final String name = metricService.name(
                                appbaseClientIntegrationService.getClass(),
                                "repairDisabledAppbaseClientIntegrationServiceType",
                                "counter");
                        return metricService
                                .getMetricRegistry()
                                .counter(name);
                    })
                    .inc();
            unhealthyClientIntegrationServiceEntryObjectPool.offerCleanObject(unhealthyClientIntegrationServiceEntry);
            return true;
        }

        unhealthyAppbaseClientIntegrationServiceCounterByTypeNameMap
                .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                    final MetricService metricService = appbaseClientCore
                            .getAppbaseCore()
                            .getMetricService();
                    final String name = metricService.name(
                            appbaseClientIntegrationService.getClass(),
                            "unhealthyAppbaseClientIntegrationService",
                            "counter");
                    return metricService
                            .getMetricRegistry()
                            .counter(name);
                })
                .inc();
        unhealthyClientIntegrationServices.add(unhealthyClientIntegrationServiceEntry);
        return true;
    }

    private <T, V extends ComparableValue> void irreparableAppbaseClientIntegrationService(
            final UnhealthyClientIntegrationServiceEntry<T, V> unhealthyClientIntegrationServiceEntry)
    {
        AppbaseUtil.notNull(unhealthyClientIntegrationServiceEntry, "unhealthyClientIntegrationServiceEntry");

        final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService =
                unhealthyClientIntegrationServiceEntry.getAppbaseClientIntegrationService();

        final Boolean disableUnregisterUnhealthyIrreparableByAppbaseClientKey =
                disableUnregisterUnhealthyIrreparableByAppbaseClientKeyMap
                        .get(appbaseClientIntegrationService.getAppbaseClientKey());
        if (disableUnregisterUnhealthyIrreparableByAppbaseClientKey != null &&
                disableUnregisterUnhealthyIrreparableByAppbaseClientKey)
        {
            disableUnregisterUnhealthyIrreparableByAppbaseClientKeyCounterMap
                    .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                        final MetricService metricService = appbaseClientCore
                                .getAppbaseCore()
                                .getMetricService();
                        final String name = metricService.name(
                                appbaseClientIntegrationService.getClass(),
                                "disableUnregisterUnhealthyIrreparableByAppbaseClientKey",
                                "counter");
                        return metricService
                                .getMetricRegistry()
                                .counter(name);
                    })
                    .inc();
            return;
        }

        final Boolean disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeName =
                disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap
                        .get(appbaseClientIntegrationService.getClass().getName());
        if (disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeName != null &&
                disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeName)
        {
            disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameCounterMap
                    .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                        final MetricService metricService = appbaseClientCore
                                .getAppbaseCore()
                                .getMetricService();
                        final String name = metricService.name(
                                appbaseClientIntegrationService.getClass(),
                                "disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceType",
                                "counter");
                        return metricService
                                .getMetricRegistry()
                                .counter(name);
                    })
                    .inc();
            return;
        }

        unhealthyClientIntegrationServiceEntry
                .getSourceAppbaseClientPool()
                .getAppbaseClientPoolRegistry()
                .unregisterAppbaseClientIntegrationServices(appbaseClientIntegrationService);
    }

    private <T, V extends ComparableValue> boolean checkMaxRepairAttempts(
            final UnhealthyClientIntegrationServiceEntry<T, V> unhealthyClientIntegrationServiceEntry,
            final boolean useCurrentRepairAttempts)
    {
        AppbaseUtil.notNull(unhealthyClientIntegrationServiceEntry, "unhealthyClientIntegrationServiceEntry");

        final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService =
                unhealthyClientIntegrationServiceEntry.getAppbaseClientIntegrationService();
        if (defaultAppbaseClientPoolManagerConfiguration.isEnableGlobalMaxRepairAttemptsCheck()) {
            final int repairAttempts;
            if (useCurrentRepairAttempts) {
                repairAttempts = unhealthyClientIntegrationServiceEntry.getRepairAtempts() + 1;
            } else {
                repairAttempts = unhealthyClientIntegrationServiceEntry.incrementAndGetRepairAttempts();
            }

            if (repairAttempts > defaultAppbaseClientPoolManagerConfiguration.getGlobalMaxRepairAttempts()) {
                globalMaxRepairCountCounterByAppbaseClientIntegrationServiceTypeNameMap
                        .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                            final MetricService metricService = appbaseClientCore
                                    .getAppbaseCore()
                                    .getMetricService();
                            final String name = metricService.name(
                                    appbaseClientIntegrationService.getClass(),
                                    "globalMaxRepairCount",
                                    "counter");
                            return metricService
                                    .getMetricRegistry()
                                    .counter(name);
                        })
                        .inc();
                irreparableAppbaseClientIntegrationService(unhealthyClientIntegrationServiceEntry);
                return false;
            }

            final Integer maxRepairAttemptsByAppbaseClientKey =
                    maxRepairAttemptsByAppbaseClientKeyMap.get(appbaseClientIntegrationService.getAppbaseClientKey());
            if (maxRepairAttemptsByAppbaseClientKey != null &&
                    repairAttempts > maxRepairAttemptsByAppbaseClientKey)
            {
                maxRepairCountAppbaseClientKeyCounterByAppbaseClientIntegrationServiceTypeNameMap
                        .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                            final MetricService metricService = appbaseClientCore
                                    .getAppbaseCore()
                                    .getMetricService();
                            final String name = metricService.name(
                                    appbaseClientIntegrationService.getClass(),
                                    "maxRepairCountAppbaseClientKey",
                                    "counter");
                            return metricService
                                    .getMetricRegistry()
                                    .counter(name);
                        })
                        .inc();
                irreparableAppbaseClientIntegrationService(unhealthyClientIntegrationServiceEntry);
                return false;
            }

            final Integer maxRepairAttemptsByAppbaseClientIntegrationServiceTypeName =
                    maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap
                            .get(appbaseClientIntegrationService.getClass().getName());
            if (maxRepairAttemptsByAppbaseClientIntegrationServiceTypeName != null &&
                    repairAttempts > maxRepairAttemptsByAppbaseClientIntegrationServiceTypeName)
            {
                maxRepairCountAppbaseClientIntegrationServiceTypeNameCounterByTypeNameMap
                        .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                            final MetricService metricService = appbaseClientCore
                                    .getAppbaseCore()
                                    .getMetricService();
                            final String name = metricService.name(
                                    appbaseClientIntegrationService.getClass(),
                                    "maxRepairCountAppbaseClientIntegrationServiceType",
                                    "counter");
                            return metricService
                                    .getMetricRegistry()
                                    .counter(name);
                        })
                        .inc();
                irreparableAppbaseClientIntegrationService(unhealthyClientIntegrationServiceEntry);
                return false;
            }
        }

        return true;
    }

    /**
     * Used to check for unhealthy {@link AppbaseClientIntegrationService}s in {@link AppbaseClientPool}s.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    private class AppbaseClientPoolCheckerTask implements Schedulable<Void> {
        private final SchedulableConfiguration schedulableConfiguration;

        AppbaseClientPoolCheckerTask(final SchedulableConfiguration schedulableConfiguration) {
            AppbaseUtil.notNull(schedulableConfiguration, "schedulableConfiguration");

            this.schedulableConfiguration = schedulableConfiguration;
        }

        @Override
        public Void schedule() {
            unhealthyClientIntegrationServicesLifecycleLock.lock();
            try {
                if (shutdown.get()) {
                    return null;
                }
                return scheduleWhileLocked();
            } finally {
                unhealthyClientIntegrationServicesLifecycleLock.unlock();
            }
        }

        private Void scheduleWhileLocked() {
            appbaseClientPools.forEach(appbaseClientPool -> {
                if (DefaultAppbaseClientPool.class.isAssignableFrom(appbaseClientPool.getClass())) {
                    final DefaultAppbaseClientPool<?, ? extends ComparableValue> defaultAppbaseClientPool =
                            (DefaultAppbaseClientPool<?, ? extends ComparableValue>) appbaseClientPool;
                    if (defaultAppbaseClientPool.getAppbaseClientIntegrationServiceQueueByKeyMap() != null) {
                        defaultAppbaseClientPool.getAppbaseClientIntegrationServiceQueueByKeyMap()
                                .forEach((appbaseClientKey, appbaseClientIntegrationServices) ->
                                        appbaseClientIntegrationServices.forEach(
                                                appbaseClientIntegrationService -> {
                                                    if (appbaseClientIntegrationService.isRepairEnabled() &&
                                                            !appbaseClientIntegrationService.isHealthy())
                                                    {
                                                        unhealthyAppbaseClientIntegrationService(
                                                                appbaseClientPool, appbaseClientIntegrationService);
                                                    }
                                        }));
                    }
                } else {
                    final AppbaseClientPoolRegistry<?, ? extends ComparableValue> appbaseClientPoolRegistry =
                            appbaseClientPool.getAppbaseClientPoolRegistry();
                    appbaseClientPoolRegistry
                            .getAppbaseClientIntegrationServicesByKeyMap()
                            .forEach((appbaseClientKey, appbaseClientIntegrationServices) ->
                                    appbaseClientIntegrationServices.forEach(appbaseClientIntegrationService -> {
                                        if (appbaseClientIntegrationService.isRepairEnabled() &&
                                                !appbaseClientIntegrationService.isHealthy())
                                        {
                                            unhealthyAppbaseClientIntegrationService(
                                                    appbaseClientPool, appbaseClientIntegrationService);
                                        }
                            }));
                }
            });

            return null;
        }

        @SuppressWarnings("unchecked")
        private void unhealthyAppbaseClientIntegrationService(
                final AppbaseClientPool<?, ? extends ComparableValue> appbaseClientPool,
                final AppbaseClientIntegrationService<?, ? extends ComparableValue> appbaseClientIntegrationService)
        {
            final UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>
                    unhealthyClientIntegrationServiceEntry =
                    unhealthyClientIntegrationServiceEntryObjectPool.pollCleanObject();
            unhealthyClientIntegrationServiceEntry
                    .init((AppbaseClientPool) appbaseClientPool,
                            (AppbaseClientIntegrationService) appbaseClientIntegrationService);

            if (enqueueUnhealthyClientWhileLocked(unhealthyClientIntegrationServiceEntry)) {
                appbaseClientPool.unhealthyAppbaseClientIntegrationService(
                        (AppbaseClientIntegrationService) appbaseClientIntegrationService);
            }
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            return schedulableConfiguration;
        }
    }


    /**
     * Used to repair unhealthy {@link AppbaseClientIntegrationService}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    private class AppbaseClientIntegrationServiceRepairTask implements Schedulable<Void> {
        private final SchedulableConfiguration schedulableConfiguration;

        AppbaseClientIntegrationServiceRepairTask(final SchedulableConfiguration schedulableConfiguration) {
            AppbaseUtil.notNull(schedulableConfiguration, "schedulableConfiguration");

            this.schedulableConfiguration = schedulableConfiguration;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Void schedule() {
            unhealthyClientIntegrationServicesLifecycleLock.lock();
            try {
                if (shutdown.get()) {
                    return null;
                }
                final UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>
                        unhealthyClientIntegrationServiceEntry =
                        unhealthyClientIntegrationServices.poll();
                if (unhealthyClientIntegrationServiceEntry != null) {
                    final AppbaseClientIntegrationService<?, ? extends ComparableValue>
                            appbaseClientIntegrationService =
                            unhealthyClientIntegrationServiceEntry.getAppbaseClientIntegrationService();
                    if (!appbaseClientIntegrationService.isHealthy()) {
                        if (appbaseClientIntegrationService.isRepairEnabled()) {
                            if (!checkMaxRepairAttempts(unhealthyClientIntegrationServiceEntry, false)) {
                                return null;
                            }

                            final RepairListenerImpl mutableRepairListener =
                                    repairListenerObjectPool.pollCleanObject();
                            mutableRepairListener.init(unhealthyClientIntegrationServiceEntry);

                            appbaseClientIntegrationService.repair(mutableRepairListener);
                        } else {
                            logger.warn("Ignore unhealthy {} since repair is disabled",
                                    appbaseClientIntegrationService.getClass().getName());
                        }
                    } else {
                        unhealthyClientIntegrationServiceEntry
                                .getSourceAppbaseClientPool()
                                .repairedAppbaseClientIntegrationService(
                                        (AppbaseClientIntegrationService) appbaseClientIntegrationService);
                    }
                }

                return null;
            } finally {
                unhealthyClientIntegrationServicesLifecycleLock.unlock();
            }
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            return schedulableConfiguration;
        }
    }


    /**
     * Implementation of {@link com.viveworks.appbase.client.core.AppbaseClientIntegrationService.RepairListener}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    private class RepairListenerImpl implements AppbaseClientIntegrationService.RepairListener, Poolable {
        private final AtomicReference<UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>>
                unhealthyClientIntegrationServiceEntryRef;

        RepairListenerImpl() {
            this.unhealthyClientIntegrationServiceEntryRef = new AtomicReference<>();
        }

        void init(final UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>
                          unhealthyClientIntegrationServiceEntry)
        {
            AppbaseUtil.notNull(unhealthyClientIntegrationServiceEntry, "unhealthyClientIntegrationServiceEntry");

            this.unhealthyClientIntegrationServiceEntryRef.set(unhealthyClientIntegrationServiceEntry);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onSuccess() {
            AppbaseUtil.valid(this, "this");

            final UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>
                    unhealthyClientIntegrationServiceEntry =
                    unhealthyClientIntegrationServiceEntryRef.get();
            final AppbaseClientIntegrationService<?, ? extends ComparableValue> appbaseClientIntegrationService =
                    unhealthyClientIntegrationServiceEntry.getAppbaseClientIntegrationService();

            repairListenerSuccessAppbaseClientIntegrationServiceCounterByTypeNameMap
                    .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                        final MetricService metricService = appbaseClientCore
                                .getAppbaseCore()
                                .getMetricService();
                        final String name = metricService.name(
                                appbaseClientIntegrationService.getClass(),
                                "repairListenerSuccess",
                                "counter");
                        return metricService
                                .getMetricRegistry()
                                .counter(name);
                    })
                    .inc();

            unhealthyClientIntegrationServiceEntry
                    .getSourceAppbaseClientPool()
                    .repairedAppbaseClientIntegrationService(
                            (AppbaseClientIntegrationService) appbaseClientIntegrationService);
            unhealthyClientIntegrationServiceEntryObjectPool
                    .offerCleanObject(unhealthyClientIntegrationServiceEntry);
        }

        @Override
        public void onFailure() {
            onFailure(null);
        }

        @Override
        public void onFailure(@Nullable final Throwable throwable) {
            AppbaseUtil.valid(this, "this");

            final UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>
                    unhealthyClientIntegrationServiceEntry =
                    unhealthyClientIntegrationServiceEntryRef.get();
            final AppbaseClientIntegrationService<?, ? extends ComparableValue> appbaseClientIntegrationService =
                    unhealthyClientIntegrationServiceEntry.getAppbaseClientIntegrationService();

            if (throwable == null) {
                repairListenerFailureCounterByAppbaseClientIntegrationServiceTypeNameCounterMap
                        .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                            final MetricService metricService = appbaseClientCore
                                    .getAppbaseCore()
                                    .getMetricService();
                            final String name = metricService.name(
                                    appbaseClientIntegrationService.getClass(),
                                    "repairListenerFailure",
                                    "counter");
                            return metricService
                                    .getMetricRegistry()
                                    .counter(name);
                        })
                        .inc();
            } else {
                repairListenerFailureExceptionCounterByAppbaseClientIntegrationServiceTypeNameCounterMap
                        .computeIfAbsent(appbaseClientIntegrationService.getClass().getName(), f -> {
                            final MetricService metricService = appbaseClientCore
                                    .getAppbaseCore()
                                    .getMetricService();
                            final String name = metricService.name(
                                    appbaseClientIntegrationService.getClass(),
                                    "repairListenerFailureException",
                                    "counter");
                            return metricService
                                    .getMetricRegistry()
                                    .counter(name);
                        })
                        .inc();
                logger.error("Failed to repair {}", appbaseClientIntegrationService.getClass().getName(), throwable);
            }

            if (checkMaxRepairAttempts(unhealthyClientIntegrationServiceEntry, true)) {
                unhealthyClientIntegrationServiceEntry.incrementAndGetRepairAttempts();
                enqueueUnhealthyClient(unhealthyClientIntegrationServiceEntry);
            } else {
                unhealthyClientIntegrationServiceEntryObjectPool
                        .offerCleanObject(unhealthyClientIntegrationServiceEntry);
            }
        }

        @Override
        public boolean isValid() {
            return unhealthyClientIntegrationServiceEntryRef.get() != null;
        }

        @Override
        public boolean isClean() {
            return unhealthyClientIntegrationServiceEntryRef.get() == null;
        }

        @Override
        public void clean() {
            unhealthyClientIntegrationServiceEntryRef.set(null);
        }
    }

    /**
     * {@link ObjectFactory} for {@link RepairListenerImpl}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    private final class MutableRepairListenerImplFactory implements ObjectFactory<RepairListenerImpl> {
        @Override
        public RepairListenerImpl newObject() {
            return new RepairListenerImpl();
        }
    }


    /**
     * Represents unhealthy {@link AppbaseClientIntegrationService} entry.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    private static class UnhealthyClientIntegrationServiceEntry<T, V extends ComparableValue> implements Poolable {
        private final AtomicReference<AppbaseClientPool<T, V>> sourceAppbaseClientPoolRef;
        private final AtomicReference<AppbaseClientIntegrationService<T, V>>
                appbaseClientIntegrationServiceRef;
        private final AtomicInteger repairAttemptsRef;

        UnhealthyClientIntegrationServiceEntry() {
            this.sourceAppbaseClientPoolRef = new AtomicReference<>();
            this.appbaseClientIntegrationServiceRef = new AtomicReference<>();
            this.repairAttemptsRef = new AtomicInteger(0);
        }

        void init(final AppbaseClientPool<T, V> sourceAppbaseClientPool,
                  final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
        {
            AppbaseUtil.notNull(sourceAppbaseClientPool, "sourceAppbaseClientPool");
            AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

            this.sourceAppbaseClientPoolRef.set(sourceAppbaseClientPool);
            this.appbaseClientIntegrationServiceRef.set(appbaseClientIntegrationService);
        }

        AppbaseClientPool<T, V> getSourceAppbaseClientPool() {
            return sourceAppbaseClientPoolRef.get();
        }

        AppbaseClientIntegrationService<T, V> getAppbaseClientIntegrationService() {
            return appbaseClientIntegrationServiceRef.get();
        }

        int getRepairAtempts() {
            return repairAttemptsRef.get();
        }

        int incrementAndGetRepairAttempts() {
            return repairAttemptsRef.incrementAndGet();
        }

        @Override
        public boolean isValid() {
            return sourceAppbaseClientPoolRef.get() != null &&
                    appbaseClientIntegrationServiceRef.get() != null &&
                    repairAttemptsRef.get() >= 0;
        }

        @Override
        public boolean isClean() {
            return sourceAppbaseClientPoolRef.get() == null &&
                    appbaseClientIntegrationServiceRef.get() == null &&
                    repairAttemptsRef.get() == 0;
        }

        @Override
        public void clean() {
            sourceAppbaseClientPoolRef.set(null);
            appbaseClientIntegrationServiceRef.set(null);
            repairAttemptsRef.set(0);
        }

        @Override
        public String toString() {
            return "UnhealthyClientIntegrationServiceEntry{" +
                    "sourceAppbaseClientPoolRef=" + sourceAppbaseClientPoolRef +
                    ", appbaseClientIntegrationServiceRef=" + appbaseClientIntegrationServiceRef +
                    ", repairAttempts=" + repairAttemptsRef.get() +
                    '}';
        }
    }

    /**
     * {@link ObjectFactory} for {@link RepairListenerImpl}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    private final class UnhealthyClientIntegrationServiceEntryFactory
            implements ObjectFactory<UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue>>
    {
        @Override
        public UnhealthyClientIntegrationServiceEntry<?, ? extends ComparableValue> newObject() {
            return new UnhealthyClientIntegrationServiceEntry<>();
        }
    }
}
