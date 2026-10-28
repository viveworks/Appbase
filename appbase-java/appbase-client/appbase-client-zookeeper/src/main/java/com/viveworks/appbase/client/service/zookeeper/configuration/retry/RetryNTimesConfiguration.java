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
import org.apache.curator.retry.RetryNTimes;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Reperesents {@link RetryNTimes} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RetryNTimesConfiguration.Builder.class)
public final class RetryNTimesConfiguration implements RetryPolicyConfiguration {
    private final int numberOfTimes;
    private final int sleepMillisBetweenRetries;

    private RetryNTimesConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.numberOfTimes, "builder.numberOfTimes");
        AppbaseUtil.positiveOrZero(builder.sleepMillisBetweenRetries, "builder.sleepMillisBetweenRetries");

        numberOfTimes = builder.numberOfTimes;
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

    public int getNumberOfTimes() {
        return numberOfTimes;
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
        final RetryNTimesConfiguration that = (RetryNTimesConfiguration) o;
        return numberOfTimes == that.numberOfTimes &&
                sleepMillisBetweenRetries == that.sleepMillisBetweenRetries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfTimes, sleepMillisBetweenRetries);
    }

    @Override
    public String toString() {
        return "RetryNTimesConfiguration{" +
                "numberOfTimes=" + numberOfTimes +
                ", sleepMillisBetweenRetries=" + sleepMillisBetweenRetries +
                '}';
    }

    /**
     * Builder for {@link RetryNTimesConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final int DEFAULT_NUMBER_OF_TIMES = 3;
        private static final int DEFAULT_SLEEP_MILLIS_BETWEEN_RETRIES = 1000;

        private int numberOfTimes = DEFAULT_NUMBER_OF_TIMES;
        private int sleepMillisBetweenRetries = DEFAULT_SLEEP_MILLIS_BETWEEN_RETRIES;

        private Builder() {
        }

        public Builder numberOfTimes(final int numberOfTimes) {
            this.numberOfTimes = numberOfTimes;
            return this;
        }

        public Builder sleepMillisBetweenRetries(final int sleepMillisBetweenRetries) {
            this.sleepMillisBetweenRetries = sleepMillisBetweenRetries;
            return this;
        }

        public RetryNTimesConfiguration build() {
            return new RetryNTimesConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
