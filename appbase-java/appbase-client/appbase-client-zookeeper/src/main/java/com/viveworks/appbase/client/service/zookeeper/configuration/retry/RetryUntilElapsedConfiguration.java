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

package com.viveworks.appbase.client.service.zookeeper.configuration.retry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.curator.retry.RetryUntilElapsed;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents {@link RetryUntilElapsed} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RetryUntilElapsedConfiguration.Builder.class)
public final class RetryUntilElapsedConfiguration implements RetryPolicyConfiguration {
    private final int maxElapsedTimeMillis;
    private final int sleepMillisBetweenRetries;

    private RetryUntilElapsedConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.maxElapsedTimeMillis, "builder.maxElapsedTimeMillis");
        AppbaseUtil.positiveOrZero(builder.sleepMillisBetweenRetries, "builder.sleepMillisBetweenRetries");

        maxElapsedTimeMillis = builder.maxElapsedTimeMillis;
        sleepMillisBetweenRetries = builder.sleepMillisBetweenRetries;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getMaxElapsedTimeMillis() {
        return maxElapsedTimeMillis;
    }

    public int getSleepMillisBetweenRetries() {
        return sleepMillisBetweenRetries;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RetryUntilElapsedConfiguration that = (RetryUntilElapsedConfiguration) o;
        return maxElapsedTimeMillis == that.maxElapsedTimeMillis &&
                sleepMillisBetweenRetries == that.sleepMillisBetweenRetries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxElapsedTimeMillis, sleepMillisBetweenRetries);
    }

    @Override
    public String toString() {
        return "RetryUntilElapsedConfiguration{" +
                "maxElapsedTimeMillis=" + maxElapsedTimeMillis +
                ", sleepMillisBetweenRetries=" + sleepMillisBetweenRetries +
                '}';
    }

    /**
     * Builder for {@link RetryUntilElapsedConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final int DEFAULT_MAX_ELAPSED_TIME_MILLIS = 15 * 60 * 1000;
        private static final int DEFAULT_SLEEP_MILLIS_BETWEEN_RETRIES = 1000;

        private int maxElapsedTimeMillis = DEFAULT_MAX_ELAPSED_TIME_MILLIS;
        private int sleepMillisBetweenRetries = DEFAULT_SLEEP_MILLIS_BETWEEN_RETRIES;

        private Builder() {
        }

        public Builder maxElapsedTimeMillis(final int maxElapsedTimeMillis) {
            this.maxElapsedTimeMillis = maxElapsedTimeMillis;
            return this;
        }

        public Builder sleepMillisBetweenRetries(final int sleepMillisBetweenRetries) {
            this.sleepMillisBetweenRetries = sleepMillisBetweenRetries;
            return this;
        }

        public RetryUntilElapsedConfiguration build() {
            return new RetryUntilElapsedConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
