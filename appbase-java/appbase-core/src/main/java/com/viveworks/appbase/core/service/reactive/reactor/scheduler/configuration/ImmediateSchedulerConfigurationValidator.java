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
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ImmediateSchedulerConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ImmediateSchedulerConfigurationValidator
        implements ConstraintValidator
        <ImmediateSchedulerConfiguration.Valid, ImmediateSchedulerConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ImmediateSchedulerConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ImmediateSchedulerConfiguration.class);

    private static final String ID = "id";
    private static final String ID_BLANK = ValidationService.concat(PREFIX, ID, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public ImmediateSchedulerConfigurationValidator() {
        this(null);
    }

    public ImmediateSchedulerConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ImmediateSchedulerConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(value.getId(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link ImmediateSchedulerConfiguration} properties.
     *
     * @param id {@link ImmediateSchedulerConfiguration#getId()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final String id) throws AppbaseRuntimeException {
        validate(id, null, null);
    }

    /**
     * Validate {@link ImmediateSchedulerConfiguration} properties.
     *
     * @param id {@link ImmediateSchedulerConfiguration#getId()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final String id,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return ValidationService.validateBlank(
                id,
                ID_BLANK,
                ID,
                NODE_NAME_PREFIX,
                validationService,
                context);
    }
}
