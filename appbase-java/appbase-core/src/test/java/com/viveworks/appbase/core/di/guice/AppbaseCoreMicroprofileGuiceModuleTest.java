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

package com.viveworks.appbase.core.di.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.core.microprofile.AppbaseCoreMicroprofile;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationServiceImpl;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.DefaultReadinessChecker;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ReadinessChecker;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistryImpl;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseCoreMicroprofileGuiceModuleTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultServiceRegistry defaultServiceRegistry;
    private static AppbaseCoreMicroprofile appbaseCoreMicroprofile;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService);

        final SchedulableConfiguration schedulableConfiguration = SchedulableConfiguration
                .builder()
                .initialDelay(100L)
                .delay(10L)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        final DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration =
                DefaultServiceRegistryConfiguration
                        .builder()
                        .enablePolling(true)
                        .registerAsConfigurationListener(true)
                        .pollingSchedulableConfiguration(schedulableConfiguration)
                        .build();
        configurationService.putTypedConfiguration(
                DefaultServiceRegistryConfiguration.class, defaultServiceRegistryConfiguration);

        final ReadinessChecker readinessChecker = new DefaultReadinessChecker();
        defaultServiceRegistry = new DefaultServiceRegistry(
                logService, metricService, mediatorService, validationService, configurationService, readinessChecker);
    }

    @AfterAll
    static void tearDown() {
        if (appbaseCoreMicroprofile != null) {
            appbaseCoreMicroprofile.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (defaultServiceRegistry != null) {
            defaultServiceRegistry.shutdown();
        }
    }

    @Test
    void testConfigure() {
        final AppbaseCoreGuiceModule appbaseCoreGuiceModule = new AppbaseCoreGuiceModule();

        final AppbaseCoreMicroprofileGuiceModule appbaseCoreMicroprofileGuiceModule1 =
                new AppbaseCoreMicroprofileGuiceModule();
        runTests(appbaseCoreGuiceModule, appbaseCoreMicroprofileGuiceModule1, false);

        final MicroprofileConfigurationService microprofileConfigurationService =
                new MicroprofileConfigurationServiceImpl(configurationService);
        final MicroprofileServiceRegistry microprofileServiceRegistry =
                new MicroprofileServiceRegistryImpl(defaultServiceRegistry);
        final AppbaseCoreMicroprofileGuiceModule appbaseCoreMicroprofileGuiceModule2 =
                new AppbaseCoreMicroprofileGuiceModule(microprofileConfigurationService, microprofileServiceRegistry);
        runTests(appbaseCoreGuiceModule, appbaseCoreMicroprofileGuiceModule2, true);
    }

    private void runTests(
            final AppbaseCoreGuiceModule appbaseCoreGuiceModule,
            final AppbaseCoreMicroprofileGuiceModule appbaseCoreMicroprofileGuiceModule,
            final boolean testAppbaseCoreMicroprofile)
    {
        final Injector injector = Guice.createInjector(
                appbaseCoreGuiceModule,
                appbaseCoreMicroprofileGuiceModule);

        final LogService injectedLogService = injector.getInstance(LogService.class);
        Assertions.assertNotNull(injectedLogService);

        final MetricService injectedMetricService = injector.getInstance(MetricService.class);
        Assertions.assertNotNull(injectedMetricService);

        final ValidationService localValidationService = injector.getInstance(ValidationService.class);
        Assertions.assertNotNull(localValidationService);

        final ConfigurationService boundConfigurationService = injector.getInstance(ConfigurationService.class);
        Assertions.assertNotNull(boundConfigurationService);

        final MicroprofileConfigurationService localMicroprofileConfigurationService =
                injector.getInstance(MicroprofileConfigurationService.class);
        Assertions.assertNotNull(localMicroprofileConfigurationService);

        final MediatorService localMediatorService = injector.getInstance(MediatorService.class);
        Assertions.assertNotNull(localMediatorService);
        Assertions.assertTrue(localMediatorService.isInitWithConfigurationFile());
        Assertions.assertFalse(localMediatorService.isInitWithDefaultConfiguration());
        Assertions.assertNotNull(localMediatorService.getConfigurationFilePath());

        final ServiceRegistry serviceRegistry = injector.getInstance(ServiceRegistry.class);
        Assertions.assertNotNull(serviceRegistry);

        final MicroprofileServiceRegistry microprofileServiceRegistry =
                injector.getInstance(MicroprofileServiceRegistry.class);
        Assertions.assertNotNull(microprofileServiceRegistry);

        final ExceptionService exceptionService = injector.getInstance(ExceptionService.class);
        Assertions.assertNotNull(exceptionService);

        final TracerService tracerService = injector.getInstance(TracerService.class);
        Assertions.assertNotNull(tracerService);

        final ReactorService reactorService = injector.getInstance(ReactorService.class);
        Assertions.assertNotNull(reactorService);

        final JmxService jmxService = injector.getInstance(JmxService.class);
        Assertions.assertNotNull(jmxService);

        final GuiceInjectorService guiceInjectorService = injector.getInstance(GuiceInjectorService.class);
        Assertions.assertNotNull(guiceInjectorService);

        if (testAppbaseCoreMicroprofile) {
            appbaseCoreMicroprofile = injector.getInstance(AppbaseCoreMicroprofile.class);
            Assertions.assertNotNull(appbaseCoreMicroprofile);

            appbaseCoreMicroprofile.shutdown();
        }
    }
}
