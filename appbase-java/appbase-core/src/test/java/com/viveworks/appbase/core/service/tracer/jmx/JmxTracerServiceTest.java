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

package com.viveworks.appbase.core.service.tracer.jmx;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxService;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.DefaultTracerService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.InMemoryReporterConfiguration;
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

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxTracerServiceTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static ConfigurationService configurationService;
    private static DefaultJmxService jmxService;
    private static TracerService tracerService;
    private static JmxTracerService jmxTracerService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
        configurationService = CoreTestFixtures.getEmptyDefaultConfigurationService(
                logService, metricService, mediatorService, validationService);
        final InMemoryReporterConfiguration inMemoryReporterConfiguration =
                InMemoryReporterConfiguration.builder().enabled(true).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerReporterConfigurations(List.of(inMemoryReporterConfiguration))
                .tags(Map.of("k1", "v1"))
                .build();
        configurationService.putTypedConfiguration(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.builder().build());
        jmxService = new DefaultJmxService(
                logService, metricService, mediatorService, validationService, configurationService);
        configurationService.putTypedConfiguration(
                DefaultTracerServiceConfiguration.class,
                defaultTracerServiceConfiguration);
        tracerService = new DefaultTracerService(
                logService, metricService, mediatorService, validationService, configurationService);
        // AUD-08: reload()/shutdown() below are now denied by default (owner decision DQ-1).
        CoreTestFixtures.grantJmxOperationPermissions(configurationService, JmxTracerService.class);
        jmxTracerService = new JmxTracerService(
                logService,
                tracerService,
                defaultTracerServiceConfiguration.getClass(),
                configurationService);
    }

    @AfterAll
    static void tearDown() {
        if (jmxTracerService != null) {
            jmxTracerService.shutdown();
        }

        if (tracerService != null) {
            tracerService.shutdown();
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
    @Order(1)
    void testMXBean() throws Exception {
        final Class<? extends JmxTracerService> jmxTraceServiceType = jmxTracerService.getClass();
        if (jmxService.isMXBeanRegistered(jmxTraceServiceType)) {
            jmxService.unregisterMXBean(jmxTraceServiceType);
        }
        jmxService.registerMXBean(jmxTracerService);

        final JmxTracerService mxBean = jmxService.getMXBean(JmxTracerService.class);
        Assertions.assertNotNull(mxBean);

        final MXBeanEntry mxBeanEntry = jmxService.getMXBeanEntry(JmxTracerService.class);
        Assertions.assertNotNull(mxBeanEntry);
        Assertions.assertEquals(JmxTracerService.class.getName(), mxBeanEntry.getClassName());
    }

    @Test
    @Order(2)
    void testService() throws Exception {
        Assertions.assertEquals(ServiceStatus.STARTED, jmxTracerService.getStatus());

        Assertions.assertDoesNotThrow(() -> jmxTracerService.reload());
        Assertions.assertEquals(ServiceStatus.STARTED, jmxTracerService.getStatus());

        final InMemoryReporterConfiguration inMemoryReporterConfiguration =
                InMemoryReporterConfiguration.builder().enabled(true).build();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerReporterConfigurations(List.of(inMemoryReporterConfiguration))
                .tags(Map.of("k2", "v2"))
                .build();
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultTracerServiceConfiguration.class)
                .writeValueAsString(defaultTracerServiceConfiguration);
        Assertions.assertDoesNotThrow(() -> jmxTracerService.reload(yaml));
        Assertions.assertEquals(ServiceStatus.STARTED, jmxTracerService.getStatus());

        Assertions.assertDoesNotThrow(() -> jmxTracerService.shutdown());
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, jmxTracerService.getStatus());
    }
}
