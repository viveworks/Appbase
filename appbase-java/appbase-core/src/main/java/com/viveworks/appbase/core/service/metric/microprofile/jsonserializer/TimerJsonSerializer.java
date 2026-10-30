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
import org.eclipse.microprofile.metrics.Timer;

import java.time.Duration;

/**
 * JSON serialization object (no de-serialization) for {@link Timer}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class TimerJsonSerializer {
    @JsonIgnore
    private final Timer timer;

    public TimerJsonSerializer(final Timer timer) {
        AppbaseUtil.notNull(timer, "timer");

        this.timer = timer;
    }

    public Duration getElapsedTime() {
        return timer.getElapsedTime();
    }

    public long getCount() {
        return timer.getCount();
    }

    public double getFifteenMinuteRate() {
        return timer.getFifteenMinuteRate();
    }

    public double getFiveMinuteRate() {
        return timer.getFiveMinuteRate();
    }

    public double getMeanRate() {
        return timer.getMeanRate();
    }

    public double getOneMinuteRate() {
        return timer.getOneMinuteRate();
    }

    public SnapshotJsonSerializer getSnapshot() {
        return new SnapshotJsonSerializer(timer.getSnapshot());
    }

    @Override
    public String toString() {
        return "TimerJsonSerializer{" +
                "timer=" + timer +
                '}';
    }
}
