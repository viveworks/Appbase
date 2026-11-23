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

package com.viveworks.appbase.core.service.mediator.executor;

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
class ScheduledExecutorServiceConfigurationValidatorTest {
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
        final ScheduledExecutorServiceConfigurationValidator scheduledExecutorServiceConfigurationValidator1 =
                new ScheduledExecutorServiceConfigurationValidator();
        final ScheduledExecutorServiceConfigurationValidator scheduledExecutorServiceConfigurationValidator2 =
                new ScheduledExecutorServiceConfigurationValidator(validationService);

        final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration1 =
                ScheduledExecutorServiceConfiguration.builder().build();
        final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration2 =
                ScheduledExecutorServiceConfiguration
                        .builder()
                        .scheduledExecutorType(ScheduledExecutorServiceType.THREAD_POOL)
                        .build();

        Assertions.assertTrue(
                scheduledExecutorServiceConfigurationValidator1.isValid(
                        scheduledExecutorServiceConfiguration1, null));
        Assertions.assertTrue(
                scheduledExecutorServiceConfigurationValidator2.isValid(
                        scheduledExecutorServiceConfiguration2, null));

        Assertions.assertTrue(
                scheduledExecutorServiceConfigurationValidator1.isValid(
                        scheduledExecutorServiceConfiguration1, null));
        Assertions.assertTrue(
                scheduledExecutorServiceConfigurationValidator2.isValid(
                        scheduledExecutorServiceConfiguration2, null));

        Assertions.assertFalse(
                scheduledExecutorServiceConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                scheduledExecutorServiceConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                ScheduledExecutorServiceConfigurationValidator.validate(
                        scheduledExecutorServiceConfiguration1.getScheduledExecutorServiceType(),
                        scheduledExecutorServiceConfiguration1.getThreadPoolSize(),
                        scheduledExecutorServiceConfiguration1.getThreadFactoryConfiguration()));
        Assertions.assertDoesNotThrow(() ->
                ScheduledExecutorServiceConfigurationValidator.validate(
                        scheduledExecutorServiceConfiguration1.getScheduledExecutorServiceType(),
                        scheduledExecutorServiceConfiguration1.getThreadPoolSize(),
                        scheduledExecutorServiceConfiguration1.getThreadFactoryConfiguration(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {

                    ScheduledExecutorServiceConfigurationValidator.validate(
                            null,
                            scheduledExecutorServiceConfiguration1.getThreadPoolSize(),
                            scheduledExecutorServiceConfiguration1.getThreadFactoryConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {

                    ScheduledExecutorServiceConfigurationValidator.validate(
                            ScheduledExecutorServiceType.THREAD_POOL,
                            0,
                            scheduledExecutorServiceConfiguration1.getThreadFactoryConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {

                    ScheduledExecutorServiceConfigurationValidator.validate(
                            scheduledExecutorServiceConfiguration1.getScheduledExecutorServiceType(),
                            scheduledExecutorServiceConfiguration1.getThreadPoolSize(),
                            null);
                });
    }
}
