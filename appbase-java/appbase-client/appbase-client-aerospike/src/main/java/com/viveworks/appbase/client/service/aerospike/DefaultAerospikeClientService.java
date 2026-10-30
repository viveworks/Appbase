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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.reactor.AerospikeReactorClient;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientServiceConfiguration;
import com.viveworks.appbase.client.service.aerospike.reactive.ReactiveAerospikeAccessor;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link AerospikeClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAerospikeClientService
        extends AbstractAppbaseClientService<IAerospikeClient, ClusterIdValue, DefaultAerospikeClientServiceConfiguration>
        implements AerospikeClientService
{
    /**
     * Cache of non-owning {@link IAerospikeReactorClient} wrappers by {@link AppbaseClientKey}, populated
     * eagerly (once per key) by {@link #getReactiveAccessor(AppbaseClientKey)} and cleared -- never
     * closed -- on {@link #shutdownImpl()}.
     */
    private final ConcurrentMap<AppbaseClientKey<ClusterIdValue>, IAerospikeReactorClient> reactiveClients =
            new ConcurrentHashMap<>();

    @Inject
    public DefaultAerospikeClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultAerospikeClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultAerospikeClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<IAerospikeClient, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultAerospikeClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultAerospikeClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAerospikeClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultAerospikeClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<IAerospikeClient, ClusterIdValue> appbaseClientPool,
            final DefaultAerospikeClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultAerospikeClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultAerospikeClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultAerospikeClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultAerospikeClientServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultAerospikeClientIntegrationServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultAerospikeClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultAerospikeClientIntegrationServiceConfiguration
                defaultDefaultAerospikeClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultAerospikeClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAerospikeClientIntegrationServiceConfiguration,
                "defaultDefaultAerospikeClientIntegrationServiceConfiguration");

        return AerospikeClientUtil.appbaseClientKey(defaultDefaultAerospikeClientIntegrationServiceConfiguration);
    }

    @Override
    protected
    Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultAerospikeClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>,
                AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue>> appbaseClientIntegrationServices =
                new ConcurrentHashMap<>();


        final DefaultAerospikeClientIntegrationServiceConfiguration
                defaultDefaultAerospikeClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultAerospikeClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAerospikeClientIntegrationServiceConfiguration,
                "defaultDefaultAerospikeClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultAerospikeClientIntegrationService defaultDefaultAerospikeClientIntegrationService =
                new DefaultAerospikeClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultAerospikeClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultAerospikeClientIntegrationService);


        final Set<DefaultAerospikeClientIntegrationServiceConfiguration>
                keyedDefaultAerospikeClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultAerospikeClientIntegrationServiceConfigurations();
        if (keyedDefaultAerospikeClientIntegrationServiceConfigurations != null) {
            keyedDefaultAerospikeClientIntegrationServiceConfigurations.forEach(
                    defaultAerospikeClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                AerospikeClientUtil.appbaseClientKey(defaultAerospikeClientIntegrationServiceConfiguration);
                        final DefaultAerospikeClientIntegrationService defaultAerospikeClientIntegrationService =
                                new DefaultAerospikeClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        defaultAerospikeClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultAerospikeClientIntegrationService);
                    });
        }


        return appbaseClientIntegrationServices;
    }

    @Override
    protected void registerService() {
        super.registerService();

        appbaseClientCore
                .getAppbaseCore()
                .getServiceRegistry()
                .registerAppbaseService(AerospikeClientService.class, this);
    }

    @Override
    public AerospikeClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (AerospikeClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public AerospikeClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (AerospikeClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveAerospikeAccessor getReactiveAccessor() {
        return getReactiveAccessor(getDefaultAppbaseClientKey());
    }

    @Override
    public ReactiveAerospikeAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        final IAerospikeReactorClient reactorClient =
                reactiveClients.computeIfAbsent(appbaseClientKey, this::buildReactorClient);

        return new ReactiveAerospikeAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                reactorClient);
    }

    @Override
    public AerospikeAccessor getAccessor() {
        return new AerospikeAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public AerospikeAccessor getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new AerospikeAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    /**
     * Builds the non-owning {@link AerospikeReactorClient} wrapping the integration service's shared
     * {@link IAerospikeClient} + {@link EventLoops}. Cached per key by
     * {@link #getReactiveAccessor}; NEVER closed (its close() would close the shared sync client -- a
     * double-close hazard). Protected so tests can supply a mock.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @return the non-owning {@link IAerospikeReactorClient} for the given key
     */
    protected IAerospikeReactorClient buildReactorClient(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        try {
            final AerospikeClientIntegrationService integrationService = getClientIntegrationService(appbaseClientKey);
            return new AerospikeReactorClient(integrationService.getClient(), integrationService.getEventLoops());
        } catch (final AppbaseClientIntegrationService.NotFoundException e) {
            throw new AppbaseRuntimeException("No Aerospike integration service for key: " + appbaseClientKey, e);
        }
    }

    /**
     * Clears the cached reactor-client wrappers. They are NON-OWNING -- the integration service owns
     * and closes the shared {@link IAerospikeClient}; closing these here would double-close it. So we
     * only drop the references.
     */
    @Override
    protected void shutdownImpl() {
        super.shutdownImpl();
        reactiveClients.clear();
    }
}
