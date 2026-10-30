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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.metrics.MetricsPolicy;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for the driver-metrics enable/disable seams of {@link DefaultAerospikeClientIntegrationService}:
 * {@code configureDriverMetrics(IAerospikeClient)} and the {@code disableMetrics()} gating in
 * {@code shutdownImpl()}.
 *
 * <p>The service under test is built via the pre-built-client constructor with a Mockito mock
 * {@link AerospikeClient} and mock {@link EventLoops} — this constructor does NOT call
 * {@code init()} / {@code initImpl()}, so no live cluster connection is attempted. Because that
 * constructor leaves the configuration {@code null}, {@link DefaultAerospikeClientIntegrationService
 * #isDriverMetricsEnabled()} is hard-wired to {@code false} for an un-spied instance (mirrors
 * {@code config != null && config.isDriverMetricsEnabled()}); the disabled-path assertions use the
 * service directly, and the enabled-path assertions use a {@link Mockito#spy} that stubs
 * {@code isDriverMetricsEnabled()} to control the gate without wiring a full, connecting config.
 * The end-to-end path — a real {@code driverMetricsEnabled} config flag flowing through
 * {@code initImpl()} to an actual {@code enableMetrics()} call against a live cluster — is exercised
 * by the Task 7 gated integration test, not here.</p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultAerospikeClientIntegrationServiceMetricsTest {
    private MetricService metricService;
    private AerospikeClient aerospikeClient;
    private DefaultAerospikeClientIntegrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultAerospikeClientIntegrationServiceMetricsTest.class));

        metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(new SimpleMeterRegistry());

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<IAerospikeClient, ClusterIdValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final EventLoops eventLoops = Mockito.mock(EventLoops.class);

        aerospikeClient = Mockito.mock(AerospikeClient.class);

        // Pre-built-client constructor does NOT call init() → no live connect / no initImpl.
        service = new DefaultAerospikeClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, eventLoops, aerospikeClient);
    }

    @Test
    void testIsDriverMetricsEnabledFalseWhenConfigIsNull() {
        // Assert — pre-built-client constructor leaves configuration null.
        Assertions.assertFalse(service.isDriverMetricsEnabled());
    }

    @Test
    void testConfigureDriverMetricsDisabledDoesNotEnableOrConsultRegistry() {
        // Act
        service.configureDriverMetrics(Mockito.mock(IAerospikeClient.class));

        // Assert
        Mockito.verify(metricService, Mockito.never()).getCompositeMeterRegistry();
    }

    @Test
    void testConfigureDriverMetricsEnabledCallsEnableMetricsAndConsultsRegistry() {
        // Arrange
        final DefaultAerospikeClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);

        // Act
        spiedService.configureDriverMetrics(client);

        // Assert
        Mockito.verify(metricService).getCompositeMeterRegistry();
        Mockito.verify(client).enableMetrics(Mockito.any(MetricsPolicy.class));
    }

    @Test
    void testShutdownDisabledDoesNotDisableMetricsButClosesClient() {
        // Act
        service.shutdownImpl();

        // Assert
        Mockito.verify(aerospikeClient, Mockito.never()).disableMetrics();
        Mockito.verify(aerospikeClient).close();
    }

    @Test
    void testShutdownEnabledDisablesMetricsBeforeClosingClient() {
        // Arrange
        final DefaultAerospikeClientIntegrationService spiedService = Mockito.spy(service);
        Mockito.doReturn(true).when(spiedService).isDriverMetricsEnabled();

        // Act
        spiedService.shutdownImpl();

        // Assert — the driver-metrics listener is stopped before the client is closed.
        final InOrder inOrder = Mockito.inOrder(aerospikeClient);
        inOrder.verify(aerospikeClient).disableMetrics();
        inOrder.verify(aerospikeClient).close();
    }
}
