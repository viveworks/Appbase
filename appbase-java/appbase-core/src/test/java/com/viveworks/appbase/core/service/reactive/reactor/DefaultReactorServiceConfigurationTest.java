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

package com.viveworks.appbase.core.service.reactive.reactor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.BoundedElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ImmediateSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ParallelSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SingleSchedulerConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultReactorServiceConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration =
                DefaultReactorServiceConfiguration
                        .builder()
                        .enabled(true)
                        .schedulerConfigurations(List.of(
                                BoundedElasticSchedulerConfiguration.builder().build(),
                                ElasticSchedulerConfiguration.builder().build(),
                                ExecutorServiceSchedulerConfiguration.builder().build(),
                                ImmediateSchedulerConfiguration.builder().build(),
                                MediatorExecutorServiceSchedulerConfiguration.builder().build(),
                                ParallelSchedulerConfiguration.builder().build(),
                                SingleSchedulerConfiguration.builder().build()))
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultReactorServiceConfiguration.class)
                .writeValueAsString(defaultReactiveServiceConfiguration);
        final DefaultReactorServiceConfiguration defaultReactiveServiceConfiguration1 = yamlMapper
                .readerFor(DefaultReactorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultReactiveServiceConfiguration,
                defaultReactiveServiceConfiguration1);
        Assertions.assertEquals(
                defaultReactiveServiceConfiguration.isEnabled(),
                defaultReactiveServiceConfiguration1.isEnabled());
        Assertions.assertEquals(
                defaultReactiveServiceConfiguration.getSchedulerConfigurations(),
                defaultReactiveServiceConfiguration1.getSchedulerConfigurations());
        Assertions.assertEquals(
                defaultReactiveServiceConfiguration.hashCode(),
                defaultReactiveServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultReactiveServiceConfiguration.toString(),
                defaultReactiveServiceConfiguration1.toString());
    }
}
