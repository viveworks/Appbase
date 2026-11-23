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
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseCoreGuiceModuleTest {
    private static LogService logService;
    private static AppbaseCore appbaseCore;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
    }

    @AfterAll
    static void tearDown() {
        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testConfigure() {
        // Simple Init with the default "classpath:///appbase-core.yaml"
        runTests(new AppbaseCoreGuiceModule(), false);
        runTests(CoreTestFixtures.getCoreModule(logService), false);

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                new DefaultAppbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
        runTests(new AppbaseCoreGuiceModule(appbaseCoreBootstrap), false);

        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                CoreTestFixtures.getLuceneDisabledBootstrapConfiguration();
        runTests(new AppbaseCoreGuiceModule(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration), true);
    }

    private void runTests(final AppbaseCoreGuiceModule appbaseCoreGuiceModule, final boolean nullLuceneService) {
        final Injector injector = Guice.createInjector(appbaseCoreGuiceModule);

        final LogService injectedLogService = injector.getInstance(LogService.class);
        Assertions.assertNotNull(injectedLogService);

        final MetricService injectedMetricService = injector.getInstance(MetricService.class);
        Assertions.assertNotNull(injectedMetricService);

        final ValidationService validationService = injector.getInstance(ValidationService.class);
        Assertions.assertNotNull(validationService);

        final MediatorService mediatorService = injector.getInstance(MediatorService.class);
        Assertions.assertNotNull(mediatorService);
        Assertions.assertTrue(mediatorService.isInitWithConfigurationFile());
        Assertions.assertFalse(mediatorService.isInitWithDefaultConfiguration());
        Assertions.assertNotNull(mediatorService.getConfigurationFilePath());

        final ConfigurationService configurationService = injector.getInstance(ConfigurationService.class);
        Assertions.assertNotNull(configurationService);

        final ServiceRegistry serviceRegistry = injector.getInstance(ServiceRegistry.class);
        Assertions.assertNotNull(serviceRegistry);

        final ExceptionService exceptionService = injector.getInstance(ExceptionService.class);
        Assertions.assertNotNull(exceptionService);

        final TracerService tracerService = injector.getInstance(TracerService.class);
        Assertions.assertNotNull(tracerService);

        final ReactorService reactorService = injector.getInstance(ReactorService.class);
        Assertions.assertNotNull(reactorService);

        final JmxService jmxService = injector.getInstance(JmxService.class);
        Assertions.assertNotNull(jmxService);

        if (nullLuceneService) {
            Assertions.assertNull(injector.getInstance(LuceneService.class));
        } else {
            Assertions.assertNotNull(injector.getInstance(LuceneService.class));
        }

        final GuiceInjectorService guiceInjectorService = injector.getInstance(GuiceInjectorService.class);
        Assertions.assertNotNull(guiceInjectorService);

        appbaseCore = injector.getInstance(AppbaseCore.class);
        Assertions.assertNotNull(appbaseCore);

        Assertions.assertNotNull(appbaseCoreGuiceModule.getAppbaseCore());

        appbaseCore.shutdown();
    }
}
