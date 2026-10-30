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

package com.viveworks.appbase.client.service.cassandra.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@code localdatacenter} option.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = LocalDatacenterConfiguration.Builder.class)
public final class LocalDatacenterConfiguration {
    private final boolean enabled;
    private final String localDatacenter;

    private LocalDatacenterConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.enabled) {
            AppbaseUtil.notBlank(builder.localDatacenter, "builder.localDatacenter");
        }

        enabled = builder.enabled;
        localDatacenter = builder.localDatacenter;
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

    public String getLocalDatacenter() {
        return localDatacenter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocalDatacenterConfiguration that = (LocalDatacenterConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(localDatacenter, that.localDatacenter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, localDatacenter);
    }

    @Override
    public String toString() {
        return "LocalDatacenterConfiguration{" +
                "enabled=" + enabled +
                ", localDatacenter='" + localDatacenter + '\'' +
                '}';
    }

    /**
     * Builder for {@link LocalDatacenterConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final String DEFAULT_LOCAL_DATA_CENTER = "datacenter1";

        private boolean enabled = DEFAULT_ENABLED;
        private String localDatacenter = DEFAULT_LOCAL_DATA_CENTER;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder localDatacenter(final String localDatacenter) {
            this.localDatacenter = localDatacenter;
            return this;
        }

        public LocalDatacenterConfiguration build() {
            return new LocalDatacenterConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
