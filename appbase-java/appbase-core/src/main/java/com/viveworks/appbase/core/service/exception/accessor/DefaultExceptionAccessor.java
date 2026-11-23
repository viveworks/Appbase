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

package com.viveworks.appbase.core.service.exception.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.ExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.ExceptionStore;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.ReactiveExceptionStore;
import com.viveworks.appbase.core.service.exception.store.SchedulerOffloadReactiveExceptionStore;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStore;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.lucene.LuceneExceptionStore;
import com.viveworks.appbase.core.service.exception.store.lucene.ReactiveLuceneExceptionStore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.read.DefaultReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundaryConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
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
 * Default implementation of {@link ExceptionAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings("checkstyle:IllegalCatch")
public class DefaultExceptionAccessor implements ExceptionAccessor {
    private final Logger logger;
    private final LogService logService;
    private final MetricService metricService;
    private final MediatorService mediatorService;
    private final ValidationService validationService;
    private final ExceptionMessageContextCodec exceptionMessageContextCodec;
    private final Map<String, ExceptionStore> exceptionStoreByTypeNameMap;
    private final Map<String, ExceptionStoreConfiguration> exceptionStoreConfigurationByTypeNameMap;
    private final Map<String, ExceptionStoreConfiguration> exceptionStoreConfigurationByExceptionStoreTypeNameMap;
    private final Map<String, ExceptionStoreFactory<? extends ExceptionStore, ? extends ExceptionStoreConfiguration>>
            exceptionStoreFactoryByConfigurationTypeNameMap;
    private final Gauge<Integer> exceptionStoreByTypeNameMapSizeGauge;
    private final Gauge<Integer> exceptionStoreConfigurationByTypeNameMapSizeGauge;
    private final Gauge<Integer> exceptionStoreConfigurationByExceptionStoreTypeNameMapSizeGauge;
    private final Gauge<Integer> exceptionStoreFactoryByConfigurationTypeNameMapSizeGauge;
    private final Map<String, Counter> reloadErrorCounterByStoreTypeNameMap;
    private final Map<String, Timer> timerByExceptionStoreTypeNameMap;
    private final Map<String, Counter> errorCounterByExceptionStoreTypeNameMap;
    private final ReactiveReadBoundary reactiveReadBoundary;

    private final AtomicBoolean shutdown;

    public DefaultExceptionAccessor(final LogService logService,
                                    final MetricService metricService,
                                    final MediatorService mediatorService,
                                    final ValidationService validationService,
                                    final ConfigurationService configurationService,
                                    final ExceptionMessageContextCodec exceptionMessageContextCodec)
    {
        this(logService, metricService, mediatorService, validationService, configurationService,
                exceptionMessageContextCodec,
                new DefaultReactiveReadBoundary(ReactiveReadBoundaryConfiguration.builder().build(), null));
    }

    public DefaultExceptionAccessor(final LogService logService,
                                    final MetricService metricService,
                                    final MediatorService mediatorService,
                                    final ValidationService validationService,
                                    final ConfigurationService configurationService,
                                    final ExceptionMessageContextCodec exceptionMessageContextCodec,
                                    final ReactiveReadBoundary reactiveReadBoundary)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(configurationService, "configurationService");
        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");

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

        final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration =
                defaultExceptionServiceConfiguration.getDefaultExceptionAccessorConfiguration();
        if (defaultExceptionAccessorConfiguration == null) {
            throw new AppbaseRuntimeException(DefaultExceptionAccessorConfiguration.class.getName() +
                    " is null in " + DefaultExceptionServiceConfiguration.class.getName() + " in " +
                    ConfigurationService.class.getName());
        }
        AppbaseUtil.validate(validationService, defaultExceptionAccessorConfiguration);

        this.logService = logService;
        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
        this.mediatorService = mediatorService;
        this.validationService = validationService;
        this.exceptionMessageContextCodec = exceptionMessageContextCodec;
        this.reactiveReadBoundary = reactiveReadBoundary;
        this.timerByExceptionStoreTypeNameMap = new ConcurrentHashMap<>();
        this.errorCounterByExceptionStoreTypeNameMap = new ConcurrentHashMap<>();

        this.exceptionStoreByTypeNameMap = new ConcurrentHashMap<>();
        this.exceptionStoreByTypeNameMapSizeGauge = exceptionStoreByTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "exceptionStoreByTypeNameMap", "size", "gauge"),
                        this.exceptionStoreByTypeNameMapSizeGauge);

        this.exceptionStoreFactoryByConfigurationTypeNameMap = new ConcurrentHashMap<>();
        this.exceptionStoreFactoryByConfigurationTypeNameMapSizeGauge =
                exceptionStoreFactoryByConfigurationTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), "exceptionStoreFactoryByConfigurationTypeNameMap", "size", "gauge"),
                        exceptionStoreFactoryByConfigurationTypeNameMapSizeGauge);

        this.exceptionStoreConfigurationByTypeNameMap = new ConcurrentHashMap<>();
        this.exceptionStoreConfigurationByTypeNameMapSizeGauge = exceptionStoreConfigurationByTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(getClass(), "exceptionStoreConfigurationByTypeNameMap", "size", "gauge"),
                        exceptionStoreConfigurationByTypeNameMapSizeGauge);

        this.exceptionStoreConfigurationByExceptionStoreTypeNameMap = new ConcurrentHashMap<>();
        this.exceptionStoreConfigurationByExceptionStoreTypeNameMapSizeGauge =
                exceptionStoreConfigurationByExceptionStoreTypeNameMap::size;
        metricService.getMetricRegistry()
                .register(
                        metricService.name(
                                getClass(), "exceptionStoreConfigurationByExceptionStoreTypeNameMap", "size", "gauge"),
                        this.exceptionStoreConfigurationByExceptionStoreTypeNameMapSizeGauge);

        final List<ExceptionStoreConfiguration> exceptionStoreConfigurations =
                defaultExceptionAccessorConfiguration.getExceptionStoreConfigurations();
        try {
            initExceptionStoreConfigurationByTypeNameMap(exceptionStoreConfigurations);
        } catch (ExceptionStoreFactoryException esfe) {
            throw new AppbaseRuntimeException("Error while initializing exception stores", esfe);
        }

        this.reloadErrorCounterByStoreTypeNameMap = new ConcurrentHashMap<>();
        this.shutdown = new AtomicBoolean(false);
    }

    private void initExceptionStoreConfigurationByTypeNameMap(
            final List<ExceptionStoreConfiguration> exceptionStoreConfigurations)
            throws ExceptionStoreFactoryException
    {
        this.exceptionStoreConfigurationByExceptionStoreTypeNameMap.clear();

        if (exceptionStoreConfigurations != null && !exceptionStoreConfigurations.isEmpty()) {
            for (final ExceptionStoreConfiguration exceptionStoreConfiguration : exceptionStoreConfigurations) {
                AppbaseUtil.validate(validationService, exceptionStoreConfiguration);

                final String exceptionStoreConfigurationTypeName = exceptionStoreConfiguration.getClass().getName();

                logger.info("Registering exception store configuration of type {}: {}",
                        exceptionStoreConfigurationTypeName, exceptionStoreConfiguration);
                this.exceptionStoreConfigurationByTypeNameMap.put(
                        exceptionStoreConfiguration.getClass().getName(), exceptionStoreConfiguration);
                this.exceptionStoreConfigurationByExceptionStoreTypeNameMap.put(
                        exceptionStoreConfiguration.getExceptionStoreType().getName(), exceptionStoreConfiguration);

                if (exceptionStoreConfiguration instanceof JpaExceptionStoreConfiguration) {
                    final JpaExceptionStoreFactory jpaExceptionStoreFactory =
                            new JpaExceptionStoreFactory(
                                    logService,
                                    metricService,
                                    mediatorService,
                                    validationService,
                                    exceptionMessageContextCodec);
                    registerExceptionStoreFactoryByConfigurationType(
                            JpaExceptionStoreConfiguration.class, jpaExceptionStoreFactory);
                }

                if (exceptionStoreConfiguration instanceof InMemoryExceptionStoreConfiguration) {
                    final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration =
                            (InMemoryExceptionStoreConfiguration) exceptionStoreConfiguration;
                    final StackTraceHashGenerator stackTraceHashGenerator =
                            new DefaultStackTraceHashGenerator(
                                    inMemoryExceptionStoreConfiguration.getStackTraceSerializationMaxLength());
                    final InMemoryExceptionStoreFactory inMemoryExceptionStoreFactory =
                            new InMemoryExceptionStoreFactory(stackTraceHashGenerator, exceptionMessageContextCodec);
                    registerExceptionStoreFactoryByConfigurationType(
                            InMemoryExceptionStoreConfiguration.class, inMemoryExceptionStoreFactory);
                }
            }
        }
    }

    @Override
    public Map<String, ExceptionMessageDetails> readExceptionMessageDetailsById(
            final Integer id) throws ExceptionAccessorException
    {
        AppbaseUtil.notNull(id, "id");

        final Map<String, ExceptionMessageDetails> result = new ConcurrentHashMap<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "readExceptionMessageDetailsById"));
                final Timer.Context context = timer.time();
                try {
                    final ExceptionMessageDetails exceptionMessageDetails = exceptionStore.readExceptionMessageDetailsById(id);
                    if (exceptionMessageDetails != null) {
                        result.put(exceptionStoreTypeName, exceptionMessageDetails);
                    }
                } catch (Exception ex) {
                    logger.error("Error while 'readExceptionMessageDetailsById' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "readExceptionMessageDetailsById"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<ExceptionMessageDetails>> asyncReadExceptionMessageDetailsById(final Integer id) {
        final Map<String, CompletableFuture<ExceptionMessageDetails>> resultFutures = new ConcurrentHashMap<>();

        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncReadExceptionMessageDetailsById"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncReadExceptionMessageDetailsById(id));
                } catch (Exception ex) {
                    logger.error("Error while 'asyncReadExceptionMessageDetailsById' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "asyncReadExceptionMessageDetailsById"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Mono<Map<String, ExceptionMessageDetails>> readExceptionMessageDetailsByIdReactive(final Integer id) {
        AppbaseUtil.notNull(id, "id");

        return Flux.fromIterable(exceptionStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readExceptionMessageDetailsByIdReactive(id)
                        .map(details -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), details)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Map<String, Collection<ExceptionMessageDetails>>
        readAllExceptionMessageDetails() throws ExceptionAccessorException
    {
        final Map<String, Collection<ExceptionMessageDetails>> result = new ConcurrentHashMap<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "readAllExceptionMessageDetails"));
                final Timer.Context context = timer.time();
                try {
                    final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection =
                            exceptionStore.readAllExceptionMessageDetails();
                    if (exceptionMessageDetailsCollection != null) {
                        result.put(exceptionStoreTypeName, exceptionMessageDetailsCollection);
                    }
                } catch (Exception ex) {
                    logger.error("Error while 'readAllExceptionMessageDetails' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "readAllExceptionMessageDetails"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>> asyncReadAllExceptionMessageDetails() {
        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>> resultFutures =
                new ConcurrentHashMap<>();

        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncReadAllExceptionMessageDetails"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncReadAllExceptionMessageDetails());
                } catch (Exception ex) {
                    logger.error("Error while 'asyncReadAllExceptionMessageDetails' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "asyncReadAllExceptionMessageDetails"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });
        return resultFutures;
    }

    @Override
    public Mono<Map<String, Collection<ExceptionMessageDetails>>> readAllExceptionMessageDetailsReactive() {
        return Flux.fromIterable(exceptionStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readAllExceptionMessageDetailsReactive()
                        .map(details -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), details)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Map<String, Collection<ExceptionMessageDetails>> readAllExceptionMessageDetailsByTimestamps(
            final long inclusiveMinTimestamp,
            final long exclusiveMaxTimestamp)
            throws ExceptionAccessorException
    {
        final Map<String, Collection<ExceptionMessageDetails>> result = new ConcurrentHashMap<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f -> getTimer(
                                exceptionStoreTypeName,
                                "readAllExceptionMessageDetailsByTimestamps"));
                final Timer.Context context = timer.time();
                try {
                    final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection =
                            exceptionStore.readAllExceptionMessageDetailsByTimestamps(
                                    inclusiveMinTimestamp, exclusiveMaxTimestamp);
                    if (exceptionMessageDetailsCollection != null) {
                        result.put(
                                exceptionStoreTypeName,
                                exceptionMessageDetailsCollection);
                    }
                } catch (Exception ex) {
                    logger.error("Error while 'readAllExceptionMessageDetailsByTimestamps' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f -> getErrorCounter(
                                    exceptionStoreTypeName,
                                    "readAllExceptionMessageDetailsByTimestamps"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
    asyncReadAllExceptionMessageDetailsByTimestamps(final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        AppbaseUtil.positive(inclusiveMinTimestamp, "beforeTimestamp");
        AppbaseUtil.positive(exclusiveMaxTimestamp, "afterTimestamp");

        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>> resultFutures = 
                new ConcurrentHashMap<>();

        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncReadAllExceptionMessageDetailsByTimestamps"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncReadAllExceptionMessageDetailsByTimestamps(
                                    inclusiveMinTimestamp, exclusiveMaxTimestamp));
                } catch (Exception ex) {
                    logger.error("Error while 'asyncReadAllExceptionMessageDetailsByTimestamps' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(
                                            exceptionStoreTypeName, "asyncReadAllExceptionMessageDetailsByTimestamps"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });
        return resultFutures;
    }

    @Override
    public Mono<Map<String, Collection<ExceptionMessageDetails>>> readAllExceptionMessageDetailsByTimestampsReactive(
            final long inclusiveMinTimestamp, final long exclusiveMaxTimestamp)
    {
        return Flux.fromIterable(exceptionStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readAllExceptionMessageDetailsByTimestampsReactive(
                                inclusiveMinTimestamp, exclusiveMaxTimestamp)
                        .map(details -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), details)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Map<String, Collection<String>> readUniqueExceptionTypeNames() throws ExceptionAccessorException {
        final Map<String, Collection<String>> result = new ConcurrentHashMap<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f -> getTimer(
                                exceptionStoreTypeName,
                                "readUniqueExceptionTypeNames"));
                final Timer.Context context = timer.time();
                try {
                    final Collection<String> uniqueThrowableTypeNames = exceptionStore.readUniqueThrowableTypeNames();
                    if (uniqueThrowableTypeNames != null) {
                        result.put(exceptionStoreTypeName, uniqueThrowableTypeNames);
                    }
                } catch (Exception ex) {
                    logger.error("Error while 'readUniqueExceptionTypeNames' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f -> getErrorCounter(
                                    exceptionStoreTypeName,
                                    "readUniqueExceptionTypeNames"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<String>>> asyncReadUniqueExceptionTypeNames() {
        final Map<String, CompletableFuture<Collection<String>>> resultFutures = new ConcurrentHashMap<>();

        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncReadUniqueExceptionTypeNames"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncReadUniqueThrowableTypeNames());
                } catch (Exception ex) {
                    logger.error("Error while 'asyncReadUniqueExceptionTypeNames' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "asyncReadUniqueExceptionTypeNames"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Mono<Map<String, Collection<String>>> readUniqueExceptionTypeNamesReactive() {
        return Flux.fromIterable(exceptionStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readUniqueThrowableTypeNamesReactive()
                        .map(names -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), names)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Map<String, Collection<ExceptionMessageDetails>> readExceptionMessageDetailsByThrowableTypeName(
            final String throwableTypeName) throws ExceptionAccessorException
    {
        final Map<String, Collection<ExceptionMessageDetails>> result = new ConcurrentHashMap<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f -> getTimer(
                                exceptionStoreTypeName,
                                "readExceptionMessageDetailsByThrowableTypeName"));
                final Timer.Context context = timer.time();
                try {
                    final Collection<ExceptionMessageDetails> exceptionMessageDetailsCollection =
                            exceptionStore.readExceptionMessageDetailsByThrowableTypeName(throwableTypeName);
                    result.put(exceptionStoreTypeName, exceptionMessageDetailsCollection);
                } catch (Exception ex) {
                    logger.error("Error while 'readExceptionMessageDetailsByThrowableTypeName' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f -> getErrorCounter(
                                    exceptionStoreTypeName,
                                    "readExceptionMessageDetailsByThrowableTypeName"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>>
        asyncReadExceptionMessageDetailsByThrowableTypeName(final String throwableTypeName)
    {
        AppbaseUtil.notBlank(throwableTypeName, "exceptionTypeName");

        final Map<String, CompletableFuture<Collection<ExceptionMessageDetails>>> resultFutures =
                new ConcurrentHashMap<>();

        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncReadExceptionMessageDetailsByThrowableTypeName"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncReadExceptionMessageDetailsByThrowableTypeName(throwableTypeName));
                } catch (Exception ex) {
                    logger.error("Error while 'asyncReadExceptionMessageDetailsByThrowableTypeName' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(
                                            exceptionStoreTypeName, "asyncReadExceptionMessageDetailsByThrowableTypeName"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Mono<Map<String, Collection<ExceptionMessageDetails>>>
        readExceptionMessageDetailsByThrowableTypeNameReactive(final String throwableTypeName)
    {
        return Flux.fromIterable(exceptionStoreByTypeNameMap.values())
                .filter(Objects::nonNull)
                .flatMap(store -> reactiveViewOf(store)
                        .readExceptionMessageDetailsByThrowableTypeNameReactive(throwableTypeName)
                        .map(details -> new AbstractMap.SimpleEntry<>(store.getClass().getName(), details)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Build the {@link ReactiveExceptionStore} view of an {@link ExceptionStore} — a {@link
     * LuceneExceptionStore} routes through {@link ReactiveLuceneExceptionStore} (cooperative cancellation
     * over its Lucene fetch loops); any other {@link ExceptionStore} (JPA, in-memory) routes through {@link
     * SchedulerOffloadReactiveExceptionStore}, offloading its blocking sync reads onto {@link
     * #reactiveReadBoundary}. Built fresh on every call rather than cached: the wrappers are trivial (1-2
     * fields), and caching by {@link ExceptionStore} instance would never be evicted by {@link
     * #unregisterExceptionStore}, {@link #clearExceptionStores} or {@link #reload}, retaining shut-down
     * store instances indefinitely. Mirrors {@code DefaultLogAccessor#reactiveViewOf}.
     *
     * @param exceptionStore Exception store to get the reactive view of
     * @return the exception store's reactive view
     */
    private ReactiveExceptionStore reactiveViewOf(final ExceptionStore exceptionStore) {
        return (exceptionStore instanceof LuceneExceptionStore)
                ? new ReactiveLuceneExceptionStore((LuceneExceptionStore) exceptionStore)
                : new SchedulerOffloadReactiveExceptionStore(exceptionStore, reactiveReadBoundary);
    }

    @Override
    public Collection<String> writeExceptionMessage(
            final ExceptionMessage exceptionMessage) throws ExceptionAccessorException
    {
        final Collection<String> result = new ArrayList<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f -> getTimer(
                                exceptionStoreTypeName,
                                "writeExceptionMessage"));
                final Timer.Context context = timer.time();
                try {
                    exceptionStore.writeExceptionMessage(exceptionMessage);
                    result.add(exceptionStoreTypeName);
                } catch (Exception ex) {
                    logger.error("Error while 'writeExceptionMessage' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f -> getErrorCounter(
                                    exceptionStoreTypeName,
                                    "writeExceptionMessage"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncWriteExceptionMessage(
            final ExceptionMessage exceptionMessage)
    {
        AppbaseUtil.notNull(exceptionMessage, "exceptionMessage");

        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();
        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncWriteExceptionMessage"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncWriteExceptionMessage(exceptionMessage));
                } catch (Exception ex) {
                    logger.error("Error while 'asyncWriteExceptionMessage' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "asyncWriteExceptionMessage"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Collection<String> removeExceptionMessageDetailsById(
            final int id) throws ExceptionAccessorException
    {
        final Collection<String> result = new ArrayList<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f -> getTimer(
                                exceptionStoreTypeName,
                                "removeExceptionMessageDetailsById"));
                final Timer.Context context = timer.time();
                try {
                    exceptionStore.removeExceptionMessageDetailsById(id);
                    result.add(exceptionStoreTypeName);
                } catch (Exception ex) {
                    logger.error("Error while 'removeExceptionMessageDetailsById' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f -> getErrorCounter(
                                    exceptionStoreTypeName,
                                    "removeExceptionMessageDetailsById"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncRemoveExceptionMessageDetailsById(final int id) {
        AppbaseUtil.positive(id, "id");

        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();
        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncRemoveExceptionMessageDetailsById"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncRemoveExceptionMessageDetailsById(id));
                } catch (Exception ex) {
                    logger.error("Error while 'asyncRemoveExceptionMessageDetailsById' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(exceptionStoreTypeName, "asyncRemoveExceptionMessageDetailsById"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    @Override
    public Collection<String> removeExceptionMessageDetailsExclusiveBefore(
            final long timestamp) throws ExceptionAccessorException
    {
        final Collection<String> result = new ArrayList<>();

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f -> getTimer(
                                exceptionStoreTypeName,
                                "removeExceptionMessageDetailsExclusiveBefore"));
                final Timer.Context context = timer.time();
                try {
                    exceptionStore.removeExceptionMessageDetailsExclusiveBefore(timestamp);
                    result.add(exceptionStoreTypeName);
                } catch (Exception ex) {
                    logger.error("Error while 'removeExceptionMessageDetailsExclusiveBefore' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f -> getErrorCounter(
                                    exceptionStoreTypeName,
                                    "removeExceptionMessageDetailsExclusiveBefore"))
                            .inc();
                    throw new ExceptionAccessorException(ex);
                } finally {
                    context.stop();
                }
            }
        }

        return new CopyOnWriteArrayList<>(result);
    }

    @Override
    public Map<String, CompletableFuture<Void>> asyncRemoveExceptionMessageDetailsExclusiveBefore(final long timestamp) {
        AppbaseUtil.positive(timestamp, "timestamp");

        final Map<String, CompletableFuture<Void>> resultFutures = new ConcurrentHashMap<>();
        exceptionStoreByTypeNameMap.values().forEach(exceptionStore -> {
            if (exceptionStore != null) {
                final String exceptionStoreTypeName = exceptionStore.getClass().getName();
                final Timer timer = timerByExceptionStoreTypeNameMap
                        .computeIfAbsent(exceptionStoreTypeName, f ->
                                getTimer(exceptionStoreTypeName, "asyncRemoveExceptionMessageDetailsExclusiveBefore"));
                final Timer.Context context = timer.time();
                try {
                    resultFutures.put(
                            exceptionStoreTypeName,
                            exceptionStore.asyncRemoveExceptionMessageDetailsExclusiveBefore(timestamp));
                } catch (Exception ex) {
                    logger.error("Error while 'asyncRemoveExceptionMessageDetailsExclusiveBefore' using {}",
                            exceptionStoreTypeName, ex);
                    errorCounterByExceptionStoreTypeNameMap
                            .computeIfAbsent(exceptionStoreTypeName, f ->
                                    getErrorCounter(
                                            exceptionStoreTypeName, "asyncRemoveExceptionMessageDetailsExclusiveBefore"))
                            .inc();
                } finally {
                    context.stop();
                }
            }
        });

        return resultFutures;
    }

    private Timer getTimer(final String exceptionStoreTypeName, final String methodName) {
        return metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), exceptionStoreTypeName, methodName, "timer"));
    }

    private Counter getErrorCounter(final String exceptionStoreTypeName, final String methodName) {
        return metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), exceptionStoreTypeName, methodName, "error", "counter"));
    }

    @Unmodifiable
    @Override
    public Map<String, ExceptionStoreFactory> getExceptionStoreFactoryByConfigurationTypeNameMap()
    {
        return Collections.unmodifiableMap(exceptionStoreFactoryByConfigurationTypeNameMap);
    }

    @Override
    public void registerExceptionStoreFactoryByConfigurationType(
            final Class<? extends ExceptionStoreConfiguration> exceptionStoreConfigurationType,
            final ExceptionStoreFactory exceptionStoreFactory)
            throws ExceptionStoreFactoryException
    {
        AppbaseUtil.notNull(exceptionStoreConfigurationType, "exceptionStoreConfigurationType");
        AppbaseUtil.notNull(exceptionStoreFactory, "exceptionStoreFactory");

        logger.info("Registering exception store factory {} for configuration type {}",
                exceptionStoreFactory.getClass().getName(), exceptionStoreConfigurationType);
        exceptionStoreFactoryByConfigurationTypeNameMap.put(
                exceptionStoreConfigurationType.getName(), exceptionStoreFactory);

        final ExceptionStoreConfiguration exceptionStoreConfiguration =
                exceptionStoreConfigurationByTypeNameMap.get(exceptionStoreConfigurationType.getName());
        if (exceptionStoreConfiguration != null && exceptionStoreConfiguration.isEnabled()) {
            @SuppressWarnings("unchecked")
            final ExceptionStoreFactory<ExceptionStore, ExceptionStoreConfiguration> exceptionStoreFactory1 =
                    (ExceptionStoreFactory<ExceptionStore, ExceptionStoreConfiguration>) exceptionStoreFactory;
            final ExceptionStore exceptionStore =
                    exceptionStoreFactory1.newExceptionStore(exceptionStoreConfiguration);
            registerExceptionStore(exceptionStore);
        }
    }

    @Override
    public void unregisterExceptionStoreFactoryByConfigurationType(
            final Class<? extends ExceptionStoreConfiguration> exceptionStoreConfigurationType,
            final Class<? extends ExceptionStoreFactory>
                    exceptionStoreFactoryType)
    {
        AppbaseUtil.notNull(exceptionStoreConfigurationType, "exceptionStoreConfigurationType");
        AppbaseUtil.notNull(exceptionStoreFactoryType, "exceptionStoreFactoryType");

        logger.info("Unregistering exception store factory type [{}] for configuration type [{}]",
                exceptionStoreFactoryType, exceptionStoreConfigurationType);
        final String exceptionStoreConfigurationTypeName = exceptionStoreConfigurationType.getName();
        exceptionStoreFactoryByConfigurationTypeNameMap.remove(exceptionStoreConfigurationTypeName);
        exceptionStoreConfigurationByTypeNameMap.remove(exceptionStoreConfigurationTypeName);
    }

    @Override
    public void clearExceptionStoreFactoryByConfigurationTypeMap() {
        logger.info("Clearing exceptionStoreConfigurationByTypeNameMap");
        exceptionStoreFactoryByConfigurationTypeNameMap.clear();
        exceptionStoreConfigurationByTypeNameMap.clear();
    }

    @Unmodifiable
    @Override
    public Collection<ExceptionStore> getExceptionStores() {
        return Collections.unmodifiableCollection(exceptionStoreByTypeNameMap.values());
    }

    @Override
    public void registerExceptionStore(final ExceptionStore exceptionStore) {
        AppbaseUtil.notNull(exceptionStore, "exceptionStore");

        final String exceptionStoreTypeName = exceptionStore.getClass().getName();
        logger.info("Registering exception store: {}", exceptionStoreTypeName);
        exceptionStoreByTypeNameMap.put(exceptionStoreTypeName, exceptionStore);
    }

    @Override
    public void unregisterExceptionStore(final Class<? extends ExceptionStore> exceptionStoreType) {
        AppbaseUtil.notNull(exceptionStoreType, "exceptionStoreType");
        logger.info("Unregistering exception store {}", exceptionStoreType);

        final String exceptionStoreTypeName = exceptionStoreType.getName();
        exceptionStoreByTypeNameMap.remove(exceptionStoreTypeName);

        exceptionStoreConfigurationByExceptionStoreTypeNameMap.forEach(
                (currentExceptionStoreTypeName, currentExceptionStoreConfiguration) -> {
                    if (currentExceptionStoreTypeName.equals(exceptionStoreTypeName)) {
                        exceptionStoreConfigurationByExceptionStoreTypeNameMap.remove(currentExceptionStoreTypeName);
                    }
                }
        );
    }

    @Override
    public void clearExceptionStores() {
        logger.info("Clearing exceptionStoreByTypeNameMap");
        exceptionStoreByTypeNameMap.clear();
    }

    /**
     * Re-configure exception store, specifically {@link JpaExceptionStore}.
     *
     * @param exceptionAccessorConfiguration New configuration
     */
    @Override
    public void reload(
            final ExceptionAccessorConfiguration exceptionAccessorConfiguration) throws ExceptionAccessorException
    {
        AppbaseUtil.notNull(exceptionAccessorConfiguration, "exceptionAccessorConfiguration");

        if (!(exceptionAccessorConfiguration instanceof DefaultExceptionAccessorConfiguration)) {
            throw new ExceptionAccessorException("exceptionAccessorConfiguration must be of type " +
                    DefaultExceptionAccessorConfiguration.class.getName());
        }

        final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration =
                (DefaultExceptionAccessorConfiguration) exceptionAccessorConfiguration;
        AppbaseUtil.validate(validationService, defaultExceptionAccessorConfiguration);

        logger.info("Reloading {} with {}", getClass().getName(), defaultExceptionAccessorConfiguration);
        final List<ExceptionStoreConfiguration> exceptionStoreConfigurations =
                defaultExceptionAccessorConfiguration.getExceptionStoreConfigurations();
        initExceptionStoreConfigurationByTypeNameMap(exceptionStoreConfigurations);

        exceptionStoreByTypeNameMap.forEach((exceptionStoreTypeName, exceptionStore) -> {
            final ExceptionStoreConfiguration exceptionStoreConfiguration =
                    exceptionStoreConfigurationByExceptionStoreTypeNameMap.get(exceptionStoreTypeName);
            if (exceptionStoreConfiguration != null && exceptionStoreConfiguration.isEnabled()) {
                @SuppressWarnings("unchecked")
                final ExceptionStoreFactory<ExceptionStore, ExceptionStoreConfiguration> exceptionStoreFactory =
                        (ExceptionStoreFactory<ExceptionStore, ExceptionStoreConfiguration>)
                                exceptionStoreFactoryByConfigurationTypeNameMap.get(
                                exceptionStoreConfiguration.getClass().getName());
                if (exceptionStoreFactory != null) {
                    try {
                        final ExceptionStore newExceptionStore =
                                exceptionStoreFactory.newExceptionStore(exceptionStoreConfiguration);
                        logger.info("Replacing exception store: {}", exceptionStoreTypeName);
                        exceptionStoreByTypeNameMap.replace(exceptionStoreTypeName, newExceptionStore);
                        exceptionStore.shutdown();
                    } catch (ExceptionStoreFactoryException esfe) {
                        logger.error("Error while reloading exception store {}", exceptionStoreTypeName, esfe);
                        reloadErrorCounterByStoreTypeNameMap
                                .computeIfAbsent(exceptionStoreTypeName, f -> {
                                    return metricService
                                            .getMetricRegistry()
                                            .counter(metricService.name(
                                                    getClass(), exceptionStoreTypeName, "error", "counter"));
                                })
                                .inc();
                    }
                }
            }
        });
    }

    @Override
    public void synchronizeExceptionStores() throws ExceptionAccessorException {
        for (final Map.Entry<String, ExceptionStore> entry : exceptionStoreByTypeNameMap.entrySet()) {
            final ExceptionStore exceptionStore = entry.getValue();
            logger.info("Synchronizing exception store: {}", exceptionStore.getClass().getName());
            exceptionStore.synchronize();
        }
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        for (final ExceptionStore exceptionStore : exceptionStoreByTypeNameMap.values()) {
            try {
                exceptionStore.shutdown();
            } catch (Exception ex) {
                logger.error("Error shutting down {}", exceptionStore.getClass().getName(), ex);
            }
        }

        clearExceptionStores();
        clearExceptionStoreFactoryByConfigurationTypeMap();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    public Gauge<Integer> getExceptionStoreByTypeNameMapSizeGauge() {
        return exceptionStoreByTypeNameMapSizeGauge;
    }

    public Gauge<Integer> getExceptionStoreFactoryByConfigurationTypeNameMapSizeGauge() {
        return exceptionStoreFactoryByConfigurationTypeNameMapSizeGauge;
    }

    public Gauge<Integer> getExceptionStoreConfigurationByExceptionStoreTypeNameMapSizeGauge() {
        return exceptionStoreConfigurationByExceptionStoreTypeNameMapSizeGauge;
    }

    public Gauge<Integer> getExceptionStoreConfigurationByTypeNameMapSizeGauge() {
        return exceptionStoreConfigurationByTypeNameMapSizeGauge;
    }
}
