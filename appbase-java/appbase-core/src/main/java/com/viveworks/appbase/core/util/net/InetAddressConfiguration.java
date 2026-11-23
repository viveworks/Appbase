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

package com.viveworks.appbase.core.util.net;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link java.net.InetAddress}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = InetAddressConfiguration.Builder.class)
public final class InetAddressConfiguration {
    private final InetAddressVersion inetAddressVersion;
    private final String host;

    private InetAddressConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.inetAddressVersion, "builder.inetAddressVersion");
        AppbaseUtil.notBlank(builder.host, "builder.host");

        inetAddressVersion = builder.inetAddressVersion;
        host = builder.host;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InetAddressVersion getInetAddressVersion() {
        return inetAddressVersion;
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InetAddressConfiguration that = (InetAddressConfiguration) o;
        return inetAddressVersion == that.inetAddressVersion &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inetAddressVersion, host);
    }

    @Override
    public String toString() {
        return "InetAddressConfiguration{" +
                "inetAddressVersion=" + inetAddressVersion +
                ", host='" + host + '\'' +
                '}';
    }

    /**
     * Builder for {@link InetAddressConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final InetAddressVersion DEFAULT_INET_ADDRESS_VERSION = InetAddressVersion.IPV4;
        private static final String DEFAULT_HOST = "localhost";

        private InetAddressVersion inetAddressVersion = DEFAULT_INET_ADDRESS_VERSION;
        private String host = DEFAULT_HOST;

        private Builder() {
        }

        public Builder inetAddressVersion(final InetAddressVersion inetAddressVersion) {
            this.inetAddressVersion = inetAddressVersion;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public InetAddressConfiguration build() {
            return new InetAddressConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
