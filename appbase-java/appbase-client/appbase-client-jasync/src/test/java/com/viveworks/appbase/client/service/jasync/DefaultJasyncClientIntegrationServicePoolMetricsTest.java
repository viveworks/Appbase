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
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for the connection-pool metrics bridge seam of {@link
 * DefaultJasyncClientIntegrationService}: {@code bindPoolMetrics(Connection)}.
 *
 * <p>The service under test is built via the pre-built-client constructor with a mocked jasync
 * {@link Connection} -- this constructor does NOT call {@code init()}/{@code initImpl()}, so no
 * live database connection is attempted. Because that constructor leaves the configuration
 * {@code null}, {@link DefaultJasyncClientIntegrationService#isDriverMetricsEnabled()} is
 * hard-wired to {@code false} for an un-spied instance (mirrors {@code config != null &&
 * config.isDriverMetricsEnabled()}); the disabled-path assertion uses the service directly, and
 * the enabled-path assertions use a {@link Mockito#spy} that stubs {@code
 * isDriverMetricsEnabled()} to control the gate without wiring a full, connecting config. Mirrors
 * {@code DefaultAerospikeClientIntegrationServiceMetricsTest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultJasyncClientIntegrationServicePoolMetricsTest {
    private SimpleMeterRegistry registry;
    private DefaultJasyncClientIntegrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultJasyncClientIntegrationServicePoolMetricsTest.class));

        registry = new SimpleMeterRegistry();
        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(registry);

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<Connection, TextValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<TextValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final Connection connection = Mockito.mock(Connection.class);

        // Pre-built-client constructor does NOT call init() -> no live connect / no initImpl.
        service = new DefaultJasyncClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, connection);
    }

    @Test
    void testBindPoolMetricsDisabledRegistersNoGaugesAndDoesNotConsultRegistry() {
        // Arrange — un-spied service: pre-built constructor leaves configuration null.
        final ConnectionPool<?> pool = Mockito.mock(ConnectionPool.class);

        // Act
        service.bindPoolMetrics(pool);

        // Assert
        Assertions.assertTrue(registry.getMeters().isEmpty());
    }

    @Test
    void testBindPoolMetricsEnabledRegistersGaugesPollingLivePoolCounters() {
        // Arrange
        final DefaultJasyncClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        final ConnectionPool<?> pool = Mockito.mock(ConnectionPool.class);
        Mockito.when(pool.getInUseConnectionsCount()).thenReturn(2);
        Mockito.when(pool.getIdleConnectionsCount()).thenReturn(3);
        Mockito.when(pool.getFuturesWaitingForConnectionCount()).thenReturn(1);

        // Act
        spiedService.bindPoolMetrics(pool);

        // Assert
        Assertions.assertEquals(2.0, registry.get("jasync.pool.connections.inuse").gauge().value());
        Assertions.assertEquals(3.0, registry.get("jasync.pool.connections.idle").gauge().value());
        Assertions.assertEquals(1.0, registry.get("jasync.pool.connections.waiting").gauge().value());

        // Assert — the gauges poll the live pool rather than snapshotting the value at bind time.
        Mockito.when(pool.getInUseConnectionsCount()).thenReturn(5);
        Assertions.assertEquals(5.0, registry.get("jasync.pool.connections.inuse").gauge().value());
    }

    @Test
    void testBindPoolMetricsEnabledButNotConnectionPoolRegistersNoGauges() {
        // Arrange
        final DefaultJasyncClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();
        final Connection plainConnection = Mockito.mock(Connection.class);

        // Act
        spiedService.bindPoolMetrics(plainConnection);

        // Assert
        Assertions.assertTrue(registry.getMeters().isEmpty());
    }

    @Test
    void testRebindAfterUnbindPollsNewPool() {
        // Arrange
        final DefaultJasyncClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        final ConnectionPool<?> poolA = Mockito.mock(ConnectionPool.class);
        Mockito.when(poolA.getInUseConnectionsCount()).thenReturn(2);
        Mockito.when(poolA.getIdleConnectionsCount()).thenReturn(3);
        Mockito.when(poolA.getFuturesWaitingForConnectionCount()).thenReturn(1);

        // Act — bind against the first (now-stale, e.g. disconnected) pool.
        spiedService.bindPoolMetrics(poolA);

        // Assert — gauges poll poolA.
        Assertions.assertEquals(2.0, registry.get("jasync.pool.connections.inuse").gauge().value());

        // Act — simulate shutdown's unbind step.
        spiedService.unbindPoolMetrics();

        // Assert — this service's own gauges are gone from the shared registry.
        Assertions.assertNull(registry.find("jasync.pool.connections.inuse").gauge());
        Assertions.assertNull(registry.find("jasync.pool.connections.idle").gauge());
        Assertions.assertNull(registry.find("jasync.pool.connections.waiting").gauge());

        final ConnectionPool<?> poolB = Mockito.mock(ConnectionPool.class);
        Mockito.when(poolB.getInUseConnectionsCount()).thenReturn(9);
        Mockito.when(poolB.getIdleConnectionsCount()).thenReturn(4);
        Mockito.when(poolB.getFuturesWaitingForConnectionCount()).thenReturn(0);

        // Act — re-init/repair rebinds against the new pool.
        spiedService.bindPoolMetrics(poolB);

        // Assert — gauge now polls poolB, not the stale poolA.
        Assertions.assertEquals(9.0, registry.get("jasync.pool.connections.inuse").gauge().value());
    }

    /**
     * Reproduces the multi-keyed-pool metrics collision: with >1 {@link
     * DefaultJasyncClientIntegrationService} instance (one per {@code AppbaseClientKey}) sharing
     * the same composite registry, un-tagged gauges register under identical Micrometer Ids, so
     * Micrometer's dedup-by-Id either drops the second pool's gauges or -- worse -- lets one
     * service's {@code unbindPoolMetrics()} remove a still-live sibling's gauges. Each service must
     * tag its gauges with its own stable identity so keyed pools stay independently observable and
     * independently removable.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testBindPoolMetricsIsolatesGaugesAndUnbindAcrossKeyedServices() {
        // Arrange — two integration services with DISTINCT client keys sharing ONE registry.
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultJasyncClientIntegrationServicePoolMetricsTest.class));

        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(registry);

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<Connection, TextValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);

        final AppbaseClientKey<TextValue> keyA = Mockito.mock(AppbaseClientKey.class);
        Mockito.when(keyA.toString()).thenReturn("keyA");
        final AppbaseClientKey<TextValue> keyB = Mockito.mock(AppbaseClientKey.class);
        Mockito.when(keyB.toString()).thenReturn("keyB");

        final DefaultJasyncClientIntegrationService serviceA = Mockito.spy(new DefaultJasyncClientIntegrationService(
                appbaseClientCore, appbaseClientPool, keyA, Mockito.mock(Connection.class)));
        Mockito.doReturn(true).when(serviceA).isDriverMetricsEnabled();

        final DefaultJasyncClientIntegrationService serviceB = Mockito.spy(new DefaultJasyncClientIntegrationService(
                appbaseClientCore, appbaseClientPool, keyB, Mockito.mock(Connection.class)));
        Mockito.doReturn(true).when(serviceB).isDriverMetricsEnabled();

        final ConnectionPool<?> poolA = Mockito.mock(ConnectionPool.class);
        Mockito.when(poolA.getInUseConnectionsCount()).thenReturn(2);

        final ConnectionPool<?> poolB = Mockito.mock(ConnectionPool.class);
        Mockito.when(poolB.getInUseConnectionsCount()).thenReturn(9);

        // Act
        serviceA.bindPoolMetrics(poolA);
        serviceB.bindPoolMetrics(poolB);

        // Assert — both keyed pools are independently observable on the shared registry.
        Assertions.assertEquals(
                2.0, registry.get("jasync.pool.connections.inuse").tags("key", "keyA").gauge().value());
        Assertions.assertEquals(
                9.0, registry.get("jasync.pool.connections.inuse").tags("key", "keyB").gauge().value());

        // Act — service A shuts down; its own gauges are removed.
        serviceA.unbindPoolMetrics();

        // Assert — sibling service B's still-live gauge is unaffected by A's removal.
        Assertions.assertNull(registry.find("jasync.pool.connections.inuse").tags("key", "keyA").gauge());
        Assertions.assertEquals(
                9.0, registry.get("jasync.pool.connections.inuse").tags("key", "keyB").gauge().value());
    }
}
