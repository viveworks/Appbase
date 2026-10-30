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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.IAerospikeClient;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.aerospike.reactive.ReactiveAerospikeAccessor;

/**
 * Represents {@link AppbaseClientService} for <a href="https://aerospike.com/">Aerospike</a>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AerospikeClientService extends AppbaseClientService<IAerospikeClient, ClusterIdValue> {
    @Override
    AerospikeClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    AerospikeClientIntegrationService getClientIntegrationService(
            AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveAerospikeAccessor} wired to the default {@link AppbaseClientKey}'s
     * reactor client.
     *
     * @return {@link ReactiveAerospikeAccessor} for the default client key
     */
    ReactiveAerospikeAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveAerospikeAccessor} wired to the given {@link AppbaseClientKey}'s reactor
     * client.
     *
     * @param appbaseClientKey {@link AppbaseClientKey}
     * @return {@link ReactiveAerospikeAccessor} for the given client key
     */
    ReactiveAerospikeAccessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Returns an {@link AerospikeAccessor} bound to this service's default integration service.
     *
     * @return synchronous accessor for the default Aerospike integration service
     */
    AerospikeAccessor getAccessor();

    /**
     * Returns an {@link AerospikeAccessor} bound to the integration service for the given key.
     *
     * @param appbaseClientKey key identifying the target Aerospike integration service
     * @return synchronous accessor for the keyed Aerospike integration service
     */
    AerospikeAccessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
