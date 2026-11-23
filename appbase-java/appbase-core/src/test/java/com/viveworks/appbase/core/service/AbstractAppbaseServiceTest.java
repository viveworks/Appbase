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

package com.viveworks.appbase.core.service;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.AppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AbstractAppbaseServiceTest {
    private static LogService logService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
    }

    @AfterAll
    static void tearDown() {
        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testService() throws Exception {
        final CoreTestFixtures.TestServiceConfiguration testServiceConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("localhost");
        final AppbaseCore appbaseCore = CoreTestFixtures.getAppbaseCore(logService);
        appbaseCore
                .getConfigurationService()
                .putTypedConfiguration(CoreTestFixtures.TestServiceConfiguration.class, testServiceConfiguration);
        final CoreTestFixtures.TestService testService =
                new CoreTestFixtures.DefaultTestService(appbaseCore, testServiceConfiguration);

        final ServiceStatus status = testService.getStatus();
        Assertions.assertNotEquals(ServiceStatus.STARTING, status);
        Assertions.assertNotEquals(ServiceStatus.FAILED, status);
        Assertions.assertNotEquals(ServiceStatus.RELOADING, status);
        Assertions.assertNotEquals(ServiceStatus.SHUTDOWN, status);
        Assertions.assertEquals(ServiceStatus.STARTED, status);
        Assertions.assertFalse(testService.isReloaded());
        Assertions.assertFalse(testService.isShutdown());

        final CoreTestFixtures.TestServiceConfiguration testServiceConfiguration1 =
                new CoreTestFixtures.TestServiceConfiguration("remotehost");
        testService.reload(testServiceConfiguration1);
        Assertions.assertTrue(testService.isReloaded());
        Assertions.assertEquals(ServiceStatus.STARTED, testService.getStatus());

        testService.shutdown();
        Assertions.assertTrue(testService.isShutdown());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, testService.getStatus());
    }

    /**
     * Regression for the re-entrant {@code reload()} guard at {@code AbstractAppbaseService:121-123}
     * (cited by the Lens-5 test-suite audit, finding Q3: {@code RELOADING} was never produced by any
     * test, only ever used as an {@code assertNotEquals} operand that cannot fail independently). The
     * guard reads only {@code statusRef}, so forcing that state directly is the same net effect as a
     * genuine same-thread re-entrant call through the lock -- and it is what lets this test distinguish
     * the guarded implementation from a broken one without racing a second thread.
     */
    @Test
    void reloadWhileAlreadyReloadingReturnsWithoutRunningReloadImplAgain() throws Exception {
        final CoreTestFixtures.TestServiceConfiguration initialConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("localhost");
        final AppbaseCore appbaseCore = CoreTestFixtures.getAppbaseCore(logService);
        appbaseCore
                .getConfigurationService()
                .putTypedConfiguration(CoreTestFixtures.TestServiceConfiguration.class, initialConfiguration);
        final CoreTestFixtures.DefaultTestService testService =
                new CoreTestFixtures.DefaultTestService(appbaseCore, initialConfiguration);

        // Drive the guard's branch directly: statusRef is protected on AbstractAppbaseService, and
        // this test class lives in the same package, so no reflection is needed.
        ((AbstractAppbaseService) testService).statusRef.set(ServiceStatus.RELOADING);

        final CoreTestFixtures.TestServiceConfiguration unseenConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("must-not-be-applied");
        testService.reload(unseenConfiguration);

        // If the guard had not returned early, reloadImpl (-> initImpl) would have run: isReloaded()
        // would flip true, the configuration would become unseenConfiguration, and the status would
        // land on STARTED. None of that may happen.
        Assertions.assertFalse(testService.isReloaded());
        Assertions.assertEquals(initialConfiguration, testService.getConfiguration());
        Assertions.assertEquals(ServiceStatus.RELOADING, testService.getStatus());
    }

    /**
     * Regression for the Lens-3 business-logic audit finding L3-02
     * ({@code internal review record}): {@code shutdown()}'s early-return
     * guard only recognizes {@code SHUTTING_DOWN}, not the terminal {@code SHUTDOWN} state it leaves
     * behind, and the guard is read before the lifecycle lock is acquired. A second call made after the
     * first has fully completed therefore falls through the guard and re-invokes {@code shutdownImpl()}
     * -- a resource-owning subclass could double-close a client. This does not require concurrency to
     * reproduce: two ordinary sequential calls are enough.
     */
    @Test
    void aSecondSequentialShutdownCallDoesNotReinvokeShutdownImpl() throws Exception {
        final CoreTestFixtures.TestServiceConfiguration testServiceConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("localhost");
        final AppbaseCore appbaseCore = CoreTestFixtures.getAppbaseCore(logService);
        appbaseCore
                .getConfigurationService()
                .putTypedConfiguration(CoreTestFixtures.TestServiceConfiguration.class, testServiceConfiguration);

        final AtomicInteger shutdownImplInvocations = new AtomicInteger(0);
        final CoreTestFixtures.DefaultTestService testService =
                new CoreTestFixtures.DefaultTestService(appbaseCore, testServiceConfiguration) {
                    @Override
                    protected void shutdownImpl() {
                        shutdownImplInvocations.incrementAndGet();
                        super.shutdownImpl();
                    }
                };

        testService.shutdown();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, testService.getStatus());
        Assertions.assertEquals(1, shutdownImplInvocations.get(),
                "the first shutdown() call must run shutdownImpl() exactly once");

        testService.shutdown();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, testService.getStatus());
        Assertions.assertEquals(1, shutdownImplInvocations.get(),
                "a second shutdown() call on an already-SHUTDOWN service must be a no-op and must not " +
                        "re-invoke shutdownImpl()");
    }

    /** AUD-14 / AUD-17: reload is valid while started, but cannot resurrect terminal resources. */
    @Test
    void reloadRefusesTerminalShutdownStatesWithoutInvokingReloadImpl() throws Exception {
        final CoreTestFixtures.TestServiceConfiguration initialConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("localhost");
        final AppbaseCore appbaseCore = CoreTestFixtures.getAppbaseCore(logService);
        appbaseCore
                .getConfigurationService()
                .putTypedConfiguration(CoreTestFixtures.TestServiceConfiguration.class, initialConfiguration);

        final AtomicInteger reloadImplInvocations = new AtomicInteger(0);
        final CoreTestFixtures.DefaultTestService testService =
                new CoreTestFixtures.DefaultTestService(appbaseCore, initialConfiguration) {
                    @Override
                    protected void initImpl(final AppbaseManagedServiceConfiguration configuration)
                            throws AppbaseException
                    {
                        reloadImplInvocations.incrementAndGet();
                        super.initImpl(configuration);
                    }
                };

        final CoreTestFixtures.TestServiceConfiguration liveConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("live-reload");
        testService.reload(liveConfiguration);
        Assertions.assertEquals(1, reloadImplInvocations.get(),
                "positive control: a STARTED service must still execute reloadImpl()");
        Assertions.assertEquals(ServiceStatus.STARTED, testService.getStatus());

        testService.shutdown();
        final CoreTestFixtures.TestServiceConfiguration forbiddenConfiguration =
                new CoreTestFixtures.TestServiceConfiguration("must-not-resurrect");
        Assertions.assertThrows(ServiceReloadException.class,
                () -> testService.reload(forbiddenConfiguration));
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, testService.getStatus());
        Assertions.assertEquals(1, reloadImplInvocations.get(),
                "reloadImpl() must not run after terminal SHUTDOWN");

        testService.statusRef.set(ServiceStatus.SHUTTING_DOWN);
        Assertions.assertThrows(ServiceReloadException.class,
                () -> testService.reload(forbiddenConfiguration));
        Assertions.assertEquals(ServiceStatus.SHUTTING_DOWN, testService.getStatus());
        Assertions.assertEquals(1, reloadImplInvocations.get(),
                "reloadImpl() must not run while shutdown is in progress");

        testService.statusRef.set(ServiceStatus.SHUTDOWN_FAILED);
        Assertions.assertThrows(ServiceReloadException.class,
                () -> testService.reload(forbiddenConfiguration));
        Assertions.assertEquals(ServiceStatus.SHUTDOWN_FAILED, testService.getStatus());
        Assertions.assertEquals(1, reloadImplInvocations.get(),
                "reloadImpl() must not run after terminal SHUTDOWN_FAILED");
    }

    @Test
    void terminatedMediatorDoesNotReplaceShutdownOutcomeWithReportingFailure() throws Exception {
        final MetricService metricService = CoreTestFixtures.getMetricService(logService);
        final MediatorService mediatorService = Mockito.mock(MediatorService.class);
        final Mediator mediator = Mockito.mock(Mediator.class);
        Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
        Mockito.when(mediator.getMessageFromPool()).thenAnswer(invocation -> new Message());
        Mockito.doThrow(new RejectedExecutionException("mediator terminated"))
                .when(mediator).asyncHandleMessage(Mockito.any(Message.class), Mockito.eq(true));

        try {
            final TestShutdownService cleanService =
                    new TestShutdownService(metricService, mediatorService, false);
            Assertions.assertDoesNotThrow(cleanService::shutdown);
            Assertions.assertEquals(ServiceStatus.SHUTDOWN, cleanService.getStatus());

            final TestShutdownService failingService =
                    new TestShutdownService(metricService, mediatorService, true);
            Assertions.assertDoesNotThrow(failingService::shutdown);
            Assertions.assertEquals(ServiceStatus.SHUTDOWN_FAILED, failingService.getStatus());
        } finally {
            metricService.shutdown();
        }
    }

    private static final class TestShutdownService extends AbstractAppbaseService {
        private final boolean failShutdown;

        private TestShutdownService(
                final MetricService metricService,
                final MediatorService mediatorService,
                final boolean failShutdown)
        {
            super(AbstractAppbaseServiceTest.logService, metricService, mediatorService);
            this.failShutdown = failShutdown;
        }

        @Override
        protected <C extends AppbaseConfiguration> void reloadImpl(final C configuration) {
            // Not exercised by this shutdown regression.
        }

        @Override
        protected void shutdownImpl() {
            if (failShutdown) {
                throw new IllegalStateException("genuine cleanup failure");
            }
        }

        @Override
        public <C extends AppbaseConfiguration> C getConfiguration() {
            return null;
        }
    }
}
