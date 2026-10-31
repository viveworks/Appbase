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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Meter;

/**
 * JSON serialization object (no de-serialization) for {@link Meter}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class MeterJsonSerializer {
    @JsonIgnore
    private final Meter meter;

    public MeterJsonSerializer(final Meter meter) {
        AppbaseUtil.notNull(meter, "meter");

        this.meter = meter;
    }

    public long getCount() {
        return meter.getCount();
    }


    public double getFifteenMinuteRate() {
        return meter.getFifteenMinuteRate();
    }


    public double getFiveMinuteRate() {
        return meter.getFiveMinuteRate();
    }


    public double getMeanRate() {
        return meter.getMeanRate();
    }

    public double getOneMinuteRate() {
        return meter.getOneMinuteRate();
    }

    @Override
    public String toString() {
        return "MeterJsonSerializer{" +
                "count=" + getCount() +
                "fifteenMinuteRate=" + getFifteenMinuteRate() +
                "fiveMinuteRate=" + getFiveMinuteRate() +
                "meanRate=" + getMeanRate() +
                "oneMinuteRate=" + getOneMinuteRate() +
                '}';
    }
}
