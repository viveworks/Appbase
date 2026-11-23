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

package com.viveworks.appbase.core.service.log.filter.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DisruptorLogAndTurboLogEventFilterDelegatorConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration =
                DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                .builder()
                .acceptLogAfterTurboLogEventListeners(true)
                .acceptLogAfterLogEventListeners(true)
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.class)
                .writeValueAsString(disruptorLogAndTurboLogEventFilterDelegatorConfiguration);

        final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1 = yamlMapper
                .readerFor(DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isAcceptLogAfterLogEventListeners(),
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1.isAcceptLogAfterLogEventListeners());
        Assertions.assertEquals(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isAcceptLogAfterTurboLogEventListeners(),
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1.isAcceptLogAfterTurboLogEventListeners());
        Assertions.assertEquals(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isEnabled(),
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1.isEnabled());
        Assertions.assertEquals(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1);
        Assertions.assertEquals(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.hashCode(),
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.toString(),
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration1.toString());
    }
}
