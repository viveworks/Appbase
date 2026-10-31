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
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.service.user.account.store.JdbiUserAccountStore;
import com.viveworks.appbase.service.user.account.store.JdbiUserAccountStoreConfiguration;
import com.viveworks.appbase.service.user.account.store.UserAccountStore;
import org.jdbi.v3.core.Handle;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link UserAccountStore} using {@link JdbiUserAccountStore}.
 *
 * <p>Cross-service dependencies:
 * <ul>
 *   <li>{@link PasswordService} — retrieved from the partial {@link AppbaseServices} container;
 *       register a {@link DefaultPasswordServiceFactory} (or another password-service factory)
 *       before this factory.</li>
 *   <li>{@link JdbiClientService} — used to open a JDBI handle internally; the factory manages
 *       handle creation so callers do not need to open or close handles themselves.</li>
 * </ul>
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #JdbiUserAccountStoreFactory(UserAccountStore)}</li>
 *   <li>Config-based: {@link #JdbiUserAccountStoreFactory(JdbiUserAccountStoreConfiguration, JdbiClientService)}</li>
 *   <li>Default: {@link #JdbiUserAccountStoreFactory(JdbiClientService)} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class JdbiUserAccountStoreFactory implements AppbaseServiceFactory<UserAccountStore> {

    @Nullable private final UserAccountStore predefinedService;
    @Nullable private final JdbiUserAccountStoreConfiguration config;
    @Nullable private final JdbiClientService jdbiClientService;

    /** Wraps a pre-built {@link UserAccountStore} instance. */
    public JdbiUserAccountStoreFactory(final UserAccountStore service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.jdbiClientService = null;
    }

    /**
     * Creates a {@link JdbiUserAccountStore} from {@code config}, opening a JDBI handle
     * internally from the supplied {@link JdbiClientService}.
     *
     * @param config           store configuration
     * @param jdbiClientService the JDBI client service used to open a handle
     */
    public JdbiUserAccountStoreFactory(final JdbiUserAccountStoreConfiguration config,
                                       final JdbiClientService jdbiClientService) {
        this.config            = Objects.requireNonNull(config, "config");
        this.jdbiClientService = Objects.requireNonNull(jdbiClientService, "jdbiClientService");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link JdbiUserAccountStore} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService},
     * opening a JDBI handle internally from the supplied {@link JdbiClientService}.
     *
     * @param jdbiClientService the JDBI client service used to open a handle
     */
    public JdbiUserAccountStoreFactory(final JdbiClientService jdbiClientService) {
        this.jdbiClientService = Objects.requireNonNull(jdbiClientService, "jdbiClientService");
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<UserAccountStore> serviceType() {
        return UserAccountStore.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.USER_ACCOUNT_STORE.order();
    }

    @Override
    public UserAccountStore create(final AppbaseCoreServices coreServices,
                                    final AppbaseClientServices clientServices,
                                    final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                JdbiUserAccountStoreConfiguration.class, config);
        }
        final JdbiUserAccountStoreConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(JdbiUserAccountStoreConfiguration.class);
        final Handle handle = jdbiClientService.getClient().open();
        return new JdbiUserAccountStore(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            partial.get(PasswordService.class),
            handle,
            cfg);
    }
}
