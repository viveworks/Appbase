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

package com.viveworks.appbase.core.service.tracer.configuration.propagation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JaegerPropagationConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final JaegerPropagationConfiguration jaegerPropagationConfiguration = JaegerPropagationConfiguration
                .builder()
                .jaegerPropagationTypes(Set.of(JaegerPropagationType.B3))
                .baggagePrefix("prefix-")
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(JaegerPropagationConfiguration.class)
                .writeValueAsString(jaegerPropagationConfiguration);
        final JaegerPropagationConfiguration jaegerPropagationConfiguration1 = yamlMapper
                .readerFor(JaegerPropagationConfiguration.class)
                .readValue(yaml);

        final Set<JaegerPropagationType> jaegerPropagationTypes =
                jaegerPropagationConfiguration1.getJaegerPropagationTypes();
        Assertions.assertFalse(jaegerPropagationTypes.isEmpty());
        jaegerPropagationTypes.forEach(jaegerPropagationType -> {
            Assertions.assertNotNull(jaegerPropagationType.getValue());
        });


        Assertions.assertEquals(
                jaegerPropagationConfiguration,
                jaegerPropagationConfiguration1);
        Assertions.assertEquals(
                jaegerPropagationConfiguration.getJaegerPropagationTypes(),
                jaegerPropagationConfiguration1.getJaegerPropagationTypes());
        Assertions.assertEquals(
                jaegerPropagationConfiguration.getBaggagePrefix(),
                jaegerPropagationConfiguration1.getBaggagePrefix());
        Assertions.assertEquals(
                jaegerPropagationConfiguration,
                jaegerPropagationConfiguration1);
        Assertions.assertEquals(
                jaegerPropagationConfiguration.hashCode(),
                jaegerPropagationConfiguration1.hashCode());
        Assertions.assertEquals(
                jaegerPropagationConfiguration.toString(),
                jaegerPropagationConfiguration1.toString());
    }
}
