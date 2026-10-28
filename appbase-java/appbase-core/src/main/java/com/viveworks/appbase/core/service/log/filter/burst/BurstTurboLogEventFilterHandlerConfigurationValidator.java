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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultDuplicateLogHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultLogDeduplicatorConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link BurstTurboLogEventFilterHandlerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class BurstTurboLogEventFilterHandlerConfigurationValidator
        implements ConstraintValidator
        <BurstTurboLogEventFilterHandlerConfiguration.Valid, BurstTurboLogEventFilterHandlerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(BurstTurboLogEventFilterHandlerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(BurstTurboLogEventFilterHandlerConfiguration.class);

    private static final String BURST_RATE_THRESHOLD = "burstRateThreshold";
    private static final String DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION = "defaultLogDeduplicatorConfiguration";
    private static final String DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION = "defaultLogRateCalculatorConfiguration";
    private static final String DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION = "defaultDuplicateLogHandlerConfiguration";

    private static final String BURST_RATE_THRESHOLD_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, BURST_RATE_THRESHOLD, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_LOG_DEDUPLICATOR_CONFIGURATION, ValidationService.NULL);
    private static final String DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION, ValidationService.NULL);
    private static final String DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_DUPLICATE_LOG_HANDLER_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public BurstTurboLogEventFilterHandlerConfigurationValidator() {
        this(null);
    }

    public BurstTurboLogEventFilterHandlerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final BurstTurboLogEventFilterHandlerConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getBurstRateThreshold(),
                    value.getDefaultLogDeduplicatorConfiguration(),
                    value.getDefaultLogRateCalculatorConfiguration(),
                    value.getDefaultDuplicateLogHandlerConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link BurstTurboLogEventFilterHandlerConfiguration} properties.
     *
     * @param enabled
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#isEnabled()}
     * @param burstRateThreshold
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getBurstRateThreshold()}
     * @param defaultLogDeduplicatorConfiguration
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getDefaultLogDeduplicatorConfiguration()}
     * @param defaultLogRateCalculatorConfiguration
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getDefaultLogRateCalculatorConfiguration()}
     * @param defaultDuplicateLogHandlerConfiguration
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getDefaultDuplicateLogHandlerConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final double burstRateThreshold,
                                final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration,
                                final DefaultLogRateCalculatorConfiguration defaultLogRateCalculatorConfiguration,
                                final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enabled,
                burstRateThreshold,
                defaultLogDeduplicatorConfiguration,
                defaultLogRateCalculatorConfiguration,
                defaultDuplicateLogHandlerConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link BurstTurboLogEventFilterHandlerConfiguration} properties.
     *
     * @param enabled
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#isEnabled()}
     * @param burstRateThreshold
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getBurstRateThreshold()}
     * @param defaultLogDeduplicatorConfiguration
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getDefaultLogDeduplicatorConfiguration()}
     * @param defaultLogRateCalculatorConfiguration
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getDefaultLogRateCalculatorConfiguration()}
     * @param defaultDuplicateLogHandlerConfiguration
     *      {@link BurstTurboLogEventFilterHandlerConfiguration#getDefaultDuplicateLogHandlerConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final boolean enabled,
            final double burstRateThreshold,
            final DefaultLogDeduplicatorConfiguration defaultLogDeduplicatorConfiguration,
            final DefaultLogRateCalculatorConfiguration defaultLogRateCalculatorConfiguration,
            final DefaultDuplicateLogHandlerConfiguration defaultDuplicateLogHandlerConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context)
            throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositiveOrZero(
                    burstRateThreshold,
                    BURST_RATE_THRESHOLD_NOT_POSITIVE_OR_ZERO,
                    BURST_RATE_THRESHOLD,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

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
                    defaultLogRateCalculatorConfiguration,
                    DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION_NULL,
                    DEFAULT_LOG_RATE_CALCULATOR_CONFIGURATION,
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
