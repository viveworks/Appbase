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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExecutorServiceSchedulerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final ExecutorServiceSchedulerConfiguration executorServiceSchedulerConfiguration =
                ExecutorServiceSchedulerConfiguration
                        .builder()
                        .executorName("ExecutorName")
                        .defaultScheduler(false)
                        .id("ID1")
                        .scheduledExecutorServiceConfiguration(ScheduledExecutorServiceConfiguration.builder().build())
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ExecutorServiceSchedulerConfiguration.class)
                .writeValueAsString(executorServiceSchedulerConfiguration);

        final ExecutorServiceSchedulerConfiguration executorServiceSchedulerConfiguration1 = yamlMapper
                .readerFor(ExecutorServiceSchedulerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                executorServiceSchedulerConfiguration,
                executorServiceSchedulerConfiguration1);
        Assertions.assertEquals(
                executorServiceSchedulerConfiguration.getExecutorName(),
                executorServiceSchedulerConfiguration1.getExecutorName());
        Assertions.assertEquals(
                executorServiceSchedulerConfiguration.getId(),
                executorServiceSchedulerConfiguration1.getId());
        Assertions.assertEquals(
                executorServiceSchedulerConfiguration.getScheduledExecutorServiceConfiguration(),
                executorServiceSchedulerConfiguration1.getScheduledExecutorServiceConfiguration());
        Assertions.assertEquals(
                executorServiceSchedulerConfiguration.isDefaultScheduler(),
                executorServiceSchedulerConfiguration1.isDefaultScheduler());
        Assertions.assertEquals(
                executorServiceSchedulerConfiguration.hashCode(),
                executorServiceSchedulerConfiguration1.hashCode());
        Assertions.assertEquals(
                executorServiceSchedulerConfiguration.toString(),
                executorServiceSchedulerConfiguration1.toString());
    }
}
