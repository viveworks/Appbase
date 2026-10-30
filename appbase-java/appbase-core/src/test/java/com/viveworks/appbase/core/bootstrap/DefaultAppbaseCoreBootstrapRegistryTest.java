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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.system.SystemConfigurationStore;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultAppbaseCoreBootstrapRegistryTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() {
        logService =
                new DefaultLogService(
                        CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                        DefaultLogServiceConfiguration.builder().build());
        metricService = new DefaultMetricService(
                logService,
                DefaultMetricServiceConfiguration.builder().build());
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
    void testRegistry() {
        final DefaultAppbaseCoreBootstrapRegistry bootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();


        // ExceptionMessageContextCodec
        final ExceptionMessageContextCodec exceptionMessageContextCodec = new DefaultExceptionMessageContextCodec();
        bootstrapRegistry.setExceptionMessageContextCodec(exceptionMessageContextCodec);
        Assertions.assertNotNull(bootstrapRegistry.getExceptionMessageContextCodec());
        Assertions.assertEquals(exceptionMessageContextCodec, bootstrapRegistry.getExceptionMessageContextCodec());


        // ConfigurationCodec
        final ConfigurationCodec configurationCodec = new YamlConfigurationCodec();
        bootstrapRegistry.registerConfigurationCodecByMimeType(configurationCodec, MimeType.YAML);
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap =
                bootstrapRegistry.getConfigurationCodecByMimeTypeMap();
        Assertions.assertNotNull(configurationCodecByMimeTypeMap);
        Assertions.assertEquals(1, configurationCodecByMimeTypeMap.size());
        final ConfigurationCodec configurationCodec1 = configurationCodecByMimeTypeMap.get(MimeType.YAML);
        Assertions.assertEquals(configurationCodec, configurationCodec1);

        bootstrapRegistry.unregisterConfigurationCodecByMimeType(MimeType.YAML);
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap1 =
                bootstrapRegistry.getConfigurationCodecByMimeTypeMap();
        Assertions.assertNotNull(configurationCodecByMimeTypeMap1);
        Assertions.assertTrue(configurationCodecByMimeTypeMap1.isEmpty());

        bootstrapRegistry.registerConfigurationCodecByMimeType(configurationCodec, MimeType.YAML);
        bootstrapRegistry.clearConfigurationCodecByMimeTypeMap();
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMap2 =
                bootstrapRegistry.getConfigurationCodecByMimeTypeMap();
        Assertions.assertNotNull(configurationCodecByMimeTypeMap2);
        Assertions.assertTrue(configurationCodecByMimeTypeMap2.isEmpty());


        // ConfigurationStore
        final ConfigurationStore configurationStore = new SystemConfigurationStore(logService, metricService);
        bootstrapRegistry.registerConfigurationStore(configurationStore);

        final Map<String, ConfigurationStore> configurationStoreByIdMap = bootstrapRegistry.getConfigurationStoreByIdMap();
        Assertions.assertNotNull(configurationStoreByIdMap);

        final Collection<ConfigurationStore> configurationStores = configurationStoreByIdMap.values();
        Assertions.assertNotNull(configurationStores);
        Assertions.assertEquals(1, configurationStores.size());
        final ConfigurationStore configurationStore1 = configurationStores.stream().findFirst().orElse(null);
        Assertions.assertNotNull(configurationStore1);
        Assertions.assertEquals(configurationStore, configurationStore1);

        bootstrapRegistry.unregisterConfigurationStore(SystemConfigurationStore.class);
        final Map<String, ConfigurationStore> configurationStoreByIdMap1 = bootstrapRegistry.getConfigurationStoreByIdMap();
        final Collection<ConfigurationStore> configurationStores1 = configurationStoreByIdMap1.values();
        Assertions.assertNotNull(configurationStores1);
        Assertions.assertTrue(configurationStores1.isEmpty());

        bootstrapRegistry.registerConfigurationStore(configurationStore);
        bootstrapRegistry.clearConfigurationStores();
        final Map<String, ConfigurationStore> configurationStoreByIdMap2 = bootstrapRegistry.getConfigurationStoreByIdMap();
        final Collection<ConfigurationStore> configurationStores2 = configurationStoreByIdMap2.values();
        Assertions.assertNotNull(configurationStores2);
        Assertions.assertTrue(configurationStores2.isEmpty());


        // configurationTypeByRawConfigurationName
        bootstrapRegistry.registerConfigurationTypeByRawConfigurationName(
                ConfigurationServiceConfiguration.class, "configurationServiceConfiguration");
        final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByRawConfigurationNameMap =
                bootstrapRegistry.getConfigurationTypeByRawConfigurationNameMap();
        Assertions.assertNotNull(configurationTypeByRawConfigurationNameMap);
        Assertions.assertEquals(1, configurationTypeByRawConfigurationNameMap.size());
        final Map.Entry<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameEntry =
                configurationTypeByRawConfigurationNameMap.entrySet().stream().findFirst().orElse(null);
        Assertions.assertNotNull(configurationTypeByRawConfigurationNameEntry);
        Assertions.assertEquals(
                "configurationServiceConfiguration", configurationTypeByRawConfigurationNameEntry.getKey());
        Assertions.assertEquals(
                ConfigurationServiceConfiguration.class, configurationTypeByRawConfigurationNameEntry.getValue());

        bootstrapRegistry.unregisterConfigurationTypeByRawConfigurationName("configurationServiceConfiguration");
        final Map.Entry<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameEntry1 =
                configurationTypeByRawConfigurationNameMap.entrySet().stream().findFirst().orElse(null);
        Assertions.assertNull(configurationTypeByRawConfigurationNameEntry1);

        bootstrapRegistry.registerConfigurationTypeByRawConfigurationName(
                ConfigurationServiceConfiguration.class, "configurationServiceConfiguration");
        bootstrapRegistry.clearConfigurationTypeByRawConfigurationNameMap();
        final Map.Entry<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameEntry2 =
                configurationTypeByRawConfigurationNameMap.entrySet().stream().findFirst().orElse(null);
        Assertions.assertNull(configurationTypeByRawConfigurationNameEntry2);


        // configurationTypeByYamlNodeNameToDecode
        bootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                ConfigurationServiceConfiguration.class, "configurationServiceConfiguration");
        final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByYamlNodeNameToDecodeMap =
                bootstrapRegistry.getConfigurationTypeByYamlNodeNameToDecodeMap();
        Assertions.assertNotNull(configurationTypeByYamlNodeNameToDecodeMap);
        Assertions.assertEquals(1, configurationTypeByYamlNodeNameToDecodeMap.size());
        final Map.Entry<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByYamlNodeNameToDecodeEntry =
                configurationTypeByYamlNodeNameToDecodeMap.entrySet().stream().findFirst().orElse(null);
        Assertions.assertNotNull(configurationTypeByYamlNodeNameToDecodeEntry);
        Assertions.assertEquals(
                "configurationServiceConfiguration", configurationTypeByYamlNodeNameToDecodeEntry.getKey());
        Assertions.assertEquals(
                ConfigurationServiceConfiguration.class, configurationTypeByYamlNodeNameToDecodeEntry.getValue());

        bootstrapRegistry.unregisterConfigurationTypeByYamlNodeNameToDecode("configurationServiceConfiguration");
        final Map.Entry<String, Class<? extends AppbaseConfiguration>>
        configurationTypeByYamlNodeNameToDecodeEntry1 =
                configurationTypeByYamlNodeNameToDecodeMap.entrySet().stream().findFirst().orElse(null);
        Assertions.assertNull(configurationTypeByYamlNodeNameToDecodeEntry1);


        // writingConfigurationStoreTypeByKey
        final SystemConfigurationStore systemConfigurationStore = new SystemConfigurationStore(logService, metricService);
        bootstrapRegistry.registerWritingConfigurationStore(
                systemConfigurationStore,
                ConfigurationKey.fromPropertyKey("systemConfigurationStore"));
        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap =
                bootstrapRegistry.getWritingConfigurationStoreIdsByKeyMap();
        Assertions.assertNotNull(writingConfigurationStoreIdsByKeyMap);
        Assertions.assertEquals(1, writingConfigurationStoreIdsByKeyMap.size());


        final Map.Entry<ConfigurationKey, Set<String>> configurationKeySetEntry =
                writingConfigurationStoreIdsByKeyMap.entrySet().stream().findFirst().orElse(null);
        Assertions.assertNotNull(configurationKeySetEntry);
        Assertions.assertEquals(
                ConfigurationKey.fromPropertyKey("systemConfigurationStore"), configurationKeySetEntry.getKey());

        final Set<String> configurationKeySetEntryValue = configurationKeySetEntry.getValue();
        Assertions.assertNotNull(configurationKeySetEntryValue);
        Assertions.assertEquals(1, configurationKeySetEntryValue.size());

        final String configurationStoreId = configurationKeySetEntryValue.stream().findFirst().orElse(null);
        Assertions.assertNotNull(configurationStoreId);
        Assertions.assertEquals(configurationStoreId, systemConfigurationStore.getId());

        bootstrapRegistry.unregisterWritingConfigurationStore(
                systemConfigurationStore.getId(), ConfigurationKey.fromPropertyKey("systemConfigurationStore"));
        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap1 =
                bootstrapRegistry.getWritingConfigurationStoreIdsByKeyMap();
        Assertions.assertNotNull(writingConfigurationStoreIdsByKeyMap1);
        Assertions.assertTrue(writingConfigurationStoreIdsByKeyMap1.isEmpty());

        bootstrapRegistry.registerWritingConfigurationStore(
                systemConfigurationStore, ConfigurationKey.fromPropertyKey("systemConfigurationStore"));
        bootstrapRegistry.clearWritingConfigurationStores();

        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap2 =
                bootstrapRegistry.getWritingConfigurationStoreIdsByKeyMap();
        Assertions.assertNotNull(writingConfigurationStoreIdsByKeyMap2);
        Assertions.assertTrue(writingConfigurationStoreIdsByKeyMap2.isEmpty());


        Assertions.assertDoesNotThrow(bootstrapRegistry::clear);
    }
}
