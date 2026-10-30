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
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class BoundedElasticSchedulerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final BoundedElasticSchedulerConfiguration boundedElasticSchedulerConfiguration =
                BoundedElasticSchedulerConfiguration
                        .builder()
                        .defaultScheduler(false)
                        .id("ID1")
                        .queuedTaskCap(123)
                        .threadCap(5)
                        .threadFactoryConfiguration(
                                ThreadFactoryConfiguration
                                        .builder()
                                        .nameFormat("t-%d")
                                        .build())
                        .ttlSeconds(5)
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(BoundedElasticSchedulerConfiguration.class)
                .writeValueAsString(boundedElasticSchedulerConfiguration);

        final BoundedElasticSchedulerConfiguration boundedElasticSchedulerConfiguration1 = yamlMapper
                .readerFor(BoundedElasticSchedulerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration,
                boundedElasticSchedulerConfiguration1);
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.getId(),
                boundedElasticSchedulerConfiguration1.getId());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.getThreadFactoryConfiguration(),
                boundedElasticSchedulerConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.getQueuedTaskCap(),
                boundedElasticSchedulerConfiguration1.getQueuedTaskCap());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.getThreadCap(),
                boundedElasticSchedulerConfiguration1.getThreadCap());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.getTtlSeconds(),
                boundedElasticSchedulerConfiguration1.getTtlSeconds());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.isDefaultScheduler(),
                boundedElasticSchedulerConfiguration1.isDefaultScheduler());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.hashCode(),
                boundedElasticSchedulerConfiguration1.hashCode());
        Assertions.assertEquals(
                boundedElasticSchedulerConfiguration.toString(),
                boundedElasticSchedulerConfiguration1.toString());
    }
}
