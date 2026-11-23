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

import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;

import java.util.Objects;

/**
 * {@link Histogram} implementation using <code>Micrometer</code>.
 * 
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroHistogram implements Histogram {
    private final DistributionSummary micrometerDistributionSummary;

    public MicroHistogram(final DistributionSummary micrometerDistributionSummary) {
        AppbaseUtil.notNull(micrometerDistributionSummary, "micrometerDistributionSummary");

        this.micrometerDistributionSummary = micrometerDistributionSummary;
    }

    @Override
    public void update(final int value) {
        micrometerDistributionSummary.record(value);
    }

    @Override
    public void update(final long value) {
        micrometerDistributionSummary.record(value);
    }

    @Override
    public long getCount() {
        return micrometerDistributionSummary.count();
    }

    @Override
    public Snapshot getSnapshot() {
        final HistogramSnapshot histogramSnapshot = micrometerDistributionSummary.takeSnapshot();
        return new MicroSnapshot(histogramSnapshot);
    }

    public DistributionSummary getMicrometerDistributionSummary() {
        return micrometerDistributionSummary;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MicroHistogram that = (MicroHistogram) o;
        return Objects.equals(micrometerDistributionSummary, that.micrometerDistributionSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(micrometerDistributionSummary);
    }

    @Override
    public String toString() {
        return "MicroHistogram{" +
                "micrometerDistributionSummary=" + micrometerDistributionSummary +
                '}';
    }
}
