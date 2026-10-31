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

package com.viveworks.appbase.core.service.metric.microprofile.jsonserializer;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.microprofile.micrometer.MicroMetricRegistry;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MetricRegistryJsonSerializerTest {
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
        final Gauge<Integer> gauge1 = () -> 1;
        microMetricRegistry.register("gauge1", gauge1);

        final Gauge<Integer> gauge2 = () -> 2;
        microMetricRegistry.register("gauge2", gauge2);

        microMetricRegistry.counter("counter1");
        microMetricRegistry.counter("counter2");

        microMetricRegistry.concurrentGauge("concurrentGauge1");
        microMetricRegistry.concurrentGauge("concurrentGauge2");

        microMetricRegistry.histogram("histogram1");
        microMetricRegistry.histogram("histogram2");

        microMetricRegistry.meter("meter1");
        microMetricRegistry.meter("meter2");

        microMetricRegistry.timer("timer1");
        microMetricRegistry.timer("timer2");

        microMetricRegistry.simpleTimer("simpleTimer1");
        microMetricRegistry.simpleTimer("simpleTimer2");

        final MetricRegistryJsonSerializer metricRegistryJsonEncoder =
                new MetricRegistryJsonSerializer(microMetricRegistry);
        final String json = JsonUtil
                .getJsonMapper()
                .writerFor(MetricRegistryJsonSerializer.class)
                .writeValueAsString(metricRegistryJsonEncoder);
        Assertions.assertNotNull(json);

        Assertions.assertNotNull(metricRegistryJsonEncoder.toString());
    }
}
