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
class DefaultLuceneServiceConfigurationValidatorTest {
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
        final DefaultLuceneServiceConfigurationValidator defaultLuceneServiceConfigurationValidator1 =
                new DefaultLuceneServiceConfigurationValidator();
        final DefaultLuceneServiceConfigurationValidator defaultLuceneServiceConfigurationValidator2 =
                new DefaultLuceneServiceConfigurationValidator(validationService);
        final DefaultLuceneServiceConfiguration defaultLuceneServiceConfiguration =
                DefaultLuceneServiceConfiguration.builder().build();

        Assertions.assertTrue(
                defaultLuceneServiceConfigurationValidator1.isValid(
                        defaultLuceneServiceConfiguration, null));
        Assertions.assertTrue(
                defaultLuceneServiceConfigurationValidator2.isValid(
                        defaultLuceneServiceConfiguration, null));

        Assertions.assertFalse(
                defaultLuceneServiceConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                defaultLuceneServiceConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneServiceConfigurationValidator.validate(
                        defaultLuceneServiceConfiguration.isEnabled(),
                        defaultLuceneServiceConfiguration.getIndexBaseDirectory(),
                        defaultLuceneServiceConfiguration.getDirectoryType(),
                        defaultLuceneServiceConfiguration.getMmapDirectoryConfiguration()));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneServiceConfigurationValidator.validate(
                        defaultLuceneServiceConfiguration.isEnabled(),
                        defaultLuceneServiceConfiguration.getIndexBaseDirectory(),
                        defaultLuceneServiceConfiguration.getDirectoryType(),
                        defaultLuceneServiceConfiguration.getMmapDirectoryConfiguration(),
                        validationService,
                        null));

        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                DefaultLuceneServiceConfigurationValidator.validate(
                        defaultLuceneServiceConfiguration.isEnabled(),
                        "",
                        defaultLuceneServiceConfiguration.getDirectoryType(),
                        defaultLuceneServiceConfiguration.getMmapDirectoryConfiguration()));
        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                DefaultLuceneServiceConfigurationValidator.validate(
                        defaultLuceneServiceConfiguration.isEnabled(),
                        defaultLuceneServiceConfiguration.getIndexBaseDirectory(),
                        null,
                        defaultLuceneServiceConfiguration.getMmapDirectoryConfiguration()));
        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                DefaultLuceneServiceConfigurationValidator.validate(
                        defaultLuceneServiceConfiguration.isEnabled(),
                        defaultLuceneServiceConfiguration.getIndexBaseDirectory(),
                        DirectoryType.MMAP,
                        null));
    }
}
