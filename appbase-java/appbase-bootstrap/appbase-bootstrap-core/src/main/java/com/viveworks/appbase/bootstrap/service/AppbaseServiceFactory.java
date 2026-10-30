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

package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseException;

/**
 * Factory that creates one typed Appbase application service.
 *
 * <p>Factories are sorted by {@link #initializationOrder()} before execution so that
 * cross-service dependencies (e.g. {@code UserAccountStore} depends on {@code PasswordService})
 * are available in the partial container when {@link #create} is called.
 *
 * <p>The return type of {@link #initializationOrder()} is {@code int} (not
 * {@link AppbaseServiceOrder}) so that external factories can slot between built-in values
 * without modifying the enum.
 *
 * <p>The type parameter {@code T} is intentionally unbounded so that any application-layer
 * service or store type can be managed by this factory.
 *
 * @param <T> the service type produced by this factory
 */
public interface AppbaseServiceFactory<T> {

    /**
     * Returns the service interface class this factory produces.
     *
     * @return service interface class; used as the key in {@link AppbaseServices}
     */
    Class<T> serviceType();

    /**
     * Returns numeric initialization order; lower values initialize first.
     * Use {@link AppbaseServiceOrder#order()} to supply the value for built-in services.
     *
     * @return initialization order
     */
    int initializationOrder();

    /**
     * Creates the application service.
     *
     * <p>If a pre-built service was supplied at construction time, return it directly.
     * If a configuration was supplied, register it with
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService#putTypedConfiguration}
     * then construct the {@code Default*} implementation using the core service getters.
     * Otherwise, read the configuration from
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService#getTypedConfiguration}.
     *
     * <p>Call {@code clientServices.get(XxxClientService.class)} to retrieve client-layer
     * dependencies. Call {@code partial.get(XxxService.class)} to retrieve application-layer
     * services initialized earlier in the same build pass.
     *
     * @param coreServices   the fully-built core services container
     * @param clientServices the fully-built client services container
     * @param partial        partially-built application services container — all factories with
     *                       a lower {@link #initializationOrder()} have already been run
     * @return the newly created service instance (must not be null)
     * @throws AppbaseException if creation fails
     */
    T create(AppbaseCoreServices coreServices,
             AppbaseClientServices clientServices,
             AppbaseServices partial) throws AppbaseException;
}
