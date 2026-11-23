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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodecException;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.configuration.accessor.store.ReactiveConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.SchedulerOffloadReactiveConfigurationStore;
import com.viveworks.appbase.core.service.configuration.microprofile.ConfigSourceAdapter;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.read.DefaultReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundaryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link ConfigurationAccessor}.
 *
 * <b>NOTE:</b> This class must be shutdown explicitly. The
 * {@link com.viveworks.appbase.core.service.configuration.DefaultConfigurationService} does not shut this down,
 * because it is not possible to restart it when it reloads.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = {"IMC_IMMATURE_CLASS_NO_TOSTRING", "CRLF_INJECTION_LOGS"})
public class DefaultConfigurationAccessor extends AbstractConfigurationAccessorRegistry implements ConfigurationAccessor {
    private final Logger logger;
    private final ConfigurationAccessorContext configurationAccessorContext;

    private final Histogram readSizeHistogram;
    private final Histogram readBytesSizeHistogram;
    private final Timer readTimer;
    private final Histogram writeSizeHistogram;
    private final Histogram writeBytesSizeHistogram;
    private final Timer writeTimer;
    private final Timer shutdownTimer;

    private final Gauge configurationCodecByMimeTypeMappingSizeGauge;
    private final Gauge configurationTypeByRawConfigurationNameMappingSizeGauge;
    private final Gauge configurationStoreByIdMappingSizeGauge;
    private final Gauge writingConfigurationStoreIdsByKeyMappingSizeGauge;

    private final AtomicReference<ServiceStatus> statusRef;
    private final ReentrantLock lifecycleReentrantLock;

    private final ReactiveReadBoundary reactiveReadBoundary;

    @Inject
    public DefaultConfigurationAccessor(
            final LogService logService,
            final MetricService metricService,
            final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap,
            final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMap,
            final Collection<ConfigurationStore> configurationStores,
            final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap)
    {
        this(logService,
                metricService,
                configurationCodecByMimeTypeMap,
                configurationTypeByRawConfigurationNameMap,
                configurationStores,
                writingConfigurationStoreIdsByKeyMap,
                new DefaultReactiveReadBoundary(ReactiveReadBoundaryConfiguration.builder().build(), null));
    }

