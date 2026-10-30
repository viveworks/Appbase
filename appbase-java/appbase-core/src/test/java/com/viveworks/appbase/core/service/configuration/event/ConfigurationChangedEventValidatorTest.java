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

package com.viveworks.appbase.core.service.configuration.event;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
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
class ConfigurationChangedEventValidatorTest {
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
        final ConfigurationChangedEventValidator configurationChangedEventValidator1 =
                new ConfigurationChangedEventValidator();
        final ConfigurationChangedEventValidator configurationChangedEventValidator2 =
                new ConfigurationChangedEventValidator(validationService);
        final ConfigurationChangedEvent configurationChangedEvent = new ConfigurationChangedEvent(
                ConfigurationKey.fromPropertyKey("key"),
                ConfigurationValue.fromValue("oldValue"),
                ConfigurationValue.fromValue("newValue"),
                System.currentTimeMillis());

        Assertions.assertTrue(
                configurationChangedEventValidator1.isValid(configurationChangedEvent, null));
        Assertions.assertTrue(
                configurationChangedEventValidator2.isValid(configurationChangedEvent, null));

        Assertions.assertFalse(
                configurationChangedEventValidator1.isValid(null, null));
        Assertions.assertFalse(
                configurationChangedEventValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                ConfigurationChangedEventValidator.validate(
                        configurationChangedEvent.getConfigurationKey(),
                        configurationChangedEvent.getOldConfigurationValue(),
                        configurationChangedEvent.getNewConfigurationValue(),
                        configurationChangedEvent.getTimestamp()));
        Assertions.assertDoesNotThrow(() ->
                ConfigurationChangedEventValidator.validate(
                        configurationChangedEvent.getConfigurationKey(),
                        configurationChangedEvent.getOldConfigurationValue(),
                        configurationChangedEvent.getNewConfigurationValue(),
                        configurationChangedEvent.getTimestamp(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ConfigurationChangedEventValidator.validate(
                            null,
                            configurationChangedEvent.getOldConfigurationValue(),
                            configurationChangedEvent.getNewConfigurationValue(),
                            configurationChangedEvent.getTimestamp());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ConfigurationChangedEventValidator.validate(
                            configurationChangedEvent.getConfigurationKey(),
                            null,
                            configurationChangedEvent.getNewConfigurationValue(),
                            configurationChangedEvent.getTimestamp());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ConfigurationChangedEventValidator.validate(
                            configurationChangedEvent.getConfigurationKey(),
                            configurationChangedEvent.getOldConfigurationValue(),
                            null,
                            configurationChangedEvent.getTimestamp());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ConfigurationChangedEventValidator.validate(
                            configurationChangedEvent.getConfigurationKey(),
                            configurationChangedEvent.getOldConfigurationValue(),
                            configurationChangedEvent.getNewConfigurationValue(),
                            0);
                });
    }
}
