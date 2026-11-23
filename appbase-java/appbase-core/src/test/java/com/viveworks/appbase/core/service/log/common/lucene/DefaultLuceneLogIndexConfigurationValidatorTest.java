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

package com.viveworks.appbase.core.service.log.common.lucene;

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
class DefaultLuceneLogIndexConfigurationValidatorTest {
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
        final DefaultLuceneLogIndexConfigurationValidator defaultLuceneLogIndexConfigurationValidator1 =
                new DefaultLuceneLogIndexConfigurationValidator();
        final DefaultLuceneLogIndexConfigurationValidator defaultLuceneLogIndexConfigurationValidator2 =
                new DefaultLuceneLogIndexConfigurationValidator(validationService);
        final DefaultLuceneLogIndexConfiguration defaultLuceneLogIndexConfiguration =
                DefaultLuceneLogIndexConfiguration.builder().enabled(true).build();

        Assertions.assertTrue(
                defaultLuceneLogIndexConfigurationValidator1.isValid(defaultLuceneLogIndexConfiguration, null));
        Assertions.assertTrue(
                defaultLuceneLogIndexConfigurationValidator2.isValid(defaultLuceneLogIndexConfiguration, null));

        Assertions.assertFalse(
                defaultLuceneLogIndexConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                defaultLuceneLogIndexConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        defaultLuceneLogIndexConfiguration.isEnabled(),
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName()));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        defaultLuceneLogIndexConfiguration.isEnabled(),
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        false,
                        null,
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        true,
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        false,
                        null,
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        true,
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        false,
                        null,
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        true,
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        false,
                        null,
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        true,
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getCallerStackFieldName(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                DefaultLuceneLogIndexConfigurationValidator.validate(
                        true,
                        defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                        defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                        defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                        defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                        defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                        defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                        defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                        defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                        false,
                        null,
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            null,
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            null,
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            null,
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            null,
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            null,
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            true,
                            null,
                            defaultLuceneLogIndexConfiguration.isCallerStackIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getCallerStackFieldName());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultLuceneLogIndexConfigurationValidator.validate(
                            defaultLuceneLogIndexConfiguration.isEnabled(),
                            defaultLuceneLogIndexConfiguration.getLuceneServiceConfiguration(),
                            defaultLuceneLogIndexConfiguration.getTimestampStoredFieldName(),
                            defaultLuceneLogIndexConfiguration.getTimestampLongPointName(),
                            defaultLuceneLogIndexConfiguration.getThreadNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLoggerNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getLogLevelFieldName(),
                            defaultLuceneLogIndexConfiguration.getFormattedMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableStackTraceFieldName(),
                            defaultLuceneLogIndexConfiguration.isMarkerIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getMarkerFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseTypeNameFieldName(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseMessageFieldName(),
                            defaultLuceneLogIndexConfiguration.isThrowableCauseStackTraceIndexEnabled(),
                            defaultLuceneLogIndexConfiguration.getThrowableCauseStackTraceFieldName(),
                            true,
                            null);
                });
    }
}
