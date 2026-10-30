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

package com.viveworks.appbase.core.service.reactive.reactor;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationService;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.SchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.BoundedElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ImmediateSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ParallelSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SingleSchedulerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultReactorServiceTest {
    private static LogService logService;
    private static DefaultMetricService metricService;
    private static MediatorService mediatorService;
    private static ValidationService validationService;
    private static DefaultConfigurationService configurationService;
    private static DefaultReactorService defaultReactiveService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (defaultReactiveService != null) {
            defaultReactiveService.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
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
    void testBoundedElasticScheduler() throws Exception {
        runTests(
                List.of(BoundedElasticSchedulerConfiguration.builder().build()),
                List.of(BoundedElasticSchedulerConfiguration.builder().threadCap(2).build()),
                "Appbase-Reactor-BoundedElasticScheduler",
                false);
    }

    @Test
    @Order(2)
    void testElasticScheduler() throws Exception {
        runTests(
                List.of(ElasticSchedulerConfiguration.builder().build()),
                List.of(ElasticSchedulerConfiguration.builder().ttlSeconds(10).build()),
                "Appbase-Reactor-ElasticScheduler",
                false);
    }

    @Test
    @Order(3)
    void testExecutorServiceScheduler() throws Exception {
        runTests(
                List.of(ExecutorServiceSchedulerConfiguration.builder().build()),
                List.of(ExecutorServiceSchedulerConfiguration.builder().executorName("EN").build()),
                "Appbase-Reactor-ExecutorServiceScheduler",
                false);
    }

    @Test
    @Order(4)
    void testImmediateServiceScheduler() throws Exception {
        runTests(
                List.of(ImmediateSchedulerConfiguration.builder().build()),
                List.of(ImmediateSchedulerConfiguration.builder().build()),
                "Appbase-Reactor-ImmediateScheduler",
                false);
    }

    @Test
    @Order(5)
    void testMediatorExecutorServiceScheduler() throws Exception {
        runTests(
                List.of(MediatorExecutorServiceSchedulerConfiguration.builder().build()),
                List.of(MediatorExecutorServiceSchedulerConfiguration.builder().executorName("EN").build()),
                "Appbase-Reactor-MediatorExecutorServiceScheduler",
                true);

        final MediatorExecutorServiceSchedulerConfiguration mediatorExecutorServiceSchedulerConfiguration =
                MediatorExecutorServiceSchedulerConfiguration.builder().defaultScheduler(false).build();
        runTests(
                List.of(mediatorExecutorServiceSchedulerConfiguration),
                List.of(mediatorExecutorServiceSchedulerConfiguration),
                "Appbase-Reactor-MediatorExecutorServiceScheduler",
                false);
    }

    @Test
    @Order(6)
    void testParallelScheduler() throws Exception {
        runTests(
                List.of(ParallelSchedulerConfiguration.builder().build()),
                List.of(ParallelSchedulerConfiguration.builder().parallelism(1).build()),
                "Appbase-Reactor-ParallelScheduler",
                false);
    }

    @Test
    @Order(7)
    void testSingleScheduler() throws Exception {
        final SingleSchedulerConfiguration singleSchedulerConfiguration = SingleSchedulerConfiguration
                .builder()
                .threadFactoryConfiguration(
                        ThreadFactoryConfiguration
                                .builder()
                                .nameFormat("TN-%d")
                                .build())
                .build();
        runTests(
                List.of(SingleSchedulerConfiguration.builder().build()),
                List.of(singleSchedulerConfiguration),
                "Appbase-Reactor-SingleScheduler",
                false);
    }

    @Test
    @Order(8)
    void testCollections() {
        defaultReactiveService.clearSchedulerFactoryByConfigurationTypeMap();

        defaultReactiveService.registerSchedulerFactoryByConfigurationType(
                TestSchedulerConfiguration.class, new TestSchedulerFactory());
        final Map<String, SchedulerFactory<?>> schedulerFactoryByConfigurationTypeMap =
                defaultReactiveService.getSchedulerFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(schedulerFactoryByConfigurationTypeMap);
        Assertions.assertEquals(1, schedulerFactoryByConfigurationTypeMap.size());
        Assertions.assertTrue(schedulerFactoryByConfigurationTypeMap
                .containsKey(TestSchedulerConfiguration.class.getName()));

        defaultReactiveService.unregisterSchedulerFactoryByConfigurationType(TestSchedulerConfiguration.class);
        final Map<String, SchedulerFactory<?>> schedulerFactoryByConfigurationTypeMap1 =
                defaultReactiveService.getSchedulerFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(schedulerFactoryByConfigurationTypeMap1);
        Assertions.assertTrue(schedulerFactoryByConfigurationTypeMap1.isEmpty());

        defaultReactiveService.registerSchedulerFactoryByConfigurationType(
                TestSchedulerConfiguration.class, new TestSchedulerFactory());
        final Map<String, SchedulerFactory<?>> schedulerFactoryByConfigurationTypeMap2 =
                defaultReactiveService.getSchedulerFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(schedulerFactoryByConfigurationTypeMap2);
        Assertions.assertEquals(1, schedulerFactoryByConfigurationTypeMap2.size());
        Assertions.assertTrue(schedulerFactoryByConfigurationTypeMap2
                .containsKey(TestSchedulerConfiguration.class.getName()));

        defaultReactiveService.clearSchedulerFactoryByConfigurationTypeMap();
        final Map<String, SchedulerFactory<?>> schedulerFactoryByConfigurationTypeMap3 =
                defaultReactiveService.getSchedulerFactoryByConfigurationTypeMap();
        Assertions.assertNotNull(schedulerFactoryByConfigurationTypeMap3);
        Assertions.assertTrue(schedulerFactoryByConfigurationTypeMap3.isEmpty());
    }

    private void runTests(
            final List<SchedulerConfiguration> originalSchedulerConfigurations,
            final List<SchedulerConfiguration> newSchedulerConfigurations,
            final String schedulerId,
            final boolean isDefaultScheduler) throws Exception
    {
        final DefaultReactorServiceConfiguration originalDefaultReactiveServiceConfiguration =
                DefaultReactorServiceConfiguration
                        .builder()
                        .enabled(true)
                        .schedulerConfigurations(originalSchedulerConfigurations)
                        .build();
        configurationService.putTypedConfiguration(
                DefaultReactorServiceConfiguration.class, originalDefaultReactiveServiceConfiguration);
        defaultReactiveService = new DefaultReactorService(
                logService, metricService, mediatorService, validationService, configurationService);

        final DefaultReactorServiceConfiguration newDefaultReactiveServiceConfiguration =
                DefaultReactorServiceConfiguration
                        .builder()
                        .enabled(true)
                        .schedulerConfigurations(newSchedulerConfigurations)
                        .build();

        checkScheduler(
                defaultReactiveService,
                schedulerId,
                isDefaultScheduler);

        defaultReactiveService.reload(originalDefaultReactiveServiceConfiguration);
        checkScheduler(
                defaultReactiveService,
                schedulerId,
                isDefaultScheduler);

        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DefaultReactorServiceConfiguration.class);
        final ConfigurationValue<DefaultReactorServiceConfiguration> originalConfigurationValue =
                ConfigurationValue.fromValue(originalDefaultReactiveServiceConfiguration);
        final ConfigurationValue<DefaultReactorServiceConfiguration> newConfigurationValue =
                ConfigurationValue.fromValue(newDefaultReactiveServiceConfiguration);
        final long timestamp = System.currentTimeMillis();
        checkEvents(
                defaultReactiveService,
                configurationKey,
                originalConfigurationValue,
                newConfigurationValue,
                timestamp);

        defaultReactiveService.shutdown();
        final ServiceStatus status = defaultReactiveService.getStatus();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, status);
    }

    private void checkScheduler(
            final DefaultReactorService defaultReactiveService,
            final String schedulerId,
            final boolean isDefaultScheduler)
    {
        final Scheduler schedulerById = defaultReactiveService.getSchedulerById(schedulerId);
        Assertions.assertNotNull(schedulerById);

        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration =
                defaultReactiveService.getConfiguration();
        Assertions.assertNotNull(defaultReactiveServiceConfiguration);

        if (isDefaultScheduler) {
            final Scheduler defaultScheduler = defaultReactiveService.getDefaultScheduler();
            Assertions.assertNotNull(defaultScheduler);
        }
    }

    private void checkEvents(final DefaultReactorService defaultReactiveService,
                             final ConfigurationKey configurationKey,
                             final ConfigurationValue<DefaultReactorServiceConfiguration> originalConfigurationValue,
                             final ConfigurationValue<DefaultReactorServiceConfiguration> newConfigurationValue,
                             final long timestamp) throws Exception
    {
        final ConfigurationAddedEvent configurationAddedEvent =
                new ConfigurationAddedEvent(configurationKey, newConfigurationValue, timestamp);
        defaultReactiveService.onConfigurationAdded(configurationAddedEvent);
        Assertions.assertEquals(ServiceStatus.STARTED, defaultReactiveService.getStatus());

        final ConfigurationChangedEvent configurationChangedEvent =
                new ConfigurationChangedEvent(
                        configurationKey, originalConfigurationValue, newConfigurationValue, timestamp);
        defaultReactiveService.onConfigurationChanged(configurationChangedEvent);
        Assertions.assertEquals(ServiceStatus.STARTED, defaultReactiveService.getStatus());

        final ConfigurationRemovedEvent configurationRemovedEvent =
                new ConfigurationRemovedEvent(configurationKey, timestamp);
        defaultReactiveService.onConfigurationRemoved(configurationRemovedEvent);
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, defaultReactiveService.getStatus());
    }

    private static class TestScheduler implements Scheduler {
        @Override
        public Disposable schedule(final Runnable task) {
            return null;
        }

        @Override
        public Worker createWorker() {
            return null;
        }
    }

    private static class TestSchedulerConfiguration implements SchedulerConfiguration {
        @Override
        public String getId() {
            return getClass().getName();
        }

        @Override
        public boolean isDefaultScheduler() {
            return false;
        }
    }

    private static class TestSchedulerFactory implements SchedulerFactory<TestSchedulerConfiguration> {
        @Override
        public Scheduler newScheduler(final TestSchedulerConfiguration schedulerConfiguration) {
            return new TestScheduler();
        }
    }
}
