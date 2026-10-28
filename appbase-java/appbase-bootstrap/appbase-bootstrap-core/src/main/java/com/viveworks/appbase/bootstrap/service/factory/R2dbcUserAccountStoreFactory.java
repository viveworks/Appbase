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
import com.viveworks.appbase.client.service.r2dbc.R2dbcClientService;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.store.R2dbcReactiveUserAccountStore;
import com.viveworks.appbase.service.user.account.store.R2dbcReactiveUserAccountStoreConfiguration;
import com.viveworks.appbase.service.user.account.store.ReactiveUserAccountStore;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link ReactiveUserAccountStore} using {@link R2dbcReactiveUserAccountStore}
 * -- the reactive JPA path's read twin, module-per-backing sibling of {@link
 * JdbiUserAccountStoreFactory} (blocking).
 *
 * <p>Cross-service dependencies:
 * <ul>
 *   <li>{@link PasswordService} — retrieved from the partial {@link AppbaseServices} container;
 *       register a {@link DefaultPasswordServiceFactory} (or another password-service factory)
 *       before this factory.</li>
 *   <li>{@link R2dbcClientService} — retrieved from {@link AppbaseClientServices}; its {@link
 *       R2dbcClientService#getReactiveAccessor()} supplies the reactive accessor. Register an
 *       {@code R2dbcClientServiceFactory} before this factory.</li>
 * </ul>
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #R2dbcUserAccountStoreFactory(ReactiveUserAccountStore)}</li>
 *   <li>Config-based: {@link #R2dbcUserAccountStoreFactory(R2dbcReactiveUserAccountStoreConfiguration)}</li>
 *   <li>Default: {@link #R2dbcUserAccountStoreFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class R2dbcUserAccountStoreFactory implements AppbaseServiceFactory<ReactiveUserAccountStore> {

    @Nullable private final ReactiveUserAccountStore predefinedService;
    @Nullable private final R2dbcReactiveUserAccountStoreConfiguration config;

    /** Wraps a pre-built {@link ReactiveUserAccountStore} instance. */
    public R2dbcUserAccountStoreFactory(final ReactiveUserAccountStore service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link R2dbcReactiveUserAccountStore} from {@code config}. */
    public R2dbcUserAccountStoreFactory(final R2dbcReactiveUserAccountStoreConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link R2dbcReactiveUserAccountStore} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public R2dbcUserAccountStoreFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<ReactiveUserAccountStore> serviceType() {
        return ReactiveUserAccountStore.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.USER_ACCOUNT_STORE.order();
    }

    @Override
    public ReactiveUserAccountStore create(final AppbaseCoreServices coreServices,
                                            final AppbaseClientServices clientServices,
                                            final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                R2dbcReactiveUserAccountStoreConfiguration.class, config);
        }
        final R2dbcReactiveUserAccountStoreConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(R2dbcReactiveUserAccountStoreConfiguration.class);
        return new R2dbcReactiveUserAccountStore(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            partial.get(PasswordService.class),
            clientServices.get(R2dbcClientService.class).getReactiveAccessor(),
            cfg);
    }
}
