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
class SleepingWaitStrategyConfigurationValidatorTest {
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
        final SleepingWaitStrategyConfigurationValidator sleepingWaitStrategyConfigurationValidator1 =
                new SleepingWaitStrategyConfigurationValidator();
        final SleepingWaitStrategyConfigurationValidator sleepingWaitStrategyConfigurationValidator2 =
                new SleepingWaitStrategyConfigurationValidator(validationService);

        final SleepingWaitStrategyConfiguration sleepingWaitStrategyConfiguration1 =
                SleepingWaitStrategyConfiguration.builder().build();
        final SleepingWaitStrategyConfiguration sleepingWaitStrategyConfiguration2 =
                SleepingWaitStrategyConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                sleepingWaitStrategyConfigurationValidator1.isValid(
                        sleepingWaitStrategyConfiguration1, null));
        Assertions.assertTrue(
                sleepingWaitStrategyConfigurationValidator1.isValid(
                        sleepingWaitStrategyConfiguration2, null));

        Assertions.assertTrue(
                sleepingWaitStrategyConfigurationValidator2.isValid(
                        sleepingWaitStrategyConfiguration1, null));
        Assertions.assertTrue(
                sleepingWaitStrategyConfigurationValidator2.isValid(
                        sleepingWaitStrategyConfiguration2, null));


        Assertions.assertFalse(
                sleepingWaitStrategyConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                sleepingWaitStrategyConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        sleepingWaitStrategyConfiguration1.isEnabled(),
                        sleepingWaitStrategyConfiguration1.getRetries(),
                        sleepingWaitStrategyConfiguration1.getSleepTimeNanoSeconds()
                ));
        Assertions.assertDoesNotThrow(() ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        sleepingWaitStrategyConfiguration1.isEnabled(),
                        sleepingWaitStrategyConfiguration1.getRetries(),
                        sleepingWaitStrategyConfiguration1.getSleepTimeNanoSeconds(),
                        validationService,
                        null
                ));

        Assertions.assertDoesNotThrow(() ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        sleepingWaitStrategyConfiguration2.isEnabled(),
                        sleepingWaitStrategyConfiguration2.getRetries(),
                        sleepingWaitStrategyConfiguration2.getSleepTimeNanoSeconds()
                ));
        Assertions.assertDoesNotThrow(() ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        sleepingWaitStrategyConfiguration2.isEnabled(),
                        sleepingWaitStrategyConfiguration2.getRetries(),
                        sleepingWaitStrategyConfiguration2.getSleepTimeNanoSeconds(),
                        validationService,
                        null
                ));

        Assertions.assertDoesNotThrow(() ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        false,
                        -1,
                        sleepingWaitStrategyConfiguration1.getSleepTimeNanoSeconds()
                ));
        Assertions.assertDoesNotThrow(() ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        false,
                        sleepingWaitStrategyConfiguration2.getRetries(),
                        -1
                ));

        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        sleepingWaitStrategyConfiguration2.isEnabled(),
                        -1,
                        sleepingWaitStrategyConfiguration2.getSleepTimeNanoSeconds()
                ));
        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                SleepingWaitStrategyConfigurationValidator.validate(
                        sleepingWaitStrategyConfiguration2.isEnabled(),
                        sleepingWaitStrategyConfiguration2.getRetries(),
                        -1
                ));
    }
}
