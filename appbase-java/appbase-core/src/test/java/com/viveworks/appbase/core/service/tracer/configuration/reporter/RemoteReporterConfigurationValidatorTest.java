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

package com.viveworks.appbase.core.service.tracer.configuration.reporter;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class RemoteReporterConfigurationValidatorTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
    }

    @AfterAll
    static void tearDown() {
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
    void testValidator() {
        final RemoteReporterConfigurationValidator remoteReporterConfigurationValidator1 =
                new RemoteReporterConfigurationValidator();
        final RemoteReporterConfigurationValidator remoteReporterConfigurationValidator2 =
                new RemoteReporterConfigurationValidator(validationService);
        final RemoteReporterConfiguration remoteReporterConfiguration =
                RemoteReporterConfiguration
                        .builder()
                        .enabled(true)
                        .collectorEndpoint("/collector")
                        .build();

        Assertions.assertTrue(
                remoteReporterConfigurationValidator1.isValid(remoteReporterConfiguration, null));
        Assertions.assertTrue(
                remoteReporterConfigurationValidator2.isValid(remoteReporterConfiguration, null));

        Assertions.assertFalse(
                remoteReporterConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                remoteReporterConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                RemoteReporterConfigurationValidator.validate(
                        remoteReporterConfiguration.isEnabled(),
                        remoteReporterConfiguration.getAgentHost(),
                        remoteReporterConfiguration.getAgentPort(),
                        remoteReporterConfiguration.getCollectorEndpoint(),
                        remoteReporterConfiguration.getMaxPacketSizeBytes(),
                        remoteReporterConfiguration.getFlushIntervalMillis(),
                        remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                        remoteReporterConfiguration.getMaxQueueSize(),
                        remoteReporterConfiguration.getMetricsPrefix()));
        Assertions.assertDoesNotThrow(() ->
                RemoteReporterConfigurationValidator.validate(
                        remoteReporterConfiguration.isEnabled(),
                        remoteReporterConfiguration.getAgentHost(),
                        remoteReporterConfiguration.getAgentPort(),
                        remoteReporterConfiguration.getCollectorEndpoint(),
                        remoteReporterConfiguration.getMaxPacketSizeBytes(),
                        remoteReporterConfiguration.getFlushIntervalMillis(),
                        remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                        remoteReporterConfiguration.getMaxQueueSize(),
                        remoteReporterConfiguration.getMetricsPrefix(),
                        validationService,
                        null));
        Assertions.assertDoesNotThrow(() ->
                RemoteReporterConfigurationValidator.validate(
                        false,
                        "",
                        remoteReporterConfiguration.getAgentPort(),
                        remoteReporterConfiguration.getCollectorEndpoint(),
                        remoteReporterConfiguration.getMaxPacketSizeBytes(),
                        remoteReporterConfiguration.getFlushIntervalMillis(),
                        remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                        remoteReporterConfiguration.getMaxQueueSize(),
                        remoteReporterConfiguration.getMetricsPrefix(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            "",
                            remoteReporterConfiguration.getAgentPort(),
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            remoteReporterConfiguration.getMaxQueueSize(),
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            -1,
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            remoteReporterConfiguration.getMaxQueueSize(),
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            remoteReporterConfiguration.getAgentPort(),
                            "",
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            remoteReporterConfiguration.getMaxQueueSize(),
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            remoteReporterConfiguration.getAgentPort(),
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            -1,
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            remoteReporterConfiguration.getMaxQueueSize(),
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            remoteReporterConfiguration.getAgentPort(),
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            0,
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            remoteReporterConfiguration.getMaxQueueSize(),
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            remoteReporterConfiguration.getAgentPort(),
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            -1,
                            remoteReporterConfiguration.getMaxQueueSize(),
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            remoteReporterConfiguration.getAgentPort(),
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            0,
                            remoteReporterConfiguration.getMetricsPrefix());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    RemoteReporterConfigurationValidator.validate(
                            remoteReporterConfiguration.isEnabled(),
                            remoteReporterConfiguration.getAgentHost(),
                            remoteReporterConfiguration.getAgentPort(),
                            remoteReporterConfiguration.getCollectorEndpoint(),
                            remoteReporterConfiguration.getMaxPacketSizeBytes(),
                            remoteReporterConfiguration.getFlushIntervalMillis(),
                            remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                            remoteReporterConfiguration.getMaxQueueSize(),
                            "");
                });
    }
}
