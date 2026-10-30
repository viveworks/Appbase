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

package com.viveworks.appbase.core.service.configuration;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AbstractAppbaseService;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializerNotFoundException;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationsJson;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.Validatable;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolation;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe configuration service backed by concurrent property and typed-configuration maps. Reload and
 * initialization are serialized separately, while published snapshots are replaced through atomic references.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings({
        "checkstyle:MethodCount",
        "checkstyle:FileLength",
        "checkstyle:WeakerAccess"
})
@SuppressFBWarnings({
        "CRLF_INJECTION_LOGS",
        "IMC_IMMATURE_CLASS_NO_TOSTRING",
        "WEM_WEAK_EXCEPTION_MESSAGING"
})
@ThreadSafe
public class DefaultConfigurationService extends AbstractAppbaseService implements ConfigurationService {
    private final ValidationService validationService;
    private final ConfigurationAccessor configurationAccessor;
    private final AtomicReference<CoreBootstrapConfigurationLoader> bootstrapConfigurationLoaderRef;
    private final AtomicReference<DefaultConfigurationServiceConfiguration> defaultConfigurationServiceConfigurationRef;
    private final AtomicReference<ConcurrentMap<ConfigurationKey, Map<String, String>>> configurationContextByKeyMapRef;
    private final AtomicReference<ScheduledFuture<Void>> loaderTaskScheduledFutureRef;

    private final ConcurrentMap<String, Object> configurationByPropertyNameMap;
    private final AtomicReference<ConcurrentMap<String, ConfigurationValue<?>>>
            loadedConfigurationValueByPropertyNameMapRef;
    private final ConcurrentMap<String, Object> configurationByTypeNameMap;
    private final AtomicReference<ConcurrentMap<String, ConfigurationValue<?>>>
            loadedConfigurationValueByTypeNameMapRef;

    private final ConcurrentMap<String, ConfigurationListener> globalConfigurationListenerByTypeNameMap;
    private final ConcurrentMap<ConfigurationKey, ConcurrentMap<String, ConfigurationListener>>
            configurationListenerByTypeNameByConfigurationKeyMap;
    private final ConcurrentMap<ConfigurationKey, ConcurrentMap<String, AppbaseService>>
            managedServiceByTypeNameByConfigurationKeyMap;
    private final ConcurrentMap<String, PropertyDeserializerAndConfigurationType>
            propertyDeserializerAndTypeEntryByPropertyKeyMap;
    private final ConcurrentMap<String, PropertyDeserializer> propertyDeserializerByTypeNameMap;

    private final List<String> nonValidatingTypeNames;
    private final List<String> securePropertyKeys;
    private final List<String> secureTypeNameKeys;
    private final ConcurrentMap<String, Collection<String>> secureFieldNamesByTypeNameMap;
    private final AtomicReference<ConfigurationsJson> configurationsJsonRef;
    private final AtomicReference<ConfigurationsJson> secureConfigurationsJsonRef;

    private final boolean initWithConfigurationFile;
    private final ReentrantLock initLock;
    private final ReentrantLock loadLock;

    private final Gauge configurationValueByPropertyNameMapSizeGauge;
    private final Gauge loadedConfigurationValueByPropertyNameMapSizeGauge;
    private final Gauge configurationValueByTypeNameMapSizeGauge;
    private final Gauge loadedConfigurationValueByTypeNameMapSizeGauge;
    private final Gauge globalConfigurationListenerByTypeNameMapSizeGauge;
    private final Gauge configurationListenerByTypeNameByConfigurationKeyMapSizeGauge;
    private final Gauge managedServiceByTypeNameByConfigurationKeyMapSizeGauge;
    private final Gauge propertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge;
    private final Gauge propertyDeserializerByTypeNameMapSizeGauge;
    private final Gauge nonValidatingTypeNamesSizeGauge;
    private final Gauge securePropertyKeysSizeGauge;
    private final Gauge secureTypeNameKeysSizeGauge;
    private final Gauge secureFieldNameSetByTypeNameMapSizeGauge;
    private final Counter invalidConfigurationCounter;
    private final Histogram removedPropertiesHistogram;
    private final Counter removedPropertiesErrorCounter;
    private final Histogram addedPropertiesHistogram;
    private final Counter addedPropertiesErrorCounter;
    private final Histogram changedPropertiesHistogram;
    private final Counter changedPropertiesErrorCounter;
    private final Counter reloadErrorCounter;
    private final Counter shutdownErrorCounter;
    private final Histogram removedTypedConfigurationsHistogram;
    private final Counter removedTypedConfigurationsErrorCounter;
    private final Histogram addedTypedConfigurationsHistogram;
    private final Counter addedTypedConfigurationsErrorCounter;
    private final Histogram changedTypedConfigurationsHistogram;
    private final Counter changedTypedConfigurationsErrorCounter;
    private final Timer configurationAccessorLoadTimer;
    private final Counter configurationAccessorErrorCounter;

