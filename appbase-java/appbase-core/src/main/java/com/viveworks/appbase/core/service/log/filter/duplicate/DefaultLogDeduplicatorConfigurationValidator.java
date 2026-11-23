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
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultLogDeduplicatorConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLogDeduplicatorConfigurationValidator
        implements ConstraintValidator
        <DefaultLogDeduplicatorConfiguration.Valid, DefaultLogDeduplicatorConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultLogDeduplicatorConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultLogDeduplicatorConfiguration.class);

    private static final String DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION =
            "duplicateLogHandlerSchedulableConfiguration";

    private static final String DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultLogDeduplicatorConfigurationValidator() {
        this(null);
    }

    public DefaultLogDeduplicatorConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultLogDeduplicatorConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.getDuplicateLogHandlerSchedulableConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultLogDeduplicatorConfiguration} properties.
     *
     * @param duplicateLogHandlerSchedulableConfiguration
     *      {@link DefaultLogDeduplicatorConfiguration#getDuplicateLogHandlerSchedulableConfiguration()}.
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final SchedulableConfiguration duplicateLogHandlerSchedulableConfiguration)
            throws AppbaseRuntimeException
    {
        validate(duplicateLogHandlerSchedulableConfiguration, null, null);
    }

    /**
     * Validate {@link DefaultLogDeduplicatorConfiguration} properties.
     *
     * @param duplicateLogHandlerSchedulableConfiguration
     *      {@link DefaultLogDeduplicatorConfiguration#getDuplicateLogHandlerSchedulableConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final SchedulableConfiguration duplicateLogHandlerSchedulableConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context)
            throws AppbaseRuntimeException
    {
        return ValidationService.validateNull(
                duplicateLogHandlerSchedulableConfiguration,
                DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION_NULL,
                DUPLICATE_LOG_HANDLER_SCHEDULABLE_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
    }
}
