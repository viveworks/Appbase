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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.Meter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroMeterGaugeTest {
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
    void testMeterGauge() {
        final Meter meter1 = microMetricRegistry.meter("meter1");
        Assertions.assertNotNull(meter1);
        Assertions.assertTrue(MicroMeterGauge.class.isAssignableFrom(meter1.getClass()));

        final MicroMeterGauge microMeterGauge = (MicroMeterGauge) meter1;
        microMeterGauge.mark();
        Assertions.assertEquals(1, microMeterGauge.getCount());

        microMeterGauge.mark(24);
        Assertions.assertEquals(25, microMeterGauge.getCount());

        final double fifteenMinuteRate = microMeterGauge.getFifteenMinuteRate();
        Assertions.assertTrue(fifteenMinuteRate >= 0);

        final double fiveMinuteRate = microMeterGauge.getFiveMinuteRate();
        Assertions.assertTrue(fiveMinuteRate >= 0);

        final double meanRate = microMeterGauge.getMeanRate();
        Assertions.assertTrue(meanRate >= 0);

        final double oneMinuteRate = microMeterGauge.getOneMinuteRate();
        Assertions.assertTrue(oneMinuteRate >= 0);

        Assertions.assertNotNull(microMeterGauge.getMeter());
        Assertions.assertNotNull(microMeterGauge.getFifteenMinuteRateGauge());
        Assertions.assertNotNull(microMeterGauge.getFiveMinuteRateGauge());
        Assertions.assertNotNull(microMeterGauge.getMeanRateGauge());
        Assertions.assertNotNull(microMeterGauge.getOneMinuteRateGauge());


        final Meter microMeterGauge1 = microMetricRegistry.meter("meter1");
        Assertions.assertEquals(microMeterGauge, microMeterGauge1);
        Assertions.assertEquals(microMeterGauge.hashCode(), microMeterGauge1.hashCode());
        Assertions.assertEquals(microMeterGauge.toString(), microMeterGauge1.toString());
    }
}
