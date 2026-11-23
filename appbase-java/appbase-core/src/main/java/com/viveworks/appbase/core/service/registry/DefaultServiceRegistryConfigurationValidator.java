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

package com.viveworks.appbase.core.service.registry;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultServiceRegistryConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultServiceRegistryConfigurationValidator
        implements ConstraintValidator
        <DefaultServiceRegistryConfiguration.Valid, DefaultServiceRegistryConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultServiceRegistryConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultServiceRegistryConfiguration.class);

    private static final String POLLING_SCHEDULABLE_CONFIGURATION = "pollingSchedulableConfiguration";
    private static final String POLLING_SCHEDULABLE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, POLLING_SCHEDULABLE_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultServiceRegistryConfigurationValidator() {
        this(null);
    }

    public DefaultServiceRegistryConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultServiceRegistryConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getAppName(),
                    value.isEnablePolling(),
                    value.isRegisterAsConfigurationListener(),
                    value.getPollingSchedulableConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultServiceRegistryConfiguration} properties.
     *
     * @param appName
     *       {@link DefaultServiceRegistryConfiguration#getAppName()}
     * @param enablePolling
     *      {@link DefaultServiceRegistryConfiguration#isEnablePolling()}
     * @param registerAsConfigurationListener
     *      {@link DefaultServiceRegistryConfiguration#isRegisterAsConfigurationListener()}
     * @param pollingSchedulableConfiguration
     *      {@link DefaultServiceRegistryConfiguration#getPollingSchedulableConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final String appName,
                                final boolean enablePolling,
                                final boolean registerAsConfigurationListener,
                                final SchedulableConfiguration pollingSchedulableConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(appName, enablePolling, registerAsConfigurationListener, pollingSchedulableConfiguration, null, null);
    }

    /**
     * Validate {@link DefaultServiceRegistryConfiguration} properties.
     *
     * @param appName
     *       {@link DefaultServiceRegistryConfiguration#getAppName()}
     * @param enablePolling
     *      {@link DefaultServiceRegistryConfiguration#isEnablePolling()}
     * @param registerAsConfigurationListener
     *      {@link DefaultServiceRegistryConfiguration#isRegisterAsConfigurationListener()}
     * @param pollingSchedulableConfiguration
     *      {@link DefaultServiceRegistryConfiguration#getPollingSchedulableConfiguration()}
     * @param validationService Validation service
     * @param context Validator context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final String appName,
                                   final boolean enablePolling,
                                   final boolean registerAsConfigurationListener,
                                   final SchedulableConfiguration pollingSchedulableConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enablePolling) {
            return ValidationService.validateNull(
                    pollingSchedulableConfiguration,
                    POLLING_SCHEDULABLE_CONFIGURATION_NULL,
                    POLLING_SCHEDULABLE_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
        }

        return true;
    }
}
