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

package com.viveworks.appbase.core.service.tracer.configuration;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.InMemoryReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.JaegerReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.RemoteReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.GuaranteedThroughputSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.JaegerSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.PerOperationSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ProbabilisticSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RateLimitingSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RemoteControlledSamplerConfiguration;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultTracerServiceConfigurationValidatorTest {
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
        final DefaultTracerServiceConfigurationValidator defaultTracerServiceConfigurationValidator1 =
                new DefaultTracerServiceConfigurationValidator();
        final DefaultTracerServiceConfigurationValidator defaultTracerServiceConfigurationValidator2 =
                new DefaultTracerServiceConfigurationValidator();
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration =
                DefaultTracerServiceConfiguration.builder().build();

        Assertions.assertTrue(
                defaultTracerServiceConfigurationValidator1.isValid(defaultTracerServiceConfiguration, null));
        Assertions.assertTrue(
                defaultTracerServiceConfigurationValidator2.isValid(defaultTracerServiceConfiguration, null));

        Assertions.assertFalse(
                defaultTracerServiceConfigurationValidator1.isValid(null, null));
        Assertions.assertFalse(
                defaultTracerServiceConfigurationValidator2.isValid(null, null));

        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        validationService,
                        null));

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultTracerServiceConfigurationValidator.validate(
                            "",
                            defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerReporterConfigurations());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultTracerServiceConfigurationValidator.validate(
                            defaultTracerServiceConfiguration.getServiceName(),
                            null,
                            defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerReporterConfigurations());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultTracerServiceConfigurationValidator.validate(
                            defaultTracerServiceConfiguration.getServiceName(),
                            defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                            null,
                            defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerReporterConfigurations());
                });
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    DefaultTracerServiceConfigurationValidator.validate(
                            defaultTracerServiceConfiguration.getServiceName(),
                            defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                            defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                            null);
                });


        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        new TestJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        ConstSamplerConfiguration.builder().build(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        GuaranteedThroughputSamplerConfiguration.builder().build(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        PerOperationSamplerConfiguration.builder().build(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        ProbabilisticSamplerConfiguration.builder().build(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        RateLimitingSamplerConfiguration.builder().build(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        RemoteControlledSamplerConfiguration.builder().build(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerReporterConfigurations()));


        Assertions.assertThrows(AppbaseRuntimeException.class, () ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        List.of(new TestJaegerReporterConfiguration())));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        List.of(InMemoryReporterConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        List.of(LoggingReporterConfiguration.builder().build())));
        Assertions.assertDoesNotThrow(() ->
                DefaultTracerServiceConfigurationValidator.validate(
                        defaultTracerServiceConfiguration.getServiceName(),
                        defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                        defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration(),
                        List.of(RemoteReporterConfiguration.builder().build())));
    }

    private static class TestJaegerReporterConfiguration implements JaegerReporterConfiguration {
    }

    private static class TestJaegerSamplerConfiguration implements JaegerSamplerConfiguration {
    }
}
