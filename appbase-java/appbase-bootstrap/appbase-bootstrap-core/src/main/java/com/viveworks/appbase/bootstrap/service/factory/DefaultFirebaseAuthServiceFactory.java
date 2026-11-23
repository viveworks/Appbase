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

package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.user.account.firebaseauth.DefaultFirebaseAuthService;
import com.viveworks.appbase.service.user.account.firebaseauth.DefaultFirebaseAuthServiceConfiguration;
import com.viveworks.appbase.service.user.account.firebaseauth.FirebaseAuthService;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link FirebaseAuthService}.
 *
 * <p>{@link FirebaseAuthService} extends {@link com.viveworks.appbase.service.user.account.IdpTokenVerifier}
 * and is used by {@link DefaultUserAccountServiceFactory} for IDP token verification.
 * Therefore it must be registered before {@link DefaultUserAccountServiceFactory} in the initialization order.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultFirebaseAuthServiceFactory(FirebaseAuthService)}</li>
 *   <li>Config-based: {@link #DefaultFirebaseAuthServiceFactory(DefaultFirebaseAuthServiceConfiguration)}</li>
 *   <li>Default: {@link #DefaultFirebaseAuthServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 *
 * <p>The config and default modes obtain a {@link com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientIntegrationService}
 * from {@link AppbaseClientServices} via {@link FirebaseAuthClientService#getClientIntegrationService()}.
 * Register a {@link com.viveworks.appbase.bootstrap.client.factory.FirebaseAuthClientServiceFactory}
 * in the {@link AppbaseClientServices} builder before calling this factory.
 */
public final class DefaultFirebaseAuthServiceFactory implements AppbaseServiceFactory<FirebaseAuthService> {

    @Nullable private final FirebaseAuthService predefinedService;
    @Nullable private final DefaultFirebaseAuthServiceConfiguration config;

    /** Wraps a pre-built {@link FirebaseAuthService} instance. */
    public DefaultFirebaseAuthServiceFactory(final FirebaseAuthService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultFirebaseAuthService} from {@code config}. */
    public DefaultFirebaseAuthServiceFactory(final DefaultFirebaseAuthServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultFirebaseAuthService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public DefaultFirebaseAuthServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<FirebaseAuthService> serviceType() {
        return FirebaseAuthService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.FIREBASE_AUTH.order();
    }

    @Override
    public FirebaseAuthService create(final AppbaseCoreServices coreServices,
                                       final AppbaseClientServices clientServices,
                                       final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultFirebaseAuthServiceConfiguration.class, config);
        }
        final DefaultFirebaseAuthServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultFirebaseAuthServiceConfiguration.class);
        return new DefaultFirebaseAuthService(
            coreServices.getLogService(),
            coreServices.getValidationService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg,
            clientServices.get(FirebaseAuthClientService.class).getClientIntegrationService());
    }
}
