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

package com.viveworks.appbase.client.service.redis.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for a single Redis endpoint managed by
 * {@link com.viveworks.appbase.client.service.redis.DefaultRedisClientIntegrationService}.
 *
 * <p>Exposes the full set of Lettuce {@code RedisURI}, {@code ClientOptions},
 * {@code SocketOptions}, and {@code SslOptions} knobs so every aspect of the connection
 * can be tuned via YAML. Host and port are represented as a {@link HostPortConfiguration}.
 * A {@code null} {@code database} means "let Lettuce use its default (index 0)"; an
 * explicit value triggers a {@code SELECT} on connect.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultRedisClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultRedisClientIntegrationServiceConfiguration
        implements RedisClientIntegrationServiceConfiguration
{
    /** TCP-level connection establishment timeout (maps to {@code SocketOptions.connectTimeout}). */
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 5_000L;
    /** Redis command timeout (maps to {@code RedisURI.withTimeout}). */
    private static final long DEFAULT_COMMAND_TIMEOUT_MS = 60_000L;
    /** Default for {@link #isCommandLatencyMetricsEnabled()}. */
    private static final boolean DEFAULT_COMMAND_LATENCY_METRICS_ENABLED = true;

    /** Key used to look up this integration service. Defaults to {@code TextValueConfiguration.PRIMARY}. */
    private final TextValueConfiguration textValueKeyConfiguration;

    // ── Connection ────────────────────────────────────────────────────────
    private final HostPortConfiguration hostPortConfiguration;
    /** Redis SELECT database index (0–15). {@code null} = use Lettuce default (0). */
    @Nullable private final Integer database;
    /** SETNAME sent to Redis on connect ({@code CLIENT SETNAME}). */
    @Nullable private final String clientName;

    // ── Auth ──────────────────────────────────────────────────────────────
    /** ACL username (Lettuce 6+). {@code null} = no ACL auth. */
    @Nullable private final String username;
    /** Redis password. {@code null} = no password auth. */
    @Nullable private final String password;

    // ── TLS/SSL ───────────────────────────────────────────────────────────
    /** Enable TLS/SSL for the connection. */
    private final boolean ssl;
    /** Verify the server's TLS certificate. Only relevant when {@code ssl=true}. */
    private final boolean sslVerifyPeer;
    /** Use STARTTLS upgrade instead of direct TLS. Only relevant when {@code ssl=true}. */
    private final boolean startTls;

    // ── Timeouts ──────────────────────────────────────────────────────────
    /** TCP connection timeout in milliseconds (maps to {@code SocketOptions.connectTimeout}). */
    private final long connectTimeoutMs;
    /** Per-command timeout in milliseconds (maps to {@code RedisURI.withTimeout}). */
    private final long commandTimeoutMs;

    // ── Behaviour (ClientOptions) ─────────────────────────────────────────
    /** Reconnect automatically after a dropped connection. */
    private final boolean autoReconnect;
    /** Send a PING before activating a pooled connection. */
    private final boolean pingBeforeActivate;
    /** Max number of commands that may be queued while the connection is not ready. */
    private final int requestQueueSize;

    // ── Socket options ────────────────────────────────────────────────────
    /** Disable Nagle's algorithm (TCP_NODELAY). Reduces latency for small commands. */
    private final boolean tcpNoDelay;
    /** Enable TCP keep-alive probes (SO_KEEPALIVE). */
    private final boolean tcpKeepAlive;

    // ── Advanced behaviour ────────────────────────────────────────────────
    /**
     * Command behaviour while the connection is not ready.
     * {@code null} = use Lettuce default ({@code REJECT_COMMANDS}).
     */
    @Nullable private final DisconnectedBehavior disconnectedBehavior;

    // ── Mutual TLS / custom CA (SslOptions) ──────────────────────────────
    /** SSL/TLS provider. {@code null} = use Lettuce default ({@code JDK}). */
    @Nullable private final SslProvider sslProvider;
    /** Filesystem path to the keystore (mutual TLS). {@code null} = not set. */
    @Nullable private final String sslKeystore;
    /** Password for the keystore. {@code null} = no password. */
    @Nullable private final String sslKeystorePassword;
    /** Filesystem path to the truststore (custom CA). {@code null} = not set. */
    @Nullable private final String sslTruststore;
    /** Password for the truststore. {@code null} = no password. */
    @Nullable private final String sslTruststorePassword;

    // ── Granular TCP keepalive ────────────────────────────────────────────
    /** Number of keepalive probes before the connection is considered dead. {@code null} = Lettuce default (9). */
    @Nullable private final Integer keepAliveCount;
    /** Idle time before keepalive probes start (milliseconds). {@code null} = Lettuce default (7 200 000 ms). */
    @Nullable private final Long keepAliveIdle;
    /** Interval between keepalive probes (milliseconds). {@code null} = Lettuce default (75 000 ms). */
    @Nullable private final Long keepAliveInterval;

    // ── Protocol ──────────────────────────────────────────────────────────
    /** Redis Serialization Protocol version. {@code null} = let Lettuce negotiate. */
    @Nullable private final ProtocolVersion protocolVersion;

    // ── Metrics ───────────────────────────────────────────────────────────
    /** Route Lettuce's own per-command latency metrics into Appbase's Micrometer composite (default true). */
    private final boolean commandLatencyMetricsEnabled;


    private DefaultRedisClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder.hostPortConfiguration, "builder.hostPortConfiguration");

        this.textValueKeyConfiguration = builder.textValueKeyConfiguration;
        this.hostPortConfiguration = builder.hostPortConfiguration;
        this.database = builder.database;
        this.clientName = builder.clientName;
        this.username = builder.username;
        this.password = builder.password;
        this.ssl = builder.ssl;
        this.sslVerifyPeer = builder.sslVerifyPeer;
        this.startTls = builder.startTls;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.commandTimeoutMs = builder.commandTimeoutMs;
        this.autoReconnect = builder.autoReconnect;
        this.pingBeforeActivate = builder.pingBeforeActivate;
        this.requestQueueSize = builder.requestQueueSize;
        this.tcpNoDelay = builder.tcpNoDelay;
        this.tcpKeepAlive = builder.tcpKeepAlive;
        this.disconnectedBehavior = builder.disconnectedBehavior;
        this.sslProvider = builder.sslProvider;
        this.sslKeystore = builder.sslKeystore;
        this.sslKeystorePassword = builder.sslKeystorePassword;
        this.sslTruststore = builder.sslTruststore;
        this.sslTruststorePassword = builder.sslTruststorePassword;
        this.keepAliveCount = builder.keepAliveCount;
        this.keepAliveIdle = builder.keepAliveIdle;
        this.keepAliveInterval = builder.keepAliveInterval;
        this.protocolVersion = builder.protocolVersion;
        this.commandLatencyMetricsEnabled = builder.commandLatencyMetricsEnabled;
    }

    /**
     * Returns a new builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns the client key configuration (used to look up this integration service). */
    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    /** Redis server host and port. */
    public HostPortConfiguration getHostPortConfiguration() {
        return hostPortConfiguration;
    }

    /**
     * Redis SELECT database index (0–15).
     * {@code null} means the index was not explicitly configured; Lettuce defaults to 0.
     */
    @Nullable
    public Integer getDatabase() {
        return database;
    }

    /** Optional client name sent via {@code CLIENT SETNAME} on connect. */
    @Nullable
    public String getClientName() {
        return clientName;
    }

    /** ACL username for Redis 6+ ACL auth, or {@code null} for no ACL auth. */
    @Nullable
    public String getUsername() {
        return username;
    }

    /** Redis password for {@code AUTH}, or {@code null} for no password auth. */
    @Nullable
    public String getPassword() {
        return password;
    }

    /** Whether TLS/SSL is enabled. */
    public boolean isSsl() {
        return ssl;
    }

    /** Whether to verify the server's TLS certificate (only relevant when {@code ssl=true}). */
    public boolean isSslVerifyPeer() {
        return sslVerifyPeer;
    }

    /** Whether to use STARTTLS instead of direct TLS (only relevant when {@code ssl=true}). */
    public boolean isStartTls() {
        return startTls;
    }

    /** TCP connection establishment timeout in milliseconds (default 5000). */
    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /** Per-command timeout in milliseconds (default 60000). */
    public long getCommandTimeoutMs() {
        return commandTimeoutMs;
    }

    /** Whether to reconnect automatically after a dropped connection (default true). */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /** Whether to PING before activating a connection from the pool (default false). */
    public boolean isPingBeforeActivate() {
        return pingBeforeActivate;
    }

    /** Max queued commands while the connection is not ready (default {@link Integer#MAX_VALUE}). */
    public int getRequestQueueSize() {
        return requestQueueSize;
    }

    /** Whether TCP_NODELAY is enabled — disables Nagle's algorithm (default true). */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /** Whether SO_KEEPALIVE is enabled (default false). */
    public boolean isTcpKeepAlive() {
        return tcpKeepAlive;
    }

    /**
     * Command behaviour while disconnected, or {@code null} to use the Lettuce default.
     *
     * @return disconnected behaviour, or {@code null}
     */
    @Nullable
    public DisconnectedBehavior getDisconnectedBehavior() {
        return disconnectedBehavior;
    }

    /**
     * SSL/TLS provider, or {@code null} to use the Lettuce default ({@code JDK}).
     *
     * @return SSL provider, or {@code null}
     */
    @Nullable
    public SslProvider getSslProvider() {
        return sslProvider;
    }

    /**
     * Filesystem path to the TLS keystore (mutual TLS), or {@code null} if not configured.
     *
     * @return keystore path, or {@code null}
     */
    @Nullable
    public String getSslKeystore() {
        return sslKeystore;
    }

    /**
     * Password for the TLS keystore, or {@code null} if not set.
     *
     * @return keystore password, or {@code null}
     */
    @Nullable
    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    /**
     * Filesystem path to the TLS truststore (custom CA), or {@code null} if not configured.
     *
     * @return truststore path, or {@code null}
     */
    @Nullable
    public String getSslTruststore() {
        return sslTruststore;
    }

    /**
     * Password for the TLS truststore, or {@code null} if not set.
     *
     * @return truststore password, or {@code null}
     */
    @Nullable
    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    /**
     * Number of TCP keepalive probes before the connection is declared dead,
     * or {@code null} to use the Lettuce default (9).
     *
     * @return keepalive probe count, or {@code null}
     */
    @Nullable
    public Integer getKeepAliveCount() {
        return keepAliveCount;
    }

    /**
     * Idle time (in milliseconds) before TCP keepalive probes start,
     * or {@code null} to use the Lettuce default (7 200 000 ms).
     *
     * @return keepalive idle time in milliseconds, or {@code null}
     */
    @Nullable
    public Long getKeepAliveIdle() {
        return keepAliveIdle;
    }

    /**
     * Interval (in milliseconds) between TCP keepalive probes,
     * or {@code null} to use the Lettuce default (75 000 ms).
     *
     * @return keepalive interval in milliseconds, or {@code null}
     */
    @Nullable
    public Long getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Redis Serialization Protocol version, or {@code null} to let Lettuce negotiate.
     *
     * @return protocol version, or {@code null}
     */
    @Nullable
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Whether Lettuce's own per-command latency metrics are routed into Appbase's Micrometer composite
     * (and therefore fan out to JMX and any other registered reporting registry).
     *
     * @return {@code true} if command-latency metrics are routed into the composite (default {@code true})
     */
    public boolean isCommandLatencyMetricsEnabled() {
        return commandLatencyMetricsEnabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultRedisClientIntegrationServiceConfiguration that =
                (DefaultRedisClientIntegrationServiceConfiguration) o;
        return ssl == that.ssl &&
                sslVerifyPeer == that.sslVerifyPeer &&
                startTls == that.startTls &&
                connectTimeoutMs == that.connectTimeoutMs &&
                commandTimeoutMs == that.commandTimeoutMs &&
                autoReconnect == that.autoReconnect &&
                pingBeforeActivate == that.pingBeforeActivate &&
                requestQueueSize == that.requestQueueSize &&
                tcpNoDelay == that.tcpNoDelay &&
                tcpKeepAlive == that.tcpKeepAlive &&
                Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(hostPortConfiguration, that.hostPortConfiguration) &&
                Objects.equals(database, that.database) &&
                Objects.equals(clientName, that.clientName) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                disconnectedBehavior == that.disconnectedBehavior &&
                sslProvider == that.sslProvider &&
                Objects.equals(sslKeystore, that.sslKeystore) &&
                Objects.equals(sslKeystorePassword, that.sslKeystorePassword) &&
                Objects.equals(sslTruststore, that.sslTruststore) &&
                Objects.equals(sslTruststorePassword, that.sslTruststorePassword) &&
                Objects.equals(keepAliveCount, that.keepAliveCount) &&
                Objects.equals(keepAliveIdle, that.keepAliveIdle) &&
                Objects.equals(keepAliveInterval, that.keepAliveInterval) &&
                protocolVersion == that.protocolVersion &&
                commandLatencyMetricsEnabled == that.commandLatencyMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration, hostPortConfiguration, database, clientName,
                username, password,
                ssl, sslVerifyPeer, startTls,
                connectTimeoutMs, commandTimeoutMs,
                autoReconnect, pingBeforeActivate, requestQueueSize,
                tcpNoDelay, tcpKeepAlive,
                disconnectedBehavior,
                sslProvider, sslKeystore, sslKeystorePassword, sslTruststore, sslTruststorePassword,
                keepAliveCount, keepAliveIdle, keepAliveInterval,
                protocolVersion, commandLatencyMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultRedisClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", hostPortConfiguration=" + hostPortConfiguration +
                ", database=" + database +
                ", clientName='" + clientName + '\'' +
                ", username='" + username + '\'' +
                ", ssl=" + ssl +
                ", sslVerifyPeer=" + sslVerifyPeer +
                ", startTls=" + startTls +
                ", connectTimeoutMs=" + connectTimeoutMs +
                ", commandTimeoutMs=" + commandTimeoutMs +
                ", autoReconnect=" + autoReconnect +
                ", pingBeforeActivate=" + pingBeforeActivate +
                ", requestQueueSize=" + requestQueueSize +
                ", tcpNoDelay=" + tcpNoDelay +
                ", tcpKeepAlive=" + tcpKeepAlive +
                ", disconnectedBehavior=" + disconnectedBehavior +
                ", sslProvider=" + sslProvider +
                ", sslKeystore='" + sslKeystore + '\'' +
                ", sslKeystorePassword=" + (sslKeystorePassword != null ? "[REDACTED]" : null) +
                ", sslTruststore='" + sslTruststore + '\'' +
                ", sslTruststorePassword=" + (sslTruststorePassword != null ? "[REDACTED]" : null) +
                ", keepAliveCount=" + keepAliveCount +
                ", keepAliveIdle=" + keepAliveIdle +
                ", keepAliveInterval=" + keepAliveInterval +
                ", protocolVersion=" + protocolVersion +
                ", commandLatencyMetricsEnabled=" + commandLatencyMetricsEnabled +
                '}';
    }


    /**
     * Builder for {@link DefaultRedisClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private static final HostPortConfiguration DEFAULT_HOST_PORT_CONFIGURATION =
                HostPortConfiguration.builder().host("localhost").port(6379).build();

        private TextValueConfiguration textValueKeyConfiguration = TextValueConfiguration.PRIMARY;

        private HostPortConfiguration hostPortConfiguration = DEFAULT_HOST_PORT_CONFIGURATION;
        @Nullable private Integer database;
        @Nullable private String clientName;
        @Nullable private String username;
        @Nullable private String password;
        private boolean ssl = false;
        private boolean sslVerifyPeer = true;
        private boolean startTls = false;
        private long connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        private long commandTimeoutMs = DEFAULT_COMMAND_TIMEOUT_MS;
        private boolean autoReconnect = true;
        private boolean pingBeforeActivate = false;
        private int requestQueueSize = Integer.MAX_VALUE;
        private boolean tcpNoDelay = true;
        private boolean tcpKeepAlive = false;
        @Nullable private DisconnectedBehavior disconnectedBehavior;
        @Nullable private SslProvider sslProvider;
        @Nullable private String sslKeystore;
        @Nullable private String sslKeystorePassword;
        @Nullable private String sslTruststore;
        @Nullable private String sslTruststorePassword;
        @Nullable private Integer keepAliveCount;
        @Nullable private Long keepAliveIdle;
        @Nullable private Long keepAliveInterval;
        @Nullable private ProtocolVersion protocolVersion;
        private boolean commandLatencyMetricsEnabled = DEFAULT_COMMAND_LATENCY_METRICS_ENABLED;

        private Builder() {
        }

        /**
         * Sets the client key configuration.
         *
         * @param v configuration
         * @return this builder
         */
        public Builder textValueKeyConfiguration(@Nullable final TextValueConfiguration v) {
            this.textValueKeyConfiguration = v;
            return this;
        }

        /**
         * Sets the Redis server host and port.
         *
         * @param v host-port configuration
         * @return this builder
         */
        public Builder hostPortConfiguration(final HostPortConfiguration v) {
            this.hostPortConfiguration = v;
            return this;
        }

        /**
         * Sets the Redis database index (SELECT). {@code null} = use Lettuce default (0).
         *
         * @param v database index (0–15), or {@code null}
         * @return this builder
         */
        public Builder database(@Nullable final Integer v) {
            this.database = v;
            return this;
        }

        /**
         * Sets the client name sent via {@code CLIENT SETNAME}.
         *
         * @param v client name, or {@code null}
         * @return this builder
         */
        public Builder clientName(@Nullable final String v) {
            this.clientName = v;
            return this;
        }

        /**
         * Sets the ACL username for Redis 6+ auth.
         *
         * @param v ACL username, or {@code null}
         * @return this builder
         */
        public Builder username(@Nullable final String v) {
            this.username = v;
            return this;
        }

        /**
         * Sets the Redis password.
         *
         * @param v password, or {@code null}
         * @return this builder
         */
        public Builder password(@Nullable final String v) {
            this.password = v;
            return this;
        }

        /**
         * Enables or disables TLS/SSL.
         *
         * @param v {@code true} to enable TLS
         * @return this builder
         */
        public Builder ssl(final boolean v) {
            this.ssl = v;
            return this;
        }

        /**
         * Controls TLS peer verification (only relevant when {@code ssl=true}).
         *
         * @param v {@code true} to verify the server certificate
         * @return this builder
         */
        public Builder sslVerifyPeer(final boolean v) {
            this.sslVerifyPeer = v;
            return this;
        }

        /**
         * Controls STARTTLS mode (only relevant when {@code ssl=true}).
         *
         * @param v {@code true} to use STARTTLS
         * @return this builder
         */
        public Builder startTls(final boolean v) {
            this.startTls = v;
            return this;
        }

        /**
         * Sets the TCP connection establishment timeout.
         *
         * @param v timeout in milliseconds
         * @return this builder
         */
        public Builder connectTimeoutMs(final long v) {
            this.connectTimeoutMs = v;
            return this;
        }

        /**
         * Sets the per-command timeout.
         *
         * @param v timeout in milliseconds
         * @return this builder
         */
        public Builder commandTimeoutMs(final long v) {
            this.commandTimeoutMs = v;
            return this;
        }

        /**
         * Controls automatic reconnection after a dropped connection.
         *
         * @param v {@code true} to enable auto-reconnect
         * @return this builder
         */
        public Builder autoReconnect(final boolean v) {
            this.autoReconnect = v;
            return this;
        }

        /**
         * Controls whether a PING is sent before activating a connection from the pool.
         *
         * @param v {@code true} to PING before activate
         * @return this builder
         */
        public Builder pingBeforeActivate(final boolean v) {
            this.pingBeforeActivate = v;
            return this;
        }

        /**
         * Sets the maximum number of commands that may be queued.
         *
         * @param v queue size
         * @return this builder
         */
        public Builder requestQueueSize(final int v) {
            this.requestQueueSize = v;
            return this;
        }

        /**
         * Controls TCP_NODELAY (Nagle's algorithm disabled when {@code true}).
         *
         * @param v {@code true} to enable TCP_NODELAY
         * @return this builder
         */
        public Builder tcpNoDelay(final boolean v) {
            this.tcpNoDelay = v;
            return this;
        }

        /**
         * Controls SO_KEEPALIVE.
         *
         * @param v {@code true} to enable TCP keep-alive
         * @return this builder
         */
        public Builder tcpKeepAlive(final boolean v) {
            this.tcpKeepAlive = v;
            return this;
        }

        /**
         * Sets the command behaviour while disconnected.
         * {@code null} = use Lettuce default ({@code REJECT_COMMANDS}).
         *
         * @param v disconnected behaviour, or {@code null}
         * @return this builder
         */
        public Builder disconnectedBehavior(@Nullable final DisconnectedBehavior v) {
            this.disconnectedBehavior = v;
            return this;
        }

        /**
         * Sets the SSL/TLS provider. {@code null} = use Lettuce default ({@code JDK}).
         *
         * @param v SSL provider, or {@code null}
         * @return this builder
         */
        public Builder sslProvider(@Nullable final SslProvider v) {
            this.sslProvider = v;
            return this;
        }

        /**
         * Sets the filesystem path to the TLS keystore (for mutual TLS).
         *
         * @param v keystore path, or {@code null}
         * @return this builder
         */
        public Builder sslKeystore(@Nullable final String v) {
            this.sslKeystore = v;
            return this;
        }

        /**
         * Sets the password for the TLS keystore.
         *
         * @param v keystore password, or {@code null}
         * @return this builder
         */
        public Builder sslKeystorePassword(@Nullable final String v) {
            this.sslKeystorePassword = v;
            return this;
        }

        /**
         * Sets the filesystem path to the TLS truststore (for custom CA).
         *
         * @param v truststore path, or {@code null}
         * @return this builder
         */
        public Builder sslTruststore(@Nullable final String v) {
            this.sslTruststore = v;
            return this;
        }

        /**
         * Sets the password for the TLS truststore.
         *
         * @param v truststore password, or {@code null}
         * @return this builder
         */
        public Builder sslTruststorePassword(@Nullable final String v) {
            this.sslTruststorePassword = v;
            return this;
        }

        /**
         * Sets the number of TCP keepalive probes before the connection is declared dead.
         * {@code null} = use Lettuce default (9).
         *
         * @param v probe count, or {@code null}
         * @return this builder
         */
        public Builder keepAliveCount(@Nullable final Integer v) {
            this.keepAliveCount = v;
            return this;
        }

        /**
         * Sets the idle time (in milliseconds) before TCP keepalive probes start.
         * {@code null} = use Lettuce default (7 200 000 ms).
         *
         * @param v idle time in milliseconds, or {@code null}
         * @return this builder
         */
        public Builder keepAliveIdle(@Nullable final Long v) {
            this.keepAliveIdle = v;
            return this;
        }

        /**
         * Sets the interval (in milliseconds) between TCP keepalive probes.
         * {@code null} = use Lettuce default (75 000 ms).
         *
         * @param v interval in milliseconds, or {@code null}
         * @return this builder
         */
        public Builder keepAliveInterval(@Nullable final Long v) {
            this.keepAliveInterval = v;
            return this;
        }

        /**
         * Sets the Redis Serialization Protocol version.
         * {@code null} = let Lettuce negotiate.
         *
         * @param v protocol version, or {@code null}
         * @return this builder
         */
        public Builder protocolVersion(@Nullable final ProtocolVersion v) {
            this.protocolVersion = v;
            return this;
        }

        /**
         * Controls whether Lettuce's own per-command latency metrics are routed into Appbase's
         * Micrometer composite (and therefore fan out to JMX and any other registered reporting registry).
         *
         * @param v {@code true} to route command-latency metrics into the composite (default {@code true})
         * @return this builder
         */
        public Builder commandLatencyMetricsEnabled(final boolean v) {
            this.commandLatencyMetricsEnabled = v;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return new {@link DefaultRedisClientIntegrationServiceConfiguration}
         */
        public DefaultRedisClientIntegrationServiceConfiguration build() {
            return new DefaultRedisClientIntegrationServiceConfiguration(this);
        }
    }
}
