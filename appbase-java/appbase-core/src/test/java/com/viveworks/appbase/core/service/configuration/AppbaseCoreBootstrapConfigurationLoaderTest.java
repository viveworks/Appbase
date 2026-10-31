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

package com.viveworks.appbase.core.service.configuration;

import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseCoreBootstrapConfigurationLoaderTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static Path filePath;
    private static DefaultConfigurationService configurationService;
    private static AtomicInteger globalAddedCounter = new AtomicInteger(0);
    private static AtomicInteger configurationAddedCounter = new AtomicInteger(0);
    private static AtomicInteger globalChangedCounter = new AtomicInteger(0);
    private static AtomicInteger configurationChangedCounter = new AtomicInteger(0);
    private static AtomicInteger globalRemovedCounter = new AtomicInteger(0);
    private static AtomicInteger configurationRemovedCounter = new AtomicInteger(0);

    @BeforeAll
    static void setUp() throws Exception {
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

        final String filename = System.getProperty("java.io.tmpdir") +
                File.separator +
                AppbaseCoreBootstrapConfigurationLoaderTest.class.getName() +
                "-file-" +
                System.nanoTime() +
                "-test-dev.yaml";
        filePath = Files.createFile(Path.of(filename));

        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                        logService, metricService, mediatorService, validationService);
        configurationService.registerGlobalConfigurationListener(new SampleConfigurationListener1());

        final SampleConfigurationListener2 sampleConfigurationListener2 = new SampleConfigurationListener2();
        configurationService.registerConfigurationListenerByKey(
                sampleConfigurationListener2,
                ConfigurationKey.fromTypeKey(DefaultExceptionServiceConfiguration.class));
        configurationService.registerConfigurationListenerByKey(
                sampleConfigurationListener2,
                ConfigurationKey.fromTypeKey(DefaultMediatorServiceConfiguration.class));
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (filePath != null) {
            Files.deleteIfExists(filePath);
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

    @Test
    void testLoadAndReload() throws Exception {
        final InputStream resourceAsStream1 = getClass().getResourceAsStream("/appbase-core.yaml");
        final byte[] bytes1 = resourceAsStream1.readAllBytes();
        Files.write(Path.of(filePath.toUri()), bytes1);

        final Map<String, Class<? extends AppbaseConfiguration>> configurationTypeByYamlNodeNameToDecodeMap =
                Map.of(
                        "defaultMetricServiceConfiguration", DefaultMetricServiceConfiguration.class,
                        "defaultMediatorServiceConfiguration", DefaultMediatorServiceConfiguration.class,
                        "defaultConfigurationServiceConfiguration", DefaultConfigurationServiceConfiguration.class,
                        "defaultServiceRegistryConfiguration", DefaultServiceRegistryConfiguration.class,
                        "defaultExceptionServiceConfiguration", DefaultExceptionServiceConfiguration.class,
                        "defaultTracerServiceConfiguration", DefaultTracerServiceConfiguration.class,
                        "defaultReactiveServiceConfiguration", DefaultReactorServiceConfiguration.class,
                        "defaultJmxServiceConfiguration", DefaultJmxServiceConfiguration.class,
                        DefaultAppbaseCoreBootstrapConfiguration.DEFAULT_YAML_NODE_NAME,
                        DefaultAppbaseCoreBootstrapConfiguration.class);
        final CoreBootstrapConfigurationLoader coreBootstrapConfigurationLoader =
                new CoreBootstrapConfigurationLoader(
                        filePath.toString(),
                        configurationTypeByYamlNodeNameToDecodeMap,
                        configurationService);
        coreBootstrapConfigurationLoader.load();
        Assertions.assertEquals(9, globalAddedCounter.get());
        Assertions.assertEquals(2, configurationAddedCounter.get());

        final Map<String, AppbaseConfiguration> configurationByTypeNameMap1 =
                coreBootstrapConfigurationLoader.getConfigurationByTypeNameMap();
        Assertions.assertNotNull(configurationByTypeNameMap1);
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultMetricServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultMediatorServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultConfigurationServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultServiceRegistryConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultExceptionServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultTracerServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultReactorServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultJmxServiceConfiguration.class.getName()));
        Assertions.assertTrue(
                configurationByTypeNameMap1.containsKey(DefaultAppbaseCoreBootstrapConfiguration.class.getName()));
        final AppbaseConfiguration exceptionServiceConfiguration1 =
                configurationByTypeNameMap1.get(DefaultExceptionServiceConfiguration.class.getName());

        final InputStream resourceAsStream2 =
                getClass().getResourceAsStream("/appbaseBootstrapConfigurationWithJpa4.yaml");
        final byte[] bytes2 = resourceAsStream2.readAllBytes();
        Files.write(Path.of(filePath.toUri()), bytes2);
        Thread.sleep(500);
        coreBootstrapConfigurationLoader.load();
        Assertions.assertTrue(globalChangedCounter.get() >= 1);
        Assertions.assertTrue(configurationChangedCounter.get() >= 1);

        final Map<String, AppbaseConfiguration> configurationByTypeNameMap2 =
                coreBootstrapConfigurationLoader.getConfigurationByTypeNameMap();
        Assertions.assertEquals(8, configurationByTypeNameMap2.size());
        final AppbaseConfiguration exceptionServiceConfiguration2 =
                configurationByTypeNameMap2.get(DefaultExceptionServiceConfiguration.class.getName());
        Assertions.assertNotEquals(exceptionServiceConfiguration1, exceptionServiceConfiguration2);

        final InputStream resourceAsStream3 =
                getClass().getResourceAsStream("/appbase-BootstrapConfigurationLoaderTest.yaml");
        final byte[] bytes3 = resourceAsStream3.readAllBytes();
        Files.write(Path.of(filePath.toUri()), bytes3);
        coreBootstrapConfigurationLoader.load();
        Assertions.assertEquals(2, globalRemovedCounter.get());
        Assertions.assertEquals(1, configurationRemovedCounter.get());

        final Map<String, AppbaseConfiguration> configurationByTypeNameMap3 =
                coreBootstrapConfigurationLoader.getConfigurationByTypeNameMap();
        Assertions.assertEquals(7, configurationByTypeNameMap3.size());
        Assertions.assertFalse(
                configurationByTypeNameMap3.containsKey(DefaultExceptionServiceConfiguration.class.getName()));
    }


    private static class SampleConfigurationListener1 implements ConfigurationListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(SampleConfigurationListener1.class);

        @Override
        public void onConfigurationAdded(final ConfigurationAddedEvent configurationAddedEvent) {
            LOGGER.info("Configuration added: {}", configurationAddedEvent);
            globalAddedCounter.incrementAndGet();
        }

        @Override
        public void onConfigurationChanged(final ConfigurationChangedEvent configurationChangedEvent) {
            LOGGER.info("Configuration changed: {}", configurationChangedEvent);
            globalChangedCounter.incrementAndGet();

        }

        @Override
        public void onConfigurationRemoved(final ConfigurationRemovedEvent configurationRemovedEvent) {
            LOGGER.info("Configuration removed: {}", configurationRemovedEvent);
            globalRemovedCounter.incrementAndGet();
        }
    }

    private static class SampleConfigurationListener2 implements ConfigurationListener {
        private static final Logger LOGGER = LoggerFactory.getLogger(SampleConfigurationListener2.class);

        @Override
        public void onConfigurationAdded(final ConfigurationAddedEvent configurationAddedEvent) {
            LOGGER.info("Configuration added: {}", configurationAddedEvent);
            configurationAddedCounter.incrementAndGet();
        }

        @Override
        public void onConfigurationChanged(final ConfigurationChangedEvent configurationChangedEvent) {
            LOGGER.info("Configuration changed: {}", configurationChangedEvent);
            configurationChangedCounter.incrementAndGet();
        }

        @Override
        public void onConfigurationRemoved(final ConfigurationRemovedEvent configurationRemovedEvent) {
            LOGGER.info("Configuration removed: {}", configurationRemovedEvent);
            configurationRemovedCounter.incrementAndGet();
        }
    }
}
