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

package com.viveworks.appbase.client.service.r2dbc.configuration;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationServiceConfiguration;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.r2dbc.R2dbcClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface R2dbcClientIntegrationServiceConfiguration extends AppbaseClientIntegrationServiceConfiguration {
    /**
     * Whether native R2DBC connection-pool metrics ({@code io.r2dbc.pool.PoolMetrics}, obtained
     * via {@code io.r2dbc.pool.ConnectionPool#getMetrics()}) should be bridged onto Appbase's
     * shared Micrometer composite registry (&rarr; JMX) as polling gauges. Defaults to
     * {@code true}, uniform with the jasync/Aerospike/Cassandra/ZooKeeper twins, which all
     * default this gate on.
     *
     * @return {@code true} if driver connection-pool metrics should be enabled
     */
    boolean isDriverMetricsEnabled();
}
