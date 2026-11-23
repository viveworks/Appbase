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

package com.viveworks.appbase.core.service.exception.store.inmemory;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link InMemoryExceptionStoreConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class InMemoryExceptionStoreConfigurationValidator
        implements ConstraintValidator
        <InMemoryExceptionStoreConfiguration.Valid, InMemoryExceptionStoreConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(InMemoryExceptionStoreConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(InMemoryExceptionStoreConfiguration.class);

    private static final String STACK_TRACE_SERIALIZATION_MAX_LENGTH = "stackTraceSerializationMaxLength";
    private static final String STACK_TRACE_SERIALIZATION_MAX_LENGTH_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(
                    PREFIX, STACK_TRACE_SERIALIZATION_MAX_LENGTH, ValidationService.NOT_POSITIVE_OR_ZERO);

    @Nullable
    private final ValidationService validationService;

    public InMemoryExceptionStoreConfigurationValidator() {
        this(null);
    }

    public InMemoryExceptionStoreConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final InMemoryExceptionStoreConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.getStackTraceSerializationMaxLength(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link InMemoryExceptionStoreConfiguration} properties.
     *
     * @param stackTraceSerializationMaxLength
     *      {@link InMemoryExceptionStoreConfiguration#getStackTraceSerializationMaxLength()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final int stackTraceSerializationMaxLength) throws AppbaseRuntimeException {
        validate(stackTraceSerializationMaxLength, null, null);
    }

    /**
     * Validate {@link InMemoryExceptionStoreConfiguration} properties.
     *
     * @param stackTraceSerializationMaxLength
     *      {@link InMemoryExceptionStoreConfiguration#getStackTraceSerializationMaxLength()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final int stackTraceSerializationMaxLength,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return ValidationService.validatePositiveOrZero(
                stackTraceSerializationMaxLength,
                STACK_TRACE_SERIALIZATION_MAX_LENGTH_NOT_POSITIVE_OR_ZERO,
                STACK_TRACE_SERIALIZATION_MAX_LENGTH,
                NODE_NAME_PREFIX,
                validationService,
                context);
    }
}
