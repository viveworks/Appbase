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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Scheduler;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MediatorExecutorServiceSchedulerFactoryTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
    }

    @AfterAll
    static void tearDown() {
        if (mediatorService != null) {
            mediatorService.shutdown();
        }

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
    void newScheduler() {
        final MediatorExecutorServiceSchedulerFactory mediatorExecutorServiceSchedulerFactory =
                new MediatorExecutorServiceSchedulerFactory(mediatorService);
        final MediatorExecutorServiceSchedulerConfiguration mediatorExecutorServiceSchedulerConfiguration =
                MediatorExecutorServiceSchedulerConfiguration.builder().build();

        final Scheduler scheduler =
                mediatorExecutorServiceSchedulerFactory.newScheduler(mediatorExecutorServiceSchedulerConfiguration);
        Assertions.assertNotNull(scheduler);
        Assertions.assertFalse(scheduler.isDisposed());
        scheduler.dispose();
        Assertions.assertTrue(scheduler.isDisposed());
    }
}
