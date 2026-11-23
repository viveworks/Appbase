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

package com.viveworks.appbase.core.service.metric.microprofile.micrometer;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroCounterTest {
    private static LogService logService;
    private static DefaultMetricService metricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
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
    void testCounter() {
        metricService.registerMicrometerRegistry(new SimpleMeterRegistry());
        final CompositeMeterRegistry compositeMeterRegistry = metricService.getCompositeMeterRegistry();
        final Counter counter1 = compositeMeterRegistry.counter("counter1");

        final MicroCounter microCounter = new MicroCounter(counter1);
        Assertions.assertNotNull(microCounter.getMicrometerCounter());
        Assertions.assertEquals(0, microCounter.getCount());

        microCounter.inc();
        microCounter.inc();
        Assertions.assertEquals(2, microCounter.getCount());

        microCounter.inc(8);
        Assertions.assertEquals(10, microCounter.getCount());

        final MicroCounter microCounter1 = new MicroCounter(counter1);
        Assertions.assertEquals(microCounter, microCounter1);
        Assertions.assertEquals(microCounter.hashCode(), microCounter1.hashCode());
        Assertions.assertEquals(microCounter.toString(), microCounter1.toString());
    }
}
