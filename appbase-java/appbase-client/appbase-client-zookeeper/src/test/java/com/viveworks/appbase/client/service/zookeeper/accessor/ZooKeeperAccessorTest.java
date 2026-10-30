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

package com.viveworks.appbase.client.service.zookeeper.accessor;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.zookeeper.KeeperException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ZooKeeperAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ZooKeeperAccessorTest {

    /**
     * Holds an accessor together with the mocks backing its metrics, so tests can assert on
     * timer/counter interactions without re-deriving them from a fresh mock.
     */
    private static final class Fixture {
        private final AppbaseCore appbaseCore;
        private final Timer timer;
        private final Timer.Context timerContext;
        private final Counter errorCounter;

        private Fixture(final AppbaseCore appbaseCore, final Timer timer,
                         final Timer.Context timerContext, final Counter errorCounter) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.errorCounter = errorCounter;
        }
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>
            mockIntegrationService(final CuratorFramework client) {
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(client);
        return integrationService;
    }

    private static Fixture mockFixture() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(ZooKeeperAccessorTest.class));

        final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
        final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
        final Timer timer = Mockito.mock(Timer.class);
        final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        final Counter errorCounter = Mockito.mock(Counter.class);
        Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
        Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
        Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(errorCounter);
        Mockito.when(timer.time()).thenReturn(timerContext);

        final TracerService tracerService = Mockito.mock(TracerService.class);
        final Tracer tracer = Mockito.mock(Tracer.class);
        final Tracer.SpanBuilder spanBuilder = Mockito.mock(Tracer.SpanBuilder.class);
        final Span span = Mockito.mock(Span.class);
        Mockito.when(appbaseCore.getTracerService()).thenReturn(tracerService);
        Mockito.when(tracerService.getTracer()).thenReturn(tracer);
        Mockito.when(tracer.buildSpan(Mockito.anyString())).thenReturn(spanBuilder);
        Mockito.when(spanBuilder.start()).thenReturn(span);

        final MediatorService mediatorService = Mockito.mock(MediatorService.class);
        final Mediator mediator = Mockito.mock(Mediator.class);
        Mockito.when(appbaseCore.getMediatorService()).thenReturn(mediatorService);
        Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
        Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

        return new Fixture(appbaseCore, timer, timerContext, errorCounter);
    }

    private static ZooKeeperAccessor accessor(
            final AppbaseCore appbaseCore,
            final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow) {
        return new ZooKeeperAccessor(appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    @Test
    void getDataRunsAndReturnsValue() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final GetDataBuilder getDataBuilder = Mockito.mock(GetDataBuilder.class);
        final byte[] data = {1, 2, 3};
        Mockito.when(client.getData()).thenReturn(getDataBuilder);
        Mockito.when(getDataBuilder.forPath("/a")).thenReturn(data);
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final ZooKeeperAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final byte[] result = accessor.getData("getData", "/a");

        assertArrayEquals(data, result);
    }

    @Test
    void createRunsAndReturnsValue() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final CreateBuilder createBuilder = Mockito.mock(CreateBuilder.class);
        Mockito.when(client.create()).thenReturn(createBuilder);
        Mockito.when(createBuilder.forPath("/a", "v".getBytes())).thenReturn("/a");
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final ZooKeeperAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final String result = accessor.create("create", "/a", "v".getBytes());

        assertEquals("/a", result);
    }

    @Test
    void commandRunsAndReturnsValue() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final ZooKeeperAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final String result = accessor.command("select", cf -> "v");

        assertEquals("v", result);
    }

    @Test
    void commandRunsCheckedCuratorLambda() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final GetDataBuilder getDataBuilder = Mockito.mock(GetDataBuilder.class);
        final byte[] data = {4, 5, 6};
        Mockito.when(client.getData()).thenReturn(getDataBuilder);
        Mockito.when(getDataBuilder.forPath("/a")).thenReturn(data);
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final ZooKeeperAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        // c.getData().forPath("/a") throws checked Exception; compiles only because command takes
        // a ZooKeeperCommand, not a java.util.function.Function.
        final byte[] result = accessor.command("get", c -> c.getData().forPath("/a"));

        assertArrayEquals(data, result);
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void integrationServiceReturnedToPoolOnSuccessAndError() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final ZooKeeperAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.command("select", cf -> "v");
        try {
            accessor.command("select", cf -> {
                throw new IllegalStateException("x");
            });
        } catch (final Exception ignored) {
            // expected: mapped/rethrown failure, asserted separately by keeperExceptionMappedToZooKeeperAccessorException
        }

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void keeperExceptionMappedToZooKeeperAccessorException() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final GetDataBuilder getDataBuilder = Mockito.mock(GetDataBuilder.class);
        Mockito.when(client.getData()).thenReturn(getDataBuilder);
        Mockito.when(getDataBuilder.forPath(Mockito.anyString()))
                .thenThrow(KeeperException.create(KeeperException.Code.NONODE));
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final ZooKeeperAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        assertThrows(ZooKeeperAccessorException.class, () -> accessor.getData("getData", "/a"));
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void metricsRecordedOnCommand() throws Exception {
        final CuratorFramework client = Mockito.mock(CuratorFramework.class);
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final Fixture fixture = mockFixture();
        final ZooKeeperAccessor accessor = accessor(fixture.appbaseCore, () -> integrationService);

        accessor.command("select", cf -> "v");

        Mockito.verify(fixture.timer).time();
        Mockito.verify(fixture.timerContext).stop();
    }
}
