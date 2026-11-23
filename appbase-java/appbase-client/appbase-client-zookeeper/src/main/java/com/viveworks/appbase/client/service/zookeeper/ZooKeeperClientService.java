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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.zookeeper.accessor.ZooKeeperAccessor;
import com.viveworks.appbase.client.service.zookeeper.accessor.reactive.ReactiveZooKeeperAccessor;
import org.apache.curator.framework.CuratorFramework;

/**
 * Represents {@link AppbaseClientService} for <a href="http://zookeeper.apache.org/">ZooKeeper</a> using
 * <a href="https://curator.apache.org/">Curator</a>.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ZooKeeperClientService extends AppbaseClientService<CuratorFramework, ClusterIdValue> {
    @Override
    ZooKeeperClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;

    @Override
    ZooKeeperClientIntegrationService getClientIntegrationService(
            AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveZooKeeperAccessor} bound to this service's default connection pool.
     *
     * @return reactive accessor for the default ZooKeeper connection pool
     */
    ReactiveZooKeeperAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveZooKeeperAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target ZooKeeper connection pool
     * @return reactive accessor for the keyed ZooKeeper connection pool
     */
    ReactiveZooKeeperAccessor getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Returns a {@link ZooKeeperAccessor} bound to this service's default connection pool.
     *
     * @return synchronous accessor for the default ZooKeeper connection pool
     */
    ZooKeeperAccessor getAccessor();

    /**
     * Returns a {@link ZooKeeperAccessor} bound to the connection pool for the given key.
     *
     * @param appbaseClientKey key identifying the target ZooKeeper connection pool
     * @return synchronous accessor for the keyed ZooKeeper connection pool
     */
    ZooKeeperAccessor getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
