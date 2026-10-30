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

package com.viveworks.appbase.client.service.druid.sql;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClient;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlParameterFactory;
import com.viveworks.appbase.client.service.druid.sql.configuration.DefaultDruidSqlClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.druid.sql.configuration.DefaultDruidSqlClientServiceConfiguration;
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
 * Default implementation of {@link DruidSqlClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidSqlClientService
        extends AbstractAppbaseClientService
        <DruidSqlClient, ClusterIdValue, DefaultDruidSqlClientServiceConfiguration>
        implements DruidSqlClientService
{
    private final DruidSqlClientSharedResources druidSqlClientSharedResources;
    private final DruidSqlParameterFactory druidSqlParameterFactory;

    @Inject
    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory)
    {
        super(appbaseClientCore, DefaultDruidSqlClientServiceConfiguration.class, false, null);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultDruidSqlClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<DruidSqlClient, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultDruidSqlClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultDruidSqlClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final DefaultDruidSqlClientServiceConfiguration defaultDruidSqlClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultDruidSqlClientServiceConfiguration.class,
                defaultDruidSqlClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<DruidSqlClient, ClusterIdValue> appbaseClientPool,
            final DefaultDruidSqlClientServiceConfiguration defaultDruidSqlClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultDruidSqlClientServiceConfiguration.class,
                defaultDruidSqlClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public DefaultDruidSqlClientService(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultDruidSqlClientServiceConfiguration defaultDruidSqlClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultDruidSqlClientServiceConfiguration.class,
                defaultDruidSqlClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidSqlParameterFactory, "druidSqlParameterFactory");

        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidSqlParameterFactory = druidSqlParameterFactory;

        startOnCreate();
    }

    public static void registerObjectMapperSubtypes() {
        DefaultDruidSqlClientSharedResources.registerObjectMapperSubtypes();
        AppbaseCoreObjectMapperSubTypes.register(DefaultDruidSqlClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultDruidSqlClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultDruidSqlClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultDruidSqlClientIntegrationServiceConfiguration defaultDruidSqlClientIntegrationServiceConfiguration =
                configuration.getDefaultDruidSqlClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDruidSqlClientIntegrationServiceConfiguration,
                "defaultDruidSqlClientIntegrationServiceConfiguration");

        return DruidSqlClientUtil.appbaseClientKey(defaultDruidSqlClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<DruidSqlClient, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultDruidSqlClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<DruidSqlClient, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultDruidSqlClientIntegrationServiceConfiguration defaultDruidSqlClientIntegrationServiceConfiguration =
                configuration.getDefaultDruidSqlClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDruidSqlClientIntegrationServiceConfiguration,
                "defaultDruidSqlClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultDruidSqlClientIntegrationService defaultDefaultDruidSqlClientIntegrationService =
                new DefaultDruidSqlClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        druidSqlClientSharedResources,
                        defaultDruidSqlClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultDruidSqlClientIntegrationService);

        final Set<DefaultDruidSqlClientIntegrationServiceConfiguration>
                keyedDefaultDruidSqlClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultDruidSqlClientIntegrationServiceConfigurations();
        if (keyedDefaultDruidSqlClientIntegrationServiceConfigurations != null) {
            keyedDefaultDruidSqlClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultDruidSqlClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                DruidSqlClientUtil
                                        .appbaseClientKey(keyedDefaultDruidSqlClientIntegrationServiceConfiguration);
                        final DefaultDruidSqlClientIntegrationService defaultDruidSqlClientIntegrationService =
                                new DefaultDruidSqlClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        druidSqlClientSharedResources,
                                        keyedDefaultDruidSqlClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultDruidSqlClientIntegrationService);
                    });
        }

        return appbaseClientIntegrationServices;
    }

    @Override
    public DruidSqlParameterFactory getDruidSqlParameterFactory() {
        return druidSqlParameterFactory;
    }

    @Override
    public DruidSqlClientSharedResources getDruidSqlClientSharedResources() {
        return druidSqlClientSharedResources;
    }

    @Override
    protected void registerService() {
        super.registerService();

        appbaseClientCore
                .getAppbaseCore()
                .getServiceRegistry()
                .registerAppbaseService(DruidSqlClientService.class, this);
    }

    @Override
    public DruidSqlClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (DruidSqlClientIntegrationService) super.getClientIntegrationService();
    }


    @Override
    public DruidSqlClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (DruidSqlClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
