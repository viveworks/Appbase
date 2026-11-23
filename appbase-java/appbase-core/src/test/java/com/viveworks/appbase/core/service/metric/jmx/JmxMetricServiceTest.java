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

package com.viveworks.appbase.core.service.metric.jmx;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxMetricServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static MediatorService mediatorService;
    private static ValidationService validationService;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;
    private static JmxMetricService jmxMetricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = new DefaultValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxMetricService.class);
        jmxMetricService = new JmxMetricService(
                logService,
                metricService,
                DefaultMetricServiceConfiguration.class,
                configurationService);
    }

    @AfterAll
    static void tearDown() {
        if (jmxMetricService != null) {
            jmxMetricService.shutdown();
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
    @Order(1)
    void testMXBean() throws Exception {
        final Class<? extends JmxMetricService> type = jmxMetricService.getClass();
        if (jmxService.isMXBeanRegistered(type)) {
            jmxService.unregisterMXBean(type);
        }
        jmxService.registerMXBean(jmxMetricService);

        final JmxMetricService mxBean = jmxService.getMXBean(type);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(type);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxMetricService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    @Order(2)
    void testService() throws Exception {

        Assertions.assertEquals(ServiceStatus.STARTED, jmxMetricService.getStatus());
        Assertions.assertDoesNotThrow(() -> jmxMetricService.reload());
        Assertions.assertEquals(ServiceStatus.STARTED, jmxMetricService.getStatus());

        final DefaultMetricServiceConfiguration defaultMetricServiceConfiguration =
                DefaultMetricServiceConfiguration.builder().build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultMetricServiceConfiguration.class)
                .writeValueAsString(defaultMetricServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxMetricService.reload(yaml));
        Assertions.assertEquals(ServiceStatus.STARTED, jmxMetricService.getStatus());

        metricService.registerMicrometerRegistry(new LoggingMeterRegistry());
        final Set<String> micrometerRegistryNames = jmxMetricService.getMicrometerRegistryNames();
        Assertions.assertNotNull(micrometerRegistryNames);
        Assertions.assertFalse(micrometerRegistryNames.isEmpty());

        final String metricRegistryAsJson = jmxMetricService.serializeMetricRegistryAsJson();
        Assertions.assertNotNull(metricRegistryAsJson);

        jmxMetricService.clearMicrometerRegistries();

        final Set<String> micrometerRegistryNames1 = jmxMetricService.getMicrometerRegistryNames();
        Assertions.assertNotNull(micrometerRegistryNames1);
        Assertions.assertTrue(micrometerRegistryNames1.isEmpty());

        metricService.registerMicrometerRegistry(new LoggingMeterRegistry());
        final Set<String> micrometerRegistryNames2 = jmxMetricService.getMicrometerRegistryNames();
        Assertions.assertNotNull(micrometerRegistryNames2);
        Assertions.assertFalse(micrometerRegistryNames2.isEmpty());

        jmxMetricService.unregisterMicrometerRegistry(LoggingMeterRegistry.class.getName());
        final Set<String> micrometerRegistryNames3 = jmxMetricService.getMicrometerRegistryNames();
        Assertions.assertNotNull(micrometerRegistryNames3);
        Assertions.assertTrue(micrometerRegistryNames3.isEmpty());

        Assertions.assertDoesNotThrow(() -> jmxMetricService.shutdown());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, jmxMetricService.getStatus());
    }
}
