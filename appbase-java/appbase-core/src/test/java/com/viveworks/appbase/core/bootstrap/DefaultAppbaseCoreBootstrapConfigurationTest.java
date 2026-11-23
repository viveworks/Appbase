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

package com.viveworks.appbase.core.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultAppbaseCoreBootstrapConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultAppbaseCoreBootstrapConfiguration defaultAppbaseBootstrapConfiguration =
                DefaultAppbaseCoreBootstrapConfiguration
                .builder()
                .defaultConfigurationServiceConfiguration(DefaultConfigurationServiceConfiguration.builder().build())
                .defaultDisruptorServiceConfiguration(DefaultDisruptorServiceConfiguration.builder().build())
                .defaultExceptionServiceConfiguration(DefaultExceptionServiceConfiguration.builder().build())
                .defaultJmxServiceConfiguration(DefaultJmxServiceConfiguration.builder().build())
                .defaultLogServiceConfiguration(DefaultLogServiceConfiguration.builder().build())
                .defaultLuceneServiceConfiguration(DefaultLuceneServiceConfiguration.builder().build())
                .defaultMediatorServiceConfiguration(DefaultMediatorServiceConfiguration.builder().build())
                .defaultMetricServiceConfiguration(DefaultMetricServiceConfiguration.builder().build())
                .defaultReactiveServiceConfiguration(DefaultReactorServiceConfiguration.builder().build())
                .defaultServiceRegistryConfiguration(DefaultServiceRegistryConfiguration.builder().build())
                .defaultTracerServiceConfiguration(DefaultTracerServiceConfiguration.builder().build())
                .defaultValidationServiceConfiguration(new DefaultValidationServiceConfiguration())
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultAppbaseCoreBootstrapConfiguration.class)
                .writeValueAsString(defaultAppbaseBootstrapConfiguration);

        final DefaultAppbaseCoreBootstrapConfiguration defaultAppbaseBootstrapConfiguration1 = yamlMapper
                .readerFor(DefaultAppbaseCoreBootstrapConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getConfigurationServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getConfigurationServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getDisruptorServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getDisruptorServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getExceptionServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getExceptionServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getJmxServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getJmxServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getLogServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getLogServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getLuceneServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getLuceneServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getMediatorServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getMediatorServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getMetricServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getMetricServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getReactorServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getReactorServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getServiceRegistryConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getServiceRegistryConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getTracerServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getTracerServiceConfiguration());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.getValidationServiceConfiguration(),
                defaultAppbaseBootstrapConfiguration1.getValidationServiceConfiguration());
        Assertions.assertEquals(defaultAppbaseBootstrapConfiguration, defaultAppbaseBootstrapConfiguration1);
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.hashCode(),
                defaultAppbaseBootstrapConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultAppbaseBootstrapConfiguration.toString(),
                defaultAppbaseBootstrapConfiguration1.toString());
    }
}
