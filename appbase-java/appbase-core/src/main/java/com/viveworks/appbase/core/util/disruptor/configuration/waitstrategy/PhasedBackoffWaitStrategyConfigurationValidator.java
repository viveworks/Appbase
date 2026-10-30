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

package com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * JSR 303 validator for {@link PhasedBackoffWaitStrategyConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class PhasedBackoffWaitStrategyConfigurationValidator
        implements ConstraintValidator
        <PhasedBackoffWaitStrategyConfiguration.Valid, PhasedBackoffWaitStrategyConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(PhasedBackoffWaitStrategyConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(PhasedBackoffWaitStrategyConfiguration.class);

    private static final String SPIN_TIMEOUT = "spinTimeout";
    private static final String YIELD_TIMEOUT = "yieldTimeout";
    private static final String UNITS = "units";
    private static final String WAIT_STRATEGY_CONFIGURATION = "waitStrategyConfiguration";

    private static final String SPIN_TIMEOUT_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, SPIN_TIMEOUT, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String YIELD_TIMEOUT_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, YIELD_TIMEOUT, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String UNITS_NULL =
            ValidationService.concat(PREFIX, UNITS, ValidationService.NULL);
    private static final String WAIT_STRATEGY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, WAIT_STRATEGY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public PhasedBackoffWaitStrategyConfigurationValidator() {
        this(null);
    }

    public PhasedBackoffWaitStrategyConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final PhasedBackoffWaitStrategyConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getSpinTimeout(),
                    value.getYieldTimeout(),
                    value.getUnits(),
                    value.getWaitStrategyConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link PhasedBackoffWaitStrategyConfiguration} properties.
     *
     * @param enabled
     *      {@link PhasedBackoffWaitStrategyConfiguration#isEnabled()}
     * @param spinTimeout
     *      {@link PhasedBackoffWaitStrategyConfiguration#getSpinTimeout()}
     * @param yieldTimeout
     *      {@link PhasedBackoffWaitStrategyConfiguration#getYieldTimeout()}
     * @param units
     *      {@link PhasedBackoffWaitStrategyConfiguration#getUnits()}
     * @param waitStrategyConfiguration
     *      {@link PhasedBackoffWaitStrategyConfiguration#getWaitStrategyConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final long spinTimeout,
                                final long yieldTimeout,
                                final TimeUnit units,
                                final WaitStrategyConfiguration waitStrategyConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enabled, spinTimeout, yieldTimeout, units, waitStrategyConfiguration, null, null);
    }

    /**
     * Validate {@link PhasedBackoffWaitStrategyConfiguration} properties.
     *
     * @param enabled
     *      {@link PhasedBackoffWaitStrategyConfiguration#isEnabled()}
     * @param spinTimeout
     *      {@link PhasedBackoffWaitStrategyConfiguration#getSpinTimeout()}
     * @param yieldTimeout
     *      {@link PhasedBackoffWaitStrategyConfiguration#getYieldTimeout()}
     * @param units
     *      {@link PhasedBackoffWaitStrategyConfiguration#getUnits()}
     * @param waitStrategyConfiguration
     *      {@link PhasedBackoffWaitStrategyConfiguration#getWaitStrategyConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final long spinTimeout,
                                   final long yieldTimeout,
                                   final TimeUnit units,
                                   final WaitStrategyConfiguration waitStrategyConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context)
                                   throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositiveOrZero(
                    spinTimeout,
                    SPIN_TIMEOUT_NOT_POSITIVE_OR_ZERO,
                    SPIN_TIMEOUT,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    yieldTimeout,
                    YIELD_TIMEOUT_NOT_POSITIVE_OR_ZERO,
                    YIELD_TIMEOUT,
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

            valid = ValidationService.validateNull(
                    waitStrategyConfiguration,
                    WAIT_STRATEGY_CONFIGURATION_NULL,
                    WAIT_STRATEGY_CONFIGURATION,
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
