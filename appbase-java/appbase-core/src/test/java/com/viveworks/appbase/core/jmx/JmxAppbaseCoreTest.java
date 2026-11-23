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

package com.viveworks.appbase.core.jmx;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.DefaultAppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceTest;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.DefaultReadinessChecker;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistry;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.ReadinessChecker;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxAppbaseCoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static DefaultMediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static AppbaseCore appbaseCore;
    private static ServiceRegistry serviceRegistry;
    private static JmxServiceRegistryMXBean jmxServiceRegistryMXBean;
    private static DefaultJmxService jmxService;
    private static JmxJmxServiceMXBean jmxJmxServiceMXBean;
    private static JmxAppbaseCore jmxAppbaseCore;

    @BeforeAll
    static void setUp() {
        appbaseCore = new DefaultAppbaseCore();

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

        final SchedulableConfiguration schedulableConfiguration = SchedulableConfiguration
                .builder()
                .initialDelay(100L)
                .delay(10L)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        final DefaultServiceRegistryConfiguration defaultServiceRegistryConfiguration =
                DefaultServiceRegistryConfiguration
                        .builder()
                        .enablePolling(false)
                        .registerAsConfigurationListener(true)
                        .pollingSchedulableConfiguration(schedulableConfiguration)
                        .build();
        configurationService.putTypedConfiguration(
                DefaultServiceRegistryConfiguration.class, defaultServiceRegistryConfiguration);

        final ReadinessChecker readinessChecker = new DefaultReadinessChecker();
        serviceRegistry = new DefaultServiceRegistry(
                logService, metricService, mediatorService, validationService, configurationService, readinessChecker);
        // AUD-08: JmxAppbaseCore.shutdown() cascades into both MXBeans below, which are now denied
        // by default unless explicitly permitted (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(
                configurationService, JmxServiceRegistry.class, JmxJmxService.class);
        jmxServiceRegistryMXBean = new JmxServiceRegistry(
                logService,
                serviceRegistry,
                DefaultServiceRegistryConfiguration.class,
                configurationService);

        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        jmxJmxServiceMXBean = new JmxJmxService(
                logService, jmxService, DefaultJmxServiceConfiguration.class, configurationService);
        jmxAppbaseCore = new JmxAppbaseCore(
                appbaseCore, serviceRegistry, jmxServiceRegistryMXBean, jmxJmxServiceMXBean);
    }

    @AfterAll
    static void tearDown() {
        if (jmxAppbaseCore != null) {
            jmxAppbaseCore.shutdown();
        }

        if (jmxJmxServiceMXBean != null) {
            jmxJmxServiceMXBean.shutdown();
        }

        if (jmxService != null) {
            jmxService.shutdown();
        }

        if (jmxServiceRegistryMXBean != null) {
            jmxServiceRegistryMXBean.shutdown();
        }

        if (serviceRegistry != null) {
            serviceRegistry.shutdown();
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

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    @Order(1)
    void testMXBean() throws Exception {
        final Class<? extends JmxAppbaseCore> type = jmxAppbaseCore.getClass();
        if (jmxService.isMXBeanRegistered(type)) {
            jmxService.unregisterMXBean(type);
        }
        jmxService.registerMXBean(jmxAppbaseCore);

        final JmxAppbaseCore mxBean = jmxService.getMXBean(type);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(type);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(type.getName(), mxBeanEntry.getClassName());
    }

    @Test
    @Order(2)
    void testService() throws Exception {
        Assertions.assertEquals(ServiceStatus.STARTED, jmxAppbaseCore.getStatus());

        Assertions.assertDoesNotThrow(() -> jmxAppbaseCore.reload());
        Assertions.assertEquals(ServiceStatus.STARTED, jmxAppbaseCore.getStatus());

        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                DefaultAppbaseCoreBootstrapConfiguration
                        .builder()
                        .build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultAppbaseCoreBootstrapConfiguration.class)
                .writeValueAsString(appbaseCoreBootstrapConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxAppbaseCore.reload(yaml));
        Assertions.assertEquals(ServiceStatus.STARTED, jmxAppbaseCore.getStatus());

        serviceRegistry.registerAppbaseService(MediatorService.class, mediatorService);
        final Map<String, ServiceStatus> allServiceStatusByServiceTypeName =
                jmxAppbaseCore.getAllServiceStatusByServiceTypeName();
        Assertions.assertNotNull(allServiceStatusByServiceTypeName);
        Assertions.assertFalse(allServiceStatusByServiceTypeName.isEmpty());

        final ServiceStatus appbaseServiceStatus =
                jmxAppbaseCore.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertNotNull(appbaseServiceStatus);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceStatus);

        // AUD-08: shutdownService() is now denied by default (owner decision DQ-1). The two
        // reload() calls above re-bootstrap appbaseCore's internal ConfigurationService each time
        // (DefaultAppbaseCore.reload() discards the old instance), so the grant must be made on
        // the CURRENT instance, immediately before this call, rather than once in setUp().
        appbaseCore.getConfigurationService().putBoolean(
                AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY, true);
        jmxAppbaseCore.shutdownService(MediatorService.class.getName());
        final ServiceStatus appbaseServiceStatus4 =
                jmxAppbaseCore.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, appbaseServiceStatus4);

        final String mxBeanEntryJson = jmxAppbaseCore.getMXBeanEntryJson(jmxAppbaseCore.getClass().getName());
        Assertions.assertNotNull(mxBeanEntryJson);

        final List<String> mxBeanEntriesJson = jmxAppbaseCore.getMXBeanEntriesJson();
        Assertions.assertNotNull(mxBeanEntriesJson);
        Assertions.assertFalse(mxBeanEntriesJson.isEmpty());

        Assertions.assertTrue(jmxAppbaseCore.isMXBeanRegistered(jmxAppbaseCore.getClass().getName()));
        jmxAppbaseCore.unregisterMXBean(jmxAppbaseCore.getClass().getName());
        Assertions.assertFalse(jmxAppbaseCore.isMXBeanRegistered(jmxAppbaseCore.getClass().getName()));

        jmxAppbaseCore.clearMXBeans();
        final List<String> mxBeanEntriesJson1 = jmxAppbaseCore.getMXBeanEntriesJson();
        Assertions.assertNotNull(mxBeanEntriesJson1);
        Assertions.assertTrue(mxBeanEntriesJson1.isEmpty());

        jmxAppbaseCore.unregisterAppbaseService(MediatorService.class.getName());
        final ServiceStatus appbaseServiceStatus1 =
                jmxAppbaseCore.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertNull(appbaseServiceStatus1);

        serviceRegistry.registerAppbaseService(MediatorService.class, mediatorService);
        jmxAppbaseCore.clearAppbaseServices();
        final ServiceStatus appbaseServiceStatus2 =
                jmxAppbaseCore.getAppbaseServiceStatus(MediatorService.class.getName());
        Assertions.assertNull(appbaseServiceStatus2);

        Assertions.assertDoesNotThrow(() -> jmxAppbaseCore.pollAndUpdateStatuses());

        serviceRegistry.registerAppbaseService(MetricService.class, metricService);
        final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration =
                DefaultMetricServiceConfiguration
                .builder()
                .build();
        final String yaml1 = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultMetricServiceConfiguration.class)
                .writeValueAsString(defaultMetricServiceConfiguration);
        jmxAppbaseCore.reloadService(
                MetricService.class.getName(),
                yaml1,
                DefaultMetricServiceConfiguration.class.getName());
        final ServiceStatus appbaseServiceStatus3 =
                jmxAppbaseCore.getAppbaseServiceStatus(MetricService.class.getName());
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceStatus3);

        jmxAppbaseCore.shutdownAllServices();
        final ServiceStatus appbaseServiceStatus5 =
                jmxAppbaseCore.getAppbaseServiceStatus(MetricService.class.getName());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, appbaseServiceStatus5);
    }
}
