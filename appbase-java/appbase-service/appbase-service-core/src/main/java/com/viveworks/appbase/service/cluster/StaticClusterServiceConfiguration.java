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

package com.viveworks.appbase.service.cluster;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Immutable cluster configuration whose builder defaults the home data center to {@code dev}.
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = StaticClusterServiceConfiguration.Builder.class)
public class StaticClusterServiceConfiguration implements AppbaseConfiguration {
    private final String homeDataCenter;

    private StaticClusterServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.homeDataCenter, "builder.homeDataCenter");

        homeDataCenter = builder.homeDataCenter;
    }

    /** @return a builder initialized with the {@code dev} data-center default */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the non-null configured home data-center name */
    public String getHomeDataCenter() {
        return homeDataCenter;
    }

    /** Compares configurations by home data-center name. */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StaticClusterServiceConfiguration that = (StaticClusterServiceConfiguration) o;
        return Objects.equals(homeDataCenter, that.homeDataCenter);
    }

    /** @return hash derived from the home data-center name */
    @Override
    public int hashCode() {
        return Objects.hash(homeDataCenter);
    }

    /** @return diagnostic representation containing the home data center */
    @Override
    public String toString() {
        return "StaticClusterServiceConfiguration{" +
                "homeDataCenter='" + homeDataCenter + '\'' +
                '}';
    }


    // CheckStyle: Builder start
/**
     * Builder for {@link StaticClusterServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_HOME_DATACENTER = "dev";

        private String homeDataCenter = DEFAULT_HOME_DATACENTER;

        private Builder() {
        }

        /**
         * Overrides the default home data-center name.
         * @param homeDataCenter home data-center name
         * @return this builder
         */
        public Builder homeDataCenter(final String homeDataCenter) {
            this.homeDataCenter = homeDataCenter;
            return this;
        }

        /**
         * Builds an immutable configuration, rejecting a null home data center.
         * @return a new immutable configuration
         */
        public StaticClusterServiceConfiguration build() {
            return new StaticClusterServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
