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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;

import io.r2dbc.pool.ConnectionPool;

/**
 * Represents {@link AppbaseClientIntegrationService} for {@code R2DBC} (Reactive Relational
 * Database Connectivity). {@link #getClient()} returns the native {@link ConnectionPool} built
 * and held for this key; the pool itself is the reactive connection source &mdash; callers obtain
 * a {@code io.r2dbc.spi.Connection} via {@code ConnectionPool#create()}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface R2dbcClientIntegrationService extends AppbaseClientIntegrationService<ConnectionPool, TextValue> {
    /**
     * Whether native R2DBC connection-pool metrics ({@code io.r2dbc.pool.PoolMetrics}) should be
     * bridged onto Appbase's shared Micrometer composite registry (&rarr; JMX) as polling gauges.
     * Defaults to {@code true}.
     *
     * @return {@code true} if driver connection-pool metrics should be enabled
     */
    boolean isDriverMetricsEnabled();
}
