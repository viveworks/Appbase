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

package com.viveworks.appbase.client.service.kafka.producer;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Unit tests for the driver-metrics binding seam of {@link DefaultKafkaProducerClientIntegrationService}.
 *
 * <p>Exercises the package-private {@code bindDriverMetrics(Producer, boolean)} helper in isolation —
 * never runs {@code initImpl()} (which would need the whole config/pool graph). The seam is asserted by
 * verifying whether Appbase's shared Micrometer composite registry (owned by {@code MetricService}) is
 * consulted. A mock {@link Producer} whose {@code metrics()} returns an empty map stands in for a live
 * driver client — {@link KafkaClientMetrics#bindTo} only registers gauges that poll {@code metrics()}.</p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultKafkaProducerClientIntegrationServiceMetricsTest {

    private MetricService metricService;
    private Producer<String, String> producer;
    private DefaultKafkaProducerClientIntegrationService<String, String> service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultKafkaProducerClientIntegrationServiceMetricsTest.class));

        metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(new SimpleMeterRegistry());

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);

        final AppbaseClientPool<Producer<String, String>, ClusterIdValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);

        producer = Mockito.mock(Producer.class);
        Mockito.when(producer.metrics()).thenReturn(Map.of());

        // Constructor with a pre-built Producer does NOT call init() → no live connect / no initImpl.
        service = new DefaultKafkaProducerClientIntegrationService<>(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, producer);
    }

    @Test
    void testDriverMetricsEnabledBindsCompositeMeterRegistry() {
        // Act
        final KafkaClientMetrics binder = service.bindDriverMetrics(producer, true);

        // Assert — the shared composite registry was consulted (the binder was bound onto it).
        Mockito.verify(metricService).getCompositeMeterRegistry();
        Assertions.assertNotNull(binder);
        binder.close();
    }

    @Test
    void testDriverMetricsDisabledDoesNotBindCompositeMeterRegistry() {
        // Act
        final KafkaClientMetrics binder = service.bindDriverMetrics(producer, false);

        // Assert — no registry is consulted and no binder is created when driver metrics are disabled.
        Mockito.verify(metricService, Mockito.never()).getCompositeMeterRegistry();
        Assertions.assertNull(binder);
    }
}
