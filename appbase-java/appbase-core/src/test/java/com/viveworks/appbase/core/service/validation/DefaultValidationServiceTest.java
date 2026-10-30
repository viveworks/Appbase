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

package com.viveworks.appbase.core.service.validation;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultValidationServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testDefaultConstructor() throws Exception {
        Assertions.assertNotNull(validationService.getConfiguration());
        Assertions.assertNotNull(validationService.getValidator());

        validationService.reload(new DefaultValidationServiceConfiguration());
        Assertions.assertNotNull(validationService.getConfiguration());
        Assertions.assertNotNull(validationService.getValidator());

        Assertions.assertEquals(ServiceStatus.STARTED, validationService.getStatus());
        validationService.shutdown();
        Assertions.assertNotNull(validationService.getConfiguration());
        Assertions.assertNotNull(validationService.getValidator());

        final ServiceStatus status = validationService.getStatus();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, status);
    }

    @Test
    void testCustomValidatorConstructor() throws Exception {
        final DefaultValidationService customConstructorValidationService = new DefaultValidationService(
                logService,
                metricService,
                Validation.buildDefaultValidatorFactory().getValidator());
        Assertions.assertNotNull(customConstructorValidationService.getConfiguration());
        Assertions.assertNotNull(customConstructorValidationService.getValidator());

        customConstructorValidationService.reload(new DefaultValidationServiceConfiguration());
        Assertions.assertNotNull(customConstructorValidationService.getConfiguration());
        Assertions.assertNotNull(customConstructorValidationService.getValidator());

        customConstructorValidationService.shutdown();
        Assertions.assertNotNull(customConstructorValidationService.getConfiguration());
        Assertions.assertNotNull(customConstructorValidationService.getValidator());
    }
}
