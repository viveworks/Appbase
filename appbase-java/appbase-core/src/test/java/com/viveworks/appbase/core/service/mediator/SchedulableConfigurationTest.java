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

package com.viveworks.appbase.core.service.mediator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class SchedulableConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final SchedulableConfiguration schedulableConfiguration = SchedulableConfiguration
                .builder()
                .initialDelay(100L)
                .delay(2000L)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(SchedulableConfiguration.class)
                .writeValueAsString(schedulableConfiguration);
        final SchedulableConfiguration schedulableConfiguration1 = yamlMapper
                .readerFor(SchedulableConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(schedulableConfiguration.getInterval(), schedulableConfiguration1.getInterval());
        Assertions.assertEquals(
                schedulableConfiguration.getInitialDelay(),
                schedulableConfiguration1.getInitialDelay());
        Assertions.assertEquals(schedulableConfiguration.getTimeUnit(), schedulableConfiguration1.getTimeUnit());
        Assertions.assertEquals(schedulableConfiguration.hashCode(), schedulableConfiguration1.hashCode());
        Assertions.assertEquals(schedulableConfiguration, schedulableConfiguration1);
        Assertions.assertEquals(schedulableConfiguration.toString(), schedulableConfiguration1.toString());
    }
}
