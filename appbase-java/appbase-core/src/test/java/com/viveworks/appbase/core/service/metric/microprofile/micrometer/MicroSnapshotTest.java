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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroSnapshotTest {
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
    void testSnapshot() {
        metricService.registerMicrometerRegistry(new SimpleMeterRegistry());
        final MicroHistogram histogram1 = (MicroHistogram) metricService.getMetricRegistry().histogram("histogram1");
        final DistributionSummary distributionSummary1 = histogram1.getMicrometerDistributionSummary();

        final MicroHistogram microHistogram = new MicroHistogram(distributionSummary1);
        for (int i = 0; i < 10000; ++i) {
            microHistogram.update(24 + (i * 2));
            microHistogram.update(9 + (i * 2));
            microHistogram.update(23 + (i * 2));
        }

        final Snapshot snapshot = microHistogram.getSnapshot();
        Assertions.assertNotNull(snapshot);
        Assertions.assertTrue(snapshot.getMax() > 1);
        Assertions.assertTrue(snapshot.getMean() > 1);
        Assertions.assertEquals((long) Double.NaN, snapshot.getMin());
        Assertions.assertEquals(Double.NaN, snapshot.getStdDev());
        Assertions.assertEquals(snapshot.get95thPercentile(), snapshot.getValue(0.95));
        Assertions.assertEquals(snapshot, snapshot);

        final long[] values = snapshot.getValues();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(5, values.length);

        final int size = snapshot.size();
        Assertions.assertEquals(5, size);

        final double snapshot75thPercentile = snapshot.get75thPercentile();
        Assertions.assertTrue(snapshot75thPercentile > 1);

        final double snapshot95thPercentile = snapshot.get95thPercentile();
        Assertions.assertTrue(snapshot95thPercentile > 1);

        final double snapshot98thPercentile = snapshot.get98thPercentile();
        Assertions.assertTrue(snapshot98thPercentile > 1);

        final double snapshot99thPercentile = snapshot.get99thPercentile();
        Assertions.assertTrue(snapshot99thPercentile > 1);

        final double snapshot999thPercentile = snapshot.get999thPercentile();
        Assertions.assertTrue(snapshot999thPercentile > 1);

        final double median = snapshot.getMedian();
        Assertions.assertEquals(Double.NaN, median);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Assertions.assertDoesNotThrow(() -> snapshot.dump(outputStream));
    }
}
