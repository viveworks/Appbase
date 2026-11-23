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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.vitess.accessor.VitessGrpcAccessor;
import com.viveworks.appbase.client.service.vitess.accessor.reactive.ReactiveVitessGrpcAccessor;
import com.viveworks.appbase.client.service.vitess.configuration.DefaultVitessGrpcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.vitess.configuration.DefaultVitessGrpcClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link VitessGrpcClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultVitessGrpcClientService
        extends AbstractAppbaseClientService<VitessGrpcClient, ClusterIdValue, DefaultVitessGrpcClientServiceConfiguration>
        implements VitessGrpcClientService
{
    @Inject
    public DefaultVitessGrpcClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultVitessGrpcClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultVitessGrpcClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultVitessGrpcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultVitessGrpcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<VitessGrpcClient, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultVitessGrpcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultVitessGrpcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultVitessGrpcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultVitessGrpcClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultVitessGrpcClientServiceConfiguration defaultVitessGrpcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultVitessGrpcClientServiceConfiguration.class,
                defaultVitessGrpcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultVitessGrpcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<VitessGrpcClient, ClusterIdValue> appbaseClientPool,
            final DefaultVitessGrpcClientServiceConfiguration defaultVitessGrpcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultVitessGrpcClientServiceConfiguration.class,
                defaultVitessGrpcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultVitessGrpcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultVitessGrpcClientServiceConfiguration defaultVitessGrpcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultVitessGrpcClientServiceConfiguration.class,
                defaultVitessGrpcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultVitessGrpcClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultVitessGrpcClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultVitessGrpcClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultVitessGrpcClientIntegrationServiceConfiguration
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultVitessGrpcClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration,
                "defaultDefaultVitessGrpcClientIntegrationServiceConfiguration");

        return VitessGrpcClientUtil.appbaseClientKey(defaultDefaultVitessGrpcClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<VitessGrpcClient, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultVitessGrpcClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<VitessGrpcClient, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultVitessGrpcClientIntegrationServiceConfiguration
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultVitessGrpcClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultVitessGrpcClientIntegrationServiceConfiguration,
                "defaultDefaultVitessGrpcClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultVitessGrpcClientIntegrationService defaultDefaultVitessGrpcClientIntegrationService =
                new DefaultVitessGrpcClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultVitessGrpcClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultVitessGrpcClientIntegrationService);


        final Set<DefaultVitessGrpcClientIntegrationServiceConfiguration>
                keyedDefaultVitessGrpcClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultVitessGrpcClientIntegrationServiceConfigurations();
        if (keyedDefaultVitessGrpcClientIntegrationServiceConfigurations != null) {
            keyedDefaultVitessGrpcClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultVitessGrpcClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                VitessGrpcClientUtil.appbaseClientKey(
                                        keyedDefaultVitessGrpcClientIntegrationServiceConfiguration);
                        final DefaultVitessGrpcClientIntegrationService defaultVitessGrpcClientIntegrationService =
                                new DefaultVitessGrpcClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultVitessGrpcClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultVitessGrpcClientIntegrationService);
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
                .registerAppbaseService(VitessGrpcClientService.class, this);
    }

    @Override
    public VitessGrpcClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (VitessGrpcClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public VitessGrpcClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (VitessGrpcClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveVitessGrpcAccessor getReactiveAccessor() {
        return new ReactiveVitessGrpcAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveVitessGrpcAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new ReactiveVitessGrpcAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public VitessGrpcAccessor getAccessor() {
        return new VitessGrpcAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public VitessGrpcAccessor getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new VitessGrpcAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
