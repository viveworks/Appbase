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

package com.viveworks.appbase.core.service.log.accessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLogAccessorConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration =
                DefaultLogAccessorConfiguration
                        .builder()
                        .enabled(true)
                        .logStoreConfigurations(List.of(LuceneLogStoreConfiguration.builder().build()))
                        .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultLogAccessorConfiguration.class)
                .writeValueAsString(defaultLogAccessorConfiguration);

        final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration1 = yamlMapper
                .readerFor(DefaultLogAccessorConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultLogAccessorConfiguration.getLogStoreConfigurations(),
                defaultLogAccessorConfiguration1.getLogStoreConfigurations());
        Assertions.assertEquals(
                defaultLogAccessorConfiguration.isEnabled(),
                defaultLogAccessorConfiguration1.isEnabled());
        Assertions.assertEquals(
                defaultLogAccessorConfiguration,
                defaultLogAccessorConfiguration1);
        Assertions.assertEquals(
                defaultLogAccessorConfiguration.hashCode(),
                defaultLogAccessorConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultLogAccessorConfiguration.toString(),
                defaultLogAccessorConfiguration1.toString());
    }
}
