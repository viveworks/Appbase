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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultLogRateCalculatorConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLogRateCalculatorConfigurationValidator
        implements ConstraintValidator
        <DefaultLogRateCalculatorConfiguration.Valid, DefaultLogRateCalculatorConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultLogRateCalculatorConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultLogRateCalculatorConfiguration.class);

    private static final String EWMA_RATE = "ewmaRate";

    private static final String EWMA_RATE_NULL =
            ValidationService.concat(PREFIX, EWMA_RATE, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultLogRateCalculatorConfigurationValidator() {
        this(null);
    }

    public DefaultLogRateCalculatorConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultLogRateCalculatorConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.getEwmaRate(), validationService, context);
        }

        return false;
    }

    /**
     * Valiate {@link DefaultLogRateCalculatorConfiguration} properties.
     *
     * @param ewmaRate
     *      {@link DefaultLogRateCalculatorConfiguration#getEwmaRate()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final EwmaRate ewmaRate) throws AppbaseRuntimeException {
        validate(ewmaRate, null, null);
    }

    /**
     * Valiate {@link DefaultLogRateCalculatorConfiguration} properties.
     *
     * @param ewmaRate
     *      {@link DefaultLogRateCalculatorConfiguration#getEwmaRate()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final EwmaRate ewmaRate,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return ValidationService.validateEnumNull(
                ewmaRate,
                EWMA_RATE_NULL,
                EWMA_RATE,
                context);
    }
}
