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

package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link JdbcClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJdbcClientService
        extends AbstractAppbaseClientService<DataSource, TextValue, DefaultJdbcClientServiceConfiguration>
        implements JdbcClientService
{
    @Inject
    public DefaultJdbcClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultJdbcClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultJdbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJdbcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJdbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<DataSource, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJdbcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJdbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJdbcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJdbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultJdbcClientServiceConfiguration defaultJdbcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJdbcClientServiceConfiguration.class,
                defaultJdbcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJdbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<DataSource, TextValue> appbaseClientPool,
            final DefaultJdbcClientServiceConfiguration defaultJdbcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJdbcClientServiceConfiguration.class,
                defaultJdbcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJdbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultJdbcClientServiceConfiguration defaultJdbcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJdbcClientServiceConfiguration.class,
                defaultJdbcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultJdbcClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultJdbcClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultJdbcClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultJdbcClientIntegrationServiceConfiguration defaultDefaultJdbcClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJdbcClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJdbcClientIntegrationServiceConfiguration,
                "defaultDefaultJdbcClientIntegrationServiceConfiguration");

        return JdbcClientUtil.appbaseClientKey(defaultDefaultJdbcClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<DataSource, TextValue>>
    appbaseClientIntegrationServices(final DefaultJdbcClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<DataSource, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultJdbcClientIntegrationServiceConfiguration defaultDefaultJdbcClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJdbcClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJdbcClientIntegrationServiceConfiguration,
                "defaultDefaultJdbcClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultJdbcClientIntegrationService defaultDefaultJdbcClientIntegrationService =
                new DefaultJdbcClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultJdbcClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultJdbcClientIntegrationService);


        final Set<DefaultJdbcClientIntegrationServiceConfiguration> keyedDefaultJdbcClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultJdbcClientIntegrationServiceConfigurations();
        if (keyedDefaultJdbcClientIntegrationServiceConfigurations != null) {
            keyedDefaultJdbcClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultJdbcClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                               JdbcClientUtil.appbaseClientKey(keyedDefaultJdbcClientIntegrationServiceConfiguration);
                        final DefaultJdbcClientIntegrationService defaultJdbcClientIntegrationService =
                                new DefaultJdbcClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultJdbcClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultJdbcClientIntegrationService);
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
                .registerAppbaseService(JdbcClientService.class, this);
    }

    @Override
    public JdbcClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JdbcClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public JdbcClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JdbcClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
