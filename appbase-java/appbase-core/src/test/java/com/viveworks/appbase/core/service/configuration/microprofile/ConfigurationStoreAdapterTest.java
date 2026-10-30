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

package com.viveworks.appbase.core.service.configuration.microprofile;

import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationStoreAdapterTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testAdapter() throws ConfigurationStoreException {
        final ConfigSource configSource = new TestConfigSource();
        final ConfigurationStoreAdapter configSourceAdapter = new ConfigurationStoreAdapter(configSource);
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final Collection<RawConfiguration> rawConfigurations =
                configSourceAdapter.readRawConfigurations(configurationAccessorContext);
        Assertions.assertNotNull(rawConfigurations);
        Assertions.assertEquals(2, rawConfigurations.size());

        boolean foundK1 = false;
        boolean foundK2 = false;
        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            final String name = rawConfiguration.getName();
            final byte[] bytesValue = rawConfiguration.getValue();
            final String value = new String(bytesValue, StandardCharsets.UTF_8);

            if ("k1".equals(name) && "v1".equals(value)) {
                foundK1 = true;
            }
            if ("k2".equals(name) && "v2".equals(value)) {
                foundK2 = true;
            }
            Assertions.assertDoesNotThrow(() ->
                    configSourceAdapter.writeRawConfiguration(rawConfiguration, configurationAccessorContext));
        }
        Assertions.assertTrue(foundK1);
        Assertions.assertTrue(foundK2);

        Assertions.assertFalse(configSourceAdapter.supportsWrite());
        Assertions.assertDoesNotThrow(configSourceAdapter::shutdown);
    }

    private static class TestConfigSource implements ConfigSource {
        @Override
        public Map<String, String> getProperties() {
            final Map<String, String> map = new HashMap<>();
            map.put("k1", "v1");
            map.put("k2", "v2");

            return map;
        }

        @Override
        public String getValue(final String propertyName) {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }
    }
}
