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
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link AwsSdkAsyncHttpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkAsyncHttpClientService
        extends AbstractAppbaseClientService<SdkAsyncHttpClient, TextValue, DefaultAwsSdkAsyncHttpClientServiceConfiguration>
        implements AwsSdkAsyncHttpClientService
{
    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    @Inject
    public DefaultAwsSdkAsyncHttpClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SdkAsyncHttpClient, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAwsSdkAsyncHttpClientServiceConfiguration appbaseClientServiceConfiguration)
    {
        this(appbaseClientCore, appbaseClientServiceConfiguration, false, null);
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultAwsSdkAsyncHttpClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<SdkAsyncHttpClient, TextValue> appbaseClientPool,
            final DefaultAwsSdkAsyncHttpClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
                appbaseClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Creates a DefaultAwsSdkAsyncHttpClientService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkAsyncHttpClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultAwsSdkAsyncHttpClientServiceConfiguration appbaseClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultAwsSdkAsyncHttpClientServiceConfiguration.class,
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
        AppbaseCoreObjectMapperSubTypes.register(DefaultAwsSdkAsyncHttpClientServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultAwsSdkAsyncHttpClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                "defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration");

        return AwsSdkHttpClientUtil.appbaseClientKey(
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<SdkAsyncHttpClient, TextValue>>
    appbaseClientIntegrationServices(final DefaultAwsSdkAsyncHttpClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<SdkAsyncHttpClient, TextValue>>
                appbaseClientIntegrationServices =
                new ConcurrentHashMap<>();


        final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                "defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultAwsSdkAsyncHttpClientIntegrationService defaultDefaultAwsSdkAsyncHttpClientIntegrationService =
                new DefaultAwsSdkAsyncHttpClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultAwsSdkAsyncHttpClientIntegrationService);


        final Set<DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration>
                keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations();
        if (keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations != null) {
            keyedDefaultAwsSdkAsyncHttpClientIntegrationServiceConfigurations.forEach(
                    defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                AwsSdkHttpClientUtil.appbaseClientKey(
                                        defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration);
                        final DefaultAwsSdkAsyncHttpClientIntegrationService defaultAwsSdkAsyncHttpClientIntegrationService =
                                new DefaultAwsSdkAsyncHttpClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        defaultAppbaseClientKey,
                                        defaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices
                                .put(appbaseClientKey, defaultAwsSdkAsyncHttpClientIntegrationService);
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
                .registerAppbaseService(AwsSdkAsyncHttpClientService.class, this);
    }

    @Override
    public AwsSdkAsyncHttpClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (AwsSdkAsyncHttpClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public AwsSdkAsyncHttpClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (AwsSdkAsyncHttpClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }
}
