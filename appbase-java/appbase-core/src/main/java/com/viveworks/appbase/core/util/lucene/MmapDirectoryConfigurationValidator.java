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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link MmapDirectoryConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MmapDirectoryConfigurationValidator
        implements ConstraintValidator
        <MmapDirectoryConfiguration.Valid, MmapDirectoryConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(MmapDirectoryConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(MmapDirectoryConfiguration.class);

    private static final String MAX_CHUNK_SIZE = "maxChunkSize";
    private static final String MAX_CHUNK_SIZE_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MAX_CHUNK_SIZE, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public MmapDirectoryConfigurationValidator() {
        this(null);
    }

    public MmapDirectoryConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final MmapDirectoryConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(value.getMaxChunkSize(), validationService, context);
        }

        return false;
    }

    /**
     * Validate {@link MmapDirectoryConfiguration} properties.
     *
     * @param maxChunkSize {@link MmapDirectoryConfiguration#getMaxChunkSize()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final long maxChunkSize) throws AppbaseRuntimeException {
        validate(maxChunkSize, null, null);
    }

    /**
     * Validate {@link MmapDirectoryConfiguration} properties.
     *
     * @param maxChunkSize {@link MmapDirectoryConfiguration#getMaxChunkSize()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final long maxChunkSize,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        return ValidationService.validatePositive(
                maxChunkSize,
                MAX_CHUNK_SIZE_NOT_POSITIVE,
                MAX_CHUNK_SIZE,
                NODE_NAME_PREFIX,
                validationService,
                context);
    }
}
