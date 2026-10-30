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

package com.viveworks.appbase.core.util.net.ssl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Wrapper configuration for {@link io.netty.handler.ssl.ApplicationProtocolConfig}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SslApplicationProtocolConfiguration.Builder.class)
public final class SslApplicationProtocolConfiguration {
    private final SslApplicationProtocolNegotiation sslApplicationProtocolNegotiation;
    private final SslSelectorFailureBehavior sslSelectorFailureBehavior;
    private final SslSelectedListenerFailureBehavior sslSelectedListenerFailureBehavior;
    @JsonSerialize(contentAs = SslApplicationProtocol.class)
    @JsonDeserialize(contentAs = SslApplicationProtocol.class)
    private final Set<SslApplicationProtocol> sslApplicationProtocols;

    private SslApplicationProtocolConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        sslApplicationProtocolNegotiation = builder.sslApplicationProtocolNegotiation;
        sslSelectorFailureBehavior = builder.sslSelectorFailureBehavior;
        sslSelectedListenerFailureBehavior = builder.sslSelectedListenerFailureBehavior;
        sslApplicationProtocols = builder.sslApplicationProtocols;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public SslApplicationProtocolNegotiation getSslApplicationProtocolNegotiation() {
        return sslApplicationProtocolNegotiation;
    }

    public SslSelectorFailureBehavior getSslSelectorFailureBehavior() {
        return sslSelectorFailureBehavior;
    }

    public SslSelectedListenerFailureBehavior getSslSelectedListenerFailureBehavior() {
        return sslSelectedListenerFailureBehavior;
    }

    public Set<SslApplicationProtocol> getSslApplicationProtocols() {
        return sslApplicationProtocols;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SslApplicationProtocolConfiguration that = (SslApplicationProtocolConfiguration) o;
        return sslApplicationProtocolNegotiation == that.sslApplicationProtocolNegotiation &&
                sslSelectorFailureBehavior == that.sslSelectorFailureBehavior &&
                sslSelectedListenerFailureBehavior == that.sslSelectedListenerFailureBehavior &&
                Objects.equals(sslApplicationProtocols, that.sslApplicationProtocols);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sslApplicationProtocolNegotiation,
                sslSelectorFailureBehavior,
                sslSelectedListenerFailureBehavior,
                sslApplicationProtocols);
    }

    @Override
    public String toString() {
        return "SslApplicationProtocolConfiguration{" +
                "sslApplicationProtocolNegotiation=" + sslApplicationProtocolNegotiation +
                "sslSelectorFailureBehavior=" + sslSelectorFailureBehavior +
                "sslSelectedListenerFailureBehavior=" + sslSelectedListenerFailureBehavior +
                ", sslApplicationProtocols=" + sslApplicationProtocols +
                '}';
    }

    /**
     * Builder for {@link SslApplicationProtocolConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static  final SslApplicationProtocolNegotiation DEFAULT_SSL_APPLICATION_PROTOCOL_NEGOTIATION =
                SslApplicationProtocolNegotiation.ALPN;
        private static final SslSelectorFailureBehavior DEFAULT_SSL_SELECTOR_FAILURE_BEHAVIOR =
                SslSelectorFailureBehavior.NO_ADVERTISE;
        private static final SslSelectedListenerFailureBehavior DEFAULT_SSL_SELECTED_LISTENER_FAILURE_BEHAVIOR =
                SslSelectedListenerFailureBehavior.ACCEPT;
        private static final Set<SslApplicationProtocol> DEFAULT_SSL_APPLICATION_PROTOCOLS =
                Set.of(SslApplicationProtocol.HTTP_2, SslApplicationProtocol.HTTP_1_1);

        private SslApplicationProtocolNegotiation sslApplicationProtocolNegotiation =
                DEFAULT_SSL_APPLICATION_PROTOCOL_NEGOTIATION;
        private SslSelectorFailureBehavior sslSelectorFailureBehavior =
                DEFAULT_SSL_SELECTOR_FAILURE_BEHAVIOR;
        private SslSelectedListenerFailureBehavior sslSelectedListenerFailureBehavior =
                DEFAULT_SSL_SELECTED_LISTENER_FAILURE_BEHAVIOR;
        @JsonSerialize(contentAs = SslApplicationProtocol.class)
        @JsonDeserialize(contentAs = SslApplicationProtocol.class)
        private Set<SslApplicationProtocol> sslApplicationProtocols = DEFAULT_SSL_APPLICATION_PROTOCOLS;

        private Builder() {
        }

        public Builder sslApplicationProtocolNegotiation(
                final SslApplicationProtocolNegotiation sslApplicationProtocolNegotiation)
        {
            this.sslApplicationProtocolNegotiation = sslApplicationProtocolNegotiation;
            return this;
        }

        public Builder sslSelectorFailureBehavior(final SslSelectorFailureBehavior sslSelectorFailureBehavior) {
            this.sslSelectorFailureBehavior = sslSelectorFailureBehavior;
            return this;
        }

        public Builder sslSelectedListenerFailureBehavior(
                final SslSelectedListenerFailureBehavior sslSelectedListenerFailureBehavior)
        {
            this.sslSelectedListenerFailureBehavior = sslSelectedListenerFailureBehavior;
            return this;
        }

        public Builder sslApplicationProtocols(final Set<SslApplicationProtocol> sslApplicationProtocols) {
            this.sslApplicationProtocols = sslApplicationProtocols;
            return this;
        }

        public SslApplicationProtocolConfiguration build() {
            return new SslApplicationProtocolConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
