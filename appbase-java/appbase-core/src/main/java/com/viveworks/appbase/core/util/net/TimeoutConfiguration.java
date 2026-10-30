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

package com.viveworks.appbase.core.util.net;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for a timeout (e.g write / receive timeout).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = TimeoutConfiguration.Builder.class)
public final class TimeoutConfiguration {
    private final boolean enabled;
    private final long timeout;
    private final TimeUnit timeoutTimeUnit;

    private TimeoutConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.timeout, "builder.timeout");
        AppbaseUtil.notNull(builder.timeoutTimeUnit, "builder.timeoutTimeUnit");

        enabled = builder.enabled;
        timeout = builder.timeout;
        timeoutTimeUnit = builder.timeoutTimeUnit;
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

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutTimeUnit() {
        return timeoutTimeUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TimeoutConfiguration that = (TimeoutConfiguration) o;
        return enabled == that.enabled &&
                timeout == that.timeout &&
                timeoutTimeUnit == that.timeoutTimeUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, timeout, timeoutTimeUnit);
    }

    @Override
    public String toString() {
        return "TimeoutConfiguration{" +
                "enabled=" + enabled +
                ", timeout=" + timeout +
                ", timeoutTimeUnit=" + timeoutTimeUnit +
                '}';
    }

    /**
     * Builder for {@link TimeoutConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final long DEFAULT_TIMEOUT = 200;
        private static final TimeUnit DEFAULT_TIMEOUT_TIME_UNIT = TimeUnit.MILLISECONDS;

        private boolean enabled = DEFAULT_ENABLED;
        private long timeout = DEFAULT_TIMEOUT;
        private TimeUnit timeoutTimeUnit = DEFAULT_TIMEOUT_TIME_UNIT;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder timeout(final long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder timeoutTimeUnit(final TimeUnit timeoutTimeUnit) {
            this.timeoutTimeUnit = timeoutTimeUnit;
            return this;
        }

        public TimeoutConfiguration build() {
            return new TimeoutConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