    /**
     * Creates the service after resolving and validating its configuration, then initializes its concurrent state,
     * metrics, and optional accessor-polling task.
     *
     * @param logService logging dependency passed to the service base
     * @param metricService registry used for configuration and lifecycle metrics
     * @param mediatorService mediator used for status and exception messages
     * @param validationService validator used for service and decoded configurations
     * @param configurationAccessor accessor used for initial loads and optional scheduled polling
     * @param defaultConfigurationServiceConfiguration explicit settings, or {@code null} to resolve them externally
     */
    @SuppressWarnings({"FinalParameters", "ParameterAssignment"})
    @SuppressFBWarnings({
            "EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS",
            "PCOA_PARTIALLY_CONSTRUCTED_OBJECT_ACCESS",
            "WOC_WRITE_ONLY_COLLECTION_FIELD"
    })
    @Inject
    public DefaultConfigurationService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final ConfigurationAccessor configurationAccessor,
            @Nullable DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration)
    {
        super(logService, metricService, mediatorService);

        AppbaseUtil.notNull(validationService, "validationService");
        this.validationService = validationService;

        logger.info("Creating {}", getClass().getName());
        AppbaseUtil.notNull(configurationAccessor, "configurationAccessor");
        this.configurationAccessor = configurationAccessor;

        if (defaultConfigurationServiceConfiguration == null) {
            String configurationFilePathFromSystem = AppbaseUtil.propertyOrEnv(
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY,
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_ENV_KEY);
            if (configurationFilePathFromSystem == null) {
                initWithConfigurationFile = false;
                defaultConfigurationServiceConfiguration =
                        DefaultConfigurationServiceConfiguration.builder().build();
            } else {
                configurationFilePathFromSystem = configurationFilePathFromSystem.trim();

                logger.info("Reading {} from path {}",
                        DefaultConfigurationServiceConfiguration.class.getName(), configurationFilePathFromSystem);
                try {
                    defaultConfigurationServiceConfiguration = YamlUtil.readConfigurationFromFile(
                            configurationFilePathFromSystem,
                            AppbaseConstants.CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                            AppbaseConstants.CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME_ENV_NAME,
                            AppbaseConstants.DEFAULT_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME,
                            DefaultConfigurationServiceConfiguration.class);
                } catch (IOException ioe) {
                    logger.info("Using defaults from {}", DefaultConfigurationServiceConfiguration.class.getName());
                    defaultConfigurationServiceConfiguration =
                            DefaultConfigurationServiceConfiguration.builder().build();
                }
                initWithConfigurationFile = true;
            }
        } else {
            initWithConfigurationFile = false;
        }

        AppbaseUtil.validate(validationService, defaultConfigurationServiceConfiguration);
        bootstrapConfigurationLoaderRef = new AtomicReference<>();
        defaultConfigurationServiceConfigurationRef =
                new AtomicReference<>(defaultConfigurationServiceConfiguration);
        configurationByPropertyNameMap = new ConcurrentHashMap<>();
        configurationByTypeNameMap = new ConcurrentHashMap<>();

        loadedConfigurationValueByPropertyNameMapRef = new AtomicReference<>(new ConcurrentHashMap<>());
        loadedConfigurationValueByTypeNameMapRef = new AtomicReference<>(new ConcurrentHashMap<>());
        configurationContextByKeyMapRef = new AtomicReference<>(new ConcurrentHashMap<>());

        globalConfigurationListenerByTypeNameMap = new ConcurrentHashMap<>();
        configurationListenerByTypeNameByConfigurationKeyMap = new ConcurrentHashMap<>();
        managedServiceByTypeNameByConfigurationKeyMap = new ConcurrentHashMap<>();

        propertyDeserializerAndTypeEntryByPropertyKeyMap = new ConcurrentHashMap<>();
        propertyDeserializerByTypeNameMap = new ConcurrentHashMap<>();
        nonValidatingTypeNames = new CopyOnWriteArrayList<>();
        securePropertyKeys = new CopyOnWriteArrayList<>();
        secureTypeNameKeys = new CopyOnWriteArrayList<>();
        secureFieldNamesByTypeNameMap = new ConcurrentHashMap<>();

        configurationValueByPropertyNameMapSizeGauge = configurationByPropertyNameMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "configurationByPropertyNameMap", "size", "gauge"),
                configurationValueByPropertyNameMapSizeGauge);

        loadedConfigurationValueByPropertyNameMapSizeGauge = loadedConfigurationValueByPropertyNameMapRef.get()::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "loadedConfigurationValueByPropertyNameMap", "size", "gauge"),
                loadedConfigurationValueByPropertyNameMapSizeGauge);

        configurationValueByTypeNameMapSizeGauge = configurationByTypeNameMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "configurationByTypeNameMap", "size", "gauge"),
                configurationValueByTypeNameMapSizeGauge);

        loadedConfigurationValueByTypeNameMapSizeGauge = loadedConfigurationValueByTypeNameMapRef.get()::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "loadedConfigurationValueByPropertyNameMap", "size", "gauge"),
                loadedConfigurationValueByTypeNameMapSizeGauge);

        globalConfigurationListenerByTypeNameMapSizeGauge = globalConfigurationListenerByTypeNameMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "globalConfigurationListenerByTypeNameMap", "size", "gauge"),
                globalConfigurationListenerByTypeNameMapSizeGauge);

        configurationListenerByTypeNameByConfigurationKeyMapSizeGauge =
                configurationListenerByTypeNameByConfigurationKeyMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "configurationListenerByTypeNameByConfigurationKeyMap", "size", "gauge"),
                configurationListenerByTypeNameByConfigurationKeyMapSizeGauge);

        managedServiceByTypeNameByConfigurationKeyMapSizeGauge = managedServiceByTypeNameByConfigurationKeyMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "managedServiceByTypeNameByConfigurationKeyMap", "size", "gauge"),
                managedServiceByTypeNameByConfigurationKeyMapSizeGauge);

        nonValidatingTypeNamesSizeGauge = nonValidatingTypeNames::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "nonValidatingTypeNames", "size", "gauge"),
                nonValidatingTypeNamesSizeGauge);

        securePropertyKeysSizeGauge = securePropertyKeys::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "securePropertyKeys", "size", "gauge"),
                securePropertyKeysSizeGauge);

        secureTypeNameKeysSizeGauge = secureTypeNameKeys::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "secureTypeNameKeys", "size", "gauge"),
                secureTypeNameKeysSizeGauge);

        secureFieldNameSetByTypeNameMapSizeGauge = secureFieldNamesByTypeNameMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "secureFieldNamesByTypeNameMap", "size", "gauge"),
                secureFieldNameSetByTypeNameMapSizeGauge);

        propertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge =
                propertyDeserializerAndTypeEntryByPropertyKeyMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "propertyDeserializerAndTypeEntryByPropertyKeyMap", "size", "gauge"),
                propertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge);

        propertyDeserializerByTypeNameMapSizeGauge = propertyDeserializerByTypeNameMap::size;
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "propertyDeserializerByTypeNameMap", "size", "gauge"),
                propertyDeserializerByTypeNameMapSizeGauge);

        invalidConfigurationCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "invalidConfiguration", "counter"));
        removedPropertiesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "removedProperties", "histogram"));
        removedPropertiesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removedProperties", "error", "counter"));
        addedPropertiesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "addedProperties", "histogram"));
        addedPropertiesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "addedProperties", "error", "counter"));
        changedPropertiesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "changedProperties", "histogram"));
        changedPropertiesErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "changedProperties", "error", "counter"));
        removedTypedConfigurationsHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "removedTypedConfigurations", "histogram"));
        removedTypedConfigurationsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "removedTypedConfigurations", "error", "counter"));
        addedTypedConfigurationsHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "addedTypedConfigurations", "histogram"));
        addedTypedConfigurationsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "addedTypedConfigurations", "error", "counter"));
        changedTypedConfigurationsHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "changedTypedConfigurations", "histogram"));
        changedTypedConfigurationsErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "changedTypedConfigurations", "error", "counter"));

        reloadErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "reload", "error", "counter"));
        shutdownErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "shutdown", "error", "counter"));

        configurationAccessorLoadTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationAccessor", "load", "timer"));
        configurationAccessorErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationAccessor", "error", "counter"));

        initLock = new ReentrantLock();
        loadLock = new ReentrantLock();
        loaderTaskScheduledFutureRef = new AtomicReference<>();
        configurationsJsonRef = new AtomicReference<>();
        secureConfigurationsJsonRef = new AtomicReference<>();
        statusRef.set(ServiceStatus.STARTED);
    }

    @Override
    public void load() {
        logger.debug("Loading configurations");
        init(defaultConfigurationServiceConfigurationRef.get());
    }

    @Nullable
    private Object getProperty(final String propertyKey) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        final Object value = configurationByPropertyNameMap.get(propertyKey);
        if (value != null) {
            return value;
        }

        final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByPropertyNameMap =
                loadedConfigurationValueByPropertyNameMapRef.get();
        final ConfigurationValue<?> loadedConfigurationValue =
                loadedConfigurationValueByPropertyNameMap.get(propertyKey);
        if (loadedConfigurationValue != null) {
            return loadedConfigurationValue.getValue();
        }

        return null;
    }

    private void putProperty(final String propertyKey, final Object value) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(value, "value");

        if (securePropertyKeys.contains(propertyKey)) {
            logger.debug("Adding property: [{}, {}]", propertyKey, AppbaseUtil.OBFUSCATE);
        } else {
            logger.debug("Adding property: [{}, {}]", propertyKey, value);
        }
        configurationByPropertyNameMap.put(propertyKey, value);
    }

    private void removeProperty(final String propertyKey) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        logger.debug("Removing property: {}", propertyKey);
        configurationByPropertyNameMap.remove(propertyKey);
    }

    @Nullable
    @Override
    public Byte getByte(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Byte.class.isAssignableFrom(property.getClass())) {
            return (Byte) property;
        }

        try {
            return Byte.valueOf((String) property);
        } catch (NumberFormatException nfe) {
            throw new PropertyCastException(
                    "The value for " + propertyKey + " cannot be cast to " + Byte.class.getName(), nfe);
        }
    }

    @Override
    public Byte getByte(final String propertyKey, final Byte defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Byte value = getByte(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putByte(final String propertyKey, final Byte value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeByte(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    public byte[] getBytes(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (byte[].class.isAssignableFrom(property.getClass())) {
            return (byte[]) property;
        }

        throw new PropertyCastException(
                "The value for " + propertyKey + " cannot be cast to " + byte[].class);
    }

    @Override
    public byte[] getBytes(final String propertyKey, final byte[] defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final byte[] value = getBytes(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putBytes(final String propertyKey, final byte[] value) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(value, "value");

        putProperty(propertyKey, value);
    }

    @Override
    public void removeBytes(final String propertyKey) {
       removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public Short getShort(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Short.class.isAssignableFrom(property.getClass())) {
            return (Short) property;
        }

        try {
            return Short.valueOf((String) property);
        } catch (NumberFormatException nfe) {
            throw new PropertyCastException(
                    "The value for " + propertyKey + " cannot be cast to " + Short.class.getName(), nfe);
        }
    }

    @Override
    public Short getShort(final String propertyKey, final Short defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Short value = getShort(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putShort(final String propertyKey, final Short value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeShort(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public Integer getInteger(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Integer.class.isAssignableFrom(property.getClass())) {
            return (Integer) property;
        }

        try {
            return Integer.valueOf((String) property);
        } catch (NumberFormatException nfe) {
            throw new PropertyCastException(
                    "The value for " + propertyKey + " cannot be cast to " + Integer.class.getName(), nfe);
        }
    }

    @Override
    public Integer getInteger(final String propertyKey, final Integer defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Integer value = getInteger(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putInteger(final String propertyKey, final Integer value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeInteger(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public Long getLong(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Long.class.isAssignableFrom(property.getClass())) {
            return (Long) property;
        }

        try {
            return Long.valueOf((String) property);
        } catch (NumberFormatException nfe) {
            throw new PropertyCastException(
                    "The value for " + propertyKey + " cannot be cast to " + Long.class.getName(), nfe);
        }
    }

    @Override
    public Long getLong(final String propertyKey, final Long defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Long value = getLong(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putLong(final String propertyKey, final Long value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeLong(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public Float getFloat(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Float.class.isAssignableFrom(property.getClass())) {
            return (Float) property;
        }

        try {
            return Float.valueOf((String) property);
        } catch (NumberFormatException nfe) {
            throw new PropertyCastException(
                    "The value for " + propertyKey + " cannot be cast to " + Float.class.getName(), nfe);
        }
    }

    @Override
    public Float getFloat(final String propertyKey, final Float defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Float value = getFloat(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putFloat(final String propertyKey, final Float value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeFloat(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public Double getDouble(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Double.class.isAssignableFrom(property.getClass())) {
            return (Double) property;
        }

        try {
            return Double.valueOf((String) property);
        } catch (NumberFormatException nfe) {
            throw new PropertyCastException(
                    "The value for " + propertyKey + " cannot be cast to " + Double.class.getName(), nfe);
        }
    }

    @Override
    public Double getDouble(final String propertyKey, final Double defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Double value = getDouble(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putDouble(final String propertyKey, final Double value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeDouble(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    @SuppressFBWarnings("TBP_TRISTATE_BOOLEAN_PATTERN")
    public Boolean getBoolean(final String propertyKey) {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Boolean.class.isAssignableFrom(property.getClass())) {
            return (Boolean) property;
        }

        return Boolean.valueOf((String) property);
    }

    @Override
    public Boolean getBoolean(final String propertyKey, final Boolean defaultValue) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Boolean value = getBoolean(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putBoolean(final String propertyKey, final Boolean value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeBoolean(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public Character getCharacter(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (Character.class.isAssignableFrom(property.getClass())) {
            return (Character) property;
        }

        throw new PropertyCastException(
                "The value for " + propertyKey + " cannot be cast to " + Character.class.getName());
    }

    @Override
    public Character getCharacter(final String propertyKey, final Character defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Character value = getCharacter(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putCharacter(final String propertyKey, final Character value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeCharacter(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    public String getString(final String propertyKey) throws PropertyCastException {
        final Object property = getProperty(propertyKey);

        if (property == null) {
            return null;
        }

        if (String.class.isAssignableFrom(property.getClass())) {
            return (String) property;
        }

        throw new PropertyCastException(
                "The value for " + propertyKey + " cannot be cast to " + String.class.getName());
    }

    @Override
    public String getString(final String propertyKey, final String defaultValue) throws PropertyCastException {
        AppbaseUtil.notNull(propertyKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final String value = getString(propertyKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putString(final String propertyKey, final String value) {
        putProperty(propertyKey, value);
    }

    @Override
    public void removeString(final String propertyKey) {
        removeProperty(propertyKey);
    }

    @Nullable
    @Override
    @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
    public Object getObject(final ConfigurationKey configurationKey) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        final String propertyKey = configurationKey.getPropertyKey();
        if (!AppbaseUtil.isBlank(propertyKey)) {
            return getProperty(propertyKey);
        }

        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        AppbaseUtil.notNull(typeKey, "typeKey");
        return getTypedConfiguration(typeKey);
    }

    @Override
    public Object getObject(final ConfigurationKey configurationKey, final Object defaultValue) {
        AppbaseUtil.notNull(configurationKey, "propertyKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final Object value = getObject(configurationKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public void putObject(final ConfigurationKey configurationKey, final Object value) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");
        AppbaseUtil.notNull(value, "value");

        logger.debug("Adding configuration key [{}]", configurationKey);
        final String propertyKey = configurationKey.getPropertyKey();
        if (propertyKey != null) {
            if (securePropertyKeys.contains(propertyKey)) {
                logger.debug("Adding configuration key [{}, {}]", configurationKey, AppbaseUtil.OBFUSCATE);
            } else {
                logger.debug("Adding configuration key [{}, {}]", configurationKey, value);
            }

            configurationByPropertyNameMap.put(propertyKey, value);
        }

        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        if (typeKey != null) {
            if (secureTypeNameKeys.contains(typeKey.getName())) {
                logger.debug("Adding configuration key [{}, {}]", configurationKey, AppbaseUtil.OBFUSCATE);
            } else {
                logger.debug("Adding configuration key [{}, {}]", configurationKey, value);
            }

            configurationByTypeNameMap.put(typeKey.getName(), value);
        }
    }

    @Override
    public void removeObject(final ConfigurationKey configurationKey) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.debug("Removing configuration key={}", configurationKey);
        final String propertyKey = configurationKey.getPropertyKey();
        if (propertyKey != null) {
            configurationByPropertyNameMap.remove(propertyKey);
        }

        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        if (typeKey != null) {
            configurationByTypeNameMap.remove(typeKey.getName());
        }
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends AppbaseConfiguration> T getTypedConfiguration(final Class<T> typeKey) {
        AppbaseUtil.notNull(typeKey, "typeKey");

        final Object value = configurationByTypeNameMap.get(typeKey.getName());
        if (value != null) {
            return (T) value;
        }

        final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByTypeNameMap =
                loadedConfigurationValueByTypeNameMapRef.get();
        final ConfigurationValue<?> loadedConfigurationValue =
                loadedConfigurationValueByTypeNameMap.get(typeKey.getName());
        if (loadedConfigurationValue != null) {
            return (T) loadedConfigurationValue.getValue();
        }

        return null;
    }

    @Override
    public <T extends AppbaseConfiguration> T getTypedConfiguration(final Class<T> typeKey,
                                                                    final T defaultValue)
    {
        AppbaseUtil.notNull(typeKey, "typeKey");
        AppbaseUtil.notNull(defaultValue, "defaultValue");

        final T typedConfiguration = getTypedConfiguration(typeKey);
        return typedConfiguration != null ? typedConfiguration : defaultValue;
    }

    @Override
    public <T extends AppbaseConfiguration> void putTypedConfiguration(final Class<T> typeKey, final T value) {
        AppbaseUtil.notNull(typeKey, "typeKey");
        AppbaseUtil.notNull(value, "value");

        final String typeKeyName = typeKey.getName();
        if (secureTypeNameKeys.contains(typeKeyName)) {
            logger.debug("Adding typed configuration [{}, {}]", typeKeyName, AppbaseUtil.OBFUSCATE);
        } else {
            logger.debug("Adding typed configuration [{}, {}]", typeKeyName, value);
        }
        configurationByTypeNameMap.put(typeKeyName, value);
    }

    @Override
    public <T extends AppbaseConfiguration> void removeTypedConfiguration(final Class<T> typeKey) {
        AppbaseUtil.notNull(typeKey, "typeKey");

        logger.debug("Removing typed configuration key={}", typeKey);
        configurationByTypeNameMap.remove(typeKey.getName());
    }

    @Override
    public boolean containsProperty(final String propertyKey) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        final boolean containsKey = configurationByPropertyNameMap.containsKey(propertyKey);
        if (containsKey) {
            return true;
        }

        final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByPropertyNameMap =
                loadedConfigurationValueByPropertyNameMapRef.get();
        return loadedConfigurationValueByPropertyNameMap.containsKey(propertyKey);
    }

    @Override
    public <T extends AppbaseConfiguration> boolean containsTypedConfiguration(final Class<T> typeKey) {
        AppbaseUtil.notNull(typeKey, "typeKey");

        final boolean containsKey = configurationByTypeNameMap.containsKey(typeKey.getName());
        if (containsKey) {
            return true;
        }

        final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByTypeNameMap =
                loadedConfigurationValueByTypeNameMapRef.get();
        return loadedConfigurationValueByTypeNameMap.containsKey(typeKey.getName());
    }

    @Nullable
    @Override
    public Map<String, String> getConfigurationContext(final ConfigurationKey configurationKey) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        return configurationContextByKeyMapRef.get().get(configurationKey);
    }

    @Override
    public void notifyConfigurationAddedEvent(final ConfigurationAddedEvent configurationAddedEvent) {
        AppbaseUtil.notNull(configurationAddedEvent, "configurationAddedEvent");

        logger.info("Received {}", configurationAddedEvent);
        final ConfigurationKey configurationKey = configurationAddedEvent.getConfigurationKey();
        if (configurationKey.isTyped()) {
            sendAddedEvent(configurationAddedEvent, addedTypedConfigurationsErrorCounter);
        } else {
            sendAddedEvent(configurationAddedEvent, addedPropertiesErrorCounter);
        }
    }

    private void sendAddedEvent(final ConfigurationAddedEvent configurationAddedEvent,
                                final Counter failureCounter)
    {
        final Collection<ConfigurationListener> configurationListeners =
                globalConfigurationListenerByTypeNameMap.values();
        sendAddedEventToListeners(configurationAddedEvent, configurationListeners, failureCounter);

        final ConfigurationKey configurationKey = configurationAddedEvent.getConfigurationKey();
        final ConcurrentMap<String, ConfigurationListener> configurationListenerByTypeNameMap =
                configurationListenerByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (configurationListenerByTypeNameMap != null) {
            final Collection<ConfigurationListener> configurationListeners1 =
                    configurationListenerByTypeNameMap.values();
            sendAddedEventToListeners(configurationAddedEvent, configurationListeners1, failureCounter);

            final Object value = configurationAddedEvent.getConfigurationValue().getValue();
            if (value instanceof AppbaseConfiguration) {
                reloadServicesOnAddOrChange(configurationKey, (AppbaseConfiguration) value);
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void sendAddedEventToListeners(final ConfigurationAddedEvent configurationAddedEvent,
                                           final Collection<ConfigurationListener> configurationListeners,
                                           final Counter failureCounter)
    {
        for (final ConfigurationListener configurationListener : configurationListeners) {
            try {
                configurationListener.onConfigurationAdded(configurationAddedEvent);
            } catch (Exception exception) {
                final String message = String.format(
                        "Error in %s, while handling configuration added event %s: %s",
                        configurationListener.getClass().getName(),
                        configurationAddedEvent.toString(),
                        exception.toString());
                failureCounter.inc();
                final Message exceptionMessage = MessageFactory.exceptionMessage(
                        mediatorService,
                        getClass(),
                        message,
                        AppbaseUtil.threadName(),
                        exception);
                handleMessage(exceptionMessage);
            }
        }
    }

    @Override
    public void notifyConfigurationChangedEvent(final ConfigurationChangedEvent configurationChangedEvent) {
        AppbaseUtil.notNull(configurationChangedEvent, "configurationChangedEvent");

        logger.info("Received {}", configurationChangedEvent);
        final ConfigurationKey configurationKey = configurationChangedEvent.getConfigurationKey();
        if (configurationKey.isTyped()) {
            sendChangedEvent(configurationChangedEvent, changedTypedConfigurationsErrorCounter);
        } else {
            sendChangedEvent(configurationChangedEvent, changedPropertiesErrorCounter);
        }
    }

    private void sendChangedEvent(final ConfigurationChangedEvent configurationChangedEvent,
                                  final Counter failureCounter)
    {
        final Collection<ConfigurationListener> configurationListeners =
                globalConfigurationListenerByTypeNameMap.values();
        sendChangedEventToListeners(configurationChangedEvent, configurationListeners, failureCounter);

        final ConfigurationKey configurationKey = configurationChangedEvent.getConfigurationKey();
        final ConcurrentMap<String, ConfigurationListener> configurationListenerByTypeNameMap =
                configurationListenerByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (configurationListenerByTypeNameMap != null) {
            final Collection<ConfigurationListener> configurationListeners1 =
                    configurationListenerByTypeNameMap.values();
            sendChangedEventToListeners(configurationChangedEvent, configurationListeners1, failureCounter);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void sendChangedEventToListeners(final ConfigurationChangedEvent configurationChangedEvent,
                                             final Collection<ConfigurationListener> configurationListeners,
                                             final Counter failureCounter)
    {
        for (final ConfigurationListener configurationListener : configurationListeners) {
            try {
                configurationListener.onConfigurationChanged(configurationChangedEvent);
            } catch (Exception exception) {
                final String message = String.format(
                        "Error in %s, while handling configuration changed event %s: %s",
                        configurationListener.getClass().getName(),
                        configurationChangedEvent.toString(),
                        exception.toString());
                failureCounter.inc();
                final Message exceptionMessage = MessageFactory.exceptionMessage(
                        mediatorService,
                        getClass(),
                        message,
                        AppbaseUtil.threadName(),
                        exception);
                handleMessage(exceptionMessage);
            }
        }
    }

    @Override
    public void notifyConfigurationRemovedEvent(final ConfigurationRemovedEvent configurationRemovedEvent) {
        AppbaseUtil.notNull(configurationRemovedEvent, "configurationRemovedEvent");

        logger.info("Received {}", configurationRemovedEvent);
        final ConfigurationKey configurationKey = configurationRemovedEvent.getConfigurationKey();
        if (configurationKey.isTyped()) {
            sendRemovedEvent(configurationRemovedEvent, removedTypedConfigurationsErrorCounter);
        } else {
            sendRemovedEvent(configurationRemovedEvent, removedPropertiesErrorCounter);
        }
    }

    private void sendRemovedEvent(final ConfigurationRemovedEvent configurationRemovedEvent,
                                  final Counter failureCounter)
    {
        final Collection<ConfigurationListener> configurationListeners =
                globalConfigurationListenerByTypeNameMap.values();
        sendRemovedEventToListeners(configurationRemovedEvent, configurationListeners, failureCounter);

        final ConfigurationKey configurationKey = configurationRemovedEvent.getConfigurationKey();
        final ConcurrentMap<String, ConfigurationListener> configurationListenerByTypeNameMap =
                configurationListenerByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (configurationListenerByTypeNameMap != null) {
            final Collection<ConfigurationListener> configurationListeners1 =
                    configurationListenerByTypeNameMap.values();
            sendRemovedEventToListeners(configurationRemovedEvent, configurationListeners1, failureCounter);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void sendRemovedEventToListeners(final ConfigurationRemovedEvent configurationRemovedEvent,
                                             final Collection<ConfigurationListener> configurationListeners,
                                             final Counter failureCounter)
    {
        for (final ConfigurationListener configurationListener : configurationListeners) {
            try {
                configurationListener.onConfigurationRemoved(configurationRemovedEvent);
            } catch (Exception exception) {
                final String message = String.format(
                        "Error in %s, while handling configuration removed event %s: %s",
                        configurationListener.getClass().getName(),
                        configurationRemovedEvent.toString(),
                        exception.toString());
                failureCounter.inc();
                final Message exceptionMessage = MessageFactory.exceptionMessage(
                        mediatorService,
                        getClass(),
                        message,
                        AppbaseUtil.threadName(),
                        exception);
                handleMessage(exceptionMessage);
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void reloadServicesOnAddOrChange(
            final ConfigurationKey configurationKey, final AppbaseConfiguration configuration)
    {
        final ConcurrentMap<String, AppbaseService> serviceByTypeNameMap =
                managedServiceByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (serviceByTypeNameMap != null) {
            serviceByTypeNameMap.forEach((typeName, service) -> {
                try {
                    service.reload(configuration);
                } catch (Exception exception) {
                    final String message = String.format("Error reloading service %s for type name %s",
                            service.getClass().getName(), typeName);
                    reloadErrorCounter.inc();
                    final Message exceptionMessage = MessageFactory.exceptionMessage(
                            mediatorService,
                            getClass(),
                            message,
                            AppbaseUtil.threadName(),
                            exception);
                    handleMessage(exceptionMessage);
                }
            });
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void shutdownServicesOnRemove(final ConfigurationKey configurationKey) {
        final ConcurrentMap<String, AppbaseService> serviceByTypeNameMap =
                managedServiceByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (serviceByTypeNameMap != null) {
            serviceByTypeNameMap.forEach((typeName, service) -> {
                try {
                    service.shutdown();
                } catch (Exception exception) {
                    final String message = String.format("Error shutting down service %s for type name %s",
                            service.getClass().getName(), typeName);
                    shutdownErrorCounter.inc();
                    final Message exceptionMessage = MessageFactory.exceptionMessage(
                            mediatorService,
                            getClass(),
                            message,
                            AppbaseUtil.threadName(),
                            exception);
                    handleMessage(exceptionMessage);
                }
            });
        }
    }

    private void handleMessage(final Message message) {
        try {
            mediatorService.getMediator().asyncHandleMessage(message, true);
        } catch (MediatorException me) {
            logger.error("Error sending message to mediator: {}", message, me);
        }
    }

    @Override
    public Map<String, PropertyDeserializerAndConfigurationType>
        getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap()
    {
        return Collections.unmodifiableMap(propertyDeserializerAndTypeEntryByPropertyKeyMap);
    }

    @Override
    public void registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
            final String propertyKey,
            final PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType)
            throws PropertyDeserializerNotFoundException
    {
        AppbaseUtil.notNull(propertyDeserializerAndConfigurationType, "propertyDeserializerAndConfigurationType");
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        final String propertyDeserializerTypeName = propertyDeserializerAndConfigurationType
                .getPropertyDeserializerType()
                .getName();
        if (!propertyDeserializerByTypeNameMap.containsKey(propertyDeserializerTypeName)) {
            throw new PropertyDeserializerNotFoundException("Property deserializer " +
                    propertyDeserializerTypeName +
                    " does not exist. Use registerPropertyDeserializer(PropertyDeserializer) method to add it.");
        }

        final Class<? extends AppbaseConfiguration> configurationType =
                propertyDeserializerAndConfigurationType.getConfigurationType();
        logger.info("Adding [configurationType={}, propertyKey={}, propertyDeserializerType={}]",
                configurationType.getName(), propertyKey, propertyDeserializerTypeName);
        propertyDeserializerAndTypeEntryByPropertyKeyMap.put(propertyKey, propertyDeserializerAndConfigurationType);
    }

    @Override
    public void unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(final String propertyKey) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        logger.info("Unregistering property deserializer and configuration type by property key [{}]", propertyKey);
        propertyDeserializerAndTypeEntryByPropertyKeyMap.remove(propertyKey);
    }

    @Override
    public void clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap() {
        logger.info("Clearing property deserializer and configuration type by property key map");
        propertyDeserializerAndTypeEntryByPropertyKeyMap.clear();
    }

    @Override
    public Collection<PropertyDeserializer> getPropertyDeserializers() {
        return Collections.unmodifiableCollection(propertyDeserializerByTypeNameMap.values());
    }

    @Override
    public void registerPropertyDeserializer(final PropertyDeserializer propertyDeserializer) {
        AppbaseUtil.notNull(propertyDeserializer, "propertyDeserializer");

        final String propertyDeserializerTypeName = propertyDeserializer.getClass().getName();
        logger.info("Adding propertyDeserializer of type: {}", propertyDeserializerTypeName);
        propertyDeserializerByTypeNameMap.put(propertyDeserializerTypeName, propertyDeserializer);
    }

    @Override
    public void unregisterPropertyDeserializer(final Class<? extends PropertyDeserializer> propertyDeserializerType) {
        AppbaseUtil.notNull(propertyDeserializerType, "propertyDeserializerType");

        final String propertyDeserializerTypeName = propertyDeserializerType.getName();
        logger.info("Unregistering property deserializer [{}]", propertyDeserializerTypeName);
        propertyDeserializerByTypeNameMap.remove(propertyDeserializerTypeName);
    }

    @Override
    public void clearPropertyDeserializers() {
        logger.info("Clearing propertyDeserializerByTypeNameMap");
        propertyDeserializerByTypeNameMap.clear();
    }

    @Override
    public Collection<String> getNonValidatingTypeNames() {
        return Collections.unmodifiableCollection(nonValidatingTypeNames);
    }

    @Override
    public void registerNonValidatingType(final Class<? extends AppbaseConfiguration> configurationType) {
        AppbaseUtil.notNull(configurationType, "configurationType");

        logger.info("Registering non validating type [{}]", configurationType);
        nonValidatingTypeNames.add(configurationType.getName());
    }

    @Override
    public void unregisterNonValidatingType(
            final Class<? extends AppbaseConfiguration> configurationType)
    {
        AppbaseUtil.notNull(configurationType, "configurationType");

        final String configurationTypeName = configurationType.getName();
        logger.info("Unregistering non validating type [{}]", configurationTypeName);
        nonValidatingTypeNames.remove(configurationTypeName);
    }

    @Override
    public void clearNonValidatingTypes() {
        logger.info("Clearing nonValidatingTypeNames");
        nonValidatingTypeNames.clear();
    }

    @Override
    public Collection<String> getSecurePropertyKeys() {
        return Collections.unmodifiableCollection(securePropertyKeys);
    }

    @Override
    public void registerSecurePropertyKey(final String propertyKey) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        logger.info("Adding skip log for property key [{}]", propertyKey);
        securePropertyKeys.add(propertyKey);
    }

    @Override
    public void unregisterSecurePropertyKey(final String propertyKey) {
        AppbaseUtil.notNull(propertyKey, "propertyKey");

        logger.info("Unregistering secure property key [{}]", propertyKey);
        securePropertyKeys.remove(propertyKey);
    }

    @Override
    public void clearSecurePropertyKeys() {
        logger.info("Clearing securePropertyKeys");
        securePropertyKeys.clear();
    }

    @Unmodifiable
    @Override
    public Collection<String> getSecureTypeNames() {
        return Collections.unmodifiableCollection(secureTypeNameKeys);
    }

    @Override
    public void registerSecureType(final Class<? extends AppbaseConfiguration> type) {
        AppbaseUtil.notNull(type, "type");

        final String typeName = type.getName();
        logger.info("Registering skip log for type key [{}]", typeName);
        secureTypeNameKeys.add(typeName);
    }

    @Override
    public void unregisterSecureType(final Class<? extends AppbaseConfiguration> type) {
        AppbaseUtil.notNull(type, "type");

        final String typeName = type.getName();
        logger.info("Unregistering secure type [{}]", typeName);
        secureTypeNameKeys.remove(typeName);
    }

    @Override
    public void clearSecureTypes() {
        logger.info("Clearing secureTypeNameKeys");
        secureTypeNameKeys.clear();
    }

    @Unmodifiable
    @Override
    public Map<String, Collection<String>> getSecureFieldNamesByConfigurationTypeNameMap() {
        return Collections.unmodifiableMap(secureFieldNamesByTypeNameMap);
    }

    @Override
    public void registerSecureFieldByConfigurationType(final Class<? extends AppbaseConfiguration> type,
                                                       final String fieldName) throws NoSuchFieldException
    {
        AppbaseUtil.notNull(type, "type");
        AppbaseUtil.notBlank(fieldName, "fieldName");

        final Field field = type.getDeclaredField(fieldName);
        logger.info("Registering secure field {} for type {}", field, type);
        secureFieldNamesByTypeNameMap
                .computeIfAbsent(type.getName(), f -> new CopyOnWriteArrayList<>())
                .add(fieldName);
    }

    @Override
    public void unregisterSecureFieldByConfigurationType(
            final Class<? extends AppbaseConfiguration> configurationType,
            final String fieldName)
    {
        AppbaseUtil.notNull(configurationType, "configurationType");
        AppbaseUtil.notNull(fieldName, "fieldName");

        final String configurationTypeName = configurationType.getName();
        logger.info("Unregistering secure field [{}] by configuration type[{}]", fieldName, configurationTypeName);
        final Collection<String> fields = secureFieldNamesByTypeNameMap.get(configurationTypeName);
        if (fields != null) {
            fields.remove(fieldName);

            if (fields.isEmpty()) {
                secureFieldNamesByTypeNameMap.remove(configurationTypeName);
            }
        }
    }

    @Override
    public void clearSecureFieldByConfigurationTypeMap() {
        logger.info("Clearing secureFieldNamesByTypeNameMap");
        secureFieldNamesByTypeNameMap.clear();
    }

    @Override
    public Collection<ConfigurationListener> getGlobalConfigurationListeners() {
        return Collections.unmodifiableCollection(globalConfigurationListenerByTypeNameMap.values());
    }

    @Override
    public void registerGlobalConfigurationListener(final ConfigurationListener configurationListener) {
        AppbaseUtil.notNull(configurationListener, "configurationListener");

        final String typeName = configurationListener.getClass().getName();
        logger.info("Registering global configuration listener: {}", typeName);
        globalConfigurationListenerByTypeNameMap.put(typeName, configurationListener);
    }

    @Override
    public void unregisterGlobalConfigurationListener(
            final Class<? extends ConfigurationListener> configurationListenerType)
    {
        AppbaseUtil.notNull(configurationListenerType, "configurationListenerType");

        final String configurationListenerTypeName = configurationListenerType.getName();
        logger.info("Unregistering global configuration listener [{}]", configurationListenerTypeName);
        globalConfigurationListenerByTypeNameMap.remove(configurationListenerTypeName);
    }

    @Override
    public void clearGlobalConfigurationListeners() {
        logger.info("Clearing globalConfigurationListenerByTypeNameMap");
        globalConfigurationListenerByTypeNameMap.clear();
    }

    @Unmodifiable
    @Override
    public Map<ConfigurationKey, ConfigurationListener> getConfigurationListenerByKeyMap() {
        final Map<ConfigurationKey, ConfigurationListener> resultMap = new HashMap<>();

        configurationListenerByTypeNameByConfigurationKeyMap.forEach((key, listenerMap) -> {
            if (key != null && listenerMap != null) {
                listenerMap.forEach((listenerTypeName, listener) -> {
                    if (listenerTypeName != null && listener != null) {
                        resultMap.put(key, listener);
                    }
                });
            }
        });

        return resultMap;
    }

    @Override
    public void registerConfigurationListenerByKey(final ConfigurationListener configurationListener,
                                                   final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationListener, "configurationListener");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        final String typeName = configurationListener.getClass().getName();
        logger.info("Registering configuration listener with [typeName, key] = [{}, {}]", typeName, configurationKey);
        configurationListenerByTypeNameByConfigurationKeyMap
                .computeIfAbsent(configurationKey, f -> new ConcurrentHashMap<>())
                .put(typeName, configurationListener);
    }

    @Override
    public void unregisterConfigurationListenerByKey(final ConfigurationKey configurationKey) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.info("Unregistering configuration listener by key[{}]", configurationKey);
        configurationListenerByTypeNameByConfigurationKeyMap.remove(configurationKey);
    }

    @Override
    public void unregisterConfigurationListenerByKey(
            final Class<? extends ConfigurationListener> configurationListenerType,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationListenerType, "configurationListenerType");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        final String configurationListenerTypeName = configurationListenerType.getName();
        logger.info("Unregistering configuration listener [{}] by key [{}]",
                configurationListenerType, configurationKey);
        final ConcurrentMap<String, ConfigurationListener> configurationListenerMap =
                configurationListenerByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (configurationListenerMap != null) {
            configurationListenerMap.remove(configurationListenerTypeName);

            if (configurationListenerMap.isEmpty()) {
                configurationListenerByTypeNameByConfigurationKeyMap.remove(configurationKey);
            }
        }
    }

    @Override
    public void clearConfigurationListenerByKeyMap() {
        logger.info("Clearing configurationListenerByTypeNameByConfigurationKeyMap");
        configurationListenerByTypeNameByConfigurationKeyMap.clear();
    }

    @Unmodifiable
    @Override
    public Map<ConfigurationKey, AppbaseService> getManagedServiceByConfigurationKeyMap() {
        final Map<ConfigurationKey, AppbaseService> result = new HashMap<>();
        managedServiceByTypeNameByConfigurationKeyMap.forEach((key, serviceMap) -> {
            if (key != null && serviceMap != null) {
                serviceMap.forEach((serviceTypeName, service) -> {
                    result.put(key, service);
                });
            }
        });

        return Collections.unmodifiableMap(result);
    }

    @Override
    public void registerManagedServiceByConfigurationKey(
            final AppbaseService service, final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(service, "service");
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.info("Registering managed service [{}] by configuration key [{}]",
                service.getClass().getName(), configurationKey);
        managedServiceByTypeNameByConfigurationKeyMap
                .computeIfAbsent(configurationKey, m -> new ConcurrentHashMap<>())
                .put(service.getClass().getName(), service);
    }

    @Override
    public void unregisterManagedServiceByConfigurationKey(final ConfigurationKey configurationKey) {
        AppbaseUtil.notNull(configurationKey, "configurationKey");

        logger.info("Unregistering managed service by configuration key [{}]", configurationKey);
        managedServiceByTypeNameByConfigurationKeyMap.remove(configurationKey);
    }

    @Override
    public void unregisterManagedServiceByConfigurationKey(
            final Class<? extends AppbaseService> appbaseServiceType,
            final ConfigurationKey configurationKey)
    {
        AppbaseUtil.notNull(configurationKey, "configurationKey");
        AppbaseUtil.notNull(appbaseServiceType, "appbaseServiceType");

        final String appbaseServiceTypeName = appbaseServiceType.getName();
        logger.info("Unregistering managed service by configuration key [{}] and type [{}]",
                configurationKey, appbaseServiceTypeName);
        final ConcurrentMap<String, AppbaseService> appbaseServiceMap =
                managedServiceByTypeNameByConfigurationKeyMap.get(configurationKey);
        if (appbaseServiceMap != null) {
            appbaseServiceMap.remove(appbaseServiceTypeName);

            if (appbaseServiceMap.isEmpty()) {
                managedServiceByTypeNameByConfigurationKeyMap.remove(configurationKey);
            }
        }
    }

    @Override
    public void clearManagedServiceByConfigurationKeyMap() {
        logger.info("Clearing managedServiceByTypeNameByConfigurationKeyMap");
        managedServiceByTypeNameByConfigurationKeyMap.clear();
    }

    @Nullable
    @Override
    public ConfigurationsJson getConfigurationsJson() {
        return configurationsJsonRef.get();
    }

    @Nullable
    @Override
    public ConfigurationsJson getSecureConfigurationsJson() {
        return secureConfigurationsJsonRef.get();
    }

    private void init(final DefaultConfigurationServiceConfiguration configuration) {
        AppbaseUtil.validate(validationService, configuration);

        initLock.lock();
        try {
            logger.info("Initializing {}", getClass().getName());
            if (configuration.isEnableSchedulableConfigurationAccessor()) {
                final SchedulableConfiguration schedulableConfiguration =
                        configuration.getConfigurationAccessorSchedulableConfiguration();
                if (schedulableConfiguration == null) {
                    throw new AppbaseRuntimeException("Configuration accessor's schedulable configuration is null");
                }

                final Set<ConstraintViolation<Object>> constraintViolations =
                        validationService.getValidator().validate(configuration);
                if (constraintViolations != null && !constraintViolations.isEmpty()) {
                    final ValidationServiceException validationServiceException =
                            new ValidationServiceException(configuration, constraintViolations);
                    throw new AppbaseRuntimeException(
                            "Invalid " +
                                    DefaultConfigurationServiceConfiguration.class.getName() +
                                    ": " +
                                    configuration,
                            validationServiceException);
                }

                final ConfigurationLoader configurationLoader = new ConfigurationLoader(this);
                configurationLoader.schedule();
                final ScheduledFuture<Void> scheduledFuture = mediatorService.getMediator().scheduleWithFixedDelay(
                        configurationLoader,
                        schedulableConfiguration.getInitialDelay(),
                        schedulableConfiguration.getInterval(),
                        schedulableConfiguration.getTimeUnit());
                loaderTaskScheduledFutureRef.set(scheduledFuture);
            }

            defaultConfigurationServiceConfigurationRef.set(configuration);
        } finally {
            initLock.unlock();
        }
    }

    @Override
    protected <C extends AppbaseConfiguration> void reloadImpl(
            final C configuration) throws ServiceReloadException
    {
        if (!DefaultConfigurationServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultConfigurationServiceConfiguration.class.getName());
        }

        try {
            init((DefaultConfigurationServiceConfiguration) configuration);
        } catch (AppbaseRuntimeException are) {
            Throwable cause = are.getCause();
            if (cause == null) {
                cause = are;
            }

            throw new ServiceReloadException("Failed to reload " + getClass().getName(), cause);
        }
    }

    @Override
    protected void shutdownImpl() {
        final ScheduledFuture<Void> scheduledFuture = loaderTaskScheduledFutureRef.get();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        clearConfigurationListenerByKeyMap();
        clearGlobalConfigurationListeners();
        clearManagedServiceByConfigurationKeyMap();
        clearNonValidatingTypes();
        clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        clearPropertyDeserializers();
        clearSecureFieldByConfigurationTypeMap();
        clearSecurePropertyKeys();
        clearSecureTypes();
    }

    @Unmodifiable
    @Override
    public Map<String, Object> getConfigurationByPropertyNameMap() {
        return Collections.unmodifiableMap(configurationByPropertyNameMap);
    }

    @Unmodifiable
    @Override
    public Map<String, ConfigurationValue<?>> getLoadedConfigurationValueByPropertyNameMap() {
        return Collections.unmodifiableMap(loadedConfigurationValueByPropertyNameMapRef.get());
    }

    @Unmodifiable
    @Override
    public Map<String, Object> getConfigurationByTypeNameMap() {
        return Collections.unmodifiableMap(configurationByTypeNameMap);
    }

    @Unmodifiable
    @Override
    public Map<String, ConfigurationValue<?>> getLoadedConfigurationValueByTypeNameMap() {
        return Collections.unmodifiableMap(loadedConfigurationValueByTypeNameMapRef.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultConfigurationServiceConfiguration getConfiguration() {
        return defaultConfigurationServiceConfigurationRef.get();
    }

    @Override
    public ConfigurationAccessor getConfigurationAccessor() {
        return configurationAccessor;
    }

    /**
     * Set {@link CoreBootstrapConfigurationLoader}.
     *
     * @param coreBootstrapConfigurationLoader CoreBootstrapConfigurationLoader
     */
    public void setBootstrapConfigurationLoader(
            final CoreBootstrapConfigurationLoader coreBootstrapConfigurationLoader)
    {
        AppbaseUtil.notNull(coreBootstrapConfigurationLoader, "coreBootstrapConfigurationLoader");
        bootstrapConfigurationLoaderRef.set(coreBootstrapConfigurationLoader);
    }

    /**
     * Returns the gauge tracking currently resolved property values.
     *
     * @return the gauge tracking currently resolved property values
     */
    public Gauge getConfigurationValueByPropertyNameMapSizeGauge() {
        return configurationValueByPropertyNameMapSizeGauge;
    }

    /**
     * Returns the gauge over the loaded property-value map that existed when this service was
     * constructed. It does not follow later replacements of that map.
     *
     * @return the gauge bound to the property-value map present during service construction
     */
    public Gauge getLoadedConfigurationValueByPropertyNameMapSizeGauge() {
        return loadedConfigurationValueByPropertyNameMapSizeGauge;
    }

    /**
     * Returns the gauge tracking currently resolved typed configurations.
     *
     * @return the gauge tracking currently resolved typed configurations
     */
    public Gauge getConfigurationValueByTypeNameMapSizeGauge() {
        return configurationValueByTypeNameMapSizeGauge;
    }

    /**
     * Returns the gauge over the loaded typed-value map that existed when this service was
     * constructed. It does not follow later replacements of that map.
     *
     * @return the gauge bound to the typed-value map present during service construction
     */
    public Gauge getLoadedConfigurationValueByTypeNameMapSizeGauge() {
        return loadedConfigurationValueByTypeNameMapSizeGauge;
    }

    /**
     * Returns the gauge tracking listener implementation types registered globally.
     *
     * @return the gauge tracking listener implementation types registered globally
     */
    public Gauge getGlobalConfigurationListenerByTypeNameMapSizeGauge() {
        return globalConfigurationListenerByTypeNameMapSizeGauge;
    }

    /**
     * Returns the gauge tracking configuration-key listener groups by listener type.
     *
     * @return the gauge tracking configuration-key listener groups by listener type
     */
    public Gauge getConfigurationListenerByTypeNameByConfigurationKeyMapSizeGauge() {
        return configurationListenerByTypeNameByConfigurationKeyMapSizeGauge;
    }

    /**
     * Returns the gauge tracking managed-service groups by service type and configuration key.
     *
     * @return the gauge tracking managed-service groups by service type and configuration key
     */
    public Gauge getManagedServiceByTypeNameByConfigurationKeyMapSizeGauge() {
        return managedServiceByTypeNameByConfigurationKeyMapSizeGauge;
    }

    /**
     * Returns the gauge tracking property keys with custom deserializers and target types.
     *
     * @return the gauge tracking property keys with custom deserializers and target types
     */
    public Gauge getPropertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge() {
        return propertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge;
    }

    /**
     * Returns the gauge tracking registered deserializers by target type name.
     *
     * @return the gauge tracking registered deserializers by target type name
     */
    public Gauge getPropertyDeserializerByTypeNameMapSizeGauge() {
        return propertyDeserializerByTypeNameMapSizeGauge;
    }

    /**
     * Returns the gauge tracking type names exempted from configuration validation.
     *
     * @return the gauge tracking type names exempted from configuration validation
     */
    public Gauge getNonValidatingTypeNamesSizeGauge() {
        return nonValidatingTypeNamesSizeGauge;
    }

    /**
     * Returns the gauge tracking property keys whose values are treated as secrets.
     *
     * @return the gauge tracking property keys whose values are treated as secrets
     */
    public Gauge getSecurePropertyKeysSizeGauge() {
        return securePropertyKeysSizeGauge;
    }

    /**
     * Returns the gauge tracking typed configurations treated as secrets.
     *
     * @return the gauge tracking typed configurations treated as secrets
     */
    public Gauge getSecureTypeNameKeysSizeGauge() {
        return secureTypeNameKeysSizeGauge;
    }

    /**
     * Returns the gauge tracking type names with field-level secret declarations.
     *
     * @return the gauge tracking type names with field-level secret declarations
     */
    public Gauge getSecureFieldNameSetByTypeNameMapSizeGauge() {
        return secureFieldNameSetByTypeNameMapSizeGauge;
    }

    /**
     * Returns {@code true} when an external configuration-file path was configured, including when reading it failed and
     * defaults were used.
     *
     * @return {@code true} when an external configuration-file path was configured, including when reading it failed and
     *         defaults were used
     */
    public boolean isInitWithConfigurationFile() {
        return initWithConfigurationFile;
    }

    private class ConfigurationLoader implements Schedulable<Void> {
        private final ConfigurationService configurationService;

        private ConfigurationLoader(final ConfigurationService configurationService) {
            AppbaseUtil.notNull(configurationService, "configurationService");
            this.configurationService = configurationService;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        @SuppressFBWarnings("REC_CATCH_EXCEPTION")
        public Void schedule() {
            final Timer.Context context = configurationAccessorLoadTimer.time();

            loadLock.lock();
            try {
                final CoreBootstrapConfigurationLoader coreBootstrapConfigurationLoader =
                        bootstrapConfigurationLoaderRef.get();
                if (coreBootstrapConfigurationLoader != null) {
                    coreBootstrapConfigurationLoader.load();
                }

                final Collection<Configuration> configurations = configurationAccessor.readConfigurations();
                final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByPropertyNameMap =
                        new ConcurrentHashMap<>();
                final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByTypeNameMap =
                        new ConcurrentHashMap<>();
                final ConcurrentMap<ConfigurationKey, Map<String, String>> configurationContextByKeyMap =
                        new ConcurrentHashMap<>();

                if (configurations != null) {
                    for (final Configuration configuration : configurations) {
                        final ConfigurationKey configurationKey = configuration.getConfigurationKey();
                        final ConfigurationValue<?> configurationValue = configuration.getConfigurationValue();
                        final RawConfiguration rawConfiguration = configuration.getRawConfiguration();
                        if (rawConfiguration != null) {
                            final Map<String, String> rawConfigurationContext = rawConfiguration.getContext();
                            if (rawConfigurationContext != null) {
                                configurationContextByKeyMap.put(
                                        configurationKey,
                                        Map.copyOf(rawConfigurationContext));
                            }
                        }

                        final String propertyKey = configurationKey.getPropertyKey();
                        if (!AppbaseUtil.isBlank(propertyKey)) {
                            final PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType =
                                    propertyDeserializerAndTypeEntryByPropertyKeyMap.get(propertyKey);
                            if (propertyDeserializerAndConfigurationType != null) {
                                final Class<? extends AppbaseConfiguration> configurationType =
                                        propertyDeserializerAndConfigurationType.getConfigurationType();
                                final Class<? extends PropertyDeserializer> propertyDeserializerType =
                                        propertyDeserializerAndConfigurationType.getPropertyDeserializerType();
                                final PropertyDeserializer propertyDeserializer =
                                        propertyDeserializerByTypeNameMap.get(propertyDeserializerType.getName());
                                if (propertyDeserializer == null) {
                                    throw new PropertyDeserializerNotFoundException(
                                            "Cannot find property deserializer for key " + propertyKey);
                                }

                                final Object value = configurationValue.getValue();
                                final AppbaseConfiguration appbaseConfiguration =
                                        propertyDeserializer.deserialize((String) value, configurationType);
                                if (!nonValidatingTypeNames.contains(configurationType.getName())) {
                                    final Set<ConstraintViolation<Object>> constraintViolations =
                                            validationService.getValidator().validate(value);
                                    if (constraintViolations != null && !constraintViolations.isEmpty()) {
                                        invalidConfigurationCounter.inc();
                                        throw new ValidationServiceException(
                                                "The typed configuration " + value + " is invalid",
                                                value,
                                                constraintViolations);
                                    }
                                } else {
                                    if (secureTypeNameKeys.contains(configurationType.getName())) {
                                        logger.debug("Skipping validation for {}", configurationType.getName());
                                    } else {
                                        logger.debug("Skipping validation for {}: {}",
                                                configurationType.getName(), appbaseConfiguration);
                                    }
                                }

                                final ConfigurationValue<?> configurationValue1 =
                                        ConfigurationValue.fromValue(appbaseConfiguration);
                                final String configurationTypeName = configurationType.getName();
                                if (secureTypeNameKeys.contains(configurationTypeName)) {
                                    logger.debug("Adding loaded property as typed configuration: [{}, {}]",
                                            configurationTypeName, AppbaseUtil.OBFUSCATE);
                                } else {
                                    logger.debug("Adding loaded property as typed configuration: [{}, {}]",
                                            configurationTypeName, configurationValue1);
                                }
                                loadedConfigurationValueByTypeNameMap.put(
                                        configurationTypeName, configurationValue1);
                            } else {
                                if (securePropertyKeys.contains(propertyKey)) {
                                    logger.debug("Adding loaded property: [{}, {}]", propertyKey, AppbaseUtil.OBFUSCATE);
                                } else {
                                    logger.debug("Adding loaded property: [{}, {}]", propertyKey, configurationValue);
                                }
                                loadedConfigurationValueByPropertyNameMap.put(propertyKey, configurationValue);
                            }
                        }

                        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
                        if (typeKey != null) {
                            final Object value = configurationValue.getValue();
                            if (value != null && Validatable.class.isAssignableFrom(value.getClass())) {
                                if (!nonValidatingTypeNames.contains(typeKey.getName())) {
                                    final Set<ConstraintViolation<Object>> constraintViolations =
                                            validationService.getValidator().validate(value);
                                    if (constraintViolations != null && !constraintViolations.isEmpty()) {
                                        invalidConfigurationCounter.inc();
                                        throw new ValidationServiceException(
                                                "The typed configuration " + value + " is invalid",
                                                value,
                                                constraintViolations);
                                    }
                                } else {
                                    if (secureTypeNameKeys.contains(typeKey.getName())) {
                                        logger.debug("Skipping validation for {}", typeKey);
                                    } else {
                                        logger.debug("Skipping validation for {}: {}", typeKey, value);
                                    }
                                }
                            }

                            final String typeKeyName = typeKey.getName();
                            if (secureTypeNameKeys.contains(typeKeyName)) {
                                logger.debug("Adding loaded typed configuration: [{}, {}]",
                                        typeKeyName, AppbaseUtil.OBFUSCATE);
                            } else {
                                logger.debug("Adding loaded typed configuration: [{}, {}]",
                                        typeKeyName, configurationValue);
                            }
                            loadedConfigurationValueByTypeNameMap.put(typeKeyName, configurationValue);
                        }
                    }
                }

                postProcessLoad(loadedConfigurationValueByPropertyNameMap, loadedConfigurationValueByTypeNameMap);
                loadedConfigurationValueByPropertyNameMapRef.set(loadedConfigurationValueByPropertyNameMap);
                loadedConfigurationValueByTypeNameMapRef.set(loadedConfigurationValueByTypeNameMap);
                configurationContextByKeyMapRef.set(configurationContextByKeyMap);

                final ConfigurationsJson configurationsJson = ConfigurationsJson.build(configurationService, false);
                configurationsJsonRef.set(configurationsJson);

                final ConfigurationsJson secureConfigurationsJson = ConfigurationsJson.build(configurationService, true);
                secureConfigurationsJsonRef.set(secureConfigurationsJson);
            } catch (Exception exception) {
                final String message = "Error loading configurations";
                logger.error(message, exception);
                final Message exceptionMessage = MessageFactory.exceptionMessage(
                        mediatorService,
                        getClass(),
                        message,
                        AppbaseUtil.threadName(),
                        exception,
                        null);
                handleMessage(exceptionMessage);
                configurationAccessorErrorCounter.inc();
            } finally {
                loadLock.unlock();
                context.stop();
            }

            return null;
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            final DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration =
                    defaultConfigurationServiceConfigurationRef.get();
            return defaultConfigurationServiceConfiguration.getConfigurationAccessorSchedulableConfiguration();
        }

        private void postProcessLoad(
                final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByPropertyNameMap,
                final ConcurrentMap<String, ConfigurationValue<?>> loadedConfigurationValueByTypeNameMap)
                throws ClassNotFoundException
        {
            final long timestamp = System.currentTimeMillis();
            final ConcurrentMap<String, ConfigurationValue<?>> currentLoadedConfigurationValueByPropertyNameMap =
                    loadedConfigurationValueByPropertyNameMapRef.get();
            final MapDifference<String, ConfigurationValue<?>> propertiesDifference =
                    Maps.difference(
                            currentLoadedConfigurationValueByPropertyNameMap,
                            loadedConfigurationValueByPropertyNameMap);
            if (!propertiesDifference.areEqual()) {
                final Map<String, ConfigurationValue<?>> addedProperties =
                        propertiesDifference.entriesOnlyOnRight();
                processAddedProperties(addedProperties, timestamp);

                final Map<String, MapDifference.ValueDifference<ConfigurationValue<?>>> changedProperties =
                        propertiesDifference.entriesDiffering();
                processChangedProperties(changedProperties, timestamp);

                final Map<String, ConfigurationValue<?>> removedProperties =
                        propertiesDifference.entriesOnlyOnLeft();
                processRemovedProperties(removedProperties, timestamp);
            }

            final ConcurrentMap<String, ConfigurationValue<?>> currentLoadedConfigurationValueByTypeNameMap =
                    loadedConfigurationValueByTypeNameMapRef.get();
            final MapDifference<String, ConfigurationValue<?>> typedConfigurationsDifference =
                    Maps.difference(
                            currentLoadedConfigurationValueByTypeNameMap, loadedConfigurationValueByTypeNameMap);
            if (!typedConfigurationsDifference.areEqual()) {
                final Map<String, ConfigurationValue<?>> addedTypedConfigurations =
                        typedConfigurationsDifference.entriesOnlyOnRight();
                processAddedTypedConfigurations(addedTypedConfigurations, timestamp);

                final Map<String, MapDifference.ValueDifference<ConfigurationValue<?>>> changedTypedConfigurations =
                        typedConfigurationsDifference.entriesDiffering();
                processChangedTypedConfigurations(changedTypedConfigurations, timestamp);

                final Map<String, ConfigurationValue<?>> removedTypedConfigurations =
                        typedConfigurationsDifference.entriesOnlyOnLeft();
                processRemovedTypedConfigurations(removedTypedConfigurations, timestamp);
            }
        }

        private void processAddedProperties(final Map<String, ConfigurationValue<?>> addedProperties,
                                            final long timestamp)
        {
            if (addedProperties != null) {
                addedPropertiesHistogram.update(addedProperties.size());
                addedProperties.forEach((propertyKey, configurationValue) -> {
                    final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey(propertyKey);
                    final ConfigurationAddedEvent configurationAddedEvent =
                            new ConfigurationAddedEvent(configurationKey, configurationValue, timestamp);
                    sendAddedEvent(configurationAddedEvent, addedPropertiesErrorCounter);

                    final Object value = configurationAddedEvent.getConfigurationValue().getValue();
                    if (value != null && value.getClass().isAssignableFrom(AppbaseConfiguration.class)) {
                        reloadServicesOnAddOrChange(configurationKey, (AppbaseConfiguration) value);
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        private void processAddedTypedConfigurations(
                final Map<String, ConfigurationValue<?>> addedTypedConfigurations,
                final long timestamp)
                throws ClassNotFoundException
        {
            if (addedTypedConfigurations != null) {
                addedTypedConfigurationsHistogram.update(addedTypedConfigurations.size());
                for (final Map.Entry<String, ConfigurationValue<?>> entry : addedTypedConfigurations.entrySet()) {
                    final String typedConfigurationName = entry.getKey();
                    final ConfigurationValue<?> configurationValue = entry.getValue();
                    final Class<? extends AppbaseConfiguration> typedConfiguration =
                            (Class<? extends AppbaseConfiguration>) Class.forName(typedConfigurationName);
                    final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(typedConfiguration);
                    final ConfigurationAddedEvent configurationAddedEvent = new ConfigurationAddedEvent(
                            configurationKey, configurationValue, timestamp);
                    sendAddedEvent(configurationAddedEvent, addedTypedConfigurationsErrorCounter);
                    reloadServicesOnAddOrChange(
                            configurationKey, (AppbaseConfiguration) configurationValue.getValue());
                }
            }
        }

        private void processChangedProperties(
                final Map<String, MapDifference.ValueDifference<ConfigurationValue<?>>> changedProperties,
                final long timestamp)
        {
            if (changedProperties != null) {
                changedPropertiesHistogram.update(changedProperties.size());
                changedProperties.forEach((propertyKey, changedValueDifference) -> {
                    final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey(propertyKey);
                    final ConfigurationValue<?> oldConfigurationValue = changedValueDifference.leftValue();
                    final ConfigurationValue<?> newConfigurationValue = changedValueDifference.rightValue();
                    final ConfigurationChangedEvent configurationChangedEvent = new ConfigurationChangedEvent(
                            configurationKey, oldConfigurationValue, newConfigurationValue, timestamp);
                    sendChangedEvent(configurationChangedEvent, changedPropertiesErrorCounter);

                    final Object value = configurationChangedEvent.getNewConfigurationValue().getValue();
                    if (value != null && value.getClass().isAssignableFrom(AppbaseConfiguration.class)) {
                        reloadServicesOnAddOrChange(configurationKey, (AppbaseConfiguration) value);
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        private void processChangedTypedConfigurations(
                final Map<String, MapDifference.ValueDifference<ConfigurationValue<?>>> changedTypedConfigurations,
                final long timestamp)
                throws ClassNotFoundException
        {
            if (changedTypedConfigurations != null) {
                changedTypedConfigurationsHistogram.update(changedTypedConfigurations.size());
                for (final Map.Entry<String, MapDifference.ValueDifference<ConfigurationValue<?>>>
                        entry : changedTypedConfigurations.entrySet())
                {
                    final String typedConfigurationName = entry.getKey();
                    final MapDifference.ValueDifference<ConfigurationValue<?>> value = entry.getValue();
                    final ConfigurationValue<?> oldConfigurationValue = value.leftValue();
                    final ConfigurationValue<?> newConfigurationValue = value.rightValue();
                    final Class<? extends AppbaseConfiguration> typedConfiguration =
                            (Class<? extends AppbaseConfiguration>) Class.forName(typedConfigurationName);
                    final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(typedConfiguration);
                    final ConfigurationChangedEvent configurationChangedEvent = new ConfigurationChangedEvent(
                            configurationKey, oldConfigurationValue, newConfigurationValue, timestamp);
                    sendChangedEvent(configurationChangedEvent, changedTypedConfigurationsErrorCounter);

                    final Object newValue = configurationChangedEvent.getNewConfigurationValue().getValue();
                    if (newValue instanceof AppbaseConfiguration) {
                        reloadServicesOnAddOrChange(configurationKey, (AppbaseConfiguration) newValue);
                    }
                }
            }
        }

        private void processRemovedProperties(final Map<String, ConfigurationValue<?>> removedProperties,
                                              final long timestamp)
        {
            if (removedProperties != null) {
                removedPropertiesHistogram.update(removedProperties.size());
                removedProperties.forEach((propertyKey, configurationValue) -> {
                    final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey(propertyKey);
                    final ConfigurationRemovedEvent configurationRemovedEvent = new ConfigurationRemovedEvent(
                            configurationKey, timestamp);
                    sendRemovedEvent(configurationRemovedEvent, removedPropertiesErrorCounter);
                    shutdownServicesOnRemove(configurationKey);
                });
            }
        }

        @SuppressWarnings("unchecked")
        private void processRemovedTypedConfigurations(
                final Map<String, ConfigurationValue<?>> removedTypedConfigurations,
                final long timestamp) throws ClassNotFoundException
        {
            if (removedTypedConfigurations != null) {
                removedTypedConfigurationsHistogram.update(removedTypedConfigurations.size());
                for (final Map.Entry<String, ConfigurationValue<?>> entry : removedTypedConfigurations.entrySet()) {
                    final String typedConfigurationName = entry.getKey();
                    final Class<? extends AppbaseConfiguration> typedConfiguration =
                            (Class<? extends AppbaseConfiguration>) Class.forName(typedConfigurationName);
                    final ConfigurationKey configurationKey = ConfigurationKey.fromTypeKey(typedConfiguration);
                    final ConfigurationRemovedEvent configurationRemovedEvent = new ConfigurationRemovedEvent(
                            configurationKey, timestamp);
                    sendRemovedEvent(configurationRemovedEvent, removedTypedConfigurationsErrorCounter);
                    shutdownServicesOnRemove(configurationKey);
                }
            }
        }
    }

}
