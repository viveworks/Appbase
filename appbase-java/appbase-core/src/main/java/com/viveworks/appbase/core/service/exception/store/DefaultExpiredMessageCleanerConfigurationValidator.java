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

package com.viveworks.appbase.core.service.exception.store;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * JSR 303 validator for {@link DefaultExpiredMessageCleanerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultExpiredMessageCleanerConfigurationValidator
        implements ConstraintValidator
        <DefaultExpiredMessageCleanerConfiguration.Valid, DefaultExpiredMessageCleanerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultExpiredMessageCleanerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultExpiredMessageCleanerConfiguration.class);

    private static final String EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD = "exclusiveRemoveBeforeTimePeriod";
    private static final String EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT = "exclusiveRemoveBeforeTimePeriodUnit";
    private static final String SCHEDULABLE_CONFIGURATION = "schedulableConfiguration";

    private static final String EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_NOT_POSITIVE =
            ValidationService.concat(PREFIX, EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD, ValidationService.NOT_POSITIVE);
    private static final String EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT_NULL =
            ValidationService.concat(PREFIX, EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT, ValidationService.NULL);
    private static final String SCHEDULABLE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, SCHEDULABLE_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultExpiredMessageCleanerConfigurationValidator() {
        this(null);
    }

    public DefaultExpiredMessageCleanerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultExpiredMessageCleanerConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getExclusiveRemoveBeforeTimePeriod(),
                    value.getExclusiveRemoveBeforeTimePeriodUnit(),
                    value.getSchedulableConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultExpiredMessageCleanerConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultExpiredMessageCleanerConfiguration#isEnabled()}
     * @param exclusiveRemoveBeforeTimePeriod
     *      {@link DefaultExpiredMessageCleanerConfiguration#getExclusiveRemoveBeforeTimePeriod()}
     * @param exclusiveRemoveBeforeTimePeriodUnit
     *      {@link DefaultExpiredMessageCleanerConfiguration#getExclusiveRemoveBeforeTimePeriodUnit()}
     * @param schedulableConfiguration
     *      {@link DefaultExpiredMessageCleanerConfiguration#getSchedulableConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final long exclusiveRemoveBeforeTimePeriod,
                                final TimeUnit exclusiveRemoveBeforeTimePeriodUnit,
                                final SchedulableConfiguration schedulableConfiguration) throws AppbaseRuntimeException
    {
        validate(enabled,
                exclusiveRemoveBeforeTimePeriod,
                exclusiveRemoveBeforeTimePeriodUnit,
                schedulableConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link DefaultExpiredMessageCleanerConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultExpiredMessageCleanerConfiguration#isEnabled()}
     * @param exclusiveRemoveBeforeTimePeriod
     *      {@link DefaultExpiredMessageCleanerConfiguration#getExclusiveRemoveBeforeTimePeriod()}
     * @param exclusiveRemoveBeforeTimePeriodUnit
     *      {@link DefaultExpiredMessageCleanerConfiguration#getExclusiveRemoveBeforeTimePeriodUnit()}
     * @param schedulableConfiguration
     *      {@link DefaultExpiredMessageCleanerConfiguration#getSchedulableConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final long exclusiveRemoveBeforeTimePeriod,
                                   final TimeUnit exclusiveRemoveBeforeTimePeriodUnit,
                                   final SchedulableConfiguration schedulableConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid = true;

        if (enabled) {
            valid = ValidationService.validatePositive(
                    exclusiveRemoveBeforeTimePeriod,
                    EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_NOT_POSITIVE,
                    EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    exclusiveRemoveBeforeTimePeriodUnit,
                    EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT_NULL,
                    EXCLUSIVE_REMOVE_BEFORE_TIME_PERIOD_UNIT,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    schedulableConfiguration,
                    SCHEDULABLE_CONFIGURATION_NULL,
                    SCHEDULABLE_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
        }

        return valid;
    }
}
