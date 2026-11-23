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

package com.viveworks.appbase.service.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class HealthCheck {
    private final String serviceName;

    private final boolean healthy;

    @JsonCreator
    public HealthCheck(
            @JsonProperty("serviceName")
            final String serviceName,
            @JsonProperty("healthy")
            final boolean healthy)
    {
        AppbaseUtil.notBlank(serviceName, "serviceName");

        this.serviceName = serviceName;
        this.healthy = healthy;
    }

    public static HealthCheck success(final String serviceName) {
        return new HealthCheck(serviceName, true);
    }

    public static HealthCheck failed(final String serviceName) {
        return new HealthCheck(serviceName, false);
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HealthCheck that = (HealthCheck) o;
        return healthy == that.healthy &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, healthy);
    }

    @Override
    public String toString() {
        return "HealthCheck{" +
                "serviceName='" + serviceName + '\'' +
                ", healthy=" + healthy +
                '}';
    }
}
