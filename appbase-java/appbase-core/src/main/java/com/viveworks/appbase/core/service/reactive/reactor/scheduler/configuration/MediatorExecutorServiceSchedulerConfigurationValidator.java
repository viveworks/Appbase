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
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link MediatorExecutorServiceSchedulerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MediatorExecutorServiceSchedulerConfigurationValidator
        implements ConstraintValidator
        <MediatorExecutorServiceSchedulerConfiguration.Valid, MediatorExecutorServiceSchedulerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(MediatorExecutorServiceSchedulerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(MediatorExecutorServiceSchedulerConfiguration.class);

    private static final String ID = "id";
    private static final String EXECUTOR_NAME = "executorName";

    private static final String ID_BLANK =
            ValidationService.concat(PREFIX, ID, ValidationService.BLANK);
    private static final String EXECUTOR_NAME_BLANK =
            ValidationService.concat(PREFIX, EXECUTOR_NAME, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public MediatorExecutorServiceSchedulerConfigurationValidator() {
        this(null);
    }

    public MediatorExecutorServiceSchedulerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final MediatorExecutorServiceSchedulerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(value.getId(), value.getExecutorName(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link MediatorExecutorServiceSchedulerConfiguration} properties.
     *
     * @param id {@link MediatorExecutorServiceSchedulerConfiguration#getId()}
     * @param executorName {@link MediatorExecutorServiceSchedulerConfiguration#getExecutorName()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final String id,
                                final String executorName) throws AppbaseRuntimeException
    {
        validate(id, executorName, null, null);
    }

    /**
     * Validate {@link MediatorExecutorServiceSchedulerConfiguration} properties.
     *
     * @param id {@link MediatorExecutorServiceSchedulerConfiguration#getId()}
     * @param executorName {@link MediatorExecutorServiceSchedulerConfiguration#getExecutorName()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final String id,
                                   final String executorName,
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

        valid = ValidationService.validateBlank(
                executorName,
                EXECUTOR_NAME_BLANK,
                EXECUTOR_NAME,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
