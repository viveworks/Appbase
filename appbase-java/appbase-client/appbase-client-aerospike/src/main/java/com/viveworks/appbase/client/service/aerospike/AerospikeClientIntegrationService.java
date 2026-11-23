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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.EventLoop;
import com.aerospike.client.async.EventLoops;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;

/**
 * Represents {@link AppbaseClientIntegrationService} for <a href="https://aerospike.com/">Aerospike</a>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AerospikeClientIntegrationService extends AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> {
    EventLoop nextEventLoop();

    /**
     * Returns the shared {@link EventLoops} used by the underlying {@link IAerospikeClient}. Exposed so
     * that a non-owning reactor client can be built from the same event loops the sync client is
     * already using.
     *
     * @return the shared {@link EventLoops}
     */
    EventLoops getEventLoops();

    /**
     * Whether Aerospike driver-native metrics are routed onto Appbase's shared Micrometer composite
     * registry (→ JMX).
     *
     * @return {@code true} if driver-native metrics are enabled
     */
    boolean isDriverMetricsEnabled();
}
