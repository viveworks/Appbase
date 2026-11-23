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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for the connection-state metrics bridge seam of {@link
 * DefaultZooKeeperClientIntegrationService}: {@code bindConnectionStateMetrics(CuratorFramework)}.
 *
 * <p>The service under test is built via the pre-built-client constructor with a mocked Curator
 * {@link CuratorFramework} -- this constructor does NOT call {@code init()}/{@code initImpl()}, so
 * no live ZooKeeper connection is attempted. Because that constructor leaves the configuration
 * {@code null}, {@link DefaultZooKeeperClientIntegrationService#isDriverMetricsEnabled()} is
 * hard-wired to {@code false} for an un-spied instance (mirrors {@code config != null &&
 * config.isDriverMetricsEnabled()}); the disabled-path assertion uses the service directly, and the
 * enabled-path assertions use a {@link Mockito#spy} that stubs {@code isDriverMetricsEnabled()} to
 * control the gate without wiring a full, connecting config. Mirrors {@code
 * DefaultJasyncClientIntegrationServicePoolMetricsTest}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultZooKeeperClientIntegrationServiceConnectionMetricsTest {
    private SimpleMeterRegistry registry;
    private DefaultZooKeeperClientIntegrationService service;
    private CuratorFramework curatorFramework;
    private Listenable<ConnectionStateListener> connectionStateListenable;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultZooKeeperClientIntegrationServiceConnectionMetricsTest.class));

        registry = new SimpleMeterRegistry();
        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(registry);

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<CuratorFramework, ClusterIdValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);

        curatorFramework = Mockito.mock(CuratorFramework.class);
        connectionStateListenable = Mockito.mock(Listenable.class);
        Mockito.when(curatorFramework.getConnectionStateListenable()).thenReturn(connectionStateListenable);

        // Pre-built-client constructor does NOT call init() -> no live connect / no initImpl.
        service = new DefaultZooKeeperClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, curatorFramework);
    }

    @Test
    void testBindConnectionStateMetricsDisabledRegistersNoGaugeAndDoesNotAddListener() {
        // Arrange -- un-spied service: pre-built constructor leaves configuration null.

        // Act
        service.bindConnectionStateMetrics(curatorFramework);

        // Assert
        Assertions.assertTrue(registry.getMeters().isEmpty());
        Mockito.verifyNoInteractions(connectionStateListenable);
    }

    @Test
    void testBindConnectionStateMetricsEnabledRegistersKeyTaggedGaugeAndListenerDrivesValue() {
        // Arrange
        final DefaultZooKeeperClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        final ArgumentCaptor<ConnectionStateListener> listenerCaptor =
                ArgumentCaptor.forClass(ConnectionStateListener.class);

        // Act
        spiedService.bindConnectionStateMetrics(curatorFramework);

        // Assert -- gauge registered at the initial (unknown) state.
        Assertions.assertEquals(0.0, registry.get("zookeeper.connection.state").gauge().value());

        Mockito.verify(connectionStateListenable).addListener(listenerCaptor.capture());
        final ConnectionStateListener listener = listenerCaptor.getValue();

        // Act -- drive the captured listener with LOST.
        listener.stateChanged(curatorFramework, ConnectionState.LOST);

        // Assert -- the gauge reflects the live listener state, not a snapshot at bind time.
        Assertions.assertEquals(3.0, registry.get("zookeeper.connection.state").gauge().value());
    }

    @Test
    void testUnbindConnectionStateMetricsRemovesGaugeAndListener() {
        // Arrange
        final DefaultZooKeeperClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        final ArgumentCaptor<ConnectionStateListener> listenerCaptor =
                ArgumentCaptor.forClass(ConnectionStateListener.class);
        spiedService.bindConnectionStateMetrics(curatorFramework);
        Mockito.verify(connectionStateListenable).addListener(listenerCaptor.capture());
        final ConnectionStateListener listener = listenerCaptor.getValue();

        // Act
        spiedService.unbindConnectionStateMetrics();

        // Assert -- this service's own gauge is gone from the shared registry.
        Assertions.assertNull(registry.find("zookeeper.connection.state").gauge());

        // Assert -- the listener is removed from the live client.
        Mockito.verify(connectionStateListenable).removeListener(listener);
    }

    @Test
    void testRebindAfterUnbindRegistersFreshListenerThatDrivesTheGauge() {
        // Arrange
        final DefaultZooKeeperClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        // Act -- bind, drive to LOST, then unbind (simulates shutdown's unbind step).
        spiedService.bindConnectionStateMetrics(curatorFramework);
        final ArgumentCaptor<ConnectionStateListener> firstCaptor =
                ArgumentCaptor.forClass(ConnectionStateListener.class);
        Mockito.verify(connectionStateListenable).addListener(firstCaptor.capture());
        final ConnectionStateListener firstListener = firstCaptor.getValue();
        firstListener.stateChanged(curatorFramework, ConnectionState.LOST);
        Assertions.assertEquals(3.0, registry.get("zookeeper.connection.state").gauge().value());

        spiedService.unbindConnectionStateMetrics();
        Assertions.assertNull(registry.find("zookeeper.connection.state").gauge());
        Mockito.verify(connectionStateListenable).removeListener(firstListener);

        // Act -- re-init/repair rebinds a fresh listener against the (possibly new) client.
        Mockito.clearInvocations(connectionStateListenable);
        spiedService.bindConnectionStateMetrics(curatorFramework);

        // Assert -- rebound gauge reports the reset/unknown state immediately, not the prior
        // client's stale LOST value, until the first new listener event fires.
        Assertions.assertEquals(0.0, registry.get("zookeeper.connection.state").gauge().value());

        final ArgumentCaptor<ConnectionStateListener> secondCaptor =
                ArgumentCaptor.forClass(ConnectionStateListener.class);
        Mockito.verify(connectionStateListenable).addListener(secondCaptor.capture());
        final ConnectionStateListener secondListener = secondCaptor.getValue();

        // Assert -- the newly-registered listener drives the re-registered gauge.
        secondListener.stateChanged(curatorFramework, ConnectionState.CONNECTED);
        Assertions.assertEquals(1.0, registry.get("zookeeper.connection.state").gauge().value());
    }

    /**
     * Reproduces the multi-keyed-client metrics collision: with >1 {@link
     * DefaultZooKeeperClientIntegrationService} instance (one per {@code AppbaseClientKey}) sharing
     * the same composite registry, un-tagged gauges register under identical Micrometer Ids, so
     * Micrometer's dedup-by-Id either drops the second client's gauge or -- worse -- lets one
     * service's {@code unbindConnectionStateMetrics()} remove a still-live sibling's gauge. Each
     * service must tag its gauge with its own stable identity so keyed clients stay independently
     * observable and independently removable.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testBindConnectionStateMetricsIsolatesGaugeAndUnbindAcrossKeyedServices() {
        // Arrange -- two integration services with DISTINCT client keys sharing ONE registry.
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultZooKeeperClientIntegrationServiceConnectionMetricsTest.class));

        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(registry);

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<CuratorFramework, ClusterIdValue> appbaseClientPool = Mockito.mock(AppbaseClientPool.class);

        final AppbaseClientKey<ClusterIdValue> keyA = Mockito.mock(AppbaseClientKey.class);
        Mockito.when(keyA.toString()).thenReturn("keyA");
        final AppbaseClientKey<ClusterIdValue> keyB = Mockito.mock(AppbaseClientKey.class);
        Mockito.when(keyB.toString()).thenReturn("keyB");

        final CuratorFramework curatorFrameworkA = Mockito.mock(CuratorFramework.class);
        final Listenable<ConnectionStateListener> listenableA = Mockito.mock(Listenable.class);
        Mockito.when(curatorFrameworkA.getConnectionStateListenable()).thenReturn(listenableA);

        final CuratorFramework curatorFrameworkB = Mockito.mock(CuratorFramework.class);
        final Listenable<ConnectionStateListener> listenableB = Mockito.mock(Listenable.class);
        Mockito.when(curatorFrameworkB.getConnectionStateListenable()).thenReturn(listenableB);

        final DefaultZooKeeperClientIntegrationService serviceA =
                Mockito.spy(new DefaultZooKeeperClientIntegrationService(
                        appbaseClientCore, appbaseClientPool, keyA, curatorFrameworkA));
        Mockito.doReturn(true).when(serviceA).isDriverMetricsEnabled();

        final DefaultZooKeeperClientIntegrationService serviceB =
                Mockito.spy(new DefaultZooKeeperClientIntegrationService(
                        appbaseClientCore, appbaseClientPool, keyB, curatorFrameworkB));
        Mockito.doReturn(true).when(serviceB).isDriverMetricsEnabled();

        // Act
        serviceA.bindConnectionStateMetrics(curatorFrameworkA);
        serviceB.bindConnectionStateMetrics(curatorFrameworkB);

        // Assert -- both keyed clients are independently observable on the shared registry.
        Assertions.assertNotNull(registry.get("zookeeper.connection.state").tags("key", "keyA").gauge());
        Assertions.assertNotNull(registry.get("zookeeper.connection.state").tags("key", "keyB").gauge());

        // Act -- service A shuts down; its own gauge is removed.
        serviceA.unbindConnectionStateMetrics();

        // Assert -- sibling service B's still-live gauge is unaffected by A's removal.
        Assertions.assertNull(registry.find("zookeeper.connection.state").tags("key", "keyA").gauge());
        Assertions.assertNotNull(registry.get("zookeeper.connection.state").tags("key", "keyB").gauge());
    }
}
