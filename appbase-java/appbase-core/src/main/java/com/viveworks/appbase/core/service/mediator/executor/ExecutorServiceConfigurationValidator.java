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
 * JSR 303 validator for {@link ExecutorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ExecutorServiceConfigurationValidator
        implements ConstraintValidator
        <ExecutorServiceConfiguration.Valid, ExecutorServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ExecutorServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ExecutorServiceConfiguration.class);

    private static final String EXECUTOR_SERVICE_TYPE = "executorServiceType";
    private static final String FIXED_THREAD_POOL_SIZE = "fixedThreadPoolSize";
    private static final String THREAD_FACTORY_CONFIGURATION = "threadFactoryConfiguration";

    private static final String EXECUTOR_SERVICE_TYPE_NULL =
            ValidationService.concat(PREFIX, EXECUTOR_SERVICE_TYPE, ValidationService.NULL);
    private static final String FIXED_THREAD_POOL_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, FIXED_THREAD_POOL_SIZE, ValidationService.NOT_POSITIVE);
    private static final String THREAD_FACTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, THREAD_FACTORY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public ExecutorServiceConfigurationValidator() {
        this(null);
    }

    public ExecutorServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ExecutorServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getExecutorServiceType(),
                    value.getFixedThreadPoolSize(),
                    value.getThreadFactoryConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link ExecutorServiceConfiguration} properties.
     *
     * @param executorServiceType
     *      {@link ExecutorServiceConfiguration#getExecutorServiceType()}
     * @param fixedThreadPoolSize
     *      {@link ExecutorServiceConfiguration#getFixedThreadPoolSize()}
     * @param threadFactoryConfiguration
     *      {@link ExecutorServiceConfiguration#getThreadFactoryConfiguration()} ()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final ExecutorServiceType executorServiceType,
            final int fixedThreadPoolSize,
            final ThreadFactoryConfiguration threadFactoryConfiguration)
            throws AppbaseRuntimeException
    {
        validate(executorServiceType, fixedThreadPoolSize, threadFactoryConfiguration, null, null);
    }

    /**
     * Validate {@link ExecutorServiceConfiguration} properties.
     *
     * @param executorServiceType
     *      {@link ExecutorServiceConfiguration#getExecutorServiceType()}
     * @param fixedThreadPoolSize
     *      {@link ExecutorServiceConfiguration#getFixedThreadPoolSize()}
     * @param threadFactoryConfiguration
     *      {@link ExecutorServiceConfiguration#getThreadFactoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final ExecutorServiceType executorServiceType,
            final int fixedThreadPoolSize,
            final ThreadFactoryConfiguration threadFactoryConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateEnumNull(
                executorServiceType,
                EXECUTOR_SERVICE_TYPE_NULL,
                EXECUTOR_SERVICE_TYPE,
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

        if (executorServiceType.equals(ExecutorServiceType.FIXED)) {
            valid = ValidationService.validatePositive(
                    fixedThreadPoolSize,
                    FIXED_THREAD_POOL_SIZE_NOT_POSITIVE,
                    FIXED_THREAD_POOL_SIZE,
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
