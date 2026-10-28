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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.r2dbc.accessor.reactive.ReactiveR2dbcAccessor;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.r2dbc.pool.ConnectionPool;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link R2dbcClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultR2dbcClientService
        extends AbstractAppbaseClientService<ConnectionPool, TextValue, DefaultR2dbcClientServiceConfiguration>
        implements R2dbcClientService
{
    @Inject
    public DefaultR2dbcClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultR2dbcClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultR2dbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultR2dbcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultR2dbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultR2dbcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultR2dbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultR2dbcClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultR2dbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultR2dbcClientServiceConfiguration defaultR2dbcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultR2dbcClientServiceConfiguration.class,
                defaultR2dbcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultR2dbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool,
            final DefaultR2dbcClientServiceConfiguration defaultR2dbcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultR2dbcClientServiceConfiguration.class,
                defaultR2dbcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultR2dbcClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultR2dbcClientServiceConfiguration defaultR2dbcClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultR2dbcClientServiceConfiguration.class,
                defaultR2dbcClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultR2dbcClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultR2dbcClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultR2dbcClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultR2dbcClientIntegrationServiceConfiguration defaultDefaultR2dbcClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultR2dbcClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultR2dbcClientIntegrationServiceConfiguration,
                "defaultDefaultR2dbcClientIntegrationServiceConfiguration");

        return R2dbcClientUtil.appbaseClientKey(defaultDefaultR2dbcClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<ConnectionPool, TextValue>>
    appbaseClientIntegrationServices(final DefaultR2dbcClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<ConnectionPool, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultR2dbcClientIntegrationServiceConfiguration defaultDefaultR2dbcClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultR2dbcClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultR2dbcClientIntegrationServiceConfiguration,
                "defaultDefaultR2dbcClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultR2dbcClientIntegrationService defaultDefaultR2dbcClientIntegrationService =
                new DefaultR2dbcClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultR2dbcClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultR2dbcClientIntegrationService);


        final Set<DefaultR2dbcClientIntegrationServiceConfiguration> keyedDefaultR2dbcClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultR2dbcClientIntegrationServiceConfigurations();
        if (keyedDefaultR2dbcClientIntegrationServiceConfigurations != null) {
            keyedDefaultR2dbcClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultR2dbcClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                R2dbcClientUtil.appbaseClientKey(keyedDefaultR2dbcClientIntegrationServiceConfiguration);
                        final DefaultR2dbcClientIntegrationService defaultR2dbcClientIntegrationService =
                                new DefaultR2dbcClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultR2dbcClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultR2dbcClientIntegrationService);
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
                .registerAppbaseService(R2dbcClientService.class, this);
    }

    @Override
    public R2dbcClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (R2dbcClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public R2dbcClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (R2dbcClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveR2dbcAccessor getReactiveAccessor() {
        return new ReactiveR2dbcAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveR2dbcAccessor getReactiveAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new ReactiveR2dbcAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
