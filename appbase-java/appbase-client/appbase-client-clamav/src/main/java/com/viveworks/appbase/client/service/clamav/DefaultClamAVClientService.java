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

package com.viveworks.appbase.client.service.clamav;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.clamav.client.ClamAVClient;
import com.viveworks.appbase.client.service.clamav.client.ClamAVClientSharedResources;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClientSharedResources;
import com.viveworks.appbase.client.service.clamav.configuration.DefaultClamAVClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.clamav.configuration.DefaultClamAVClientServiceConfiguration;
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
 * Default implementation of {@link ClamAVClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVClientService
        extends AbstractAppbaseClientService<ClamAVClient, TextValue, DefaultClamAVClientServiceConfiguration>
        implements ClamAVClientService
{
    private final ClamAVClientSharedResources clamAVClientSharedResources;

    @Inject
    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources)
    {
        super(appbaseClientCore, DefaultClamAVClientServiceConfiguration.class, false, null);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultClamAVClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<ClamAVClient, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultClamAVClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultClamAVClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final DefaultClamAVClientServiceConfiguration defaultClamAVClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultClamAVClientServiceConfiguration.class,
                defaultClamAVClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<ClamAVClient, TextValue> appbaseClientPool,
            final DefaultClamAVClientServiceConfiguration defaultClamAVClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultClamAVClientServiceConfiguration.class,
                defaultClamAVClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public DefaultClamAVClientService(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultClamAVClientServiceConfiguration defaultClamAVClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultClamAVClientServiceConfiguration.class,
                defaultClamAVClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        this.clamAVClientSharedResources = clamAVClientSharedResources;

        startOnCreate();
    }

    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultClamAVClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultClamAVClientServiceConfiguration.class);
        DefaultClamAVClientSharedResources.registerObjectMapperSubtypes();
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultClamAVClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultClamAVClientIntegrationServiceConfiguration defaultDefaultClamAVClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultClamAVClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultClamAVClientIntegrationServiceConfiguration,
                "defaultDefaultClamAVClientIntegrationServiceConfiguration");

        return ClamAVClientUtil.appbaseClientKey(defaultDefaultClamAVClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<ClamAVClient, TextValue>>
    appbaseClientIntegrationServices(final DefaultClamAVClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<ClamAVClient, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultClamAVClientIntegrationServiceConfiguration defaultDefaultClamAVClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultClamAVClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultClamAVClientIntegrationServiceConfiguration,
                "defaultDefaultClamAVClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultClamAVClientIntegrationService defaultDefaultClamAVClientIntegrationService =
                new DefaultClamAVClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        clamAVClientSharedResources,
                        defaultDefaultClamAVClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultClamAVClientIntegrationService);


        final Set<DefaultClamAVClientIntegrationServiceConfiguration>
                keyedDefaultClamAVClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultClamAVClientIntegrationServiceConfigurations();
        if (keyedDefaultClamAVClientIntegrationServiceConfigurations != null) {
            keyedDefaultClamAVClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultClamAVClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                ClamAVClientUtil.appbaseClientKey(keyedDefaultClamAVClientIntegrationServiceConfiguration);
                        final DefaultClamAVClientIntegrationService defaultClamAVClientIntegrationService =
                                new DefaultClamAVClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        clamAVClientSharedResources,
                                        keyedDefaultClamAVClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultClamAVClientIntegrationService);
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
                .registerAppbaseService(ClamAVClientService.class, this);
    }

    @Override
    public ClamAVClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (ClamAVClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public ClamAVClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (ClamAVClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
