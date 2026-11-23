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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.AppbaseClientPoolConfiguration;
import com.viveworks.appbase.client.core.pool.DefaultAppbaseClientPool;
import com.viveworks.appbase.client.core.pool.DefaultAppbaseClientPoolConfiguration;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManagerConfiguration;
import com.viveworks.appbase.client.core.pool.manager.DefaultAppbaseClientPoolManager;
import com.viveworks.appbase.client.core.pool.manager.DefaultAppbaseClientPoolManagerConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import org.eclipse.microprofile.metrics.Counter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract implementation of {@link AppbaseClientService}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <V> {@link ComparableValue} type
 * @param <C> {@link AppbaseClientServiceConfiguration} type
 * @since 1.0.0
 */
public abstract class AbstractAppbaseClientService<T, V extends ComparableValue, C extends AppbaseClientServiceConfiguration>
        extends AbstractAppbaseManagedService<C> implements AppbaseClientService<T, V>
{
    protected final AppbaseClientCore appbaseClientCore;
    protected final AtomicReference<AppbaseClientPool<T, V>> appbaseClientPoolRef;
    protected final AtomicReference<AppbaseClientPoolManager> appbaseClientPoolManagerRef;
    protected final AtomicReference<AppbaseClientKey<V>> defaultAppbaseClientKeyRef;
    protected final Map<AppbaseClientKey<?>, Boolean>
            disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap;

    private final Map<String, Counter> appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap;
    private final Map<String, Counter> registerAppbaseClientIntegrationServiceCounterByTypeNameMap;


    /** Derives its default key and pool from configuration.
     * @param appbaseClientCore client-core services
     * @param appbaseClientServiceConfigurationType concrete configuration class
     * @param registerAsConfigurationListener whether changes trigger reinitialization
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public AbstractAppbaseClientService(
            final AppbaseClientCore appbaseClientCore,
            final Class<C> appbaseClientServiceConfigurationType,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                appbaseClientServiceConfigurationType,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPoolRef = new AtomicReference<>();
        this.appbaseClientPoolManagerRef = new AtomicReference<>();
        this.defaultAppbaseClientKeyRef = new AtomicReference<>();
        this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap = new ConcurrentHashMap<>();

        this.appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap = new ConcurrentHashMap<>();
        this.registerAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
    }


    /** Uses a supplied default key and client pool with configuration-loaded integrations.
     * @param appbaseClientCore client-core services
     * @param defaultAppbaseClientKey key used for unqualified lookups until startup replaces it with the
     *        key returned by {@code defaultAppbaseClientKey(configuration)}
     * @param appbaseClientPool pool containing keyed integrations
     * @param appbaseClientServiceConfigurationType concrete configuration class
     * @param registerAsConfigurationListener whether changes trigger reinitialization
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public AbstractAppbaseClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<V> defaultAppbaseClientKey,
            final AppbaseClientPool<T, V> appbaseClientPool,
            final Class<C> appbaseClientServiceConfigurationType,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                appbaseClientServiceConfigurationType,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");
        AppbaseUtil.notNull(appbaseClientPool, "appbaseClientPool");

        this.appbaseClientCore = appbaseClientCore;
        this.defaultAppbaseClientKeyRef = new AtomicReference<>(defaultAppbaseClientKey);
        this.appbaseClientPoolRef = new AtomicReference<>(appbaseClientPool);
        this.appbaseClientPoolManagerRef = new AtomicReference<>();
        this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap = new ConcurrentHashMap<>();

        this.appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap = new ConcurrentHashMap<>();
        this.registerAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
    }


    /** Uses a supplied default key and pool manager with configuration-loaded integrations.
     * @param appbaseClientCore client-core services
     * @param defaultAppbaseClientKey key used for unqualified lookups until startup replaces it with the
     *        key returned by {@code defaultAppbaseClientKey(configuration)}
     * @param appbaseClientPoolManager manager supplying the pool
     * @param appbaseClientServiceConfigurationType concrete configuration class
     * @param registerAsConfigurationListener whether changes trigger reinitialization
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public AbstractAppbaseClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<V> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final Class<C> appbaseClientServiceConfigurationType,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                appbaseClientServiceConfigurationType,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");
        AppbaseUtil.notNull(appbaseClientPoolManager, "appbaseClientPoolManager");

        this.defaultAppbaseClientKeyRef = new AtomicReference<>(defaultAppbaseClientKey);
        this.appbaseClientPoolManagerRef = new AtomicReference<>(appbaseClientPoolManager);
        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPoolRef = new AtomicReference<>();
        this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap = new ConcurrentHashMap<>();

        this.appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap = new ConcurrentHashMap<>();
        this.registerAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
    }


    /** Derives runtime resources from the explicitly supplied service configuration.
     * @param appbaseClientCore client-core services
     * @param appbaseClientServiceConfigurationType concrete configuration class
     * @param appbaseClientServiceConfiguration settings consumed during startup
     * @param registerAsConfigurationListener whether changes trigger reinitialization
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public AbstractAppbaseClientService(
            final AppbaseClientCore appbaseClientCore,
            final Class<C> appbaseClientServiceConfigurationType,
            final C appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                appbaseClientServiceConfigurationType,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPoolRef = new AtomicReference<>();
        this.appbaseClientPoolManagerRef = new AtomicReference<>();
        this.defaultAppbaseClientKeyRef = new AtomicReference<>();
        this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap = new ConcurrentHashMap<>();

        this.appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap = new ConcurrentHashMap<>();
        this.registerAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
    }


    /** Combines explicit settings with a supplied key and client pool.
     * With no injected or custom pool configuration, startup retains this pool; either configured
     * source takes precedence and replaces it. Service shutdown shuts down whichever pool is retained.
     * @param appbaseClientCore client-core services
     * @param defaultAppbaseClientKey key used for unqualified lookups until startup replaces it with the
     *        key returned by {@code defaultAppbaseClientKey(configuration)}
     * @param appbaseClientPool initial pool containing keyed integrations; the service shuts down only the pool it
     *        holds at shutdown, so this pool is not shut down if startup replaces it
     * @param appbaseClientServiceConfigurationType concrete configuration class
     * @param appbaseClientServiceConfiguration settings consumed during startup
     * @param registerAsConfigurationListener whether changes trigger reinitialization
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public AbstractAppbaseClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<V> defaultAppbaseClientKey,
            final AppbaseClientPool<T, V> appbaseClientPool,
            final Class<C> appbaseClientServiceConfigurationType,
            final C appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                appbaseClientServiceConfigurationType,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");

        this.defaultAppbaseClientKeyRef = new AtomicReference<>(defaultAppbaseClientKey);
        this.appbaseClientPoolRef = new AtomicReference<>(appbaseClientPool);
        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPoolManagerRef = new AtomicReference<>();
        this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap = new ConcurrentHashMap<>();

        this.appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap = new ConcurrentHashMap<>();
        this.registerAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
    }


    /** Combines explicit settings with a supplied key and pool manager.
     * Injected manager configuration takes precedence over custom settings; when both are absent,
     * startup retains this manager. A pool must still be supplied or selected by pool configuration,
     * and service shutdown shuts down the selected pool (but not the manager directly).
     * @param appbaseClientCore client-core services
     * @param defaultAppbaseClientKey key used for unqualified lookups until startup replaces it with the
     *        key returned by {@code defaultAppbaseClientKey(configuration)}
     * @param appbaseClientPoolManager manager supplying the pool
     * @param appbaseClientServiceConfigurationType concrete configuration class
     * @param appbaseClientServiceConfiguration settings consumed during startup
     * @param registerAsConfigurationListener whether changes trigger reinitialization
     * @param additionalConfigurationTypesToRegister extra polymorphic types, or {@code null}
     */
    public AbstractAppbaseClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<V> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final Class<C> appbaseClientServiceConfigurationType,
            final C appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable
            final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore.getAppbaseCore(),
                appbaseClientServiceConfigurationType,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");
        AppbaseUtil.notNull(appbaseClientPoolManager, "appbaseClientPoolManager");

        this.defaultAppbaseClientKeyRef = new AtomicReference<>(defaultAppbaseClientKey);
        this.appbaseClientPoolManagerRef = new AtomicReference<>(appbaseClientPoolManager);
        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPoolRef = new AtomicReference<>();
        this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap = new ConcurrentHashMap<>();

        this.appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap = new ConcurrentHashMap<>();
        this.registerAppbaseClientIntegrationServiceCounterByTypeNameMap = new ConcurrentHashMap<>();
    }



    /**
     * Initialize the {@link AppbaseClientKey} in {@link #defaultAppbaseClientKeyRef}.
     *
     * <p>
     *     <b>NOTE:</b> If this method is implemented to return {@code null},
     *     {@link #sanityCheck(AppbaseClientServiceConfiguration, Set)} (Set)} ()} must also be implemented accordingly,
     *     since it expects a default {@link AppbaseClientKey} to be set.
     * </p>
     *
     * @param configuration Implementation of {@link AppbaseClientServiceConfiguration}
     * @return {@link AppbaseClientKey} or {@code null} if it's already set in the constructor or if
     * {@link #getClientIntegrationService()} is not used.
     */
    protected abstract AppbaseClientKey<V> defaultAppbaseClientKey(C configuration);



    /**
     * Initialize the backing {@link AppbaseClientIntegrationService}s and get the result.
     *
     * @param configuration Implementation of {@link AppbaseClientServiceConfiguration}
     * @return Initialized {@link AppbaseClientIntegrationService} by the {@link AppbaseClientKey}
     */
    protected abstract Map<AppbaseClientKey<V>, AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServices(
            C configuration);



    /**
     * This method can be overridden to create a new instance of {@link AppbaseClientIntegrationService} on-demand. Further,
     * this instance can also be added to {@link AppbaseClientPool}.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @return {@link AppbaseClientIntegrationService}
     */
    protected AppbaseClientIntegrationService<T, V> appbaseClientIntegrationServiceNotFound(
            final AppbaseClientKey<V> appbaseClientKey)
    {
        throw new AppbaseRuntimeException(
                AppbaseClientIntegrationService.class.getName() + " for " + appbaseClientKey + " not found");
    }


    /**
     * Override this method for any custom initialization logic by the subclass.
     *
     * @param configuration Implementation of {@link AppbaseClientServiceConfiguration}
     * @throws AppbaseException if there were any errors
     */
    protected void postInit(final C configuration) throws AppbaseException {
        // No-op
    }



    /**
     * Used to check if the initialization was successful.
     *
     * @param configuration {@link AppbaseClientServiceConfiguration}
     * @param registeredAppbaseClientKeys Registered {@link AppbaseClientKey}
     */
    protected void sanityCheck(
            final C configuration,
            final Set<AppbaseClientKey<V>> registeredAppbaseClientKeys)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final AppbaseClientPool<T, V> appbaseClientPool = getAppbaseClientPool();
        AppbaseUtil.notNull(appbaseClientPool, "appbaseClientPool");

        final AppbaseClientKey<V> defaultAppbaseClientKey = getDefaultAppbaseClientKey();
        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");

        final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService = getClientIntegrationService();
        checkAppbaseClientIntegrationService(appbaseClientIntegrationService);

        if (configuration.isCheckKeyedAppbaseClientIntegrationServicesSanity()) {
            registeredAppbaseClientKeys
                    .forEach(appbaseClientKey -> {
                        AppbaseClientIntegrationService<T, V> pooledClientIntegrationService = null;
                        try {
                            pooledClientIntegrationService = getClientIntegrationService(appbaseClientKey);
                        } catch (AppbaseClientIntegrationService.NotFoundException anfe) {
                            throw new AppbaseRuntimeException(AppbaseClientIntegrationService.class.getName() +
                                    " not found for" + appbaseClientKey);
                        } finally {
                            if (pooledClientIntegrationService != null) {
                                pooledClientIntegrationService.finalizeOperation();
                            }
                        }
                    });
        }
    }

    private void checkAppbaseClientIntegrationService(
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");
        appbaseClientIntegrationService.finalizeOperation();
    }

    /** {@inheritDoc} */
    @Override
    protected void initImpl(final C configuration) throws AppbaseException {
        initAppbaseClientPoolManager(configuration);
        initAppbaseClientPool(configuration);

        final AppbaseClientKey<V> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");
        defaultAppbaseClientKeyRef.set(defaultAppbaseClientKey);

        final Set<AppbaseClientKeyConfiguration<?>> disabledRegisterNotFoundAppbaseClientIntegrationServices =
                configuration.getDisabledRegisterNotFoundAppbaseClientIntegrationServices();
        if (disabledRegisterNotFoundAppbaseClientIntegrationServices != null) {
            disabledRegisterNotFoundAppbaseClientIntegrationServices.forEach(appbaseClientKeyConfiguration -> {
                final AppbaseClientKey<?> appbaseClientKey = AppbaseClientUtil.appbaseClientKeyFromPool(
                        appbaseClientCore,
                        appbaseClientKeyConfiguration);
                this.disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap.put(appbaseClientKey, true);
            });
        }

        final Map<AppbaseClientKey<V>, AppbaseClientIntegrationService<T, V>>
                appbaseClientKeyAppbaseClientIntegrationServices = appbaseClientIntegrationServices(configuration);
        final Set<AppbaseClientKey<V>> registeredAppbaseClientKeys = new HashSet<>();
        if (appbaseClientKeyAppbaseClientIntegrationServices != null) {
            appbaseClientKeyAppbaseClientIntegrationServices.forEach((appbaseClientKey, appbaseClientIntegrationService) -> {
                registerAppbaseClientIntegrationService(appbaseClientKey, appbaseClientIntegrationService);
                registeredAppbaseClientKeys.add(appbaseClientKey);
            });
        }

        sanityCheck(configuration, registeredAppbaseClientKeys);
        registerService();
        postInit(configuration);
    }

    /**
     * Registers this service with the core's service registry. Initialization calls it after the
     * configuration sanity check and before the configuration-specific post-initialization hook.
     * Subclasses may override it to change or skip the registration.
     */
    protected void registerService() {
        appbaseClientCore.getAppbaseCore().getServiceRegistry().registerAppbaseService(this);
    }

    private void initAppbaseClientPoolManager(final C configuration) {
        AppbaseUtil.notNull(configuration, "configuration");

        final InjectedTypeConfiguration<AppbaseClientPoolManager> appbaseClientPoolManagerInjectedTypeConfiguration =
                configuration.getAppbaseClientPoolManagerInjectedTypeConfiguration();
        if (appbaseClientPoolManagerInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore
                    .getAppbaseCore()
                    .getGuiceInjectorService();
            final AppbaseClientPoolManager appbaseClientPoolManager =
                    AppbaseUtil.injectedInstance(guiceInjectorService, appbaseClientPoolManagerInjectedTypeConfiguration);
            appbaseClientPoolManagerRef.set(appbaseClientPoolManager);
        } else {
            final AppbaseClientPoolManagerConfiguration customAppbaseClientPoolManagerConfiguration =
                    configuration.getCustomAppbaseClientPoolManagerConfiguration();
            if (customAppbaseClientPoolManagerConfiguration != null) {
                final AppbaseClientPoolManager appbaseClientPoolManager =
                        appbaseClientPoolManager(customAppbaseClientPoolManagerConfiguration);
                appbaseClientPoolManagerRef.set(appbaseClientPoolManager);
            }
        }
    }

    private AppbaseClientPoolManager appbaseClientPoolManager(
            final AppbaseClientPoolManagerConfiguration appbaseClientPoolManagerConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientPoolManagerConfiguration, "appbaseClientPoolManagerConfiguration");

        if (DefaultAppbaseClientPoolManagerConfiguration
                .class
                .isAssignableFrom(appbaseClientPoolManagerConfiguration.getClass()))
        {
            return new DefaultAppbaseClientPoolManager(
                    appbaseClientCore,
                    (DefaultAppbaseClientPoolManagerConfiguration) appbaseClientPoolManagerConfiguration);
        }

        throw new AppbaseRuntimeException("Unsupported configuration type: " +
                appbaseClientPoolManagerConfiguration.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private void initAppbaseClientPool(final C configuration) {
        AppbaseUtil.notNull(configuration, "configuration");

        final InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
                appbaseClientPoolInjectedTypeConfiguration =
                configuration.getAppbaseClientPoolInjectedTypeConfiguration();
        if (appbaseClientPoolInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore
                    .getAppbaseCore()
                    .getGuiceInjectorService();
            final AppbaseClientPool<T, V> appbaseClientPool =
                    (AppbaseClientPool<T, V>)
                            AppbaseUtil.injectedInstance(guiceInjectorService, appbaseClientPoolInjectedTypeConfiguration);
            appbaseClientPoolRef.set(appbaseClientPool);
        } else {
            final AppbaseClientPoolConfiguration appbaseClientPoolConfiguration =
                    configuration.getCustomAppbaseClientPoolConfiguration();
            if (appbaseClientPoolConfiguration != null) {
                final AppbaseClientPool<T, V> appbaseClientPool = appbaseClientPool(appbaseClientPoolConfiguration);
                appbaseClientPoolRef.set(appbaseClientPool);
            }
        }

        if (getAppbaseClientPool() == null) {
            throw new AppbaseRuntimeException(AppbaseClientPool.class.getName() + " must not be null");
        }
    }

    private AppbaseClientPool<T, V> appbaseClientPool(
            final AppbaseClientPoolConfiguration appbaseClientPoolConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientPoolConfiguration, "appbaseClientPoolConfiguration");

        if (DefaultAppbaseClientPoolConfiguration
                .class
                .isAssignableFrom(appbaseClientPoolConfiguration.getClass()))
        {
            final AppbaseClientPoolManager appbaseClientPoolManager = appbaseClientPoolManagerRef.get();
            if (appbaseClientPoolManager == null) {
                throw new AppbaseRuntimeException("appbaseClientPoolManager must not be null");
            }

            return new DefaultAppbaseClientPool<>(
                    appbaseClientCore,
                    appbaseClientPoolManager,
                    (DefaultAppbaseClientPoolConfiguration) appbaseClientPoolConfiguration);
        }

        throw new AppbaseRuntimeException("Unsupported configuration type: " +
                appbaseClientPoolConfiguration.getClass().getName());
    }

    /** {@inheritDoc} */
    @Override
    protected void shutdownImpl() {
        final AppbaseClientPool<T, V> appbaseClientPool = appbaseClientPoolRef.get();

        if (appbaseClientPool != null) {
            appbaseClientPool.shutdown();
        }
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientIntegrationService<T, V> getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        final AppbaseClientKey<V> defaultAppbaseClientKey = getDefaultAppbaseClientKey();
        AppbaseUtil.notNull(defaultAppbaseClientKey, "defaultAppbaseClientKey");

        return getClientIntegrationService(defaultAppbaseClientKey);
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientIntegrationService<T, V> getClientIntegrationService(
            final AppbaseClientKey<V> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");

        AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService = getAppbaseClientPool()
                .takeAppbaseClientIntegrationService(appbaseClientKey);

        if (appbaseClientIntegrationService == null) {
            appbaseClientIntegrationService = appbaseClientIntegrationServiceNotFound(appbaseClientKey);
        }

        if (appbaseClientIntegrationService == null) {
            throw new AppbaseClientIntegrationService.NotFoundException(
                    "No " + AppbaseClientIntegrationService.class.getName() + " found for " + appbaseClientKey);
        } else {
            registerAppbaseClientIntegrationService(appbaseClientKey, appbaseClientIntegrationService);
        }

        return appbaseClientIntegrationService;
    }

    private void registerAppbaseClientIntegrationService(
            final AppbaseClientKey<V> appbaseClientKey,
            final AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService)
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");
        AppbaseUtil.notNull(appbaseClientIntegrationService, "appbaseClientIntegrationService");

        final C configuration = getConfiguration();
        if (configuration != null) {
            if (configuration.isGlobalEnableRegisterNotFoundAppbaseClientIntegrationServices()) {
                final Boolean disabled =
                        disabledRegisterNotFoundAppbaseClientIntegrationServiceByTypeMap.get(appbaseClientKey);
                if (disabled != null && disabled) {
                    final String name = appbaseClientIntegrationService.getClass().getName();
                    appbaseClientIntegrationServiceRegisterDisabledByTypeNameMap
                            .computeIfAbsent(name, f -> {
                                final MetricService metricService = appbaseClientCore
                                        .getAppbaseCore()
                                        .getMetricService();
                                final String metricName =
                                        metricService.name(
                                                getClass(), "appbaseClientIntegrationServiceRegisterDisabled", "counter");
                                return metricService.getMetricRegistry().counter(metricName);
                            })
                            .inc();
                } else {
                    final String name = appbaseClientIntegrationService.getClass().getName();
                    registerAppbaseClientIntegrationServiceCounterByTypeNameMap
                            .computeIfAbsent(name, f -> {
                                final MetricService metricService = appbaseClientCore
                                        .getAppbaseCore()
                                        .getMetricService();
                                final String metricName =
                                        metricService.name(
                                                getClass(), "registerAppbaseClientIntegrationService", "counter");
                                return metricService.getMetricRegistry().counter(metricName);
                            })
                            .inc();
                    getAppbaseClientPool()
                            .getAppbaseClientPoolRegistry()
                            .registerAppbaseClientIntegrationService(appbaseClientKey, appbaseClientIntegrationService);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientKey<V> getDefaultAppbaseClientKey() {
        return defaultAppbaseClientKeyRef.get();
    }

    /**
     * Returns the pool this service currently holds, which initialization may replace with a pool built
     * from an injected or custom pool configuration.
     *
     * @return the current pool, or {@code null} before one has been supplied or built
     */
    protected AppbaseClientPool<T, V> getAppbaseClientPool() {
        return appbaseClientPoolRef.get();
    }
}
