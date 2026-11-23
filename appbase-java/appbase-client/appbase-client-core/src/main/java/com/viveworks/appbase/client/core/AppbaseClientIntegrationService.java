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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.core.service.Shutdownable;

/**
 * Represents integration with an underlying client of type {@code T}. It serves as the
 * {@code SPI (Service Provider Interface)} for {@code T}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
public interface AppbaseClientIntegrationService<T, V extends ComparableValue> extends Shutdownable {
    /**
     * Get the underlying client. After using the client it must be returned to the pool.
     *
     * @return Client
     */
    T getClient();


    /**
     * This <b>must</b> be called after executing <b>every</b> client operation, so it can be returned to the pool.
     */
    void finalizeOperation();


    /**
     * Called by {@link com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager} to repair when this service
     * is unhealthy. This is usually a reconnect attempt using {@link T}, creating a new {@link T} and so forth.
     *
     * @param repairListener {@link RepairListener}
     */
    void repair(RepairListener repairListener);


    /**
     * Get the key that represents an instance of this type.
     *
     * @return Key that represents an instance of this type
     */
    AppbaseClientKey<V> getAppbaseClientKey();


    /**
     * Get the underlying client type.
     *
     * @return Client type
     */
    Class<T> getClientType();


    /**
     * Check if the underlying client is healthy.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();


    /**
     * Check if {@link #repair(RepairListener)} is enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isRepairEnabled();


    /**
     * It is important to define this method and {@link #hashCode()} properly, since
     * {@link com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager} relies on them.
     *
     * @param other Other {@link Object}
     * @return true if equal, false otherwise
     */
    @Override
    boolean equals(Object other);


    /**
     * It is important to define this method and {@link #equals(Object)} properly, since
     * {@link com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager} relies on them.
     *
     * @return Hashcode for the instance of this type
     */
    @Override
    int hashCode();



    /**
     * Listener for {@link #repair(RepairListener)}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    interface RepairListener {
        void onSuccess();

        void onFailure();

        void onFailure(Throwable throwable);
    }



    /**
     * Thrown by {@link AppbaseClientService} methods that return an {@link AppbaseClientIntegrationService} from a backing
     * {@link com.viveworks.appbase.client.core.pool.AppbaseClientPool}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    class NotFoundException extends AppbaseClientIntegrationRuntimeException {
        private static final long serialVersionUID = 2322723898144418517L;

        public NotFoundException() {
        }

        public NotFoundException(final String message) {
            super(message);
        }

        public NotFoundException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public NotFoundException(final Throwable cause) {
            super(cause);
        }

        public NotFoundException(
                final String message,
                final Throwable cause,
                final boolean enableSuppression,
                final boolean writableStackTrace)
        {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
