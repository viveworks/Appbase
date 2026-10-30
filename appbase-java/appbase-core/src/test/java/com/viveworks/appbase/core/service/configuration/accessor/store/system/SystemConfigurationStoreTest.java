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

package com.viveworks.appbase.core.service.configuration.accessor.store.system;

import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.codec.PropertiesConfigurationCodec;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Properties;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class SystemConfigurationStoreTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        final SystemConfigurationStore systemConfigurationStore = new SystemConfigurationStore(
                logService, metricService);
        systemConfigurationStore.shutdown();

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void readRawConfigurations() throws Exception {
        final String key = SystemConfigurationStoreTest.class.getName() + "key1";
        final String value = "value1";
        System.setProperty(key, value);

        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final SystemConfigurationStore systemConfigurationStore = new SystemConfigurationStore(
                logService, metricService);
        final Collection<RawConfiguration> rawConfigurations =
                systemConfigurationStore.readRawConfigurations(configurationAccessorContext);
        final PropertiesConfigurationCodec propertiesConfigurationCodec = new PropertiesConfigurationCodec();
        final Collection<Configuration> configurations =
                propertiesConfigurationCodec.decode(rawConfigurations, configurationAccessorContext);

        boolean found = false;
        for (final Configuration configuration : configurations) {
            final ConfigurationKey configurationKey = configuration.getConfigurationKey();
            final String propertyKey = configurationKey.getPropertyKey();
            if (propertyKey.equals(key)) {
                final ConfigurationValue configurationValue = configuration.getConfigurationValue();
                final Object value1 = configurationValue.getValue();
                Assertions.assertEquals(value, value1);
                found = true;
                break;
            }
        }

        Assertions.assertTrue(found);
    }

    @Test
    void writeRawConfiguration() throws Exception {
        final String key = SystemConfigurationStoreTest.class.getName() + "key2";
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

        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final SystemConfigurationStore systemConfigurationStore = new SystemConfigurationStore(
                logService, metricService);
        systemConfigurationStore.writeRawConfiguration(rawConfiguration, configurationAccessorContext);

        final String systemValue = System.getProperty(key);
        Assertions.assertNotNull(systemValue);
        Assertions.assertEquals(value, systemValue);

        Assertions.assertTrue(systemConfigurationStore.supportsWrite());
    }
}
