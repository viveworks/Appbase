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

package com.viveworks.appbase.client.core.pool;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.util.di.guice.InjectedConfiguration;

import javax.annotation.Nullable;

/**
 * This class represents a {@code pool} for {@link AppbaseClientIntegrationService}s. It manages a pool of
 * {@link AppbaseClientIntegrationService}, uses {@link AppbaseClientPoolManager} to manage unhealthy clients.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
@InjectedConfiguration(at = AppbaseClientServiceConfiguration.class)
public interface AppbaseClientPool<T, V extends ComparableValue> extends Shutdownable {
    /**
     * Select a suitable {@link AppbaseClientIntegrationService} for a given {@link AppbaseClientKey}.
     * {@link #giveAppbaseClientIntegrationService(AppbaseClientIntegrationService)} is this method's counterpart and must
     * be called to give back {@link AppbaseClientIntegrationService} to the pool.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @return {@link AppbaseClientIntegrationService}
     */
    @Nullable
    AppbaseClientIntegrationService<T, V> takeAppbaseClientIntegrationService(AppbaseClientKey<V> appbaseClientKey);


    /**
     * Give {@link AppbaseClientIntegrationService} back to pool.
     *
     * @param appbaseClientIntegrationService {@link AppbaseClientIntegrationService}
     */
    void giveAppbaseClientIntegrationService(AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService);


    /**
     * Notify that a {@link AppbaseClientIntegrationService} is unhealthy. Mainly used by {@link AppbaseClientPoolManager},
     * but a.
     *
     * @param appbaseClientIntegrationService {@link AppbaseClientIntegrationService}
     */
    void unhealthyAppbaseClientIntegrationService(AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService);


    /**
     * Notify that a {@link AppbaseClientIntegrationService} is repaired.
     *
     * @param appbaseClientIntegrationService {@link AppbaseClientIntegrationService}
     */
    void repairedAppbaseClientIntegrationService(AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService);


    /**
     * Get the {@link AppbaseClientPoolRegistry} that the pool uses.
     *
     * @return {@link AppbaseClientPoolRegistry}
     */
    AppbaseClientPoolRegistry<T, V> getAppbaseClientPoolRegistry();
}
