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

package com.viveworks.appbase.service.health.checkers;

import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientIntegrationService;
import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.health.HealthCheck;
import com.viveworks.appbase.service.health.HealthChecker;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ZooKeeperHealthChecker implements HealthChecker {
    private final ZooKeeperClientIntegrationService zooKeeperClientIntegrationService;
    private final String testPath;

    public ZooKeeperHealthChecker(
            final ZooKeeperClientIntegrationService zooKeeperClientIntegrationService,
            final String testPath)
    {
        AppbaseUtil.notNull(zooKeeperClientIntegrationService, "zooKeeperClientIntegrationService");
        AppbaseUtil.notBlank(testPath, "testPath");

        this.zooKeeperClientIntegrationService = zooKeeperClientIntegrationService;
        this.testPath = testPath;
    }

    @Override
    public HealthCheck runHealthCheck() {
        final CuratorFramework curatorFramework = zooKeeperClientIntegrationService.getClient();

        if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            return HealthCheck.failed(ZooKeeperClientService.class.getName());
        }

        try {
            final Stat stat = curatorFramework.checkExists().forPath(testPath);
            return stat == null ?
                    HealthCheck.failed(ZooKeeperClientService.class.getName()) :
                    HealthCheck.success(ZooKeeperClientService.class.getName());
        } catch (Exception e) {
            return HealthCheck.failed(ZooKeeperClientService.class.getName());
        }
    }
}
