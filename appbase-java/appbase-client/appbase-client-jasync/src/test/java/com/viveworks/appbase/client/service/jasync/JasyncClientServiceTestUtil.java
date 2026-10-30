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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.Connection;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "JasyncClientServiceTests.enabled", matches = "true")
public final class JasyncClientServiceTestUtil {
    private JasyncClientServiceTestUtil() {
    }

    public static void jasyncClientServiceTest(
            final JasyncClientService jasyncClientService,
            final JasyncClientIntegrationService jasyncClientIntegrationService)
    {
        jasyncClientServiceTestImpl(jasyncClientService, jasyncClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        jasyncClientIntegrationService.shutdown();
        jasyncClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        jasyncClientServiceTestImpl(jasyncClientService, jasyncClientIntegrationService);
        jasyncClientService.shutdown();

        Assertions.assertNull(jasyncClientIntegrationService.getClient());
        Assertions.assertFalse(jasyncClientIntegrationService.isHealthy());
    }

    private static void jasyncClientServiceTestImpl(
            final JasyncClientService jasyncClientService,
            final JasyncClientIntegrationService jasyncClientIntegrationService)
    {
        Assertions.assertNotNull(jasyncClientService);
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = jasyncClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(jasyncClientIntegrationService);

        final Connection connection = jasyncClientIntegrationService.getClient();
        Assertions.assertNotNull(connection);
        Assertions.assertTrue(connection.isConnected());

        Assertions.assertTrue(jasyncClientIntegrationService.isHealthy());
        Assertions.assertTrue(jasyncClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(Connection.class, jasyncClientIntegrationService.getClientType());
    }
}
