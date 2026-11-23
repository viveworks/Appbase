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
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link LuceneExceptionStoreConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class LuceneExceptionStoreConfigurationValidator
        implements ConstraintValidator
        <LuceneExceptionStoreConfiguration.Valid, LuceneExceptionStoreConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(LuceneExceptionStoreConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(LuceneExceptionStoreConfiguration.class);

    private static final String LUCENE_SERVICE_CONFIGURATION = "luceneServiceConfiguration";
    private static final String DISCRIMINATOR = "discriminator";
    private static final String ID_FIELD_NAME = "idFieldName";
    private static final String ID_INT_POINT_NAME = "idIntPointName";
    private static final String DISCRIMINATOR_FIELD_NAME = "discriminatorFieldName";
    private static final String MESSAGE_FIELD_NAME = "messageFieldName";
    private static final String THROWABLE_TYPE_NAME_FIELD_NAME = "throwableTypeNameFieldName";
    private static final String STACK_TRACE_HASH_FIELD_NAME = "stackTraceHashFieldName";
    private static final String STACK_TRACE_HASH_INT_POINT_NAME = "stackTraceHashIntPointName";
    private static final String THROWABLE_MESSAGE_FIELD_NAME = "throwableMessageFieldName";
    private static final String STACK_TRACE_FIELD_NAME = "stackTraceFieldName";
    private static final String SOURCE_TYPE_NAME_FIELD_NAME = "sourceTypeNameFieldName";
    private static final String THREAD_NAME_FIELD_NAME = "threadNameFieldName";
    private static final String CONTEXT_FIELD_NAME = "contextFieldName";
    private static final String TIMESTAMP_FIELD_NAME = "timestampFieldName";
    private static final String TIMESTAMP_LONG_POINT_NAME = "timestampLongPointName";

    private static final String LUCENE_SERVICE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, LUCENE_SERVICE_CONFIGURATION, ValidationService.NULL);
    private static final String DISCRIMINATOR_BLANK =
            ValidationService.concat(PREFIX, DISCRIMINATOR, ValidationService.BLANK);
    private static final String ID_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, ID_FIELD_NAME, ValidationService.BLANK);
    private static final String ID_INT_POINT_NAME_BLANK =
            ValidationService.concat(PREFIX, ID_INT_POINT_NAME, ValidationService.BLANK);
    private static final String DISCRIMINATOR_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, DISCRIMINATOR_FIELD_NAME, ValidationService.BLANK);
    private static final String MESSAGE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, MESSAGE_FIELD_NAME, ValidationService.BLANK);
    private static final String THROWABLE_TYPE_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_TYPE_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String STACK_TRACE_HASH_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, STACK_TRACE_HASH_FIELD_NAME, ValidationService.BLANK);
    private static final String STACK_TRACE_HASH_INT_POINT_NAME_BLANK =
            ValidationService.concat(PREFIX, STACK_TRACE_HASH_INT_POINT_NAME, ValidationService.BLANK);
    private static final String THROWABLE_MESSAGE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_MESSAGE_FIELD_NAME, ValidationService.BLANK);
    private static final String STACK_TRACE_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, STACK_TRACE_FIELD_NAME, ValidationService.BLANK);
    private static final String SOURCE_TYPE_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, SOURCE_TYPE_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String THREAD_NAME_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, THREAD_NAME_FIELD_NAME, ValidationService.BLANK);
    private static final String CONTEXT_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, CONTEXT_FIELD_NAME, ValidationService.BLANK);
    private static final String TIMESTAMP_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, TIMESTAMP_FIELD_NAME, ValidationService.BLANK);
    private static final String TIMESTAMP_LONG_POINT_NAME_BLANK =
            ValidationService.concat(PREFIX, TIMESTAMP_LONG_POINT_NAME, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public LuceneExceptionStoreConfigurationValidator() {
        this(null);
    }

    public LuceneExceptionStoreConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final LuceneExceptionStoreConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getDiscriminator(),
                    value.getIdFieldName(),
                    value.getIdIntPointName(),
                    value.getDiscriminatorFieldName(),
                    value.getMessageFieldName(),
                    value.getThrowableTypeNameFieldName(),
                    value.getStackTraceHashFieldName(),
                    value.getStackTraceHashIntPointName(),
                    value.getThrowableMessageFieldName(),
                    value.getStackTraceFieldName(),
                    value.getSourceTypeNameFieldName(),
                    value.getThreadNameFieldName(),
                    value.getContextFieldName(),
                    value.getTimestampFieldName(),
                    value.getTimestampLongPointName(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link LuceneExceptionStoreConfiguration} properties.
     *
     * @param enabled
     *      {@link LuceneExceptionStoreConfiguration#isEnabled()}
     * @param discriminator
     *      {@link LuceneExceptionStoreConfiguration#getDiscriminator()}
     * @param idFieldName
     *      {@link LuceneExceptionStoreConfiguration#getIdFieldName()}
     * @param idIntPointName
     *      {@link LuceneExceptionStoreConfiguration#getIdIntPointName()}
     * @param discriminatorFieldName
     *      {@link LuceneExceptionStoreConfiguration#getDiscriminatorFieldName()}
     * @param messageFieldName
     *      {@link LuceneExceptionStoreConfiguration#getMessageFieldName()}
     * @param throwableTypeNameFieldName
     *      {@link LuceneExceptionStoreConfiguration#getThrowableTypeNameFieldName()}
     * @param stackTraceHashFieldName
     *      {@link LuceneExceptionStoreConfiguration#getStackTraceHashFieldName()}
     * @param stackTraceHashIntPointName
     *      {@link LuceneExceptionStoreConfiguration#getStackTraceHashIntPointName()}
     * @param throwableMessageFieldName
     *      {@link LuceneExceptionStoreConfiguration#getThrowableMessageFieldName()}
     * @param stackTraceFieldName
     *      {@link LuceneExceptionStoreConfiguration#getStackTraceFieldName()}
     * @param sourceTypeNameFieldName
     *      {@link LuceneExceptionStoreConfiguration#getSourceTypeNameFieldName()}
     * @param threadNameFieldName
     *      {@link LuceneExceptionStoreConfiguration#getThreadNameFieldName()}
     * @param contextFieldName
     *      {@link LuceneExceptionStoreConfiguration#getContextFieldName()}
     * @param timestampFieldName
     *      {@link LuceneExceptionStoreConfiguration#getTimestampFieldName()}
     * @param timestampLongPointName
     *      {@link LuceneExceptionStoreConfiguration#getTimestampLongPointName()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final String discriminator,
                                final String idFieldName,
                                final String idIntPointName,
                                final String discriminatorFieldName,
                                final String messageFieldName,
                                final String throwableTypeNameFieldName,
                                final String stackTraceHashFieldName,
                                final String stackTraceHashIntPointName,
                                final String throwableMessageFieldName,
                                final String stackTraceFieldName,
                                final String sourceTypeNameFieldName,
                                final String threadNameFieldName,
                                final String contextFieldName,
                                final String timestampFieldName,
                                final String timestampLongPointName) throws AppbaseRuntimeException
    {
        validate(enabled,
                discriminator,
                idFieldName,
                idIntPointName,
                discriminatorFieldName,
                messageFieldName,
                throwableTypeNameFieldName,
                stackTraceHashFieldName,
                stackTraceHashIntPointName,
                throwableMessageFieldName,
                stackTraceFieldName,
                sourceTypeNameFieldName,
                threadNameFieldName,
                contextFieldName,
                timestampFieldName,
                timestampLongPointName,
                null,
                null);

    }

    /**
     * Validate {@link LuceneExceptionStoreConfiguration} properties.
     *
     * @param enabled
     *      {@link LuceneExceptionStoreConfiguration#isEnabled()}
     * @param discriminator
     *      {@link LuceneExceptionStoreConfiguration#getDiscriminator()}
     * @param idFieldName
     *      {@link LuceneExceptionStoreConfiguration#getIdFieldName()}
     * @param idIntPointName
     *      {@link LuceneExceptionStoreConfiguration#getIdIntPointName()}
     * @param discriminatorFieldName
     *      {@link LuceneExceptionStoreConfiguration#getDiscriminatorFieldName()}
     * @param messageFieldName
     *      {@link LuceneExceptionStoreConfiguration#getMessageFieldName()}
     * @param throwableTypeNameFieldName
     *      {@link LuceneExceptionStoreConfiguration#getThrowableTypeNameFieldName()}
     * @param stackTraceHashFieldName
     *      {@link LuceneExceptionStoreConfiguration#getStackTraceHashFieldName()}
     * @param stackTraceHashIntPointName
     *      {@link LuceneExceptionStoreConfiguration#getStackTraceHashIntPointName()}
     * @param throwableMessageFieldName
     *      {@link LuceneExceptionStoreConfiguration#getThrowableMessageFieldName()}
     * @param stackTraceFieldName
     *      {@link LuceneExceptionStoreConfiguration#getStackTraceFieldName()}
     * @param sourceTypeNameFieldName
     *      {@link LuceneExceptionStoreConfiguration#getSourceTypeNameFieldName()}
     * @param threadNameFieldName
     *      {@link LuceneExceptionStoreConfiguration#getThreadNameFieldName()}
     * @param contextFieldName
     *      {@link LuceneExceptionStoreConfiguration#getContextFieldName()}
     * @param timestampFieldName
     *      {@link LuceneExceptionStoreConfiguration#getTimestampFieldName()}
     * @param timestampLongPointName
     *      {@link LuceneExceptionStoreConfiguration#getTimestampLongPointName()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final String discriminator,
                                   final String idFieldName,
                                   final String idIntPointName,
                                   final String discriminatorFieldName,
                                   final String messageFieldName,
                                   final String throwableTypeNameFieldName,
                                   final String stackTraceHashFieldName,
                                   final String stackTraceHashIntPointName,
                                   final String throwableMessageFieldName,
                                   final String stackTraceFieldName,
                                   final String sourceTypeNameFieldName,
                                   final String threadNameFieldName,
                                   final String contextFieldName,
                                   final String timestampFieldName,
                                   final String timestampLongPointName,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validateBlank(
                    discriminator,
                    DISCRIMINATOR_BLANK,
                    DISCRIMINATOR,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    idFieldName,
                    ID_FIELD_NAME_BLANK,
                    ID_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    idIntPointName,
                    ID_INT_POINT_NAME_BLANK,
                    ID_INT_POINT_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    discriminatorFieldName,
                    DISCRIMINATOR_FIELD_NAME_BLANK,
                    DISCRIMINATOR_FIELD_NAME,
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
                    stackTraceHashFieldName,
                    STACK_TRACE_HASH_FIELD_NAME_BLANK,
                    STACK_TRACE_HASH_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    stackTraceHashIntPointName,
                    STACK_TRACE_HASH_INT_POINT_NAME_BLANK,
                    STACK_TRACE_HASH_INT_POINT_NAME,
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

            valid = ValidationService.validateBlank(
                    stackTraceFieldName,
                    STACK_TRACE_FIELD_NAME_BLANK,
                    STACK_TRACE_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    sourceTypeNameFieldName,
                    SOURCE_TYPE_NAME_FIELD_NAME_BLANK,
                    SOURCE_TYPE_NAME_FIELD_NAME,
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
                    contextFieldName,
                    CONTEXT_FIELD_NAME_BLANK,
                    CONTEXT_FIELD_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    timestampFieldName,
                    TIMESTAMP_FIELD_NAME_BLANK,
                    TIMESTAMP_FIELD_NAME,
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

            return valid;
        }

        return true;
    }
}
