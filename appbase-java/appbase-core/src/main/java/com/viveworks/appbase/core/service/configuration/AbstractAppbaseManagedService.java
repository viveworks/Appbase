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

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AbstractAppbaseService;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListenerException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract class for services.
 *
 * @author Navaneeth Mysore G
 * @param <C> {@link AppbaseConfiguration} type
 * @since 1.0.0
 */
public abstract class AbstractAppbaseManagedService<C extends AppbaseManagedServiceConfiguration>
        extends AbstractAppbaseService implements AppbaseManagedService
{
    protected final AppbaseCore appbaseCore;

    protected final Class<C> appbaseServiceConfigurationType;
    protected final AtomicReference<C> appbaseServiceConfigurationRef;

    protected final Timer configurationAddedTimer;
    protected final Timer configurationChangedTimer;
    protected final Timer configurationRemovedTimer;

    protected final Counter reloadCounter;
    protected final Counter reloadErrorCounter;

    /**
     * Stores and validates an explicit service configuration, registers lifecycle metrics, and optionally subscribes
     * this service to its primary and additional configuration types. The initial status becomes {@link ServiceStatus#NEW}.
     *
     * @param appbaseCore core providing configuration, validation, metrics, logging, and mediation services
     * @param appbaseServiceConfigurationType primary configuration type used for listener registration
     * @param appbaseServiceConfiguration initial configuration retained by this service
     * @param registerAsConfigurationListener whether to subscribe to configuration events
     * @param additionalConfigurationTypesToRegister extra event types; {@code null} and null list entries are ignored
     * @throws NullPointerException if {@code appbaseCore} is {@code null}
     * @throws AppbaseRuntimeException if a required configuration argument is null or validation fails
     */
    public AbstractAppbaseManagedService(
            final AppbaseCore appbaseCore,
            final Class<C> appbaseServiceConfigurationType,
            final C appbaseServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService());

        AppbaseUtil.notNull(appbaseServiceConfigurationType, "appbaseServiceConfigurationType");
        AppbaseUtil.notNull(appbaseServiceConfiguration, "appbaseServiceConfiguration");
        AppbaseUtil.validate(appbaseCore.getValidationService(), appbaseServiceConfiguration);

        this.appbaseServiceConfigurationRef = new AtomicReference<>(appbaseServiceConfiguration);
        this.appbaseCore = appbaseCore;
        this.appbaseServiceConfigurationType = appbaseServiceConfigurationType;
        updateStatus(ServiceStatus.NEW);

        this.configurationAddedTimer = appbaseCore
                .getMetricService()
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationAdded", "timer"));
        this.configurationChangedTimer = appbaseCore
                .getMetricService()
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationChanged", "timer"));
        this.configurationRemovedTimer = appbaseCore
                .getMetricService()
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationRemoved", "timer"));
        this.reloadCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "reload", "counter"));
        this.reloadErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "reload", "error", "counter"));
        registerAsConfigurationListener(
                registerAsConfigurationListener,
                appbaseServiceConfigurationType,
                additionalConfigurationTypesToRegister);
    }

    /**
     * Looks up the primary typed configuration from the core, validates it, and optionally registers configuration
     * listeners. Construction fails rather than creating a service without its primary configuration.
     *
     * @param appbaseCore core providing the initial configuration and supporting services
     * @param appbaseServiceConfigurationType type looked up in the core configuration service
     * @param registerAsConfigurationListener whether to subscribe to configuration events
     * @param additionalConfigurationTypesToRegister extra event types; {@code null} and null list entries are ignored
     * @throws NullPointerException if {@code appbaseCore} is {@code null}
     * @throws AppbaseRuntimeException if the type is null, no matching configuration exists, or validation fails
     */
    public AbstractAppbaseManagedService(
            final AppbaseCore appbaseCore,
            final Class<C> appbaseServiceConfigurationType,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService());

        AppbaseUtil.notNull(appbaseServiceConfigurationType, "appbaseServiceConfigurationType");

        final C appbaseServiceConfiguration = appbaseCore
                .getConfigurationService()
                .getTypedConfiguration(appbaseServiceConfigurationType);
        if (appbaseServiceConfiguration == null) {
            throw new AppbaseRuntimeException("Cannot find configuration of type " + appbaseServiceConfigurationType +
                    " in " + appbaseCore.getConfigurationService().getClass().getName());
        }

        AppbaseUtil.validate(appbaseCore.getValidationService(), appbaseServiceConfiguration);
        this.appbaseServiceConfigurationRef = new AtomicReference<>(appbaseServiceConfiguration);
        this.appbaseCore = appbaseCore;
        this.appbaseServiceConfigurationType = appbaseServiceConfigurationType;
        updateStatus(ServiceStatus.NEW);

        this.configurationAddedTimer = appbaseCore
                .getMetricService()
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationAdded", "timer"));
        this.configurationChangedTimer = appbaseCore
                .getMetricService()
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationChanged", "timer"));
        this.configurationRemovedTimer = appbaseCore
                .getMetricService()
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "configurationRemoved", "timer"));
        this.reloadCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "reload", "counter"));
        this.reloadErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "reload", "error", "counter"));
        registerAsConfigurationListener(
                registerAsConfigurationListener,
                appbaseServiceConfigurationType,
                additionalConfigurationTypesToRegister);
    }

    /**
     * This method is used by sub classes / {@link AppbaseService}s to implement only the required reload logic.
     *
     * @param configuration {@link AppbaseConfiguration} used by the service
     * @throws AppbaseException if there were any errors
     */
    protected abstract void initImpl(C configuration) throws AppbaseException;

    protected void startOnCreate() {
        final C configuration = getConfiguration();
        if (configuration != null && configuration.isAutoStart()) {
            try {
                start();
            } catch (AppbaseException ae) {
                throw new AppbaseRuntimeException("Error starting " + getClass(), ae);
            }
        }
    }

    @Override
    public boolean isAutoStart() {
        final C configuration = getConfiguration();
        AppbaseUtil.notNull(configuration, "configuration");

        return configuration.isAutoStart();
    }

    protected void registerAdditionalConfigurationTypes(
            final boolean registerAsConfigurationListener,
            final Class<? extends C> appbaseServiceConfigurationImplType)
    {
        if (registerAsConfigurationListener) {
            appbaseCore
                    .getConfigurationService()
                    .registerConfigurationListenerByKey(
                            this, ConfigurationKey.fromTypeKey(appbaseServiceConfigurationImplType));
        }
    }

    private void registerAsConfigurationListener(
            final boolean registerAsConfigurationListener,
            final Class<C> appbaseServiceConfigurationType,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {

        if (registerAsConfigurationListener) {
            appbaseCore
                    .getConfigurationService()
                    .registerConfigurationListenerByKey(
                            this,
                            ConfigurationKey.fromTypeKey(appbaseServiceConfigurationType));

            if (additionalConfigurationTypesToRegister != null) {
                additionalConfigurationTypesToRegister.forEach(type -> {
                    if (type != null) {
                        appbaseCore
                                .getConfigurationService()
                                .registerConfigurationListenerByKey(this, ConfigurationKey.fromTypeKey(type));
                    }
                });
            }
        }
    }

    protected void initWithNoStatusUpdate(final C appbaseServiceConfiguration) throws AppbaseException {
        lifecycleReentrantLock.lock();

        try {
            if (isStarted()) {
                shutdown();
            }

            this.logger.info("Initializing {} using {}", getClass().getName(), getConfiguration());

            AppbaseUtil.notNull(appbaseServiceConfiguration, "appbaseServiceConfiguration");
            AppbaseUtil.validate(appbaseCore.getValidationService(), appbaseServiceConfiguration);

            initImpl(appbaseServiceConfiguration);
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void init(final C appbaseServiceConfiguration) throws AppbaseException {
        lifecycleReentrantLock.lock();

        try {
            initWithNoStatusUpdate(appbaseServiceConfiguration);
            this.updateStatus(ServiceStatus.STARTED);
        } catch (Exception ex) {
            this.updateStatus(ServiceStatus.FAILED);
            throw ex;
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    protected void onConfigurationAddedImpl(final C appbaseServiceConfiguration) throws AppbaseException {
        init(appbaseServiceConfiguration);
    }

    protected void onConfigurationChangedImpl(final C appbaseServiceConfiguration) throws AppbaseException {
        init(appbaseServiceConfiguration);
    }

    protected void onConfigurationRemovedImpl() {
        shutdown();
    }

    @Override
    public void start() throws AppbaseException {
        logger.info("Starting {} using {}", getClass().getName(), getConfiguration());
        init(getConfiguration());
    }

    @Override
    public boolean isStarted() {
        return getStatus() == ServiceStatus.STARTED;
    }

    @Override
    @SuppressWarnings({ "checkstyle:IllegalCatch", "unchecked" })
    public void onConfigurationAdded(
            final ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationAddedEvent, "configurationAddedEvent");
        AppbaseUtil.validate(appbaseCore.getValidationService(), configurationAddedEvent);

        lifecycleReentrantLock.lock();
        final Timer.Context timerContext = configurationAddedTimer.time();
        try {
            logger.info("Received configuration added event: {}", configurationAddedEvent);

            final ConfigurationKey configurationKey = configurationAddedEvent.getConfigurationKey();
            final ConfigurationValue<?> configurationValue = configurationAddedEvent.getConfigurationValue();
            if (configurationKey != null &&
                    appbaseServiceConfigurationType.isAssignableFrom(configurationKey.getTypeKey()) &&
                    configurationValue != null)
            {
                final Object value = configurationValue.getValue();
                if (value != null && appbaseServiceConfigurationType.isAssignableFrom(value.getClass())) {
                    final C appbaseServiceConfiguration = (C) value;
                    AppbaseUtil.validate(appbaseCore.getValidationService(), appbaseServiceConfiguration);
                    onConfigurationAddedImpl(appbaseServiceConfiguration);
                    return;
                }
            }

            throw new ConfigurationListenerException(
                    "Invalid configuration add event: " + configurationAddedEvent);
        } catch (ConfigurationListenerException cle) {
          throw cle;
        } catch (Exception ex) {
          throw new ConfigurationListenerException("Failed on configuration added: " + configurationAddedEvent, ex);
        } finally {
            lifecycleReentrantLock.unlock();
            timerContext.stop();
        }
    }

    @Override
    @SuppressWarnings({ "checkstyle:IllegalCatch", "unchecked" })
    public void onConfigurationChanged(
            final ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationChangedEvent, "configurationChangedEvent");
        AppbaseUtil.validate(appbaseCore.getValidationService(), configurationChangedEvent);

        lifecycleReentrantLock.lock();
        final Timer.Context timerContext = configurationChangedTimer.time();
        try {
            logger.info("Received configuration changed event: {}", configurationChangedEvent);

            final ConfigurationKey configurationKey = configurationChangedEvent.getConfigurationKey();
            final ConfigurationValue<?> newConfigurationValue = configurationChangedEvent.getNewConfigurationValue();
            if (configurationKey != null &&
                    configurationKey.isTyped() &&
                    appbaseServiceConfigurationType.isAssignableFrom(configurationKey.getTypeKey()) &&
                    newConfigurationValue != null)
            {
                final Object value = newConfigurationValue.getValue();
                if (value != null && appbaseServiceConfigurationType.isAssignableFrom(value.getClass())) {
                    final C appbaseServiceConfiguration = (C) value;
                    AppbaseUtil.validate(appbaseCore.getValidationService(), appbaseServiceConfiguration);
                    onConfigurationChangedImpl(appbaseServiceConfiguration);
                    return;
                }
            }

            throw new ConfigurationListenerException(
                    "Invalid configuration changed event: " + configurationChangedEvent);
        } catch (ConfigurationListenerException cle) {
            throw cle;
        } catch (Exception ex) {
            throw new ConfigurationListenerException("Failed on configuration changed: " + configurationChangedEvent, ex);
        } finally {
            lifecycleReentrantLock.unlock();
            timerContext.stop();
        }
    }

    @Override
    public void onConfigurationRemoved(
            final ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationRemovedEvent, "configurationRemovedEvent");
        AppbaseUtil.validate(appbaseCore.getValidationService(), configurationRemovedEvent);

        lifecycleReentrantLock.lock();
        final Timer.Context timerContext = configurationRemovedTimer.time();
        try {
            logger.info("Received configuration removed event: {}", configurationRemovedEvent);
            final ConfigurationKey configurationKey = configurationRemovedEvent.getConfigurationKey();
            if (configurationKey != null &&
                    configurationKey.isTyped() &&
                    appbaseServiceConfigurationType.isAssignableFrom(configurationKey.getTypeKey()))
            {
                onConfigurationRemovedImpl();
                return;
            }

            throw new ConfigurationListenerException(
                    "Invalid configuration removed event: " + configurationRemovedEvent);
        } finally {
            lifecycleReentrantLock.unlock();
            timerContext.stop();
        }
    }

    @Override
    @SuppressWarnings({ "checkstyle:IllegalCatch", "unchecked" })
    protected <D extends AppbaseConfiguration> void reloadImpl(
            final D configuration) throws ServiceReloadException
    {
        AppbaseUtil.notNull(configuration, "configuration");
        AppbaseUtil.validate(appbaseCore.getValidationService(), configuration);

        try {
            if (!appbaseServiceConfigurationType.isAssignableFrom(configuration.getClass())) {
                throw new ServiceReloadException("The configuration " +
                        configuration.getClass().getName() +
                        " is not of type " +
                        appbaseServiceConfigurationType);
            }

            final C appbaseServiceConfiguration = (C) configuration;
            init(appbaseServiceConfiguration);
            appbaseServiceConfigurationRef.set(appbaseServiceConfiguration);
        } catch (ServiceReloadException sre) {
            throw sre;
        } catch (Exception ex) {
            throw new ServiceReloadException("Failed on reload: " + configuration, ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public C getConfiguration() {
        return appbaseServiceConfigurationRef.get();
    }
}
