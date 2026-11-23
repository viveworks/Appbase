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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.Connection;
import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.jasync.configuration.DefaultJasyncClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jasync.configuration.DefaultJasyncClientServiceConfiguration;
import com.viveworks.appbase.client.service.jasync.reactive.ReactiveJasyncAccessor;
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
 * Default implementation of {@link JasyncClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJasyncClientService
        extends AbstractAppbaseClientService<Connection, TextValue, DefaultJasyncClientServiceConfiguration>
        implements JasyncClientService
{
    @Inject
    public DefaultJasyncClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore, DefaultJasyncClientServiceConfiguration.class, false, null);

        startOnCreate();
    }

    public DefaultJasyncClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Connection, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJasyncClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJasyncClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJasyncClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJasyncClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultJasyncClientServiceConfiguration defaultJasyncClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJasyncClientServiceConfiguration.class,
                defaultJasyncClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJasyncClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Connection, TextValue> appbaseClientPool,
            final DefaultJasyncClientServiceConfiguration defaultJasyncClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJasyncClientServiceConfiguration.class,
                defaultJasyncClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJasyncClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultJasyncClientServiceConfiguration defaultJasyncClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJasyncClientServiceConfiguration.class,
                defaultJasyncClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultJasyncClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultJasyncClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultJasyncClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultJasyncClientIntegrationServiceConfiguration defaultDefaultJasyncClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJasyncClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJasyncClientIntegrationServiceConfiguration,
                "defaultDefaultJasyncClientIntegrationServiceConfiguration");

        return JasyncClientUtil.appbaseClientKey(defaultDefaultJasyncClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<Connection, TextValue>>
    appbaseClientIntegrationServices(final DefaultJasyncClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");


        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<Connection, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultJasyncClientIntegrationServiceConfiguration defaultDefaultJasyncClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJasyncClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJasyncClientIntegrationServiceConfiguration,
                "defaultDefaultJasyncClientIntegrationServiceConfiguration");


        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultJasyncClientIntegrationService defaultDefaultJasyncClientIntegrationService =
                new DefaultJasyncClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultJasyncClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultJasyncClientIntegrationService);


        final Set<DefaultJasyncClientIntegrationServiceConfiguration>
                keyedDefaultJasyncClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultJasyncClientIntegrationServiceConfigurations();
        if (keyedDefaultJasyncClientIntegrationServiceConfigurations != null) {
            keyedDefaultJasyncClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultJasyncClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                JasyncClientUtil.appbaseClientKey(keyedDefaultJasyncClientIntegrationServiceConfiguration);
                        final DefaultJasyncClientIntegrationService defaultJasyncClientIntegrationService =
                              new DefaultJasyncClientIntegrationService(
                                      appbaseClientCore,
                                      getAppbaseClientPool(),
                                      appbaseClientKey,
                                      keyedDefaultJasyncClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultJasyncClientIntegrationService);
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
                .registerAppbaseService(JasyncClientService.class, this);
    }

    @Override
    public JasyncClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JasyncClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public JasyncClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JasyncClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveJasyncAccessor getReactiveAccessor() {
        return new ReactiveJasyncAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveJasyncAccessor getReactiveAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new ReactiveJasyncAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public JasyncAccessor getAccessor() {
        return new JasyncAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public JasyncAccessor getAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new JasyncAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
