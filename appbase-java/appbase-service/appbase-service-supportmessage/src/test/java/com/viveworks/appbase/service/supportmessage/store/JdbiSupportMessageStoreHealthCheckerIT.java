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
package com.viveworks.appbase.service.supportmessage.store;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.service.jdbc.JdbcClientServiceTestHelper;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientServiceTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.service.health.HealthCheck;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Reclassified from {@code JdbiSupportMessageStoreHealthCheckerTest} gated by P-051/E1135/N11 (internal work item 1301):
 * this class opens a live JDBI {@code Handle} against the vtgate declared by
 * {@code appbase-service-vitess-jdbc.yaml} ({@code localhost:15306}), so under its former
 * {@code *Test} name Surefire ran it on every ordinary offline build and it failed on
 * connection-refused.
 *
 * <p>The {@code *IT} name moves it off Surefire and onto the Failsafe opt-in this module now
 * declares; the gate below keeps it skipped until a live vtgate is explicitly opted into with
 * {@code -DJdbiClientServiceTests.enabled=true}. Both halves are load-bearing — under
 * {@code develop-profile} Failsafe does select {@code *IT} classes, so the rename alone would
 * not keep an offline build green.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "JdbiClientServiceTests.enabled", matches = "true")
class JdbiSupportMessageStoreHealthCheckerIT {
    private static AppbaseCore appbaseCore;
    private static Handle handle;
    private static JdbiClientService jdbiClientService;
    private static JdbiSupportMessageStoreHealthChecker jdbiSupportMessageStoreHealthChecker;

    @BeforeAll
    static void beforeAll() {
        System.setProperty(
                ClientCoreTestHelper.CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-core.yaml");
        System.setProperty(
                ClientCoreTestHelper.CLIENT_CORE_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-client-core.yaml");
        System.setProperty(
                JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                AppbaseTestInfraProfile.resolve(
                        AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-vitess-jdbc.yaml"));
        System.setProperty(
                JdbiClientServiceTestHelper.JDBI_CLIENT_CONFIG_FILE_PROPERTY,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-service-vitess-jdbi.yaml");

        appbaseCore = JdbiClientServiceTestHelper.appbaseCore();
        jdbiClientService = JdbiClientServiceTestHelper.defaultJdbiClientService();
        handle = jdbiClientService.getClient().open();

        final JdbiSupportMessageStoreConfiguration jdbiSupportMessageStoreConfiguration =
                JdbiSupportMessageStoreConfiguration.builder().build();
        jdbiSupportMessageStoreHealthChecker =
                new JdbiSupportMessageStoreHealthChecker(handle, jdbiSupportMessageStoreConfiguration);
    }

    @AfterAll
    static void afterAll() {
        if (handle != null && !handle.isClosed()) {
            handle.close();
        }

        if (jdbiClientService != null) {
            jdbiClientService.shutdown();
        }
    }

    @Test
    void testRunHealthCheck() {
        final HealthCheck healthCheck1 = jdbiSupportMessageStoreHealthChecker.runHealthCheck();
        Assertions.assertEquals(JdbiSupportMessageStore.class.getName(), healthCheck1.getServiceName());
        Assertions.assertTrue(healthCheck1.isHealthy());

        handle.close();
        final HealthCheck healthCheck2 = jdbiSupportMessageStoreHealthChecker.runHealthCheck();
        Assertions.assertEquals(JdbiSupportMessageStore.class.getName(), healthCheck2.getServiceName());
        Assertions.assertFalse(healthCheck2.isHealthy());
    }
}