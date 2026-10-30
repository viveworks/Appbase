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

package com.viveworks.appbase.client.service.spark;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.spark.configuration.DefaultSparkClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.spark.configuration.DefaultSparkClientServiceConfiguration;
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
 * Default implementation of {@link SparkClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultSparkClientService
        extends AbstractAppbaseClientService<SparkClient, ClusterIdValue, DefaultSparkClientServiceConfiguration>
        implements SparkClientService
{
    @Inject
    public DefaultSparkClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultSparkClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultSparkClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultSparkClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSparkClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SparkClient, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultSparkClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSparkClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultSparkClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSparkClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultSparkClientServiceConfiguration defaultSparkClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultSparkClientServiceConfiguration.class,
                defaultSparkClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSparkClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SparkClient, ClusterIdValue> appbaseClientPool,
            final DefaultSparkClientServiceConfiguration defaultSparkClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultSparkClientServiceConfiguration.class,
                defaultSparkClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultSparkClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultSparkClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultSparkClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultSparkClientIntegrationServiceConfiguration defaultDefaultSparkClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultSparkClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultSparkClientIntegrationServiceConfiguration,
                "defaultDefaultSparkClientIntegrationServiceConfiguration");

        return SparkClientUtil.appbaseClientKey(defaultDefaultSparkClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<SparkClient, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultSparkClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<SparkClient, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultSparkClientIntegrationServiceConfiguration defaultDefaultSparkClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultSparkClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultSparkClientIntegrationServiceConfiguration,
                "defaultDefaultSparkClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultSparkClientIntegrationService defaultDefaultSparkClientIntegrationService =
                new DefaultSparkClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultSparkClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultSparkClientIntegrationService);


        final Set<DefaultSparkClientIntegrationServiceConfiguration>
                keyedDefaultSparkClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultSparkClientIntegrationServiceConfigurations();
        if (keyedDefaultSparkClientIntegrationServiceConfigurations != null) {
            keyedDefaultSparkClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultSparkClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                SparkClientUtil.appbaseClientKey(keyedDefaultSparkClientIntegrationServiceConfiguration);
                        final DefaultSparkClientIntegrationService defaultSparkClientIntegrationService =
                                new DefaultSparkClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultSparkClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultSparkClientIntegrationService);
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
                .registerAppbaseService(SparkClientService.class, this);
    }

    @Override
    public SparkClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (SparkClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public SparkClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (SparkClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
