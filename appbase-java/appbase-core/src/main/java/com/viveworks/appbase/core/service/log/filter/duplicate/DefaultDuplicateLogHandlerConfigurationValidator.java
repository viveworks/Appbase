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

package com.viveworks.appbase.core.service.log.filter.duplicate;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultDuplicateLogHandlerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDuplicateLogHandlerConfigurationValidator
        implements ConstraintValidator
        <DefaultDuplicateLogHandlerConfiguration.Valid, DefaultDuplicateLogHandlerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultDuplicateLogHandlerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultDuplicateLogHandlerConfiguration.class);

    private static final String FORMATTED_MESSAGE_LOG_LEVEL = "formattedMessageLogLevel";
    private static final String THROWABLE_LOG_LEVEL = "throwableLogLevel";

    private static final String FORMATTED_MESSAGE_LOG_LEVEL_NULL =
            ValidationService.concat(PREFIX, FORMATTED_MESSAGE_LOG_LEVEL, ValidationService.NULL);
    private static final String THROWABLE_LOG_LEVEL_NULL =
            ValidationService.concat(PREFIX, THROWABLE_LOG_LEVEL, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultDuplicateLogHandlerConfigurationValidator() {
        this(null);
    }

    public DefaultDuplicateLogHandlerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultDuplicateLogHandlerConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getFormattedMessageLogLevel(),
                    value.getThrowableLogLevel(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultDuplicateLogHandlerConfiguration} properties.
     *
     * @param formattedMessageLogLevel
     *      {@link DefaultDuplicateLogHandlerConfiguration#getFormattedMessageLogLevel()}
     * @param throwableLogLevel
     *      {@link DefaultDuplicateLogHandlerConfiguration#getThrowableLogLevel()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final LogLevel formattedMessageLogLevel,
                                final LogLevel throwableLogLevel) throws AppbaseRuntimeException
    {
        validate(formattedMessageLogLevel, throwableLogLevel, null, null);
    }

    /**
     * Validate {@link DefaultDuplicateLogHandlerConfiguration} properties.
     *
     * @param formattedMessageLogLevel
     *      {@link DefaultDuplicateLogHandlerConfiguration#getFormattedMessageLogLevel()}
     * @param throwableLogLevel
     *      {@link DefaultDuplicateLogHandlerConfiguration#getThrowableLogLevel()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final LogLevel formattedMessageLogLevel,
                                   final LogLevel throwableLogLevel,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        final boolean valid = ValidationService.validateEnumNull(
                formattedMessageLogLevel,
                FORMATTED_MESSAGE_LOG_LEVEL_NULL,
                FORMATTED_MESSAGE_LOG_LEVEL,
                context);
        if (!valid) {
            return valid;
        }

        return ValidationService.validateEnumNull(
                throwableLogLevel,
                THROWABLE_LOG_LEVEL_NULL,
                THROWABLE_LOG_LEVEL,
                context);
    }
}
