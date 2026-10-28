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

package com.viveworks.appbase.client.core.clientkey.clusterid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link ClusterIdValue}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ClusterIdValueConfiguration.Builder.class)
public final class ClusterIdValueConfiguration {
    public static final ClusterIdValueConfiguration PRIMARY = ClusterIdValueConfiguration
            .builder()
            .clusterId("primary")
            .build();

    private final String clusterId;

    private ClusterIdValueConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        clusterId = builder.clusterId;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /** @return configured cluster identifier; the builder defaults it to {@code primary} */
    public String getClusterId() {
        return clusterId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ClusterIdValueConfiguration that = (ClusterIdValueConfiguration) o;
        return Objects.equals(clusterId, that.clusterId);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(clusterId);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ClusterIdValueConfiguration{" +
                "clusterId='" + clusterId + '\'' +
                '}';
    }

    /**
     * Builder for the configured cluster identifier text.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_CLUSTER_ID = "primary";

        private String clusterId = DEFAULT_CLUSTER_ID;

        private Builder() {
        }

        public Builder clusterId(final String clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public ClusterIdValueConfiguration build() {
            return new ClusterIdValueConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
