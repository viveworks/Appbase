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

package com.viveworks.appbase.core.service.jmx;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Collection;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxJmxServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;
    private static JmxJmxService jmxJmxService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxJmxService.class);
        jmxJmxService = new JmxJmxService(
                logService, jmxService, DefaultJmxServiceConfiguration.class, configurationService);
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
    @Order(1)
    void testMXBean() throws Exception {
        final Class<? extends JmxJmxService> jmxJmxServiceType = jmxJmxService.getClass();
        if (jmxService.isMXBeanRegistered(jmxJmxServiceType)) {
            jmxService.unregisterMXBean(jmxJmxServiceType);
        }
        jmxService.registerMXBean(jmxJmxService);

        final JmxJmxService mxBean = jmxService.getMXBean(JmxJmxService.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxJmxService.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxJmxService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    @Order(2)
    void testService() throws Exception {
        Assertions.assertEquals(ServiceStatus.STARTED, jmxJmxService.getStatus());

        Assertions.assertDoesNotThrow(() -> jmxJmxService.reload());
        Assertions.assertEquals(ServiceStatus.STARTED, jmxJmxService.getStatus());

        final DefaultJmxServiceConfiguration defaultJmxServiceConfiguration =
                DefaultJmxServiceConfiguration.builder().build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultJmxServiceConfiguration.class)
                .writeValueAsString(defaultJmxServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxJmxService.reload(yaml));
        Assertions.assertEquals(ServiceStatus.STARTED, jmxJmxService.getStatus());

        final TestJmxMXBean testMxBean = new TestJmx();
        jmxService.registerMXBean(testMxBean);

        final Collection<String> mxBeanEntries = jmxJmxService.getMXBeanEntriesJson();
        Assertions.assertNotNull(mxBeanEntries);
        Assertions.assertEquals(1, mxBeanEntries.size());

        final String mxBean = jmxJmxService.getMXBeanEntryJson(TestJmx.class.getName());
        Assertions.assertNotNull(mxBean);

        Assertions.assertTrue(jmxJmxService.isMXBeanRegistered(TestJmx.class.getName()));

        jmxJmxService.unregisterMXBean(TestJmx.class.getName());
        Assertions.assertFalse(jmxJmxService.isMXBeanRegistered(TestJmx.class.getName()));

        jmxService.registerMXBean(testMxBean);
        Assertions.assertTrue(jmxJmxService.isMXBeanRegistered(TestJmx.class.getName()));
        jmxJmxService.clearMXBeans();
        Assertions.assertFalse(jmxJmxService.isMXBeanRegistered(TestJmx.class.getName()));

        Assertions.assertDoesNotThrow(() -> jmxJmxService.shutdown());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, jmxJmxService.getStatus());
    }
}
