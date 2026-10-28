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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ThreadFactoryConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ThreadFactoryConfigurationValidator
        implements ConstraintValidator
        <ThreadFactoryConfiguration.Valid, ThreadFactoryConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ThreadFactoryConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ThreadFactoryConfiguration.class);

    private static final String NAME_FORMAT = "nameFormat";

    private static final String NAME_FORMAT_BLANK =
            ValidationService.concat(PREFIX, NAME_FORMAT, ValidationService.BLANK);

    @Nullable
    private final ValidationService validationService;

    public ThreadFactoryConfigurationValidator() {
        this(null);
    }

    public ThreadFactoryConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ThreadFactoryConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.getNameFormat(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link ThreadFactoryConfiguration} properties.
     *
     * @param nameFormat
     *      {@link ThreadFactoryConfiguration#getNameFormat()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(
            final String nameFormat) throws AppbaseRuntimeException
    {
        validate(nameFormat, null, null);
    }

    /**
     * Validate {@link ThreadFactoryConfiguration} properties.
     *
     * @param nameFormat
     *      {@link ThreadFactoryConfiguration#getNameFormat()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final String nameFormat,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return ValidationService.validateBlank(
                nameFormat,
                NAME_FORMAT_BLANK,
                NAME_FORMAT,
                NODE_NAME_PREFIX,
                validationService,
                context);
    }
}
