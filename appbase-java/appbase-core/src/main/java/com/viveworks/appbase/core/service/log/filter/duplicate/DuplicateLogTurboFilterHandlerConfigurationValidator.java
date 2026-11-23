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
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DuplicateLogTurboFilterHandlerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DuplicateLogTurboFilterHandlerConfigurationValidator
        implements ConstraintValidator
        <DuplicateLogTurboFilterHandlerConfiguration.Valid, DuplicateLogTurboFilterHandlerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DuplicateLogTurboFilterHandlerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DuplicateLogTurboFilterHandlerConfiguration.class);

    private static final String DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION = "defaultLogDeduplicatorConfiguration";
    private static final String DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION = "defaultDuplicateLogHandlerConfiguration";

    private static final String DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION, ValidationService.NULL);
    private static final String DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DuplicateLogTurboFilterHandlerConfigurationValidator() {
        this(null);
    }

    public DuplicateLogTurboFilterHandlerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DuplicateLogTurboFilterHandlerConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getDefaultLogDeduplicatorConfiguration(),
                    value.getDefaultDuplicateLogHandlerConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DuplicateLogTurboFilterHandlerConfiguration} properties.
     *
     * @param enabled
     *      {@link DuplicateLogTurboFilterHandlerConfiguration#isEnabled()}
     * @param defaultLogDeduplicatorConfiguration
     *      {@link DuplicateLogTurboFilterHandlerConfiguration#getDefaultLogDeduplicatorConfiguration()}
     * @param defaultDuplicateLogHandlerConfiguration
     *      {@link DuplicateLogTurboFilterHandlerConfiguration#getDefaultDuplicateLogHandlerConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration,
                                final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enabled,
                defaultLogDeduplicatorConfiguration,
                defaultDuplicateLogHandlerConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link DuplicateLogTurboFilterHandlerConfiguration} properties.
     *
     * @param enabled
     *      {@link DuplicateLogTurboFilterHandlerConfiguration#isEnabled()}
     * @param defaultLogDeduplicatorConfiguration
     *      {@link DuplicateLogTurboFilterHandlerConfiguration#getDefaultLogDeduplicatorConfiguration()}
     * @param defaultDuplicateLogHandlerConfiguration
     *      {@link DuplicateLogTurboFilterHandlerConfiguration#getDefaultDuplicateLogHandlerConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final boolean enabled,
            final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration,
            final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context)
            throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validateNull(
                    defaultLogDeduplicatorConfiguration,
                    DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION_NULL,
                    DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    defaultDuplicateLogHandlerConfiguration,
                    DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION_NULL,
                    DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }
        }

        return true;
    }
}
