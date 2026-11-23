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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientIntegrationServiceConfiguration;
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
 * Unit tests for the driver-metrics wiring of {@link DefaultCassandraClientIntegrationService}.
 *
 * <p>Exercises {@code applyProgrammaticDriverConfig} in isolation — never calls
 * {@code sessionBuilder.build()} (which would connect to a live Cassandra cluster). The seam is
 * asserted by verifying whether Appbase's shared Micrometer composite registry is consulted.</p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultCassandraClientIntegrationServiceMetricsTest {

    private AppbaseClientCore appbaseClientCore;
    private MetricService metricService;
    private DefaultCassandraClientIntegrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultCassandraClientIntegrationServiceMetricsTest.class));

        metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(new SimpleMeterRegistry());

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<CqlSession, ClusterIdValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final CqlSession cqlSession = Mockito.mock(CqlSession.class);

        // Constructor with a pre-built CqlSession does NOT call init() → no live connect.
        service = new DefaultCassandraClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, cqlSession);
    }

    @Test
    void testDriverMetricsEnabledBindsCompositeMeterRegistry() {
        // Arrange
        final DefaultCassandraClientIntegrationServiceConfiguration cfg =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(true)
                        .build();
        final AppbaseCassandraSessionBuilder sessionBuilder =
                new AppbaseCassandraSessionBuilder(appbaseClientCore);

        // Act
        service.applyProgrammaticDriverConfig(sessionBuilder, cfg);

        // Assert — the shared composite registry was consulted (bound onto the session builder).
        Mockito.verify(metricService).getCompositeMeterRegistry();
    }

    @Test
    void testDriverMetricsDisabledDoesNotBindCompositeMeterRegistry() {
        // Arrange
        final DefaultCassandraClientIntegrationServiceConfiguration cfg =
                DefaultCassandraClientIntegrationServiceConfiguration.builder()
                        .driverMetricsEnabled(false)
                        .build();
        final AppbaseCassandraSessionBuilder sessionBuilder =
                new AppbaseCassandraSessionBuilder(appbaseClientCore);

        // Act
        service.applyProgrammaticDriverConfig(sessionBuilder, cfg);

        // Assert — no registry is consulted when driver metrics are disabled.
        Mockito.verify(metricService, Mockito.never()).getCompositeMeterRegistry();
        Assertions.assertNotNull(sessionBuilder);
    }
}
