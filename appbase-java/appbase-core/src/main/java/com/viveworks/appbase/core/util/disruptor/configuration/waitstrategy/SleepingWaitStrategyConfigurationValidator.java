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

/**
 * JSR 303 validator for {@link SleepingWaitStrategyConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SleepingWaitStrategyConfigurationValidator
        implements ConstraintValidator
        <SleepingWaitStrategyConfiguration.Valid, SleepingWaitStrategyConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(SleepingWaitStrategyConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(SleepingWaitStrategyConfiguration.class);

    private static final String RETRIES = "retries";
    private static final String SLEEP_TIME_NANO_SECONDS = "sleepTimeNanoSeconds";

    private static final String RETRIES_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, RETRIES, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String SLEEP_TIME_NANO_SECONDS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, SLEEP_TIME_NANO_SECONDS, ValidationService.NOT_POSITIVE_OR_ZERO);

    @Nullable
    private final ValidationService validationService;

    public SleepingWaitStrategyConfigurationValidator() {
        this(null);
    }

    public SleepingWaitStrategyConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final SleepingWaitStrategyConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getRetries(),
                    value.getSleepTimeNanoSeconds(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link SleepingWaitStrategyConfiguration} properties.
     *
     * @param enabled
     *      {@link SleepingWaitStrategyConfiguration#isEnabled()}
     * @param retries
     *      {@link SleepingWaitStrategyConfiguration#getRetries()}
     * @param sleepTimeNanoSeconds
     *      {@link SleepingWaitStrategyConfiguration#getSleepTimeNanoSeconds()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final int retries,
                                final long sleepTimeNanoSeconds) throws AppbaseRuntimeException
    {
        validate(enabled, retries, sleepTimeNanoSeconds, null, null);
    }

    /**
     * Validate {@link SleepingWaitStrategyConfiguration} properties.
     *
     * @param enabled
     *      {@link SleepingWaitStrategyConfiguration#isEnabled()}
     * @param retries
     *      {@link SleepingWaitStrategyConfiguration#getRetries()}
     * @param sleepTimeNanoSeconds
     *      {@link SleepingWaitStrategyConfiguration#getSleepTimeNanoSeconds()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final int retries,
                                   final long sleepTimeNanoSeconds,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositiveOrZero(
                    retries,
                    RETRIES_NOT_POSITIVE_OR_ZERO,
                    RETRIES,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    sleepTimeNanoSeconds,
                    SLEEP_TIME_NANO_SECONDS_NOT_POSITIVE_OR_ZERO,
                    SLEEP_TIME_NANO_SECONDS,
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
