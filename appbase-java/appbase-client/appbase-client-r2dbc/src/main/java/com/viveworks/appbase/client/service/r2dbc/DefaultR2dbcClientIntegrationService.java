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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link R2dbcClientIntegrationService}. Builds, holds, and disposes a
 * native {@link ConnectionPool} for a single {@link AppbaseClientKey} &mdash; the pool itself
 * performs connection pooling reactively (via Reactor's {@code reactor.pool.InstrumentedPool}
 * under the hood), so this class does no additional buffering of its own.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultR2dbcClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <ConnectionPool, TextValue, DefaultR2dbcClientIntegrationServiceConfiguration>
        implements R2dbcClientIntegrationService
{
    private final AtomicReference<ConnectionPool> connectionPoolRef;
    private final List<Meter> poolMeters = new CopyOnWriteArrayList<>();

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultR2dbcClientIntegrationServiceConfiguration defaultR2dbcClientIntegrationServiceConfiguration;


    public DefaultR2dbcClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final ConnectionPool connectionPool)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, ConnectionPool.class);

        AppbaseUtil.notNull(connectionPool, "connectionPool");

        this.connectionPoolRef = new AtomicReference<>(connectionPool);
        this.defaultR2dbcClientIntegrationServiceConfiguration = null;
    }

    public DefaultR2dbcClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultR2dbcClientIntegrationServiceConfiguration defaultR2dbcClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, ConnectionPool.class);

        AppbaseUtil.notNull(
                defaultR2dbcClientIntegrationServiceConfiguration, "defaultR2dbcClientIntegrationServiceConfiguration");

        this.connectionPoolRef = new AtomicReference<>();
        this.defaultR2dbcClientIntegrationServiceConfiguration = defaultR2dbcClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultR2dbcClientIntegrationServiceConfiguration, "defaultR2dbcClientIntegrationServiceConfiguration");

        final ConnectionPool connectionPool = R2dbcClientUtil.connectionPool(
                defaultR2dbcClientIntegrationServiceConfiguration.getR2dbcConnectionConfiguration(),
                defaultR2dbcClientIntegrationServiceConfiguration.getR2dbcPoolConfiguration());

        this.connectionPoolRef.set(connectionPool);

        // Config-gated, default ON (uniform with the jasync/Aerospike/Cassandra/ZooKeeper twins):
        // R2DBC's native ConnectionPool#getMetrics() is a real driver-native metrics source, bridged
        // onto the shared registry by default; disable via driverMetricsEnabled(false).
        bindPoolMetrics(connectionPool);
    }

    /**
     * Bridges native R2DBC {@link ConnectionPool} metrics ({@link PoolMetrics}) onto Micrometer
     * gauges on the shared composite registry (&rarr; JMX). No-op unless
     * {@link #isDriverMetricsEnabled()} is {@code true} AND {@link ConnectionPool#getMetrics()}
     * returns a present {@link PoolMetrics} for the given pool. The gauges POLL the live
     * {@link ConnectionPool} each scrape (via {@link ConnectionPool#getMetrics()}), so they always
     * reflect the pool's current state rather than a bind-time snapshot.
     *
     * <p>Every gauge is tagged with this service's own {@link #appbaseClientKey}, stringified, so
     * that multiple keyed R2DBC pools sharing the same composite registry register DISTINCT
     * Micrometer Ids. Without this tag, every {@code DefaultR2dbcClientIntegrationService}
     * instance would register the exact same four gauge Ids: Micrometer's {@code register()}
     * dedup-by-Id would silently drop the second-and-later keyed pools' gauges, and {@link
     * #unbindPoolMetrics()} on one keyed service would remove a still-live sibling's gauges.
     *
     * @param connectionPool the native {@link ConnectionPool} to bind gauges against
     */
    protected void bindPoolMetrics(final ConnectionPool connectionPool) {
        if (!isDriverMetricsEnabled() || connectionPool == null || !connectionPool.getMetrics().isPresent()) {
            return;
        }

        // Defensive: a re-bind without an intervening shutdown (e.g. re-init/repair) still
        // refreshes against the new pool rather than being silently dropped by Micrometer's
        // register() dedup-by-Id.
        unbindPoolMetrics();

        final MeterRegistry registry = appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
        final Tags keyTag = Tags.of("key", appbaseClientKey.toString());
        poolMeters.add(
                Gauge.builder(
                                "r2dbc.pool.connections.acquired",
                                connectionPool,
                                pool -> pool.getMetrics().map(PoolMetrics::acquiredSize).orElse(0))
                        .tags(keyTag)
                        .register(registry));
        poolMeters.add(
                Gauge.builder(
                                "r2dbc.pool.connections.idle",
                                connectionPool,
                                pool -> pool.getMetrics().map(PoolMetrics::idleSize).orElse(0))
                        .tags(keyTag)
                        .register(registry));
        poolMeters.add(
                Gauge.builder(
                                "r2dbc.pool.connections.pending",
                                connectionPool,
                                pool -> pool.getMetrics().map(PoolMetrics::pendingAcquireSize).orElse(0))
                        .tags(keyTag)
                        .register(registry));
        poolMeters.add(
                Gauge.builder(
                                "r2dbc.pool.connections.allocated",
                                connectionPool,
                                pool -> pool.getMetrics().map(PoolMetrics::allocatedSize).orElse(0))
                        .tags(keyTag)
                        .register(registry));
    }

    /**
     * Removes this service's own {@code r2dbc.pool.*} gauges from the shared composite registry
     * so that a later re-init (repair/reconnect) re-registers fresh gauges bound to the new pool,
     * instead of Micrometer's {@code register()} dedup-by-Id silently discarding the re-bind and
     * leaving the old gauges polling a stale, disposed {@link ConnectionPool}. This does NOT shut
     * down the shared composite registry itself -- that is MetricService-owned. No-op if no
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
        return defaultR2dbcClientIntegrationServiceConfiguration != null
                && defaultR2dbcClientIntegrationServiceConfiguration.isDriverMetricsEnabled();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultR2dbcClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        unbindPoolMetrics();

        final ConnectionPool connectionPool = getClient();

        if (connectionPool != null) {
            // ConnectionPool#dispose() (from reactor.core.Disposable) schedules disposal
            // asynchronously and returns immediately; #disposeLater() gives back the same
            // work as a Mono<Void>, so blocking on it here mirrors how the JDBC twin's
            // shutdownImpl() synchronously closes its HikariDataSource before returning.
            connectionPool.disposeLater().block();
        }

        connectionPoolRef.set(null);
    }

    @Override
    public ConnectionPool getClient() {
        return connectionPoolRef.get();
    }

    @Override
    public boolean isHealthy() {
        final ConnectionPool connectionPool = getClient();

        return connectionPool != null && !connectionPool.isDisposed();
    }
}