    public DefaultConfigurationAccessor(
            final LogService logService,
            final MetricService metricService,
            final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap,
            final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMap,
            final Collection<ConfigurationStore> configurationStores,
            final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap,
            final ReactiveReadBoundary reactiveReadBoundary)
    {
        super(configurationCodecByMimeTypeMap,
                configurationTypeByRawConfigurationNameMap,
                configurationStores,
                writingConfigurationStoreIdsByKeyMap);

        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");

        this.reactiveReadBoundary = reactiveReadBoundary;
        this.logger = logService.getLogger(getClass());
        logger.info("Initializing {}", getClass().getName());


        this.configurationAccessorContext = new ConfigurationAccessorContext(
                this.configurationCodecByMimeTypeMap,
                this.configurationTypeByRawConfigurationNameMap,
                this.writingConfigurationStoreIdsByKeyMap);

        this.readSizeHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "read", "size", "histogram"));
        this.readBytesSizeHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "readBytes", "size", "histogram"));
        this.readTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "read", "timer"));
        this.writeSizeHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "write", "size", "histogram"));
        this.writeBytesSizeHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "writeBytes", "size", "histogram"));
        this.writeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "write", "timer"));
        this.shutdownTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "shutdown", "timer"));


        this.configurationStoreByIdMappingSizeGauge = configurationStoreByIdMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "configurationStoreByClassNameMapping", "size", "gauge"),
                this.configurationStoreByIdMappingSizeGauge);
        this.writingConfigurationStoreIdsByKeyMappingSizeGauge = writingConfigurationStoreIdsByKeyMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "writingConfigurationStoreTypesByKeyMapping", "size", "gauge"),
                this.writingConfigurationStoreIdsByKeyMappingSizeGauge);
        this.configurationCodecByMimeTypeMappingSizeGauge = configurationCodecByMimeTypeMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "configurationCodecByMimeTypeMapping", "size", "gauge"),
                this.configurationCodecByMimeTypeMappingSizeGauge);
        this.configurationTypeByRawConfigurationNameMappingSizeGauge = configurationTypeByRawConfigurationNameMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(
                        getClass(), "configurationTypeByRawConfigurationNameMapping", "size", "gauge"),
                this.configurationTypeByRawConfigurationNameMappingSizeGauge);


        this.lifecycleReentrantLock = new ReentrantLock();
        this.statusRef = new AtomicReference<>(ServiceStatus.STARTED);
    }

    @Override
    public Collection<Configuration> readConfigurations() throws ConfigurationAccessorException {
        logger.debug("Reading configurations");

        final Timer.Context context = readTimer.time();
        try {
            logger.debug("Reading raw configurations from all stores: {}", configurationStoreByIdMap);
            logger.debug("Reading raw configurations using context: {}", configurationAccessorContext);

            final Collection<Configuration> configurations = new ArrayList<>();
            final Collection<ConfigurationStore> configurationStores = configurationStoreByIdMap.values();
            for (final ConfigurationStore configurationStore : configurationStores) {
                logger.debug("Reading raw configurations from configuration store: [type={}, id={}]",
                        configurationStore.getClass().getName(), configurationStore.getId());

                final Collection<RawConfiguration> rawConfigurations;
                try {
                    rawConfigurations = configurationStore.readRawConfigurations(configurationAccessorContext);
                } catch (ConfigurationStoreException cse) {
                    throw new ConfigurationAccessorException(
                            "Failed to load " + configurationStore.getClass().getName(), cse);
                }

                if (rawConfigurations != null) {
                    configurations.addAll(decodeConfigurations(rawConfigurations));

                    final AtomicInteger bytesCount = new AtomicInteger(0);
                    for (final RawConfiguration rawConfiguration : rawConfigurations) {
                        final byte[] value = rawConfiguration.getValue();
                        if (value != null) {
                            bytesCount.addAndGet(value.length);
                        }
                    }
                    readBytesSizeHistogram.update(bytesCount.get());

                    final Map<String, String> properties =
                            ConfigurationUtil.rawConfigurations2Properties(rawConfigurations);
                    final ConfigSourceAdapter configSourceAdapter = configSourceAdapterByIdMap
                            .computeIfAbsent(
                                    configurationStore.getId(),
                                    f -> new ConfigSourceAdapter(configurationStore, configurationAccessorContext));
                    configSourceAdapter.reload(properties);
                }
            }

            readSizeHistogram.update(configurations.size());
            return configurations;
        } finally {
            context.stop();
        }
    }

    @Override
    public Mono<Collection<Configuration>> readConfigurationsReactive() {
        return Flux.fromIterable(configurationStoreByIdMap.values())
                .filter(Objects::nonNull)
                .flatMap(configurationStore -> reactiveViewOf(configurationStore)
                        .readRawConfigurationsReactive(configurationAccessorContext)
                        .onErrorMap(ConfigurationStoreException.class, cse -> new ConfigurationAccessorException(
                                "Failed to load " + configurationStore.getClass().getName(), cse))
                        .flatMapMany(this::decodeToFlux))
                .collectList()
                .map(readConfigurations -> (Collection<Configuration>) readConfigurations);
    }

    /**
     * Build a fresh {@link ReactiveConfigurationStore} view of the given store on every call rather than
     * caching it: the wrapper holds only two fields, and caching by {@link ConfigurationStore} instance
     * would never be evicted on store unregister/clear, retaining shut-down store instances indefinitely
     * (mirrors {@code DefaultLogAccessor#reactiveViewOf}). Config is offload-only — there is no
     * Lucene-backed reactive variant to branch on, so this always returns a
     * {@link SchedulerOffloadReactiveConfigurationStore}.
     *
     * @param configurationStore Configuration store to get the reactive view of
     * @return the store's reactive view
     */
    private ReactiveConfigurationStore reactiveViewOf(final ConfigurationStore configurationStore) {
        return new SchedulerOffloadReactiveConfigurationStore(configurationStore, reactiveReadBoundary);
    }

    /**
     * Decode raw configurations into a {@link Flux}, surfacing a decode failure as a {@link Flux#error}
     * (the reactive counterpart of {@link #decodeConfigurations} throwing) so it propagates through the
     * reactive read's error channel rather than as a checked throw from a lambda.
     */
    private Flux<Configuration> decodeToFlux(final Collection<RawConfiguration> rawConfigurations) {
        try {
            return Flux.fromIterable(decodeConfigurations(rawConfigurations));
        } catch (ConfigurationAccessorException ex) {
            return Flux.error(ex);
        }
    }

    /**
     * Decode raw configurations into {@link Configuration}s using the registered per-mime-type codecs —
     * the pure decode shared by the blocking {@link #readConfigurations()} and the reactive
     * {@link #readConfigurationsReactive()} so the two cannot drift. Side-effects of the blocking read
     * (read-size histograms, microprofile config-source reload) are intentionally NOT here.
     */
    private Collection<Configuration> decodeConfigurations(final Collection<RawConfiguration> rawConfigurations)
            throws ConfigurationAccessorException
    {
        final Collection<Configuration> configurations = new ArrayList<>();
        if (rawConfigurations != null) {
            for (final RawConfiguration rawConfiguration : rawConfigurations) {
                final MimeType mimeType = rawConfiguration.getMimeType();
                final ConfigurationCodec configurationCodec = configurationCodecByMimeTypeMap.get(mimeType);
                if (configurationCodec == null) {
                    throw new ConfigurationAccessorException(
                            "No configuration codec registered for mime type " + mimeType);
                }

                try {
                    configurations.addAll(
                            configurationCodec.decode(List.of(rawConfiguration), configurationAccessorContext));
                } catch (ConfigurationCodecException cce) {
                    throw new ConfigurationAccessorException(
                            "Failed to decode " + rawConfiguration.getName(), cce);
                }
            }
        }
        return configurations;
    }

    @Override
    public void writeConfiguration(
            final ConfigurationKey configurationKey,
            final ConfigurationValue<?> configurationValue,
            final MimeType mimeType) throws ConfigurationAccessorException
    {
        AppbaseUtil.notNull(configurationKey, "configurationKey");
        AppbaseUtil.notNull(configurationValue, "configurationValue");
        AppbaseUtil.notNull(mimeType, "mimeType");

        logger.debug("Writing configuration with key: {}", configurationKey);
        final Timer.Context context = writeTimer.time();
        try {
            final ConfigurationCodec configurationCodec = configurationCodecByMimeTypeMap.get(mimeType);
            if (configurationCodec == null) {
                throw new ConfigurationAccessorException("No configuration codec found for mime type " + mimeType);
            }

            final Set<String> configurationStoreIds = writingConfigurationStoreIdsByKeyMap.get(configurationKey);
            if (configurationStoreIds == null) {
                throw new ConfigurationAccessorException(
                        "No configuration stores configured to write configuration key " + configurationKey);
            }

            for (final String configurationStoreId : configurationStoreIds) {
                final ConfigurationStore configurationStore = configurationStoreByIdMap.get(configurationStoreId);
                if (configurationStore == null) {
                    throw new ConfigurationAccessorException("No configuration store of type " + configurationStoreId);
                }

                final Collection<RawConfiguration> rawConfigurations;
                try {
                    rawConfigurations = configurationCodec.encode(
                            Map.of(configurationKey, configurationValue), configurationAccessorContext);
                } catch (ConfigurationCodecException cce) {
                    throw new ConfigurationAccessorException("Failed to encode " + configurationKey, cce);
                }
                writeSizeHistogram.update(rawConfigurations.size());

                final AtomicInteger bytesCount = new AtomicInteger(0);
                for (final RawConfiguration rawConfiguration : rawConfigurations) {
                    try {
                        if (configurationStore.supportsWrite()) {
                            configurationStore.writeRawConfiguration(rawConfiguration, configurationAccessorContext);
                        }
                    } catch (ConfigurationStoreException cse) {
                        throw new ConfigurationAccessorException(
                                "Failed to write raw configuration for key " + configurationKey, cse);
                    }

                    final byte[] value = rawConfiguration.getValue();
                    if (value != null) {
                        bytesCount.addAndGet(value.length);
                    }
                }
                writeBytesSizeHistogram.update(bytesCount.get());
            }
        } finally {
            context.stop();
        }
    }




    @Override
    public void registerConfigurationCodecByMimeType(final ConfigurationCodec configurationCodec, final MimeType mimeType) {
        AppbaseUtil.notNull(configurationCodec, "configurationCodec");
        AppbaseUtil.notNull(mimeType, "mimeType");

        logger.info("Registering configuration codec by mime type mapping: {} -> {}",
                mimeType.getValue(), configurationCodec.getClass().getName());

        super.registerConfigurationCodecByMimeType(configurationCodec, mimeType);
    }

    @Override
    public void unregisterConfigurationCodecByMimeType(final MimeType mimeType) {
        AppbaseUtil.notNull(mimeType, "mimeType");

        logger.info("Unregistering configuration codec for mime type: {}", mimeType);
        super.unregisterConfigurationCodecByMimeType(mimeType);
    }

    @Override
    public void clearConfigurationCodecByMimeTypeMap() {
        logger.info("Clearing configurationCodecByMimeTypeMap");

        super.clearConfigurationCodecByMimeTypeMap();
    }




    @Override
    public void registerConfigurationTypeByRawConfigurationName(
            final Class<? extends AppbaseConfiguration> configurationType,
            final String rawConfigurationNameMapping)
    {
        AppbaseUtil.notNull(configurationType, "configurationType");
        AppbaseUtil.notNull(rawConfigurationNameMapping, "rawConfigurationNameMapping");

        logger.info("Registering configuration type by raw configuration name mapping: {} -> {}",
                rawConfigurationNameMapping, configurationType.getName());
        super.registerConfigurationTypeByRawConfigurationName(configurationType, rawConfigurationNameMapping);
    }

    @Override
    public void unregisterConfigurationTypeByRawConfigurationName(final String rawConfigurationName) {
        AppbaseUtil.notNull(rawConfigurationName, "rawConfigurationName");

        logger.info("Unregistering configuration type by raw configuration name: {}", rawConfigurationName);
        super.unregisterConfigurationTypeByRawConfigurationName(rawConfigurationName);
    }

    @Override
    public void clearConfigurationTypeByRawConfigurationNameMap() {
        logger.info("Clearing configurationTypeByRawConfigurationNameMap");

        super.clearConfigurationTypeByRawConfigurationNameMap();
    }


    @Override
    public void registerWritingConfigurationStore(
            final ConfigurationStore configurationStore,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationStore, "configurationStore");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.info("Adding writing configuration store by key mapping: {} -> {}",
                configurationStore.getId(), configurationKey);
        super.registerWritingConfigurationStore(configurationStore, configurationKey);
    }

    @Override
    public void unregisterWritingConfigurationStore(
            final String configurationStoreId,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationStoreId, "configurationStoreId");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.info("Unregistering writing configuration store [id={}, configurationKey={}]",
                configurationStoreId, configurationKey);
        super.unregisterWritingConfigurationStore(configurationStoreId, configurationKey);
    }

    @Override
    public void unregisterWritingConfigurationStore(
            final Class<? extends ConfigurationStore> configurationStoreType,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationStoreType, "configurationStoreType");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.info("Unregistering writing configuration store [type={}, configurationKey={}]",
                configurationStoreType, configurationKey);
        super.unregisterWritingConfigurationStore(configurationStoreType, configurationKey);
    }

    @Override
    public void clearWritingConfigurationStores() {
        logger.info("Clearing writingConfigurationStoreIdsByKeyMap");

        super.clearWritingConfigurationStores();
    }




    @Override
    public void registerConfigurationStore(final ConfigurationStore configurationStore) {
        AppbaseUtil.notNull(configurationStore, "configurationStore");

        logger.info("Registering configuration store: [type={}, id={}]",
                configurationStore.getClass().getName(), configurationStore.getId());
        super.registerConfigurationStore(configurationStore);
    }

    @Override
    public void unregisterConfigurationStore(final String configurationStoreId) {
        AppbaseUtil.notNull(configurationStoreId, "configurationStoreId");

        logger.info("Unregistering configuration store by id={}", configurationStoreId);
        super.unregisterConfigurationStore(configurationStoreId);
    }

    @Override
    public void unregisterConfigurationStore(final Class<? extends ConfigurationStore> configurationStoreType) {
        AppbaseUtil.notNull(configurationStoreType, "configurationStoreType");

        logger.info("Unregistering configuration store by type={}", configurationStoreType.getName());
        super.unregisterConfigurationStore(configurationStoreType);
    }

    @Override
    public void clearConfigurationStores() {
        logger.info("Clearing configurationStoreByIdMap");

        super.clearConfigurationStores();
    }




    @Override
    public Iterable<ConfigSource> getConfigSources() {
        final Collection<ConfigSourceAdapter> configSources = configSourceAdapterByIdMap.values();
        return Collections.unmodifiableCollection(configSources);
    }




    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = {"MDM_WAIT_WITHOUT_TIMEOUT", "DRE_DECLARED_RUNTIME_EXCEPTION"})
    public void shutdown() throws AppbaseRuntimeException {
        if (statusRef.get() == ServiceStatus.SHUTTING_DOWN ||
                statusRef.get() == ServiceStatus.SHUTDOWN ||
                statusRef.get() == ServiceStatus.SHUTDOWN_FAILED)
        {
            return;
        }

        lifecycleReentrantLock.lock();
        Timer.Context context = null;
        try {
            context = shutdownTimer.time();
            logger.info("Shutting down {}", getClass().getName());

            boolean error = false;
            final StringBuilder exceptionMessage = new StringBuilder();
            final Collection<ConfigurationStore> configurationStores = configurationStoreByIdMap.values();
            for (final ConfigurationStore configurationStore : configurationStores) {
                try {
                    configurationStore.shutdown();
                } catch (AppbaseRuntimeException are) {
                    exceptionMessage.append(' ').append(are);
                    error = true;
                }
            }

            if (error) {
                logger.error("Error shutting down {}: {}", getClass().getName(), exceptionMessage);
            }

            super.shutdown();
        } catch (final Exception ex) {
            statusRef.set(ServiceStatus.SHUTDOWN_FAILED);
            logger.error("Error while shutting down service {}", getClass().getName(), ex);
        } finally {
            lifecycleReentrantLock.unlock();

            if (context != null) {
                context.stop();
            }
        }
    }

    public ServiceStatus getStatus() {
        return statusRef.get();
    }




    public Gauge getConfigurationCodecByMimeTypeMappingSizeGauge() {
        return configurationCodecByMimeTypeMappingSizeGauge;
    }

    public Gauge getConfigurationTypeByRawConfigurationNameMappingSizeGauge() {
        return configurationTypeByRawConfigurationNameMappingSizeGauge;
    }

    public Gauge getConfigurationStoreByIdMappingSizeGauge() {
        return configurationStoreByIdMappingSizeGauge;
    }

    public Gauge getWritingConfigurationStoreIdsByKeyMappingSizeGauge() {
        return writingConfigurationStoreIdsByKeyMappingSizeGauge;
    }
}
