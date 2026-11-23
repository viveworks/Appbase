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

package com.viveworks.appbase.core.util.disruptor.configuration;

import com.lmax.disruptor.dsl.ProducerType;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BusySpinWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.LiteTimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.PhasedBackoffWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.TimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.WaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultDisruptorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDisruptorServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultDisruptorServiceConfiguration.Valid, DefaultDisruptorServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultDisruptorServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultDisruptorServiceConfiguration.class);

    private static final String WAIT_STRATEGY_CONFIGURATION = "waitStrategyConfiguration";
    private static final String EVENT_HANDLER_COUNT = "eventHandlerCount";
    private static final String RING_BUFFER_SIZE = "ringBufferSize";
    private static final String PRODUCER_TYPE = "producerType";
    private static final String THREAD_FACTORY_CONFIGURATION = "threadFactoryConfiguration";

    private static final String WAIT_STRATEGY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, WAIT_STRATEGY_CONFIGURATION, ValidationService.NULL);
    private static final String WAIT_STRATEGY_CONFIGURATION_UNSUPPORTED =
            ValidationService.concat(PREFIX, WAIT_STRATEGY_CONFIGURATION, "unsupported");
    private static final String EVENT_HANDLER_COUNT_NOT_POSITIVE =
            ValidationService.concat(PREFIX, EVENT_HANDLER_COUNT, ValidationService.NOT_POSITIVE);
    private static final String RING_BUFFER_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, RING_BUFFER_SIZE, ValidationService.NOT_POSITIVE);
    private static final String PRODUCER_TYPE_NULL =
            ValidationService.concat(PREFIX, PRODUCER_TYPE, ValidationService.NULL);
    private static final String THREAD_FACTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, THREAD_FACTORY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultDisruptorServiceConfigurationValidator() {
        this(null);
    }

    public DefaultDisruptorServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultDisruptorServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getWaitStrategyConfiguration(),
                    value.getEventHandlerCount(),
                    value.getRingBufferSize(),
                    value.getProducerType(),
                    value.getThreadFactoryConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultDisruptorServiceConfiguration} properties.
     *
     * @param waitStrategyConfiguration
     *      {@link DefaultDisruptorServiceConfiguration#getWaitStrategyConfiguration()}
     * @param eventHandlerCount
     *      {@link DefaultDisruptorServiceConfiguration#getEventHandlerCount()}
     * @param ringBufferSize
     *      {@link DefaultDisruptorServiceConfiguration#getRingBufferSize()}
     * @param producerType
     *      {@link DefaultDisruptorServiceConfiguration#getProducerType()}
     * @param threadFactoryConfiguration
     *      {@link DefaultDisruptorServiceConfiguration#getThreadFactoryConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final WaitStrategyConfiguration waitStrategyConfiguration,
                                final int eventHandlerCount,
                                final int ringBufferSize,
                                final ProducerType producerType,
                                final ThreadFactoryConfiguration threadFactoryConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(waitStrategyConfiguration,
                eventHandlerCount,
                ringBufferSize,
                producerType,
                threadFactoryConfiguration,
                null,
                null);
    }

    /**
     * Validate {@link DefaultDisruptorServiceConfiguration} properties.
     *
     * @param waitStrategyConfiguration
     *      {@link DefaultDisruptorServiceConfiguration#getWaitStrategyConfiguration()}
     * @param eventHandlerCount
     *      {@link DefaultDisruptorServiceConfiguration#getEventHandlerCount()}
     * @param ringBufferSize
     *      {@link DefaultDisruptorServiceConfiguration#getRingBufferSize()}
     * @param producerType
     *      {@link DefaultDisruptorServiceConfiguration#getProducerType()}
     * @param threadFactoryConfiguration
     *      {@link DefaultDisruptorServiceConfiguration#getThreadFactoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final WaitStrategyConfiguration waitStrategyConfiguration,
                                   final int eventHandlerCount,
                                   final int ringBufferSize,
                                   final ProducerType producerType,
                                   final ThreadFactoryConfiguration threadFactoryConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context)
                                   throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateNull(
                waitStrategyConfiguration,
                WAIT_STRATEGY_CONFIGURATION_NULL,
                WAIT_STRATEGY_CONFIGURATION,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        boolean validWaitStrategyConfiguration = false;
        final Class<? extends WaitStrategyConfiguration> waitStrategyConfigurationType =
                waitStrategyConfiguration.getClass();
        if (waitStrategyConfigurationType.isAssignableFrom(BlockingWaitStrategyConfiguration.class) ||
                waitStrategyConfigurationType.isAssignableFrom(BusySpinWaitStrategyConfiguration.class) ||
                waitStrategyConfigurationType.isAssignableFrom(LiteTimeoutBlockingWaitStrategyConfiguration.class) ||
                waitStrategyConfigurationType.isAssignableFrom(PhasedBackoffWaitStrategyConfiguration.class) ||
                waitStrategyConfigurationType.isAssignableFrom(SleepingWaitStrategyConfiguration.class) ||
                waitStrategyConfigurationType.isAssignableFrom(TimeoutBlockingWaitStrategyConfiguration.class) ||
                waitStrategyConfigurationType.isAssignableFrom(YieldingWaitStrategyConfiguration.class))
        {
            validWaitStrategyConfiguration = true;
        }

        if (!validWaitStrategyConfiguration) {
            ValidationService.addViolationOrThrow(
                    WAIT_STRATEGY_CONFIGURATION_UNSUPPORTED,
                    WAIT_STRATEGY_CONFIGURATION,
                    context);
            return false;
        }

        valid = ValidationService.validatePositive(
                eventHandlerCount,
                EVENT_HANDLER_COUNT_NOT_POSITIVE,
                EVENT_HANDLER_COUNT,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                ringBufferSize,
                RING_BUFFER_SIZE_NOT_POSITIVE,
                RING_BUFFER_SIZE,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateEnumNull(
                producerType,
                PRODUCER_TYPE_NULL,
                PRODUCER_TYPE,
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

        return valid;
    }
}
