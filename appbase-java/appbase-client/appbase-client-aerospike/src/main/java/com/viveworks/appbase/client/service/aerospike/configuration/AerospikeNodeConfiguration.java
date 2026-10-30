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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.aerospike.client.Host;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * JSON serializable / de-serializable representation of {@link Host}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AerospikeNodeConfiguration.Builder.class)
public final class AerospikeNodeConfiguration {
    private final String hostnameOrIp;

    private final int port;

    @Nullable
    private final String tlsName;



    private AerospikeNodeConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.hostnameOrIp, "builder.hostnameOrIp");
        AppbaseUtil.positiveOrZero(builder.port, "builder.port");

        hostnameOrIp = builder.hostnameOrIp;
        port = builder.port;
        tlsName = builder.tlsName;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getHostnameOrIp() {
        return hostnameOrIp;
    }

    public int getPort() {
        return port;
    }

    @Nullable
    public String getTlsName() {
        return tlsName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AerospikeNodeConfiguration that = (AerospikeNodeConfiguration) o;
        return port == that.port &&
                Objects.equals(hostnameOrIp, that.hostnameOrIp) &&
                Objects.equals(tlsName, that.tlsName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostnameOrIp, port, tlsName);
    }

    @Override
    public String toString() {
        return "AerospikeNodeConfiguration{" +
                "hostnameOrIp='" + hostnameOrIp + '\'' +
                ", port=" + port +
                ", tlsName='" + tlsName + '\'' +
                '}';
    }


    /**
     * Builder for {@link AerospikeNodeConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String hostnameOrIp;

        private int port;

        @Nullable
        private String tlsName;



        private Builder() {
        }

        public Builder hostnameOrIp(final String hostnameOrIp) {
            this.hostnameOrIp = hostnameOrIp;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder tlsName(@Nullable final String tlsName) {
            this.tlsName = tlsName;
            return this;
        }

        public AerospikeNodeConfiguration build() {
            return new AerospikeNodeConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
