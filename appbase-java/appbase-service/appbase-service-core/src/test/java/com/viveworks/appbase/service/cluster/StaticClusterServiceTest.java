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

package com.viveworks.appbase.service.cluster;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.service.testhelper.AppbaseServiceCoreTestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class StaticClusterServiceTest {
    private static final String TEST_HOME_DATA_CENTER = "local-test";

    private static AppbaseCore appbaseCore;

    private static StaticClusterService staticClusterService;

    @BeforeAll
    static void init() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");
        appbaseCore = ClientCoreTestHelper.appbaseCore();
        staticClusterService = new StaticClusterService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                StaticClusterServiceConfiguration.builder().homeDataCenter(TEST_HOME_DATA_CENTER).build());
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();
        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(ClusterService.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceCoreTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                ClusterService.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(staticClusterService, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 = serviceRegistry.getAppbaseService(StaticClusterService.class);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(staticClusterService, appbaseServiceEntry2.getService());
    }

    @Test
    void getHomeDataCenter() {
        final String homeDataCenter = staticClusterService.getHomeDataCenter();
        Assertions.assertNotNull(homeDataCenter);
        Assertions.assertEquals(TEST_HOME_DATA_CENTER, homeDataCenter);
    }
}