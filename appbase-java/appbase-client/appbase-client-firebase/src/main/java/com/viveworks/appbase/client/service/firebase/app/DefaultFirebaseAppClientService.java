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

package com.viveworks.appbase.client.service.firebase.app;

import com.google.api.client.http.HttpTransport;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;
import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientServiceConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link FirebaseAppClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultFirebaseAppClientService
        extends AbstractAppbaseClientService<FirebaseApp, TextValue, DefaultFirebaseAppClientServiceConfiguration>
        implements FirebaseAppClientService
{
    private final AtomicBoolean emulatedEnvironmentRef;

    @NullableWhenUnused(info = "See defaultFirebaseAppClientIntegrationService()")
    private final HttpTransport httpTransport;

    @NullableWhenUnused(info = "See defaultFirebaseAppClientIntegrationService()")
    private final ThreadManager threadManager;

    @NullableWhenUnused(info = "See defaultFirebaseAppClientIntegrationService()")
    private final Map<String, Object> firebaseDatabaseAuthVariableOverride;

    @Inject
    public DefaultFirebaseAppClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                false,
                null);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }




    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }




    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                false,
                null);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");

        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }




    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                false,
                null);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }




    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                false,
                null);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }

    public DefaultFirebaseAppClientService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final AppbaseClientKey<TextValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultFirebaseAppClientServiceConfiguration defaultFirebaseAppClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultFirebaseAppClientServiceConfiguration.class,
                defaultFirebaseAppClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.emulatedEnvironmentRef = new AtomicBoolean(false);

        startOnCreate();
    }




    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultFirebaseAppClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultFirebaseAppClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<TextValue> defaultAppbaseClientKey(
            final DefaultFirebaseAppClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultFirebaseAppClientIntegrationServiceConfiguration
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultFirebaseAppClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultDefaultFirebaseAppClientIntegrationServiceConfiguration");

        return FirebaseAppClientUtil.appbaseClientKey(defaultDefaultFirebaseAppClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<FirebaseApp, TextValue>>
    appbaseClientIntegrationServices(final DefaultFirebaseAppClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<TextValue>, AppbaseClientIntegrationService<FirebaseApp, TextValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultFirebaseAppClientIntegrationServiceConfiguration
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultFirebaseAppClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultDefaultFirebaseAppClientIntegrationServiceConfiguration");

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultFirebaseAppClientIntegrationService defaultDefaultFirebaseAppClientIntegrationService =
                defaultFirebaseAppClientIntegrationService(
                        defaultAppbaseClientKey,
                        defaultDefaultFirebaseAppClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultFirebaseAppClientIntegrationService);


        final Set<DefaultFirebaseAppClientIntegrationServiceConfiguration>
                keyedDefaultFirebaseAppClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultFirebaseAppClientIntegrationServiceConfigurations();
        if (keyedDefaultFirebaseAppClientIntegrationServiceConfigurations != null) {
            keyedDefaultFirebaseAppClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultFirebaseAppClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<TextValue> appbaseClientKey =
                                FirebaseAppClientUtil.appbaseClientKey(
                                        keyedDefaultFirebaseAppClientIntegrationServiceConfiguration);
                        final DefaultFirebaseAppClientIntegrationService defaultFirebaseAppClientIntegrationService =
                                defaultFirebaseAppClientIntegrationService(
                                        appbaseClientKey,
                                        keyedDefaultFirebaseAppClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(
                                appbaseClientKey, defaultFirebaseAppClientIntegrationService);
                    });
        }

        return appbaseClientIntegrationServices;
    }

    private DefaultFirebaseAppClientIntegrationService defaultFirebaseAppClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultFirebaseAppClientIntegrationServiceConfiguration
                    defaultFirebaseAppClientIntegrationServiceConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");
        AppbaseUtil.notNull(
                defaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultFirebaseAppClientIntegrationServiceConfiguration");

        this.emulatedEnvironmentRef.set(
                defaultFirebaseAppClientIntegrationServiceConfiguration.isEmulatedEnvironment());
        if (httpTransport != null && threadManager != null && firebaseDatabaseAuthVariableOverride != null) {
            return new DefaultFirebaseAppClientIntegrationService(
                    appbaseClientCore,
                    httpTransport,
                    threadManager,
                    firebaseDatabaseAuthVariableOverride,
                    getAppbaseClientPool(),
                    appbaseClientKey,
                    defaultFirebaseAppClientIntegrationServiceConfiguration);
        } else if (httpTransport != null && threadManager != null) {
            return new DefaultFirebaseAppClientIntegrationService(
                    appbaseClientCore,
                    httpTransport,
                    threadManager,
                    getAppbaseClientPool(),
                    appbaseClientKey,
                    defaultFirebaseAppClientIntegrationServiceConfiguration);
        } else if (httpTransport != null) {
            return new DefaultFirebaseAppClientIntegrationService(
                    appbaseClientCore,
                    httpTransport,
                    getAppbaseClientPool(),
                    appbaseClientKey,
                    defaultFirebaseAppClientIntegrationServiceConfiguration);
        }

        return new DefaultFirebaseAppClientIntegrationService(
                appbaseClientCore,
                getAppbaseClientPool(),
                appbaseClientKey,
                defaultFirebaseAppClientIntegrationServiceConfiguration);
    }

    @Override
    protected void registerService() {
        super.registerService();

        appbaseClientCore
                .getAppbaseCore()
                .getServiceRegistry()
                .registerAppbaseService(FirebaseAppClientService.class, this);
    }

    @Override
    public FirebaseAppClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (FirebaseAppClientIntegrationService) super.getClientIntegrationService();
    }

    @Override
    public FirebaseAppClientIntegrationService getClientIntegrationService(
            final AppbaseClientKey<TextValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (FirebaseAppClientIntegrationService) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public boolean isEmulatedEnvironment() {
        return emulatedEnvironmentRef.get();
    }
}
