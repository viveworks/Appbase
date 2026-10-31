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

package com.viveworks.appbase.core.bootstrap;

import com.viveworks.appbase.core.microprofile.AppbaseCoreMicroprofile;
import com.viveworks.appbase.core.microprofile.DefaultAppbaseCoreMicroprofile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseCoreBootstrappedServicesTest {
    private static AppbaseCoreMicroprofile appbaseCoreMicroprofile;

    @BeforeAll
    static void setUp() {
        appbaseCoreMicroprofile = new DefaultAppbaseCoreMicroprofile();
    }

    @AfterAll
    static void tearDown() {
        if (appbaseCoreMicroprofile != null) {
            appbaseCoreMicroprofile.shutdown();
        }
    }

    @Test
    void testBootstrap() {
        final AppbaseCoreBootstrappedServices appbaseCoreBootstrappedServices = AppbaseCoreBootstrappedServices
                .builder()
                .configurationService(appbaseCoreMicroprofile.getMicroprofileConfigurationService())
                .exceptionService(appbaseCoreMicroprofile.getExceptionService())
                .jmxService(appbaseCoreMicroprofile.getJmxService())
                .logService(appbaseCoreMicroprofile.getLogService())
                .luceneService(appbaseCoreMicroprofile.getLuceneService())
                .mediatorService(appbaseCoreMicroprofile.getMediatorService())
                .metricService(appbaseCoreMicroprofile.getMetricService())
                .configurationService(appbaseCoreMicroprofile.getConfigurationService())
                .microprofileConfigurationService(appbaseCoreMicroprofile.getMicroprofileConfigurationService())
                .serviceRegistry(appbaseCoreMicroprofile.getServiceRegistry())
                .microprofileServiceRegistry(appbaseCoreMicroprofile.getMicroprofileServiceRegistry())
                .reactorService(appbaseCoreMicroprofile.getReactorService())
                .tracerService(appbaseCoreMicroprofile.getTracerService())
                .validationService(appbaseCoreMicroprofile.getValidationService())
                .build();
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getConfigurationService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getExceptionService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getJmxService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getLogService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getLuceneService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMediatorService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMetricService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getConfigurationService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMicroprofileConfigurationService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getServiceRegistry());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getMicroprofileServiceRegistry());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getReactorService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getTracerService());
        Assertions.assertNotNull(appbaseCoreBootstrappedServices.getValidationService());
    }
}
