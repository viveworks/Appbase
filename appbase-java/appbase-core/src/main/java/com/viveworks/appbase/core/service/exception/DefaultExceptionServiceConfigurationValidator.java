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

package com.viveworks.appbase.core.service.exception;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessorConfiguration;
import com.viveworks.appbase.core.service.exception.store.DefaultExpiredMessageCleanerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultExceptionServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultExceptionServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultExceptionServiceConfiguration.Valid, DefaultExceptionServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultExceptionServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultExceptionServiceConfiguration.class);

    private static final String DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION =
            "defaultExceptionAccessorConfiguration";
    private static final String DEFAULT_EXPIRED_MESSAGE_CLEANER_CONFIGURATION =
            "defaultExpiredMessageCleanerConfiguration";

    private static final String DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION, ValidationService.NULL);
    private static final String DEFAULT_EXPIRED_MESSAGE_CLEANER_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_EXPIRED_MESSAGE_CLEANER_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultExceptionServiceConfigurationValidator() {
        this(null);
    }

    public DefaultExceptionServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultExceptionServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getDefaultExceptionAccessorConfiguration(),
                    value.getDefaultExpiredMessageCleanerConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate configuration properties.
     *
     * @param defaultExceptionAccessorConfiguration
     *      {@link DefaultExceptionServiceConfiguration#getDefaultExceptionAccessorConfiguration()}
     * @param defaultExpiredMessageCleanerConfiguration
     *      {@link DefaultExceptionServiceConfiguration#getDefaultExpiredMessageCleanerConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration,
            final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration)
            throws AppbaseRuntimeException
    {
        validate(defaultExceptionAccessorConfiguration, defaultExpiredMessageCleanerConfiguration, null, null);
    }

    /**
     * Validate configuration properties.
     *
     * @param defaultExceptionAccessorConfiguration
     *      {@link DefaultExceptionServiceConfiguration#getDefaultExceptionAccessorConfiguration()}
     * @param defaultExpiredMessageCleanerConfiguration
     *      {@link DefaultExceptionServiceConfiguration#getDefaultExpiredMessageCleanerConfiguration()}
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration,
            final DefaultExpiredMessageCleanerConfiguration defaultExpiredMessageCleanerConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateNull(
                defaultExceptionAccessorConfiguration,
                DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION_NULL,
                DEFAULT_EXCEPTION_ACCESSOR_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                defaultExpiredMessageCleanerConfiguration,
                DEFAULT_EXPIRED_MESSAGE_CLEANER_CONFIGURATION_NULL,
                DEFAULT_EXPIRED_MESSAGE_CLEANER_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);

        return valid;
    }
}
