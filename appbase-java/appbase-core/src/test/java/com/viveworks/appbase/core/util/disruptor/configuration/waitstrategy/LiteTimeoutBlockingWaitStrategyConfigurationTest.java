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
public class LiteTimeoutBlockingWaitStrategyConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final LiteTimeoutBlockingWaitStrategyConfiguration liteTimeoutBlockingWaitStrategyConfiguration
                = LiteTimeoutBlockingWaitStrategyConfiguration
                .builder()
                .timeout(100)
                .units(TimeUnit.SECONDS)
                .enabled(true)
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(LiteTimeoutBlockingWaitStrategyConfiguration.class)
                .writeValueAsString(liteTimeoutBlockingWaitStrategyConfiguration);

        final LiteTimeoutBlockingWaitStrategyConfiguration liteTimeoutBlockingWaitStrategyConfiguration1 = yamlMapper
                .readerFor(LiteTimeoutBlockingWaitStrategyConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                liteTimeoutBlockingWaitStrategyConfiguration.getTimeout(),
                liteTimeoutBlockingWaitStrategyConfiguration1.getTimeout());
        Assertions.assertEquals(
                liteTimeoutBlockingWaitStrategyConfiguration.getUnits(),
                liteTimeoutBlockingWaitStrategyConfiguration1.getUnits());
        Assertions.assertEquals(
                liteTimeoutBlockingWaitStrategyConfiguration.isEnabled(),
                liteTimeoutBlockingWaitStrategyConfiguration1.isEnabled());
        Assertions.assertEquals(
                liteTimeoutBlockingWaitStrategyConfiguration,
                liteTimeoutBlockingWaitStrategyConfiguration1);
        Assertions.assertEquals(
                liteTimeoutBlockingWaitStrategyConfiguration.hashCode(),
                liteTimeoutBlockingWaitStrategyConfiguration1.hashCode());
        Assertions.assertEquals(
                liteTimeoutBlockingWaitStrategyConfiguration.toString(),
                liteTimeoutBlockingWaitStrategyConfiguration1.toString());
    }
}
