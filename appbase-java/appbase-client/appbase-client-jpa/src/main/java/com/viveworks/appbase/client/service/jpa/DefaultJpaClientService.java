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

package com.viveworks.appbase.client.service.jpa;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.jpa.configuration.DefaultJpaClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jpa.configuration.DefaultJpaClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

import javax.annotation.Nullable;
import javax.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link JpaClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJpaClientService
        extends AbstractAppbaseClientService<EntityManager, TextValue, DefaultJpaClientServiceConfiguration>
        implements JpaClientService
{
    @Inject
    public DefaultJpaClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultJpaClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultJpaClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJpaClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJpaClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<EntityManager, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJpaClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJpaClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJpaClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJpaClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultJpaClientServiceConfiguration defaultJpaClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultJpaClientServiceConfiguration.class,
                defaultJpaClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJpaClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<EntityManager, TextValue> appbaseClientPool,
            final DefaultJpaClientServiceConfiguration defaultJpaClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultJpaClientServiceConfiguration.class,
                defaultJpaClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultJpaClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultJpaClientServiceConfiguration defaultJpaClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultJpaClientServiceConfiguration.class,
                defaultJpaClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultJpaClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultJpaClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(final DefaultJpaClientServiceConfiguration configuration) {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultJpaClientIntegrationServiceConfiguration defaultDefaultJpaClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJpaClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJpaClientIntegrationServiceConfiguration,
                "defaultDefaultJpaClientIntegrationServiceConfiguration");

        return JpaClientUtil.appbaseClientKey(defaultDefaultJpaClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<EntityManager, TextValue>>
    appbaseClientIntegrationServices(final DefaultJpaClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<EntityManager, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultJpaClientIntegrationServiceConfiguration defaultDefaultJpaClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultJpaClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultJpaClientIntegrationServiceConfiguration,
                "defaultDefaultJpaClientIntegrationServiceConfiguration");


        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultJpaClientIntegrationService defaultDefaultJpaClientIntegrationService =
                new DefaultJpaClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultJpaClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultJpaClientIntegrationService);


        final Set<DefaultJpaClientIntegrationServiceConfiguration> keyedDefaultJpaClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultJpaClientIntegrationServiceConfigurations();
        if (keyedDefaultJpaClientIntegrationServiceConfigurations != null) {
            keyedDefaultJpaClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultJpaClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                JpaClientUtil.appbaseClientKey(keyedDefaultJpaClientIntegrationServiceConfiguration);
                        final DefaultJpaClientIntegrationService defaultJpaClientIntegrationService =
                                new DefaultJpaClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultJpaClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultJpaClientIntegrationService);
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
                .registerAppbaseService(JpaClientService.class, this);
    }

    @Override
    public JpaClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JpaClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public JpaClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (JpaClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
