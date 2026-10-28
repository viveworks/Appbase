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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkCommonUtil;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import software.amazon.awssdk.http.SdkHttpClient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkHttpClientService
        extends AbstractAppbaseClientService<SdkHttpClient, TextValue, DefaultAwsSdkHttpClientServiceConfiguration>
        implements AwsSdkHttpClientService
{
    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    @Inject
    public DefaultAwsSdkHttpClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore, DefaultAwsSdkHttpClientServiceConfiguration.class, false, null);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultAwsSdkHttpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SdkHttpClient, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultAwsSdkHttpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultAwsSdkHttpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAwsSdkHttpClientServiceConfiguration appbaseClientServiceConfiguration)
    {
        this(appbaseClientCore, appbaseClientServiceConfiguration, false, null);
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAwsSdkHttpClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultAwsSdkHttpClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SdkHttpClient, TextValue> appbaseClientPool,
            final DefaultAwsSdkHttpClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultAwsSdkHttpClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultAwsSdkHttpClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultAwsSdkHttpClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AwsSdkCommonUtil.registerObjectMapperSubtypes();
        AppbaseCoreObjectMapperSubTypes.register(DefaultAwsSdkHttpClientServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultAwsSdkHttpClientIntegrationServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultAwsSdkHttpClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration,
                "defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration");

        return AwsSdkHttpClientUtil
                .appbaseClientKey(defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<SdkHttpClient, TextValue>> appbaseClientIntegrationServices(
            final DefaultAwsSdkHttpClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<SdkHttpClient, TextValue>>
                appbaseClientIntegrationServices =
                new ConcurrentHashMap<>();

        final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration,
                "defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultAwsSdkHttpClientIntegrationService defaultDefaultAwsSdkHttpClientIntegrationService =
                new DefaultAwsSdkHttpClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultAwsSdkHttpClientIntegrationService);

        final Set<DefaultAwsSdkHttpClientIntegrationServiceConfiguration>
                keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations();
        if (keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations != null) {
            keyedDefaultAwsSdkHttpClientIntegrationServiceConfigurations.forEach(
                    defaultAwsSdkHttpClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                AwsSdkHttpClientUtil.appbaseClientKey(defaultAwsSdkHttpClientIntegrationServiceConfiguration);
                        final DefaultAwsSdkHttpClientIntegrationService defaultAwsSdkHttpClientIntegrationService =
                                new DefaultAwsSdkHttpClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        defaultAwsSdkHttpClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultAwsSdkHttpClientIntegrationService);
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
                .registerAppbaseService(AwsSdkHttpClientService.class, this);
    }

    @Override
    public AwsSdkHttpClientIntegrationService getClientIntegrationService() throws AppbaseClientIntegrationService.NotFoundException {
        return (AwsSdkHttpClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public AwsSdkHttpClientIntegrationService getClientIntegrationService(final AppbaseClientKey<TextValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (AwsSdkHttpClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
