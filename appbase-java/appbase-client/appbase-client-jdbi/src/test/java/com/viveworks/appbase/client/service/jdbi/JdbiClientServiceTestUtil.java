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

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class JdbiClientServiceTestUtil {
    private JdbiClientServiceTestUtil() {
    }

    public static void jdbiClientServiceTest(
            final JdbiClientService jdbiClientService,
            final JdbiClientIntegrationService jdbiClientIntegrationService)
    {
        jdbiClientServiceTestImpl(jdbiClientService, jdbiClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        jdbiClientIntegrationService.shutdown();
        jdbiClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        jdbiClientServiceTestImpl(jdbiClientService, jdbiClientIntegrationService);
        jdbiClientService.shutdown();

        Assertions.assertNull(jdbiClientIntegrationService.getClient());
        Assertions.assertFalse(jdbiClientIntegrationService.isHealthy());

        ((DefaultJdbiClientIntegrationService) jdbiClientIntegrationService).getJdbcClientIntegrationService().shutdown();
    }

    public static void jdbiClientServiceTestImpl(
            final JdbiClientService jdbiClientService,
            final JdbiClientIntegrationService jdbiClientIntegrationService)
    {
        Assertions.assertNotNull(jdbiClientService);

        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = jdbiClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(jdbiClientIntegrationService);

        final Jdbi jdbi = jdbiClientIntegrationService.getClient();
        Assertions.assertNotNull(jdbi);

        Assertions.assertTrue(jdbiClientIntegrationService.isHealthy());
        Assertions.assertTrue(jdbiClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(Jdbi.class, jdbiClientIntegrationService.getClientType());
    }
}
