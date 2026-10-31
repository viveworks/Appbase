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

package com.viveworks.appbase.client.service.kafka.producer;

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
@EnabledIfSystemProperty(named = "KafkaClientServiceTests.enabled", matches = "true")
class KafkaProducerClientServiceGuiceModuleTest {
    private static AppbaseCore appbaseCore;
    private static AppbaseClientCore appbaseClientCore;
    private static KafkaProducerClientService<?, ?> kafkaProducerClientService;

    @AfterAll
    static void tearDown() {
        if (kafkaProducerClientService != null) {
            kafkaProducerClientService.shutdown();
        }

        if (appbaseClientCore != null) {
            appbaseClientCore.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    void testKafkaProducerClientServiceGuiceModule() throws AppbaseClientIntegrationService.NotFoundException {
        appbaseCore = KafkaProducerClientServiceTestHelper.appbaseCore();
        appbaseClientCore = ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);


        final AppbaseCoreGuiceModule appbaseCoreGuiceModule = new AppbaseCoreGuiceModule(appbaseCore);
        final AppbaseClientCoreGuiceModule appbaseClientCoreGuiceModule =
                new AppbaseClientCoreGuiceModule(appbaseClientCore);

        final KafkaProducerClientServiceGuiceModule kafkaProducerClientServiceGuiceModule =
                new KafkaProducerClientServiceGuiceModule();
        final Injector injector = Guice.createInjector(
                appbaseCoreGuiceModule, appbaseClientCoreGuiceModule, kafkaProducerClientServiceGuiceModule);
        Assertions.assertNotNull(injector);


        kafkaProducerClientService = injector.getInstance(KafkaProducerClientService.class);
        Assertions.assertNotNull(kafkaProducerClientService);

        final KafkaProducerClientIntegrationService<?, ?> kafkaProducerClientIntegrationService =
                kafkaProducerClientService.getClientIntegrationService();
        KafkaProducerClientServiceTestUtil.kafkaProducerClientServiceTest(
                kafkaProducerClientService, kafkaProducerClientIntegrationService);
    }
}
