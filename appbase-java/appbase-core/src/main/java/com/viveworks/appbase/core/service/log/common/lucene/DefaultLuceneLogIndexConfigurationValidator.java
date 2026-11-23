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
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.lucene.LuceneServiceConfiguration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultLuceneLogIndexConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLuceneLogIndexConfigurationValidator
        implements ConstraintValidator
        <DefaultLuceneLogIndexConfiguration.Valid, DefaultLuceneLogIndexConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultLuceneLogIndexConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultLuceneLogIndexConfiguration.class);

    private static final String LUCENE_SERVICE_CONFIGURATION = "luceneServiceConfiguration";
    private static final String TIMESTAMP_STORED_FIELD_NAME = "timestampStoredFieldName";
    private static final String TIMESTAMP_LONG_POINT_NAME = "timestampLongPointName";
    private static final String THREAD_NAME_FIELD_NAME = "threadNameFieldName";
    private static final String LOGGER_NAME_FIELD_NAME = "loggerNameFieldName";
    private static final String LOG_LEVEL_FIELD_NAME = "logLevelFieldName";
    private static final String FORMATTED_MESSAGE_FIELD_NAME = "formattedMessageFieldName";
    private static final String MESSAGE_FIELD_NAME = "messageFieldName";
    private static final String THROWABLE_TYPE_NAME_FIELD_NAME = "throwableTypeNameFieldName";
    private static final String THROWABLE_MESSAGE_FIELD_NAME = "throwableMessageFieldName";
    private static final String THROWABLE_STACK_TRACE_FIELD_NAME = "throwableStackTraceFieldName";
    private static final String MARKER_FIELD_NAME = "markerFieldName";
    private static final String THROWABLE_CAUSE_TYPE_NAME_FIELD_NAME = "throwableCauseTypeNameFieldName";
    private static final String THROWABLE_CAUSE_MESSAGE_FIELD_NAME = "throwableCauseMessageFieldName";
    private static final String THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME = "throwableCauseStackTraceFieldName";
    private static final String CALLER_STACK_FIELD_NAME = "callerStackFieldName";

    private static final String LUCENE_SERVICE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, LUCENE_SERVICE_CONFIGURATION, ValidationService.NULL);
    private static final String TIMESTAMP_STORED_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, TIMESTAMP_STORED_FIELD_NAME, ValidationService.BLANK);
    private static final String TIMESTAMP_LONG_POINT_NAME_BLANK =
            ValidationService.concat(PREFIX, TIMESTAMP_LONG_POINT_NAME, ValidationService.BLANK);
    private static final String THREAD_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THREAD_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String LOGGER_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, LOGGER_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String LOG_LEVEL_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, LOG_LEVEL_FIELD_NAME, ValidationService.BLANK);
    private static final String FORMATTED_MESSAGE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, FORMATTED_MESSAGE_FIELD_NAME, ValidationService.BLANK);
    private static final String MESSAGE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, MESSAGE_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_TYPE_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_TYPE_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_MESSAGE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_MESSAGE_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_STACK_TRACE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_STACK_TRACE_FIELD_NAME, ValidationService.BLANK);
    private static final String MARKER_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, MARKER_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_CAUSE_TYPE_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_CAUSE_TYPE_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_CAUSE_MESSAGE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_CAUSE_MESSAGE_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME, ValidationService.BLANK);
    private static final String CALLER_STACK_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, CALLER_STACK_FIELD_NAME, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public DefaultLuceneLogIndexConfigurationValidator() {
        this(null);
    }

    public DefaultLuceneLogIndexConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultLuceneLogIndexConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getLuceneServiceConfiguration(),
                    value.getTimestampStoredFieldName(),
                    value.getTimestampLongPointName(),
                    value.getThreadNameFieldName(),
                    value.getLoggerNameFieldName(),
                    value.getLogLevelFieldName(),
                    value.getFormattedMessageFieldName(),
                    value.getMessageFieldName(),
                    value.getThrowableTypeNameFieldName(),
                    value.getThrowableMessageFieldName(),
                    value.isThrowableStackTraceIndexEnabled(),
                    value.getThrowableStackTraceFieldName(),
                    value.isMarkerIndexEnabled(),
                    value.getMarkerFieldName(),
                    value.getThrowableCauseTypeNameFieldName(),
                    value.getThrowableCauseMessageFieldName(),
                    value.isThrowableCauseStackTraceIndexEnabled(),
                    value.getThrowableCauseStackTraceFieldName(),
                    value.isCallerStackIndexEnabled(),
                    value.getCallerStackFieldName(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultLuceneLogIndexConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultLuceneLogIndexConfiguration#isEnabled()}
     * @param luceneServiceConfiguration
     *      {@link DefaultLuceneLogIndexConfiguration#getLuceneServiceConfiguration()}
     * @param timestampStoredFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getTimestampStoredFieldName()}
     * @param timestampLongPointName
     *      {@link DefaultLuceneLogIndexConfiguration#getTimestampLongPointName()}
     * @param threadNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThreadNameFieldName()}
     * @param loggerNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getLoggerNameFieldName()}
     * @param logLevelFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getLogLevelFieldName()}
     * @param formattedMessageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getFormattedMessageFieldName()}
     * @param messageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getMessageFieldName()}
     * @param throwableTypeNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableTypeNameFieldName()}
     * @param throwableMessageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableMessageFieldName()}
     * @param throwableStackTraceIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isThrowableStackTraceIndexEnabled()}
     * @param throwableStackTraceFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableStackTraceFieldName()}
     * @param markerIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isMarkerIndexEnabled()}
     * @param markerFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getMarkerFieldName()}
     * @param throwableCauseTypeNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableCauseTypeNameFieldName()}
     * @param throwableCauseMessageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableCauseMessageFieldName()}
     * @param throwableCauseStackTraceIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isThrowableCauseStackTraceIndexEnabled()}
     * @param throwableCauseStackTraceFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableCauseStackTraceFieldName()}
     * @param callerStackIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isCallerStackIndexEnabled()}
     * @param callerStackFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getCallerStackFieldName()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final LuceneServiceConfiguration luceneServiceConfiguration,
                                final String timestampStoredFieldName,
                                final String timestampLongPointName,
                                final String threadNameFieldName,
                                final String loggerNameFieldName,
                                final String logLevelFieldName,
                                final String formattedMessageFieldName,
                                final String messageFieldName,
                                final String throwableTypeNameFieldName,
                                final String throwableMessageFieldName,
                                final boolean throwableStackTraceIndexEnabled,
                                final String throwableStackTraceFieldName,
                                final boolean markerIndexEnabled,
                                final String markerFieldName,
                                final String throwableCauseTypeNameFieldName,
                                final String throwableCauseMessageFieldName,
                                final boolean throwableCauseStackTraceIndexEnabled,
                                final String throwableCauseStackTraceFieldName,
                                final boolean callerStackIndexEnabled,
                                final String callerStackFieldName) throws AppbaseRuntimeException
    {
        validate(enabled,
                luceneServiceConfiguration,
                timestampStoredFieldName,
                timestampLongPointName,
                threadNameFieldName,
                loggerNameFieldName,
                logLevelFieldName,
                formattedMessageFieldName,
                messageFieldName,
                throwableTypeNameFieldName,
                throwableMessageFieldName,
                throwableStackTraceIndexEnabled,
                throwableStackTraceFieldName,
                markerIndexEnabled,
                markerFieldName,
                throwableCauseTypeNameFieldName,
                throwableCauseMessageFieldName,
                throwableCauseStackTraceIndexEnabled,
                throwableCauseStackTraceFieldName,
                callerStackIndexEnabled,
                callerStackFieldName,
                null,
                null);
    }

    /**
     * Validate {@link DefaultLuceneLogIndexConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultLuceneLogIndexConfiguration#isEnabled()}
     * @param luceneServiceConfiguration
     *      {@link DefaultLuceneLogIndexConfiguration#getLuceneServiceConfiguration()}
     * @param timestampStoredFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getTimestampStoredFieldName()}
     * @param timestampLongPointName
     *      {@link DefaultLuceneLogIndexConfiguration#getTimestampLongPointName()}
     * @param threadNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThreadNameFieldName()}
     * @param loggerNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getLoggerNameFieldName()}
     * @param logLevelFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getLogLevelFieldName()}
     * @param formattedMessageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getFormattedMessageFieldName()}
     * @param messageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getMessageFieldName()}
     * @param throwableTypeNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableTypeNameFieldName()}
     * @param throwableMessageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableMessageFieldName()}
     * @param throwableStackTraceIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isThrowableStackTraceIndexEnabled()}
     * @param throwableStackTraceFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableStackTraceFieldName()}
     * @param markerIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isMarkerIndexEnabled()}
     * @param markerFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getMarkerFieldName()}
     * @param throwableCauseTypeNameFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableCauseTypeNameFieldName()}
     * @param throwableCauseMessageFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableCauseMessageFieldName()}
     * @param throwableCauseStackTraceIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isThrowableCauseStackTraceIndexEnabled()}
     * @param throwableCauseStackTraceFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getThrowableCauseStackTraceFieldName()}
     * @param callerStackIndexEnabled
     *      {@link DefaultLuceneLogIndexConfiguration#isCallerStackIndexEnabled()}
     * @param callerStackFieldName
     *      {@link DefaultLuceneLogIndexConfiguration#getCallerStackFieldName()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final LuceneServiceConfiguration luceneServiceConfiguration,
                                   final String timestampStoredFieldName,
                                   final String timestampLongPointName,
                                   final String threadNameFieldName,
                                   final String loggerNameFieldName,
                                   final String logLevelFieldName,
                                   final String formattedMessageFieldName,
                                   final String messageFieldName,
                                   final String throwableTypeNameFieldName,
                                   final String throwableMessageFieldName,
                                   final boolean throwableStackTraceIndexEnabled,
                                   final String throwableStackTraceFieldName,
                                   final boolean markerIndexEnabled,
                                   final String markerFieldName,
                                   final String throwableCauseTypeNameFieldName,
                                   final String throwableCauseMessageFieldName,
                                   final boolean throwableCauseStackTraceIndexEnabled,
                                   final String throwableCauseStackTraceFieldName,
                                   final boolean callerStackIndexEnabled,
                                   final String callerStackFieldName,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validateNull(
                    luceneServiceConfiguration,
                    LUCENE_SERVICE_CONFIGURATION_NULL,
                    LUCENE_SERVICE_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    timestampStoredFieldName,
                    TIMESTAMP_STORED_FIELD_NAME_BLANK,
                    TIMESTAMP_STORED_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    timestampLongPointName,
                    TIMESTAMP_LONG_POINT_NAME_BLANK,
                    TIMESTAMP_LONG_POINT_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    threadNameFieldName,
                    THREAD_NAME_FIELD_NAME_BLANK,
                    THREAD_NAME_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    loggerNameFieldName,
                    LOGGER_NAME_FIELD_NAME_BLANK,
                    LOGGER_NAME_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    logLevelFieldName,
                    LOG_LEVEL_FIELD_NAME_BLANK,
                    LOG_LEVEL_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    formattedMessageFieldName,
                    FORMATTED_MESSAGE_FIELD_NAME_BLANK,
                    FORMATTED_MESSAGE_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    messageFieldName,
                    MESSAGE_FIELD_NAME_BLANK,
                    MESSAGE_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    throwableTypeNameFieldName,
                    THROWABLE_TYPE_NAME_FIELD_NAME_BLANK,
                    THROWABLE_TYPE_NAME_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    throwableMessageFieldName,
                    THROWABLE_MESSAGE_FIELD_NAME_BLANK,
                    THROWABLE_MESSAGE_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            if (throwableStackTraceIndexEnabled) {
                valid = ValidationService.validateBlank(
                        throwableStackTraceFieldName,
                        THROWABLE_STACK_TRACE_FIELD_NAME_BLANK,
                        THROWABLE_STACK_TRACE_FIELD_NAME,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
                if (!valid) {
                    return valid;
                }
            }

            if (markerIndexEnabled) {
                valid = ValidationService.validateBlank(
                        markerFieldName,
                        MARKER_FIELD_NAME_BLANK,
                        MARKER_FIELD_NAME,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
                if (!valid) {
                    return valid;
                }
            }

            valid = ValidationService.validateBlank(
                    throwableCauseTypeNameFieldName,
                    THROWABLE_CAUSE_TYPE_NAME_FIELD_NAME_BLANK,
                    THROWABLE_CAUSE_TYPE_NAME_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    throwableCauseMessageFieldName,
                    THROWABLE_CAUSE_MESSAGE_FIELD_NAME_BLANK,
                    THROWABLE_CAUSE_MESSAGE_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            if (throwableCauseStackTraceIndexEnabled) {
                valid = ValidationService.validateBlank(
                        throwableCauseStackTraceFieldName,
                        THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME_BLANK,
                        THROWABLE_CAUSE_STACK_TRACE_FIELD_NAME,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
                if (!valid) {
                    return valid;
                }
            }

            if (callerStackIndexEnabled) {
                valid = ValidationService.validateBlank(
                        callerStackFieldName,
                        CALLER_STACK_FIELD_NAME_BLANK,
                        CALLER_STACK_FIELD_NAME,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
                if (!valid) {
                    return valid;
                }
            }
        }

        return true;
    }
}
