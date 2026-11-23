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
 * JSR 303 validator for {@link LruQueryCacheConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class LruQueryCacheConfigurationValidator
        implements ConstraintValidator
        <LruQueryCacheConfiguration.Valid, LruQueryCacheConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(LruQueryCacheConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(LruQueryCacheConfiguration.class);

    private static final String MAX_QUERIES = "maxQueries";
    private static final String MAX_RAM_BYTES_USED = "maxRamBytesUsed";

    private static final String MAX_QUERIES_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MAX_QUERIES, ValidationService.NOT_POSITIVE);
    private static final String MAX_RAM_BYTES_USED_NOT_POSITIVE =
            ValidationService.concat(PREFIX, MAX_RAM_BYTES_USED, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public LruQueryCacheConfigurationValidator() {
        this(null);
    }

    public LruQueryCacheConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final LruQueryCacheConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getMaxQueries(),
                    value.getMaxRamBytesUsed(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link LruQueryCacheConfiguration} properties.
     *
     * @param enabled
     *      {@link LruQueryCacheConfiguration#isEnabled()}
     * @param maxQueries
     *      {@link LruQueryCacheConfiguration#getMaxQueries()}
     * @param maxRamBytesUsed
     *      {@link LruQueryCacheConfiguration#getMaxRamBytesUsed()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final int maxQueries,
                                final long maxRamBytesUsed) throws AppbaseRuntimeException
    {
        validate(enabled, maxQueries, maxRamBytesUsed, null, null);
    }

    /**
     * Validate {@link LruQueryCacheConfiguration} properties.
     *
     * @param enabled
     *      {@link LruQueryCacheConfiguration#isEnabled()}
     * @param maxQueries
     *      {@link LruQueryCacheConfiguration#getMaxQueries()}
     * @param maxRamBytesUsed
     *      {@link LruQueryCacheConfiguration#getMaxRamBytesUsed()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final int maxQueries,
                                   final long maxRamBytesUsed,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (enabled) {
            boolean valid;

            valid = ValidationService.validatePositive(
                    maxQueries,
                    MAX_QUERIES_NOT_POSITIVE,
                    MAX_QUERIES,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            valid = ValidationService.validatePositive(
                    maxRamBytesUsed,
                    MAX_RAM_BYTES_USED_NOT_POSITIVE,
                    MAX_RAM_BYTES_USED,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }

            return valid;
        }

        return true;
    }
}
