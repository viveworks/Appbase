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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.redis.configuration.DisconnectedBehavior;
import com.viveworks.appbase.client.service.redis.configuration.ProtocolVersion;
import com.viveworks.appbase.client.service.redis.configuration.SslProvider;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link RedisClientIntegrationService}.
 *
 * <p>Manages a single {@link StatefulRedisConnection} to a Redis endpoint.
 * Uses Lettuce's synchronous connection model. The underlying {@link RedisClient}
 * is stored separately so it can be shut down on {@link #shutdownImpl()}. When
 * {@code isCommandLatencyMetricsEnabled()} is set, a dedicated {@link ClientResources} carrying a
 * {@link MicrometerCommandLatencyRecorder} bound to Appbase's composite meter registry is also held —
 * {@link RedisClient#shutdown()} does <b>not</b> shut down externally-supplied {@link ClientResources},
 * so it must be (and is) shut down separately in {@link #shutdownImpl()}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultRedisClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <StatefulRedisConnection<String, String>, TextValue,
                DefaultRedisClientIntegrationServiceConfiguration>
        implements RedisClientIntegrationService
{
    /** Bound wait for {@link ClientResources#shutdown()} so a stuck shutdown cannot hang forever. */
    private static final long CLIENT_RESOURCES_SHUTDOWN_TIMEOUT_SECONDS = 10L;

    private final AtomicReference<StatefulRedisConnection<String, String>> connectionRef;

    /** Held for shutdown — {@link RedisClient} owns the Netty event loop. */
    private final AtomicReference<RedisClient> redisClientRef;

    /**
     * Held for shutdown — set only when {@code isCommandLatencyMetricsEnabled()} is true. {@link RedisClient}
     * built from externally-supplied {@link ClientResources} does not own (and will not shut down) them.
     */
    private final AtomicReference<ClientResources> clientResourcesRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultRedisClientIntegrationServiceConfiguration
            defaultRedisClientIntegrationServiceConfiguration;


    /** Pre-built constructor — wraps an already-connected session. */
    @SuppressWarnings("unchecked")
    public DefaultRedisClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final StatefulRedisConnection<String, String> connection,
            final RedisClient redisClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey,
                (Class<StatefulRedisConnection<String, String>>) (Class<?>) StatefulRedisConnection.class);

        AppbaseUtil.notNull(connection, "connection");
        AppbaseUtil.notNull(redisClient, "redisClient");

        this.connectionRef = new AtomicReference<>(connection);
        this.redisClientRef = new AtomicReference<>(redisClient);
        this.clientResourcesRef = new AtomicReference<>();
        this.defaultRedisClientIntegrationServiceConfiguration = null;
    }

    /** Config-based constructor — triggers {@link #initImpl()} immediately. */
    @SuppressWarnings("unchecked")
    public DefaultRedisClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultRedisClientIntegrationServiceConfiguration configuration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey,
                (Class<StatefulRedisConnection<String, String>>) (Class<?>) StatefulRedisConnection.class);

        AppbaseUtil.notNull(configuration, "configuration");

        this.connectionRef = new AtomicReference<>();
        this.redisClientRef = new AtomicReference<>();
        this.clientResourcesRef = new AtomicReference<>();
        this.defaultRedisClientIntegrationServiceConfiguration = configuration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultRedisClientIntegrationServiceConfiguration,
                "defaultRedisClientIntegrationServiceConfiguration");

        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                defaultRedisClientIntegrationServiceConfiguration;

        final RedisURI.Builder uriBuilder = RedisURI.Builder
                .redis(cfg.getHostPortConfiguration().getHost(),
                        cfg.getHostPortConfiguration().getPort())
                .withTimeout(Duration.ofMillis(cfg.getCommandTimeoutMs()))
                .withSsl(cfg.isSsl())
                .withVerifyPeer(cfg.isSslVerifyPeer())
                .withStartTls(cfg.isStartTls());

        if (cfg.getDatabase() != null) {
            uriBuilder.withDatabase(cfg.getDatabase());
        }

        if (cfg.getUsername() != null && cfg.getPassword() != null) {
            uriBuilder.withAuthentication(cfg.getUsername(), cfg.getPassword().toCharArray());
        } else if (cfg.getPassword() != null) {
            uriBuilder.withPassword(cfg.getPassword().toCharArray());
        }

        if (cfg.getClientName() != null) {
            uriBuilder.withClientName(cfg.getClientName());
        }

        // ── Granular TCP keepalive ─────────────────────────────────────────
        final SocketOptions.KeepAliveOptions.Builder keepAliveBuilder =
                SocketOptions.KeepAliveOptions.builder();
        keepAliveBuilder.enable(cfg.isTcpKeepAlive());
        if (cfg.getKeepAliveCount() != null) {
            keepAliveBuilder.count(cfg.getKeepAliveCount());
        }
        if (cfg.getKeepAliveIdle() != null) {
            keepAliveBuilder.idle(Duration.ofMillis(cfg.getKeepAliveIdle()));
        }
        if (cfg.getKeepAliveInterval() != null) {
            keepAliveBuilder.interval(Duration.ofMillis(cfg.getKeepAliveInterval()));
        }

        final SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()))
                .tcpNoDelay(cfg.isTcpNoDelay())
                .keepAlive(keepAliveBuilder.build())
                .build();

        // ── ClientOptions ─────────────────────────────────────────────────
        final ClientOptions.Builder clientOptionsBuilder = ClientOptions.builder()
                .autoReconnect(cfg.isAutoReconnect())
                .pingBeforeActivateConnection(cfg.isPingBeforeActivate())
                .requestQueueSize(cfg.getRequestQueueSize())
                .socketOptions(socketOptions);

        // Disconnected behaviour
        final DisconnectedBehavior disconnectedBehavior = cfg.getDisconnectedBehavior();
        if (disconnectedBehavior != null) {
            clientOptionsBuilder.disconnectedBehavior(
                    ClientOptions.DisconnectedBehavior.valueOf(disconnectedBehavior.getText()));
        }

        // ── SSL options (mutual TLS / custom CA) ──────────────────────────
        final SslProvider sslProvider = cfg.getSslProvider();
        final String sslKeystore = cfg.getSslKeystore();
        final String sslTruststore = cfg.getSslTruststore();
        if (sslProvider != null || sslKeystore != null || sslTruststore != null) {
            final SslOptions.Builder sslOptionsBuilder = SslOptions.builder();
            if (sslProvider != null) {
                switch (sslProvider) {
                    case JDK:
                        sslOptionsBuilder.jdkSslProvider();
                        break;
                    case OPENSSL:
                        sslOptionsBuilder.openSslProvider();
                        break;
                    default:
                        break;
                }
            }
            try {
                if (sslKeystore != null) {
                    final URL keystoreUrl = new File(sslKeystore).toURI().toURL();
                    final char[] keystorePassword = cfg.getSslKeystorePassword() != null
                            ? cfg.getSslKeystorePassword().toCharArray() : new char[0];
                    sslOptionsBuilder.keystore(keystoreUrl, keystorePassword);
                }
                if (sslTruststore != null) {
                    final URL truststoreUrl = new File(sslTruststore).toURI().toURL();
                    final String truststorePassword = cfg.getSslTruststorePassword();
                    sslOptionsBuilder.truststore(truststoreUrl, truststorePassword);
                }
            } catch (MalformedURLException e) {
                throw new AppbaseClientException(
                        "Invalid SSL keystore or truststore path in Redis configuration", e);
            }
            clientOptionsBuilder.sslOptions(sslOptionsBuilder.build());
        }

        // Protocol version
        final ProtocolVersion protocolVersion = cfg.getProtocolVersion();
        if (protocolVersion != null) {
            clientOptionsBuilder.protocolVersion(
                    io.lettuce.core.protocol.ProtocolVersion.valueOf(protocolVersion.getText()));
        }

        final ClientOptions clientOptions = clientOptionsBuilder.build();

        // ── Lettuce-native command-latency metrics ─────────────────────────
        // Bind Lettuce's own per-command latency recorder to Appbase's composite meter registry so
        // Lettuce metrics fan out to JMX (and any other registered reporting registry) alongside the
        // app-level per-op timers. RedisClient does not own externally-supplied ClientResources, so
        // clientResourcesRef must be shut down separately in shutdownImpl().
        final RedisClient redisClient;
        if (cfg.isCommandLatencyMetricsEnabled()) {
            final MeterRegistry meterRegistry = appbaseClientCore.getAppbaseCore().getMetricService()
                    .getCompositeMeterRegistry();
            final MicrometerCommandLatencyRecorder commandLatencyRecorder =
                    new MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.create());
            final ClientResources clientResources = DefaultClientResources.builder()
                    .commandLatencyRecorder(commandLatencyRecorder)
                    .build();
            this.clientResourcesRef.set(clientResources);
            redisClient = RedisClient.create(clientResources, uriBuilder.build());
        } else {
            redisClient = RedisClient.create(uriBuilder.build());
        }
        redisClient.setOptions(clientOptions);

        // RedisClient#connect(...) throws an UNCHECKED RedisConnectionException on any connection
        // failure (bad host/port, refused connection, Redis down). If that escapes here with
        // clientResources/redisClient already built but their refs not yet used for shutdown, the
        // Netty event-loop thread pool they own leaks for the JVM lifetime (the half-constructed
        // service instance is unreachable, so shutdownImpl() never runs). Clean both up before
        // letting any such failure propagate.
        final StatefulRedisConnection<String, String> connection;
        try {
            connection = redisClient.connect(StringCodec.UTF8);
            this.redisClientRef.set(redisClient);
        } catch (final RuntimeException e) {
            try {
                redisClient.shutdown();
            } catch (final RuntimeException shutdownEx) {
                logger.warn("Error shutting down Redis RedisClient after failed connect", shutdownEx);
            }
            final ClientResources heldClientResources = clientResourcesRef.getAndSet(null);
            if (heldClientResources != null) {
                shutdownClientResources(heldClientResources);
            }
            throw e;
        }

        this.connectionRef.set(connection);

        super.postInit();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultRedisClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final StatefulRedisConnection<String, String> connection = connectionRef.get();
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        connectionRef.set(null);

        final RedisClient redisClient = redisClientRef.get();
        if (redisClient != null) {
            redisClient.shutdown();
        }
        redisClientRef.set(null);

        final ClientResources clientResources = clientResourcesRef.getAndSet(null);
        if (clientResources != null) {
            shutdownClientResources(clientResources);
        }
    }

    /**
     * Bounded, interrupt-safe shutdown of externally-supplied {@link ClientResources}. Shared by
     * {@link #shutdownImpl()} (normal lifecycle teardown) and {@link #initImpl()}'s failed-connect
     * cleanup path, so a connection failure during {@code init()}/{@code repair()} cannot leak the
     * Netty event-loop thread pool {@link ClientResources} owns.
     *
     * <p>Callers are responsible for clearing {@code clientResourcesRef} (typically via
     * {@code getAndSet(null)}) before invoking this method, so it is never shut down twice.
     *
     * @param clientResources the resources to shut down; must not be {@code null}
     */
    private void shutdownClientResources(final ClientResources clientResources) {
        try {
            clientResources.shutdown().get(CLIENT_RESOURCES_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while shutting down Redis ClientResources", ie);
        } catch (ExecutionException | TimeoutException ex) {
            logger.warn("Error while shutting down Redis ClientResources", ex);
        }
    }

    @Override
    public StatefulRedisConnection<String, String> getClient() {
        return connectionRef.get();
    }

    @Override
    public boolean isHealthy() {
        final StatefulRedisConnection<String, String> connection = connectionRef.get();
        return connection != null && connection.isOpen();
    }

    /**
     * Test-only accessor for the held {@link ClientResources}, deliberately package-private. Lets
     * unit tests confirm that a failed {@link #initImpl()} clears the ref (i.e. shuts the resources
     * down) rather than leaking them, without exposing this as part of the public API.
     *
     * @return the currently held {@link ClientResources}, or {@code null} if none is held
     */
    ClientResources getClientResourcesRefForTesting() {
        return clientResourcesRef.get();
    }
}
