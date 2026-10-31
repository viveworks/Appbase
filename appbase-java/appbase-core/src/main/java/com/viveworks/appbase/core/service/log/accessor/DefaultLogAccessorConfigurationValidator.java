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

package com.viveworks.appbase.core.service.log.accessor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * JSR 303 validator for {@link DefaultLogAccessorConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLogAccessorConfigurationValidator
        implements ConstraintValidator<DefaultLogAccessorConfiguration.Valid, DefaultLogAccessorConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultLogAccessorConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultLogAccessorConfiguration.class);

    private static final String LOG_STORE_CONFIGURATIONS = "logStoreConfigurations";
    private static final String LOG_STORE_CONFIGURATIONS_ITEM =
            LOG_STORE_CONFIGURATIONS + ValidationService.MESSAGE_TEMPLATE_SEPARATOR + ValidationService.ITEM;

    private static final String LOG_STORE_CONFIGURATIONS_NULL =
            ValidationService.concat(PREFIX, LOG_STORE_CONFIGURATIONS, ValidationService.NULL);
    private static final String LOG_STORE_CONFIGURATIONS_ITEM_NULL =
            ValidationService.concat(PREFIX, LOG_STORE_CONFIGURATIONS_ITEM, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultLogAccessorConfigurationValidator() {
        this(null);
    }

    public DefaultLogAccessorConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultLogAccessorConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getLogStoreConfigurations(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultLogAccessorConfiguration} properties.
     *
     * @param enabled {@link DefaultLogAccessorConfiguration#isEnabled()}
     * @param logStoreConfigurations {@link DefaultLogAccessorConfiguration#getLogStoreConfigurations()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final boolean enabled,
            final List<LogStoreConfiguration> logStoreConfigurations) throws AppbaseRuntimeException
    {
        validate(enabled, logStoreConfigurations, null, null);
    }

    /**
     * Validate {@link DefaultLogAccessorConfiguration} properties.
     *
     * @param enabled {@link DefaultLogAccessorConfiguration#isEnabled()}
     * @param logStoreConfigurations {@link DefaultLogAccessorConfiguration#getLogStoreConfigurations()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final boolean enabled,
            final List<LogStoreConfiguration> logStoreConfigurations,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validateNull(
                    logStoreConfigurations,
                    LOG_STORE_CONFIGURATIONS_NULL,
                    LOG_STORE_CONFIGURATIONS,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);

            for (final LogStoreConfiguration logStoreConfiguration : logStoreConfigurations) {
                valid = ValidationService.validateNull(
                        logStoreConfiguration,
                        LOG_STORE_CONFIGURATIONS_ITEM_NULL,
                        LOG_STORE_CONFIGURATIONS_ITEM,
                        NODE_NAME_PREFIX,
                        validationService,
                        context);
                if (!valid) {
                    return valid;
                }
            }
        }

        return true;
    }
}
