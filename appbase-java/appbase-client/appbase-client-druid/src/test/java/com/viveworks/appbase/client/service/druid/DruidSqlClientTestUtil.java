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

package com.viveworks.appbase.client.service.druid;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientIntegrationService;
import com.viveworks.appbase.client.service.druid.sql.DruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameter;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSet;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSetHandler;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClient;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientException;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlType;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class DruidSqlClientTestUtil {
    public static final int TEST_SLEEP_MILLIS = 5_000;
    private static final String DRUID_SQL_CLIENT_SHARED_RESOURCES_CONFIGURATION =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-druid-sql-shared-resources.yaml";
    private static final String DRUID_SQL_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-druid-sql.yaml";

    private DruidSqlClientTestUtil() {
    }

    public static void druidSqlClientServiceTest(
            final DruidSqlClientService druidSqlClientService,
            final DruidSqlClientIntegrationService druidSqlClientIntegrationService)
    {
        druidSqlClientServiceTestImpl(druidSqlClientService, druidSqlClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        druidSqlClientIntegrationService.shutdown();
        druidSqlClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        druidSqlClientServiceTestImpl(druidSqlClientService, druidSqlClientIntegrationService);
        druidSqlClientService.shutdown();

        Assertions.assertNull(druidSqlClientIntegrationService.getClient());
        Assertions.assertFalse(druidSqlClientIntegrationService.isHealthy());
    }

    public static void druidSqlClientServiceTestImpl(
            final DruidSqlClientService druidSqlClientService,
            final DruidSqlClientIntegrationService druidSqlClientIntegrationService)
    {
        Assertions.assertNotNull(druidSqlClientService);

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = druidSqlClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(druidSqlClientIntegrationService);

        final DruidSqlClient druidSqlClient = druidSqlClientIntegrationService.getClient();
        executeQueryTest(druidSqlClient);
        executeParameterizedQueryTest(druidSqlClient);

        Assertions.assertTrue(druidSqlClientIntegrationService.isHealthy());
        Assertions.assertTrue(druidSqlClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(DruidSqlClient.class, druidSqlClientIntegrationService.getClientType());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void executeQueryTest(final DruidSqlClient druidSqlClient) {
        Assertions.assertNotNull(druidSqlClient);

        final AtomicInteger counter = new AtomicInteger(0);
        try {
            druidSqlClient.asyncExecuteQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES", new CustomDruidResultSetHandler(counter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            throw new AppbaseRuntimeException("Error while executing druid SQL query", ex);
        }

        Assertions.assertTrue(counter.get() > 0);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void executeParameterizedQueryTest(final DruidSqlClient druidSqlClient) {
        Assertions.assertNotNull(druidSqlClient);

        final AtomicInteger counter = new AtomicInteger(0);
        try {
            druidSqlClient.asyncExecuteParameterizedQuery(
                    "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?",
                    List.of(new DefaultDruidSqlParameter(1, "servers", DruidSqlType.VARCHAR)),
                    new CustomDruidResultSetHandler(counter));
            Thread.sleep(TEST_SLEEP_MILLIS);
        } catch (Exception ex) {
            throw new AppbaseRuntimeException("Error while executing druid SQL query", ex);
        }

        Assertions.assertTrue(counter.get() > 0);
    }

    public static final class CustomDruidResultSetHandler implements DruidResultSetHandler {
        private final AtomicInteger counter;

        private CustomDruidResultSetHandler(final AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void complete(final DruidResultSet result) {
            Assertions.assertNotNull(result);
            try {
                while (result.next()) {
                    counter.incrementAndGet();

                    final String tableName = result.getVarchar("TABLE_NAME");
                    Assertions.assertNotNull(tableName);
                }
            } catch (DruidSqlClientException dsce) {
                Assertions.fail(dsce);
            }
        }

        @Override
        public void completeWithException(final Throwable throwable) {
            Assertions.fail(throwable);
        }
    }
}
