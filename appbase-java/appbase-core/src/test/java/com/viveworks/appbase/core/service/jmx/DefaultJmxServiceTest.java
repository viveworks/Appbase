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

package com.viveworks.appbase.core.service.jmx;

import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceTest;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultJmxServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static DefaultMediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        configurationService = DefaultConfigurationServiceTest.newConfigurationService(
                logService, metricService, mediatorService, validationService, false);
        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
    }

    @AfterAll
    static void tearDown() {
        if (jmxService != null) {
            jmxService.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testService() throws Exception {
        final JmxServiceConfiguration jmxServiceConfiguration = jmxService.getConfiguration();
        Assertions.assertNotNull(jmxServiceConfiguration);

        final TestJmxMXBean testMxBean = new TestJmx();
        jmxService.registerMXBean(testMxBean);

        final Collection<MXBeanEntry> mxBeanEntries = jmxService.getMXBeanEntries();
        Assertions.assertNotNull(mxBeanEntries);
        Assertions.assertEquals(1, mxBeanEntries.size());

        final TestJmxMXBean mxBean = jmxService.getMXBean(TestJmx.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(TestJmx.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(TestJmx.class.getName(), mxBeanEntry.getClassName());

        Assertions.assertTrue(jmxService.isMXBeanRegistered(TestJmx.class));

        final DefaultJmxServiceConfiguration defaultJmxServiceConfiguration =
                DefaultJmxServiceConfiguration.builder().build();
        final ConfigurationKey configurationKey =
                ConfigurationKey.fromTypeKey(DefaultJmxServiceConfiguration.class);
        final ConfigurationValue<DefaultJmxServiceConfiguration> configurationValue =
                ConfigurationValue.fromValue(defaultJmxServiceConfiguration);
        final ConfigurationAddedEvent configurationAddedEvent =
                new ConfigurationAddedEvent(
                        configurationKey, configurationValue, System.currentTimeMillis());
        Assertions.assertDoesNotThrow(() -> jmxService.onConfigurationAdded(configurationAddedEvent));

        final ConfigurationChangedEvent configurationChangedEvent =
                new ConfigurationChangedEvent(
                        configurationKey,
                        configurationValue,
                        configurationValue,
                        System.currentTimeMillis());
        Assertions.assertDoesNotThrow(() -> jmxService.onConfigurationChanged(configurationChangedEvent));

        jmxService.reload(DefaultJmxServiceConfiguration.builder().build());
        jmxService.reloadImpl(jmxServiceConfiguration);
        Assertions.assertTrue(jmxService.isMXBeanRegistered(TestJmx.class));

        jmxService.unregisterMXBean(TestJmx.class);
        Assertions.assertFalse(jmxService.isMXBeanRegistered(TestJmx.class));

        jmxService.registerMXBean(testMxBean);
        Assertions.assertTrue(jmxService.isMXBeanRegistered(TestJmx.class));
        jmxService.clearMXBeans();
        Assertions.assertFalse(jmxService.isMXBeanRegistered(TestJmx.class));

        jmxService.registerMXBean(testMxBean);
        Assertions.assertTrue(jmxService.isMXBeanRegistered(TestJmx.class));

        jmxService.shutdownImpl();
        Assertions.assertFalse(jmxService.isMXBeanRegistered(TestJmx.class));

        final ConfigurationRemovedEvent configurationRemovedEvent =
                new ConfigurationRemovedEvent(configurationKey, System.currentTimeMillis());
        Assertions.assertDoesNotThrow(() -> jmxService.onConfigurationRemoved(configurationRemovedEvent));
    }
}
