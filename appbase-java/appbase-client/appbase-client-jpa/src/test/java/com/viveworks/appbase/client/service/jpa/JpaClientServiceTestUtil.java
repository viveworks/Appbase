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

package com.viveworks.appbase.client.service.jpa;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

import jakarta.persistence.EntityManager;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class JpaClientServiceTestUtil {
    private JpaClientServiceTestUtil() {
    }

    public static void jpaClientServiceTest(
            final JpaClientService jpaClientService,
            final JpaClientIntegrationService jpaClientIntegrationService)
    {
        jpaClientServiceTestImpl(jpaClientService, jpaClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        jpaClientIntegrationService.shutdown();
        jpaClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        jpaClientServiceTestImpl(jpaClientService, jpaClientIntegrationService);
        jpaClientService.shutdown();

        Assertions.assertNull(jpaClientIntegrationService.getClient());
        Assertions.assertFalse(jpaClientIntegrationService.isHealthy());
    }

    public static void jpaClientServiceTestImpl(
            final JpaClientService jpaClientService,
            final JpaClientIntegrationService jpaClientIntegrationService)
    {
        Assertions.assertNotNull(jpaClientService);
        final AppbaseClientKey<TextValue> defaultAppbaseClientKey = jpaClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(jpaClientIntegrationService);

        final EntityManager entityManager = jpaClientIntegrationService.getClient();
        Assertions.assertNotNull(entityManager);
        Assertions.assertTrue(entityManager.isOpen());

        Assertions.assertTrue(jpaClientIntegrationService.isHealthy());
        Assertions.assertTrue(jpaClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(EntityManager.class, jpaClientIntegrationService.getClientType());
    }
}
