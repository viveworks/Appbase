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

package com.viveworks.appbase.core.service.exception.store.jpa;

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
class JpaExceptionStoreConfigurationValidatorTest {
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
        final JpaExceptionStoreConfigurationValidator jpaExceptionStoreConfigurationValidator1 =
                new JpaExceptionStoreConfigurationValidator();
        final JpaExceptionStoreConfigurationValidator jpaExceptionStoreConfigurationValidator2 =
                new JpaExceptionStoreConfigurationValidator(validationService);
        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration =
                JpaExceptionStoreConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                jpaExceptionStoreConfigurationValidator1.isValid(jpaExceptionStoreConfiguration, null));
        Assertions.assertTrue(
                jpaExceptionStoreConfigurationValidator2.isValid(jpaExceptionStoreConfiguration, null));

        Assertions.assertFalse(
                jpaExceptionStoreConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                jpaExceptionStoreConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                JpaExceptionStoreConfigurationValidator.validate(
                        jpaExceptionStoreConfiguration.isEnabled(),
                        jpaExceptionStoreConfiguration.getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                        jpaExceptionStoreConfiguration.getDiscriminator(),
                        jpaExceptionStoreConfiguration.getIdColumnName(),
                        jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                        jpaExceptionStoreConfiguration.getMessageColumnName(),
                        jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                        jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                        jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                        jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                        jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                        jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                        jpaExceptionStoreConfiguration.getTimestampColumnName(),
                        jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                        jpaExceptionStoreConfiguration.getEntityManagerConfiguration()));
        Assertions.assertDoesNotThrow(() ->
                JpaExceptionStoreConfigurationValidator.validate(
                        jpaExceptionStoreConfiguration.isEnabled(),
                        null,
                        jpaExceptionStoreConfiguration.getDiscriminator(),
                        jpaExceptionStoreConfiguration.getIdColumnName(),
                        jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                        jpaExceptionStoreConfiguration.getMessageColumnName(),
                        jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                        jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                        jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                        jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                        jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                        jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                        jpaExceptionStoreConfiguration.getTimestampColumnName(),
                        jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                        jpaExceptionStoreConfiguration.getEntityManagerConfiguration(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            "",
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            "",
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            "",
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            -1,
                            jpaExceptionStoreConfiguration.getEntityManagerConfiguration());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    JpaExceptionStoreConfigurationValidator.validate(
                            jpaExceptionStoreConfiguration.isEnabled(),
                            jpaExceptionStoreConfiguration
                                    .getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                            jpaExceptionStoreConfiguration.getDiscriminator(),
                            jpaExceptionStoreConfiguration.getIdColumnName(),
                            jpaExceptionStoreConfiguration.getDiscriminatorColumnName(),
                            jpaExceptionStoreConfiguration.getMessageColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceHashColumnName(),
                            jpaExceptionStoreConfiguration.getThrowableMessageColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceColumnName(),
                            jpaExceptionStoreConfiguration.getSourceTypeNameColumnName(),
                            jpaExceptionStoreConfiguration.getThreadNameColumnName(),
                            jpaExceptionStoreConfiguration.getTimestampColumnName(),
                            jpaExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                            null);
                });
    }
}
