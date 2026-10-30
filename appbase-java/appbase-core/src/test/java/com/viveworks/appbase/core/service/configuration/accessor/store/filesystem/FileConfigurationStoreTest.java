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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.environment.DefaultEnvironmentResolver;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.configuration.accessor.target.TargetResolver;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileConfigurationStoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static String derivedFileBaseName;
    private static Path derivedFilePath;
    private static Properties properties;
    private static FileConfigurationStore fileConfigurationStore1;
    private static FileConfigurationStore fileConfigurationStore2;
    private static Path filePath;

    @BeforeAll
    static void setUp() throws Exception {
        System.setProperty(AppbaseConstants.APPBASE_SYSTEM_PROPERTY, "test-dev");

        derivedFileBaseName = System.getProperty("java.io.tmpdir") +
                File.separator +
                FileConfigurationStoreTest.class.getName() +
                "-derived-" +
                System.nanoTime();
        final String derivedFilename = derivedFileBaseName + "-test-dev.properties";
        derivedFilePath = Files.createFile(Path.of(derivedFilename));

        properties = new Properties();
        properties.put("ABC", "ZZZ");
        properties.put("DEF", "YYY");
        properties.store(new FileOutputStream(derivedFilePath.toFile()), "comment");

        final String filename = System.getProperty("java.io.tmpdir") +
                File.separator +
                FileConfigurationStoreTest.class.getName() +
                "-file-" +
                System.nanoTime() +
                "-test-dev.properties";
        filePath = Files.createFile(Path.of(filename));

        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fileConfigurationStore1 != null) {
            fileConfigurationStore1.shutdown();
        }

        if (fileConfigurationStore2 != null) {
            fileConfigurationStore2.shutdown();
        }

        if (derivedFilePath != null) {
            Files.deleteIfExists(derivedFilePath);
        }

        if (filePath != null) {
            Files.deleteIfExists(filePath);
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
    void testDerivedFilenameConstructorAndRead() throws Exception {
        final DefaultEnvironmentResolver environmentResolver = new DefaultEnvironmentResolver();

        final Map<String, MimeType> mimeTypeByExtensionMap = new HashMap<>();
        mimeTypeByExtensionMap.put("properties", MimeType.PROPERTIES);
        final DefaultFileMimeTypeResolver fileMimeTypeResolver =
                new DefaultFileMimeTypeResolver(mimeTypeByExtensionMap);

        final String derivedFilename = derivedFileBaseName + ".properties";
        final FileTargetResolver targetResolver = new FileTargetResolver();
        fileConfigurationStore1 = new FileConfigurationStore(
                derivedFilePath.getParent(),
                derivedFilename,
                environmentResolver,
                fileMimeTypeResolver,
                targetResolver);
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final Collection<RawConfiguration> rawConfigurations =
                fileConfigurationStore1.readRawConfigurations(configurationAccessorContext);

        Assertions.assertNotNull(rawConfigurations);
        Assertions.assertEquals(1, rawConfigurations.size());

        boolean found = false;
        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            Assertions.assertEquals(MimeType.PROPERTIES, rawConfiguration.getMimeType());

            final byte[] bytes = rawConfiguration.getValue();
            final Properties properties1 = new Properties();
            properties1.load(new ByteArrayInputStream(bytes));

            Assertions.assertEquals(2, properties1.size());
            Assertions.assertEquals("ZZZ", properties1.getProperty("ABC"));
            Assertions.assertEquals("YYY", properties1.getProperty("DEF"));

            // There is only 1 element, break
            found = true;
            break;
        }
        Assertions.assertTrue(found);
    }

    @Test
    @Order(2)
    void testSimpleConstructorAndWrite() throws Exception {
        properties.put("GHI", "XXX");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        properties.store(outputStream, "comment");

        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(getClass().getName())
                .mimeType(MimeType.PROPERTIES)
                .timestamp(System.currentTimeMillis())
                .value(outputStream.toByteArray())
                .build();
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final File file = filePath.toFile();
        fileConfigurationStore2 = new FileConfigurationStore(file.getAbsolutePath(), MimeType.PROPERTIES);
        fileConfigurationStore2.writeRawConfiguration(rawConfiguration, configurationAccessorContext);

        final Properties properties1 = new Properties();
        properties1.load(new FileInputStream(file));
        Assertions.assertEquals(3, properties1.size());
        Assertions.assertEquals("ZZZ", properties1.getProperty("ABC"));
        Assertions.assertEquals("YYY", properties1.getProperty("DEF"));
        Assertions.assertEquals("XXX", properties1.getProperty("GHI"));
    }

    @Test
    @Order(3)
    void resolvedTargetOutsideExplicitRootIsRejectedBeforeAccess()
            throws Exception
    {
        final Path root = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-root-");
        final Path outsideDir = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-outside-");
        final Path resolvedTarget = outsideDir.resolve("escaped.properties");
        final TargetResolver noConfinementResolver = (targetName, environment) -> resolvedTarget.toString();

        final Map<String, MimeType> mimeTypeByExtensionMap = new HashMap<>();
        mimeTypeByExtensionMap.put("properties", MimeType.PROPERTIES);
        final DefaultFileMimeTypeResolver fileMimeTypeResolver =
                new DefaultFileMimeTypeResolver(mimeTypeByExtensionMap);
        final DefaultEnvironmentResolver environmentResolver = new DefaultEnvironmentResolver();

        try {
            Assertions.assertThrows(AppbaseRuntimeException.class,
                    () -> new FileConfigurationStore(
                            root,
                            "ignored-base-name.properties",
                            environmentResolver,
                            fileMimeTypeResolver,
                            noConfinementResolver));
            Assertions.assertFalse(Files.exists(resolvedTarget));
        } finally {
            Files.deleteIfExists(root);
            Files.deleteIfExists(outsideDir);
        }
    }

    @Test
    @Order(4)
    void writeRejectsLeafSymlinkWithoutTouchingItsOutsideTarget() throws Exception {
        final Path root = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-leaf-root-");
        final Path outside = Files.createTempFile(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-leaf-outside-", ".properties");
        final Path target = root.resolve("config.properties");
        Files.writeString(outside, "sentinel=outside");
        Files.createSymbolicLink(target, outside);

        final FileConfigurationStore store = new FileConfigurationStore(root, target.toString(), MimeType.PROPERTIES);
        try {
            Assertions.assertThrows(ConfigurationStoreException.class,
                    () -> store.writeRawConfiguration(rawConfiguration("sentinel=changed"), accessorContext()));
            Assertions.assertEquals("sentinel=outside", Files.readString(outside));
            Assertions.assertTrue(Files.isSymbolicLink(target));
        } finally {
            store.shutdown();
            Files.deleteIfExists(target);
            Files.deleteIfExists(outside);
            Files.deleteIfExists(root);
        }
    }

    @Test
    @Order(5)
    void writeRejectsRetainedParentMovedOutsideRootAndReplacedBySymlink() throws Exception {
        final Path root = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-race-root-");
        final Path outside = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-race-outside-");
        final Path parent = Files.createDirectory(root.resolve("nested"));
        final Path movedParent = outside.resolve("moved");
        final Path target = parent.resolve("config.properties");
        Files.writeString(target, "sentinel=inside");

        final FileConfigurationStore store = new FileConfigurationStore(
                root,
                target.toString(),
                MimeType.PROPERTIES,
                ignored -> {
                    Files.move(parent, movedParent, StandardCopyOption.ATOMIC_MOVE);
                    Files.createSymbolicLink(parent, movedParent);
                });
        try {
            Assertions.assertThrows(ConfigurationStoreException.class,
                    () -> store.writeRawConfiguration(rawConfiguration("sentinel=escaped"), accessorContext()));
            Assertions.assertEquals("sentinel=inside", Files.readString(movedParent.resolve("config.properties")));
        } finally {
            store.shutdown();
            Files.deleteIfExists(parent);
            Files.deleteIfExists(movedParent.resolve("config.properties"));
            Files.deleteIfExists(movedParent);
            Files.deleteIfExists(outside);
            Files.deleteIfExists(root);
        }
    }

    @Test
    @Order(6)
    void writeUsesAtomicTemporarySiblingAndLeavesNoResidue() throws Exception {
        final Path root = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-atomic-root-");
        final Path target = Files.writeString(root.resolve("config.properties"), "old=value");
        final FileConfigurationStore store = new FileConfigurationStore(root, target.toString(), MimeType.PROPERTIES);
        try {
            store.writeRawConfiguration(rawConfiguration("new=value"), accessorContext());
            Assertions.assertEquals("new=value", Files.readString(target));
            try (var entries = Files.list(root)) {
                Assertions.assertEquals(1, entries.count(), "temporary sibling must be atomically consumed");
            }
        } finally {
            store.shutdown();
            Files.deleteIfExists(target);
            Files.deleteIfExists(root);
        }
    }

    @Test
    @Order(7)
    void failureBeforeAtomicRenameLeavesExistingTargetAndNoTemporarySibling() throws Exception {
        final Path root = Files.createTempDirectory(
                FileConfigurationStoreTest.class.getSimpleName() + "-l405-atomic-failure-root-");
        final Path target = Files.writeString(root.resolve("config.properties"), "old=value");
        final FileConfigurationStore store = new FileConfigurationStore(
                root,
                target.toString(),
                MimeType.PROPERTIES,
                ignored -> { },
                ignored -> {
                    throw new java.io.IOException("deterministic failure before atomic rename");
                });
        try {
            Assertions.assertThrows(ConfigurationStoreException.class,
                    () -> store.writeRawConfiguration(rawConfiguration("new=value"), accessorContext()));
            Assertions.assertEquals("old=value", Files.readString(target));
            try (var entries = Files.list(root)) {
                Assertions.assertEquals(1, entries.count(), "failed write must clean its temporary sibling");
            }
        } finally {
            store.shutdown();
            Files.deleteIfExists(target);
            Files.deleteIfExists(root);
        }
    }

    private static RawConfiguration rawConfiguration(final String value) {
        return RawConfiguration
                .builder()
                .name("l405-sentinel")
                .mimeType(MimeType.PROPERTIES)
                .timestamp(System.currentTimeMillis())
                .value(value.getBytes())
                .build();
    }

    private static ConfigurationAccessorContext accessorContext() {
        return CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
    }
}
