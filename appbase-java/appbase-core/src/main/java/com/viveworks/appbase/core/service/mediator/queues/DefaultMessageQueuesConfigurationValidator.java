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

package com.viveworks.appbase.core.service.mediator.queues;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultMessageQueuesConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultMessageQueuesConfigurationValidator
        implements ConstraintValidator
        <DefaultMessageQueuesConfiguration.Valid, DefaultMessageQueuesConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultMessageQueuesConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultMessageQueuesConfiguration.class);

    private static final String MESSAGE_QUEUE_TYPE = "messageQueueType";
    private static final String BOUND_MESSAGE_QUEUE_CAPACITY = "boundMessageQueueCapacity";
    private static final String EXPIRED_MESSAGE_THRESHOLD_MILLIS = "expiredMessageThresholdMillis";
    private static final String MESSAGE_QUEUES_CLEANER_INITIAL_DELAY_MILLIS = "messageQueuesCleanerInitialDelayMillis";
    private static final String MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS = "messageQueuesCleanerIntervalMillis";

    private static final String MESSAGE_QUEUE_TYPE_NULL =
            ValidationService.concat(PREFIX, MESSAGE_QUEUE_TYPE, ValidationService.NULL);
    private static final String BOUND_MESSAGE_QUEUE_CAPACITY_NOT_POSITIVE =
            ValidationService.concat(PREFIX, BOUND_MESSAGE_QUEUE_CAPACITY, ValidationService.NOT_POSITIVE);
    private static final String EXPIRED_MESSAGE_THRESHOLD_MILLIS_NOT_POSITIVE =
            ValidationService.concat(PREFIX, BOUND_MESSAGE_QUEUE_CAPACITY, ValidationService.NOT_POSITIVE);
    private static final String MESSAGE_QUEUES_CLEANER_INITIAL_DELAY_MILLIS_NOT_POSITIVE_OR_ZERO =
            ValidationService.concat(PREFIX, BOUND_MESSAGE_QUEUE_CAPACITY, ValidationService.NOT_POSITIVE_OR_ZERO);
    private static final String MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public DefaultMessageQueuesConfigurationValidator() {
        this(null);
    }

    public DefaultMessageQueuesConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultMessageQueuesConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.getMessageQueueType(),
                    value.getBoundMessageQueueCapacity(),
                    value.isRemoveExpiredMessages(),
                    value.getExpiredMessageThresholdMillis(),
                    value.getMessageQueuesCleanerInitialDelayMillis(),
                    value.getMessageQueuesCleanerIntervalMillis(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultMessageQueuesConfiguration} properties.
     *
     * @param messageQueueType
     *      {@link DefaultMessageQueuesConfiguration#getMessageQueueType()}
     * @param boundMessageQueueCapacity
     *      {@link DefaultMessageQueuesConfiguration#getBoundMessageQueueCapacity()}
     * @param removeExpiredMessages
     *      {@link DefaultMessageQueuesConfiguration#isRemoveExpiredMessages()}
     * @param expiredMessageThresholdMillis
     *      {@link DefaultMessageQueuesConfiguration#getExpiredMessageThresholdMillis()}
     * @param messageQueuesCleanerInitialDelayMillis
     *      {@link DefaultMessageQueuesConfiguration#getMessageQueuesCleanerInitialDelayMillis()}
     * @param messageQueuesCleanerIntervalMillis
     *      {@link DefaultMessageQueuesConfiguration#getMessageQueuesCleanerIntervalMillis()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final MessageQueueType messageQueueType,
                                final int boundMessageQueueCapacity,
                                final boolean removeExpiredMessages,
                                final long expiredMessageThresholdMillis,
                                final long messageQueuesCleanerInitialDelayMillis,
                                final long messageQueuesCleanerIntervalMillis) throws AppbaseRuntimeException
    {
        validate(messageQueueType,
                boundMessageQueueCapacity,
                removeExpiredMessages,
                expiredMessageThresholdMillis,
                messageQueuesCleanerInitialDelayMillis,
                messageQueuesCleanerIntervalMillis,
                null,
                null);
    }

    /**
     * Validate {@link DefaultMessageQueuesConfiguration} properties.
     *
     * @param messageQueueType
     *      {@link DefaultMessageQueuesConfiguration#getMessageQueueType()}
     * @param boundMessageQueueCapacity
     *      {@link DefaultMessageQueuesConfiguration#getBoundMessageQueueCapacity()}
     * @param removeExpiredMessages
     *      {@link DefaultMessageQueuesConfiguration#isRemoveExpiredMessages()}
     * @param expiredMessageThresholdMillis
     *      {@link DefaultMessageQueuesConfiguration#getExpiredMessageThresholdMillis()}
     * @param messageQueuesCleanerInitialDelayMillis
     *      {@link DefaultMessageQueuesConfiguration#getMessageQueuesCleanerInitialDelayMillis()}
     * @param messageQueuesCleanerIntervalMillis
     *      {@link DefaultMessageQueuesConfiguration#getMessageQueuesCleanerIntervalMillis()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final MessageQueueType messageQueueType,
                                   final int boundMessageQueueCapacity,
                                   final boolean removeExpiredMessages,
                                   final long expiredMessageThresholdMillis,
                                   final long messageQueuesCleanerInitialDelayMillis,
                                   final long messageQueuesCleanerIntervalMillis,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateEnumNull(
                messageQueueType,
                MESSAGE_QUEUE_TYPE_NULL,
                MESSAGE_QUEUE_TYPE,
                context);
        if (!valid) {
            return valid;
        }

        if (messageQueueType.equals(MessageQueueType.BOUND)) {
            valid = ValidationService.validatePositive(
                    boundMessageQueueCapacity,
                    BOUND_MESSAGE_QUEUE_CAPACITY_NOT_POSITIVE,
                    BOUND_MESSAGE_QUEUE_CAPACITY,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }
        }

        if (removeExpiredMessages) {
            valid = ValidationService.validatePositive(
                    expiredMessageThresholdMillis,
                    EXPIRED_MESSAGE_THRESHOLD_MILLIS_NOT_POSITIVE,
                    EXPIRED_MESSAGE_THRESHOLD_MILLIS,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositiveOrZero(
                    messageQueuesCleanerInitialDelayMillis,
                    MESSAGE_QUEUES_CLEANER_INITIAL_DELAY_MILLIS_NOT_POSITIVE_OR_ZERO,
                    MESSAGE_QUEUES_CLEANER_INITIAL_DELAY_MILLIS,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositive(
                    messageQueuesCleanerIntervalMillis,
                    MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS_NOT_POSITIVE,
                    MESSAGE_QUEUES_CLEANER_INTERVAL_MILLIS,
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
