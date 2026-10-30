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

package com.viveworks.appbase.client.service.zookeeper.configuration.retry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents {@link BoundedExponentialBackoffRetry} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = BoundedExponentialBackoffRetryConfiguration.Builder.class)
public final class BoundedExponentialBackoffRetryConfiguration implements RetryPolicyConfiguration {
    private final int baseSleepTimeMillis;
    private final int maxSleepTimeMillis;
    private final int maxRetries;

    private BoundedExponentialBackoffRetryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.baseSleepTimeMillis, "builder.baseSleepTimeMillis");
        AppbaseUtil.positiveOrZero(builder.maxSleepTimeMillis, "builder.maxSleepTimeMillis");
        AppbaseUtil.positiveOrZero(builder.maxRetries, "builder.maxRetries");

        baseSleepTimeMillis = builder.baseSleepTimeMillis;
        maxSleepTimeMillis = builder.maxSleepTimeMillis;
        maxRetries = builder.maxRetries;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getBaseSleepTimeMillis() {
        return baseSleepTimeMillis;
    }

    public int getMaxSleepTimeMillis() {
        return maxSleepTimeMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BoundedExponentialBackoffRetryConfiguration that = (BoundedExponentialBackoffRetryConfiguration) o;
        return baseSleepTimeMillis == that.baseSleepTimeMillis &&
                maxSleepTimeMillis == that.maxSleepTimeMillis &&
                maxRetries == that.maxRetries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseSleepTimeMillis, maxSleepTimeMillis, maxRetries);
    }

    @Override
    public String toString() {
        return "BoundedExponentialBackoffRetryConfiguration{" +
                "baseSleepTimeMillis=" + baseSleepTimeMillis +
                ", maxSleepTimeMillis=" + maxSleepTimeMillis +
                ", maxRetries=" + maxRetries +
                '}';
    }

    /**
     * Builder for {@link BoundedExponentialBackoffRetryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final int DEFAULT_BASE_SLEEP_TIME_MILLIS = 1000;
        private static final int DEFAULT_MAX_SLEEP_TIME_MILLIS = Integer.MAX_VALUE;
        private static final int DEFAULT_MAX_RETRIES = 30;

        private int baseSleepTimeMillis = DEFAULT_BASE_SLEEP_TIME_MILLIS;
        private int maxSleepTimeMillis = DEFAULT_MAX_SLEEP_TIME_MILLIS;
        private int maxRetries = DEFAULT_MAX_RETRIES;

        private Builder() {
        }

        public Builder baseSleepTimeMillis(final int baseSleepTimeMillis) {
            this.baseSleepTimeMillis = baseSleepTimeMillis;
            return this;
        }

        public Builder maxSleepTimeMillis(final int maxSleepTimeMillis) {
            this.maxSleepTimeMillis = maxSleepTimeMillis;
            return this;
        }

        public Builder maxRetries(final int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public BoundedExponentialBackoffRetryConfiguration build() {
            return new BoundedExponentialBackoffRetryConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
