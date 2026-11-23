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

package com.viveworks.appbase.core.service.log.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreException;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.LogStoreFactoryException;
import com.viveworks.appbase.core.service.log.store.ReactiveLogStore;
import com.viveworks.appbase.core.service.log.store.SchedulerOffloadReactiveLogStore;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore;
import com.viveworks.appbase.core.service.log.store.lucene.ReactiveLuceneLogStore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.read.DefaultReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundaryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link LogAccessor}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings("IllegalCatch")
public class DefaultLogAccessor implements LogAccessor {
    private final Logger logger;
    private final MetricService metricService;
    private final Map<String, LogStore> logStoreByTypeNameMap;
    private final Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>>
            logStoreFactoryByConfigurationTypeNameMap;
    private final Map<String, LogStoreConfiguration> logStoreConfigurationByTypeNameMap;
    private final Map<String, LogStoreConfiguration> logStoreConfigurationByLogStoreTypeNameMap;
    private final Gauge logStoreByTypeNameMapSizeGauge;
    private final Gauge logStoreConfigurationByTypeNameMapSizeGauge;
    private final Gauge logStoreFactoryByTypeNameMapSizeGauge;
    private final Gauge logStoreConfigurationByLogStoreTypeNameMapSizeGauge;
    private final Map<String, Counter> reloadErrorCounterByStoreTypeNameMap;
    private final Map<String, Timer> timerByExceptionStoreTypeNameMap;
    private final Map<String, Counter> errorCounterByExceptionStoreTypeNameMap;
    private final AtomicBoolean shutdown;
    private final ReactiveReadBoundary reactiveReadBoundary;

    public DefaultLogAccessor(final LogService logService,
                              final MetricService metricService,
                              final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration,
                              final LogStore... logStores)
    {
        this(logService, metricService, defaultLogAccessorConfiguration,
                new DefaultReactiveReadBoundary(ReactiveReadBoundaryConfiguration.builder().build(), null),
                logStores);
    }

    public DefaultLogAccessor(final LogService logService,
                              final MetricService metricService,
                              final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration,
                              final ReactiveReadBoundary reactiveReadBoundary,
                              final LogStore... logStores)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(defaultLogAccessorConfiguration, "defaultLogAccessorConfiguration");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");

        DefaultLogAccessorConfigurationValidator.validate(
                defaultLogAccessorConfiguration.isEnabled(),
                defaultLogAccessorConfiguration.getLogStoreConfigurations());

