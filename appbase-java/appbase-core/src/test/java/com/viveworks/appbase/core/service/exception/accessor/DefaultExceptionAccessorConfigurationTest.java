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

package com.viveworks.appbase.core.service.exception.accessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultExceptionAccessorConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration =
                DefaultExceptionAccessorConfiguration
                .builder()
                .exceptionStoreConfigurations(List.of(InMemoryExceptionStoreConfiguration.builder().build()))
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultExceptionAccessorConfiguration.class)
                .writeValueAsString(defaultExceptionAccessorConfiguration);
        final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration1 = yamlMapper
                .readerFor(DefaultExceptionAccessorConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultExceptionAccessorConfiguration.getExceptionStoreConfigurations(),
                defaultExceptionAccessorConfiguration1.getExceptionStoreConfigurations());
        Assertions.assertEquals(
                defaultExceptionAccessorConfiguration,
                defaultExceptionAccessorConfiguration1);
        Assertions.assertEquals(
                defaultExceptionAccessorConfiguration.hashCode(),
                defaultExceptionAccessorConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultExceptionAccessorConfiguration.toString(),
                defaultExceptionAccessorConfiguration1.toString());
    }
}
