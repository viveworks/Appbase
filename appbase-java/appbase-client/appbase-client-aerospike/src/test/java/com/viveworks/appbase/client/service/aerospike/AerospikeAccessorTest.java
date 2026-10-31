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

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AerospikeAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AerospikeAccessorTest {

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
    private static AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue>
            mockIntegrationService(final IAerospikeClient client) {
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(client);
        return integrationService;
    }

    private static Fixture mockFixture() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(AerospikeAccessorTest.class));

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

    private static AerospikeAccessor accessor(
            final AppbaseCore appbaseCore,
            final Supplier<AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue>> borrow) {
        return new AerospikeAccessor(appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    @Test
    void getWithNoPolicyDelegatesWithNullPolicyAndReturnsRecord() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final Key key = new Key("ns", "set", "key");
        final Record record = new Record(null, 0, 0);
        Mockito.when(client.get(null, key)).thenReturn(record);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final Record result = accessor.get("get", key);

        assertSame(record, result);
        Mockito.verify(client).get(null, key);
        Mockito.verify(integrationService, Mockito.times(1)).finalizeOperation();
    }

    @Test
    void getWithPolicyDelegatesWithGivenPolicy() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final Policy policy = new Policy();
        final Key key = new Key("ns", "set", "key");
        final Record record = new Record(null, 0, 0);
        Mockito.when(client.get(policy, key)).thenReturn(record);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final Record result = accessor.get("get", policy, key);

        assertSame(record, result);
        Mockito.verify(client).get(policy, key);
    }

    @Test
    void putWithNoPolicyDelegatesWithNullPolicy() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final Key key = new Key("ns", "set", "key");
        final Bin bin = new Bin("bin", "value");
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.put("put", key, bin);

        Mockito.verify(client).put(null, key, bin);
        Mockito.verify(integrationService, Mockito.times(1)).finalizeOperation();
    }

    @Test
    void putWithPolicyDelegatesWithGivenPolicy() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final WritePolicy policy = new WritePolicy();
        final Key key = new Key("ns", "set", "key");
        final Bin bin = new Bin("bin", "value");
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.put("put", policy, key, bin);

        Mockito.verify(client).put(policy, key, bin);
    }

    @Test
    void deleteWithNoPolicyDelegatesWithNullPolicyAndReturnsResult() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final Key key = new Key("ns", "set", "key");
        Mockito.when(client.delete(null, key)).thenReturn(true);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final boolean result = accessor.delete("delete", key);

        assertTrue(result);
        Mockito.verify(client).delete(null, key);
        Mockito.verify(integrationService, Mockito.times(1)).finalizeOperation();
    }

    @Test
    void deleteWithPolicyDelegatesWithGivenPolicy() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final WritePolicy policy = new WritePolicy();
        final Key key = new Key("ns", "set", "key");
        Mockito.when(client.delete(policy, key)).thenReturn(false);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final boolean result = accessor.delete("delete", policy, key);

        assertFalse(result);
        Mockito.verify(client).delete(policy, key);
    }

    @Test
    void existsWithNoPolicyDelegatesWithNullPolicyAndReturnsResult() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final Key key = new Key("ns", "set", "key");
        Mockito.when(client.exists(null, key)).thenReturn(true);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final boolean result = accessor.exists("exists", key);

        assertTrue(result);
        Mockito.verify(client).exists(null, key);
        Mockito.verify(integrationService, Mockito.times(1)).finalizeOperation();
    }

    @Test
    void operateDelegatesWithGivenPolicyAndReturnsRecord() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final WritePolicy policy = new WritePolicy();
        final Key key = new Key("ns", "set", "key");
        final Operation operation = Operation.get();
        final Record record = new Record(null, 0, 0);
        Mockito.when(client.operate(policy, key, operation)).thenReturn(record);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final Record result = accessor.operate("operate", policy, key, operation);

        assertSame(record, result);
        Mockito.verify(client).operate(policy, key, operation);
    }

    @Test
    void commandRunsAndReturnsValue() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final String result = accessor.command("command", c -> "v");

        assertEquals("v", result);
    }

    @Test
    void clientReturnsBorrowedIAerospikeClient() {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        assertSame(client, accessor.client());
    }

    @Test
    void integrationServiceFinalizedOnSuccessAndError() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.command("command", c -> "v");
        try {
            accessor.command("command", c -> {
                throw new IllegalStateException("x");
            });
        } catch (final Exception ignored) {
            // expected: mapped/rethrown failure, asserted separately by driverErrorMappedToAerospikeAccessorException
        }

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void driverErrorMappedToAerospikeAccessorException() {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final AerospikeAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        assertThrows(AerospikeAccessorException.class, () -> accessor.command("command", c -> {
            throw new AerospikeException("down");
        }));
    }

    @Test
    void metricsRecordedOnCommand() throws Exception {
        final IAerospikeClient client = Mockito.mock(IAerospikeClient.class);
        final AppbaseClientIntegrationService<IAerospikeClient, ClusterIdValue> integrationService
                = mockIntegrationService(client);
        final Fixture fixture = mockFixture();
        final AerospikeAccessor accessor = accessor(fixture.appbaseCore, () -> integrationService);

        accessor.command("command", c -> "v");

        Mockito.verify(fixture.timer).time();
        Mockito.verify(fixture.timerContext).stop();
    }
}
