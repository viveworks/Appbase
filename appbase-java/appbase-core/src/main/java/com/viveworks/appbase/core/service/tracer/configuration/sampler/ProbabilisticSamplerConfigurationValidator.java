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

package com.viveworks.appbase.core.service.tracer.configuration.sampler;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ProbabilisticSamplerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ProbabilisticSamplerConfigurationValidator
        implements ConstraintValidator
        <ProbabilisticSamplerConfiguration.Valid, ProbabilisticSamplerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ProbabilisticSamplerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ProbabilisticSamplerConfiguration.class);

    private static final String SAMPLING_RATE = "samplingRate";
    private static final String SAMPLING_RATE_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, SAMPLING_RATE, ValidationService.NOT_POSITIVE_OR_ZERO);

    @Nullable
    private final ValidationService validationService;

    public ProbabilisticSamplerConfigurationValidator() {
        this(null);
    }

    public ProbabilisticSamplerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ProbabilisticSamplerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(value.getSamplingRate(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link ProbabilisticSamplerConfiguration} properties.
     *
     * @param samplingRate {@link ProbabilisticSamplerConfiguration#getSamplingRate()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final double samplingRate) throws AppbaseRuntimeException {
        validate(samplingRate, null, null);
    }

    /**
     * Validate {@link ProbabilisticSamplerConfiguration} properties.
     *
     * @param samplingRate {@link ProbabilisticSamplerConfiguration#getSamplingRate()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final double samplingRate,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return ValidationService.validatePositiveOrZero(
                samplingRate,
                SAMPLING_RATE_NOT_POSITIVE_OR_ZERO,
                SAMPLING_RATE,
                NODE_NAME_PREFIX,
                validationService,
                context);
    }
}
