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

package com.viveworks.appbase.core.service.metric.microprofile.jsonserializer;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.microprofile.micrometer.MicroMetricRegistry;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
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
class ConcurrentGaugeJsonSerializerTest {
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
    void testSerializer() throws Exception {
        final ConcurrentGauge concurrentGauge = microMetricRegistry.concurrentGauge("concurrent-gauge-1");
        Assertions.assertNotNull(concurrentGauge);

        final ConcurrentGaugeJsonSerializer concurrentGaugeJsonEncoder =
                new ConcurrentGaugeJsonSerializer(concurrentGauge);
        Assertions.assertEquals(0, concurrentGaugeJsonEncoder.getCount());
        Assertions.assertEquals(0, concurrentGaugeJsonEncoder.getMin());
        Assertions.assertEquals(0, concurrentGaugeJsonEncoder.getMax());

        concurrentGauge.inc();
        Assertions.assertEquals(1, concurrentGaugeJsonEncoder.getCount());
        Assertions.assertNotNull(concurrentGaugeJsonEncoder.toString());

        final String json = JsonUtil
                .getJsonMapper()
                .writerFor(ConcurrentGaugeJsonSerializer.class)
                .writeValueAsString(concurrentGaugeJsonEncoder);
        Assertions.assertNotNull(json);
    }
}
