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

package com.viveworks.appbase.core.util.net;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for a host:port combination.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = HostPortConfiguration.Builder.class)
public final class HostPortConfiguration {
    private final String host;
    private final int port;

    private HostPortConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.host, "builder.host");
        AppbaseUtil.positiveOrZero(builder.port, "builder.port");

        host = builder.host;
        port = builder.port;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HostPortConfiguration that = (HostPortConfiguration) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "HostPortConfiguration{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    /**
     * Builder for {@link HostPortConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_HOST = "localhost";
        private static final int DEFAULT_PORT = 6060;

        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;

        private Builder() {
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public HostPortConfiguration build() {
            return new HostPortConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
