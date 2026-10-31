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

package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.cloud.aws.common.AwsSdkCommonUtil;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkAsyncHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.AwsSdkHttpClientService;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkAsyncHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.S3Accessor;
import com.viveworks.appbase.client.cloud.aws.s3.accessor.reactive.ReactiveS3Accessor;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.s3.configuration.DefaultS3ClientsServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link S3ClientsService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultS3ClientsService
        extends AbstractAppbaseClientService<S3Clients, ClusterIdValue, DefaultS3ClientsServiceConfiguration>
        implements S3ClientsService
{
    private final AtomicReference<AwsSdkHttpClientService> awsSdkHttpClientServiceRef;
    private final AtomicReference<AwsSdkAsyncHttpClientService> awsSdkAsyncHttpClientServiceRef;


    // DefaultS3ClientsServiceConfiguration is read from ConfigurationService, AwsSdkAsyncHttpClientService is injected

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    @Inject
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                false,
                null);

        AppbaseUtil.notNull(awsSdkHttpClientService, "awsSdkHttpClientService");
        AppbaseUtil.notNull(awsSdkAsyncHttpClientService, "awsSdkAsyncHttpClientService");

        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(awsSdkHttpClientService, "awsSdkHttpClientService");
        AppbaseUtil.notNull(awsSdkAsyncHttpClientService, "awsSdkAsyncHttpClientService");

        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultS3ClientsServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(awsSdkHttpClientService, "awsSdkHttpClientService");
        AppbaseUtil.notNull(awsSdkAsyncHttpClientService, "awsSdkAsyncHttpClientService");

        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultS3ClientsServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(awsSdkHttpClientService, "awsSdkHttpClientService");
        AppbaseUtil.notNull(awsSdkAsyncHttpClientService, "awsSdkAsyncHttpClientService");

        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }



    // DefaultS3ClientsServiceConfiguration is read from ConfigurationService, AwsSdkAsyncHttpClientService is built
    // using the configuration in it

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                false,
                null);

        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultS3ClientsServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultS3ClientsServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }



    // DefaultS3ClientsServiceConfiguration is provided, AwsSdkAsyncHttpClientService is built using the configuration in it

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                false,
                null);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>();
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>();

        startOnCreate();
    }





    // DefaultS3ClientsServiceConfiguration and AwsSdkAsyncHttpClientService are provided

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                false,
                null);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<S3Clients, ClusterIdValue> appbaseClientPool,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }

    /**
     * Creates a DefaultS3ClientsService using the supplied configuration and collaborators.
     */
    public DefaultS3ClientsService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final AwsSdkHttpClientService awsSdkHttpClientService,
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultS3ClientsServiceConfiguration.class,
                defaultS3ClientsServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        checkCustomDefaultAwsSdkHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(defaultS3ClientsServiceConfiguration);
        this.awsSdkHttpClientServiceRef = new AtomicReference<>(awsSdkHttpClientService);
        this.awsSdkAsyncHttpClientServiceRef = new AtomicReference<>(awsSdkAsyncHttpClientService);

        startOnCreate();
    }


    private void checkCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration)
    {
        final DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientsIntegrationServiceConfiguration =
                defaultS3ClientsServiceConfiguration.getDefaultDefaultS3ClientIntegrationServiceConfiguration();

        final DefaultAwsSdkAsyncHttpClientServiceConfiguration customDefaultAwsSdkAsyncHttpClientServiceConfiguration =
                defaultDefaultS3ClientsIntegrationServiceConfiguration.getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration();
        AppbaseUtil.notNull(
                customDefaultAwsSdkAsyncHttpClientServiceConfiguration,
                "customDefaultAwsSdkAsyncHttpClientServiceConfiguration");

        final DefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration =
                customDefaultAwsSdkAsyncHttpClientServiceConfiguration
                .getDefaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration,
                "defaultDefaultAwsSdkAsyncHttpClientIntegrationServiceConfiguration");

        final Set<DefaultS3ClientsIntegrationServiceConfiguration> keyedDefaultS3ClientsIntegrationServiceConfigurations =
                defaultS3ClientsServiceConfiguration.getKeyedDefaultS3ClientIntegrationServiceConfigurations();
        if (keyedDefaultS3ClientsIntegrationServiceConfigurations != null) {
            keyedDefaultS3ClientsIntegrationServiceConfigurations.forEach(
                    keyedDefaultS3ClientIntegrationServiceConfiguration -> {
                        AppbaseUtil.notNull(
                                keyedDefaultS3ClientIntegrationServiceConfiguration
                                        .getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration(),
                                "keyedDefaultS3ClientIntegrationServiceConfiguration" +
                                        ".getCustomDefaultAwsSdkAsyncHttpClientServiceConfiguration()");
                    });
        }
    }


    private void checkCustomDefaultAwsSdkHttpClientServiceConfiguration(
            final DefaultS3ClientsServiceConfiguration defaultS3ClientsServiceConfiguration)
    {
        final DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientIntegrationServiceConfiguration =
                defaultS3ClientsServiceConfiguration.getDefaultDefaultS3ClientIntegrationServiceConfiguration();

        final DefaultAwsSdkHttpClientServiceConfiguration customDefaultAwsSdkHttpClientServiceConfiguration =
                defaultDefaultS3ClientIntegrationServiceConfiguration
                .getCustomDefaultAwsSdkHttpClientServiceConfiguration();
        AppbaseUtil.notNull(
                customDefaultAwsSdkHttpClientServiceConfiguration,
                "customDefaultAwsSdkHttpClientServiceConfiguration");

        final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration =
                customDefaultAwsSdkHttpClientServiceConfiguration.getDefaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration,
                "defaultDefaultAwsSdkHttpClientIntegrationServiceConfiguration");

        final Set<DefaultS3ClientsIntegrationServiceConfiguration> keyedDefaultS3ClientIntegrationServiceConfigurations =
                defaultS3ClientsServiceConfiguration.getKeyedDefaultS3ClientIntegrationServiceConfigurations();
        if (keyedDefaultS3ClientIntegrationServiceConfigurations != null) {
            keyedDefaultS3ClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultS3ClientIntegrationServiceConfiguration -> {
                        AppbaseUtil.notNull(
                                keyedDefaultS3ClientIntegrationServiceConfiguration
                                        .getCustomDefaultAwsSdkHttpClientServiceConfiguration(),
                                "keyedDefaultS3ClientIntegrationServiceConfiguration" +
                                        ".getCustomDefaultAwsSdkHttpClientServiceConfiguration()");
                    });
        }
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AwsSdkCommonUtil.registerObjectMapperSubtypes();
        AppbaseCoreObjectMapperSubTypes.register(DefaultS3ClientsIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultS3ClientsServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultS3ClientsServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientsIntegrationServiceConfiguration =
                configuration.getDefaultDefaultS3ClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultS3ClientsIntegrationServiceConfiguration,
                "defaultDefaultS3ClientsIntegrationServiceConfiguration");

        return S3ClientsUtil.appbaseClientKey(defaultDefaultS3ClientsIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<S3Clients, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultS3ClientsServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<S3Clients, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultS3ClientsIntegrationServiceConfiguration defaultDefaultS3ClientsIntegrationServiceConfiguration =
                configuration.getDefaultDefaultS3ClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultS3ClientsIntegrationServiceConfiguration,
                "defaultDefaultS3ClientsIntegrationServiceConfiguration");

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultS3ClientsIntegrationService defaultDefaultS3ClientsIntegrationService = defaultS3ClientIntegrationService(
                defaultAppbaseClientKey,
                defaultDefaultS3ClientsIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultS3ClientsIntegrationService);


        final Set<DefaultS3ClientsIntegrationServiceConfiguration> keyedDefaultS3ClientsIntegrationServiceConfigurations =
                configuration.getKeyedDefaultS3ClientIntegrationServiceConfigurations();
        if (keyedDefaultS3ClientsIntegrationServiceConfigurations != null) {
            keyedDefaultS3ClientsIntegrationServiceConfigurations.forEach(
                    keyedDefaultS3ClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                S3ClientsUtil.appbaseClientKey(keyedDefaultS3ClientIntegrationServiceConfiguration);
                        final DefaultS3ClientsIntegrationService defaultS3ClientsIntegrationService =
                                defaultS3ClientIntegrationService(
                                        appbaseClientKey,
                                        keyedDefaultS3ClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultS3ClientsIntegrationService);
                    });
        }

        return appbaseClientIntegrationServices;
    }

    private DefaultS3ClientsIntegrationService defaultS3ClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultS3ClientsIntegrationServiceConfiguration defaultS3ClientsIntegrationServiceConfiguration)
    {
        final AwsSdkHttpClientService awsSdkHttpClientService = awsSdkHttpClientServiceRef.get();
        final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService = awsSdkAsyncHttpClientServiceRef.get();

        return awsSdkHttpClientService == null && awsSdkAsyncHttpClientService == null ?
                new DefaultS3ClientsIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        appbaseClientKey,
                        defaultS3ClientsIntegrationServiceConfiguration) :
                new DefaultS3ClientsIntegrationService(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        appbaseClientKey,
                        awsSdkHttpClientService,
                        awsSdkAsyncHttpClientService,
                        defaultS3ClientsIntegrationServiceConfiguration);
    }

    @Override
    protected void registerService() {
        super.registerService();

        appbaseClientCore
                .getAppbaseCore()
                .getServiceRegistry()
                .registerAppbaseService(S3ClientsService.class, this);
    }

    @Override
    public S3ClientsIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (S3ClientsIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public S3ClientsIntegrationService getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (S3ClientsIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveS3Accessor getReactiveAccessor() {
        return new ReactiveS3Accessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveS3Accessor getReactiveAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new ReactiveS3Accessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public S3Accessor getAccessor() {
        return new S3Accessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public S3Accessor getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new S3Accessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
