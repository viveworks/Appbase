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

package com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * JSR 303 validator for {@link LiteTimeoutBlockingWaitStrategyConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class LiteTimeoutBlockingWaitStrategyConfigurationValidator
        implements ConstraintValidator
        <LiteTimeoutBlockingWaitStrategyConfiguration.Valid, LiteTimeoutBlockingWaitStrategyConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(LiteTimeoutBlockingWaitStrategyConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(LiteTimeoutBlockingWaitStrategyConfiguration.class);

    private static final String TIMEOUT = "timeout";
    private static final String UNITS = "units";

    private static final String TIMEOUT_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, TIMEOUT, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String UNITS_NULL =
            ValidationService.concat(PREFIX, TIMEOUT, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public LiteTimeoutBlockingWaitStrategyConfigurationValidator() {
        this(null);
    }

    public LiteTimeoutBlockingWaitStrategyConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final LiteTimeoutBlockingWaitStrategyConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(value.isEnabled(), value.getTimeout(), value.getUnits(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link LiteTimeoutBlockingWaitStrategyConfiguration} properties.
     *
     * @param enabled {@link LiteTimeoutBlockingWaitStrategyConfiguration#isEnabled()}
     * @param timeout {@link LiteTimeoutBlockingWaitStrategyConfiguration#getTimeout()}
     * @param units {@link LiteTimeoutBlockingWaitStrategyConfiguration#getUnits()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final long timeout,
                                final TimeUnit units) throws AppbaseRuntimeException
    {
        validate(enabled, timeout, units, null, null);
    }

    /**
     * Validate {@link LiteTimeoutBlockingWaitStrategyConfiguration} properties.
     *
     * @param enabled {@link LiteTimeoutBlockingWaitStrategyConfiguration#isEnabled()}
     * @param timeout {@link LiteTimeoutBlockingWaitStrategyConfiguration#getTimeout()}
     * @param units {@link LiteTimeoutBlockingWaitStrategyConfiguration#getUnits()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final long timeout,
                                   final TimeUnit units,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositiveOrZero(
                    timeout,
                    TIMEOUT_NOT_POSITIVE_OR_ZERO,
                    TIMEOUT,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateEnumNull(
                    units,
                    UNITS_NULL,
                    UNITS,
                    context);
            if (!valid) {
                return valid;
            }

            return valid;
        }

        return true;
    }
}
