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

package com.viveworks.appbase.core.service.exception.store.inmemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class InMemoryExceptionStoreConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration =
                InMemoryExceptionStoreConfiguration
                .builder()
                .discriminator("DISCRMINATOR")
                .stackTraceSerializationMaxLength(2048)
                .enabled(true)
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(InMemoryExceptionStoreConfiguration.class)
                .writeValueAsString(inMemoryExceptionStoreConfiguration);

        final InMemoryExceptionStoreConfiguration inMemoryExceptionStoreConfiguration1 = yamlMapper
                .readerFor(InMemoryExceptionStoreConfiguration.class)
                .readValue(yaml);
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration.getStackTraceSerializationMaxLength(),
                inMemoryExceptionStoreConfiguration1.getStackTraceSerializationMaxLength());
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration.getDiscriminator(),
                inMemoryExceptionStoreConfiguration1.getDiscriminator());
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration.getExceptionStoreType(),
                inMemoryExceptionStoreConfiguration1.getExceptionStoreType());
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration.isEnabled(),
                inMemoryExceptionStoreConfiguration1.isEnabled());
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration,
                inMemoryExceptionStoreConfiguration1);
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration.hashCode(),
                inMemoryExceptionStoreConfiguration1.hashCode());
        Assertions.assertEquals(
                inMemoryExceptionStoreConfiguration.toString(),
                inMemoryExceptionStoreConfiguration1.toString());
    }
}
