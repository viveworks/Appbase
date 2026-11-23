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

package com.viveworks.appbase.client.service.smtp;

import com.hubspot.smtp.client.SmtpSession;
import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.smtp.configuration.DefaultSmtpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.smtp.configuration.DefaultSmtpClientServiceConfiguration;
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
 * Default implementation of {@link SmtpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultSmtpClientService
        extends AbstractAppbaseClientService<SmtpSession, TextValue, DefaultSmtpClientServiceConfiguration>
        implements SmtpClientService
{
    @Inject
    public DefaultSmtpClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultSmtpClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultSmtpClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultSmtpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSmtpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SmtpSession, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultSmtpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSmtpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultSmtpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSmtpClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultSmtpClientServiceConfiguration defaultSmtpClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultSmtpClientServiceConfiguration.class,
                defaultSmtpClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSmtpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SmtpSession, TextValue> appbaseClientPool,
            final DefaultSmtpClientServiceConfiguration defaultSmtpClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultSmtpClientServiceConfiguration.class,
                defaultSmtpClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultSmtpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultSmtpClientServiceConfiguration defaultSmtpClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultSmtpClientServiceConfiguration.class,
                defaultSmtpClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultSmtpClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultSmtpClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultSmtpClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultSmtpClientIntegrationServiceConfiguration defaultDefaultSmtpClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultSmtpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultSmtpClientIntegrationServiceConfiguration,
                "defaultDefaultSmtpClientIntegrationServiceConfiguration");

        return SmtpClientUtil.appbaseClientKey(defaultDefaultSmtpClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<SmtpSession, TextValue>>
    appbaseClientIntegrationServices(final DefaultSmtpClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<SmtpSession, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultSmtpClientIntegrationServiceConfiguration defaultDefaultSmtpClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultSmtpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultSmtpClientIntegrationServiceConfiguration,
                "defaultDefaultSmtpClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultSmtpClientIntegrationService defaultDefaultSmtpClientIntegrationService =
                new DefaultSmtpClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultSmtpClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultSmtpClientIntegrationService);

        final Set<DefaultSmtpClientIntegrationServiceConfiguration> keyedDefaultSmtpClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultSmtpClientIntegrationServiceConfigurations();
        if (keyedDefaultSmtpClientIntegrationServiceConfigurations != null) {
            keyedDefaultSmtpClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultSmtpClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                SmtpClientUtil.appbaseClientKey(keyedDefaultSmtpClientIntegrationServiceConfiguration);
                        final DefaultSmtpClientIntegrationService defaultSmtpClientIntegrationService =
                                new DefaultSmtpClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultSmtpClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultSmtpClientIntegrationService);
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
                .registerAppbaseService(SmtpClientService.class, this);
    }

    @Override
    public SmtpClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (SmtpClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public SmtpClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (SmtpClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
