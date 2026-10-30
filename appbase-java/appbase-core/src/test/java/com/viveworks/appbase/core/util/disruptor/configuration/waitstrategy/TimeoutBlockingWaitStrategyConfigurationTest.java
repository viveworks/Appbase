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

package com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class TimeoutBlockingWaitStrategyConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final TimeoutBlockingWaitStrategyConfiguration timeoutBlockingWaitStrategyConfiguration =
                TimeoutBlockingWaitStrategyConfiguration
                .builder()
                .timeout(100L)
                .units(TimeUnit.SECONDS)
                .enabled(true)
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(TimeoutBlockingWaitStrategyConfiguration.class)
                .writeValueAsString(timeoutBlockingWaitStrategyConfiguration);

        final TimeoutBlockingWaitStrategyConfiguration timeoutBlockingWaitStrategyConfiguration1 = yamlMapper
                .readerFor(TimeoutBlockingWaitStrategyConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                timeoutBlockingWaitStrategyConfiguration.getTimeout(),
                timeoutBlockingWaitStrategyConfiguration1.getTimeout());
        Assertions.assertEquals(
                timeoutBlockingWaitStrategyConfiguration.getUnits(),
                timeoutBlockingWaitStrategyConfiguration1.getUnits());
        Assertions.assertEquals(
                timeoutBlockingWaitStrategyConfiguration.isEnabled(),
                timeoutBlockingWaitStrategyConfiguration1.isEnabled());
        Assertions.assertEquals(
                timeoutBlockingWaitStrategyConfiguration,
                timeoutBlockingWaitStrategyConfiguration1);
        Assertions.assertEquals(
                timeoutBlockingWaitStrategyConfiguration.hashCode(),
                timeoutBlockingWaitStrategyConfiguration1.hashCode());
        Assertions.assertEquals(
                timeoutBlockingWaitStrategyConfiguration.toString(),
                timeoutBlockingWaitStrategyConfiguration1.toString());
    }
}
