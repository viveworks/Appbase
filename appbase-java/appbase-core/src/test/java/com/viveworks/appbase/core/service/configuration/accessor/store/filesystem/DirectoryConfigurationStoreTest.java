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
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DirectoryConfigurationStoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static DirectoryConfigurationStore directoryConfigurationStore;

    private static Properties properties1;
    private static Properties properties2;
    private static Properties properties3;

    private static Path rootDirectoryPath;
    private static Path recursiveDir;
    private static Path filePath1;
    private static Path filePath2;
    private static Path filePath3;

    @BeforeAll
    static void setUp() throws Exception {
        rootDirectoryPath = Files.createTempDirectory(DirectoryConfigurationStoreTest.class.getName());

        properties1 = new Properties();
        properties1.put("a", "z");
        properties1.put("b", "y");
        properties1.put("c", "x");
        filePath1 = Files.createTempFile(rootDirectoryPath, "filePath1", ".properties");
        properties1.store(new FileOutputStream(filePath1.toFile()), "comment1");

        properties2 = new Properties();
        properties2.put("aa", "zz");
        properties2.put("bb", "yy");
        properties2.put("cc", "xx");

        recursiveDir = Path.of(rootDirectoryPath.toFile().getAbsolutePath(), "recursiveDir");
        Files.createDirectory(recursiveDir);
        filePath2 = Files.createTempFile(recursiveDir, "filePath2", ".properties");
        properties2.store(new FileOutputStream(filePath2.toFile()), "comment2");

        properties3 = new Properties();
        properties3.put("aaa", "zzz");
        properties3.put("bbb", "yyy");
        properties3.put("ccc", "xxx");
        filePath3 = Files.createTempFile(rootDirectoryPath, "filePath3", ".properties");
        properties3.store(new FileOutputStream(filePath3.toFile()), "comment3");

        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        directoryConfigurationStore = new DirectoryConfigurationStore(
                logService,
                metricService,
                rootDirectoryPath.toFile().getAbsolutePath(),
                Set.of(MimeType.PROPERTIES),
                new DefaultFileMimeTypeResolver(Map.of("properties", MimeType.PROPERTIES)),
                Set.of(filePath3.toFile().getName()));
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (filePath1 != null) {
            Files.deleteIfExists(filePath1);
        }

        if (filePath2 != null) {
            Files.deleteIfExists(filePath2);
        }

        if (filePath3 != null) {
            Files.deleteIfExists(filePath3);
        }

        if (recursiveDir != null) {
            Files.deleteIfExists(recursiveDir);
        }

        if (rootDirectoryPath != null) {
            Files.deleteIfExists(rootDirectoryPath);
        }

        directoryConfigurationStore.shutdown();

        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }
    }

    @Test
    @Order(1)
    void testReadRawConfigurations() throws Exception {
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        final Collection<RawConfiguration> rawConfigurations =
                directoryConfigurationStore.readRawConfigurations(configurationAccessorContext);

        Assertions.assertNotNull(rawConfigurations);
        Assertions.assertEquals(2, rawConfigurations.size());

        final Properties properties = new Properties();
        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            Assertions.assertEquals(MimeType.PROPERTIES, rawConfiguration.getMimeType());

            final byte[] bytes = rawConfiguration.getValue();
            final Properties resultProperties = new Properties();
            resultProperties.load(new ByteArrayInputStream(bytes));
            properties.putAll(resultProperties);
        }

        Assertions.assertNull(properties.getProperty("aaa"));
        Assertions.assertNull(properties.getProperty("bbb"));
        Assertions.assertNull(properties.getProperty("ccc"));

        Assertions.assertEquals("z", properties.getProperty("a"));
        Assertions.assertEquals("y", properties.getProperty("b"));
        Assertions.assertEquals("x", properties.getProperty("c"));

        Assertions.assertEquals("zz", properties.getProperty("aa"));
        Assertions.assertEquals("yy", properties.getProperty("bb"));
        Assertions.assertEquals("xx", properties.getProperty("cc"));

        Assertions.assertNotNull(directoryConfigurationStore.getFileConfigurationStoreMapSizeGauge());
    }

    @Test
    @Order(2)
    void testWriteConfigurations() throws Exception {
        properties1.put("ddddd", "wwwww");
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        properties1.store(outputStream, "comment");

        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(filePath1.toFile().getName())
                .mimeType(MimeType.PROPERTIES)
                .timestamp(System.currentTimeMillis())
                .value(outputStream.toByteArray())
                .build();
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
        directoryConfigurationStore.writeRawConfiguration(rawConfiguration, configurationAccessorContext);

        final Properties resultProperties = new Properties();
        resultProperties.load(new FileInputStream(filePath1.toFile()));

        Assertions.assertEquals(4, resultProperties.size());
        Assertions.assertEquals("wwwww", resultProperties.getProperty("ddddd"));
        Assertions.assertEquals("z", resultProperties.getProperty("a"));
        Assertions.assertEquals("y", resultProperties.getProperty("b"));
        Assertions.assertEquals("x", resultProperties.getProperty("c"));

        Assertions.assertTrue(directoryConfigurationStore.supportsWrite());
    }

    /**
     * SEC-4 (CWE-59/CWE-200): {@code createMimeTypeByFilenamesMap}'s {@code Files.walk(rootPath,
     * FileVisitOption.FOLLOW_LINKS)} followed symlinks with no bound and no check that a visited
     * entry wasn't itself a link -- a symlink planted inside the confined root, pointing at a file
     * OUTSIDE it, was read as though it were a regular file within the root. Plants a symlink
     * inside {@code rootDirectoryPath} pointing at a properties file in a completely separate
     * temp directory, with a sentinel key/value no legitimate root file has, and asserts the
     * sentinel never appears in what the store reads back.
     *
     * @throws Exception on I/O failure
     */
    @Test
    @Order(3)
    void readRawConfigurations_doesNotFollowSymlinkToFileOutsideRoot() throws Exception {
        final Path outsideDir = Files.createTempDirectory(
                DirectoryConfigurationStoreTest.class.getName() + "-outside");
        final Path outsideSecret = Files.createTempFile(outsideDir, "secret", ".properties");
        final Properties secretProperties = new Properties();
        secretProperties.put("sentinelSecretKey", "sentinelSecretValue");
        secretProperties.store(new FileOutputStream(outsideSecret.toFile()), "secret, outside the root");

        final Path linkInsideRoot = rootDirectoryPath.resolve("leak.properties");
        Files.createSymbolicLink(linkInsideRoot, outsideSecret);

        try {
            final ConfigurationAccessorContext configurationAccessorContext =
                    CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);
            final Collection<RawConfiguration> rawConfigurations =
                    directoryConfigurationStore.readRawConfigurations(configurationAccessorContext);

            for (final RawConfiguration rawConfiguration : rawConfigurations) {
                final Properties resultProperties = new Properties();
                resultProperties.load(new ByteArrayInputStream(rawConfiguration.getValue()));
                Assertions.assertNull(resultProperties.getProperty("sentinelSecretKey"),
                        "a symlink planted inside the confined root must not expose content from "
                                + "a file outside it");
            }
        } finally {
            Files.deleteIfExists(linkInsideRoot);
            Files.deleteIfExists(outsideSecret);
            Files.deleteIfExists(outsideDir);
        }
    }

    /**
     * L2-08 (CWE-22): {@code writeRawConfiguration} treated SUBSTRING CONTAINMENT
     * ({@code filename.contains(rootDirectoryName)}) as confinement. A name using {@code "../"}
     * escapes the root entirely (the root text never appears in the result), and a sibling
     * directory whose name merely starts with the root's own name (e.g. {@code
     * "<root>-evil/out.properties"}) textually CONTAINS the root string although it is not a
     * child of it, so the naive check treats it as already-confined and skips the root-prefixing
     * branch. Pins that neither shape can make the write land outside the confined root.
     *
     * @throws Exception on I/O failure
     */
    @Test
    @Order(4)
    void writeRejectsTraversalAndSiblingPrefixOutsideRoot() throws Exception {
        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);

        final Path escapedViaTraversal = rootDirectoryPath.getParent().resolve("escaped-traversal.properties");
        Files.deleteIfExists(escapedViaTraversal);
        try {
            final RawConfiguration traversalConfiguration = RawConfiguration
                    .builder()
                    .name("../" + escapedViaTraversal.getFileName())
                    .mimeType(MimeType.PROPERTIES)
                    .timestamp(System.currentTimeMillis())
                    .value("x=y".getBytes())
                    .build();

            Assertions.assertThrows(RuntimeException.class,
                    () -> directoryConfigurationStore.writeRawConfiguration(
                            traversalConfiguration, configurationAccessorContext),
                    "a '../'-prefixed name must not escape the confined root");
            Assertions.assertFalse(Files.exists(escapedViaTraversal),
                    "the traversal write must not have landed outside the confined root");
        } finally {
            Files.deleteIfExists(escapedViaTraversal);
        }

        final Path siblingEvilDir = Path.of(rootDirectoryPath.toAbsolutePath() + "-evil");
        Files.createDirectories(siblingEvilDir);
        final Path siblingEvilFile = siblingEvilDir.resolve("out.properties");
        try {
            final RawConfiguration siblingConfiguration = RawConfiguration
                    .builder()
                    .name(siblingEvilFile.toString())
                    .mimeType(MimeType.PROPERTIES)
                    .timestamp(System.currentTimeMillis())
                    .value("x=y".getBytes())
                    .build();

            Assertions.assertThrows(RuntimeException.class,
                    () -> directoryConfigurationStore.writeRawConfiguration(
                            siblingConfiguration, configurationAccessorContext),
                    "a sibling path whose text merely starts with the root's own name must not be " +
                            "treated as already confined");
            Assertions.assertFalse(Files.exists(siblingEvilFile),
                    "the sibling-prefix write must not have landed outside the confined root");
        } finally {
            Files.deleteIfExists(siblingEvilFile);
            Files.deleteIfExists(siblingEvilDir);
        }
    }
}
