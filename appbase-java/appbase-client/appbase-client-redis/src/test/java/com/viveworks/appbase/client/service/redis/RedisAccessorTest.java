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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.redis.reactive.RedisAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link RedisAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class RedisAccessorTest {

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
    private static AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>
            mockIntegrationService(final RedisCommands<String, String> commands) {
        final StatefulRedisConnection<String, String> connection = Mockito.mock(StatefulRedisConnection.class);
        Mockito.when(connection.sync()).thenReturn(commands);

        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(connection);
        return integrationService;
    }

    private static Fixture mockFixture() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(RedisAccessorTest.class));

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

    private static RedisAccessor accessor(
            final AppbaseCore appbaseCore,
            final Supplier<AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue>>
                    borrow) {
        return new RedisAccessor(appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    @Test
    void commandRunsAndReturnsValue() throws Exception {
        final RedisCommands<String, String> commands = Mockito.mock(RedisCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final RedisAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final String result = accessor.command("get", cmds -> "v");

        assertEquals("v", result);
    }

    @Test
    void integrationServiceReturnedToPoolOnSuccessAndError() throws Exception {
        final RedisCommands<String, String> commands = Mockito.mock(RedisCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final RedisAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.command("get", cmds -> "v");
        try {
            accessor.command("get", cmds -> {
                throw new IllegalStateException("x");
            });
        } catch (final Exception ignored) {
            // expected: mapped/rethrown failure, asserted separately by lettuceErrorMappedToRedisAccessorException
        }

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void lettuceErrorMappedToRedisAccessorException() {
        final RedisCommands<String, String> commands = Mockito.mock(RedisCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final RedisAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        assertThrows(RedisAccessorException.class, () -> accessor.command("get", cmds -> {
            throw new RedisException("down");
        }));
    }

    @Test
    void metricsRecordedOnCommand() throws Exception {
        final RedisCommands<String, String> commands = Mockito.mock(RedisCommands.class);
        final AppbaseClientIntegrationService<StatefulRedisConnection<String, String>, TextValue> integrationService
                = mockIntegrationService(commands);
        final Fixture fixture = mockFixture();
        final RedisAccessor accessor = accessor(fixture.appbaseCore, () -> integrationService);

        accessor.command("get", cmds -> "v");

        Mockito.verify(fixture.timer).time();
        Mockito.verify(fixture.timerContext).stop();
    }
}
