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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.zookeeper.accessor.ZooKeeperAccessor;
import com.viveworks.appbase.client.service.zookeeper.accessor.reactive.ReactiveZooKeeperAccessor;
import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientServiceConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.BoundedExponentialBackoffRetryConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.ExponentialBackoffRetryConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryForeverConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryNTimesConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryOneTimeConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryUntilElapsedConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.SessionFailedRetryConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.apache.curator.framework.CuratorFramework;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link ZooKeeperClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultZooKeeperClientService
        extends AbstractAppbaseClientService
        <CuratorFramework, ClusterIdValue, DefaultZooKeeperClientServiceConfiguration>
        implements ZooKeeperClientService
{
    @Inject
    public DefaultZooKeeperClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultZooKeeperClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultZooKeeperClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultZooKeeperClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultZooKeeperClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<CuratorFramework, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultZooKeeperClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultZooKeeperClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultZooKeeperClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultZooKeeperClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultZooKeeperClientServiceConfiguration defaultZooKeeperClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultZooKeeperClientServiceConfiguration.class,
                defaultZooKeeperClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultZooKeeperClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<CuratorFramework, ClusterIdValue> appbaseClientPool,
            final DefaultZooKeeperClientServiceConfiguration defaultZooKeeperClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultZooKeeperClientServiceConfiguration.class,
                defaultZooKeeperClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultZooKeeperClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultZooKeeperClientServiceConfiguration defaultZooKeeperClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultZooKeeperClientServiceConfiguration.class,
                defaultZooKeeperClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(BoundedExponentialBackoffRetryConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(ExponentialBackoffRetryConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(RetryForeverConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(RetryNTimesConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(RetryOneTimeConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(RetryUntilElapsedConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(SessionFailedRetryConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultZooKeeperClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultZooKeeperClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultZooKeeperClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultZooKeeperClientIntegrationServiceConfiguration
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultZooKeeperClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration,
                "defaultDefaultZooKeeperClientIntegrationServiceConfiguration");

        return ZooKeeperClientUtil.appbaseClientKey(defaultDefaultZooKeeperClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultZooKeeperClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();


        final DefaultZooKeeperClientIntegrationServiceConfiguration
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultZooKeeperClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultZooKeeperClientIntegrationServiceConfiguration,
                "defaultDefaultZooKeeperClientIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultZooKeeperClientIntegrationService defaultDefaultZooKeeperClientIntegrationService =
                new DefaultZooKeeperClientIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultZooKeeperClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultZooKeeperClientIntegrationService);


        final Set<DefaultZooKeeperClientIntegrationServiceConfiguration>
                keyedDefaultZooKeeperClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultZooKeeperClientIntegrationServiceConfigurations();
        if (keyedDefaultZooKeeperClientIntegrationServiceConfigurations != null) {
            keyedDefaultZooKeeperClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultZooKeeperClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                ZooKeeperClientUtil.appbaseClientKey(
                                        keyedDefaultZooKeeperClientIntegrationServiceConfiguration);
                        final DefaultZooKeeperClientIntegrationService defaultZooKeeperClientIntegrationService =
                                new DefaultZooKeeperClientIntegrationService(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultZooKeeperClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultZooKeeperClientIntegrationService);
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
                .registerAppbaseService(ZooKeeperClientService.class, this);
    }

    @Override
    public ZooKeeperClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (ZooKeeperClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public ZooKeeperClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (ZooKeeperClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveZooKeeperAccessor getReactiveAccessor() {
        return new ReactiveZooKeeperAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveZooKeeperAccessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new ReactiveZooKeeperAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public ZooKeeperAccessor getAccessor() {
        return new ZooKeeperAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ZooKeeperAccessor getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new ZooKeeperAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
