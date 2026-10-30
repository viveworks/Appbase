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
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Timer;

import java.util.Objects;

/**
 * {@link Timer.Context} implementation using {@link io.micrometer.core.instrument.Timer.Sample}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroTimerContext implements Timer.Context {
    private final io.micrometer.core.instrument.Timer timer;
    private final io.micrometer.core.instrument.Timer.Sample sample;
    private final Meter meter;

    public MicroTimerContext(final io.micrometer.core.instrument.Timer timer,
                             final io.micrometer.core.instrument.Timer.Sample sample,
                             final Meter meter)
    {
        AppbaseUtil.notNull(timer, "timer");
        AppbaseUtil.notNull(sample, "sample");
        AppbaseUtil.notNull(meter, "meter");

        this.timer = timer;
        this.sample = sample;
        this.meter = meter;
    }

    @Override
    public long stop() {
        meter.mark();
        return sample.stop(timer);
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MicroTimerContext that = (MicroTimerContext) o;
        return Objects.equals(timer, that.timer) &&
                Objects.equals(sample, that.sample) &&
                Objects.equals(meter, that.meter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timer, sample, meter);
    }

    @Override
    public String toString() {
        return "MicroTimerContext{" +
                "timer=" + timer +
                ", sample=" + sample +
                ", meter=" + meter +
                '}';
    }
}
