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

package com.viveworks.appbase.client.cloud.aws.common.configuration.backoff;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link EqualJitterBackoffStrategy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = EqualJitterBackoffStrategyConfiguration.Builder.class)
public final class EqualJitterBackoffStrategyConfiguration implements AwsSdkBackoffStrategyConfiguration {
    private final Duration baseDelay;
    private final Duration maxBackoffTime;

    private EqualJitterBackoffStrategyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.baseDelay, "builder.baseDelay");
        AppbaseUtil.notNull(builder.maxBackoffTime, "builder.maxBackoffTime");

        baseDelay = builder.baseDelay;
        maxBackoffTime = builder.maxBackoffTime;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the configured base delay.
     *
     * @return the configured base delay
     */
    public Duration getBaseDelay() {
        return baseDelay;
    }

    /**
     * Returns the configured max backoff time.
     *
     * @return the configured max backoff time
     */
    public Duration getMaxBackoffTime() {
        return maxBackoffTime;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EqualJitterBackoffStrategyConfiguration that = (EqualJitterBackoffStrategyConfiguration) o;
        return Objects.equals(baseDelay, that.baseDelay) && Objects.equals(maxBackoffTime, that.maxBackoffTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseDelay, maxBackoffTime);
    }

    @Override
    public String toString() {
        return "EqualJitterBackoffStrategyConfiguration{" +
                "baseDelay=" + baseDelay +
                ", maxBackoffTime=" + maxBackoffTime +
                '}';
    }


    /**
     * Builder for {@link EqualJitterBackoffStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Duration DEFAULT_BASE_DELAY = Duration.ofSeconds(1);
        private static final Duration DEFAULT_MAX_BACKOFF_TIME = SdkDefaultRetrySetting.MAX_BACKOFF;

        private Duration baseDelay = DEFAULT_BASE_DELAY;
        private Duration maxBackoffTime = DEFAULT_MAX_BACKOFF_TIME;

        private Builder() {
        }

        public Builder baseDelay(final Duration baseDelay) {
            this.baseDelay = baseDelay;
            return this;
        }

        public Builder maxBackoffTime(final Duration maxBackoffTime) {
            this.maxBackoffTime = maxBackoffTime;
            return this;
        }

        public EqualJitterBackoffStrategyConfiguration build() {
            return new EqualJitterBackoffStrategyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
