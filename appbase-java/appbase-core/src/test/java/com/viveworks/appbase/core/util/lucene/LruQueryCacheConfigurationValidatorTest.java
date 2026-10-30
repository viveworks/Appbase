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
class LruQueryCacheConfigurationValidatorTest {
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
        final LruQueryCacheConfigurationValidator lruQueryCacheConfigurationValidator1 =
                new LruQueryCacheConfigurationValidator();
        final LruQueryCacheConfigurationValidator lruQueryCacheConfigurationValidator2 =
                new LruQueryCacheConfigurationValidator(validationService);

        final LruQueryCacheConfiguration lruQueryCacheConfiguration1 =
                LruQueryCacheConfiguration.builder().build();
        final LruQueryCacheConfiguration lruQueryCacheConfiguration2 =
                LruQueryCacheConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                lruQueryCacheConfigurationValidator1.isValid(
                        lruQueryCacheConfiguration1, null));
        Assertions.assertTrue(
                lruQueryCacheConfigurationValidator1.isValid(
                        lruQueryCacheConfiguration2, null));

        Assertions.assertTrue(
                lruQueryCacheConfigurationValidator2.isValid(
                        lruQueryCacheConfiguration1, null));
        Assertions.assertTrue(
                lruQueryCacheConfigurationValidator2.isValid(
                        lruQueryCacheConfiguration2, null));

        Assertions.assertFalse(
                lruQueryCacheConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                lruQueryCacheConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                LruQueryCacheConfigurationValidator.validate(
                        lruQueryCacheConfiguration1.isEnabled(),
                        lruQueryCacheConfiguration1.getMaxQueries(),
                        lruQueryCacheConfiguration1.getMaxRamBytesUsed()
                ));
        Assertions.assertDoesNotThrow(() ->
                LruQueryCacheConfigurationValidator.validate(
                        lruQueryCacheConfiguration1.isEnabled(),
                        lruQueryCacheConfiguration1.getMaxQueries(),
                        lruQueryCacheConfiguration1.getMaxRamBytesUsed(),
                        validationService,
                        null
                ));
        Assertions.assertDoesNotThrow(() ->
                LruQueryCacheConfigurationValidator.validate(
                        lruQueryCacheConfiguration2.isEnabled(),
                        lruQueryCacheConfiguration2.getMaxQueries(),
                        lruQueryCacheConfiguration2.getMaxRamBytesUsed()
                ));
        Assertions.assertDoesNotThrow(() ->
                LruQueryCacheConfigurationValidator.validate(
                        lruQueryCacheConfiguration2.isEnabled(),
                        lruQueryCacheConfiguration2.getMaxQueries(),
                        lruQueryCacheConfiguration2.getMaxRamBytesUsed(),
                        validationService,
                        null
                ));
        Assertions.assertDoesNotThrow(() ->
                LruQueryCacheConfigurationValidator.validate(
                        false,
                        0,
                        lruQueryCacheConfiguration2.getMaxRamBytesUsed()
                ));
        Assertions.assertDoesNotThrow(() ->
                LruQueryCacheConfigurationValidator.validate(
                        false,
                        lruQueryCacheConfiguration2.getMaxQueries(),
                        0,
                        validationService,
                        null
                ));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LruQueryCacheConfigurationValidator.validate(
                            lruQueryCacheConfiguration1.isEnabled(),
                            0,
                            lruQueryCacheConfiguration1.getMaxRamBytesUsed()
                    );
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LruQueryCacheConfigurationValidator.validate(
                            lruQueryCacheConfiguration1.isEnabled(),
                            lruQueryCacheConfiguration1.getMaxQueries(),
                            0
                    );
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LruQueryCacheConfigurationValidator.validate(
                            lruQueryCacheConfiguration2.isEnabled(),
                            0,
                            lruQueryCacheConfiguration2.getMaxRamBytesUsed()
                    );
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LruQueryCacheConfigurationValidator.validate(
                            lruQueryCacheConfiguration2.isEnabled(),
                            lruQueryCacheConfiguration2.getMaxQueries(),
                            0
                    );
                });
    }
}
