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

package com.viveworks.appbase.core.service.metric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.jmx.registry.JmxMeterRegistryConfiguration;
import com.viveworks.appbase.core.service.metric.microprofile.jsonserializer.MetricRegistryJsonSerializer;
import com.viveworks.appbase.core.service.metric.microprofile.micrometer.MicroMetricRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.jmx.JmxMeterRegistry;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link MetricService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DefaultMetricService implements MetricService {
    private final Logger logger;
    private final AtomicReference<CompositeMeterRegistry> compositeMeterRegistryRef;
    private final AtomicReference<DefaultMetricServiceConfiguration> metricServiceConfigurationRef;
    private final Map<String, List<MeterRegistry>> meterRegistriesByTypeNameMap;
    private final MicroMetricRegistry metricRegistry;
    private final AtomicReference<ServiceStatus> statusRef;
    private final AtomicInteger reloadFailureCounter;
    private final AtomicInteger shutdownFailureCounter;
    private final Map<Long, Long> reloadTimeInMillisMap;
    private final Map<Long, Long> shutdownTimeInMillisMap;
    private final ReentrantLock lifecycleReentrantLock;
    private final AtomicBoolean jmxInitialized;

    @SuppressFBWarnings("PCOA_PARTIALLY_CONSTRUCTED_OBJECT_ACCESS")
    public DefaultMetricService(final LogService logService,
                                final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(defaultMetricServiceConfiguration, "defaultMetricServiceConfiguration");

        this.logger = logService.getLogger(getClass());

        final CompositeMeterRegistry compositeMeterRegistry = new CompositeMeterRegistry();

        this.compositeMeterRegistryRef = new AtomicReference<>();
        this.compositeMeterRegistryRef.set(compositeMeterRegistry);
        this.metricRegistry = new MicroMetricRegistry(compositeMeterRegistryRef.get());

        this.metricServiceConfigurationRef = new AtomicReference<>();
        this.metricServiceConfigurationRef.set(defaultMetricServiceConfiguration);

        this.meterRegistriesByTypeNameMap = new ConcurrentHashMap<>();

        this.jmxInitialized = new AtomicBoolean(false);
        this.reloadFailureCounter = new AtomicInteger(0);
        this.shutdownFailureCounter = new AtomicInteger(0);
        this.reloadTimeInMillisMap = new ConcurrentHashMap<>();
        this.shutdownTimeInMillisMap = new ConcurrentHashMap<>();
        this.lifecycleReentrantLock = new ReentrantLock();
        this.statusRef = new AtomicReference<>(ServiceStatus.STARTED);
    }

    public DefaultMetricService(final LogService logService,
                                final CompositeMeterRegistry compositeMeterRegistry,
                                final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(compositeMeterRegistry, "compositeMeterRegistry");
        AppbaseUtil.notNull(defaultMetricServiceConfiguration, "defaultMetricServiceConfiguration");

        this.logger = logService.getLogger(getClass());

        this.compositeMeterRegistryRef = new AtomicReference<>();
        this.compositeMeterRegistryRef.set(compositeMeterRegistry);
        this.metricRegistry = new MicroMetricRegistry(compositeMeterRegistryRef.get());
        if (defaultMetricServiceConfiguration.isEnableJvmAndSystemMetrics()) {
            this.metricRegistry.registerJvmAndSystemMetrics();
        }

        this.metricServiceConfigurationRef = new AtomicReference<>();
        this.metricServiceConfigurationRef.set(defaultMetricServiceConfiguration);

        this.meterRegistriesByTypeNameMap = new ConcurrentHashMap<>();

        this.jmxInitialized = new AtomicBoolean(false);
        this.reloadFailureCounter = new AtomicInteger(0);
        this.shutdownFailureCounter = new AtomicInteger(0);
        this.reloadTimeInMillisMap = new ConcurrentHashMap<>();
        this.shutdownTimeInMillisMap = new ConcurrentHashMap<>();
        this.lifecycleReentrantLock = new ReentrantLock();
        this.statusRef = new AtomicReference<>(ServiceStatus.STARTED);
    }

    /**
     * Initialize {@link JmxMeterRegistry}.
     *
     * <b>NOTE</b> Due to the static state of the MBean server, it can lead to deadlocks during unit tests.
     */
    public synchronized void initJmxMeterRegistryIfEnabled() {
        final JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration =
                metricServiceConfigurationRef.get().getJmxMeterRegistryConfiguration();
        if (jmxMeterRegistryConfiguration != null &&
                jmxMeterRegistryConfiguration.isEnabled() &&
                !jmxInitialized.get())
        {
            final JmxMeterRegistry jmxMeterRegistry =
                    new JmxMeterRegistry(jmxMeterRegistryConfiguration, Clock.SYSTEM);
            final CompositeMeterRegistry compositeMeterRegistry = compositeMeterRegistryRef.get();
            compositeMeterRegistry.remove(jmxMeterRegistry);

            final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            final String domain = jmxMeterRegistryConfiguration.domain();
            try {
                final Set<ObjectInstance> objectInstances =
                        platformMBeanServer.queryMBeans(new ObjectName(domain, "name", "*"), null);
                for (final ObjectInstance objectInstance : objectInstances) {
                    final ObjectName objectName = objectInstance.getObjectName();
                    platformMBeanServer.unregisterMBean(objectName);
                }
            } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanRegistrationException ex) {
                throw new AppbaseRuntimeException("Error while registering JmxMeterRegistry", ex);
            }

            compositeMeterRegistry.add(jmxMeterRegistry);
            jmxInitialized.set(true);
        }
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @Override
    public void registerExecutorServiceMetrics(
            final ExecutorService executorService, final String executorServiceName, final String... tagsKeyValue)
    {
        final Tags tags = Tags.of(tagsKeyValue);
        final Tag[] microProfileTags = MetricService.toMicroProfileTags(tags);
        final ExecutorServiceMetrics executorServiceMetrics =
                new ExecutorServiceMetrics(executorService, executorServiceName, tags);

        final CompositeMeterRegistry compositeMeterRegistry = compositeMeterRegistryRef.get();
        executorServiceMetrics.bindTo(compositeMeterRegistry);

        final Search executorCompletedSearch = compositeMeterRegistry.find("executor.completed").tags(tags);
        final FunctionCounter executorCompleted = executorCompletedSearch.functionCounter();
        if (executorCompleted != null) {
            final Gauge<Double> executorCompletedGauge = executorCompleted::count;
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.completed")
                    .build();
            metricRegistry.register(metadata, executorCompletedGauge, microProfileTags);
        }

        final Search executorActiveSearch = compositeMeterRegistry.find("executor.active").tags(tags);
        final io.micrometer.core.instrument.Gauge executorActive = executorActiveSearch.gauge();
        if (executorActive != null) {
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.active")
                    .build();
            final Gauge<Integer> executorActiveGauge = () -> (int) executorActive.value();
            metricRegistry.register(metadata, executorActiveGauge, microProfileTags);
        }

        final Search executorQueuedSearch = compositeMeterRegistry.find("executor.queued").tags(tags);
        final io.micrometer.core.instrument.Gauge executorQueued = executorQueuedSearch.gauge();
        if (executorQueued != null) {
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.queued")
                    .build();
            final Gauge<Integer> executorQueuedGauge = () -> (int) executorQueued.value();
            metricRegistry.register(metadata, executorQueuedGauge, microProfileTags);
        }

        final Search executorQueueRemainingSearch = compositeMeterRegistry.find("executor.queue.remaining").tags(tags);
        final io.micrometer.core.instrument.Gauge executorQueueRemaining = executorQueueRemainingSearch.gauge();
        if (executorQueueRemaining != null) {
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.queue.remaining")
                    .build();
            final Gauge<Integer> executorQueueRemainingGauge = () -> (int) executorQueueRemaining.value();
            metricRegistry.register(metadata, executorQueueRemainingGauge, microProfileTags);
        }

        final Search executorPoolSizeSearch = compositeMeterRegistry.find("executor.pool.size").tags(tags);
        final io.micrometer.core.instrument.Gauge executorPoolSize = executorPoolSizeSearch.gauge();
        if (executorPoolSize != null) {
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.pool.size")
                    .build();
            final Gauge<Integer> executorPoolSizeGauge = () -> (int) executorPoolSize.value();
            metricRegistry.register(metadata, executorPoolSizeGauge, microProfileTags);
        }

        final Search executorStealsSearch = compositeMeterRegistry.find("executor.steals").tags(tags);
        final FunctionCounter executorSteals = executorStealsSearch.functionCounter();
        if (executorSteals != null) {
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.steals")
                    .build();
            final Gauge<Integer> executorStealsGauge = () -> (int) executorSteals.count();
            metricRegistry.register(metadata, executorStealsGauge, microProfileTags);
        }

        final Search executorRunningSearch = compositeMeterRegistry.find("executor.running").tags(tags);
        final io.micrometer.core.instrument.Gauge executorRunning = executorRunningSearch.gauge();
        if (executorRunning != null) {
            final Metadata metadata = new MetadataBuilder()
                    .withName(executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.running")
                    .build();
            final Gauge<Integer> executorRunningGauge = () -> (int) executorRunning.value();
            metricRegistry.register(metadata, executorRunningGauge, microProfileTags);
        }
    }

    @Override
    public void registerMicrometerRegistry(final MeterRegistry meterRegistry) {
        AppbaseUtil.notNull(meterRegistry, "meterRegistry");

        final String typeName = meterRegistry.getClass().getName();
        logger.info("Registering meter registry: {}", typeName);

        compositeMeterRegistryRef.get().add(meterRegistry);
        meterRegistriesByTypeNameMap
                .computeIfAbsent(typeName, f -> new CopyOnWriteArrayList<>())
                .add(meterRegistry);
    }

    @Override
    @Unmodifiable
    public Set<MeterRegistry> getMicrometerRegistries() {
        return compositeMeterRegistryRef.get().getRegistries();
    }

    @Override
    public void unregisterMicrometerRegistry(final MeterRegistry meterRegistry) {
        AppbaseUtil.notNull(meterRegistry, "meterRegistry");

        logger.info("Unregistering micrometer meter registry: {}", meterRegistry.getClass().getName());
        compositeMeterRegistryRef.get().remove(meterRegistry);
        meterRegistriesByTypeNameMap.forEach((typeName, meterRegistries) -> {
            if (meterRegistry.getClass().getName().equals(typeName)) {
                meterRegistriesByTypeNameMap.remove(typeName);
            }
        });
    }

    @Override
    public void unregisterMicrometerRegistry(final Class<? extends MeterRegistry> meterRegistryType) {
        AppbaseUtil.notNull(meterRegistryType, "meterRegistryType");

        logger.info("Unregistering micrometer meter registry of type: {}", meterRegistryType);
        final String typeName = meterRegistryType.getName();
        final List<MeterRegistry> meterRegistries = meterRegistriesByTypeNameMap.get(typeName);
        if (meterRegistries != null) {
            meterRegistries.forEach(meterRegistry -> {
                if (!meterRegistry.isClosed()) {
                    meterRegistry.close();
                    compositeMeterRegistryRef.get().remove(meterRegistry);
                }
            });

            meterRegistriesByTypeNameMap.remove(typeName);
        }
    }

    @Override
    public void clearMicrometerRegistries() {
        logger.info("Clearing micrometer meter registries");
        meterRegistriesByTypeNameMap.forEach((typeName, meterRegistries) -> {
            meterRegistries.forEach(meterRegistry -> {
                if (!meterRegistry.isClosed()) {
                    meterRegistry.close();
                    compositeMeterRegistryRef.get().remove(meterRegistry);
                }
            });
        });
        meterRegistriesByTypeNameMap.clear();
    }

    @Override
    public String serializeMetricRegistryAsJson() throws MetricServiceException {
        final MetricRegistryJsonSerializer metricRegistryJsonEncoder = new MetricRegistryJsonSerializer(metricRegistry);
        try {
            return JsonUtil
                    .getJsonMapper()
                    .writerFor(MetricRegistryJsonSerializer.class)
                    .writeValueAsString(metricRegistryJsonEncoder);
        } catch (JsonProcessingException jpe) {
            throw new MetricServiceException("Error while encoding metric registry as JSON", jpe);
        }
    }

    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultMetricServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultMetricServiceConfiguration.class.getName());
        }

        final long startTimestamp = System.currentTimeMillis();
        lifecycleReentrantLock.lock();
        try {
            if (statusRef.get() == ServiceStatus.RELOADING) {
                return;
            }

            final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration =
                    (DefaultMetricServiceConfiguration) configuration;
            logger.info("Reloading {}", getClass().getName());
            statusRef.set(ServiceStatus.RELOADING);
            shutdown();

            final CompositeMeterRegistry compositeMeterRegistry = new CompositeMeterRegistry();
            compositeMeterRegistryRef.set(compositeMeterRegistry);
            metricServiceConfigurationRef.set((DefaultMetricServiceConfiguration) configuration);
            statusRef.set(ServiceStatus.STARTED);
            reloadTimeInMillisMap.put(startTimestamp, System.currentTimeMillis());
        } catch (Exception ex) {
            reloadFailureCounter.incrementAndGet();
            statusRef.set(ServiceStatus.RELOAD_FAILED);
            logger.error("Error while reloading service {}", getClass().getName(), ex);
            reloadTimeInMillisMap.put(startTimestamp, System.currentTimeMillis());
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    public boolean isShutdown() {
        return getStatus() == ServiceStatus.SHUTDOWN;
    }

    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        final long startTimestamp = System.currentTimeMillis();
        lifecycleReentrantLock.lock();
        try {
            final ServiceStatus serviceStatus = statusRef.get();
            if (serviceStatus == ServiceStatus.SHUTTING_DOWN || serviceStatus == ServiceStatus.SHUTDOWN) {
                return;
            }

            statusRef.set(ServiceStatus.SHUTTING_DOWN);
            logger.info("Shutting down {}", getClass().getName());
            clearMicrometerRegistries();
            compositeMeterRegistryRef.get().close();
            statusRef.set(ServiceStatus.SHUTDOWN);
            shutdownTimeInMillisMap.put(startTimestamp, System.currentTimeMillis());
        } catch (Exception ex) {
            shutdownFailureCounter.incrementAndGet();
            statusRef.set(ServiceStatus.SHUTDOWN_FAILED);
            logger.error("Error while shutting down service {}", getClass().getName(), ex);
            shutdownTimeInMillisMap.put(startTimestamp, System.currentTimeMillis());
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    public ServiceStatus getStatus() {
        return statusRef.get();
    }

    @Override
    public CompositeMeterRegistry getCompositeMeterRegistry() {
        return compositeMeterRegistryRef.get();
    }

    @Override
    public DefaultMetricServiceConfiguration getConfiguration() {
        return metricServiceConfigurationRef.get();
    }

    public AtomicInteger getReloadFailureCounter() {
        return reloadFailureCounter;
    }

    public AtomicInteger getShutdownFailureCounter() {
        return shutdownFailureCounter;
    }

    public Map<Long, Long> getReloadTimeInMillisMap() {
        return reloadTimeInMillisMap;
    }

    public Map<Long, Long> getShutdownTimeInMillisMap() {
        return shutdownTimeInMillisMap;
    }
}
