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

package com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy;

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
 * @since 1.0.0
 */
class PhasedBackoffWaitStrategyConfigurationValidatorTest {
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
        final PhasedBackoffWaitStrategyConfigurationValidator phasedBackoffWaitStrategyConfigurationValidator1 =
                new PhasedBackoffWaitStrategyConfigurationValidator();
        final PhasedBackoffWaitStrategyConfigurationValidator phasedBackoffWaitStrategyConfigurationValidator2 =
                new PhasedBackoffWaitStrategyConfigurationValidator(validationService);

        final PhasedBackoffWaitStrategyConfiguration phasedBackoffWaitStrategyConfiguration1 =
                PhasedBackoffWaitStrategyConfiguration.builder().build();
        final PhasedBackoffWaitStrategyConfiguration phasedBackoffWaitStrategyConfiguration2 =
                PhasedBackoffWaitStrategyConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                phasedBackoffWaitStrategyConfigurationValidator1.isValid(
                        phasedBackoffWaitStrategyConfiguration1, null));
        Assertions.assertTrue(
                phasedBackoffWaitStrategyConfigurationValidator1.isValid(
                        phasedBackoffWaitStrategyConfiguration2, null));

        Assertions.assertTrue(
                phasedBackoffWaitStrategyConfigurationValidator2.isValid(
                        phasedBackoffWaitStrategyConfiguration1, null));
        Assertions.assertTrue(
                phasedBackoffWaitStrategyConfigurationValidator2.isValid(
                        phasedBackoffWaitStrategyConfiguration2, null));

        Assertions.assertFalse(
                phasedBackoffWaitStrategyConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                phasedBackoffWaitStrategyConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                PhasedBackoffWaitStrategyConfigurationValidator.validate(
                        phasedBackoffWaitStrategyConfiguration1.isEnabled(),
                        phasedBackoffWaitStrategyConfiguration1.getSpinTimeout(),
                        phasedBackoffWaitStrategyConfiguration1.getYieldTimeout(),
                        phasedBackoffWaitStrategyConfiguration1.getUnits(),
                        phasedBackoffWaitStrategyConfiguration1.getWaitStrategyConfiguration()));
        Assertions.assertDoesNotThrow(() ->
                PhasedBackoffWaitStrategyConfigurationValidator.validate(
                        phasedBackoffWaitStrategyConfiguration1.isEnabled(),
                        phasedBackoffWaitStrategyConfiguration1.getSpinTimeout(),
                        phasedBackoffWaitStrategyConfiguration1.getYieldTimeout(),
                        phasedBackoffWaitStrategyConfiguration1.getUnits(),
                        phasedBackoffWaitStrategyConfiguration1.getWaitStrategyConfiguration(),
                        validationService,
                        null));

        Assertions.assertDoesNotThrow(() ->
                PhasedBackoffWaitStrategyConfigurationValidator.validate(
                        phasedBackoffWaitStrategyConfiguration2.isEnabled(),
                        phasedBackoffWaitStrategyConfiguration2.getSpinTimeout(),
                        phasedBackoffWaitStrategyConfiguration2.getYieldTimeout(),
                        phasedBackoffWaitStrategyConfiguration2.getUnits(),
                        phasedBackoffWaitStrategyConfiguration2.getWaitStrategyConfiguration()));
        Assertions.assertDoesNotThrow(() ->
                PhasedBackoffWaitStrategyConfigurationValidator.validate(
                        phasedBackoffWaitStrategyConfiguration2.isEnabled(),
                        phasedBackoffWaitStrategyConfiguration2.getSpinTimeout(),
                        phasedBackoffWaitStrategyConfiguration2.getYieldTimeout(),
                        phasedBackoffWaitStrategyConfiguration2.getUnits(),
                        phasedBackoffWaitStrategyConfiguration2.getWaitStrategyConfiguration(),
                        validationService,
                        null));

        Assertions.assertDoesNotThrow(() ->
                PhasedBackoffWaitStrategyConfigurationValidator.validate(
                        false,
                        -1,
                        phasedBackoffWaitStrategyConfiguration2.getYieldTimeout(),
                        phasedBackoffWaitStrategyConfiguration2.getUnits(),
                        phasedBackoffWaitStrategyConfiguration2.getWaitStrategyConfiguration()));
        Assertions.assertDoesNotThrow(() ->
                PhasedBackoffWaitStrategyConfigurationValidator.validate(
                        false,
                        phasedBackoffWaitStrategyConfiguration2.getSpinTimeout(),
                        -1,
                        phasedBackoffWaitStrategyConfiguration2.getUnits(),
                        phasedBackoffWaitStrategyConfiguration2.getWaitStrategyConfiguration(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    PhasedBackoffWaitStrategyConfigurationValidator.validate(
                            phasedBackoffWaitStrategyConfiguration1.isEnabled(),
                            -1,
                            phasedBackoffWaitStrategyConfiguration1.getYieldTimeout(),
                            phasedBackoffWaitStrategyConfiguration1.getUnits(),
                            phasedBackoffWaitStrategyConfiguration1.getWaitStrategyConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    PhasedBackoffWaitStrategyConfigurationValidator.validate(
                            phasedBackoffWaitStrategyConfiguration1.isEnabled(),
                            phasedBackoffWaitStrategyConfiguration1.getSpinTimeout(),
                            -1,
                            phasedBackoffWaitStrategyConfiguration1.getUnits(),
                            phasedBackoffWaitStrategyConfiguration1.getWaitStrategyConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    PhasedBackoffWaitStrategyConfigurationValidator.validate(
                            phasedBackoffWaitStrategyConfiguration1.isEnabled(),
                            phasedBackoffWaitStrategyConfiguration1.getSpinTimeout(),
                            phasedBackoffWaitStrategyConfiguration1.getYieldTimeout(),
                            null,
                            phasedBackoffWaitStrategyConfiguration1.getWaitStrategyConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    PhasedBackoffWaitStrategyConfigurationValidator.validate(
                            phasedBackoffWaitStrategyConfiguration1.isEnabled(),
                            phasedBackoffWaitStrategyConfiguration1.getSpinTimeout(),
                            phasedBackoffWaitStrategyConfiguration1.getYieldTimeout(),
                            phasedBackoffWaitStrategyConfiguration1.getUnits(),
                            null);
                });
    }
}
