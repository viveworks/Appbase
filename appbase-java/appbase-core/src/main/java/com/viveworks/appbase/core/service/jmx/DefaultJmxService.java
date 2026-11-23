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

package com.viveworks.appbase.core.service.jmx;

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
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link JmxService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJmxService extends AbstractAppbaseService implements JmxService, ConfigurationListener {
    private final Logger logger;
    private final ValidationService validationService;
    private final AtomicReference<JmxServiceConfiguration> jmxServiceConfigurationRef;
    private final ConcurrentMap<String, Object> mxBeanByTypeNameMap;
    private final ConcurrentMap<String, MXBeanEntry> mxBeanEntryByTypeNameMap;
    private final Counter configurationAddedCounter;
    private final Counter configurationChangedCounter;
    private final Counter configurationRemovedCounter;

    public DefaultJmxService(final LogService logService,
                             final MetricService metricService,
                             final MediatorService mediatorService,
                             final ValidationService validationService,
                             final ConfigurationService configurationService)
    {
        super(logService, metricService, mediatorService);
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(configurationService, "configurationService");

        this.logger = logService.getLogger(getClass());
        this.validationService = validationService;

        DefaultJmxServiceConfiguration defaultJmxServiceConfiguration =
                configurationService.getTypedConfiguration(DefaultJmxServiceConfiguration.class);
        if (defaultJmxServiceConfiguration == null) {
            final JmxServiceConfiguration jmxServiceConfiguration =
                    configurationService.getTypedConfiguration(JmxServiceConfiguration.class);
            if (jmxServiceConfiguration instanceof DefaultJmxServiceConfiguration) {
                defaultJmxServiceConfiguration = (DefaultJmxServiceConfiguration) jmxServiceConfiguration;
            } else {
                throw new AppbaseRuntimeException("Cannot find " +
                        DefaultJmxServiceConfiguration.class.getName() +
                        " in " +
                        ConfigurationService.class.getName());
            }
        }

        AppbaseUtil.validate(validationService, defaultJmxServiceConfiguration);
        this.jmxServiceConfigurationRef = new AtomicReference<>(defaultJmxServiceConfiguration);
        this.mxBeanByTypeNameMap = new ConcurrentHashMap<>();
        this.mxBeanEntryByTypeNameMap = new ConcurrentHashMap<>();

        this.configurationAddedCounter = metricService
            .getMetricRegistry()
            .counter(metricService.name(getClass(), "configurationAdded", "counter"));
        this.configurationChangedCounter = metricService
            .getMetricRegistry()
            .counter(metricService.name(getClass(), "configurationChanged", "counter"));
        this.configurationRemovedCounter = metricService
            .getMetricRegistry()
            .counter(metricService.name(getClass(), "configurationRemoved", "counter"));
        configurationService.registerConfigurationListenerByKey(
                this, ConfigurationKey.fromTypeKey(DefaultJmxServiceConfiguration.class));
        this.updateStatus(ServiceStatus.STARTED);
    }

    @Override
    @Nullable
    public MXBeanEntry getMXBeanEntry(final Class<?> mxBeanType) {
        AppbaseUtil.notNull(mxBeanType, "mxBeanType");

        return mxBeanEntryByTypeNameMap.get(mxBeanType.getName());
    }

    @Override
    public Collection<MXBeanEntry> getMXBeanEntries() {
        return Collections.unmodifiableCollection(mxBeanEntryByTypeNameMap.values());
    }

    @Override
    public <T> boolean isMXBeanRegistered(final Class<T> mxBeanType) throws JmxServiceException {
        AppbaseUtil.notNull(mxBeanType, "mxBeanType");

        try {
            final ObjectName objectName = objectName(mxBeanType);
            return ManagementFactory.getPlatformMBeanServer().isRegistered(objectName);
        } catch (MalformedObjectNameException mone) {
            throw new JmxServiceException("Error when checking if MxBean is registered", mone);
        }
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getMXBean(final Class<T> mxBeanType) {
        AppbaseUtil.notNull(mxBeanType, "mxBeanType");

        return (T) mxBeanByTypeNameMap.get(mxBeanType.getName());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T> void registerMXBean(final T mxBean) throws JmxServiceException {
        AppbaseUtil.notNull(mxBean, "mxBean");

        final String mxBeanTypeName = mxBean.getClass().getName();
        try {
            final ObjectName objectName = objectName(mxBean.getClass());
            if (ManagementFactory.getPlatformMBeanServer().isRegistered(objectName)) {
                logger.warn("MXBean {} is already registered", mxBean.getClass().getName());
            } else {
                logger.info("Registering MXBean: {}", objectName);
                ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, objectName);
            }

            mxBeanByTypeNameMap.putIfAbsent(mxBeanTypeName, mxBean);

            final MBeanInfo mBeanInfo = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(objectName);
            final MXBeanEntry mxBeanEntry = MXBeanEntry.from(mBeanInfo);
            mxBeanEntryByTypeNameMap.putIfAbsent(mxBeanTypeName, mxBeanEntry);
        } catch (Exception ex) {
            throw new JmxServiceException("Error while registering MxBean: " + mxBeanTypeName, ex);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <T> void unregisterMXBean(final Class<T> mxBeanType) throws JmxServiceException {
        AppbaseUtil.notNull(mxBeanType, "mxBeanType");

        final String mxBeanTypeName = mxBeanType.getName();
        try {
            final ObjectName objectName = objectName(mxBeanType);
            logger.info("Unregistering MXBean: {}", objectName);

            ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
            mxBeanByTypeNameMap.remove(mxBeanTypeName);
            mxBeanEntryByTypeNameMap.remove(mxBeanTypeName);
        } catch (Exception ex) {
            throw new JmxServiceException("Error while registering MxBean: " + mxBeanTypeName, ex);
        }
    }

    @Override
    public void clearMXBeans() throws JmxServiceException {
        logger.info("Clearing MXBeans");
        shutdownImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public JmxServiceConfiguration getConfiguration() {
        return jmxServiceConfigurationRef.get();
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
        final DefaultJmxServiceConfiguration defaultJmxServiceConfiguration =
                (DefaultJmxServiceConfiguration) configurationValue.getValue();
        reload(defaultJmxServiceConfiguration);
    }

    private void verifyConfigurationKey(
            final ConfigurationKey configurationKey) throws ConfigurationListenerException
    {
        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        if (typeKey == null || !typeKey.isAssignableFrom(DefaultJmxServiceConfiguration.class)) {
            throw new ConfigurationListenerException(
                    "Configuration type key is not of type " + DefaultJmxServiceConfiguration.class.getName());
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected <C extends AppbaseConfiguration> void reloadImpl(
            final C configuration) throws ServiceReloadException
    {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!JmxServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    JmxServiceConfiguration.class.getName());
        }

        final JmxServiceConfiguration jmxServiceConfiguration = (JmxServiceConfiguration) configuration;
        AppbaseUtil.validate(validationService, jmxServiceConfiguration);
        if (jmxServiceConfiguration.isEnabled()) {
            final Map<String, Object> copy = new HashMap<>(mxBeanByTypeNameMap);
            shutdownImpl();

            for (final Map.Entry<String, Object> entry : copy.entrySet()) {
                final String typeName = entry.getKey();
                final Object mxBean = entry.getValue();

                try {
                    final ObjectName objectName = objectName(typeName);
                    logger.info("Registering MXBean: {}", objectName);
                    ManagementFactory.getPlatformMBeanServer().registerMBean(mxBean, objectName);
                } catch (Exception ex) {
                    throw new ServiceReloadException("Error while reloading MxBean " + typeName, ex);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void shutdownImpl() {
        for (final String typeName : mxBeanByTypeNameMap.keySet()) {
            try {
                final ObjectName objectName = objectName(typeName);
                if (ManagementFactory.getPlatformMBeanServer().isRegistered(objectName)) {
                    logger.info("Unregistering MXBean: {}", objectName);
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
                } else {
                    mxBeanByTypeNameMap.remove(typeName);
                    mxBeanEntryByTypeNameMap.remove(typeName);
                }
            } catch (Exception ex) {
                logger.error("Error while creating object name", ex);
            }
        }

        mxBeanByTypeNameMap.clear();
        mxBeanEntryByTypeNameMap.clear();
    }

    private ObjectName objectName(final Class<?> mxBeanType) throws MalformedObjectNameException {
        AppbaseUtil.notNull(mxBeanType, "mxBeanType");

        final String packageName = mxBeanType.getPackageName();
        final String simpleName = mxBeanType.getSimpleName();
        return new ObjectName(packageName, "type", simpleName);
    }

    private ObjectName objectName(final String typeName) throws MalformedObjectNameException, ClassNotFoundException {
        AppbaseUtil.notNull(typeName, "typeName");

        final Class<?> type = Class.forName(typeName);
        final String packageName = type.getPackageName();
        final String simpleName = type.getSimpleName();
        return new ObjectName(packageName, "type", simpleName);
    }
}
