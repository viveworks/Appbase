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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultMediatorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultMediatorServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultMediatorServiceConfiguration.Valid, DefaultMediatorServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultMediatorServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultMediatorServiceConfiguration.class);

    private static final String MESSAGE_OBJECT_POOL_CAPACITY = "messageObjectPoolCapacity";
    private static final String REMOVED_MESSAGE_QUEUE_CAPACITY = "removedMessageQueueCapacity";
    private static final String APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY = "appbaseRuntimeExceptionQueueCapacity";
    private static final String DEFAULT_MESSAGE_QUEUES_CONFIGURATION = "defaultMessageQueuesConfiguration";
    private static final String SHARED_EXECUTOR_SERVICE_CONFIGURATION = "sharedExecutorServiceConfiguration";
    private static final String SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION =
            "sharedScheduledExecutorServiceConfiguration";

    private static final String OBJECT_POOL_CAPACITY_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MESSAGE_OBJECT_POOL_CAPACITY, ValidationService.NOT_POSITIVE);
    private static final String REMOVED_MESSAGE_QUEUE_CAPACITY_NOT_POSITIVE =
            ValidationService.concat(PREFIX, REMOVED_MESSAGE_QUEUE_CAPACITY, ValidationService.NOT_POSITIVE);
    private static final String APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY_NOT_POSITIVE =
            ValidationService.concat(PREFIX, APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY, ValidationService.NOT_POSITIVE);
    private static final String DEFAULT_MESSAGE_QUEUES_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, DEFAULT_MESSAGE_QUEUES_CONFIGURATION, ValidationService.NULL);
    private static final String SHARED_EXECUTOR_SERVICE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, SHARED_EXECUTOR_SERVICE_CONFIGURATION, ValidationService.NULL);
    private static final String SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultMediatorServiceConfigurationValidator() {
        this(null);
    }

    public DefaultMediatorServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultMediatorServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getMessageObjectPoolCapacity(),
                    value.getRemovedMessageQueueCapacity(),
                    value.getAppbaseRuntimeExceptionQueueCapacity(),
                    value.getDefaultMessageQueuesConfiguration(),
                    value.getSharedExecutorServiceConfiguration(),
                    value.getSharedScheduledExecutorServiceConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultMediatorServiceConfiguration} properties.
     *
     * @param messageObjectPoolCapacity
     *      {@link DefaultMediatorServiceConfiguration#getMessageObjectPoolCapacity()}
     * @param removedMessageQueueCapacity
     *      {@link DefaultMediatorServiceConfiguration#getRemovedMessageQueueCapacity()}
     * @param appbaseRuntimeExceptionQueueCapacity
     *      {@link DefaultMediatorServiceConfiguration#getAppbaseRuntimeExceptionQueueCapacity()}
     * @param defaultMessageQueuesConfiguration
     *      {@link DefaultMediatorServiceConfiguration#getDefaultMessageQueuesConfiguration()}
     * @param sharedExecutorServiceConfiguration
     *      {@link DefaultMediatorServiceConfiguration#getSharedExecutorServiceConfiguration()}
     * @param sharedScheduledExecutorServiceConfiguration
     *      {@link DefaultMediatorServiceConfiguration#getSharedScheduledExecutorServiceConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final int messageObjectPoolCapacity,
                                final int removedMessageQueueCapacity,
                                final int appbaseRuntimeExceptionQueueCapacity,
                                final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration,
                                final ExecutorServiceConfiguration sharedExecutorServiceConfiguration,
                                final ScheduledExecutorServiceConfiguration sharedScheduledExecutorServiceConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(messageObjectPoolCapacity,
                removedMessageQueueCapacity,
                appbaseRuntimeExceptionQueueCapacity,
                defaultMessageQueuesConfiguration,
                sharedExecutorServiceConfiguration,
                sharedScheduledExecutorServiceConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link DefaultMediatorServiceConfiguration} properties.
     *
     * @param messageObjectPoolCapacity
     *      {@link DefaultMediatorServiceConfiguration#getMessageObjectPoolCapacity()}
     * @param removedMessageQueueCapacity
     *      {@link DefaultMediatorServiceConfiguration#getRemovedMessageQueueCapacity()}
     * @param appbaseRuntimeExceptionQueueCapacity
     *      {@link DefaultMediatorServiceConfiguration#getAppbaseRuntimeExceptionQueueCapacity()}
     * @param defaultMessageQueuesConfiguration
     *      {@link DefaultMediatorServiceConfiguration#getDefaultMessageQueuesConfiguration()}
     * @param sharedExecutorServiceConfiguration
     *      {@link DefaultMediatorServiceConfiguration#getSharedExecutorServiceConfiguration()}
     * @param sharedScheduledExecutorServiceConfiguration
     *      {@link DefaultMediatorServiceConfiguration#getSharedScheduledExecutorServiceConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final int messageObjectPoolCapacity,
            final int removedMessageQueueCapacity,
            final int appbaseRuntimeExceptionQueueCapacity,
            final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration,
            final ExecutorServiceConfiguration sharedExecutorServiceConfiguration,
            final ScheduledExecutorServiceConfiguration sharedScheduledExecutorServiceConfiguration,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validatePositive(
                messageObjectPoolCapacity,
                OBJECT_POOL_CAPACITY_NOT_POSITIVE,
                MESSAGE_OBJECT_POOL_CAPACITY,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                removedMessageQueueCapacity,
                REMOVED_MESSAGE_QUEUE_CAPACITY_NOT_POSITIVE,
                REMOVED_MESSAGE_QUEUE_CAPACITY,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                appbaseRuntimeExceptionQueueCapacity,
                APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY_NOT_POSITIVE,
                APPBASE_RUNTIME_EXCEPTION_QUEUE_CAPACITY,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                defaultMessageQueuesConfiguration,
                DEFAULT_MESSAGE_QUEUES_CONFIGURATION_NULL,
                DEFAULT_MESSAGE_QUEUES_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                sharedExecutorServiceConfiguration,
                SHARED_EXECUTOR_SERVICE_CONFIGURATION_NULL,
                SHARED_EXECUTOR_SERVICE_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                sharedScheduledExecutorServiceConfiguration,
                SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION_NULL,
                SHARED_SCHEDULED_EXECUTOR_SERVICE_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
