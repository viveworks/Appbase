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
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.eclipse.microprofile.metrics.Snapshot;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

/**
 * {@link Snapshot} implementation using {@link HistogramSnapshot}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroSnapshot extends Snapshot {
    private static final double HUNDRED = 100;
    private final HistogramSnapshot histogramSnapshot;

    public MicroSnapshot(final HistogramSnapshot histogramSnapshot) {
        AppbaseUtil.notNull(histogramSnapshot, "histogramSnapshot");

        this.histogramSnapshot = histogramSnapshot;
    }

    @Override
    public double getValue(final double quantile) {
        final ValueAtPercentile[] valueAtPercentiles = histogramSnapshot.percentileValues();
        for (final ValueAtPercentile valueAtPercentile : valueAtPercentiles) {
            final double percentile = valueAtPercentile.percentile();
            if (percentile == quantile) {
                return valueAtPercentile.value();
            }
        }

        return Double.NaN;
    }

    @Override
    public long[] getValues() {
        final ValueAtPercentile[] valueAtPercentiles = histogramSnapshot.percentileValues();
        final long[] values = new long[valueAtPercentiles.length];

        for (int i = 0; i < valueAtPercentiles.length; ++i) {
            final ValueAtPercentile valueAtPercentile = valueAtPercentiles[i];
            final double value = valueAtPercentile.value();
            values[i] = (long) value;
        }

        return values;

    }

    @Override
    public int size() {
        final ValueAtPercentile[] valueAtPercentiles = histogramSnapshot.percentileValues();
        return valueAtPercentiles.length;
    }

    @Override
    public long getMax() {
        return (long) histogramSnapshot.max();
    }

    @Override
    public double getMean() {
        return histogramSnapshot.mean();
    }

    @Override
    public long getMin() {
        return (long) Double.NaN;
    }

    @Override
    public double getStdDev() {
        return Double.NaN;
    }

    @Override
    public void dump(final OutputStream output) {
        AppbaseUtil.notNull(output, "output");

        final PrintStream printStream = new PrintStream(output);
        histogramSnapshot.outputSummary(printStream, HUNDRED);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MicroSnapshot that = (MicroSnapshot) o;
        return Objects.equals(histogramSnapshot, that.histogramSnapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(histogramSnapshot);
    }

    @Override
    public String toString() {
        return "MicroSnapshot{" +
                "histogramSnapshot=" + histogramSnapshot +
                '}';
    }
}
