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

package com.viveworks.appbase.client.service.firebase.auth.accessor.reactive;

import com.google.api.core.ApiFutures;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.accessor.FirebaseAuthAccessorException;
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

import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveFirebaseAuthAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveFirebaseAuthAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the timer/mediator mocks it was built from, so
     * tests can both drive the accessor and verify the shared spine's interactions.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Timer timer;
        final Mediator mediator;

        private CoreFixtures(final AppbaseCore appbaseCore, final Timer timer, final Mediator mediator) {
            this.appbaseCore = appbaseCore;
            this.timer = timer;
            this.mediator = mediator;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveFirebaseAuthAccessorTest.class));

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

            return new CoreFixtures(appbaseCore, timer, mediator);
        }
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<FirebaseAuth, TextValue> mockIntegrationService(
            final FirebaseAuth firebaseAuth) {
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(firebaseAuth);
        return integrationService;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveFirebaseAuthAccessor accessor(
            final CoreFixtures core,
            final Supplier<AppbaseClientIntegrationService<FirebaseAuth, TextValue>> borrow) {
        return new ReactiveFirebaseAuthAccessor(
                core.appbaseCore, mockReactorService(), defaultConfig(), borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    @Test
    void getUserEmitsUserRecord() {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUserAsync("uid-1")).thenReturn(ApiFutures.immediateFuture(userRecord));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.getUser("getUser", "uid-1"))
                .expectNext(userRecord)
                .verifyComplete();
    }

    @Test
    void deleteUserYieldsEmptyCompletingMono() {
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.deleteUserAsync("uid-1"))
                .thenReturn(ApiFutures.<Void>immediateFuture(null));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.deleteUser("deleteUser", "uid-1"))
                .verifyComplete();
    }

    @Test
    void firebaseAuthExceptionMappedToFirebaseAuthAccessorException() {
        final FirebaseAuthException firebaseAuthException = Mockito.mock(FirebaseAuthException.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUserAsync("uid-err"))
                .thenReturn(ApiFutures.immediateFailedFuture(firebaseAuthException));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.getUser("getUser", "uid-err"))
                .expectError(FirebaseAuthAccessorException.class)
                .verify();
    }

    @Test
    void integrationServiceFinalizedOnSuccessAndError() {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuthException firebaseAuthException = Mockito.mock(FirebaseAuthException.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUserAsync("uid-ok")).thenReturn(ApiFutures.immediateFuture(userRecord));
        Mockito.when(firebaseAuth.getUserAsync("uid-err"))
                .thenReturn(ApiFutures.immediateFailedFuture(firebaseAuthException));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        accessor.getUser("getUser", "uid-ok").block();
        accessor.getUser("getUser", "uid-err").onErrorResume(e -> Mono.empty()).block();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void timerStartedForOperation() {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUserAsync("uid-1")).thenReturn(ApiFutures.immediateFuture(userRecord));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveFirebaseAuthAccessor accessor = accessor(core, () -> integrationService);

        accessor.getUser("getUser", "uid-1").block();

        Mockito.verify(core.timer).time();
    }

    @Test
    void commandDelegatesToArbitraryFirebaseAuthCall() {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUserByEmailAsync("a@b.com"))
                .thenReturn(ApiFutures.immediateFuture(userRecord));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.command("command", auth -> auth.getUserByEmailAsync("a@b.com")))
                .expectNext(userRecord)
                .verifyComplete();
    }

    @Test
    void getUserByEmailEmitsUserRecord() {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.getUserByEmailAsync("e@x.com"))
                .thenReturn(ApiFutures.immediateFuture(userRecord));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.getUserByEmail("getUserByEmail", "e@x.com"))
                .expectNext(userRecord)
                .verifyComplete();
    }

    @Test
    void updateUserEmitsUpdatedUserRecord() {
        final UserRecord userRecord = Mockito.mock(UserRecord.class);
        final UserRecord.UpdateRequest updateRequest = Mockito.mock(UserRecord.UpdateRequest.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.updateUserAsync(updateRequest))
                .thenReturn(ApiFutures.immediateFuture(userRecord));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.updateUser("updateUser", updateRequest))
                .expectNext(userRecord)
                .verifyComplete();
    }

    @Test
    void verifyIdTokenEmitsFirebaseToken() {
        final FirebaseToken firebaseToken = Mockito.mock(FirebaseToken.class);
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.verifyIdTokenAsync("token"))
                .thenReturn(ApiFutures.immediateFuture(firebaseToken));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.verifyIdToken("verifyIdToken", "token"))
                .expectNext(firebaseToken)
                .verifyComplete();
    }

    @Test
    void createCustomTokenEmitsToken() {
        final FirebaseAuth firebaseAuth = Mockito.mock(FirebaseAuth.class);
        Mockito.when(firebaseAuth.createCustomTokenAsync("uid"))
                .thenReturn(ApiFutures.immediateFuture("custom-token"));
        final AppbaseClientIntegrationService<FirebaseAuth, TextValue> integrationService =
                mockIntegrationService(firebaseAuth);
        final ReactiveFirebaseAuthAccessor accessor = accessor(CoreFixtures.create(), () -> integrationService);

        StepVerifier.create(accessor.createCustomToken("createCustomToken", "uid"))
                .expectNext("custom-token")
                .verifyComplete();
    }
}
