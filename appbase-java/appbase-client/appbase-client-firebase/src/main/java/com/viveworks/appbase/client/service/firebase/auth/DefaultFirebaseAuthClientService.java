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

package com.viveworks.appbase.client.service.firebase.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.auth.accessor.FirebaseAuthAccessor;
import com.viveworks.appbase.client.service.firebase.auth.accessor.reactive.ReactiveFirebaseAuthAccessor;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientServiceConfiguration;
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
 * Default implementation of {@link FirebaseAuthClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultFirebaseAuthClientService
        extends AbstractAppbaseClientService<FirebaseAuth, TextValue, DefaultFirebaseAuthClientServiceConfiguration>
        implements FirebaseAuthClientService
{
    private final FirebaseAppClientService firebaseAppClientService;

    @Inject
    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService)
    {
        super(appbaseClientCore,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                false,
                null);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }

    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }

    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseAuth, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }

    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }




    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final DefaultFirebaseAuthClientServiceConfiguration defaultFirebaseAuthClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                defaultFirebaseAuthClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }

    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseAuth, TextValue> appbaseClientPool,
            final DefaultFirebaseAuthClientServiceConfiguration defaultFirebaseAuthClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                defaultFirebaseAuthClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }

    public DefaultFirebaseAuthClientService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultFirebaseAuthClientServiceConfiguration defaultFirebaseAuthClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAuthClientServiceConfiguration.class,
                defaultFirebaseAuthClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        this.firebaseAppClientService = firebaseAppClientService;

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultFirebaseAuthClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultFirebaseAuthClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultFirebaseAuthClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultFirebaseAuthClientIntegrationServiceConfiguration
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultFirebaseAuthClientIntegrationServiceConfiguration();
        return FirebaseAuthClientUtil.appbaseClientKey(
                firebaseAppClientService, defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<FirebaseAuth, TextValue>>
    appbaseClientIntegrationServices(final DefaultFirebaseAuthClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<FirebaseAuth, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultFirebaseAuthClientIntegrationServiceConfiguration
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultFirebaseAuthClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration,
                "defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration");


        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultFirebaseAuthClientIntegrationService defaultDefaultFirebaseAuthClientIntegrationService =
                new DefaultFirebaseAuthClientIntegrationService(
                        appbaseClientCore,
                        firebaseAppClientService,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultFirebaseAuthClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultFirebaseAuthClientIntegrationService);


        final Set<DefaultFirebaseAuthClientIntegrationServiceConfiguration>
                keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultFirebaseAuthClientIntegrationServiceConfigurations();
        if (keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations != null) {
            keyedDefaultFirebaseAuthClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultFirebaseAuthClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey = FirebaseAuthClientUtil.appbaseClientKey(
                                firebaseAppClientService, keyedDefaultFirebaseAuthClientIntegrationServiceConfiguration);
                        final DefaultFirebaseAuthClientIntegrationService defaultFirebaseAuthClientIntegrationService =
                                new DefaultFirebaseAuthClientIntegrationService(
                                        appbaseClientCore,
                                        firebaseAppClientService,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultFirebaseAuthClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultFirebaseAuthClientIntegrationService);
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
                .registerAppbaseService(FirebaseAuthClientService.class, this);
    }

    @Override
    public FirebaseAuthClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (FirebaseAuthClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public FirebaseAuthClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (FirebaseAuthClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public ReactiveFirebaseAuthAccessor getReactiveAccessor() {
        return new ReactiveFirebaseAuthAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public ReactiveFirebaseAuthAccessor getReactiveAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new ReactiveFirebaseAuthAccessor(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public FirebaseAuthAccessor getAccessor() {
        return new FirebaseAuthAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public FirebaseAuthAccessor getAccessor(final AppbaseClientKey<TextValue> appbaseClientKey) {
        return new FirebaseAuthAccessor(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }
}
