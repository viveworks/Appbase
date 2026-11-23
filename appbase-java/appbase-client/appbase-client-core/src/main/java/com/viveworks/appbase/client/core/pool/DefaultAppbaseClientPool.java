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

package com.viveworks.appbase.client.core.pool;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link AppbaseClientPool}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
public class DefaultAppbaseClientPool<T, V extends ComparableValue>
        implements AppbaseClientPool<T, V>, AppbaseClientPoolRegistry<T, V>
{
    private final Logger logger;
    private final AppbaseClientCore appbaseClientCore;
    private final AppbaseClientPoolManager appbaseClientPoolManager;
    private final Map<AppbaseClientKey<V>, Queue<AppbaseClientIntegrationService<T, V>>>
            appbaseClientIntegrationServiceQueueByKeyMap;
    private final Map<AppbaseClientKey<V>, Collection<AppbaseClientIntegrationService<T, V>>>
            unhealthyAppbaseClientIntegrationServiceByClientKeyMap;
    private final Map<String, Counter> appbaseClientIntegrationServiceShutdownByErrorCounterMap;
    private final Map<AppbaseClientKey<V>, Set<String>>
            unregisteredAppbaseClientIntegrationServicesByKeyMap;
    private final Object unhealthyAppbaseClientIntegrationServiceLifecycleMonitor;
    private final AtomicBoolean shutdown;

    @Inject
    public DefaultAppbaseClientPool(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultAppbaseClientPoolConfiguration defaultAppbaseClientPoolConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(appbaseClientPoolManager, "appbaseClientPoolManager");
        AppbaseUtil.notNull(defaultAppbaseClientPoolConfiguration, "defaultAppbaseClientPoolConfiguration");

        this.logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPoolManager = appbaseClientPoolManager;
        this.appbaseClientIntegrationServiceQueueByKeyMap = new ConcurrentHashMap<>();
        this.appbaseClientIntegrationServiceShutdownByErrorCounterMap = new ConcurrentHashMap<>();
        this.unhealthyAppbaseClientIntegrationServiceByClientKeyMap = new ConcurrentHashMap<>();
        this.unregisteredAppbaseClientIntegrationServicesByKeyMap = new ConcurrentHashMap<>();
        this.unhealthyAppbaseClientIntegrationServiceLifecycleMonitor = new Object();
        this.shutdown = new AtomicBoolean(false);
    }

    @Nullable
    @Override
    public AppbaseClientIntegrationService<T, V> takeAppbaseClientIntegrationService(
            final AppbaseClientKey<V> appbaseClientKey)
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");

        final Queue<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServices =
                appbaseClientIntegrationServiceQueueByKeyMap.get(appbaseClientKey);
        if (appbaseClientIntegrationServices != null) {
            while (!appbaseClientIntegrationServices.isEmpty()) {
                final AppbaseClientIntegrationService<T, V> polled = appbaseClientIntegrationServices.poll();
                if (polled != null) {
                    if (!polled.isHealthy()) {
                        appbaseClientPoolManager.handleUnhealthyAppbaseClientIntegrationService(this, polled);
                        unhealthyAppbaseClientIntegrationService(polled);
                    } else {
                        appbaseClientIntegrationServices.offer(polled);
                        return polled;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void giveAppbaseClientIntegrationService(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        final AppbaseClientKey<V> appbaseClientKey = appbaseClientIntegrationService.getAppbaseClientKey();

        appbaseClientIntegrationServiceQueueByKeyMap
                .computeIfAbsent(appbaseClientKey, f -> new ManyToOneConcurrentLinkedQueue<>())
                .offer(appbaseClientIntegrationService);
    }

    @Override
    public void unhealthyAppbaseClientIntegrationService(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        synchronized (unhealthyAppbaseClientIntegrationServiceLifecycleMonitor) {
            if (shutdown.get()) {
                return;
            }
            logger.info("Unhealthy {} found: {}",
                    AppbaseClientIntegrationService.class.getName(), appbaseClientIntegrationService);

            final AppbaseClientKey<V> appbaseClientKey = appbaseClientIntegrationService.getAppbaseClientKey();

            final Queue<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServices =
                    appbaseClientIntegrationServiceQueueByKeyMap.get(appbaseClientKey);
            if (appbaseClientIntegrationServices != null) {
                final AppbaseClientIntegrationService<T, V> polled = appbaseClientIntegrationServices.poll();
                if (polled != null && !appbaseClientIntegrationService.getClass().isAssignableFrom(polled.getClass())) {
                    appbaseClientIntegrationServices.offer(polled);
                }
            }

            unhealthyAppbaseClientIntegrationServiceByClientKeyMap
                    .computeIfAbsent(appbaseClientKey, f -> new CopyOnWriteArrayList<>())
                    .add(appbaseClientIntegrationService);
        }
    }

    @Override
    public void repairedAppbaseClientIntegrationService(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        synchronized (unhealthyAppbaseClientIntegrationServiceLifecycleMonitor) {
            if (shutdown.get()) {
                return;
            }
            logger.info("Repaired {}: {}",
                    AppbaseClientIntegrationService.class.getName(), appbaseClientIntegrationService);

            removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(appbaseClientIntegrationService);
            registerAppbaseClientIntegrationService(appbaseClientIntegrationService);
        }
    }

    @Override
    public AppbaseClientPoolRegistry<T, V> getAppbaseClientPoolRegistry() {
        return this;
    }

    @Override
    @Unmodifiable
    public Map<AppbaseClientKey<V>, Collection<AppbaseClientIntegrationService<T, V>>>
    getAppbaseClientIntegrationServicesByKeyMap()
    {
        final Map<AppbaseClientKey<V>, Collection<AppbaseClientIntegrationService<T, V>>> result = new HashMap<>();

        appbaseClientIntegrationServiceQueueByKeyMap.forEach((appbaseClientKey, appbaseClientIntegrationServices) -> {
            appbaseClientIntegrationServices.forEach(appbaseClientIntegrationService -> {
                result.computeIfAbsent(appbaseClientKey, f -> new ArrayList<>())
                        .add(appbaseClientIntegrationService);
            });
        });

        return Collections.unmodifiableMap(result);
    }

    @Override
    @Unmodifiable
    public Collection<AppbaseClientIntegrationService<T, V>> getAppbaseClientIntegrationServices(
            final AppbaseClientKey<V> appbaseClientKey)
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");

        final Queue<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServices =
                appbaseClientIntegrationServiceQueueByKeyMap.get(appbaseClientKey);
        if (appbaseClientIntegrationServices != null) {
            final Collection<AppbaseClientIntegrationService<T, V>> result =
                    new ArrayList<>(appbaseClientIntegrationServices);
            return Collections.unmodifiableCollection(result);
        }

        return null;
    }

    @Override
    public void registerAppbaseClientIntegrationService(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        final AppbaseClientKey<V> appbaseClientKey = appbaseClientIntegrationService.getAppbaseClientKey();
        registerAppbaseClientIntegrationService(appbaseClientKey, appbaseClientIntegrationService);
    }

    @Override
    public void registerAppbaseClientIntegrationService(
            final AppbaseClientKey<V> appbaseClientKey,
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");
        logger.debug("Registering {}: {}", appbaseClientKey, appbaseClientIntegrationService);

        appbaseClientIntegrationServiceQueueByKeyMap
                .computeIfAbsent(appbaseClientKey, f -> new ManyToOneConcurrentLinkedQueue<>())
                .offer(appbaseClientIntegrationService);

        unregisteredAppbaseClientIntegrationServicesByKeyMap
                .values()
                .forEach(strings -> strings.remove(appbaseClientIntegrationService.getClass().getName()));
        removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(appbaseClientIntegrationService);
    }

    @Override
    public void unregisterAppbaseClientIntegrationServices(final AppbaseClientKey<V> appbaseClientKey) {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");

        logger.info("Unregistering {}", appbaseClientKey);

        final Queue<AppbaseClientIntegrationService<T, V>> queuedAppbaseClientIntegrationServices =
                appbaseClientIntegrationServiceQueueByKeyMap.remove(appbaseClientKey);
        if (queuedAppbaseClientIntegrationServices != null) {
            queuedAppbaseClientIntegrationServices.forEach(queuedAppbaseClientIntegrationService -> {
                shutdown(queuedAppbaseClientIntegrationService);
                queuedAppbaseClientIntegrationServices.remove(queuedAppbaseClientIntegrationService);

                unregisteredAppbaseClientIntegrationServicesByKeyMap
                        .computeIfAbsent(appbaseClientKey, f -> new ConcurrentSkipListSet<>())
                        .add(queuedAppbaseClientIntegrationService.getClass().getName());
                removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(queuedAppbaseClientIntegrationService);
            });
        }
    }

    @Override
    public void unregisterAppbaseClientIntegrationServices(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        logger.info("Unregistering {}: {}",
                appbaseClientIntegrationService.getAppbaseClientKey(), appbaseClientIntegrationService);

        appbaseClientIntegrationServiceQueueByKeyMap
                .values()
                .forEach(queuedAppbaseClientIntegrationServices -> queuedAppbaseClientIntegrationServices
                        .stream()
                        .filter(appbaseClientIntegrationService::equals)
                        .forEach(queuedAppbaseClientIntegrationService -> {
                            shutdown(queuedAppbaseClientIntegrationService);
                            queuedAppbaseClientIntegrationServices.remove(queuedAppbaseClientIntegrationService);

                            unregisteredAppbaseClientIntegrationServicesByKeyMap
                                    .computeIfAbsent(
                                            queuedAppbaseClientIntegrationService.getAppbaseClientKey(),
                                            f -> new ConcurrentSkipListSet<>())
                                    .add(queuedAppbaseClientIntegrationService.getClass().getName());
                            removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(
                                    queuedAppbaseClientIntegrationService);
                        }));
    }

    @Override
    public void unregisterAppbaseClientIntegrationServices(
            final Class<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServiceType)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationServiceType, "appbaseClientIntegrationServiceType");

        logger.info("Unregistering {}", appbaseClientIntegrationServiceType);

        appbaseClientIntegrationServiceQueueByKeyMap
                .values()
                .forEach(queuedAppbaseClientIntegrationServices -> queuedAppbaseClientIntegrationServices
                        .stream()
                        .filter(queuedAppbaseClientIntegrationService ->
                                queuedAppbaseClientIntegrationService.getClass().equals(appbaseClientIntegrationServiceType))
                        .forEach(queuedAppbaseClientIntegrationService -> {
                            shutdown(queuedAppbaseClientIntegrationService);
                            queuedAppbaseClientIntegrationServices.remove(queuedAppbaseClientIntegrationService);

                            unregisteredAppbaseClientIntegrationServicesByKeyMap
                                    .computeIfAbsent(
                                            queuedAppbaseClientIntegrationService.getAppbaseClientKey(),
                                            f -> new ConcurrentSkipListSet<>())
                                    .add(queuedAppbaseClientIntegrationService.getClass().getName());
                            removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(
                                    queuedAppbaseClientIntegrationService);
                        }));
    }

    @Override
    public void unregisterAppbaseClientIntegrationService(
            final AppbaseClientKey<V> appbaseClientKey,
            final Class<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServiceType)
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");
        AppbaseUtil.notNull(appbaseClientIntegrationServiceType, "appbaseClientIntegrationServiceType");

        logger.info("Unregistering {}: {}", appbaseClientKey, appbaseClientIntegrationServiceType);

        final Queue<AppbaseClientIntegrationService<T, V>> queuedAppbaseClientIntegrationServices =
                appbaseClientIntegrationServiceQueueByKeyMap.get(appbaseClientKey);
        if (queuedAppbaseClientIntegrationServices != null) {
            queuedAppbaseClientIntegrationServices
                    .stream()
                    .filter(queuedAppbaseClientIntegrationService ->
                            queuedAppbaseClientIntegrationService.getClass().equals(appbaseClientIntegrationServiceType))
                    .forEach(queuedAppbaseClientIntegrationService -> {
                        shutdown(queuedAppbaseClientIntegrationService);
                        queuedAppbaseClientIntegrationServices.remove(queuedAppbaseClientIntegrationService);

                        unregisteredAppbaseClientIntegrationServicesByKeyMap
                                .computeIfAbsent(
                                        queuedAppbaseClientIntegrationService.getAppbaseClientKey(),
                                        f -> new ConcurrentSkipListSet<>())
                                .add(queuedAppbaseClientIntegrationService.getClass().getName());
                        removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(queuedAppbaseClientIntegrationService);
                    });
        }
    }

    @Override
    public void clearAllAppbaseClientIntegrationServices() {
        logger.info("Clearing all {}", AppbaseClientIntegrationService.class.getName());


        appbaseClientIntegrationServiceQueueByKeyMap
                .values()
                .forEach(queuedAppbaseClientIntegrationServices -> {
                    while (!queuedAppbaseClientIntegrationServices.isEmpty()) {
                        final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService =
                                queuedAppbaseClientIntegrationServices.poll();
                        if (appbaseClientIntegrationService != null) {
                            appbaseClientIntegrationService.shutdown();
                        }
                    }
                });
        appbaseClientIntegrationServiceQueueByKeyMap.clear();
        unregisteredAppbaseClientIntegrationServicesByKeyMap.clear();
        unhealthyAppbaseClientIntegrationServiceByClientKeyMap.clear();
    }

    public Map<AppbaseClientKey<V>, Queue<AppbaseClientIntegrationService<T, V>>>
    getAppbaseClientIntegrationServiceQueueByKeyMap()
    {
        return appbaseClientIntegrationServiceQueueByKeyMap;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        synchronized (unhealthyAppbaseClientIntegrationServiceLifecycleMonitor) {
            if (!shutdown.compareAndSet(false, true)) {
                return;
            }
        }

        logger.info("Shutting down: {}", getClass().getName());

        clearAllAppbaseClientIntegrationServices();
        appbaseClientPoolManager.shutdown();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void shutdown(final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService) {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        try {
            appbaseClientIntegrationService.shutdown();
        } catch (Exception ex) {
            logger.error("Error while shutting down {}", appbaseClientIntegrationService, ex);
            appbaseClientIntegrationServiceShutdownByErrorCounterMap
                    .computeIfAbsent(
                            appbaseClientIntegrationService.getClass().getName(),
                            f -> {
                                final MetricService metricService = appbaseClientCore
                                        .getAppbaseCore()
                                        .getMetricService();
                                final String name = metricService.name(
                                        AppbaseClientIntegrationService.class, "shutdown", "errorCounter");
                                return metricService
                                        .getMetricRegistry()
                                        .counter(name);
                            });
        }
    }

    private void removeUnhealthyAppbaseClientIntegrationServiceByClientKeyMap(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        unhealthyAppbaseClientIntegrationServiceByClientKeyMap
                .values()
                .forEach(appbaseClientIntegrationServices ->
                        appbaseClientIntegrationServices
                                .stream()
                                .filter(appbaseClientIntegrationService::equals)
                                .forEach(appbaseClientIntegrationServices::remove));
    }
}
