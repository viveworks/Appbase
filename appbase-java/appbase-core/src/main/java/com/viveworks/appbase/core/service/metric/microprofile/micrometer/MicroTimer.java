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

import com.codahale.metrics.Meter;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * {@link Timer} implementation using {@link io.micrometer.core.instrument.Timer}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroTimer implements Timer {
    private final io.micrometer.core.instrument.Timer timer;
    private final Meter meter;

    public MicroTimer(final io.micrometer.core.instrument.Timer timer, final Meter meter) {
        AppbaseUtil.notNull(timer, "timer");
        AppbaseUtil.notNull(meter, "meter");

        this.timer = timer;
        this.meter = meter;
    }

    @Override
    public void update(final Duration duration) {
        timer.record(duration);
        meter.mark();
    }

    @Override
    public <T> T time(final Callable<T> event) throws Exception {
        final io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        try {
            meter.mark();
            return event.call();
        } finally {
            sample.stop(timer);
        }
    }

    @Override
    public void time(final Runnable event) {
        final io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        try {
            meter.mark();
            event.run();
        } finally {
            sample.stop(timer);
        }
    }

    @Override
    public Context time() {
        final io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        return new MicroTimerContext(timer, sample, meter);
    }

    @Override
    public Duration getElapsedTime() {
        final double totalTime = timer.totalTime(TimeUnit.NANOSECONDS);
        return Duration.ofNanos((long) totalTime);
    }

    @Override
    public long getCount() {
        return timer.count();
    }

    @Override
    public double getFifteenMinuteRate() {
        return meter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return meter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return meter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return meter.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        final HistogramSnapshot histogramSnapshot = timer.takeSnapshot();
        return new MicroSnapshot(histogramSnapshot);
    }

    public io.micrometer.core.instrument.Timer getTimer() {
        return timer;
    }

    public Meter getMeter() {
        return meter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MicroTimer that = (MicroTimer) o;
        return Objects.equals(timer, that.timer) &&
                Objects.equals(meter, that.meter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timer, meter);
    }

    @Override
    public String toString() {
        return "MicroTimer{" +
                "timer=" + timer +
                ", meter=" + meter +
                '}';
    }
}
