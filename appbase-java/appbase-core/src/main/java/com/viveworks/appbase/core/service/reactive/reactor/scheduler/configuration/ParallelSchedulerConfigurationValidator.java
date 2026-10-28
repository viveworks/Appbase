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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfigurationValidator;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ParallelSchedulerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ParallelSchedulerConfigurationValidator
        implements ConstraintValidator<ParallelSchedulerConfiguration.Valid, ParallelSchedulerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ParallelSchedulerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ParallelSchedulerConfiguration.class);

    private static final String ID = "id";
    private static final String THREAD_FACTORY_CONFIGURATION = "threadFactoryConfiguration";
    private static final String PARALLELISM = "parallelism";

    private static final String ID_BLANK =
            ValidationService.concat(PREFIX, ID, ValidationService.BLANK);
    private static final String THREAD_FACTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, THREAD_FACTORY_CONFIGURATION, ValidationService.BLANK);
    private static final String PARALLELISM_NOT_POSITIVE =
            ValidationService.concat(PREFIX, PARALLELISM, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public ParallelSchedulerConfigurationValidator() {
        this(null);
    }

    public ParallelSchedulerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ParallelSchedulerConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getId(),
                    value.getThreadFactoryConfiguration(),
                    value.getParallelism(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link ParallelSchedulerConfiguration} properties.
     *
     * @param id {@link ParallelSchedulerConfiguration#getId()}
     * @param threadFactoryConfiguration {@link ParallelSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @param parallelism {@link ParallelSchedulerConfiguration#getParallelism()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final String id,
                                final ThreadFactoryConfiguration threadFactoryConfiguration,
                                final int parallelism) throws AppbaseRuntimeException
    {
        validate(id, threadFactoryConfiguration, parallelism, null, null);
    }

    /**
     * Validate {@link ParallelSchedulerConfiguration} properties.
     *
     * @param id {@link ParallelSchedulerConfiguration#getId()}
     * @param threadFactoryConfiguration {@link ParallelSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @param parallelism {@link ParallelSchedulerConfiguration#getParallelism()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final String id,
                                   final ThreadFactoryConfiguration threadFactoryConfiguration,
                                   final int parallelism,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateBlank(
                id,
                ID_BLANK,
                ID,
                NODE_NAME_PREFIX,
                validationService,
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

        valid = ThreadFactoryConfigurationValidator.validate(
                threadFactoryConfiguration.getNameFormat(),
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                parallelism,
                PARALLELISM_NOT_POSITIVE,
                PARALLELISM,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
