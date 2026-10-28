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

package com.viveworks.appbase.core.service;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AbstractAppbaseManagedServiceTest {
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
        Assertions.assertEquals(ServiceStatus.STARTED, status);
        Assertions.assertNotNull(testService.getConfiguration());

        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(CoreTestFixtures.TestServiceConfiguration.class);
        final ConfigurationValue<CoreTestFixtures.TestServiceConfiguration> oldConfigurationValue =
                ConfigurationValue.fromValue(testServiceConfiguration);
        final CoreTestFixtures.TestServiceConfiguration testServiceConfiguration1 =
                new CoreTestFixtures.TestServiceConfiguration("127.0.0.1");
        final ConfigurationValue<CoreTestFixtures.TestServiceConfiguration> newConfigurationValue =
                ConfigurationValue.fromValue(testServiceConfiguration1);
        final long timestamp = System.currentTimeMillis();

        final ConfigurationAddedEvent configurationAddedEvent =
                new ConfigurationAddedEvent(configurationKey, oldConfigurationValue, timestamp);
        testService.onConfigurationAdded(configurationAddedEvent);
        Assertions.assertEquals(testServiceConfiguration, testService.getConfiguration());

        final ConfigurationChangedEvent configurationChangedEvent =
                new ConfigurationChangedEvent(
                        configurationKey, oldConfigurationValue, newConfigurationValue, timestamp);
        testService.onConfigurationChanged(configurationChangedEvent);
        Assertions.assertEquals(testServiceConfiguration1, testService.getConfiguration());

        final ConfigurationRemovedEvent configurationRemovedEvent =
                new ConfigurationRemovedEvent(configurationKey, timestamp);
        testService.onConfigurationRemoved(configurationRemovedEvent);
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, testService.getStatus());

        Assertions.assertThrows(ServiceReloadException.class,
                () -> testService.reload(testServiceConfiguration1));
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, testService.getStatus());
    }
}
