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

package com.viveworks.appbase.core.service.metric.microprofile.micrometer;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroConcurrentGaugeTest {
    private static LogService logService;
    private static DefaultMetricService metricService;
    private static MicroMetricRegistry microMetricRegistry;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        metricService.registerMicrometerRegistry(new SimpleMeterRegistry());
        microMetricRegistry = new MicroMetricRegistry(metricService.getCompositeMeterRegistry());
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testConcurrentGauge() {
        final ConcurrentGauge concurrentGauge = microMetricRegistry.concurrentGauge("concurrent-gauge-1");
        Assertions.assertNotNull(concurrentGauge);
        Assertions.assertTrue(MicroConcurrentGauge.class.isAssignableFrom(concurrentGauge.getClass()));

        final MicroConcurrentGauge microConcurrentGauge = (MicroConcurrentGauge) concurrentGauge;
        Assertions.assertEquals(0, microConcurrentGauge.getCount());

        microConcurrentGauge.inc();
        microConcurrentGauge.inc();
        Assertions.assertEquals(2, microConcurrentGauge.getCount());

        microConcurrentGauge.dec();
        Assertions.assertEquals(1, microConcurrentGauge.getCount());

        final long max = microConcurrentGauge.getMax();
        Assertions.assertTrue(max >= 0);

        final long min = microConcurrentGauge.getMin();
        Assertions.assertTrue(min >= 0);

        final ConcurrentGauge concurrentGauge1 = microMetricRegistry.concurrentGauge("concurrent-gauge-1");
        Assertions.assertEquals(concurrentGauge, concurrentGauge1);
        Assertions.assertEquals(concurrentGauge.hashCode(), concurrentGauge1.hashCode());
        Assertions.assertEquals(concurrentGauge.toString(), concurrentGauge1.toString());
    }
}
