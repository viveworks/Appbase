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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link NetHttpTransport}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = NetHttpTransportConfiguration.Builder.class)
public final class NetHttpTransportConfiguration {
    private final Boolean keepAlive;
    private final Integer maxConnections;

    private NetHttpTransportConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        keepAlive = builder.keepAlive;
        maxConnections = builder.maxConnections;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NetHttpTransportConfiguration that = (NetHttpTransportConfiguration) o;
        return Objects.equals(keepAlive, that.keepAlive) &&
                Objects.equals(maxConnections, that.maxConnections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keepAlive, maxConnections);
    }

    @Override
    public String toString() {
        return "NetHttpTransportConfiguration{" +
                "keepAlive=" + keepAlive +
                ", maxConnections=" + maxConnections +
                '}';
    }


    /**
     * Builder for {@link NetHttpTransportConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Boolean DEFAULT_KEEP_ALIVE = true;
        private static final Integer DEFAULT_MAX_CONNECTIONS = 64;


        private Boolean keepAlive = DEFAULT_KEEP_ALIVE;
        private Integer maxConnections = DEFAULT_MAX_CONNECTIONS;


        private Builder() {
        }

        public Builder keepAlive(final Boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder maxConnections(final Integer maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public NetHttpTransportConfiguration build() {
            return new NetHttpTransportConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
