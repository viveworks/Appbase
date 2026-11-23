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
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.user.session.store.DefaultReactiveUserSessionStore;
import com.viveworks.appbase.service.user.session.store.DefaultUserSessionStore;
import com.viveworks.appbase.service.user.session.store.DefaultUserSessionStoreConfiguration;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link UserSessionStore}.
 *
 * <p>Cross-service dependencies:
 * <ul>
 *   <li>{@link IdService} — retrieved from the partial {@link AppbaseServices} container;
 *       register an {@link DefaultIdServiceFactory} before this factory.</li>
 *   <li>{@link com.viveworks.appbase.client.service.aerospike.AerospikeClientIntegrationService} —
 *       retrieved from {@link AppbaseClientServices} via
 *       {@link AerospikeClientService#getClientIntegrationService()}.</li>
 * </ul>
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #AerospikeUserSessionStoreFactory(UserSessionStore)}</li>
 *   <li>Config-based: {@link #AerospikeUserSessionStoreFactory(DefaultUserSessionStoreConfiguration)}</li>
 *   <li>Default: {@link #AerospikeUserSessionStoreFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class AerospikeUserSessionStoreFactory implements AppbaseServiceFactory<UserSessionStore> {

    @Nullable private final UserSessionStore predefinedService;
    @Nullable private final DefaultUserSessionStoreConfiguration config;

    /** Wraps a pre-built {@link UserSessionStore} instance. */
    public AerospikeUserSessionStoreFactory(final UserSessionStore service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultUserSessionStore} from {@code config}. */
    public AerospikeUserSessionStoreFactory(final DefaultUserSessionStoreConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultUserSessionStore} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public AerospikeUserSessionStoreFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<UserSessionStore> serviceType() {
        return UserSessionStore.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.USER_SESSION_STORE.order();
    }

    @Override
    public UserSessionStore create(final AppbaseCoreServices coreServices,
                                    final AppbaseClientServices clientServices,
                                    final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultUserSessionStoreConfiguration.class, config);
        }
        final DefaultUserSessionStoreConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultUserSessionStoreConfiguration.class);
        final DefaultUserSessionStore userSessionStore = new DefaultUserSessionStore(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            partial.get(IdService.class),
            clientServices.get(AerospikeClientService.class).getClientIntegrationService(),
            cfg);

        // Reactive read twin -- self-registers into the ServiceRegistry via its own ctor, exactly
        // as DefaultUserSessionStore registers its own extra types in registerService().
        new DefaultReactiveUserSessionStore(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            partial.get(IdService.class),
            clientServices.get(AerospikeClientService.class).getReactiveAccessor(),
            cfg);

        return userSessionStore;
    }
}
