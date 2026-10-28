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

package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.cluster.ClusterService;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.DefaultUserAccountService;
import com.viveworks.appbase.service.user.account.DefaultUserAccountServiceConfiguration;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.firebaseauth.FirebaseAuthService;
import com.viveworks.appbase.service.user.account.store.UserAccountStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link UserAccountService}.
 *
 * <p>Cross-service dependencies (all retrieved from the partial {@link AppbaseServices} container):
 * <ul>
 *   <li>{@link IdService} — register an {@link DefaultIdServiceFactory}</li>
 *   <li>{@link ClusterService} — register a {@link StaticClusterServiceFactory}</li>
 *   <li>{@link AuthorizationService} — register an {@link DefaultAuthorizationServiceFactory}</li>
 *   <li>{@link PasswordService} — register a {@link DefaultPasswordServiceFactory}</li>
 *   <li>{@link FirebaseAuthService} — used as
 *       {@link com.viveworks.appbase.service.user.account.IdpTokenVerifier};
 *       register a {@link DefaultFirebaseAuthServiceFactory}</li>
 *   <li>{@link UserSessionStore} — register a {@link AerospikeUserSessionStoreFactory}</li>
 *   <li>{@link UserAccountStore} — register a {@link JdbiUserAccountStoreFactory}</li>
 * </ul>
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultUserAccountServiceFactory(UserAccountService)}</li>
 *   <li>Config-based: {@link #DefaultUserAccountServiceFactory(DefaultUserAccountServiceConfiguration)}</li>
 *   <li>Default: {@link #DefaultUserAccountServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class DefaultUserAccountServiceFactory implements AppbaseServiceFactory<UserAccountService> {

    @Nullable private final UserAccountService predefinedService;
    @Nullable private final DefaultUserAccountServiceConfiguration config;

    /** Wraps a pre-built {@link UserAccountService} instance. */
    public DefaultUserAccountServiceFactory(final UserAccountService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultUserAccountService} from {@code config}. */
    public DefaultUserAccountServiceFactory(final DefaultUserAccountServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultUserAccountService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public DefaultUserAccountServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<UserAccountService> serviceType() {
        return UserAccountService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.USER_ACCOUNT.order();
    }

    @Override
    public UserAccountService create(final AppbaseCoreServices coreServices,
                                      final AppbaseClientServices clientServices,
                                      final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultUserAccountServiceConfiguration.class, config);
        }
        final DefaultUserAccountServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultUserAccountServiceConfiguration.class);
        return new DefaultUserAccountService(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getValidationService(),
            coreServices.getServiceRegistry(),
            partial.get(IdService.class),
            partial.get(ClusterService.class),
            partial.get(AuthorizationService.class),
            partial.get(UserSessionStore.class),
            partial.get(UserAccountStore.class),
            partial.get(FirebaseAuthService.class),
            partial.get(PasswordService.class),
            cfg);
    }
}
