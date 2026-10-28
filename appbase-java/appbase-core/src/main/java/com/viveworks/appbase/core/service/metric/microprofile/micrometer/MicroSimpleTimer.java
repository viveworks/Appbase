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

import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.eclipse.microprofile.metrics.SimpleTimer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * {@link SimpleTimer} implementation using {@link io.micrometer.core.instrument.Timer}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroSimpleTimer implements SimpleTimer {
    private final io.micrometer.core.instrument.Timer timer;

    public MicroSimpleTimer(final Timer timer) {
        AppbaseUtil.notNull(timer, "timer");

        this.timer = timer;
    }

    @Override
    public void update(final Duration duration) {
        timer.record(duration);
    }

    @Override
    public <T> T time(final Callable<T> event) throws Exception {
        final io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        try {
            return event.call();
        } finally {
            sample.stop(timer);
        }
    }

    @Override
    public void time(final Runnable event) {
        final io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        try {
            event.run();
        } finally {
            sample.stop(timer);
        }
    }

    @Override
    public Context time() {
        final io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();
        return new MicroSimpleTimerContext(timer, sample);
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
    public Duration getMaxTimeDuration() {
        final HistogramSnapshot histogramSnapshot = timer.takeSnapshot();
        final double max = histogramSnapshot.max(TimeUnit.NANOSECONDS);
        return Duration.of((long) max, ChronoUnit.NANOS);
    }

    @Override
    public Duration getMinTimeDuration() {
        final HistogramSnapshot histogramSnapshot = timer.takeSnapshot();
        final double mean = histogramSnapshot.mean(TimeUnit.NANOSECONDS);
        return Duration.of((long) mean, ChronoUnit.NANOS);
    }

    public Timer getTimer() {
        return timer;
    }
}
