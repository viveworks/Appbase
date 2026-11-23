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

import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ConfigurationValueTest {
    @Test
    void testYamlSerde1() throws Exception {
        final ConfigurationValue<Integer> value1 = ConfigurationValue.fromValue(1);
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationValue.class)
                .writeValueAsString(value1);
        Assertions.assertNotNull(yaml);

        final ConfigurationValue<Integer> value2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationValue.class)
                .readValue(yaml);
        Assertions.assertNotNull(value2);

        Assertions.assertEquals(value1.getValue(), value2.getValue());
        Assertions.assertEquals(value1, value2);
        Assertions.assertEquals(value1.hashCode(), value2.hashCode());
        Assertions.assertEquals(value1.toString(), value2.toString());
    }

    @Test
    void testYamlSerde2() throws Exception {
        final ConfigurationValue<Void> value1 = ConfigurationValue.fromValue(null);
        final String yaml = YamlUtil
                .getYamlMapper()
                .writerFor(ConfigurationValue.class)
                .writeValueAsString(value1);
        Assertions.assertNotNull(yaml);

        final ConfigurationValue<Void> value2 = YamlUtil
                .getYamlMapper()
                .readerFor(ConfigurationValue.class)
                .readValue(yaml);
        Assertions.assertNotNull(value2);

        Assertions.assertEquals(value1.getValue(), value2.getValue());
        Assertions.assertEquals(value1, value2);
        Assertions.assertEquals(value1.hashCode(), value2.hashCode());
        Assertions.assertEquals(value1.toString(), value2.toString());
    }
}
