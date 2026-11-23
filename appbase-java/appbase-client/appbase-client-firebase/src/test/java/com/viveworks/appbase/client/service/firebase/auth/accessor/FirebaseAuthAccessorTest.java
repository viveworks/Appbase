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

package com.viveworks.appbase.client.service.firebase.auth.accessor;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
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
 * Tests for {@link FirebaseAuthAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class FirebaseAuthAccessorTest {

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
    private static AppbaseClientIntegrationService<FirebaseAuth, TextValue> mockIntegrationService(
            final FirebaseAuth firebaseAuth) {
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(firebaseAuth);
        return integrationService;
    }

    private static Fixture mockFixture() {
        final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
        Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                .thenReturn(LoggerFactory.getLogger(FirebaseAuthAccessorTest.class));

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

    private static FirebaseAuthAccessor accessor(
            final AppbaseCore appbaseCore,
            final Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow) {
        return new FirebaseAuthAccessor(appbaseCore,
                DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build(), borrow);
    }

    @Test
    void getUserRunsAndReturnsValue() throws Exception {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUser("uid-1")).thenReturn(userRecord);
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = mockIntegrationService(firebaseAuth);
        final FirebaseAuthAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final UserRecord result = accessor.getUser("getUser", "uid-1");

        assertEquals(userRecord, result);
    }

    @Test
    void createUserRunsAndReturnsValue() throws Exception {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final UserRecord.CreateRequest request = new UserRecord.CreateRequest();
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.createUser(request)).thenReturn(userRecord);
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = mockIntegrationService(firebaseAuth);
        final FirebaseAuthAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        final UserRecord result = accessor.createUser("createUser", request);

        assertEquals(userRecord, result);
    }

    @Test
    void commandRunsCheckedFirebaseAuthLambda() throws Exception {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUser("uid-1")).thenReturn(userRecord);
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = mockIntegrationService(firebaseAuth);
        final FirebaseAuthAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        // a.getUser(uid) throws checked FirebaseAuthException; compiles only because command
        // takes a FirebaseAuthCommand, not a java.util.function.Function.
        final UserRecord result = accessor.command("get", a -> a.getUser("uid-1"));

        assertEquals(userRecord, result);
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void integrationServiceFinalizedOnSuccessAndError() throws Exception {
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUser("uid-1")).thenReturn(Mockito.mock(UserRecord.class));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = mockIntegrationService(firebaseAuth);
        final FirebaseAuthAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        accessor.getUser("getUser", "uid-1");
        try {
            accessor.command("select", a -> {
                throw new IllegalStateException("x");
            });
        } catch (final Exception ignored) {
            // expected: mapped/rethrown failure, asserted separately below
        }

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void firebaseAuthExceptionMappedToFirebaseAuthAccessorException() throws Exception {
        final FirebaseAuthException firebaseAuthException = Mockito.mock(FirebaseAuthException.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUser("uid-err")).thenThrow(firebaseAuthException);
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = mockIntegrationService(firebaseAuth);
        final FirebaseAuthAccessor accessor = accessor(mockFixture().appbaseCore, () -> integrationService);

        assertThrows(FirebaseAuthAccessorException.class, () -> accessor.getUser("getUser", "uid-err"));
        Mockito.verify(integrationService).finalizeOperation();
    }

    @Test
    void metricsRecordedOnCommand() throws Exception {
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUser("uid-1")).thenReturn(Mockito.mock(UserRecord.class));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService
                = mockIntegrationService(firebaseAuth);
        final Fixture fixture = mockFixture();
        final FirebaseAuthAccessor accessor = accessor(fixture.appbaseCore, () -> integrationService);

        accessor.getUser("getUser", "uid-1");

        Mockito.verify(fixture.timer).time();
        Mockito.verify(fixture.timerContext).stop();
    }
}
