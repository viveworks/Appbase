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

package com.viveworks.appbase.core.service.metric.jmx.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JmxMeterRegistryConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration = JmxMeterRegistryConfiguration
                .builder()
                .enabled(true)
                .additionalProperties(Map.of("k1", "v1"))
                .domain("domain1")
                .prefix("prefix1")
                .step(Duration.ofMillis(1000))
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(JmxMeterRegistryConfiguration.class)
                .writeValueAsString(jmxMeterRegistryConfiguration);
        final JmxMeterRegistryConfiguration jmxMeterRegistryConfiguration1 = yamlMapper
                .readerFor(JmxMeterRegistryConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.domain(),
                jmxMeterRegistryConfiguration1.domain());
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.prefix(),
                jmxMeterRegistryConfiguration1.prefix());
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.isEnabled(),
                jmxMeterRegistryConfiguration1.isEnabled());
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.step(),
                jmxMeterRegistryConfiguration1.step());
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.getAdditionalProperties(),
                jmxMeterRegistryConfiguration1.getAdditionalProperties());
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.get("no-key"),
                jmxMeterRegistryConfiguration1.get("no-key"));
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration,
                jmxMeterRegistryConfiguration1);
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.hashCode(),
                jmxMeterRegistryConfiguration1.hashCode());
        Assertions.assertEquals(
                jmxMeterRegistryConfiguration.toString(),
                jmxMeterRegistryConfiguration1.toString());
    }
}
