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

package com.viveworks.appbase.core.service.configuration.util;

import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.PropertyCastException;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration utilities.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ConfigurationUtil {

    private ConfigurationUtil() {
    }

    /**
     * Convert {@link RawConfiguration} to {@link Map} of property key to value.
     *
     * @param rawConfigurations Raw configurations
     * @return Map of property key to value
     */
    public static Map<String, String> rawConfigurations2Properties(
            final Collection<RawConfiguration> rawConfigurations)
    {
        final Map<String, String> newConfigurations = new ConcurrentHashMap<>();

        if (rawConfigurations != null) {
            rawConfigurations.forEach(rawConfiguration -> {
                final String name = rawConfiguration.getName();
                final byte[] value = rawConfiguration.getValue();
                final String stringValue = new String(value, StandardCharsets.UTF_8);

                newConfigurations.put(name, stringValue);
            });
        }

        return newConfigurations;
    }

    /**
     * Convert properties ({@link Map}) to {@link RawConfiguration}s.
     *
     * @param properties Properties
     * @return Raw configurations
     */
    public static Collection<RawConfiguration> properties2RawConfigurations(final Map<String, String> properties) {
        final List<RawConfiguration> rawConfigurations = new ArrayList<>();

        properties.forEach((key, value) -> {
            final RawConfiguration rawConfiguration = RawConfiguration
                    .builder()
                    .name(key)
                    .value(value.getBytes())
                    .mimeType(MimeType.KEY_VALUE)
                    .timestamp(System.currentTimeMillis())
                    .build();
            rawConfigurations.add(rawConfiguration);
        });

        return rawConfigurations;
    }

    public static boolean config(
            final ConfigurationService configurationService,
            final String environmentVariable,
            final String property,
            final boolean defaultValue)
    {
        Boolean result = defaultValue;
        try {
            result = configurationService.getBoolean(environmentVariable);
        } catch (PropertyCastException e) {
            // ignore
        }

        try {
            final Boolean propertyResult = configurationService.getBoolean(property);
            if (propertyResult != null) {
                result = propertyResult;
            }
        } catch (PropertyCastException ex) {
            // ignore
        }

        return result == null ? defaultValue : result;
    }

    /**
     * Throws {@link PermissionDeniedException} unless {@link #config} resolves to {@code true}
     * for the given environment-variable/property pair.
     *
     * @param configurationService the configuration source for both the environment-variable and
     *                              property lookups
     * @param environmentVariable the environment-variable key to check
     * @param property the property key to check; overrides the environment-variable value when set
     * @param defaultValue the value to use when neither source is configured
     * @param operationName the operation name reported in the exception when permission is denied
     * @throws PermissionDeniedException if the resolved value is not {@code true}
     */
    public static void checkPermission(
            final ConfigurationService configurationService,
            final String environmentVariable,
            final String property,
            final boolean defaultValue,
            final String operationName)
    {
        if (!config(
                configurationService,
                environmentVariable,
                property,
                defaultValue))
        {
            throw new PermissionDeniedException(
                    environmentVariable, property, defaultValue, operationName);
        }
    }
}
