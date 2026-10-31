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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMediatorServiceTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }
    }

    @Test
    void testDefaultMediatorService() throws Exception {
        final DefaultValidationService validatorService = new DefaultValidationService(logService, metricService);

        // Init from classpath
        final String classpathFile1 = "classpath:///appbaseBootstrapConfiguration.yaml";
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile1);
        final DefaultMediatorService mediatorService1 = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validatorService);
        assertInitFromFile(mediatorService1, classpathFile1);


        // Init from classpath and composed configuration
        final String classpathFile2 = "classpath:///sampleComposingConfiguration.yaml";
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile2);
        final DefaultMediatorService mediatorService2 = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validatorService);
        assertInitFromFile(mediatorService2, classpathFile2);


        // Init from classpath, composed configuration and default node
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile2);
        System.setProperty(
                AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                "defaultMediatorServiceConfiguration");
        final DefaultMediatorService mediatorService3 = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validatorService);
        assertInitFromFile(mediatorService3, classpathFile2);


        // Init from classpath, composed configuration and custom node
        final String classpathFile3 = "classpath:///sampleComposingConfigurationWithCustomNodeName.yaml";
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile3);
        System.setProperty(
                AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                "mediatorServiceConfig");
        final DefaultMediatorService mediatorService4 = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validatorService);
        assertInitFromFile(mediatorService4, classpathFile3);


        // Write files
        final Path mediatorServiceConfigurationTempFile =
                Files.createTempFile("defaultMediatorServiceConfiguration", ".yaml");
        final Path sampleComposingConfigurationTempFile =
                Files.createTempFile("sampleComposingConfiguration", ".yaml");
        final Path sampleComposingConfigurationWithCustomNodeNameTempFile =
                Files.createTempFile("sampleComposingConfigurationWithCustomNodeName", ".yaml");
        writeFile(classpathFile1.substring(12), mediatorServiceConfigurationTempFile);
        writeFile(classpathFile2.substring(12), sampleComposingConfigurationTempFile);
        writeFile(classpathFile3.substring(12), sampleComposingConfigurationWithCustomNodeNameTempFile);

        try {
            // Init from filesystem
            final String file1 = mediatorServiceConfigurationTempFile.toFile().getAbsolutePath();
            System.setProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, file1);
            System.setProperty(AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME, "");
            final DefaultMediatorService mediatorService5 = new DefaultMediatorService(
                    logService,
                    metricService,
                    CoreTestFixtures.getSharedDisruptorService(),
                    CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                    validatorService);
            assertInitFromFile(mediatorService5, file1);


            // Init from filesystem and composed configuration
            final String file2 = sampleComposingConfigurationTempFile.toFile().getAbsolutePath();
            System.setProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, file2);
            System.setProperty(AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME, "");
            final DefaultMediatorService mediatorService6 = new DefaultMediatorService(
                    logService,
                    metricService,
                    CoreTestFixtures.getSharedDisruptorService(),
                    CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                    validatorService);
            assertInitFromFile(mediatorService6, file2);


            // Init from filesystem, composed configuration and custom node
            final String file3 = sampleComposingConfigurationWithCustomNodeNameTempFile.toFile().getAbsolutePath();
            System.setProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, file3);
            System.setProperty(
                    AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                    "mediatorServiceConfig");
            final DefaultMediatorService mediatorService7 = new DefaultMediatorService(
                    logService,
                    metricService,
                    CoreTestFixtures.getSharedDisruptorService(),
                    CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                    validatorService);
            assertInitFromFile(mediatorService7, file3);


            // Init from non-existing file
            String file4 = null;
            for (int i = 0; i < 25; ++i) {
                file4 = sampleComposingConfigurationWithCustomNodeNameTempFile.toFile().getAbsolutePath() +
                        Math.random();
                if (!Files.exists(Path.of(file4))) {
                    break;
                } else {
                    file4 = null;
                }
            }

            if (file4 != null) {
                System.setProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, file4);
                System.setProperty(
                        AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                        "mediatorServiceConfig");
                Assertions.assertDoesNotThrow(() -> {
                    new DefaultLogService(
                            CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                            DefaultLogServiceConfiguration.builder().build());
                });
            } else {
                System.err.println("Could not create a random file name for non-existing file test");
            }
        } finally {
            mediatorServiceConfigurationTempFile.toFile().deleteOnExit();
            sampleComposingConfigurationTempFile.toFile().deleteOnExit();
            sampleComposingConfigurationWithCustomNodeNameTempFile.toFile().deleteOnExit();
        }


        // Init from default configuration
        System.setProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, "");
        System.setProperty(AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME, "");
        final DefaultMediatorService mediatorService8 = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validatorService);
        Assertions.assertNotNull(mediatorService8.getMediator());
        Assertions.assertFalse(mediatorService8.isInitWithConfigurationFile());
        Assertions.assertTrue(mediatorService8.isInitWithDefaultConfiguration());
        Assertions.assertNull(mediatorService8.getConfigurationFilePath());
        Assertions.assertNotNull(mediatorService8.getConfiguration());


        // Init with explicit configuration
        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration
                        .builder()
                        .messageObjectPoolCapacity(64)
                        .droppedMessageQueueCapacity(32)
                        .appbaseRuntimeExceptionQueueCapacity(32)
                        .build();
        final DefaultMediatorService mediatorService9 = new DefaultMediatorService(
                logService,
                metricService,
                validatorService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                defaultMediatorServiceConfiguration);
        Assertions.assertNotNull(mediatorService9.getMediator());
        Assertions.assertFalse(mediatorService9.isInitWithConfigurationFile());
        Assertions.assertFalse(mediatorService9.isInitWithDefaultConfiguration());
        Assertions.assertNull(mediatorService9.getConfigurationFilePath());
        Assertions.assertNotNull(mediatorService9.getConfiguration());
        Assertions.assertEquals(defaultMediatorServiceConfiguration, mediatorService9.getConfiguration());

        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration1 =
                DefaultMediatorServiceConfiguration
                        .builder()
                        .messageObjectPoolCapacity(32)
                        .droppedMessageQueueCapacity(64)
                        .appbaseRuntimeExceptionQueueCapacity(64)
                        .build();
        mediatorService9.reload(defaultMediatorServiceConfiguration1);
        Assertions.assertEquals(defaultMediatorServiceConfiguration1, mediatorService9.getConfiguration());

        mediatorService9.shutdown();
        final ServiceStatus status = mediatorService9.getStatus();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, status);
    }

    private void assertInitFromFile(final DefaultMediatorService mediatorService, final String filename) {
        Assertions.assertNotNull(mediatorService.getMediator());
        Assertions.assertTrue(mediatorService.isInitWithConfigurationFile());
        Assertions.assertFalse(mediatorService.isInitWithDefaultConfiguration());
        Assertions.assertEquals(filename, mediatorService.getConfigurationFilePath());
        Assertions.assertNotNull(mediatorService.getConfiguration());
    }

    private void writeFile(final String classpathFile, final Path destination) throws Exception {
        final InputStream inputStream = getClass().getResourceAsStream(classpathFile);
        Files.write(destination, inputStream.readAllBytes());
        inputStream.close();
    }
}
