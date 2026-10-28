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

package com.viveworks.appbase.client.service.firebase.app;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.ThreadManager;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.firebase.app.configuration.CustomFirebaseThreadManagerConfiguration;
import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.app.configuration.FirebaseAccessTokenConfiguration;
import com.viveworks.appbase.client.service.firebase.app.configuration.GoogleCredentialsConfiguration;
import com.viveworks.appbase.client.service.firebase.app.configuration.NetHttpTransportConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link FirebaseAppClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultFirebaseAppClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <FirebaseApp, TextValue, DefaultFirebaseAppClientIntegrationServiceConfiguration>
        implements FirebaseAppClientIntegrationService
{
    private final AtomicReference<FirebaseApp> firebaseAppRef;

    @NullableWhenUnused(info = "See initHttpTransport()")
    private final HttpTransport httpTransport;

    @NullableWhenUnused(info = "See initThreadManager()")
    private final ThreadManager threadManager;

    @NullableWhenUnused(info = "See initFirebaseDatabaseAuthVariableOverride()")
    private final Map<String, Object> firebaseDatabaseAuthVariableOverride;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultFirebaseAppClientIntegrationServiceConfiguration
            defaultFirebaseAppClientIntegrationServiceConfiguration;




    public DefaultFirebaseAppClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final FirebaseApp firebaseApp)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseApp.class);

        AppbaseUtil.notNull(firebaseApp, "firebaseApp");

        this.firebaseAppRef = new AtomicReference<>(firebaseApp);
        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.defaultFirebaseAppClientIntegrationServiceConfiguration = null;
    }

    public DefaultFirebaseAppClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultFirebaseAppClientIntegrationServiceConfiguration
                    defaultFirebaseAppClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseApp.class);

        AppbaseUtil.notNull(
                defaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultFirebaseAppClientIntegrationServiceConfiguration");

        this.firebaseAppRef = new AtomicReference<>();
        this.httpTransport = null;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.defaultFirebaseAppClientIntegrationServiceConfiguration =
                defaultFirebaseAppClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    public DefaultFirebaseAppClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultFirebaseAppClientIntegrationServiceConfiguration
                    defaultFirebaseAppClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseApp.class);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(
                defaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultFirebaseAppClientIntegrationServiceConfiguration");

        this.firebaseAppRef = new AtomicReference<>();
        this.httpTransport = httpTransport;
        this.threadManager = null;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.defaultFirebaseAppClientIntegrationServiceConfiguration =
                defaultFirebaseAppClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    public DefaultFirebaseAppClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultFirebaseAppClientIntegrationServiceConfiguration
                    defaultFirebaseAppClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseApp.class);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(
                defaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultFirebaseAppClientIntegrationServiceConfiguration");

        this.firebaseAppRef = new AtomicReference<>();
        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = null;
        this.defaultFirebaseAppClientIntegrationServiceConfiguration =
                defaultFirebaseAppClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    public DefaultFirebaseAppClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final HttpTransport httpTransport,
            final ThreadManager threadManager,
            final Map<String, Object> firebaseDatabaseAuthVariableOverride,
            final AppbaseClientPool<FirebaseApp, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultFirebaseAppClientIntegrationServiceConfiguration
                    defaultFirebaseAppClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseApp.class);

        AppbaseUtil.notNull(httpTransport, "httpTransport");
        AppbaseUtil.notNull(threadManager, "threadManager");
        AppbaseUtil.notNull(
                defaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultFirebaseAppClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(firebaseDatabaseAuthVariableOverride, "firebaseDatabaseAuthVariableOverride");

        this.firebaseAppRef = new AtomicReference<>();
        this.httpTransport = httpTransport;
        this.threadManager = threadManager;
        this.firebaseDatabaseAuthVariableOverride = firebaseDatabaseAuthVariableOverride;
        this.defaultFirebaseAppClientIntegrationServiceConfiguration =
                defaultFirebaseAppClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }




    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultFirebaseAppClientIntegrationServiceConfiguration,
                "defaultFirebaseAppClientIntegrationServiceConfiguration");

        final FirebaseOptions.Builder firebaseOptionsBuilder = FirebaseOptions.builder();
        initGoogleCredentials(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration.isEmulatedEnvironment(),
                defaultFirebaseAppClientIntegrationServiceConfiguration.getGoogleCredentialsConfiguration());
        initConnectTimeout(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration.getConnectTimeoutConfiguration());
        initReadTimeout(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration.getReadTimeoutConfiguration());
        initThreadManager(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration.getThreadManagerInjectedTypeConfiguration(),
                defaultFirebaseAppClientIntegrationServiceConfiguration.getCustomFirebaseThreadManagerConfiguration());
        initFirebaseDatabaseAuthVariableOverride(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration
                        .getFirebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration());
        initJsonFactory(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration.isUseDefaultJacksonFactory(),
                defaultFirebaseAppClientIntegrationServiceConfiguration.getJsonFactoryInjectedTypeConfiguration());
        initHttpTransport(
                firebaseOptionsBuilder,
                defaultFirebaseAppClientIntegrationServiceConfiguration.getHttpTransportInjectedTypeConfiguration(),
                defaultFirebaseAppClientIntegrationServiceConfiguration.getNetHttpTransportConfiguration());

        final String databaseUrl = defaultFirebaseAppClientIntegrationServiceConfiguration.getDatabaseUrl();
        if (databaseUrl != null) {
            firebaseOptionsBuilder.setDatabaseUrl(databaseUrl);
        }

        final String projectId = defaultFirebaseAppClientIntegrationServiceConfiguration.getProjectId();
        if (projectId != null) {
            firebaseOptionsBuilder.setProjectId(projectId);
        }

        final String serviceAccountId = defaultFirebaseAppClientIntegrationServiceConfiguration.getServiceAccountId();
        if (serviceAccountId != null) {
            firebaseOptionsBuilder.setServiceAccountId(serviceAccountId);
        }

        final String storageBucket = defaultFirebaseAppClientIntegrationServiceConfiguration.getStorageBucket();
        if (storageBucket != null) {
            firebaseOptionsBuilder.setStorageBucket(storageBucket);
        }

        final FirebaseOptions firebaseOptions = firebaseOptionsBuilder.build();
        final FirebaseApp firebaseApp = FirebaseApp.initializeApp(firebaseOptions);
        this.firebaseAppRef.set(firebaseApp);
    }

    private void initHttpTransport(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final InjectedTypeConfiguration<? extends HttpTransport> httpTransportInjectedTypeConfiguration,
            final NetHttpTransportConfiguration netHttpTransportConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (httpTransport != null) {
            firebaseOptionsBuilder.setHttpTransport(httpTransport);
        } else if (httpTransportInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();
            final HttpTransport localHttpTransport = AppbaseUtil
                    .injectedInstance(guiceInjectorService, httpTransportInjectedTypeConfiguration);
            firebaseOptionsBuilder.setHttpTransport(localHttpTransport);
        } else if (netHttpTransportConfiguration != null) {
            final Boolean keepAlive = netHttpTransportConfiguration.getKeepAlive();
            if (keepAlive != null) {
                System.setProperty("http.keepAlive", String.valueOf(keepAlive));
            }

            final Integer maxConnections = netHttpTransportConfiguration.getMaxConnections();
            if (maxConnections != null) {
                System.setProperty("http.maxConnections", String.valueOf(maxConnections));
            }

            final NetHttpTransport netHttpTransport = new NetHttpTransport();
            firebaseOptionsBuilder.setHttpTransport(netHttpTransport);
        }
    }

    private void initJsonFactory(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final boolean createJacksonFactory,
            final InjectedTypeConfiguration<? extends JsonFactory> jsonFactoryInjectedTypeConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (createJacksonFactory) {
            final JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
            firebaseOptionsBuilder.setJsonFactory(jacksonFactory);
        } else if (jsonFactoryInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();
            final JsonFactory jsonFactory = AppbaseUtil
                    .injectedInstance(guiceInjectorService, jsonFactoryInjectedTypeConfiguration);
            if (jsonFactory != null) {
                firebaseOptionsBuilder.setJsonFactory(jsonFactory);
            }
        }
    }

    private void initFirebaseDatabaseAuthVariableOverride(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final InjectedTypeConfiguration<? extends FirebaseDatabaseAuthVariableOverrideFactory>
                    firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (firebaseDatabaseAuthVariableOverride != null) {
            firebaseOptionsBuilder.setDatabaseAuthVariableOverride(firebaseDatabaseAuthVariableOverride);
        } else if (firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService =
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService();
            final FirebaseDatabaseAuthVariableOverrideFactory localFirebaseDatabaseAuthVariableOverrideFactory =
                    AppbaseUtil.injectedInstance(
                            guiceInjectorService,
                            firebaseDatabaseAuthVariableOverrideFactoryInjectedTypeConfiguration);
            if (localFirebaseDatabaseAuthVariableOverrideFactory != null) {
                final Map<String, Object> localFirebaseDatabaseAuthVariableOverride =
                        localFirebaseDatabaseAuthVariableOverrideFactory.getInstance();
                if (localFirebaseDatabaseAuthVariableOverride != null) {
                    firebaseOptionsBuilder.setDatabaseAuthVariableOverride(localFirebaseDatabaseAuthVariableOverride);
                }
            }
        }
    }

    private void initGoogleCredentials(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final boolean emulatedEnvironment,
            final GoogleCredentialsConfiguration googleCredentialsConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (googleCredentialsConfiguration == null || emulatedEnvironment) {
            final GoogleCredentials googleCredentials = GoogleCredentials.create(new AccessToken(
                    getClass().getSimpleName(),
                    Date.from(LocalDateTime.now().plusYears(1).atZone(ZoneId.systemDefault()).toInstant())));
            firebaseOptionsBuilder.setCredentials(googleCredentials);
        } else {
            final FirebaseAccessTokenConfiguration firebaseAccessTokenConfiguration =
                    googleCredentialsConfiguration.getFirebaseAccessTokenConfiguration();
            final String tokenValue =
                    firebaseAccessTokenConfiguration.getTokenValue();
            final Date expirationTimeMillis = firebaseAccessTokenConfiguration.getExpirationTimeMillis();
            final AccessToken accessToken = new AccessToken(tokenValue, expirationTimeMillis);
            final GoogleCredentials googleCredentials = GoogleCredentials.create(accessToken);
            firebaseOptionsBuilder.setCredentials(googleCredentials);
        }
    }

    private void initThreadManager(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final InjectedTypeConfiguration<? extends ThreadManager> threadManagerInjectedTypeConfiguration,
            final CustomFirebaseThreadManagerConfiguration customFirebaseThreadManagerConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (threadManager != null) {
            firebaseOptionsBuilder.setThreadManager(threadManager);
        } else if (threadManagerInjectedTypeConfiguration != null) {
            final GuiceInjectorService guiceInjectorService =
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService();
            final ThreadManager localThreadManager = AppbaseUtil
                    .injectedInstance(guiceInjectorService, threadManagerInjectedTypeConfiguration);
            if (localThreadManager != null) {
                firebaseOptionsBuilder.setThreadManager(localThreadManager);
            }
        } else if (customFirebaseThreadManagerConfiguration != null) {
            final CustomFirebaseThreadManager customFirebaseThreadManager =
                    new CustomFirebaseThreadManager(appbaseClientCore, customFirebaseThreadManagerConfiguration);
            firebaseOptionsBuilder.setThreadManager(customFirebaseThreadManager);
        }
    }

    private void initReadTimeout(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final TimeoutConfiguration readTimeoutConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (readTimeoutConfiguration != null) {
            final long millis = readTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(readTimeoutConfiguration.getTimeout());
            firebaseOptionsBuilder.setReadTimeout((int) millis);
        }
    }

    private void initConnectTimeout(
            final FirebaseOptions.Builder firebaseOptionsBuilder,
            final TimeoutConfiguration connectTimeoutConfiguration)
    {
        AppbaseUtil.notNull(firebaseOptionsBuilder, "firebaseOptionsBuilder");

        if (connectTimeoutConfiguration != null) {
            final long millis = connectTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(connectTimeoutConfiguration.getTimeout());
            firebaseOptionsBuilder.setConnectTimeout((int) millis);
        }
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultFirebaseAppClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final FirebaseApp firebaseApp = getClient();

        firebaseApp.delete();
        firebaseAppRef.set(null);
    }

    @Override
    public FirebaseApp getClient() {
        return firebaseAppRef.get();
    }

    @Override
    public boolean isHealthy() {
        final FirebaseApp firebaseApp = getClient();
        return firebaseApp != null;
    }
}
