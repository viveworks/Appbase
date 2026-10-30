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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.service.AppbaseService;

/**
 * Represents a {@code client} {@link AppbaseService}. It serves as the {@code API} for a client of type {@code T}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
public interface AppbaseClientService<T, V extends ComparableValue> extends AppbaseService {
    /**
     * Get the backing {@link AppbaseClientIntegrationService}. The {@link AppbaseClientIntegrationService} <b>must</b> be
     * returned to {@link AppbaseClientPool} after use, typically after each
     * operation. This can be done using {@link AppbaseClientIntegrationService#finalizeOperation()}.
     *
     * <pre>
     *     final AppbaseClientIntegrationService&#60;T&#62; appbaseClientIntegrationService =
     *                  appbaseClientService.takeAppbaseClientIntegrationService();
     *     if (appbaseClientIntegrationService == null) {
     *         throw new IllegalStateException("appbaseClientService is not initialized");
     *     }
     *
     *     try {
     *         final T client = appbaseClientIntegrationService.getClient();
     *
     *         // use 'client' .....
     *     } catch(Exception ex) {
     *         // handle exception .....
     *     } finally {
     *         appbaseClientIntegrationService.finalizeOperation();
     *     }
     * </pre>
     *
     * @return {@link AppbaseClientIntegrationService}
     * @throws AppbaseClientIntegrationService.NotFoundException if no {@link AppbaseClientIntegrationService} was found
     * that was keyed by {@link #getDefaultAppbaseClientKey()}
     */
    AppbaseClientIntegrationService<T, V> getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Get {@link AppbaseClientIntegrationService} that is represented by a specific {@link AppbaseClientKey}.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @return Client
     * @throws AppbaseClientIntegrationService.NotFoundException if no {@link AppbaseClientIntegrationService} was found
     * that was keyed by {@link AppbaseClientKey} argument.
     */
    AppbaseClientIntegrationService<T, V> getClientIntegrationService(
            AppbaseClientKey<V> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;


    /**
     * Convenience method: returns the client from the default
     * {@link AppbaseClientIntegrationService}. Equivalent to
     * {@code getClientIntegrationService().getClient()}.
     *
     * <p>Note: this method does <em>not</em> call
     * {@link AppbaseClientIntegrationService#finalizeOperation()} — callers that
     * need explicit pool lifecycle control should use
     * {@link #getClientIntegrationService()} directly.
     *
     * @return Client of type {@code T}
     * @throws AppbaseClientIntegrationService.NotFoundException if no default
     *         integration service was found
     * @see #getClientIntegrationService()
     */
    default T getClient() throws AppbaseClientIntegrationService.NotFoundException {
        return getClientIntegrationService().getClient();
    }

    /**
     * Get {@link AppbaseClientKey} of this client service.
     *
     * @return {@link AppbaseClientKey}
     */
    AppbaseClientKey<V> getDefaultAppbaseClientKey();
}
