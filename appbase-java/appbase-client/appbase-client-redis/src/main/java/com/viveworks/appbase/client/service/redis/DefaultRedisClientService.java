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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientServiceConfiguration;
import com.viveworks.appbase.client.service.redis.reactive.ReactiveRedisAccessor;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.lettuce.core.api.StatefulRedisConnection;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link RedisClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public class DefaultRedisClientService
        extends AbstractAppbaseClientService
        <StatefulRedisConnection<String, String>, TextValue, DefaultRedisClientServiceConfiguration>
        implements RedisClientService
{
    @Inject
    public DefaultRedisClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultRedisClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultRedisClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultRedisClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultRedisClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultRedisClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultRedisClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultRedisClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultRedisClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultRedisClientServiceConfiguration defaultRedisClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultRedisClientServiceConfiguration.class,
                defaultRedisClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultRedisClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool,
            final DefaultRedisClientServiceConfiguration defaultRedisClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultRedisClientServiceConfiguration.class,
                defaultRedisClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultRedisClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultRedisClientServiceConfiguration defaultRedisClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultRedisClientServiceConfiguration.class,
                defaultRedisClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultRedisClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultRedisClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultRedisClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultRedisClientIntegrationServiceConfiguration defaultConfig =
                configuration.getDefaultDefaultRedisClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(defaultConfig, "defaultDefaultRedisClientIntegrationServiceConfiguration");

        return RedisClientUtil.appbaseClientKey(defaultConfig);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService
            <StatefulRedisConnection<String, String>, TextValue>>
    appbaseClientIntegrationServices(final DefaultRedisClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService
                <StatefulRedisConnection<String, String>, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultRedisClientIntegrationServiceConfiguration defaultConfig =
                configuration.getDefaultDefaultRedisClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(defaultConfig, "defaultDefaultRedisClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultKey = defaultAppbaseClientKey(configuration);
        final DefaultRedisClientIntegrationService defaultIntegrationService =
                new DefaultRedisClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultKey,
                        defaultConfig);
        appbaseClientIntegrationServices.put(defaultKey, defaultIntegrationService);

        final Set<DefaultRedisClientIntegrationServiceConfiguration> keyedConfigs =
                configuration.getKeyedDefaultRedisClientIntegrationServiceConfigurations();
        if (keyedConfigs != null) {
            keyedConfigs.forEach(keyedConfig -> {
                final AppbaseClientKey<TextValue> key = RedisClientUtil.appbaseClientKey(keyedConfig);
                final DefaultRedisClientIntegrationService integrationService =
                        new DefaultRedisClientIntegrationService(
                                appbaseClientCore,
                                getAppbaseClientPool(),
                                key,
                                keyedConfig);
                appbaseClientIntegrationServices.put(key, integrationService);
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
                .registerAppbaseService(RedisClientService.class, this);
    }

    @Override
    public RedisClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (RedisClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public RedisClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (RedisClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveRedisAccessor getReactiveAccessor() {
        return new ReactiveRedisAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveRedisAccessor getReactiveAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new ReactiveRedisAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public RedisAccessor getAccessor() {
        return new RedisAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public RedisAccessor getAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new RedisAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
