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
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.core.util.AppbaseRegistry;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Registry for {@link AppbaseClientPool}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
public interface AppbaseClientPoolRegistry<T, V extends ComparableValue> extends AppbaseRegistry {
    /**
     * Get the map of {@link AppbaseClientIntegrationService}s keyed by {@link AppbaseClientKey}.
     *
     * @return Map of {@link AppbaseClientIntegrationService}s keyed by {@link AppbaseClientKey}
     */
    @Unmodifiable
    Map<AppbaseClientKey<V>, Collection<AppbaseClientIntegrationService<T, V>>>
    getAppbaseClientIntegrationServicesByKeyMap();


    /**
     * Get {@link AppbaseClientIntegrationService}s for a specific {@link AppbaseClientKey}.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @return {@link AppbaseClientIntegrationService}s for a specific {@link AppbaseClientKey}
     */
    @Unmodifiable
    @Nullable
    Collection<AppbaseClientIntegrationService<T, V>> getAppbaseClientIntegrationServices(
            AppbaseClientKey<V> appbaseClientKey);


    /**
     * Register {@link AppbaseClientIntegrationService} by using the {@link AppbaseClientKey} from
     * {@link AppbaseClientIntegrationService}.
     *
     * @param appbaseClientIntegrationService {@link AppbaseClientIntegrationService}
     */
    void registerAppbaseClientIntegrationService(AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService);


    /**
     * Register {@link AppbaseClientIntegrationService} by using the {@link AppbaseClientKey} from
     * {@link AppbaseClientIntegrationService}.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @param appbaseClientIntegrationService {@link AppbaseClientIntegrationService}
     */
    void registerAppbaseClientIntegrationService(
            AppbaseClientKey<V> appbaseClientKey,
            AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService);


    /**
     * Unregister {@link AppbaseClientIntegrationService} using its {@code type} and {@link AppbaseClientKey}.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @param appbaseClientIntegrationServiceType {@link AppbaseClientIntegrationService} type
     */
    void unregisterAppbaseClientIntegrationService(
            AppbaseClientKey<V> appbaseClientKey,
            Class<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServiceType);


    /**
     * Unregister {@link AppbaseClientIntegrationService} by {@link AppbaseClientKey}.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     */
    void unregisterAppbaseClientIntegrationServices(AppbaseClientKey<V> appbaseClientKey);


    /**
     * Unregister {@link AppbaseClientIntegrationService}.
     *
     * @param appbaseClientIntegrationService {@link AppbaseClientIntegrationService}
     */
    void unregisterAppbaseClientIntegrationServices(AppbaseClientIntegrationService<T, V> appbaseClientIntegrationService);


    /**
     * Unregister {@link AppbaseClientIntegrationService}.
     *
     * @param appbaseClientIntegrationServiceType {@link AppbaseClientIntegrationService} type
     */
    void unregisterAppbaseClientIntegrationServices(
            Class<AppbaseClientIntegrationService<T, V>> appbaseClientIntegrationServiceType);


    /**
     * Clear all {@link AppbaseClientIntegrationService}s.
     */
    void clearAllAppbaseClientIntegrationServices();
}
