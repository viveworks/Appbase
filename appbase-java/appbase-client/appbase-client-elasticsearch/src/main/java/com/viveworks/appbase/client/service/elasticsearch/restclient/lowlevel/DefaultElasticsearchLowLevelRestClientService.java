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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.elasticsearch.client.RestClient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link ElasticsearchLowLevelRestClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultElasticsearchLowLevelRestClientService
        extends AbstractAppbaseClientService
        <RestClient, ClusterIdValue, DefaultElasticsearchLowLevelRestClientServiceConfiguration>
        implements ElasticsearchLowLevelRestClientService
{
    @Inject
    public DefaultElasticsearchLowLevelRestClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultElasticsearchLowLevelRestClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultElasticsearchLowLevelRestClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultElasticsearchLowLevelRestClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultElasticsearchLowLevelRestClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultElasticsearchLowLevelRestClientServiceConfiguration
                    defaultElasticsearchLowLevelRestClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                defaultElasticsearchLowLevelRestClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultElasticsearchLowLevelRestClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<RestClient, ClusterIdValue> appbaseClientPool,
            final DefaultElasticsearchLowLevelRestClientServiceConfiguration
                    defaultElasticsearchLowLevelRestClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                defaultElasticsearchLowLevelRestClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultElasticsearchLowLevelRestClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultElasticsearchLowLevelRestClientServiceConfiguration
                    defaultElasticsearchLowLevelRestClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultElasticsearchLowLevelRestClientServiceConfiguration.class,
                defaultElasticsearchLowLevelRestClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes
                .register(DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes
                .register(DefaultElasticsearchLowLevelRestClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultElasticsearchLowLevelRestClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        return ElasticsearchLowLevelRestClientUtil.appbaseClientKey(
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<RestClient, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultElasticsearchLowLevelRestClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<RestClient, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration,
                "defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultElasticsearchLowLevelRestClientIntegrationService
                defaultDefaultElasticsearchLowLevelRestClientIntegrationService =
                new DefaultElasticsearchLowLevelRestClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(
                defaultAppbaseClientKey, defaultDefaultElasticsearchLowLevelRestClientIntegrationService);


        final Set<DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration>
                keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations();
        if (keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations != null) {
            keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                ElasticsearchLowLevelRestClientUtil.appbaseClientKey(
                                        keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration);
                        final DefaultElasticsearchLowLevelRestClientIntegrationService
                                defaultElasticsearchLowLevelRestClientIntegrationService =
                                new DefaultElasticsearchLowLevelRestClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices
                                .put(appbaseClientKey, defaultElasticsearchLowLevelRestClientIntegrationService);
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
                .registerAppbaseService(ElasticsearchLowLevelRestClientService.class, this);
    }

    @Override
    public ElasticsearchLowLevelRestClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (ElasticsearchLowLevelRestClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public ElasticsearchLowLevelRestClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (ElasticsearchLowLevelRestClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
