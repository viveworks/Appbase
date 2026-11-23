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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.DefaultConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.codec.PropertiesConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.DefaultFileMimeTypeResolver;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.DirectoryConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.FileConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.system.SystemConfigurationStore;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializer;
import com.viveworks.appbase.core.service.configuration.property.deserializer.PropertyDeserializerNotFoundException;
import com.viveworks.appbase.core.service.configuration.property.deserializer.YamlPropertyDeserializer;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("IllegalCatch")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultConfigurationServiceTest {
    private static DefaultConfigurationAccessor configurationAccessor;
    private static Path filePath;
    private static Path rootDirectoryPath;
    private static Path filePath1;
    private static Path typedFilePath1;
    private static Path typedFilePath2;
    private static Path typedFilePath3;
    private static Properties properties1;
    private static DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration;
    private static AtomicInteger globalConfigurationCounter = new AtomicInteger(0);
    private static AtomicInteger configurationCounter = new AtomicInteger(0);

    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static DefaultConfigurationService configurationService;

    @BeforeAll
    static void setUp() {
        logService =
                new DefaultLogService(
                        CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                        DefaultLogServiceConfiguration.builder().build());
        metricService = new DefaultMetricService(
                logService,
                DefaultMetricServiceConfiguration.builder().build());
        validationService = new DefaultValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                        metricService,
                        CoreTestFixtures.getSharedDisruptorService(),
                        CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                        validationService);
        newConfigurationService(logService, metricService, mediatorService, validationService, false);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (configurationAccessor != null) {
            configurationAccessor.shutdown();
        }

        if (filePath != null) {
            Files.deleteIfExists(filePath);
        }

        if (filePath1 != null) {
            Files.deleteIfExists(filePath1);
        }

        if (typedFilePath1 != null) {
            Files.deleteIfExists(typedFilePath1);
        }

        if (typedFilePath2 != null) {
            Files.deleteIfExists(typedFilePath2);
        }

        if (typedFilePath3 != null) {
            Files.deleteIfExists(typedFilePath3);
        }

        if (rootDirectoryPath != null) {
            Files.deleteIfExists(rootDirectoryPath);
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    public static ConfigurationService newConfigurationService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService,
            final boolean createMicroprofileConfigurationService)
    {
        try {
            final String classpathFile1 = "classpath:///appbaseBootstrapConfiguration.yaml";
            System.setProperty(
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile1);

            configurationAccessor = new DefaultConfigurationAccessor(
                    logService,
                    metricService,
                    Map.of(), Map.of(), List.of(), Map.of());
            configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                    DefaultAppbaseCoreBootstrapConfiguration.class,
                    classpathFile1);
            configurationAccessor.registerConfigurationCodecByMimeType(
                    new PropertiesConfigurationCodec(),
                    MimeType.PROPERTIES);
            configurationAccessor.registerConfigurationCodecByMimeType(
                    new YamlConfigurationCodec(),
                    MimeType.YAML);
            configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                    DefaultMediatorServiceConfiguration.class,
                    "mediatorServiceConfiguration");

            configurationAccessor.registerWritingConfigurationStore(
                    new SystemConfigurationStore(logService, metricService),
                    ConfigurationKey.fromPropertyKey("systemProperties"));

            final ClasspathConfigurationStore classpathConfigurationStore =
                    new ClasspathConfigurationStore(classpathFile1, MimeType.YAML);
            configurationAccessor.registerWritingConfigurationStore(
                    classpathConfigurationStore,
                    ConfigurationKey.fromPropertyKey("fileProperties"));


            final String filename = System.getProperty("java.io.tmpdir") +
                    File.separator +
                    DefaultConfigurationServiceTest.class.getName() +
                    "-file-" +
                    System.nanoTime() +
                    "-test-dev.properties";
            filePath = Files.createFile(Path.of(filename));
            final FileConfigurationStore fileConfigurationStore =
                    new FileConfigurationStore(filename, MimeType.PROPERTIES);
            configurationAccessor.registerConfigurationStore(fileConfigurationStore);
            final Properties fileProperties = new Properties();
            fileProperties.put("aa1", "zz1");
            fileProperties.put("bb1", "yy1");
            fileProperties.put("cc1", "xx1");
            fileProperties.put("a1", "z1");
            fileProperties.put("b1", "y1");
            fileProperties.put("c1", "x1");
            fileProperties.put("lby1", "1");
            fileProperties.put("lsh", "12");
            fileProperties.put("lin", "123");
            fileProperties.put("llo", "1234");
            fileProperties.put("lfl", "1234.34");
            fileProperties.put("ldo", "12345.3456");
            fileProperties.put("lbo", "true");
            fileProperties.store(new FileOutputStream(filePath.toFile()), "comment1");

            // macOS exposes the temporary directory through both /var and /private/var. The
            // directory store resolves discovered files to their real path, so register the
            // fixture paths from the same canonical root or typed YAML mappings will not match.
            rootDirectoryPath = Files.createTempDirectory(
                    DefaultConfigurationServiceTest.class.getName()).toRealPath();
            properties1 = new Properties();
            properties1.put("a1", "z1");
            properties1.put("b1", "y1");
            properties1.put("c1", "x1");
            properties1.put("lby1", "1");
            properties1.put("lsh", "12");
            properties1.put("lin", "123");
            properties1.put("llo", "1234");
            properties1.put("lfl", "1234.34");
            properties1.put("ldo", "12345.3456");
            properties1.put("lbo", "true");

            final String yaml = "---" + System.lineSeparator() +
                    "dc: \"east\"" + System.lineSeparator() +
                    "hostname: \"host34\"" + System.lineSeparator() +
                    "port: 9092";
            properties1.put("sampleAppbaseServiceConfiguration5", yaml);
            filePath1 = Files.createTempFile(rootDirectoryPath, "filePath1", ".properties");
            properties1.store(new FileOutputStream(filePath1.toFile()), "comment2");

            final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
            typedFilePath1 = Files.createTempFile(rootDirectoryPath, "typedFilePath1", ".yaml");
            final SampleAppbaseServiceConfiguration1 sampleAppbaseServiceConfiguration1 =
                    new SampleAppbaseServiceConfiguration1("host1", -1);
            final String sampleString1 = yamlMapper
                    .writerFor(SampleAppbaseServiceConfiguration1.class)
                    .writeValueAsString(sampleAppbaseServiceConfiguration1);
            Files.writeString(typedFilePath1, sampleString1);
            configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                    SampleAppbaseServiceConfiguration1.class,
                    typedFilePath1.toFile().getAbsolutePath());

            typedFilePath2 = Files.createTempFile(rootDirectoryPath, "typedFilePath2", ".yaml");
            final SampleAppbaseServiceConfiguration2 sampleAppbaseServiceConfiguration2 =
                    new SampleAppbaseServiceConfiguration2("host2", 8090);
            final String sampleString2 = yamlMapper
                    .writerFor(SampleAppbaseServiceConfiguration2.class)
                    .writeValueAsString(sampleAppbaseServiceConfiguration2);
            Files.writeString(typedFilePath2, sampleString2);
            configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                    SampleAppbaseServiceConfiguration2.class,
                    typedFilePath2.toFile().getAbsolutePath());

            typedFilePath3 = Files.createTempFile(rootDirectoryPath, "typedFilePath3", ".yaml");
            final SampleAppbaseServiceConfiguration3 sampleAppbaseServiceConfiguration3 =
                    new SampleAppbaseServiceConfiguration3("dc", "host3", 9090);
            final String sampleString3 = yamlMapper
                    .writerFor(SampleAppbaseServiceConfiguration3.class)
                    .writeValueAsString(sampleAppbaseServiceConfiguration3);
            Files.writeString(typedFilePath3, sampleString3);
            configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                    SampleAppbaseServiceConfiguration3.class,
                    typedFilePath3.toFile().getAbsolutePath());


            final DirectoryConfigurationStore directoryConfigurationStore = new DirectoryConfigurationStore(
                    logService,
                    metricService,
                    rootDirectoryPath.toFile().getAbsolutePath(),
                    Set.of(MimeType.PROPERTIES, MimeType.YAML),
                    new DefaultFileMimeTypeResolver(Map.of(
                            "properties", MimeType.PROPERTIES,
                            "yml", MimeType.YAML,
                            "yaml", MimeType.YAML)),
                    null);
            configurationAccessor.registerWritingConfigurationStore(
                    directoryConfigurationStore,
                    ConfigurationKey.fromPropertyKey("directoryProperties"));

            if (createMicroprofileConfigurationService) {
                configurationService = new DefaultConfigurationService(
                        logService,
                        metricService,
                        mediatorService,
                        validationService,
                        configurationAccessor,
                        null);
            } else {
                configurationService = new DefaultConfigurationService(
                        logService,
                        metricService,
                        mediatorService,
                        validationService,
                        configurationAccessor,
                        null);
            }
            defaultConfigurationServiceConfiguration = configurationService.getConfiguration();
            configurationService.registerSecurePropertyKey("byteKey1");
            configurationService.registerSecureType(SampleAppbaseServiceConfiguration1.class);
            configurationService.putString("putStringPropertyKey", "putStringPropertyValue");
            final SampleAppbaseServiceConfiguration4 sampleAppbaseServiceConfiguration4 =
                    new SampleAppbaseServiceConfiguration4("east", "localhost", 80);
            configurationService.putTypedConfiguration(
                    SampleAppbaseServiceConfiguration4.class, sampleAppbaseServiceConfiguration4);
            configurationService.registerGlobalConfigurationListener(new SampleConfigurationListener1());
            configurationService.registerConfigurationListenerByKey(
                    new SampleConfigurationListener2(),
                    ConfigurationKey.fromPropertyKey("aa1"));
            configurationService.registerManagedServiceByConfigurationKey(
                    new SampleService1(),
                    ConfigurationKey.fromTypeKey(SampleAppbaseServiceConfiguration1.class));
            configurationService.registerPropertyDeserializer(new YamlPropertyDeserializer());
            configurationService.registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                    "sampleAppbaseServiceConfiguration5",
                    new ConfigurationService
                            .PropertyDeserializerAndConfigurationType(
                                    YamlPropertyDeserializer.class, SampleAppbaseServiceConfiguration5.class)
            );
            configurationService.registerNonValidatingType(SampleAppbaseServiceConfiguration1.class);
            configurationService.load();
            return configurationService;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    @Order(1)
    void testLoad() throws ServiceReloadException, IOException {
        final SampleAppbaseServiceConfiguration1 sampleAppbaseServiceConfiguration1 =
                new SampleAppbaseServiceConfiguration1("host123", 8080);
        final String sampleString1 = YamlUtil.getYamlMapper()
                .writerFor(SampleAppbaseServiceConfiguration1.class)
                .writeValueAsString(sampleAppbaseServiceConfiguration1);
        Files.writeString(typedFilePath1, sampleString1);

        final Properties fileProperties = new Properties();
        fileProperties.put("aa1", "zz11");
        fileProperties.put("cc1", "xx1");
        fileProperties.put("cc11", "xx11");
        fileProperties.store(new FileOutputStream(filePath.toFile()), "comment1");

        final SampleAppbaseServiceConfiguration5 sampleAppbaseServiceConfiguration5 =
                configurationService.getTypedConfiguration(SampleAppbaseServiceConfiguration5.class);
        Assertions.assertNotNull(sampleAppbaseServiceConfiguration5);
        Assertions.assertEquals("east", sampleAppbaseServiceConfiguration5.getDc());
        Assertions.assertEquals("host34", sampleAppbaseServiceConfiguration5.getHostname());
        Assertions.assertEquals(9092, sampleAppbaseServiceConfiguration5.getPort());

        configurationService.reload(defaultConfigurationServiceConfiguration);
        Assertions.assertTrue(globalConfigurationCounter.get() > 20);
        Assertions.assertTrue(configurationCounter.get() > 0);

        globalConfigurationCounter.set(0);
        configurationCounter.set(0);
        Files.deleteIfExists(typedFilePath1);

        configurationService.reload(defaultConfigurationServiceConfiguration);
        Assertions.assertTrue(globalConfigurationCounter.get() > 0);
        Assertions.assertEquals(0, configurationCounter.get());

        Files.createFile(typedFilePath1);
        Files.writeString(typedFilePath1, sampleString1);
        configurationService.reload(defaultConfigurationServiceConfiguration);
    }

    @Test
    @Order(2)
    void testByte() throws PropertyCastException {
        final Byte value1 = Byte.valueOf("1");
        configurationService.putByte("byteKey1", value1);
        final Byte byteValue1 = configurationService.getByte("byteKey1");
        Assertions.assertNotNull(byteValue1);
        Assertions.assertEquals(value1, byteValue1);
        configurationService.removeByte("byteKey1");
        final Byte byteValue11 = configurationService.getByte("byteKey1");
        Assertions.assertNull(byteValue11);

        configurationService.putString("byteKey2", "2");
        final Byte byteValue2 = configurationService.getByte("byteKey2");
        Assertions.assertNotNull(byteValue2);
        final Byte value2 = Byte.valueOf("2");
        Assertions.assertEquals(value2, byteValue2);

        configurationService.putString("byteKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getByte("byteKey3");
        });

        final Byte unknownValue = configurationService.getByte("unknownKey");
        Assertions.assertNull(unknownValue);

        final Byte defaultValue = configurationService.getByte("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Byte lby1 = configurationService.getByte("lby1");
        Assertions.assertNotNull(lby1);
        Assertions.assertEquals(1, (byte) lby1);
    }

    @Test
    @Order(3)
    void testBytes() throws PropertyCastException {
        final byte[] value1 = "1".getBytes();
        configurationService.putBytes("bytesKey1", value1);
        final byte[] bytesValue1 = configurationService.getBytes("bytesKey1");
        Assertions.assertNotNull(bytesValue1);
        Assertions.assertEquals(value1, bytesValue1);
        configurationService.removeBytes("bytesKey1");
        final byte[] bytesValue11 = configurationService.getBytes("bytesKey1");
        Assertions.assertNull(bytesValue11);

        configurationService.putString("bytesKey2", "2");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getBytes("bytesKey2");
        });

        configurationService.putString("byteKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getBytes("byteKey3");
        });

        final byte[] unknownValue = configurationService.getBytes("unknownKey");
        Assertions.assertNull(unknownValue);

        final byte[] defaultValue = configurationService.getBytes("unknownKey", value1);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value1, defaultValue);
    }

    @Test
    @Order(4)
    void testShort() throws PropertyCastException {
        final Short value1 = Short.valueOf("1");
        configurationService.putShort("shortKey1", value1);
        final Short shortValue1 = configurationService.getShort("shortKey1");
        Assertions.assertNotNull(shortValue1);
        Assertions.assertEquals(value1, shortValue1);
        configurationService.removeShort("shortKey1");
        final Short shortValue11 = configurationService.getShort("shortKey1");
        Assertions.assertNull(shortValue11);

        configurationService.putString("shortKey2", "2");
        final Short shortValue2 = configurationService.getShort("shortKey2");
        Assertions.assertNotNull(shortValue2);
        final Short value2 = Short.valueOf("2");
        Assertions.assertEquals(value2, shortValue2);

        configurationService.putString("shortKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getShort("shortKey3");
        });

        final Short unknownValue = configurationService.getShort("unknownKey");
        Assertions.assertNull(unknownValue);

        final Short defaultValue = configurationService.getShort("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Short lsh = configurationService.getShort("lsh");
        Assertions.assertNotNull(lsh);
        Assertions.assertEquals(12, (short) lsh);
    }

    @Test
    @Order(5)
    void testInteger() throws PropertyCastException {
        final Integer value1 = Integer.valueOf("1");
        configurationService.putInteger("integerKey1", value1);
        final Integer integerValue1 = configurationService.getInteger("integerKey1");
        Assertions.assertNotNull(integerValue1);
        Assertions.assertEquals(value1, integerValue1);
        configurationService.removeInteger("integerKey1");
        final Integer integerValue11 = configurationService.getInteger("integerKey1");
        Assertions.assertNull(integerValue11);

        configurationService.putString("integerKey2", "2");
        final Integer integerValue2 = configurationService.getInteger("integerKey2");
        Assertions.assertNotNull(integerValue2);
        final Integer value2 = Integer.valueOf("2");
        Assertions.assertEquals(value2, integerValue2);

        configurationService.putString("integerKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getInteger("integerKey3");
        });

        final Integer unknownValue = configurationService.getInteger("unknownKey");
        Assertions.assertNull(unknownValue);

        final Integer defaultValue = configurationService.getInteger("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Integer lin = configurationService.getInteger("lin");
        Assertions.assertNotNull(lin);
        Assertions.assertEquals(123, (int) lin);
    }

    @Test
    @Order(6)
    void testLong() throws PropertyCastException {
        final Long value1 = Long.valueOf("1");
        configurationService.putLong("longKey1", value1);
        final Long longValue1 = configurationService.getLong("longKey1");
        Assertions.assertNotNull(longValue1);
        Assertions.assertEquals(value1, longValue1);
        configurationService.removeLong("longKey1");
        final Long longValue11 = configurationService.getLong("longKey1");
        Assertions.assertNull(longValue11);

        configurationService.putString("longKey2", "2");
        final Long longValue2 = configurationService.getLong("longKey2");
        Assertions.assertNotNull(longValue2);
        final Long value2 = Long.valueOf("2");
        Assertions.assertEquals(value2, longValue2);

        configurationService.putString("longKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getLong("longKey3");
        });

        final Long unknownValue = configurationService.getLong("unknownKey");
        Assertions.assertNull(unknownValue);

        final Long defaultValue = configurationService.getLong("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Long llo = configurationService.getLong("llo");
        Assertions.assertNotNull(llo);
        Assertions.assertEquals(1234, (long) llo);
    }

    @Test
    @Order(7)
    void testFloat() throws PropertyCastException {
        final Float value1 = Float.valueOf("1");
        configurationService.putFloat("floatKey1", value1);
        final Float floatValue1 = configurationService.getFloat("floatKey1");
        Assertions.assertNotNull(floatValue1);
        Assertions.assertEquals(value1, floatValue1);
        configurationService.removeFloat("floatKey1");
        final Float floatValue11 = configurationService.getFloat("floatKey1");
        Assertions.assertNull(floatValue11);

        configurationService.putString("floatKey2", "2");
        final Float floatValue2 = configurationService.getFloat("floatKey2");
        Assertions.assertNotNull(floatValue2);
        final Float value2 = Float.valueOf("2");
        Assertions.assertEquals(value2, floatValue2);

        configurationService.putString("floatKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getFloat("floatKey3");
        });

        final Float unknownValue = configurationService.getFloat("unknownKey");
        Assertions.assertNull(unknownValue);

        final Float defaultValue = configurationService.getFloat("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Float lfl = configurationService.getFloat("lfl");
        Assertions.assertNotNull(lfl);
        Assertions.assertTrue(lfl > 1234.3);
        Assertions.assertTrue(lfl < 1234.4);
    }

    @Test
    @Order(8)
    void testDouble() throws PropertyCastException {
        final Double value1 = Double.valueOf("1");
        configurationService.putDouble("doubleKey1", value1);
        final Double doubleValue1 = configurationService.getDouble("doubleKey1");
        Assertions.assertNotNull(doubleValue1);
        Assertions.assertEquals(value1, doubleValue1);
        configurationService.removeDouble("doubleKey1");
        final Double doubleValue11 = configurationService.getDouble("doubleKey1");
        Assertions.assertNull(doubleValue11);

        configurationService.putString("doubleKey2", "2");
        final Double doubleValue2 = configurationService.getDouble("doubleKey2");
        Assertions.assertNotNull(doubleValue2);
        final Double value2 = Double.valueOf("2");
        Assertions.assertEquals(value2, doubleValue2);

        configurationService.putString("doubleKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getDouble("doubleKey3");
        });

        final Double unknownValue = configurationService.getDouble("unknownKey");
        Assertions.assertNull(unknownValue);

        final Double defaultValue = configurationService.getDouble("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Double ldo = configurationService.getDouble("ldo");
        Assertions.assertNotNull(ldo);
        Assertions.assertTrue(ldo > 12345.345);
        Assertions.assertTrue(ldo < 12345.346);
    }

    @Test
    @Order(9)
    void testBoolean() {
        final Boolean value1 = Boolean.valueOf("true");
        configurationService.putBoolean("booleanKey1", value1);
        final Boolean booleanValue1 = configurationService.getBoolean("booleanKey1");
        Assertions.assertNotNull(booleanValue1);
        Assertions.assertEquals(value1, booleanValue1);
        configurationService.removeBoolean("booleanKey1");
        final Boolean booleanValue11 = configurationService.getBoolean("booleanKey1");
        Assertions.assertNull(booleanValue11);

        configurationService.putString("booleanKey2", "false");
        final Boolean booleanValue2 = configurationService.getBoolean("booleanKey2");
        Assertions.assertNotNull(booleanValue2);
        final Boolean value2 = Boolean.valueOf("2");
        Assertions.assertEquals(value2, booleanValue2);

        configurationService.putString("booleanKey3", "aaa");
        final Boolean booleanKey3 = configurationService.getBoolean("booleanKey3");
        Assertions.assertNotNull(booleanKey3);
        Assertions.assertFalse(booleanKey3);

        final Boolean unknownValue = configurationService.getBoolean("unknownKey");
        Assertions.assertNull(unknownValue);

        final Boolean defaultValue = configurationService.getBoolean("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);

        final Boolean lbo = configurationService.getBoolean("lbo");
        Assertions.assertNotNull(lbo);
        Assertions.assertTrue(lbo);
    }

    @Test
    @Order(10)
    void testCharacter() throws PropertyCastException {
        final Character value1 = 'a';
        configurationService.putCharacter("characterKey1", value1);
        final Character characterValue1 = configurationService.getCharacter("characterKey1");
        Assertions.assertNotNull(characterValue1);
        Assertions.assertEquals(value1, characterValue1);
        configurationService.removeCharacter("characterKey1");
        final Character characterValue11 = configurationService.getCharacter("characterKey1");
        Assertions.assertNull(characterValue11);

        configurationService.putString("characterKey3", "aaa");
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getCharacter("characterKey3");
        });

        final Character unknownValue = configurationService.getCharacter("unknownKey");
        Assertions.assertNull(unknownValue);

        final Character value2 = 'b';
        final Character defaultValue = configurationService.getCharacter("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);
    }

    @Test
    @Order(11)
    void testString() throws PropertyCastException {
        final String value1 = "abcd";
        configurationService.putString("stringKey1", value1);
        final String stringValue1 = configurationService.getString("stringKey1");
        Assertions.assertNotNull(stringValue1);
        Assertions.assertEquals(value1, stringValue1);
        configurationService.removeString("stringKey1");
        final String stringValue11 = configurationService.getString("stringKey1");
        Assertions.assertNull(stringValue11);

        configurationService.putInteger("stringKey3", 123);
        Assertions.assertThrows(PropertyCastException.class, () -> {
            configurationService.getString("stringKey3");
        });

        final String unknownValue = configurationService.getString("unknownKey");
        Assertions.assertNull(unknownValue);

        final String value2 = "bbb";
        final String defaultValue = configurationService.getString("unknownKey", value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);
    }

    @Test
    @Order(12)
    void testObject() {
        final String value1 = "abcd";
        final ConfigurationKey configurationKey1 = ConfigurationKey.fromPropertyKey("pk1");
        configurationService.putObject(configurationKey1, value1);
        final Object objectValue1 = configurationService.getObject(configurationKey1);
        Assertions.assertNotNull(objectValue1);
        Assertions.assertEquals(value1, objectValue1);
        configurationService.removeObject(configurationKey1);
        final Object objectValue11 = configurationService.getObject(configurationKey1);
        Assertions.assertNull(objectValue11);

        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration.builder().build();
        final ConfigurationKey configurationKey2 =
                ConfigurationKey.fromTypeKey(DefaultMediatorServiceConfiguration.class);
        configurationService.putObject(configurationKey2, defaultMediatorServiceConfiguration);
        final Object objectValue2 = configurationService.getObject(configurationKey2);
        Assertions.assertNotNull(objectValue2);
        Assertions.assertEquals(defaultMediatorServiceConfiguration, objectValue2);
        configurationService.removeObject(configurationKey2);
        final Object objectValue21 = configurationService.getObject(configurationKey2);
        Assertions.assertNull(objectValue21);

        final ConfigurationKey unknownConfigurationKey = ConfigurationKey.fromPropertyKey("unknownKey");
        final Object unknownValue = configurationService.getObject(unknownConfigurationKey);
        Assertions.assertNull(unknownValue);

        final String value2 = "efgh";
        final Object defaultValue = configurationService.getObject(unknownConfigurationKey, value2);
        Assertions.assertNotNull(defaultValue);
        Assertions.assertEquals(value2, defaultValue);
    }

    @Test
    @Order(13)
    void testTypedConfiguration() {
        final SampleAppbaseServiceConfiguration7 sampleAppbaseServiceConfiguration7 =
                new SampleAppbaseServiceConfiguration7("dc1", "host2", 8090);
        configurationService.putTypedConfiguration(
                SampleAppbaseServiceConfiguration7.class, sampleAppbaseServiceConfiguration7);
        final SampleAppbaseServiceConfiguration7 typedConfiguration =
                configurationService.getTypedConfiguration(SampleAppbaseServiceConfiguration7.class);
        Assertions.assertNotNull(typedConfiguration);
        Assertions.assertEquals(sampleAppbaseServiceConfiguration7, typedConfiguration);
        configurationService.removeTypedConfiguration(SampleAppbaseServiceConfiguration7.class);
        final SampleAppbaseServiceConfiguration7 typedConfiguration1 =
                configurationService.getTypedConfiguration(SampleAppbaseServiceConfiguration7.class);
        Assertions.assertNull(typedConfiguration1);


        final SampleAppbaseServiceConfiguration7 sampleAppbaseServiceConfiguration71 =
                new SampleAppbaseServiceConfiguration7("east", "localhost", 8090);
        final SampleAppbaseServiceConfiguration7 typedConfiguration2 =
                configurationService.getTypedConfiguration(
                        SampleAppbaseServiceConfiguration7.class, sampleAppbaseServiceConfiguration71);
        Assertions.assertNotNull(typedConfiguration2);
        Assertions.assertEquals(sampleAppbaseServiceConfiguration71, typedConfiguration2);

        final SampleAppbaseServiceConfiguration3 typedConfiguration3 =
                configurationService.getTypedConfiguration(SampleAppbaseServiceConfiguration3.class);
        Assertions.assertNotNull(typedConfiguration3);
    }

    @Test
    @Order(14)
    void testContainsProperty() {
        final boolean containsProperty = configurationService.containsProperty("putStringPropertyKey");
        Assertions.assertTrue(containsProperty);
    }

    @Test
    @Order(15)
    void testContainsTypedConfiguration() {
        final boolean containsTypedConfiguration =
                configurationService.containsTypedConfiguration(SampleAppbaseServiceConfiguration4.class);
        Assertions.assertTrue(containsTypedConfiguration);
    }

    @Test
    @Order(16)
    void testRegisterConfigurationTypeByPropertyKey() throws PropertyDeserializerNotFoundException {
        configurationService.registerPropertyDeserializer(new YamlPropertyDeserializer());
        configurationService.registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                "custom.SampleAppbaseServiceConfiguration1",
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        YamlPropertyDeserializer.class, SampleAppbaseServiceConfiguration1.class));
        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType>
                propertyDeserializerAndTypeEntryByPropertyKeyMap =
                configurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        Assertions.assertNotNull(propertyDeserializerAndTypeEntryByPropertyKeyMap);
    }

    @Test
    @Order(17)
    void testRegisterPropertyDeserializer() {
        configurationService.registerPropertyDeserializer(new YamlPropertyDeserializer());
        final Collection<PropertyDeserializer> propertyDeserializers =
                configurationService.getPropertyDeserializers();
        Assertions.assertNotNull(propertyDeserializers);
        Assertions.assertFalse(propertyDeserializers.isEmpty());

        boolean found = false;
        for (final PropertyDeserializer propertyDeserializer : propertyDeserializers) {
            if (propertyDeserializer.getClass().equals(YamlPropertyDeserializer.class)) {
                found = true;
            }
        }
        Assertions.assertTrue(found);

        final Gauge propertyDeserializerByTypeNameMapSizeGauge =
                configurationService.getPropertyDeserializerByTypeNameMapSizeGauge();
        Assertions.assertNotNull(propertyDeserializerByTypeNameMapSizeGauge);

        final Gauge propertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge =
                configurationService.getPropertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge();
        Assertions.assertNotNull(propertyDeserializerAndTypeEntryByPropertyKeyMapSizeGauge);
    }

    @Test
    @Order(18)
    void testRegisterSkipValidation() {
        configurationService.registerNonValidatingType(SampleAppbaseServiceConfiguration2.class);
        final Collection<String> nonValidatingTypeNameSet = configurationService.getNonValidatingTypeNames();
        Assertions.assertNotNull(nonValidatingTypeNameSet);
        final Gauge skipValidationTypeNameSetSizeGauge =
                configurationService.getNonValidatingTypeNamesSizeGauge();
        Assertions.assertNotNull(skipValidationTypeNameSetSizeGauge);
    }

    @Test
    @Order(19)
    void testRegisterGlobalConfigurationListener() {
        configurationService.registerGlobalConfigurationListener(new SampleConfigurationListener1());
        final Collection<ConfigurationListener> globalConfigurationListeners =
                configurationService.getGlobalConfigurationListeners();
        Assertions.assertNotNull(globalConfigurationListeners);
        Assertions.assertFalse(globalConfigurationListeners.isEmpty());
        final Gauge globalConfigurationListenerByTypeNameMapGauge =
                configurationService.getGlobalConfigurationListenerByTypeNameMapSizeGauge();
        Assertions.assertNotNull(globalConfigurationListenerByTypeNameMapGauge);
    }

    @Test
    @Order(20)
    void testRegisterConfigurationListener() {
        final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey("configurationListenerKey");
        configurationService.registerConfigurationListenerByKey(
                new SampleConfigurationListener2(), configurationKey);
        final Map<ConfigurationKey, ConfigurationListener>
                configurationListenerByKeyMap =
                    configurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap);
        Assertions.assertFalse(configurationListenerByKeyMap.isEmpty());
        Assertions.assertTrue(
                configurationListenerByKeyMap.containsKey(configurationKey));
        final Gauge configurationListenerByTypeNameByConfigurationKeyMapGauge =
                configurationService.getConfigurationListenerByTypeNameByConfigurationKeyMapSizeGauge();
        Assertions.assertNotNull(configurationListenerByTypeNameByConfigurationKeyMapGauge);
    }

    @Test
    @Order(21)
    void testRemainingGetters() {
        final Map<String, Object> configurationValueByPropertyNameMap =
                configurationService.getConfigurationByPropertyNameMap();
        Assertions.assertNotNull(configurationValueByPropertyNameMap);
        Assertions.assertFalse(configurationValueByPropertyNameMap.isEmpty());
        final Gauge configurationValueByPropertyNameMapSizeGauge =
                configurationService.getConfigurationValueByPropertyNameMapSizeGauge();
        Assertions.assertNotNull(configurationValueByPropertyNameMapSizeGauge);

        final Map<String, ConfigurationValue<?>> loadedConfigurationValueByPropertyNameMap =
                configurationService.getLoadedConfigurationValueByPropertyNameMap();
        Assertions.assertNotNull(loadedConfigurationValueByPropertyNameMap);
        Assertions.assertFalse(loadedConfigurationValueByPropertyNameMap.isEmpty());
        final Gauge loadedConfigurationValueByPropertyNameMapSizeGauge =
                configurationService.getLoadedConfigurationValueByPropertyNameMapSizeGauge();
        Assertions.assertNotNull(loadedConfigurationValueByPropertyNameMapSizeGauge);

        final Map<String, Object> configurationValueByTypeNameMap =
                configurationService.getConfigurationByTypeNameMap();
        Assertions.assertNotNull(configurationValueByTypeNameMap);
        Assertions.assertFalse(configurationValueByTypeNameMap.isEmpty());
        final Gauge configurationValueByTypeNameMapSizeGauge =
                configurationService.getConfigurationValueByTypeNameMapSizeGauge();
        Assertions.assertNotNull(configurationValueByTypeNameMapSizeGauge);

        final Map<String, ConfigurationValue<?>> loadedConfigurationValueByTypeNameMap =
                configurationService.getLoadedConfigurationValueByTypeNameMap();
        Assertions.assertNotNull(loadedConfigurationValueByTypeNameMap);
        final Gauge loadedConfigurationValueByTypeNameMapSizeGauge =
                configurationService.getLoadedConfigurationValueByTypeNameMapSizeGauge();
        Assertions.assertNotNull(loadedConfigurationValueByTypeNameMapSizeGauge);

        final ConfigurationAccessor configurationAccessor1 = configurationService.getConfigurationAccessor();
        Assertions.assertNotNull(configurationAccessor1);

        final DefaultConfigurationServiceConfiguration configuration = configurationService.getConfiguration();
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configurationService.isInitWithConfigurationFile());

        final Gauge secureFieldNameSetByTypeNameMapSizeGauge =
                configurationService.getSecureFieldNameSetByTypeNameMapSizeGauge();
        Assertions.assertNotNull(secureFieldNameSetByTypeNameMapSizeGauge);

        Assertions.assertNull(
                configurationService.getConfigurationContext(ConfigurationKey.fromPropertyKey("propKey")));
    }

    @Test
    @Order(22)
    void testPropertyDeserializerAndTypeEntry() {
        final ConfigurationService.PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType =
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        YamlPropertyDeserializer.class,
                        DefaultConfigurationServiceConfiguration.class);
        Assertions.assertNotNull(propertyDeserializerAndConfigurationType.getConfigurationType());
        Assertions.assertNotNull(propertyDeserializerAndConfigurationType.getPropertyDeserializerType());

        final ConfigurationService.PropertyDeserializerAndConfigurationType propertyDeserializerAndConfigurationType1 =
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        YamlPropertyDeserializer.class,
                        DefaultConfigurationServiceConfiguration.class);
        Assertions.assertEquals(propertyDeserializerAndConfigurationType, propertyDeserializerAndConfigurationType1);
        Assertions.assertEquals(
                propertyDeserializerAndConfigurationType.hashCode(),
                propertyDeserializerAndConfigurationType1.hashCode());
        Assertions.assertEquals(
                propertyDeserializerAndConfigurationType.toString(),
                propertyDeserializerAndConfigurationType1.toString());
    }

    @Test
    @Order(23)
    void testSecureTypes() {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        localConfigurationService.clearSecureTypes();

        localConfigurationService.registerSecureType(DefaultLogServiceConfiguration.class);
        final Collection<String> secureTypeNames = localConfigurationService.getSecureTypeNames();
        Assertions.assertNotNull(secureTypeNames);
        Assertions.assertEquals(1, secureTypeNames.size());
        Assertions.assertTrue(secureTypeNames.contains(DefaultLogServiceConfiguration.class.getName()));

        localConfigurationService.unregisterSecureType(DefaultLogServiceConfiguration.class);
        final Collection<String> secureTypeNames1 = localConfigurationService.getSecureTypeNames();
        Assertions.assertNotNull(secureTypeNames1);
        Assertions.assertTrue(secureTypeNames1.isEmpty());

        localConfigurationService.registerSecureType(DefaultLogServiceConfiguration.class);
        final Collection<String> secureTypeNames2 = localConfigurationService.getSecureTypeNames();
        Assertions.assertNotNull(secureTypeNames2);
        Assertions.assertEquals(1, secureTypeNames2.size());
        Assertions.assertTrue(secureTypeNames2.contains(DefaultLogServiceConfiguration.class.getName()));

        localConfigurationService.clearSecureTypes();
        final Collection<String> secureTypeNames3 = localConfigurationService.getSecureTypeNames();
        Assertions.assertNotNull(secureTypeNames3);
        Assertions.assertTrue(secureTypeNames3.isEmpty());
    }

    @Test
    @Order(24)
    void testSecurePropertyKeys() {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        localConfigurationService.clearSecurePropertyKeys();

        final String key = "securePropertyKey123";
        localConfigurationService.registerSecurePropertyKey(key);
        final Collection<String> securePropertyKeys = localConfigurationService.getSecurePropertyKeys();
        Assertions.assertNotNull(securePropertyKeys);
        Assertions.assertEquals(1, securePropertyKeys.size());
        Assertions.assertTrue(securePropertyKeys.contains(key));

        localConfigurationService.unregisterSecurePropertyKey(key);
        final Collection<String> securePropertyKeys1 = localConfigurationService.getSecurePropertyKeys();
        Assertions.assertNotNull(securePropertyKeys1);
        Assertions.assertTrue(securePropertyKeys1.isEmpty());

        localConfigurationService.registerSecurePropertyKey(key);
        final Collection<String> securePropertyKeys2 = localConfigurationService.getSecurePropertyKeys();
        Assertions.assertEquals(1, securePropertyKeys2.size());
        Assertions.assertTrue(securePropertyKeys2.contains(key));

        localConfigurationService.clearSecurePropertyKeys();
        final Collection<String> securePropertyKeys3 = localConfigurationService.getSecurePropertyKeys();
        Assertions.assertNotNull(securePropertyKeys3);
        Assertions.assertTrue(securePropertyKeys3.isEmpty());
    }

    @Test
    @Order(25)
    void testSecureFieldByConfigurationTypeMap() throws NoSuchFieldException {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        configurationService.clearSecureFieldByConfigurationTypeMap();

        localConfigurationService.registerSecureFieldByConfigurationType(
                DefaultLogServiceConfiguration.class, "logbackXml");
        final Map<String, Collection<String>> secureFieldNamesByConfigurationTypeNameMap =
                localConfigurationService.getSecureFieldNamesByConfigurationTypeNameMap();
        Assertions.assertNotNull(secureFieldNamesByConfigurationTypeNameMap);
        Assertions.assertEquals(1, secureFieldNamesByConfigurationTypeNameMap.size());
        Assertions.assertTrue(secureFieldNamesByConfigurationTypeNameMap
                .containsKey(DefaultLogServiceConfiguration.class.getName()));

        localConfigurationService.unregisterSecureFieldByConfigurationType(
                DefaultLogServiceConfiguration.class, "logbackXml");
        final Map<String, Collection<String>> secureFieldNamesByConfigurationTypeNameMap1 =
                localConfigurationService.getSecureFieldNamesByConfigurationTypeNameMap();
        Assertions.assertNotNull(secureFieldNamesByConfigurationTypeNameMap1);
        Assertions.assertTrue(secureFieldNamesByConfigurationTypeNameMap1.isEmpty());

        localConfigurationService.registerSecureFieldByConfigurationType(
                DefaultLogServiceConfiguration.class, "logbackXml");
        final Map<String, Collection<String>> secureFieldNamesByConfigurationTypeNameMap2 =
                localConfigurationService.getSecureFieldNamesByConfigurationTypeNameMap();
        Assertions.assertNotNull(secureFieldNamesByConfigurationTypeNameMap2);
        Assertions.assertEquals(1, secureFieldNamesByConfigurationTypeNameMap2.size());
        Assertions.assertTrue(secureFieldNamesByConfigurationTypeNameMap2
                .containsKey(DefaultLogServiceConfiguration.class.getName()));

        localConfigurationService.clearSecureFieldByConfigurationTypeMap();
        final Map<String, Collection<String>> secureFieldNamesByConfigurationTypeNameMap3 =
                localConfigurationService.getSecureFieldNamesByConfigurationTypeNameMap();
        Assertions.assertNotNull(secureFieldNamesByConfigurationTypeNameMap3);
        Assertions.assertTrue(secureFieldNamesByConfigurationTypeNameMap3.isEmpty());
    }

    @Test
    @Order(26)
    void testPropertyDeserializers() throws PropertyDeserializerNotFoundException {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        localConfigurationService.clearPropertyDeserializers();
        localConfigurationService.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();

        final String key = "propertyKey2409";
        localConfigurationService.registerPropertyDeserializer(new YamlPropertyDeserializer());
        localConfigurationService.registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                key,
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        YamlPropertyDeserializer.class,
                        DefaultLogServiceConfiguration.class));
        final Collection<PropertyDeserializer> propertyDeserializers =
                localConfigurationService.getPropertyDeserializers();
        Assertions.assertNotNull(propertyDeserializers);
        Assertions.assertEquals(1, propertyDeserializers.size());
        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType>
                propertyDeserializerAndConfigurationTypesByPropertyKeyMap =
                localConfigurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        Assertions.assertNotNull(propertyDeserializerAndConfigurationTypesByPropertyKeyMap);
        Assertions.assertEquals(1, propertyDeserializerAndConfigurationTypesByPropertyKeyMap.size());

        localConfigurationService.unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey(key);
        localConfigurationService.unregisterPropertyDeserializer(YamlPropertyDeserializer.class);

        final Collection<PropertyDeserializer> propertyDeserializers1 =
                localConfigurationService.getPropertyDeserializers();
        Assertions.assertNotNull(propertyDeserializers1);
        Assertions.assertTrue(propertyDeserializers1.isEmpty());

        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType>
                propertyDeserializerAndConfigurationTypesByPropertyKeyMap1 =
                localConfigurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        Assertions.assertNotNull(propertyDeserializerAndConfigurationTypesByPropertyKeyMap1);
        Assertions.assertTrue(propertyDeserializerAndConfigurationTypesByPropertyKeyMap1.isEmpty());

        localConfigurationService.registerPropertyDeserializer(new YamlPropertyDeserializer());
        localConfigurationService.registerPropertyDeserializerAndConfigurationTypesByPropertyKey(
                key,
                new ConfigurationService.PropertyDeserializerAndConfigurationType(
                        YamlPropertyDeserializer.class,
                        DefaultLogServiceConfiguration.class));
        final Collection<PropertyDeserializer> propertyDeserializers2 =
                localConfigurationService.getPropertyDeserializers();
        Assertions.assertNotNull(propertyDeserializers2);
        Assertions.assertEquals(1, propertyDeserializers2.size());
        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType>
                propertyDeserializerAndConfigurationTypesByPropertyKeyMap2 =
                localConfigurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        Assertions.assertNotNull(propertyDeserializerAndConfigurationTypesByPropertyKeyMap2);
        Assertions.assertEquals(1, propertyDeserializerAndConfigurationTypesByPropertyKeyMap2.size());

        localConfigurationService.clearPropertyDeserializers();
        localConfigurationService.clearPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();

        final Collection<PropertyDeserializer> propertyDeserializers3 =
                localConfigurationService.getPropertyDeserializers();
        Assertions.assertNotNull(propertyDeserializers3);
        Assertions.assertTrue(propertyDeserializers3.isEmpty());

        final Map<String, ConfigurationService.PropertyDeserializerAndConfigurationType>
                propertyDeserializerAndConfigurationTypesByPropertyKeyMap3 =
                localConfigurationService.getPropertyDeserializerAndConfigurationTypesByPropertyKeyMap();
        Assertions.assertNotNull(propertyDeserializerAndConfigurationTypesByPropertyKeyMap3);
        Assertions.assertTrue(propertyDeserializerAndConfigurationTypesByPropertyKeyMap3.isEmpty());
    }

    @Test
    @Order(28)
    void testNonValidatingTypes() {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        localConfigurationService.clearNonValidatingTypes();

        localConfigurationService.registerNonValidatingType(DefaultLogServiceConfiguration.class);
        final Collection<String> nonValidatingTypeNames = localConfigurationService.getNonValidatingTypeNames();
        Assertions.assertNotNull(nonValidatingTypeNames);
        Assertions.assertEquals(1, nonValidatingTypeNames.size());

        localConfigurationService.unregisterNonValidatingType(DefaultLogServiceConfiguration.class);
        final Collection<String> nonValidatingTypeNames1 = localConfigurationService.getNonValidatingTypeNames();
        Assertions.assertNotNull(nonValidatingTypeNames1);
        Assertions.assertTrue(nonValidatingTypeNames1.isEmpty());

        localConfigurationService.registerNonValidatingType(DefaultLogServiceConfiguration.class);
        final Collection<String> nonValidatingTypeNames2 = localConfigurationService.getNonValidatingTypeNames();
        Assertions.assertNotNull(nonValidatingTypeNames2);
        Assertions.assertEquals(1, nonValidatingTypeNames2.size());

        localConfigurationService.clearNonValidatingTypes();
        final Collection<String> nonValidatingTypeNames3 = localConfigurationService.getNonValidatingTypeNames();
        Assertions.assertNotNull(nonValidatingTypeNames3);
        Assertions.assertTrue(nonValidatingTypeNames3.isEmpty());
    }

    @Test
    @Order(29)
    void testManagedServiceByConfigurationKeyMap() {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        localConfigurationService.clearManagedServiceByConfigurationKeyMap();

        final String key = "key2330";
        localConfigurationService.registerManagedServiceByConfigurationKey(
                new TestAppbaseService(), ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap =
                localConfigurationService.getManagedServiceByConfigurationKeyMap();
        Assertions.assertNotNull(managedServiceByConfigurationKeyMap);
        Assertions.assertEquals(1, managedServiceByConfigurationKeyMap.size());

        localConfigurationService.unregisterManagedServiceByConfigurationKey(ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap1 =
                localConfigurationService.getManagedServiceByConfigurationKeyMap();
        Assertions.assertNotNull(managedServiceByConfigurationKeyMap1);
        Assertions.assertTrue(managedServiceByConfigurationKeyMap1.isEmpty());

        localConfigurationService.registerManagedServiceByConfigurationKey(
                new TestAppbaseService(), ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap2 =
                localConfigurationService.getManagedServiceByConfigurationKeyMap();
        Assertions.assertNotNull(managedServiceByConfigurationKeyMap2);
        Assertions.assertEquals(1, managedServiceByConfigurationKeyMap2.size());

        localConfigurationService.unregisterManagedServiceByConfigurationKey(
                TestAppbaseService.class, ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap3 =
                localConfigurationService.getManagedServiceByConfigurationKeyMap();
        Assertions.assertNotNull(managedServiceByConfigurationKeyMap3);
        Assertions.assertTrue(managedServiceByConfigurationKeyMap3.isEmpty());


        localConfigurationService.registerManagedServiceByConfigurationKey(
                new TestAppbaseService(), ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap4 =
                localConfigurationService.getManagedServiceByConfigurationKeyMap();
        Assertions.assertNotNull(managedServiceByConfigurationKeyMap4);
        Assertions.assertEquals(1, managedServiceByConfigurationKeyMap4.size());

        localConfigurationService.clearManagedServiceByConfigurationKeyMap();
        final Map<ConfigurationKey, AppbaseService> managedServiceByConfigurationKeyMap5 =
                localConfigurationService.getManagedServiceByConfigurationKeyMap();
        Assertions.assertNotNull(managedServiceByConfigurationKeyMap5);
        Assertions.assertTrue(managedServiceByConfigurationKeyMap5.isEmpty());
    }

    @Test
    @Order(30)
    void testGlobalConfigurationListeners() {
        final ConfigurationService localConfigurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);
        localConfigurationService.clearGlobalConfigurationListeners();

        localConfigurationService.registerGlobalConfigurationListener(new SampleConfigurationListener1());
        final Collection<ConfigurationListener> globalConfigurationListeners =
                localConfigurationService.getGlobalConfigurationListeners();
        Assertions.assertNotNull(globalConfigurationListeners);
        Assertions.assertEquals(1, globalConfigurationListeners.size());

        localConfigurationService.unregisterGlobalConfigurationListener(SampleConfigurationListener1.class);
        final Collection<ConfigurationListener> globalConfigurationListeners1 =
                localConfigurationService.getGlobalConfigurationListeners();
        Assertions.assertNotNull(globalConfigurationListeners1);
        Assertions.assertTrue(globalConfigurationListeners1.isEmpty());

        localConfigurationService.registerGlobalConfigurationListener(new SampleConfigurationListener1());
        final Collection<ConfigurationListener> globalConfigurationListeners2 =
                localConfigurationService.getGlobalConfigurationListeners();
        Assertions.assertNotNull(globalConfigurationListeners2);
        Assertions.assertEquals(1, globalConfigurationListeners2.size());

        localConfigurationService.clearGlobalConfigurationListeners();
        final Collection<ConfigurationListener> globalConfigurationListeners3 =
                localConfigurationService.getGlobalConfigurationListeners();
        Assertions.assertNotNull(globalConfigurationListeners3);
        Assertions.assertTrue(globalConfigurationListeners3.isEmpty());
    }

    @Test
    @Order(31)
    void testConfigurationListenerByKeyMap() {
        final DefaultConfigurationService localConfigurationService =
                CoreTestFixtures.getEmptyDefaultConfigurationService(
                        logService,
                        metricService,
                        mediatorService,
                        validationService);
        localConfigurationService.clearConfigurationListenerByKeyMap();

        final String key = "key2409";
        localConfigurationService.registerConfigurationListenerByKey(
                new SampleConfigurationListener2(), ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap =
                localConfigurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap);
        Assertions.assertEquals(1, configurationListenerByKeyMap.size());

        localConfigurationService.unregisterConfigurationListenerByKey(ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap1 =
                localConfigurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap1);
        Assertions.assertTrue(configurationListenerByKeyMap1.isEmpty());

        localConfigurationService.registerConfigurationListenerByKey(
                new SampleConfigurationListener2(), ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap2 =
                localConfigurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap2);
        Assertions.assertEquals(1, configurationListenerByKeyMap2.size());

        localConfigurationService.unregisterConfigurationListenerByKey(
                SampleConfigurationListener2.class, ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap3 =
                localConfigurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap3);
        Assertions.assertTrue(configurationListenerByKeyMap3.isEmpty());

        localConfigurationService.registerConfigurationListenerByKey(
                new SampleConfigurationListener2(), ConfigurationKey.fromPropertyKey(key));
        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap4 =
                localConfigurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap4);
        Assertions.assertEquals(1, configurationListenerByKeyMap4.size());

        localConfigurationService.clearConfigurationListenerByKeyMap();
        final Map<ConfigurationKey, ConfigurationListener> configurationListenerByKeyMap5 =
                localConfigurationService.getConfigurationListenerByKeyMap();
        Assertions.assertNotNull(configurationListenerByKeyMap5);
        Assertions.assertTrue(configurationListenerByKeyMap5.isEmpty());

        Assertions.assertNotNull(
                localConfigurationService.getManagedServiceByTypeNameByConfigurationKeyMapSizeGauge());
        Assertions.assertNotNull(
                localConfigurationService.getSecurePropertyKeysSizeGauge());
        Assertions.assertNotNull(
                localConfigurationService.getSecureTypeNameKeysSizeGauge());
        Assertions.assertNotNull(
                localConfigurationService);
        Assertions.assertNotNull(
                localConfigurationService);
    }

    @Test
    @Order(32)
    void testNotifyMethods() {
        final DefaultConfigurationService localConfigurationService =
                CoreTestFixtures.getEmptyDefaultConfigurationService(
                        logService,
                        metricService,
                        mediatorService,
                        validationService);
        localConfigurationService.registerConfigurationListenerByKey(
                new SampleConfigurationListener1(),
                ConfigurationKey.fromTypeKey(SampleAppbaseServiceConfiguration4.class));
        localConfigurationService.registerGlobalConfigurationListener(new SampleConfigurationListener2());

        final int globalConfigurationCount = globalConfigurationCounter.get();
        final int configurationCount = configurationCounter.get();

        final SampleAppbaseServiceConfiguration4 sampleAppbaseServiceConfiguration4 =
                new SampleAppbaseServiceConfiguration4("DC", "host", 2409);
        final ConfigurationAddedEvent configurationAddedEvent = new ConfigurationAddedEvent(
                ConfigurationKey.fromTypeKey(SampleAppbaseServiceConfiguration4.class),
                ConfigurationValue.fromValue(sampleAppbaseServiceConfiguration4),
                System.currentTimeMillis());
        localConfigurationService.notifyConfigurationAddedEvent(configurationAddedEvent);
        final int globalCount2 = globalConfigurationCounter.get();
        final int configurationCount2 = configurationCounter.get();
        Assertions.assertTrue(globalCount2 > globalConfigurationCount);
        Assertions.assertTrue(configurationCount2 > configurationCount);

        final SampleAppbaseServiceConfiguration4 sampleAppbaseServiceConfiguration41 =
                new SampleAppbaseServiceConfiguration4("DC1", "host1", 9024);
        final ConfigurationChangedEvent configurationChangedEvent = new ConfigurationChangedEvent(
                ConfigurationKey.fromTypeKey(SampleAppbaseServiceConfiguration4.class),
                ConfigurationValue.fromValue(sampleAppbaseServiceConfiguration4),
                ConfigurationValue.fromValue(sampleAppbaseServiceConfiguration41),
                System.currentTimeMillis());
        localConfigurationService.notifyConfigurationChangedEvent(configurationChangedEvent);
        final int globalCount3 = globalConfigurationCounter.get();
        final int configurationCount3 = configurationCounter.get();
        Assertions.assertTrue(globalCount3 > globalCount2);
        Assertions.assertTrue(configurationCount3 > configurationCount2);

        final ConfigurationRemovedEvent configurationRemovedEvent =
                new ConfigurationRemovedEvent(
                        ConfigurationKey.fromTypeKey(SampleAppbaseServiceConfiguration4.class),
                        System.currentTimeMillis());
        localConfigurationService.notifyConfigurationRemovedEvent(configurationRemovedEvent);
        Assertions.assertTrue(globalConfigurationCounter.get() > globalCount3);
        Assertions.assertTrue(configurationCounter.get() > configurationCount3);
    }

    /**
     * Lens 5 (second pass) S2 regression: {@code sendAddedEventToListeners}' per-listener
     * {@code catch (Exception)} (swallow, increment the failure counter, keep going) was never
     * entered by any test in the suite -- every existing listener either logs and increments a
     * counter or does nothing, so the catch branch was unexercised and a regression that made it
     * either rethrow or abort the remaining listeners would not have failed anything. Uses
     * dedicated local counters rather than the shared static {@code globalConfigurationCounter}/
     * {@code configurationCounter} fields so this test needs no baseline snapshot and cannot be
     * perturbed by any other test's use of those fields.
     */
    @Test
    void sendAddedEventToListenersContinuesToTheNextListenerAfterOneThrows() {
        final DefaultConfigurationService localConfigurationService =
                CoreTestFixtures.getEmptyDefaultConfigurationService(
                        logService, metricService, mediatorService, validationService);

        final AtomicInteger secondListenerInvocations = new AtomicInteger(0);
        localConfigurationService.registerGlobalConfigurationListener(new ConfigurationListener() {
            @Override
            public void onConfigurationAdded(final ConfigurationAddedEvent configurationAddedEvent) {
                throw new RuntimeException("S2 regression test: deliberate listener failure");
            }

            @Override
            public void onConfigurationChanged(final ConfigurationChangedEvent configurationChangedEvent) {
            }

            @Override
            public void onConfigurationRemoved(final ConfigurationRemovedEvent configurationRemovedEvent) {
            }
        });
        localConfigurationService.registerGlobalConfigurationListener(new ConfigurationListener() {
            @Override
            public void onConfigurationAdded(final ConfigurationAddedEvent configurationAddedEvent) {
                secondListenerInvocations.incrementAndGet();
            }

            @Override
            public void onConfigurationChanged(final ConfigurationChangedEvent configurationChangedEvent) {
            }

            @Override
            public void onConfigurationRemoved(final ConfigurationRemovedEvent configurationRemovedEvent) {
            }
        });

        final SampleAppbaseServiceConfiguration4 sampleAppbaseServiceConfiguration4 =
                new SampleAppbaseServiceConfiguration4("DC", "host", 2409);
        final ConfigurationAddedEvent configurationAddedEvent = new ConfigurationAddedEvent(
                ConfigurationKey.fromTypeKey(SampleAppbaseServiceConfiguration4.class),
                ConfigurationValue.fromValue(sampleAppbaseServiceConfiguration4),
                System.currentTimeMillis());

        Assertions.assertDoesNotThrow(
                () -> localConfigurationService.notifyConfigurationAddedEvent(configurationAddedEvent),
                "a listener's exception must not propagate out of dispatch -- the per-listener catch "
                        + "exists precisely so one bad listener cannot block the rest");
        Assertions.assertEquals(1, secondListenerInvocations.get(),
                "dispatch must continue to the next listener after an earlier one throws");
    }

    private static class TestAppbaseService implements AppbaseService {
        @Override
        public <C extends AppbaseConfiguration> void reload(
                final C configuration) throws ServiceReloadException
        {
        }

        @Override
        public ServiceStatus getStatus() {
            return ServiceStatus.STARTED;
        }

        @Override
        public <C extends AppbaseConfiguration> C getConfiguration() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public void shutdown() throws AppbaseRuntimeException {
        }
    }

    public static class SampleAppbaseServiceConfiguration1 implements AppbaseConfiguration {
        @NotNull
        private final String hostname;
        @PositiveOrZero
        private final Integer port;

        @JsonCreator
        public SampleAppbaseServiceConfiguration1(
                @JsonProperty("hostname")
                final String hostname,
                @JsonProperty("port")
                final Integer port)
        {
            this.hostname = hostname;
            this.port = port;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleAppbaseServiceConfiguration1 that = (SampleAppbaseServiceConfiguration1) o;
            return Objects.equals(hostname, that.hostname) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hostname, port);
        }

        @Override
        public String toString() {
            return "SampleAppbaseServiceConfiguration1{" +
                    "hostname='" + hostname + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class SampleAppbaseServiceConfiguration2 implements AppbaseConfiguration {
        @NotNull
        private final String hostname;

        @PositiveOrZero
        private final Integer port;

        @JsonCreator
        public SampleAppbaseServiceConfiguration2(
                @JsonProperty("hostname")
                final String hostname,
                @JsonProperty("port")
                final Integer port)
        {
            this.hostname = hostname;
            this.port = port;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleAppbaseServiceConfiguration2 that = (SampleAppbaseServiceConfiguration2) o;
            return Objects.equals(hostname, that.hostname) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hostname, port);
        }

        @Override
        public String toString() {
            return "SampleAppbaseServiceConfiguration2{" +
                    "hostname='" + hostname + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class SampleAppbaseServiceConfiguration3 implements AppbaseConfiguration {
        @NotNull
        private final String dc;

        @NotNull
        private final String hostname;

        @PositiveOrZero
        private final Integer port;

        @JsonCreator
        public SampleAppbaseServiceConfiguration3(
                @NotNull
                @JsonProperty("dc")
                final String dc,
                @NotNull
                @JsonProperty("hostname")
                final String hostname,
                @PositiveOrZero
                @JsonProperty("port")
                final Integer port)
        {
            this.dc = dc;
            this.hostname = hostname;
            this.port = port;
        }

        public String getDc() {
            return dc;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleAppbaseServiceConfiguration3 that = (SampleAppbaseServiceConfiguration3) o;
            return Objects.equals(dc, that.dc) &&
                    Objects.equals(hostname, that.hostname) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dc, hostname, port);
        }

        @Override
        public String toString() {
            return "SampleAppbaseServiceConfiguration3{" +
                    "dc='" + dc + '\'' +
                    ", hostname='" + hostname + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class SampleAppbaseServiceConfiguration4 implements AppbaseConfiguration {
        @NotNull
        private final String dc;

        @NotNull
        private final String hostname;

        @PositiveOrZero
        private final Integer port;

        @JsonCreator
        public SampleAppbaseServiceConfiguration4(
                @NotNull
                @JsonProperty("dc")
                final String dc,
                @NotNull
                @JsonProperty("hostname")
                final String hostname,
                @PositiveOrZero
                @JsonProperty("port")
                final Integer port)
        {
            this.dc = dc;
            this.hostname = hostname;
            this.port = port;
        }

        public String getDc() {
            return dc;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleAppbaseServiceConfiguration4 that = (SampleAppbaseServiceConfiguration4) o;
            return Objects.equals(dc, that.dc) &&
                    Objects.equals(hostname, that.hostname) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dc, hostname, port);
        }

        @Override
        public String toString() {
            return "SampleAppbaseServiceConfiguration4{" +
                    "dc='" + dc + '\'' +
                    ", hostname='" + hostname + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    public static class SampleAppbaseServiceConfiguration5 implements AppbaseConfiguration {
        @NotNull
        private final String dc;

        @NotNull
        private final String hostname;

        @PositiveOrZero
        private final Integer port;

        @JsonCreator
        public SampleAppbaseServiceConfiguration5(
                @NotNull
                @JsonProperty("dc")
                final String dc,
                @NotNull
                @JsonProperty("hostname")
                final String hostname,
                @PositiveOrZero
                @JsonProperty("port")
                final Integer port)
        {
            this.dc = dc;
            this.hostname = hostname;
            this.port = port;
        }

        public String getDc() {
            return dc;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleAppbaseServiceConfiguration5 that = (SampleAppbaseServiceConfiguration5) o;
            return Objects.equals(dc, that.dc) &&
                    Objects.equals(hostname, that.hostname) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dc, hostname, port);
        }

        @Override
        public String toString() {
            return "SampleAppbaseServiceConfiguration5{" +
                    "dc='" + dc + '\'' +
                    ", hostname='" + hostname + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    private static class SampleAppbaseServiceConfiguration7 implements AppbaseConfiguration {
        @NotNull
        private final String dc;

        @NotNull
        private final String hostname;

        @PositiveOrZero
        private final Integer port;

        @JsonCreator
        SampleAppbaseServiceConfiguration7(
                @NotNull
                @JsonProperty("dc")
                final String dc,
                @NotNull
                @JsonProperty("hostname")
                final String hostname,
                @PositiveOrZero
                @JsonProperty("port")
                final Integer port)
        {
            this.dc = dc;
            this.hostname = hostname;
            this.port = port;
        }

        public String getDc() {
            return dc;
        }

        public String getHostname() {
            return hostname;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final SampleAppbaseServiceConfiguration7 that = (SampleAppbaseServiceConfiguration7) o;
            return Objects.equals(dc, that.dc) &&
                    Objects.equals(hostname, that.hostname) &&
                    Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dc, hostname, port);
        }

        @Override
        public String toString() {
            return "SampleAppbaseServiceConfiguration7{" +
                    "dc='" + dc + '\'' +
                    ", hostname='" + hostname + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    private static class SampleConfigurationListener1 implements ConfigurationListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(SampleConfigurationListener1.class);

        @Override
        public void onConfigurationAdded(final ConfigurationAddedEvent configurationAddedEvent) {
            LOGGER.info("Configuration added: {}", configurationAddedEvent);
            globalConfigurationCounter.incrementAndGet();
        }

        @Override
        public void onConfigurationChanged(final ConfigurationChangedEvent configurationChangedEvent) {
            LOGGER.info("Configuration changed: {}", configurationChangedEvent);
            globalConfigurationCounter.incrementAndGet();

        }

        @Override
        public void onConfigurationRemoved(final ConfigurationRemovedEvent configurationRemovedEvent) {
            LOGGER.info("Configuration removed: {}", configurationRemovedEvent);
            globalConfigurationCounter.incrementAndGet();
        }
    }

    private static class SampleConfigurationListener2 implements ConfigurationListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(SampleConfigurationListener2.class);

        @Override
        public void onConfigurationAdded(final ConfigurationAddedEvent configurationAddedEvent) {
            LOGGER.info("Configuration added: {}", configurationAddedEvent);
            configurationCounter.incrementAndGet();
        }

        @Override
        public void onConfigurationChanged(final ConfigurationChangedEvent configurationChangedEvent) {
            LOGGER.info("Configuration changed: {}", configurationChangedEvent);
            configurationCounter.incrementAndGet();
        }

        @Override
        public void onConfigurationRemoved(final ConfigurationRemovedEvent configurationRemovedEvent) {
            LOGGER.info("Configuration removed: {}", configurationRemovedEvent);
            configurationCounter.incrementAndGet();
        }
    }

    private interface ISampleService1 extends AppbaseService {
    }

    private static class SampleService1 implements ISampleService1 {
        private static final Logger LOGGER = LoggerFactory.getLogger(SampleService1.class);
        private final AtomicReference<SampleAppbaseServiceConfiguration1> sampleAppbaseServiceConfiguration1Ref;

        SampleService1() {
            sampleAppbaseServiceConfiguration1Ref = new AtomicReference<>();
        }

        @Override
        public <C extends AppbaseConfiguration> void reload(
                final C configuration) throws ServiceReloadException
        {
            LOGGER.info("Reloading {} on configuration change", getClass().getName());
        }

        @Override
        public ServiceStatus getStatus() {
            return ServiceStatus.STARTED;
        }

        @Override
        @SuppressWarnings("unchecked")
        public SampleAppbaseServiceConfiguration1 getConfiguration() {
            return sampleAppbaseServiceConfiguration1Ref.get();
        }

        @Override
        public boolean isShutdown() {
            return getStatus() == ServiceStatus.SHUTDOWN;
        }

        @Override
        public void shutdown() throws AppbaseRuntimeException {
            LOGGER.info("Shutting down {}", getClass().getName());
        }
    }
}
