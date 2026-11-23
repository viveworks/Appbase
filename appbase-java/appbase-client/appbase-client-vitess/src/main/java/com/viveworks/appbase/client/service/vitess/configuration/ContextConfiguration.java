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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vitess.client.Context;

import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for {@link Context}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ContextConfiguration.Builder.class)
public final class ContextConfiguration {
    private final Duration deadlineDuration;
    private final CallerIdConfiguration callerIdConfiguration;

    private ContextConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        deadlineDuration = builder.deadlineDuration;
        callerIdConfiguration = builder.callerIdConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Duration getDeadlineDuration() {
        return deadlineDuration;
    }

    public CallerIdConfiguration getCallerIdConfiguration() {
        return callerIdConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ContextConfiguration that = (ContextConfiguration) o;
        return Objects.equals(deadlineDuration, that.deadlineDuration) &&
                Objects.equals(callerIdConfiguration, that.callerIdConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deadlineDuration, callerIdConfiguration);
    }

    @Override
    public String toString() {
        return "ContextConfiguration{" +
                "deadline=" + deadlineDuration +
                ", callerIdConfiguration=" + callerIdConfiguration +
                '}';
    }

    /**
     * Builder for {@link ContextConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(10);

        private Duration deadlineDuration = DEFAULT_DEADLINE;
        private CallerIdConfiguration callerIdConfiguration;

        private Builder() {
        }

        public Builder deadlineDuration(final Duration deadlineDuration) {
            this.deadlineDuration = deadlineDuration;
            return this;
        }

        public Builder callerIdConfiguration(final CallerIdConfiguration callerIdConfiguration) {
            this.callerIdConfiguration = callerIdConfiguration;
            return this;
        }

        public ContextConfiguration build() {
            return new ContextConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
