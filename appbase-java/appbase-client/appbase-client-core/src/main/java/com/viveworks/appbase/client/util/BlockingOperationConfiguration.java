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

package com.viveworks.appbase.client.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a blocking operation configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = BlockingOperationConfiguration.Builder.class)
public final class BlockingOperationConfiguration {
    private final boolean enabled;
    private final boolean waitUntilComplete;
    private final long waitTimeout;
    private final TimeUnit waitTimeoutTimeUnit;

    private BlockingOperationConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (!builder.waitUntilComplete) {
            AppbaseUtil.positiveOrZero(builder.waitTimeout, "builder.waitTimeout");
            AppbaseUtil.notNull(builder.waitTimeoutTimeUnit, "builder.waitTimeoutTimeUnit");
        }

        enabled = builder.enabled;
        waitUntilComplete = builder.waitUntilComplete;
        waitTimeout = builder.waitTimeout;
        waitTimeoutTimeUnit = builder.waitTimeoutTimeUnit;
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

    public boolean isWaitUntilComplete() {
        return waitUntilComplete;
    }

    public long getWaitTimeout() {
        return waitTimeout;
    }

    public TimeUnit getWaitTimeoutTimeUnit() {
        return waitTimeoutTimeUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BlockingOperationConfiguration that = (BlockingOperationConfiguration) o;
        return enabled == that.enabled &&
                waitUntilComplete == that.waitUntilComplete &&
                waitTimeout == that.waitTimeout &&
                waitTimeoutTimeUnit == that.waitTimeoutTimeUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, waitUntilComplete, waitTimeout, waitTimeoutTimeUnit);
    }

    @Override
    public String toString() {
        return "BlockingOperationConfiguration{" +
                "enabled=" + enabled +
                ", waitUntilComplete=" + waitUntilComplete +
                ", waitTimeout=" + waitTimeout +
                ", waitTimeoutTimeUnit=" + waitTimeoutTimeUnit +
                '}';
    }


    /**
     * Builder for {@link BlockingOperationConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final boolean DEFAULT_WAIT_UNTIL_COMPLETE = false;
        private static final long DEFAULT_WAIT_TIMEOUT = 30;
        private static final TimeUnit DEFAULT_WAIT_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;


        private boolean enabled = DEFAULT_ENABLED;
        private boolean waitUntilComplete = DEFAULT_WAIT_UNTIL_COMPLETE;
        private long waitTimeout = DEFAULT_WAIT_TIMEOUT;
        private TimeUnit waitTimeoutTimeUnit = DEFAULT_WAIT_TIMEOUT_TIME_UNIT;


        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder waitUntilComplete(final boolean waitUntilComplete) {
            this.waitUntilComplete = waitUntilComplete;
            return this;
        }

        public Builder waitTimeout(final long waitTimeout) {
            this.waitTimeout = waitTimeout;
            return this;
        }

        public Builder waitTimeoutTimeUnit(final TimeUnit waitTimeoutTimeUnit) {
            this.waitTimeoutTimeUnit = waitTimeoutTimeUnit;
            return this;
        }

        public BlockingOperationConfiguration build() {
            return new BlockingOperationConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
