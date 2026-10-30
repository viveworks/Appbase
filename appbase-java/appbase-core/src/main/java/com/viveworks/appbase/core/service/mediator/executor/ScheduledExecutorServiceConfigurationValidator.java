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

package com.viveworks.appbase.core.service.mediator.executor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ScheduledExecutorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ScheduledExecutorServiceConfigurationValidator
        implements ConstraintValidator
        <ScheduledExecutorServiceConfiguration.Valid, ScheduledExecutorServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ScheduledExecutorServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ScheduledExecutorServiceConfiguration.class);

    private static final String SCHEDULED_EXECUTOR_SERVICE_TYPE = "scheduledExecutorServiceType";
    private static final String THREAD_POOL_SIZE = "threadPoolSize";
    private static final String THREAD_FACTORY_CONFIGURATION = "threadFactoryConfiguration";

    private static final String SCHEDULED_EXECUTOR_SERVICE_TYPE_NULL =
            ValidationService.concat(PREFIX, SCHEDULED_EXECUTOR_SERVICE_TYPE, ValidationService.NULL);
    private static final String THREAD_POOL_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, THREAD_POOL_SIZE, ValidationService.NOT_POSITIVE);
    private static final String THREAD_FACTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, THREAD_FACTORY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public ScheduledExecutorServiceConfigurationValidator() {
        this(null);
    }

    public ScheduledExecutorServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ScheduledExecutorServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getScheduledExecutorServiceType(),
                    value.getThreadPoolSize(),
                    value.getThreadFactoryConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link ScheduledExecutorServiceConfiguration} properties.
     *
     * @param scheduledExecutorServiceType
     *      {@link ScheduledExecutorServiceConfiguration#getScheduledExecutorServiceType()}
     * @param threadPoolSize
     *      {@link ScheduledExecutorServiceConfiguration#getThreadPoolSize()}
     * @param threadFactoryConfiguration
     *      {@link ScheduledExecutorServiceConfiguration#getThreadFactoryConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final ScheduledExecutorServiceType scheduledExecutorServiceType,
            final int threadPoolSize,
            final ThreadFactoryConfiguration threadFactoryConfiguration) throws AppbaseRuntimeException
    {
        validate(scheduledExecutorServiceType, threadPoolSize, threadFactoryConfiguration, null, null);
    }

    /**
     * Validate {@link ScheduledExecutorServiceConfiguration} properties.
     *
     * @param scheduledExecutorServiceType
     *      {@link ScheduledExecutorServiceConfiguration#getScheduledExecutorServiceType()}
     * @param threadPoolSize
     *      {@link ScheduledExecutorServiceConfiguration#getThreadPoolSize()}
     * @param threadFactoryConfiguration
     *      {@link ScheduledExecutorServiceConfiguration#getThreadFactoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final ScheduledExecutorServiceType scheduledExecutorServiceType,
            final int threadPoolSize,
            final ThreadFactoryConfiguration threadFactoryConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateEnumNull(
                scheduledExecutorServiceType,
                SCHEDULED_EXECUTOR_SERVICE_TYPE_NULL,
                SCHEDULED_EXECUTOR_SERVICE_TYPE,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                threadFactoryConfiguration,
                THREAD_FACTORY_CONFIGURATION_NULL,
                THREAD_FACTORY_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        if (scheduledExecutorServiceType.equals(ScheduledExecutorServiceType.THREAD_POOL)) {
            valid = ValidationService.validatePositive(
                    threadPoolSize,
                    THREAD_POOL_SIZE_NOT_POSITIVE,
                    THREAD_POOL_SIZE,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }
        }

        return valid;
    }
}
