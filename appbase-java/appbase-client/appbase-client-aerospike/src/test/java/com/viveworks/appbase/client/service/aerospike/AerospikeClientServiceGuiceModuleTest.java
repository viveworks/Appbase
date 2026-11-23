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

package com.viveworks.appbase.client.service.aerospike;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientCoreGuiceModule;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.di.guice.AppbaseCoreGuiceModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "AerospikeClientServiceTests.enabled", matches = "true")
class AerospikeClientServiceGuiceModuleTest {
    private static AppbaseCore appbaseCore;
    private static AppbaseClientCore appbaseClientCore;
    private static AerospikeClientService aerospikeClientService;

    @AfterAll
    static void tearDown() {
        if (aerospikeClientService != null) {
            aerospikeClientService.shutdown();
        }

        if (appbaseClientCore != null) {
            appbaseClientCore.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    void testAerospikeClientServiceGuiceModule() throws AppbaseClientIntegrationService.NotFoundException {
        appbaseCore = AerospikeClientTestHelper.appbaseCore();
        final AppbaseCoreGuiceModule appbaseCoreGuiceModule = new AppbaseCoreGuiceModule(appbaseCore);


        appbaseClientCore = ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        final AppbaseClientCoreGuiceModule appbaseClientCoreGuiceModule =
                new AppbaseClientCoreGuiceModule(appbaseClientCore);


        final AerospikeClientServiceGuiceModule aerospikeClientServiceGuiceModule =
                new AerospikeClientServiceGuiceModule();
        final Injector injector = Guice.createInjector(
                appbaseCoreGuiceModule, appbaseClientCoreGuiceModule, aerospikeClientServiceGuiceModule);
        Assertions.assertNotNull(injector);

        aerospikeClientService = injector.getInstance(AerospikeClientService.class);
        Assertions.assertNotNull(aerospikeClientService);

        final AerospikeClientIntegrationService aerospikeClientIntegrationService =
                aerospikeClientService.getClientIntegrationService();
        AerospikeClientTestUtil
                .aerospikeClientServiceTest(aerospikeClientService, aerospikeClientIntegrationService);
    }
}
