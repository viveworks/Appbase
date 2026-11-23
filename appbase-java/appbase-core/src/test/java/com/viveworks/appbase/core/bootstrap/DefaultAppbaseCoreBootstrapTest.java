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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.DefaultAppbaseCore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultAppbaseCoreBootstrapTest {
    private static AppbaseCore appbaseCore;

    @AfterAll
    static void tearDown() {
        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    void testBootstrap() {
        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                new DefaultAppbaseCoreBootstrapRegistry();
        final String configurationFilePath =
                "classpath:///appbaseBootstrapConfiguration.yaml";
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, configurationFilePath);
        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                DefaultAppbaseCoreBootstrapConfiguration.builder().build();

        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices =
                appbaseCoreBootstrap.bootstrap(appbaseCoreBootstrapConfiguration);
        appbaseCore = new DefaultAppbaseCore(
                appbaseCoreBootstrappedServices.getLogService(),
                appbaseCoreBootstrappedServices.getMetricService(),
                appbaseCoreBootstrappedServices.getValidationService(),
                appbaseCoreBootstrappedServices.getMediatorService(),
                appbaseCoreBootstrappedServices.getConfigurationService(),
                appbaseCoreBootstrappedServices.getServiceRegistry(),
                appbaseCoreBootstrappedServices.getExceptionService(),
                appbaseCoreBootstrappedServices.getTracerService(),
                appbaseCoreBootstrappedServices.getReactorService(),
                appbaseCoreBootstrappedServices.getJmxService(),
                appbaseCoreBootstrappedServices.getLuceneService());

        Assertions.assertNotNull(appbaseCoreBootstrappedServices);
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getLogService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMediatorService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getValidationService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMediatorService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getConfigurationService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMicroprofileConfigurationService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getServiceRegistry());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMicroprofileServiceRegistry());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getExceptionService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getTracerService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getReactorService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getJmxService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getLuceneService());

    }
}
