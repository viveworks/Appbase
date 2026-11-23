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

package com.viveworks.appbase.core.service.configuration;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationKeyTest {
    @Test
    void testYamlSerde1() throws IOException {
        final ConfigurationKey configurationKey1 = ConfigurationKey
                .fromPropertyKey("abcd");
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationKey.class)
                .writeValueAsString(configurationKey1);
        Assertions.assertNotNull(yaml);

        final ConfigurationKey configurationKey2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationKey.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationKey2);

        Assertions.assertFalse(configurationKey2.isTyped());
        Assertions.assertEquals(configurationKey1.getPropertyKey(), configurationKey2.getPropertyKey());
        Assertions.assertEquals(configurationKey1.getTypeKey(), configurationKey2.getTypeKey());
        Assertions.assertEquals(configurationKey1.getSource(), configurationKey2.getSource());

        Assertions.assertEquals(0, configurationKey2.compareTo(configurationKey1));
        Assertions.assertEquals(configurationKey1, configurationKey2);
        Assertions.assertEquals(configurationKey1.hashCode(), configurationKey2.hashCode());
        Assertions.assertEquals(configurationKey1.toString(), configurationKey2.toString());
    }

    @Test
    void testYamlSerde2() throws IOException {
        final ConfigurationKey configurationKey1 = ConfigurationKey.from("cde", ConfigurationAccessor.class);
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationKey.class)
                .writeValueAsString(configurationKey1);
        Assertions.assertNotNull(yaml);

        final ConfigurationKey configurationKey2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationKey.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationKey2);

        Assertions.assertFalse(configurationKey2.isTyped());
        Assertions.assertEquals(configurationKey1.getPropertyKey(), configurationKey2.getPropertyKey());
        Assertions.assertEquals(configurationKey1.getTypeKey(), configurationKey2.getTypeKey());
        Assertions.assertEquals(configurationKey1.getSource(), configurationKey2.getSource());

        Assertions.assertEquals(0, configurationKey2.compareTo(configurationKey1));
        Assertions.assertEquals(configurationKey1, configurationKey2);
        Assertions.assertEquals(configurationKey1.hashCode(), configurationKey2.hashCode());
        Assertions.assertEquals(configurationKey1.toString(), configurationKey2.toString());
    }

    @Test
    void testYamlSerde3() throws IOException {
        final ConfigurationKey configurationKey1 = ConfigurationKey.fromTypeKey(AppbaseConfiguration.class);
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationKey.class)
                .writeValueAsString(configurationKey1);
        Assertions.assertNotNull(yaml);

        final ConfigurationKey configurationKey2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationKey.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationKey2);

        Assertions.assertTrue(configurationKey2.isTyped());
        Assertions.assertEquals(configurationKey1.getPropertyKey(), configurationKey2.getPropertyKey());
        Assertions.assertEquals(configurationKey1.getTypeKey(), configurationKey2.getTypeKey());
        Assertions.assertEquals(configurationKey1.getSource(), configurationKey2.getSource());

        Assertions.assertEquals(0, configurationKey2.compareTo(configurationKey1));
        Assertions.assertEquals(configurationKey1, configurationKey2);
        Assertions.assertEquals(configurationKey1.hashCode(), configurationKey2.hashCode());
        Assertions.assertEquals(configurationKey1.toString(), configurationKey2.toString());
    }

    @Test
    void testYamlSerde4() throws IOException {
        final ConfigurationKey configurationKey1 = ConfigurationKey.from(
                AppbaseConfiguration.class, ConfigurationAccessor.class);
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationKey.class)
                .writeValueAsString(configurationKey1);
        Assertions.assertNotNull(yaml);

        final ConfigurationKey configurationKey2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationKey.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationKey2);

        Assertions.assertTrue(configurationKey2.isTyped());
        Assertions.assertEquals(configurationKey1.getPropertyKey(), configurationKey2.getPropertyKey());
        Assertions.assertEquals(configurationKey1.getTypeKey(), configurationKey2.getTypeKey());
        Assertions.assertEquals(configurationKey1.getSource(), configurationKey2.getSource());

        Assertions.assertEquals(0, configurationKey2.compareTo(configurationKey1));
        Assertions.assertEquals(configurationKey1, configurationKey2);
        Assertions.assertEquals(configurationKey1.hashCode(), configurationKey2.hashCode());
        Assertions.assertEquals(configurationKey1.toString(), configurationKey2.toString());
    }

    @Test
    void testYamlSerde5() throws IOException {
        final ConfigurationKey configurationKey1 = ConfigurationKey.from(
                "zyx", null, ConfigurationAccessor.class);
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationKey.class)
                .writeValueAsString(configurationKey1);
        Assertions.assertNotNull(yaml);

        final ConfigurationKey configurationKey2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationKey.class)
                .readValue(yaml);
        Assertions.assertNotNull(configurationKey2);

        Assertions.assertFalse(configurationKey2.isTyped());
        Assertions.assertEquals(configurationKey1.getPropertyKey(), configurationKey2.getPropertyKey());
        Assertions.assertEquals(configurationKey1.getTypeKey(), configurationKey2.getTypeKey());
        Assertions.assertEquals(configurationKey1.getSource(), configurationKey2.getSource());

        Assertions.assertEquals(0, configurationKey2.compareTo(configurationKey1));
        Assertions.assertEquals(configurationKey1, configurationKey2);
        Assertions.assertEquals(configurationKey1.hashCode(), configurationKey2.hashCode());
        Assertions.assertEquals(configurationKey1.toString(), configurationKey2.toString());
    }
}
