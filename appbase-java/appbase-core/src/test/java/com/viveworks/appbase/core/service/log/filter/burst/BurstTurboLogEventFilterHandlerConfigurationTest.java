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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultDuplicateLogHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultLogDeduplicatorConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 */
class BurstTurboLogEventFilterHandlerConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration =
                BurstTurboLogEventFilterHandlerConfiguration
                .builder()
                .burstRateThreshold(3.4)
                .defaultDuplicateLogHandlerConfiguration(DefaultDuplicateLogHandlerConfiguration.builder().build())
                .defaultLogDeduplicatorConfiguration(DefaultLogDeduplicatorConfiguration.builder().build())
                .defaultLogRateCalculatorConfiguration(DefaultLogRateCalculatorConfiguration.builder().build())
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(BurstTurboLogEventFilterHandlerConfiguration.class)
                .writeValueAsString(burstTurboLogEventFilterHandlerConfiguration);

        final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration1 = yamlMapper
                .readerFor(BurstTurboLogEventFilterHandlerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.isEnabled(),
                burstTurboLogEventFilterHandlerConfiguration1.isEnabled());
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.getBurstRateThreshold(),
                burstTurboLogEventFilterHandlerConfiguration1.getBurstRateThreshold());
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.getDefaultDuplicateLogHandlerConfiguration(),
                burstTurboLogEventFilterHandlerConfiguration1.getDefaultDuplicateLogHandlerConfiguration());
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.getDefaultLogDeduplicatorConfiguration(),
                burstTurboLogEventFilterHandlerConfiguration1.getDefaultLogDeduplicatorConfiguration());
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.getDefaultLogRateCalculatorConfiguration(),
                burstTurboLogEventFilterHandlerConfiguration1.getDefaultLogRateCalculatorConfiguration());
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration,
                burstTurboLogEventFilterHandlerConfiguration1);
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.hashCode(),
                burstTurboLogEventFilterHandlerConfiguration1.hashCode());
        Assertions.assertEquals(
                burstTurboLogEventFilterHandlerConfiguration.toString(),
                burstTurboLogEventFilterHandlerConfiguration1.toString());
    }
}
