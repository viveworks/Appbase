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

package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;

/**
 * Factory that creates one typed Appbase client service.
 *
 * <p>Factories are sorted by {@link #initializationOrder()} before execution.
 *
 * <p>The return type of {@link #initializationOrder()} is {@code int} (not {@link ClientServiceOrder})
 * so that external factories can slot between built-in values without modifying the enum.
 *
 * <p>The type parameter {@code T} is intentionally unbounded so that any client service type can
 * be managed by this factory, regardless of its supertype hierarchy.
 *
 * @param <T> the client service type produced by this factory
 */
public interface AppbaseClientServiceFactory<T> {

    /**
     * Returns the client service interface class this factory produces.
     *
     * @return client service interface class; used as the key in {@link AppbaseClientServices}
     */
    Class<T> serviceType();

    /**
     * Returns numeric initialization order; lower values initialize first.
     * Use {@link ClientServiceOrder#order()} to supply the value for built-in services.
     *
     * @return initialization order
     */
    int initializationOrder();

    /**
     * Registers configuration types and classpath stores needed by this factory's service.
     *
     * <p>Called by {@link AppbaseClientServices.Builder} for every factory, in
     * {@link #initializationOrder()} order, before any {@link #create} calls.
     * {@link ConfigurationService#load()} is invoked once by the builder after all factories
     * have returned from this method.
     *
     * <p>The default implementation is a no-op. Factories that need to register
     * YAML classpath stores or configuration type mappings should override this method.
     *
     * @param coreServices the fully-built core services container
     */
    default void registerConfiguration(AppbaseCoreServices coreServices) {}

    /**
     * Creates the client service.
     *
     * <p>If a pre-built service was supplied at construction time, return it directly.
     * Configuration has already been loaded by the time this method is called — see
     * {@link #registerConfiguration}.
     *
     * @param coreServices       the fully-built core services container
     * @param appbaseClientCore  the shared client core built by {@link AppbaseClientServices}
     * @return the newly created client service instance (must not be null)
     * @throws AppbaseException if creation fails
     */
    T create(AppbaseCoreServices coreServices, AppbaseClientCore appbaseClientCore) throws AppbaseException;
}
