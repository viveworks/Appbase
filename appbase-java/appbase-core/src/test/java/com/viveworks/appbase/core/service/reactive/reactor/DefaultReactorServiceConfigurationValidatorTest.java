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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultReactorServiceConfigurationValidatorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testValidator() {
        final DefaultReactorServiceConfigurationValidator defaultReactorServiceConfigurationValidator1 =
                new DefaultReactorServiceConfigurationValidator(validationService);
        final DefaultReactorServiceConfigurationValidator defaultReactorServiceConfigurationValidator2 =
                new DefaultReactorServiceConfigurationValidator();
        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration1 =
                DefaultReactorServiceConfiguration.builder().build();

        Assertions.assertTrue(
                defaultReactorServiceConfigurationValidator1.isValid(defaultReactiveServiceConfiguration1, null));
        Assertions.assertTrue(
                defaultReactorServiceConfigurationValidator2.isValid(defaultReactiveServiceConfiguration1, null));

        Assertions.assertFalse(
                defaultReactorServiceConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                defaultReactorServiceConfigurationValidator2.isValid(null, null));

        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration2 =
                DefaultReactorServiceConfiguration
                        .builder()
                        .enabled(true)
                        .schedulerConfigurations(null)
                        .build();
        Assertions.assertTrue(
                defaultReactorServiceConfigurationValidator1.isValid(defaultReactiveServiceConfiguration2, null));

        final List<SchedulerConfiguration> schedulerConfigurations = new ArrayList<>();
        schedulerConfigurations.add(null);
        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            DefaultReactorServiceConfiguration
                    .builder()
                    .enabled(true)
                    .schedulerConfigurations(schedulerConfigurations)
                    .build();
        });
        schedulerConfigurations.clear();


        Assertions.assertThrows(AppbaseRuntimeException.class, () -> {
            DefaultReactorServiceConfiguration
                    .builder()
                    .enabled(true)
                    .schedulerConfigurations(List.of(new TestSchedulerConfiguration()))
                    .build();
        });

        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(new TestSchedulerConfiguration())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(BoundedElasticSchedulerConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(ElasticSchedulerConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(ExecutorServiceSchedulerConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(ImmediateSchedulerConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(MediatorExecutorServiceSchedulerConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(ParallelSchedulerConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultReactorServiceConfigurationValidator.validate(
                        true,
                        List.of(SingleSchedulerConfiguration.builder().build())));
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
}
