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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link org.apache.http.HttpHost}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = HttpHostConfiguration.Builder.class)
public final class HttpHostConfiguration {
    private final String hostname;
    private final int port;
    private final String scheme;

    private HttpHostConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.hostname, "builder.hostname");
        AppbaseUtil.positiveOrZero(builder.port, "builder.port");
        AppbaseUtil.notNull(builder.scheme, "builder.scheme");

        hostname = builder.hostname;
        port = builder.port;
        scheme = builder.scheme;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HttpHostConfiguration that = (HttpHostConfiguration) o;
        return port == that.port &&
                Objects.equals(hostname, that.hostname) &&
                Objects.equals(scheme, that.scheme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port, scheme);
    }

    @Override
    public String toString() {
        return "HttpHostConfiguration{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                ", scheme='" + scheme + '\'' +
                '}';
    }


    /**
     * Builder for {@link HttpHostConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_HOSTNAME = "elasticsearch.local";
        private static final int DEFAULT_PORT = 9200;
        private static final String DEFAULT_SCHEME = "http";

        private String hostname = DEFAULT_HOSTNAME;
        private int port = DEFAULT_PORT;
        private String scheme = DEFAULT_SCHEME;


        private Builder() {
        }

        public Builder hostname(final String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder scheme(final String scheme) {
            this.scheme = scheme;
            return this;
        }

        public HttpHostConfiguration build() {
            return new HttpHostConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
