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

package com.viveworks.appbase.core.service.metric.jmx.registry;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;

/**
 * Validate {@link JmxMeterRegistryConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxMeterRegistryConfigurationValidator
        implements ConstraintValidator
        <JmxMeterRegistryConfiguration.Valid, JmxMeterRegistryConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(JmxMeterRegistryConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(JmxMeterRegistryConfiguration.class);

    private static final String PREFIX_PROPERTY = "prefix";
    private static final String DOMAIN = "domain";
    private static final String STEP = "step";

    private static final String PREFIX_PROPERTY_NULL =
            ValidationService.concat(PREFIX, PREFIX_PROPERTY, ValidationService.NULL);
    private static final String DOMAIN_NULL =
            ValidationService.concat(PREFIX, DOMAIN, ValidationService.NULL);
    private static final String STEP_NULL =
            ValidationService.concat(PREFIX, STEP, ValidationService.NULL);
    private static final String STEP_NOT_POSITIVE =
            ValidationService.concat(PREFIX, STEP, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public JmxMeterRegistryConfigurationValidator() {
        this(null);
    }

    public JmxMeterRegistryConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final JmxMeterRegistryConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getAdditionalProperties(),
                    value.prefix(),
                    value.domain(),
                    value.step(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link JmxMeterRegistryConfiguration} properties.
     *
     * @param enabled
     *          {@link JmxMeterRegistryConfiguration#isEnabled()}
     * @param additionalProperties
     *          {@link JmxMeterRegistryConfiguration#get(String)}
     * @param prefix
     *          {@link JmxMeterRegistryConfiguration#prefix()}
     * @param domain
     *          {@link JmxMeterRegistryConfiguration#domain()}
     * @param step
     *          {@link JmxMeterRegistryConfiguration#step()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final Map<String, String> additionalProperties,
                                final String prefix,
                                final String domain,
                                final Duration step) throws AppbaseRuntimeException
    {
        validate(enabled, additionalProperties, prefix, domain, step, null, null);
    }

    /**
     * Validate {@link JmxMeterRegistryConfiguration} properties.
     *
     * @param enabled
     *          {@link JmxMeterRegistryConfiguration#isEnabled()}
     * @param additionalProperties
     *          {@link JmxMeterRegistryConfiguration#get(String)}
     * @param prefix
     *          {@link JmxMeterRegistryConfiguration#prefix()}
     * @param domain
     *          {@link JmxMeterRegistryConfiguration#domain()}
     * @param step
     *          {@link JmxMeterRegistryConfiguration#step()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final Map<String, String> additionalProperties,
                                   final String prefix,
                                   final String domain,
                                   final Duration step,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validateNull(
                    prefix,
                    PREFIX_PROPERTY_NULL,
                    PREFIX_PROPERTY,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    domain,
                    DOMAIN_NULL,
                    DOMAIN,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validateNull(
                    step,
                    STEP_NULL,
                    STEP,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            if (step.isNegative() || step.isZero()) {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate(STEP_NOT_POSITIVE)
                            .addPropertyNode(STEP)
                            .addConstraintViolation();
                    valid = false;
                } else {
                    throw new AppbaseRuntimeException(STEP_NOT_POSITIVE);
                }
            }
            if (!valid) {
                return valid;
            }
        }

        return true;
    }
}
