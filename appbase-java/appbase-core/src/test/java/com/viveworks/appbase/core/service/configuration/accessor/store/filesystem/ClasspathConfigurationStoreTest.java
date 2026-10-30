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

package com.viveworks.appbase.core.service.configuration.accessor.store.filesystem;

import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.environment.DefaultEnvironmentResolver;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ClasspathConfigurationStoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ClasspathConfigurationStore classpathConfigurationStore1;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }

        if (classpathConfigurationStore1 != null) {
            classpathConfigurationStore1.shutdown();
        }
    }

    @Test
    void testReadAndWriteRawConfigurations() throws ConfigurationStoreException {
        System.setProperty(AppbaseConstants.APPBASE_SYSTEM_PROPERTY, "test-dev");
        final String classpathFile = "classpath:///appbaseBootstrapConfiguration.yaml";

        classpathConfigurationStore1 =
                new ClasspathConfigurationStore(classpathFile, MimeType.YAML);
        check(classpathConfigurationStore1);


        final DefaultEnvironmentResolver environmentResolver = new DefaultEnvironmentResolver();
        final Map<String, MimeType> mimeTypeByExtensionMap = new HashMap<>();
        mimeTypeByExtensionMap.put("properties", MimeType.PROPERTIES);
        mimeTypeByExtensionMap.put("yml", MimeType.YAML);
        mimeTypeByExtensionMap.put("yaml", MimeType.YAML);
        final DefaultFileMimeTypeResolver fileMimeTypeResolver =
                new DefaultFileMimeTypeResolver(mimeTypeByExtensionMap);
        final FileTargetResolver targetResolver = new FileTargetResolver();

        final ClasspathConfigurationStore classpathConfigurationStore2 =
                new ClasspathConfigurationStore(
                        classpathFile,
                        environmentResolver,
                        fileMimeTypeResolver,
                        targetResolver);
        check(classpathConfigurationStore2);
    }

    private void check(
            final ClasspathConfigurationStore classpathConfigurationStore) throws ConfigurationStoreException
    {
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final Collection<RawConfiguration> rawConfigurations =
                classpathConfigurationStore.readRawConfigurations(configurationAccessorContext);

        Assertions.assertNotNull(rawConfigurations);
        Assertions.assertEquals(1, rawConfigurations.size());

        boolean found = false;
        byte[] bytes = null;
        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            Assertions.assertEquals(MimeType.YAML, rawConfiguration.getMimeType());
            bytes = rawConfiguration.getValue();
            found = true;
            break;
        }
        Assertions.assertNotNull(bytes);
        Assertions.assertTrue(found);


        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(getClass().getName())
                .mimeType(MimeType.YAML)
                .timestamp(System.currentTimeMillis())
                .value(bytes)
                .build();
        Assertions.assertThrows(ConfigurationStoreException.class, () -> {
            classpathConfigurationStore.writeRawConfiguration(rawConfiguration, configurationAccessorContext);
        });

        Assertions.assertFalse(classpathConfigurationStore.supportsWrite());
    }
}
