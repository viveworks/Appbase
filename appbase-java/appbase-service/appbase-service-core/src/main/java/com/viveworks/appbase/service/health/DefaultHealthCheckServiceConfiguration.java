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

package com.viveworks.appbase.service.health;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultHealthCheckServiceConfiguration.Builder.class)
public class DefaultHealthCheckServiceConfiguration implements AppbaseConfiguration {
    private final TimeUnitConfiguration cacheSchedulerDelayConfiguration;

    private final boolean cacheEnabled;

    private final TimeUnitConfiguration staleAfterConfiguration;

    private DefaultHealthCheckServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(
                builder.cacheSchedulerDelayConfiguration,
                "builder.cacheSchedulerDelayConfiguration");

        cacheSchedulerDelayConfiguration = builder.cacheSchedulerDelayConfiguration;
        cacheEnabled = builder.cacheEnabled;
        staleAfterConfiguration = builder.staleAfterConfiguration != null
                ? builder.staleAfterConfiguration
                : TimeUnitConfiguration.builder().enabled(true).time(90).timeUnit(TimeUnit.SECONDS).build();
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TimeUnitConfiguration getCacheSchedulerDelayConfiguration() {
        return cacheSchedulerDelayConfiguration;
    }

    /**
     * Governs only the explicit {@link HealthCheckService#checkHealth(boolean)} path: when
     * {@code true} (the default), {@code checkHealth(false)} serves the scheduler-maintained
     * snapshot; when {@code false}, {@code checkHealth(false)} re-runs the checkers inline on the
     * calling thread instead. It does not affect {@link HealthCheckService#checkHealth()} or
     * {@link HealthCheckService#checkHealthSnapshot()}, which always serve the snapshot regardless
     * of this flag, nor does it stop the background scheduler, which keeps refreshing the snapshot
     * either way.
     *
     * @return whether {@code checkHealth(false)} serves the snapshot instead of running inline
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * A snapshot older than this reports unhealthy rather than serving stale data. Guards against
     * a wedged scheduler silently pinning health to a long-stale "healthy".
     *
     * @return the staleness threshold
     */
    public TimeUnitConfiguration getStaleAfterConfiguration() {
        return staleAfterConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultHealthCheckServiceConfiguration that = (DefaultHealthCheckServiceConfiguration) o;
        return cacheEnabled == that.cacheEnabled &&
                Objects.equals(cacheSchedulerDelayConfiguration, that.cacheSchedulerDelayConfiguration) &&
                Objects.equals(staleAfterConfiguration, that.staleAfterConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheSchedulerDelayConfiguration, cacheEnabled, staleAfterConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultHealthCheckServiceConfiguration{" +
                "cacheSchedulerDelayConfiguration=" + cacheSchedulerDelayConfiguration +
                ", cacheEnabled=" + cacheEnabled +
                ", staleAfterConfiguration=" + staleAfterConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultHealthCheckServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TimeUnitConfiguration DEFAULT_CACHE_SCHEDULER_DELAY_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .time(1)
                .timeUnit(TimeUnit.MINUTES)
                .build();

        private TimeUnitConfiguration cacheSchedulerDelayConfiguration = DEFAULT_CACHE_SCHEDULER_DELAY_CONFIGURATION;

        private boolean cacheEnabled = true;

        private TimeUnitConfiguration staleAfterConfiguration;

        private Builder() {
        }

        public Builder cacheSchedulerDelayConfiguration(
                final TimeUnitConfiguration cacheSchedulerDelayConfiguration)
        {
            this.cacheSchedulerDelayConfiguration = cacheSchedulerDelayConfiguration;
            return this;
        }

        public Builder cacheEnabled(final boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
            return this;
        }

        public Builder staleAfterConfiguration(final TimeUnitConfiguration staleAfterConfiguration) {
            this.staleAfterConfiguration = staleAfterConfiguration;
            return this;
        }

        public DefaultHealthCheckServiceConfiguration build() {
            return new DefaultHealthCheckServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
