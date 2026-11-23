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

package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.core.AppbaseException;

/**
 * Factory that creates one typed Appbase core service.
 *
 * <p>Factories are sorted by {@link #initializationOrder()} before execution so that
 * dependencies already exist in the partial container when {@link #create} is called.
 *
 * <p>The return type of {@link #initializationOrder()} is {@code int} (not {@link CoreServiceOrder})
 * so that external factories can slot between built-in values without modifying the enum.
 *
 * <p>The type parameter {@code T} is intentionally unbounded so that utility services such as
 * {@link com.viveworks.appbase.core.util.lucene.LuceneService} — which extend
 * {@link com.viveworks.appbase.core.service.Shutdownable} but not
 * {@link com.viveworks.appbase.core.service.AppbaseService} — can also be managed by this factory.
 *
 * @param <T> the service type produced by this factory
 */
public interface AppbaseCoreServiceFactory<T> {

    /**
     * Returns the service interface class this factory produces.
     *
     * @return service interface class; must be the same type registered in {@link AppbaseCoreServices}
     */
    Class<T> serviceType();

    /**
     * Returns numeric initialization order; lower values initialize first.
     * Use {@link CoreServiceOrder#order()} to supply the value for built-in services.
     *
     * @return initialization order
     */
    int initializationOrder();

    /**
     * Registers configuration stores or pre-registers typed configuration before {@link #create}
     * is invoked for this factory.
     *
     * <p>Called in initialization order; the {@code partial} container contains all core services
     * initialized before this factory, including
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService} once it has
     * been initialized (factories with higher {@link #initializationOrder()} than the configuration
     * service can register additional stores here).
     *
     * <p>The default implementation is a no-op.
     *
     * @param partial partially-built container (never null)
     */
    default void registerConfiguration(AppbaseCoreServices partial) {
        // no-op by default
    }

    /**
     * Creates the service. Call {@link AppbaseCoreServices#get(Class)} on {@code partial}
     * to retrieve services that were initialized before this one.
     *
     * @param partial partially-built container — all factories with lower order have already run
     * @return the newly created service instance (must not be null)
     * @throws AppbaseException if creation fails
     */
    T create(AppbaseCoreServices partial) throws AppbaseException;
}
