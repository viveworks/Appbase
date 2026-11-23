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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.cassandra.reactive.ReactiveCassandraAccessor;

/**
 * Represents {@link AppbaseClientService} for <a href="https://cassandra.apache.org/">Apache Cassandra</a>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface CassandraClientService extends AppbaseClientService<CqlSession, ClusterIdValue> {
    @Override
    CassandraClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    CassandraClientIntegrationService getClientIntegrationService(
            AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveCassandraAccessor} bound to this service's default connection pool.
     *
     * @return reactive accessor for the default Cassandra connection pool
     */
    ReactiveCassandraAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveCassandraAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target Cassandra connection pool
     * @return reactive accessor for the keyed Cassandra connection pool
     */
    ReactiveCassandraAccessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Returns a {@link CassandraAccessor} bound to this service's default connection pool.
     *
     * @return synchronous accessor for the default Cassandra connection pool
     */
    CassandraAccessor getAccessor();

    /**
     * Returns a {@link CassandraAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target Cassandra connection pool
     * @return synchronous accessor for the keyed Cassandra connection pool
     */
    CassandraAccessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
