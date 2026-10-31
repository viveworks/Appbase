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

package com.viveworks.appbase.core.service.tracer.configuration.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class InMemoryReporterConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final InMemoryReporterConfiguration inMemoryReporterConfiguration = InMemoryReporterConfiguration
                .builder()
                .enabled(false)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(InMemoryReporterConfiguration.class)
                .writeValueAsString(inMemoryReporterConfiguration);
        final InMemoryReporterConfiguration inMemoryReporterConfiguration1 = yamlMapper
                .readerFor(InMemoryReporterConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                inMemoryReporterConfiguration,
                inMemoryReporterConfiguration1);
        Assertions.assertEquals(
                inMemoryReporterConfiguration.isEnabled(),
                inMemoryReporterConfiguration1.isEnabled());
        Assertions.assertEquals(
                inMemoryReporterConfiguration,
                inMemoryReporterConfiguration1);
        Assertions.assertEquals(
                inMemoryReporterConfiguration.hashCode(),
                inMemoryReporterConfiguration1.hashCode());
        Assertions.assertEquals(
                inMemoryReporterConfiguration.toString(),
                inMemoryReporterConfiguration1.toString());
    }
}
