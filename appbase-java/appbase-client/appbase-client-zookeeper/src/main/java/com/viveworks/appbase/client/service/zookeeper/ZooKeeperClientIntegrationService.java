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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import org.apache.curator.framework.CuratorFramework;

/**
 * Represents {@link AppbaseClientIntegrationService} for <a href="http://zookeeper.apache.org/">ZooKeeper</a> using
 * <a href="https://curator.apache.org/">Curator</a>.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ZooKeeperClientIntegrationService
        extends AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>
{
    /**
     * Whether the Curator connection-state should be bridged onto Appbase's shared Micrometer
     * composite registry (→ JMX) as a gauge fed by a Curator {@code ConnectionStateListener} --
     * the listener updates the gauge value on each connection-state-change event rather than
     * the gauge polling a live counter. Defaults to {@code true}.
     *
     * @return {@code true} if driver connection-state metrics should be enabled
     */
    boolean isDriverMetricsEnabled();
}
