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

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Unit tests for the connection-pool metrics bridge seam of {@link
 * DefaultR2dbcClientIntegrationService}: {@code bindPoolMetrics(ConnectionPool)}.
 *
 * <p>The service under test is built via the pre-built-client constructor with a mocked native
 * {@link ConnectionPool} -- this constructor does NOT call {@code init()}/{@code initImpl()}, so
 * no live database connection is attempted. Because that constructor leaves the configuration
 * {@code null}, {@link DefaultR2dbcClientIntegrationService#isDriverMetricsEnabled()} is
 * hard-wired to {@code false} for an un-spied instance (mirrors {@code config != null &&
 * config.isDriverMetricsEnabled()}); the disabled-path assertion uses the service directly, and
 * the enabled-path assertions use a {@link Mockito#spy} that stubs {@code
 * isDriverMetricsEnabled()} to control the gate without wiring a full, connecting config. Mirrors
 * {@code DefaultJasyncClientIntegrationServicePoolMetricsTest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultR2dbcClientIntegrationServicePoolMetricsTest {
    private SimpleMeterRegistry registry;
    private DefaultR2dbcClientIntegrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultR2dbcClientIntegrationServicePoolMetricsTest.class));

        registry = new SimpleMeterRegistry();
        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(registry);

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<TextValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        // Pre-built-client constructor does NOT call init() -> no live connect / no initImpl.
        service = new DefaultR2dbcClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, connectionPool);
    }

    private static PoolMetrics poolMetrics(final int acquired, final int idle, final int pending, final int allocated) {
        final PoolMetrics poolMetrics = Mockito.mock(PoolMetrics.class);
        Mockito.when(poolMetrics.acquiredSize()).thenReturn(acquired);
        Mockito.when(poolMetrics.idleSize()).thenReturn(idle);
        Mockito.when(poolMetrics.pendingAcquireSize()).thenReturn(pending);
        Mockito.when(poolMetrics.allocatedSize()).thenReturn(allocated);
        return poolMetrics;
    }

    @Test
    void testBindPoolMetricsDisabledRegistersNoGaugesAndDoesNotConsultRegistry() {
        // Arrange — un-spied service: pre-built constructor leaves configuration null.
        final ConnectionPool pool = Mockito.mock(ConnectionPool.class);
        final PoolMetrics metrics = poolMetrics(1, 1, 1, 1);
        Mockito.when(pool.getMetrics()).thenReturn(Optional.of(metrics));

        // Act
        service.bindPoolMetrics(pool);

        // Assert
        Assertions.assertTrue(registry.getMeters().isEmpty());
    }

    @Test
    void testBindPoolMetricsEnabledRegistersGaugesPollingLivePoolCounters() {
        // Arrange
        final DefaultR2dbcClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        final ConnectionPool pool = Mockito.mock(ConnectionPool.class);
        final PoolMetrics poolMetrics = poolMetrics(2, 3, 1, 6);
        Mockito.when(pool.getMetrics()).thenReturn(Optional.of(poolMetrics));

        // Act
        spiedService.bindPoolMetrics(pool);

        // Assert
        Assertions.assertEquals(2.0, registry.get("r2dbc.pool.connections.acquired").gauge().value());
        Assertions.assertEquals(3.0, registry.get("r2dbc.pool.connections.idle").gauge().value());
        Assertions.assertEquals(1.0, registry.get("r2dbc.pool.connections.pending").gauge().value());
        Assertions.assertEquals(6.0, registry.get("r2dbc.pool.connections.allocated").gauge().value());

        // Assert — the gauges poll the live pool rather than snapshotting the value at bind time.
        Mockito.when(poolMetrics.acquiredSize()).thenReturn(5);
        Assertions.assertEquals(5.0, registry.get("r2dbc.pool.connections.acquired").gauge().value());
    }

    @Test
    void testBindPoolMetricsEnabledButNoMetricsPresentRegistersNoGauges() {
        // Arrange
        final DefaultR2dbcClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();
        final ConnectionPool pool = Mockito.mock(ConnectionPool.class);
        Mockito.when(pool.getMetrics()).thenReturn(Optional.empty());

        // Act
        spiedService.bindPoolMetrics(pool);

        // Assert
        Assertions.assertTrue(registry.getMeters().isEmpty());
    }

    @Test
    void testRebindAfterUnbindPollsNewPool() {
        // Arrange
        final DefaultR2dbcClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        final ConnectionPool poolA = Mockito.mock(ConnectionPool.class);
        final PoolMetrics poolAMetrics = poolMetrics(2, 3, 1, 6);
        Mockito.when(poolA.getMetrics()).thenReturn(Optional.of(poolAMetrics));

        // Act — bind against the first (now-stale, e.g. disposed) pool.
        spiedService.bindPoolMetrics(poolA);

        // Assert — gauges poll poolA.
        Assertions.assertEquals(2.0, registry.get("r2dbc.pool.connections.acquired").gauge().value());

        // Act — simulate shutdown's unbind step.
        spiedService.unbindPoolMetrics();

        // Assert — this service's own gauges are gone from the shared registry.
        Assertions.assertNull(registry.find("r2dbc.pool.connections.acquired").gauge());
        Assertions.assertNull(registry.find("r2dbc.pool.connections.idle").gauge());
        Assertions.assertNull(registry.find("r2dbc.pool.connections.pending").gauge());
        Assertions.assertNull(registry.find("r2dbc.pool.connections.allocated").gauge());

        final ConnectionPool poolB = Mockito.mock(ConnectionPool.class);
        final PoolMetrics poolBMetrics = poolMetrics(9, 4, 0, 10);
        Mockito.when(poolB.getMetrics()).thenReturn(Optional.of(poolBMetrics));

        // Act — re-init/repair rebinds against the new pool.
        spiedService.bindPoolMetrics(poolB);

        // Assert — gauge now polls poolB, not the stale poolA.
        Assertions.assertEquals(9.0, registry.get("r2dbc.pool.connections.acquired").gauge().value());
    }

    /**
     * Reproduces the multi-keyed-pool metrics collision: with &gt;1 {@link
     * DefaultR2dbcClientIntegrationService} instance (one per {@code AppbaseClientKey}) sharing
     * the same composite registry, un-tagged gauges register under identical Micrometer Ids, so
     * Micrometer's dedup-by-Id either drops the second pool's gauges or -- worse -- lets one
     * service's {@code unbindPoolMetrics()} remove a still-live sibling's gauges. Each service
     * must tag its gauges with its own stable identity so keyed pools stay independently
     * observable and independently removable.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testBindPoolMetricsIsolatesGaugesAndUnbindAcrossKeyedServices() {
        // Arrange — two integration services with DISTINCT client keys sharing ONE registry.
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultR2dbcClientIntegrationServicePoolMetricsTest.class));

        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(registry);

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<ConnectionPool, TextValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);

        final AppbaseClientKey<TextValue> keyA = Mockito.mock(AppbaseClientKey.class);
        Mockito.when(keyA.toString()).thenReturn("keyA");
        final AppbaseClientKey<TextValue> keyB = Mockito.mock(AppbaseClientKey.class);
        Mockito.when(keyB.toString()).thenReturn("keyB");

        final DefaultR2dbcClientIntegrationService serviceA = Mockito.spy(new DefaultR2dbcClientIntegrationService(
                appbaseClientCore, appbaseClientPool, keyA, Mockito.mock(ConnectionPool.class)));
        Mockito.doReturn(true).when(serviceA).isDriverMetricsEnabled();

        final DefaultR2dbcClientIntegrationService serviceB = Mockito.spy(new DefaultR2dbcClientIntegrationService(
                appbaseClientCore, appbaseClientPool, keyB, Mockito.mock(ConnectionPool.class)));
        Mockito.doReturn(true).when(serviceB).isDriverMetricsEnabled();

        final ConnectionPool poolA = Mockito.mock(ConnectionPool.class);
        final PoolMetrics poolAMetrics = poolMetrics(2, 0, 0, 2);
        Mockito.when(poolA.getMetrics()).thenReturn(Optional.of(poolAMetrics));

        final ConnectionPool poolB = Mockito.mock(ConnectionPool.class);
        final PoolMetrics poolBMetrics = poolMetrics(9, 0, 0, 9);
        Mockito.when(poolB.getMetrics()).thenReturn(Optional.of(poolBMetrics));

        // Act
        serviceA.bindPoolMetrics(poolA);
        serviceB.bindPoolMetrics(poolB);

        // Assert — both keyed pools are independently observable on the shared registry.
        Assertions.assertEquals(
                2.0, registry.get("r2dbc.pool.connections.acquired").tags("key", "keyA").gauge().value());
        Assertions.assertEquals(
                9.0, registry.get("r2dbc.pool.connections.acquired").tags("key", "keyB").gauge().value());

        // Act — service A shuts down; its own gauges are removed.
        serviceA.unbindPoolMetrics();

        // Assert — sibling service B's still-live gauge is unaffected by A's removal.
        Assertions.assertNull(registry.find("r2dbc.pool.connections.acquired").tags("key", "keyA").gauge());
        Assertions.assertEquals(
                9.0, registry.get("r2dbc.pool.connections.acquired").tags("key", "keyB").gauge().value());
    }
}
