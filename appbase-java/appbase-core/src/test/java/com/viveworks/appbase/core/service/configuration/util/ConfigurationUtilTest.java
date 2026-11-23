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

import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationUtilTest {
    @Test
    void config_environmentValueSurvivesAbsentProperty() throws Exception {
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getBoolean("ENV_KEY")).thenReturn(Boolean.TRUE);
        when(configurationService.getBoolean("property.key")).thenReturn(null);

        Assertions.assertTrue(ConfigurationUtil.config(
                configurationService, "ENV_KEY", "property.key", false));
    }

    @Test
    void config_propertyValueOverridesEnvironmentValue() throws Exception {
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getBoolean("ENV_KEY")).thenReturn(Boolean.TRUE);
        when(configurationService.getBoolean("property.key")).thenReturn(Boolean.FALSE);

        Assertions.assertFalse(ConfigurationUtil.config(
                configurationService, "ENV_KEY", "property.key", true));
    }

    @Test
    void config_bothValuesAbsentUsesDefault() throws Exception {
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getBoolean("ENV_KEY")).thenReturn(null);
        when(configurationService.getBoolean("property.key")).thenReturn(null);

        Assertions.assertTrue(ConfigurationUtil.config(
                configurationService, "ENV_KEY", "property.key", true));
    }

    @Test
    void testRawConfigurationsToProperties() {
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name("name")
                .value("value".getBytes())
                .mimeType(MimeType.JSON)
                .timestamp(System.currentTimeMillis())
                .build();
        final Collection<RawConfiguration> rawConfigurations = List.of(rawConfiguration);
        final Map<String, String> properties = ConfigurationUtil.rawConfigurations2Properties(rawConfigurations);
        Assertions.assertNotNull(properties);
        Assertions.assertEquals(1, properties.size());
        Assertions.assertTrue(properties.containsKey("name"));
    }

    @Test
    void testProperties2Configurations() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Key1", "Value1");

        final Collection<RawConfiguration> rawConfigurations =
                ConfigurationUtil.properties2RawConfigurations(properties);
        Assertions.assertNotNull(rawConfigurations);
        Assertions.assertEquals(1, rawConfigurations.size());

        boolean found = false;
        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            Assertions.assertNotNull(rawConfiguration);

            final String name = rawConfiguration.getName();
            Assertions.assertEquals("Key1", name);

            final byte[] value = rawConfiguration.getValue();
            Assertions.assertEquals("Value1", new String(value, StandardCharsets.UTF_8));

            found = true;
            break;
        }
        Assertions.assertTrue(found);
    }
}
