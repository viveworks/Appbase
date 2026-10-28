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
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroSimpleTimerTest {
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
    void testSimpleTimer() throws Exception {
        metricService.registerMicrometerRegistry(new SimpleMeterRegistry());
        final CompositeMeterRegistry compositeMeterRegistry = metricService.getCompositeMeterRegistry();
        final Timer timer = compositeMeterRegistry.timer("timer");

        final MicroSimpleTimer microSimpleTimer = new MicroSimpleTimer(timer);
        Assertions.assertNotNull(microSimpleTimer.getTimer());

        microSimpleTimer.update(Duration.ofMillis(1000));
        Assertions.assertEquals(1, timer.count());

        microSimpleTimer.time((Callable<Void>) () -> null);
        Assertions.assertEquals(2, timer.count());

        microSimpleTimer.time(() -> { });
        Assertions.assertEquals(3, timer.count());

        final SimpleTimer.Context context = microSimpleTimer.time();
        Assertions.assertNotNull(context);
        Assertions.assertDoesNotThrow(context::stop);
        Assertions.assertEquals(4, timer.count());

        Assertions.assertDoesNotThrow(context::close);
        Assertions.assertEquals(5, timer.count());

        Thread.sleep(300);
        final Duration elapsedTime = microSimpleTimer.getElapsedTime();
        final long elapsedTimeMillis = elapsedTime.toMillis();
        Assertions.assertTrue(elapsedTimeMillis > 300);

        final long count = microSimpleTimer.getCount();
        Assertions.assertEquals(5, count);

        final Duration maxTimeDuration = microSimpleTimer.getMaxTimeDuration();
        Assertions.assertNotNull(maxTimeDuration);
        Assertions.assertTrue(maxTimeDuration.getNano() >= 0);

        final Duration minTimeDuration = microSimpleTimer.getMinTimeDuration();
        Assertions.assertNotNull(minTimeDuration);
        Assertions.assertTrue(minTimeDuration.getNano() >= 0);
    }
}
