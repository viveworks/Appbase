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

package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class JdbcClientServiceTestUtil {
    private JdbcClientServiceTestUtil() {
    }

    public static void jdbcClientServiceTest(
            final JdbcClientService jdbcClientService,
            final JdbcClientIntegrationService jdbcClientIntegrationService)
    {
        jdbcClientServiceTestImpl(jdbcClientService, jdbcClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        jdbcClientIntegrationService.shutdown();
        jdbcClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        jdbcClientServiceTestImpl(jdbcClientService, jdbcClientIntegrationService);
        jdbcClientService.shutdown();

        Assertions.assertNull(jdbcClientIntegrationService.getClient());
        Assertions.assertFalse(jdbcClientIntegrationService.isHealthy());
    }

    private static void jdbcClientServiceTestImpl(
            final JdbcClientService jdbcClientService,
            final JdbcClientIntegrationService jdbcClientIntegrationService)
    {
        Assertions.assertNotNull(jdbcClientService);
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = jdbcClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(jdbcClientIntegrationService);

        final DataSource dataSource = jdbcClientIntegrationService.getClient();
        Assertions.assertNotNull(dataSource);

        try {
            final Connection connection = dataSource.getConnection();
            Assertions.assertNotNull(connection);

            Assertions.assertFalse(connection.isClosed());
        } catch (SQLException se) {
            throw new AppbaseRuntimeException("Unexpected exception", se);
        }

        Assertions.assertTrue(jdbcClientIntegrationService.isHealthy());
        Assertions.assertTrue(jdbcClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(DataSource.class, jdbcClientIntegrationService.getClientType());
    }
}
