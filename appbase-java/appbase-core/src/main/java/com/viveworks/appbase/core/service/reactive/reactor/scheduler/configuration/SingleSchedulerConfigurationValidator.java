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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfigurationValidator;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link SingleSchedulerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SingleSchedulerConfigurationValidator
        implements ConstraintValidator
        <SingleSchedulerConfiguration.Valid, SingleSchedulerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(SingleSchedulerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(SingleSchedulerConfiguration.class);

    private static final String ID = "id";
    private static final String THREAD_FACTORY_CONFIGURATION = "threadFactoryConfiguration";

    private static final String ID_BLANK =
            ValidationService.concat(PREFIX, ID, ValidationService.BLANK);
    private static final String THREAD_FACTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, THREAD_FACTORY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public SingleSchedulerConfigurationValidator() {
        this(null);
    }

    public SingleSchedulerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final SingleSchedulerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.getId(),
                    value.getThreadFactoryConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link SingleSchedulerConfiguration} properties.
     *
     * @param id {@link SingleSchedulerConfiguration#getId()}
     * @param threadFactoryConfiguration {@link SingleSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final String id,
            final ThreadFactoryConfiguration threadFactoryConfiguration) throws AppbaseRuntimeException
    {
        validate(id, threadFactoryConfiguration, null, null);
    }

    /**
     * Validate {@link SingleSchedulerConfiguration} properties.
     *
     * @param id {@link SingleSchedulerConfiguration#getId()}
     * @param threadFactoryConfiguration {@link SingleSchedulerConfiguration#getThreadFactoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final String id,
            final ThreadFactoryConfiguration threadFactoryConfiguration,
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

        return valid;
    }
}
