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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultTracerServiceConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration = DefaultTracerServiceConfiguration
                .builder()
                .jaegerPropagationConfiguration(JaegerPropagationConfiguration.builder().build())
                .jaegerSamplerConfiguration(ConstSamplerConfiguration.builder().build())
                .serviceName("serviceName")
                .expandExceptionLogs(false)
                .jaegerReporterConfigurations(List.of(LoggingReporterConfiguration.builder().build()))
                .use128BitTraceId(true)
                .tags(Map.of("k1", "v1"))
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultTracerServiceConfiguration.class)
                .writeValueAsString(defaultTracerServiceConfiguration);
        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration1 = yamlMapper
                .readerFor(DefaultTracerServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultTracerServiceConfiguration.getJaegerPropagationConfiguration(),
                defaultTracerServiceConfiguration1.getJaegerPropagationConfiguration());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.getJaegerReporterConfigurations(),
                defaultTracerServiceConfiguration1.getJaegerReporterConfigurations());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.getJaegerSamplerConfiguration(),
                defaultTracerServiceConfiguration1.getJaegerSamplerConfiguration());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.getServiceName(),
                defaultTracerServiceConfiguration1.getServiceName());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.isExpandExceptionLogs(),
                defaultTracerServiceConfiguration1.isExpandExceptionLogs());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.isUse128BitTraceId(),
                defaultTracerServiceConfiguration1.isUse128BitTraceId());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.getTags(),
                defaultTracerServiceConfiguration1.getTags());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration,
                defaultTracerServiceConfiguration1);
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.hashCode(),
                defaultTracerServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultTracerServiceConfiguration.toString(),
                defaultTracerServiceConfiguration1.toString());
    }
}
