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

package com.viveworks.appbase.core.service.registry.jmx;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceTest;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.DefaultReadinessChecker;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ReadinessChecker;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxServiceRegistryTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static DefaultMediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration;
    private static DefaultServiceRegistry defaultServiceRegistry;
    private static DefaultJmxService jmxService;
    private static JmxServiceRegistry jmxServiceRegistry;

    @BeforeAll
    static void setUp() throws Exception {
        logService = new DefaultLogService(
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                DefaultLogServiceConfiguration.builder().build());
        metricService = new DefaultMetricService(
                logService,
                DefaultMetricServiceConfiguration.builder().build());
        validationService = new DefaultValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        configurationService = DefaultConfigurationServiceTest.newConfigurationService(
                logService, metricService, mediatorService, validationService, false);

        defaultServiceRegistryConfiguration =
                DefaultServiceRegistryConfiguration
                        .builder()
                        .enablePolling(false)
                        .registerAsConfigurationListener(true)
                        .build();
        configurationService.putTypedConfiguration(
                DefaultServiceRegistryConfiguration.class, defaultServiceRegistryConfiguration);

        final ReadinessChecker readinessChecker = new DefaultReadinessChecker();
        defaultServiceRegistry = new DefaultServiceRegistry(
                logService, metricService, mediatorService, validationService, configurationService, readinessChecker);

        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);

        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxServiceRegistry.class);
        jmxServiceRegistry = new JmxServiceRegistry(
                logService,
                defaultServiceRegistry,
                DefaultServiceRegistryConfiguration.class,
                configurationService);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (jmxServiceRegistry != null) {
            jmxServiceRegistry.shutdown();
        }

        if (jmxService != null) {
            jmxService.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (defaultServiceRegistry != null) {
            defaultServiceRegistry.shutdown();
        }

        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testMXBean() throws Exception {
        final Class<? extends JmxServiceRegistry> serviceRegistryType = jmxServiceRegistry.getClass();
        if (jmxService.isMXBeanRegistered(serviceRegistryType)) {
            jmxService.unregisterMXBean(serviceRegistryType);
        }
        jmxService.registerMXBean(jmxServiceRegistry);

        final JmxServiceRegistry mxBean = jmxService.getMXBean(JmxServiceRegistry.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxServiceRegistry.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxServiceRegistry.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    void testService() throws Exception {
        Assertions.assertDoesNotThrow(() -> jmxServiceRegistry.pollAndUpdateStatuses());

        defaultServiceRegistry.registerAppbaseService(MediatorService.class, mediatorService);

        final Map<String, ServiceStatus> allServiceStatusByServiceTypeName =
                jmxServiceRegistry.getAllServiceStatusByServiceTypeName();
        Assertions.assertNotNull(allServiceStatusByServiceTypeName);
        Assertions.assertFalse(allServiceStatusByServiceTypeName.isEmpty());

        final ServiceStatus appbaseServiceStatus =
                jmxServiceRegistry.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertNotNull(appbaseServiceStatus);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceStatus);

        jmxServiceRegistry.unregisterAppbaseService(MediatorService.class.getName());
        final ServiceStatus appbaseServiceStatus1 =
                jmxServiceRegistry.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertNull(appbaseServiceStatus1);

        defaultServiceRegistry.registerAppbaseService(MediatorService.class, mediatorService);
        jmxServiceRegistry.clearAppbaseServices();
        final ServiceStatus appbaseServiceStatus2 =
                jmxServiceRegistry.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertNull(appbaseServiceStatus2);
    }
}
