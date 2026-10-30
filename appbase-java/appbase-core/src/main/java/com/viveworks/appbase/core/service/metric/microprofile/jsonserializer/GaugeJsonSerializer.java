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
import org.eclipse.microprofile.metrics.Gauge;

/**
 * JSON serialization object (no de-serialization) for {@link Gauge}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class GaugeJsonSerializer {
    @JsonIgnore
    private final Gauge<?> gauge;

    public GaugeJsonSerializer(final Gauge<?> gauge) {
        AppbaseUtil.notNull(gauge, "gauge");

        this.gauge = gauge;
    }

    /**
     * Get the {@link Gauge} value as a {@link Number}, else {@link Double#NaN}.
     *
     * @return Numeric value
     */
    public Number getValue() {
        final Object value = gauge.getValue();

        if (value != null) {
            if (value instanceof Number) {
                return (Number) value;
            }
        }

        return Double.NaN;
    }

    @Override
    public String toString() {
        return "GaugeJsonSerializer{" +
                "value=" + getValue() +
                '}';
    }
}
