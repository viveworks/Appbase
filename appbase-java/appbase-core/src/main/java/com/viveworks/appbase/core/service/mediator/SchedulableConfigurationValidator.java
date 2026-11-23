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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * JSR 303 validator for {@link SchedulableConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SchedulableConfigurationValidator
        implements ConstraintValidator
        <SchedulableConfiguration.Valid, SchedulableConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(SchedulableConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(SchedulableConfiguration.class);

    private static final String INITIAL_DELAY = "initialDelay";
    private static final String INTERVAL = "interval";
    private static final String TIME_UNIT = "timeUnit";

    private static final String INITIAL_DELAY_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, INITIAL_DELAY, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String INTERVAL_NOT_POSITIVE =
            ValidationService.concat(PREFIX, INTERVAL, ValidationService.NOT_POSITIVE);
    private static final String TIME_UNIT_NULL =
            ValidationService.concat(PREFIX, TIME_UNIT, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public SchedulableConfigurationValidator() {
        this(null);
    }

    public SchedulableConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final SchedulableConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getInitialDelay(),
                    value.getInterval(),
                    value.getTimeUnit(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link SchedulableConfiguration} properties.
     *
     * @param enabled
     *      {@link SchedulableConfiguration#isEnabled()}
     * @param initialDelay
     *      {@link SchedulableConfiguration#getInitialDelay()}
     * @param interval
     *      {@link SchedulableConfiguration#getInterval()}
     * @param timeUnit
     *      {@link SchedulableConfiguration#getTimeUnit()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final long initialDelay,
                                final long interval,
                                final TimeUnit timeUnit) throws AppbaseRuntimeException
    {
        validate(enabled, initialDelay, interval, timeUnit, null, null);
    }

    /**
     * Validate {@link SchedulableConfiguration} properties.
     *
     * @param enabled
     *      {@link SchedulableConfiguration#isEnabled()}
     * @param initialDelay
     *      {@link SchedulableConfiguration#getInitialDelay()}
     * @param interval
     *      {@link SchedulableConfiguration#getInterval()}
     * @param timeUnit
     *      {@link SchedulableConfiguration#getTimeUnit()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final long initialDelay,
                                   final long interval,
                                   final TimeUnit timeUnit,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositiveOrZero(
                    initialDelay,
                    INITIAL_DELAY_NOT_POSITIVE_OR_ZERO,
                    INITIAL_DELAY,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositive(
                    interval,
                    INTERVAL_NOT_POSITIVE,
                    INTERVAL,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateEnumNull(
                    timeUnit,
                    TIME_UNIT_NULL,
                    TIME_UNIT,
                    context);
            if (!valid) {
                return valid;
            }
        }

        return true;
    }
}
