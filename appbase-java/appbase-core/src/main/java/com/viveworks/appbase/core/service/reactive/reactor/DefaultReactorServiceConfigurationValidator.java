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

package com.viveworks.appbase.core.service.reactive.reactor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.BoundedElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ImmediateSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ParallelSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SingleSchedulerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * JSR 303 validator for {@link DefaultReactorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultReactorServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultReactorServiceConfiguration.Valid, DefaultReactorServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultReactorServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultReactorServiceConfiguration.class);

    private static final String SCHEDULER_CONFIGURATIONS = "schedulerConfigurations";
    private static final String SCHEDULER_CONFIGURATIONS_ITEM =
            "schedulerConfigurations" + ValidationService.MESSAGE_TEMPLATE_SEPARATOR + ValidationService.ITEM;

    private static final String SCHEDULER_CONFIGURATIONS_ITEM_NULL =
            ValidationService.concat(PREFIX, SCHEDULER_CONFIGURATIONS_ITEM, "null");
    private static final String SCHEDULER_CONFIGURATIONS_ITEM_UNSUPPORTED =
            ValidationService.concat(PREFIX, SCHEDULER_CONFIGURATIONS_ITEM, "unsupported");

    @Nullable
    private final ValidationService validationService;

    public DefaultReactorServiceConfigurationValidator() {
        this(null);
    }

    public DefaultReactorServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultReactorServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.isEnabled(), value.getSchedulerConfigurations(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultReactorServiceConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultReactorServiceConfiguration#isEnabled()}
     * @param schedulerConfigurations
     *      {@link DefaultReactorServiceConfiguration#getSchedulerConfigurations()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final List<SchedulerConfiguration> schedulerConfigurations)
                                throws AppbaseRuntimeException
    {
        validate(enabled, schedulerConfigurations, null, null);
    }

    /**
     * Validate {@link DefaultReactorServiceConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultReactorServiceConfiguration#isEnabled()}
     * @param schedulerConfigurations
     *      {@link DefaultReactorServiceConfiguration#getSchedulerConfigurations()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final List<SchedulerConfiguration> schedulerConfigurations,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled && schedulerConfigurations != null) {
            for (final SchedulerConfiguration schedulerConfiguration : schedulerConfigurations) {
                if (schedulerConfiguration == null) {
                    return ValidationService.validateNull(
                            schedulerConfiguration,
                            SCHEDULER_CONFIGURATIONS_ITEM_NULL,
                            SCHEDULER_CONFIGURATIONS_ITEM,
                            NODE_NAME_PREFIX,
                            validationService,
                            context);
                }

                boolean validType = false;
                final Class<? extends SchedulerConfiguration> type = schedulerConfiguration.getClass();
                if (type.isAssignableFrom(BoundedElasticSchedulerConfiguration.class) ||
                        type.isAssignableFrom(ElasticSchedulerConfiguration.class) ||
                        type.isAssignableFrom(ExecutorServiceSchedulerConfiguration.class) ||
                        type.isAssignableFrom(ImmediateSchedulerConfiguration.class) ||
                        type.isAssignableFrom(MediatorExecutorServiceSchedulerConfiguration.class) ||
                        type.isAssignableFrom(ParallelSchedulerConfiguration.class) ||
                        type.isAssignableFrom(SingleSchedulerConfiguration.class))
                {
                    validType = true;
                }

                if (!validType) {
                    ValidationService.addViolationOrThrow(
                            SCHEDULER_CONFIGURATIONS_ITEM_UNSUPPORTED,
                            SCHEDULER_CONFIGURATIONS_ITEM,
                            context);
                    return false;
                }
            }
        }

        return true;
    }
}
