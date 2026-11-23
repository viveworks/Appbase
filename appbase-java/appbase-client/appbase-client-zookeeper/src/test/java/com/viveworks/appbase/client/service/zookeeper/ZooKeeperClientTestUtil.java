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
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
final class ZooKeeperClientTestUtil {
    private ZooKeeperClientTestUtil() {
    }

    public static void zookeeperClientServiceTest(
            final ZooKeeperClientService zooKeeperClientService,
            final ZooKeeperClientIntegrationService zooKeeperClientIntegrationService)
    {
        zookeeperClientIntegrationServiceTestImpl(zooKeeperClientService, zooKeeperClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>(false);
        zooKeeperClientIntegrationService.shutdown();
        zooKeeperClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
            @Override
            public void onSuccess() {
                repaired.setReference(true);
            }

            @Override
            public void onFailure() {
                repaired.setReference(false);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                throw new AppbaseRuntimeException(throwable);
            }
        });
        Assertions.assertTrue(repaired.getReference());

        zookeeperClientIntegrationServiceTestImpl(zooKeeperClientService, zooKeeperClientIntegrationService);
        zooKeeperClientService.shutdown();

        Assertions.assertNull(zooKeeperClientIntegrationService.getClient());
        Assertions.assertFalse(zooKeeperClientIntegrationService.isHealthy());
    }

    private static void zookeeperClientIntegrationServiceTestImpl(
            final ZooKeeperClientService zooKeeperClientService,
            final ZooKeeperClientIntegrationService zooKeeperClientIntegrationService)
    {
        Assertions.assertNotNull(zooKeeperClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = zooKeeperClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(zooKeeperClientIntegrationService);

        final CuratorFramework curatorFramework = zooKeeperClientIntegrationService.getClient();
        Assertions.assertNotNull(curatorFramework);

        Assertions.assertEquals(CuratorFrameworkState.STARTED, curatorFramework.getState());

        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = zooKeeperClientIntegrationService.getAppbaseClientKey();
        Assertions.assertNotNull(appbaseClientKey);

        Assertions.assertTrue(zooKeeperClientIntegrationService.isHealthy());
        Assertions.assertTrue(zooKeeperClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(CuratorFramework.class, zooKeeperClientIntegrationService.getClientType());
    }
}
