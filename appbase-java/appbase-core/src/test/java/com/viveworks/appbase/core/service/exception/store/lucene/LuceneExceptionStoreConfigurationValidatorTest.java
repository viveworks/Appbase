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

package com.viveworks.appbase.core.service.exception.store.lucene;

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
class LuceneExceptionStoreConfigurationValidatorTest {
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
        final LuceneExceptionStoreConfigurationValidator luceneExceptionStoreConfigurationValidator1 =
                new LuceneExceptionStoreConfigurationValidator();
        final LuceneExceptionStoreConfigurationValidator luceneExceptionStoreConfigurationValidator2 =
                new LuceneExceptionStoreConfigurationValidator(validationService);

        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration1 =
                LuceneExceptionStoreConfiguration.builder().enabled(true).build();
        final LuceneExceptionStoreConfiguration luceneExceptionStoreConfiguration2 =
                LuceneExceptionStoreConfiguration.builder().build();

        Assertions.assertTrue(
                luceneExceptionStoreConfigurationValidator1.isValid(luceneExceptionStoreConfiguration1, null));
        Assertions.assertTrue(
                luceneExceptionStoreConfigurationValidator2.isValid(luceneExceptionStoreConfiguration1, null));
        Assertions.assertTrue(
                luceneExceptionStoreConfigurationValidator1.isValid(luceneExceptionStoreConfiguration2, null));
        Assertions.assertTrue(
                luceneExceptionStoreConfigurationValidator2.isValid(luceneExceptionStoreConfiguration2, null));

        Assertions.assertFalse(
                luceneExceptionStoreConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                luceneExceptionStoreConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                LuceneExceptionStoreConfigurationValidator.validate(
                        luceneExceptionStoreConfiguration1.isEnabled(),
                        luceneExceptionStoreConfiguration1.getDiscriminator(),
                        luceneExceptionStoreConfiguration1.getIdFieldName(),
                        luceneExceptionStoreConfiguration1.getIdIntPointName(),
                        luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                        luceneExceptionStoreConfiguration1.getMessageFieldName(),
                        luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                        luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                        luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                        luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                        luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                        luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                        luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                        luceneExceptionStoreConfiguration1.getContextFieldName(),
                        luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                        luceneExceptionStoreConfiguration1.getTimestampLongPointName()
                ));
        Assertions.assertDoesNotThrow(() ->
                LuceneExceptionStoreConfigurationValidator.validate(
                        luceneExceptionStoreConfiguration1.isEnabled(),
                        luceneExceptionStoreConfiguration1.getDiscriminator(),
                        luceneExceptionStoreConfiguration1.getIdFieldName(),
                        luceneExceptionStoreConfiguration1.getIdIntPointName(),
                        luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                        luceneExceptionStoreConfiguration1.getMessageFieldName(),
                        luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                        luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                        luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                        luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                        luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                        luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                        luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                        luceneExceptionStoreConfiguration1.getContextFieldName(),
                        luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                        luceneExceptionStoreConfiguration1.getTimestampLongPointName(),
                        validationService,
                        null
                ));

