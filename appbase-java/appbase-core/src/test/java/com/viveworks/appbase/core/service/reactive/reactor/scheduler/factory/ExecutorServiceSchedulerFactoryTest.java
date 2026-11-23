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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
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
class ExecutorServiceSchedulerFactoryTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testNewScheduler() {
        final ExecutorServiceSchedulerFactory executorServiceSchedulerFactory =
                new ExecutorServiceSchedulerFactory(logService, metricService);
        final ExecutorServiceSchedulerConfiguration executorServiceSchedulerConfiguration =
                ExecutorServiceSchedulerConfiguration.builder().build();

        final Scheduler scheduler = executorServiceSchedulerFactory.newScheduler(executorServiceSchedulerConfiguration);
        Assertions.assertNotNull(scheduler);
        Assertions.assertFalse(scheduler.isDisposed());
        scheduler.dispose();
        Assertions.assertTrue(scheduler.isDisposed());
    }
}
