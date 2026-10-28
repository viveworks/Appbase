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

package com.viveworks.appbase.client.core.accessor;

import com.viveworks.appbase.client.core.reactive.ReactiveOperationException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
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

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link AbstractAppbaseAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractAppbaseAccessorTest {

    private static final class TestAccessor extends AbstractAppbaseAccessor {
        TestAccessor(final AppbaseCore appbaseCore, final boolean metricsEnabled,
                     final boolean traceEnabled, final boolean exceptionStoreEnabled) {
            super(appbaseCore, metricsEnabled, traceEnabled, exceptionStoreEnabled);
        }

        <T> T call(final String op, final Callable<T> callable) throws Exception {
            return execute(op, callable);
        }
    }

    /**
     * Bundles a fully-wired {@link TestAccessor} with the mocks it was built from, so tests can
     * both drive the accessor and verify the cross-cutting spine's interactions with its
     * dependencies.
     */
    private static final class Fixtures {
        final Timer timer;
        final Timer.Context timerContext;
        final Counter errorCounter;
        final Mediator mediator;
        final TestAccessor accessor;

        private Fixtures(final Timer timer, final Timer.Context timerContext, final Counter errorCounter,
                         final Mediator mediator, final TestAccessor accessor) {
            this.timer = timer;
            this.timerContext = timerContext;
            this.errorCounter = errorCounter;
            this.mediator = mediator;
            this.accessor = accessor;
        }

        static Fixtures create(final boolean metricsEnabled, final boolean traceEnabled,
                               final boolean exceptionStoreEnabled) {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(AbstractAppbaseAccessorTest.class));

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
            // Message/ExceptionMessage are final classes (no inline mock-maker configured in this
            // repo), so MessageFactory.exceptionMessage needs a real pooled Message instance.
            Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

            final TestAccessor accessor =
                    new TestAccessor(appbaseCore, metricsEnabled, traceEnabled, exceptionStoreEnabled);
            return new Fixtures(timer, timerContext, errorCounter, mediator, accessor);
        }
    }

    @Test
    void executeRecordsTimerAndReturnsValue() throws Exception {
        final Fixtures f = Fixtures.create(true, false, false);
        final String out = f.accessor.call("get", () -> "v");
        assertEquals("v", out);
        Mockito.verify(f.timer).time();
        Mockito.verify(f.timerContext).stop();
    }

    @Test
    void executeOnFailureIncrementsErrorCounterAndRethrows() {
        final Fixtures f = Fixtures.create(true, false, false);
        final IllegalStateException original = new IllegalStateException("boom");
        final ReactiveOperationException mapped = assertThrows(ReactiveOperationException.class,
                () -> f.accessor.call("get", () -> { throw original; }));
        assertSame(original, mapped.getCause());
        Mockito.verify(f.errorCounter).inc();
    }

    @Test
    void reportNotableHitsMediatorOnlyWhenEnabledAndNotable() throws MediatorException {
        final Fixtures f = Fixtures.create(false, false, true);
        assertThrows(RuntimeException.class,
                () -> f.accessor.call("get", () -> { throw new RuntimeException("x"); }));
        Mockito.verify(f.mediator).asyncHandleMessage(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void exceptionStoreDisabledDoesNotTouchMediator() {
        final Fixtures f = Fixtures.create(true, false, false);
        assertThrows(RuntimeException.class,
                () -> f.accessor.call("get", () -> { throw new RuntimeException("x"); }));
        Mockito.verifyNoInteractions(f.mediator);
    }
}
