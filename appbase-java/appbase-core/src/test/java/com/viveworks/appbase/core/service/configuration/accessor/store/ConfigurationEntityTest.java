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

package com.viveworks.appbase.core.service.configuration.accessor.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationEntityTest {
    @Test
    void testSerde() throws Exception {
        final ConfigurationEntity configurationEntity = new ConfigurationEntity();
        configurationEntity.setId(1);
        configurationEntity.setDiscriminator("US-EAST");
        configurationEntity.setKey(DefaultConfigurationServiceConfiguration.class.getName());
        configurationEntity.setKeyJavaTypeName(DefaultConfigurationServiceConfiguration.class.getName());

        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultConfigurationServiceConfiguration.class)
                .writeValueAsString(DefaultConfigurationServiceConfiguration.builder().build());
        configurationEntity.setValue(yaml);
        configurationEntity.setValueMimeType(MimeType.JSON.getValue());
        configurationEntity.setLastUpdatedTimestamp(Date.from(Instant.now()));

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml1 = yamlMapper
                .writerFor(ConfigurationEntity.class)
                .writeValueAsString(configurationEntity);
        final ConfigurationEntity configurationEntity1 = yamlMapper
                .readerFor(ConfigurationEntity.class)
                .readValue(yaml1);

        Assertions.assertEquals(
                configurationEntity.getId(),
                configurationEntity1.getId());
        Assertions.assertEquals(
                configurationEntity.getDiscriminator(),
                configurationEntity1.getDiscriminator());
        Assertions.assertEquals(
                configurationEntity.getKey(),
                configurationEntity1.getKey());
        Assertions.assertEquals(
                configurationEntity.getKeyJavaTypeName(),
                configurationEntity1.getKeyJavaTypeName());
        Assertions.assertEquals(
                configurationEntity.getValue(),
                configurationEntity1.getValue());
        Assertions.assertEquals(
                configurationEntity.getValueMimeType(),
                configurationEntity1.getValueMimeType());
        Assertions.assertEquals(
                configurationEntity.getLastUpdatedTimestamp(),
                configurationEntity1.getLastUpdatedTimestamp());
        Assertions.assertEquals(
                configurationEntity,
                configurationEntity1);
        Assertions.assertEquals(
                configurationEntity.hashCode(),
                configurationEntity1.hashCode());
        Assertions.assertEquals(
                configurationEntity.toString(),
                configurationEntity1.toString());
    }
}
