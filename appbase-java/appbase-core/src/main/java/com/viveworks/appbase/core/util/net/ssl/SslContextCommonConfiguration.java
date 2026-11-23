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

package com.viveworks.appbase.core.util.net.ssl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.handler.codec.http2.Http2SecurityUtil;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Set;

/**
 * Common SSL / TLS configuration for client and server.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SslContextCommonConfiguration.Builder.class)
public final class SslContextCommonConfiguration {
    private final SslImplementation sslImplementation;
    private final SslKeyStore sslKeyStore;
    private final SslTrustManagerConfiguration sslTrustManagerConfiguration;
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> ciphers;
    private final SslApplicationProtocolConfiguration sslApplicationProtocolConfiguration;
    private final int sessionCacheSize;
    private final long sessionTimeoutSeconds;
    private final SslClientAuth sslClientAuth;
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> protocols;
    private final boolean startTls;
    private final boolean enableOcsp;

    private SslContextCommonConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.sessionCacheSize, "builder.sessionCacheSize");
        AppbaseUtil.positiveOrZero(builder.sessionTimeoutSeconds, "builder.sessionTimeoutSeconds");

        sslImplementation = builder.sslImplementation;
        sslKeyStore = builder.sslKeyStore;
        sslTrustManagerConfiguration = builder.sslTrustManagerConfiguration;
        ciphers = builder.ciphers;
        sslApplicationProtocolConfiguration = builder.sslApplicationProtocolConfiguration;
        sessionCacheSize = builder.sessionCacheSize;
        sessionTimeoutSeconds = builder.sessionTimeoutSeconds;
        sslClientAuth = builder.sslClientAuth;
        protocols = builder.protocols;
        startTls = builder.startTls;
        enableOcsp = builder.enableOcsp;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public SslImplementation getSslImplementation() {
        return sslImplementation;
    }

    public SslKeyStore getSslKeyStore() {
        return sslKeyStore;
    }

    public SslTrustManagerConfiguration getSslTrustManagerConfiguration() {
        return sslTrustManagerConfiguration;
    }

    public Set<String> getCiphers() {
        return ciphers;
    }

    public SslApplicationProtocolConfiguration getSslApplicationProtocolConfiguration() {
        return sslApplicationProtocolConfiguration;
    }

    public int getSessionCacheSize() {
        return sessionCacheSize;
    }

    public long getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public SslClientAuth getSslClientAuth() {
        return sslClientAuth;
    }

    public Set<String> getProtocols() {
        return protocols;
    }

    public boolean isStartTls() {
        return startTls;
    }

    public boolean isEnableOcsp() {
        return enableOcsp;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SslContextCommonConfiguration that = (SslContextCommonConfiguration) o;
        return sessionCacheSize == that.sessionCacheSize &&
                sessionTimeoutSeconds == that.sessionTimeoutSeconds &&
                startTls == that.startTls &&
                enableOcsp == that.enableOcsp &&
                sslImplementation == that.sslImplementation &&
                sslKeyStore == that.sslKeyStore &&
                Objects.equals(sslTrustManagerConfiguration, that.sslTrustManagerConfiguration) &&
                Objects.equals(ciphers, that.ciphers) &&
                Objects.equals(sslApplicationProtocolConfiguration, that.sslApplicationProtocolConfiguration) &&
                sslClientAuth == that.sslClientAuth &&
                Objects.equals(protocols, that.protocols);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sslImplementation,
                sslKeyStore,
                sslTrustManagerConfiguration,
                ciphers,
                sslApplicationProtocolConfiguration,
                sessionCacheSize,
                sessionTimeoutSeconds,
                sslClientAuth,
                protocols,
                startTls,
                enableOcsp);
    }

    @Override
    public String toString() {
        return "SslContextCommonConfiguration{" +
                "sslImplementation=" + sslImplementation +
                ", sslKeyStore=" + sslKeyStore +
                ", sslTrustManagerConfiguration=" + sslTrustManagerConfiguration +
                ", ciphers=" + ciphers +
                ", sslApplicationProtocolConfiguration=" + sslApplicationProtocolConfiguration +
                ", sessionCacheSize=" + sessionCacheSize +
                ", sessionTimeoutSeconds=" + sessionTimeoutSeconds +
                ", sslClientAuth=" + sslClientAuth +
                ", protocols=" + protocols +
                ", startTls=" + startTls +
                ", enableOcsp=" + enableOcsp +
                '}';
    }

    /**
     * Builder for {@link SslContextCommonConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        public static final SslImplementation DEFAULT_SSL_IMPLEMENTATION = SslImplementation.OPENSSL_POOLED;
        private static final SslKeyStore DEFAULT_SSL_KEY_STORE = SslKeyStore.JKS;
        private static final SslTrustManagerConfiguration DEFAULT_SSL_TRUST_MANAGER_CONFIGURATION =
                SslTrustManagerConfiguration.builder().build();
        private static final Set<String> DEFAULT_CIPHERS =
                Set.copyOf(Http2SecurityUtil.CIPHERS);
        private static final SslApplicationProtocolConfiguration DEFAULT_SSL_APPLICATION_PROTOCOL_CONFIGURATION =
                SslApplicationProtocolConfiguration.builder().build();
        private static final int DEFAULT_SESSION_CACHE_SIZE = 0;
        private static final long DEFAULT_SESSION_TIMEOUT_SECONDS = 0;
        private static final SslClientAuth DEFAULT_SSL_CLIENT_AUTH = SslClientAuth.NONE;
        private static final Set<String> DEFAULT_PROTOCOLS =
                Set.of("SSLv2Hello", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2");
        private static final boolean DEFAULT_STARTTLS = false;
        private static final boolean DEFAULT_ENABLE_OCSP = false;

        private SslImplementation sslImplementation = DEFAULT_SSL_IMPLEMENTATION;
        private SslKeyStore sslKeyStore = DEFAULT_SSL_KEY_STORE;
        private SslTrustManagerConfiguration sslTrustManagerConfiguration = DEFAULT_SSL_TRUST_MANAGER_CONFIGURATION;
        @JsonSerialize(contentAs = String.class)
        @JsonDeserialize(contentAs = String.class)
        private Set<String> ciphers = DEFAULT_CIPHERS;
        private SslApplicationProtocolConfiguration sslApplicationProtocolConfiguration =
                DEFAULT_SSL_APPLICATION_PROTOCOL_CONFIGURATION;
        private int sessionCacheSize = DEFAULT_SESSION_CACHE_SIZE;
        private long sessionTimeoutSeconds = DEFAULT_SESSION_TIMEOUT_SECONDS;
        private SslClientAuth sslClientAuth = DEFAULT_SSL_CLIENT_AUTH;
        @JsonSerialize(contentAs = String.class)
        @JsonDeserialize(contentAs = String.class)
        private Set<String> protocols = DEFAULT_PROTOCOLS;
        private boolean startTls = DEFAULT_STARTTLS;
        private boolean enableOcsp = DEFAULT_ENABLE_OCSP;

        private Builder() {
        }

        public Builder sslKeyStore(final SslKeyStore sslKeyStore) {
            this.sslKeyStore = sslKeyStore;
            return this;
        }

        public Builder sslImplementation(final SslImplementation sslImplementation) {
            this.sslImplementation = sslImplementation;
            return this;
        }

        public Builder sslTrustManagerConfiguration(final SslTrustManagerConfiguration sslTrustManagerConfiguration) {
            this.sslTrustManagerConfiguration = sslTrustManagerConfiguration;
            return this;
        }

        public Builder ciphers(
                @JsonSerialize(contentAs = String.class)
                @JsonDeserialize(contentAs = String.class)
                final Set<String> ciphers)
        {
            this.ciphers = ciphers;
            return this;
        }

        public Builder sslApplicationProtocolConfig(
                final SslApplicationProtocolConfiguration sslApplicationProtocolConfiguration)
        {
            this.sslApplicationProtocolConfiguration = sslApplicationProtocolConfiguration;
            return this;
        }

        public Builder sessionCacheSize(final int sessionCacheSize) {
            this.sessionCacheSize = sessionCacheSize;
            return this;
        }

        public Builder sessionTimeoutSeconds(final long sessionTimeoutSeconds) {
            this.sessionTimeoutSeconds = sessionTimeoutSeconds;
            return this;
        }

        public Builder sslClientAuth(final SslClientAuth sslClientAuth) {
            this.sslClientAuth = sslClientAuth;
            return this;
        }

        public Builder protocols(
                @JsonSerialize(contentAs = String.class)
                @JsonDeserialize(contentAs = String.class)
                final Set<String> protocols)
        {
            this.protocols = protocols;
            return this;
        }

        public Builder startTls(final boolean startTls) {
            this.startTls = startTls;
            return this;
        }

        public Builder enableOcsp(final boolean enableOcsp) {
            this.enableOcsp = enableOcsp;
            return this;
        }

        public SslContextCommonConfiguration build() {
            return new SslContextCommonConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
