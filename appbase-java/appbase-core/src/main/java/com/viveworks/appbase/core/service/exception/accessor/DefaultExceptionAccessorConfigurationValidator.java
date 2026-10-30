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

package com.viveworks.appbase.core.service.exception.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * JSR 303 validator for {@link DefaultExceptionAccessorConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultExceptionAccessorConfigurationValidator
        implements ConstraintValidator
        <DefaultExceptionAccessorConfiguration.Valid, DefaultExceptionAccessorConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultExceptionAccessorConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultExceptionAccessorConfiguration.class);

    private static final String EXCEPTION_STORE_CONFIGURATIONS = "exceptionStoreConfigurations";
    private static final String EXCEPTION_STORE_CONFIGURATIONS_ITEM =
            "exceptionStoreConfigurations" + ValidationService.MESSAGE_TEMPLATE_SEPARATOR + ValidationService.ITEM;

    private static final String EXCEPTION_STORE_CONFIGURATIONS_NULL =
            ValidationService.concat(PREFIX, EXCEPTION_STORE_CONFIGURATIONS, ValidationService.NULL);
    private static final String EXCEPTION_STORE_CONFIGURATIONS_ITEM_NULL =
            ValidationService.concat(PREFIX, EXCEPTION_STORE_CONFIGURATIONS_ITEM, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultExceptionAccessorConfigurationValidator() {
        this(null);
    }

    public DefaultExceptionAccessorConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultExceptionAccessorConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.getExceptionStoreConfigurations(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultExceptionAccessorConfiguration} properties.
     *
     * @param exceptionStoreConfigurations
     *      {@link DefaultExceptionAccessorConfiguration#getExceptionStoreConfigurations()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final List<ExceptionStoreConfiguration> exceptionStoreConfigurations) throws AppbaseRuntimeException
    {
        validate(exceptionStoreConfigurations, null, null);
    }

    /**
     * Validate {@link DefaultExceptionAccessorConfiguration} properties.
     *
     * @param exceptionStoreConfigurations
     *      {@link DefaultExceptionAccessorConfiguration#getExceptionStoreConfigurations()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final List<ExceptionStoreConfiguration> exceptionStoreConfigurations,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        final boolean valid;

        valid = ValidationService.validateNull(
                exceptionStoreConfigurations,
                EXCEPTION_STORE_CONFIGURATIONS_NULL,
                EXCEPTION_STORE_CONFIGURATIONS,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        for (final ExceptionStoreConfiguration configuration : exceptionStoreConfigurations) {
            if (configuration == null) {
                return ValidationService.validateNull(
                        exceptionStoreConfigurations,
                        EXCEPTION_STORE_CONFIGURATIONS_ITEM_NULL,
                        EXCEPTION_STORE_CONFIGURATIONS_ITEM,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
            }
        }

        return valid;
    }
}
