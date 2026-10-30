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

package com.viveworks.appbase.core.util.lucene;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link DefaultLuceneServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultLuceneServiceConfigurationValidator
        implements ConstraintValidator
        <DefaultLuceneServiceConfiguration.Valid, DefaultLuceneServiceConfiguration>
{
    private static final String PREFIX =
            ValidationService.prefix(DefaultLuceneServiceConfiguration.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(DefaultLuceneServiceConfiguration.class);

    private static final String INDEX_BASE_DIRECTORY = "indexBaseDirectory";
    private static final String DIRECTORY_TYPE = "directoryType";
    private static final String MMAP_DIRECTORY_CONFIGURATION = "mmapDirectoryConfiguration";

    private static final String INDEX_BASE_DIRECTORY_BLANK =
            ValidationService.concat(PREFIX, INDEX_BASE_DIRECTORY, ValidationService.BLANK);
    private static final String DIRECTORY_TYPE_NULL =
            ValidationService.concat(PREFIX, DIRECTORY_TYPE, ValidationService.NULL);
    private static final String MMAP_DIRECTORY_CONFIGURATION_NULL =
            ValidationService.concat(PREFIX, MMAP_DIRECTORY_CONFIGURATION, ValidationService.NULL);

    @Nullable
    private final ValidationService validationService;

    public DefaultLuceneServiceConfigurationValidator() {
        this(null);
    }

    public DefaultLuceneServiceConfigurationValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final DefaultLuceneServiceConfiguration value,
                           @Nullable final ConstraintValidatorContext context)
    {
        if (value != null) {
            return validate(
                    value.isEnabled(),
                    value.getIndexBaseDirectory(),
                    value.getDirectoryType(),
                    value.getMmapDirectoryConfiguration(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link DefaultLuceneServiceConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultLuceneServiceConfiguration#isEnabled()}
     * @param indexBaseDirectory
     *      {@link DefaultLuceneServiceConfiguration#getIndexBaseDirectory()}
     * @param directoryType
     *      {@link DefaultLuceneServiceConfiguration#getDirectoryType()}
     * @param mmapDirectoryConfiguration
     *      {@link DefaultLuceneServiceConfiguration#getMmapDirectoryConfiguration()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final boolean enabled,
                                final String indexBaseDirectory,
                                final DirectoryType directoryType,
                                final MmapDirectoryConfiguration mmapDirectoryConfiguration)
                                throws AppbaseRuntimeException
    {
        validate(enabled, indexBaseDirectory, directoryType, mmapDirectoryConfiguration, null, null);
    }

    /**
     * Validate {@link DefaultLuceneServiceConfiguration} properties.
     *
     * @param enabled
     *      {@link DefaultLuceneServiceConfiguration#isEnabled()}
     * @param indexBaseDirectory
     *      {@link DefaultLuceneServiceConfiguration#getIndexBaseDirectory()}
     * @param directoryType
     *      {@link DefaultLuceneServiceConfiguration#getDirectoryType()}
     * @param mmapDirectoryConfiguration
     *      {@link DefaultLuceneServiceConfiguration#getMmapDirectoryConfiguration()}
     * @param validationService Validation service
     * @param context Context
     * @return true if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final boolean enabled,
                                   final String indexBaseDirectory,
                                   final DirectoryType directoryType,
                                   final MmapDirectoryConfiguration mmapDirectoryConfiguration,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context)
                                   throws AppbaseRuntimeException
    {
        if (!enabled) {
            return true;
        }

        boolean valid;

        valid = ValidationService.validateBlank(
                indexBaseDirectory,
                INDEX_BASE_DIRECTORY_BLANK,
                INDEX_BASE_DIRECTORY,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateEnumNull(
                directoryType,
                DIRECTORY_TYPE_NULL,
                DIRECTORY_TYPE,
                context);
        if (!valid) {
            return valid;
        }

        if (directoryType.equals(DirectoryType.MMAP)) {
            valid = ValidationService.validateNull(
                    mmapDirectoryConfiguration,
                    MMAP_DIRECTORY_CONFIGURATION_NULL,
                    MMAP_DIRECTORY_CONFIGURATION,
                    NODE_NAME_PREFIX,
                    validationService,
                    context);
            if (!valid) {
                return valid;
            }
        }

        return valid;
    }
}
