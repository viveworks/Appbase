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
import org.apache.curator.retry.RetryOneTime;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents {@link RetryOneTime} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = RetryOneTimeConfiguration.Builder.class)
public final class RetryOneTimeConfiguration implements RetryPolicyConfiguration {
    private final int sleepMillisBetweenRetry;

    private RetryOneTimeConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.sleepMillisBetweenRetry, "builder.sleepMillisBetweenRetry");

        sleepMillisBetweenRetry = builder.sleepMillisBetweenRetry;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getSleepMillisBetweenRetry() {
        return sleepMillisBetweenRetry;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RetryOneTimeConfiguration that = (RetryOneTimeConfiguration) o;
        return sleepMillisBetweenRetry == that.sleepMillisBetweenRetry;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sleepMillisBetweenRetry);
    }

    @Override
    public String toString() {
        return "RetryOneTimeConfiguration{" +
                "sleepMillisBetweenRetry=" + sleepMillisBetweenRetry +
                '}';
    }

    /**
     * Builder for {@link RetryOneTimeConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final int DEFAULT_SLEEP_MILLIS_BETWEEN_RETRY = 1000;

        private int sleepMillisBetweenRetry = DEFAULT_SLEEP_MILLIS_BETWEEN_RETRY;

        private Builder() {
        }

        public Builder sleepMillisBetweenRetry(final int sleepMillisBetweenRetry) {
            this.sleepMillisBetweenRetry = sleepMillisBetweenRetry;
            return this;
        }

        public RetryOneTimeConfiguration build() {
            return new RetryOneTimeConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
