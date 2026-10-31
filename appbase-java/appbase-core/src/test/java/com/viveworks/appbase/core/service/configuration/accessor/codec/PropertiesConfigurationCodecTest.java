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

package com.viveworks.appbase.core.service.configuration.accessor.codec;

import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class PropertiesConfigurationCodecTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }
    }

    @Test
    void testDecode() throws Exception {
        final String key = PropertiesConfigurationCodecTest.class.getName();
        final String value = "value2";
        final Properties properties = new Properties();
        properties.put(key, value);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        properties.store(byteArrayOutputStream, "");
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .timestamp(System.currentTimeMillis())
                .mimeType(MimeType.PROPERTIES)
                .value(byteArrayOutputStream.toByteArray())
                .name(key)
                .build();

        final PropertiesConfigurationCodec propertiesConfigurationCodec =
                new PropertiesConfigurationCodec();
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final Collection<Configuration> configurations =
                propertiesConfigurationCodec.decode(List.of(rawConfiguration), configurationAccessorContext);

        boolean found = false;
        for (final Configuration configuration : configurations) {
            final ConfigurationKey configurationKey = configuration.getConfigurationKey();
            if (!configurationKey.isTyped()) {
                final String propertyKey = configurationKey.getPropertyKey();
                if (propertyKey.equals(key)) {
                    final ConfigurationValue configurationValue = configuration.getConfigurationValue();
                    final Object value1 = configurationValue.getValue();
                    Assertions.assertEquals(value, value1);
                    found = true;
                    break;
                }
            }
        }

        Assertions.assertTrue(found);
    }

    @Test
    void testEncode() throws Exception {
        final Configuration configuration = CoreTestFixtures.getConfiguration();
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final PropertiesConfigurationCodec propertiesConfigurationCodec = new PropertiesConfigurationCodec();
        final Map<ConfigurationKey, ConfigurationValue<?>> configurationMap = new HashMap<>();
        configurationMap.put(configuration.getConfigurationKey(), configuration.getConfigurationValue());

        final Collection<RawConfiguration> rawConfigurations =
                propertiesConfigurationCodec.encode(configurationMap, configurationAccessorContext);

        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            final MimeType mimeType = rawConfiguration.getMimeType();
            if (mimeType.equals(MimeType.PROPERTIES)) {
                final byte[] rawConfigurationValue = rawConfiguration.getValue();
                final Properties properties = new Properties();
                properties.load(new ByteArrayInputStream(rawConfigurationValue));

                Assertions.assertNotNull(properties);
                Assertions.assertEquals(1, properties.size());

                final String propertyKey = CoreTestFixtures.getConfigurationKey().getPropertyKey();
                final String property = properties.getProperty(propertyKey);
                final String value = CoreTestFixtures.getStringConfigurationValue().getValue();
                Assertions.assertEquals(value, property);
            }
        }
    }
}
