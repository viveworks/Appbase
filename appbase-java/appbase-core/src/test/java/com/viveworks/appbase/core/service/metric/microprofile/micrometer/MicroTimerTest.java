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

import com.codahale.metrics.Meter;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
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
class MicroTimerTest {
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
    void testTimer() throws Exception {
        metricService.registerMicrometerRegistry(new SimpleMeterRegistry());
        final CompositeMeterRegistry compositeMeterRegistry = metricService.getCompositeMeterRegistry();
        final Timer timer1 = compositeMeterRegistry.timer("timer1");

        final com.codahale.metrics.Meter codehaleMeter = new com.codahale.metrics.Meter();
        final MicroTimer microTimer = new MicroTimer(timer1, codehaleMeter);
        final Timer timer = microTimer.getTimer();
        final Meter meter = microTimer.getMeter();
        Assertions.assertNotNull(timer);
        Assertions.assertNotNull(meter);

        microTimer.update(Duration.ofMillis(1000));
        Assertions.assertEquals(1, timer.count());
        Assertions.assertEquals(1, meter.getCount());

        microTimer.time((Callable<Void>) () -> null);
        Assertions.assertEquals(2, timer.count());
        Assertions.assertEquals(2, meter.getCount());

        microTimer.time(() -> { });
        Assertions.assertEquals(3, timer.count());
        Assertions.assertEquals(3, meter.getCount());

        final org.eclipse.microprofile.metrics.Timer.Context context = microTimer.time();
        Assertions.assertNotNull(context);
        Assertions.assertDoesNotThrow(context::stop);
        Assertions.assertEquals(4, timer.count());
        Assertions.assertEquals(4, meter.getCount());

        Assertions.assertDoesNotThrow(context::close);
        Assertions.assertEquals(5, timer.count());
        Assertions.assertEquals(5, meter.getCount());

        Thread.sleep(300);
        final Duration elapsedTime = microTimer.getElapsedTime();
        final long elapsedTimeMillis = elapsedTime.toMillis();
        Assertions.assertTrue(elapsedTimeMillis > 300);

        final long count = microTimer.getCount();
        Assertions.assertEquals(5, count);

        final double fifteenMinuteRate = microTimer.getFifteenMinuteRate();
        Assertions.assertTrue(fifteenMinuteRate >= 0);

        final double fiveMinuteRate = microTimer.getFiveMinuteRate();
        Assertions.assertTrue(fiveMinuteRate >= 0);

        final double meanRate = microTimer.getMeanRate();
        Assertions.assertTrue(meanRate >= 0);

        final double oneMinuteRate = microTimer.getOneMinuteRate();
        Assertions.assertTrue(oneMinuteRate >= 0);

        final Snapshot snapshot = microTimer.getSnapshot();
        Assertions.assertNotNull(snapshot);

        final MicroTimer microTimer1 = new MicroTimer(timer1, codehaleMeter);
        Assertions.assertEquals(microTimer, microTimer1);
        Assertions.assertEquals(microTimer.hashCode(), microTimer1.hashCode());
        Assertions.assertEquals(microTimer.toString(), microTimer1.toString());
    }
}
