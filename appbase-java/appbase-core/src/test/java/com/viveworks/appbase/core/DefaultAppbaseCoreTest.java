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

package com.viveworks.appbase.core;

import com.viveworks.appbase.core.bootstrap.AppbaseBootstrapConfigurationException;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultAppbaseCoreTest {
    private static LogService logService;
    private static DefaultAppbaseCore defaultAppbaseCore1;
    private static DefaultAppbaseCore defaultAppbaseCore2;
    private static DefaultAppbaseCore defaultAppbaseCore3;
    private static DefaultAppbaseCore defaultAppbaseCore4;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
    }

    @AfterAll
    static void tearDown() {
        if (defaultAppbaseCore1 != null) {
            defaultAppbaseCore1.shutdown();
        }

        if (defaultAppbaseCore2 != null) {
            defaultAppbaseCore2.shutdown();
        }

        if (defaultAppbaseCore3 != null) {
            defaultAppbaseCore3.shutdown();
        }

        if (defaultAppbaseCore4 != null) {
            defaultAppbaseCore4.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testConstants() {
        Assertions.assertEquals(
                AppbaseConstants.APPBASE_BASE_DIRECTORY_PROPERTY,
                "appbase.base.directory");
        Assertions.assertNotNull(AppbaseConstants.DEFAULT_APPBASE_BASE_DIRECTORY);
        Assertions.assertNotNull(AppbaseConstants.APPBASE_BASE_DIRECTORY);
        Assertions.assertEquals(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY,
                "appbase.configuration.file.path");
        Assertions.assertEquals(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_ENV_KEY,
                "APPBASE_CONFIGURATION_FILE_PATH");
        Assertions.assertEquals(
                AppbaseConstants.DEFAULT_CONFIGURATION_FILE,
                "classpath:///appbase-core.yaml");
        Assertions.assertEquals(
                AppbaseConstants.APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME_PROPERTY_KEY,
                "appbase.bootstrap.configuration.node.name");
        Assertions.assertEquals(
                AppbaseConstants.APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME_ENV_KEY,
                "APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME");
    }

    @Test
    void testDefaultAppbaseCore() {
        defaultAppbaseCore1 = new DefaultAppbaseCore();
        runTests(defaultAppbaseCore1, false);
        defaultAppbaseCore1.shutdown();

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                new DefaultAppbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
        defaultAppbaseCore2 = new DefaultAppbaseCore(appbaseCoreBootstrap);
        runTests(defaultAppbaseCore2, false);
        defaultAppbaseCore2.shutdown();

        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                CoreTestFixtures.getLuceneDisabledBootstrapConfiguration();
        defaultAppbaseCore3 = new DefaultAppbaseCore(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
        runTests(defaultAppbaseCore3, true);

        defaultAppbaseCore4 = new DefaultAppbaseCore(
                defaultAppbaseCore3.getLogService(),
                defaultAppbaseCore3.getMetricService(),
                defaultAppbaseCore3.getValidationService(),
                defaultAppbaseCore3.getMediatorService(),
                defaultAppbaseCore3.getConfigurationService(),
                defaultAppbaseCore3.getServiceRegistry(),
                defaultAppbaseCore3.getExceptionService(),
                defaultAppbaseCore3.getTracerService(),
                defaultAppbaseCore3.getReactorService(),
                defaultAppbaseCore3.getJmxService(),
                defaultAppbaseCore3.getLuceneService());
        runTests(defaultAppbaseCore4, true);
    }

    @Test
    void testShutdownWithTimeoutSuccessfulAction() {
        final DefaultAppbaseCore core = new DefaultAppbaseCore();
        final Logger logger = core.getLogService().getLogger(DefaultAppbaseCoreTest.class);
        final AtomicBoolean ran = new AtomicBoolean(false);

        final boolean result = core.shutdownWithTimeout(() -> ran.set(true), "TestService", 1000L, logger);

        core.shutdown();

        Assertions.assertTrue(result, "Clean shutdown should return true");
        Assertions.assertTrue(ran.get(), "Shutdown action must have run");
    }

    @Test
    void testShutdownWithTimeoutHungActionReturnsWithinDeadline() throws InterruptedException {
        final DefaultAppbaseCore core = new DefaultAppbaseCore();
        final Logger logger = core.getLogService().getLogger(DefaultAppbaseCoreTest.class);
        final CountDownLatch hangLatch = new CountDownLatch(1);

        final Runnable hungAction = () -> {
            try {
                hangLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        };

        final long startNs = System.nanoTime();
        final boolean result = core.shutdownWithTimeout(hungAction, "HungTestService", 100L, logger);
        final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        hangLatch.countDown();
        core.shutdown();

        Assertions.assertFalse(result, "Hung service should return false");
        Assertions.assertTrue(elapsedMs < 500L,
                "Expected < 500ms for 100ms timeout, got " + elapsedMs + "ms");
    }

    @Test
    void shutdownReportingRejectionDoesNotReplaceActionFailure() {
        final DefaultAppbaseCore core = new DefaultAppbaseCore();
        final Logger rejectingLogger = Mockito.mock(Logger.class);
        Mockito.doThrow(new RejectedExecutionException("mediator terminated"))
                .when(rejectingLogger).error(Mockito.anyString(), Mockito.<Object[]>any());
        final CountDownLatch hangLatch = new CountDownLatch(1);

        try {
            final boolean failedResult = Assertions.assertDoesNotThrow(() -> core.shutdownWithTimeout(
                    () -> {
                        throw new IllegalStateException("genuine shutdown failure");
                    },
                    "FailingTestService", 1000L, rejectingLogger));
            Assertions.assertFalse(failedResult);

            final boolean timedOutResult = Assertions.assertDoesNotThrow(() -> core.shutdownWithTimeout(
                    () -> {
                        try {
                            hangLatch.await(30, TimeUnit.SECONDS);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                        }
                    },
                    "TimedOutTestService", 10L, rejectingLogger));
            Assertions.assertFalse(timedOutResult);
        } finally {
            hangLatch.countDown();
            core.shutdown();
        }
    }

    /**
     * Regression for the Lens-3 business-logic audit finding L3-01
     * ({@code internal-agents-docs/agent-records/quality/implementation-L3-20260928T0118.md}): {@code shutdown()} reads the
     * {@code shutdown} flag with a plain {@code get()} before any lock, and only sets it after every
     * service has been processed. Two concurrent callers can both pass that read before either write
     * happens, so both proceed to shut every service down. A mocked {@link JmxService} whose
     * {@code shutdown()} blocks on a latch pins the first caller mid-sequence (where the real,
     * unfixed code has already read {@code shutdown.get() == false}) so a second, concurrent caller
     * is deterministically given the chance to race past the same unlocked read.
     */
    @Test
    void concurrentShutdownCallsDoNotReinvokeAServicesShutdownTwice() throws Exception {
        final DefaultAppbaseCore seed = new DefaultAppbaseCore();
        final JmxService jmxServiceMock = Mockito.mock(JmxService.class);
        final AtomicInteger jmxShutdownInvocations = new AtomicInteger(0);
        final CountDownLatch releaseLatch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            jmxShutdownInvocations.incrementAndGet();
            releaseLatch.await(10, TimeUnit.SECONDS);
            return null;
        }).when(jmxServiceMock).shutdown();

        final DefaultAppbaseCore core = new DefaultAppbaseCore(
                seed.getLogService(), seed.getMetricService(), seed.getValidationService(),
                seed.getMediatorService(), seed.getConfigurationService(), seed.getServiceRegistry(),
                seed.getExceptionService(), seed.getTracerService(), seed.getReactorService(),
                jmxServiceMock, seed.getLuceneService());

        try {
            final Thread firstShutdown = new Thread(core::shutdown, "L3-01-shutdown-1");
            firstShutdown.start();

            // Wait until the first caller is inside the mocked jmx service's shutdown() -- i.e. it has
            // already read shutdown.get() == false and is mid-sequence, exactly the window L3-01
            // describes -- before starting a second, concurrent caller.
            final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (jmxShutdownInvocations.get() < 1 && System.nanoTime() < deadlineNanos) {
                Thread.sleep(5);
            }
            Assertions.assertEquals(1, jmxShutdownInvocations.get(),
                    "the first shutdown() call must have entered the jmx service's shutdown() by now");

            final Thread secondShutdown = new Thread(core::shutdown, "L3-01-shutdown-2");
            secondShutdown.start();
            // Give the second caller every chance to race past the unlocked `shutdown.get()` read
            // before the first caller's blocked service is released.
            Thread.sleep(200);
            releaseLatch.countDown();

            firstShutdown.join(10_000);
            secondShutdown.join(10_000);
            Assertions.assertFalse(firstShutdown.isAlive(), "first shutdown() call did not complete");
            Assertions.assertFalse(secondShutdown.isAlive(), "second shutdown() call did not complete");

            Assertions.assertEquals(1, jmxShutdownInvocations.get(),
                    "a concurrent second core-level shutdown() call must not re-invoke a service's " +
                            "shutdown() a second time (Lens 3 L3-01)");
        } finally {
            releaseLatch.countDown();
            seed.shutdown();
        }
    }

    /**
     * Lens 3 finding L3-03 ({@code internal-agents-docs/agent-records/quality/implementation-L3-20260928T0118.md}), updated for
     * the owner ruling of 2026-10-10 (relayed via EA-Super): fail-closed config load is required
     * Release 1.0.0 behavior, implemented as a typed
     * {@link AppbaseBootstrapConfigurationException} thrown by
     * {@code DefaultAppbaseCore.readDefaultAppbaseBootstrapConfiguration} -- no silent fallback for
     * operator-selected configuration paths. An earlier measurement pass had left the audited code
     * unchanged because the no-argument constructor only avoided silent defaults by accident: a
     * second, independent reader
     * ({@code DefaultAppbaseCoreBootstrap.newDefaultConfigurationService} ->
     * {@code CoreBootstrapConfigurationLoader.load} -> {@code YamlUtil.readConfigurationNodes})
     * happened to throw, while the one-argument constructor with a caller-supplied bootstrap had no
     * such second reader and really did start silently on all-defaults (see the two tests below).
     * The typed first-reader failure now fires in every constructor before any bootstrap work; the
     * second reader remains as defense-in-depth.
     */
    @Test
    void explicitButUnparsableConfigurationPathFailsTheConstructorInsteadOfSilentlyUsingDefaults()
            throws IOException
    {
        final String propertyKey = AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY;
        final String previousValue = System.getProperty(propertyKey);
        final Path malformedFile = Files.createTempFile("implementation-L3-03-malformed", ".yaml");
        try {
            Files.writeString(malformedFile, "this: [ is not valid yaml {{{");
            System.setProperty(propertyKey, malformedFile.toAbsolutePath().toString());

            Assertions.assertThrows(AppbaseBootstrapConfigurationException.class, DefaultAppbaseCore::new,
                    "an explicit, present-but-unparsable configuration path must fail the constructor " +
                            "with the typed fail-closed exception (Lens 3 L3-03, owner ruling " +
                            "2026-10-10), never silently fall back to defaults");
        } finally {
            Files.deleteIfExists(malformedFile);
            if (previousValue == null) {
                System.clearProperty(propertyKey);
            } else {
                System.setProperty(propertyKey, previousValue);
            }
        }
    }

    /**
     * Failing-first proof for the L3-03 silent-fallback surface the no-argument constructor could
     * never expose: {@code readDefaultAppbaseBootstrapConfiguration} swallowed every load failure
     * for an operator-selected path, and the one-argument constructor given a bootstrap with no
     * configuration path of its own ({@code new DefaultAppbaseCoreBootstrap(registry, null)}, as
     * {@code testDefaultAppbaseCore} builds) never reaches the second independent reader -- so a
     * missing explicit file started the core on all-defaults with nobody noticing. The core must
     * refuse to start instead.
     */
    @Test
    void explicitMissingConfigurationPathRefusesToStartInsteadOfSilentlyUsingDefaults()
            throws IOException
    {
        final Path missingDirectory = Files.createTempDirectory("implementation-L3-03-missing");
        try {
            assertSuppliedBootstrapRefusesToStart(
                    missingDirectory.resolve("does-not-exist.yaml").toAbsolutePath().toString());
        } finally {
            Files.deleteIfExists(missingDirectory.resolve("does-not-exist.yaml"));
            Files.deleteIfExists(missingDirectory);
        }
    }

    /**
     * Same L3-03 fail-closed requirement for a present-but-unparsable explicit path, exercised
     * through the one-argument constructor whose caller-supplied bootstrap has no configuration
     * path of its own -- the exact path where the audited catch block's silent fallback was fully
     * observable.
     */
    @Test
    void explicitUnparsableConfigurationPathRefusesToStartViaSuppliedBootstrap()
            throws IOException
    {
        final Path malformedFile = Files.createTempFile("implementation-L3-03-supplied", ".yaml");
        try {
            Files.writeString(malformedFile, "this: [ is not valid yaml {{{");
            assertSuppliedBootstrapRefusesToStart(malformedFile.toAbsolutePath().toString());
        } finally {
            Files.deleteIfExists(malformedFile);
        }
    }

    private void assertSuppliedBootstrapRefusesToStart(final String configurationFilePath) {
        final String propertyKey = AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY;
        final String previousValue = System.getProperty(propertyKey);
        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                new DefaultAppbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
        final DefaultAppbaseCore[] constructed = new DefaultAppbaseCore[1];
        try {
            System.setProperty(propertyKey, configurationFilePath);
            Assertions.assertThrows(AppbaseBootstrapConfigurationException.class,
                    () -> constructed[0] = new DefaultAppbaseCore(appbaseCoreBootstrap),
                    "an operator-selected configuration path that cannot be loaded must refuse to " +
                            "start the core (Lens 3 L3-03 fail closed), never silently fall back " +
                            "to defaults");
        } finally {
            if (constructed[0] != null) {
                constructed[0].shutdown();
            }
            if (previousValue == null) {
                System.clearProperty(propertyKey);
            } else {
                System.setProperty(propertyKey, previousValue);
            }
        }
    }

    private void runTests(final DefaultAppbaseCore defaultAppbaseCore, final boolean nullLuceneService) {
        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                defaultAppbaseCore1.getAppbaseBootstrapConfiguration();
        runTestsImpl(defaultAppbaseCore, appbaseCoreBootstrapConfiguration, nullLuceneService);

        defaultAppbaseCore1.reload();
        runTestsImpl(defaultAppbaseCore, appbaseCoreBootstrapConfiguration, nullLuceneService);

        defaultAppbaseCore1.reload(appbaseCoreBootstrapConfiguration);
        runTestsImpl(defaultAppbaseCore, appbaseCoreBootstrapConfiguration, nullLuceneService);
    }

    private void runTestsImpl(final DefaultAppbaseCore defaultAppbaseCore,
                              final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration,
                              final boolean nullLuceneService)
    {
        Assertions.assertNotNull(appbaseCoreBootstrapConfiguration);

        Assertions.assertNotNull(defaultAppbaseCore.getConfigurationService());
        Assertions.assertNotNull(defaultAppbaseCore.getExceptionService());
        Assertions.assertNotNull(defaultAppbaseCore.getJmxService());
        Assertions.assertNotNull(defaultAppbaseCore.getLogService());

        if (nullLuceneService) {
            Assertions.assertNull(defaultAppbaseCore.getLuceneService());
        } else {
            Assertions.assertNotNull(defaultAppbaseCore.getLuceneService());
        }
        Assertions.assertNotNull(defaultAppbaseCore.getMediatorService());
        Assertions.assertNotNull(defaultAppbaseCore.getMetricService());
        Assertions.assertNotNull(defaultAppbaseCore.getReactorService());
        Assertions.assertNotNull(defaultAppbaseCore.getServiceRegistry());
        Assertions.assertNotNull(defaultAppbaseCore.getTracerService());
        Assertions.assertNotNull(defaultAppbaseCore.getValidationService());
        Assertions.assertNotNull(defaultAppbaseCore.getAppbaseCoreBootstrap());
        Assertions.assertNotNull(defaultAppbaseCore.getBootstrappedServices());
        Assertions.assertEquals(ServiceStatus.STARTED, defaultAppbaseCore.getStatus());
    }
}
