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

package com.viveworks.appbase.core.service.tracer.configuration.propagation;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JaegerPropagationConfigurationValidatorTest {
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
        final JaegerPropagationConfigurationValidator jaegerPropagationConfigurationValidator1 =
                new JaegerPropagationConfigurationValidator();
        final JaegerPropagationConfigurationValidator jaegerPropagationConfigurationValidator2 =
                new JaegerPropagationConfigurationValidator(validationService);
        final JaegerPropagationConfiguration jaegerPropagationConfiguration =
                JaegerPropagationConfiguration.builder().build();

        Assertions.assertTrue(
                jaegerPropagationConfigurationValidator1.isValid(jaegerPropagationConfiguration, null));
        Assertions.assertTrue(
                jaegerPropagationConfigurationValidator2.isValid(jaegerPropagationConfiguration, null));

        Assertions.assertFalse(
                jaegerPropagationConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                jaegerPropagationConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                JaegerPropagationConfigurationValidator.validate(
                        jaegerPropagationConfiguration.getJaegerPropagationTypes(),
                        jaegerPropagationConfiguration.getBaggagePrefix()));
        Assertions.assertDoesNotThrow(() ->
                JaegerPropagationConfigurationValidator.validate(
                        jaegerPropagationConfiguration.getJaegerPropagationTypes(),
                        jaegerPropagationConfiguration.getBaggagePrefix(),
                        validationService,
                        null));

        Assertions.assertDoesNotThrow(() ->
                JaegerPropagationConfigurationValidator.validate(
                        Set.of(JaegerPropagationType.W3C_TRACE_CONTEXT),
                        "",
                        validationService,
                        null));
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JaegerPropagationConfigurationValidator.validate(
                            Set.of(JaegerPropagationType.B3),
                            null);
                });

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JaegerPropagationConfigurationValidator.validate(
                            null,
                            jaegerPropagationConfiguration.getBaggagePrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JaegerPropagationConfigurationValidator.validate(
                            Set.of(),
                            jaegerPropagationConfiguration.getBaggagePrefix());
                });

        final Set<JaegerPropagationType> propagationTypes = new HashSet<>();
        propagationTypes.add(JaegerPropagationType.W3C_TRACE_CONTEXT);
        propagationTypes.add(null);
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JaegerPropagationConfigurationValidator.validate(
                            propagationTypes,
                            jaegerPropagationConfiguration.getBaggagePrefix());
                });
    }
}
