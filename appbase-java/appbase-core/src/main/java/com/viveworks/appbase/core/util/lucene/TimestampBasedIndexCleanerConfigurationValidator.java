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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * JSR 303 validator for {@link TimestampBasedIndexCleanerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class TimestampBasedIndexCleanerConfigurationValidator
        implements ConstraintValidator
        <TimestampBasedIndexCleanerConfiguration.Valid, TimestampBasedIndexCleanerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(TimestampBasedIndexCleanerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(TimestampBasedIndexCleanerConfiguration.class);

    private static final String REMOVE_BEFORE_TIME_PERIOD = "removeBeforeTimePeriod";
    private static final String REMOVE_BEFORE_TIME_PERIOD_UNIT = "removeBeforeTimePeriodUnit";
    private static final String CREATION_TIMESTAMP_FIELD_NAME = "creationTimestampFieldName";
    private static final String SCHEDULABLE_CONFIGURATION = "schedulableConfiguration";

    private static final String REMOVE_BEFORE_TIME_PERIOD_NOT_POSITIVE =
            ValidationService.concat(PREFIX, REMOVE_BEFORE_TIME_PERIOD, ValidationService.NOT_POSITIVE);
    private static final String REMOVE_BEFORE_TIME_PERIOD_UNIT_NULL =
            ValidationService.concat(PREFIX, REMOVE_BEFORE_TIME_PERIOD_UNIT, ValidationService.NULL);
    private static final String CREATION_TIMESTAMP_FIELD_NAME_BLANK =
            ValidationService.concat(PREFIX, CREATION_TIMESTAMP_FIELD_NAME, ValidationService.BLANK);
    private static final String SCHEDULABLE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, SCHEDULABLE_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public TimestampBasedIndexCleanerConfigurationValidator() {
        this(null);
    }

    public TimestampBasedIndexCleanerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final TimestampBasedIndexCleanerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getRemoveBeforeTimePeriod(),
                    value.getRemoveBeforeTimePeriodUnit(),
                    value.getCreationTimestampFieldName(),
                    value.getSchedulableConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link TimestampBasedIndexCleanerConfiguration} properties.
     *
     * @param enabled
     *      {@link TimestampBasedIndexCleanerConfiguration#isEnabled()}
     * @param removeBeforeTimePeriod
     *      {@link TimestampBasedIndexCleanerConfiguration#getRemoveBeforeTimePeriod()}
     * @param removeBeforeTimePeriodUnit
     *      {@link TimestampBasedIndexCleanerConfiguration#getRemoveBeforeTimePeriodUnit()}
     * @param creationTimestampFieldName
     *      {@link TimestampBasedIndexCleanerConfiguration#getCreationTimestampFieldName()}
     * @param schedulableConfiguration
     *      {@link TimestampBasedIndexCleanerConfiguration#getSchedulableConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final long removeBeforeTimePeriod,
                                final TimeUnit removeBeforeTimePeriodUnit,
                                final String creationTimestampFieldName,
                                final SchedulableConfiguration schedulableConfiguration) throws AppbaseRuntimeException
    {
        validate(enabled,
                removeBeforeTimePeriod,
                removeBeforeTimePeriodUnit,
                creationTimestampFieldName,
                schedulableConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link TimestampBasedIndexCleanerConfiguration} properties.
     *
     * @param enabled
     *      {@link TimestampBasedIndexCleanerConfiguration#isEnabled()}
     * @param removeBeforeTimePeriod
     *      {@link TimestampBasedIndexCleanerConfiguration#getRemoveBeforeTimePeriod()}
     * @param removeBeforeTimePeriodUnit
     *      {@link TimestampBasedIndexCleanerConfiguration#getRemoveBeforeTimePeriodUnit()}
     * @param creationTimestampFieldName
     *      {@link TimestampBasedIndexCleanerConfiguration#getCreationTimestampFieldName()}
     * @param schedulableConfiguration
     *      {@link TimestampBasedIndexCleanerConfiguration#getSchedulableConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final long removeBeforeTimePeriod,
                                   final TimeUnit removeBeforeTimePeriodUnit,
                                   final String creationTimestampFieldName,
                                   final SchedulableConfiguration schedulableConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositive(
                    removeBeforeTimePeriod,
                    REMOVE_BEFORE_TIME_PERIOD_NOT_POSITIVE,
                    REMOVE_BEFORE_TIME_PERIOD,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateEnumNull(
                    removeBeforeTimePeriodUnit,
                    REMOVE_BEFORE_TIME_PERIOD_UNIT_NULL,
                    REMOVE_BEFORE_TIME_PERIOD_UNIT,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateBlank(
                    creationTimestampFieldName,
                    CREATION_TIMESTAMP_FIELD_NAME_BLANK,
                    CREATION_TIMESTAMP_FIELD_NAME,
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
            if (!valid) {
                return valid;
            }

            return valid;
        }

        return true;
    }
}