        this.reactiveReadBoundary = reactiveReadBoundary;
        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
        this.logStoreByTypeNameMap = new ConcurrentHashMap<>();
        this.logStoreConfigurationByTypeNameMap = new ConcurrentHashMap<>();
        this.logStoreFactoryByConfigurationTypeNameMap = new ConcurrentHashMap<>();
        this.logStoreConfigurationByLogStoreTypeNameMap = new ConcurrentHashMap<>();
        this.timerByExceptionStoreTypeNameMap = new ConcurrentHashMap<>();
        this.errorCounterByExceptionStoreTypeNameMap = new ConcurrentHashMap<>();
        this.logStoreByTypeNameMapSizeGauge = metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "logStoreByTypeNameMap", "size", "gauge"),
                        logStoreByTypeNameMap::size
                );
        this.logStoreFactoryByTypeNameMapSizeGauge = logStoreFactoryByConfigurationTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "logStoreFactoryByConfigurationTypeNameMap", "size", "gauge"),
                        this.logStoreFactoryByTypeNameMapSizeGauge);

        this.logStoreConfigurationByTypeNameMapSizeGauge = logStoreConfigurationByTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "logStoreConfigurationByTypeNameMap", "size", "gauge"),
                        this.logStoreConfigurationByTypeNameMapSizeGauge);
        this.logStoreConfigurationByLogStoreTypeNameMapSizeGauge = logStoreConfigurationByLogStoreTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "logStoreConfigurationByLogStoreTypeNameMap", "size", "gauge"),
                        this.logStoreConfigurationByLogStoreTypeNameMapSizeGauge);
        this.reloadErrorCounterByStoreTypeNameMap = new ConcurrentHashMap<>();

        initLogStoreConfigurationByTypeNameMap(defaultLogAccessorConfiguration);
        if (logStores != null) {
            for (final LogStore logStore : logStores) {
                registerLogStore(logStore);
            }
        }

        this.shutdown = new AtomicBoolean(false);
    }

    private void initLogStoreConfigurationByTypeNameMap(
            final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration)
    {
        DefaultLogAccessorConfigurationValidator.validate(
                defaultLogAccessorConfiguration.isEnabled(),
                defaultLogAccessorConfiguration.getLogStoreConfigurations());

        final List<LogStoreConfiguration> logStoreConfigurations =
                defaultLogAccessorConfiguration.getLogStoreConfigurations();

        if (logStoreConfigurations != null) {
            logStoreConfigurationByLogStoreTypeNameMap.clear();

            for (final LogStoreConfiguration logStoreConfiguration : logStoreConfigurations) {
                if (logStoreConfiguration != null) {
                    final String logStoreConfigurationTypeName = logStoreConfiguration.getClass().getName();
                    logger.info("Registering log store configuration of type {}: {}",
                            logStoreConfigurationTypeName, logStoreConfiguration);
                    logStoreConfigurationByTypeNameMap.put(
                            logStoreConfiguration.getClass().getName(), logStoreConfiguration);
                    logStoreConfigurationByLogStoreTypeNameMap.put(
                            logStoreConfiguration.getLogStoreType().getName(), logStoreConfiguration);
                }
            }
        }
    }

    @Override
    public Map<String, Collection<LogRecord>> readLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp,
            final int pageSize)
            throws LogAccessorException
    {
        LogReadPageSize.requireValid(pageSize);

        final Map<String, Collection<LogRecord>> result = new ConcurrentHashMap<>();
        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "readLogRecordsBetween";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    result.put(
                            logStoreTypeName,
                            logStore.readLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize));
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<LogRecord>>> asyncReadLogRecordsBetween(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize)
    {
        LogReadPageSize.requireValid(pageSize);

        final Map<String, CompletableFuture<Collection<LogRecord>>> resultFutures = new ConcurrentHashMap<>();
        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncReadLogRecordsBetween";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncReadLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Flux<Map.Entry<String, LogRecord>> streamLogRecordsBetweenReactive(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize) {
        LogReadPageSize.requireValid(pageSize);

        return Flux.fromIterable(logStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .concatMap(store -> reactiveViewOf(store)
                        .streamLogRecordsBetweenReactive(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize)
                        .map(record -> new AbstractMap.SimpleImmutableEntry<>(store.getClass().getName(), record)));
    }

    @Override
    public Mono<Map<String, Collection<LogRecord>>> readLogRecordsBetweenReactive(
            final long inclusiveStartTimestamp, final long exclusiveEndTimestamp, final int pageSize)
    {
        LogReadPageSize.requireValid(pageSize);

        return Flux.fromIterable(logStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readLogRecordsBetweenReactive(inclusiveStartTimestamp, exclusiveEndTimestamp, pageSize)
                        .map(records -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), records)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Map<String, Collection<LogRecord>> readLogRecordsAt(
            final long timestamp,
            final int pageSize)
            throws LogAccessorException
    {
        LogReadPageSize.requireValid(pageSize);

        final Map<String, Collection<LogRecord>> result = new ConcurrentHashMap<>();
        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "readLogRecordsAt";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    result.put(
                            logStoreTypeName,
                            logStore.readLogRecordsAt(timestamp, pageSize));
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<LogRecord>>> asyncReadLogRecordsAt(
            final long timestamp, final int pageSize)
    {
        LogReadPageSize.requireValid(pageSize);

        final Map<String, CompletableFuture<Collection<LogRecord>>> resultFutures = new ConcurrentHashMap<>();
        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncReadLogRecordsAt";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncReadLogRecordsAt(timestamp, pageSize));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Mono<Map<String, Collection<LogRecord>>> readLogRecordsAtReactive(
            final long timestamp, final int pageSize)
    {
        LogReadPageSize.requireValid(pageSize);

        return Flux.fromIterable(logStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readLogRecordsAtReactive(timestamp, pageSize)
                        .map(records -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), records)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Map<String, Collection<LogRecord>> readLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria,
            final int pageSize)
            throws LogAccessorException
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");
        LogReadPageSize.requireValid(pageSize);

        final Map<String, Collection<LogRecord>> result = new ConcurrentHashMap<>();

        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "readLogRecordsByCriteria";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    result.put(
                            logStoreTypeName,
                            logStore.readLogRecordsByCriteria(logRecordCriteria, pageSize));
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<LogRecord>>> asyncReadLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria, final int pageSize)
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");
        LogReadPageSize.requireValid(pageSize);

        final Map<String, CompletableFuture<Collection<LogRecord>>> resultFutures = new ConcurrentHashMap<>();
        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncReadLogRecordsByCriteria";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncReadLogRecordsByCriteria(logRecordCriteria, pageSize));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Mono<Map<String, Collection<LogRecord>>> readLogRecordsByCriteriaReactive(
            final LogRecordCriteria logRecordCriteria, final int pageSize)
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");
        LogReadPageSize.requireValid(pageSize);

        return Flux.fromIterable(logStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readLogRecordsByCriteriaReactive(logRecordCriteria, pageSize)
                        .map(records -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), records)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Build the {@link ReactiveLogStore} view of a {@link LogStore} — a {@link LuceneLogStore} routes
     * through {@link ReactiveLuceneLogStore}; any other {@link LogStore} routes through {@link
     * SchedulerOffloadReactiveLogStore}, offloading its blocking sync reads onto {@link
     * #reactiveReadBoundary}. Built fresh on every call rather than cached: the wrappers are trivial
     * (1-2 fields), and caching by {@link LogStore} instance would never be evicted by {@link
     * #unregisterLogStore}, {@link #clearLogStores} or {@link #reload}, retaining shut-down store
     * instances indefinitely.
     *
     * @param logStore Log store to get the reactive view of
     * @return the log store's reactive view
     */
    private ReactiveLogStore reactiveViewOf(final LogStore logStore) {
        return (logStore instanceof LuceneLogStore)
                ? new ReactiveLuceneLogStore((LuceneLogStore) logStore)
                : new SchedulerOffloadReactiveLogStore(logStore, reactiveReadBoundary);
    }

    @Override
    public Collection<String> writeLogRecord(final LogRecord logRecord) throws LogAccessorException {
        AppbaseUtil.notNull(logRecord, "logRecord");

        final List<String> result = new ArrayList<>();

        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "writeLogRecord";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    logStore.writeLogRecord(logRecord);
                    result.add(logStoreTypeName);
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncWriteLogRecord(final LogRecord logRecord) {
        AppbaseUtil.notNull(logRecord, "logRecord");

        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();

        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncWriteLogRecord";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncWriteLogRecord(logRecord));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Collection<String> removeLogRecordsBetween(
            final long inclusiveStartTimestamp,
            final long exclusiveEndTimestamp)
            throws LogAccessorException
    {
        final List<String> result = new ArrayList<>();

        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "removeLogRecordsBetween";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    logStore.removeLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp);
                    result.add(logStoreTypeName);
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncRemoveLogRecordsBetween(final long inclusiveStartTimestamp,
                                                                             final long exclusiveEndTimestamp)
    {
        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();

        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncRemoveLogRecordsBetween";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncRemoveLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Collection<String> removeLogRecordsAt(final long timestamp) throws LogAccessorException {
        final List<String> result = new ArrayList<>();

        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "removeLogRecordsBetween";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    logStore.removeLogRecordsAt(timestamp);
                    result.add(logStoreTypeName);
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncRemoveLogRecordsAt(final long timestamp) {
        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();

        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncRemoveLogRecordsAt";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncRemoveLogRecordsAt(timestamp));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Collection<String> removeLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria) throws LogAccessorException
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");

        final Collection<String> result = new ArrayList<>();

        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "removeLogRecordsByCriteria";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    logStore.removeLogRecordsByCriteria(logRecordCriteria);
                    result.add(logStoreTypeName);
                } catch (Exception ex) {
                    handleErrorAndRethrow(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncRemoveLogRecordsByCriteria(
            final LogRecordCriteria logRecordCriteria)
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");

        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();

        logStoreByTypeNameMap.values().forEach(logStore -> {
            if (logStore != null) {
                final String logStoreTypeName = logStore.getClass().getName();
                final String operationName = "asyncRemoveLogRecordsByCriteria";
                final Timer timer = timer(logStoreTypeName, operationName);
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            logStoreTypeName,
                            logStore.asyncRemoveLogRecordsByCriteria(logRecordCriteria));
                } catch (Exception ex) {
                    handleError(logStoreTypeName, operationName, ex);
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Map<String, LogStoreFactory<LogStore, LogStoreConfiguration>>
    getLogStoreFactoryByConfigurationTypeMap()
    {
        return Collections.unmodifiableMap(logStoreFactoryByConfigurationTypeNameMap);
    }

    @Override
    public void registerLogStoreFactoryByConfigurationType(
            final Class<? extends LogStoreConfiguration> logStoreConfigurationType,
            final LogStoreFactory logStoreFactory) throws LogStoreFactoryException
    {
        AppbaseUtil.notNull(logStoreConfigurationType, "logStoreConfigurationType");
        AppbaseUtil.notNull(logStoreFactory, "logStoreFactory");

        logger.info("Registering log store factory {} for configuration type {}",
                logStoreFactory.getClass().getName(), logStoreConfigurationType);
        logStoreFactoryByConfigurationTypeNameMap.put(logStoreConfigurationType.getName(), logStoreFactory);

        final String logStoreConfigurationTypeName = logStoreConfigurationType.getName();
        final LogStoreConfiguration logStoreConfiguration =
                logStoreConfigurationByTypeNameMap.get(logStoreConfigurationTypeName);
        if (logStoreConfiguration != null && logStoreConfiguration.isEnabled()) {
            final LogStore logStore = logStoreFactory.newLogStore(logStoreConfiguration);
            registerLogStore(logStore);
        }
    }

    @Override
    public void unregisterLogStoreFactoryByConfigurationType(
            final Class<? extends LogStoreConfiguration> logStoreConfigurationType,
            final Class<? extends LogStoreFactory> logStoreFactoryType)
    {
        AppbaseUtil.notNull(logStoreConfigurationType, "logStoreConfigurationType");
        AppbaseUtil.notNull(logStoreFactoryType, "logStoreFactoryType");

        logger.info("Unregistering log store factory [{}] for configuration type [{}]",
                logStoreFactoryType, logStoreConfigurationType);
        final String logStoreConfigurationTypeName = logStoreConfigurationType.getName();
        logStoreFactoryByConfigurationTypeNameMap.remove(logStoreConfigurationTypeName);
        logStoreConfigurationByTypeNameMap.remove(logStoreConfigurationTypeName);
    }

    @Override
    public void clearLogStoreFactoryByConfigurationTypeMap() {
        logger.info("Clearing logStoreFactoryByConfigurationTypeNameMap");
        logStoreFactoryByConfigurationTypeNameMap.clear();
    }

    @Override
    @Unmodifiable
    public Collection<LogStore> getLogStores() {
        return Collections.unmodifiableCollection(logStoreByTypeNameMap.values());
    }

    @Override
    public void unregisterLogStore(final Class<? extends LogStore> logStoreType) {
        AppbaseUtil.notNull(logStoreType, "logStoreType");

        logger.info("Unregistering log store: {}", logStoreType);
        logStoreByTypeNameMap.remove(logStoreType.getName());
    }

    @Override
    public void clearLogStores() {
        logger.info("Clearing logStoreByTypeNameMap");
        logStoreByTypeNameMap.clear();
    }

    @Override
    public void registerLogStore(final LogStore logStore) {
        AppbaseUtil.notNull(logStore, "logStore");

        final String logStoreTypeName = logStore.getClass().getName();
        if (!logStoreByTypeNameMap.containsKey(logStoreTypeName)) {
            logger.info("Registering log store: {}", logStoreTypeName);
            logStoreByTypeNameMap.put(logStoreTypeName, logStore);
        }
    }

    @Override
    public <C extends LogAccessorConfiguration> void reload(
            final C logAccessorConfiguration) throws LogAccessorException
    {
        AppbaseUtil.notNull(logAccessorConfiguration, "logAccessorConfiguration");

        logger.info("Reloading {} with {}", getClass().getName(), logAccessorConfiguration);

        if (!(logAccessorConfiguration instanceof DefaultLogAccessorConfiguration)) {
            throw new LogAccessorException("logAccessorConfiguration must be of type " +
                    DefaultLogAccessorConfiguration.class.getName());
        }

        final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration =
                (DefaultLogAccessorConfiguration) logAccessorConfiguration;
        DefaultLogAccessorConfigurationValidator.validate(
                defaultLogAccessorConfiguration.isEnabled(),
                defaultLogAccessorConfiguration.getLogStoreConfigurations());

        logger.info("Reloading {} with {}", getClass().getName(), defaultLogAccessorConfiguration);
        initLogStoreConfigurationByTypeNameMap(defaultLogAccessorConfiguration);

        logStoreByTypeNameMap.forEach((logStoreTypeName, logStore) -> {
            final LogStoreConfiguration logStoreConfiguration =
                    logStoreConfigurationByLogStoreTypeNameMap.get(logStoreTypeName);
            if (logStoreConfiguration != null && logStoreConfiguration.isEnabled()) {
                final LogStoreFactory<LogStore, LogStoreConfiguration> logStoreFactory =
                        logStoreFactoryByConfigurationTypeNameMap.get(
                                logStoreConfiguration.getClass().getName());
                if (logStoreFactory != null) {
                    try {
                        final LogStore newLogStore = logStoreFactory.newLogStore(logStoreConfiguration);
                        logger.info("Replacing log store: {}", logStoreTypeName);
                        logStoreByTypeNameMap.replace(logStoreTypeName, newLogStore);
                        logStore.shutdown();
                    } catch (LogStoreFactoryException lsfe) {
                        logger.error("Error while reloading log store {}", logStoreTypeName, lsfe);
                        reloadErrorCounterByStoreTypeNameMap
                                .computeIfAbsent(logStoreTypeName, f -> {
                                    return metricService
                                            .getMetricRegistry()
                                            .counter(metricService.name(
                                                    getClass(), logStoreTypeName, "error", "counter"));
                                })
                                .inc();
                    }
                }
            }
        });
    }

    @Override
    public void synchronizeLogStores() throws LogAccessorException {
        for (final Map.Entry<String, LogStore> entry : logStoreByTypeNameMap.entrySet()) {
            final String logStoreTypeName = entry.getKey();
            final LogStore logStore = entry.getValue();

            if (logStoreTypeName != null && logStore != null) {
                try {
                    logStore.synchronize();
                } catch (LogStoreException lse) {
                    throw new LogAccessorException("Error synchronizing " + logStore.getClass().getName(), lse);
                }
            }
        }
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        for (final LogStore logStore : logStoreByTypeNameMap.values()) {
            try {
                logStore.shutdown();
            } catch (Exception ex) {
                logger.error("Error shutting down {}", logStore.getClass().getName(), ex);
            }
        }

        clearLogStores();
        clearLogStoreFactoryByConfigurationTypeMap();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    public Gauge getLogStoreByTypeNameMapSizeGauge() {
        return logStoreByTypeNameMapSizeGauge;
    }

    public Gauge getLogStoreFactoryByTypeNameMapSizeGauge() {
        return logStoreFactoryByTypeNameMapSizeGauge;
    }

    public Gauge getLogStoreConfigurationByLogStoreTypeNameMapSizeGauge() {
        return logStoreConfigurationByLogStoreTypeNameMapSizeGauge;
    }

    public Gauge getLogStoreConfigurationByTypeNameMapSizeGauge() {
        return logStoreConfigurationByTypeNameMapSizeGauge;
    }

    private Timer timer(final String logStoreTypeName, final String operationName) {
        return timerByExceptionStoreTypeNameMap
                .computeIfAbsent(logStoreTypeName, f -> {
                    return metricService
                            .getMetricRegistry()
                            .timer(metricService.name(
                                    getClass(),
                                    logStoreTypeName,
                                    operationName,
                                    "timer"));
                });
    }

    private void handleError(final String logStoreTypeName, final String operationName, final Exception ex) {
        logger.error("Error while '{}' using {}",
                operationName, logStoreTypeName, ex);
        errorCounterByExceptionStoreTypeNameMap
                .computeIfAbsent(logStoreTypeName, f -> {
                    return metricService
                            .getMetricRegistry()
                            .counter(metricService.name(
                                    getClass(),
                                    logStoreTypeName,
                                    operationName,
                                    "error",
                                    "counter"));
                })
                .inc();
    }

    private void handleErrorAndRethrow(
            final String logStoreTypeName,
            final String operationName,
            final Exception ex)
            throws LogAccessorException
    {
        final String message = String.format("Error while '%s' using %s", operationName, logStoreTypeName);
        logger.error(message, ex);
        errorCounterByExceptionStoreTypeNameMap
                .computeIfAbsent(logStoreTypeName, f -> {
                    return metricService
                            .getMetricRegistry()
                            .counter(metricService.name(
                                    getClass(),
                                    logStoreTypeName,
                                    operationName,
                                    "error",
                                    "counter"));
                })
                .inc();
        throw new LogAccessorException(message, ex);
    }
}
