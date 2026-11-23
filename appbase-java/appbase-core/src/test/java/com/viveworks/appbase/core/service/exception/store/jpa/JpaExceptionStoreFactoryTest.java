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

package com.viveworks.appbase.core.service.exception.store.jpa;

import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JpaExceptionStoreFactoryTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testNewExceptionStore() throws Exception {
        final JpaExceptionStoreFactory jpaExceptionStoreFactory = new JpaExceptionStoreFactory(
                logService,
                metricService,
                mediatorService,
                validationService,
                new DefaultExceptionMessageContextCodec());

        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        entityManagerFactoryProperties.put("jakarta.persistence.schema-generation.database.action", "create");
        entityManagerFactoryProperties.put("jakarta.persistence.validation.mode", "none");
        entityManagerFactoryProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        entityManagerFactoryProperties.put("hibernate.flushMode", "AUTO");
        entityManagerFactoryProperties.put("show_sql", "true");
        entityManagerFactoryProperties.put("format_sql", "true");
        entityManagerFactoryProperties.put("use_sql_comments", "true");
        entityManagerFactoryProperties.put(
                "jakarta.persistence.jdbc.url",
                // class-unique DB name so this class never shares a schema with another (internal work item 1073).
                "jdbc:h2:mem:TestAppbaseDBJpaExcFactory;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=KEY,VALUE");
        final EntityManagerConfiguration entityManagerConfiguration = new
                EntityManagerConfiguration("testAppbasePersistenceUnit1", entityManagerFactoryProperties, null);

        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration1 =
                JpaExceptionStoreConfiguration
                        .builder()
                        .enabled(true)
                        .entityManagerConfiguration(entityManagerConfiguration)
                        .build();
        final JpaExceptionStore jpaExceptionStore1 =
                jpaExceptionStoreFactory.newExceptionStore(jpaExceptionStoreConfiguration1);
        Assertions.assertNotNull(jpaExceptionStore1);

        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration2 =
                JpaExceptionStoreConfiguration
                        .builder()
                        .enabled(false)
                        .entityManagerConfiguration(
                                new EntityManagerConfiguration("testAppbasePersistenceUnit1", null, null))
                        .build();
        Assertions.assertThrows(ExceptionStoreFactoryException.class, () -> {
            jpaExceptionStoreFactory.newExceptionStore(jpaExceptionStoreConfiguration2);
        });
    }
}
