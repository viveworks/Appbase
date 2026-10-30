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

import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.codec.PropertiesConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.FileConfigurationStore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultConfigurationAccessor#readConfigurationsReactive()} — the reactive mirror of the
 * blocking {@link DefaultConfigurationAccessor#readConfigurations()}. It fans across every registered
 * {@link ConfigurationStore} through {@link
 * com.viveworks.appbase.core.service.configuration.accessor.store.SchedulerOffloadReactiveConfigurationStore}
 * and the N01 {@link com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary} (config is
 * offload-only — no Lucene variant), decodes the raw configurations, and returns the same flat
 * {@code Collection<Configuration>} the blocking read produces.
 *
 * <p>Per the "add no new dependency" constraint, {@code reactor-test}'s {@code StepVerifier} is
 * unavailable — this suite asserts observable behaviour via {@link reactor.core.publisher.Mono#block(
 * Duration)} instead, and by comparing the reactive result against the blocking one (parity).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultConfigurationAccessorReactiveReadTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(5);

    private static LogService logService;
    private static MetricService metricService;
    private static DefaultConfigurationAccessor configurationAccessor;
    private static Path filePathA;
    private static Path filePathB;

    @BeforeAll
    static void setUpAll() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);

        configurationAccessor = new DefaultConfigurationAccessor(
                logService, metricService, Map.of(), Map.of(), List.of(), Map.of());
        configurationAccessor.registerConfigurationCodecByMimeType(
                new PropertiesConfigurationCodec(), MimeType.PROPERTIES);

        filePathA = writeProperties("a", Map.of("a1", "z1", "b1", "y1", "c1", "x1"));
        filePathB = writeProperties("b", Map.of("d1", "w1", "e1", "v1"));
        configurationAccessor.registerConfigurationStore(
                new FileConfigurationStore(filePathA.toString(), MimeType.PROPERTIES));
        configurationAccessor.registerConfigurationStore(
                new FileConfigurationStore(filePathB.toString(), MimeType.PROPERTIES));
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        if (configurationAccessor != null) {
            configurationAccessor.shutdown();
        }
        if (filePathA != null) {
            Files.deleteIfExists(filePathA);
        }
        if (filePathB != null) {
            Files.deleteIfExists(filePathB);
        }
        if (metricService != null) {
            metricService.shutdown();
        }
        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void reactiveReadMatchesTheBlockingReadAcrossStores() throws Exception {
        final Collection<Configuration> sync = configurationAccessor.readConfigurations();
        final Collection<Configuration> reactive =
                configurationAccessor.readConfigurationsReactive().block(BLOCK_TIMEOUT);

        Assertions.assertNotNull(reactive);
        Assertions.assertEquals(sync.size(), reactive.size());
        Assertions.assertEquals(propertyKeys(sync), propertyKeys(reactive));
        Assertions.assertTrue(propertyKeys(reactive).containsAll(Set.of("a1", "b1", "c1", "d1", "e1")),
                "reactive read must fan across both stores; got " + propertyKeys(reactive));
    }

    @Test
    void storeErrorSurfacesThroughTheMono() throws Exception {
        final LogService localLogService = CoreTestFixtures.getLogService();
        final MetricService localMetricService = CoreTestFixtures.getMetricService(localLogService);
        final DefaultConfigurationAccessor accessor = new DefaultConfigurationAccessor(
                localLogService, localMetricService, Map.of(), Map.of(), List.of(), Map.of());
        accessor.registerConfigurationCodecByMimeType(new PropertiesConfigurationCodec(), MimeType.PROPERTIES);

        final ConfigurationStore boom = mock(ConfigurationStore.class);
        when(boom.getId()).thenReturn("boom-store");
        when(boom.readRawConfigurations(any())).thenThrow(new ConfigurationStoreException("boom"));
        accessor.registerConfigurationStore(boom);

        try {
            Assertions.assertThrows(Throwable.class,
                    () -> accessor.readConfigurationsReactive().block(BLOCK_TIMEOUT));
        } finally {
            accessor.shutdown();
            localMetricService.shutdown();
            localLogService.shutdown();
        }
    }

    private static Set<String> propertyKeys(final Collection<Configuration> configurations) {
        return configurations.stream()
                .map(Configuration::getConfigurationKey)
                .map(ConfigurationKey::getPropertyKey)
                .collect(Collectors.toSet());
    }

    private static Path writeProperties(final String tag, final Map<String, String> entries) throws Exception {
        final String filename = System.getProperty("java.io.tmpdir") + File.separator
                + DefaultConfigurationAccessorReactiveReadTest.class.getName() + "-" + tag + "-"
                + System.nanoTime() + "-test-dev.properties";
        final Path path = Files.createFile(Path.of(filename));
        final Properties properties = new Properties();
        entries.forEach(properties::put);
        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            properties.store(out, "test " + tag);
        }
        return path;
    }
}
