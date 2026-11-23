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

package com.viveworks.appbase.core.service.validation.jmx;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxValidationServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;
    private static JmxValidationService jmxValidationService;

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
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxValidationService.class);
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        jmxValidationService = new JmxValidationService(
                logService, validationService, DefaultValidationServiceConfiguration.class, configurationService);
    }

    @AfterAll
    static void tearDown() {
        if (jmxValidationService != null) {
            jmxValidationService.shutdown();
        }

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
    void testMXBean() throws Exception {
        final Class<? extends JmxValidationService> jmxValidationServiceType = jmxValidationService.getClass();
        if (jmxService.isMXBeanRegistered(jmxValidationServiceType)) {
            jmxService.unregisterMXBean(jmxValidationServiceType);
        }
        jmxService.registerMXBean(jmxValidationService);

        final JmxValidationService mxBean = jmxService.getMXBean(JmxValidationService.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxValidationService.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxValidationService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    void testService() throws Exception {
        Assertions.assertEquals(ServiceStatus.STARTED, jmxValidationService.getStatus());

        Assertions.assertDoesNotThrow(() -> jmxValidationService.reload());
        Assertions.assertEquals(ServiceStatus.STARTED, jmxValidationService.getStatus());

        final DefaultValidationServiceConfiguration defaultValidationServiceConfiguration =
                new DefaultValidationServiceConfiguration();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultValidationServiceConfiguration.class)
                .writeValueAsString(defaultValidationServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxValidationService.reload(yaml));
        Assertions.assertEquals(ServiceStatus.STARTED, jmxValidationService.getStatus());

        Assertions.assertDoesNotThrow(() -> jmxValidationService.shutdown());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, jmxValidationService.getStatus());
    }
}
