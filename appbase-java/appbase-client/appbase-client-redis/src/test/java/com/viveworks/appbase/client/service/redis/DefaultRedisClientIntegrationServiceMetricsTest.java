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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the Lettuce command-latency-metrics wiring in {@link DefaultRedisClientIntegrationService}.
 *
 * <p>The integration service's {@code initImpl()} always attempts a real Lettuce connection, which cannot be
 * mocked out (no seam is exposed for the {@code RedisClient}/{@code ClientResources} construction). These tests
 * instead point at an unreachable endpoint (a very short {@code connectTimeoutMs} against a port nothing listens
 * on) so the connection attempt fails fast, and assert the OBSERVABLE seam that matters for this task: whether
 * {@link MetricService#getCompositeMeterRegistry()} was consulted (i.e. Lettuce's recorder would have been bound
 * to Appbase's composite) before that failure. End-to-end verification that a {@code lettuce.command.*} meter
 * actually appears in the registry after a real command belongs in the (currently infra-blocked) integration
 * test — see {@code ReactiveRedisAccessorIT}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultRedisClientIntegrationServiceMetricsTest {

    @SuppressWarnings("unchecked")
    private static AppbaseClientCore mockAppbaseClientCore(final MetricService metricService) {
        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(DefaultRedisClientIntegrationServiceMetricsTest.class));

        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);

        final AppbaseClientCore appbaseClientCore = Mockito.mock(AppbaseClientCore.class);
        Mockito.when(appbaseClientCore.getAppbaseCore()).thenReturn(appbaseCore);
        return appbaseClientCore;
    }

    @SuppressWarnings("unchecked")
    private static DefaultRedisClientIntegrationServiceConfiguration unreachableConfiguration(
            final boolean commandLatencyMetricsEnabled)
    {
        return DefaultRedisClientIntegrationServiceConfiguration.builder()
                .hostPortConfiguration(HostPortConfiguration.builder().host("localhost").port(1).build())
                .connectTimeoutMs(200L)
                .commandLatencyMetricsEnabled(commandLatencyMetricsEnabled)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    void init_withCommandLatencyMetricsEnabled_consultsCompositeMeterRegistry() {
        // Arrange
        final MetricService metricService = Mockito.mock(MetricService.class);
        final MeterRegistry compositeMeterRegistry = new SimpleMeterRegistry();
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(compositeMeterRegistry);

        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(metricService);
        final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<TextValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final DefaultRedisClientIntegrationServiceConfiguration configuration =
                unreachableConfiguration(true);

        // Act — connect fails fast (unreachable port); we only care what happened before that.
        assertThrows(RuntimeException.class, () -> new DefaultRedisClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, configuration));

        // Assert — the composite meter registry was consulted to bind Lettuce's command-latency recorder.
        Mockito.verify(metricService, Mockito.atLeastOnce()).getCompositeMeterRegistry();
    }

    @SuppressWarnings("unchecked")
    @Test
    void init_withCommandLatencyMetricsDisabled_doesNotConsultCompositeMeterRegistry() {
        // Arrange
        final MetricService metricService = Mockito.mock(MetricService.class);
        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(metricService);
        final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<TextValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final DefaultRedisClientIntegrationServiceConfiguration configuration =
                unreachableConfiguration(false);

        // Act
        assertThrows(RuntimeException.class, () -> new DefaultRedisClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, configuration));

        // Assert — the flag being off means the composite registry is never touched during init.
        Mockito.verify(metricService, Mockito.never()).getCompositeMeterRegistry();
    }

    @SuppressWarnings("unchecked")
    @Test
    void init_withCommandLatencyMetricsEnabled_andFailedConnect_shutsDownClientResources() {
        // Arrange — the built ClientResources is only reachable via a package-private test seam
        // (see CapturingRedisClientIntegrationService below), because the half-constructed service
        // instance itself is unreachable once the config-based constructor throws.
        CapturingRedisClientIntegrationService.CAPTURED_CLIENT_RESOURCES.set(null);

        final MetricService metricService = Mockito.mock(MetricService.class);
        final MeterRegistry compositeMeterRegistry = new SimpleMeterRegistry();
        Mockito.when(metricService.getCompositeMeterRegistry()).thenReturn(compositeMeterRegistry);

        final AppbaseClientCore appbaseClientCore = mockAppbaseClientCore(metricService);
        final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool =
                Mockito.mock(AppbaseClientPool.class);
        final AppbaseClientKey<TextValue> appbaseClientKey = Mockito.mock(AppbaseClientKey.class);
        final DefaultRedisClientIntegrationServiceConfiguration configuration =
                unreachableConfiguration(true);

        // Act — connect fails fast (unreachable port).
        assertThrows(RuntimeException.class, () -> new CapturingRedisClientIntegrationService(
                appbaseClientCore, appbaseClientPool, appbaseClientKey, configuration));

        // Assert — the ClientResources ref must have been cleared (i.e. shut down) before the
        // connect failure propagated out of initImpl(). Before the fix, the ref is left holding the
        // never-shut-down ClientResources, leaking its Netty event-loop thread pool for the JVM
        // lifetime; this assertion is RED against the unfixed code and GREEN once the failed-connect
        // path shuts down and clears clientResourcesRef.
        assertNull(CapturingRedisClientIntegrationService.CAPTURED_CLIENT_RESOURCES.get());
    }

    /**
     * Test-only subclass that captures the {@code clientResourcesRef} value into a static field as
     * {@link DefaultRedisClientIntegrationService#initImpl()} unwinds, success or failure.
     *
     * <p>This is necessary because when the config-based constructor's {@code super.init()} call
     * throws (as it does here, via the unchecked {@code RedisConnectionException}), the entire
     * constructor call aborts and no reference to the partially-constructed instance is ever
     * returned to the caller — so the instance's state cannot be inspected directly afterward. The
     * {@code finally} block below runs while the instance is still reachable (during stack unwind),
     * letting the test observe whether {@code clientResourcesRef} was cleared before the failure
     * propagated.
     */
    private static final class CapturingRedisClientIntegrationService
            extends DefaultRedisClientIntegrationService {

        static final AtomicReference<ClientResources> CAPTURED_CLIENT_RESOURCES = new AtomicReference<>();

        CapturingRedisClientIntegrationService(
                final AppbaseClientCore appbaseClientCore,
                final AppbaseClientPool<StatefulRedisConnection<String, String>, TextValue> appbaseClientPool,
                final AppbaseClientKey<TextValue> appbaseClientKey,
                final DefaultRedisClientIntegrationServiceConfiguration configuration)
        {
            super(appbaseClientCore, appbaseClientPool, appbaseClientKey, configuration);
        }

        @Override
        protected void initImpl() throws AppbaseClientException {
            try {
                super.initImpl();
            } finally {
                CAPTURED_CLIENT_RESOURCES.set(getClientResourcesRefForTesting());
            }
        }
    }
}
