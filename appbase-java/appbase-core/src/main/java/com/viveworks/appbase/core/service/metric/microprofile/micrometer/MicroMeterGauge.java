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
import io.micrometer.core.instrument.Gauge;
import org.eclipse.microprofile.metrics.Meter;

import java.util.Objects;

/**
 * {@link Meter} using {@link com.codahale.metrics.Meter} and {@link io.micrometer.core.instrument.Gauge}s.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MicroMeterGauge implements Meter {
    private final com.codahale.metrics.Meter meter;
    private final Gauge fifteenMinuteRateGauge;
    private final Gauge fiveMinuteRateGauge;
    private final Gauge meanRateGauge;
    private final Gauge oneMinuteRateGauge;

    public MicroMeterGauge(final com.codahale.metrics.Meter meter,
                           final Gauge fifteenMinuteRateGauge,
                           final Gauge fiveMinuteRateGauge,
                           final Gauge meanRateGauge,
                           final Gauge oneMinuteRateGauge)
    {
        AppbaseUtil.notNull(meter, "meter");
        AppbaseUtil.notNull(fifteenMinuteRateGauge, "fifteenMinuteRateGauge");
        AppbaseUtil.notNull(fiveMinuteRateGauge, "fiveMinuteRateGauge");
        AppbaseUtil.notNull(meanRateGauge, "meanRateGauge");
        AppbaseUtil.notNull(oneMinuteRateGauge, "oneMinuteRateGauge");

        this.meter = meter;
        this.fifteenMinuteRateGauge = fifteenMinuteRateGauge;
        this.fiveMinuteRateGauge = fiveMinuteRateGauge;
        this.meanRateGauge = meanRateGauge;
        this.oneMinuteRateGauge = oneMinuteRateGauge;
    }

    @Override
    public void mark() {
        meter.mark();
    }

    @Override
    public void mark(final long l) {
        meter.mark(l);
    }

    @Override
    public long getCount() {
        return meter.getCount();
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

    public com.codahale.metrics.Meter getMeter() {
        return meter;
    }

    public Gauge getFifteenMinuteRateGauge() {
        return fifteenMinuteRateGauge;
    }

    public Gauge getFiveMinuteRateGauge() {
        return fiveMinuteRateGauge;
    }

    public Gauge getMeanRateGauge() {
        return meanRateGauge;
    }

    public Gauge getOneMinuteRateGauge() {
        return oneMinuteRateGauge;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MicroMeterGauge that = (MicroMeterGauge) o;
        return Objects.equals(meter, that.meter) &&
                Objects.equals(fifteenMinuteRateGauge, that.fifteenMinuteRateGauge) &&
                Objects.equals(fiveMinuteRateGauge, that.fiveMinuteRateGauge) &&
                Objects.equals(meanRateGauge, that.meanRateGauge) &&
                Objects.equals(oneMinuteRateGauge, that.oneMinuteRateGauge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meter, fifteenMinuteRateGauge, fiveMinuteRateGauge, meanRateGauge, oneMinuteRateGauge);
    }

    @Override
    public String toString() {
        return "MicroMeterGauge{" +
                "meter=" + meter +
                ", fifteenMinuteRateGauge=" + fifteenMinuteRateGauge +
                ", fiveMinuteRateGauge=" + fiveMinuteRateGauge +
                ", meanRateGauge=" + meanRateGauge +
                ", oneMinuteRateGauge=" + oneMinuteRateGauge +
                '}';
    }
}
