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

package com.viveworks.appbase.core.util.lucene;

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
class TimestampBasedIndexCleanerConfigurationValidatorTest {
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
        final TimestampBasedIndexCleanerConfigurationValidator timestampBasedIndexCleanerConfigurationValidator1 =
                new TimestampBasedIndexCleanerConfigurationValidator();
        final TimestampBasedIndexCleanerConfigurationValidator timestampBasedIndexCleanerConfigurationValidator2 =
                new TimestampBasedIndexCleanerConfigurationValidator(validationService);

        final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration1 =
                TimestampBasedIndexCleanerConfiguration.builder().build();
        final TimestampBasedIndexCleanerConfiguration timestampBasedIndexCleanerConfiguration2 =
                TimestampBasedIndexCleanerConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                timestampBasedIndexCleanerConfigurationValidator1.isValid(
                        timestampBasedIndexCleanerConfiguration1, null));
        Assertions.assertTrue(
                timestampBasedIndexCleanerConfigurationValidator1.isValid(
                        timestampBasedIndexCleanerConfiguration2, null));

        Assertions.assertTrue(
                timestampBasedIndexCleanerConfigurationValidator2.isValid(
                        timestampBasedIndexCleanerConfiguration1, null));
        Assertions.assertTrue(
                timestampBasedIndexCleanerConfigurationValidator2.isValid(
                        timestampBasedIndexCleanerConfiguration2, null));

        Assertions.assertFalse(
                timestampBasedIndexCleanerConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                timestampBasedIndexCleanerConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration1.isEnabled(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriod(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriodUnit(),
                        timestampBasedIndexCleanerConfiguration1.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration1.getSchedulableConfiguration()
                ));
        Assertions.assertDoesNotThrow(() ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration1.isEnabled(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriod(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriodUnit(),
                        timestampBasedIndexCleanerConfiguration1.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration1.getSchedulableConfiguration(),
                        validationService,
                        null
                ));
        Assertions.assertDoesNotThrow(() ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration2.isEnabled(),
                        timestampBasedIndexCleanerConfiguration2.getRemoveBeforeTimePeriod(),
                        timestampBasedIndexCleanerConfiguration2.getRemoveBeforeTimePeriodUnit(),
                        timestampBasedIndexCleanerConfiguration2.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration2.getSchedulableConfiguration()
                ));
        Assertions.assertDoesNotThrow(() ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration2.isEnabled(),
                        timestampBasedIndexCleanerConfiguration2.getRemoveBeforeTimePeriod(),
                        timestampBasedIndexCleanerConfiguration2.getRemoveBeforeTimePeriodUnit(),
                        timestampBasedIndexCleanerConfiguration2.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration2.getSchedulableConfiguration(),
                        validationService,
                        null
                ));
        Assertions.assertDoesNotThrow(() ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        false,
                        timestampBasedIndexCleanerConfiguration2.getRemoveBeforeTimePeriod(),
                        null,
                        timestampBasedIndexCleanerConfiguration2.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration2.getSchedulableConfiguration(),
                        validationService,
                        null
                ));

        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration1.isEnabled(),
                        0,
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriodUnit(),
                        timestampBasedIndexCleanerConfiguration1.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration1.getSchedulableConfiguration()
                ));
        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration1.isEnabled(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriod(),
                        null,
                        timestampBasedIndexCleanerConfiguration1.getCreationTimestampFieldName(),
                        timestampBasedIndexCleanerConfiguration1.getSchedulableConfiguration()
                ));
        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration1.isEnabled(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriod(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriodUnit(),
                        "",
                        timestampBasedIndexCleanerConfiguration1.getSchedulableConfiguration()
                ));
        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                TimestampBasedIndexCleanerConfigurationValidator.validate(
                        timestampBasedIndexCleanerConfiguration1.isEnabled(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriod(),
                        timestampBasedIndexCleanerConfiguration1.getRemoveBeforeTimePeriodUnit(),
                        timestampBasedIndexCleanerConfiguration1.getCreationTimestampFieldName(),
                        null
                ));
    }
}
