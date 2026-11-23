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

package com.viveworks.appbase.core.service.metric.jmx.registry;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxMeterRegistryConfigurationValidatorTest {
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
        final JmxMeterRegistryConfigurationValidator jmxMeterRegistryConfigurationValidator1 =
                new JmxMeterRegistryConfigurationValidator();
        final JmxMeterRegistryConfigurationValidator jmxMeterRegistryConfigurationValidator2 =
                new JmxMeterRegistryConfigurationValidator(validationService);
        final JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration =
                JmxMeterRegistryConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                jmxMeterRegistryConfigurationValidator1.isValid(jmxMeterRegistryConfiguration, null));
        Assertions.assertTrue(
                jmxMeterRegistryConfigurationValidator2.isValid(jmxMeterRegistryConfiguration, null));

        Assertions.assertFalse(
                jmxMeterRegistryConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                jmxMeterRegistryConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                JmxMeterRegistryConfigurationValidator.validate(
                        jmxMeterRegistryConfiguration.isEnabled(),
                        jmxMeterRegistryConfiguration.getAdditionalProperties(),
                        jmxMeterRegistryConfiguration.prefix(),
                        jmxMeterRegistryConfiguration.domain(),
                        jmxMeterRegistryConfiguration.step()));
        Assertions.assertDoesNotThrow(() ->
                JmxMeterRegistryConfigurationValidator.validate(
                        jmxMeterRegistryConfiguration.isEnabled(),
                        jmxMeterRegistryConfiguration.getAdditionalProperties(),
                        jmxMeterRegistryConfiguration.prefix(),
                        jmxMeterRegistryConfiguration.domain(),
                        jmxMeterRegistryConfiguration.step(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JmxMeterRegistryConfigurationValidator.validate(
                            jmxMeterRegistryConfiguration.isEnabled(),
                            jmxMeterRegistryConfiguration.getAdditionalProperties(),
                            null,
                            jmxMeterRegistryConfiguration.domain(),
                            jmxMeterRegistryConfiguration.step());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JmxMeterRegistryConfigurationValidator.validate(
                            jmxMeterRegistryConfiguration.isEnabled(),
                            jmxMeterRegistryConfiguration.getAdditionalProperties(),
                            jmxMeterRegistryConfiguration.prefix(),
                            null,
                            jmxMeterRegistryConfiguration.step());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JmxMeterRegistryConfigurationValidator.validate(
                            jmxMeterRegistryConfiguration.isEnabled(),
                            jmxMeterRegistryConfiguration.getAdditionalProperties(),
                            jmxMeterRegistryConfiguration.prefix(),
                            jmxMeterRegistryConfiguration.domain(),
                            null);
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JmxMeterRegistryConfigurationValidator.validate(
                            jmxMeterRegistryConfiguration.isEnabled(),
                            jmxMeterRegistryConfiguration.getAdditionalProperties(),
                            jmxMeterRegistryConfiguration.prefix(),
                            jmxMeterRegistryConfiguration.domain(),
                            Duration.ofMillis(-1000));
                });
    }
}
