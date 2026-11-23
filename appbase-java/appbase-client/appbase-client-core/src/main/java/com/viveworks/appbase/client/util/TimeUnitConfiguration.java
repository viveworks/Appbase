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

package com.viveworks.appbase.client.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link TimeUnit}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = TimeUnitConfiguration.Builder.class)
public final class TimeUnitConfiguration {
    private final boolean enabled;
    private final long time;
    private final TimeUnit timeUnit;

    private TimeUnitConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.enabled) {
            AppbaseUtil.positiveOrZero(builder.time, "builder.time");
            AppbaseUtil.notNull(builder.timeUnit, "builder.timeUnit");
        }

        enabled = builder.enabled;
        time = builder.time;
        timeUnit = builder.timeUnit;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TimeUnitConfiguration that = (TimeUnitConfiguration) o;
        return enabled == that.enabled &&
                time == that.time &&
                timeUnit == that.timeUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, time, timeUnit);
    }

    @Override
    public String toString() {
        return "TimeUnitConfiguration{" +
                "enabled=" + enabled +
                ", time=" + time +
                ", timeUnit=" + timeUnit +
                '}';
    }

    /**
     * Builder for {@link TimeUnitConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;

        private boolean enabled = DEFAULT_ENABLED;
        private long time;
        private TimeUnit timeUnit;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder time(final long time) {
            this.time = time;
            return this;
        }

        public Builder timeUnit(final TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public TimeUnitConfiguration build() {
            return new TimeUnitConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
