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

package com.viveworks.appbase.core.service.configuration.microprofile;

import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigSourceAdapterTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ClasspathConfigurationStore classpathConfigurationStore;
    private static ConfigurationAccessorContext configurationAccessorContext;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);

        System.setProperty(AppbaseConstants.APPBASE_SYSTEM_PROPERTY, "test-dev");
        final String classpathFile = "classpath:///appbaseBootstrapConfiguration.yaml";
        classpathConfigurationStore =
                new ClasspathConfigurationStore(classpathFile, MimeType.YAML);
        configurationAccessorContext = CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }

        if (classpathConfigurationStore != null) {
            classpathConfigurationStore.shutdown();
        }
    }

    @Test
    void testReloadWithConfigurationStore() {
        final ConfigSourceAdapter configSourceAdapter = new ConfigSourceAdapter(
                classpathConfigurationStore, configurationAccessorContext);
        final Map<String, String> properties = configSourceAdapter.getProperties();
        Assertions.assertNotNull(properties);
        Assertions.assertFalse(properties.isEmpty());
    }

    @Test
    void testReloadWithProperties() throws ConfigurationStoreException {
        final Collection<RawConfiguration> rawConfigurations =
                classpathConfigurationStore.readRawConfigurations(configurationAccessorContext);
        final Map<String, String> properties = ConfigurationUtil.rawConfigurations2Properties(rawConfigurations);
        Assertions.assertNotNull(properties);
        Assertions.assertFalse(properties.isEmpty());

        final ConfigSourceAdapter configSourceAdapter = new ConfigSourceAdapter(
                classpathConfigurationStore, configurationAccessorContext);
        configSourceAdapter.reload(properties);
        final Map<String, String> properties1 = configSourceAdapter.getProperties();

        Assertions.assertEquals(properties, properties1);

        final String name = configSourceAdapter.getName();
        Assertions.assertEquals(ClasspathConfigurationStore.class.getName(), name);

        final String value = configSourceAdapter.getValue(
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbaseBootstrapConfiguration.yaml");
        Assertions.assertNotNull(value);
    }
}
