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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.ConnectionPoolConfiguration;
import com.github.jasync.sql.db.SSLConfiguration;
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.jasync.configuration.DefaultJasyncClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jasync.configuration.JasyncClientServiceConfiguration;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.util.CharsetUtil;
import kotlin.collections.CollectionsKt;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.Dispatchers;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link JasyncClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJasyncClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <Connection, TextValue, DefaultJasyncClientIntegrationServiceConfiguration>
        implements JasyncClientIntegrationService
{
    private static final long DEFAULT_MAX_IDLE_TIME = TimeUnit.MINUTES.toMillis(1);
    private static final long DEFAULT_CONNECTION_VALIDATION_INTERVAL = TimeUnit.SECONDS.toMillis(5);
    private static final long DEFAULT_CONNECTION_CREATE_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final long DEFAULT_CONNECTION_TEST_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final Long DEFAULT_QUERY_TIMEOUT = null;
    private static final CoroutineDispatcher DEFAULT_COROUTINE_DISPATCHER = Dispatchers.getDefault();
    private static final SSLConfiguration DEFAULT_SSL_CONFIGURATION = new SSLConfiguration();
    private static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;
    private static final int DEFAULT_MAXIMUM_MESSAGE_SIZE = 16777216;
    private static final Long DEFAULT_MAX_CONNECTION_TTL = null;


    private final AtomicReference<Connection> connectionRef;
    private final List<Meter> poolMeters = new CopyOnWriteArrayList<>();

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultJasyncClientIntegrationServiceConfiguration defaultJasyncClientIntegrationServiceConfiguration;



    public DefaultJasyncClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Connection, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final Connection connection)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, Connection.class);

        AppbaseUtil.notNull(connection, "connection");

        this.connectionRef = new AtomicReference<>(connection);
        this.defaultJasyncClientIntegrationServiceConfiguration = null;
    }

    public DefaultJasyncClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Connection, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultJasyncClientIntegrationServiceConfiguration defaultJasyncClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, Connection.class);

        AppbaseUtil.notNull(
                defaultJasyncClientIntegrationServiceConfiguration,
                "defaultJasyncClientIntegrationServiceConfiguration");

        this.connectionRef = new AtomicReference<>();
        this.defaultJasyncClientIntegrationServiceConfiguration = defaultJasyncClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultJasyncClientIntegrationServiceConfiguration, "defaultJasyncClientIntegrationServiceConfiguration");

        final ConnectionPoolConfiguration connectionPoolConfiguration = connectionPoolConfiguration();
        final JasyncClientServiceConfiguration.DatabaseType databaseType =
                defaultJasyncClientIntegrationServiceConfiguration.getDatabaseType();

        final Connection connection;
        switch (databaseType) {
            case MYSQL_OR_MARIADB:
                connection = MySQLConnectionBuilder.createConnectionPool(connectionPoolConfiguration);
                break;
            case POSTGRESQL:
                connection = PostgreSQLConnectionBuilder.createConnectionPool(connectionPoolConfiguration);
                break;
            default:
                throw new AppbaseRuntimeException("Unsupported database type: " + databaseType);
        }

        connection.connect();
        connectionRef.set(connection);

        // jasync has no driver-native metrics registry (unlike Aerospike/Cassandra/Kafka): bridge
        // the pooled connection's own size counters onto the shared composite instead.
        bindPoolMetrics(connection);
    }

    /**
     * Bridges the jasync connection-pool size counters to Micrometer gauges on the shared
     * composite registry (→ JMX). jasync has no driver-native metrics registry, so these gauges
     * POLL the live {@link ConnectionPool} each scrape. No-op when {@link #isDriverMetricsEnabled()}
     * is {@code false} or {@code client} is not a {@link ConnectionPool}. The composite registry is
     * MetricService-owned -- it is never shut down here (unlike a driver-native metrics listener,
     * there is no per-driver metrics resource to stop; the gauges simply stop being scraped once
     * the pool is discarded).
     *
     * <p>Every gauge is tagged with this service's own {@link #appbaseClientKey}, stringified, so
     * that multiple keyed jasync pools sharing the same composite registry register DISTINCT
     * Micrometer Ids. Without this tag, every {@code DefaultJasyncClientIntegrationService}
     * instance would register the exact same three gauge Ids: Micrometer's {@code register()}
     * dedup-by-Id would silently drop the second-and-later keyed pools' gauges, and {@link
     * #unbindPoolMetrics()} on one keyed service would remove a still-live sibling's gauges.
     *
     * @param client the driver client, expected to be a {@link ConnectionPool} at runtime
     */
    protected void bindPoolMetrics(final Connection client) {
        if (!isDriverMetricsEnabled() || !(client instanceof ConnectionPool)) {
            return;
        }

        // Defensive: a re-bind without an intervening shutdown (e.g. re-init/repair) still
        // refreshes against the new pool rather than being silently dropped by Micrometer's
        // register() dedup-by-Id.
        unbindPoolMetrics();

        final ConnectionPool<?> pool = (ConnectionPool<?>) client;
        final MeterRegistry registry = appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
        final Tags keyTag = Tags.of("key", appbaseClientKey.toString());
        poolMeters.add(
                Gauge.builder("jasync.pool.connections.inuse", pool, p -> ((ConnectionPool<?>) p).getInUseConnectionsCount())
                        .tags(keyTag)
                        .register(registry));
        poolMeters.add(
                Gauge.builder("jasync.pool.connections.idle", pool, p -> ((ConnectionPool<?>) p).getIdleConnectionsCount())
                        .tags(keyTag)
                        .register(registry));
        poolMeters.add(
                Gauge.builder(
                                "jasync.pool.connections.waiting",
                                pool,
                                p -> ((ConnectionPool<?>) p).getFuturesWaitingForConnectionCount())
                        .tags(keyTag)
                        .register(registry));
    }

    /**
     * Removes this service's own {@code jasync.pool.*} gauges from the shared composite registry
     * so that a later re-init (repair/reconnect) re-registers fresh gauges bound to the new pool,
     * instead of Micrometer's {@code register()} dedup-by-Id silently discarding the re-bind and
     * leaving the old gauges polling a stale, disconnected {@link ConnectionPool}. This does NOT
     * shut down the shared composite registry itself -- that is MetricService-owned. No-op if no
     * gauges are currently bound.
     */
    protected void unbindPoolMetrics() {
        if (poolMeters.isEmpty()) {
            return;
        }

        final MeterRegistry registry = appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
        for (final Meter meter : poolMeters) {
            registry.remove(meter);
        }
        poolMeters.clear();
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return defaultJasyncClientIntegrationServiceConfiguration != null
                && defaultJasyncClientIntegrationServiceConfiguration.isDriverMetricsEnabled();
    }

    private ConnectionPoolConfiguration connectionPoolConfiguration() {
        final JasyncClientServiceConfiguration.DatabaseType databaseType =
                defaultJasyncClientIntegrationServiceConfiguration.getDatabaseType();
        AppbaseUtil.notNull(databaseType, "databaseType");


        final String host = defaultJasyncClientIntegrationServiceConfiguration.getHost();
        AppbaseUtil.notNull(host, "host");

        final int port = defaultJasyncClientIntegrationServiceConfiguration.getPort();
        AppbaseUtil.positiveOrZero(port, "port");

        final String user = defaultJasyncClientIntegrationServiceConfiguration.getUser();
        AppbaseUtil.notNull(user, "user");

        final String password = defaultJasyncClientIntegrationServiceConfiguration.getPassword();
        final String database = defaultJasyncClientIntegrationServiceConfiguration.getDatabase();
        final int maxActiveConnections = defaultJasyncClientIntegrationServiceConfiguration.getMaxActiveConnections();

        final long maxIdleTime;
        final TimeUnitConfiguration maxIdleTimeConfiguration =
                defaultJasyncClientIntegrationServiceConfiguration.getMaxIdleTimeConfiguration();
        if (maxIdleTimeConfiguration != null && maxIdleTimeConfiguration.isEnabled()) {
            final TimeUnit timeUnit = maxIdleTimeConfiguration.getTimeUnit();
            final long time = maxIdleTimeConfiguration.getTime();
            maxIdleTime = timeUnit.toMillis(time);
        } else {
            maxIdleTime = DEFAULT_MAX_IDLE_TIME;
        }

        final int maxPendingQueries = defaultJasyncClientIntegrationServiceConfiguration.getMaxPendingQueries();

        final long connectionValidationInterval;
        final TimeUnitConfiguration connectionValidationIntervalConfiguration =
                defaultJasyncClientIntegrationServiceConfiguration.getConnectionValidationIntervalConfiguration();
        if (connectionValidationIntervalConfiguration != null && connectionValidationIntervalConfiguration.isEnabled()) {
            final TimeUnit timeUnit = connectionValidationIntervalConfiguration.getTimeUnit();
            final long time = connectionValidationIntervalConfiguration.getTime();
            connectionValidationInterval = timeUnit.toMillis(time);
        } else {
            connectionValidationInterval = DEFAULT_CONNECTION_VALIDATION_INTERVAL;
        }

        final long connectionCreateTimeout;
        final TimeoutConfiguration connectionCreateTimeoutConfiguration =
                defaultJasyncClientIntegrationServiceConfiguration.getConnectionCreateTimeoutConfiguration();
        if (connectionCreateTimeoutConfiguration != null && connectionCreateTimeoutConfiguration.isEnabled()) {
            final long timeout = connectionCreateTimeoutConfiguration.getTimeout();
            final TimeUnit timeoutTimeUnit = connectionCreateTimeoutConfiguration.getTimeoutTimeUnit();
            connectionCreateTimeout = timeoutTimeUnit.toMillis(timeout);
        } else {
            connectionCreateTimeout = DEFAULT_CONNECTION_CREATE_TIMEOUT;
        }

        final long connectionTestTimeout;
        final TimeoutConfiguration connectionTestTimeoutConfiguration =
                defaultJasyncClientIntegrationServiceConfiguration.getConnectionTestTimeoutConfiguration();
        if (connectionTestTimeoutConfiguration != null && connectionTestTimeoutConfiguration.isEnabled()) {
            final long timeout = connectionTestTimeoutConfiguration.getTimeout();
            final TimeUnit timeoutTimeUnit = connectionTestTimeoutConfiguration.getTimeoutTimeUnit();
            connectionTestTimeout = timeoutTimeUnit.toMillis(timeout);
        } else {
            connectionTestTimeout = DEFAULT_CONNECTION_TEST_TIMEOUT;
        }

        final Long queryTimeout;
        final TimeoutConfiguration queryTimeoutConfiguration =
                defaultJasyncClientIntegrationServiceConfiguration.getQueryTimeoutConfiguration();
        if (queryTimeoutConfiguration != null && queryTimeoutConfiguration.isEnabled()) {
            final TimeUnit timeoutTimeUnit = queryTimeoutConfiguration.getTimeoutTimeUnit();
            final long timeout = queryTimeoutConfiguration.getTimeout();
            queryTimeout = timeoutTimeUnit.toMillis(timeout);
        } else {
            queryTimeout = DEFAULT_QUERY_TIMEOUT;
        }

        final ExecutorService executorService = appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedExecutorService();

        final EventLoopGroup eventLoopGroup = null;
        // byteBufAllocator: jasync's ConnectionPoolConfiguration null-checks this @NotNull Kotlin
        // param (Intrinsics.checkNotNullParameter) → passing null is a deterministic construction-time
        // NPE. ByteBufAllocator.DEFAULT is netty's stateless shared allocator (no lifecycle), the
        // correct non-null default. (eventLoopGroup above still needs a managed group — see internal work item 1135.)
        final ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;
        final String applicationName = defaultJasyncClientIntegrationServiceConfiguration.getApplicationName();


        final Long maxConnectionTtl;
        final TimeUnitConfiguration maxConnectionTtlConfiguration =
                defaultJasyncClientIntegrationServiceConfiguration.getMaxConnectionTtlConfiguration();
        if (maxConnectionTtlConfiguration != null && maxConnectionTtlConfiguration.isEnabled()) {
            final TimeUnit timeUnit = maxConnectionTtlConfiguration.getTimeUnit();
            final long time = maxConnectionTtlConfiguration.getTime();
            maxConnectionTtl = timeUnit.toMillis(time);
        } else {
            maxConnectionTtl = DEFAULT_MAX_CONNECTION_TTL;
        }

        @SuppressWarnings("unchecked")
        final ConnectionPoolConfiguration connectionPoolConfiguration = new ConnectionPoolConfiguration(
                host,
                port,
                database,
                user,
                password,
                maxActiveConnections,
                maxIdleTime,
                maxPendingQueries,
                connectionValidationInterval,
                connectionCreateTimeout,
                connectionTestTimeout,
                queryTimeout,
                eventLoopGroup,
                executorService,
                DEFAULT_COROUTINE_DISPATCHER,
                DEFAULT_SSL_CONFIGURATION,
                DEFAULT_CHARSET,
                DEFAULT_MAXIMUM_MESSAGE_SIZE,
                byteBufAllocator,
                applicationName,
                CollectionsKt.mutableListOf(),
                maxConnectionTtl);


        return connectionPoolConfiguration;
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultJasyncClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        unbindPoolMetrics();

        final Connection connection = getClient();
        if (connection != null) {
            connection.disconnect();
        }

        connectionRef.set(null);
    }

    @Override
    public Connection getClient() {
        return connectionRef.get();
    }

    @Override
    public boolean isHealthy() {
        final Connection connection = getClient();
        if (connection == null) {
            return false;
        }

        return connection.isConnected();
    }
}
