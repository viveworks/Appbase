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

package com.viveworks.appbase.core.service.reactive.read;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveReadBoundaryConfigurationValidatorTest {
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
    void testValidator() {
        final ReactiveReadBoundaryConfigurationValidator reactiveReadBoundaryConfigurationValidator1 =
                new ReactiveReadBoundaryConfigurationValidator();
        final ReactiveReadBoundaryConfigurationValidator reactiveReadBoundaryConfigurationValidator2 =
                new ReactiveReadBoundaryConfigurationValidator(validationService);
        final ReactiveReadBoundaryConfiguration reactiveReadBoundaryConfiguration =
                ReactiveReadBoundaryConfiguration.builder().build();

        Assertions.assertTrue(
                reactiveReadBoundaryConfigurationValidator1.isValid(reactiveReadBoundaryConfiguration, null));
        Assertions.assertTrue(
                reactiveReadBoundaryConfigurationValidator2.isValid(reactiveReadBoundaryConfiguration, null));

        Assertions.assertFalse(
                reactiveReadBoundaryConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                reactiveReadBoundaryConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                ReactiveReadBoundaryConfigurationValidator.validate(
                        reactiveReadBoundaryConfiguration.getMaxPageSize(),
                        reactiveReadBoundaryConfiguration.getOffloadTimeoutSeconds()));
        Assertions.assertDoesNotThrow(() ->
                ReactiveReadBoundaryConfigurationValidator.validate(
                        reactiveReadBoundaryConfiguration.getMaxPageSize(),
                        reactiveReadBoundaryConfiguration.getOffloadTimeoutSeconds(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ReactiveReadBoundaryConfigurationValidator.validate(
                            0,
                            reactiveReadBoundaryConfiguration.getOffloadTimeoutSeconds());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ReactiveReadBoundaryConfigurationValidator.validate(
                            reactiveReadBoundaryConfiguration.getMaxPageSize(),
                            0);
                });
    }
}
