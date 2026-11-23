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

package com.viveworks.appbase.client.service.aerospike.reactive;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * Tests for {@link ReactiveAerospikeAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveAerospikeAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the metric fixtures it was built from, so tests
     * can both drive the accessor and verify the shared spine's interactions (e.g. timer
     * recording).
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Timer timer;
        final Timer.Context timerContext;
        final MetricRegistry metricRegistry;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer,
                              final Timer.Context timerContext, final MetricRegistry metricRegistry) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.timerContext = timerContext;
            this.metricRegistry = metricRegistry;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveAerospikeAccessorTest.class));

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

            return new CoreFixtures(appbaseCore, timer, timerContext, metricRegistry);
        }
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveAerospikeAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final IAerospikeReactorClient reactorClient) {
        return new ReactiveAerospikeAccessor(core.appbaseCore, mockReactorService(), configuration, reactorClient);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    @Test
    void getEmitsKeyRecord() {
        final Key key = new Key("test", "set", "id");
        final KeyRecord keyRecord = new KeyRecord(key, new Record(null, 0, 0));
        final IAerospikeReactorClient reactorClient = Mockito.mock(IAerospikeReactorClient.class);
        Mockito.when(reactorClient.get(key)).thenReturn(Mono.just(keyRecord));
        final ReactiveAerospikeAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), reactorClient);

        StepVerifier.create(accessor.get("get", key))
                .expectNext(keyRecord)
                .verifyComplete();
    }

    @Test
    void getCompletesEmptyForMissingRecordSentinel() {
        final Key key = new Key("test", "set", "missing");
        final IAerospikeReactorClient reactorClient = Mockito.mock(IAerospikeReactorClient.class);
        Mockito.when(reactorClient.get(key)).thenReturn(Mono.just(new KeyRecord(key, null)));
        final ReactiveAerospikeAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), reactorClient);

        StepVerifier.create(accessor.get("getMissing", key))
                .verifyComplete();
    }

    @Test
    void getWithPolicyCompletesEmptyForMissingRecordSentinel() {
        final Key key = new Key("test", "set", "missing-policy");
        final Policy policy = new Policy();
        final IAerospikeReactorClient reactorClient = Mockito.mock(IAerospikeReactorClient.class);
        Mockito.when(reactorClient.get(policy, key)).thenReturn(Mono.just(new KeyRecord(key, null)));
        final ReactiveAerospikeAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), reactorClient);

        StepVerifier.create(accessor.get("getMissingWithPolicy", policy, key))
                .verifyComplete();
    }

    @Test
    void driverExceptionMappedToAerospikeAccessorException() {
        final Key key = new Key("test", "set", "id");
        final IAerospikeReactorClient reactorClient = Mockito.mock(IAerospikeReactorClient.class);
        Mockito.when(reactorClient.get(key))
                .thenReturn(Mono.error(new AerospikeException("driver failure")));
        final ReactiveAerospikeAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), reactorClient);

        StepVerifier.create(accessor.get("get", key))
                .expectError(AerospikeAccessorException.class)
                .verify();
    }

    @Test
    void existsCompletesEmptyWhenRecordAbsent() {
        final Key key = new Key("test", "set", "id");
        final IAerospikeReactorClient reactorClient = Mockito.mock(IAerospikeReactorClient.class);
        Mockito.when(reactorClient.exists(key)).thenReturn(Mono.empty());
        final ReactiveAerospikeAccessor accessor = accessor(CoreFixtures.create(), defaultConfig(), reactorClient);

        StepVerifier.create(accessor.exists("exists", key))
                .verifyComplete();
    }

    @Test
    void timerRecordedForSuccessfulOperation() {
        final Key key = new Key("test", "set", "id");
        final KeyRecord keyRecord = new KeyRecord(key, new Record(null, 0, 0));
        final IAerospikeReactorClient reactorClient = Mockito.mock(IAerospikeReactorClient.class);
        Mockito.when(reactorClient.get(key)).thenReturn(Mono.just(keyRecord));
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveAerospikeAccessor accessor = accessor(core, defaultConfig(), reactorClient);

        accessor.get("get", key).block();

        Mockito.verify(core.metricRegistry).timer(Mockito.anyString());
        Mockito.verify(core.timer).time();
        Mockito.verify(core.timerContext).stop();
    }
}
