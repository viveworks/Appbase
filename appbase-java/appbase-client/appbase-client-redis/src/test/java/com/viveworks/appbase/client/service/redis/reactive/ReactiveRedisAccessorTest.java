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

package com.viveworks.appbase.client.service.redis.reactive;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
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

import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveRedisAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveRedisAccessorTest {

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>
            mockIntegrationService(final RedisReactiveCommands<String, String> commands) {
        final StatefulRedisConnection<String, String> connection = Mockito.mock(StatefulRedisConnection.class);
        Mockito.when(connection.reactive()).thenReturn(commands);

        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(connection);
        return integrationService;
    }

    private static AppbaseCore mockAppbaseCore() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(ReactiveRedisAccessorTest.class));

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

        return appbaseCore;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveRedisAccessor accessor(
            final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
                    borrow) {
        return new ReactiveRedisAccessor(mockAppbaseCore(), mockReactorService(),
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    @Test
    void commandRunsAndReturnsValue() {
        final RedisReactiveCommands<String, String> commands = Mockito.mock(RedisReactiveCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final ReactiveRedisAccessor accessor = accessor(() -> integrationService);

        StepVerifier.create(accessor.command("get", cmds -> Mono.just("v")))
                .expectNext("v")
                .verifyComplete();
    }

    @Test
    void integrationServiceReturnedToPoolOnSuccessAndError() {
        final RedisReactiveCommands<String, String> commands = Mockito.mock(RedisReactiveCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final ReactiveRedisAccessor accessor = accessor(() -> integrationService);

        accessor.command("get", cmds -> Mono.just("v")).block();
        accessor.command("get", cmds -> Mono.error(new IllegalStateException("x")))
                .onErrorResume(e -> Mono.empty())
                .block();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void lettuceErrorMappedToRedisAccessorException() {
        final RedisReactiveCommands<String, String> commands = Mockito.mock(RedisReactiveCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final ReactiveRedisAccessor accessor = accessor(() -> integrationService);

        StepVerifier.create(accessor.command("get", cmds -> Mono.error(new RedisException("down"))))
                .expectError(RedisAccessorException.class)
                .verify();
    }

    @Test
    void borrowHappensPerSubscriptionNotAtAssembly() {
        final RedisReactiveCommands<String, String> commands = Mockito.mock(RedisReactiveCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        @SuppressWarnings("unchecked")
        final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
                borrowSupplier = Mockito.mock(Supplier.class);
        Mockito.when(borrowSupplier.get()).thenReturn(integrationService);
        final ReactiveRedisAccessor accessor = accessor(borrowSupplier);

        final Mono<String> unsubscribed = accessor.command("get", cmds -> Mono.just("v"));

        Mockito.verifyNoInteractions(borrowSupplier);
    }
}