        Assertions.assertDoesNotThrow(() ->
                LuceneExceptionStoreConfigurationValidator.validate(
                        luceneExceptionStoreConfiguration2.isEnabled(),
                        luceneExceptionStoreConfiguration2.getDiscriminator(),
                        luceneExceptionStoreConfiguration2.getIdFieldName(),
                        luceneExceptionStoreConfiguration2.getIdIntPointName(),
                        luceneExceptionStoreConfiguration2.getDiscriminatorFieldName(),
                        luceneExceptionStoreConfiguration2.getMessageFieldName(),
                        luceneExceptionStoreConfiguration2.getThrowableTypeNameFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceHashFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceHashIntPointName(),
                        luceneExceptionStoreConfiguration2.getThrowableMessageFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceFieldName(),
                        luceneExceptionStoreConfiguration2.getSourceTypeNameFieldName(),
                        luceneExceptionStoreConfiguration2.getThreadNameFieldName(),
                        luceneExceptionStoreConfiguration2.getContextFieldName(),
                        luceneExceptionStoreConfiguration2.getTimestampFieldName(),
                        luceneExceptionStoreConfiguration2.getTimestampLongPointName()
                ));
        Assertions.assertDoesNotThrow(() ->
                LuceneExceptionStoreConfigurationValidator.validate(
                        luceneExceptionStoreConfiguration2.isEnabled(),
                        luceneExceptionStoreConfiguration2.getDiscriminator(),
                        luceneExceptionStoreConfiguration2.getIdFieldName(),
                        luceneExceptionStoreConfiguration2.getIdIntPointName(),
                        luceneExceptionStoreConfiguration2.getDiscriminatorFieldName(),
                        luceneExceptionStoreConfiguration2.getMessageFieldName(),
                        luceneExceptionStoreConfiguration2.getThrowableTypeNameFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceHashFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceHashIntPointName(),
                        luceneExceptionStoreConfiguration2.getThrowableMessageFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceFieldName(),
                        luceneExceptionStoreConfiguration2.getSourceTypeNameFieldName(),
                        luceneExceptionStoreConfiguration2.getThreadNameFieldName(),
                        luceneExceptionStoreConfiguration2.getContextFieldName(),
                        luceneExceptionStoreConfiguration2.getTimestampFieldName(),
                        luceneExceptionStoreConfiguration2.getTimestampLongPointName(),
                        validationService,
                        null
                ));
        Assertions.assertDoesNotThrow(() ->
                LuceneExceptionStoreConfigurationValidator.validate(
                        false,
                        "",
                        luceneExceptionStoreConfiguration2.getIdFieldName(),
                        luceneExceptionStoreConfiguration2.getIdIntPointName(),
                        luceneExceptionStoreConfiguration2.getDiscriminatorFieldName(),
                        luceneExceptionStoreConfiguration2.getMessageFieldName(),
                        luceneExceptionStoreConfiguration2.getThrowableTypeNameFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceHashFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceHashIntPointName(),
                        luceneExceptionStoreConfiguration2.getThrowableMessageFieldName(),
                        luceneExceptionStoreConfiguration2.getStackTraceFieldName(),
                        luceneExceptionStoreConfiguration2.getSourceTypeNameFieldName(),
                        luceneExceptionStoreConfiguration2.getThreadNameFieldName(),
                        luceneExceptionStoreConfiguration2.getContextFieldName(),
                        luceneExceptionStoreConfiguration2.getTimestampFieldName(),
                        luceneExceptionStoreConfiguration2.getTimestampLongPointName(),
                        validationService,
                        null
                ));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            "",
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            null,
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            "",
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            "",
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getThrowableMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            "",
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            "",
                            luceneExceptionStoreConfiguration1.getTimestampLongPointName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    LuceneExceptionStoreConfigurationValidator.validate(
                            luceneExceptionStoreConfiguration1.isEnabled(),
                            luceneExceptionStoreConfiguration1.getDiscriminator(),
                            luceneExceptionStoreConfiguration1.getIdFieldName(),
                            luceneExceptionStoreConfiguration1.getIdIntPointName(),
                            luceneExceptionStoreConfiguration1.getMessageFieldName(),
                            luceneExceptionStoreConfiguration1.getDiscriminatorFieldName(),
                            luceneExceptionStoreConfiguration1.getThrowableTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceHashIntPointName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getStackTraceFieldName(),
                            luceneExceptionStoreConfiguration1.getSourceTypeNameFieldName(),
                            luceneExceptionStoreConfiguration1.getThreadNameFieldName(),
                            luceneExceptionStoreConfiguration1.getContextFieldName(),
                            luceneExceptionStoreConfiguration1.getTimestampFieldName(),
                            "");
                });
    }
}
