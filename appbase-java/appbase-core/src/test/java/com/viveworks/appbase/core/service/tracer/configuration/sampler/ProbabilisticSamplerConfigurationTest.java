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

package com.viveworks.appbase.core.service.tracer.configuration.sampler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ProbabilisticSamplerConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final ProbabilisticSamplerConfiguration probabilisticSamplerConfiguration = ProbabilisticSamplerConfiguration
                .builder()
                .samplingRate(11.223)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(ProbabilisticSamplerConfiguration.class)
                .writeValueAsString(probabilisticSamplerConfiguration);
        final ProbabilisticSamplerConfiguration probabilisticSamplerConfiguration1 = yamlMapper
                .readerFor(ProbabilisticSamplerConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                probabilisticSamplerConfiguration.getSamplingRate(),
                probabilisticSamplerConfiguration1.getSamplingRate());
        Assertions.assertEquals(
                probabilisticSamplerConfiguration,
                probabilisticSamplerConfiguration1);
        Assertions.assertEquals(
                probabilisticSamplerConfiguration.hashCode(),
                probabilisticSamplerConfiguration1.hashCode());
        Assertions.assertEquals(
                probabilisticSamplerConfiguration.toString(),
                probabilisticSamplerConfiguration1.toString());
    }
}
