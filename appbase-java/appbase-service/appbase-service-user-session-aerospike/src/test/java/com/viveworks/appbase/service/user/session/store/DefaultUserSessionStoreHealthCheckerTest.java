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

package com.viveworks.appbase.service.user.session.store;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientTestHelper;
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.service.health.HealthCheck;
import com.viveworks.appbase.service.id.DefaultIdService;
import com.viveworks.appbase.service.id.DefaultIdServiceConfiguration;
import com.viveworks.appbase.service.id.IdService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "AerospikeClientServiceTests.enabled", matches = "true")
class DefaultUserSessionStoreHealthCheckerTest {
    private static AppbaseCore appbaseCore;
    private static DefaultAerospikeClientService defaultAerospikeClientService;
    private static IdService idService;
    private static DefaultUserSessionStore userSessionStore;
    private static DefaultUserSessionStoreHealthChecker defaultUserSessionStoreHealthChecker;

    @BeforeAll
    static void beforeAll() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");
        System.setProperty(
                AerospikeClientTestHelper.AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-aerospike.yaml");

        appbaseCore = AerospikeClientTestHelper.appbaseCore();
        idService = new DefaultIdService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultIdServiceConfiguration.builder().build());
        defaultAerospikeClientService = AerospikeClientTestHelper.defaultAerospikeClientService();
        final DefaultUserSessionStoreConfiguration defaultUserSessionStoreConfiguration =
                DefaultUserSessionStoreConfiguration.builder().build();
        userSessionStore = new DefaultUserSessionStore(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                idService,
                defaultAerospikeClientService.getClientIntegrationService(),
                defaultUserSessionStoreConfiguration);
        defaultUserSessionStoreHealthChecker = new DefaultUserSessionStoreHealthChecker(idService, userSessionStore);
    }

    @AfterAll
    static void afterAll() {
        if (defaultAerospikeClientService != null && !defaultAerospikeClientService.isShutdown()) {
            defaultAerospikeClientService.shutdown();
        }
    }

    @Test
    void testRunHealthCheck() {
        final HealthCheck healthCheck1 = defaultUserSessionStoreHealthChecker.runHealthCheck();
        Assertions.assertEquals(DefaultUserSessionStore.class.getName(), healthCheck1.getServiceName());
        Assertions.assertTrue(healthCheck1.isHealthy());

        defaultAerospikeClientService.shutdown();

        final HealthCheck healthCheck2 = defaultUserSessionStoreHealthChecker.runHealthCheck();
        Assertions.assertEquals(DefaultUserSessionStore.class.getName(), healthCheck2.getServiceName());
        Assertions.assertFalse(healthCheck2.isHealthy());
    }
}