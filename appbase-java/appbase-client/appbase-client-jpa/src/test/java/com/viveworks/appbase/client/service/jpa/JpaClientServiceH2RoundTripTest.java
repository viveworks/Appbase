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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

/** Exercises the supported blocking-JPA service against a real in-memory SQL engine. */
class JpaClientServiceH2RoundTripTest {

    private DefaultJpaClientService service;
    private String previousConfigFile;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
        if (previousConfigFile == null) {
            System.clearProperty(JpaClientServiceTestHelper.JPA_CONFIG_FILE_PROPERTY);
        } else {
            System.setProperty(JpaClientServiceTestHelper.JPA_CONFIG_FILE_PROPERTY, previousConfigFile);
        }
    }

    @RepeatedTest(2)
    @Timeout(60)
    void createInsertSelectRollbackAndCleanupThroughPublicService() throws Exception {
        previousConfigFile = System.getProperty(JpaClientServiceTestHelper.JPA_CONFIG_FILE_PROPERTY);
        System.setProperty(JpaClientServiceTestHelper.JPA_CONFIG_FILE_PROPERTY,
                "classpath:///appbase-client-jpa-h2.yaml");
        service = JpaClientServiceTestHelper.defaultJpaClientService();
        JpaClientIntegrationService integration = service.getClientIntegrationService();
        EntityManager entityManager = integration.getClient();
        assertNotNull(entityManager, "public JPA service must expose its configured EntityManager");

        String table = "APPBASE_JPA_" + UUID.randomUUID().toString().replace("-", "");
        try {
            inTransaction(entityManager, () -> {
                entityManager.createNativeQuery("CREATE TABLE " + table
                        + " (ID INTEGER PRIMARY KEY, DATA_VALUE VARCHAR(64))").executeUpdate();
                entityManager.createNativeQuery(
                        "INSERT INTO " + table + " (ID, DATA_VALUE) VALUES (1, 'committed')")
                        .executeUpdate();
            });

            assertEquals("committed", entityManager.createNativeQuery(
                    "SELECT DATA_VALUE FROM " + table + " WHERE ID = 1").getSingleResult());

            EntityTransaction rollback = entityManager.getTransaction();
            rollback.begin();
            entityManager.createNativeQuery(
                    "INSERT INTO " + table + " (ID, DATA_VALUE) VALUES (2, 'rolled-back')")
                    .executeUpdate();
            rollback.rollback();
            Number rolledBackRows = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE ID = 2").getSingleResult();
            assertEquals(0L, rolledBackRows.longValue(), "rolled-back write must not be visible");
        } finally {
            inTransaction(entityManager,
                    () -> entityManager.createNativeQuery("DROP TABLE IF EXISTS " + table).executeUpdate());
        }

        service.shutdown();
        assertFalse(integration.isHealthy(), "service shutdown must close the EntityManager");
        service = null;
    }

    private static void inTransaction(EntityManager entityManager, Runnable operation) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            operation.run();
            transaction.commit();
        } catch (RuntimeException failure) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw failure;
        }
    }
}
