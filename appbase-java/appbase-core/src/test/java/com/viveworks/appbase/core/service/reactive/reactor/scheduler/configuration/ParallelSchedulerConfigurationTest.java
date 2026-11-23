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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ParallelSchedulerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final ParallelSchedulerConfiguration parallelSchedulerConfiguration =
                ParallelSchedulerConfiguration
                        .builder()
                        .defaultScheduler(false)
                        .id("ID1")
                        .parallelism(4)
                        .threadFactoryConfiguration(
                                ThreadFactoryConfiguration
                                        .builder()
                                        .nameFormat("t-%d")
                                        .build())
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ParallelSchedulerConfiguration.class)
                .writeValueAsString(parallelSchedulerConfiguration);

        final ParallelSchedulerConfiguration parallelSchedulerConfiguration1 = yamlMapper
                .readerFor(ParallelSchedulerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                parallelSchedulerConfiguration,
                parallelSchedulerConfiguration1);
        Assertions.assertEquals(
                parallelSchedulerConfiguration.getParallelism(),
                parallelSchedulerConfiguration1.getParallelism());
        Assertions.assertEquals(
                parallelSchedulerConfiguration.getThreadFactoryConfiguration(),
                parallelSchedulerConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                parallelSchedulerConfiguration.getId(),
                parallelSchedulerConfiguration1.getId());
        Assertions.assertEquals(
                parallelSchedulerConfiguration.isDefaultScheduler(),
                parallelSchedulerConfiguration1.isDefaultScheduler());
        Assertions.assertEquals(
                parallelSchedulerConfiguration.hashCode(),
                parallelSchedulerConfiguration1.hashCode());
        Assertions.assertEquals(
                parallelSchedulerConfiguration.toString(),
                parallelSchedulerConfiguration1.toString());
    }
}
