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

import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Live MariaDB CRUD contract through JPA's native-query surface. */
@EnabledIfSystemProperty(named = "JpaClientServiceTests.enabled", matches = "true")
class JpaLiveCrudIT {
    private static final String TABLE = "appbase_jpa_live_it";
    private static DefaultJpaClientService service;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultJpaClientService.registerObjectMapperSubtypes();
        service = JpaClientServiceTestHelper.defaultJpaClientService();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (service != null) {
            final EntityManager entityManager = service.getClientIntegrationService().getClient();
            inTransaction(entityManager, () -> entityManager
                    .createNativeQuery("DROP TABLE IF EXISTS " + TABLE).executeUpdate());
            service.shutdown();
        }
    }

    @Test
    void createReadUpdateDeleteAndNegativeQuery() throws Exception {
        final EntityManager entityManager = service.getClientIntegrationService().getClient();
        inTransaction(entityManager, () -> {
            entityManager.createNativeQuery("DROP TABLE IF EXISTS " + TABLE).executeUpdate();
            entityManager.createNativeQuery("CREATE TABLE " + TABLE + " (id INT PRIMARY KEY, value VARCHAR(64))")
                    .executeUpdate();
            Assertions.assertEquals(1, entityManager.createNativeQuery(
                    "INSERT INTO " + TABLE + " (id,value) VALUES (1,'before')").executeUpdate());
        });
        Assertions.assertEquals("before", entityManager.createNativeQuery(
                "SELECT value FROM " + TABLE + " WHERE id=1").getSingleResult());
        inTransaction(entityManager, () -> {
            Assertions.assertEquals(1, entityManager.createNativeQuery(
                    "UPDATE " + TABLE + " SET value='after' WHERE id=1").executeUpdate());
            Assertions.assertEquals(1, entityManager.createNativeQuery(
                    "DELETE FROM " + TABLE + " WHERE id=1").executeUpdate());
            entityManager.createNativeQuery("DROP TABLE " + TABLE).executeUpdate();
        });
        Assertions.assertThrows(Exception.class,
                () -> entityManager.createNativeQuery("SELECT * FROM appbase_jpa_missing_table").getResultList());
    }

    private static void inTransaction(final EntityManager entityManager, final Runnable operation) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            operation.run();
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
    }
}
