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

package com.viveworks.appbase.core.service.reactive.read;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ReactiveReadBoundaryConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveReadBoundaryConfigurationValidator
        implements ConstraintValidator<ReactiveReadBoundaryConfiguration.Valid, ReactiveReadBoundaryConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(ReactiveReadBoundaryConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ReactiveReadBoundaryConfiguration.class);

    private static final String MAX_PAGE_SIZE = "maxPageSize";
    private static final String OFFLOAD_TIMEOUT_SECONDS = "offloadTimeoutSeconds";

    private static final String MAX_PAGE_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MAX_PAGE_SIZE, ValidationService.NOT_POSITIVE);
    private static final String OFFLOAD_TIMEOUT_SECONDS_NOT_POSITIVE =
            ValidationService.concat(PREFIX, OFFLOAD_TIMEOUT_SECONDS, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public ReactiveReadBoundaryConfigurationValidator() {
        this(null);
    }

    public ReactiveReadBoundaryConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ReactiveReadBoundaryConfiguration value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(
                    value.getMaxPageSize(),
                    value.getOffloadTimeoutSeconds(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link ReactiveReadBoundaryConfiguration} properties.
     *
     * @param maxPageSize
     *      {@link ReactiveReadBoundaryConfiguration#getMaxPageSize()}
     * @param offloadTimeoutSeconds
     *      {@link ReactiveReadBoundaryConfiguration#getOffloadTimeoutSeconds()}
     * @throws AppbaseRuntimeException if invalid
     */
    public static void validate(
            final int maxPageSize,
            final int offloadTimeoutSeconds) throws AppbaseRuntimeException
    {
        validate(maxPageSize, offloadTimeoutSeconds, null, null);
    }

    /**
     * Validate {@link ReactiveReadBoundaryConfiguration} properties.
     *
     * @param maxPageSize
     *      {@link ReactiveReadBoundaryConfiguration#getMaxPageSize()}
     * @param offloadTimeoutSeconds
     *      {@link ReactiveReadBoundaryConfiguration#getOffloadTimeoutSeconds()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(
            final int maxPageSize,
            final int offloadTimeoutSeconds,
            @Nullable final ValidationService validationService,
            @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid = ValidationService.validatePositive(
                maxPageSize,
                MAX_PAGE_SIZE_NOT_POSITIVE,
                MAX_PAGE_SIZE,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                offloadTimeoutSeconds,
                OFFLOAD_TIMEOUT_SECONDS_NOT_POSITIVE,
                OFFLOAD_TIMEOUT_SECONDS,
                NODE_NAME_PREFIX,
                validationService,
                context);

        return valid;
    }
}
