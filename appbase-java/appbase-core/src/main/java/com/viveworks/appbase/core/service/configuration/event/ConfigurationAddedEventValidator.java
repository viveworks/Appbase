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

package com.viveworks.appbase.core.service.configuration.event;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.validation.ValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import javax.annotation.Nullable;

/**
 * JSR 303 validator for {@link ConfigurationChangedEvent}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigurationAddedEventValidator
        implements ConstraintValidator
        <ConfigurationAddedEvent.Valid, ConfigurationAddedEvent>
{
    private static final String PREFIX =
            ValidationService.prefix(ConfigurationAddedEvent.class);
    private static final String NODE_NAME_PREFIX =
            ValidationService.nodeNamePrefix(ConfigurationAddedEvent.class);

    private static final String CONFIGURATION_KEY = "configurationKey";
    private static final String CONFIGURATION_VALUE = "configurationValue";
    private static final String TIMESTAMP = "timestamp";

    private static final String CONFIGURATION_KEY_NULL =
            ValidationService.concat(PREFIX, CONFIGURATION_KEY, ValidationService.NULL);
    private static final String CONFIGURATION_VALUE_NULL =
            ValidationService.concat(PREFIX, CONFIGURATION_VALUE, ValidationService.NULL);
    private static final String TIMESTAMP_NOT_POSITIVE =
            ValidationService.concat(PREFIX, TIMESTAMP, ValidationService.NOT_POSITIVE);

    @Nullable
    private final ValidationService validationService;

    public ConfigurationAddedEventValidator() {
        this(null);
    }

    public ConfigurationAddedEventValidator(@Nullable final ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean isValid(final ConfigurationAddedEvent value,
                           @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        if (value != null) {
            return validate(value.getConfigurationKey(),
                    value.getConfigurationValue(),
                    value.getTimestamp(),
                    validationService,
                    context);
        }

        return false;
    }

    /**
     * Validate {@link ConfigurationAddedEvent} properties.
     *
     * @param configurationKey
     *      {@link ConfigurationAddedEvent#getConfigurationKey()}
     * @param configurationValue
     *      {@link ConfigurationAddedEvent#getConfigurationValue()}
     * @param timestamp
     *      {@link ConfigurationAddedEvent#getTimestamp()}
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static void validate(final ConfigurationKey configurationKey,
                                final ConfigurationValue<?> configurationValue,
                                final long timestamp) throws AppbaseRuntimeException
    {
        validate(configurationKey, configurationValue, timestamp, null, null);
    }

    /**
     * Validate {@link ConfigurationAddedEvent} properties.
     *
     * @param configurationKey
     *      {@link ConfigurationAddedEvent#getConfigurationKey()}
     * @param configurationValue
     *      {@link ConfigurationAddedEvent#getConfigurationValue()}
     * @param timestamp
     *      {@link ConfigurationAddedEvent#getTimestamp()}
     * @param validationService Validation service
     * @param context Validator context
     * @return true, if valid, false otherwise
     * @throws AppbaseRuntimeException if invalid and if validation service and context is not null
     */
    public static boolean validate(final ConfigurationKey configurationKey,
                                   final ConfigurationValue<?> configurationValue,
                                   final long timestamp,
                                   @Nullable final ValidationService validationService,
                                   @Nullable final ConstraintValidatorContext context) throws AppbaseRuntimeException
    {
        boolean valid;

        valid = ValidationService.validateNull(
                configurationKey,
                CONFIGURATION_KEY_NULL,
                CONFIGURATION_KEY,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validateNull(
                configurationValue,
                CONFIGURATION_VALUE_NULL,
                CONFIGURATION_VALUE,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        valid = ValidationService.validatePositive(
                timestamp,
                TIMESTAMP_NOT_POSITIVE,
                TIMESTAMP,
                NODE_NAME_PREFIX,
                validationService,
                context);
        if (!valid) {
            return valid;
        }

        return valid;
    }
}
