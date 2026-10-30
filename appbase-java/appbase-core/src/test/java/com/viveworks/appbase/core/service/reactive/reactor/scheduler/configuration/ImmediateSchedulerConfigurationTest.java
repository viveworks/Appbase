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
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ImmediateSchedulerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final ImmediateSchedulerConfiguration immediateSchedulerConfiguration =
                ImmediateSchedulerConfiguration
                        .builder()
                        .defaultScheduler(false)
                        .id("ID1")
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ImmediateSchedulerConfiguration.class)
                .writeValueAsString(immediateSchedulerConfiguration);

        final ImmediateSchedulerConfiguration immediateSchedulerConfiguration1 = yamlMapper
                .readerFor(ImmediateSchedulerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                immediateSchedulerConfiguration,
                immediateSchedulerConfiguration1);
        Assertions.assertEquals(
                immediateSchedulerConfiguration.getId(),
                immediateSchedulerConfiguration1.getId());
        Assertions.assertEquals(
                immediateSchedulerConfiguration.isDefaultScheduler(),
                immediateSchedulerConfiguration1.isDefaultScheduler());
        Assertions.assertEquals(
                immediateSchedulerConfiguration.hashCode(),
                immediateSchedulerConfiguration1.hashCode());
        Assertions.assertEquals(
                immediateSchedulerConfiguration.toString(),
                immediateSchedulerConfiguration1.toString());
    }
}
