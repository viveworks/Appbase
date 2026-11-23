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

package com.viveworks.appbase.client.cloud.aws.httpclient.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration used to generate {@link ProxyConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AwsSdkProxyConfiguration.Builder.class)
public final class AwsSdkProxyConfiguration {
    private final String scheme;
    private final String host;
    private final int port;
    private final Set<String> nonProxyHosts;


    private AwsSdkProxyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.scheme, "builder.scheme");
        AppbaseUtil.positiveOrZero(builder.port, "builder.port");

        scheme = builder.scheme;
        host = builder.host;
        port = builder.port;
        nonProxyHosts = builder.nonProxyHosts;
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
     * Returns the configured scheme.
     *
     * @return the configured scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Returns the configured host.
     *
     * @return the configured host
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the configured port.
     *
     * @return the configured port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the configured non proxy hosts.
     *
     * @return the configured non proxy hosts
     */
    public Set<String> getNonProxyHosts() {
        return nonProxyHosts;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AwsSdkProxyConfiguration that = (AwsSdkProxyConfiguration) o;
        return port == that.port &&
                Objects.equals(scheme, that.scheme) &&
                Objects.equals(host, that.host) &&
                Objects.equals(nonProxyHosts, that.nonProxyHosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, host, port, nonProxyHosts);
    }

    @Override
    public String toString() {
        return "AwsSdkProxyConfiguration{" +
                "scheme='" + scheme + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", nonProxyHosts=" + nonProxyHosts +
                '}';
    }


    /**
     * Builder for {@link AwsSdkProxyConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String scheme;
        private String host;
        private int port;
        private Set<String> nonProxyHosts;


        private Builder() {
        }

        public Builder scheme(final String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder nonProxyHosts(final Set<String> nonProxyHosts) {
            this.nonProxyHosts = nonProxyHosts;
            return this;
        }

        public AwsSdkProxyConfiguration build() {
            return new AwsSdkProxyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
