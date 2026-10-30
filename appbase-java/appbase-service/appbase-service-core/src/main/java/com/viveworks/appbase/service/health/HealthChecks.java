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

package com.viveworks.appbase.service.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class HealthChecks {
    private final boolean healthy;

    @JsonSerialize(contentAs = HealthCheck.class)
    @JsonDeserialize(contentAs = HealthCheck.class)
    private final List<HealthCheck> healthChecks;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final List<String> failedHealthCheckers;

    private final long snapshotAgeMillis;

    /**
     * Backward-compatible constructor. Reports an age of zero, i.e. "produced just now".
     *
     * @param healthy overall health
     * @param healthChecks individual results
     * @param failedHealthCheckers checkers that threw
     */
    public HealthChecks(
            final boolean healthy,
            final List<HealthCheck> healthChecks,
            final List<String> failedHealthCheckers)
    {
        this(healthy, healthChecks, failedHealthCheckers, 0L);
    }

    /**
     * @param healthy overall health
     * @param healthChecks individual results
     * @param failedHealthCheckers checkers that threw
     * @param snapshotAgeMillis milliseconds since the snapshot was produced; lets a caller judge staleness
     */
    @JsonCreator
    public HealthChecks(
            @JsonProperty("healthy")
            final boolean healthy,
            @JsonProperty("healthChecks")
            final List<HealthCheck> healthChecks,
            @JsonProperty("failedHealthCheckers")
            final List<String> failedHealthCheckers,
            @JsonProperty("snapshotAgeMillis")
            final long snapshotAgeMillis)
    {
        this.healthy = healthy;
        this.snapshotAgeMillis = snapshotAgeMillis;

        if (healthChecks != null) {
            this.healthChecks = new ArrayList<>(healthChecks);
        } else {
            this.healthChecks = List.of();
        }

        if (failedHealthCheckers != null) {
            this.failedHealthCheckers = new ArrayList<>(failedHealthCheckers);
        } else {
            this.failedHealthCheckers = List.of();
        }
    }

    public boolean isHealthy() {
        return healthy;
    }

    public List<HealthCheck> getHealthChecks() {
        return healthChecks;
    }

    public List<String> getFailedHealthCheckers() {
        return failedHealthCheckers;
    }

    /**
     * @return milliseconds since this snapshot was produced
     */
    public long getSnapshotAgeMillis() {
        return snapshotAgeMillis;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HealthChecks that = (HealthChecks) o;
        return healthy == that.healthy &&
                snapshotAgeMillis == that.snapshotAgeMillis &&
                Objects.equals(healthChecks, that.healthChecks) &&
                Objects.equals(failedHealthCheckers, that.failedHealthCheckers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthy, healthChecks, failedHealthCheckers, snapshotAgeMillis);
    }

    @Override
    public String toString() {
        return "HealthChecks{" +
                "healthy=" + healthy +
                ", healthChecks=" + healthChecks +
                ", failedHealthCheckers=" + failedHealthCheckers +
                ", snapshotAgeMillis=" + snapshotAgeMillis +
                '}';
    }
}
