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

package com.viveworks.appbase.client.cloud.aws.common.configuration.backoff;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link FixedDelayBackoffStrategy}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = FixedDelayBackoffStrategyConfiguration.Builder.class)
public final class FixedDelayBackoffStrategyConfiguration implements AwsSdkBackoffStrategyConfiguration {
    private final Duration fixedBackoff;

    private FixedDelayBackoffStrategyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.fixedBackoff, "builder.fixedBackoff");

        fixedBackoff = builder.fixedBackoff;
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
     * Returns the configured fixed backoff.
     *
     * @return the configured fixed backoff
     */
    public Duration getFixedBackoff() {
        return fixedBackoff;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FixedDelayBackoffStrategyConfiguration that = (FixedDelayBackoffStrategyConfiguration) o;
        return Objects.equals(fixedBackoff, that.fixedBackoff);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixedBackoff);
    }

    @Override
    public String toString() {
        return "FixedDelayBackoffStrategyConfiguration{" +
                "fixedBackoff=" + fixedBackoff +
                '}';
    }


    /**
     * Builder for {@link FixedDelayBackoffStrategyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Duration DEFAULT_FIXED_BACKOFF = Duration.ofMillis(1);

        private Duration fixedBackoff = DEFAULT_FIXED_BACKOFF;

        private Builder() {
        }

        public Builder fixedBackoff(final Duration fixedBackoff) {
            this.fixedBackoff = fixedBackoff;
            return this;
        }

        public FixedDelayBackoffStrategyConfiguration build() {
            return new FixedDelayBackoffStrategyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
