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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.PropertiesConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.DefaultFileMimeTypeResolver;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.DirectoryConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.FileConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.system.SystemConfigurationStore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DefaultConfigurationAccessorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static DefaultConfigurationAccessor configurationAccessor;

    private static Path filePath;
    private static Path rootDirectoryPath;
    private static Path filePath1;
    private static Properties properties1;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);

        configurationAccessor = new DefaultConfigurationAccessor(
                logService,
                metricService,
                Map.of(), Map.of(), List.of(), Map.of());
        configurationAccessor.registerConfigurationCodecByMimeType(
                new PropertiesConfigurationCodec(), MimeType.PROPERTIES);
        configurationAccessor.registerConfigurationCodecByMimeType(
                new YamlConfigurationCodec(), MimeType.YAML);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultMediatorServiceConfiguration.class, "mediatorServiceConfiguration");

        final SystemConfigurationStore systemConfigurationStore = new SystemConfigurationStore(logService, metricService);
        configurationAccessor.registerWritingConfigurationStore(
                systemConfigurationStore, ConfigurationKey.fromPropertyKey("systemProperties"));

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultAppbaseCoreBootstrapConfiguration.class,
                AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbaseBootstrapConfiguration.yaml");
        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(
                        AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbaseBootstrapConfiguration.yaml", MimeType.YAML);
        configurationAccessor.registerWritingConfigurationStore(
                classpathConfigurationStore, ConfigurationKey.fromPropertyKey("fileProperties"));

        final DirectoryConfigurationStore directoryConfigurationStore =
                new DirectoryConfigurationStore(
                        logService,
                        metricService,
                        System.getProperty("user.dir"),
                        Set.of(MimeType.PROPERTIES),
                        new DefaultFileMimeTypeResolver(),
                        null);
        configurationAccessor.registerWritingConfigurationStore(
                directoryConfigurationStore, ConfigurationKey.fromPropertyKey("directoryProperties"));

        final String filename = System.getProperty("java.io.tmpdir") +
                File.separator +
                DefaultConfigurationAccessorTest.class.getName() +
                "-file-" +
                System.nanoTime() +
                "-test-dev.properties";
        filePath = Files.createFile(Path.of(filename));

        final FileConfigurationStore fileConfigurationStore = new FileConfigurationStore(filename, MimeType.PROPERTIES);
        configurationAccessor.registerConfigurationStore(fileConfigurationStore);
        final Properties fileProperties = new Properties();
        fileProperties.put("aa1", "zz1");
        fileProperties.put("bb1", "yy1");
        fileProperties.put("cc1", "xx1");
        fileProperties.put("a1", "z1");
        fileProperties.put("b1", "y1");
        fileProperties.put("c1", "x1");
        fileProperties.store(new FileOutputStream(filePath.toFile()), "comment1");

        rootDirectoryPath = Files.createTempDirectory(DefaultConfigurationAccessorTest.class.getName());
        properties1 = new Properties();
        properties1.put("a1", "z1");
        properties1.put("b1", "y1");
        properties1.put("c1", "x1");
        filePath1 = Files.createTempFile(rootDirectoryPath, "filePath1", ".properties");
        properties1.store(new FileOutputStream(filePath1.toFile()), "comment2");
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (configurationAccessor != null) {
            configurationAccessor.shutdown();
        }

        if (filePath != null) {
            Files.deleteIfExists(filePath);
        }

        if (filePath1 != null) {
            Files.deleteIfExists(filePath1);
        }

        if (rootDirectoryPath != null) {
            Files.deleteIfExists(rootDirectoryPath);
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testReadConfigurations() throws Exception {
        final Collection<Configuration> configurations = configurationAccessor.readConfigurations();
        Assertions.assertNotNull(configurations);
        Assertions.assertTrue(configurations.size() > 10);
        final Set<String> keys = Set.of("aa1", "bb1", "cc1", "a1", "b1", "c1");

        int foundCount = 0;
        for (final Configuration configuration : configurations) {
            final ConfigurationKey configurationKey = configuration.getConfigurationKey();
            final String propertyKey = configurationKey.getPropertyKey();
            if (propertyKey != null && keys.contains(propertyKey)) {
                ++foundCount;
            }
        }
        Assertions.assertEquals(6, foundCount);
    }

    @Test
    @Order(2)
    void testWriteConfigurations() throws Exception {
        properties1.put("dd1", "ww1");

        final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey("fileProperties");
        final ConfigurationValue<?> configurationValue = ConfigurationValue.fromValue(properties1);
        configurationAccessor.writeConfiguration(configurationKey, configurationValue, MimeType.PROPERTIES);

        final Properties resultProperties = new Properties();
        resultProperties.load(new FileInputStream(filePath1.toFile().getAbsolutePath()));
        Assertions.assertFalse(resultProperties.isEmpty());
    }

    @Test
    @Order(3)
    void testConfigurationCodecByMimeTypeMappings() {
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMappings1 =
                configurationAccessor.getConfigurationCodecByMimeTypeMap();
        Assertions.assertNotNull(configurationCodecByMimeTypeMappings1);
        Assertions.assertEquals(2, configurationCodecByMimeTypeMappings1.size());
        Assertions.assertTrue(configurationCodecByMimeTypeMappings1.containsKey(MimeType.PROPERTIES));

        configurationAccessor.unregisterConfigurationCodecByMimeType(MimeType.PROPERTIES);
        configurationAccessor.unregisterConfigurationCodecByMimeType(MimeType.YAML);
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMappings2 =
                configurationAccessor.getConfigurationCodecByMimeTypeMap();
        Assertions.assertNotNull(configurationCodecByMimeTypeMappings2);
        Assertions.assertTrue(configurationCodecByMimeTypeMappings2.isEmpty());

        configurationAccessor.registerConfigurationCodecByMimeType(
                new PropertiesConfigurationCodec(), MimeType.PROPERTIES);
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMappings3 =
                configurationAccessor.getConfigurationCodecByMimeTypeMap();
        Assertions.assertTrue(configurationCodecByMimeTypeMappings3.containsKey(MimeType.PROPERTIES));

        configurationAccessor.clearConfigurationCodecByMimeTypeMap();
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMappings4 =
                configurationAccessor.getConfigurationCodecByMimeTypeMap();
        Assertions.assertTrue(configurationCodecByMimeTypeMappings4.isEmpty());
    }

    @Test
    @Order(4)
    void testConfigurationTypeByRawConfigurationNameMappings() {
        final Map<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameMappings1 =
                configurationAccessor.getConfigurationTypeByRawConfigurationNameMap();
        Assertions.assertNotNull(configurationTypeByRawConfigurationNameMappings1);
        Assertions.assertEquals(2, configurationTypeByRawConfigurationNameMappings1.size());

        configurationAccessor
                .unregisterConfigurationTypeByRawConfigurationName("mediatorServiceConfiguration");
        configurationAccessor
                .unregisterConfigurationTypeByRawConfigurationName(
                        AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbaseBootstrapConfiguration.yaml");

        final Map<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameMappings2 =
                configurationAccessor.getConfigurationTypeByRawConfigurationNameMap();
        Assertions.assertNotNull(configurationTypeByRawConfigurationNameMappings2);
        Assertions.assertTrue(configurationTypeByRawConfigurationNameMappings2.isEmpty());

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultMediatorServiceConfiguration.class, "mediatorServiceConfiguration");
        final Map<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameMappings3 =
                configurationAccessor.getConfigurationTypeByRawConfigurationNameMap();
        Assertions.assertTrue(
                configurationTypeByRawConfigurationNameMappings3.containsKey("mediatorServiceConfiguration"));

        configurationAccessor.clearConfigurationTypeByRawConfigurationNameMap();
        final Map<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameMappings4 =
                configurationAccessor.getConfigurationTypeByRawConfigurationNameMap();
        Assertions.assertTrue(configurationTypeByRawConfigurationNameMappings4.isEmpty());
    }

    @Test
    @Order(5)
    void testConfigurationStores() {
        final Map<String, ConfigurationStore> configurationStoreByIdMap1 =
                configurationAccessor.getConfigurationStoreByIdMap();
        Assertions.assertNotNull(configurationStoreByIdMap1);

        final Collection<ConfigurationStore> configurationStores1 = configurationStoreByIdMap1.values();
        Assertions.assertNotNull(configurationStores1);
        Assertions.assertEquals(4, configurationStores1.size());



        configurationAccessor.unregisterConfigurationStore(SystemConfigurationStore.class);
        configurationAccessor.unregisterConfigurationStore(ClasspathConfigurationStore.class);
        configurationAccessor.unregisterConfigurationStore(FileConfigurationStore.class);
        configurationAccessor.unregisterConfigurationStore(DirectoryConfigurationStore.class);
        final Map<String, ConfigurationStore> configurationStoreByIdMap2 =
                configurationAccessor.getConfigurationStoreByIdMap();
        Assertions.assertNotNull(configurationStoreByIdMap2);

        final Collection<ConfigurationStore> configurationStores2 = configurationStoreByIdMap2.values();
        Assertions.assertNotNull(configurationStores2);
        Assertions.assertTrue(configurationStores2.isEmpty());



        configurationAccessor.registerConfigurationStore(new SystemConfigurationStore(logService, metricService));
        final Map<String, ConfigurationStore> configurationStoreByIdMap3 =
                configurationAccessor.getConfigurationStoreByIdMap();
        Assertions.assertNotNull(configurationStoreByIdMap3);

        final Collection<ConfigurationStore> configurationStores3 = configurationStoreByIdMap3.values();
        Assertions.assertNotNull(configurationStores3);
        Assertions.assertEquals(1, configurationStores3.size());



        configurationAccessor.clearConfigurationStores();



        final Map<String, ConfigurationStore> configurationStoreByIdMap4 =
                configurationAccessor.getConfigurationStoreByIdMap();
        Assertions.assertNotNull(configurationStoreByIdMap4);

        final Collection<ConfigurationStore> configurationStores4 = configurationStoreByIdMap4.values();
        Assertions.assertNotNull(configurationStores4);
        Assertions.assertTrue(configurationStores4.isEmpty());
    }

    @Test
    @Order(6)
    void testWritingConfigurationStoreTypeByKeyMappings() {
        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap1 =
                configurationAccessor.getWritingConfigurationStoreIdsByKeyMap();
        Assertions.assertNotNull(writingConfigurationStoreIdsByKeyMap1);
        Assertions.assertEquals(3, writingConfigurationStoreIdsByKeyMap1.size());

        configurationAccessor.unregisterWritingConfigurationStore(
                FileConfigurationStore.class, ConfigurationKey.fromPropertyKey("fileProperties"));

        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap2 =
                configurationAccessor.getWritingConfigurationStoreIdsByKeyMap();
        Assertions.assertNotNull(writingConfigurationStoreIdsByKeyMap2);
        Assertions.assertEquals(3, writingConfigurationStoreIdsByKeyMap2.size());

        configurationAccessor.clearWritingConfigurationStores();
        final Map<ConfigurationKey, Set<String>> writingConfigurationStoreIdsByKeyMap3 =
                configurationAccessor.getWritingConfigurationStoreIdsByKeyMap();
        Assertions.assertNotNull(writingConfigurationStoreIdsByKeyMap3);
        Assertions.assertTrue(writingConfigurationStoreIdsByKeyMap3.isEmpty());
    }

    @Test
    @Order(7)
    void testConfigStores() {
        final Iterable<ConfigSource> configSources = configurationAccessor.getConfigSources();
        Assertions.assertNotNull(configSources);

        boolean found = true;
        for (final ConfigSource configSource : configSources) {
            Assertions.assertNotNull(configSource);
            found = true;
        }
        Assertions.assertTrue(found);
    }

    @Test
    @Order(8)
    void testOtherMethods() {
        Assertions.assertNotNull(configurationAccessor.getConfigurationCodecByMimeTypeMappingSizeGauge());
        Assertions.assertNotNull(configurationAccessor.getConfigurationStoreByIdMappingSizeGauge());
        Assertions.assertNotNull(configurationAccessor.getConfigurationTypeByRawConfigurationNameMappingSizeGauge());
        Assertions.assertNotNull(configurationAccessor.getWritingConfigurationStoreIdsByKeyMappingSizeGauge());
        Assertions.assertNotNull(configurationAccessor.getStatus());
    }
}
