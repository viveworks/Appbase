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

package com.viveworks.appbase.core.service.mediator.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExecutorServiceConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final ExecutorServiceConfiguration executorServiceConfiguration = ExecutorServiceConfiguration
                .builder()
                .executorType(ExecutorServiceType.FIXED)
                .fixedThreadPoolSize(4)
                .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ExecutorServiceConfiguration.class)
                .writeValueAsString(executorServiceConfiguration);
        final ExecutorServiceConfiguration executorServiceConfiguration1 = yamlMapper
                .readerFor(ExecutorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                executorServiceConfiguration.getExecutorServiceType(),
                executorServiceConfiguration1.getExecutorServiceType());
        Assertions.assertEquals(
                executorServiceConfiguration.getFixedThreadPoolSize(),
                executorServiceConfiguration1.getFixedThreadPoolSize());
        Assertions.assertEquals(
                executorServiceConfiguration.getThreadFactoryConfiguration(),
                executorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(executorServiceConfiguration, executorServiceConfiguration1);
        Assertions.assertEquals(executorServiceConfiguration.hashCode(), executorServiceConfiguration1.hashCode());
        Assertions.assertEquals(executorServiceConfiguration.toString(), executorServiceConfiguration1.toString());
    }
}
