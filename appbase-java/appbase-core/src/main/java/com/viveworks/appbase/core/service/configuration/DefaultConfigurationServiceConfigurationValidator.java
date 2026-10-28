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

package com.viveworks.appbase.core.service.configuration;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultConfigurationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultConfigurationServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultConfigurationServiceConfiguration.Valid, DefaultConfigurationServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultConfigurationServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultConfigurationServiceConfiguration.class);

    private static final String CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION =
            "configurationAccessorSchedulableConfiguration";
    private static final String CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    /**
     * Creates a validator that performs the local cross-field checks without a nested validation service.
     */
    public DefaultConfigurationServiceConfigurationValidator() {
        this(null);
    }

    /**
     * Creates a validator that also delegates schedule validation when a service is supplied.
     *
     * @param validationService nested validator, or {@code null} to run only local checks
     */
    public DefaultConfigurationServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public boolean isValid(final DefaultConfigurationServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.isEnableSchedulableConfigurationAccessor(),
                    value.getConfigurationAccessorSchedulableConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate configuration properties.
     *
     * @param enableSchedulableConfigurationAccessor
     *      {@link DefaultConfigurationServiceConfiguration#isEnableSchedulableConfigurationAccessor()}
     * @param configurationAccessorSchedulableConfiguration
     *      {@link DefaultConfigurationServiceConfiguration#getConfigurationAccessorSchedulableConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enableSchedulableConfigurationAccessor,
                                final SchedulableConfiguration configurationAccessorSchedulableConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enableSchedulableConfigurationAccessor, configurationAccessorSchedulableConfiguration, null, null);
    }

    /**
     * Validate configuration properties.
     *
     * @param enableSchedulableConfigurationAccessor
     *      {@link DefaultConfigurationServiceConfiguration#isEnableSchedulableConfigurationAccessor()}
     * @param configurationAccessorSchedulableConfiguration
     *      {@link DefaultConfigurationServiceConfiguration#getConfigurationAccessorSchedulableConfiguration()}
     * @param validationService Validation service
     * @param context Validator context
     * @return true, if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enableSchedulableConfigurationAccessor,
                                   final SchedulableConfiguration configurationAccessorSchedulableConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enableSchedulableConfigurationAccessor) {
            return ValidationService.validateNull(
                    configurationAccessorSchedulableConfiguration,
                    CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION_NULL,
                    CONFIGURATION_ACCESSOR_SCHEDULABLE_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
        }

        return true;
    }
}
