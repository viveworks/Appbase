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
import java.util.Map;

/**
 * JSR 303 validator for {@link PerOperationSamplerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class PerOperationSamplerConfigurationValidator
        implements ConstraintValidator
        <PerOperationSamplerConfiguration.Valid, PerOperationSamplerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(PerOperationSamplerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(PerOperationSamplerConfiguration.class);

    private static final String MAX_OPERATIONS = "maxOperations";
    private static final String DEFAULT_SAMPLING_PROBABILITY = "defaultSamplingProbability";
    private static final String DEFAULT_LOWER_BOUND_TRACES_PER_SECOND = "defaultLowerBoundTracesPerSecond";
    private static final String SAMPLING_RATE_BY_OPERATION_NAME_MAP = "samplingRateByOperationNameMap";

    private static final String MAX_OPERATIONS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, MAX_OPERATIONS, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String DEFAULT_SAMPLING_PROBABILITY_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, DEFAULT_SAMPLING_PROBABILITY, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String DEFAULT_LOWER_BOUND_TRACES_PER_SECOND_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(
                    PREFIX,
                    DEFAULT_LOWER_BOUND_TRACES_PER_SECOND,
                    ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String SAMPLING_RATE_BY_OPERATION_NAME_MAP_NULL =
            ValidationService.concat(PREFIX, SAMPLING_RATE_BY_OPERATION_NAME_MAP, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public PerOperationSamplerConfigurationValidator() {
        this(null);
    }

    public PerOperationSamplerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final PerOperationSamplerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.getMaxOperations(),
                    value.getDefaultSamplingProbability(),
                    value.getDefaultLowerBoundTracesPerSecond(),
                    value.getSamplingRateByOperationNameMap(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link PerOperationSamplerConfiguration} properties.
     *
     * @param maxOperations
     *      {@link PerOperationSamplerConfiguration#getMaxOperations()}
     * @param defaultSamplingProbability
     *      {@link PerOperationSamplerConfiguration#getDefaultSamplingProbability()}
     * @param defaultLowerBoundTracesPerSecond
     *      {@link PerOperationSamplerConfiguration#getDefaultLowerBoundTracesPerSecond()}
     * @param samplingRateByOperationNameMap
     *      {@link PerOperationSamplerConfiguration#getSamplingRateByOperationNameMap()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final int maxOperations,
                                final double defaultSamplingProbability,
                                final double defaultLowerBoundTracesPerSecond,
                                final Map<String, Double> samplingRateByOperationNameMap) throws AppbaseRuntimeException
    {
        validate(maxOperations,
                defaultSamplingProbability,
                defaultLowerBoundTracesPerSecond,
                samplingRateByOperationNameMap,
                null,
                null);
    }

    /**
     * Validate {@link PerOperationSamplerConfiguration} properties.
     *
     * @param maxOperations
     *      {@link PerOperationSamplerConfiguration#getMaxOperations()}
     * @param defaultSamplingProbability
     *      {@link PerOperationSamplerConfiguration#getDefaultSamplingProbability()}
     * @param defaultLowerBoundTracesPerSecond
     *      {@link PerOperationSamplerConfiguration#getDefaultLowerBoundTracesPerSecond()}
     * @param samplingRateByOperationNameMap
     *      {@link PerOperationSamplerConfiguration#getSamplingRateByOperationNameMap()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final int maxOperations,
                                   final double defaultSamplingProbability,
                                   final double defaultLowerBoundTracesPerSecond,
                                   final Map<String, Double> samplingRateByOperationNameMap,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validatePositiveOrZero(
                maxOperations,
                MAX_OPERATIONS_NOT_POSITIVE_OR_ZERO,
                MAX_OPERATIONS,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositiveOrZero(
                defaultSamplingProbability,
                DEFAULT_SAMPLING_PROBABILITY_NOT_POSITIVE_OR_ZERO,
                DEFAULT_SAMPLING_PROBABILITY,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositiveOrZero(
                defaultLowerBoundTracesPerSecond,
                DEFAULT_LOWER_BOUND_TRACES_PER_SECOND_NOT_POSITIVE_OR_ZERO,
                DEFAULT_LOWER_BOUND_TRACES_PER_SECOND,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                samplingRateByOperationNameMap,
                SAMPLING_RATE_BY_OPERATION_NAME_MAP_NULL,
                SAMPLING_RATE_BY_OPERATION_NAME_MAP,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
