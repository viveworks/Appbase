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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientServiceConfiguration;
import com.viveworks.appbase.client.service.cassandra.reactive.ReactiveCassandraAccessor;
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
 * Default implementation of {@link CassandraClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultCassandraClientService
        extends AbstractAppbaseClientService<CqlSession, ClusterIdValue, DefaultCassandraClientServiceConfiguration>
        implements CassandraClientService
{
    @Inject
    public DefaultCassandraClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore, DefaultCassandraClientServiceConfiguration.class, false, null);

        startOnCreate();
    }

    public DefaultCassandraClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultCassandraClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultCassandraClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<CqlSession, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultCassandraClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultCassandraClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultCassandraClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultCassandraClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultCassandraClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultCassandraClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultCassandraClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<CqlSession, ClusterIdValue> appbaseClientPool,
            final DefaultCassandraClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultCassandraClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultCassandraClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultCassandraClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultCassandraClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultCassandraClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultCassandraClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultCassandraClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultCassandraClientIntegrationServiceConfiguration
                defaultDefaultCassandraClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultCassandraClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultCassandraClientIntegrationServiceConfiguration,
                "defaultDefaultCassandraClientIntegrationServiceConfiguration");

        return CassandraClientUtil.appbaseClientKey(defaultDefaultCassandraClientIntegrationServiceConfiguration);
    }

    @Override
    protected
    Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<CqlSession, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultCassandraClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<CqlSession, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultCassandraClientIntegrationServiceConfiguration
                defaultDefaultCassandraClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultCassandraClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultCassandraClientIntegrationServiceConfiguration,
                "defaultDefaultCassandraClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultCassandraClientIntegrationService defaultDefaultCassandraClientIntegrationService =
                new DefaultCassandraClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultCassandraClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultCassandraClientIntegrationService);

        final Set<DefaultCassandraClientIntegrationServiceConfiguration>
                keyedDefaultCassandraClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultCassandraClientIntegrationServiceConfigurations();
        if (keyedDefaultCassandraClientIntegrationServiceConfigurations != null) {
            keyedDefaultCassandraClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultCassandraClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                CassandraClientUtil.appbaseClientKey(
                                        keyedDefaultCassandraClientIntegrationServiceConfiguration);
                        final DefaultCassandraClientIntegrationService defaultCassandraClientIntegrationService =
                                new DefaultCassandraClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultCassandraClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultCassandraClientIntegrationService);
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
                .registerAppbaseService(CassandraClientService.class, this);
    }

    @Override
    public CassandraClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (CassandraClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public CassandraClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (CassandraClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveCassandraAccessor getReactiveAccessor() {
        return new ReactiveCassandraAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveCassandraAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new ReactiveCassandraAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public CassandraAccessor getAccessor() {
        return new CassandraAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public CassandraAccessor getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new CassandraAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
