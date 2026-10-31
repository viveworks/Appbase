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

package com.viveworks.appbase.core.service.tracer.configuration.sampler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class PerOperationSamplerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final PerOperationSamplerConfiguration perOperationSamplerConfiguration = PerOperationSamplerConfiguration
                .builder()
                .defaultLowerBoundTracesPerSecond(13.21)
                .defaultSamplingProbability(32.11)
                .maxOperations(10)
                .samplingRateByOperationNameMap(Map.of("operation", 123.456))
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(PerOperationSamplerConfiguration.class)
                .writeValueAsString(perOperationSamplerConfiguration);
        final PerOperationSamplerConfiguration perOperationSamplerConfiguration1 = yamlMapper
                .readerFor(PerOperationSamplerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                perOperationSamplerConfiguration.getDefaultLowerBoundTracesPerSecond(),
                perOperationSamplerConfiguration1.getDefaultLowerBoundTracesPerSecond());
        Assertions.assertEquals(
                perOperationSamplerConfiguration.getDefaultSamplingProbability(),
                perOperationSamplerConfiguration1.getDefaultSamplingProbability());
        Assertions.assertEquals(
                perOperationSamplerConfiguration.getMaxOperations(),
                perOperationSamplerConfiguration1.getMaxOperations());
        Assertions.assertEquals(
                perOperationSamplerConfiguration.getSamplingRateByOperationNameMap(),
                perOperationSamplerConfiguration1.getSamplingRateByOperationNameMap());
        Assertions.assertEquals(
                perOperationSamplerConfiguration,
                perOperationSamplerConfiguration1);
        Assertions.assertEquals(
                perOperationSamplerConfiguration.hashCode(),
                perOperationSamplerConfiguration1.hashCode());
        Assertions.assertEquals(
                perOperationSamplerConfiguration.toString(),
                perOperationSamplerConfiguration1.toString());
    }
}
