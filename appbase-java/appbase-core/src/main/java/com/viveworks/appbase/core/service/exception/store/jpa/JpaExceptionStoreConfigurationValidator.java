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
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link JpaExceptionStoreConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JpaExceptionStoreConfigurationValidator
        implements ConstraintValidator<JpaExceptionStoreConfiguration.Valid, JpaExceptionStoreConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(JpaExceptionStoreConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(JpaExceptionStoreConfiguration.class);

    private static final String DISCRIMINATOR = "discriminator";
    private static final String ID_COLUMN_NAME = "idColumnName";
    private static final String DISCRIMINATOR_COLUMN_NAME = "discriminatorColumnName";
    private static final String MESSAGE_COLUMN_NAME = "messageColumnName";
    private static final String THROWABLE_TYPE_NAME_COLUMN_NAME = "throwableTypeNameColumnName";
    private static final String STACK_TRACE_HASH_COLUMN_NAME = "stackTraceHashColumnName";
    private static final String THROWABLE_MESSAGE_COLUMN_NAME = "throwableMessageColumnName";
    private static final String STACK_TRACE_COLUMN_NAME = "stackTraceColumnName";
    private static final String SOURCE_TYPE_NAME_COLUMN_NAME = "sourceTypeNameColumnName";
    private static final String THREAD_NAME_COLUMN_NAME = "threadNameColumnName";
    private static final String TIMESTAMP_COLUMN_NAME = "timestampColumnName";
    private static final String STACK_TRACE_SERIALIZATION_MAX_LENGTH = "stackTraceSerializationMaxLength";
    private static final String ENTITY_MANAGER_CONFIGURATION = "entityManagerConfiguration";

    private static final String DISCRIMINATOR_BLANK =
            ValidationService.concat(PREFIX, DISCRIMINATOR, ValidationService.BLANK);
    private static final String ID_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, ID_COLUMN_NAME, ValidationService.BLANK);
    private static final String DISCRIMINATOR_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, DISCRIMINATOR_COLUMN_NAME, ValidationService.BLANK);
    private static final String MESSAGE_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, MESSAGE_COLUMN_NAME, ValidationService.BLANK);
    private static final String THROWABLE_TYPE_NAME_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_TYPE_NAME_COLUMN_NAME, ValidationService.BLANK);
    private static final String STACK_TRACE_HASH_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, STACK_TRACE_HASH_COLUMN_NAME, ValidationService.BLANK);
    private static final String THROWABLE_MESSAGE_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, THROWABLE_MESSAGE_COLUMN_NAME, ValidationService.BLANK);
    private static final String STACK_TRACE_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, STACK_TRACE_COLUMN_NAME, ValidationService.BLANK);
    private static final String SOURCE_TYPE_NAME_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, SOURCE_TYPE_NAME_COLUMN_NAME, ValidationService.BLANK);
    private static final String THREAD_NAME_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, THREAD_NAME_COLUMN_NAME, ValidationService.BLANK);
    private static final String TIMESTAMP_COLUMN_NAME_BLANK =
            ValidationService.concat(PREFIX, TIMESTAMP_COLUMN_NAME, ValidationService.BLANK);
    private static final String STACK_TRACE_SERIALIZATION_MAX_LENGTH_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(
                    PREFIX, STACK_TRACE_SERIALIZATION_MAX_LENGTH, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String ENTITY_MANAGER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, ENTITY_MANAGER_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public JpaExceptionStoreConfigurationValidator() {
        this(null);
    }

    public JpaExceptionStoreConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final JpaExceptionStoreConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded(),
                    value.getDiscriminator(),
                    value.getIdColumnName(),
                    value.getDiscriminatorColumnName(),
                    value.getMessageColumnName(),
                    value.getThrowableTypeNameColumnName(),
                    value.getStackTraceHashColumnName(),
                    value.getThrowableMessageColumnName(),
                    value.getStackTraceColumnName(),
                    value.getSourceTypeNameColumnName(),
                    value.getThreadNameColumnName(),
                    value.getTimestampColumnName(),
                    value.getStackTraceSerializationMaxLength(),
                    value.getEntityManagerConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link JpaExceptionStoreConfiguration} properties.
     *
     * @param enabled
     *      {@link JpaExceptionStoreConfiguration#isEnabled()}
     * @param threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded
     *      {@link JpaExceptionStoreConfiguration#getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded()}
     * @param discriminator
     *      {@link JpaExceptionStoreConfiguration#getDiscriminator()}
     * @param idColumnName
     *      {@link JpaExceptionStoreConfiguration#getIdColumnName()}
     * @param discriminatorColumnName
     *      {@link JpaExceptionStoreConfiguration#getDiscriminatorColumnName()}
     * @param messageColumnName
     *      {@link JpaExceptionStoreConfiguration#getMessageColumnName()}
     * @param throwableTypeNameColumnName
     *      {@link JpaExceptionStoreConfiguration#getThrowableTypeNameColumnName()}
     * @param stackTraceHashColumnName
     *      {@link JpaExceptionStoreConfiguration#getStackTraceHashColumnName()}
     * @param throwableMessageColumnName
     *      {@link JpaExceptionStoreConfiguration#getThrowableMessageColumnName()}
     * @param stackTraceColumnName
     *      {@link JpaExceptionStoreConfiguration#getStackTraceColumnName()}
     * @param sourceTypeNameColumnName
     *      {@link JpaExceptionStoreConfiguration#getSourceTypeNameColumnName()}
     * @param threadNameColumnName
     *      {@link JpaExceptionStoreConfiguration#getThreadNameColumnName()}
     * @param timestampColumnName
     *      {@link JpaExceptionStoreConfiguration#getTimestampColumnName()}
     * @param stackTraceSerializationMaxLength
     *      {@link JpaExceptionStoreConfiguration#getStackTraceSerializationMaxLength()}
     * @param entityManagerConfiguration
     *      {@link JpaExceptionStoreConfiguration#getEntityManagerConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final String threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded,
                                final String discriminator,
                                final String idColumnName,
                                final String discriminatorColumnName,
                                final String messageColumnName,
                                final String throwableTypeNameColumnName,
                                final String stackTraceHashColumnName,
                                final String throwableMessageColumnName,
                                final String stackTraceColumnName,
                                final String sourceTypeNameColumnName,
                                final String threadNameColumnName,
                                final String timestampColumnName,
                                final Integer stackTraceSerializationMaxLength,
                                final EntityManagerConfiguration entityManagerConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enabled,
                threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded,
                discriminator,
                idColumnName,
                discriminatorColumnName,
                messageColumnName,
                throwableTypeNameColumnName,
                stackTraceHashColumnName,
                throwableMessageColumnName,
                stackTraceColumnName,
                sourceTypeNameColumnName,
                threadNameColumnName,
                timestampColumnName,
                stackTraceSerializationMaxLength,
                entityManagerConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link JpaExceptionStoreConfiguration} properties.
     *
     * @param enabled
     *      {@link JpaExceptionStoreConfiguration#isEnabled()}
     * @param threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded
     *      {@link JpaExceptionStoreConfiguration#getThreadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded()}
     * @param discriminator
     *      {@link JpaExceptionStoreConfiguration#getDiscriminator()}
     * @param idColumnName
     *      {@link JpaExceptionStoreConfiguration#getIdColumnName()}
     * @param discriminatorColumnName
     *      {@link JpaExceptionStoreConfiguration#getDiscriminatorColumnName()}
     * @param messageColumnName
     *      {@link JpaExceptionStoreConfiguration#getMessageColumnName()}
     * @param throwableTypeNameColumnName
     *      {@link JpaExceptionStoreConfiguration#getThrowableTypeNameColumnName()}
     * @param stackTraceHashColumnName
     *      {@link JpaExceptionStoreConfiguration#getStackTraceHashColumnName()}
     * @param throwableMessageColumnName
     *      {@link JpaExceptionStoreConfiguration#getThrowableMessageColumnName()}
     * @param stackTraceColumnName
     *      {@link JpaExceptionStoreConfiguration#getStackTraceColumnName()}
     * @param sourceTypeNameColumnName
     *      {@link JpaExceptionStoreConfiguration#getSourceTypeNameColumnName()}
     * @param threadNameColumnName
     *      {@link JpaExceptionStoreConfiguration#getThreadNameColumnName()}
     * @param timestampColumnName
     *      {@link JpaExceptionStoreConfiguration#getTimestampColumnName()}
     * @param stackTraceSerializationMaxLength
     *      {@link JpaExceptionStoreConfiguration#getStackTraceSerializationMaxLength()}
     * @param entityManagerConfiguration
     *      {@link JpaExceptionStoreConfiguration#getEntityManagerConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final String threadNameFormatIfMediatorSharedExecutorIsNotSingleThreaded,
                                   final String discriminator,
                                   final String idColumnName,
                                   final String discriminatorColumnName,
                                   final String messageColumnName,
                                   final String throwableTypeNameColumnName,
                                   final String stackTraceHashColumnName,
                                   final String throwableMessageColumnName,
                                   final String stackTraceColumnName,
                                   final String sourceTypeNameColumnName,
                                   final String threadNameColumnName,
                                   final String timestampColumnName,
                                   final Integer stackTraceSerializationMaxLength,
                                   final EntityManagerConfiguration entityManagerConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context)
                                   throws AppbaseRuntimeException
    {
        boolean valid = true;

        if (enabled) {
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
                    idColumnName,
                    ID_COLUMN_NAME_BLANK,
                    ID_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    discriminatorColumnName,
                    DISCRIMINATOR_COLUMN_NAME_BLANK,
                    DISCRIMINATOR_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    messageColumnName,
                    MESSAGE_COLUMN_NAME_BLANK,
                    MESSAGE_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    throwableTypeNameColumnName,
                    THROWABLE_TYPE_NAME_COLUMN_NAME_BLANK,
                    THROWABLE_TYPE_NAME_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    stackTraceHashColumnName,
                    STACK_TRACE_HASH_COLUMN_NAME_BLANK,
                    STACK_TRACE_HASH_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    throwableMessageColumnName,
                    THROWABLE_MESSAGE_COLUMN_NAME_BLANK,
                    THROWABLE_MESSAGE_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    stackTraceColumnName,
                    STACK_TRACE_COLUMN_NAME_BLANK,
                    STACK_TRACE_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    sourceTypeNameColumnName,
                    SOURCE_TYPE_NAME_COLUMN_NAME_BLANK,
                    SOURCE_TYPE_NAME_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    threadNameColumnName,
                    THREAD_NAME_COLUMN_NAME_BLANK,
                    THREAD_NAME_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    timestampColumnName,
                    TIMESTAMP_COLUMN_NAME_BLANK,
                    TIMESTAMP_COLUMN_NAME,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    stackTraceSerializationMaxLength,
                    STACK_TRACE_SERIALIZATION_MAX_LENGTH_NOT_POSITIVE_OR_ZERO,
                    STACK_TRACE_SERIALIZATION_MAX_LENGTH,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    entityManagerConfiguration,
                    ENTITY_MANAGER_CONFIGURATION_NULL,
                    ENTITY_MANAGER_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }
        }

        return valid;
    }
}
