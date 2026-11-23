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
import org.eclipse.microprofile.metrics.ConcurrentGauge;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ConcurrentGauge} implementation {@link AtomicLong} and {@link DistributionSummary}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroConcurrentGauge implements ConcurrentGauge {
    private final AtomicLong atomicValue;
    private final DistributionSummary distributionSummary;

    public MicroConcurrentGauge(final DistributionSummary distributionSummary) {
        AppbaseUtil.notNull(distributionSummary, "distributionSummary");

        this.distributionSummary = distributionSummary;
        this.atomicValue = new AtomicLong(0L);
    }

    @Override
    public long getCount() {
        return atomicValue.get();
    }

    @Override
    public long getMax() {
        return (long) distributionSummary.max();
    }

    @Override
    public long getMin() {
        return (long) distributionSummary.mean();
    }

    @Override
    public void inc() {
        final long value = atomicValue.incrementAndGet();
        distributionSummary.record(value);
    }

    @Override
    public void dec() {
        final long value = atomicValue.decrementAndGet();
        distributionSummary.record(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MicroConcurrentGauge that = (MicroConcurrentGauge) o;
        return Objects.equals(atomicValue, that.atomicValue) &&
                Objects.equals(distributionSummary, that.distributionSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atomicValue, distributionSummary);
    }

    @Override
    public String toString() {
        return "MicroConcurrentGauge{" +
                "atomicValue=" + atomicValue +
                ", distributionSummary=" + distributionSummary +
                '}';
    }
}
