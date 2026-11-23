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

package com.viveworks.appbase.core.service.metric.microprofile.jsonserializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Snapshot;

/**
 * JSON serialization object (no de-serialization) for {@link Snapshot}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class SnapshotJsonSerializer {
    @JsonIgnore
    private final Snapshot snapshot;

    public SnapshotJsonSerializer(final Snapshot snapshot) {
        AppbaseUtil.notNull(snapshot, "snapshot");

        this.snapshot = snapshot;
    }

    public double getMedian() {
        return snapshot.getMedian();
    }

    public double get75thPercentile() {
        return snapshot.get75thPercentile();
    }

    public double get95thPercentile() {
        return snapshot.get95thPercentile();
    }

    public double get98thPercentile() {
        return snapshot.get98thPercentile();
    }

    public double get99thPercentile() {
        return snapshot.get99thPercentile();
    }

    public double get999thPercentile() {
        return snapshot.get999thPercentile();
    }

    public long getMax() {
        return snapshot.getMax();
    }

    public double getMean() {
        return snapshot.getMean();
    }

    public long getMin() {
        return snapshot.getMin();
    }

    public double getStdDev() {
        return snapshot.getStdDev();
    }

    @Override
    public String toString() {
        return "SnapshotJsonSerializer{" +
                "median=" + getMedian() +
                "75thPercentile=" + get75thPercentile() +
                "95thPercentile=" + get95thPercentile() +
                "98thPercentile=" + get98thPercentile() +
                "99thPercentile=" + get99thPercentile() +
                "999thPercentile=" + get999thPercentile() +
                "max=" + getMax() +
                "mean=" + getMean() +
                "min=" + getMin() +
                "stdDev=" + getStdDev() +
                '}';
    }
}
