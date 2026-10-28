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

package com.viveworks.appbase.client.service.firebase.auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientIntegrationService;
import com.viveworks.appbase.client.service.firebase.app.FirebaseAppClientService;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link FirebaseAuthClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultFirebaseAuthClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <FirebaseAuth, TextValue, DefaultFirebaseAuthClientIntegrationServiceConfiguration>
        implements FirebaseAuthClientIntegrationService
{
    private static final String FIREBASE_AUTH_EMULATOR_HOST_ENV = "FIREBASE_AUTH_EMULATOR_HOST";

    private final AtomicReference<FirebaseAuth> firebaseAuthRef;

    private final FirebaseAppClientService firebaseAppClientService;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultFirebaseAuthClientIntegrationServiceConfiguration
            defaultFirebaseAuthClientIntegrationServiceConfiguration;


    public DefaultFirebaseAuthClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<FirebaseAuth, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final FirebaseAuth firebaseAuth)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseAuth.class);

        AppbaseUtil.notNull(firebaseAuth, "firebaseAuth");

        this.firebaseAuthRef = new AtomicReference<>(firebaseAuth);
        this.firebaseAppClientService = null;
        this.defaultFirebaseAuthClientIntegrationServiceConfiguration = null;
    }

    public DefaultFirebaseAuthClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final FirebaseAppClientService firebaseAppClientService,
            final AppbaseClientPool<FirebaseAuth, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultFirebaseAuthClientIntegrationServiceConfiguration
                    defaultFirebaseAuthClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, FirebaseAuth.class);

        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        AppbaseUtil.notNull(
                defaultFirebaseAuthClientIntegrationServiceConfiguration,
                "defaultFirebaseAuthClientIntegrationServiceConfiguration");

        this.firebaseAppClientService = firebaseAppClientService;
        this.firebaseAuthRef = new AtomicReference<>();
        this.defaultFirebaseAuthClientIntegrationServiceConfiguration =
                defaultFirebaseAuthClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }


    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(firebaseAppClientService, "firebaseAppClientService");
        AppbaseUtil.notNull(
                defaultFirebaseAuthClientIntegrationServiceConfiguration,
                "defaultFirebaseAuthClientIntegrationServiceConfiguration");

        if (defaultFirebaseAuthClientIntegrationServiceConfiguration.isEmulatedEnvironment()) {
            checkEmulatedEnvironment();
        }

        final FirebaseAppClientIntegrationService firebaseAppClientIntegrationService;
        if (defaultFirebaseAuthClientIntegrationServiceConfiguration
                .isUseDefaultFirebaseAppClientIntegrationService())
        {
            firebaseAppClientIntegrationService = firebaseAppClientService.getClientIntegrationService();
        } else {
            final TextValueConfiguration firebaseAppClientIntegrationServiceKeyConfiguration =
                    defaultFirebaseAuthClientIntegrationServiceConfiguration
                            .getFirebaseAppClientIntegrationServiceKeyConfiguration();
            final AppbaseClientKey<TextValue> appbaseClientKey =
                    AppbaseClientUtil.textValueKey(firebaseAppClientIntegrationServiceKeyConfiguration);
            firebaseAppClientIntegrationService =
                    firebaseAppClientService.getClientIntegrationService(appbaseClientKey);
        }

        final FirebaseApp firebaseApp = firebaseAppClientIntegrationService.getClient();
        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
        this.firebaseAuthRef.set(firebaseAuth);
    }

    private void checkEmulatedEnvironment() throws AppbaseClientException {
        final String firebaseAuthEmulatorHost = System.getenv(FIREBASE_AUTH_EMULATOR_HOST_ENV);
        if (AppbaseUtil.isBlank(firebaseAuthEmulatorHost)) {
            throw new AppbaseClientException(
                    FIREBASE_AUTH_EMULATOR_HOST_ENV + " must be set to a value with format host:port " +
                            "(export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099)");
        }
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultFirebaseAuthClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        firebaseAuthRef.set(null);
    }

    @Override
    public FirebaseAuth getClient() {
        return firebaseAuthRef.get();
    }

    @Override
    public boolean isHealthy() {
        final FirebaseAuth firebaseAuth = getClient();
        return firebaseAuth != null;
    }
}
